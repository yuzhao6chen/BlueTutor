from __future__ import annotations

import json
from typing import Any


def _json(data: Any) -> str:
	return json.dumps(data, ensure_ascii=False, indent=2)


def build_problem_analysis_prompt(report_payload: dict[str, Any], recommendation_type: str) -> str:
	return f"""
你是 BlueTutor 错题推荐系统中的 problem-analyzer。
你的任务：分析一条已有错题，提炼出适合生成相似题/变式题的结构化信息。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- problem_type: string
- knowledge_tags: string[]
- method_pattern: string
- error_focus: string
- variant_axes: string[]
- recommendation_goal: string

推荐类型：{recommendation_type}
原始错题报告：
{_json(report_payload)}
""".strip()


def build_variant_generation_prompt(analysis: dict[str, Any], report_payload: dict[str, Any], recommendation_type: str) -> str:
	return f"""
你是 BlueTutor 错题推荐系统中的 variant-generator。
你的任务：基于题目分析结果，为小学生生成一道适合手机端展示的单选题。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

要求：
1. 题目必须可解，且只有一个正确选项。
2. 选项数量固定为 4 个。
3. 题目文本要简洁易读。
4. 干扰项要贴近学生真实错误。
5. explanation 不要过长，控制在 80 字以内。
6. difficulty 只能是 easy / medium / hard。

输出字段：
- title: string
- difficulty: string
- question: string
- options: array<{{id: string, text: string}}>
- correct_option_id: string
- answer: string
- explanation: string
- knowledge_tags: string[]
- why_recommended: string

推荐类型：{recommendation_type}
分析结果：
{_json(analysis)}

原始错题报告：
{_json(report_payload)}
""".strip()


def build_quality_review_prompt(candidate: dict[str, Any], analysis: dict[str, Any]) -> str:
	return f"""
你是 BlueTutor 错题推荐系统中的 quality-reviewer。
你的任务：检查候选推荐题是否满足基本质量要求。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- passed: boolean
- issues: string[]
- normalized_result: object

检查要求：
1. 题目必须有且仅有 4 个选项。
2. correct_option_id 必须出现在 options 中。
3. question、answer、why_recommended 不能为空。
4. 如果存在可以安全修正的小问题，直接在 normalized_result 中修正。

题目分析：
{_json(analysis)}

候选推荐题：
{_json(candidate)}
""".strip()


def build_redo_plan_prompt(report_payload: dict[str, Any], stage: str, history: list[dict[str, Any]]) -> str:
	return f"""
你是 BlueTutor 错题重做系统中的 pedagogy-planner。
你的任务：根据错题内容、当前阶段和历史作答，决定下一轮该如何引导学生。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- stage: string
- interaction_mode: string
- teaching_goal: string
- prompt: string

可选 stage：
- understand_problem
- identify_first_step
- solve
- final_check
- completed

可选 interaction_mode：
- single_choice
- free_text

原始错题报告：
{_json(report_payload)}
当前阶段：{stage}
历史记录：
{_json(history)}
""".strip()


def build_redo_option_prompt(report_payload: dict[str, Any], planning: dict[str, Any]) -> str:
	return f"""
你是 BlueTutor 错题重做系统中的 option-generator。
你的任务：为当前引导问题生成 4 个单选项。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- options: array<{{id: string, text: string}}>
- correct_option_id: string

要求：
1. 选项固定 4 个
2. 只有 1 个最佳答案
3. 干扰项要贴近学生常见误区

原始错题报告：
{_json(report_payload)}
当前规划：
{_json(planning)}
""".strip()


def build_redo_hint_prompt(
	report_payload: dict[str, Any],
	planning: dict[str, Any],
	history: list[dict[str, Any]],
	hint_level: int = 1,
) -> str:
	level_desc = {1: "轻微引导（只给方向，不涉及具体数字或公式）", 2: "中度提示（可以提到关键公式或中间步骤，但不给最终答案）", 3: "直接提示（给出接近完整答案的提示，让学生只需做最后一步）"}
	desc = level_desc.get(hint_level, level_desc[1])
	return f"""
你是 BlueTutor 错题重做系统中的 hint-generator。
你的任务：为当前步骤生成一个简短提示。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- hint: string

要求：
1. 提示要短，适合手机端
2. 当前提示等级：{hint_level}（共3级）
3. 提示策略：{desc}
4. 不要直接把完整答案全说出来

原始错题报告：
{_json(report_payload)}
当前规划：
{_json(planning)}
历史记录：
{_json(history)}
""".strip()


def build_redo_evaluation_prompt(
	report_payload: dict[str, Any],
	planning: dict[str, Any],
	answer: str,
	consecutive_correct: int = 0,
	required_consecutive_correct: int = 1,
) -> str:
	return f"""
你是 BlueTutor 错题重做系统中的 response-evaluator。
你的任务：评估学生当前回答是否正确、部分正确或错误。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- result: string（correct / partial / incorrect）
- feedback: string（给学生的简短反馈，80字以内）
- next_stage: string（understand_problem / identify_first_step / solve / final_check / completed）
- should_offer_hint: boolean（答错时是否应给提示）
- can_clear_mistake: boolean（是否可以标记错题已巩固）
- confidence: number（0-1，你对这个判断的置信度）

评估标准：
1. 对于单选题：选项ID匹配即为 correct，否则为 incorrect。
2. 对于自由文本回答：
   - 如果学生回答与参考答案在数值上一致（允许单位差异），判为 correct。
   - 如果学生回答包含关键步骤但未给出最终答案，判为 partial。
   - 如果学生回答明显偏离正确方向，判为 incorrect。
3. 完成判定：当前连续答对 {consecutive_correct} 次，需要连续答对 {required_consecutive_correct} 次才能完成。
   - 只有当 result=correct 且连续答对次数已达到要求时，can_clear_mistake 才为 true。
   - 否则 can_clear_mistake 为 false。

原始错题报告：
{_json(report_payload)}
当前规划：
{_json(planning)}
学生回答：{answer}
当前连续答对次数：{consecutive_correct}
完成所需连续答对次数：{required_consecutive_correct}
""".strip()


def build_profile_aware_analysis_prompt(
	report_payload: dict[str, Any],
	recommendation_type: str,
	user_profile: dict[str, Any] | None = None,
) -> str:
	profile_section = ""
	if user_profile:
		profile_section = f"""
用户画像：
{_json(user_profile)}

请结合用户画像中的薄弱知识点和错误模式，使分析结果更具针对性。例如：
- 如果用户在某个知识点反复犯错，variant_axes 应包含针对该知识点的变式方向
- error_focus 应优先对应用户画像中最突出的薄弱环节
- recommendation_goal 应体现对用户个性化弱点的针对性训练
"""
	return f"""
你是 BlueTutor 错题推荐系统中的 problem-analyzer。
你的任务：分析一条已有错题，结合用户画像，提炼出适合生成相似题/变式题的结构化信息。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- problem_type: string
- knowledge_tags: string[]
- method_pattern: string
- error_focus: string
- variant_axes: string[]
- recommendation_goal: string

推荐类型：{recommendation_type}
原始错题报告：
{_json(report_payload)}
{profile_section}
""".strip()


def build_profile_aware_variant_prompt(
	analysis: dict[str, Any],
	report_payload: dict[str, Any],
	recommendation_type: str,
	user_profile: dict[str, Any] | None = None,
) -> str:
	profile_section = ""
	if user_profile:
		profile_section = f"""
用户画像：
{_json(user_profile)}

请结合用户画像生成题目：
- 难度应参考用户画像中的薄弱程度，频繁出错的知识点先出 easy 难度
- 干扰项应参考用户画像中的常见错误类型，贴近用户真实易错点
- why_recommended 应说明这道题如何针对该用户的个性化薄弱环节
"""
	return f"""
你是 BlueTutor 错题推荐系统中的 variant-generator。
你的任务：基于题目分析结果和用户画像，为小学生生成一道适合手机端展示的单选题。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

要求：
1. 题目必须可解，且只有一个正确选项。
2. 选项数量固定为 4 个。
3. 题目文本要简洁易读。
4. 干扰项要贴近学生真实错误。
5. explanation 不要过长，控制在 80 字以内。
6. difficulty 只能是 easy / medium / hard。

输出字段：
- title: string
- difficulty: string
- question: string
- options: array<{{id: string, text: string}}>
- correct_option_id: string
- answer: string
- explanation: string
- knowledge_tags: string[]
- why_recommended: string

推荐类型：{recommendation_type}
分析结果：
{_json(analysis)}

原始错题报告：
{_json(report_payload)}
{profile_section}
""".strip()


def build_dialogue_listener_prompt(
	report_payload: dict[str, Any],
	similar_question: dict[str, Any] | None = None,
	dialogue_history: list[dict[str, Any]] | None = None,
) -> str:
	similar_section = ""
	if similar_question:
		similar_section = f"""
关联相似题（可用于提问参考）：
{_json(similar_question)}
"""
	history_section = ""
	if dialogue_history:
		history_section = f"""
对话历史：
{_json(dialogue_history)}
"""
	return f"""
你是 BlueTutor 错题讲题验证系统中的 listener。
你的任务：扮演一个认真听讲的同伴，让学生自己讲解这道错题的解题思路，通过提问和追问来检验学生是否真正掌握。

核心原则：
1. 你是"听者"，不是"讲者"——不要主动讲解，而是让学生说。
2. 根据学生的讲解内容，提出针对性的疑问或追问。
3. 可以参考原题讲题报告中的思维链和错因分析，判断学生是否遗漏了关键步骤或仍存在错误认知。
4. 可以参考关联相似题，引导学生将理解迁移到相似情境中。
5. 如果学生的讲解清晰完整、逻辑正确，应给予肯定。
6. 如果学生存在明显错误或遗漏，用提问的方式引导其自我发现，而非直接纠正。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- reply: string（你的回复，80字以内，适合手机端展示）
- is_probing: boolean（当前回复是否为追问/质疑）
- topic_focus: string（当前关注的知识点或解题步骤）
- should_continue: boolean（是否应继续对话）
- mastery_signal: number（0-1，学生当前展现的掌握程度信号，0=完全不懂，1=完全掌握）

原始错题报告（含讲题报告、思维链、错因分析）：
{_json(report_payload)}
{similar_section}
{history_section}
""".strip()


def build_dialogue_mastery_prompt(
	report_payload: dict[str, Any],
	dialogue_history: list[dict[str, Any]],
	similar_question: dict[str, Any] | None = None,
) -> str:
	similar_section = ""
	if similar_question:
		similar_section = f"""
关联相似题：
{_json(similar_question)}
"""
	return f"""
你是 BlueTutor 错题讲题验证系统中的 mastery-judge。
你的任务：根据学生的完整讲解对话记录，判定学生是否真正掌握了这道错题涉及的知识点和解题方法。

判定标准：
1. mastered：学生能清晰讲解完整的解题思路，逻辑正确，关键步骤无遗漏，对相似题也能正确分析。
2. not_mastered：学生讲解存在明显错误、关键步骤遗漏、或对核心概念理解有误。
3. in_progress：对话尚未充分展开，暂时无法做出明确判定。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- verdict: string（mastered / not_mastered / in_progress）
- detail: string（判定详情，说明判定理由，100字以内）
- weak_aspects: string[]（如果未掌握，指出具体的薄弱方面）
- confidence: number（0-1，判定置信度）

原始错题报告：
{_json(report_payload)}
{similar_section}

完整对话记录：
{_json(dialogue_history)}
""".strip()


def build_profile_update_prompt(
	existing_profile: dict[str, Any],
	new_report_payload: dict[str, Any],
) -> str:
	return f"""
你是 BlueTutor 用户画像更新系统中的 profile-updater。
你的任务：根据新增的错题报告，判断是否需要更新用户画像，并输出更新后的画像。

规则：
1. 如果新报告中的错误类型或薄弱知识点在现有画像中已存在，更新 frequency 和 latest_report_ids。
2. 如果新报告中包含画像中未记录的错误类型或薄弱知识点，将其添加到画像中。
3. 如果新报告显示用户已掌握某个知识点（independence_evaluation.level 较高且无错误），不修改该知识点的薄弱标记。
4. 重新计算 weak_knowledge_tags，按 frequency 降序排列，最多保留 10 个。
5. 更新 error_type_distribution，合并新旧数据。
6. 更新 independence_summary，综合所有报告的独立完成度。
7. 生成 learning_style_hints，基于错误模式给出最多 5 条个性化学习建议。

必须输出合法 JSON 对象，不能输出 markdown，不能输出解释文字。

输出字段：
- updated: boolean（是否有实际更新）
- profile: object（完整的更新后用户画像，结构同输入 existing_profile）

现有用户画像：
{_json(existing_profile)}

新增错题报告：
{_json(new_report_payload)}
""".strip()
