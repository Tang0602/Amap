"""
指令 1 验证脚本：告诉我美食排行榜中评分最高的美食

功能说明：
- 验证应用是否正确记录了美食排行榜中评分最高的美食信息
- 通过 ADB 读取应用私有存储中的 JSON 文件
- 检查 JSON 文件中是否包含必要的字段：name（名称）、rating（评分）

验证逻辑：
1. 使用 ADB 读取 1_highest_score_food.json 文件
2. 解析 JSON 内容
3. 验证 name 字段不为空
4. 验证 rating 字段大于 0
5. 返回验证结果（PASS/FAIL）
"""

import json
import subprocess
import sys


def verify_highest_score_food(device_id=None):
    """
    验证美食排行榜评分最高的美食信息

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
            "files/1_highest_score_food.json"  # JSON 文件路径
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
        if "name" not in json_data:
            print("❌ FAIL: 缺少 'name' 字段")
            return False

        if "rating" not in json_data:
            print("❌ FAIL: 缺少 'rating' 字段")
            return False

        # 获取字段值
        name = json_data["name"]
        rating = json_data["rating"]

        # 验证字段值是否有效
        if not name or name == "":
            print("❌ FAIL: 'name' 字段为空")
            print(f"   当前值: '{name}'")
            return False

        if rating <= 0:
            print("❌ FAIL: 'rating' 字段无效（应大于 0）")
            print(f"   当前值: {rating}")
            return False

        # 验证通过，输出结果
        print("✓ PASS: 美食排行榜评分最高的美食验证成功")
        print(f"   美食名称: {name}")
        print(f"   评分: {rating}")
        if "category" in json_data:
            print(f"   分类: {json_data['category']}")
        if "address" in json_data:
            print(f"   地址: {json_data['address']}")
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
    print("指令 1 验证：告诉我美食排行榜中评分最高的美食")
    print("=" * 60)

    # 执行验证
    success = verify_highest_score_food()

    # 输出最终结果
    print("=" * 60)
    if success:
        print("✓ 验证通过")
        sys.exit(0)
    else:
        print("✗ 验证失败")
        sys.exit(1)
