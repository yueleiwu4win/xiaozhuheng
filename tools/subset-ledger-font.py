"""从已下载的 OFL Noto Serif SC 生成离线 UI 正文字体（先安装 fontTools）。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path('.tools/fonttools').resolve()))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

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
