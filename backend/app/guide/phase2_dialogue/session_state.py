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
    node_id: str                              # 节点唯一标识
    content: str                              # 节点描述
    status: NodeStatus                        # 节点当前状态
    parent_id: Optional[str] = None          # 父节点ID，根节点为None
    error_history: list[str] = field(default_factory=list)  # 错误历史列表，按时间顺序追加
    children: list[str] = field(default_factory=list)       # 子节点ID列表


@dataclass
class SessionState:
    """完整的会话状态对象"""
    # 第一阶段输出（固定不变）
    parsed_problem: dict
    raw_problem: str = ""   # 原始题目文本，用于讲题报告

    # 思维树：以节点ID为键的字典，便于O(1)查找
    thinking_tree: dict[str, ThinkingNode] = field(default_factory=dict)

    # 完整对话历史：[{"role": "student"/"tutor", "content": "..."}]
    dialogue_history: list[dict] = field(default_factory=list)

    # 当前卡点节点ID
    current_stuck_node_id: Optional[str] = None

    # 当前卡点被引导的次数
    stuck_count: int = 0

    # 记录最新被操作的节点ID
    last_updated_node_id: Optional[str] = None

    # TODO: 预留扩展接口 - 后续可从用户画像模块注入学生历史错误画像
    # student_profile: Optional[dict] = None

    def __post_init__(self):
        """初始化时自动创建总根节点 n0"""
        if not self.thinking_tree:
            root = ThinkingNode(
                node_id="n0",
                content="解题起点",
                status=NodeStatus.STUCK,
                parent_id=None
            )
            self.thinking_tree["n0"] = root
            self.current_stuck_node_id = "n0"
            self.stuck_count = 0

    def next_node_id(self) -> str:
        """生成下一个可用的全局自增节点 ID，格式为 n1、n2、n3……"""
        nums = [
            int(node_id[1:]) for node_id in self.thinking_tree
            if node_id.startswith("n") and node_id[1:].isdigit()
        ]
        return f"n{max(nums) + 1}" if nums else "n1"

    def add_node(self, node: ThinkingNode) -> None:
        """将新节点加入思维树，并更新父节点的 children 列表。
        若父节点处于 stuck 状态，自动将其标记为 correct（卡点已被突破）。
        """
        self.thinking_tree[node.node_id] = node
        if node.parent_id and node.parent_id in self.thinking_tree:
            parent = self.thinking_tree[node.parent_id]
            parent.children.append(node.node_id)
            # 若父节点是卡点，说明学生已推进，自动解除 stuck
            if parent.status == NodeStatus.STUCK:
                parent.status = NodeStatus.CORRECT
                if self.current_stuck_node_id == node.parent_id:
                    self.current_stuck_node_id = None
                    self.stuck_count = 0

    def update_node_status(self, node_id: str, status: NodeStatus) -> None:
        """更新已有节点的状态"""
        if node_id in self.thinking_tree:
            self.thinking_tree[node_id].status = status

    def append_error(self, node_id: str, error_type: str) -> None:
        """向节点的错误历史中追加一条错误记录"""
        if node_id in self.thinking_tree:
            self.thinking_tree[node_id].error_history.append(error_type)
