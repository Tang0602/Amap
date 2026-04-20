"""
指令 1 验证脚本：告诉我美食排行榜中评分最高的美食, 把你的答案放在<ans>和</ans>之间.

答案：肖记公安牛肉鱼杂馆
"""

import re
from openai import OpenAI

# 1. 配置你的 API 信息
client = OpenAI(
    api_key="0915fc93-c8b4-4d1d-9f9d-5ab7b9ea8ea9",
    base_url="https://ark.cn-beijing.volces.com/api/v3"
)
MODEL_ID = "ep-20260415155052-4htzp"

# 2. 定义校验逻辑
def validate_answer(ai_message):
    EXPECTED_ANSWER = "肖记公安牛肉鱼杂馆"
    ANSWER_PATTERN = re.compile(r"<ans>\s*(.*?)\s*</ans>", re.IGNORECASE | re.DOTALL)
    
    match = ANSWER_PATTERN.search(ai_message)
    if not match:
        print("错误：AI 的回答中没有包含 <ans> 标签")
        return False
    
    answer = match.group(1).strip()
    print(f"AI 提取出的答案是: {answer}")
    return answer == EXPECTED_ANSWER

# 3. 执行真正的 AI 调用
def run_real_ai():
    print("正在请求豆包 AI 模型...")
    
    try:
        # 发送指令给 AI
        completion = client.chat.completions.create(
            model=MODEL_ID,
            messages=[
                {"role": "user", "content": "告诉我美食排行榜中评分最高的美食, 把你的答案放在<ans>和</ans>之间。简短回答"}
            ]
        )
        
        # 获取 AI 的回复
        ai_response = completion.choices[0].message.content
        print(f"AI 完整回答: \n{ai_response}")
        print("-" * 30)
        
        # 验证答案
        result = validate_answer(ai_response)
        
        if result:
            print("✅ 验证通过！AI 给出了正确答案。")
        else:
            print("❌ 验证失败！AI 的回答不符合预期。")
            
    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    run_real_ai()