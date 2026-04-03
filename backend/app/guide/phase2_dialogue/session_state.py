from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional
from enum import Enum


class NodeStatus(str, Enum):
    CORRECT = "correct"  # 该步骤正确
    INCORRECT = "incorrect"  # 该步骤有误
    STUCK = "stuck"  # 当前卡点
    ABANDONED = "abandoned"  # 该思路已放弃


@dataclass
class ThinkingNode:
    """思维树的单个节点"""
    node_id: str  # 节点唯一标识，如 "approach_1_step_2"
    content: str  # 节点描述，如 "假设全是鸡，共60只脚"
    status: NodeStatus  # 节点状态
    parent_id: Optional[str] = None  # 父节点ID，根节点为None
    error_type: Optional[str] = None  # 仅在status为INCORRECT或STUCK时有值
    children: list[str] = field(default_factory=list)  # 子节点ID列表


@dataclass
class SessionState:
    """完整的会话状态对象"""
    # 第一阶段输出（固定不变）
    parsed_problem: dict

    # 思维树：以节点ID为键的字典，便于O(1)查找
    thinking_tree: dict[str, ThinkingNode] = field(default_factory=dict)

    # 完整对话历史：[{"role": "student"/"tutor", "content": "..."}]
    dialogue_history: list[dict] = field(default_factory=list)

    # 当前卡点节点ID
    current_stuck_node_id: Optional[str] = None

    # 当前卡点被引导的次数
    stuck_count: int = 0

    # TODO: 预留扩展接口 - 后续可从用户画像模块注入学生历史错误画像
    # student_profile: Optional[dict] = None

    def add_node(self, node: ThinkingNode) -> None:
        """将新节点加入思维树，并更新父节点的children列表"""
        self.thinking_tree[node.node_id] = node
        if node.parent_id and node.parent_id in self.thinking_tree:
            self.thinking_tree[node.parent_id].children.append(node.node_id)

    def update_node(self, node_id: str, status: NodeStatus,
                    error_type: Optional[str] = None) -> None:
        """更新已有节点的状态"""
        if node_id in self.thinking_tree:
            self.thinking_tree[node_id].status = status
            self.thinking_tree[node_id].error_type = error_type
