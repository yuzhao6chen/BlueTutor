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
    node_id: str  # 节点唯一标识
    content: str  # 节点描述
    status: NodeStatus  # 节点当前状态
    parent_id: Optional[str] = None  # 父节点ID，根节点为None
    error_history: list[str] = field(default_factory=list)  # 错误历史列表，按时间顺序追加
    children: list[str] = field(default_factory=list)  # 子节点ID列表


@dataclass
class SessionState:
    """完整的会话状态对象"""
    # 第一阶段输出（固定不变）
    parsed_problem: dict
    raw_problem: str = ""  # 原始题目文本，用于讲题报告

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

    # 标记题目是否已被学生解决
    is_solved: bool = False

    # 已生成的题解文本，None 表示尚未生成
    solution: Optional[str] = None

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

    def get_solution_path(self) -> list:
        """
        从思维树中提取学生最终解决问题时所使用的路径。

        逻辑：以 last_updated_node_id 为终点，沿 parent_id 向上回溯至 n0，
        返回这条路径上的节点列表（从根到叶的顺序）。

        Returns:
            ThinkingNode 列表，从 n0 到解题终点节点，按顺序排列。
            若 last_updated_node_id 为 None 或节点不存在，返回空列表。
        """
        if not self.last_updated_node_id:
            return []

        path = []
        current_id = self.last_updated_node_id

        # 沿 parent_id 向上回溯，防止循环引用设置最大步数
        visited = set()
        while current_id is not None:
            if current_id in visited or current_id not in self.thinking_tree:
                break
            visited.add(current_id)
            path.append(self.thinking_tree[current_id])
            current_id = self.thinking_tree[current_id].parent_id

        # 回溯结果是从叶到根，反转后返回从根到叶的顺序
        path.reverse()
        return path

    def to_dict(self) -> dict:
        """将 SessionState 序列化为可 JSON 持久化的字典"""
        return {
            "raw_problem": self.raw_problem,
            "parsed_problem": self.parsed_problem,
            "thinking_tree": {
                node_id: {
                    "node_id": node.node_id,
                    "content": node.content,
                    "status": node.status.value,
                    "parent_id": node.parent_id,
                    "error_history": node.error_history,
                    "children": node.children,
                }
                for node_id, node in self.thinking_tree.items()
            },
            "dialogue_history": self.dialogue_history,
            "current_stuck_node_id": self.current_stuck_node_id,
            "stuck_count": self.stuck_count,
            "last_updated_node_id": self.last_updated_node_id,
            "is_solved": self.is_solved,
            "solution": self.solution
        }

    @classmethod
    def from_dict(cls, data: dict) -> "SessionState":
        """从持久化字典还原 SessionState 实例"""
        # 先还原 thinking_tree
        thinking_tree = {
            node_id: ThinkingNode(
                node_id=node_data["node_id"],
                content=node_data["content"],
                status=NodeStatus(node_data["status"]),
                parent_id=node_data["parent_id"],
                error_history=node_data["error_history"],
                children=node_data["children"],
            )
            for node_id, node_data in data["thinking_tree"].items()
        }

        # 使用 object.__setattr__ 绕过 __post_init__ 的自动初始化逻辑
        # 因为 thinking_tree 已经有数据，不需要重新创建 n0 根节点
        instance = cls.__new__(cls)
        object.__setattr__(instance, "raw_problem", data["raw_problem"])
        object.__setattr__(instance, "parsed_problem", data["parsed_problem"])
        object.__setattr__(instance, "thinking_tree", thinking_tree)
        object.__setattr__(instance, "dialogue_history", data["dialogue_history"])
        object.__setattr__(instance, "current_stuck_node_id", data["current_stuck_node_id"])
        object.__setattr__(instance, "stuck_count", data["stuck_count"])
        object.__setattr__(instance, "last_updated_node_id", data["last_updated_node_id"])
        object.__setattr__(instance, "is_solved", data.get("is_solved", False))
        object.__setattr__(instance, "solution", data.get("solution", None))

        return instance
