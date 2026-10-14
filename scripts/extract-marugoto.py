#!/usr/bin/env python3
"""Extrait les 668 lignes du PDF Marugoto sans confondre les deux tableaux de chaque page."""

import argparse
import csv
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET


# Positions des quatre colonnes dans chacun des deux tableaux du document A4.
TABLES = ((34, 62, 159, 264, 285), (300, 332, 428, 534, 555))
NS = {'pdf': 'http://www.w3.org/1999/xhtml'}


def extract(pdf):
    """Rassemble les cellules par numéro et position verticale, y compris les lectures sur deux lignes."""
    xml = subprocess.check_output(['pdftotext', '-bbox', str(pdf), '-'])
    document = ET.fromstring(xml)
    rows = {}

    for page in document.findall('.//pdf:page', NS):
        words = page.findall('pdf:word', NS)
        for number_x, written_x, reading_x, lesson_x, end_x in TABLES:
            numbers = [word for word in words
                       if number_x <= float(word.get('xMin')) < written_x
                       and (word.text or '').isdigit()]
            for number in numbers:
                identifier = int(number.text)
                y = float(number.get('yMin'))
                cells = []
                for start, end in ((written_x, reading_x), (reading_x, lesson_x), (lesson_x, end_x)):
                    # Le numéro est centré : une cellule repliée occupe y-6 et y+6 environ.
                    cell = [word for word in words
                            if start <= float(word.get('xMin')) < end
                            and abs(float(word.get('yMin')) - y) < 8]
                    cell.sort(key=lambda word: (float(word.get('yMin')), float(word.get('xMin'))))
                    cells.append(''.join(word.text or '' for word in cell))
                written, reading, lesson = cells
                if not written or not reading:
                    raise ValueError(f'Cellule manquante pour la ligne {identifier}')
                if identifier in rows:
                    raise ValueError(f'Numéro dupliqué : {identifier}')
                rows[identifier] = [identifier, written, reading, int(lesson) if lesson else 18]

    if set(rows) != set(range(1, 669)):
        raise ValueError('Les numéros extraits ne correspondent pas exactement à 1..668')
    if any(not 1 <= row[3] <= 18 for row in rows.values()):
        raise ValueError('Numéro de leçon hors de 1..18')
    return [rows[number] for number in range(1, 669)]


def main():
    """Produit le CSV à cinq colonnes en préservant les traductions déjà renseignées lors d'une réextraction."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('pdf', type=Path)
    parser.add_argument('csv', type=Path)
    args = parser.parse_args()
    meanings = {}
    if args.csv.exists():
        with args.csv.open(encoding='utf-8', newline='') as source:
            meanings = {int(row[0]): row[4] for row in csv.reader(source, delimiter=';') if row}
    rows = extract(args.pdf)
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    with args.csv.open('w', encoding='utf-8', newline='') as target:
        writer = csv.writer(target, delimiter=';', lineterminator='\n')
        writer.writerows([*row, meanings.get(row[0], '')] for row in rows)
    print(f'{len(rows)} lignes extraites ; les cellules sans leçon sont affectées à 18.')


if __name__ == '__main__':
    main()
