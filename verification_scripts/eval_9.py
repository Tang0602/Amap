"""
指令 9 验证脚本：删除最早的一次历史导航记录

答案：已删除最早的历史记录

功能说明：
- 验证应用是否正确执行了删除最早历史路线的任务
- 通过 ADB 读取应用私有存储中的 JSON 文件
- 检查 JSON 文件中是否包含必要的字段：deleted（���否已删除）、routeId（路线ID）、timestamp（时间戳）

验证逻辑：
1. 使用 ADB 读取 9_delete_recent_route.json 文件
2. 解析 JSON 内容
3. 验证 deleted 字段为 true（表示已删除）
4. 验证 timestamp 字段存在（用于标识被删除记录的时间，应该是最早的时间戳）
5. 返回验证结果（PASS/FAIL）
"""

import json
import subprocess
import sys


def verify_delete_recent_route(device_id=None):
    """
    验证删除历史路线任务是否完成

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
            "files/9_delete_recent_route.json"  # JSON 文件路径
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
        if "deleted" not in json_data:
            print("❌ FAIL: 缺少 'deleted' 字段")
            return False

        if "routeId" not in json_data:
            print("❌ FAIL: 缺少 'routeId' 字段")
            return False

        if "timestamp" not in json_data:
            print("❌ FAIL: 缺少 'timestamp' 字段")
            return False

        # 获取字段值
        deleted = json_data["deleted"]
        route_id = json_data["routeId"]
        timestamp = json_data["timestamp"]

        # 验证是否已删除
        if not deleted:
            print("❌ FAIL: 'deleted' 字段为 false，任务未完成")
            print(f"   当前值: {deleted}")
            return False

        # 验证时间戳是否有效（应该是最早的历史记录时间戳）
        if timestamp <= 0:
            print("❌ FAIL: 'timestamp' 字段无效（应大于 0）")
            print(f"   当前值: {timestamp}")
            return False

        # 验证通过，输出结果
        print("✓ PASS: 删除最早历史路线任务验证成功")
        print(f"   已删除: {deleted}")
        print(f"   路线ID: {route_id}")
        print(f"   时间戳: {timestamp}")

        # 尝试将时间戳转换为可读时间
        try:
            from datetime import datetime
            readable_time = datetime.fromtimestamp(timestamp / 1000).strftime('%Y-%m-%d %H:%M:%S')
            print(f"   删除时间: {readable_time}")
        except:
            pass

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
    print("指令 9 验证：删除最早的一次历史导航记录")
    print("=" * 60)

    # 执行验证
    success = verify_delete_recent_route()

    # 输出最终结果
    print("=" * 60)
    if success:
        print("✓ 验证通过")
        sys.exit(0)
    else:
        print("✗ 验证失败")
        sys.exit(1)
