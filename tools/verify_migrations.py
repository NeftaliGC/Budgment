"""Uso: python3 tools/verify_migrations.py .   (desde la raíz del proyecto; requiere app/schemas)

Verifica la cadena de migraciones de Budgment sobre SQLite real.

Para cada versión inicial (1..N-1): crea la base con el esquema exportado de esa versión,
inserta datos, aplica las migraciones extraídas de Migrations.kt y compara el esquema final
(columnas, tipos, NOT NULL, PK, FKs, índices) con el de la última versión. También comprueba
que los datos sigan ahí.
"""
import glob, json, re, sqlite3, sys

ROOT = sys.argv[1]
schemas = {int(p.rsplit('/', 1)[1][:-5]): json.load(open(p))['database']
           for p in glob.glob(f'{ROOT}/app/schemas/*/*.json')}
LATEST = max(schemas)
# v1 = las tablas users/accounts/categories del esquema v2 (no cambiaron entre v1 y v2).
schemas[1] = {'entities': [e for e in schemas[2]['entities'] if e['tableName'] in ('users', 'accounts', 'categories')]}

src = open(f'{ROOT}/app/src/main/java/com/nintech/budgment/data/local/Migrations.kt').read()
migrations = {}
for m in re.finditer(r'Migration\((\d+),\s*(\d+)\)\s*\{(.*?)\n\}', src, re.S):
    stmts = []
    for call in re.finditer(r'execSQL\(\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)\)', m.group(3)):
        lits = re.findall(r'"((?:[^"\\]|\\.)*)"', call.group(1))
        stmts.append(''.join(lits).replace('\\$', '$').replace('\\"', '"'))
    migrations[(int(m.group(1)), int(m.group(2)))] = stmts

def create(conn, version):
    for e in schemas[version]['entities']:
        conn.execute(e['createSql'].replace('${TABLE_NAME}', e['tableName']))
        for i in e.get('indices', []):
            conn.execute(i['createSql'].replace('${TABLE_NAME}', e['tableName']))
    conn.execute('CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)')

def describe(conn):
    out = {}
    for (t,) in conn.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'"):
        cols = sorted((r[1], r[2].upper(), r[3], r[5]) for r in conn.execute(f'PRAGMA table_info(`{t}`)'))
        fks = sorted((r[2], r[3], r[4], r[5], r[6]) for r in conn.execute(f'PRAGMA foreign_key_list(`{t}`)'))
        idx = sorted(
            (r[1], r[2], tuple(c[2] for c in conn.execute(f'PRAGMA index_info(`{r[1]}`)')))
            for r in conn.execute(f'PRAGMA index_list(`{t}`)') if not r[1].startswith('sqlite_autoindex')
        )
        out[t] = (cols, fks, idx)
    return out

def seed(conn, version):
    tables = {e['tableName'] for e in schemas[version]['entities']}
    conn.execute("INSERT INTO users (id, username, email, passwordHash, name, defaultCurrency, createdAt) VALUES ('u','ana','a@x','h','Ana','MXN',1)") if 'users' in tables else None
    cols = lambda t: [r[1] for r in conn.execute(f'PRAGMA table_info(`{t}`)')]
    def insert(t, values):
        known = {k: v for k, v in values.items() if k in cols(t)}
        conn.execute(f"INSERT INTO `{t}` ({', '.join(known)}) VALUES ({', '.join('?' * len(known))})", list(known.values()))
    insert('accounts', dict(id='a', userId='u', name='Débito', accountType='DEBIT', currency='MXN', balanceCache=150000,
                            includeInTotal=1, archived=0, createdAt=1, initialBalance=0))
    insert('categories', dict(id='c', userId='u', name='Comida', type='EXPENSE', isDefault=0, createdAt=1))
    if 'transactions' in tables:
        insert('transactions', dict(id='t', userId='u', accountId='a', categoryId='c', amount=-5000, date=2, createdAt=2))
    if 'goals' in tables:
        insert('goals', dict(id='g', userId='u', name='Viaje', targetAmount=100000, currentAmount=2000, createdAt=3))

def counts(conn):
    return {t: conn.execute(f'SELECT COUNT(*) FROM `{t}`').fetchone()[0] for t in ('users', 'accounts', 'categories', 'transactions', 'goals')
            if conn.execute("SELECT 1 FROM sqlite_master WHERE name=?", (t,)).fetchone()}

expected_conn = sqlite3.connect(':memory:'); create(expected_conn, LATEST)
expected = describe(expected_conn)

ok = True
print(f'SQLite {sqlite3.sqlite_version}; migraciones encontradas: {sorted(migrations)}')
for start in range(1, LATEST):
    conn = sqlite3.connect(':memory:')
    create(conn, start); seed(conn, start); conn.commit()
    before = counts(conn)
    v = start
    try:
        while v < LATEST:
            for stmt in migrations[(v, v + 1)]:
                conn.execute(stmt)
            v += 1
        conn.commit()
    except Exception as e:
        print(f'v{start}: FALLA en {v}→{v + 1}: {e}'); ok = False; continue
    got = describe(conn)
    diffs = [t for t in set(expected) | set(got) if expected.get(t) != got.get(t)]
    after = counts(conn)
    lost = {t: (n, after.get(t)) for t, n in before.items() if after.get(t) != n}
    fk = conn.execute('PRAGMA foreign_key_check').fetchall()
    if diffs or lost or fk:
        ok = False
        print(f'v{start} → v{LATEST}: ERROR esquema={diffs} datos={lost} fk={fk}')
        for t in diffs: print('  esperado', expected.get(t)); print('  obtenido', got.get(t))
    else:
        print(f'v{start} → v{LATEST}: OK (datos conservados: {before})')
sys.exit(0 if ok else 1)
