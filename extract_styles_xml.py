# -*- coding: utf-8 -*-
"""
从XML层面提取表格样式和段落样式的字体定义
"""
from docx import Document
from docx.oxml.ns import qn
import lxml.etree as ET

REF_PATH = r"D:\Project\anti_fraud_platform\docx\《项目阶段二》评审\[2024]3-系统设计.docx"
doc = Document(REF_PATH)

def emu_to_pt(emu_str):
    if not emu_str:
        return None
    try:
        emu = int(emu_str)
        return emu / 12700
    except:
        # 半点数
        if 'halfPoints' in emu_str:
            return None
        return None

def get_cn_size(pt):
    if pt is None:
        return 'None'
    if abs(pt - 16) < 0.5: return '三号 (16pt)'
    elif abs(pt - 14) < 0.5: return '四号 (14pt)'
    elif abs(pt - 12) < 0.5: return '小四 (12pt)'
    elif abs(pt - 10.5) < 0.5: return '五号 (10.5pt)'
    elif abs(pt - 9) < 0.5: return '小五 (9pt)'
    else: return f'{pt:.1f}pt'

# 获取styles.xml
styles_xml = None
for part in doc.part.package.iter_parts():
    if 'styles' in part.partname:
        styles_xml = part.blob
        break

if styles_xml is None:
    # 从文档的styles部分获取
    from docx.oxml.ns import nsmap
    styles_element = doc.styles.element
    styles_xml = ET.tostring(styles_element, pretty_print=True)

# 解析styles.xml
root = ET.fromstring(styles_xml)
ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}

print("=" * 70)
print("段落样式字体定义")
print("=" * 70)

# 查找所有段落样式
for style in root.findall('.//w:style[@w:type="paragraph"]', ns):
    style_id = style.get(qn('w:styleId'))
    name_elem = style.find('w:name', ns)
    style_name = name_elem.get(qn('w:val')) if name_elem is not None else style_id
    
    # 只关注关键样式
    key_styles = ['Heading 1', 'Heading 2', 'Heading 3', 'Heading 4', 'Normal', 'List Paragraph', 'heading 1', 'heading 2', 'heading 3', 'heading 4']
    if style_name not in key_styles and style_id not in ['1', '2', '3', '4']:
        continue
    
    rPr = style.find('w:rPr', ns)
    if rPr is not None:
        sz = rPr.find('w:sz', ns)
        szCs = rPr.find('w:szCs', ns)
        rFonts = rPr.find('w:rFonts', ns)
        b = rPr.find('w:b', ns)
        
        size_pt = None
        if sz is not None:
            sz_val = sz.get(qn('w:val'))
            if sz_val:
                size_pt = int(sz_val) / 2  # sz单位是半磅
        
        eastAsia_font = None
        ascii_font = None
        if rFonts is not None:
            eastAsia_font = rFonts.get(qn('w:eastAsia'))
            ascii_font = rFonts.get(qn('w:ascii'))
        
        print(f"\n【{style_name}】(styleId: {style_id})")
        print(f"  字号: {size_pt}pt → {get_cn_size(size_pt)}")
        print(f"  加粗: {'是' if b is not None else '否'}")
        print(f"  ASCII字体: {ascii_font}")
        print(f"  东亚字体: {eastAsia_font}")
        
        # 检查pPr中的对齐和间距
        pPr = style.find('w:pPr', ns)
        if pPr is not None:
            jc = pPr.find('w:jc', ns)
            spacing = pPr.find('w:spacing', ns)
            if jc is not None:
                print(f"  对齐: {jc.get(qn('w:val'))}")
            if spacing is not None:
                before = spacing.get(qn('w:before'))
                after = spacing.get(qn('w:after'))
                line = spacing.get(qn('w:line'))
                if before: print(f"  段前: {int(before)/20}pt")
                if after: print(f"  段后: {int(after)/20}pt")
                if line: print(f"  行距: {line} (line)")

print("\n" + "=" * 70)
print("表格样式字体定义")
print("=" * 70)

# 查找所有表格样式
for style in root.findall('.//w:style[@w:type="table"]', ns):
    style_id = style.get(qn('w:styleId'))
    name_elem = style.find('w:name', ns)
    style_name = name_elem.get(qn('w:val')) if name_elem is not None else style_id
    
    if '网格' not in style_name and 'Table' not in style_name:
        continue
    
    print(f"\n【{style_name}】(styleId: {style_id})")
    
    # 查找tblPr中的信息
    tblPr = style.find('w:tblPr', ns)
    if tblPr is not None:
        print(f"  表格属性: 找到")
    
    # 查找trPr中的信息
    trPr = style.find('w:trPr', ns)
    if trPr is not None:
        print(f"  行属性: 找到")
    
    # 查找tcPr中的信息
    tcPr = style.find('w:tcPr', ns)
    if tcPr is not None:
        print(f"  单元格属性: 找到")
    
    # 查找rPr（字体信息）
    rPr = style.find('w:rPr', ns)
    if rPr is not None:
        sz = rPr.find('w:sz', ns)
        rFonts = rPr.find('w:rFonts', ns)
        b = rPr.find('w:b', ns)
        
        size_pt = None
        if sz is not None:
            sz_val = sz.get(qn('w:val'))
            if sz_val:
                size_pt = int(sz_val) / 2
        
        eastAsia_font = None
        ascii_font = None
        if rFonts is not None:
            eastAsia_font = rFonts.get(qn('w:eastAsia'))
            ascii_font = rFonts.get(qn('w:ascii'))
        
        print(f"  字号: {size_pt}pt → {get_cn_size(size_pt)}")
        print(f"  加粗: {'是' if b is not None else '否'}")
        print(f"  ASCII字体: {ascii_font}")
        print(f"  东亚字体: {eastAsia_font}")
    else:
        print(f"  无直接字体定义（继承自正文样式）")
    
    # 检查是否有表头行样式
    tblStylePr = style.findall('w:tblStylePr', ns)
    for pr in tblStylePr:
        pr_type = pr.get(qn('w:type'))
        if pr_type in ['firstRow', 'lastRow', 'wholeTable']:
            print(f"\n  子样式: {pr_type}")
            rPr2 = pr.find('w:rPr', ns)
            if rPr2 is not None:
                sz2 = rPr2.find('w:sz', ns)
                b2 = rPr2.find('w:b', ns)
                size2 = None
                if sz2 is not None:
                    sz_val = sz2.get(qn('w:val'))
                    if sz_val:
                        size2 = int(sz_val) / 2
                print(f"    字号: {size2}pt → {get_cn_size(size2)}")
                print(f"    加粗: {'是' if b2 is not None else '否'}")
