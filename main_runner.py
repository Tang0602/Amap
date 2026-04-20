import importlib.util
import sys
import os
from openai import OpenAI

# API 配置保持不变
BASE_URL = "https://ark.cn-beijing.volces.com/api/v3"
API_KEY = "0915fc93-c8b4-4d1d-9f9d-5ab7b9ea8ea9"
MODEL_ID = "ep-20260415155052-4htzp"

def run_task(script_file):
    # 1. 构造路径并检查文件
    script_path = os.path.join("verification_scripts", script_file)
    if not os.path.exists(script_path):
        print(f"❌ 错误：找不到脚本 {script_path}")
        return

    # 2. 动态加载脚本
    spec = importlib.util.spec_from_file_location("eval_task", script_path)
    eval_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(eval_module)

    # 3. 提取指令
    instruction = eval_module.__doc__.strip()
    
    # 打印人工步骤提示（如果在脚本中定义了这些 print，也可以在这里触发）
    print("=" * 60)
    print(f"🚀 正在执行任务：{script_file}")
    print(f"📝 指令：{instruction}")
    print("=" * 60)

    # 4. 调用豆包 AI
    client = OpenAI(api_key=API_KEY, base_url=BASE_URL)
    try:
        completion = client.chat.completions.create(
            model=MODEL_ID,
            messages=[{"role": "user", "content": instruction}],
        )
        ai_response = completion.choices[0].message.content
        print(f"\n🤖 AI 回复：{ai_response}")

        # 5. 通用校验逻辑
        # 统一寻找名为 'validate' 的函数
        if hasattr(eval_module, 'validate'):
            result_payload = {"final_message": ai_response}
            success = eval_module.validate(result=result_payload)
            
            if success:
                print("\n✅ 检测成功！符合预期答案。")
            else:
                print("\n❌ 检测失败！答案错误或格式不符。")
        else:
            print("\n⚠️ 脚本中没有定义 validate 函数！")

    except Exception as e:
        print(f"\n引发错误: {e}")

if __name__ == "__main__":
    # 使用方法：python main_runner.py eval_2.py
    if len(sys.argv) > 1:
        target = sys.argv[1]
    else:
        # 如果没传参数，默认跑 eval_1.py
        target = "eval_1.py"
    
    run_task(target)