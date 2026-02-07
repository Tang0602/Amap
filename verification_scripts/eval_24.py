"""
指令 24 验证脚本：告诉我现在的位置，距离武汉市公安局（江岸分局）的周边美食排行榜第一名驾车需要几分钟

答案：8分钟 或 8 或 八分钟

功能说明：
- 验证应用是否正确回答了驾车到指定地点周边美食排行榜第一名需要的时间
- 通过 ADB 读取应用私有存储中的 JSON 文件
- 检查 JSON 文件中是否包含必要的字段：location（位置）、topFood（第一名美食）、drivingMinutes（驾车分钟数）

验证逻辑：
1. 使用 ADB 读取 24_walking_time_to_food.json 文件
2. 解析 JSON 内容
3. 验证 drivingMinutes 字段包含 "8" 或 "八"
4. 验证 location 和 topFood 字段不为空
5. 返回验证结果（PASS/FAIL）
"""

import json
import subprocess
import sys

# 预设的正确答案
EXPECTED_MINUTES = ["8", "八"]


def verify_driving_time_to_food(device_id=None):
    """
    验证驾车到美食店的时间信息

    参数：
        device_id (str): Android 设备 ID，如果为 None 则使用默认设备

    返回：
        bool: 验证通过返回 True，否则返回 False
    """
    try:
        # 构建 ADB 命令，读取应用私有存储中的 JSON 文件
        cmd = ["adb"]
        if device_id:
            cmd.extend(["-s", device_id])
        cmd.extend([
            "exec-out",
            "run-as",
            "com.example.amap_sim",  # 应用包名
            "cat",
            "files/24_walking_time_to_food.json"  # JSON 文件路径
        ])

        print("正在执行 ADB 命令读取文件...")
        result = subprocess.run(cmd, capture_output=True, text=False, check=True)

        # 处理输出编码（支持 UTF-8 和 GBK）
        try:
            stdout_text = result.stdout.decode("utf-8")
        except UnicodeDecodeError:
            try:
                stdout_text = result.stdout.decode("gbk")
            except UnicodeDecodeError:
                stdout_text = result.stdout.decode("utf-8", errors="ignore")

        # 检查文件是否为空
        if not stdout_text.strip():
            print("❌ FAIL: JSON 文件为空")
            return False

        # 解析 JSON 内容
        print("正在解析 JSON 内容...")
        json_data = json.loads(stdout_text)

        # 验证必要字段是否存在
        if "location" not in json_data:
            print("❌ FAIL: 缺少 'location' 字段")
            return False

        if "topFood" not in json_data:
            print("❌ FAIL: 缺少 'topFood' 字段")
            return False

        if "drivingMinutes" not in json_data:
            print("❌ FAIL: 缺少 'drivingMinutes' 字段")
            return False

        # 获取字段值
        location = json_data["location"]
        top_food = json_data["topFood"]
        driving_minutes = json_data["drivingMinutes"]

        # 将驾车时间转换为字符串进行检查
        driving_minutes_str = str(driving_minutes)

        # 验证驾车时间是否包含预设答案中的任意一个
        found_match = False
        matched_answer = None
        for expected in EXPECTED_MINUTES:
            if expected in driving_minutes_str:
                found_match = True
                matched_answer = expected
                break

        if not found_match:
            print("❌ FAIL: 驾车时间中未包含预期答案")
            print(f"   预期答案（任意一个）: {', '.join(EXPECTED_MINUTES)}")
            print(f"   实际结果: {driving_minutes_str}")
            return False

        # 验证地点是否有效
        if not location or location == "":
            print("❌ FAIL: 'location' 字段为空")
            print(f"   当前值: '{location}'")
            return False

        # 验证美食名称是否有效
        if not top_food or top_food == "":
            print("❌ FAIL: 'topFood' 字段为空")
            print(f"   当前值: '{top_food}'")
            return False

        # 验证通过，输出结果
        print("✓ PASS: 驾车到美食店时间验证成功")
        print(f"   地点: {location}")
        print(f"   第一名美食: {top_food}")
        print(f"   驾车时间: {driving_minutes_str}")
        print(f"   匹配到的答案: {matched_answer}")

        return True

    except subprocess.CalledProcessError as e:
        print(f"❌ FAIL: ADB 命令执行失败 - {e}")
        try:
            error_text = e.stderr.decode("utf-8") if e.stderr else "无错误输出"
        except:
            error_text = "无法解码错误信息"
        print(f"   错误信息: {error_text}")
        return False

    except json.JSONDecodeError as e:
        print(f"❌ FAIL: JSON 解析错误 - {e}")
        print(f"   原始内容: {stdout_text}")
        return False

    except Exception as e:
        print(f"❌ FAIL: 未预期的错误 - {e}")
        return False


if __name__ == "__main__":
    print("=" * 60)
    print("指令 24 验证：告诉我现在的位置，距离武汉市公安局（江岸分局）的周边美食排行榜第一名驾车需要几分钟")
    print("=" * 60)

    # 执行验证
    success = verify_driving_time_to_food()

    # 输出最终结果
    print("=" * 60)
    if success:
        print("✓ 验证通过")
        sys.exit(0)
    else:
        print("✗ 验证失败")
        sys.exit(1)
