"""从已下载的 OFL Noto Serif SC 生成离线 UI 正文字体（先安装 fontTools）。"""
import sys
import argparse
from pathlib import Path

sys.path.insert(0, str(Path('.tools/fonttools').resolve()))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

parser = argparse.ArgumentParser()
parser.add_argument('--medium-only', action='store_true', help='仅生成阅读设置使用的完整500字重')
args = parser.parse_args()
if args.medium_only:
    medium = TTFont('.tools/fonts/NotoSerifSC.ttf')
    instantiateVariableFont(medium, {'wght': 500}, inplace=True)
    # 与正文保持相同覆盖，动态备注和分类不能回落到其他字体。
    body = TTFont('app/src/main/res/font/ledger_text.ttf')
    medium_subset = subset.Subsetter(options=subset.Options())
    medium_subset.populate(unicodes=list(body.getBestCmap()))
    medium_subset.subset(medium)
    for entry in medium['name'].names:
        if entry.nameID in (1, 3, 4, 6, 16):
            name = 'XiaozhuhengLedgerMedium' if entry.nameID == 6 else 'Xiaozhuheng Ledger Medium'
            entry.string = name.encode(entry.getEncoding())
    output = Path('app/src/main/res/font/ledger_medium.ttf')
    medium.save(output)
    assert medium['OS/2'].usWeightClass == 500
    assert set(body.getBestCmap()) <= set(medium.getBestCmap())
    print('Medium font bytes:', output.stat().st_size)
    sys.exit(0)

font = TTFont('.tools/fonts/NotoSerifSC.ttf')
instantiateVariableFont(font, {'wght': 400}, inplace=True)
text = ''.join(p.read_text(encoding='utf-8') for p in Path('app/src/main/java').rglob('*.kt'))
text += ''.join(chr(c) for c in range(32, 127))
options = subset.Options()
subsetter = subset.Subsetter(options=options)
# 账单备注/用户分类是动态内容，不能只收录源码中的汉字，否则正文会混入系统黑体。
subsetter.populate(text=text, unicodes=list(range(0x4E00, 0xA000)) + list(range(0x3000, 0x3040)) + list(range(0xFF00, 0xFFF0)))
subsetter.subset(font)
for entry in font['name'].names:
    if entry.nameID in (1, 3, 4, 6, 16):
        name = 'XiaozhuhengLedgerText' if entry.nameID == 6 else 'Xiaozhuheng Ledger Text'
        entry.string = name.encode(entry.getEncoding())
output = Path('app/src/main/res/font/ledger_text.ttf')
font.save(output)
print('Ledger font bytes:', output.stat().st_size)

# 标题使用600字重；资源字符串也必须收录（品牌名位于strings.xml）。
heading = TTFont('.tools/fonts/NotoSerifSC.ttf')
instantiateVariableFont(heading, {'wght': 600}, inplace=True)
heading_text = text + ''.join(p.read_text(encoding='utf-8') for p in Path('app/src/main/res').rglob('*.xml'))
# 纳入截图库的真实分类、备注示例，避免视觉验收出现系统字体回落。
heading_text += ''.join(p.read_text(encoding='utf-8') for p in Path('app/src/visualQa').rglob('*.kt'))
heading_subset = subset.Subsetter(options=subset.Options())
heading_subset.populate(text=heading_text)
heading_subset.subset(heading)
for entry in heading['name'].names:
    if entry.nameID in (1, 3, 4, 6, 16):
        name = 'XiaozhuhengSongHeadings' if entry.nameID == 6 else 'Xiaozhuheng Song Headings'
        entry.string = name.encode(entry.getEncoding())
heading.save('app/src/main/res/font/song_headings.ttf')
print('Heading glyphs:', len(heading.getBestCmap()))
