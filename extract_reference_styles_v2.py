# -*- coding: utf-8 -*-
"""
提取参考文档的样式定义（从styles集合中读取）
"""
from docx import Document
from docx.shared import Pt
from docx.oxml.ns import qn
import json

REF_PATH = r"D:\Project\anti_fraud_platform\docx\《项目阶段二》评审\[2024]3-系统设计.docx"
doc = Document(REF_PATH)

# 提取所有样式定义
styles_data = {}
for style in doc.styles:
    if style.type == 1:  # PARAGRAPH
        style_info = {
            'name': style.name,
            'type': 'paragraph',
            'font': {},
            'paragraph_format': {}
        }
        
        # 字体信息
        font = style.font
        style_info['font']['name'] = font.name
        style_info['font']['size'] = str(font.size) if font.size else None
        style_info['font']['bold'] = font.bold
        if font.color and font.color.rgb:
            rgb = font.color.rgb
            style_info['font']['color'] = f"{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"
        
        # 段落格式
        pf = style.paragraph_format
        style_info['paragraph_format']['alignment'] = pf.alignment
        style_info['paragraph_format']['first_line_indent'] = str(pf.first_line_indent) if pf.first_line_indent else None
        style_info['paragraph_format']['line_spacing'] = pf.line_spacing
        style_info['paragraph_format']['space_before'] = str(pf.space_before) if pf.space_before else None
        style_info['paragraph_format']['space_after'] = str(pf.space_after) if pf.space_after else None
        
        # 中文字体
        if style.element.rPr:
            rFonts = style.element.rPr.find(qn('w:rFonts'))
            if rFonts is not None:
                style_info['font']['eastAsia'] = rFonts.get(qn('w:eastAsia'))
        
        styles_data[style.name] = style_info

# 打印关键样式
print("=" * 60)
print("参考文档样式定义")
print("=" * 60)

for style_name in ['Heading 1', 'Heading 2', 'Heading 3', 'Heading 4', 'Normal', 'List Paragraph']:
    if style_name in styles_data:
        info = styles_data[style_name]
        print(f"\n{style_name}:")
        print(f"  字体: {info['font'].get('name')} / {info['font'].get('eastAsia')}")
        print(f"  字号: {info['font'].get('size')}")
        print(f"  加粗: {info['font'].get('bold')}")
        print(f"  颜色: {info['font'].get('color')}")
        print(f"  对齐: {info['paragraph_format'].get('alignment')}")
        print(f"  首行缩进: {info['paragraph_format'].get('first_line_indent')}")
        print(f"  行距: {info['paragraph_format'].get('line_spacing')}")
        print(f"  段前: {info['paragraph_format'].get('space_before')}")
        print(f"  段后: {info['paragraph_format'].get('space_after')}")

# 提取表格样式
table_styles = {}
for style in doc.styles:
    if style.type == 2:  # TABLE
        table_styles[style.name] = {
            'name': style.name,
            'type': 'table'
        }

print("\n表格样式:")
for name in table_styles:
    print(f"  {name}")

# 保存到文件
with open('reference_styles_v2.json', 'w', encoding='utf-8') as f:
    json.dump({'paragraph_styles': styles_data, 'table_styles': table_styles}, f, ensure_ascii=False, indent=2)

print("\n样式信息已保存到 reference_styles_v2.json")
