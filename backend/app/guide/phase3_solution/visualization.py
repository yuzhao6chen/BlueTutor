"""
phase3_solution/visualization.py

根据 SessionState 生成题解可视化 SVG 片段。

核心函数：generate_visualization(session_state) -> dict
  流程：
  1. 调用图意规划 Agent（qwen3-max）一次性生成所有步骤的图意方案
  2. 对每个步骤分别调用 SVG 生成 Agent（qwen3.6-flash）生成 SVG 代码
  3. 对每个 SVG 片段做 XML 合法性校验，失败时单步重试一次
  4. 仍失败则对该步骤返回降级占位图，不影响其他步骤
"""

import json
import logging
import xml.etree.ElementTree as ET

from langchain_core.output_parsers import JsonOutputParser, StrOutputParser

from ..llm_provider import get_llm
from ..phase2_dialogue.session_state import SessionState
from .prompts import VISUAL_PLANNER_PROMPT, SVG_GENERATOR_PROMPT

logger = logging.getLogger(__name__)

# SVG 画布包裹模板，统一由后端添加
_SVG_WRAPPER = (
    '<svg viewBox="0 0 600 300" '
    'style="width:100%;height:auto;display:block;" '
    'xmlns="http://www.w3.org/2000/svg">'
    '{content}'
    '</svg>'
 )

# 降级占位 SVG（当单步生成失败时返回）
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

    Raises:
        ValueError: XML 解析失败时抛出
    """
    full_svg = _SVG_WRAPPER.format(content=svg_content)
    ET.fromstring(full_svg)
    return full_svg


def _call_planner(inputs: dict) -> list:
    """
    调用图意规划 Agent（qwen3-max），返回所有步骤的图意规划列表。

    Returns:
        list: 包含 step_id、title、visual_type、description、layout_hint 的字典列表
    """
    llm = get_llm(model="qwen3-max")
    chain = VISUAL_PLANNER_PROMPT | llm | JsonOutputParser()
    result = chain.invoke(inputs)

    if isinstance(result, dict):
        result = result.get("steps", result.get("visuals", [result]))
    if not isinstance(result, list):
        raise ValueError(f"图意规划返回格式不符合预期：{type(result)}")

    return result


def _call_svg_generator(step_plan: dict) -> str:
    """
    调用 SVG 生成 Agent（qwen3.6-flash），为单个步骤生成 SVG 内部元素代码。

    Args:
        step_plan: 单个步骤的图意规划字典，包含 visual_type、description、layout_hint

    Returns:
        str: SVG 内部元素代码（不含 <svg> 标签）
    """
    llm = get_llm(model="qwen3.6-flash")
    chain = SVG_GENERATOR_PROMPT | llm | StrOutputParser()
    result = chain.invoke({
        "visual_type": step_plan.get("visual_type", "flow_diagram"),
        "description": step_plan.get("description", ""),
        "layout_hint": step_plan.get("layout_hint", ""),
    })
    return result.strip()


def _generate_single_svg(step_plan: dict) -> dict:
    """
    为单个步骤生成并校验 SVG，失败时重试一次，仍失败则返回降级占位图。

    Returns:
        dict: 包含 step_id、title、svg 的字典
    """
    step_id = step_plan.get("step_id", "step_unknown")
    title = step_plan.get("title", "")

    for attempt in range(2):
        try:
            svg_content = _call_svg_generator(step_plan)
            full_svg = _validate_and_wrap_svg(svg_content)
            logger.info(
                "SVG 生成成功（step_id=%s，attempt=%d）", step_id, attempt + 1
            )
            return {"step_id": step_id, "title": title, "svg": full_svg}
        except Exception as e:
            logger.warning(
                "SVG 生成失败（step_id=%s，attempt=%d）：%s",
                step_id, attempt + 1, e
            )

    logger.error("SVG 生成最终失败，返回降级占位图（step_id=%s）", step_id)
    return {"step_id": step_id, "title": title, "svg": _FALLBACK_SVG}


def generate_visualization(session_state: SessionState) -> dict:
    """
    根据 SessionState 生成题解可视化数据。

    流程：
    1. 调用图意规划 Agent 一次性生成所有步骤的图意方案
    2. 对每个步骤分别调用 SVG 生成 Agent 生成 SVG 代码
    3. 单步失败时降级处理，不影响其他步骤

    Returns:
        dict，格式如下：
        {
            "problem_type": "chicken_rabbit" | "distance" | "unknown",
            "visuals": [
                {
                    "step_id": "step_overview",
                    "title": "题意理解",
                    "svg": "<svg>...</svg>"
                },
                ...
            ]
        }
    """
    solution_path = session_state.get_solution_path()
    planner_inputs = {
        "parsed_problem": json.dumps(
            session_state.parsed_problem, ensure_ascii=False
        ),
        "solution_path": _serialize_solution_path(solution_path),
        "solution_text": session_state.solution or "（题解尚未生成）",
    }

    # 第一阶段：图意规划（一次调用）
    try:
        step_plans = _call_planner(planner_inputs)
        logger.info("图意规划完成，共 %d 个步骤", len(step_plans))
    except Exception as e:
        logger.error("图意规划失败，返回降级结果：%s", e)
        return {
            "problem_type": "unknown",
            "visuals": [
                {"step_id": "step_overview", "title": "可视化", "svg": _FALLBACK_SVG}
            ],
        }

    # 第二阶段：逐步生成 SVG（每步独立调用）
    visuals = [_generate_single_svg(plan) for plan in step_plans]

    problem_type = _detect_problem_type(session_state)
    logger.info(
        "可视化生成完成（steps=%d，type=%s）", len(visuals), problem_type
    )
    return {
        "problem_type": problem_type,
        "visuals": visuals,
    }


def _detect_problem_type(session_state: SessionState) -> str:
    """
    根据题目信息和解题路径内容简单识别题型。

    Returns:
        "chicken_rabbit" | "distance" | "unknown"
    """
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
