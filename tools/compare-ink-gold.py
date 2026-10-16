"""将本次用户原图与 Android 原生截图并排检视，保持 2:3 参考比例。"""
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1]
out = root / 'docs/design/guofeng/qa'
out.mkdir(parents=True, exist_ok=True)
captures = root / 'app/src/test/snapshots/images'
prefix = 'com.example.bookkeeping.ui.home_InkGoldReferenceSnapshotTest_'
reference = Image.open(root / 'docs/design/guofeng/ink-gold-user-target.png').convert('RGB')
assert reference.size == (1024, 1536), reference.size
reference = reference.resize((600, 900), Image.Resampling.LANCZOS)
actual = Image.open(captures / (prefix + 'referenceRatio.png')).convert('RGB')
assert actual.size == (600, 900)
board = Image.new('RGB', (1200, 900))
board.paste(reference, (0, 0))
board.paste(actual, (600, 0))
board.save(out / 'ink-gold-v112-comparison.png')
board.crop((0, 0, 1200, 320)).save(out / 'ink-gold-v112-top.png')
board.crop((0, 320, 1200, 900)).save(out / 'ink-gold-v112-list.png')
phone = Image.open(captures / (prefix + 'phone.png')).convert('RGB')
photo = Image.open(captures / (prefix + 'photoBackground.png')).convert('RGB')
assert phone.size == photo.size == (393, 850)
variants = Image.new('RGB', (786, 850))
variants.paste(phone, (0, 0))
variants.paste(photo, (393, 0))
variants.save(out / 'ink-gold-v112-backgrounds.png')
print(out / 'ink-gold-v112-comparison.png')
print(out / 'ink-gold-v112-backgrounds.png')
