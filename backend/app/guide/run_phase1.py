from phase1_parser.parser import parse_problem


def main():
    print("=== 苏格拉底讲题模块 - 第一阶段测试 ===\n")
    print("请输入题目内容（输入 'quit' 退出）：\n")

    while True:
        problem = input(">>> ")

        if problem.strip().lower() == 'quit':
            print("退出测试")
            break

        if not problem.strip():
            print("题目不能为空，请重新输入\n")
            continue

        try:
            result = parse_problem(problem)
            print("\n【解析结果】")
            print(f"已知条件：{result['known_conditions']}")
            print(f"求解目标：{result['goal']}")
            print(f"标准答案：{result['answer']}")
            print("\n" + "=" * 50 + "\n")
        except Exception as e:
            print(f"解析失败：{e}\n")


if __name__ == "__main__":
    main()
