#!/usr/bin/env python3

import sys
import sqlite3
import re
from ftplib import FTP
import io
from urllib.parse import urlparse
import zipfile

UNICODE_VERSIONS = [
  600,
  610,
  620,
  630,
  700,
  800,
  900,
  1000,
  1100,
  1200,
  1210,
  1300,
  1400,
  1500,
  1510,
  1600,
  1700,
]

def main():
  with FTP('ftp.unicode.org') as ftp:
    print(ftp.getwelcome())
    print(ftp.login())
    with sqlite3.connect('namedb') as con:
      cur = con.cursor()

      print(f'RETR /Public/{UNICODE_VERSIONS[-1] // 100}.{UNICODE_VERSIONS[-1] // 10 % 10}.{UNICODE_VERSIONS[-1] % 10}/ucd/')
      print(ftp.cwd(f'/Public/{UNICODE_VERSIONS[-1] // 100}.{UNICODE_VERSIONS[-1] // 10 % 10}.{UNICODE_VERSIONS[-1] % 10}/ucd/'))
      cur.execute('CREATE TABLE unihan_table (id integer NOT NULL PRIMARY KEY, kRSUnicode text, kTotalStrokes text, kAlternateTotalStrokes text, kCantonese text, kDefinition text, kFanqie text, kHangul text, kHanyuPinlu text, kHanyuPinyin text, kJapanese text, kJapaneseKun text, kJapaneseOn text, kKorean text, kMandarin text, kSMSZD2003Readings text, kTang text, kTGHZ2013 text, kVietnamese text, kXHC1983 text, kZhuang text, kSemanticVariant text, kSimplifiedVariant text, kSpecializedSemanticVariant text, kSpoofingVariant text, kTraditionalVariant text, kZVariant text);')
      cur.execute('CREATE TABLE rsindex_table (id integer NOT NULL PRIMARY KEY, radical integer NOT NULL, strokes integer NOT NULL, codepoint integer NOT NULL);')
      with io.BytesIO() as b:
        ftp.retrbinary('RETR Unihan.zip', b.write)
        with zipfile.ZipFile(b) as z:
          blocks = [
            (0x4E00, 0), # CJK Unified Ideographs
            (0x3400, 1), # CJK Unified Ideographs Extension A
            (0x20000, 2), # CJK Unified Ideographs Extension B
            (0x2A700, 3), # CJK Unified Ideographs Extension C
            (0x2B740, 4), # CJK Unified Ideographs Extension D
            (0x2B820, 5), # CJK Unified Ideographs Extension E
            (0x2CEB0, 6), # CJK Unified Ideographs Extension F
            (0x30000, 7), # CJK Unified Ideographs Extension G
            (0x31350, 8), # CJK Unified Ideographs Extension H
            (0x2EBF0, 9), # CJK Unified Ideographs Extension I
            (0x323B0, 10), # CJK Unified Ideographs Extension J
            (0xF900, 254), # CJK Compatibility Ideographs
            (0x2F800, 255), # CJK Compatibility Ideographs Supplement
          ]
          last_codepoint: int | None = None
          properties: dict[str, str] = {}
          def set_or_insert(codepoint, prop, value):
            nonlocal last_codepoint
            nonlocal properties
            if last_codepoint is not None and codepoint != last_codepoint:
              if len(properties) > 0:
                values = ("'" + v.replace("'", "''") + "'" for v in properties.values())
                set_expr = (f"{k} = excluded.{k}" for k in properties.keys())
                exp = f'INSERT INTO unihan_table (id, {", ".join(properties.keys())}) values ({last_codepoint}, {", ".join(values)}) on conflict(id) do update set {", ".join(set_expr)};'
                try:
                  cur.execute(exp)
                except:
                  print(exp)
                  raise
              properties = {}
            if codepoint is not None:
              properties[prop] = value
              last_codepoint = codepoint
          def process_file(filename, columns):
            with z.open(filename) as f:
              for line in f:
                line = line.decode('utf-8').rstrip('\n')
                if len(line) == 0 or line[0] == '#':
                  continue
                tokens = line.split('\t')
                if len(tokens) != 3:
                  print(f'Malformed line: {line}', file=sys.stderr)
                  continue
                codepoint = int(tokens[0][2:], 16)
                if tokens[1] in columns:
                  set_or_insert(codepoint, tokens[1], tokens[2])
                if tokens[1] == 'kRSUnicode':
                  for token in tokens[2].split(' '):
                    m = re.match(r'^([1-9]\d{0,2})(\'{0,3})\.(-?\d{1,2})$', token)
                    if not m:
                      print(f'Malformed kRSUnicode value: {token}', file=sys.stderr)
                      continue
                    key = codepoint | [r for b, r in blocks if codepoint >= b][0] << 20 | len(m.group(2)) << 28 | max(int(m.group(3)), 0) << 36 | int(m.group(1)) << 44
                    exp = f'INSERT INTO rsindex_table (id, radical, strokes, codepoint) values ({key}, {m.group(1)}, {max(int(m.group(3)), 0)}, {codepoint});'
                    try:
                      cur.execute(exp)
                    except:
                      print(exp)
                      raise
              else:
                set_or_insert(None, None, None)
          process_file('Unihan_IRGSources.txt', ['kRSUnicode', 'kTotalStrokes'])
          process_file('Unihan_DictionaryLikeData.txt', ['kAlternateTotalStrokes'])
          process_file('Unihan_Readings.txt', ['kCantonese', 'kDefinition', 'kFanqie', 'kHangul', 'kHanyuPinlu', 'kHanyuPinyin', 'kJapanese', 'kJapaneseKun', 'kJapaneseOn', 'kKorean', 'kMandarin', 'kSMSZD2003Readings', 'kTang', 'kTGHZ2013', 'kVietnamese', 'kXHC1983', 'kZhuang'])
          process_file('Unihan_Variants.txt', ['kSemanticVariant', 'kSimplifiedVariant', 'kSpecializedSemanticVariant', 'kSpoofingVariant', 'kTraditionalVariant', 'kZVariant'])
      con.commit()

      cur.execute('CREATE TABLE name_table (id integer NOT NULL PRIMARY KEY, words text NOT NULL, name text NOT NULL, version integer NOT NULL, lines text);')
      characters = {}
      class OneCharacter:
        id = None
        name = None
        version = 0
        words = []
        lines = []
        def __init__(self, code, name, version):
          self.id = int(code, 16)
          self.name = name
          self.version = version
          self.words = [name]
          self.lines = []
        def update(self):
          nonlocal characters
          if self.id in characters:
            self.version = characters[self.id].version
          characters[self.id] = self
        def append_line(self, line_type, line, is_word=False):
          if is_word:
            self.words.append(line.replace('\'', '\'\''))
          self.lines.append(line_type + ' ' + line.replace('\'', '\'\''))
        def insert(self):
          nonlocal cur
          def list_to_str(l):
            return '\'' + '\n'.join(l) + '\'' if len(l) > 0 else 'NULL'
          exp = f'INSERT INTO name_table (id, words, name, version, lines) values ({self.id}, \'{" ".join(self.words)}\', \'{self.name}\', {self.version}, {list_to_str(self.lines)});'
          try:
            cur.execute(exp)
          except:
            print(exp)
            raise
      for version in UNICODE_VERSIONS:
        ftp.encoding = 'cp1252' if version < 620 else 'utf-8'
        print(ftp.cwd(f'/Public/{version // 100}.{version // 10 % 10}.{version % 10}/ucd/'))
        current = None
        def oneline(line):
          nonlocal current
          if len(line) == 0:
            return
          elif line[0] in ['@', ';'] and not line.startswith('@+'):
            return
          elif line[0] == '\t' or line.startswith('@+'):
            if current is None:
              return
            if len(line) < 3:
              print(f'Malformed line: {line}', file=sys.stderr)
              return
            if line[1] == '\t':
              return
            if line[1] == '*':
              current.append_line('*', line[3:])
            if line.startswith('@+'):
              current.append_line('@', line[3:].lstrip('\t').lstrip('* '))
            if line[1] == '=':
              current.append_line('=', line[3:], True)
            if line[1] == '%':
              current.append_line('%', line[3:], True)
            if line[1] == 'x':
              current.append_line('x', (line.split(' - ')[0][4:] + ' ' + line.split(' ')[-1][:-1] if line[3] == '(' else line[3:]).lstrip('0'))
            if line[1] == '~':
              current.append_line('~', line[3:])
            if line[1] == ':':
              current.append_line(':', line[3:])
            if line[1] == '#':
              current.append_line('#', line[3:])
          else:
            if current is not None:
              current.update()
            tokens = line.split('\t')
            if len(tokens) != 2:
              print(f'Malformed line: {line}', file=sys.stderr)
              return
            if tokens[1] == '<not a character>':
              current = None
              return
            current = OneCharacter(tokens[0], tokens[1], version)
        print(f'RETR /Public/{version // 100}.{version // 10 % 10}.{version % 10}/ucd/NamesList.txt')
        print(ftp.retrlines(f'RETR NamesList.txt', oneline))
        if current is not None:
          current.update()
      for v in characters.values():
        v.insert()
      print(f'RETR /Public/{UNICODE_VERSIONS[-1] // 100}.{UNICODE_VERSIONS[-1] // 10 % 10}.{version % 10}/emoji/')
      print(ftp.cwd(f'/Public/{UNICODE_VERSIONS[-1] // 100}.{UNICODE_VERSIONS[-1] // 10 % 10}.{version % 10}/emoji/'))
      group = ''
      subgroup = ''
      def emoji_line(line):
        nonlocal group
        nonlocal subgroup
        if len(line) == 0:
          return
        if line[0] == '#':
          if line.startswith('# group: '):
            group = line[9:]
          if line.startswith('# subgroup: '):
            subgroup = line[12:]
          return
        m = re.match(r'^((?:[0-9A-F]+ )+) *; ([^ ]+) *# [^ ]+ E([0-9]+)\.([0-9]) (.*)$', line)
        if not m:
          print(f'Malformed line: {line}', file=sys.stderr)
          return
        if m.group(2) != 'fully-qualified':
          return
        id = m.group(1).rstrip(' ')
        m_tone = re.match(r'^(.*?) (1F3F[B-F])(| .*)$', id)
        m_direction = re.match(r'(.*?) 200D (27A1)(?: FE0F)?$', id)
        exp = 'INSERT INTO emoji_table1510 (id, name, version, grp, subgrp, tone, direction) values (\'{}\', \'{}\', {}, \'{}\', \'{}\', {}, {});'.format(
          id, m.group(5), int(m.group(3) + m.group(4) + '0'), group, subgroup, str(int(m_tone.group(2), 16)) if m_tone else '0', str(int(m_direction.group(2), 16)) if m_direction else '0')
        try:
          cur.execute(exp)
        except:
          print(exp)
          raise
        if m_tone:
          exp = 'UPDATE emoji_table1510 SET tone = 11034 WHERE id = \'{0}\' or id = \'{0} FE0F\';'.format(m_tone.group(1) + re.sub(r' (1F3F[B-F])', '', m_tone.group(3)))
          try:
            cur.execute(exp)
          except:
            print(exp)
            raise
        if m_direction:
          exp = 'UPDATE emoji_table1510 SET direction = 11013 WHERE id = \'{0}\' or id = \'{0} FE0F\';'.format(m_direction.group(1))
          try:
            cur.execute(exp)
          except:
            print(exp)
            raise
      cur.execute('CREATE TABLE emoji_table1510 (id text NOT NULL PRIMARY KEY, name text NOT NULL, version integer NOT NULL, grp text NOT NULL, subgrp text NOT NULL, tone integer NOT NULL, direction integer NOT NULL);')
      print(ftp.retrlines(f'RETR emoji-test.txt', emoji_line))
      con.commit()

      cur.execute('CREATE TABLE version_code as SELECT 72 as version;')
      con.commit()

      print('SELECT * FROM \'version_code\';')
      cur.execute('SELECT * FROM \'version_code\';')
      r = cur.fetchone()
      print(r)

if __name__ == '__main__':
  main()

#SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='name_table' OR name='emoji_table1510'
#(2,)
#SELECT COUNT(*) FROM 'name_table';
#(34936,)
#SELECT COUNT(*) FROM 'emoji_table1510';
#(3773,)
#1F469 200D 2764 FE0F 200D 1F48B 200D 1F469
#1F469 1F3FB 200D 2764 FE0F 200D 1F48B 200D 1F469 1F3FB