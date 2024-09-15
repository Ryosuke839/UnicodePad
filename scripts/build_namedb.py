#!/usr/bin/env python3

import sys
import sqlite3
import re
from ftplib import FTP
from urllib.parse import urlparse

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
]

def main():
  with FTP('www.unicode.org') as ftp:
    print(ftp.getwelcome())
    print(ftp.login())
    with sqlite3.connect('namedb') as con:
      cur = con.cursor()
      cur.execute('CREATE TABLE name_table (id integer NOT NULL PRIMARY KEY, words text NOT NULL, name text NOT NULL, version integer NOT NULL, comment text, alias text, formal text, xref text, vari text, decomp text, compat text);')
      characters = {}
      class OneCharacter:
        id = None
        name = None
        version = 0
        comment = []
        alias = []
        formal = []
        xref = []
        vari = []
        decomp = []
        compat = []
        def __init__(self, code, name, version):
          self.id = int(code, 16)
          self.name = name
          self.version = version
          self.comment = []
          self.alias = []
          self.formal = []
          self.xref = []
          self.vari = []
          self.decomp = []
          self.compat = []
        def update(self):
          nonlocal characters
          if self.id in characters:
            self.version = characters[self.id].version
          characters[self.id] = self
        def insert(self):
          nonlocal cur
          def list_to_str(l):
            return '\'' + '\n'.join(l) + '\'' if len(l) > 0 else 'NULL'
          exp = f'INSERT INTO name_table (id, words, name, version, comment, alias, formal, xref, vari, decomp, compat) values ({self.id}, \'{" ".join([self.name] + self.alias + self.formal)}\', \'{self.name}\', {self.version}, {list_to_str(self.comment)}, {list_to_str(self.alias)}, {list_to_str(self.formal)}, {list_to_str(self.xref)}, {list_to_str(self.vari)}, {list_to_str(self.decomp)}, {list_to_str(self.compat)});'
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
          elif line[0] in ['@', ';']:
            return
          elif line[0] == '\t':
            if current is None:
              return
            if len(line) < 3:
              print(f'Malformed line: {line}', file=sys.stderr)
              return
            if line[1] == '\t':
              return
            if line[1] == '*':
              current.comment.append(line[3:].replace('\'', '\'\''))
            if line[1] == '=':
              current.alias.append(line[3:].replace('\'', '\'\''))
            if line[1] == '%':
              current.formal.append(line[3:].replace('\'', '\'\''))
            if line[1] == 'x':
              current.xref.append((line.split(' ')[-1][:-1] if line[3] == '(' else line[3:]).lstrip('0').replace('\'', '\'\''))
            if line[1] == '~':
              current.vari.append(' '.join(s.lstrip('0') for s in line[3:].split(' ')).replace('\'', '\'\''))
            if line[1] == ':':
              current.decomp.append(' '.join(s.lstrip('0') for s in line[3:].split(' ') if re.match('^[0-9A-F]+$', s)).replace('\'', '\'\''))
            if line[1] == '#':
              current.compat.append(' '.join(s.lstrip('0') for s in line[3:].split(' ') if re.match('^([0-9A-F]+|<.*>)$', s)).replace('\'', '\'\''))
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
      print(f'RETR /Public/emoji/{UNICODE_VERSIONS[-1] // 100}.{UNICODE_VERSIONS[-1] // 10 % 10}/')
      print(ftp.cwd(f'/Public/emoji/{UNICODE_VERSIONS[-1] // 100}.{UNICODE_VERSIONS[-1] // 10 % 10}/'))
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
      cur.execute('CREATE TABLE version_code as SELECT 59 as version;')
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