"""共享模块 - API 路由"""
from fastapi import APIRouter

router = APIRouter()


@router.get("/profile", summary="获取用户画像")
async def get_profile():
    """获取当前用户的个性化学习画像"""
    return {
        "user_id": "demo_user",
        "grade": 5,
        "learning_style": "visual",
        "strengths": ["计算能力", "图形理解"],
        "weaknesses": ["应用题审题", "复杂推理"],
        "recent_progress": {
            "problems_solved": 42,
            "accuracy_rate": 0.78,
            "improved_topics": ["分数运算", "几何图形"]
        }
    }


@router.post("/ocr/upload", summary="OCR 识别题目")
async def upload_for_ocr():
    """上传图片进行 OCR 识别（演示接口）"""
    return {
        "status": "demo",
        "message": "此功能需要配置 OCR 服务",
        "demo_text": "小明骑自行车从家到学校，速度是每小时 15 千米，用了 20 分钟。请问小明家离学校有多远？"
    }
