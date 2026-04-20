"""
指令 22 验证脚本：告诉我台北路公共停车场停车收费标准（一个小时多少钱）

"""
import logging
import sys
import re

def validate(result=None, **kwargs):
    """
    更宽容的校验器：
    只要 AI 的回答中包含了关键词列表里的【任意一个】，就算通过。
    """
    # 这里放所有可能的正确表达方式（比如数字和汉字大写）
    EXPECTED_KEYWORDS = ["4", "四"] 

    if not result or "final_message" not in result:
        return False

    final_msg = str(result["final_message"])

    # 1. 提取检测范围（有标签看标签，没标签看全文）
    tag_match = re.search(r"<ans>\s*(.*?)\s*</ans>", final_msg, re.IGNORECASE | re.DOTALL)
    text_to_check = tag_match.group(1).strip() if tag_match else final_msg.strip()

    # 2. 宽容匹配逻辑：使用 any()
    # 只要 EXPECTED_KEYWORDS 里的【任意一个】词在 text_to_check 中，就返回 True
    is_correct = any(kw in text_to_check for kw in EXPECTED_KEYWORDS)

    if is_correct:
        logging.info(f"✅ 检测成功！匹配到关键信息之一: {EXPECTED_KEYWORDS}")
        return True
    else:
        logging.error(f"❌ 检测失败！回复中未匹配到预设关键词。预期其中之一: {EXPECTED_KEYWORDS}, 实际得到: '{text_to_check}'")
        return False

if __name__ == "__main__":
    # 本地手动运行脚本时的逻辑（主要用于显示人工步骤）
    logging.basicConfig(level=logging.INFO, format='%(message)s')