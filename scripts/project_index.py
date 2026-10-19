#!/usr/bin/env python3
"""Local source navigation index. No dependencies; declarations are heuristic."""
import argparse
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parent.parent
INDEX = ROOT / '.project-index.json'
EXTENSIONS = {'.java', '.kt', '.kts', '.py', '.js', '.ts', '.tsx', '.jsx',
              '.c', '.cpp', '.cc', '.h', '.hpp', '.go', '.rs', '.sh', '.ps1',
              '.gradle', '.xml', '.yml', '.yaml', '.md', '.properties'}
DECL = re.compile(r'\b(?:class|interface|enum|record|object|struct|trait|def|fun|function|fn)\s+(\w+)')
JAVA_METHOD = re.compile(r'^\s*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|override|inline|suspend)\s+)+(?:[\w<>?,.\[\]]+\s+)(\w+)\s*\(')


def paths():
    result = subprocess.run(['git', 'ls-files', '--cached', '--others',
                             '--exclude-standard', '-z'], cwd=ROOT,
                            check=True, capture_output=True)
    return sorted({p for p in result.stdout.decode().split('\0')
                   if p and Path(p).suffix in EXTENSIONS
                   and not any(part in {'build', 'node_modules', '.gradle', 'dist'}
                               for part in Path(p).parts)})


def update():
    previous = json.loads(INDEX.read_text()) if INDEX.exists() else {}
    entries = {}
    for name in paths():
        path = ROOT / name
        if not path.is_file() or path.is_symlink():
            continue
        stat = path.stat()
        stamp = [stat.st_mtime_ns, stat.st_size]
        old = previous.get(name)
        if old and old['stamp'] == stamp:
            entries[name] = old
            continue
        symbols = []
        if path.suffix in {'.java', '.kt', '.kts', '.py', '.js', '.ts', '.tsx',
                           '.jsx', '.c', '.cpp', '.cc', '.h', '.hpp', '.go', '.rs'}:
            for number, line in enumerate(path.read_text(errors='replace').splitlines(), 1):
                if line.lstrip().startswith(('//', '*', '#')):
                    continue
                matches = list(DECL.finditer(line))
                method = JAVA_METHOD.match(line)
                if method:
                    matches.append(method)
                symbols.extend([m.group(1), number] for m in matches)
        entries[name] = {'stamp': stamp, 'symbols': symbols}
    temporary = INDEX.with_suffix('.tmp')
    temporary.write_text(json.dumps(entries, ensure_ascii=False, separators=(',', ':')))
    temporary.replace(INDEX)
    print(f'Indexed {len(entries)} files, {sum(len(e["symbols"]) for e in entries.values())} declarations.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('query', nargs='?', help='case-insensitive name/path substring')
    parser.add_argument('--update', action='store_true', help='incrementally refresh first')
    parser.add_argument('--limit', type=int, default=40)
    args = parser.parse_args()
    if args.update or not INDEX.exists():
        update()
    if not args.query:
        return
    query = args.query.casefold()
    results = []
    for path, entry in json.loads(INDEX.read_text()).items():
        hits = [(name, line) for name, line in entry['symbols'] if query in name.casefold()]
        if not hits and query in path.casefold():
            hits = [('(file)', 1)]
        for name, line in hits:
            results.append((name.casefold() != query, path, line, name))
    for _, path, line, name in sorted(results)[:args.limit]:
        print(f'{path}:{line}: {name}')
    if len(results) > args.limit:
        print('Result limit reached; narrow query or use --limit.')
    if not results:
        print('No matches. Try --update or rg for usages and undeclared symbols.')


if __name__ == '__main__':
    main()
