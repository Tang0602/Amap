"""
指令 1 验证脚本：告诉我美食排行榜中评分最高的美食

答案：肖记公安牛肉鱼杂馆

功能说明：
- 验证应用是否正确记录了美食排行榜中评分最高的美食信息
- 预期目标：肖记公安牛肉鱼杂
"""

import json
import subprocess
import sys


def verify_highest_score_food(device_id=None):
    # --- 设定预期正确答案 ---
    EXPECTED_NAME = "肖记公安牛肉鱼杂馆"
    # -----------------------

    try:
        # 构建 ADB 命令
        cmd = ["adb"]
        if device_id:
            cmd.extend(["-s", device_id])
        cmd.extend([
            "exec-out",
            "run-as",
            "com.example.amap_sim", 
            "cat",
            "files/1_highest_score_food.json"
        ])

        print(f"正在读取设备数据，匹配预期目标: {EXPECTED_NAME}...")
        result = subprocess.run(cmd, capture_output=True, text=False, check=True)

        # 处理编码
        try:
            stdout_text = result.stdout.decode("utf-8")
        except UnicodeDecodeError:
            try:
                stdout_text = result.stdout.decode("gbk")
            except UnicodeDecodeError:
                stdout_text = result.stdout.decode("utf-8", errors="ignore")

        if not stdout_text.strip():
            print("❌ FAIL: JSON 文件为空")
            return False

        # 解析 JSON
        json_data = json.loads(stdout_text)

        # 1. 验证字段是否存在
        if "name" not in json_data or "rating" not in json_data:
            print("❌ FAIL: 缺少必要字段 'name' 或 'rating'")
            return False

        name = json_data["name"]
        rating = json_data["rating"]

        # 2. 验证评分是否有效
        if rating <= 0:
            print(f"❌ FAIL: 评分无效 ({rating})")
            return False

        # 3. 【核心修改】验证名称是否包含正确答案
        if EXPECTED_NAME not in name:
            print(f"❌ FAIL: 美食名称中未包含预期答案")
            print(f"   预期答案: {EXPECTED_NAME}")
            print(f"   实际结果: {name}")
            return False

        # 验证通过
        print("✓ PASS: 验证成功！")
        print(f"   匹配到正确答案: {name}")
        print(f"   评分: {rating}")
        return True

    except subprocess.CalledProcessError:
        print("❌ FAIL: ADB 执行失败，请检查设备连接或 App 是否安装")
        return False
    except json.JSONDecodeError:
        print(f"❌ FAIL: JSON 格式错误。原始输出: {stdout_text}")
        return False
    except Exception as e:
        print(f"❌ FAIL: 发生未知错误: {e}")
        return False


if __name__ == "__main__":
    print("=" * 60)
    print("指令验证：寻找美食排行榜中评分最高的美食")
    print("预期答案：肖记公安牛肉鱼杂馆")
    print("=" * 60)

    if verify_highest_score_food():
        print("\n结果：[通过]")
        sys.exit(0)
    else:
        print("\n结果：[不通过]")
        sys.exit(1)