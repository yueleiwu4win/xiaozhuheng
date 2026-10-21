"""生成参考/原生渲染的等比例比对板；不修改源参考和截图。需要 Pillow。"""
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = root / 'docs/design/guofeng'
captures = root / 'app/src/test/snapshots/images'
output = root / 'app/build/visual-qa'
output.mkdir(parents=True, exist_ok=True)
for name, filename in [('celadon', '02-celadon-v2.png'), ('inkGold', '03-ink-gold-v2.png')]:
    reference = Image.open(source / filename).convert('RGB').resize((393, 850), Image.Resampling.LANCZOS)
    actual = Image.open(captures / f'com.example.bookkeeping.ui.home_LedgerHomeSnapshotTest_{name}.png').convert('RGB')
    assert actual.size == (393, 850), actual.size
    board = Image.new('RGB', (786, 850), 'white')
    board.paste(reference, (0, 0))
    board.paste(actual, (393, 0))
    board.save(output / f'{name}-comparison.png')
    # 头部/概览及列表/底栏，保留相同坐标，放大便于核对文字。
    board.crop((0, 0, 786, 390)).resize((1179, 585)).save(output / f'{name}-top.png')
    board.crop((0, 390, 786, 850)).resize((1179, 690)).save(output / f'{name}-list.png')
    print(output / f'{name}-comparison.png')
