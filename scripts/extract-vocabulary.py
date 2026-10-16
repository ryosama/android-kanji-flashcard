#!/usr/bin/env python3
"""Extrait les trois PDF de vocabulaire Marugoto en CSV UTF-8, avec provenance contrôlable."""

import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import unicodedata
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'listes/francais'
NS = {'p': 'http://www.w3.org/1999/xhtml'}
JAPANESE = re.compile(r'[\u3040-\u30ff\u3400-\u9fff]')
KANJI = re.compile(r'[\u3400-\u9fff]')


def clean(text):
    """Harmonise Unicode et les espaces sans supprimer les accents ni la ponctuation utile."""
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFKC', text)).strip()


def joined(words, japanese=False):
    """Reconstitue une cellule par lignes, en conservant les espaces des traductions françaises."""
    lines = []
    for word in sorted(words, key=lambda w: (w['y'], w['x'])):
        line = next((line for line in reversed(lines) if abs(line[0]['y'] - word['y']) < 3), None)
        if line is None:
            lines.append([word])
        else:
            line.append(word)
    values = []
    for line in lines:
        line.sort(key=lambda w: w['x'])
        value = ''
        previous = None
        for word in line:
            gap = word['x'] - (previous['x'] + previous.get('width', 0)) if previous else 0
            separator = '' if japanese or not previous or gap < 1.5 else ' '
            value += separator + word['text']
            previous = word
        values.append(value)
    return clean(('' if japanese else ' ').join(values))


def meanings(text):
    """Sépare les sens aux barres obliques hors parenthèses, sans tronquer les précisions."""
    parts, current, depth = [], [], 0
    for character in clean(text):
        if character == '(':
            depth += 1
        elif character == ')':
            depth = max(0, depth - 1)
        if character == '/' and depth == 0:
            if clean(''.join(current)):
                parts.append(clean(''.join(current)))
            current = []
        else:
            current.append(character)
    if clean(''.join(current)):
        parts.append(clean(''.join(current)))
    return list(dict.fromkeys(parts))


def entry(book, topic, written, kana, french, source, roman=''):
    """Construit une entrée complète ; chaque problème d'extraction reste localisable dans le PDF."""
    written = clean(re.sub(r'【[^】]*】', '', written)).strip()
    kana = clean(re.sub(r'[￢￣―]', '', kana)).replace(' ', '')
    kana = re.sub(r'【[^】]*】', '', kana).strip()
    kana = re.sub(r'\(\s*\)', '', kana)
    written = re.sub(r'\(\s*\)', '', written)
    if KANJI.search(kana):
        phonetic = re.findall(r'\(([\u3040-\u30ff]+)\)', kana)
        if phonetic:
            kana = '/'.join(phonetic)
            written = re.sub(r'\([\u3040-\u30ff]+\)', '', written)
    if not JAPANESE.search(kana):
        kana = written
    assert written and (JAPANESE.search(written) or JAPANESE.search(kana)) and french and 1 <= topic <= 9, (source, written, french, topic)
    return dict(book=book, topic=topic, written=written, kana=kana, roman=clean(roman),
                meanings=meanings(french), sources=[source])


def index_entries(path, book):
    """Lit les tableaux natifs : numéro de leçon reporté, cellules françaises sur plusieurs lignes."""
    xml = subprocess.check_output(['pdftotext', '-bbox-layout', str(path), '-'])
    document = ET.fromstring(xml)
    current_lesson = None
    result = []
    for page_number, page in enumerate(document.findall('.//p:page', NS), 1):
        words = [dict(x=float(w.get('xMin')), y=float(w.get('yMin')), width=float(w.get('xMax')) - float(w.get('xMin')), text=w.text or '')
                 for w in page.findall('.//p:word', NS)]
        header = next(w for w in words if w['text'] == 'ことば' and 100 < w['x'] < 120)
        body = [w for w in words if header['y'] + 7 < w['y'] < 810]
        left = 54.6 if book == 1 else 50.16
        anchors = sorted([w for w in body if abs(w['x'] - left) < 1 and JAPANESE.search(w['text'])], key=lambda w: w['y'])
        lessons = sorted([w for w in body if 33 < w['x'] < left - 5 and w['text'].isdigit()], key=lambda w: w['y'])
        # Les cellules sont centrées verticalement : les milieux entre deux ancres délimitent une ligne.
        for i, anchor in enumerate(anchors):
            previous_y = anchors[i - 1]['y'] if i else header['y'] + 7
            next_y = anchors[i + 1]['y'] if i + 1 < len(anchors) else 817
            low, high = (previous_y + anchor['y']) / 2, (anchor['y'] + next_y) / 2
            for lesson in lessons:
                if lesson['y'] <= anchor['y'] + 2:
                    current_lesson = int(lesson['text'])
            assert current_lesson is not None, (path, page_number)
            cells = [w for w in body if low <= w['y'] < high]
            if book == 1:
                raw = joined([w for w in cells if left - 1 <= w['x'] < 183], True)
                accent = joined([w for w in cells if 183 <= w['x'] < 282], True)
                roman = joined([w for w in cells if 282 <= w['x'] < 360])
                french = joined([w for w in cells if 360 <= w['x'] < 544])
                # Une graphie kanji éventuelle est séparée du kana par « ／ » dans cet index.
                kanas = [s for s in re.split('[／/]', raw) if not KANJI.search(s)]
                kana = '/'.join(kanas) if kanas else accent
                written = '/'.join(s for s in re.split('[／/]', raw) if KANJI.search(s)) or raw
            else:
                raw = joined([w for w in cells if left - 1 <= w['x'] < 180], True)
                written = joined([w for w in cells if 180 <= w['x'] < 250], True) or raw
                kana = raw
                roman = ''
                french = joined([w for w in cells if 346 <= w['x'] < 536])
            result.append(entry(book, (current_lesson + 1) // 2, written, kana, french,
                                f'{path.name}:p{page_number}:ligne{i + 1}', roman))
    return result


def wordbook_entries(path):
    """Utilise les polices et coordonnées du lexique illustré, limité aux pages PDF 6 à 81."""
    with tempfile.TemporaryDirectory() as temp:
        destination = Path(temp) / 'wordbook.xml'
        subprocess.run(['pdftohtml', '-f', '6', '-l', '81', '-xml', '-hidden', '-i', str(path), str(destination)],
                       check=True, stdout=subprocess.DEVNULL)
        document = ET.parse(destination)
    fonts = {f.get('id'): f.attrib for f in document.findall('.//fontspec')}
    result = []
    errors = []
    for page in document.findall('page'):
        texts = [dict(x=int(t.get('left')), y=int(t.get('top')), width=int(t.get('width')),
                      text=unicodedata.normalize('NFKC', ''.join(t.itertext())), font=fonts[t.get('font')]) for t in page.findall('text')]
        if not texts:
            continue  # Pages images transcrites et vérifiées dans le complément contrôlé.
        headers = [t['text'] for t in texts if t['font']['color'] == '#ffffff' and t['y'] < 120
                   and re.fullmatch(r'[1-9](?:[ /]+[1-9])*', t['text'])]
        topic = min(map(int, re.findall('[1-9]', headers[0]))) if headers else 9
        anchors = [t for t in texts if 'DeBold' in t['font']['family'] and 11 <= int(t['font']['size']) <= 24
                   and t['font']['color'] not in ('#444041', '#ffffff') and t['y'] > 125 and JAPANESE.search(t['text'])]
        # Certains mots figurent à deux lignes : on les rattache à leur ancre supérieure.
        groups = []
        for anchor in sorted(anchors, key=lambda t: (t['y'], t['x'])):
            prior = next((g for g in reversed(groups) if abs(g['x'] - anchor['x']) < 8 and 0 < anchor['y'] - g['y'] < 27), None)
            if prior:
                prior['text'] += anchor['text']
            else:
                groups.append(dict(anchor))
        assigned = {i: [] for i in range(len(groups))}
        for text in texts:
            candidates = [(abs(text['x'] - a['x']) * 2 + text['y'] - a['y'], i)
                          for i, a in enumerate(groups)
                          if -5 <= text['y'] - a['y'] <= 160 and -35 <= text['x'] - a['x'] < 280]
            if candidates:
                assigned[min(candidates)[1]].append(text)
        for i, anchor in enumerate(groups):
            cell = assigned[i]
            latin = [t for t in cell if 'Arial' in t['font']['family'] and t['font']['color'] not in ('#444041', '#ffffff', '#5b5f60', '#828589')
                     and int(t['font']['size']) <= 14 and re.search('[a-zA-Z]', t['text'])]
            if not latin:
                errors.append((page.get('number'), anchor['text'], 'roman'))
                continue
            start = min(t['y'] for t in latin)
            french = [t for t in cell if t['y'] >= start - 2 and t['font']['color'] in ('#444041', '#231f1f')
                      and 'Arial' in t['font']['family'] and int(t['font']['size']) <= 12]
            if not joined(french):
                errors.append((page.get('number'), anchor['text'], 'french'))
                continue
            result.append(entry(1, topic, anchor['text'], anchor['text'],
                                joined(french), f"{path.name}:p{page.get('number')}:entrée{i + 1}", joined(latin)))
    supplements = supplemental_entries()
    controlled = {(int(e['sources'][0].split(':p')[1].split(':')[0]), key_japanese(value)) for e in supplements for value in (e['kana'], e['written'])}
    uncovered = [(p, word, issue) for p, word, issue in errors if (int(p), key_japanese(word)) not in controlled]
    assert not uncovered, ('Entrées nécessitant une transcription contrôlée', uncovered)
    # Les légendes isolées peuvent empiéter sur une cellule voisine : la transcription prime.
    overrides = {(int(e['sources'][0].split(':p')[1].split(':')[0]), key_japanese(e['kana'])): e for e in supplements}
    for item in result:
        page = int(item['sources'][0].split(':p')[1].split(':')[0])
        checked = overrides.get((page, key_japanese(item['kana'])))
        if checked:
            item['meanings'] = checked['meanings'][:]
    return result


def supplemental_entries():
    """Lit les transcriptions vérifiées des pages images, calendriers, compteurs et légendes isolées."""
    result = []
    path = SOURCES / 'Marugoto vocabulaire complements.tsv'
    for line_number, line in enumerate(path.read_text().splitlines(), 1):
        if not line or line.startswith('#'):
            continue
        page, topic, written, kana, french = line.split('\t')
        card = entry(1, int(topic), written, kana, french.replace('|', ' / '),
                     f'MarugotoStarterWordbook_FR.pdf:p{page}:complément{line_number}')
        result.append(card)
    return result


def key_japanese(value):
    """Compare les graphies kana en ignorant espaces, préfixes décoratifs et variantes entre parenthèses."""
    value = re.sub(r'\([^)]*\)', '', clean(value).split('/')[0])
    return ''.join(chr(ord(c) - 0x60) if '\u30a1' <= c <= '\u30f6' else c for c in value if JAPANESE.search(c))


def key_meaning(value):
    """Compare les traductions identiques malgré les espaces, articles et apostrophes typographiques."""
    value = clean(value).lower().replace('’', "'")
    value = re.sub(r'^(?:le |la |les |un |une |des |l\')', '', value)
    return ''.join(c for c in value if c.isalnum())


def merge(entries):
    """Fusionne les doublons mot/sens et conserve leur première apparition, les graphies et la provenance."""
    groups = {}
    for item in sorted(entries, key=lambda x: (x['book'], x['topic'], int(x['sources'][0].split(':p')[1].split(':')[0]))):
        word = key_japanese(item['kana'])
        senses = {key_meaning(value) for value in item['meanings']}
        candidates = groups.setdefault(word, [])
        matches = [g for g in candidates if senses.intersection(key_meaning(v) for v in g['meanings'])]
        if not matches:
            candidates.append(item)
            continue
        target = matches[0]
        # Un sens commun permet de réunir aussi les traductions supplémentaires d'un doublon.
        for other in matches[1:] + [item]:
            known = {key_meaning(value) for value in target['meanings']}
            for value in other['meanings']:
                if key_meaning(value) not in known:
                    target['meanings'].append(value)
                    known.add(key_meaning(value))
            target['sources'] += other['sources']
            if KANJI.search(other['written']) and not KANJI.search(target['written']):
                target['written'] = other['written']
        for other in matches[1:]:
            candidates.remove(other)
    cards = [card for group in groups.values() for card in group]
    cards.sort(key=lambda x: (x['book'], x['topic']))
    for card in cards:
        key = (key_japanese(card['kana']), tuple(sorted(key_meaning(s) for s in card['meanings'])))
        card['id'] = hashlib.sha256(json.dumps(key, ensure_ascii=False).encode()).hexdigest()[:20]
    return cards


def main():
    """Génère le CSV final et un relevé complet des entrées sources pour vérifier l'extraction."""
    entries = index_entries(SOURCES / 'MarugotoStarterCompetencesVocabularyIndex2_FR.pdf', 1)
    entries += index_entries(SOURCES / 'MarugotoElementary1CompetencesVocabularyIndex2_FR.pdf', 2)
    entries += wordbook_entries(SOURCES / 'MarugotoStarterWordbook_FR.pdf')
    entries += supplemental_entries()
    audit_entries = json.loads(json.dumps(entries))
    cards = merge(entries)
    output = SOURCES / 'Marugoto vocabulaire.csv'
    with output.open('w', encoding='utf-8', newline='') as target:
        writer = csv.writer(target, delimiter=';', lineterminator='\n')
        for card in cards:
            writer.writerow([card['id'], card['book'], card['topic'], card['written'], card['kana'], card['roman'],
                             ' | '.join(card['meanings']), ' | '.join(dict.fromkeys(card['sources']))])
    audit = ROOT / '.tools/vocabulary/extraction.json'
    audit.parent.mkdir(parents=True, exist_ok=True)
    audit.write_text(json.dumps(audit_entries, ensure_ascii=False, indent=2) + '\n')
    print(f'{len(entries)} entrées sources -> {len(cards)} cartes ; CSV : {output.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
