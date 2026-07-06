# -*- coding: utf-8 -*-
"""检查5.3.9模块完整性"""
from docx import Document

path = r"D:\Project\anti_fraud_platform\docx\《项目阶段二》评审\专业综合课程设计-系统设计说明书-江乐霖-23201317.docx"
doc = Document(path)

print("=" * 80)
print("【5.3.9 积分成就与排行榜模块】")
print("=" * 80)
for i, p in enumerate(doc.paragraphs):
    text = p.text.strip()
    style = p.style.name if p.style else ""
    if i >= 937:  # 从5.3.9开始
        if "6 用户界面" in text:
            break
        if style.startswith("Heading") or (style == "Normal" and text):
            print(f"[{i:03d}] <{style}> {text[:70]}")