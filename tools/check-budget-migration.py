"""Exercise the production migration SQL against SQLite and exported Room schemas.

Run: python tools/check-budget-migration.py
Complements Android instrumentation; does not claim to run Room on a device.
"""
import json
import re
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
schema_dir = root / 'app/schemas/com.example.bookkeeping.data.db.AppDatabase'
old = json.loads((schema_dir / '1.json').read_text())['database']
new = json.loads((schema_dir / '2.json').read_text())['database']
source = (root / 'app/src/main/java/com/example/bookkeeping/data/db/AppDatabase.kt').read_text(encoding='utf-8-sig')
sql = re.search(r'db.execSQL\("([^"]+)"\)', source).group(1)
db = sqlite3.connect(':memory:')
db.execute('PRAGMA foreign_keys=ON')
for entity in old['entities']:
    name = entity['tableName']
    db.execute(entity['createSql'].replace('${TABLE_NAME}', name))
    for index in entity['indices']:
        db.execute(index['createSql'].replace('${TABLE_NAME}', name))
db.execute("INSERT INTO category VALUES (1, '餐饮', 'expense', 'food', '#214F43', 0, 1, 0, 1)")
db.execute("INSERT INTO bill (id,amount_cents,type,category_id,date,year,month,note,currency,created_at,updated_at) VALUES (1,12345,'expense',1,'2026-10-28',2026,10,'升级前记录','CNY',1,1)")
db.commit()
before = {t: db.execute(f'SELECT * FROM {t}').fetchall() for t in ['bill', 'category']}
with db:
    db.execute(sql)
for entity in old['entities']:
    assert entity == next(e for e in new['entities'] if e['tableName'] == entity['tableName']), 'Existing Room schema changed'
for table, rows in before.items():
    assert db.execute(f'SELECT * FROM {table}').fetchall() == rows
expected = sqlite3.connect(':memory:')
entity = next(e for e in new['entities'] if e['tableName'] == 'monthly_budget')
expected.execute(entity['createSql'].replace('${TABLE_NAME}', 'monthly_budget'))
assert db.execute('PRAGMA table_info(monthly_budget)').fetchall() == expected.execute('PRAGMA table_info(monthly_budget)').fetchall()
with db:
    db.execute("INSERT INTO monthly_budget VALUES ('2026-09', 200000, 1)")
    db.execute("INSERT INTO monthly_budget VALUES ('2026-10', 300000, 1)")
    db.execute("UPDATE monthly_budget SET amountCents=350000 WHERE month='2026-10'")
assert db.execute("SELECT amountCents FROM monthly_budget WHERE month='2026-09'").fetchone()[0] == 200000
assert not db.execute('PRAGMA foreign_key_check').fetchall()
try:
    with db:
        db.execute('DELETE FROM monthly_budget')
        db.execute("INSERT INTO bill (id) VALUES (1)")
except sqlite3.IntegrityError:
    pass
assert db.execute('SELECT COUNT(*) FROM monthly_budget').fetchone()[0] == 2, 'Transaction failed to roll back'
assert db.execute('SELECT * FROM bill').fetchall() == before['bill']
print('PASS: v1 data preserved; v2 schema matches; months independent; foreign keys valid; transaction rollback preserves budgets.')
