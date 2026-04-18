"""
phase3_solution/visualization.py

根据 SessionState 生成题解可视化 SVG 片段。

核心函数：generate_visualization(session_state) -> dict
  - 调用 LLM 生成 SVG 内容
  - 对生成结果做 XML 合法性校验
  - 校验失败时重试一次，仍失败则返回降级结果
  - 将 <svg> 包裹层统一由本模块添加，LLM 只负责内部元素
"""

import json
import logging
import xml.etree.ElementTree as ET

from langchain_core.output_parsers import JsonOutputParser

from ..llm_provider import get_llm
from ..phase2_dialogue.session_state import SessionState
from .prompts import VISUALIZATION_PROMPT

logger = logging.getLogger(__name__)

# SVG 画布包裹模板，统一由后端添加
_SVG_WRAPPER = (
    '<svg viewBox="0 0 600 300" '
    'style="width:100%;height:auto;display:block;" '
    'xmlns="http://www.w3.org/2000/svg">'
    '{content}'
    '</svg>'
)

# 降级占位 SVG（当生成失败时返回）
_FALLBACK_SVG = _SVG_WRAPPER.format(
    content=(
        '<rect x="20" y="20" width="560" height="260" '
        'fill="#F0F0F0" stroke="#CCCCCC" stroke-width="1"/>'
        '<text x="300" y="150" font-size="16" fill="#999999" '
        'text-anchor="middle" dominant-baseline="middle">'
        '可视化生成失败，请查看文字题解</text>'
    )
)


def _serialize_solution_path(solution_path: list) -> str:
    """将解题路径节点列表序列化为可读字符串"""
    if not solution_path:
        return "（解题路径为空）"
    lines = []
    for i, node in enumerate(solution_path):
        prefix = "起点" if node.node_id == "n0" else f"第{i}步"
        error_info = (
            f"（曾出现错误：{'、'.join(node.error_history)}）"
            if node.error_history else ""
        )
        lines.append(
            f"{prefix}：[{node.node_id}] {node.content} "
            f"({node.status.value}){error_info}"
        )
    return "\n".join(lines)


def _validate_and_wrap_svg(svg_content: str) -> str:
    """
    校验 SVG 内部元素的 XML 合法性，并包裹 <svg> 标签。

    Args:
        svg_content: LLM 生成的 SVG 内部元素字符串（不含 <svg> 标签）

    Returns:
        完整的 SVG 字符串（含 <svg> 标签）

    Raises:
        ValueError: XML 解析失败时抛出
    """
    # 包裹成完整 XML 进行校验
    full_svg = _SVG_WRAPPER.format(content=svg_content)
    # 若解析不抛异常，则 XML 合法
    ET.fromstring(full_svg)
    return full_svg


def _call_llm(inputs: dict) -> list:
    """
    调用 LLM 生成可视化数据，返回解析后的 JSON 列表。

    Args:
        inputs: 传入 VISUALIZATION_PROMPT 的变量字典

    Returns:
        list: 包含 step_id、title、svg_content 的字典列表

    Raises:
        Exception: LLM 调用失败或 JSON 解析失败时抛出
    """
    llm = get_llm()
    parser = JsonOutputParser()
    chain = VISUALIZATION_PROMPT | llm | parser
    result = chain.invoke(inputs)

    # 兼容 LLM 直接返回列表或包裹在 dict 中的情况
    if isinstance(result, dict):
        result = result.get("visuals", result.get("steps", [result]))
    if not isinstance(result, list):
        raise ValueError(f"LLM 返回格式不符合预期：{type(result)}")

    return result


def generate_visualization(session_state: SessionState) -> dict:
    """
    根据 SessionState 生成题解可视化数据。

    流程：
    1. 从 session_state 提取题目信息、解题路径、题解文本
    2. 调用 LLM 生成 SVG 片段列表
    3. 对每个片段做 XML 合法性校验
    4. 校验失败时整体重试一次
    5. 仍失败则返回降级结果（包含占位 SVG）

    Args:
        session_state: 对话结束后的完整会话状态，is_solved 应为 True，
                       solution 应已生成

    Returns:
        dict，格式如下：
        {
            "problem_type": "chicken_rabbit" | "distance" | "unknown",
            "visuals": [
                {
                    "step_id": "step_overview",
                    "title": "题意理解",
                    "svg": "<svg>...</svg>"   # 完整 SVG，含 <svg> 标签
                },
                ...
            ]
        }
    """
    solution_path = session_state.get_solution_path()
    inputs = {
        "parsed_problem": json.dumps(
            session_state.parsed_problem, ensure_ascii=False
        ),
        "solution_path": _serialize_solution_path(solution_path),
        "solution_text": session_state.solution or "（题解尚未生成）",
    }

    # 最多尝试两次（首次 + 一次重试）
    last_error = None
    for attempt in range(2):
        try:
            raw_visuals = _call_llm(inputs)
            validated_visuals = []

            for item in raw_visuals:
                step_id = item.get("step_id", f"step_{len(validated_visuals)}")
                title = item.get("title", "")
                svg_content = item.get("svg_content", "")

                try:
                    full_svg = _validate_and_wrap_svg(svg_content)
                except (ET.ParseError, ValueError) as e:
                    logger.warning(
                        "SVG 校验失败（step_id=%s，attempt=%d）：%s",
                        step_id, attempt + 1, e
                    )
                    raise ValueError(
                        f"step_id={step_id} 的 SVG 内容 XML 不合法：{e}"
                    ) from e

                validated_visuals.append({
                    "step_id": step_id,
                    "title": title,
                    "svg": full_svg,
                })

            # 识别题型（基于解题路径内容简单判断）
            problem_type = _detect_problem_type(session_state)

            logger.info(
                "可视化生成成功（attempt=%d，steps=%d，type=%s）",
                attempt + 1, len(validated_visuals), problem_type
            )
            return {
                "problem_type": problem_type,
                "visuals": validated_visuals,
            }

        except Exception as e:
            last_error = e
            logger.warning(
                "可视化生成失败（attempt=%d）：%s，%s",
                attempt + 1, type(e).__name__, e
            )

    # 两次均失败，返回降级结果
    logger.error("可视化生成最终失败，返回降级占位图。最后错误：%s", last_error)
    return {
        "problem_type": "unknown",
        "visuals": [
            {
                "step_id": "step_overview",
                "title": "可视化",
                "svg": _FALLBACK_SVG,
            }
        ],
    }


def _detect_problem_type(session_state: SessionState) -> str:
    """
    根据题目信息和解题路径内容简单识别题型。

    Returns:
        "chicken_rabbit" | "distance" | "unknown"
    """
    # 从 parsed_problem 和 raw_problem 中提取文本进行关键词匹配
    text_sources = [
        session_state.raw_problem,
        json.dumps(session_state.parsed_problem, ensure_ascii=False),
    ]
    combined_text = " ".join(text_sources).lower()

    chicken_rabbit_keywords = ["鸡", "兔", "头", "脚", "同笼", "腿"]
    distance_keywords = ["速度", "路程", "时间", "千米", "公里", "小时", "分钟",
                         "相遇", "追及", "出发", "行驶", "行走", "km", "km/h"]

    chicken_score = sum(1 for kw in chicken_rabbit_keywords if kw in combined_text)
    distance_score = sum(1 for kw in distance_keywords if kw in combined_text)

    if chicken_score >= 2:
        return "chicken_rabbit"
    if distance_score >= 2:
        return "distance"
    return "unknown"