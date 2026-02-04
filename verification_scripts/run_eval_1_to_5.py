"""
批量验证脚本：运行前 5 个指令的验证

功能说明：
- 依次运行前 5 个指令的验证脚本
- 统计验证通过和失败的数量
- 输出详细的验证报告

使用方法：
    python run_eval_1_to_5.py
"""

import subprocess
import sys


def run_verification_script(script_name, instruction_desc):
    """
    运行单个验证脚本

    参数：
        script_name (str): 脚本文件名
        instruction_desc (str): 指令描述

    返回：
        bool: 验证通过返回 True，否则返回 False
    """
    print(f"\n{'=' * 70}")
    print(f"正在验证: {instruction_desc}")
    print(f"脚本: {script_name}")
    print(f"{'=' * 70}")

    try:
        # 运行验证脚本
        result = subprocess.run(
            ["python", script_name],
            capture_output=True,
            text=True,
            timeout=30
        )

        # 输出脚本的标准输出
        if result.stdout:
            print(result.stdout)

        # 输出脚本的标准错误（如果有）
        if result.stderr:
            print("错误输出:")
            print(result.stderr)

        # 检查返回码
        return result.returncode == 0

    except subprocess.TimeoutExpired:
        print(f"❌ 超时: 脚本执行超过 30 秒")
        return False
    except Exception as e:
        print(f"❌ 执行失败: {e}")
        return False


def main():
    """
    主函数：运行所有验证脚本并生成报告
    """
    print("=" * 70)
    print("Agent 指令验证 - 前 5 个指令批量验证")
    print("=" * 70)

    # 定义要验证的脚本列表
    verifications = [
        ("eval_1.py", "指令 1: 告诉我美食排行榜中评分最高的美食"),
        ("eval_2.py", "指令 2: 告诉我最近一次导航去了哪个地点"),
        ("eval_3.py", "指令 3: 告诉我账号的名字和id"),
        ("eval_4.py", "指令 4: 告诉我周边最近的酒店名字"),
        ("eval_5.py", "指令 5: 告诉我收藏夹收藏了几个地点"),
    ]

    # 记录验证结果
    results = []
    passed_count = 0
    failed_count = 0

    # 依次运行每个验证脚本
    for script_name, instruction_desc in verifications:
        success = run_verification_script(script_name, instruction_desc)
        results.append((instruction_desc, success))

        if success:
            passed_count += 1
        else:
            failed_count += 1

    # 输出验证报告
    print("\n" + "=" * 70)
    print("验证报告")
    print("=" * 70)

    for instruction_desc, success in results:
        status = "✓ PASS" if success else "✗ FAIL"
        print(f"{status} - {instruction_desc}")

    print("=" * 70)
    print(f"总计: {len(results)} 个指令")
    print(f"通过: {passed_count} 个")
    print(f"失败: {failed_count} 个")
    print(f"通过率: {passed_count / len(results) * 100:.1f}%")
    print("=" * 70)

    # 根据结果返回退出码
    if failed_count == 0:
        print("\n✓ 所有验证通过！")
        sys.exit(0)
    else:
        print(f"\n✗ 有 {failed_count} 个验证失败")
        sys.exit(1)


if __name__ == "__main__":
    main()
