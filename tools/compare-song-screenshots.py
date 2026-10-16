"""并排检查绢本山水参考稿与原生 Compose 截图；只生成检视板。"""
from pathlib import Path
from PIL import Image
root = Path(__file__).resolve().parents[1]
output = root / 'docs/design/guofeng/qa'
output.mkdir(parents=True, exist_ok=True)
captures = root / 'app/src/test/snapshots/images'
prefix = 'com.example.bookkeeping.ui.home_SongReferenceSnapshotTest_'
reference = Image.open(root / 'docs/design/guofeng/song-landscape-target.png').convert('RGB')
assert reference.size == (853, 1844)
reference = reference.resize((390, 844), Image.Resampling.LANCZOS)
actual = Image.open(captures / (prefix + 'referenceRatio.png')).convert('RGB')
assert actual.size == (390, 844)
board = Image.new('RGB', (780, 844))
board.paste(reference, (0, 0))
board.paste(actual, (390, 0))
board.save(output / 'song-v114-comparison.png')
board.crop((0, 0, 780, 400)).save(output / 'song-v114-top.png')
board.crop((0, 390, 780, 844)).save(output / 'song-v114-list.png')
print(output / 'song-v114-comparison.png')
