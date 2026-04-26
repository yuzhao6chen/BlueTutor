from __future__ import annotations

import hashlib
import json
import os
import tempfile
import time
import uuid
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener

from alibabacloud_credentials.client import Client as CredClient
from alibabacloud_docmind_api20220711.client import Client as DocMindClient
from alibabacloud_docmind_api20220711 import models as docmind_models
from alibabacloud_tea_openapi import models as open_api_models
from alibabacloud_tea_util import models as util_models


DEFAULT_ENV_PATH = Path(__file__).resolve().parents[2] / ".env"
DEFAULT_ENDPOINT = "docmind-api.cn-hangzhou.aliyuncs.com"
DEFAULT_CACHE_DIR = Path(__file__).resolve().parents[2] / "data" / "shared_doc_parse_cache"
DEFAULT_LAYOUT_STEP_SIZE = 200
DEFAULT_POLL_INTERVAL_SECONDS = 3
DEFAULT_MAX_WAIT_SECONDS = 240
_DIRECT_HTTP_OPENER = build_opener(ProxyHandler({}))

_SUPPORTED_EXTENSIONS = {
    ".pdf",
    ".doc",
    ".docx",
    ".ppt",
    ".pptx",
    ".xls",
    ".xlsx",
    ".xlsm",
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".gif",
    ".md",
    ".markdown",
    ".html",
    ".epub",
    ".mobi",
    ".rtf",
    ".txt",
}
_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".gif"}


def _load_env_file(env_path: Path) -> None:
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


class DocumentParserAgent:
    def __init__(
        self,
        *,
        endpoint: str = DEFAULT_ENDPOINT,
        env_path: Path | None = None,
        cache_dir: Path | None = None,
        poll_interval_seconds: int = DEFAULT_POLL_INTERVAL_SECONDS,
        max_wait_seconds: int = DEFAULT_MAX_WAIT_SECONDS,
        layout_step_size: int = DEFAULT_LAYOUT_STEP_SIZE,
    ) -> None:
        self.env_path = env_path or DEFAULT_ENV_PATH
        _load_env_file(self.env_path)

        access_key_id = os.getenv("ACCESS_KEY_ID")
        access_key_secret = os.getenv("ACCESS_KEY_SECRET")
        if not access_key_id or not access_key_secret:
            raise ValueError("ACCESS_KEY_ID / ACCESS_KEY_SECRET 未配置，无法调用阿里云文档解析服务。")

        config = open_api_models.Config(
            access_key_id=access_key_id,
            access_key_secret=access_key_secret,
        )
        config.endpoint = endpoint

        self.client = DocMindClient(config)
        self.runtime = util_models.RuntimeOptions()
        self.cache_dir = cache_dir or DEFAULT_CACHE_DIR
        self.poll_interval_seconds = poll_interval_seconds
        self.max_wait_seconds = max_wait_seconds
        self.layout_step_size = layout_step_size

    def parse_local_file(
        self,
        file_path: str | Path,
        *,
        llm_enhancement: bool = True,
        formula_enhancement: bool = True,
    ) -> dict[str, Any]:
        source_path = Path(file_path)
        if not source_path.exists():
            raise FileNotFoundError(f"文档文件不存在：{source_path}")
        file_bytes = source_path.read_bytes()
        return self.parse_bytes(
            file_bytes=file_bytes,
            file_name=source_path.name,
            llm_enhancement=llm_enhancement,
            formula_enhancement=formula_enhancement,
        )

    def parse_bytes(
        self,
        *,
        file_bytes: bytes,
        file_name: str,
        llm_enhancement: bool = True,
        formula_enhancement: bool = True,
    ) -> dict[str, Any]:
        normalized_name = file_name.strip() or f"upload_{uuid.uuid4().hex[:8]}.bin"
        suffix = Path(normalized_name).suffix.lower()
        if suffix not in _SUPPORTED_EXTENSIONS:
            raise ValueError(f"当前不支持的文件类型：{suffix or '无后缀'}")
        if not file_bytes:
            raise ValueError("上传文件为空")

        self._validate_size(file_bytes=file_bytes, suffix=suffix)

        cache_key = self._build_cache_key(
            file_bytes=file_bytes,
            file_name=normalized_name,
            llm_enhancement=llm_enhancement,
            formula_enhancement=formula_enhancement,
        )
        cached = self._load_cached_result(cache_key)
        if cached is not None:
            cached["cache_hit"] = True
            return cached

        temp_dir = self.cache_dir / "_uploads"
        temp_dir.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix, dir=temp_dir) as temp_file:
            temp_file.write(file_bytes)
            temp_path = Path(temp_file.name)

        try:
            job_id = self._submit_job(
                file_path=temp_path,
                file_name=normalized_name,
                llm_enhancement=llm_enhancement,
                formula_enhancement=formula_enhancement,
            )
            status_data = self._wait_for_completion(job_id)
            markdown_text, raw_layouts = self._collect_markdown(job_id)
            result = {
                "file_id": f"doc_{uuid.uuid4().hex[:8]}",
                "file_name": normalized_name,
                "file_extension": suffix.lstrip("."),
                "file_size_bytes": len(file_bytes),
                "docmind_job_id": job_id,
                "status": "success",
                "markdown_text": markdown_text,
                "plain_text": _markdown_to_text(markdown_text),
                "cache_hit": False,
                "page_count_estimate": _lookup_any(status_data, "PageCountEstimate", "pageCountEstimate"),
                "paragraph_count": _lookup_any(status_data, "ParagraphCount", "paragraphCount"),
                "token_count": _lookup_any(status_data, "Tokens", "tokens"),
                "image_count": _lookup_any(status_data, "ImageCount", "imageCount"),
                "table_count": _lookup_any(status_data, "TableCount", "tableCount"),
                "llm_enhancement": llm_enhancement,
                "formula_enhancement": formula_enhancement,
                "raw_layout_count": len(raw_layouts),
            }
            self._save_cached_result(cache_key, result)
            return result
        finally:
            temp_path.unlink(missing_ok=True)

    def _submit_job(
        self,
        *,
        file_path: Path,
        file_name: str,
        llm_enhancement: bool,
        formula_enhancement: bool,
    ) -> str:
        with file_path.open("rb") as file_stream:
            request = docmind_models.SubmitDocParserJobAdvanceRequest(
                file_url_object=file_stream,
                file_name=file_name,
                file_name_extension=Path(file_name).suffix.lstrip(".") or None,
                llm_enhancement=llm_enhancement,
                enhancement_mode="VLM" if llm_enhancement else None,
                formula_enhancement=formula_enhancement,
                output_format=["markdown"],
                output_html_table=True,
            )
            try:
                response = self.client.submit_doc_parser_job_advance(request, self.runtime)
            except Exception as exc:
                raise RuntimeError(f"提交阿里云文档解析任务失败：{exc}") from exc

        body = response.body
        task_id = getattr(getattr(body, "data", None), "id", None)
        if not task_id:
            raise RuntimeError("阿里云文档解析提交成功但未返回任务 ID")
        return str(task_id)

    def _wait_for_completion(self, job_id: str) -> dict[str, Any]:
        deadline = time.monotonic() + self.max_wait_seconds
        while time.monotonic() < deadline:
            request = docmind_models.QueryDocParserStatusRequest(id=job_id)
            try:
                response = self.client.query_doc_parser_status(request)
            except Exception as exc:
                raise RuntimeError(f"查询阿里云文档解析状态失败：{exc}") from exc
            status_data = _to_plain_data(response.body.data)
            status = str(_lookup_any(status_data, "Status", "status") or "").lower()
            if status == "success":
                return status_data
            if status in {"fail", "failed"}:
                message = _lookup_any(status_data, "Message", "message") or "文档解析失败"
                raise RuntimeError(str(message))
            time.sleep(self.poll_interval_seconds)

        raise TimeoutError(f"文档解析等待超时，超过 {self.max_wait_seconds} 秒仍未完成。")

    def _collect_markdown(self, job_id: str) -> tuple[str, list[dict[str, Any]]]:
        layout_num = 0
        all_layouts: list[dict[str, Any]] = []
        while True:
            request = docmind_models.GetDocParserResultRequest(
                id=job_id,
                layout_num=layout_num,
                layout_step_size=self.layout_step_size,
            )
            try:
                response = self.client.get_doc_parser_result(request)
            except Exception as exc:
                raise RuntimeError(f"获取阿里云文档解析结果失败：{exc}") from exc

            result_data = _to_plain_data(response.body.data)
            layouts = _extract_layouts(result_data)
            if not layouts:
                break
            all_layouts.extend(layouts)
            if len(layouts) < self.layout_step_size:
                break
            layout_num += len(layouts)

        markdown_text = self._generate_markdown(all_layouts)
        if not markdown_text.strip():
            download_url = _extract_markdown_output_url(result_data if 'result_data' in locals() else {})
            if download_url:
                markdown_text = self._download_output_file(download_url)
        if not markdown_text.strip():
            raise RuntimeError("文档解析完成，但未获取到 Markdown 内容")
        return markdown_text, all_layouts

    def _download_output_file(self, file_url: str) -> str:
        request = Request(file_url, method="GET")
        try:
            with _DIRECT_HTTP_OPENER.open(request, timeout=30) as response:
                return response.read().decode("utf-8", errors="ignore")
        except HTTPError as exc:
            raise RuntimeError(f"下载文档解析 Markdown 结果失败（HTTP {exc.code}）") from exc
        except URLError as exc:
            raise RuntimeError(f"下载文档解析 Markdown 结果失败：{exc.reason}") from exc

    def _generate_markdown(self, layouts: list[dict[str, Any]]) -> str:
        parts: list[str] = []
        for layout in layouts:
            layout_type = str(layout.get("type") or "").lower()
            if layout_type == "table":
                table_html = _table_to_html(layout)
                if table_html:
                    parts.append(table_html)
                    continue

            llm_result = str(layout.get("llmResult") or "").strip()
            markdown_content = str(layout.get("markdownContent") or "").strip()
            text_content = str(layout.get("text") or "").strip()
            block = markdown_content or llm_result or text_content
            if block:
                parts.append(block)
        return "\n\n".join(part for part in parts if part).strip()

    def _build_cache_key(
        self,
        *,
        file_bytes: bytes,
        file_name: str,
        llm_enhancement: bool,
        formula_enhancement: bool,
    ) -> str:
        payload = {
            "file_name": file_name,
            "content_sha256": hashlib.sha256(file_bytes).hexdigest(),
            "llm_enhancement": llm_enhancement,
            "formula_enhancement": formula_enhancement,
        }
        serialized = json.dumps(payload, ensure_ascii=False, sort_keys=True)
        return hashlib.sha256(serialized.encode("utf-8")).hexdigest()

    def _load_cached_result(self, cache_key: str) -> dict[str, Any] | None:
        cache_path = self.cache_dir / f"{cache_key}.json"
        if not cache_path.exists():
            return None
        try:
            return json.loads(cache_path.read_text(encoding="utf-8"))
        except Exception:
            return None

    def _save_cached_result(self, cache_key: str, result: dict[str, Any]) -> None:
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        cache_path = self.cache_dir / f"{cache_key}.json"
        cache_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")

    def _validate_size(self, *, file_bytes: bytes, suffix: str) -> None:
        size_mb = len(file_bytes) / (1024 * 1024)
        if suffix in _IMAGE_EXTENSIONS and size_mb > 20:
            raise ValueError("图片大小超过 20 MB，无法调用文档解析服务。")
        if suffix not in _IMAGE_EXTENSIONS and size_mb > 150:
            raise ValueError("文档大小超过 150 MB，无法调用文档解析服务。")


def _to_plain_data(value: Any) -> dict[str, Any]:
    if value is None:
        return {}
    if isinstance(value, dict):
        return value
    if hasattr(value, "to_map"):
        return value.to_map()
    if hasattr(value, "__dict__"):
        return dict(value.__dict__)
    return {}


def _lookup_any(data: dict[str, Any], *keys: str) -> Any:
    for key in keys:
        if key in data:
            return data[key]
    return None


def _extract_layouts(result_data: dict[str, Any]) -> list[dict[str, Any]]:
    layouts = _lookup_any(result_data, "layouts", "Layouts")
    if isinstance(layouts, list):
        return [item for item in layouts if isinstance(item, dict)]
    return []


def _extract_markdown_output_url(result_data: dict[str, Any]) -> str | None:
    output_results = _lookup_any(result_data, "outputFormatResult", "OutputFormatResult")
    if not isinstance(output_results, list):
        return None
    for item in output_results:
        if not isinstance(item, dict):
            continue
        output_type = str(_lookup_any(item, "outputType", "OutputType") or "").lower()
        if output_type == "markdown":
            output_url = _lookup_any(item, "outputFileUrl", "OutputFileUrl")
            if output_url:
                return str(output_url)
    return None


def _table_to_html(table_layout: dict[str, Any]) -> str:
    cells = table_layout.get("cells")
    if not isinstance(cells, list) or not cells:
        llm_result = str(table_layout.get("llmResult") or "").strip()
        return llm_result

    rows: dict[int, list[dict[str, Any]]] = {}
    for cell in cells:
        if not isinstance(cell, dict):
            continue
        row_start = int(cell.get("ysc", 0))
        rows.setdefault(row_start, []).append(cell)

    processed: set[tuple[int, int]] = set()
    html_parts = ['<table border="1" cellspacing="0" cellpadding="4">']
    for row_key in sorted(rows.keys()):
        html_parts.append("<tr>")
        row_cells = sorted(rows[row_key], key=lambda item: int(item.get("xsc", 0)))
        for cell in row_cells:
            cell_key = (int(cell.get("ysc", 0)), int(cell.get("xsc", 0)))
            if cell_key in processed:
                continue

            rowspan = int(cell.get("yec", 0)) - int(cell.get("ysc", 0)) + 1
            colspan = int(cell.get("xec", 0)) - int(cell.get("xsc", 0)) + 1
            for row_offset in range(max(rowspan, 1)):
                for column_offset in range(max(colspan, 1)):
                    processed.add((cell_key[0] + row_offset, cell_key[1] + column_offset))

            text_parts: list[str] = []
            for layout in cell.get("layouts", []):
                if isinstance(layout, dict) and layout.get("text"):
                    text_parts.append(str(layout["text"]).strip())
            cell_text = " ".join(part for part in text_parts if part)
            attrs: list[str] = []
            if rowspan > 1:
                attrs.append(f'rowspan="{rowspan}"')
            if colspan > 1:
                attrs.append(f'colspan="{colspan}"')
            html_parts.append(f"<td {' '.join(attrs)}>{cell_text}</td>")
        html_parts.append("</tr>")
    html_parts.append("</table>")
    return "".join(html_parts)


def _markdown_to_text(markdown_text: str) -> str:
    lines = [line.strip() for line in markdown_text.splitlines() if line.strip()]
    return "\n".join(lines)


__all__ = ["DocumentParserAgent"]