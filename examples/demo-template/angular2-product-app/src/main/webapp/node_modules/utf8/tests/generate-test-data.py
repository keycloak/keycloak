#!/usr/bin/env python

import re
import json

# https://mathiasbynens.be/notes/javascript-encoding#surrogate-formulae
# http://stackoverflow.com/a/13436167/96656
def unisymbol(codePoint):
	if codePoint >= 0x0000 and codePoint <= 0xFFFF:
		return unichr(codePoint)
	elif codePoint >= 0x010000 and codePoint <= 0x10FFFF:
		highSurrogate = int((codePoint - 0x10000) / 0x400) + 0xD800
		lowSurrogate = int((codePoint - 0x10000) % 0x400) + 0xDC00
		return unichr(highSurrogate) + unichr(lowSurrogate)
	else:
		return 'Error'

def hexify(codePoint):
	return 'U+' + hex(codePoint)[2:].upper().zfill(6)

def writeFile(filename, contents):
	print filename
	with open(filename, 'w') as f:
		f.write(contents.strip() + '\n')

data = []
for codePoint in range(0x000000, 0x10FFFF + 1):
	# Skip non-scalar values.
	if codePoint >= 0xD800 and codePoint <= 0xDFFF:
		continue
	symbol = unisymbol(codePoint)
	# http://stackoverflow.com/a/17199950/96656
	bytes = symbol.encode('utf8').decode('latin1')
	data.append({
		'codePoint': codePoint,
		'decoded': symbol,
		'encoded': bytes
	});

jsonData = json.dumps(data, sort_keys=False, indent=2, separators=(',', ': '))
# Use tabs instead of double spaces for indentation
jsonData = jsonData.replace('  ', '\t')
# Escape hexadecimal digits in escape sequences
jsonData = re.sub(
	r'\\u([a-fA-F0-9]{4})',
	lambda match: r'\u{}'.format(match.group(1).upper()),
	jsonData
)

writeFile('data.json', jsonData)
