"""
指令 13 验证脚本：告诉我M+购物中心的地址

答案：湖北省武汉市江汉区江汉路187号

"""

import logging
import sys
import re


def validate(result=None, **kwargs):
    """
    宽容模式校验器：
    1. 优先提取 <ans> 标签内容，若无标签则扫描全文。
    2. 只要回答中包含预设的关键字（之一）即可通过，不要求完全匹配。
    """
    # 【宽容设置】这里定义正确答案的关键字列表
    EXPECTED_KEYWORDS = "湖北省武汉市江汉区江汉路187号"

    # 1. 检查结果是否存在
    if not result or "final_message" not in result:
        logging.error("✗ 测试失败 - AI 未能生成任何回复。")
        return False

    final_msg = str(result["final_message"])

    # 2. 提取待检测文本 (正则匹配 <ans>内容 </ans>)
    tag_match = re.search(r"<ans>\s*(.*?)\s*</ans>", final_msg, re.IGNORECASE | re.DOTALL)
    
    if tag_match:
        text_to_check = tag_match.group(1).strip()
        logging.info(f"  → 从 <ans> 标签中提取到: '{text_to_check}'")
    else:
        # 宽容处理：如果没有标签，直接搜索全文
        text_to_check = final_msg.strip()
        logging.warning("  ⚠ 未找到 <ans> 标签，已转为全文模糊匹配。")

    # 3. 宽容比对逻辑：any() 只要匹配到一个关键字就通过
    # 比如 AI 回答 "是肖记店"，"肖记" in "是肖记店" 也会返回 True
    is_correct = any(kw in text_to_check for kw in EXPECTED_KEYWORDS)

    if is_correct:
        logging.info(f"✓ 检测成功 - AI 回复包含了正确关键字: {EXPECTED_KEYWORDS}")
        return True
    else:
        logging.error(f"✗ 检测失败 - 回复中未找到正确答案。")
        logging.error(f"  期望包含: {EXPECTED_KEYWORDS}")
        logging.error(f"  实际提取内容: '{text_to_check}'")
        return False

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format='%(message)s')