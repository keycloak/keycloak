/*
  @license
	Rollup.js v1.32.1
	Fri, 06 Mar 2020 09:32:39 GMT - commit f458cbf6cb8cfcc1678593d8dc595e4b8757eb6d


	https://github.com/rollup/rollup

	Released under the MIT License.
*/
import util from 'util';
import path, { relative as relative$1, extname, basename, dirname, resolve, sep } from 'path';
import { readFile as readFile$1, writeFile as writeFile$1, readdirSync, mkdirSync, lstatSync, realpathSync, statSync, watch as watch$1 } from 'fs';
import * as acorn__default from 'acorn';
import { Parser } from 'acorn';
import { createHash as createHash$2 } from 'crypto';
import { EventEmitter } from 'events';
import module from 'module';

/*! *****************************************************************************
Copyright (c) Microsoft Corporation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache Version 2.0 License for specific language governing permissions
and limitations under the License.
***************************************************************************** */
function __awaiter(thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try {
            step(generator.next(value));
        }
        catch (e) {
            reject(e);
        } }
        function rejected(value) { try {
            step(generator["throw"](value));
        }
        catch (e) {
            reject(e);
        } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
}

var version = "1.32.1";

var charToInteger = {};
var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
for (var i = 0; i < chars.length; i++) {
    charToInteger[chars.charCodeAt(i)] = i;
}
function decode(mappings) {
    var decoded = [];
    var line = [];
    var segment = [
        0,
        0,
        0,
        0,
        0,
    ];
    var j = 0;
    for (var i = 0, shift = 0, value = 0; i < mappings.length; i++) {
        var c = mappings.charCodeAt(i);
        if (c === 44) { // ","
            segmentify(line, segment, j);
            j = 0;
        }
        else if (c === 59) { // ";"
            segmentify(line, segment, j);
            j = 0;
            decoded.push(line);
            line = [];
            segment[0] = 0;
        }
        else {
            var integer = charToInteger[c];
            if (integer === undefined) {
                throw new Error('Invalid character (' + String.fromCharCode(c) + ')');
            }
            var hasContinuationBit = integer & 32;
            integer &= 31;
            value += integer << shift;
            if (hasContinuationBit) {
                shift += 5;
            }
            else {
                var shouldNegate = value & 1;
                value >>>= 1;
                if (shouldNegate) {
                    value = value === 0 ? -0x80000000 : -value;
                }
                segment[j] += value;
                j++;
                value = shift = 0; // reset
            }
        }
    }
    segmentify(line, segment, j);
    decoded.push(line);
    return decoded;
}
function segmentify(line, segment, j) {
    // This looks ugly, but we're creating specialized arrays with a specific
    // length. This is much faster than creating a new array (which v8 expands to
    // a capacity of 17 after pushing the first item), or slicing out a subarray
    // (which is slow). Length 4 is assumed to be the most frequent, followed by
    // length 5 (since not everything will have an associated name), followed by
    // length 1 (it's probably rare for a source substring to not have an
    // associated segment data).
    if (j === 4)
        line.push([segment[0], segment[1], segment[2], segment[3]]);
    else if (j === 5)
        line.push([segment[0], segment[1], segment[2], segment[3], segment[4]]);
    else if (j === 1)
        line.push([segment[0]]);
}
function encode(decoded) {
    var sourceFileIndex = 0; // second field
    var sourceCodeLine = 0; // third field
    var sourceCodeColumn = 0; // fourth field
    var nameIndex = 0; // fifth field
    var mappings = '';
    for (var i = 0; i < decoded.length; i++) {
        var line = decoded[i];
        if (i > 0)
            mappings += ';';
        if (line.length === 0)
            continue;
        var generatedCodeColumn = 0; // first field
        var lineMappings = [];
        for (var _i = 0, line_1 = line; _i < line_1.length; _i++) {
            var segment = line_1[_i];
            var segmentMappings = encodeInteger(segment[0] - generatedCodeColumn);
            generatedCodeColumn = segment[0];
            if (segment.length > 1) {
                segmentMappings +=
                    encodeInteger(segment[1] - sourceFileIndex) +
                        encodeInteger(segment[2] - sourceCodeLine) +
                        encodeInteger(segment[3] - sourceCodeColumn);
                sourceFileIndex = segment[1];
                sourceCodeLine = segment[2];
                sourceCodeColumn = segment[3];
            }
            if (segment.length === 5) {
                segmentMappings += encodeInteger(segment[4] - nameIndex);
                nameIndex = segment[4];
            }
            lineMappings.push(segmentMappings);
        }
        mappings += lineMappings.join(',');
    }
    return mappings;
}
function encodeInteger(num) {
    var result = '';
    num = num < 0 ? (-num << 1) | 1 : num << 1;
    do {
        var clamped = num & 31;
        num >>>= 5;
        if (num > 0) {
            clamped |= 32;
        }
        result += chars[clamped];
    } while (num > 0);
    return result;
}

var BitSet = function BitSet(arg) {
    this.bits = arg instanceof BitSet ? arg.bits.slice() : [];
};
BitSet.prototype.add = function add(n) {
    this.bits[n >> 5] |= 1 << (n & 31);
};
BitSet.prototype.has = function has(n) {
    return !!(this.bits[n >> 5] & (1 << (n & 31)));
};
var Chunk = function Chunk(start, end, content) {
    this.start = start;
    this.end = end;
    this.original = content;
    this.intro = '';
    this.outro = '';
    this.content = content;
    this.storeName = false;
    this.edited = false;
    // we make these non-enumerable, for sanity while debugging
    Object.defineProperties(this, {
        previous: { writable: true, value: null },
        next: { writable: true, value: null }
    });
};
Chunk.prototype.appendLeft = function appendLeft(content) {
    this.outro += content;
};
Chunk.prototype.appendRight = function appendRight(content) {
    this.intro = this.intro + content;
};
Chunk.prototype.clone = function clone() {
    var chunk = new Chunk(this.start, this.end, this.original);
    chunk.intro = this.intro;
    chunk.outro = this.outro;
    chunk.content = this.content;
    chunk.storeName = this.storeName;
    chunk.edited = this.edited;
    return chunk;
};
Chunk.prototype.contains = function contains(index) {
    return this.start < index && index < this.end;
};
Chunk.prototype.eachNext = function eachNext(fn) {
    var chunk = this;
    while (chunk) {
        fn(chunk);
        chunk = chunk.next;
    }
};
Chunk.prototype.eachPrevious = function eachPrevious(fn) {
    var chunk = this;
    while (chunk) {
        fn(chunk);
        chunk = chunk.previous;
    }
};
Chunk.prototype.edit = function edit(content, storeName, contentOnly) {
    this.content = content;
    if (!contentOnly) {
        this.intro = '';
        this.outro = '';
    }
    this.storeName = storeName;
    this.edited = true;
    return this;
};
Chunk.prototype.prependLeft = function prependLeft(content) {
    this.outro = content + this.outro;
};
Chunk.prototype.prependRight = function prependRight(content) {
    this.intro = content + this.intro;
};
Chunk.prototype.split = function split(index) {
    var sliceIndex = index - this.start;
    var originalBefore = this.original.slice(0, sliceIndex);
    var originalAfter = this.original.slice(sliceIndex);
    this.original = originalBefore;
    var newChunk = new Chunk(index, this.end, originalAfter);
    newChunk.outro = this.outro;
    this.outro = '';
    this.end = index;
    if (this.edited) {
        // TODO is this block necessary?...
        newChunk.edit('', false);
        this.content = '';
    }
    else {
        this.content = originalBefore;
    }
    newChunk.next = this.next;
    if (newChunk.next) {
        newChunk.next.previous = newChunk;
    }
    newChunk.previous = this;
    this.next = newChunk;
    return newChunk;
};
Chunk.prototype.toString = function toString() {
    return this.intro + this.content + this.outro;
};
Chunk.prototype.trimEnd = function trimEnd(rx) {
    this.outro = this.outro.replace(rx, '');
    if (this.outro.length) {
        return true;
    }
    var trimmed = this.content.replace(rx, '');
    if (trimmed.length) {
        if (trimmed !== this.content) {
            this.split(this.start + trimmed.length).edit('', undefined, true);
        }
        return true;
    }
    else {
        this.edit('', undefined, true);
        this.intro = this.intro.replace(rx, '');
        if (this.intro.length) {
            return true;
        }
    }
};
Chunk.prototype.trimStart = function trimStart(rx) {
    this.intro = this.intro.replace(rx, '');
    if (this.intro.length) {
        return true;
    }
    var trimmed = this.content.replace(rx, '');
    if (trimmed.length) {
        if (trimmed !== this.content) {
            this.split(this.end - trimmed.length);
            this.edit('', undefined, true);
        }
        return true;
    }
    else {
        this.edit('', undefined, true);
        this.outro = this.outro.replace(rx, '');
        if (this.outro.length) {
            return true;
        }
    }
};
var btoa = function () {
    throw new Error('Unsupported environment: `window.btoa` or `Buffer` should be supported.');
};
if (typeof window !== 'undefined' && typeof window.btoa === 'function') {
    btoa = function (str) { return window.btoa(unescape(encodeURIComponent(str))); };
}
else if (typeof Buffer === 'function') {
    btoa = function (str) { return Buffer.from(str, 'utf-8').toString('base64'); };
}
var SourceMap = function SourceMap(properties) {
    this.version = 3;
    this.file = properties.file;
    this.sources = properties.sources;
    this.sourcesContent = properties.sourcesContent;
    this.names = properties.names;
    this.mappings = encode(properties.mappings);
};
SourceMap.prototype.toString = function toString() {
    return JSON.stringify(this);
};
SourceMap.prototype.toUrl = function toUrl() {
    return 'data:application/json;charset=utf-8;base64,' + btoa(this.toString());
};
function guessIndent(code) {
    var lines = code.split('\n');
    var tabbed = lines.filter(function (line) { return /^\t+/.test(line); });
    var spaced = lines.filter(function (line) { return /^ {2,}/.test(line); });
    if (tabbed.length === 0 && spaced.length === 0) {
        return null;
    }
    // More lines tabbed than spaced? Assume tabs, and
    // default to tabs in the case of a tie (or nothing
    // to go on)
    if (tabbed.length >= spaced.length) {
        return '\t';
    }
    // Otherwise, we need to guess the multiple
    var min = spaced.reduce(function (previous, current) {
        var numSpaces = /^ +/.exec(current)[0].length;
        return Math.min(numSpaces, previous);
    }, Infinity);
    return new Array(min + 1).join(' ');
}
function getRelativePath(from, to) {
    var fromParts = from.split(/[/\\]/);
    var toParts = to.split(/[/\\]/);
    fromParts.pop(); // get dirname
    while (fromParts[0] === toParts[0]) {
        fromParts.shift();
        toParts.shift();
    }
    if (fromParts.length) {
        var i = fromParts.length;
        while (i--) {
            fromParts[i] = '..';
        }
    }
    return fromParts.concat(toParts).join('/');
}
var toString = Object.prototype.toString;
function isObject(thing) {
    return toString.call(thing) === '[object Object]';
}
function getLocator(source) {
    var originalLines = source.split('\n');
    var lineOffsets = [];
    for (var i = 0, pos = 0; i < originalLines.length; i++) {
        lineOffsets.push(pos);
        pos += originalLines[i].length + 1;
    }
    return function locate(index) {
        var i = 0;
        var j = lineOffsets.length;
        while (i < j) {
            var m = (i + j) >> 1;
            if (index < lineOffsets[m]) {
                j = m;
            }
            else {
                i = m + 1;
            }
        }
        var line = i - 1;
        var column = index - lineOffsets[line];
        return { line: line, column: column };
    };
}
var Mappings = function Mappings(hires) {
    this.hires = hires;
    this.generatedCodeLine = 0;
    this.generatedCodeColumn = 0;
    this.raw = [];
    this.rawSegments = this.raw[this.generatedCodeLine] = [];
    this.pending = null;
};
Mappings.prototype.addEdit = function addEdit(sourceIndex, content, loc, nameIndex) {
    if (content.length) {
        var segment = [this.generatedCodeColumn, sourceIndex, loc.line, loc.column];
        if (nameIndex >= 0) {
            segment.push(nameIndex);
        }
        this.rawSegments.push(segment);
    }
    else if (this.pending) {
        this.rawSegments.push(this.pending);
    }
    this.advance(content);
    this.pending = null;
};
Mappings.prototype.addUneditedChunk = function addUneditedChunk(sourceIndex, chunk, original, loc, sourcemapLocations) {
    var originalCharIndex = chunk.start;
    var first = true;
    while (originalCharIndex < chunk.end) {
        if (this.hires || first || sourcemapLocations.has(originalCharIndex)) {
            this.rawSegments.push([this.generatedCodeColumn, sourceIndex, loc.line, loc.column]);
        }
        if (original[originalCharIndex] === '\n') {
            loc.line += 1;
            loc.column = 0;
            this.generatedCodeLine += 1;
            this.raw[this.generatedCodeLine] = this.rawSegments = [];
            this.generatedCodeColumn = 0;
            first = true;
        }
        else {
            loc.column += 1;
            this.generatedCodeColumn += 1;
            first = false;
        }
        originalCharIndex += 1;
    }
    this.pending = sourceIndex > 0
        ? [this.generatedCodeColumn, sourceIndex, loc.line, loc.column]
        : null;
};
Mappings.prototype.advance = function advance(str) {
    if (!str) {
        return;
    }
    var lines = str.split('\n');
    if (lines.length > 1) {
        for (var i = 0; i < lines.length - 1; i++) {
            this.generatedCodeLine++;
            this.raw[this.generatedCodeLine] = this.rawSegments = [];
        }
        this.generatedCodeColumn = 0;
    }
    this.generatedCodeColumn += lines[lines.length - 1].length;
};
var n = '\n';
var warned = {
    insertLeft: false,
    insertRight: false,
    storeName: false
};
var MagicString = function MagicString(string, options) {
    if (options === void 0)
        options = {};
    var chunk = new Chunk(0, string.length, string);
    Object.defineProperties(this, {
        original: { writable: true, value: string },
        outro: { writable: true, value: '' },
        intro: { writable: true, value: '' },
        firstChunk: { writable: true, value: chunk },
        lastChunk: { writable: true, value: chunk },
        lastSearchedChunk: { writable: true, value: chunk },
        byStart: { writable: true, value: {} },
        byEnd: { writable: true, value: {} },
        filename: { writable: true, value: options.filename },
        indentExclusionRanges: { writable: true, value: options.indentExclusionRanges },
        sourcemapLocations: { writable: true, value: new BitSet() },
        storedNames: { writable: true, value: {} },
        indentStr: { writable: true, value: guessIndent(string) }
    });
    this.byStart[0] = chunk;
    this.byEnd[string.length] = chunk;
};
MagicString.prototype.addSourcemapLocation = function addSourcemapLocation(char) {
    this.sourcemapLocations.add(char);
};
MagicString.prototype.append = function append(content) {
    if (typeof content !== 'string') {
        throw new TypeError('outro content must be a string');
    }
    this.outro += content;
    return this;
};
MagicString.prototype.appendLeft = function appendLeft(index, content) {
    if (typeof content !== 'string') {
        throw new TypeError('inserted content must be a string');
    }
    this._split(index);
    var chunk = this.byEnd[index];
    if (chunk) {
        chunk.appendLeft(content);
    }
    else {
        this.intro += content;
    }
    return this;
};
MagicString.prototype.appendRight = function appendRight(index, content) {
    if (typeof content !== 'string') {
        throw new TypeError('inserted content must be a string');
    }
    this._split(index);
    var chunk = this.byStart[index];
    if (chunk) {
        chunk.appendRight(content);
    }
    else {
        this.outro += content;
    }
    return this;
};
MagicString.prototype.clone = function clone() {
    var cloned = new MagicString(this.original, { filename: this.filename });
    var originalChunk = this.firstChunk;
    var clonedChunk = (cloned.firstChunk = cloned.lastSearchedChunk = originalChunk.clone());
    while (originalChunk) {
        cloned.byStart[clonedChunk.start] = clonedChunk;
        cloned.byEnd[clonedChunk.end] = clonedChunk;
        var nextOriginalChunk = originalChunk.next;
        var nextClonedChunk = nextOriginalChunk && nextOriginalChunk.clone();
        if (nextClonedChunk) {
            clonedChunk.next = nextClonedChunk;
            nextClonedChunk.previous = clonedChunk;
            clonedChunk = nextClonedChunk;
        }
        originalChunk = nextOriginalChunk;
    }
    cloned.lastChunk = clonedChunk;
    if (this.indentExclusionRanges) {
        cloned.indentExclusionRanges = this.indentExclusionRanges.slice();
    }
    cloned.sourcemapLocations = new BitSet(this.sourcemapLocations);
    cloned.intro = this.intro;
    cloned.outro = this.outro;
    return cloned;
};
MagicString.prototype.generateDecodedMap = function generateDecodedMap(options) {
    var this$1 = this;
    options = options || {};
    var sourceIndex = 0;
    var names = Object.keys(this.storedNames);
    var mappings = new Mappings(options.hires);
    var locate = getLocator(this.original);
    if (this.intro) {
        mappings.advance(this.intro);
    }
    this.firstChunk.eachNext(function (chunk) {
        var loc = locate(chunk.start);
        if (chunk.intro.length) {
            mappings.advance(chunk.intro);
        }
        if (chunk.edited) {
            mappings.addEdit(sourceIndex, chunk.content, loc, chunk.storeName ? names.indexOf(chunk.original) : -1);
        }
        else {
            mappings.addUneditedChunk(sourceIndex, chunk, this$1.original, loc, this$1.sourcemapLocations);
        }
        if (chunk.outro.length) {
            mappings.advance(chunk.outro);
        }
    });
    return {
        file: options.file ? options.file.split(/[/\\]/).pop() : null,
        sources: [options.source ? getRelativePath(options.file || '', options.source) : null],
        sourcesContent: options.includeContent ? [this.original] : [null],
        names: names,
        mappings: mappings.raw
    };
};
MagicString.prototype.generateMap = function generateMap(options) {
    return new SourceMap(this.generateDecodedMap(options));
};
MagicString.prototype.getIndentString = function getIndentString() {
    return this.indentStr === null ? '\t' : this.indentStr;
};
MagicString.prototype.indent = function indent(indentStr, options) {
    var pattern = /^[^\r\n]/gm;
    if (isObject(indentStr)) {
        options = indentStr;
        indentStr = undefined;
    }
    indentStr = indentStr !== undefined ? indentStr : this.indentStr || '\t';
    if (indentStr === '') {
        return this;
    } // noop
    options = options || {};
    // Process exclusion ranges
    var isExcluded = {};
    if (options.exclude) {
        var exclusions = typeof options.exclude[0] === 'number' ? [options.exclude] : options.exclude;
        exclusions.forEach(function (exclusion) {
            for (var i = exclusion[0]; i < exclusion[1]; i += 1) {
                isExcluded[i] = true;
            }
        });
    }
    var shouldIndentNextCharacter = options.indentStart !== false;
    var replacer = function (match) {
        if (shouldIndentNextCharacter) {
            return ("" + indentStr + match);
        }
        shouldIndentNextCharacter = true;
        return match;
    };
    this.intro = this.intro.replace(pattern, replacer);
    var charIndex = 0;
    var chunk = this.firstChunk;
    while (chunk) {
        var end = chunk.end;
        if (chunk.edited) {
            if (!isExcluded[charIndex]) {
                chunk.content = chunk.content.replace(pattern, replacer);
                if (chunk.content.length) {
                    shouldIndentNextCharacter = chunk.content[chunk.content.length - 1] === '\n';
                }
            }
        }
        else {
            charIndex = chunk.start;
            while (charIndex < end) {
                if (!isExcluded[charIndex]) {
                    var char = this.original[charIndex];
                    if (char === '\n') {
                        shouldIndentNextCharacter = true;
                    }
                    else if (char !== '\r' && shouldIndentNextCharacter) {
                        shouldIndentNextCharacter = false;
                        if (charIndex === chunk.start) {
                            chunk.prependRight(indentStr);
                        }
                        else {
                            this._splitChunk(chunk, charIndex);
                            chunk = chunk.next;
                            chunk.prependRight(indentStr);
                        }
                    }
                }
                charIndex += 1;
            }
        }
        charIndex = chunk.end;
        chunk = chunk.next;
    }
    this.outro = this.outro.replace(pattern, replacer);
    return this;
};
MagicString.prototype.insert = function insert() {
    throw new Error('magicString.insert(...) is deprecated. Use prependRight(...) or appendLeft(...)');
};
MagicString.prototype.insertLeft = function insertLeft(index, content) {
    if (!warned.insertLeft) {
        console.warn('magicString.insertLeft(...) is deprecated. Use magicString.appendLeft(...) instead'); // eslint-disable-line no-console
        warned.insertLeft = true;
    }
    return this.appendLeft(index, content);
};
MagicString.prototype.insertRight = function insertRight(index, content) {
    if (!warned.insertRight) {
        console.warn('magicString.insertRight(...) is deprecated. Use magicString.prependRight(...) instead'); // eslint-disable-line no-console
        warned.insertRight = true;
    }
    return this.prependRight(index, content);
};
MagicString.prototype.move = function move(start, end, index) {
    if (index >= start && index <= end) {
        throw new Error('Cannot move a selection inside itself');
    }
    this._split(start);
    this._split(end);
    this._split(index);
    var first = this.byStart[start];
    var last = this.byEnd[end];
    var oldLeft = first.previous;
    var oldRight = last.next;
    var newRight = this.byStart[index];
    if (!newRight && last === this.lastChunk) {
        return this;
    }
    var newLeft = newRight ? newRight.previous : this.lastChunk;
    if (oldLeft) {
        oldLeft.next = oldRight;
    }
    if (oldRight) {
        oldRight.previous = oldLeft;
    }
    if (newLeft) {
        newLeft.next = first;
    }
    if (newRight) {
        newRight.previous = last;
    }
    if (!first.previous) {
        this.firstChunk = last.next;
    }
    if (!last.next) {
        this.lastChunk = first.previous;
        this.lastChunk.next = null;
    }
    first.previous = newLeft;
    last.next = newRight || null;
    if (!newLeft) {
        this.firstChunk = first;
    }
    if (!newRight) {
        this.lastChunk = last;
    }
    return this;
};
MagicString.prototype.overwrite = function overwrite(start, end, content, options) {
    if (typeof content !== 'string') {
        throw new TypeError('replacement content must be a string');
    }
    while (start < 0) {
        start += this.original.length;
    }
    while (end < 0) {
        end += this.original.length;
    }
    if (end > this.original.length) {
        throw new Error('end is out of bounds');
    }
    if (start === end) {
        throw new Error('Cannot overwrite a zero-length range – use appendLeft or prependRight instead');
    }
    this._split(start);
    this._split(end);
    if (options === true) {
        if (!warned.storeName) {
            console.warn('The final argument to magicString.overwrite(...) should be an options object. See https://github.com/rich-harris/magic-string'); // eslint-disable-line no-console
            warned.storeName = true;
        }
        options = { storeName: true };
    }
    var storeName = options !== undefined ? options.storeName : false;
    var contentOnly = options !== undefined ? options.contentOnly : false;
    if (storeName) {
        var original = this.original.slice(start, end);
        this.storedNames[original] = true;
    }
    var first = this.byStart[start];
    var last = this.byEnd[end];
    if (first) {
        if (end > first.end && first.next !== this.byStart[first.end]) {
            throw new Error('Cannot overwrite across a split point');
        }
        first.edit(content, storeName, contentOnly);
        if (first !== last) {
            var chunk = first.next;
            while (chunk !== last) {
                chunk.edit('', false);
                chunk = chunk.next;
            }
            chunk.edit('', false);
        }
    }
    else {
        // must be inserting at the end
        var newChunk = new Chunk(start, end, '').edit(content, storeName);
        // TODO last chunk in the array may not be the last chunk, if it's moved...
        last.next = newChunk;
        newChunk.previous = last;
    }
    return this;
};
MagicString.prototype.prepend = function prepend(content) {
    if (typeof content !== 'string') {
        throw new TypeError('outro content must be a string');
    }
    this.intro = content + this.intro;
    return this;
};
MagicString.prototype.prependLeft = function prependLeft(index, content) {
    if (typeof content !== 'string') {
        throw new TypeError('inserted content must be a string');
    }
    this._split(index);
    var chunk = this.byEnd[index];
    if (chunk) {
        chunk.prependLeft(content);
    }
    else {
        this.intro = content + this.intro;
    }
    return this;
};
MagicString.prototype.prependRight = function prependRight(index, content) {
    if (typeof content !== 'string') {
        throw new TypeError('inserted content must be a string');
    }
    this._split(index);
    var chunk = this.byStart[index];
    if (chunk) {
        chunk.prependRight(content);
    }
    else {
        this.outro = content + this.outro;
    }
    return this;
};
MagicString.prototype.remove = function remove(start, end) {
    while (start < 0) {
        start += this.original.length;
    }
    while (end < 0) {
        end += this.original.length;
    }
    if (start === end) {
        return this;
    }
    if (start < 0 || end > this.original.length) {
        throw new Error('Character is out of bounds');
    }
    if (start > end) {
        throw new Error('end must be greater than start');
    }
    this._split(start);
    this._split(end);
    var chunk = this.byStart[start];
    while (chunk) {
        chunk.intro = '';
        chunk.outro = '';
        chunk.edit('');
        chunk = end > chunk.end ? this.byStart[chunk.end] : null;
    }
    return this;
};
MagicString.prototype.lastChar = function lastChar() {
    if (this.outro.length) {
        return this.outro[this.outro.length - 1];
    }
    var chunk = this.lastChunk;
    do {
        if (chunk.outro.length) {
            return chunk.outro[chunk.outro.length - 1];
        }
        if (chunk.content.length) {
            return chunk.content[chunk.content.length - 1];
        }
        if (chunk.intro.length) {
            return chunk.intro[chunk.intro.length - 1];
        }
    } while (chunk = chunk.previous);
    if (this.intro.length) {
        return this.intro[this.intro.length - 1];
    }
    return '';
};
MagicString.prototype.lastLine = function lastLine() {
    var lineIndex = this.outro.lastIndexOf(n);
    if (lineIndex !== -1) {
        return this.outro.substr(lineIndex + 1);
    }
    var lineStr = this.outro;
    var chunk = this.lastChunk;
    do {
        if (chunk.outro.length > 0) {
            lineIndex = chunk.outro.lastIndexOf(n);
            if (lineIndex !== -1) {
                return chunk.outro.substr(lineIndex + 1) + lineStr;
            }
            lineStr = chunk.outro + lineStr;
        }
        if (chunk.content.length > 0) {
            lineIndex = chunk.content.lastIndexOf(n);
            if (lineIndex !== -1) {
                return chunk.content.substr(lineIndex + 1) + lineStr;
            }
            lineStr = chunk.content + lineStr;
        }
        if (chunk.intro.length > 0) {
            lineIndex = chunk.intro.lastIndexOf(n);
            if (lineIndex !== -1) {
                return chunk.intro.substr(lineIndex + 1) + lineStr;
            }
            lineStr = chunk.intro + lineStr;
        }
    } while (chunk = chunk.previous);
    lineIndex = this.intro.lastIndexOf(n);
    if (lineIndex !== -1) {
        return this.intro.substr(lineIndex + 1) + lineStr;
    }
    return this.intro + lineStr;
};
MagicString.prototype.slice = function slice(start, end) {
    if (start === void 0)
        start = 0;
    if (end === void 0)
        end = this.original.length;
    while (start < 0) {
        start += this.original.length;
    }
    while (end < 0) {
        end += this.original.length;
    }
    var result = '';
    // find start chunk
    var chunk = this.firstChunk;
    while (chunk && (chunk.start > start || chunk.end <= start)) {
        // found end chunk before start
        if (chunk.start < end && chunk.end >= end) {
            return result;
        }
        chunk = chunk.next;
    }
    if (chunk && chunk.edited && chunk.start !== start) {
        throw new Error(("Cannot use replaced character " + start + " as slice start anchor."));
    }
    var startChunk = chunk;
    while (chunk) {
        if (chunk.intro && (startChunk !== chunk || chunk.start === start)) {
            result += chunk.intro;
        }
        var containsEnd = chunk.start < end && chunk.end >= end;
        if (containsEnd && chunk.edited && chunk.end !== end) {
            throw new Error(("Cannot use replaced character " + end + " as slice end anchor."));
        }
        var sliceStart = startChunk === chunk ? start - chunk.start : 0;
        var sliceEnd = containsEnd ? chunk.content.length + end - chunk.end : chunk.content.length;
        result += chunk.content.slice(sliceStart, sliceEnd);
        if (chunk.outro && (!containsEnd || chunk.end === end)) {
            result += chunk.outro;
        }
        if (containsEnd) {
            break;
        }
        chunk = chunk.next;
    }
    return result;
};
// TODO deprecate this? not really very useful
MagicString.prototype.snip = function snip(start, end) {
    var clone = this.clone();
    clone.remove(0, start);
    clone.remove(end, clone.original.length);
    return clone;
};
MagicString.prototype._split = function _split(index) {
    if (this.byStart[index] || this.byEnd[index]) {
        return;
    }
    var chunk = this.lastSearchedChunk;
    var searchForward = index > chunk.end;
    while (chunk) {
        if (chunk.contains(index)) {
            return this._splitChunk(chunk, index);
        }
        chunk = searchForward ? this.byStart[chunk.end] : this.byEnd[chunk.start];
    }
};
MagicString.prototype._splitChunk = function _splitChunk(chunk, index) {
    if (chunk.edited && chunk.content.length) {
        // zero-length edited chunks are a special case (overlapping replacements)
        var loc = getLocator(this.original)(index);
        throw new Error(("Cannot split a chunk that has already been edited (" + (loc.line) + ":" + (loc.column) + " – \"" + (chunk.original) + "\")"));
    }
    var newChunk = chunk.split(index);
    this.byEnd[index] = chunk;
    this.byStart[index] = newChunk;
    this.byEnd[newChunk.end] = newChunk;
    if (chunk === this.lastChunk) {
        this.lastChunk = newChunk;
    }
    this.lastSearchedChunk = chunk;
    return true;
};
MagicString.prototype.toString = function toString() {
    var str = this.intro;
    var chunk = this.firstChunk;
    while (chunk) {
        str += chunk.toString();
        chunk = chunk.next;
    }
    return str + this.outro;
};
MagicString.prototype.isEmpty = function isEmpty() {
    var chunk = this.firstChunk;
    do {
        if (chunk.intro.length && chunk.intro.trim() ||
            chunk.content.length && chunk.content.trim() ||
            chunk.outro.length && chunk.outro.trim()) {
            return false;
        }
    } while (chunk = chunk.next);
    return true;
};
MagicString.prototype.length = function length() {
    var chunk = this.firstChunk;
    var length = 0;
    do {
        length += chunk.intro.length + chunk.content.length + chunk.outro.length;
    } while (chunk = chunk.next);
    return length;
};
MagicString.prototype.trimLines = function trimLines() {
    return this.trim('[\\r\\n]');
};
MagicString.prototype.trim = function trim(charType) {
    return this.trimStart(charType).trimEnd(charType);
};
MagicString.prototype.trimEndAborted = function trimEndAborted(charType) {
    var rx = new RegExp((charType || '\\s') + '+$');
    this.outro = this.outro.replace(rx, '');
    if (this.outro.length) {
        return true;
    }
    var chunk = this.lastChunk;
    do {
        var end = chunk.end;
        var aborted = chunk.trimEnd(rx);
        // if chunk was trimmed, we have a new lastChunk
        if (chunk.end !== end) {
            if (this.lastChunk === chunk) {
                this.lastChunk = chunk.next;
            }
            this.byEnd[chunk.end] = chunk;
            this.byStart[chunk.next.start] = chunk.next;
            this.byEnd[chunk.next.end] = chunk.next;
        }
        if (aborted) {
            return true;
        }
        chunk = chunk.previous;
    } while (chunk);
    return false;
};
MagicString.prototype.trimEnd = function trimEnd(charType) {
    this.trimEndAborted(charType);
    return this;
};
MagicString.prototype.trimStartAborted = function trimStartAborted(charType) {
    var rx = new RegExp('^' + (charType || '\\s') + '+');
    this.intro = this.intro.replace(rx, '');
    if (this.intro.length) {
        return true;
    }
    var chunk = this.firstChunk;
    do {
        var end = chunk.end;
        var aborted = chunk.trimStart(rx);
        if (chunk.end !== end) {
            // special case...
            if (chunk === this.lastChunk) {
                this.lastChunk = chunk.next;
            }
            this.byEnd[chunk.end] = chunk;
            this.byStart[chunk.next.start] = chunk.next;
            this.byEnd[chunk.next.end] = chunk.next;
        }
        if (aborted) {
            return true;
        }
        chunk = chunk.next;
    } while (chunk);
    return false;
};
MagicString.prototype.trimStart = function trimStart(charType) {
    this.trimStartAborted(charType);
    return this;
};
var hasOwnProp = Object.prototype.hasOwnProperty;
var Bundle = function Bundle(options) {
    if (options === void 0)
        options = {};
    this.intro = options.intro || '';
    this.separator = options.separator !== undefined ? options.separator : '\n';
    this.sources = [];
    this.uniqueSources = [];
    this.uniqueSourceIndexByFilename = {};
};
Bundle.prototype.addSource = function addSource(source) {
    if (source instanceof MagicString) {
        return this.addSource({
            content: source,
            filename: source.filename,
            separator: this.separator
        });
    }
    if (!isObject(source) || !source.content) {
        throw new Error('bundle.addSource() takes an object with a `content` property, which should be an instance of MagicString, and an optional `filename`');
    }
    ['filename', 'indentExclusionRanges', 'separator'].forEach(function (option) {
        if (!hasOwnProp.call(source, option)) {
            source[option] = source.content[option];
        }
    });
    if (source.separator === undefined) {
        // TODO there's a bunch of this sort of thing, needs cleaning up
        source.separator = this.separator;
    }
    if (source.filename) {
        if (!hasOwnProp.call(this.uniqueSourceIndexByFilename, source.filename)) {
            this.uniqueSourceIndexByFilename[source.filename] = this.uniqueSources.length;
            this.uniqueSources.push({ filename: source.filename, content: source.content.original });
        }
        else {
            var uniqueSource = this.uniqueSources[this.uniqueSourceIndexByFilename[source.filename]];
            if (source.content.original !== uniqueSource.content) {
                throw new Error(("Illegal source: same filename (" + (source.filename) + "), different contents"));
            }
        }
    }
    this.sources.push(source);
    return this;
};
Bundle.prototype.append = function append(str, options) {
    this.addSource({
        content: new MagicString(str),
        separator: (options && options.separator) || ''
    });
    return this;
};
Bundle.prototype.clone = function clone() {
    var bundle = new Bundle({
        intro: this.intro,
        separator: this.separator
    });
    this.sources.forEach(function (source) {
        bundle.addSource({
            filename: source.filename,
            content: source.content.clone(),
            separator: source.separator
        });
    });
    return bundle;
};
Bundle.prototype.generateDecodedMap = function generateDecodedMap(options) {
    var this$1 = this;
    if (options === void 0)
        options = {};
    var names = [];
    this.sources.forEach(function (source) {
        Object.keys(source.content.storedNames).forEach(function (name) {
            if (!~names.indexOf(name)) {
                names.push(name);
            }
        });
    });
    var mappings = new Mappings(options.hires);
    if (this.intro) {
        mappings.advance(this.intro);
    }
    this.sources.forEach(function (source, i) {
        if (i > 0) {
            mappings.advance(this$1.separator);
        }
        var sourceIndex = source.filename ? this$1.uniqueSourceIndexByFilename[source.filename] : -1;
        var magicString = source.content;
        var locate = getLocator(magicString.original);
        if (magicString.intro) {
            mappings.advance(magicString.intro);
        }
        magicString.firstChunk.eachNext(function (chunk) {
            var loc = locate(chunk.start);
            if (chunk.intro.length) {
                mappings.advance(chunk.intro);
            }
            if (source.filename) {
                if (chunk.edited) {
                    mappings.addEdit(sourceIndex, chunk.content, loc, chunk.storeName ? names.indexOf(chunk.original) : -1);
                }
                else {
                    mappings.addUneditedChunk(sourceIndex, chunk, magicString.original, loc, magicString.sourcemapLocations);
                }
            }
            else {
                mappings.advance(chunk.content);
            }
            if (chunk.outro.length) {
                mappings.advance(chunk.outro);
            }
        });
        if (magicString.outro) {
            mappings.advance(magicString.outro);
        }
    });
    return {
        file: options.file ? options.file.split(/[/\\]/).pop() : null,
        sources: this.uniqueSources.map(function (source) {
            return options.file ? getRelativePath(options.file, source.filename) : source.filename;
        }),
        sourcesContent: this.uniqueSources.map(function (source) {
            return options.includeContent ? source.content : null;
        }),
        names: names,
        mappings: mappings.raw
    };
};
Bundle.prototype.generateMap = function generateMap(options) {
    return new SourceMap(this.generateDecodedMap(options));
};
Bundle.prototype.getIndentString = function getIndentString() {
    var indentStringCounts = {};
    this.sources.forEach(function (source) {
        var indentStr = source.content.indentStr;
        if (indentStr === null) {
            return;
        }
        if (!indentStringCounts[indentStr]) {
            indentStringCounts[indentStr] = 0;
        }
        indentStringCounts[indentStr] += 1;
    });
    return (Object.keys(indentStringCounts).sort(function (a, b) {
        return indentStringCounts[a] - indentStringCounts[b];
    })[0] || '\t');
};
Bundle.prototype.indent = function indent(indentStr) {
    var this$1 = this;
    if (!arguments.length) {
        indentStr = this.getIndentString();
    }
    if (indentStr === '') {
        return this;
    } // noop
    var trailingNewline = !this.intro || this.intro.slice(-1) === '\n';
    this.sources.forEach(function (source, i) {
        var separator = source.separator !== undefined ? source.separator : this$1.separator;
        var indentStart = trailingNewline || (i > 0 && /\r?\n$/.test(separator));
        source.content.indent(indentStr, {
            exclude: source.indentExclusionRanges,
            indentStart: indentStart //: trailingNewline || /\r?\n$/.test( separator )  //true///\r?\n/.test( separator )
        });
        trailingNewline = source.content.lastChar() === '\n';
    });
    if (this.intro) {
        this.intro =
            indentStr +
                this.intro.replace(/^[^\n]/gm, function (match, index) {
                    return index > 0 ? indentStr + match : match;
                });
    }
    return this;
};
Bundle.prototype.prepend = function prepend(str) {
    this.intro = str + this.intro;
    return this;
};
Bundle.prototype.toString = function toString() {
    var this$1 = this;
    var body = this.sources
        .map(function (source, i) {
        var separator = source.separator !== undefined ? source.separator : this$1.separator;
        var str = (i > 0 ? separator : '') + source.content.toString();
        return str;
    })
        .join('');
    return this.intro + body;
};
Bundle.prototype.isEmpty = function isEmpty() {
    if (this.intro.length && this.intro.trim()) {
        return false;
    }
    if (this.sources.some(function (source) { return !source.content.isEmpty(); })) {
        return false;
    }
    return true;
};
Bundle.prototype.length = function length() {
    return this.sources.reduce(function (length, source) { return length + source.content.length(); }, this.intro.length);
};
Bundle.prototype.trimLines = function trimLines() {
    return this.trim('[\\r\\n]');
};
Bundle.prototype.trim = function trim(charType) {
    return this.trimStart(charType).trimEnd(charType);
};
Bundle.prototype.trimStart = function trimStart(charType) {
    var rx = new RegExp('^' + (charType || '\\s') + '+');
    this.intro = this.intro.replace(rx, '');
    if (!this.intro) {
        var source;
        var i = 0;
        do {
            source = this.sources[i++];
            if (!source) {
                break;
            }
        } while (!source.content.trimStartAborted(charType));
    }
    return this;
};
Bundle.prototype.trimEnd = function trimEnd(charType) {
    var rx = new RegExp((charType || '\\s') + '+$');
    var source;
    var i = this.sources.length - 1;
    do {
        source = this.sources[i--];
        if (!source) {
            this.intro = this.intro.replace(rx, '');
            break;
        }
    } while (!source.content.trimEndAborted(charType));
    return this;
};

var minimalisticAssert = assert;
function assert(val, msg) {
    if (!val)
        throw new Error(msg || 'Assertion failed');
}
assert.equal = function assertEqual(l, r, msg) {
    if (l != r)
        throw new Error(msg || ('Assertion failed: ' + l + ' != ' + r));
};

function createCommonjsModule(fn, module) {
	return module = { exports: {} }, fn(module, module.exports), module.exports;
}

var inherits_browser = createCommonjsModule(function (module) {
    if (typeof Object.create === 'function') {
        // implementation from standard node.js 'util' module
        module.exports = function inherits(ctor, superCtor) {
            ctor.super_ = superCtor;
            ctor.prototype = Object.create(superCtor.prototype, {
                constructor: {
                    value: ctor,
                    enumerable: false,
                    writable: true,
                    configurable: true
                }
            });
        };
    }
    else {
        // old school shim for old browsers
        module.exports = function inherits(ctor, superCtor) {
            ctor.super_ = superCtor;
            var TempCtor = function () { };
            TempCtor.prototype = superCtor.prototype;
            ctor.prototype = new TempCtor();
            ctor.prototype.constructor = ctor;
        };
    }
});

var inherits = createCommonjsModule(function (module) {
    try {
        var util$1 = util;
        if (typeof util$1.inherits !== 'function')
            throw '';
        module.exports = util$1.inherits;
    }
    catch (e) {
        module.exports = inherits_browser;
    }
});

var inherits_1 = inherits;
function isSurrogatePair(msg, i) {
    if ((msg.charCodeAt(i) & 0xFC00) !== 0xD800) {
        return false;
    }
    if (i < 0 || i + 1 >= msg.length) {
        return false;
    }
    return (msg.charCodeAt(i + 1) & 0xFC00) === 0xDC00;
}
function toArray(msg, enc) {
    if (Array.isArray(msg))
        return msg.slice();
    if (!msg)
        return [];
    var res = [];
    if (typeof msg === 'string') {
        if (!enc) {
            // Inspired by stringToUtf8ByteArray() in closure-library by Google
            // https://github.com/google/closure-library/blob/8598d87242af59aac233270742c8984e2b2bdbe0/closure/goog/crypt/crypt.js#L117-L143
            // Apache License 2.0
            // https://github.com/google/closure-library/blob/master/LICENSE
            var p = 0;
            for (var i = 0; i < msg.length; i++) {
                var c = msg.charCodeAt(i);
                if (c < 128) {
                    res[p++] = c;
                }
                else if (c < 2048) {
                    res[p++] = (c >> 6) | 192;
                    res[p++] = (c & 63) | 128;
                }
                else if (isSurrogatePair(msg, i)) {
                    c = 0x10000 + ((c & 0x03FF) << 10) + (msg.charCodeAt(++i) & 0x03FF);
                    res[p++] = (c >> 18) | 240;
                    res[p++] = ((c >> 12) & 63) | 128;
                    res[p++] = ((c >> 6) & 63) | 128;
                    res[p++] = (c & 63) | 128;
                }
                else {
                    res[p++] = (c >> 12) | 224;
                    res[p++] = ((c >> 6) & 63) | 128;
                    res[p++] = (c & 63) | 128;
                }
            }
        }
        else if (enc === 'hex') {
            msg = msg.replace(/[^a-z0-9]+/ig, '');
            if (msg.length % 2 !== 0)
                msg = '0' + msg;
            for (i = 0; i < msg.length; i += 2)
                res.push(parseInt(msg[i] + msg[i + 1], 16));
        }
    }
    else {
        for (i = 0; i < msg.length; i++)
            res[i] = msg[i] | 0;
    }
    return res;
}
var toArray_1 = toArray;
function toHex(msg) {
    var res = '';
    for (var i = 0; i < msg.length; i++)
        res += zero2(msg[i].toString(16));
    return res;
}
var toHex_1 = toHex;
function htonl(w) {
    var res = (w >>> 24) |
        ((w >>> 8) & 0xff00) |
        ((w << 8) & 0xff0000) |
        ((w & 0xff) << 24);
    return res >>> 0;
}
var htonl_1 = htonl;
function toHex32(msg, endian) {
    var res = '';
    for (var i = 0; i < msg.length; i++) {
        var w = msg[i];
        if (endian === 'little')
            w = htonl(w);
        res += zero8(w.toString(16));
    }
    return res;
}
var toHex32_1 = toHex32;
function zero2(word) {
    if (word.length === 1)
        return '0' + word;
    else
        return word;
}
var zero2_1 = zero2;
function zero8(word) {
    if (word.length === 7)
        return '0' + word;
    else if (word.length === 6)
        return '00' + word;
    else if (word.length === 5)
        return '000' + word;
    else if (word.length === 4)
        return '0000' + word;
    else if (word.length === 3)
        return '00000' + word;
    else if (word.length === 2)
        return '000000' + word;
    else if (word.length === 1)
        return '0000000' + word;
    else
        return word;
}
var zero8_1 = zero8;
function join32(msg, start, end, endian) {
    var len = end - start;
    minimalisticAssert(len % 4 === 0);
    var res = new Array(len / 4);
    for (var i = 0, k = start; i < res.length; i++, k += 4) {
        var w;
        if (endian === 'big')
            w = (msg[k] << 24) | (msg[k + 1] << 16) | (msg[k + 2] << 8) | msg[k + 3];
        else
            w = (msg[k + 3] << 24) | (msg[k + 2] << 16) | (msg[k + 1] << 8) | msg[k];
        res[i] = w >>> 0;
    }
    return res;
}
var join32_1 = join32;
function split32(msg, endian) {
    var res = new Array(msg.length * 4);
    for (var i = 0, k = 0; i < msg.length; i++, k += 4) {
        var m = msg[i];
        if (endian === 'big') {
            res[k] = m >>> 24;
            res[k + 1] = (m >>> 16) & 0xff;
            res[k + 2] = (m >>> 8) & 0xff;
            res[k + 3] = m & 0xff;
        }
        else {
            res[k + 3] = m >>> 24;
            res[k + 2] = (m >>> 16) & 0xff;
            res[k + 1] = (m >>> 8) & 0xff;
            res[k] = m & 0xff;
        }
    }
    return res;
}
var split32_1 = split32;
function rotr32(w, b) {
    return (w >>> b) | (w << (32 - b));
}
var rotr32_1 = rotr32;
function rotl32(w, b) {
    return (w << b) | (w >>> (32 - b));
}
var rotl32_1 = rotl32;
function sum32(a, b) {
    return (a + b) >>> 0;
}
var sum32_1 = sum32;
function sum32_3(a, b, c) {
    return (a + b + c) >>> 0;
}
var sum32_3_1 = sum32_3;
function sum32_4(a, b, c, d) {
    return (a + b + c + d) >>> 0;
}
var sum32_4_1 = sum32_4;
function sum32_5(a, b, c, d, e) {
    return (a + b + c + d + e) >>> 0;
}
var sum32_5_1 = sum32_5;
function sum64(buf, pos, ah, al) {
    var bh = buf[pos];
    var bl = buf[pos + 1];
    var lo = (al + bl) >>> 0;
    var hi = (lo < al ? 1 : 0) + ah + bh;
    buf[pos] = hi >>> 0;
    buf[pos + 1] = lo;
}
var sum64_1 = sum64;
function sum64_hi(ah, al, bh, bl) {
    var lo = (al + bl) >>> 0;
    var hi = (lo < al ? 1 : 0) + ah + bh;
    return hi >>> 0;
}
var sum64_hi_1 = sum64_hi;
function sum64_lo(ah, al, bh, bl) {
    var lo = al + bl;
    return lo >>> 0;
}
var sum64_lo_1 = sum64_lo;
function sum64_4_hi(ah, al, bh, bl, ch, cl, dh, dl) {
    var carry = 0;
    var lo = al;
    lo = (lo + bl) >>> 0;
    carry += lo < al ? 1 : 0;
    lo = (lo + cl) >>> 0;
    carry += lo < cl ? 1 : 0;
    lo = (lo + dl) >>> 0;
    carry += lo < dl ? 1 : 0;
    var hi = ah + bh + ch + dh + carry;
    return hi >>> 0;
}
var sum64_4_hi_1 = sum64_4_hi;
function sum64_4_lo(ah, al, bh, bl, ch, cl, dh, dl) {
    var lo = al + bl + cl + dl;
    return lo >>> 0;
}
var sum64_4_lo_1 = sum64_4_lo;
function sum64_5_hi(ah, al, bh, bl, ch, cl, dh, dl, eh, el) {
    var carry = 0;
    var lo = al;
    lo = (lo + bl) >>> 0;
    carry += lo < al ? 1 : 0;
    lo = (lo + cl) >>> 0;
    carry += lo < cl ? 1 : 0;
    lo = (lo + dl) >>> 0;
    carry += lo < dl ? 1 : 0;
    lo = (lo + el) >>> 0;
    carry += lo < el ? 1 : 0;
    var hi = ah + bh + ch + dh + eh + carry;
    return hi >>> 0;
}
var sum64_5_hi_1 = sum64_5_hi;
function sum64_5_lo(ah, al, bh, bl, ch, cl, dh, dl, eh, el) {
    var lo = al + bl + cl + dl + el;
    return lo >>> 0;
}
var sum64_5_lo_1 = sum64_5_lo;
function rotr64_hi(ah, al, num) {
    var r = (al << (32 - num)) | (ah >>> num);
    return r >>> 0;
}
var rotr64_hi_1 = rotr64_hi;
function rotr64_lo(ah, al, num) {
    var r = (ah << (32 - num)) | (al >>> num);
    return r >>> 0;
}
var rotr64_lo_1 = rotr64_lo;
function shr64_hi(ah, al, num) {
    return ah >>> num;
}
var shr64_hi_1 = shr64_hi;
function shr64_lo(ah, al, num) {
    var r = (ah << (32 - num)) | (al >>> num);
    return r >>> 0;
}
var shr64_lo_1 = shr64_lo;
var utils = {
    inherits: inherits_1,
    toArray: toArray_1,
    toHex: toHex_1,
    htonl: htonl_1,
    toHex32: toHex32_1,
    zero2: zero2_1,
    zero8: zero8_1,
    join32: join32_1,
    split32: split32_1,
    rotr32: rotr32_1,
    rotl32: rotl32_1,
    sum32: sum32_1,
    sum32_3: sum32_3_1,
    sum32_4: sum32_4_1,
    sum32_5: sum32_5_1,
    sum64: sum64_1,
    sum64_hi: sum64_hi_1,
    sum64_lo: sum64_lo_1,
    sum64_4_hi: sum64_4_hi_1,
    sum64_4_lo: sum64_4_lo_1,
    sum64_5_hi: sum64_5_hi_1,
    sum64_5_lo: sum64_5_lo_1,
    rotr64_hi: rotr64_hi_1,
    rotr64_lo: rotr64_lo_1,
    shr64_hi: shr64_hi_1,
    shr64_lo: shr64_lo_1
};

function BlockHash() {
    this.pending = null;
    this.pendingTotal = 0;
    this.blockSize = this.constructor.blockSize;
    this.outSize = this.constructor.outSize;
    this.hmacStrength = this.constructor.hmacStrength;
    this.padLength = this.constructor.padLength / 8;
    this.endian = 'big';
    this._delta8 = this.blockSize / 8;
    this._delta32 = this.blockSize / 32;
}
var BlockHash_1 = BlockHash;
BlockHash.prototype.update = function update(msg, enc) {
    // Convert message to array, pad it, and join into 32bit blocks
    msg = utils.toArray(msg, enc);
    if (!this.pending)
        this.pending = msg;
    else
        this.pending = this.pending.concat(msg);
    this.pendingTotal += msg.length;
    // Enough data, try updating
    if (this.pending.length >= this._delta8) {
        msg = this.pending;
        // Process pending data in blocks
        var r = msg.length % this._delta8;
        this.pending = msg.slice(msg.length - r, msg.length);
        if (this.pending.length === 0)
            this.pending = null;
        msg = utils.join32(msg, 0, msg.length - r, this.endian);
        for (var i = 0; i < msg.length; i += this._delta32)
            this._update(msg, i, i + this._delta32);
    }
    return this;
};
BlockHash.prototype.digest = function digest(enc) {
    this.update(this._pad());
    minimalisticAssert(this.pending === null);
    return this._digest(enc);
};
BlockHash.prototype._pad = function pad() {
    var len = this.pendingTotal;
    var bytes = this._delta8;
    var k = bytes - ((len + this.padLength) % bytes);
    var res = new Array(k + this.padLength);
    res[0] = 0x80;
    for (var i = 1; i < k; i++)
        res[i] = 0;
    // Append length
    len <<= 3;
    if (this.endian === 'big') {
        for (var t = 8; t < this.padLength; t++)
            res[i++] = 0;
        res[i++] = 0;
        res[i++] = 0;
        res[i++] = 0;
        res[i++] = 0;
        res[i++] = (len >>> 24) & 0xff;
        res[i++] = (len >>> 16) & 0xff;
        res[i++] = (len >>> 8) & 0xff;
        res[i++] = len & 0xff;
    }
    else {
        res[i++] = len & 0xff;
        res[i++] = (len >>> 8) & 0xff;
        res[i++] = (len >>> 16) & 0xff;
        res[i++] = (len >>> 24) & 0xff;
        res[i++] = 0;
        res[i++] = 0;
        res[i++] = 0;
        res[i++] = 0;
        for (t = 8; t < this.padLength; t++)
            res[i++] = 0;
    }
    return res;
};
var common = {
    BlockHash: BlockHash_1
};

var rotr32$1 = utils.rotr32;
function ft_1(s, x, y, z) {
    if (s === 0)
        return ch32(x, y, z);
    if (s === 1 || s === 3)
        return p32(x, y, z);
    if (s === 2)
        return maj32(x, y, z);
}
var ft_1_1 = ft_1;
function ch32(x, y, z) {
    return (x & y) ^ ((~x) & z);
}
var ch32_1 = ch32;
function maj32(x, y, z) {
    return (x & y) ^ (x & z) ^ (y & z);
}
var maj32_1 = maj32;
function p32(x, y, z) {
    return x ^ y ^ z;
}
var p32_1 = p32;
function s0_256(x) {
    return rotr32$1(x, 2) ^ rotr32$1(x, 13) ^ rotr32$1(x, 22);
}
var s0_256_1 = s0_256;
function s1_256(x) {
    return rotr32$1(x, 6) ^ rotr32$1(x, 11) ^ rotr32$1(x, 25);
}
var s1_256_1 = s1_256;
function g0_256(x) {
    return rotr32$1(x, 7) ^ rotr32$1(x, 18) ^ (x >>> 3);
}
var g0_256_1 = g0_256;
function g1_256(x) {
    return rotr32$1(x, 17) ^ rotr32$1(x, 19) ^ (x >>> 10);
}
var g1_256_1 = g1_256;
var common$1 = {
    ft_1: ft_1_1,
    ch32: ch32_1,
    maj32: maj32_1,
    p32: p32_1,
    s0_256: s0_256_1,
    s1_256: s1_256_1,
    g0_256: g0_256_1,
    g1_256: g1_256_1
};

var sum32$1 = utils.sum32;
var sum32_4$1 = utils.sum32_4;
var sum32_5$1 = utils.sum32_5;
var ch32$1 = common$1.ch32;
var maj32$1 = common$1.maj32;
var s0_256$1 = common$1.s0_256;
var s1_256$1 = common$1.s1_256;
var g0_256$1 = common$1.g0_256;
var g1_256$1 = common$1.g1_256;
var BlockHash$1 = common.BlockHash;
var sha256_K = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
    0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
    0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
    0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
    0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
    0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
    0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
    0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
];
function SHA256() {
    if (!(this instanceof SHA256))
        return new SHA256();
    BlockHash$1.call(this);
    this.h = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    ];
    this.k = sha256_K;
    this.W = new Array(64);
}
utils.inherits(SHA256, BlockHash$1);
var _256 = SHA256;
SHA256.blockSize = 512;
SHA256.outSize = 256;
SHA256.hmacStrength = 192;
SHA256.padLength = 64;
SHA256.prototype._update = function _update(msg, start) {
    var W = this.W;
    for (var i = 0; i < 16; i++)
        W[i] = msg[start + i];
    for (; i < W.length; i++)
        W[i] = sum32_4$1(g1_256$1(W[i - 2]), W[i - 7], g0_256$1(W[i - 15]), W[i - 16]);
    var a = this.h[0];
    var b = this.h[1];
    var c = this.h[2];
    var d = this.h[3];
    var e = this.h[4];
    var f = this.h[5];
    var g = this.h[6];
    var h = this.h[7];
    minimalisticAssert(this.k.length === W.length);
    for (i = 0; i < W.length; i++) {
        var T1 = sum32_5$1(h, s1_256$1(e), ch32$1(e, f, g), this.k[i], W[i]);
        var T2 = sum32$1(s0_256$1(a), maj32$1(a, b, c));
        h = g;
        g = f;
        f = e;
        e = sum32$1(d, T1);
        d = c;
        c = b;
        b = a;
        a = sum32$1(T1, T2);
    }
    this.h[0] = sum32$1(this.h[0], a);
    this.h[1] = sum32$1(this.h[1], b);
    this.h[2] = sum32$1(this.h[2], c);
    this.h[3] = sum32$1(this.h[3], d);
    this.h[4] = sum32$1(this.h[4], e);
    this.h[5] = sum32$1(this.h[5], f);
    this.h[6] = sum32$1(this.h[6], g);
    this.h[7] = sum32$1(this.h[7], h);
};
SHA256.prototype._digest = function digest(enc) {
    if (enc === 'hex')
        return utils.toHex32(this.h, 'big');
    else
        return utils.split32(this.h, 'big');
};

const createHash = () => _256();

function relative(from, to) {
    const fromParts = from.split(/[/\\]/).filter(Boolean);
    const toParts = to.split(/[/\\]/).filter(Boolean);
    if (fromParts[0] === '.')
        fromParts.shift();
    if (toParts[0] === '.')
        toParts.shift();
    while (fromParts[0] && toParts[0] && fromParts[0] === toParts[0]) {
        fromParts.shift();
        toParts.shift();
    }
    while (toParts[0] === '..' && fromParts.length > 0) {
        toParts.shift();
        fromParts.pop();
    }
    while (fromParts.pop()) {
        toParts.unshift('..');
    }
    return toParts.join('/');
}

const UnknownKey = Symbol('Unknown Key');
const EMPTY_PATH = [];
const UNKNOWN_PATH = [UnknownKey];
const EntitiesKey = Symbol('Entities');
class PathTracker {
    constructor() {
        this.entityPaths = Object.create(null, { [EntitiesKey]: { value: new Set() } });
    }
    getEntities(path) {
        let currentPaths = this.entityPaths;
        for (const pathSegment of path) {
            currentPaths = currentPaths[pathSegment] =
                currentPaths[pathSegment] ||
                    Object.create(null, { [EntitiesKey]: { value: new Set() } });
        }
        return currentPaths[EntitiesKey];
    }
}
const SHARED_RECURSION_TRACKER = new PathTracker();

const BROKEN_FLOW_NONE = 0;
const BROKEN_FLOW_BREAK_CONTINUE = 1;
const BROKEN_FLOW_ERROR_RETURN_LABEL = 2;
function createInclusionContext() {
    return {
        brokenFlow: BROKEN_FLOW_NONE,
        includedLabels: new Set()
    };
}
function createHasEffectsContext() {
    return {
        accessed: new PathTracker(),
        assigned: new PathTracker(),
        brokenFlow: BROKEN_FLOW_NONE,
        called: new PathTracker(),
        ignore: {
            breaks: false,
            continues: false,
            labels: new Set(),
            returnAwaitYield: false
        },
        includedLabels: new Set(),
        instantiated: new PathTracker(),
        replacedVariableInits: new Map()
    };
}

const BlockStatement = 'BlockStatement';
const CallExpression = 'CallExpression';
const ExportNamespaceSpecifier = 'ExportNamespaceSpecifier';
const ExpressionStatement = 'ExpressionStatement';
const FunctionExpression = 'FunctionExpression';
const Identifier = 'Identifier';
const ImportDefaultSpecifier = 'ImportDefaultSpecifier';
const ImportNamespaceSpecifier = 'ImportNamespaceSpecifier';
const Program = 'Program';
const Property = 'Property';
const ReturnStatement = 'ReturnStatement';

function treeshakeNode(node, code, start, end) {
    code.remove(start, end);
    if (node.annotations) {
        for (const annotation of node.annotations) {
            if (annotation.start < start) {
                code.remove(annotation.start, annotation.end);
            }
            else {
                return;
            }
        }
    }
}
function removeAnnotations(node, code) {
    if (!node.annotations && node.parent.type === ExpressionStatement) {
        node = node.parent;
    }
    if (node.annotations) {
        for (const annotation of node.annotations) {
            code.remove(annotation.start, annotation.end);
        }
    }
}

const NO_SEMICOLON = { isNoStatement: true };
// This assumes there are only white-space and comments between start and the string we are looking for
function findFirstOccurrenceOutsideComment(code, searchString, start = 0) {
    let searchPos, charCodeAfterSlash;
    searchPos = code.indexOf(searchString, start);
    while (true) {
        start = code.indexOf('/', start);
        if (start === -1 || start >= searchPos)
            return searchPos;
        charCodeAfterSlash = code.charCodeAt(++start);
        ++start;
        // With our assumption, '/' always starts a comment. Determine comment type:
        start =
            charCodeAfterSlash === 47 /*"/"*/
                ? code.indexOf('\n', start) + 1
                : code.indexOf('*/', start) + 2;
        if (start > searchPos) {
            searchPos = code.indexOf(searchString, start);
        }
    }
}
// This assumes "code" only contains white-space and comments
function findFirstLineBreakOutsideComment(code) {
    let lineBreakPos, charCodeAfterSlash, start = 0;
    lineBreakPos = code.indexOf('\n', start);
    while (true) {
        start = code.indexOf('/', start);
        if (start === -1 || start > lineBreakPos)
            return lineBreakPos;
        // With our assumption, '/' always starts a comment. Determine comment type:
        charCodeAfterSlash = code.charCodeAt(++start);
        if (charCodeAfterSlash === 47 /*"/"*/)
            return lineBreakPos;
        start = code.indexOf('*/', start + 2) + 2;
        if (start > lineBreakPos) {
            lineBreakPos = code.indexOf('\n', start);
        }
    }
}
function renderStatementList(statements, code, start, end, options) {
    let currentNode, currentNodeStart, currentNodeNeedsBoundaries, nextNodeStart;
    let nextNode = statements[0];
    let nextNodeNeedsBoundaries = !nextNode.included || nextNode.needsBoundaries;
    if (nextNodeNeedsBoundaries) {
        nextNodeStart =
            start + findFirstLineBreakOutsideComment(code.original.slice(start, nextNode.start)) + 1;
    }
    for (let nextIndex = 1; nextIndex <= statements.length; nextIndex++) {
        currentNode = nextNode;
        currentNodeStart = nextNodeStart;
        currentNodeNeedsBoundaries = nextNodeNeedsBoundaries;
        nextNode = statements[nextIndex];
        nextNodeNeedsBoundaries =
            nextNode === undefined ? false : !nextNode.included || nextNode.needsBoundaries;
        if (currentNodeNeedsBoundaries || nextNodeNeedsBoundaries) {
            nextNodeStart =
                currentNode.end +
                    findFirstLineBreakOutsideComment(code.original.slice(currentNode.end, nextNode === undefined ? end : nextNode.start)) +
                    1;
            if (currentNode.included) {
                currentNodeNeedsBoundaries
                    ? currentNode.render(code, options, {
                        end: nextNodeStart,
                        start: currentNodeStart
                    })
                    : currentNode.render(code, options);
            }
            else {
                treeshakeNode(currentNode, code, currentNodeStart, nextNodeStart);
            }
        }
        else {
            currentNode.render(code, options);
        }
    }
}
// This assumes that the first character is not part of the first node
function getCommaSeparatedNodesWithBoundaries(nodes, code, start, end) {
    const splitUpNodes = [];
    let node, nextNode, nextNodeStart, contentEnd, char;
    let separator = start - 1;
    for (let nextIndex = 0; nextIndex < nodes.length; nextIndex++) {
        nextNode = nodes[nextIndex];
        if (node !== undefined) {
            separator =
                node.end +
                    findFirstOccurrenceOutsideComment(code.original.slice(node.end, nextNode.start), ',');
        }
        nextNodeStart = contentEnd =
            separator +
                2 +
                findFirstLineBreakOutsideComment(code.original.slice(separator + 1, nextNode.start));
        while (((char = code.original.charCodeAt(nextNodeStart)),
            char === 32 /*" "*/ || char === 9 /*"\t"*/ || char === 10 /*"\n"*/ || char === 13) /*"\r"*/)
            nextNodeStart++;
        if (node !== undefined) {
            splitUpNodes.push({
                contentEnd,
                end: nextNodeStart,
                node,
                separator,
                start
            });
        }
        node = nextNode;
        start = nextNodeStart;
    }
    splitUpNodes.push({
        contentEnd: end,
        end,
        node: node,
        separator: null,
        start
    });
    return splitUpNodes;
}
// This assumes there are only white-space and comments between start and end
function removeLineBreaks(code, start, end) {
    while (true) {
        const lineBreakPos = findFirstLineBreakOutsideComment(code.original.slice(start, end));
        if (lineBreakPos === -1) {
            break;
        }
        start = start + lineBreakPos + 1;
        code.remove(start - 1, start);
    }
}

const chars$1 = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$';
const base = 64;
function toBase64(num) {
    let outStr = '';
    do {
        const curDigit = num % base;
        num = Math.floor(num / base);
        outStr = chars$1[curDigit] + outStr;
    } while (num !== 0);
    return outStr;
}

// Verified on IE 6/7 that these keywords can't be used for object properties without escaping:
//   break case catch class const continue debugger default delete do
//   else enum export extends false finally for function if import
//   in instanceof new null return super switch this throw true
//   try typeof var void while with
const RESERVED_NAMES = Object.assign(Object.create(null), {
    await: true,
    break: true,
    case: true,
    catch: true,
    class: true,
    const: true,
    continue: true,
    debugger: true,
    default: true,
    delete: true,
    do: true,
    else: true,
    enum: true,
    eval: true,
    export: true,
    extends: true,
    false: true,
    finally: true,
    for: true,
    function: true,
    if: true,
    implements: true,
    import: true,
    in: true,
    instanceof: true,
    interface: true,
    let: true,
    new: true,
    null: true,
    package: true,
    private: true,
    protected: true,
    public: true,
    return: true,
    static: true,
    super: true,
    switch: true,
    this: true,
    throw: true,
    true: true,
    try: true,
    typeof: true,
    undefined: true,
    var: true,
    void: true,
    while: true,
    with: true,
    yield: true
});

function getSafeName(baseName, usedNames) {
    let safeName = baseName;
    let count = 1;
    while (usedNames.has(safeName) || RESERVED_NAMES[safeName]) {
        safeName = `${baseName}$${toBase64(count++)}`;
    }
    usedNames.add(safeName);
    return safeName;
}

const NO_ARGS = [];

function assembleMemberDescriptions(memberDescriptions, inheritedDescriptions = null) {
    return Object.create(inheritedDescriptions, memberDescriptions);
}
const UnknownValue = Symbol('Unknown Value');
const UNKNOWN_EXPRESSION = {
    deoptimizePath: () => { },
    getLiteralValueAtPath: () => UnknownValue,
    getReturnExpressionWhenCalledAtPath: () => UNKNOWN_EXPRESSION,
    hasEffectsWhenAccessedAtPath: path => path.length > 0,
    hasEffectsWhenAssignedAtPath: path => path.length > 0,
    hasEffectsWhenCalledAtPath: () => true,
    include: () => { },
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    },
    included: true,
    toString: () => '[[UNKNOWN]]'
};
const UNDEFINED_EXPRESSION = {
    deoptimizePath: () => { },
    getLiteralValueAtPath: () => undefined,
    getReturnExpressionWhenCalledAtPath: () => UNKNOWN_EXPRESSION,
    hasEffectsWhenAccessedAtPath: path => path.length > 0,
    hasEffectsWhenAssignedAtPath: path => path.length > 0,
    hasEffectsWhenCalledAtPath: () => true,
    include: () => { },
    includeCallArguments() { },
    included: true,
    toString: () => 'undefined'
};
const returnsUnknown = {
    value: {
        callsArgs: null,
        mutatesSelf: false,
        returns: null,
        returnsPrimitive: UNKNOWN_EXPRESSION
    }
};
const mutatesSelfReturnsUnknown = {
    value: { returns: null, returnsPrimitive: UNKNOWN_EXPRESSION, callsArgs: null, mutatesSelf: true }
};
const callsArgReturnsUnknown = {
    value: { returns: null, returnsPrimitive: UNKNOWN_EXPRESSION, callsArgs: [0], mutatesSelf: false }
};
class UnknownArrayExpression {
    constructor() {
        this.included = false;
    }
    deoptimizePath() { }
    getLiteralValueAtPath() {
        return UnknownValue;
    }
    getReturnExpressionWhenCalledAtPath(path) {
        if (path.length === 1) {
            return getMemberReturnExpressionWhenCalled(arrayMembers, path[0]);
        }
        return UNKNOWN_EXPRESSION;
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenAssignedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length === 1) {
            return hasMemberEffectWhenCalled(arrayMembers, path[0], this.included, callOptions, context);
        }
        return true;
    }
    include() {
        this.included = true;
    }
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    }
    toString() {
        return '[[UNKNOWN ARRAY]]';
    }
}
const returnsArray = {
    value: {
        callsArgs: null,
        mutatesSelf: false,
        returns: UnknownArrayExpression,
        returnsPrimitive: null
    }
};
const mutatesSelfReturnsArray = {
    value: {
        callsArgs: null,
        mutatesSelf: true,
        returns: UnknownArrayExpression,
        returnsPrimitive: null
    }
};
const callsArgReturnsArray = {
    value: {
        callsArgs: [0],
        mutatesSelf: false,
        returns: UnknownArrayExpression,
        returnsPrimitive: null
    }
};
const callsArgMutatesSelfReturnsArray = {
    value: {
        callsArgs: [0],
        mutatesSelf: true,
        returns: UnknownArrayExpression,
        returnsPrimitive: null
    }
};
const UNKNOWN_LITERAL_BOOLEAN = {
    deoptimizePath: () => { },
    getLiteralValueAtPath: () => UnknownValue,
    getReturnExpressionWhenCalledAtPath: path => {
        if (path.length === 1) {
            return getMemberReturnExpressionWhenCalled(literalBooleanMembers, path[0]);
        }
        return UNKNOWN_EXPRESSION;
    },
    hasEffectsWhenAccessedAtPath: path => path.length > 1,
    hasEffectsWhenAssignedAtPath: path => path.length > 0,
    hasEffectsWhenCalledAtPath: path => {
        if (path.length === 1) {
            const subPath = path[0];
            return typeof subPath !== 'string' || !literalBooleanMembers[subPath];
        }
        return true;
    },
    include: () => { },
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    },
    included: true,
    toString: () => '[[UNKNOWN BOOLEAN]]'
};
const returnsBoolean = {
    value: {
        callsArgs: null,
        mutatesSelf: false,
        returns: null,
        returnsPrimitive: UNKNOWN_LITERAL_BOOLEAN
    }
};
const callsArgReturnsBoolean = {
    value: {
        callsArgs: [0],
        mutatesSelf: false,
        returns: null,
        returnsPrimitive: UNKNOWN_LITERAL_BOOLEAN
    }
};
const UNKNOWN_LITERAL_NUMBER = {
    deoptimizePath: () => { },
    getLiteralValueAtPath: () => UnknownValue,
    getReturnExpressionWhenCalledAtPath: path => {
        if (path.length === 1) {
            return getMemberReturnExpressionWhenCalled(literalNumberMembers, path[0]);
        }
        return UNKNOWN_EXPRESSION;
    },
    hasEffectsWhenAccessedAtPath: path => path.length > 1,
    hasEffectsWhenAssignedAtPath: path => path.length > 0,
    hasEffectsWhenCalledAtPath: path => {
        if (path.length === 1) {
            const subPath = path[0];
            return typeof subPath !== 'string' || !literalNumberMembers[subPath];
        }
        return true;
    },
    include: () => { },
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    },
    included: true,
    toString: () => '[[UNKNOWN NUMBER]]'
};
const returnsNumber = {
    value: {
        callsArgs: null,
        mutatesSelf: false,
        returns: null,
        returnsPrimitive: UNKNOWN_LITERAL_NUMBER
    }
};
const mutatesSelfReturnsNumber = {
    value: {
        callsArgs: null,
        mutatesSelf: true,
        returns: null,
        returnsPrimitive: UNKNOWN_LITERAL_NUMBER
    }
};
const callsArgReturnsNumber = {
    value: {
        callsArgs: [0],
        mutatesSelf: false,
        returns: null,
        returnsPrimitive: UNKNOWN_LITERAL_NUMBER
    }
};
const UNKNOWN_LITERAL_STRING = {
    deoptimizePath: () => { },
    getLiteralValueAtPath: () => UnknownValue,
    getReturnExpressionWhenCalledAtPath: path => {
        if (path.length === 1) {
            return getMemberReturnExpressionWhenCalled(literalStringMembers, path[0]);
        }
        return UNKNOWN_EXPRESSION;
    },
    hasEffectsWhenAccessedAtPath: path => path.length > 1,
    hasEffectsWhenAssignedAtPath: path => path.length > 0,
    hasEffectsWhenCalledAtPath: (path, callOptions, context) => {
        if (path.length === 1) {
            return hasMemberEffectWhenCalled(literalStringMembers, path[0], true, callOptions, context);
        }
        return true;
    },
    include: () => { },
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    },
    included: true,
    toString: () => '[[UNKNOWN STRING]]'
};
const returnsString = {
    value: {
        callsArgs: null,
        mutatesSelf: false,
        returns: null,
        returnsPrimitive: UNKNOWN_LITERAL_STRING
    }
};
class UnknownObjectExpression {
    constructor() {
        this.included = false;
    }
    deoptimizePath() { }
    getLiteralValueAtPath() {
        return UnknownValue;
    }
    getReturnExpressionWhenCalledAtPath(path) {
        if (path.length === 1) {
            return getMemberReturnExpressionWhenCalled(objectMembers, path[0]);
        }
        return UNKNOWN_EXPRESSION;
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenAssignedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length === 1) {
            return hasMemberEffectWhenCalled(objectMembers, path[0], this.included, callOptions, context);
        }
        return true;
    }
    include() {
        this.included = true;
    }
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    }
    toString() {
        return '[[UNKNOWN OBJECT]]';
    }
}
const objectMembers = assembleMemberDescriptions({
    hasOwnProperty: returnsBoolean,
    isPrototypeOf: returnsBoolean,
    propertyIsEnumerable: returnsBoolean,
    toLocaleString: returnsString,
    toString: returnsString,
    valueOf: returnsUnknown
});
const arrayMembers = assembleMemberDescriptions({
    concat: returnsArray,
    copyWithin: mutatesSelfReturnsArray,
    every: callsArgReturnsBoolean,
    fill: mutatesSelfReturnsArray,
    filter: callsArgReturnsArray,
    find: callsArgReturnsUnknown,
    findIndex: callsArgReturnsNumber,
    forEach: callsArgReturnsUnknown,
    includes: returnsBoolean,
    indexOf: returnsNumber,
    join: returnsString,
    lastIndexOf: returnsNumber,
    map: callsArgReturnsArray,
    pop: mutatesSelfReturnsUnknown,
    push: mutatesSelfReturnsNumber,
    reduce: callsArgReturnsUnknown,
    reduceRight: callsArgReturnsUnknown,
    reverse: mutatesSelfReturnsArray,
    shift: mutatesSelfReturnsUnknown,
    slice: returnsArray,
    some: callsArgReturnsBoolean,
    sort: callsArgMutatesSelfReturnsArray,
    splice: mutatesSelfReturnsArray,
    unshift: mutatesSelfReturnsNumber
}, objectMembers);
const literalBooleanMembers = assembleMemberDescriptions({
    valueOf: returnsBoolean
}, objectMembers);
const literalNumberMembers = assembleMemberDescriptions({
    toExponential: returnsString,
    toFixed: returnsString,
    toLocaleString: returnsString,
    toPrecision: returnsString,
    valueOf: returnsNumber
}, objectMembers);
const literalStringMembers = assembleMemberDescriptions({
    charAt: returnsString,
    charCodeAt: returnsNumber,
    codePointAt: returnsNumber,
    concat: returnsString,
    endsWith: returnsBoolean,
    includes: returnsBoolean,
    indexOf: returnsNumber,
    lastIndexOf: returnsNumber,
    localeCompare: returnsNumber,
    match: returnsBoolean,
    normalize: returnsString,
    padEnd: returnsString,
    padStart: returnsString,
    repeat: returnsString,
    replace: {
        value: {
            callsArgs: [1],
            mutatesSelf: false,
            returns: null,
            returnsPrimitive: UNKNOWN_LITERAL_STRING
        }
    },
    search: returnsNumber,
    slice: returnsString,
    split: returnsArray,
    startsWith: returnsBoolean,
    substr: returnsString,
    substring: returnsString,
    toLocaleLowerCase: returnsString,
    toLocaleUpperCase: returnsString,
    toLowerCase: returnsString,
    toUpperCase: returnsString,
    trim: returnsString,
    valueOf: returnsString
}, objectMembers);
function getLiteralMembersForValue(value) {
    switch (typeof value) {
        case 'boolean':
            return literalBooleanMembers;
        case 'number':
            return literalNumberMembers;
        case 'string':
            return literalStringMembers;
        default:
            return Object.create(null);
    }
}
function hasMemberEffectWhenCalled(members, memberName, parentIncluded, callOptions, context) {
    if (typeof memberName !== 'string' ||
        !members[memberName] ||
        (members[memberName].mutatesSelf && parentIncluded))
        return true;
    if (!members[memberName].callsArgs)
        return false;
    for (const argIndex of members[memberName].callsArgs) {
        if (callOptions.args[argIndex] &&
            callOptions.args[argIndex].hasEffectsWhenCalledAtPath(EMPTY_PATH, {
                args: NO_ARGS,
                withNew: false
            }, context))
            return true;
    }
    return false;
}
function getMemberReturnExpressionWhenCalled(members, memberName) {
    if (typeof memberName !== 'string' || !members[memberName])
        return UNKNOWN_EXPRESSION;
    return members[memberName].returnsPrimitive !== null
        ? members[memberName].returnsPrimitive
        : new members[memberName].returns();
}

class Variable {
    constructor(name) {
        this.alwaysRendered = false;
        this.exportName = null;
        this.included = false;
        this.isId = false;
        this.isReassigned = false;
        this.renderBaseName = null;
        this.renderName = null;
        this.safeExportName = null;
        this.name = name;
    }
    /**
     * Binds identifiers that reference this variable to this variable.
     * Necessary to be able to change variable names.
     */
    addReference(_identifier) { }
    deoptimizePath(_path) { }
    getBaseVariableName() {
        return this.renderBaseName || this.renderName || this.name;
    }
    getLiteralValueAtPath(_path, _recursionTracker, _origin) {
        return UnknownValue;
    }
    getName() {
        const name = this.renderName || this.name;
        return this.renderBaseName ? `${this.renderBaseName}${getPropertyAccess(name)}` : name;
    }
    getReturnExpressionWhenCalledAtPath(_path, _recursionTracker, _origin) {
        return UNKNOWN_EXPRESSION;
    }
    hasEffectsWhenAccessedAtPath(path, _context) {
        return path.length > 0;
    }
    hasEffectsWhenAssignedAtPath(_path, _context) {
        return true;
    }
    hasEffectsWhenCalledAtPath(_path, _callOptions, _context) {
        return true;
    }
    /**
     * Marks this variable as being part of the bundle, which is usually the case when one of
     * its identifiers becomes part of the bundle. Returns true if it has not been included
     * previously.
     * Once a variable is included, it should take care all its declarations are included.
     */
    include(_context) {
        this.included = true;
    }
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    }
    markCalledFromTryStatement() { }
    setRenderNames(baseName, name) {
        this.renderBaseName = baseName;
        this.renderName = name;
    }
    setSafeName(name) {
        this.renderName = name;
    }
    toString() {
        return this.name;
    }
}
const getPropertyAccess = (name) => {
    return /^(?!\d)[\w$]+$/.test(name) ? `.${name}` : `[${JSON.stringify(name)}]`;
};

class ExternalVariable extends Variable {
    constructor(module, name) {
        super(name);
        this.module = module;
        this.isNamespace = name === '*';
        this.referenced = false;
    }
    addReference(identifier) {
        this.referenced = true;
        if (this.name === 'default' || this.name === '*') {
            this.module.suggestName(identifier.name);
        }
    }
    include() {
        if (!this.included) {
            this.included = true;
            this.module.used = true;
        }
    }
}

const reservedWords = 'break case class catch const continue debugger default delete do else export extends finally for function if import in instanceof let new return super switch this throw try typeof var void while with yield enum await implements package protected static interface private public'.split(' ');
const builtins = 'Infinity NaN undefined null true false eval uneval isFinite isNaN parseFloat parseInt decodeURI decodeURIComponent encodeURI encodeURIComponent escape unescape Object Function Boolean Symbol Error EvalError InternalError RangeError ReferenceError SyntaxError TypeError URIError Number Math Date String RegExp Array Int8Array Uint8Array Uint8ClampedArray Int16Array Uint16Array Int32Array Uint32Array Float32Array Float64Array Map Set WeakMap WeakSet SIMD ArrayBuffer DataView JSON Promise Generator GeneratorFunction Reflect Proxy Intl'.split(' ');
const blacklisted = new Set(reservedWords.concat(builtins));
const illegalCharacters = /[^$_a-zA-Z0-9]/g;
const startsWithDigit = (str) => /\d/.test(str[0]);
function isLegal(str) {
    if (startsWithDigit(str) || blacklisted.has(str)) {
        return false;
    }
    return !illegalCharacters.test(str);
}
function makeLegal(str) {
    str = str.replace(/-(\w)/g, (_, letter) => letter.toUpperCase()).replace(illegalCharacters, '_');
    if (startsWithDigit(str) || blacklisted.has(str))
        str = `_${str}`;
    return str || '_';
}

const absolutePath = /^(?:\/|(?:[A-Za-z]:)?[\\|/])/;
const relativePath = /^\.?\.\//;
function isAbsolute(path) {
    return absolutePath.test(path);
}
function isRelative(path) {
    return relativePath.test(path);
}
function normalize(path) {
    if (path.indexOf('\\') == -1)
        return path;
    return path.replace(/\\/g, '/');
}

class ExternalModule {
    constructor(graph, id, moduleSideEffects) {
        this.exportsNames = false;
        this.exportsNamespace = false;
        this.mostCommonSuggestion = 0;
        this.reexported = false;
        this.renderPath = undefined;
        this.renormalizeRenderPath = false;
        this.used = false;
        this.graph = graph;
        this.id = id;
        this.execIndex = Infinity;
        this.moduleSideEffects = moduleSideEffects;
        const parts = id.split(/[\\/]/);
        this.variableName = makeLegal(parts.pop());
        this.nameSuggestions = Object.create(null);
        this.declarations = Object.create(null);
        this.exportedVariables = new Map();
    }
    getVariableForExportName(name) {
        if (name === '*') {
            this.exportsNamespace = true;
        }
        else if (name !== 'default') {
            this.exportsNames = true;
        }
        let declaration = this.declarations[name];
        if (declaration)
            return declaration;
        this.declarations[name] = declaration = new ExternalVariable(this, name);
        this.exportedVariables.set(declaration, name);
        return declaration;
    }
    setRenderPath(options, inputBase) {
        this.renderPath = '';
        if (options.paths) {
            this.renderPath =
                typeof options.paths === 'function' ? options.paths(this.id) : options.paths[this.id];
        }
        if (!this.renderPath) {
            if (!isAbsolute(this.id)) {
                this.renderPath = this.id;
            }
            else {
                this.renderPath = normalize(relative$1(inputBase, this.id));
                this.renormalizeRenderPath = true;
            }
        }
        return this.renderPath;
    }
    suggestName(name) {
        if (!this.nameSuggestions[name])
            this.nameSuggestions[name] = 0;
        this.nameSuggestions[name] += 1;
        if (this.nameSuggestions[name] > this.mostCommonSuggestion) {
            this.mostCommonSuggestion = this.nameSuggestions[name];
            this.variableName = name;
        }
    }
    warnUnusedImports() {
        const unused = Object.keys(this.declarations).filter(name => {
            if (name === '*')
                return false;
            const declaration = this.declarations[name];
            return !declaration.included && !this.reexported && !declaration.referenced;
        });
        if (unused.length === 0)
            return;
        const names = unused.length === 1
            ? `'${unused[0]}' is`
            : `${unused
                .slice(0, -1)
                .map(name => `'${name}'`)
                .join(', ')} and '${unused.slice(-1)}' are`;
        this.graph.warn({
            code: 'UNUSED_EXTERNAL_IMPORT',
            message: `${names} imported from external module '${this.id}' but never used`,
            names: unused,
            source: this.id
        });
    }
}

function markModuleAndImpureDependenciesAsExecuted(baseModule) {
    baseModule.isExecuted = true;
    const modules = [baseModule];
    const visitedModules = new Set();
    for (const module of modules) {
        for (const dependency of module.dependencies) {
            if (!(dependency instanceof ExternalModule) &&
                !dependency.isExecuted &&
                dependency.moduleSideEffects &&
                !visitedModules.has(dependency.id)) {
                dependency.isExecuted = true;
                visitedModules.add(dependency.id);
                modules.push(dependency);
            }
        }
    }
}

// To avoid infinite recursions
const MAX_PATH_DEPTH = 7;
class LocalVariable extends Variable {
    constructor(name, declarator, init, context) {
        super(name);
        this.additionalInitializers = null;
        this.calledFromTryStatement = false;
        this.expressionsToBeDeoptimized = [];
        this.declarations = declarator ? [declarator] : [];
        this.init = init;
        this.deoptimizationTracker = context.deoptimizationTracker;
        this.module = context.module;
    }
    addDeclaration(identifier, init) {
        this.declarations.push(identifier);
        if (this.additionalInitializers === null) {
            this.additionalInitializers = this.init === null ? [] : [this.init];
            this.init = UNKNOWN_EXPRESSION;
            this.isReassigned = true;
        }
        if (init !== null) {
            this.additionalInitializers.push(init);
        }
    }
    consolidateInitializers() {
        if (this.additionalInitializers !== null) {
            for (const initializer of this.additionalInitializers) {
                initializer.deoptimizePath(UNKNOWN_PATH);
            }
            this.additionalInitializers = null;
        }
    }
    deoptimizePath(path) {
        if (path.length > MAX_PATH_DEPTH || this.isReassigned)
            return;
        const trackedEntities = this.deoptimizationTracker.getEntities(path);
        if (trackedEntities.has(this))
            return;
        trackedEntities.add(this);
        if (path.length === 0) {
            if (!this.isReassigned) {
                this.isReassigned = true;
                const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized;
                this.expressionsToBeDeoptimized = [];
                for (const expression of expressionsToBeDeoptimized) {
                    expression.deoptimizeCache();
                }
                if (this.init) {
                    this.init.deoptimizePath(UNKNOWN_PATH);
                }
            }
        }
        else if (this.init) {
            this.init.deoptimizePath(path);
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        if (this.isReassigned || !this.init || path.length > MAX_PATH_DEPTH) {
            return UnknownValue;
        }
        const trackedEntities = recursionTracker.getEntities(path);
        if (trackedEntities.has(this.init)) {
            return UnknownValue;
        }
        this.expressionsToBeDeoptimized.push(origin);
        trackedEntities.add(this.init);
        const value = this.init.getLiteralValueAtPath(path, recursionTracker, origin);
        trackedEntities.delete(this.init);
        return value;
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        if (this.isReassigned || !this.init || path.length > MAX_PATH_DEPTH) {
            return UNKNOWN_EXPRESSION;
        }
        const trackedEntities = recursionTracker.getEntities(path);
        if (trackedEntities.has(this.init)) {
            return UNKNOWN_EXPRESSION;
        }
        this.expressionsToBeDeoptimized.push(origin);
        trackedEntities.add(this.init);
        const value = this.init.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
        trackedEntities.delete(this.init);
        return value;
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (path.length === 0)
            return false;
        if (this.isReassigned || path.length > MAX_PATH_DEPTH)
            return true;
        const trackedExpressions = context.accessed.getEntities(path);
        if (trackedExpressions.has(this))
            return false;
        trackedExpressions.add(this);
        return (this.init && this.init.hasEffectsWhenAccessedAtPath(path, context));
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (this.included || path.length > MAX_PATH_DEPTH)
            return true;
        if (path.length === 0)
            return false;
        if (this.isReassigned)
            return true;
        const trackedExpressions = context.assigned.getEntities(path);
        if (trackedExpressions.has(this))
            return false;
        trackedExpressions.add(this);
        return (this.init && this.init.hasEffectsWhenAssignedAtPath(path, context));
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length > MAX_PATH_DEPTH || this.isReassigned)
            return true;
        const trackedExpressions = (callOptions.withNew
            ? context.instantiated
            : context.called).getEntities(path);
        if (trackedExpressions.has(this))
            return false;
        trackedExpressions.add(this);
        return (this.init && this.init.hasEffectsWhenCalledAtPath(path, callOptions, context));
    }
    include(context) {
        if (!this.included) {
            this.included = true;
            if (!this.module.isExecuted) {
                markModuleAndImpureDependenciesAsExecuted(this.module);
            }
            for (const declaration of this.declarations) {
                // If node is a default export, it can save a tree-shaking run to include the full declaration now
                if (!declaration.included)
                    declaration.include(context, false);
                let node = declaration.parent;
                while (!node.included) {
                    // We do not want to properly include parents in case they are part of a dead branch
                    // in which case .include() might pull in more dead code
                    node.included = true;
                    if (node.type === Program)
                        break;
                    node = node.parent;
                }
            }
        }
    }
    includeCallArguments(context, args) {
        if (this.isReassigned) {
            for (const arg of args) {
                arg.include(context, false);
            }
        }
        else if (this.init) {
            this.init.includeCallArguments(context, args);
        }
    }
    markCalledFromTryStatement() {
        this.calledFromTryStatement = true;
    }
}

class Scope {
    constructor() {
        this.children = [];
        this.variables = new Map();
    }
    addDeclaration(identifier, context, init = null, _isHoisted) {
        const name = identifier.name;
        let variable = this.variables.get(name);
        if (variable) {
            variable.addDeclaration(identifier, init);
        }
        else {
            variable = new LocalVariable(identifier.name, identifier, init || UNDEFINED_EXPRESSION, context);
            this.variables.set(name, variable);
        }
        return variable;
    }
    contains(name) {
        return this.variables.has(name);
    }
    findVariable(_name) {
        throw new Error('Internal Error: findVariable needs to be implemented by a subclass');
    }
}

class ChildScope extends Scope {
    constructor(parent) {
        super();
        this.accessedOutsideVariables = new Map();
        this.parent = parent;
        parent.children.push(this);
    }
    addAccessedDynamicImport(importExpression) {
        (this.accessedDynamicImports || (this.accessedDynamicImports = new Set())).add(importExpression);
        if (this.parent instanceof ChildScope) {
            this.parent.addAccessedDynamicImport(importExpression);
        }
    }
    addAccessedGlobalsByFormat(globalsByFormat) {
        const accessedGlobalVariablesByFormat = this.accessedGlobalVariablesByFormat || (this.accessedGlobalVariablesByFormat = new Map());
        for (const format of Object.keys(globalsByFormat)) {
            let accessedGlobalVariables = accessedGlobalVariablesByFormat.get(format);
            if (!accessedGlobalVariables) {
                accessedGlobalVariables = new Set();
                accessedGlobalVariablesByFormat.set(format, accessedGlobalVariables);
            }
            for (const name of globalsByFormat[format]) {
                accessedGlobalVariables.add(name);
            }
        }
        if (this.parent instanceof ChildScope) {
            this.parent.addAccessedGlobalsByFormat(globalsByFormat);
        }
    }
    addNamespaceMemberAccess(name, variable) {
        this.accessedOutsideVariables.set(name, variable);
        this.parent.addNamespaceMemberAccess(name, variable);
    }
    addReturnExpression(expression) {
        this.parent instanceof ChildScope && this.parent.addReturnExpression(expression);
    }
    addUsedOutsideNames(usedNames, format) {
        for (const variable of this.accessedOutsideVariables.values()) {
            if (variable.included) {
                usedNames.add(variable.getBaseVariableName());
                if (variable.exportName && format === 'system') {
                    usedNames.add('exports');
                }
            }
        }
        const accessedGlobalVariables = this.accessedGlobalVariablesByFormat && this.accessedGlobalVariablesByFormat.get(format);
        if (accessedGlobalVariables) {
            for (const name of accessedGlobalVariables) {
                usedNames.add(name);
            }
        }
    }
    contains(name) {
        return this.variables.has(name) || this.parent.contains(name);
    }
    deconflict(format) {
        const usedNames = new Set();
        this.addUsedOutsideNames(usedNames, format);
        if (this.accessedDynamicImports) {
            for (const importExpression of this.accessedDynamicImports) {
                if (importExpression.inlineNamespace) {
                    usedNames.add(importExpression.inlineNamespace.getBaseVariableName());
                }
            }
        }
        for (const [name, variable] of this.variables) {
            if (variable.included || variable.alwaysRendered) {
                variable.setSafeName(getSafeName(name, usedNames));
            }
        }
        for (const scope of this.children) {
            scope.deconflict(format);
        }
    }
    findLexicalBoundary() {
        return this.parent.findLexicalBoundary();
    }
    findVariable(name) {
        const knownVariable = this.variables.get(name) || this.accessedOutsideVariables.get(name);
        if (knownVariable) {
            return knownVariable;
        }
        const variable = this.parent.findVariable(name);
        this.accessedOutsideVariables.set(name, variable);
        return variable;
    }
}

function getLocator$1(source, options) {
    if (options === void 0) {
        options = {};
    }
    var offsetLine = options.offsetLine || 0;
    var offsetColumn = options.offsetColumn || 0;
    var originalLines = source.split('\n');
    var start = 0;
    var lineRanges = originalLines.map(function (line, i) {
        var end = start + line.length + 1;
        var range = { start: start, end: end, line: i };
        start = end;
        return range;
    });
    var i = 0;
    function rangeContains(range, index) {
        return range.start <= index && index < range.end;
    }
    function getLocation(range, index) {
        return { line: offsetLine + range.line, column: offsetColumn + index - range.start, character: index };
    }
    function locate(search, startIndex) {
        if (typeof search === 'string') {
            search = source.indexOf(search, startIndex || 0);
        }
        var range = lineRanges[i];
        var d = search >= range.end ? 1 : -1;
        while (range) {
            if (rangeContains(range, search))
                return getLocation(range, search);
            i += d;
            range = lineRanges[i];
        }
    }
    return locate;
}
function locate(source, search, options) {
    if (typeof options === 'number') {
        throw new Error('locate takes a { startIndex, offsetLine, offsetColumn } object as the third argument');
    }
    return getLocator$1(source, options)(search, options && options.startIndex);
}

const keys = {
    Literal: [],
    Program: ['body']
};
function getAndCreateKeys(esTreeNode) {
    keys[esTreeNode.type] = Object.keys(esTreeNode).filter(key => typeof esTreeNode[key] === 'object');
    return keys[esTreeNode.type];
}

const INCLUDE_PARAMETERS = 'variables';
class NodeBase {
    constructor(esTreeNode, parent, parentScope) {
        this.included = false;
        this.keys = keys[esTreeNode.type] || getAndCreateKeys(esTreeNode);
        this.parent = parent;
        this.context = parent.context;
        this.createScope(parentScope);
        this.parseNode(esTreeNode);
        this.initialise();
        this.context.magicString.addSourcemapLocation(this.start);
        this.context.magicString.addSourcemapLocation(this.end);
    }
    /**
     * Override this to bind assignments to variables and do any initialisations that
     * require the scopes to be populated with variables.
     */
    bind() {
        for (const key of this.keys) {
            const value = this[key];
            if (value === null || key === 'annotations')
                continue;
            if (Array.isArray(value)) {
                for (const child of value) {
                    if (child !== null)
                        child.bind();
                }
            }
            else {
                value.bind();
            }
        }
    }
    /**
     * Override if this node should receive a different scope than the parent scope.
     */
    createScope(parentScope) {
        this.scope = parentScope;
    }
    declare(_kind, _init) {
        return [];
    }
    deoptimizePath(_path) { }
    getLiteralValueAtPath(_path, _recursionTracker, _origin) {
        return UnknownValue;
    }
    getReturnExpressionWhenCalledAtPath(_path, _recursionTracker, _origin) {
        return UNKNOWN_EXPRESSION;
    }
    hasEffects(context) {
        for (const key of this.keys) {
            const value = this[key];
            if (value === null || key === 'annotations')
                continue;
            if (Array.isArray(value)) {
                for (const child of value) {
                    if (child !== null && child.hasEffects(context))
                        return true;
                }
            }
            else if (value.hasEffects(context))
                return true;
        }
        return false;
    }
    hasEffectsWhenAccessedAtPath(path, _context) {
        return path.length > 0;
    }
    hasEffectsWhenAssignedAtPath(_path, _context) {
        return true;
    }
    hasEffectsWhenCalledAtPath(_path, _callOptions, _context) {
        return true;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        for (const key of this.keys) {
            const value = this[key];
            if (value === null || key === 'annotations')
                continue;
            if (Array.isArray(value)) {
                for (const child of value) {
                    if (child !== null)
                        child.include(context, includeChildrenRecursively);
                }
            }
            else {
                value.include(context, includeChildrenRecursively);
            }
        }
    }
    includeCallArguments(context, args) {
        for (const arg of args) {
            arg.include(context, false);
        }
    }
    includeWithAllDeclaredVariables(includeChildrenRecursively, context) {
        this.include(context, includeChildrenRecursively);
    }
    /**
     * Override to perform special initialisation steps after the scope is initialised
     */
    initialise() { }
    insertSemicolon(code) {
        if (code.original[this.end - 1] !== ';') {
            code.appendLeft(this.end, ';');
        }
    }
    locate() {
        // useful for debugging
        const location = locate(this.context.code, this.start, { offsetLine: 1 });
        location.file = this.context.fileName;
        location.toString = () => JSON.stringify(location);
        return location;
    }
    parseNode(esTreeNode) {
        for (const key of Object.keys(esTreeNode)) {
            // That way, we can override this function to add custom initialisation and then call super.parseNode
            if (this.hasOwnProperty(key))
                continue;
            const value = esTreeNode[key];
            if (typeof value !== 'object' || value === null || key === 'annotations') {
                this[key] = value;
            }
            else if (Array.isArray(value)) {
                this[key] = [];
                for (const child of value) {
                    this[key].push(child === null
                        ? null
                        : new (this.context.nodeConstructors[child.type] ||
                            this.context.nodeConstructors.UnknownNode)(child, this, this.scope));
                }
            }
            else {
                this[key] = new (this.context.nodeConstructors[value.type] ||
                    this.context.nodeConstructors.UnknownNode)(value, this, this.scope);
            }
        }
    }
    render(code, options) {
        for (const key of this.keys) {
            const value = this[key];
            if (value === null || key === 'annotations')
                continue;
            if (Array.isArray(value)) {
                for (const child of value) {
                    if (child !== null)
                        child.render(code, options);
                }
            }
            else {
                value.render(code, options);
            }
        }
    }
    shouldBeIncluded(context) {
        return this.included || (!context.brokenFlow && this.hasEffects(createHasEffectsContext()));
    }
    toString() {
        return this.context.code.slice(this.start, this.end);
    }
}

class ClassNode extends NodeBase {
    createScope(parentScope) {
        this.scope = new ChildScope(parentScope);
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenAssignedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (!callOptions.withNew)
            return true;
        return (this.body.hasEffectsWhenCalledAtPath(path, callOptions, context) ||
            (this.superClass !== null &&
                this.superClass.hasEffectsWhenCalledAtPath(path, callOptions, context)));
    }
    initialise() {
        if (this.id !== null) {
            this.id.declare('class', this);
        }
    }
}

class ClassDeclaration extends ClassNode {
    initialise() {
        super.initialise();
        if (this.id !== null) {
            this.id.variable.isId = true;
        }
    }
    parseNode(esTreeNode) {
        if (esTreeNode.id !== null) {
            this.id = new this.context.nodeConstructors.Identifier(esTreeNode.id, this, this.scope
                .parent);
        }
        super.parseNode(esTreeNode);
    }
    render(code, options) {
        if (options.format === 'system' && this.id && this.id.variable.exportName) {
            code.appendLeft(this.end, ` exports('${this.id.variable.exportName}', ${this.id.variable.getName()});`);
        }
        super.render(code, options);
    }
}

class ArgumentsVariable extends LocalVariable {
    constructor(context) {
        super('arguments', null, UNKNOWN_EXPRESSION, context);
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenAssignedAtPath() {
        return true;
    }
    hasEffectsWhenCalledAtPath() {
        return true;
    }
}

class ThisVariable extends LocalVariable {
    constructor(context) {
        super('this', null, null, context);
    }
    getLiteralValueAtPath() {
        return UnknownValue;
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        return (this.getInit(context).hasEffectsWhenAccessedAtPath(path, context) ||
            super.hasEffectsWhenAccessedAtPath(path, context));
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        return (this.getInit(context).hasEffectsWhenAssignedAtPath(path, context) ||
            super.hasEffectsWhenAssignedAtPath(path, context));
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        return (this.getInit(context).hasEffectsWhenCalledAtPath(path, callOptions, context) ||
            super.hasEffectsWhenCalledAtPath(path, callOptions, context));
    }
    getInit(context) {
        return context.replacedVariableInits.get(this) || UNKNOWN_EXPRESSION;
    }
}

class ParameterScope extends ChildScope {
    constructor(parent, context) {
        super(parent);
        this.parameters = [];
        this.hasRest = false;
        this.context = context;
        this.hoistedBodyVarScope = new ChildScope(this);
    }
    /**
     * Adds a parameter to this scope. Parameters must be added in the correct
     * order, e.g. from left to right.
     */
    addParameterDeclaration(identifier) {
        const name = identifier.name;
        let variable = this.hoistedBodyVarScope.variables.get(name);
        if (variable) {
            variable.addDeclaration(identifier, null);
        }
        else {
            variable = new LocalVariable(name, identifier, UNKNOWN_EXPRESSION, this.context);
        }
        this.variables.set(name, variable);
        return variable;
    }
    addParameterVariables(parameters, hasRest) {
        this.parameters = parameters;
        for (const parameterList of parameters) {
            for (const parameter of parameterList) {
                parameter.alwaysRendered = true;
            }
        }
        this.hasRest = hasRest;
    }
    includeCallArguments(context, args) {
        let calledFromTryStatement = false;
        let argIncluded = false;
        const restParam = this.hasRest && this.parameters[this.parameters.length - 1];
        for (let index = args.length - 1; index >= 0; index--) {
            const paramVars = this.parameters[index] || restParam;
            const arg = args[index];
            if (paramVars) {
                calledFromTryStatement = false;
                for (const variable of paramVars) {
                    if (variable.included) {
                        argIncluded = true;
                    }
                    if (variable.calledFromTryStatement) {
                        calledFromTryStatement = true;
                    }
                }
            }
            if (!argIncluded && arg.shouldBeIncluded(context)) {
                argIncluded = true;
            }
            if (argIncluded) {
                arg.include(context, calledFromTryStatement);
            }
        }
    }
}

class ReturnValueScope extends ParameterScope {
    constructor() {
        super(...arguments);
        this.returnExpression = null;
        this.returnExpressions = [];
    }
    addReturnExpression(expression) {
        this.returnExpressions.push(expression);
    }
    getReturnExpression() {
        if (this.returnExpression === null)
            this.updateReturnExpression();
        return this.returnExpression;
    }
    updateReturnExpression() {
        if (this.returnExpressions.length === 1) {
            this.returnExpression = this.returnExpressions[0];
        }
        else {
            this.returnExpression = UNKNOWN_EXPRESSION;
            for (const expression of this.returnExpressions) {
                expression.deoptimizePath(UNKNOWN_PATH);
            }
        }
    }
}

class FunctionScope extends ReturnValueScope {
    constructor(parent, context) {
        super(parent, context);
        this.variables.set('arguments', (this.argumentsVariable = new ArgumentsVariable(context)));
        this.variables.set('this', (this.thisVariable = new ThisVariable(context)));
    }
    findLexicalBoundary() {
        return this;
    }
    includeCallArguments(context, args) {
        super.includeCallArguments(context, args);
        if (this.argumentsVariable.included) {
            for (const arg of args) {
                if (!arg.included) {
                    arg.include(context, false);
                }
            }
        }
    }
}

function isReference(node, parent) {
    if (node.type === 'MemberExpression') {
        return !node.computed && isReference(node.object, node);
    }
    if (node.type === 'Identifier') {
        if (!parent)
            return true;
        switch (parent.type) {
            // disregard `bar` in `foo.bar`
            case 'MemberExpression': return parent.computed || node === parent.object;
            // disregard the `foo` in `class {foo(){}}` but keep it in `class {[foo](){}}`
            case 'MethodDefinition': return parent.computed;
            // disregard the `bar` in `{ bar: foo }`, but keep it in `{ [bar]: foo }`
            case 'Property': return parent.computed || node === parent.value;
            // disregard the `bar` in `export { foo as bar }` or
            // the foo in `import { foo as bar }`
            case 'ExportSpecifier':
            case 'ImportSpecifier': return node === parent.local;
            // disregard the `foo` in `foo: while (...) { ... break foo; ... continue foo;}`
            case 'LabeledStatement':
            case 'BreakStatement':
            case 'ContinueStatement': return false;
            default: return true;
        }
    }
    return false;
}

const BLANK = Object.create(null);

const ValueProperties = Symbol('Value Properties');
const PURE = { pure: true };
const IMPURE = { pure: false };
// We use shortened variables to reduce file size here
/* OBJECT */
const O = {
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: IMPURE
};
/* PURE FUNCTION */
const PF = {
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: PURE
};
/* CONSTRUCTOR */
const C = {
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: IMPURE,
    prototype: O
};
/* PURE CONSTRUCTOR */
const PC = {
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: PURE,
    prototype: O
};
const ARRAY_TYPE = {
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: PURE,
    from: PF,
    of: PF,
    prototype: O
};
const INTL_MEMBER = {
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: PURE,
    supportedLocalesOf: PC
};
const knownGlobals = {
    // Placeholders for global objects to avoid shape mutations
    global: O,
    globalThis: O,
    self: O,
    window: O,
    // Common globals
    // @ts-ignore
    __proto__: null,
    [ValueProperties]: IMPURE,
    Array: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: IMPURE,
        from: PF,
        isArray: PF,
        of: PF,
        prototype: O
    },
    ArrayBuffer: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: PURE,
        isView: PF,
        prototype: O
    },
    Atomics: O,
    BigInt: C,
    BigInt64Array: C,
    BigUint64Array: C,
    Boolean: PC,
    // @ts-ignore
    constructor: C,
    DataView: PC,
    Date: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: PURE,
        now: PF,
        parse: PF,
        prototype: O,
        UTC: PF
    },
    decodeURI: PF,
    decodeURIComponent: PF,
    encodeURI: PF,
    encodeURIComponent: PF,
    Error: PC,
    escape: PF,
    eval: O,
    EvalError: PC,
    Float32Array: ARRAY_TYPE,
    Float64Array: ARRAY_TYPE,
    Function: C,
    // @ts-ignore
    hasOwnProperty: O,
    Infinity: O,
    Int16Array: ARRAY_TYPE,
    Int32Array: ARRAY_TYPE,
    Int8Array: ARRAY_TYPE,
    isFinite: PF,
    isNaN: PF,
    // @ts-ignore
    isPrototypeOf: O,
    JSON: O,
    Map: PC,
    Math: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: IMPURE,
        abs: PF,
        acos: PF,
        acosh: PF,
        asin: PF,
        asinh: PF,
        atan: PF,
        atan2: PF,
        atanh: PF,
        cbrt: PF,
        ceil: PF,
        clz32: PF,
        cos: PF,
        cosh: PF,
        exp: PF,
        expm1: PF,
        floor: PF,
        fround: PF,
        hypot: PF,
        imul: PF,
        log: PF,
        log10: PF,
        log1p: PF,
        log2: PF,
        max: PF,
        min: PF,
        pow: PF,
        random: PF,
        round: PF,
        sign: PF,
        sin: PF,
        sinh: PF,
        sqrt: PF,
        tan: PF,
        tanh: PF,
        trunc: PF
    },
    NaN: O,
    Number: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: PURE,
        isFinite: PF,
        isInteger: PF,
        isNaN: PF,
        isSafeInteger: PF,
        parseFloat: PF,
        parseInt: PF,
        prototype: O
    },
    Object: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: PURE,
        create: PF,
        getNotifier: PF,
        getOwn: PF,
        getOwnPropertyDescriptor: PF,
        getOwnPropertyNames: PF,
        getOwnPropertySymbols: PF,
        getPrototypeOf: PF,
        is: PF,
        isExtensible: PF,
        isFrozen: PF,
        isSealed: PF,
        keys: PF,
        prototype: O
    },
    parseFloat: PF,
    parseInt: PF,
    Promise: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: IMPURE,
        all: PF,
        prototype: O,
        race: PF,
        resolve: PF
    },
    // @ts-ignore
    propertyIsEnumerable: O,
    Proxy: O,
    RangeError: PC,
    ReferenceError: PC,
    Reflect: O,
    RegExp: PC,
    Set: PC,
    SharedArrayBuffer: C,
    String: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: PURE,
        fromCharCode: PF,
        fromCodePoint: PF,
        prototype: O,
        raw: PF
    },
    Symbol: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: PURE,
        for: PF,
        keyFor: PF,
        prototype: O
    },
    SyntaxError: PC,
    // @ts-ignore
    toLocaleString: O,
    // @ts-ignore
    toString: O,
    TypeError: PC,
    Uint16Array: ARRAY_TYPE,
    Uint32Array: ARRAY_TYPE,
    Uint8Array: ARRAY_TYPE,
    Uint8ClampedArray: ARRAY_TYPE,
    // Technically, this is a global, but it needs special handling
    // undefined: ?,
    unescape: PF,
    URIError: PC,
    // @ts-ignore
    valueOf: O,
    WeakMap: PC,
    WeakSet: PC,
    // Additional globals shared by Node and Browser that are not strictly part of the language
    clearInterval: C,
    clearTimeout: C,
    console: O,
    Intl: {
        // @ts-ignore
        __proto__: null,
        [ValueProperties]: IMPURE,
        Collator: INTL_MEMBER,
        DateTimeFormat: INTL_MEMBER,
        ListFormat: INTL_MEMBER,
        NumberFormat: INTL_MEMBER,
        PluralRules: INTL_MEMBER,
        RelativeTimeFormat: INTL_MEMBER
    },
    setInterval: C,
    setTimeout: C,
    TextDecoder: C,
    TextEncoder: C,
    URL: C,
    URLSearchParams: C,
    // Browser specific globals
    AbortController: C,
    AbortSignal: C,
    addEventListener: O,
    alert: O,
    AnalyserNode: C,
    Animation: C,
    AnimationEvent: C,
    applicationCache: O,
    ApplicationCache: C,
    ApplicationCacheErrorEvent: C,
    atob: O,
    Attr: C,
    Audio: C,
    AudioBuffer: C,
    AudioBufferSourceNode: C,
    AudioContext: C,
    AudioDestinationNode: C,
    AudioListener: C,
    AudioNode: C,
    AudioParam: C,
    AudioProcessingEvent: C,
    AudioScheduledSourceNode: C,
    AudioWorkletNode: C,
    BarProp: C,
    BaseAudioContext: C,
    BatteryManager: C,
    BeforeUnloadEvent: C,
    BiquadFilterNode: C,
    Blob: C,
    BlobEvent: C,
    blur: O,
    BroadcastChannel: C,
    btoa: O,
    ByteLengthQueuingStrategy: C,
    Cache: C,
    caches: O,
    CacheStorage: C,
    cancelAnimationFrame: O,
    cancelIdleCallback: O,
    CanvasCaptureMediaStreamTrack: C,
    CanvasGradient: C,
    CanvasPattern: C,
    CanvasRenderingContext2D: C,
    ChannelMergerNode: C,
    ChannelSplitterNode: C,
    CharacterData: C,
    clientInformation: O,
    ClipboardEvent: C,
    close: O,
    closed: O,
    CloseEvent: C,
    Comment: C,
    CompositionEvent: C,
    confirm: O,
    ConstantSourceNode: C,
    ConvolverNode: C,
    CountQueuingStrategy: C,
    createImageBitmap: O,
    Credential: C,
    CredentialsContainer: C,
    crypto: O,
    Crypto: C,
    CryptoKey: C,
    CSS: C,
    CSSConditionRule: C,
    CSSFontFaceRule: C,
    CSSGroupingRule: C,
    CSSImportRule: C,
    CSSKeyframeRule: C,
    CSSKeyframesRule: C,
    CSSMediaRule: C,
    CSSNamespaceRule: C,
    CSSPageRule: C,
    CSSRule: C,
    CSSRuleList: C,
    CSSStyleDeclaration: C,
    CSSStyleRule: C,
    CSSStyleSheet: C,
    CSSSupportsRule: C,
    CustomElementRegistry: C,
    customElements: O,
    CustomEvent: C,
    DataTransfer: C,
    DataTransferItem: C,
    DataTransferItemList: C,
    defaultstatus: O,
    defaultStatus: O,
    DelayNode: C,
    DeviceMotionEvent: C,
    DeviceOrientationEvent: C,
    devicePixelRatio: O,
    dispatchEvent: O,
    document: O,
    Document: C,
    DocumentFragment: C,
    DocumentType: C,
    DOMError: C,
    DOMException: C,
    DOMImplementation: C,
    DOMMatrix: C,
    DOMMatrixReadOnly: C,
    DOMParser: C,
    DOMPoint: C,
    DOMPointReadOnly: C,
    DOMQuad: C,
    DOMRect: C,
    DOMRectReadOnly: C,
    DOMStringList: C,
    DOMStringMap: C,
    DOMTokenList: C,
    DragEvent: C,
    DynamicsCompressorNode: C,
    Element: C,
    ErrorEvent: C,
    Event: C,
    EventSource: C,
    EventTarget: C,
    external: O,
    fetch: O,
    File: C,
    FileList: C,
    FileReader: C,
    find: O,
    focus: O,
    FocusEvent: C,
    FontFace: C,
    FontFaceSetLoadEvent: C,
    FormData: C,
    frames: O,
    GainNode: C,
    Gamepad: C,
    GamepadButton: C,
    GamepadEvent: C,
    getComputedStyle: O,
    getSelection: O,
    HashChangeEvent: C,
    Headers: C,
    history: O,
    History: C,
    HTMLAllCollection: C,
    HTMLAnchorElement: C,
    HTMLAreaElement: C,
    HTMLAudioElement: C,
    HTMLBaseElement: C,
    HTMLBodyElement: C,
    HTMLBRElement: C,
    HTMLButtonElement: C,
    HTMLCanvasElement: C,
    HTMLCollection: C,
    HTMLContentElement: C,
    HTMLDataElement: C,
    HTMLDataListElement: C,
    HTMLDetailsElement: C,
    HTMLDialogElement: C,
    HTMLDirectoryElement: C,
    HTMLDivElement: C,
    HTMLDListElement: C,
    HTMLDocument: C,
    HTMLElement: C,
    HTMLEmbedElement: C,
    HTMLFieldSetElement: C,
    HTMLFontElement: C,
    HTMLFormControlsCollection: C,
    HTMLFormElement: C,
    HTMLFrameElement: C,
    HTMLFrameSetElement: C,
    HTMLHeadElement: C,
    HTMLHeadingElement: C,
    HTMLHRElement: C,
    HTMLHtmlElement: C,
    HTMLIFrameElement: C,
    HTMLImageElement: C,
    HTMLInputElement: C,
    HTMLLabelElement: C,
    HTMLLegendElement: C,
    HTMLLIElement: C,
    HTMLLinkElement: C,
    HTMLMapElement: C,
    HTMLMarqueeElement: C,
    HTMLMediaElement: C,
    HTMLMenuElement: C,
    HTMLMetaElement: C,
    HTMLMeterElement: C,
    HTMLModElement: C,
    HTMLObjectElement: C,
    HTMLOListElement: C,
    HTMLOptGroupElement: C,
    HTMLOptionElement: C,
    HTMLOptionsCollection: C,
    HTMLOutputElement: C,
    HTMLParagraphElement: C,
    HTMLParamElement: C,
    HTMLPictureElement: C,
    HTMLPreElement: C,
    HTMLProgressElement: C,
    HTMLQuoteElement: C,
    HTMLScriptElement: C,
    HTMLSelectElement: C,
    HTMLShadowElement: C,
    HTMLSlotElement: C,
    HTMLSourceElement: C,
    HTMLSpanElement: C,
    HTMLStyleElement: C,
    HTMLTableCaptionElement: C,
    HTMLTableCellElement: C,
    HTMLTableColElement: C,
    HTMLTableElement: C,
    HTMLTableRowElement: C,
    HTMLTableSectionElement: C,
    HTMLTemplateElement: C,
    HTMLTextAreaElement: C,
    HTMLTimeElement: C,
    HTMLTitleElement: C,
    HTMLTrackElement: C,
    HTMLUListElement: C,
    HTMLUnknownElement: C,
    HTMLVideoElement: C,
    IDBCursor: C,
    IDBCursorWithValue: C,
    IDBDatabase: C,
    IDBFactory: C,
    IDBIndex: C,
    IDBKeyRange: C,
    IDBObjectStore: C,
    IDBOpenDBRequest: C,
    IDBRequest: C,
    IDBTransaction: C,
    IDBVersionChangeEvent: C,
    IdleDeadline: C,
    IIRFilterNode: C,
    Image: C,
    ImageBitmap: C,
    ImageBitmapRenderingContext: C,
    ImageCapture: C,
    ImageData: C,
    indexedDB: O,
    innerHeight: O,
    innerWidth: O,
    InputEvent: C,
    IntersectionObserver: C,
    IntersectionObserverEntry: C,
    isSecureContext: O,
    KeyboardEvent: C,
    KeyframeEffect: C,
    length: O,
    localStorage: O,
    location: O,
    Location: C,
    locationbar: O,
    matchMedia: O,
    MediaDeviceInfo: C,
    MediaDevices: C,
    MediaElementAudioSourceNode: C,
    MediaEncryptedEvent: C,
    MediaError: C,
    MediaKeyMessageEvent: C,
    MediaKeySession: C,
    MediaKeyStatusMap: C,
    MediaKeySystemAccess: C,
    MediaList: C,
    MediaQueryList: C,
    MediaQueryListEvent: C,
    MediaRecorder: C,
    MediaSettingsRange: C,
    MediaSource: C,
    MediaStream: C,
    MediaStreamAudioDestinationNode: C,
    MediaStreamAudioSourceNode: C,
    MediaStreamEvent: C,
    MediaStreamTrack: C,
    MediaStreamTrackEvent: C,
    menubar: O,
    MessageChannel: C,
    MessageEvent: C,
    MessagePort: C,
    MIDIAccess: C,
    MIDIConnectionEvent: C,
    MIDIInput: C,
    MIDIInputMap: C,
    MIDIMessageEvent: C,
    MIDIOutput: C,
    MIDIOutputMap: C,
    MIDIPort: C,
    MimeType: C,
    MimeTypeArray: C,
    MouseEvent: C,
    moveBy: O,
    moveTo: O,
    MutationEvent: C,
    MutationObserver: C,
    MutationRecord: C,
    name: O,
    NamedNodeMap: C,
    NavigationPreloadManager: C,
    navigator: O,
    Navigator: C,
    NetworkInformation: C,
    Node: C,
    NodeFilter: O,
    NodeIterator: C,
    NodeList: C,
    Notification: C,
    OfflineAudioCompletionEvent: C,
    OfflineAudioContext: C,
    offscreenBuffering: O,
    OffscreenCanvas: C,
    open: O,
    openDatabase: O,
    Option: C,
    origin: O,
    OscillatorNode: C,
    outerHeight: O,
    outerWidth: O,
    PageTransitionEvent: C,
    pageXOffset: O,
    pageYOffset: O,
    PannerNode: C,
    parent: O,
    Path2D: C,
    PaymentAddress: C,
    PaymentRequest: C,
    PaymentRequestUpdateEvent: C,
    PaymentResponse: C,
    performance: O,
    Performance: C,
    PerformanceEntry: C,
    PerformanceLongTaskTiming: C,
    PerformanceMark: C,
    PerformanceMeasure: C,
    PerformanceNavigation: C,
    PerformanceNavigationTiming: C,
    PerformanceObserver: C,
    PerformanceObserverEntryList: C,
    PerformancePaintTiming: C,
    PerformanceResourceTiming: C,
    PerformanceTiming: C,
    PeriodicWave: C,
    Permissions: C,
    PermissionStatus: C,
    personalbar: O,
    PhotoCapabilities: C,
    Plugin: C,
    PluginArray: C,
    PointerEvent: C,
    PopStateEvent: C,
    postMessage: O,
    Presentation: C,
    PresentationAvailability: C,
    PresentationConnection: C,
    PresentationConnectionAvailableEvent: C,
    PresentationConnectionCloseEvent: C,
    PresentationConnectionList: C,
    PresentationReceiver: C,
    PresentationRequest: C,
    print: O,
    ProcessingInstruction: C,
    ProgressEvent: C,
    PromiseRejectionEvent: C,
    prompt: O,
    PushManager: C,
    PushSubscription: C,
    PushSubscriptionOptions: C,
    queueMicrotask: O,
    RadioNodeList: C,
    Range: C,
    ReadableStream: C,
    RemotePlayback: C,
    removeEventListener: O,
    Request: C,
    requestAnimationFrame: O,
    requestIdleCallback: O,
    resizeBy: O,
    ResizeObserver: C,
    ResizeObserverEntry: C,
    resizeTo: O,
    Response: C,
    RTCCertificate: C,
    RTCDataChannel: C,
    RTCDataChannelEvent: C,
    RTCDtlsTransport: C,
    RTCIceCandidate: C,
    RTCIceTransport: C,
    RTCPeerConnection: C,
    RTCPeerConnectionIceEvent: C,
    RTCRtpReceiver: C,
    RTCRtpSender: C,
    RTCSctpTransport: C,
    RTCSessionDescription: C,
    RTCStatsReport: C,
    RTCTrackEvent: C,
    screen: O,
    Screen: C,
    screenLeft: O,
    ScreenOrientation: C,
    screenTop: O,
    screenX: O,
    screenY: O,
    ScriptProcessorNode: C,
    scroll: O,
    scrollbars: O,
    scrollBy: O,
    scrollTo: O,
    scrollX: O,
    scrollY: O,
    SecurityPolicyViolationEvent: C,
    Selection: C,
    ServiceWorker: C,
    ServiceWorkerContainer: C,
    ServiceWorkerRegistration: C,
    sessionStorage: O,
    ShadowRoot: C,
    SharedWorker: C,
    SourceBuffer: C,
    SourceBufferList: C,
    speechSynthesis: O,
    SpeechSynthesisEvent: C,
    SpeechSynthesisUtterance: C,
    StaticRange: C,
    status: O,
    statusbar: O,
    StereoPannerNode: C,
    stop: O,
    Storage: C,
    StorageEvent: C,
    StorageManager: C,
    styleMedia: O,
    StyleSheet: C,
    StyleSheetList: C,
    SubtleCrypto: C,
    SVGAElement: C,
    SVGAngle: C,
    SVGAnimatedAngle: C,
    SVGAnimatedBoolean: C,
    SVGAnimatedEnumeration: C,
    SVGAnimatedInteger: C,
    SVGAnimatedLength: C,
    SVGAnimatedLengthList: C,
    SVGAnimatedNumber: C,
    SVGAnimatedNumberList: C,
    SVGAnimatedPreserveAspectRatio: C,
    SVGAnimatedRect: C,
    SVGAnimatedString: C,
    SVGAnimatedTransformList: C,
    SVGAnimateElement: C,
    SVGAnimateMotionElement: C,
    SVGAnimateTransformElement: C,
    SVGAnimationElement: C,
    SVGCircleElement: C,
    SVGClipPathElement: C,
    SVGComponentTransferFunctionElement: C,
    SVGDefsElement: C,
    SVGDescElement: C,
    SVGDiscardElement: C,
    SVGElement: C,
    SVGEllipseElement: C,
    SVGFEBlendElement: C,
    SVGFEColorMatrixElement: C,
    SVGFEComponentTransferElement: C,
    SVGFECompositeElement: C,
    SVGFEConvolveMatrixElement: C,
    SVGFEDiffuseLightingElement: C,
    SVGFEDisplacementMapElement: C,
    SVGFEDistantLightElement: C,
    SVGFEDropShadowElement: C,
    SVGFEFloodElement: C,
    SVGFEFuncAElement: C,
    SVGFEFuncBElement: C,
    SVGFEFuncGElement: C,
    SVGFEFuncRElement: C,
    SVGFEGaussianBlurElement: C,
    SVGFEImageElement: C,
    SVGFEMergeElement: C,
    SVGFEMergeNodeElement: C,
    SVGFEMorphologyElement: C,
    SVGFEOffsetElement: C,
    SVGFEPointLightElement: C,
    SVGFESpecularLightingElement: C,
    SVGFESpotLightElement: C,
    SVGFETileElement: C,
    SVGFETurbulenceElement: C,
    SVGFilterElement: C,
    SVGForeignObjectElement: C,
    SVGGElement: C,
    SVGGeometryElement: C,
    SVGGradientElement: C,
    SVGGraphicsElement: C,
    SVGImageElement: C,
    SVGLength: C,
    SVGLengthList: C,
    SVGLinearGradientElement: C,
    SVGLineElement: C,
    SVGMarkerElement: C,
    SVGMaskElement: C,
    SVGMatrix: C,
    SVGMetadataElement: C,
    SVGMPathElement: C,
    SVGNumber: C,
    SVGNumberList: C,
    SVGPathElement: C,
    SVGPatternElement: C,
    SVGPoint: C,
    SVGPointList: C,
    SVGPolygonElement: C,
    SVGPolylineElement: C,
    SVGPreserveAspectRatio: C,
    SVGRadialGradientElement: C,
    SVGRect: C,
    SVGRectElement: C,
    SVGScriptElement: C,
    SVGSetElement: C,
    SVGStopElement: C,
    SVGStringList: C,
    SVGStyleElement: C,
    SVGSVGElement: C,
    SVGSwitchElement: C,
    SVGSymbolElement: C,
    SVGTextContentElement: C,
    SVGTextElement: C,
    SVGTextPathElement: C,
    SVGTextPositioningElement: C,
    SVGTitleElement: C,
    SVGTransform: C,
    SVGTransformList: C,
    SVGTSpanElement: C,
    SVGUnitTypes: C,
    SVGUseElement: C,
    SVGViewElement: C,
    TaskAttributionTiming: C,
    Text: C,
    TextEvent: C,
    TextMetrics: C,
    TextTrack: C,
    TextTrackCue: C,
    TextTrackCueList: C,
    TextTrackList: C,
    TimeRanges: C,
    toolbar: O,
    top: O,
    Touch: C,
    TouchEvent: C,
    TouchList: C,
    TrackEvent: C,
    TransitionEvent: C,
    TreeWalker: C,
    UIEvent: C,
    ValidityState: C,
    visualViewport: O,
    VisualViewport: C,
    VTTCue: C,
    WaveShaperNode: C,
    WebAssembly: O,
    WebGL2RenderingContext: C,
    WebGLActiveInfo: C,
    WebGLBuffer: C,
    WebGLContextEvent: C,
    WebGLFramebuffer: C,
    WebGLProgram: C,
    WebGLQuery: C,
    WebGLRenderbuffer: C,
    WebGLRenderingContext: C,
    WebGLSampler: C,
    WebGLShader: C,
    WebGLShaderPrecisionFormat: C,
    WebGLSync: C,
    WebGLTexture: C,
    WebGLTransformFeedback: C,
    WebGLUniformLocation: C,
    WebGLVertexArrayObject: C,
    WebSocket: C,
    WheelEvent: C,
    Window: C,
    Worker: C,
    WritableStream: C,
    XMLDocument: C,
    XMLHttpRequest: C,
    XMLHttpRequestEventTarget: C,
    XMLHttpRequestUpload: C,
    XMLSerializer: C,
    XPathEvaluator: C,
    XPathExpression: C,
    XPathResult: C,
    XSLTProcessor: C
};
for (const global of ['window', 'global', 'self', 'globalThis']) {
    knownGlobals[global] = knownGlobals;
}
function getGlobalAtPath(path) {
    let currentGlobal = knownGlobals;
    for (const pathSegment of path) {
        if (typeof pathSegment !== 'string') {
            return null;
        }
        currentGlobal = currentGlobal[pathSegment];
        if (!currentGlobal) {
            return null;
        }
    }
    return currentGlobal[ValueProperties];
}
function isPureGlobal(path) {
    const globalAtPath = getGlobalAtPath(path);
    return globalAtPath !== null && globalAtPath.pure;
}
function isGlobalMember(path) {
    if (path.length === 1) {
        return path[0] === 'undefined' || getGlobalAtPath(path) !== null;
    }
    return getGlobalAtPath(path.slice(0, -1)) !== null;
}

class GlobalVariable extends Variable {
    hasEffectsWhenAccessedAtPath(path) {
        return !isGlobalMember([this.name, ...path]);
    }
    hasEffectsWhenCalledAtPath(path) {
        return !isPureGlobal([this.name, ...path]);
    }
}

class Identifier$1 extends NodeBase {
    constructor() {
        super(...arguments);
        this.variable = null;
        this.bound = false;
    }
    addExportedVariables(variables) {
        if (this.variable !== null && this.variable.exportName) {
            variables.push(this.variable);
        }
    }
    bind() {
        if (this.bound)
            return;
        this.bound = true;
        if (this.variable === null && isReference(this, this.parent)) {
            this.variable = this.scope.findVariable(this.name);
            this.variable.addReference(this);
        }
        if (this.variable !== null &&
            this.variable instanceof LocalVariable &&
            this.variable.additionalInitializers !== null) {
            this.variable.consolidateInitializers();
        }
    }
    declare(kind, init) {
        let variable;
        switch (kind) {
            case 'var':
                variable = this.scope.addDeclaration(this, this.context, init, true);
                break;
            case 'function':
                variable = this.scope.addDeclaration(this, this.context, init, 'function');
                break;
            case 'let':
            case 'const':
            case 'class':
                variable = this.scope.addDeclaration(this, this.context, init, false);
                break;
            case 'parameter':
                variable = this.scope.addParameterDeclaration(this);
                break;
            /* istanbul ignore next */
            default:
                /* istanbul ignore next */
                throw new Error(`Internal Error: Unexpected identifier kind ${kind}.`);
        }
        return [(this.variable = variable)];
    }
    deoptimizePath(path) {
        if (!this.bound)
            this.bind();
        if (path.length === 0 && !this.scope.contains(this.name)) {
            this.disallowImportReassignment();
        }
        this.variable.deoptimizePath(path);
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        if (!this.bound)
            this.bind();
        return this.variable.getLiteralValueAtPath(path, recursionTracker, origin);
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        if (!this.bound)
            this.bind();
        return this.variable.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
    }
    hasEffects() {
        return (this.context.unknownGlobalSideEffects &&
            this.variable instanceof GlobalVariable &&
            this.variable.hasEffectsWhenAccessedAtPath(EMPTY_PATH));
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        return this.variable !== null && this.variable.hasEffectsWhenAccessedAtPath(path, context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        return !this.variable || this.variable.hasEffectsWhenAssignedAtPath(path, context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        return !this.variable || this.variable.hasEffectsWhenCalledAtPath(path, callOptions, context);
    }
    include(context) {
        if (!this.included) {
            this.included = true;
            if (this.variable !== null) {
                this.context.includeVariable(context, this.variable);
            }
        }
    }
    includeCallArguments(context, args) {
        this.variable.includeCallArguments(context, args);
    }
    render(code, _options, { renderedParentType, isCalleeOfRenderedParent, isShorthandProperty } = BLANK) {
        if (this.variable) {
            const name = this.variable.getName();
            if (name !== this.name) {
                code.overwrite(this.start, this.end, name, {
                    contentOnly: true,
                    storeName: true
                });
                if (isShorthandProperty) {
                    code.prependRight(this.start, `${this.name}: `);
                }
            }
            // In strict mode, any variable named "eval" must be the actual "eval" function
            if (name === 'eval' &&
                renderedParentType === CallExpression &&
                isCalleeOfRenderedParent) {
                code.appendRight(this.start, '0, ');
            }
        }
    }
    disallowImportReassignment() {
        return this.context.error({
            code: 'ILLEGAL_REASSIGNMENT',
            message: `Illegal reassignment to import '${this.name}'`
        }, this.start);
    }
}

class RestElement extends NodeBase {
    constructor() {
        super(...arguments);
        this.declarationInit = null;
    }
    addExportedVariables(variables) {
        this.argument.addExportedVariables(variables);
    }
    bind() {
        super.bind();
        if (this.declarationInit !== null) {
            this.declarationInit.deoptimizePath([UnknownKey, UnknownKey]);
        }
    }
    declare(kind, init) {
        this.declarationInit = init;
        return this.argument.declare(kind, UNKNOWN_EXPRESSION);
    }
    deoptimizePath(path) {
        path.length === 0 && this.argument.deoptimizePath(EMPTY_PATH);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        return path.length > 0 || this.argument.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context);
    }
}

class FunctionNode extends NodeBase {
    constructor() {
        super(...arguments);
        this.isPrototypeDeoptimized = false;
    }
    createScope(parentScope) {
        this.scope = new FunctionScope(parentScope, this.context);
    }
    deoptimizePath(path) {
        if (path.length === 1) {
            if (path[0] === 'prototype') {
                this.isPrototypeDeoptimized = true;
            }
            else if (path[0] === UnknownKey) {
                this.isPrototypeDeoptimized = true;
                // A reassignment of UNKNOWN_PATH is considered equivalent to having lost track
                // which means the return expression needs to be reassigned as well
                this.scope.getReturnExpression().deoptimizePath(UNKNOWN_PATH);
            }
        }
    }
    getReturnExpressionWhenCalledAtPath(path) {
        return path.length === 0 ? this.scope.getReturnExpression() : UNKNOWN_EXPRESSION;
    }
    hasEffects() {
        return this.id !== null && this.id.hasEffects();
    }
    hasEffectsWhenAccessedAtPath(path) {
        if (path.length <= 1)
            return false;
        return path.length > 2 || path[0] !== 'prototype' || this.isPrototypeDeoptimized;
    }
    hasEffectsWhenAssignedAtPath(path) {
        if (path.length <= 1) {
            return false;
        }
        return path.length > 2 || path[0] !== 'prototype' || this.isPrototypeDeoptimized;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length > 0)
            return true;
        for (const param of this.params) {
            if (param.hasEffects(context))
                return true;
        }
        const thisInit = context.replacedVariableInits.get(this.scope.thisVariable);
        context.replacedVariableInits.set(this.scope.thisVariable, callOptions.withNew ? new UnknownObjectExpression() : UNKNOWN_EXPRESSION);
        const { brokenFlow, ignore } = context;
        context.ignore = {
            breaks: false,
            continues: false,
            labels: new Set(),
            returnAwaitYield: true
        };
        if (this.body.hasEffects(context))
            return true;
        context.brokenFlow = brokenFlow;
        if (thisInit) {
            context.replacedVariableInits.set(this.scope.thisVariable, thisInit);
        }
        else {
            context.replacedVariableInits.delete(this.scope.thisVariable);
        }
        context.ignore = ignore;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (this.id)
            this.id.include(context);
        const hasArguments = this.scope.argumentsVariable.included;
        for (const param of this.params) {
            if (!(param instanceof Identifier$1) || hasArguments) {
                param.include(context, includeChildrenRecursively);
            }
        }
        const { brokenFlow } = context;
        context.brokenFlow = BROKEN_FLOW_NONE;
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
    includeCallArguments(context, args) {
        this.scope.includeCallArguments(context, args);
    }
    initialise() {
        if (this.id !== null) {
            this.id.declare('function', this);
        }
        this.scope.addParameterVariables(this.params.map(param => param.declare('parameter', UNKNOWN_EXPRESSION)), this.params[this.params.length - 1] instanceof RestElement);
        this.body.addImplicitReturnExpressionToScope();
    }
    parseNode(esTreeNode) {
        this.body = new this.context.nodeConstructors.BlockStatement(esTreeNode.body, this, this.scope.hoistedBodyVarScope);
        super.parseNode(esTreeNode);
    }
}
FunctionNode.prototype.preventChildBlockScope = true;

class FunctionDeclaration extends FunctionNode {
    initialise() {
        super.initialise();
        if (this.id !== null) {
            this.id.variable.isId = true;
        }
    }
    parseNode(esTreeNode) {
        if (esTreeNode.id !== null) {
            this.id = new this.context.nodeConstructors.Identifier(esTreeNode.id, this, this.scope
                .parent);
        }
        super.parseNode(esTreeNode);
    }
}

const WHITESPACE = /\s/;
// The header ends at the first non-white-space after "default"
function getDeclarationStart(code, start = 0) {
    start = findFirstOccurrenceOutsideComment(code, 'default', start) + 7;
    while (WHITESPACE.test(code[start]))
        start++;
    return start;
}
function getIdInsertPosition(code, declarationKeyword, start = 0) {
    const declarationEnd = findFirstOccurrenceOutsideComment(code, declarationKeyword, start) + declarationKeyword.length;
    code = code.slice(declarationEnd, findFirstOccurrenceOutsideComment(code, '{', declarationEnd));
    const generatorStarPos = findFirstOccurrenceOutsideComment(code, '*');
    if (generatorStarPos === -1) {
        return declarationEnd;
    }
    return declarationEnd + generatorStarPos + 1;
}
class ExportDefaultDeclaration extends NodeBase {
    include(context, includeChildrenRecursively) {
        super.include(context, includeChildrenRecursively);
        if (includeChildrenRecursively) {
            this.context.includeVariable(context, this.variable);
        }
    }
    initialise() {
        const declaration = this.declaration;
        this.declarationName =
            (declaration.id && declaration.id.name) || this.declaration.name;
        this.variable = this.scope.addExportDefaultDeclaration(this.declarationName || this.context.getModuleName(), this, this.context);
        this.context.addExport(this);
    }
    render(code, options, nodeRenderOptions) {
        const { start, end } = nodeRenderOptions;
        const declarationStart = getDeclarationStart(code.original, this.start);
        if (this.declaration instanceof FunctionDeclaration) {
            this.renderNamedDeclaration(code, declarationStart, 'function', this.declaration.id === null, options);
        }
        else if (this.declaration instanceof ClassDeclaration) {
            this.renderNamedDeclaration(code, declarationStart, 'class', this.declaration.id === null, options);
        }
        else if (this.variable.getOriginalVariable() !== this.variable) {
            // Remove altogether to prevent re-declaring the same variable
            if (options.format === 'system' && this.variable.exportName) {
                code.overwrite(start, end, `exports('${this.variable.exportName}', ${this.variable.getName()});`);
            }
            else {
                treeshakeNode(this, code, start, end);
            }
            return;
        }
        else if (this.variable.included) {
            this.renderVariableDeclaration(code, declarationStart, options);
        }
        else {
            code.remove(this.start, declarationStart);
            this.declaration.render(code, options, {
                isCalleeOfRenderedParent: false,
                renderedParentType: ExpressionStatement
            });
            if (code.original[this.end - 1] !== ';') {
                code.appendLeft(this.end, ';');
            }
            return;
        }
        this.declaration.render(code, options);
    }
    renderNamedDeclaration(code, declarationStart, declarationKeyword, needsId, options) {
        const name = this.variable.getName();
        // Remove `export default`
        code.remove(this.start, declarationStart);
        if (needsId) {
            code.appendLeft(getIdInsertPosition(code.original, declarationKeyword, declarationStart), ` ${name}`);
        }
        if (options.format === 'system' &&
            this.declaration instanceof ClassDeclaration &&
            this.variable.exportName) {
            code.appendLeft(this.end, ` exports('${this.variable.exportName}', ${name});`);
        }
    }
    renderVariableDeclaration(code, declarationStart, options) {
        const systemBinding = options.format === 'system' && this.variable.exportName
            ? `exports('${this.variable.exportName}', `
            : '';
        code.overwrite(this.start, declarationStart, `${options.varOrConst} ${this.variable.getName()} = ${systemBinding}`);
        const hasTrailingSemicolon = code.original.charCodeAt(this.end - 1) === 59; /*";"*/
        if (systemBinding) {
            code.appendRight(hasTrailingSemicolon ? this.end - 1 : this.end, ')' + (hasTrailingSemicolon ? '' : ';'));
        }
        else if (!hasTrailingSemicolon) {
            code.appendLeft(this.end, ';');
        }
    }
}
ExportDefaultDeclaration.prototype.needsBoundaries = true;

class ExportDefaultVariable extends LocalVariable {
    constructor(name, exportDefaultDeclaration, context) {
        super(name, exportDefaultDeclaration, exportDefaultDeclaration.declaration, context);
        this.hasId = false;
        // Not initialised during construction
        this.originalId = null;
        this.originalVariable = null;
        const declaration = exportDefaultDeclaration.declaration;
        if ((declaration instanceof FunctionDeclaration || declaration instanceof ClassDeclaration) &&
            declaration.id) {
            this.hasId = true;
            this.originalId = declaration.id;
        }
        else if (declaration instanceof Identifier$1) {
            this.originalId = declaration;
        }
    }
    addReference(identifier) {
        if (!this.hasId) {
            this.name = identifier.name;
        }
    }
    getAssignedVariableName() {
        return (this.originalId && this.originalId.name) || null;
    }
    getBaseVariableName() {
        const original = this.getOriginalVariable();
        if (original === this) {
            return super.getBaseVariableName();
        }
        else {
            return original.getBaseVariableName();
        }
    }
    getName() {
        const original = this.getOriginalVariable();
        if (original === this) {
            return super.getName();
        }
        else {
            return original.getName();
        }
    }
    getOriginalVariable() {
        if (this.originalVariable === null) {
            if (!this.originalId || (!this.hasId && this.originalId.variable.isReassigned)) {
                this.originalVariable = this;
            }
            else {
                const assignedOriginal = this.originalId.variable;
                this.originalVariable =
                    assignedOriginal instanceof ExportDefaultVariable
                        ? assignedOriginal.getOriginalVariable()
                        : assignedOriginal;
            }
        }
        return this.originalVariable;
    }
    setRenderNames(baseName, name) {
        const original = this.getOriginalVariable();
        if (original === this) {
            super.setRenderNames(baseName, name);
        }
        else {
            original.setRenderNames(baseName, name);
        }
    }
    setSafeName(name) {
        const original = this.getOriginalVariable();
        if (original === this) {
            super.setSafeName(name);
        }
        else {
            original.setSafeName(name);
        }
    }
}

const MISSING_EXPORT_SHIM_VARIABLE = '_missingExportShim';
const INTEROP_DEFAULT_VARIABLE = '_interopDefault';
const INTEROP_NAMESPACE_VARIABLE = '_interopNamespace';

class ExportShimVariable extends Variable {
    constructor(module) {
        super(MISSING_EXPORT_SHIM_VARIABLE);
        this.module = module;
    }
}

class NamespaceVariable extends Variable {
    constructor(context, syntheticNamedExports) {
        super(context.getModuleName());
        this.memberVariables = Object.create(null);
        this.containsExternalNamespace = false;
        this.referencedEarly = false;
        this.references = [];
        this.context = context;
        this.module = context.module;
        this.syntheticNamedExports = syntheticNamedExports;
    }
    addReference(identifier) {
        this.references.push(identifier);
        this.name = identifier.name;
    }
    // This is only called if "UNKNOWN_PATH" is reassigned as in all other situations, either the
    // build fails due to an illegal namespace reassignment or MemberExpression already forwards
    // the reassignment to the right variable. This means we lost track of this variable and thus
    // need to reassign all exports.
    deoptimizePath() {
        for (const key in this.memberVariables) {
            this.memberVariables[key].deoptimizePath(UNKNOWN_PATH);
        }
    }
    include(context) {
        if (!this.included) {
            if (this.containsExternalNamespace) {
                return this.context.error({
                    code: 'NAMESPACE_CANNOT_CONTAIN_EXTERNAL',
                    id: this.module.id,
                    message: `Cannot create an explicit namespace object for module "${this.context.getModuleName()}" because it contains a reexported external namespace`
                });
            }
            this.included = true;
            for (const identifier of this.references) {
                if (identifier.context.getModuleExecIndex() <= this.context.getModuleExecIndex()) {
                    this.referencedEarly = true;
                    break;
                }
            }
            if (this.context.preserveModules) {
                for (const memberName of Object.keys(this.memberVariables))
                    this.memberVariables[memberName].include(context);
            }
            else {
                for (const memberName of Object.keys(this.memberVariables))
                    this.context.includeVariable(context, this.memberVariables[memberName]);
            }
        }
    }
    initialise() {
        for (const name of this.context.getExports().concat(this.context.getReexports())) {
            if (name[0] === '*' && name.length > 1)
                this.containsExternalNamespace = true;
            this.memberVariables[name] = this.context.traceExport(name);
        }
    }
    renderBlock(options) {
        const _ = options.compact ? '' : ' ';
        const n = options.compact ? '' : '\n';
        const t = options.indent;
        const members = Object.keys(this.memberVariables).map(name => {
            const original = this.memberVariables[name];
            if (this.referencedEarly || original.isReassigned) {
                return `${t}get ${name}${_}()${_}{${_}return ${original.getName()}${options.compact ? '' : ';'}${_}}`;
            }
            const safeName = RESERVED_NAMES[name] ? `'${name}'` : name;
            return `${t}${safeName}: ${original.getName()}`;
        });
        members.unshift(`${t}__proto__:${_}null`);
        if (options.namespaceToStringTag) {
            members.unshift(`${t}[Symbol.toStringTag]:${_}'Module'`);
        }
        const name = this.getName();
        let output = `{${n}${members.join(`,${n}`)}${n}}`;
        if (this.syntheticNamedExports) {
            output = `/*#__PURE__*/Object.assign(${output}, ${this.module.getDefaultExport().getName()})`;
        }
        if (options.freeze) {
            output = `/*#__PURE__*/Object.freeze(${output})`;
        }
        output = `${options.varOrConst} ${name}${_}=${_}${output};`;
        if (options.format === 'system' && this.exportName) {
            output += `${n}exports('${this.exportName}',${_}${name});`;
        }
        return output;
    }
    renderFirst() {
        return this.referencedEarly;
    }
}
NamespaceVariable.prototype.isNamespace = true;

const esModuleExport = `Object.defineProperty(exports, '__esModule', { value: true });`;
const compactEsModuleExport = `Object.defineProperty(exports,'__esModule',{value:true});`;

function getExportBlock(exports, dependencies, namedExportsMode, interop, compact, t, mechanism = 'return ') {
    const _ = compact ? '' : ' ';
    const n = compact ? '' : '\n';
    if (!namedExportsMode) {
        let local;
        if (exports.length > 0) {
            local = exports[0].local;
        }
        else {
            for (const dep of dependencies) {
                if (dep.reexports) {
                    const expt = dep.reexports[0];
                    local =
                        dep.namedExportsMode && expt.imported !== '*' && expt.imported !== 'default'
                            ? `${dep.name}.${expt.imported}`
                            : dep.name;
                    break;
                }
            }
        }
        return `${mechanism}${local};`;
    }
    let exportBlock = '';
    // star exports must always output first for precedence
    for (const { name, reexports } of dependencies) {
        if (reexports && namedExportsMode) {
            for (const specifier of reexports) {
                if (specifier.reexported === '*') {
                    if (exportBlock)
                        exportBlock += n;
                    if (specifier.needsLiveBinding) {
                        exportBlock +=
                            `Object.keys(${name}).forEach(function${_}(k)${_}{${n}` +
                                `${t}if${_}(k${_}!==${_}'default')${_}Object.defineProperty(exports,${_}k,${_}{${n}` +
                                `${t}${t}enumerable:${_}true,${n}` +
                                `${t}${t}get:${_}function${_}()${_}{${n}` +
                                `${t}${t}${t}return ${name}[k];${n}` +
                                `${t}${t}}${n}${t}});${n}});`;
                    }
                    else {
                        exportBlock +=
                            `Object.keys(${name}).forEach(function${_}(k)${_}{${n}` +
                                `${t}if${_}(k${_}!==${_}'default')${_}exports[k]${_}=${_}${name}[k];${n}});`;
                    }
                }
            }
        }
    }
    for (const { name, imports, reexports, isChunk, namedExportsMode: depNamedExportsMode, exportsNames } of dependencies) {
        if (reexports && namedExportsMode) {
            for (const specifier of reexports) {
                if (specifier.imported === 'default' && !isChunk) {
                    if (exportBlock)
                        exportBlock += n;
                    if (exportsNames &&
                        (reexports.some(specifier => specifier.imported === 'default'
                            ? specifier.reexported === 'default'
                            : specifier.imported !== '*') ||
                            (imports && imports.some(specifier => specifier.imported !== 'default')))) {
                        exportBlock += `exports.${specifier.reexported}${_}=${_}${name}${interop !== false ? '__default' : '.default'};`;
                    }
                    else {
                        exportBlock += `exports.${specifier.reexported}${_}=${_}${name};`;
                    }
                }
                else if (specifier.imported !== '*') {
                    if (exportBlock)
                        exportBlock += n;
                    const importName = specifier.imported === 'default' && !depNamedExportsMode
                        ? name
                        : `${name}.${specifier.imported}`;
                    exportBlock += specifier.needsLiveBinding
                        ? `Object.defineProperty(exports,${_}'${specifier.reexported}',${_}{${n}` +
                            `${t}enumerable:${_}true,${n}` +
                            `${t}get:${_}function${_}()${_}{${n}` +
                            `${t}${t}return ${importName};${n}${t}}${n}});`
                        : `exports.${specifier.reexported}${_}=${_}${importName};`;
                }
                else if (specifier.reexported !== '*') {
                    if (exportBlock)
                        exportBlock += n;
                    exportBlock += `exports.${specifier.reexported}${_}=${_}${name};`;
                }
            }
        }
    }
    for (const expt of exports) {
        const lhs = `exports.${expt.exported}`;
        const rhs = expt.local;
        if (lhs !== rhs) {
            if (exportBlock)
                exportBlock += n;
            exportBlock += `${lhs}${_}=${_}${rhs};`;
        }
    }
    return exportBlock;
}

function getInteropBlock(dependencies, options, varOrConst) {
    const _ = options.compact ? '' : ' ';
    return dependencies
        .map(({ name, exportsNames, exportsDefault, namedExportsMode }) => {
        if (!namedExportsMode || !exportsDefault || options.interop === false)
            return null;
        if (exportsNames) {
            return (`${varOrConst} ${name}__default${_}=${_}'default'${_}in ${name}${_}?` +
                `${_}${name}['default']${_}:${_}${name};`);
        }
        return (`${name}${_}=${_}${name}${_}&&${_}Object.prototype.hasOwnProperty.call(${name},${_}'default')${_}?` +
            `${_}${name}['default']${_}:${_}${name};`);
    })
        .filter(Boolean)
        .join(options.compact ? '' : '\n');
}

function copyPropertyLiveBinding(_, n, t, i) {
    return (`${i}var d${_}=${_}Object.getOwnPropertyDescriptor(e,${_}k);${n}` +
        `${i}Object.defineProperty(n,${_}k,${_}d.get${_}?${_}d${_}:${_}{${n}` +
        `${i}${t}enumerable:${_}true,${n}` +
        `${i}${t}get:${_}function${_}()${_}{${n}` +
        `${i}${t}${t}return e[k];${n}` +
        `${i}${t}}${n}` +
        `${i}});${n}`);
}
function copyPropertyStatic(_, n, _t, i) {
    return `${i}n[k]${_}=e${_}[k];${n}`;
}
function getInteropNamespace(_, n, t, liveBindings) {
    return (`function ${INTEROP_NAMESPACE_VARIABLE}(e)${_}{${n}` +
        `${t}if${_}(e${_}&&${_}e.__esModule)${_}{${_}return e;${_}}${_}else${_}{${n}` +
        `${t}${t}var n${_}=${_}{};${n}` +
        `${t}${t}if${_}(e)${_}{${n}` +
        `${t}${t}${t}Object.keys(e).forEach(function${_}(k)${_}{${n}` +
        (liveBindings ? copyPropertyLiveBinding : copyPropertyStatic)(_, n, t, t + t + t + t) +
        `${t}${t}${t}});${n}` +
        `${t}${t}}${n}` +
        `${t}${t}n['default']${_}=${_}e;${n}` +
        `${t}${t}return n;${n}` +
        `${t}}${n}` +
        `}${n}${n}`);
}

const builtins$1 = {
    assert: true,
    buffer: true,
    console: true,
    constants: true,
    domain: true,
    events: true,
    http: true,
    https: true,
    os: true,
    path: true,
    process: true,
    punycode: true,
    querystring: true,
    stream: true,
    string_decoder: true,
    timers: true,
    tty: true,
    url: true,
    util: true,
    vm: true,
    zlib: true
};
// Creating a browser chunk that depends on Node.js built-in modules ('util'). You might need to include https://www.npmjs.com/package/rollup-plugin-node-builtins
function warnOnBuiltins(warn, dependencies) {
    const externalBuiltins = dependencies.map(({ id }) => id).filter(id => id in builtins$1);
    if (!externalBuiltins.length)
        return;
    const detail = externalBuiltins.length === 1
        ? `module ('${externalBuiltins[0]}')`
        : `modules (${externalBuiltins
            .slice(0, -1)
            .map(name => `'${name}'`)
            .join(', ')} and '${externalBuiltins.slice(-1)}')`;
    warn({
        code: 'MISSING_NODE_BUILTINS',
        message: `Creating a browser bundle that depends on Node.js built-in ${detail}. You might need to include https://www.npmjs.com/package/rollup-plugin-node-builtins`,
        modules: externalBuiltins
    });
}

// AMD resolution will only respect the AMD baseUrl if the .js extension is omitted.
// The assumption is that this makes sense for all relative ids:
// https://requirejs.org/docs/api.html#jsfiles
function removeExtensionFromRelativeAmdId(id) {
    if (id[0] === '.' && id.endsWith('.js')) {
        return id.slice(0, -3);
    }
    return id;
}
function amd(magicString, { accessedGlobals, dependencies, exports, hasExports, indentString: t, intro, isEntryModuleFacade, namedExportsMode, outro, varOrConst, warn }, options) {
    warnOnBuiltins(warn, dependencies);
    const deps = dependencies.map(m => `'${removeExtensionFromRelativeAmdId(m.id)}'`);
    const args = dependencies.map(m => m.name);
    const n = options.compact ? '' : '\n';
    const _ = options.compact ? '' : ' ';
    if (namedExportsMode && hasExports) {
        args.unshift(`exports`);
        deps.unshift(`'exports'`);
    }
    if (accessedGlobals.has('require')) {
        args.unshift('require');
        deps.unshift(`'require'`);
    }
    if (accessedGlobals.has('module')) {
        args.unshift('module');
        deps.unshift(`'module'`);
    }
    const amdOptions = options.amd || {};
    const params = (amdOptions.id ? `'${amdOptions.id}',${_}` : ``) +
        (deps.length ? `[${deps.join(`,${_}`)}],${_}` : ``);
    const useStrict = options.strict !== false ? `${_}'use strict';` : ``;
    const define = amdOptions.define || 'define';
    const wrapperStart = `${define}(${params}function${_}(${args.join(`,${_}`)})${_}{${useStrict}${n}${n}`;
    // var foo__default = 'default' in foo ? foo['default'] : foo;
    const interopBlock = getInteropBlock(dependencies, options, varOrConst);
    if (interopBlock) {
        magicString.prepend(interopBlock + n + n);
    }
    if (accessedGlobals.has(INTEROP_NAMESPACE_VARIABLE)) {
        magicString.prepend(getInteropNamespace(_, n, t, options.externalLiveBindings !== false));
    }
    if (intro)
        magicString.prepend(intro);
    const exportBlock = getExportBlock(exports, dependencies, namedExportsMode, options.interop, options.compact, t);
    if (exportBlock)
        magicString.append(n + n + exportBlock);
    if (namedExportsMode && hasExports && isEntryModuleFacade && options.esModule)
        magicString.append(`${n}${n}${options.compact ? compactEsModuleExport : esModuleExport}`);
    if (outro)
        magicString.append(outro);
    return magicString
        .indent(t)
        .append(n + n + '});')
        .prepend(wrapperStart);
}

function cjs(magicString, { accessedGlobals, dependencies, exports, hasExports, indentString: t, intro, isEntryModuleFacade, namedExportsMode, outro, varOrConst }, options) {
    const n = options.compact ? '' : '\n';
    const _ = options.compact ? '' : ' ';
    intro =
        (options.strict === false ? intro : `'use strict';${n}${n}${intro}`) +
            (namedExportsMode && hasExports && isEntryModuleFacade && options.esModule
                ? `${options.compact ? compactEsModuleExport : esModuleExport}${n}${n}`
                : '');
    let needsInterop = false;
    const interop = options.interop !== false;
    let importBlock;
    let definingVariable = false;
    importBlock = '';
    for (const { id, namedExportsMode, isChunk, name, reexports, imports, exportsNames, exportsDefault } of dependencies) {
        if (!reexports && !imports) {
            if (importBlock) {
                importBlock += !options.compact || definingVariable ? `;${n}` : ',';
            }
            definingVariable = false;
            importBlock += `require('${id}')`;
        }
        else {
            importBlock +=
                options.compact && definingVariable ? ',' : `${importBlock ? `;${n}` : ''}${varOrConst} `;
            definingVariable = true;
            if (!interop || isChunk || !exportsDefault || !namedExportsMode) {
                importBlock += `${name}${_}=${_}require('${id}')`;
            }
            else {
                needsInterop = true;
                if (exportsNames)
                    importBlock += `${name}${_}=${_}require('${id}')${options.compact ? ',' : `;\n${varOrConst} `}${name}__default${_}=${_}${INTEROP_DEFAULT_VARIABLE}(${name})`;
                else
                    importBlock += `${name}${_}=${_}${INTEROP_DEFAULT_VARIABLE}(require('${id}'))`;
            }
        }
    }
    if (importBlock)
        importBlock += ';';
    if (needsInterop) {
        const ex = options.compact ? 'e' : 'ex';
        intro +=
            `function ${INTEROP_DEFAULT_VARIABLE}${_}(${ex})${_}{${_}return${_}` +
                `(${ex}${_}&&${_}(typeof ${ex}${_}===${_}'object')${_}&&${_}'default'${_}in ${ex})${_}` +
                `?${_}${ex}['default']${_}:${_}${ex}${options.compact ? '' : '; '}}${n}${n}`;
    }
    if (accessedGlobals.has(INTEROP_NAMESPACE_VARIABLE)) {
        intro += getInteropNamespace(_, n, t, options.externalLiveBindings !== false);
    }
    if (importBlock)
        intro += importBlock + n + n;
    const exportBlock = getExportBlock(exports, dependencies, namedExportsMode, options.interop, options.compact, t, `module.exports${_}=${_}`);
    magicString.prepend(intro);
    if (exportBlock)
        magicString.append(n + n + exportBlock);
    if (outro)
        magicString.append(outro);
    return magicString;
}

function esm(magicString, { intro, outro, dependencies, exports }, options) {
    const _ = options.compact ? '' : ' ';
    const n = options.compact ? '' : '\n';
    const importBlock = getImportBlock(dependencies, _);
    if (importBlock.length > 0)
        intro += importBlock.join(n) + n + n;
    if (intro)
        magicString.prepend(intro);
    const exportBlock = getExportBlock$1(exports, _);
    if (exportBlock.length)
        magicString.append(n + n + exportBlock.join(n).trim());
    if (outro)
        magicString.append(outro);
    return magicString.trim();
}
function getImportBlock(dependencies, _) {
    const importBlock = [];
    for (const { id, reexports, imports, name } of dependencies) {
        if (!reexports && !imports) {
            importBlock.push(`import${_}'${id}';`);
            continue;
        }
        if (imports) {
            let defaultImport = null;
            let starImport = null;
            const importedNames = [];
            for (const specifier of imports) {
                if (specifier.imported === 'default') {
                    defaultImport = specifier;
                }
                else if (specifier.imported === '*') {
                    starImport = specifier;
                }
                else {
                    importedNames.push(specifier);
                }
            }
            if (starImport) {
                importBlock.push(`import${_}*${_}as ${starImport.local} from${_}'${id}';`);
            }
            if (defaultImport && importedNames.length === 0) {
                importBlock.push(`import ${defaultImport.local} from${_}'${id}';`);
            }
            else if (importedNames.length > 0) {
                importBlock.push(`import ${defaultImport ? `${defaultImport.local},${_}` : ''}{${_}${importedNames
                    .map(specifier => {
                    if (specifier.imported === specifier.local) {
                        return specifier.imported;
                    }
                    else {
                        return `${specifier.imported} as ${specifier.local}`;
                    }
                })
                    .join(`,${_}`)}${_}}${_}from${_}'${id}';`);
            }
        }
        if (reexports) {
            let starExport = null;
            const namespaceReexports = [];
            const namedReexports = [];
            for (const specifier of reexports) {
                if (specifier.reexported === '*') {
                    starExport = specifier;
                }
                else if (specifier.imported === '*') {
                    namespaceReexports.push(specifier);
                }
                else {
                    namedReexports.push(specifier);
                }
            }
            if (starExport) {
                importBlock.push(`export${_}*${_}from${_}'${id}';`);
            }
            if (namespaceReexports.length > 0) {
                if (!imports ||
                    !imports.some(specifier => specifier.imported === '*' && specifier.local === name)) {
                    importBlock.push(`import${_}*${_}as ${name} from${_}'${id}';`);
                }
                for (const specifier of namespaceReexports) {
                    importBlock.push(`export${_}{${_}${name === specifier.reexported ? name : `${name} as ${specifier.reexported}`} };`);
                }
            }
            if (namedReexports.length > 0) {
                importBlock.push(`export${_}{${_}${namedReexports
                    .map(specifier => {
                    if (specifier.imported === specifier.reexported) {
                        return specifier.imported;
                    }
                    else {
                        return `${specifier.imported} as ${specifier.reexported}`;
                    }
                })
                    .join(`,${_}`)}${_}}${_}from${_}'${id}';`);
            }
        }
    }
    return importBlock;
}
function getExportBlock$1(exports, _) {
    const exportBlock = [];
    const exportDeclaration = [];
    for (const specifier of exports) {
        if (specifier.exported === 'default') {
            exportBlock.push(`export default ${specifier.local};`);
        }
        else {
            exportDeclaration.push(specifier.exported === specifier.local
                ? specifier.local
                : `${specifier.local} as ${specifier.exported}`);
        }
    }
    if (exportDeclaration.length) {
        exportBlock.push(`export${_}{${_}${exportDeclaration.join(`,${_}`)}${_}};`);
    }
    return exportBlock;
}

function spaces(i) {
    let result = '';
    while (i--)
        result += ' ';
    return result;
}
function tabsToSpaces(str) {
    return str.replace(/^\t+/, match => match.split('\t').join('  '));
}
function getCodeFrame(source, line, column) {
    let lines = source.split('\n');
    const frameStart = Math.max(0, line - 3);
    let frameEnd = Math.min(line + 2, lines.length);
    lines = lines.slice(frameStart, frameEnd);
    while (!/\S/.test(lines[lines.length - 1])) {
        lines.pop();
        frameEnd -= 1;
    }
    const digits = String(frameEnd).length;
    return lines
        .map((str, i) => {
        const isErrorLine = frameStart + i + 1 === line;
        let lineNum = String(i + frameStart + 1);
        while (lineNum.length < digits)
            lineNum = ` ${lineNum}`;
        if (isErrorLine) {
            const indicator = spaces(digits + 2 + tabsToSpaces(str.slice(0, column)).length) + '^';
            return `${lineNum}: ${tabsToSpaces(str)}\n${indicator}`;
        }
        return `${lineNum}: ${tabsToSpaces(str)}`;
    })
        .join('\n');
}

function sanitizeFileName(name) {
    return name.replace(/[\0?*]/g, '_');
}

function getAliasName(id) {
    const base = basename(id);
    return base.substr(0, base.length - extname(id).length);
}
function relativeId(id) {
    if (typeof process === 'undefined' || !isAbsolute(id))
        return id;
    return relative$1(process.cwd(), id);
}
function isPlainPathFragment(name) {
    // not starting with "/", "./", "../"
    return (name[0] !== '/' &&
        !(name[0] === '.' && (name[1] === '/' || name[1] === '.')) &&
        sanitizeFileName(name) === name);
}

function error(base, props) {
    if (!(base instanceof Error))
        base = Object.assign(new Error(base.message), base);
    if (props)
        Object.assign(base, props);
    throw base;
}
function augmentCodeLocation(object, pos, source, id) {
    if (typeof pos === 'object') {
        const { line, column } = pos;
        object.loc = { file: id, line, column };
    }
    else {
        object.pos = pos;
        const { line, column } = locate(source, pos, { offsetLine: 1 });
        object.loc = { file: id, line, column };
    }
    if (object.frame === undefined) {
        const { line, column } = object.loc;
        object.frame = getCodeFrame(source, line, column);
    }
}
var Errors;
(function (Errors) {
    Errors["ASSET_NOT_FINALISED"] = "ASSET_NOT_FINALISED";
    Errors["ASSET_NOT_FOUND"] = "ASSET_NOT_FOUND";
    Errors["ASSET_SOURCE_ALREADY_SET"] = "ASSET_SOURCE_ALREADY_SET";
    Errors["ASSET_SOURCE_MISSING"] = "ASSET_SOURCE_MISSING";
    Errors["BAD_LOADER"] = "BAD_LOADER";
    Errors["CANNOT_EMIT_FROM_OPTIONS_HOOK"] = "CANNOT_EMIT_FROM_OPTIONS_HOOK";
    Errors["CHUNK_NOT_GENERATED"] = "CHUNK_NOT_GENERATED";
    Errors["DEPRECATED_FEATURE"] = "DEPRECATED_FEATURE";
    Errors["FILE_NOT_FOUND"] = "FILE_NOT_FOUND";
    Errors["FILE_NAME_CONFLICT"] = "FILE_NAME_CONFLICT";
    Errors["INPUT_HOOK_IN_OUTPUT_PLUGIN"] = "INPUT_HOOK_IN_OUTPUT_PLUGIN";
    Errors["INVALID_CHUNK"] = "INVALID_CHUNK";
    Errors["INVALID_EXPORT_OPTION"] = "INVALID_EXPORT_OPTION";
    Errors["INVALID_EXTERNAL_ID"] = "INVALID_EXTERNAL_ID";
    Errors["INVALID_OPTION"] = "INVALID_OPTION";
    Errors["INVALID_PLUGIN_HOOK"] = "INVALID_PLUGIN_HOOK";
    Errors["INVALID_ROLLUP_PHASE"] = "INVALID_ROLLUP_PHASE";
    Errors["MIXED_EXPORTS"] = "MIXED_EXPORTS";
    Errors["NAMESPACE_CONFLICT"] = "NAMESPACE_CONFLICT";
    Errors["PLUGIN_ERROR"] = "PLUGIN_ERROR";
    Errors["UNRESOLVED_ENTRY"] = "UNRESOLVED_ENTRY";
    Errors["UNRESOLVED_IMPORT"] = "UNRESOLVED_IMPORT";
    Errors["VALIDATION_ERROR"] = "VALIDATION_ERROR";
    Errors["EXTERNAL_SYNTHETIC_EXPORTS"] = "EXTERNAL_SYNTHETIC_EXPORTS";
    Errors["SYNTHETIC_NAMED_EXPORTS_NEED_DEFAULT"] = "SYNTHETIC_NAMED_EXPORTS_NEED_DEFAULT";
})(Errors || (Errors = {}));
function errAssetNotFinalisedForFileName(name) {
    return {
        code: Errors.ASSET_NOT_FINALISED,
        message: `Plugin error - Unable to get file name for asset "${name}". Ensure that the source is set and that generate is called first.`
    };
}
function errCannotEmitFromOptionsHook() {
    return {
        code: Errors.CANNOT_EMIT_FROM_OPTIONS_HOOK,
        message: `Cannot emit files or set asset sources in the "outputOptions" hook, use the "renderStart" hook instead.`
    };
}
function errChunkNotGeneratedForFileName(name) {
    return {
        code: Errors.CHUNK_NOT_GENERATED,
        message: `Plugin error - Unable to get file name for chunk "${name}". Ensure that generate is called first.`
    };
}
function errAssetReferenceIdNotFoundForSetSource(assetReferenceId) {
    return {
        code: Errors.ASSET_NOT_FOUND,
        message: `Plugin error - Unable to set the source for unknown asset "${assetReferenceId}".`
    };
}
function errAssetSourceAlreadySet(name) {
    return {
        code: Errors.ASSET_SOURCE_ALREADY_SET,
        message: `Unable to set the source for asset "${name}", source already set.`
    };
}
function errNoAssetSourceSet(assetName) {
    return {
        code: Errors.ASSET_SOURCE_MISSING,
        message: `Plugin error creating asset "${assetName}" - no asset source set.`
    };
}
function errBadLoader(id) {
    return {
        code: Errors.BAD_LOADER,
        message: `Error loading ${relativeId(id)}: plugin load hook should return a string, a { code, map } object, or nothing/null`
    };
}
function errDeprecation(deprecation) {
    return Object.assign({ code: Errors.DEPRECATED_FEATURE }, (typeof deprecation === 'string' ? { message: deprecation } : deprecation));
}
function errFileReferenceIdNotFoundForFilename(assetReferenceId) {
    return {
        code: Errors.FILE_NOT_FOUND,
        message: `Plugin error - Unable to get file name for unknown file "${assetReferenceId}".`
    };
}
function errFileNameConflict(fileName) {
    return {
        code: Errors.FILE_NAME_CONFLICT,
        message: `The emitted file "${fileName}" overwrites a previously emitted file of the same name.`
    };
}
function errInputHookInOutputPlugin(pluginName, hookName) {
    return {
        code: Errors.INPUT_HOOK_IN_OUTPUT_PLUGIN,
        message: `The "${hookName}" hook used by the output plugin ${pluginName} is a build time hook and will not be run for that plugin. Either this plugin cannot be used as an output plugin, or it should have an option to configure it as an output plugin.`
    };
}
function errCannotAssignModuleToChunk(moduleId, assignToAlias, currentAlias) {
    return {
        code: Errors.INVALID_CHUNK,
        message: `Cannot assign ${relativeId(moduleId)} to the "${assignToAlias}" chunk as it is already in the "${currentAlias}" chunk.`
    };
}
function errInvalidExportOptionValue(optionValue) {
    return {
        code: Errors.INVALID_EXPORT_OPTION,
        message: `"output.exports" must be "default", "named", "none", "auto", or left unspecified (defaults to "auto"), received "${optionValue}"`,
        url: `https://rollupjs.org/guide/en/#output-exports`
    };
}
function errIncompatibleExportOptionValue(optionValue, keys, entryModule) {
    return {
        code: 'INVALID_EXPORT_OPTION',
        message: `"${optionValue}" was specified for "output.exports", but entry module "${relativeId(entryModule)}" has the following exports: ${keys.join(', ')}`
    };
}
function errInternalIdCannotBeExternal(source, importer) {
    return {
        code: Errors.INVALID_EXTERNAL_ID,
        message: `'${source}' is imported as an external by ${relativeId(importer)}, but is already an existing non-external module id.`
    };
}
function errInvalidOption(option, explanation) {
    return {
        code: Errors.INVALID_OPTION,
        message: `Invalid value for option "${option}" - ${explanation}.`
    };
}
function errInvalidRollupPhaseForAddWatchFile() {
    return {
        code: Errors.INVALID_ROLLUP_PHASE,
        message: `Cannot call addWatchFile after the build has finished.`
    };
}
function errInvalidRollupPhaseForChunkEmission() {
    return {
        code: Errors.INVALID_ROLLUP_PHASE,
        message: `Cannot emit chunks after module loading has finished.`
    };
}
function errMixedExport(facadeModuleId, name) {
    return {
        code: Errors.MIXED_EXPORTS,
        id: facadeModuleId,
        message: `Entry module "${relativeId(facadeModuleId)}" is using named and default exports together. Consumers of your bundle will have to use \`${name ||
            'chunk'}["default"]\` to access the default export, which may not be what you want. Use \`output.exports: "named"\` to disable this warning`,
        url: `https://rollupjs.org/guide/en/#output-exports`
    };
}
function errNamespaceConflict(name, reexportingModule, additionalExportAllModule) {
    return {
        code: Errors.NAMESPACE_CONFLICT,
        message: `Conflicting namespaces: ${relativeId(reexportingModule.id)} re-exports '${name}' from both ${relativeId(reexportingModule.exportsAll[name])} and ${relativeId(additionalExportAllModule.exportsAll[name])} (will be ignored)`,
        name,
        reexporter: reexportingModule.id,
        sources: [reexportingModule.exportsAll[name], additionalExportAllModule.exportsAll[name]]
    };
}
function errEntryCannotBeExternal(unresolvedId) {
    return {
        code: Errors.UNRESOLVED_ENTRY,
        message: `Entry module cannot be external (${relativeId(unresolvedId)}).`
    };
}
function errUnresolvedEntry(unresolvedId) {
    return {
        code: Errors.UNRESOLVED_ENTRY,
        message: `Could not resolve entry module (${relativeId(unresolvedId)}).`
    };
}
function errUnresolvedImport(source, importer) {
    return {
        code: Errors.UNRESOLVED_IMPORT,
        message: `Could not resolve '${source}' from ${relativeId(importer)}`
    };
}
function errUnresolvedImportTreatedAsExternal(source, importer) {
    return {
        code: Errors.UNRESOLVED_IMPORT,
        importer: relativeId(importer),
        message: `'${source}' is imported by ${relativeId(importer)}, but could not be resolved – treating it as an external dependency`,
        source,
        url: 'https://rollupjs.org/guide/en/#warning-treating-module-as-external-dependency'
    };
}
function errExternalSyntheticExports(source, importer) {
    return {
        code: Errors.EXTERNAL_SYNTHETIC_EXPORTS,
        importer: relativeId(importer),
        message: `External '${source}' can not have 'syntheticNamedExports' enabled.`,
        source
    };
}
function errFailedValidation(message) {
    return {
        code: Errors.VALIDATION_ERROR,
        message
    };
}

// Generate strings which dereference dotted properties, but use array notation `['prop-deref']`
// if the property name isn't trivial
const shouldUseDot = /^[a-zA-Z$_][a-zA-Z0-9$_]*$/;
function property(prop) {
    return shouldUseDot.test(prop) ? `.${prop}` : `['${prop}']`;
}
function keypath(keypath) {
    return keypath
        .split('.')
        .map(property)
        .join('');
}

function setupNamespace(name, root, globals, compact) {
    const parts = name.split('.');
    if (globals) {
        parts[0] = (typeof globals === 'function' ? globals(parts[0]) : globals[parts[0]]) || parts[0];
    }
    const _ = compact ? '' : ' ';
    parts.pop();
    let acc = root;
    return (parts
        .map(part => ((acc += property(part)), `${acc}${_}=${_}${acc}${_}||${_}{}${compact ? '' : ';'}`))
        .join(compact ? ',' : '\n') + (compact && parts.length ? ';' : '\n'));
}
function assignToDeepVariable(deepName, root, globals, compact, assignment) {
    const _ = compact ? '' : ' ';
    const parts = deepName.split('.');
    if (globals) {
        parts[0] = (typeof globals === 'function' ? globals(parts[0]) : globals[parts[0]]) || parts[0];
    }
    const last = parts.pop();
    let acc = root;
    let deepAssignment = parts
        .map(part => ((acc += property(part)), `${acc}${_}=${_}${acc}${_}||${_}{}`))
        .concat(`${acc}${property(last)}`)
        .join(`,${_}`)
        .concat(`${_}=${_}${assignment}`);
    if (parts.length > 0) {
        deepAssignment = `(${deepAssignment})`;
    }
    return deepAssignment;
}

function trimEmptyImports(dependencies) {
    let i = dependencies.length;
    while (i--) {
        const dependency = dependencies[i];
        if (dependency.exportsDefault || dependency.exportsNames) {
            return dependencies.slice(0, i + 1);
        }
    }
    return [];
}

const thisProp = (name) => `this${keypath(name)}`;
function iife(magicString, { dependencies, exports, hasExports, indentString: t, intro, namedExportsMode, outro, varOrConst, warn }, options) {
    const _ = options.compact ? '' : ' ';
    const n = options.compact ? '' : '\n';
    const { extend, name } = options;
    const isNamespaced = name && name.indexOf('.') !== -1;
    const useVariableAssignment = !extend && !isNamespaced;
    if (name && useVariableAssignment && !isLegal(name)) {
        return error({
            code: 'ILLEGAL_IDENTIFIER_AS_NAME',
            message: `Given name "${name}" is not a legal JS identifier. If you need this, you can try "output.extend: true".`
        });
    }
    warnOnBuiltins(warn, dependencies);
    const external = trimEmptyImports(dependencies);
    const deps = external.map(dep => dep.globalName || 'null');
    const args = external.map(m => m.name);
    if (hasExports && !name) {
        warn({
            code: 'MISSING_NAME_OPTION_FOR_IIFE_EXPORT',
            message: `If you do not supply "output.name", you may not be able to access the exports of an IIFE bundle.`
        });
    }
    if (namedExportsMode && hasExports) {
        if (extend) {
            deps.unshift(`${thisProp(name)}${_}=${_}${thisProp(name)}${_}||${_}{}`);
            args.unshift('exports');
        }
        else {
            deps.unshift('{}');
            args.unshift('exports');
        }
    }
    const useStrict = options.strict !== false ? `${t}'use strict';${n}${n}` : ``;
    let wrapperIntro = `(function${_}(${args.join(`,${_}`)})${_}{${n}${useStrict}`;
    if (hasExports && (!extend || !namedExportsMode) && name) {
        wrapperIntro =
            (useVariableAssignment ? `${varOrConst} ${name}` : thisProp(name)) +
                `${_}=${_}${wrapperIntro}`;
    }
    if (isNamespaced && hasExports) {
        wrapperIntro = setupNamespace(name, 'this', options.globals, options.compact) + wrapperIntro;
    }
    let wrapperOutro = `${n}${n}}(${deps.join(`,${_}`)}));`;
    if (!extend && namedExportsMode && hasExports) {
        wrapperOutro = `${n}${n}${t}return exports;${wrapperOutro}`;
    }
    // var foo__default = 'default' in foo ? foo['default'] : foo;
    const interopBlock = getInteropBlock(dependencies, options, varOrConst);
    if (interopBlock)
        magicString.prepend(interopBlock + n + n);
    if (intro)
        magicString.prepend(intro);
    const exportBlock = getExportBlock(exports, dependencies, namedExportsMode, options.interop, options.compact, t);
    if (exportBlock)
        magicString.append(n + n + exportBlock);
    if (outro)
        magicString.append(outro);
    return magicString
        .indent(t)
        .prepend(wrapperIntro)
        .append(wrapperOutro);
}

function getStarExcludes({ dependencies, exports }) {
    const starExcludes = new Set(exports.map(expt => expt.exported));
    if (!starExcludes.has('default'))
        starExcludes.add('default');
    // also include reexport names
    for (const { reexports } of dependencies) {
        if (reexports) {
            for (const reexport of reexports) {
                if (reexport.imported !== '*' && !starExcludes.has(reexport.reexported))
                    starExcludes.add(reexport.reexported);
            }
        }
    }
    return starExcludes;
}
const getStarExcludesBlock = (starExcludes, varOrConst, _, t, n) => starExcludes
    ? `${n}${t}${varOrConst} _starExcludes${_}=${_}{${_}${Array.from(starExcludes).join(`:${_}1,${_}`)}${starExcludes.size ? `:${_}1` : ''}${_}};`
    : '';
const getImportBindingsBlock = (importBindings, _, t, n) => (importBindings.length ? `${n}${t}var ${importBindings.join(`,${_}`)};` : '');
function getExportsBlock(exports, _, t, n) {
    if (exports.length === 0) {
        return '';
    }
    if (exports.length === 1) {
        return `${t}${t}${t}exports('${exports[0].name}',${_}${exports[0].value});${n}${n}`;
    }
    return (`${t}${t}${t}exports({${n}` +
        exports.map(({ name, value }) => `${t}${t}${t}${t}${name}:${_}${value}`).join(`,${n}`) +
        `${n}${t}${t}${t}});${n}${n}`);
}
const getHoistedExportsBlock = (exports, _, t, n) => getExportsBlock(exports
    .filter(expt => expt.hoisted || expt.uninitialized)
    .map(expt => ({ name: expt.exported, value: expt.uninitialized ? 'void 0' : expt.local })), _, t, n);
const getMissingExportsBlock = (exports, _, t, n) => getExportsBlock(exports
    .filter(expt => expt.local === MISSING_EXPORT_SHIM_VARIABLE)
    .map(expt => ({ name: expt.exported, value: MISSING_EXPORT_SHIM_VARIABLE })), _, t, n);
function system(magicString, { accessedGlobals, dependencies, exports, hasExports, indentString: t, intro, outro, usesTopLevelAwait, varOrConst }, options) {
    const n = options.compact ? '' : '\n';
    const _ = options.compact ? '' : ' ';
    const dependencyIds = dependencies.map(m => `'${m.id}'`);
    const importBindings = [];
    let starExcludes;
    const setters = [];
    for (const { imports, reexports } of dependencies) {
        const setter = [];
        if (imports) {
            for (const specifier of imports) {
                importBindings.push(specifier.local);
                if (specifier.imported === '*') {
                    setter.push(`${specifier.local}${_}=${_}module;`);
                }
                else {
                    setter.push(`${specifier.local}${_}=${_}module.${specifier.imported};`);
                }
            }
        }
        if (reexports) {
            let createdSetter = false;
            // bulk-reexport form
            if (reexports.length > 1 ||
                (reexports.length === 1 &&
                    (reexports[0].reexported === '*' || reexports[0].imported === '*'))) {
                // star reexports
                for (const specifier of reexports) {
                    if (specifier.reexported !== '*')
                        continue;
                    // need own exports list for deduping in star export case
                    if (!starExcludes) {
                        starExcludes = getStarExcludes({ dependencies, exports });
                    }
                    if (!createdSetter) {
                        setter.push(`${varOrConst} _setter${_}=${_}{};`);
                        createdSetter = true;
                    }
                    setter.push(`for${_}(var _$p${_}in${_}module)${_}{`);
                    setter.push(`${t}if${_}(!_starExcludes[_$p])${_}_setter[_$p]${_}=${_}module[_$p];`);
                    setter.push('}');
                }
                // star import reexport
                for (const specifier of reexports) {
                    if (specifier.imported !== '*' || specifier.reexported === '*')
                        continue;
                    setter.push(`exports('${specifier.reexported}',${_}module);`);
                }
                // reexports
                for (const specifier of reexports) {
                    if (specifier.reexported === '*' || specifier.imported === '*')
                        continue;
                    if (!createdSetter) {
                        setter.push(`${varOrConst} _setter${_}=${_}{};`);
                        createdSetter = true;
                    }
                    setter.push(`_setter.${specifier.reexported}${_}=${_}module.${specifier.imported};`);
                }
                if (createdSetter) {
                    setter.push('exports(_setter);');
                }
            }
            else {
                // single reexport
                for (const specifier of reexports) {
                    setter.push(`exports('${specifier.reexported}',${_}module.${specifier.imported});`);
                }
            }
        }
        setters.push(setter.join(`${n}${t}${t}${t}`));
    }
    const registeredName = options.name ? `'${options.name}',${_}` : '';
    const wrapperParams = accessedGlobals.has('module')
        ? `exports,${_}module`
        : hasExports
            ? 'exports'
            : '';
    let wrapperStart = `System.register(${registeredName}[` +
        dependencyIds.join(`,${_}`) +
        `],${_}function${_}(${wrapperParams})${_}{${n}${t}${options.strict ? "'use strict';" : ''}` +
        getStarExcludesBlock(starExcludes, varOrConst, _, t, n) +
        getImportBindingsBlock(importBindings, _, t, n) +
        `${n}${t}return${_}{${setters.length
            ? `${n}${t}${t}setters:${_}[${setters
                .map(s => s
                ? `function${_}(module)${_}{${n}${t}${t}${t}${s}${n}${t}${t}}`
                : `function${_}()${_}{}`)
                .join(`,${_}`)}],`
            : ''}${n}`;
    wrapperStart +=
        `${t}${t}execute:${_}${usesTopLevelAwait ? `async${_}` : ''}function${_}()${_}{${n}${n}` +
            getHoistedExportsBlock(exports, _, t, n);
    const wrapperEnd = `${n}${n}` +
        getMissingExportsBlock(exports, _, t, n) +
        `${t}${t}}${n}${t}}${options.compact ? '' : ';'}${n}});`;
    if (intro)
        magicString.prepend(intro);
    if (outro)
        magicString.append(outro);
    return magicString
        .indent(`${t}${t}${t}`)
        .append(wrapperEnd)
        .prepend(wrapperStart);
}

function globalProp(name, globalVar) {
    if (!name)
        return 'null';
    return `${globalVar}${keypath(name)}`;
}
function safeAccess(name, globalVar, _) {
    const parts = name.split('.');
    let acc = globalVar;
    return parts.map(part => ((acc += property(part)), acc)).join(`${_}&&${_}`);
}
function umd(magicString, { dependencies, exports, hasExports, indentString: t, intro, namedExportsMode, outro, varOrConst, warn }, options) {
    const _ = options.compact ? '' : ' ';
    const n = options.compact ? '' : '\n';
    const factoryVar = options.compact ? 'f' : 'factory';
    const globalVar = options.compact ? 'g' : 'global';
    if (hasExports && !options.name) {
        return error({
            code: 'INVALID_OPTION',
            message: 'You must supply "output.name" for UMD bundles.'
        });
    }
    warnOnBuiltins(warn, dependencies);
    const amdDeps = dependencies.map(m => `'${m.id}'`);
    const cjsDeps = dependencies.map(m => `require('${m.id}')`);
    const trimmedImports = trimEmptyImports(dependencies);
    const globalDeps = trimmedImports.map(module => globalProp(module.globalName, globalVar));
    const factoryArgs = trimmedImports.map(m => m.name);
    if (namedExportsMode && (hasExports || options.noConflict === true)) {
        amdDeps.unshift(`'exports'`);
        cjsDeps.unshift(`exports`);
        globalDeps.unshift(assignToDeepVariable(options.name, globalVar, options.globals, options.compact, `${options.extend ? `${globalProp(options.name, globalVar)}${_}||${_}` : ''}{}`));
        factoryArgs.unshift('exports');
    }
    const amdOptions = options.amd || {};
    const amdParams = (amdOptions.id ? `'${amdOptions.id}',${_}` : ``) +
        (amdDeps.length ? `[${amdDeps.join(`,${_}`)}],${_}` : ``);
    const define = amdOptions.define || 'define';
    const cjsExport = !namedExportsMode && hasExports ? `module.exports${_}=${_}` : ``;
    const useStrict = options.strict !== false ? `${_}'use strict';${n}` : ``;
    let iifeExport;
    if (options.noConflict === true) {
        const noConflictExportsVar = options.compact ? 'e' : 'exports';
        let factory;
        if (!namedExportsMode && hasExports) {
            factory = `var ${noConflictExportsVar}${_}=${_}${assignToDeepVariable(options.name, globalVar, options.globals, options.compact, `${factoryVar}(${globalDeps.join(`,${_}`)})`)};`;
        }
        else if (namedExportsMode) {
            const module = globalDeps.shift();
            factory =
                `var ${noConflictExportsVar}${_}=${_}${module};${n}` +
                    `${t}${t}${factoryVar}(${[noConflictExportsVar].concat(globalDeps).join(`,${_}`)});`;
        }
        iifeExport =
            `(function${_}()${_}{${n}` +
                `${t}${t}var current${_}=${_}${safeAccess(options.name, globalVar, _)};${n}` +
                `${t}${t}${factory}${n}` +
                `${t}${t}${noConflictExportsVar}.noConflict${_}=${_}function${_}()${_}{${_}` +
                `${globalProp(options.name, globalVar)}${_}=${_}current;${_}return ${noConflictExportsVar}${options.compact ? '' : '; '}};${n}` +
                `${t}}())`;
    }
    else {
        iifeExport = `${factoryVar}(${globalDeps.join(`,${_}`)})`;
        if (!namedExportsMode && hasExports) {
            iifeExport = assignToDeepVariable(options.name, globalVar, options.globals, options.compact, iifeExport);
        }
    }
    const iifeNeedsGlobal = hasExports || (options.noConflict === true && namedExportsMode) || globalDeps.length > 0;
    const globalParam = iifeNeedsGlobal ? `${globalVar},${_}` : '';
    const globalArg = iifeNeedsGlobal ? `this,${_}` : '';
    const iifeStart = iifeNeedsGlobal ? `(${globalVar}${_}=${_}${globalVar}${_}||${_}self,${_}` : '';
    const iifeEnd = iifeNeedsGlobal ? ')' : '';
    const cjsIntro = iifeNeedsGlobal
        ? `${t}typeof exports${_}===${_}'object'${_}&&${_}typeof module${_}!==${_}'undefined'${_}?` +
            `${_}${cjsExport}${factoryVar}(${cjsDeps.join(`,${_}`)})${_}:${n}`
        : '';
    // factory function should be wrapped by parentheses to avoid lazy parsing
    const wrapperIntro = `(function${_}(${globalParam}${factoryVar})${_}{${n}` +
        cjsIntro +
        `${t}typeof ${define}${_}===${_}'function'${_}&&${_}${define}.amd${_}?${_}${define}(${amdParams}${factoryVar})${_}:${n}` +
        `${t}${iifeStart}${iifeExport}${iifeEnd};${n}` +
        `}(${globalArg}(function${_}(${factoryArgs.join(', ')})${_}{${useStrict}${n}`;
    const wrapperOutro = n + n + '})));';
    // var foo__default = 'default' in foo ? foo['default'] : foo;
    const interopBlock = getInteropBlock(dependencies, options, varOrConst);
    if (interopBlock)
        magicString.prepend(interopBlock + n + n);
    if (intro)
        magicString.prepend(intro);
    const exportBlock = getExportBlock(exports, dependencies, namedExportsMode, options.interop, options.compact, t);
    if (exportBlock)
        magicString.append(n + n + exportBlock);
    if (namedExportsMode && hasExports && options.esModule)
        magicString.append(n + n + (options.compact ? compactEsModuleExport : esModuleExport));
    if (outro)
        magicString.append(outro);
    return magicString
        .trim()
        .indent(t)
        .append(wrapperOutro)
        .prepend(wrapperIntro);
}

var finalisers = { system, amd, cjs, es: esm, iife, umd };

const extractors = {
    ArrayPattern(names, param) {
        for (const element of param.elements) {
            if (element)
                extractors[element.type](names, element);
        }
    },
    AssignmentPattern(names, param) {
        extractors[param.left.type](names, param.left);
    },
    Identifier(names, param) {
        names.push(param.name);
    },
    MemberExpression() { },
    ObjectPattern(names, param) {
        for (const prop of param.properties) {
            if (prop.type === 'RestElement') {
                extractors.RestElement(names, prop);
            }
            else {
                extractors[prop.value.type](names, prop.value);
            }
        }
    },
    RestElement(names, param) {
        extractors[param.argument.type](names, param.argument);
    }
};
const extractAssignedNames = function extractAssignedNames(param) {
    const names = [];
    extractors[param.type](names, param);
    return names;
};

class ExportAllDeclaration extends NodeBase {
    hasEffects() {
        return false;
    }
    initialise() {
        this.context.addExport(this);
    }
    render(code, _options, nodeRenderOptions) {
        code.remove(nodeRenderOptions.start, nodeRenderOptions.end);
    }
}
ExportAllDeclaration.prototype.needsBoundaries = true;

class ArrayExpression extends NodeBase {
    bind() {
        super.bind();
        for (const element of this.elements) {
            if (element !== null)
                element.deoptimizePath(UNKNOWN_PATH);
        }
    }
    getReturnExpressionWhenCalledAtPath(path) {
        if (path.length !== 1)
            return UNKNOWN_EXPRESSION;
        return getMemberReturnExpressionWhenCalled(arrayMembers, path[0]);
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length === 1) {
            return hasMemberEffectWhenCalled(arrayMembers, path[0], this.included, callOptions, context);
        }
        return true;
    }
}

class ArrayPattern extends NodeBase {
    addExportedVariables(variables) {
        for (const element of this.elements) {
            if (element !== null) {
                element.addExportedVariables(variables);
            }
        }
    }
    declare(kind) {
        const variables = [];
        for (const element of this.elements) {
            if (element !== null) {
                variables.push(...element.declare(kind, UNKNOWN_EXPRESSION));
            }
        }
        return variables;
    }
    deoptimizePath(path) {
        if (path.length === 0) {
            for (const element of this.elements) {
                if (element !== null) {
                    element.deoptimizePath(path);
                }
            }
        }
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (path.length > 0)
            return true;
        for (const element of this.elements) {
            if (element !== null && element.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context))
                return true;
        }
        return false;
    }
}

class BlockScope extends ChildScope {
    addDeclaration(identifier, context, init = null, isHoisted) {
        if (isHoisted) {
            return this.parent.addDeclaration(identifier, context, isHoisted === 'function' ? init : UNKNOWN_EXPRESSION, isHoisted);
        }
        else {
            return super.addDeclaration(identifier, context, init, false);
        }
    }
}

class ExpressionStatement$1 extends NodeBase {
    initialise() {
        if (this.directive &&
            this.directive !== 'use strict' &&
            this.parent.type === Program) {
            this.context.warn(
            // This is necessary, because either way (deleting or not) can lead to errors.
            {
                code: 'MODULE_LEVEL_DIRECTIVE',
                message: `Module level directives cause errors when bundled, '${this.directive}' was ignored.`
            }, this.start);
        }
    }
    render(code, options) {
        super.render(code, options);
        if (this.included)
            this.insertSemicolon(code);
    }
    shouldBeIncluded(context) {
        if (this.directive && this.directive !== 'use strict')
            return this.parent.type !== Program;
        return super.shouldBeIncluded(context);
    }
}

class BlockStatement$1 extends NodeBase {
    constructor() {
        super(...arguments);
        this.directlyIncluded = false;
    }
    addImplicitReturnExpressionToScope() {
        const lastStatement = this.body[this.body.length - 1];
        if (!lastStatement || lastStatement.type !== ReturnStatement) {
            this.scope.addReturnExpression(UNKNOWN_EXPRESSION);
        }
    }
    createScope(parentScope) {
        this.scope = this.parent.preventChildBlockScope
            ? parentScope
            : new BlockScope(parentScope);
    }
    hasEffects(context) {
        if (this.deoptimizeBody)
            return true;
        for (const node of this.body) {
            if (node.hasEffects(context))
                return true;
            if (context.brokenFlow)
                break;
        }
        return false;
    }
    include(context, includeChildrenRecursively) {
        if (!this.deoptimizeBody || !this.directlyIncluded) {
            this.included = true;
            this.directlyIncluded = true;
            if (this.deoptimizeBody)
                includeChildrenRecursively = true;
            for (const node of this.body) {
                if (includeChildrenRecursively || node.shouldBeIncluded(context))
                    node.include(context, includeChildrenRecursively);
            }
        }
    }
    initialise() {
        const firstBodyStatement = this.body[0];
        this.deoptimizeBody =
            firstBodyStatement instanceof ExpressionStatement$1 &&
                firstBodyStatement.directive === 'use asm';
    }
    render(code, options) {
        if (this.body.length) {
            renderStatementList(this.body, code, this.start + 1, this.end - 1, options);
        }
        else {
            super.render(code, options);
        }
    }
}

class ArrowFunctionExpression extends NodeBase {
    createScope(parentScope) {
        this.scope = new ReturnValueScope(parentScope, this.context);
    }
    deoptimizePath(path) {
        // A reassignment of UNKNOWN_PATH is considered equivalent to having lost track
        // which means the return expression needs to be reassigned
        if (path.length === 1 && path[0] === UnknownKey) {
            this.scope.getReturnExpression().deoptimizePath(UNKNOWN_PATH);
        }
    }
    getReturnExpressionWhenCalledAtPath(path) {
        return path.length === 0 ? this.scope.getReturnExpression() : UNKNOWN_EXPRESSION;
    }
    hasEffects() {
        return false;
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenAssignedAtPath(path) {
        return path.length > 1;
    }
    hasEffectsWhenCalledAtPath(path, _callOptions, context) {
        if (path.length > 0)
            return true;
        for (const param of this.params) {
            if (param.hasEffects(context))
                return true;
        }
        const { ignore, brokenFlow } = context;
        context.ignore = {
            breaks: false,
            continues: false,
            labels: new Set(),
            returnAwaitYield: true
        };
        if (this.body.hasEffects(context))
            return true;
        context.ignore = ignore;
        context.brokenFlow = brokenFlow;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        for (const param of this.params) {
            if (!(param instanceof Identifier$1)) {
                param.include(context, includeChildrenRecursively);
            }
        }
        const { brokenFlow } = context;
        context.brokenFlow = BROKEN_FLOW_NONE;
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
    includeCallArguments(context, args) {
        this.scope.includeCallArguments(context, args);
    }
    initialise() {
        this.scope.addParameterVariables(this.params.map(param => param.declare('parameter', UNKNOWN_EXPRESSION)), this.params[this.params.length - 1] instanceof RestElement);
        if (this.body instanceof BlockStatement$1) {
            this.body.addImplicitReturnExpressionToScope();
        }
        else {
            this.scope.addReturnExpression(this.body);
        }
    }
    parseNode(esTreeNode) {
        if (esTreeNode.body.type === BlockStatement) {
            this.body = new this.context.nodeConstructors.BlockStatement(esTreeNode.body, this, this.scope.hoistedBodyVarScope);
        }
        super.parseNode(esTreeNode);
    }
}
ArrowFunctionExpression.prototype.preventChildBlockScope = true;

function getSystemExportStatement(exportedVariables) {
    if (exportedVariables.length === 1) {
        return `exports('${exportedVariables[0].safeExportName ||
            exportedVariables[0].exportName}', ${exportedVariables[0].getName()});`;
    }
    else {
        return `exports({${exportedVariables
            .map(variable => `${variable.safeExportName || variable.exportName}: ${variable.getName()}`)
            .join(', ')}});`;
    }
}

class AssignmentExpression extends NodeBase {
    constructor() {
        super(...arguments);
        this.deoptimized = false;
    }
    hasEffects(context) {
        if (!this.deoptimized)
            this.applyDeoptimizations();
        return (this.right.hasEffects(context) ||
            this.left.hasEffects(context) ||
            this.left.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context));
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        return path.length > 0 && this.right.hasEffectsWhenAccessedAtPath(path, context);
    }
    include(context, includeChildrenRecursively) {
        if (!this.deoptimized)
            this.applyDeoptimizations();
        this.included = true;
        this.left.include(context, includeChildrenRecursively);
        this.right.include(context, includeChildrenRecursively);
    }
    render(code, options) {
        this.left.render(code, options);
        this.right.render(code, options);
        if (options.format === 'system') {
            if (this.left.variable && this.left.variable.exportName) {
                const operatorPos = findFirstOccurrenceOutsideComment(code.original, this.operator, this.left.end);
                const operation = this.operator.length > 1
                    ? ` ${this.left.variable.exportName} ${this.operator.slice(0, -1)}`
                    : '';
                code.overwrite(operatorPos, operatorPos + this.operator.length, `= exports('${this.left.variable.exportName}',${operation}`);
                code.appendLeft(this.right.end, `)`);
            }
            else if ('addExportedVariables' in this.left) {
                const systemPatternExports = [];
                this.left.addExportedVariables(systemPatternExports);
                if (systemPatternExports.length > 0) {
                    code.prependRight(this.start, `function (v) {${getSystemExportStatement(systemPatternExports)} return v;} (`);
                    code.appendLeft(this.end, ')');
                }
            }
        }
    }
    applyDeoptimizations() {
        this.deoptimized = true;
        this.left.deoptimizePath(EMPTY_PATH);
        this.right.deoptimizePath(UNKNOWN_PATH);
    }
}

class AssignmentPattern extends NodeBase {
    addExportedVariables(variables) {
        this.left.addExportedVariables(variables);
    }
    bind() {
        super.bind();
        this.left.deoptimizePath(EMPTY_PATH);
        this.right.deoptimizePath(UNKNOWN_PATH);
    }
    declare(kind, init) {
        return this.left.declare(kind, init);
    }
    deoptimizePath(path) {
        path.length === 0 && this.left.deoptimizePath(path);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        return path.length > 0 || this.left.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context);
    }
    render(code, options, { isShorthandProperty } = BLANK) {
        this.left.render(code, options, { isShorthandProperty });
        this.right.render(code, options);
    }
}

class AwaitExpression extends NodeBase {
    hasEffects(context) {
        return !context.ignore.returnAwaitYield || this.argument.hasEffects(context);
    }
    include(context, includeChildrenRecursively) {
        if (!this.included) {
            this.included = true;
            checkTopLevelAwait: if (!this.context.usesTopLevelAwait) {
                let parent = this.parent;
                do {
                    if (parent instanceof FunctionNode || parent instanceof ArrowFunctionExpression)
                        break checkTopLevelAwait;
                } while ((parent = parent.parent));
                this.context.usesTopLevelAwait = true;
            }
        }
        this.argument.include(context, includeChildrenRecursively);
    }
}

const binaryOperators = {
    '!=': (left, right) => left != right,
    '!==': (left, right) => left !== right,
    '%': (left, right) => left % right,
    '&': (left, right) => left & right,
    '*': (left, right) => left * right,
    // At the moment, "**" will be transpiled to Math.pow
    '**': (left, right) => Math.pow(left, right),
    '+': (left, right) => left + right,
    '-': (left, right) => left - right,
    '/': (left, right) => left / right,
    '<': (left, right) => left < right,
    '<<': (left, right) => left << right,
    '<=': (left, right) => left <= right,
    '==': (left, right) => left == right,
    '===': (left, right) => left === right,
    '>': (left, right) => left > right,
    '>=': (left, right) => left >= right,
    '>>': (left, right) => left >> right,
    '>>>': (left, right) => left >>> right,
    '^': (left, right) => left ^ right,
    in: () => UnknownValue,
    instanceof: () => UnknownValue,
    '|': (left, right) => left | right
};
class BinaryExpression extends NodeBase {
    deoptimizeCache() { }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        if (path.length > 0)
            return UnknownValue;
        const leftValue = this.left.getLiteralValueAtPath(EMPTY_PATH, recursionTracker, origin);
        if (leftValue === UnknownValue)
            return UnknownValue;
        const rightValue = this.right.getLiteralValueAtPath(EMPTY_PATH, recursionTracker, origin);
        if (rightValue === UnknownValue)
            return UnknownValue;
        const operatorFn = binaryOperators[this.operator];
        if (!operatorFn)
            return UnknownValue;
        return operatorFn(leftValue, rightValue);
    }
    hasEffects(context) {
        // support some implicit type coercion runtime errors
        if (this.operator === '+' &&
            this.parent instanceof ExpressionStatement$1 &&
            this.left.getLiteralValueAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this) === '')
            return true;
        return super.hasEffects(context);
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
}

class BreakStatement extends NodeBase {
    hasEffects(context) {
        if (this.label) {
            if (!context.ignore.labels.has(this.label.name))
                return true;
            context.includedLabels.add(this.label.name);
            context.brokenFlow = BROKEN_FLOW_ERROR_RETURN_LABEL;
        }
        else {
            if (!context.ignore.breaks)
                return true;
            context.brokenFlow = BROKEN_FLOW_BREAK_CONTINUE;
        }
        return false;
    }
    include(context) {
        this.included = true;
        if (this.label) {
            this.label.include(context);
            context.includedLabels.add(this.label.name);
        }
        context.brokenFlow = this.label ? BROKEN_FLOW_ERROR_RETURN_LABEL : BROKEN_FLOW_BREAK_CONTINUE;
    }
}

class Literal extends NodeBase {
    getLiteralValueAtPath(path) {
        if (path.length > 0 ||
            // unknown literals can also be null but do not start with an "n"
            (this.value === null && this.context.code.charCodeAt(this.start) !== 110) ||
            typeof this.value === 'bigint' ||
            // to support shims for regular expressions
            this.context.code.charCodeAt(this.start) === 47) {
            return UnknownValue;
        }
        return this.value;
    }
    getReturnExpressionWhenCalledAtPath(path) {
        if (path.length !== 1)
            return UNKNOWN_EXPRESSION;
        return getMemberReturnExpressionWhenCalled(this.members, path[0]);
    }
    hasEffectsWhenAccessedAtPath(path) {
        if (this.value === null) {
            return path.length > 0;
        }
        return path.length > 1;
    }
    hasEffectsWhenAssignedAtPath(path) {
        return path.length > 0;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length === 1) {
            return hasMemberEffectWhenCalled(this.members, path[0], this.included, callOptions, context);
        }
        return true;
    }
    initialise() {
        this.members = getLiteralMembersForValue(this.value);
    }
    render(code) {
        if (typeof this.value === 'string') {
            code.indentExclusionRanges.push([this.start + 1, this.end - 1]);
        }
    }
}

function getResolvablePropertyKey(memberExpression) {
    return memberExpression.computed
        ? getResolvableComputedPropertyKey(memberExpression.property)
        : memberExpression.property.name;
}
function getResolvableComputedPropertyKey(propertyKey) {
    if (propertyKey instanceof Literal) {
        return String(propertyKey.value);
    }
    return null;
}
function getPathIfNotComputed(memberExpression) {
    const nextPathKey = memberExpression.propertyKey;
    const object = memberExpression.object;
    if (typeof nextPathKey === 'string') {
        if (object instanceof Identifier$1) {
            return [
                { key: object.name, pos: object.start },
                { key: nextPathKey, pos: memberExpression.property.start }
            ];
        }
        if (object instanceof MemberExpression) {
            const parentPath = getPathIfNotComputed(object);
            return (parentPath && [...parentPath, { key: nextPathKey, pos: memberExpression.property.start }]);
        }
    }
    return null;
}
function getStringFromPath(path) {
    let pathString = path[0].key;
    for (let index = 1; index < path.length; index++) {
        pathString += '.' + path[index].key;
    }
    return pathString;
}
class MemberExpression extends NodeBase {
    constructor() {
        super(...arguments);
        this.variable = null;
        this.bound = false;
        this.expressionsToBeDeoptimized = [];
        this.replacement = null;
        this.wasPathDeoptimizedWhileOptimized = false;
    }
    addExportedVariables() { }
    bind() {
        if (this.bound)
            return;
        this.bound = true;
        const path = getPathIfNotComputed(this);
        const baseVariable = path && this.scope.findVariable(path[0].key);
        if (baseVariable && baseVariable.isNamespace) {
            const resolvedVariable = this.resolveNamespaceVariables(baseVariable, path.slice(1));
            if (!resolvedVariable) {
                super.bind();
            }
            else if (typeof resolvedVariable === 'string') {
                this.replacement = resolvedVariable;
            }
            else {
                if (resolvedVariable instanceof ExternalVariable && resolvedVariable.module) {
                    resolvedVariable.module.suggestName(path[0].key);
                }
                this.variable = resolvedVariable;
                this.scope.addNamespaceMemberAccess(getStringFromPath(path), resolvedVariable);
            }
        }
        else {
            super.bind();
            // ensure the propertyKey is set for the tree-shaking passes
            this.getPropertyKey();
        }
    }
    deoptimizeCache() {
        const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized;
        this.expressionsToBeDeoptimized = [];
        this.propertyKey = UnknownKey;
        if (this.wasPathDeoptimizedWhileOptimized) {
            this.object.deoptimizePath(UNKNOWN_PATH);
        }
        for (const expression of expressionsToBeDeoptimized) {
            expression.deoptimizeCache();
        }
    }
    deoptimizePath(path) {
        if (!this.bound)
            this.bind();
        if (path.length === 0)
            this.disallowNamespaceReassignment();
        if (this.variable) {
            this.variable.deoptimizePath(path);
        }
        else {
            const propertyKey = this.getPropertyKey();
            if (propertyKey === UnknownKey) {
                this.object.deoptimizePath(UNKNOWN_PATH);
            }
            else {
                this.wasPathDeoptimizedWhileOptimized = true;
                this.object.deoptimizePath([propertyKey, ...path]);
            }
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        if (!this.bound)
            this.bind();
        if (this.variable !== null) {
            return this.variable.getLiteralValueAtPath(path, recursionTracker, origin);
        }
        this.expressionsToBeDeoptimized.push(origin);
        return this.object.getLiteralValueAtPath([this.getPropertyKey(), ...path], recursionTracker, origin);
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        if (!this.bound)
            this.bind();
        if (this.variable !== null) {
            return this.variable.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
        }
        this.expressionsToBeDeoptimized.push(origin);
        return this.object.getReturnExpressionWhenCalledAtPath([this.getPropertyKey(), ...path], recursionTracker, origin);
    }
    hasEffects(context) {
        return (this.property.hasEffects(context) ||
            this.object.hasEffects(context) ||
            (this.context.propertyReadSideEffects &&
                this.object.hasEffectsWhenAccessedAtPath([this.propertyKey], context)));
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (path.length === 0)
            return false;
        if (this.variable !== null) {
            return this.variable.hasEffectsWhenAccessedAtPath(path, context);
        }
        return this.object.hasEffectsWhenAccessedAtPath([this.propertyKey, ...path], context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (this.variable !== null) {
            return this.variable.hasEffectsWhenAssignedAtPath(path, context);
        }
        return this.object.hasEffectsWhenAssignedAtPath([this.propertyKey, ...path], context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (this.variable !== null) {
            return this.variable.hasEffectsWhenCalledAtPath(path, callOptions, context);
        }
        return this.object.hasEffectsWhenCalledAtPath([this.propertyKey, ...path], callOptions, context);
    }
    include(context, includeChildrenRecursively) {
        if (!this.included) {
            this.included = true;
            if (this.variable !== null) {
                this.context.includeVariable(context, this.variable);
            }
        }
        this.object.include(context, includeChildrenRecursively);
        this.property.include(context, includeChildrenRecursively);
    }
    includeCallArguments(context, args) {
        if (this.variable) {
            this.variable.includeCallArguments(context, args);
        }
        else {
            super.includeCallArguments(context, args);
        }
    }
    initialise() {
        this.propertyKey = getResolvablePropertyKey(this);
    }
    render(code, options, { renderedParentType, isCalleeOfRenderedParent } = BLANK) {
        const isCalleeOfDifferentParent = renderedParentType === CallExpression && isCalleeOfRenderedParent;
        if (this.variable || this.replacement) {
            let replacement = this.variable ? this.variable.getName() : this.replacement;
            if (isCalleeOfDifferentParent)
                replacement = '0, ' + replacement;
            code.overwrite(this.start, this.end, replacement, {
                contentOnly: true,
                storeName: true
            });
        }
        else {
            if (isCalleeOfDifferentParent) {
                code.appendRight(this.start, '0, ');
            }
            super.render(code, options);
        }
    }
    disallowNamespaceReassignment() {
        if (this.object instanceof Identifier$1 &&
            this.scope.findVariable(this.object.name).isNamespace) {
            return this.context.error({
                code: 'ILLEGAL_NAMESPACE_REASSIGNMENT',
                message: `Illegal reassignment to import '${this.object.name}'`
            }, this.start);
        }
    }
    getPropertyKey() {
        if (this.propertyKey === null) {
            this.propertyKey = UnknownKey;
            const value = this.property.getLiteralValueAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this);
            return (this.propertyKey = value === UnknownValue ? UnknownKey : String(value));
        }
        return this.propertyKey;
    }
    resolveNamespaceVariables(baseVariable, path) {
        if (path.length === 0)
            return baseVariable;
        if (!baseVariable.isNamespace)
            return null;
        const exportName = path[0].key;
        const variable = baseVariable instanceof ExternalVariable
            ? baseVariable.module.getVariableForExportName(exportName)
            : baseVariable.context.traceExport(exportName);
        if (!variable) {
            const fileName = baseVariable instanceof ExternalVariable
                ? baseVariable.module.id
                : baseVariable.context.fileName;
            this.context.warn({
                code: 'MISSING_EXPORT',
                exporter: relativeId(fileName),
                importer: relativeId(this.context.fileName),
                message: `'${exportName}' is not exported by '${relativeId(fileName)}'`,
                missing: exportName,
                url: `https://rollupjs.org/guide/en/#error-name-is-not-exported-by-module`
            }, path[0].pos);
            return 'undefined';
        }
        return this.resolveNamespaceVariables(variable, path.slice(1));
    }
}

class CallExpression$1 extends NodeBase {
    constructor() {
        super(...arguments);
        this.expressionsToBeDeoptimized = [];
        this.returnExpression = null;
        this.wasPathDeoptmizedWhileOptimized = false;
    }
    bind() {
        super.bind();
        if (this.callee instanceof Identifier$1) {
            const variable = this.scope.findVariable(this.callee.name);
            if (variable.isNamespace) {
                return this.context.error({
                    code: 'CANNOT_CALL_NAMESPACE',
                    message: `Cannot call a namespace ('${this.callee.name}')`
                }, this.start);
            }
            if (this.callee.name === 'eval') {
                this.context.warn({
                    code: 'EVAL',
                    message: `Use of eval is strongly discouraged, as it poses security risks and may cause issues with minification`,
                    url: 'https://rollupjs.org/guide/en/#avoiding-eval'
                }, this.start);
            }
        }
        // ensure the returnExpression is set for the tree-shaking passes
        this.getReturnExpression(SHARED_RECURSION_TRACKER);
        // This deoptimizes "this" for non-namespace calls until we have a better solution
        if (this.callee instanceof MemberExpression && !this.callee.variable) {
            this.callee.object.deoptimizePath(UNKNOWN_PATH);
        }
        for (const argument of this.arguments) {
            // This will make sure all properties of parameters behave as "unknown"
            argument.deoptimizePath(UNKNOWN_PATH);
        }
    }
    deoptimizeCache() {
        if (this.returnExpression !== UNKNOWN_EXPRESSION) {
            this.returnExpression = null;
            const returnExpression = this.getReturnExpression(SHARED_RECURSION_TRACKER);
            const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized;
            if (returnExpression !== UNKNOWN_EXPRESSION) {
                // We need to replace here because is possible new expressions are added
                // while we are deoptimizing the old ones
                this.expressionsToBeDeoptimized = [];
                if (this.wasPathDeoptmizedWhileOptimized) {
                    returnExpression.deoptimizePath(UNKNOWN_PATH);
                    this.wasPathDeoptmizedWhileOptimized = false;
                }
            }
            for (const expression of expressionsToBeDeoptimized) {
                expression.deoptimizeCache();
            }
        }
    }
    deoptimizePath(path) {
        if (path.length === 0)
            return;
        const trackedEntities = this.context.deoptimizationTracker.getEntities(path);
        if (trackedEntities.has(this))
            return;
        trackedEntities.add(this);
        const returnExpression = this.getReturnExpression(SHARED_RECURSION_TRACKER);
        if (returnExpression !== UNKNOWN_EXPRESSION) {
            this.wasPathDeoptmizedWhileOptimized = true;
            returnExpression.deoptimizePath(path);
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        const returnExpression = this.getReturnExpression(recursionTracker);
        if (returnExpression === UNKNOWN_EXPRESSION) {
            return UnknownValue;
        }
        const trackedEntities = recursionTracker.getEntities(path);
        if (trackedEntities.has(returnExpression)) {
            return UnknownValue;
        }
        this.expressionsToBeDeoptimized.push(origin);
        trackedEntities.add(returnExpression);
        const value = returnExpression.getLiteralValueAtPath(path, recursionTracker, origin);
        trackedEntities.delete(returnExpression);
        return value;
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        const returnExpression = this.getReturnExpression(recursionTracker);
        if (this.returnExpression === UNKNOWN_EXPRESSION) {
            return UNKNOWN_EXPRESSION;
        }
        const trackedEntities = recursionTracker.getEntities(path);
        if (trackedEntities.has(returnExpression)) {
            return UNKNOWN_EXPRESSION;
        }
        this.expressionsToBeDeoptimized.push(origin);
        trackedEntities.add(returnExpression);
        const value = returnExpression.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
        trackedEntities.delete(returnExpression);
        return value;
    }
    hasEffects(context) {
        for (const argument of this.arguments) {
            if (argument.hasEffects(context))
                return true;
        }
        if (this.context.annotations && this.annotatedPure)
            return false;
        return (this.callee.hasEffects(context) ||
            this.callee.hasEffectsWhenCalledAtPath(EMPTY_PATH, this.callOptions, context));
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (path.length === 0)
            return false;
        const trackedExpressions = context.accessed.getEntities(path);
        if (trackedExpressions.has(this))
            return false;
        trackedExpressions.add(this);
        return this.returnExpression.hasEffectsWhenAccessedAtPath(path, context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (path.length === 0)
            return true;
        const trackedExpressions = context.assigned.getEntities(path);
        if (trackedExpressions.has(this))
            return false;
        trackedExpressions.add(this);
        return this.returnExpression.hasEffectsWhenAssignedAtPath(path, context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        const trackedExpressions = (callOptions.withNew
            ? context.instantiated
            : context.called).getEntities(path);
        if (trackedExpressions.has(this))
            return false;
        trackedExpressions.add(this);
        return this.returnExpression.hasEffectsWhenCalledAtPath(path, callOptions, context);
    }
    include(context, includeChildrenRecursively) {
        if (includeChildrenRecursively) {
            super.include(context, includeChildrenRecursively);
            if (includeChildrenRecursively === INCLUDE_PARAMETERS &&
                this.callee instanceof Identifier$1 &&
                this.callee.variable) {
                this.callee.variable.markCalledFromTryStatement();
            }
        }
        else {
            this.included = true;
            this.callee.include(context, false);
        }
        this.callee.includeCallArguments(context, this.arguments);
        if (!this.returnExpression.included) {
            this.returnExpression.include(context, false);
        }
    }
    initialise() {
        this.callOptions = {
            args: this.arguments,
            withNew: false
        };
    }
    render(code, options, { renderedParentType } = BLANK) {
        this.callee.render(code, options);
        if (this.arguments.length > 0) {
            if (this.arguments[this.arguments.length - 1].included) {
                for (const arg of this.arguments) {
                    arg.render(code, options);
                }
            }
            else {
                let lastIncludedIndex = this.arguments.length - 2;
                while (lastIncludedIndex >= 0 && !this.arguments[lastIncludedIndex].included) {
                    lastIncludedIndex--;
                }
                if (lastIncludedIndex >= 0) {
                    for (let index = 0; index <= lastIncludedIndex; index++) {
                        this.arguments[index].render(code, options);
                    }
                    code.remove(findFirstOccurrenceOutsideComment(code.original, ',', this.arguments[lastIncludedIndex].end), this.end - 1);
                }
                else {
                    code.remove(findFirstOccurrenceOutsideComment(code.original, '(', this.callee.end) + 1, this.end - 1);
                }
            }
        }
        if (renderedParentType === ExpressionStatement &&
            this.callee.type === FunctionExpression) {
            code.appendRight(this.start, '(');
            code.prependLeft(this.end, ')');
        }
    }
    getReturnExpression(recursionTracker) {
        if (this.returnExpression === null) {
            this.returnExpression = UNKNOWN_EXPRESSION;
            return (this.returnExpression = this.callee.getReturnExpressionWhenCalledAtPath(EMPTY_PATH, recursionTracker, this));
        }
        return this.returnExpression;
    }
}

class CatchScope extends ParameterScope {
    addDeclaration(identifier, context, init, isHoisted) {
        if (isHoisted) {
            return this.parent.addDeclaration(identifier, context, init, isHoisted);
        }
        else {
            return super.addDeclaration(identifier, context, init, false);
        }
    }
}

class CatchClause extends NodeBase {
    createScope(parentScope) {
        this.scope = new CatchScope(parentScope, this.context);
    }
    initialise() {
        if (this.param) {
            this.param.declare('parameter', UNKNOWN_EXPRESSION);
        }
    }
    parseNode(esTreeNode) {
        this.body = new this.context.nodeConstructors.BlockStatement(esTreeNode.body, this, this.scope);
        super.parseNode(esTreeNode);
    }
}
CatchClause.prototype.preventChildBlockScope = true;

class ClassBody extends NodeBase {
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (path.length > 0)
            return true;
        return (this.classConstructor !== null &&
            this.classConstructor.hasEffectsWhenCalledAtPath(EMPTY_PATH, callOptions, context));
    }
    initialise() {
        for (const method of this.body) {
            if (method.kind === 'constructor') {
                this.classConstructor = method;
                return;
            }
        }
        this.classConstructor = null;
    }
}

class ClassExpression extends ClassNode {
}

class MultiExpression {
    constructor(expressions) {
        this.included = false;
        this.expressions = expressions;
    }
    deoptimizePath(path) {
        for (const expression of this.expressions) {
            expression.deoptimizePath(path);
        }
    }
    getLiteralValueAtPath() {
        return UnknownValue;
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        return new MultiExpression(this.expressions.map(expression => expression.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin)));
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        for (const expression of this.expressions) {
            if (expression.hasEffectsWhenAccessedAtPath(path, context))
                return true;
        }
        return false;
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        for (const expression of this.expressions) {
            if (expression.hasEffectsWhenAssignedAtPath(path, context))
                return true;
        }
        return false;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        for (const expression of this.expressions) {
            if (expression.hasEffectsWhenCalledAtPath(path, callOptions, context))
                return true;
        }
        return false;
    }
    include() { }
    includeCallArguments() { }
}

class ConditionalExpression extends NodeBase {
    constructor() {
        super(...arguments);
        this.expressionsToBeDeoptimized = [];
        this.isBranchResolutionAnalysed = false;
        this.usedBranch = null;
        this.wasPathDeoptimizedWhileOptimized = false;
    }
    bind() {
        super.bind();
        // ensure the usedBranch is set for the tree-shaking passes
        this.getUsedBranch();
    }
    deoptimizeCache() {
        if (this.usedBranch !== null) {
            const unusedBranch = this.usedBranch === this.consequent ? this.alternate : this.consequent;
            this.usedBranch = null;
            const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized;
            this.expressionsToBeDeoptimized = [];
            if (this.wasPathDeoptimizedWhileOptimized) {
                unusedBranch.deoptimizePath(UNKNOWN_PATH);
            }
            for (const expression of expressionsToBeDeoptimized) {
                expression.deoptimizeCache();
            }
        }
    }
    deoptimizePath(path) {
        if (path.length > 0) {
            const usedBranch = this.getUsedBranch();
            if (usedBranch === null) {
                this.consequent.deoptimizePath(path);
                this.alternate.deoptimizePath(path);
            }
            else {
                this.wasPathDeoptimizedWhileOptimized = true;
                usedBranch.deoptimizePath(path);
            }
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        const usedBranch = this.getUsedBranch();
        if (usedBranch === null)
            return UnknownValue;
        this.expressionsToBeDeoptimized.push(origin);
        return usedBranch.getLiteralValueAtPath(path, recursionTracker, origin);
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        const usedBranch = this.getUsedBranch();
        if (usedBranch === null)
            return new MultiExpression([
                this.consequent.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin),
                this.alternate.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin)
            ]);
        this.expressionsToBeDeoptimized.push(origin);
        return usedBranch.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
    }
    hasEffects(context) {
        if (this.test.hasEffects(context))
            return true;
        if (this.usedBranch === null) {
            return this.consequent.hasEffects(context) || this.alternate.hasEffects(context);
        }
        return this.usedBranch.hasEffects(context);
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (path.length === 0)
            return false;
        if (this.usedBranch === null) {
            return (this.consequent.hasEffectsWhenAccessedAtPath(path, context) ||
                this.alternate.hasEffectsWhenAccessedAtPath(path, context));
        }
        return this.usedBranch.hasEffectsWhenAccessedAtPath(path, context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (path.length === 0)
            return true;
        if (this.usedBranch === null) {
            return (this.consequent.hasEffectsWhenAssignedAtPath(path, context) ||
                this.alternate.hasEffectsWhenAssignedAtPath(path, context));
        }
        return this.usedBranch.hasEffectsWhenAssignedAtPath(path, context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (this.usedBranch === null) {
            return (this.consequent.hasEffectsWhenCalledAtPath(path, callOptions, context) ||
                this.alternate.hasEffectsWhenCalledAtPath(path, callOptions, context));
        }
        return this.usedBranch.hasEffectsWhenCalledAtPath(path, callOptions, context);
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (includeChildrenRecursively ||
            this.test.shouldBeIncluded(context) ||
            this.usedBranch === null) {
            this.test.include(context, includeChildrenRecursively);
            this.consequent.include(context, includeChildrenRecursively);
            this.alternate.include(context, includeChildrenRecursively);
        }
        else {
            this.usedBranch.include(context, includeChildrenRecursively);
        }
    }
    render(code, options, { renderedParentType, isCalleeOfRenderedParent, preventASI } = BLANK) {
        if (!this.test.included) {
            const colonPos = findFirstOccurrenceOutsideComment(code.original, ':', this.consequent.end);
            const inclusionStart = (this.consequent.included
                ? findFirstOccurrenceOutsideComment(code.original, '?', this.test.end)
                : colonPos) + 1;
            if (preventASI) {
                removeLineBreaks(code, inclusionStart, this.usedBranch.start);
            }
            code.remove(this.start, inclusionStart);
            if (this.consequent.included) {
                code.remove(colonPos, this.end);
            }
            removeAnnotations(this, code);
            this.usedBranch.render(code, options, {
                isCalleeOfRenderedParent: renderedParentType
                    ? isCalleeOfRenderedParent
                    : this.parent.callee === this,
                renderedParentType: renderedParentType || this.parent.type
            });
        }
        else {
            super.render(code, options);
        }
    }
    getUsedBranch() {
        if (this.isBranchResolutionAnalysed) {
            return this.usedBranch;
        }
        this.isBranchResolutionAnalysed = true;
        const testValue = this.test.getLiteralValueAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this);
        return testValue === UnknownValue
            ? null
            : (this.usedBranch = testValue ? this.consequent : this.alternate);
    }
}

class ContinueStatement extends NodeBase {
    hasEffects(context) {
        if (this.label) {
            if (!context.ignore.labels.has(this.label.name))
                return true;
            context.includedLabels.add(this.label.name);
            context.brokenFlow = BROKEN_FLOW_ERROR_RETURN_LABEL;
        }
        else {
            if (!context.ignore.continues)
                return true;
            context.brokenFlow = BROKEN_FLOW_BREAK_CONTINUE;
        }
        return false;
    }
    include(context) {
        this.included = true;
        if (this.label) {
            this.label.include(context);
            context.includedLabels.add(this.label.name);
        }
        context.brokenFlow = this.label ? BROKEN_FLOW_ERROR_RETURN_LABEL : BROKEN_FLOW_BREAK_CONTINUE;
    }
}

class DoWhileStatement extends NodeBase {
    hasEffects(context) {
        if (this.test.hasEffects(context))
            return true;
        const { brokenFlow, ignore: { breaks, continues } } = context;
        context.ignore.breaks = true;
        context.ignore.continues = true;
        if (this.body.hasEffects(context))
            return true;
        context.ignore.breaks = breaks;
        context.ignore.continues = continues;
        context.brokenFlow = brokenFlow;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        this.test.include(context, includeChildrenRecursively);
        const { brokenFlow } = context;
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
}

class EmptyStatement extends NodeBase {
    hasEffects() {
        return false;
    }
}

class ExportNamedDeclaration extends NodeBase {
    bind() {
        // Do not bind specifiers
        if (this.declaration !== null)
            this.declaration.bind();
    }
    hasEffects(context) {
        return this.declaration !== null && this.declaration.hasEffects(context);
    }
    initialise() {
        this.context.addExport(this);
    }
    render(code, options, nodeRenderOptions) {
        const { start, end } = nodeRenderOptions;
        if (this.declaration === null) {
            code.remove(start, end);
        }
        else {
            code.remove(this.start, this.declaration.start);
            this.declaration.render(code, options, { start, end });
        }
    }
}
ExportNamedDeclaration.prototype.needsBoundaries = true;

class ForInStatement extends NodeBase {
    bind() {
        this.left.bind();
        this.left.deoptimizePath(EMPTY_PATH);
        this.right.bind();
        this.body.bind();
    }
    createScope(parentScope) {
        this.scope = new BlockScope(parentScope);
    }
    hasEffects(context) {
        if ((this.left &&
            (this.left.hasEffects(context) ||
                this.left.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context))) ||
            (this.right && this.right.hasEffects(context)))
            return true;
        const { brokenFlow, ignore: { breaks, continues } } = context;
        context.ignore.breaks = true;
        context.ignore.continues = true;
        if (this.body.hasEffects(context))
            return true;
        context.ignore.breaks = breaks;
        context.ignore.continues = continues;
        context.brokenFlow = brokenFlow;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        this.left.includeWithAllDeclaredVariables(includeChildrenRecursively, context);
        this.left.deoptimizePath(EMPTY_PATH);
        this.right.include(context, includeChildrenRecursively);
        const { brokenFlow } = context;
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
    render(code, options) {
        this.left.render(code, options, NO_SEMICOLON);
        this.right.render(code, options, NO_SEMICOLON);
        // handle no space between "in" and the right side
        if (code.original.charCodeAt(this.right.start - 1) === 110 /* n */) {
            code.prependLeft(this.right.start, ' ');
        }
        this.body.render(code, options);
    }
}

class ForOfStatement extends NodeBase {
    bind() {
        this.left.bind();
        this.left.deoptimizePath(EMPTY_PATH);
        this.right.bind();
        this.body.bind();
    }
    createScope(parentScope) {
        this.scope = new BlockScope(parentScope);
    }
    hasEffects() {
        // Placeholder until proper Symbol.Iterator support
        return true;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        this.left.includeWithAllDeclaredVariables(includeChildrenRecursively, context);
        this.left.deoptimizePath(EMPTY_PATH);
        this.right.include(context, includeChildrenRecursively);
        const { brokenFlow } = context;
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
    render(code, options) {
        this.left.render(code, options, NO_SEMICOLON);
        this.right.render(code, options, NO_SEMICOLON);
        // handle no space between "of" and the right side
        if (code.original.charCodeAt(this.right.start - 1) === 102 /* f */) {
            code.prependLeft(this.right.start, ' ');
        }
        this.body.render(code, options);
    }
}

class ForStatement extends NodeBase {
    createScope(parentScope) {
        this.scope = new BlockScope(parentScope);
    }
    hasEffects(context) {
        if ((this.init && this.init.hasEffects(context)) ||
            (this.test && this.test.hasEffects(context)) ||
            (this.update && this.update.hasEffects(context)))
            return true;
        const { brokenFlow, ignore: { breaks, continues } } = context;
        context.ignore.breaks = true;
        context.ignore.continues = true;
        if (this.body.hasEffects(context))
            return true;
        context.ignore.breaks = breaks;
        context.ignore.continues = continues;
        context.brokenFlow = brokenFlow;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (this.init)
            this.init.include(context, includeChildrenRecursively);
        if (this.test)
            this.test.include(context, includeChildrenRecursively);
        const { brokenFlow } = context;
        if (this.update)
            this.update.include(context, includeChildrenRecursively);
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
    render(code, options) {
        if (this.init)
            this.init.render(code, options, NO_SEMICOLON);
        if (this.test)
            this.test.render(code, options, NO_SEMICOLON);
        if (this.update)
            this.update.render(code, options, NO_SEMICOLON);
        this.body.render(code, options);
    }
}

class FunctionExpression$1 extends FunctionNode {
}

const unset = Symbol('unset');
class IfStatement extends NodeBase {
    constructor() {
        super(...arguments);
        this.testValue = unset;
    }
    deoptimizeCache() {
        this.testValue = UnknownValue;
    }
    hasEffects(context) {
        if (this.test.hasEffects(context)) {
            return true;
        }
        const testValue = this.getTestValue();
        if (testValue === UnknownValue) {
            const { brokenFlow } = context;
            if (this.consequent.hasEffects(context))
                return true;
            const consequentBrokenFlow = context.brokenFlow;
            context.brokenFlow = brokenFlow;
            if (this.alternate === null)
                return false;
            if (this.alternate.hasEffects(context))
                return true;
            context.brokenFlow =
                context.brokenFlow < consequentBrokenFlow ? context.brokenFlow : consequentBrokenFlow;
            return false;
        }
        return testValue
            ? this.consequent.hasEffects(context)
            : this.alternate !== null && this.alternate.hasEffects(context);
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (includeChildrenRecursively) {
            this.includeRecursively(includeChildrenRecursively, context);
        }
        else {
            const testValue = this.getTestValue();
            if (testValue === UnknownValue) {
                this.includeUnknownTest(context);
            }
            else {
                this.includeKnownTest(context, testValue);
            }
        }
    }
    render(code, options) {
        // Note that unknown test values are always included
        const testValue = this.getTestValue();
        if (!this.test.included &&
            (testValue ? this.alternate === null || !this.alternate.included : !this.consequent.included)) {
            const singleRetainedBranch = (testValue ? this.consequent : this.alternate);
            code.remove(this.start, singleRetainedBranch.start);
            code.remove(singleRetainedBranch.end, this.end);
            removeAnnotations(this, code);
            singleRetainedBranch.render(code, options);
        }
        else {
            if (this.test.included) {
                this.test.render(code, options);
            }
            else {
                code.overwrite(this.test.start, this.test.end, testValue ? 'true' : 'false');
            }
            if (this.consequent.included) {
                this.consequent.render(code, options);
            }
            else {
                code.overwrite(this.consequent.start, this.consequent.end, ';');
            }
            if (this.alternate !== null) {
                if (this.alternate.included) {
                    if (code.original.charCodeAt(this.alternate.start - 1) === 101 /* e */) {
                        code.prependLeft(this.alternate.start, ' ');
                    }
                    this.alternate.render(code, options);
                }
                else {
                    code.remove(this.consequent.end, this.alternate.end);
                }
            }
        }
    }
    getTestValue() {
        if (this.testValue === unset) {
            return (this.testValue = this.test.getLiteralValueAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this));
        }
        return this.testValue;
    }
    includeKnownTest(context, testValue) {
        if (this.test.shouldBeIncluded(context)) {
            this.test.include(context, false);
        }
        if (testValue && this.consequent.shouldBeIncluded(context)) {
            this.consequent.include(context, false);
        }
        if (this.alternate !== null && !testValue && this.alternate.shouldBeIncluded(context)) {
            this.alternate.include(context, false);
        }
    }
    includeRecursively(includeChildrenRecursively, context) {
        this.test.include(context, includeChildrenRecursively);
        this.consequent.include(context, includeChildrenRecursively);
        if (this.alternate !== null) {
            this.alternate.include(context, includeChildrenRecursively);
        }
    }
    includeUnknownTest(context) {
        this.test.include(context, false);
        const { brokenFlow } = context;
        let consequentBrokenFlow = BROKEN_FLOW_NONE;
        if (this.consequent.shouldBeIncluded(context)) {
            this.consequent.include(context, false);
            consequentBrokenFlow = context.brokenFlow;
            context.brokenFlow = brokenFlow;
        }
        if (this.alternate !== null && this.alternate.shouldBeIncluded(context)) {
            this.alternate.include(context, false);
            context.brokenFlow =
                context.brokenFlow < consequentBrokenFlow ? context.brokenFlow : consequentBrokenFlow;
        }
    }
}

class ImportDeclaration extends NodeBase {
    bind() { }
    hasEffects() {
        return false;
    }
    initialise() {
        this.context.addImport(this);
    }
    render(code, _options, nodeRenderOptions) {
        code.remove(nodeRenderOptions.start, nodeRenderOptions.end);
    }
}
ImportDeclaration.prototype.needsBoundaries = true;

class Import extends NodeBase {
    constructor() {
        super(...arguments);
        this.exportMode = 'auto';
    }
    hasEffects() {
        return true;
    }
    include(context, includeChildrenRecursively) {
        if (!this.included) {
            this.included = true;
            this.context.includeDynamicImport(this);
            this.scope.addAccessedDynamicImport(this);
        }
        this.source.include(context, includeChildrenRecursively);
    }
    initialise() {
        this.context.addDynamicImport(this);
    }
    render(code, options) {
        if (this.inlineNamespace) {
            const _ = options.compact ? '' : ' ';
            const s = options.compact ? '' : ';';
            code.overwrite(this.start, this.end, `Promise.resolve().then(function${_}()${_}{${_}return ${this.inlineNamespace.getName()}${s}${_}})`);
            return;
        }
        const importMechanism = this.getDynamicImportMechanism(options);
        if (importMechanism) {
            code.overwrite(this.start, findFirstOccurrenceOutsideComment(code.original, '(', this.start + 6) + 1, importMechanism.left);
            code.overwrite(this.end - 1, this.end, importMechanism.right);
        }
        this.source.render(code, options);
    }
    renderFinalResolution(code, resolution, format) {
        if (this.included) {
            if (format === 'amd' && resolution.startsWith("'.") && resolution.endsWith(".js'")) {
                resolution = resolution.slice(0, -4) + "'";
            }
            code.overwrite(this.source.start, this.source.end, resolution);
        }
    }
    setResolution(exportMode, inlineNamespace) {
        this.exportMode = exportMode;
        if (inlineNamespace) {
            this.inlineNamespace = inlineNamespace;
        }
        else {
            this.scope.addAccessedGlobalsByFormat({
                amd: ['require'],
                cjs: ['require'],
                system: ['module']
            });
            if (exportMode === 'auto') {
                this.scope.addAccessedGlobalsByFormat({
                    amd: [INTEROP_NAMESPACE_VARIABLE],
                    cjs: [INTEROP_NAMESPACE_VARIABLE]
                });
            }
        }
    }
    getDynamicImportMechanism(options) {
        switch (options.format) {
            case 'cjs': {
                const _ = options.compact ? '' : ' ';
                const resolve = options.compact ? 'c' : 'resolve';
                switch (this.exportMode) {
                    case 'default':
                        return {
                            left: `new Promise(function${_}(${resolve})${_}{${_}${resolve}({${_}'default':${_}require(`,
                            right: `)${_}});${_}})`
                        };
                    case 'auto':
                        return {
                            left: `new Promise(function${_}(${resolve})${_}{${_}${resolve}(${INTEROP_NAMESPACE_VARIABLE}(require(`,
                            right: `)));${_}})`
                        };
                    default:
                        return {
                            left: `new Promise(function${_}(${resolve})${_}{${_}${resolve}(require(`,
                            right: `));${_}})`
                        };
                }
            }
            case 'amd': {
                const _ = options.compact ? '' : ' ';
                const resolve = options.compact ? 'c' : 'resolve';
                const reject = options.compact ? 'e' : 'reject';
                const resolveNamespace = this.exportMode === 'default'
                    ? `function${_}(m)${_}{${_}${resolve}({${_}'default':${_}m${_}});${_}}`
                    : this.exportMode === 'auto'
                        ? `function${_}(m)${_}{${_}${resolve}(${INTEROP_NAMESPACE_VARIABLE}(m));${_}}`
                        : resolve;
                return {
                    left: `new Promise(function${_}(${resolve},${_}${reject})${_}{${_}require([`,
                    right: `],${_}${resolveNamespace},${_}${reject})${_}})`
                };
            }
            case 'system':
                return {
                    left: 'module.import(',
                    right: ')'
                };
            case 'es':
                if (options.dynamicImportFunction) {
                    return {
                        left: `${options.dynamicImportFunction}(`,
                        right: ')'
                    };
                }
        }
        return null;
    }
}

class LabeledStatement extends NodeBase {
    hasEffects(context) {
        const brokenFlow = context.brokenFlow;
        context.ignore.labels.add(this.label.name);
        if (this.body.hasEffects(context))
            return true;
        context.ignore.labels.delete(this.label.name);
        if (context.includedLabels.has(this.label.name)) {
            context.includedLabels.delete(this.label.name);
            context.brokenFlow = brokenFlow;
        }
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        const brokenFlow = context.brokenFlow;
        this.body.include(context, includeChildrenRecursively);
        if (context.includedLabels.has(this.label.name)) {
            this.label.include(context);
            context.includedLabels.delete(this.label.name);
            context.brokenFlow = brokenFlow;
        }
    }
    render(code, options) {
        if (this.label.included) {
            this.label.render(code, options);
        }
        else {
            code.remove(this.start, findFirstOccurrenceOutsideComment(code.original, ':', this.label.end) + 1);
        }
        this.body.render(code, options);
    }
}

class LogicalExpression extends NodeBase {
    constructor() {
        super(...arguments);
        // We collect deoptimization information if usedBranch !== null
        this.expressionsToBeDeoptimized = [];
        this.isBranchResolutionAnalysed = false;
        this.unusedBranch = null;
        this.usedBranch = null;
        this.wasPathDeoptimizedWhileOptimized = false;
    }
    bind() {
        super.bind();
        // ensure the usedBranch is set for the tree-shaking passes
        this.getUsedBranch();
    }
    deoptimizeCache() {
        if (this.usedBranch !== null) {
            this.usedBranch = null;
            const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized;
            this.expressionsToBeDeoptimized = [];
            if (this.wasPathDeoptimizedWhileOptimized) {
                this.unusedBranch.deoptimizePath(UNKNOWN_PATH);
            }
            for (const expression of expressionsToBeDeoptimized) {
                expression.deoptimizeCache();
            }
        }
    }
    deoptimizePath(path) {
        const usedBranch = this.getUsedBranch();
        if (usedBranch === null) {
            this.left.deoptimizePath(path);
            this.right.deoptimizePath(path);
        }
        else {
            this.wasPathDeoptimizedWhileOptimized = true;
            usedBranch.deoptimizePath(path);
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        const usedBranch = this.getUsedBranch();
        if (usedBranch === null)
            return UnknownValue;
        this.expressionsToBeDeoptimized.push(origin);
        return usedBranch.getLiteralValueAtPath(path, recursionTracker, origin);
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        const usedBranch = this.getUsedBranch();
        if (usedBranch === null)
            return new MultiExpression([
                this.left.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin),
                this.right.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin)
            ]);
        this.expressionsToBeDeoptimized.push(origin);
        return usedBranch.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
    }
    hasEffects(context) {
        if (this.usedBranch === null) {
            return this.left.hasEffects(context) || this.right.hasEffects(context);
        }
        return this.usedBranch.hasEffects(context);
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (path.length === 0)
            return false;
        if (this.usedBranch === null) {
            return (this.left.hasEffectsWhenAccessedAtPath(path, context) ||
                this.right.hasEffectsWhenAccessedAtPath(path, context));
        }
        return this.usedBranch.hasEffectsWhenAccessedAtPath(path, context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (path.length === 0)
            return true;
        if (this.usedBranch === null) {
            return (this.left.hasEffectsWhenAssignedAtPath(path, context) ||
                this.right.hasEffectsWhenAssignedAtPath(path, context));
        }
        return this.usedBranch.hasEffectsWhenAssignedAtPath(path, context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (this.usedBranch === null) {
            return (this.left.hasEffectsWhenCalledAtPath(path, callOptions, context) ||
                this.right.hasEffectsWhenCalledAtPath(path, callOptions, context));
        }
        return this.usedBranch.hasEffectsWhenCalledAtPath(path, callOptions, context);
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (includeChildrenRecursively ||
            (this.usedBranch === this.right && this.left.shouldBeIncluded(context)) ||
            this.usedBranch === null) {
            this.left.include(context, includeChildrenRecursively);
            this.right.include(context, includeChildrenRecursively);
        }
        else {
            this.usedBranch.include(context, includeChildrenRecursively);
        }
    }
    render(code, options, { renderedParentType, isCalleeOfRenderedParent, preventASI } = BLANK) {
        if (!this.left.included || !this.right.included) {
            const operatorPos = findFirstOccurrenceOutsideComment(code.original, this.operator, this.left.end);
            if (this.right.included) {
                code.remove(this.start, operatorPos + 2);
                if (preventASI) {
                    removeLineBreaks(code, operatorPos + 2, this.right.start);
                }
            }
            else {
                code.remove(operatorPos, this.end);
            }
            removeAnnotations(this, code);
            this.usedBranch.render(code, options, {
                isCalleeOfRenderedParent: renderedParentType
                    ? isCalleeOfRenderedParent
                    : this.parent.callee === this,
                renderedParentType: renderedParentType || this.parent.type
            });
        }
        else {
            super.render(code, options);
        }
    }
    getUsedBranch() {
        if (!this.isBranchResolutionAnalysed) {
            this.isBranchResolutionAnalysed = true;
            const leftValue = this.left.getLiteralValueAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this);
            if (leftValue === UnknownValue) {
                return null;
            }
            else {
                if (this.operator === '||' ? leftValue : !leftValue) {
                    this.usedBranch = this.left;
                    this.unusedBranch = this.right;
                }
                else {
                    this.usedBranch = this.right;
                    this.unusedBranch = this.left;
                }
            }
        }
        return this.usedBranch;
    }
}

const readFile = (file) => new Promise((fulfil, reject) => readFile$1(file, 'utf-8', (err, contents) => (err ? reject(err) : fulfil(contents))));
function mkdirpath(path) {
    const dir = dirname(path);
    try {
        readdirSync(dir);
    }
    catch (err) {
        mkdirpath(dir);
        try {
            mkdirSync(dir);
        }
        catch (err2) {
            if (err2.code !== 'EEXIST') {
                throw err2;
            }
        }
    }
}
function writeFile(dest, data) {
    return new Promise((fulfil, reject) => {
        mkdirpath(dest);
        writeFile$1(dest, data, err => {
            if (err) {
                reject(err);
            }
            else {
                fulfil();
            }
        });
    });
}

function getRollupDefaultPlugin(preserveSymlinks) {
    return {
        name: 'Rollup Core',
        resolveId: createResolveId(preserveSymlinks),
        load(id) {
            return readFile(id);
        },
        resolveFileUrl({ relativePath, format }) {
            return relativeUrlMechanisms[format](relativePath);
        },
        resolveImportMeta(prop, { chunkId, format }) {
            const mechanism = importMetaMechanisms[format] && importMetaMechanisms[format](prop, chunkId);
            if (mechanism) {
                return mechanism;
            }
        }
    };
}
function findFile(file, preserveSymlinks) {
    try {
        const stats = lstatSync(file);
        if (!preserveSymlinks && stats.isSymbolicLink())
            return findFile(realpathSync(file), preserveSymlinks);
        if ((preserveSymlinks && stats.isSymbolicLink()) || stats.isFile()) {
            // check case
            const name = basename(file);
            const files = readdirSync(dirname(file));
            if (files.indexOf(name) !== -1)
                return file;
        }
    }
    catch (err) {
        // suppress
    }
}
function addJsExtensionIfNecessary(file, preserveSymlinks) {
    let found = findFile(file, preserveSymlinks);
    if (found)
        return found;
    found = findFile(file + '.mjs', preserveSymlinks);
    if (found)
        return found;
    found = findFile(file + '.js', preserveSymlinks);
    return found;
}
function createResolveId(preserveSymlinks) {
    return function (source, importer) {
        if (typeof process === 'undefined') {
            return error({
                code: 'MISSING_PROCESS',
                message: `It looks like you're using Rollup in a non-Node.js environment. This means you must supply a plugin with custom resolveId and load functions`,
                url: 'https://rollupjs.org/guide/en/#a-simple-example'
            });
        }
        // external modules (non-entry modules that start with neither '.' or '/')
        // are skipped at this stage.
        if (importer !== undefined && !isAbsolute(source) && source[0] !== '.')
            return null;
        // `resolve` processes paths from right to left, prepending them until an
        // absolute path is created. Absolute importees therefore shortcircuit the
        // resolve call and require no special handing on our part.
        // See https://nodejs.org/api/path.html#path_path_resolve_paths
        return addJsExtensionIfNecessary(resolve(importer ? dirname(importer) : resolve(), source), preserveSymlinks);
    };
}
const getResolveUrl = (path, URL = 'URL') => `new ${URL}(${path}).href`;
const getUrlFromDocument = (chunkId) => `(document.currentScript && document.currentScript.src || new URL('${chunkId}', document.baseURI).href)`;
const getGenericImportMetaMechanism = (getUrl) => (prop, chunkId) => {
    const urlMechanism = getUrl(chunkId);
    return prop === null ? `({ url: ${urlMechanism} })` : prop === 'url' ? urlMechanism : 'undefined';
};
const importMetaMechanisms = {
    amd: getGenericImportMetaMechanism(() => getResolveUrl(`module.uri, document.baseURI`)),
    cjs: getGenericImportMetaMechanism(chunkId => `(typeof document === 'undefined' ? ${getResolveUrl(`'file:' + __filename`, `(require('u' + 'rl').URL)`)} : ${getUrlFromDocument(chunkId)})`),
    iife: getGenericImportMetaMechanism(chunkId => getUrlFromDocument(chunkId)),
    system: prop => (prop === null ? `module.meta` : `module.meta.${prop}`),
    umd: getGenericImportMetaMechanism(chunkId => `(typeof document === 'undefined' ? ${getResolveUrl(`'file:' + __filename`, `(require('u' + 'rl').URL)`)} : ${getUrlFromDocument(chunkId)})`)
};
const getRelativeUrlFromDocument = (relativePath) => getResolveUrl(`'${relativePath}', document.currentScript && document.currentScript.src || document.baseURI`);
const relativeUrlMechanisms = {
    amd: relativePath => {
        if (relativePath[0] !== '.')
            relativePath = './' + relativePath;
        return getResolveUrl(`require.toUrl('${relativePath}'), document.baseURI`);
    },
    cjs: relativePath => `(typeof document === 'undefined' ? ${getResolveUrl(`'file:' + __dirname + '/${relativePath}'`, `(require('u' + 'rl').URL)`)} : ${getRelativeUrlFromDocument(relativePath)})`,
    es: relativePath => getResolveUrl(`'${relativePath}', import.meta.url`),
    iife: relativePath => getRelativeUrlFromDocument(relativePath),
    system: relativePath => getResolveUrl(`'${relativePath}', module.meta.url`),
    umd: relativePath => `(typeof document === 'undefined' ? ${getResolveUrl(`'file:' + __dirname + '/${relativePath}'`, `(require('u' + 'rl').URL)`)} : ${getRelativeUrlFromDocument(relativePath)})`
};
const accessedMetaUrlGlobals = {
    amd: ['document', 'module', 'URL'],
    cjs: ['document', 'require', 'URL'],
    iife: ['document', 'URL'],
    system: ['module'],
    umd: ['document', 'require', 'URL']
};
const accessedFileUrlGlobals = {
    amd: ['document', 'require', 'URL'],
    cjs: ['document', 'require', 'URL'],
    iife: ['document', 'URL'],
    system: ['module', 'URL'],
    umd: ['document', 'require', 'URL']
};

const ASSET_PREFIX = 'ROLLUP_ASSET_URL_';
const CHUNK_PREFIX = 'ROLLUP_CHUNK_URL_';
const FILE_PREFIX = 'ROLLUP_FILE_URL_';
class MetaProperty extends NodeBase {
    hasEffects() {
        return false;
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    include() {
        if (!this.included) {
            this.included = true;
            const parent = this.parent;
            const metaProperty = (this.metaProperty =
                parent instanceof MemberExpression && typeof parent.propertyKey === 'string'
                    ? parent.propertyKey
                    : null);
            if (metaProperty &&
                (metaProperty.startsWith(FILE_PREFIX) ||
                    metaProperty.startsWith(ASSET_PREFIX) ||
                    metaProperty.startsWith(CHUNK_PREFIX))) {
                this.scope.addAccessedGlobalsByFormat(accessedFileUrlGlobals);
            }
            else {
                this.scope.addAccessedGlobalsByFormat(accessedMetaUrlGlobals);
            }
        }
    }
    initialise() {
        if (this.meta.name === 'import') {
            this.context.addImportMeta(this);
        }
    }
    renderFinalMechanism(code, chunkId, format, outputPluginDriver) {
        if (!this.included)
            return;
        const parent = this.parent;
        const metaProperty = this.metaProperty;
        if (metaProperty &&
            (metaProperty.startsWith(FILE_PREFIX) ||
                metaProperty.startsWith(ASSET_PREFIX) ||
                metaProperty.startsWith(CHUNK_PREFIX))) {
            let referenceId = null;
            let assetReferenceId = null;
            let chunkReferenceId = null;
            let fileName;
            if (metaProperty.startsWith(FILE_PREFIX)) {
                referenceId = metaProperty.substr(FILE_PREFIX.length);
                fileName = outputPluginDriver.getFileName(referenceId);
            }
            else if (metaProperty.startsWith(ASSET_PREFIX)) {
                this.context.warnDeprecation(`Using the "${ASSET_PREFIX}" prefix to reference files is deprecated. Use the "${FILE_PREFIX}" prefix instead.`, false);
                assetReferenceId = metaProperty.substr(ASSET_PREFIX.length);
                fileName = outputPluginDriver.getFileName(assetReferenceId);
            }
            else {
                this.context.warnDeprecation(`Using the "${CHUNK_PREFIX}" prefix to reference files is deprecated. Use the "${FILE_PREFIX}" prefix instead.`, false);
                chunkReferenceId = metaProperty.substr(CHUNK_PREFIX.length);
                fileName = outputPluginDriver.getFileName(chunkReferenceId);
            }
            const relativePath = normalize(relative$1(dirname(chunkId), fileName));
            let replacement;
            if (assetReferenceId !== null) {
                replacement = outputPluginDriver.hookFirstSync('resolveAssetUrl', [
                    {
                        assetFileName: fileName,
                        chunkId,
                        format,
                        moduleId: this.context.module.id,
                        relativeAssetPath: relativePath
                    }
                ]);
            }
            if (!replacement) {
                replacement = outputPluginDriver.hookFirstSync('resolveFileUrl', [
                    {
                        assetReferenceId,
                        chunkId,
                        chunkReferenceId,
                        fileName,
                        format,
                        moduleId: this.context.module.id,
                        referenceId: referenceId || assetReferenceId || chunkReferenceId,
                        relativePath
                    }
                ]);
            }
            code.overwrite(parent.start, parent.end, replacement, { contentOnly: true });
            return;
        }
        const replacement = outputPluginDriver.hookFirstSync('resolveImportMeta', [
            metaProperty,
            {
                chunkId,
                format,
                moduleId: this.context.module.id
            }
        ]);
        if (typeof replacement === 'string') {
            if (parent instanceof MemberExpression) {
                code.overwrite(parent.start, parent.end, replacement, { contentOnly: true });
            }
            else {
                code.overwrite(this.start, this.end, replacement, { contentOnly: true });
            }
        }
    }
}

class MethodDefinition extends NodeBase {
    hasEffects(context) {
        return this.key.hasEffects(context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        return (path.length > 0 || this.value.hasEffectsWhenCalledAtPath(EMPTY_PATH, callOptions, context));
    }
}

class NewExpression extends NodeBase {
    bind() {
        super.bind();
        for (const argument of this.arguments) {
            // This will make sure all properties of parameters behave as "unknown"
            argument.deoptimizePath(UNKNOWN_PATH);
        }
    }
    hasEffects(context) {
        for (const argument of this.arguments) {
            if (argument.hasEffects(context))
                return true;
        }
        if (this.context.annotations && this.annotatedPure)
            return false;
        return (this.callee.hasEffects(context) ||
            this.callee.hasEffectsWhenCalledAtPath(EMPTY_PATH, this.callOptions, context));
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    initialise() {
        this.callOptions = {
            args: this.arguments,
            withNew: true
        };
    }
}

class SpreadElement extends NodeBase {
    bind() {
        super.bind();
        // Only properties of properties of the argument could become subject to reassignment
        // This will also reassign the return values of iterators
        this.argument.deoptimizePath([UnknownKey, UnknownKey]);
    }
}

class ObjectExpression extends NodeBase {
    constructor() {
        super(...arguments);
        this.deoptimizedPaths = new Set();
        // We collect deoptimization information if we can resolve a computed property access
        this.expressionsToBeDeoptimized = new Map();
        this.hasUnknownDeoptimizedProperty = false;
        this.propertyMap = null;
        this.unmatchablePropertiesRead = [];
        this.unmatchablePropertiesWrite = [];
    }
    bind() {
        super.bind();
        // ensure the propertyMap is set for the tree-shaking passes
        this.getPropertyMap();
    }
    // We could also track this per-property but this would quickly become much more complex
    deoptimizeCache() {
        if (!this.hasUnknownDeoptimizedProperty)
            this.deoptimizeAllProperties();
    }
    deoptimizePath(path) {
        if (this.hasUnknownDeoptimizedProperty)
            return;
        const propertyMap = this.getPropertyMap();
        const key = path[0];
        if (path.length === 1) {
            if (typeof key !== 'string') {
                this.deoptimizeAllProperties();
                return;
            }
            if (!this.deoptimizedPaths.has(key)) {
                this.deoptimizedPaths.add(key);
                // we only deoptimizeCache exact matches as in all other cases,
                // we do not return a literal value or return expression
                const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized.get(key);
                if (expressionsToBeDeoptimized) {
                    for (const expression of expressionsToBeDeoptimized) {
                        expression.deoptimizeCache();
                    }
                }
            }
        }
        const subPath = path.length === 1 ? UNKNOWN_PATH : path.slice(1);
        for (const property of typeof key === 'string'
            ? propertyMap[key]
                ? propertyMap[key].propertiesRead
                : []
            : this.properties) {
            property.deoptimizePath(subPath);
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        const propertyMap = this.getPropertyMap();
        const key = path[0];
        if (path.length === 0 ||
            this.hasUnknownDeoptimizedProperty ||
            typeof key !== 'string' ||
            this.deoptimizedPaths.has(key))
            return UnknownValue;
        if (path.length === 1 &&
            !propertyMap[key] &&
            !objectMembers[key] &&
            this.unmatchablePropertiesRead.length === 0) {
            const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized.get(key);
            if (expressionsToBeDeoptimized) {
                expressionsToBeDeoptimized.push(origin);
            }
            else {
                this.expressionsToBeDeoptimized.set(key, [origin]);
            }
            return undefined;
        }
        if (!propertyMap[key] ||
            propertyMap[key].exactMatchRead === null ||
            propertyMap[key].propertiesRead.length > 1) {
            return UnknownValue;
        }
        const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized.get(key);
        if (expressionsToBeDeoptimized) {
            expressionsToBeDeoptimized.push(origin);
        }
        else {
            this.expressionsToBeDeoptimized.set(key, [origin]);
        }
        return propertyMap[key].exactMatchRead.getLiteralValueAtPath(path.slice(1), recursionTracker, origin);
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        const propertyMap = this.getPropertyMap();
        const key = path[0];
        if (path.length === 0 ||
            this.hasUnknownDeoptimizedProperty ||
            typeof key !== 'string' ||
            this.deoptimizedPaths.has(key))
            return UNKNOWN_EXPRESSION;
        if (path.length === 1 &&
            objectMembers[key] &&
            this.unmatchablePropertiesRead.length === 0 &&
            (!propertyMap[key] || propertyMap[key].exactMatchRead === null))
            return getMemberReturnExpressionWhenCalled(objectMembers, key);
        if (!propertyMap[key] ||
            propertyMap[key].exactMatchRead === null ||
            propertyMap[key].propertiesRead.length > 1)
            return UNKNOWN_EXPRESSION;
        const expressionsToBeDeoptimized = this.expressionsToBeDeoptimized.get(key);
        if (expressionsToBeDeoptimized) {
            expressionsToBeDeoptimized.push(origin);
        }
        else {
            this.expressionsToBeDeoptimized.set(key, [origin]);
        }
        return propertyMap[key].exactMatchRead.getReturnExpressionWhenCalledAtPath(path.slice(1), recursionTracker, origin);
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (path.length === 0)
            return false;
        const key = path[0];
        const propertyMap = this.propertyMap;
        if (path.length > 1 &&
            (this.hasUnknownDeoptimizedProperty ||
                typeof key !== 'string' ||
                this.deoptimizedPaths.has(key) ||
                !propertyMap[key] ||
                propertyMap[key].exactMatchRead === null))
            return true;
        const subPath = path.slice(1);
        for (const property of typeof key !== 'string'
            ? this.properties
            : propertyMap[key]
                ? propertyMap[key].propertiesRead
                : []) {
            if (property.hasEffectsWhenAccessedAtPath(subPath, context))
                return true;
        }
        return false;
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        const key = path[0];
        const propertyMap = this.propertyMap;
        if (path.length > 1 &&
            (this.hasUnknownDeoptimizedProperty ||
                this.deoptimizedPaths.has(key) ||
                !propertyMap[key] ||
                propertyMap[key].exactMatchRead === null)) {
            return true;
        }
        const subPath = path.slice(1);
        for (const property of typeof key !== 'string'
            ? this.properties
            : path.length > 1
                ? propertyMap[key].propertiesRead
                : propertyMap[key]
                    ? propertyMap[key].propertiesWrite
                    : []) {
            if (property.hasEffectsWhenAssignedAtPath(subPath, context))
                return true;
        }
        return false;
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        const key = path[0];
        if (typeof key !== 'string' ||
            this.hasUnknownDeoptimizedProperty ||
            this.deoptimizedPaths.has(key) ||
            (this.propertyMap[key]
                ? !this.propertyMap[key].exactMatchRead
                : path.length > 1 || !objectMembers[key])) {
            return true;
        }
        const subPath = path.slice(1);
        if (this.propertyMap[key]) {
            for (const property of this.propertyMap[key].propertiesRead) {
                if (property.hasEffectsWhenCalledAtPath(subPath, callOptions, context))
                    return true;
            }
        }
        if (path.length === 1 && objectMembers[key])
            return hasMemberEffectWhenCalled(objectMembers, key, this.included, callOptions, context);
        return false;
    }
    render(code, options, { renderedParentType } = BLANK) {
        super.render(code, options);
        if (renderedParentType === ExpressionStatement) {
            code.appendRight(this.start, '(');
            code.prependLeft(this.end, ')');
        }
    }
    deoptimizeAllProperties() {
        this.hasUnknownDeoptimizedProperty = true;
        for (const property of this.properties) {
            property.deoptimizePath(UNKNOWN_PATH);
        }
        for (const expressionsToBeDeoptimized of this.expressionsToBeDeoptimized.values()) {
            for (const expression of expressionsToBeDeoptimized) {
                expression.deoptimizeCache();
            }
        }
    }
    getPropertyMap() {
        if (this.propertyMap !== null) {
            return this.propertyMap;
        }
        const propertyMap = (this.propertyMap = Object.create(null));
        for (let index = this.properties.length - 1; index >= 0; index--) {
            const property = this.properties[index];
            if (property instanceof SpreadElement) {
                this.unmatchablePropertiesRead.push(property);
                continue;
            }
            const isWrite = property.kind !== 'get';
            const isRead = property.kind !== 'set';
            let key;
            if (property.computed) {
                const keyValue = property.key.getLiteralValueAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this);
                if (keyValue === UnknownValue) {
                    if (isRead) {
                        this.unmatchablePropertiesRead.push(property);
                    }
                    else {
                        this.unmatchablePropertiesWrite.push(property);
                    }
                    continue;
                }
                key = String(keyValue);
            }
            else if (property.key instanceof Identifier$1) {
                key = property.key.name;
            }
            else {
                key = String(property.key.value);
            }
            const propertyMapProperty = propertyMap[key];
            if (!propertyMapProperty) {
                propertyMap[key] = {
                    exactMatchRead: isRead ? property : null,
                    exactMatchWrite: isWrite ? property : null,
                    propertiesRead: isRead ? [property, ...this.unmatchablePropertiesRead] : [],
                    propertiesWrite: isWrite && !isRead ? [property, ...this.unmatchablePropertiesWrite] : []
                };
                continue;
            }
            if (isRead && propertyMapProperty.exactMatchRead === null) {
                propertyMapProperty.exactMatchRead = property;
                propertyMapProperty.propertiesRead.push(property, ...this.unmatchablePropertiesRead);
            }
            if (isWrite && !isRead && propertyMapProperty.exactMatchWrite === null) {
                propertyMapProperty.exactMatchWrite = property;
                propertyMapProperty.propertiesWrite.push(property, ...this.unmatchablePropertiesWrite);
            }
        }
        return propertyMap;
    }
}

class ObjectPattern extends NodeBase {
    addExportedVariables(variables) {
        for (const property of this.properties) {
            if (property.type === Property) {
                property.value.addExportedVariables(variables);
            }
            else {
                property.argument.addExportedVariables(variables);
            }
        }
    }
    declare(kind, init) {
        const variables = [];
        for (const property of this.properties) {
            variables.push(...property.declare(kind, init));
        }
        return variables;
    }
    deoptimizePath(path) {
        if (path.length === 0) {
            for (const property of this.properties) {
                property.deoptimizePath(path);
            }
        }
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (path.length > 0)
            return true;
        for (const property of this.properties) {
            if (property.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context))
                return true;
        }
        return false;
    }
}

class Program$1 extends NodeBase {
    hasEffects(context) {
        for (const node of this.body) {
            if (node.hasEffects(context))
                return true;
        }
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        for (const node of this.body) {
            if (includeChildrenRecursively || node.shouldBeIncluded(context)) {
                node.include(context, includeChildrenRecursively);
            }
        }
    }
    render(code, options) {
        if (this.body.length) {
            renderStatementList(this.body, code, this.start, this.end, options);
        }
        else {
            super.render(code, options);
        }
    }
}

class Property$1 extends NodeBase {
    constructor() {
        super(...arguments);
        this.declarationInit = null;
        this.returnExpression = null;
    }
    bind() {
        super.bind();
        if (this.kind === 'get') {
            // ensure the returnExpression is set for the tree-shaking passes
            this.getReturnExpression();
        }
        if (this.declarationInit !== null) {
            this.declarationInit.deoptimizePath([UnknownKey, UnknownKey]);
        }
    }
    declare(kind, init) {
        this.declarationInit = init;
        return this.value.declare(kind, UNKNOWN_EXPRESSION);
    }
    // As getter properties directly receive their values from function expressions that always
    // have a fixed return value, there is no known situation where a getter is deoptimized.
    deoptimizeCache() { }
    deoptimizePath(path) {
        if (this.kind === 'get') {
            this.getReturnExpression().deoptimizePath(path);
        }
        else {
            this.value.deoptimizePath(path);
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        if (this.kind === 'get') {
            return this.getReturnExpression().getLiteralValueAtPath(path, recursionTracker, origin);
        }
        return this.value.getLiteralValueAtPath(path, recursionTracker, origin);
    }
    getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin) {
        if (this.kind === 'get') {
            return this.getReturnExpression().getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
        }
        return this.value.getReturnExpressionWhenCalledAtPath(path, recursionTracker, origin);
    }
    hasEffects(context) {
        return this.key.hasEffects(context) || this.value.hasEffects(context);
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        if (this.kind === 'get') {
            const trackedExpressions = context.accessed.getEntities(path);
            if (trackedExpressions.has(this))
                return false;
            trackedExpressions.add(this);
            return (this.value.hasEffectsWhenCalledAtPath(EMPTY_PATH, this.accessorCallOptions, context) ||
                (path.length > 0 && this.returnExpression.hasEffectsWhenAccessedAtPath(path, context)));
        }
        return this.value.hasEffectsWhenAccessedAtPath(path, context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        if (this.kind === 'get') {
            const trackedExpressions = context.assigned.getEntities(path);
            if (trackedExpressions.has(this))
                return false;
            trackedExpressions.add(this);
            return this.returnExpression.hasEffectsWhenAssignedAtPath(path, context);
        }
        if (this.kind === 'set') {
            const trackedExpressions = context.assigned.getEntities(path);
            if (trackedExpressions.has(this))
                return false;
            trackedExpressions.add(this);
            return this.value.hasEffectsWhenCalledAtPath(EMPTY_PATH, this.accessorCallOptions, context);
        }
        return this.value.hasEffectsWhenAssignedAtPath(path, context);
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        if (this.kind === 'get') {
            const trackedExpressions = (callOptions.withNew
                ? context.instantiated
                : context.called).getEntities(path);
            if (trackedExpressions.has(this))
                return false;
            trackedExpressions.add(this);
            return this.returnExpression.hasEffectsWhenCalledAtPath(path, callOptions, context);
        }
        return this.value.hasEffectsWhenCalledAtPath(path, callOptions, context);
    }
    initialise() {
        this.accessorCallOptions = {
            args: NO_ARGS,
            withNew: false
        };
    }
    render(code, options) {
        if (!this.shorthand) {
            this.key.render(code, options);
        }
        this.value.render(code, options, { isShorthandProperty: this.shorthand });
    }
    getReturnExpression() {
        if (this.returnExpression === null) {
            this.returnExpression = UNKNOWN_EXPRESSION;
            return (this.returnExpression = this.value.getReturnExpressionWhenCalledAtPath(EMPTY_PATH, SHARED_RECURSION_TRACKER, this));
        }
        return this.returnExpression;
    }
}

class ReturnStatement$1 extends NodeBase {
    hasEffects(context) {
        if (!context.ignore.returnAwaitYield ||
            (this.argument !== null && this.argument.hasEffects(context)))
            return true;
        context.brokenFlow = BROKEN_FLOW_ERROR_RETURN_LABEL;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (this.argument) {
            this.argument.include(context, includeChildrenRecursively);
        }
        context.brokenFlow = BROKEN_FLOW_ERROR_RETURN_LABEL;
    }
    initialise() {
        this.scope.addReturnExpression(this.argument || UNKNOWN_EXPRESSION);
    }
    render(code, options) {
        if (this.argument) {
            this.argument.render(code, options, { preventASI: true });
            if (this.argument.start === this.start + 6 /* 'return'.length */) {
                code.prependLeft(this.start + 6, ' ');
            }
        }
    }
}

class SequenceExpression extends NodeBase {
    deoptimizePath(path) {
        if (path.length > 0)
            this.expressions[this.expressions.length - 1].deoptimizePath(path);
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        return this.expressions[this.expressions.length - 1].getLiteralValueAtPath(path, recursionTracker, origin);
    }
    hasEffects(context) {
        for (const expression of this.expressions) {
            if (expression.hasEffects(context))
                return true;
        }
        return false;
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        return (path.length > 0 &&
            this.expressions[this.expressions.length - 1].hasEffectsWhenAccessedAtPath(path, context));
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        return (path.length === 0 ||
            this.expressions[this.expressions.length - 1].hasEffectsWhenAssignedAtPath(path, context));
    }
    hasEffectsWhenCalledAtPath(path, callOptions, context) {
        return this.expressions[this.expressions.length - 1].hasEffectsWhenCalledAtPath(path, callOptions, context);
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        for (let i = 0; i < this.expressions.length - 1; i++) {
            const node = this.expressions[i];
            if (includeChildrenRecursively || node.shouldBeIncluded(context))
                node.include(context, includeChildrenRecursively);
        }
        this.expressions[this.expressions.length - 1].include(context, includeChildrenRecursively);
    }
    render(code, options, { renderedParentType, isCalleeOfRenderedParent, preventASI } = BLANK) {
        let includedNodes = 0;
        for (const { node, start, end } of getCommaSeparatedNodesWithBoundaries(this.expressions, code, this.start, this.end)) {
            if (!node.included) {
                treeshakeNode(node, code, start, end);
                continue;
            }
            includedNodes++;
            if (includedNodes === 1 && preventASI) {
                removeLineBreaks(code, start, node.start);
            }
            if (node === this.expressions[this.expressions.length - 1] && includedNodes === 1) {
                node.render(code, options, {
                    isCalleeOfRenderedParent: renderedParentType
                        ? isCalleeOfRenderedParent
                        : this.parent.callee === this,
                    renderedParentType: renderedParentType || this.parent.type
                });
            }
            else {
                node.render(code, options);
            }
        }
    }
}

class SwitchCase extends NodeBase {
    hasEffects(context) {
        if (this.test && this.test.hasEffects(context))
            return true;
        for (const node of this.consequent) {
            if (context.brokenFlow)
                break;
            if (node.hasEffects(context))
                return true;
        }
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        if (this.test)
            this.test.include(context, includeChildrenRecursively);
        for (const node of this.consequent) {
            if (includeChildrenRecursively || node.shouldBeIncluded(context))
                node.include(context, includeChildrenRecursively);
        }
    }
    render(code, options, nodeRenderOptions) {
        if (this.consequent.length) {
            this.test && this.test.render(code, options);
            const testEnd = this.test
                ? this.test.end
                : findFirstOccurrenceOutsideComment(code.original, 'default', this.start) + 7;
            const consequentStart = findFirstOccurrenceOutsideComment(code.original, ':', testEnd) + 1;
            renderStatementList(this.consequent, code, consequentStart, nodeRenderOptions.end, options);
        }
        else {
            super.render(code, options);
        }
    }
}
SwitchCase.prototype.needsBoundaries = true;

class SwitchStatement extends NodeBase {
    createScope(parentScope) {
        this.scope = new BlockScope(parentScope);
    }
    hasEffects(context) {
        if (this.discriminant.hasEffects(context))
            return true;
        const { brokenFlow, ignore: { breaks } } = context;
        let minBrokenFlow = Infinity;
        context.ignore.breaks = true;
        for (const switchCase of this.cases) {
            if (switchCase.hasEffects(context))
                return true;
            minBrokenFlow = context.brokenFlow < minBrokenFlow ? context.brokenFlow : minBrokenFlow;
            context.brokenFlow = brokenFlow;
        }
        if (this.defaultCase !== null && !(minBrokenFlow === BROKEN_FLOW_BREAK_CONTINUE)) {
            context.brokenFlow = minBrokenFlow;
        }
        context.ignore.breaks = breaks;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        this.discriminant.include(context, includeChildrenRecursively);
        const { brokenFlow } = context;
        let minBrokenFlow = Infinity;
        let isCaseIncluded = includeChildrenRecursively ||
            (this.defaultCase !== null && this.defaultCase < this.cases.length - 1);
        for (let caseIndex = this.cases.length - 1; caseIndex >= 0; caseIndex--) {
            const switchCase = this.cases[caseIndex];
            if (switchCase.included) {
                isCaseIncluded = true;
            }
            if (!isCaseIncluded) {
                const hasEffectsContext = createHasEffectsContext();
                hasEffectsContext.ignore.breaks = true;
                isCaseIncluded = switchCase.hasEffects(hasEffectsContext);
            }
            if (isCaseIncluded) {
                switchCase.include(context, includeChildrenRecursively);
                minBrokenFlow = minBrokenFlow < context.brokenFlow ? minBrokenFlow : context.brokenFlow;
                context.brokenFlow = brokenFlow;
            }
            else {
                minBrokenFlow = brokenFlow;
            }
        }
        if (isCaseIncluded &&
            this.defaultCase !== null &&
            !(minBrokenFlow === BROKEN_FLOW_BREAK_CONTINUE)) {
            context.brokenFlow = minBrokenFlow;
        }
    }
    initialise() {
        for (let caseIndex = 0; caseIndex < this.cases.length; caseIndex++) {
            if (this.cases[caseIndex].test === null) {
                this.defaultCase = caseIndex;
                return;
            }
        }
        this.defaultCase = null;
    }
    render(code, options) {
        this.discriminant.render(code, options);
        if (this.cases.length > 0) {
            renderStatementList(this.cases, code, this.cases[0].start, this.end - 1, options);
        }
    }
}

class TaggedTemplateExpression extends NodeBase {
    bind() {
        super.bind();
        if (this.tag.type === Identifier) {
            const name = this.tag.name;
            const variable = this.scope.findVariable(name);
            if (variable.isNamespace) {
                return this.context.error({
                    code: 'CANNOT_CALL_NAMESPACE',
                    message: `Cannot call a namespace ('${name}')`
                }, this.start);
            }
            if (name === 'eval') {
                this.context.warn({
                    code: 'EVAL',
                    message: `Use of eval is strongly discouraged, as it poses security risks and may cause issues with minification`,
                    url: 'https://rollupjs.org/guide/en/#avoiding-eval'
                }, this.start);
            }
        }
    }
    hasEffects(context) {
        return (super.hasEffects(context) ||
            this.tag.hasEffectsWhenCalledAtPath(EMPTY_PATH, this.callOptions, context));
    }
    initialise() {
        this.callOptions = {
            args: NO_ARGS,
            withNew: false
        };
    }
}

class TemplateElement extends NodeBase {
    hasEffects() {
        return false;
    }
}

class TemplateLiteral extends NodeBase {
    getLiteralValueAtPath(path) {
        if (path.length > 0 || this.quasis.length !== 1) {
            return UnknownValue;
        }
        return this.quasis[0].value.cooked;
    }
    render(code, options) {
        code.indentExclusionRanges.push([this.start, this.end]);
        super.render(code, options);
    }
}

class ModuleScope extends ChildScope {
    constructor(parent, context) {
        super(parent);
        this.context = context;
        this.variables.set('this', new LocalVariable('this', null, UNDEFINED_EXPRESSION, context));
    }
    addExportDefaultDeclaration(name, exportDefaultDeclaration, context) {
        const variable = new ExportDefaultVariable(name, exportDefaultDeclaration, context);
        this.variables.set('default', variable);
        return variable;
    }
    addNamespaceMemberAccess(_name, variable) {
        if (variable instanceof GlobalVariable) {
            this.accessedOutsideVariables.set(variable.name, variable);
        }
    }
    deconflict(format) {
        // all module level variables are already deconflicted when deconflicting the chunk
        for (const scope of this.children)
            scope.deconflict(format);
    }
    findLexicalBoundary() {
        return this;
    }
    findVariable(name) {
        const knownVariable = this.variables.get(name) || this.accessedOutsideVariables.get(name);
        if (knownVariable) {
            return knownVariable;
        }
        const variable = this.context.traceVariable(name) || this.parent.findVariable(name);
        if (variable instanceof GlobalVariable) {
            this.accessedOutsideVariables.set(name, variable);
        }
        return variable;
    }
}

class ThisExpression extends NodeBase {
    bind() {
        super.bind();
        this.variable = this.scope.findVariable('this');
    }
    hasEffectsWhenAccessedAtPath(path, context) {
        return path.length > 0 && this.variable.hasEffectsWhenAccessedAtPath(path, context);
    }
    hasEffectsWhenAssignedAtPath(path, context) {
        return this.variable.hasEffectsWhenAssignedAtPath(path, context);
    }
    initialise() {
        this.alias =
            this.scope.findLexicalBoundary() instanceof ModuleScope ? this.context.moduleContext : null;
        if (this.alias === 'undefined') {
            this.context.warn({
                code: 'THIS_IS_UNDEFINED',
                message: `The 'this' keyword is equivalent to 'undefined' at the top level of an ES module, and has been rewritten`,
                url: `https://rollupjs.org/guide/en/#error-this-is-undefined`
            }, this.start);
        }
    }
    render(code) {
        if (this.alias !== null) {
            code.overwrite(this.start, this.end, this.alias, {
                contentOnly: false,
                storeName: true
            });
        }
    }
}

class ThrowStatement extends NodeBase {
    hasEffects() {
        return true;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        this.argument.include(context, includeChildrenRecursively);
        context.brokenFlow = BROKEN_FLOW_ERROR_RETURN_LABEL;
    }
    render(code, options) {
        this.argument.render(code, options, { preventASI: true });
        if (this.argument.start === this.start + 5 /* 'throw'.length */) {
            code.prependLeft(this.start + 5, ' ');
        }
    }
}

class TryStatement extends NodeBase {
    constructor() {
        super(...arguments);
        this.directlyIncluded = false;
    }
    hasEffects(context) {
        return ((this.context.tryCatchDeoptimization
            ? this.block.body.length > 0
            : this.block.hasEffects(context)) ||
            (this.finalizer !== null && this.finalizer.hasEffects(context)));
    }
    include(context, includeChildrenRecursively) {
        const { brokenFlow } = context;
        if (!this.directlyIncluded || !this.context.tryCatchDeoptimization) {
            this.included = true;
            this.directlyIncluded = true;
            this.block.include(context, this.context.tryCatchDeoptimization ? INCLUDE_PARAMETERS : includeChildrenRecursively);
            context.brokenFlow = brokenFlow;
        }
        if (this.handler !== null) {
            this.handler.include(context, includeChildrenRecursively);
            context.brokenFlow = brokenFlow;
        }
        if (this.finalizer !== null) {
            this.finalizer.include(context, includeChildrenRecursively);
        }
    }
}

const unaryOperators = {
    '!': value => !value,
    '+': value => +value,
    '-': value => -value,
    delete: () => UnknownValue,
    typeof: value => typeof value,
    void: () => undefined,
    '~': value => ~value
};
class UnaryExpression extends NodeBase {
    bind() {
        super.bind();
        if (this.operator === 'delete') {
            this.argument.deoptimizePath(EMPTY_PATH);
        }
    }
    getLiteralValueAtPath(path, recursionTracker, origin) {
        if (path.length > 0)
            return UnknownValue;
        const argumentValue = this.argument.getLiteralValueAtPath(EMPTY_PATH, recursionTracker, origin);
        if (argumentValue === UnknownValue)
            return UnknownValue;
        return unaryOperators[this.operator](argumentValue);
    }
    hasEffects(context) {
        if (this.operator === 'typeof' && this.argument instanceof Identifier$1)
            return false;
        return (this.argument.hasEffects(context) ||
            (this.operator === 'delete' &&
                this.argument.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context)));
    }
    hasEffectsWhenAccessedAtPath(path) {
        if (this.operator === 'void') {
            return path.length > 0;
        }
        return path.length > 1;
    }
}

class UnknownNode extends NodeBase {
    hasEffects() {
        return true;
    }
    include(context) {
        super.include(context, true);
    }
}

class UpdateExpression extends NodeBase {
    bind() {
        super.bind();
        this.argument.deoptimizePath(EMPTY_PATH);
        if (this.argument instanceof Identifier$1) {
            const variable = this.scope.findVariable(this.argument.name);
            variable.isReassigned = true;
        }
    }
    hasEffects(context) {
        return (this.argument.hasEffects(context) ||
            this.argument.hasEffectsWhenAssignedAtPath(EMPTY_PATH, context));
    }
    hasEffectsWhenAccessedAtPath(path) {
        return path.length > 1;
    }
    render(code, options) {
        this.argument.render(code, options);
        const variable = this.argument.variable;
        if (options.format === 'system' && variable && variable.exportName) {
            const name = variable.getName();
            if (this.prefix) {
                code.overwrite(this.start, this.end, `exports('${variable.exportName}', ${this.operator}${name})`);
            }
            else {
                let op;
                switch (this.operator) {
                    case '++':
                        op = `${name} + 1`;
                        break;
                    case '--':
                        op = `${name} - 1`;
                        break;
                }
                code.overwrite(this.start, this.end, `(exports('${variable.exportName}', ${op}), ${name}${this.operator})`);
            }
        }
    }
}

function isReassignedExportsMember(variable) {
    return variable.renderBaseName !== null && variable.exportName !== null && variable.isReassigned;
}
function areAllDeclarationsIncludedAndNotExported(declarations) {
    for (const declarator of declarations) {
        if (!declarator.included)
            return false;
        if (declarator.id.type === Identifier) {
            if (declarator.id.variable.exportName)
                return false;
        }
        else {
            const exportedVariables = [];
            declarator.id.addExportedVariables(exportedVariables);
            if (exportedVariables.length > 0)
                return false;
        }
    }
    return true;
}
class VariableDeclaration extends NodeBase {
    deoptimizePath() {
        for (const declarator of this.declarations) {
            declarator.deoptimizePath(EMPTY_PATH);
        }
    }
    hasEffectsWhenAssignedAtPath() {
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        for (const declarator of this.declarations) {
            if (includeChildrenRecursively || declarator.shouldBeIncluded(context))
                declarator.include(context, includeChildrenRecursively);
        }
    }
    includeWithAllDeclaredVariables(includeChildrenRecursively, context) {
        this.included = true;
        for (const declarator of this.declarations) {
            declarator.include(context, includeChildrenRecursively);
        }
    }
    initialise() {
        for (const declarator of this.declarations) {
            declarator.declareDeclarator(this.kind);
        }
    }
    render(code, options, nodeRenderOptions = BLANK) {
        if (areAllDeclarationsIncludedAndNotExported(this.declarations)) {
            for (const declarator of this.declarations) {
                declarator.render(code, options);
            }
            if (!nodeRenderOptions.isNoStatement &&
                code.original.charCodeAt(this.end - 1) !== 59 /*";"*/) {
                code.appendLeft(this.end, ';');
            }
        }
        else {
            this.renderReplacedDeclarations(code, options, nodeRenderOptions);
        }
    }
    renderDeclarationEnd(code, separatorString, lastSeparatorPos, actualContentEnd, renderedContentEnd, addSemicolon, systemPatternExports) {
        if (code.original.charCodeAt(this.end - 1) === 59 /*";"*/) {
            code.remove(this.end - 1, this.end);
        }
        if (addSemicolon) {
            separatorString += ';';
        }
        if (lastSeparatorPos !== null) {
            if (code.original.charCodeAt(actualContentEnd - 1) === 10 /*"\n"*/ &&
                (code.original.charCodeAt(this.end) === 10 /*"\n"*/ ||
                    code.original.charCodeAt(this.end) === 13) /*"\r"*/) {
                actualContentEnd--;
                if (code.original.charCodeAt(actualContentEnd) === 13 /*"\r"*/) {
                    actualContentEnd--;
                }
            }
            if (actualContentEnd === lastSeparatorPos + 1) {
                code.overwrite(lastSeparatorPos, renderedContentEnd, separatorString);
            }
            else {
                code.overwrite(lastSeparatorPos, lastSeparatorPos + 1, separatorString);
                code.remove(actualContentEnd, renderedContentEnd);
            }
        }
        else {
            code.appendLeft(renderedContentEnd, separatorString);
        }
        if (systemPatternExports.length > 0) {
            code.appendLeft(renderedContentEnd, ' ' + getSystemExportStatement(systemPatternExports));
        }
    }
    renderReplacedDeclarations(code, options, { start = this.start, end = this.end, isNoStatement }) {
        const separatedNodes = getCommaSeparatedNodesWithBoundaries(this.declarations, code, this.start + this.kind.length, this.end - (code.original.charCodeAt(this.end - 1) === 59 /*";"*/ ? 1 : 0));
        let actualContentEnd, renderedContentEnd;
        if (/\n\s*$/.test(code.slice(this.start, separatedNodes[0].start))) {
            renderedContentEnd = this.start + this.kind.length;
        }
        else {
            renderedContentEnd = separatedNodes[0].start;
        }
        let lastSeparatorPos = renderedContentEnd - 1;
        code.remove(this.start, lastSeparatorPos);
        let isInDeclaration = false;
        let hasRenderedContent = false;
        let separatorString = '', leadingString, nextSeparatorString;
        const systemPatternExports = [];
        for (const { node, start, separator, contentEnd, end } of separatedNodes) {
            if (!node.included ||
                (node.id instanceof Identifier$1 &&
                    isReassignedExportsMember(node.id.variable) &&
                    node.init === null)) {
                code.remove(start, end);
                continue;
            }
            leadingString = '';
            nextSeparatorString = '';
            if (node.id instanceof Identifier$1 &&
                isReassignedExportsMember(node.id.variable)) {
                if (hasRenderedContent) {
                    separatorString += ';';
                }
                isInDeclaration = false;
            }
            else {
                if (options.format === 'system' && node.init !== null) {
                    if (node.id.type !== Identifier) {
                        node.id.addExportedVariables(systemPatternExports);
                    }
                    else if (node.id.variable.exportName) {
                        code.prependLeft(code.original.indexOf('=', node.id.end) + 1, ` exports('${node.id.variable.safeExportName || node.id.variable.exportName}',`);
                        nextSeparatorString += ')';
                    }
                }
                if (isInDeclaration) {
                    separatorString += ',';
                }
                else {
                    if (hasRenderedContent) {
                        separatorString += ';';
                    }
                    leadingString += `${this.kind} `;
                    isInDeclaration = true;
                }
            }
            if (renderedContentEnd === lastSeparatorPos + 1) {
                code.overwrite(lastSeparatorPos, renderedContentEnd, separatorString + leadingString);
            }
            else {
                code.overwrite(lastSeparatorPos, lastSeparatorPos + 1, separatorString);
                code.appendLeft(renderedContentEnd, leadingString);
            }
            node.render(code, options);
            actualContentEnd = contentEnd;
            renderedContentEnd = end;
            hasRenderedContent = true;
            lastSeparatorPos = separator;
            separatorString = nextSeparatorString;
        }
        if (hasRenderedContent) {
            this.renderDeclarationEnd(code, separatorString, lastSeparatorPos, actualContentEnd, renderedContentEnd, !isNoStatement, systemPatternExports);
        }
        else {
            code.remove(start, end);
        }
    }
}

class VariableDeclarator extends NodeBase {
    declareDeclarator(kind) {
        this.id.declare(kind, this.init || UNDEFINED_EXPRESSION);
    }
    deoptimizePath(path) {
        this.id.deoptimizePath(path);
    }
    render(code, options) {
        // This can happen for hoisted variables in dead branches
        if (this.init !== null && !this.init.included) {
            code.remove(this.id.end, this.end);
            this.id.render(code, options);
        }
        else {
            super.render(code, options);
        }
    }
}

class WhileStatement extends NodeBase {
    hasEffects(context) {
        if (this.test.hasEffects(context))
            return true;
        const { brokenFlow, ignore: { breaks, continues } } = context;
        context.ignore.breaks = true;
        context.ignore.continues = true;
        if (this.body.hasEffects(context))
            return true;
        context.ignore.breaks = breaks;
        context.ignore.continues = continues;
        context.brokenFlow = brokenFlow;
        return false;
    }
    include(context, includeChildrenRecursively) {
        this.included = true;
        this.test.include(context, includeChildrenRecursively);
        const { brokenFlow } = context;
        this.body.include(context, includeChildrenRecursively);
        context.brokenFlow = brokenFlow;
    }
}

class YieldExpression extends NodeBase {
    bind() {
        super.bind();
        if (this.argument !== null) {
            this.argument.deoptimizePath(UNKNOWN_PATH);
        }
    }
    hasEffects(context) {
        return (!context.ignore.returnAwaitYield ||
            (this.argument !== null && this.argument.hasEffects(context)));
    }
    render(code, options) {
        if (this.argument) {
            this.argument.render(code, options);
            if (this.argument.start === this.start + 5 /* 'yield'.length */) {
                code.prependLeft(this.start + 5, ' ');
            }
        }
    }
}

const nodeConstructors = {
    ArrayExpression,
    ArrayPattern,
    ArrowFunctionExpression,
    AssignmentExpression,
    AssignmentPattern,
    AwaitExpression,
    BinaryExpression,
    BlockStatement: BlockStatement$1,
    BreakStatement,
    CallExpression: CallExpression$1,
    CatchClause,
    ClassBody,
    ClassDeclaration,
    ClassExpression,
    ConditionalExpression,
    ContinueStatement,
    DoWhileStatement,
    EmptyStatement,
    ExportAllDeclaration,
    ExportDefaultDeclaration,
    ExportNamedDeclaration,
    ExpressionStatement: ExpressionStatement$1,
    ForInStatement,
    ForOfStatement,
    ForStatement,
    FunctionDeclaration,
    FunctionExpression: FunctionExpression$1,
    Identifier: Identifier$1,
    IfStatement,
    ImportDeclaration,
    ImportExpression: Import,
    LabeledStatement,
    Literal,
    LogicalExpression,
    MemberExpression,
    MetaProperty,
    MethodDefinition,
    NewExpression,
    ObjectExpression,
    ObjectPattern,
    Program: Program$1,
    Property: Property$1,
    RestElement,
    ReturnStatement: ReturnStatement$1,
    SequenceExpression,
    SpreadElement,
    SwitchCase,
    SwitchStatement,
    TaggedTemplateExpression,
    TemplateElement,
    TemplateLiteral,
    ThisExpression,
    ThrowStatement,
    TryStatement,
    UnaryExpression,
    UnknownNode,
    UpdateExpression,
    VariableDeclaration,
    VariableDeclarator,
    WhileStatement,
    YieldExpression
};

class SyntheticNamedExportVariableVariable extends Variable {
    constructor(context, name, defaultVariable) {
        super(name);
        this.context = context;
        this.module = context.module;
        this.defaultVariable = defaultVariable;
        this.setRenderNames(defaultVariable.getName(), name);
    }
    include(context) {
        if (!this.included) {
            this.included = true;
            this.context.includeVariable(context, this.defaultVariable);
        }
    }
}

function getOriginalLocation(sourcemapChain, location) {
    // This cast is guaranteed. If it were a missing Map, it wouldn't have a mappings.
    const filteredSourcemapChain = sourcemapChain.filter(sourcemap => sourcemap.mappings);
    while (filteredSourcemapChain.length > 0) {
        const sourcemap = filteredSourcemapChain.pop();
        const line = sourcemap.mappings[location.line - 1];
        let locationFound = false;
        if (line !== undefined) {
            for (const segment of line) {
                if (segment[0] >= location.column) {
                    if (segment.length === 1)
                        break;
                    location = {
                        column: segment[3],
                        line: segment[2] + 1,
                        name: segment.length === 5 ? sourcemap.names[segment[4]] : undefined,
                        source: sourcemap.sources[segment[1]]
                    };
                    locationFound = true;
                    break;
                }
            }
        }
        if (!locationFound) {
            throw new Error("Can't resolve original location of error.");
        }
    }
    return location;
}

// AST walker module for Mozilla Parser API compatible trees

function skipThrough(node, st, c) { c(node, st); }
function ignore(_node, _st, _c) {}

// Node walkers.

var base$1 = {};

base$1.Program = base$1.BlockStatement = function (node, st, c) {
  for (var i = 0, list = node.body; i < list.length; i += 1)
    {
    var stmt = list[i];

    c(stmt, st, "Statement");
  }
};
base$1.Statement = skipThrough;
base$1.EmptyStatement = ignore;
base$1.ExpressionStatement = base$1.ParenthesizedExpression =
  function (node, st, c) { return c(node.expression, st, "Expression"); };
base$1.IfStatement = function (node, st, c) {
  c(node.test, st, "Expression");
  c(node.consequent, st, "Statement");
  if (node.alternate) { c(node.alternate, st, "Statement"); }
};
base$1.LabeledStatement = function (node, st, c) { return c(node.body, st, "Statement"); };
base$1.BreakStatement = base$1.ContinueStatement = ignore;
base$1.WithStatement = function (node, st, c) {
  c(node.object, st, "Expression");
  c(node.body, st, "Statement");
};
base$1.SwitchStatement = function (node, st, c) {
  c(node.discriminant, st, "Expression");
  for (var i$1 = 0, list$1 = node.cases; i$1 < list$1.length; i$1 += 1) {
    var cs = list$1[i$1];

    if (cs.test) { c(cs.test, st, "Expression"); }
    for (var i = 0, list = cs.consequent; i < list.length; i += 1)
      {
      var cons = list[i];

      c(cons, st, "Statement");
    }
  }
};
base$1.SwitchCase = function (node, st, c) {
  if (node.test) { c(node.test, st, "Expression"); }
  for (var i = 0, list = node.consequent; i < list.length; i += 1)
    {
    var cons = list[i];

    c(cons, st, "Statement");
  }
};
base$1.ReturnStatement = base$1.YieldExpression = base$1.AwaitExpression = function (node, st, c) {
  if (node.argument) { c(node.argument, st, "Expression"); }
};
base$1.ThrowStatement = base$1.SpreadElement =
  function (node, st, c) { return c(node.argument, st, "Expression"); };
base$1.TryStatement = function (node, st, c) {
  c(node.block, st, "Statement");
  if (node.handler) { c(node.handler, st); }
  if (node.finalizer) { c(node.finalizer, st, "Statement"); }
};
base$1.CatchClause = function (node, st, c) {
  if (node.param) { c(node.param, st, "Pattern"); }
  c(node.body, st, "Statement");
};
base$1.WhileStatement = base$1.DoWhileStatement = function (node, st, c) {
  c(node.test, st, "Expression");
  c(node.body, st, "Statement");
};
base$1.ForStatement = function (node, st, c) {
  if (node.init) { c(node.init, st, "ForInit"); }
  if (node.test) { c(node.test, st, "Expression"); }
  if (node.update) { c(node.update, st, "Expression"); }
  c(node.body, st, "Statement");
};
base$1.ForInStatement = base$1.ForOfStatement = function (node, st, c) {
  c(node.left, st, "ForInit");
  c(node.right, st, "Expression");
  c(node.body, st, "Statement");
};
base$1.ForInit = function (node, st, c) {
  if (node.type === "VariableDeclaration") { c(node, st); }
  else { c(node, st, "Expression"); }
};
base$1.DebuggerStatement = ignore;

base$1.FunctionDeclaration = function (node, st, c) { return c(node, st, "Function"); };
base$1.VariableDeclaration = function (node, st, c) {
  for (var i = 0, list = node.declarations; i < list.length; i += 1)
    {
    var decl = list[i];

    c(decl, st);
  }
};
base$1.VariableDeclarator = function (node, st, c) {
  c(node.id, st, "Pattern");
  if (node.init) { c(node.init, st, "Expression"); }
};

base$1.Function = function (node, st, c) {
  if (node.id) { c(node.id, st, "Pattern"); }
  for (var i = 0, list = node.params; i < list.length; i += 1)
    {
    var param = list[i];

    c(param, st, "Pattern");
  }
  c(node.body, st, node.expression ? "Expression" : "Statement");
};

base$1.Pattern = function (node, st, c) {
  if (node.type === "Identifier")
    { c(node, st, "VariablePattern"); }
  else if (node.type === "MemberExpression")
    { c(node, st, "MemberPattern"); }
  else
    { c(node, st); }
};
base$1.VariablePattern = ignore;
base$1.MemberPattern = skipThrough;
base$1.RestElement = function (node, st, c) { return c(node.argument, st, "Pattern"); };
base$1.ArrayPattern = function (node, st, c) {
  for (var i = 0, list = node.elements; i < list.length; i += 1) {
    var elt = list[i];

    if (elt) { c(elt, st, "Pattern"); }
  }
};
base$1.ObjectPattern = function (node, st, c) {
  for (var i = 0, list = node.properties; i < list.length; i += 1) {
    var prop = list[i];

    if (prop.type === "Property") {
      if (prop.computed) { c(prop.key, st, "Expression"); }
      c(prop.value, st, "Pattern");
    } else if (prop.type === "RestElement") {
      c(prop.argument, st, "Pattern");
    }
  }
};

base$1.Expression = skipThrough;
base$1.ThisExpression = base$1.Super = base$1.MetaProperty = ignore;
base$1.ArrayExpression = function (node, st, c) {
  for (var i = 0, list = node.elements; i < list.length; i += 1) {
    var elt = list[i];

    if (elt) { c(elt, st, "Expression"); }
  }
};
base$1.ObjectExpression = function (node, st, c) {
  for (var i = 0, list = node.properties; i < list.length; i += 1)
    {
    var prop = list[i];

    c(prop, st);
  }
};
base$1.FunctionExpression = base$1.ArrowFunctionExpression = base$1.FunctionDeclaration;
base$1.SequenceExpression = function (node, st, c) {
  for (var i = 0, list = node.expressions; i < list.length; i += 1)
    {
    var expr = list[i];

    c(expr, st, "Expression");
  }
};
base$1.TemplateLiteral = function (node, st, c) {
  for (var i = 0, list = node.quasis; i < list.length; i += 1)
    {
    var quasi = list[i];

    c(quasi, st);
  }

  for (var i$1 = 0, list$1 = node.expressions; i$1 < list$1.length; i$1 += 1)
    {
    var expr = list$1[i$1];

    c(expr, st, "Expression");
  }
};
base$1.TemplateElement = ignore;
base$1.UnaryExpression = base$1.UpdateExpression = function (node, st, c) {
  c(node.argument, st, "Expression");
};
base$1.BinaryExpression = base$1.LogicalExpression = function (node, st, c) {
  c(node.left, st, "Expression");
  c(node.right, st, "Expression");
};
base$1.AssignmentExpression = base$1.AssignmentPattern = function (node, st, c) {
  c(node.left, st, "Pattern");
  c(node.right, st, "Expression");
};
base$1.ConditionalExpression = function (node, st, c) {
  c(node.test, st, "Expression");
  c(node.consequent, st, "Expression");
  c(node.alternate, st, "Expression");
};
base$1.NewExpression = base$1.CallExpression = function (node, st, c) {
  c(node.callee, st, "Expression");
  if (node.arguments)
    { for (var i = 0, list = node.arguments; i < list.length; i += 1)
      {
        var arg = list[i];

        c(arg, st, "Expression");
      } }
};
base$1.MemberExpression = function (node, st, c) {
  c(node.object, st, "Expression");
  if (node.computed) { c(node.property, st, "Expression"); }
};
base$1.ExportNamedDeclaration = base$1.ExportDefaultDeclaration = function (node, st, c) {
  if (node.declaration)
    { c(node.declaration, st, node.type === "ExportNamedDeclaration" || node.declaration.id ? "Statement" : "Expression"); }
  if (node.source) { c(node.source, st, "Expression"); }
};
base$1.ExportAllDeclaration = function (node, st, c) {
  c(node.source, st, "Expression");
};
base$1.ImportDeclaration = function (node, st, c) {
  for (var i = 0, list = node.specifiers; i < list.length; i += 1)
    {
    var spec = list[i];

    c(spec, st);
  }
  c(node.source, st, "Expression");
};
base$1.ImportExpression = function (node, st, c) {
  c(node.source, st, "Expression");
};
base$1.ImportSpecifier = base$1.ImportDefaultSpecifier = base$1.ImportNamespaceSpecifier = base$1.Identifier = base$1.Literal = ignore;

base$1.TaggedTemplateExpression = function (node, st, c) {
  c(node.tag, st, "Expression");
  c(node.quasi, st, "Expression");
};
base$1.ClassDeclaration = base$1.ClassExpression = function (node, st, c) { return c(node, st, "Class"); };
base$1.Class = function (node, st, c) {
  if (node.id) { c(node.id, st, "Pattern"); }
  if (node.superClass) { c(node.superClass, st, "Expression"); }
  c(node.body, st);
};
base$1.ClassBody = function (node, st, c) {
  for (var i = 0, list = node.body; i < list.length; i += 1)
    {
    var elt = list[i];

    c(elt, st);
  }
};
base$1.MethodDefinition = base$1.Property = function (node, st, c) {
  if (node.computed) { c(node.key, st, "Expression"); }
  c(node.value, st, "Expression");
};

// @ts-ignore
function handlePureAnnotationsOfNode(node, state, type = node.type) {
    let commentNode = state.commentNodes[state.commentIndex];
    while (commentNode && node.start >= commentNode.end) {
        markPureNode(node, commentNode);
        commentNode = state.commentNodes[++state.commentIndex];
    }
    if (commentNode && commentNode.end <= node.end) {
        base$1[type](node, state, handlePureAnnotationsOfNode);
    }
}
function markPureNode(node, comment) {
    if (node.annotations) {
        node.annotations.push(comment);
    }
    else {
        node.annotations = [comment];
    }
    if (node.type === 'ExpressionStatement') {
        node = node.expression;
    }
    if (node.type === 'CallExpression' || node.type === 'NewExpression') {
        node.annotatedPure = true;
    }
}
const pureCommentRegex = /[@#]__PURE__/;
const isPureComment = (comment) => pureCommentRegex.test(comment.text);
function markPureCallExpressions(comments, esTreeAst) {
    handlePureAnnotationsOfNode(esTreeAst, {
        commentIndex: 0,
        commentNodes: comments.filter(isPureComment)
    });
}

// this looks ridiculous, but it prevents sourcemap tooling from mistaking
// this for an actual sourceMappingURL
let SOURCEMAPPING_URL = 'sourceMa';
SOURCEMAPPING_URL += 'ppingURL';
const SOURCEMAPPING_URL_RE = new RegExp(`^#\\s+${SOURCEMAPPING_URL}=.+\\n?`);

const NOOP = () => { };
let getStartTime = () => [0, 0];
let getElapsedTime = () => 0;
let getMemory = () => 0;
let timers = {};
const normalizeHrTime = (time) => time[0] * 1e3 + time[1] / 1e6;
function setTimeHelpers() {
    if (typeof process !== 'undefined' && typeof process.hrtime === 'function') {
        getStartTime = process.hrtime.bind(process);
        getElapsedTime = previous => normalizeHrTime(process.hrtime(previous));
    }
    else if (typeof performance !== 'undefined' && typeof performance.now === 'function') {
        getStartTime = () => [performance.now(), 0];
        getElapsedTime = previous => performance.now() - previous[0];
    }
    if (typeof process !== 'undefined' && typeof process.memoryUsage === 'function') {
        getMemory = () => process.memoryUsage().heapUsed;
    }
}
function getPersistedLabel(label, level) {
    switch (level) {
        case 1:
            return `# ${label}`;
        case 2:
            return `## ${label}`;
        case 3:
            return label;
        default:
            return `${'  '.repeat(level - 4)}- ${label}`;
    }
}
function timeStartImpl(label, level = 3) {
    label = getPersistedLabel(label, level);
    if (!timers.hasOwnProperty(label)) {
        timers[label] = {
            memory: 0,
            startMemory: undefined,
            startTime: undefined,
            time: 0,
            totalMemory: 0
        };
    }
    const currentMemory = getMemory();
    timers[label].startTime = getStartTime();
    timers[label].startMemory = currentMemory;
}
function timeEndImpl(label, level = 3) {
    label = getPersistedLabel(label, level);
    if (timers.hasOwnProperty(label)) {
        const currentMemory = getMemory();
        timers[label].time += getElapsedTime(timers[label].startTime);
        timers[label].totalMemory = Math.max(timers[label].totalMemory, currentMemory);
        timers[label].memory += currentMemory - timers[label].startMemory;
    }
}
function getTimings() {
    const newTimings = {};
    for (const label of Object.keys(timers)) {
        newTimings[label] = [timers[label].time, timers[label].memory, timers[label].totalMemory];
    }
    return newTimings;
}
let timeStart = NOOP, timeEnd = NOOP;
const TIMED_PLUGIN_HOOKS = {
    load: true,
    ongenerate: true,
    onwrite: true,
    resolveDynamicImport: true,
    resolveId: true,
    transform: true,
    transformBundle: true
};
function getPluginWithTimers(plugin, index) {
    const timedPlugin = {};
    for (const hook of Object.keys(plugin)) {
        if (TIMED_PLUGIN_HOOKS[hook] === true) {
            let timerLabel = `plugin ${index}`;
            if (plugin.name) {
                timerLabel += ` (${plugin.name})`;
            }
            timerLabel += ` - ${hook}`;
            timedPlugin[hook] = function () {
                timeStart(timerLabel, 4);
                const result = plugin[hook].apply(this === timedPlugin ? plugin : this, arguments);
                timeEnd(timerLabel, 4);
                if (result && typeof result.then === 'function') {
                    timeStart(`${timerLabel} (async)`, 4);
                    result.then(() => timeEnd(`${timerLabel} (async)`, 4));
                }
                return result;
            };
        }
        else {
            timedPlugin[hook] = plugin[hook];
        }
    }
    return timedPlugin;
}
function initialiseTimers(inputOptions) {
    if (inputOptions.perf) {
        timers = {};
        setTimeHelpers();
        timeStart = timeStartImpl;
        timeEnd = timeEndImpl;
        inputOptions.plugins = inputOptions.plugins.map(getPluginWithTimers);
    }
    else {
        timeStart = NOOP;
        timeEnd = NOOP;
    }
}

const defaultAcornOptions = {
    ecmaVersion: 2020,
    preserveParens: false,
    sourceType: 'module'
};
function tryParse(module, Parser, acornOptions) {
    try {
        return Parser.parse(module.code, Object.assign(Object.assign(Object.assign({}, defaultAcornOptions), acornOptions), { onComment: (block, text, start, end) => module.comments.push({ block, text, start, end }) }));
    }
    catch (err) {
        let message = err.message.replace(/ \(\d+:\d+\)$/, '');
        if (module.id.endsWith('.json')) {
            message += ' (Note that you need @rollup/plugin-json to import JSON files)';
        }
        else if (!module.id.endsWith('.js')) {
            message += ' (Note that you need plugins to import files that are not JavaScript)';
        }
        return module.error({
            code: 'PARSE_ERROR',
            message,
            parserError: err
        }, err.pos);
    }
}
function handleMissingExport(exportName, importingModule, importedModule, importerStart) {
    return importingModule.error({
        code: 'MISSING_EXPORT',
        message: `'${exportName}' is not exported by ${relativeId(importedModule)}, imported by ${relativeId(importingModule.id)}`,
        url: `https://rollupjs.org/guide/en/#error-name-is-not-exported-by-module`
    }, importerStart);
}
const MISSING_EXPORT_SHIM_DESCRIPTION = {
    identifier: null,
    localName: MISSING_EXPORT_SHIM_VARIABLE
};
function getVariableForExportNameRecursive(target, name, isExportAllSearch, searchedNamesAndModules = new Map()) {
    const searchedModules = searchedNamesAndModules.get(name);
    if (searchedModules) {
        if (searchedModules.has(target)) {
            return null;
        }
        searchedModules.add(target);
    }
    else {
        searchedNamesAndModules.set(name, new Set([target]));
    }
    return target.getVariableForExportName(name, isExportAllSearch, searchedNamesAndModules);
}
class Module {
    constructor(graph, id, moduleSideEffects, syntheticNamedExports, isEntry) {
        this.chunk = null;
        this.chunkFileNames = new Set();
        this.chunkName = null;
        this.comments = [];
        this.dependencies = [];
        this.dynamicallyImportedBy = [];
        this.dynamicDependencies = [];
        this.dynamicImports = [];
        this.entryPointsHash = new Uint8Array(10);
        this.execIndex = Infinity;
        this.exportAllModules = [];
        this.exportAllSources = new Set();
        this.exports = Object.create(null);
        this.exportsAll = Object.create(null);
        this.exportShimVariable = new ExportShimVariable(this);
        this.facadeChunk = null;
        this.importDescriptions = Object.create(null);
        this.importMetas = [];
        this.imports = new Set();
        this.isExecuted = false;
        this.isUserDefinedEntryPoint = false;
        this.manualChunkAlias = null;
        this.reexportDescriptions = Object.create(null);
        this.sources = new Set();
        this.userChunkNames = new Set();
        this.usesTopLevelAwait = false;
        this.allExportNames = null;
        this.defaultExport = null;
        this.namespaceVariable = null;
        this.syntheticExports = new Map();
        this.transformDependencies = [];
        this.transitiveReexports = null;
        this.id = id;
        this.graph = graph;
        this.excludeFromSourcemap = /\0/.test(id);
        this.context = graph.getModuleContext(id);
        this.moduleSideEffects = moduleSideEffects;
        this.syntheticNamedExports = syntheticNamedExports;
        this.isEntryPoint = isEntry;
    }
    basename() {
        const base = basename(this.id);
        const ext = extname(this.id);
        return makeLegal(ext ? base.slice(0, -ext.length) : base);
    }
    bindReferences() {
        this.ast.bind();
    }
    error(props, pos) {
        if (typeof pos === 'number') {
            props.pos = pos;
            let location = locate(this.code, pos, { offsetLine: 1 });
            try {
                location = getOriginalLocation(this.sourcemapChain, location);
            }
            catch (e) {
                this.warn({
                    code: 'SOURCEMAP_ERROR',
                    loc: {
                        column: location.column,
                        file: this.id,
                        line: location.line
                    },
                    message: `Error when using sourcemap for reporting an error: ${e.message}`,
                    pos
                });
            }
            props.loc = {
                column: location.column,
                file: this.id,
                line: location.line
            };
            props.frame = getCodeFrame(this.originalCode, location.line, location.column);
        }
        return error(props);
    }
    getAllExportNames() {
        if (this.allExportNames) {
            return this.allExportNames;
        }
        const allExportNames = (this.allExportNames = new Set());
        for (const name of Object.keys(this.exports)) {
            allExportNames.add(name);
        }
        for (const name of Object.keys(this.reexportDescriptions)) {
            allExportNames.add(name);
        }
        for (const module of this.exportAllModules) {
            if (module instanceof ExternalModule) {
                allExportNames.add(`*${module.id}`);
                continue;
            }
            for (const name of module.getAllExportNames()) {
                if (name !== 'default')
                    allExportNames.add(name);
            }
        }
        return allExportNames;
    }
    getDefaultExport() {
        if (this.defaultExport === null) {
            this.defaultExport = undefined;
            this.defaultExport = this.getVariableForExportName('default');
        }
        if (!this.defaultExport) {
            return error({
                code: Errors.SYNTHETIC_NAMED_EXPORTS_NEED_DEFAULT,
                id: this.id,
                message: `Modules with 'syntheticNamedExports' need a default export.`
            });
        }
        return this.defaultExport;
    }
    getDynamicImportExpressions() {
        return this.dynamicImports.map(({ node }) => {
            const importArgument = node.source;
            if (importArgument instanceof TemplateLiteral &&
                importArgument.quasis.length === 1 &&
                importArgument.quasis[0].value.cooked) {
                return importArgument.quasis[0].value.cooked;
            }
            if (importArgument instanceof Literal && typeof importArgument.value === 'string') {
                return importArgument.value;
            }
            return importArgument;
        });
    }
    getExportNamesByVariable() {
        const exportNamesByVariable = new Map();
        for (const exportName of this.getAllExportNames()) {
            const tracedVariable = this.getVariableForExportName(exportName);
            if (!tracedVariable ||
                !(tracedVariable.included || tracedVariable instanceof ExternalVariable)) {
                continue;
            }
            const existingExportNames = exportNamesByVariable.get(tracedVariable);
            if (existingExportNames) {
                existingExportNames.push(exportName);
            }
            else {
                exportNamesByVariable.set(tracedVariable, [exportName]);
            }
        }
        return exportNamesByVariable;
    }
    getExports() {
        return Object.keys(this.exports);
    }
    getOrCreateNamespace() {
        if (!this.namespaceVariable) {
            this.namespaceVariable = new NamespaceVariable(this.astContext, this.syntheticNamedExports);
            this.namespaceVariable.initialise();
        }
        return this.namespaceVariable;
    }
    getReexports() {
        if (this.transitiveReexports) {
            return this.transitiveReexports;
        }
        // to avoid infinite recursion when using circular `export * from X`
        this.transitiveReexports = [];
        const reexports = new Set();
        for (const name in this.reexportDescriptions) {
            reexports.add(name);
        }
        for (const module of this.exportAllModules) {
            if (module instanceof ExternalModule) {
                reexports.add(`*${module.id}`);
            }
            else {
                for (const name of module.getExports().concat(module.getReexports())) {
                    if (name !== 'default')
                        reexports.add(name);
                }
            }
        }
        return (this.transitiveReexports = Array.from(reexports));
    }
    getRenderedExports() {
        // only direct exports are counted here, not reexports at all
        const renderedExports = [];
        const removedExports = [];
        for (const exportName in this.exports) {
            const variable = this.getVariableForExportName(exportName);
            (variable && variable.included ? renderedExports : removedExports).push(exportName);
        }
        return { renderedExports, removedExports };
    }
    getTransitiveDependencies() {
        return this.dependencies.concat(this.getReexports()
            .concat(this.getExports())
            .map((exportName) => this.getVariableForExportName(exportName).module));
    }
    getVariableForExportName(name, isExportAllSearch, searchedNamesAndModules) {
        if (name[0] === '*') {
            if (name.length === 1) {
                return this.getOrCreateNamespace();
            }
            else {
                // export * from 'external'
                const module = this.graph.moduleById.get(name.slice(1));
                return module.getVariableForExportName('*');
            }
        }
        // export { foo } from './other'
        const reexportDeclaration = this.reexportDescriptions[name];
        if (reexportDeclaration) {
            const declaration = getVariableForExportNameRecursive(reexportDeclaration.module, reexportDeclaration.localName, false, searchedNamesAndModules);
            if (!declaration) {
                return handleMissingExport(reexportDeclaration.localName, this, reexportDeclaration.module.id, reexportDeclaration.start);
            }
            return declaration;
        }
        const exportDeclaration = this.exports[name];
        if (exportDeclaration) {
            if (exportDeclaration === MISSING_EXPORT_SHIM_DESCRIPTION) {
                return this.exportShimVariable;
            }
            const name = exportDeclaration.localName;
            return this.traceVariable(name) || this.graph.scope.findVariable(name);
        }
        if (name !== 'default') {
            for (const module of this.exportAllModules) {
                const declaration = getVariableForExportNameRecursive(module, name, true, searchedNamesAndModules);
                if (declaration)
                    return declaration;
            }
        }
        // we don't want to create shims when we are just
        // probing export * modules for exports
        if (!isExportAllSearch) {
            if (this.syntheticNamedExports) {
                let syntheticExport = this.syntheticExports.get(name);
                if (!syntheticExport) {
                    const defaultExport = this.getDefaultExport();
                    syntheticExport = new SyntheticNamedExportVariableVariable(this.astContext, name, defaultExport);
                    this.syntheticExports.set(name, syntheticExport);
                    return syntheticExport;
                }
                return syntheticExport;
            }
            if (this.graph.shimMissingExports) {
                this.shimMissingExport(name);
                return this.exportShimVariable;
            }
        }
        return null;
    }
    include() {
        const context = createInclusionContext();
        if (this.ast.shouldBeIncluded(context))
            this.ast.include(context, false);
    }
    includeAllExports() {
        if (!this.isExecuted) {
            this.graph.needsTreeshakingPass = true;
            markModuleAndImpureDependenciesAsExecuted(this);
        }
        const context = createInclusionContext();
        for (const exportName of this.getExports()) {
            const variable = this.getVariableForExportName(exportName);
            variable.deoptimizePath(UNKNOWN_PATH);
            if (!variable.included) {
                variable.include(context);
                this.graph.needsTreeshakingPass = true;
            }
        }
        for (const name of this.getReexports()) {
            const variable = this.getVariableForExportName(name);
            variable.deoptimizePath(UNKNOWN_PATH);
            if (!variable.included) {
                variable.include(context);
                this.graph.needsTreeshakingPass = true;
            }
            if (variable instanceof ExternalVariable) {
                variable.module.reexported = true;
            }
        }
    }
    includeAllInBundle() {
        this.ast.include(createInclusionContext(), true);
    }
    isIncluded() {
        return this.ast.included || (this.namespaceVariable && this.namespaceVariable.included);
    }
    linkDependencies() {
        for (const source of this.sources) {
            const id = this.resolvedIds[source].id;
            if (id) {
                const module = this.graph.moduleById.get(id);
                this.dependencies.push(module);
            }
        }
        for (const { resolution } of this.dynamicImports) {
            if (resolution instanceof Module || resolution instanceof ExternalModule) {
                this.dynamicDependencies.push(resolution);
            }
        }
        this.addModulesToImportDescriptions(this.importDescriptions);
        this.addModulesToImportDescriptions(this.reexportDescriptions);
        const externalExportAllModules = [];
        for (const source of this.exportAllSources) {
            const module = this.graph.moduleById.get(this.resolvedIds[source].id);
            (module instanceof ExternalModule ? externalExportAllModules : this.exportAllModules).push(module);
        }
        this.exportAllModules = this.exportAllModules.concat(externalExportAllModules);
    }
    render(options) {
        const magicString = this.magicString.clone();
        this.ast.render(magicString, options);
        this.usesTopLevelAwait = this.astContext.usesTopLevelAwait;
        return magicString;
    }
    setSource({ ast, code, customTransformCache, moduleSideEffects, originalCode, originalSourcemap, resolvedIds, sourcemapChain, syntheticNamedExports, transformDependencies, transformFiles }) {
        this.code = code;
        this.originalCode = originalCode;
        this.originalSourcemap = originalSourcemap;
        this.sourcemapChain = sourcemapChain;
        if (transformFiles) {
            this.transformFiles = transformFiles;
        }
        this.transformDependencies = transformDependencies;
        this.customTransformCache = customTransformCache;
        if (typeof moduleSideEffects === 'boolean') {
            this.moduleSideEffects = moduleSideEffects;
        }
        if (typeof syntheticNamedExports === 'boolean') {
            this.syntheticNamedExports = syntheticNamedExports;
        }
        timeStart('generate ast', 3);
        this.esTreeAst = ast || tryParse(this, this.graph.acornParser, this.graph.acornOptions);
        markPureCallExpressions(this.comments, this.esTreeAst);
        timeEnd('generate ast', 3);
        this.resolvedIds = resolvedIds || Object.create(null);
        // By default, `id` is the file name. Custom resolvers and loaders
        // can change that, but it makes sense to use it for the source file name
        const fileName = this.id;
        this.magicString = new MagicString(code, {
            filename: (this.excludeFromSourcemap ? null : fileName),
            indentExclusionRanges: []
        });
        this.removeExistingSourceMap();
        timeStart('analyse ast', 3);
        this.astContext = {
            addDynamicImport: this.addDynamicImport.bind(this),
            addExport: this.addExport.bind(this),
            addImport: this.addImport.bind(this),
            addImportMeta: this.addImportMeta.bind(this),
            annotations: (this.graph.treeshakingOptions && this.graph.treeshakingOptions.annotations),
            code,
            deoptimizationTracker: this.graph.deoptimizationTracker,
            error: this.error.bind(this),
            fileName,
            getExports: this.getExports.bind(this),
            getModuleExecIndex: () => this.execIndex,
            getModuleName: this.basename.bind(this),
            getReexports: this.getReexports.bind(this),
            importDescriptions: this.importDescriptions,
            includeDynamicImport: this.includeDynamicImport.bind(this),
            includeVariable: this.includeVariable.bind(this),
            isCrossChunkImport: importDescription => importDescription.module.chunk !== this.chunk,
            magicString: this.magicString,
            module: this,
            moduleContext: this.context,
            nodeConstructors,
            preserveModules: this.graph.preserveModules,
            propertyReadSideEffects: (!this.graph.treeshakingOptions ||
                this.graph.treeshakingOptions.propertyReadSideEffects),
            traceExport: this.getVariableForExportName.bind(this),
            traceVariable: this.traceVariable.bind(this),
            treeshake: !!this.graph.treeshakingOptions,
            tryCatchDeoptimization: (!this.graph.treeshakingOptions ||
                this.graph.treeshakingOptions.tryCatchDeoptimization),
            unknownGlobalSideEffects: (!this.graph.treeshakingOptions ||
                this.graph.treeshakingOptions.unknownGlobalSideEffects),
            usesTopLevelAwait: false,
            warn: this.warn.bind(this),
            warnDeprecation: this.graph.warnDeprecation.bind(this.graph)
        };
        this.scope = new ModuleScope(this.graph.scope, this.astContext);
        this.ast = new Program$1(this.esTreeAst, { type: 'Module', context: this.astContext }, this.scope);
        timeEnd('analyse ast', 3);
    }
    toJSON() {
        return {
            ast: this.esTreeAst,
            code: this.code,
            customTransformCache: this.customTransformCache,
            dependencies: this.dependencies.map(module => module.id),
            id: this.id,
            moduleSideEffects: this.moduleSideEffects,
            originalCode: this.originalCode,
            originalSourcemap: this.originalSourcemap,
            resolvedIds: this.resolvedIds,
            sourcemapChain: this.sourcemapChain,
            syntheticNamedExports: this.syntheticNamedExports,
            transformDependencies: this.transformDependencies,
            transformFiles: this.transformFiles
        };
    }
    traceVariable(name) {
        const localVariable = this.scope.variables.get(name);
        if (localVariable) {
            return localVariable;
        }
        if (name in this.importDescriptions) {
            const importDeclaration = this.importDescriptions[name];
            const otherModule = importDeclaration.module;
            if (otherModule instanceof Module && importDeclaration.name === '*') {
                return otherModule.getOrCreateNamespace();
            }
            const declaration = otherModule.getVariableForExportName(importDeclaration.name);
            if (!declaration) {
                return handleMissingExport(importDeclaration.name, this, otherModule.id, importDeclaration.start);
            }
            return declaration;
        }
        return null;
    }
    warn(warning, pos) {
        if (typeof pos === 'number') {
            warning.pos = pos;
            const { line, column } = locate(this.code, pos, { offsetLine: 1 }); // TODO trace sourcemaps, cf. error()
            warning.loc = { file: this.id, line, column };
            warning.frame = getCodeFrame(this.code, line, column);
        }
        warning.id = this.id;
        this.graph.warn(warning);
    }
    addDynamicImport(node) {
        this.dynamicImports.push({ node, resolution: null });
    }
    addExport(node) {
        if (node instanceof ExportDefaultDeclaration) {
            // export default foo;
            this.exports.default = {
                identifier: node.variable.getAssignedVariableName(),
                localName: 'default'
            };
        }
        else if (node instanceof ExportAllDeclaration) {
            // export * from './other'
            const source = node.source.value;
            this.sources.add(source);
            this.exportAllSources.add(source);
        }
        else if (node.source instanceof Literal) {
            // export { name } from './other'
            const source = node.source.value;
            this.sources.add(source);
            for (const specifier of node.specifiers) {
                const name = specifier.exported.name;
                this.reexportDescriptions[name] = {
                    localName: specifier.type === ExportNamespaceSpecifier ? '*' : specifier.local.name,
                    module: null,
                    source,
                    start: specifier.start
                };
            }
        }
        else if (node.declaration) {
            const declaration = node.declaration;
            if (declaration instanceof VariableDeclaration) {
                // export var { foo, bar } = ...
                // export var foo = 1, bar = 2;
                for (const declarator of declaration.declarations) {
                    for (const localName of extractAssignedNames(declarator.id)) {
                        this.exports[localName] = { identifier: null, localName };
                    }
                }
            }
            else {
                // export function foo () {}
                const localName = declaration.id.name;
                this.exports[localName] = { identifier: null, localName };
            }
        }
        else {
            // export { foo, bar, baz }
            for (const specifier of node.specifiers) {
                const localName = specifier.local.name;
                const exportedName = specifier.exported.name;
                this.exports[exportedName] = { identifier: null, localName };
            }
        }
    }
    addImport(node) {
        const source = node.source.value;
        this.sources.add(source);
        for (const specifier of node.specifiers) {
            const localName = specifier.local.name;
            if (this.importDescriptions[localName]) {
                return this.error({
                    code: 'DUPLICATE_IMPORT',
                    message: `Duplicated import '${localName}'`
                }, specifier.start);
            }
            const isDefault = specifier.type === ImportDefaultSpecifier;
            const isNamespace = specifier.type === ImportNamespaceSpecifier;
            const name = isDefault
                ? 'default'
                : isNamespace
                    ? '*'
                    : specifier.imported.name;
            this.importDescriptions[localName] = {
                module: null,
                name,
                source,
                start: specifier.start
            };
        }
    }
    addImportMeta(node) {
        this.importMetas.push(node);
    }
    addModulesToImportDescriptions(importDescription) {
        for (const name of Object.keys(importDescription)) {
            const specifier = importDescription[name];
            const id = this.resolvedIds[specifier.source].id;
            specifier.module = this.graph.moduleById.get(id);
        }
    }
    includeDynamicImport(node) {
        const resolution = this.dynamicImports.find(dynamicImport => dynamicImport.node === node).resolution;
        if (resolution instanceof Module) {
            resolution.dynamicallyImportedBy.push(this);
            resolution.includeAllExports();
        }
    }
    includeVariable(context, variable) {
        const variableModule = variable.module;
        if (!variable.included) {
            variable.include(context);
            this.graph.needsTreeshakingPass = true;
        }
        if (variableModule && variableModule !== this) {
            this.imports.add(variable);
        }
    }
    removeExistingSourceMap() {
        for (const comment of this.comments) {
            if (!comment.block && SOURCEMAPPING_URL_RE.test(comment.text)) {
                this.magicString.remove(comment.start, comment.end);
            }
        }
    }
    shimMissingExport(name) {
        if (!this.exports[name]) {
            this.graph.warn({
                code: 'SHIMMED_EXPORT',
                exporter: relativeId(this.id),
                exportName: name,
                message: `Missing export "${name}" has been shimmed in module ${relativeId(this.id)}.`
            });
            this.exports[name] = MISSING_EXPORT_SHIM_DESCRIPTION;
        }
    }
}

class Source {
    constructor(filename, content) {
        this.isOriginal = true;
        this.filename = filename;
        this.content = content;
    }
    traceSegment(line, column, name) {
        return { line, column, name, source: this };
    }
}
class Link {
    constructor(map, sources) {
        this.sources = sources;
        this.names = map.names;
        this.mappings = map.mappings;
    }
    traceMappings() {
        const sources = [];
        const sourcesContent = [];
        const names = [];
        const mappings = [];
        for (const line of this.mappings) {
            const tracedLine = [];
            for (const segment of line) {
                if (segment.length == 1)
                    continue;
                const source = this.sources[segment[1]];
                if (!source)
                    continue;
                const traced = source.traceSegment(segment[2], segment[3], segment.length === 5 ? this.names[segment[4]] : '');
                if (traced) {
                    // newer sources are more likely to be used, so search backwards.
                    let sourceIndex = sources.lastIndexOf(traced.source.filename);
                    if (sourceIndex === -1) {
                        sourceIndex = sources.length;
                        sources.push(traced.source.filename);
                        sourcesContent[sourceIndex] = traced.source.content;
                    }
                    else if (sourcesContent[sourceIndex] == null) {
                        sourcesContent[sourceIndex] = traced.source.content;
                    }
                    else if (traced.source.content != null &&
                        sourcesContent[sourceIndex] !== traced.source.content) {
                        return error({
                            message: `Multiple conflicting contents for sourcemap source ${traced.source.filename}`
                        });
                    }
                    const tracedSegment = [
                        segment[0],
                        sourceIndex,
                        traced.line,
                        traced.column
                    ];
                    if (traced.name) {
                        let nameIndex = names.indexOf(traced.name);
                        if (nameIndex === -1) {
                            nameIndex = names.length;
                            names.push(traced.name);
                        }
                        tracedSegment[4] = nameIndex;
                    }
                    tracedLine.push(tracedSegment);
                }
            }
            mappings.push(tracedLine);
        }
        return { sources, sourcesContent, names, mappings };
    }
    traceSegment(line, column, name) {
        const segments = this.mappings[line];
        if (!segments)
            return null;
        // binary search through segments for the given column
        let i = 0;
        let j = segments.length - 1;
        while (i <= j) {
            const m = (i + j) >> 1;
            const segment = segments[m];
            if (segment[0] === column) {
                if (segment.length == 1)
                    return null;
                const source = this.sources[segment[1]];
                if (!source)
                    return null;
                return source.traceSegment(segment[2], segment[3], segment.length === 5 ? this.names[segment[4]] : name);
            }
            if (segment[0] > column) {
                j = m - 1;
            }
            else {
                i = m + 1;
            }
        }
        return null;
    }
}
function getLinkMap(graph) {
    return function linkMap(source, map) {
        if (map.mappings) {
            return new Link(map, [source]);
        }
        graph.warn({
            code: 'SOURCEMAP_BROKEN',
            message: `Sourcemap is likely to be incorrect: a plugin (${map.plugin}) was used to transform ` +
                "files, but didn't generate a sourcemap for the transformation. Consult the plugin " +
                'documentation for help',
            plugin: map.plugin,
            url: `https://rollupjs.org/guide/en/#warning-sourcemap-is-likely-to-be-incorrect`
        });
        return new Link({
            mappings: [],
            names: []
        }, [source]);
    };
}
function getCollapsedSourcemap(id, originalCode, originalSourcemap, sourcemapChain, linkMap) {
    let source;
    if (!originalSourcemap) {
        source = new Source(id, originalCode);
    }
    else {
        const sources = originalSourcemap.sources;
        const sourcesContent = originalSourcemap.sourcesContent || [];
        // TODO indiscriminately treating IDs and sources as normal paths is probably bad.
        const directory = dirname(id) || '.';
        const sourceRoot = originalSourcemap.sourceRoot || '.';
        const baseSources = sources.map((source, i) => new Source(resolve(directory, sourceRoot, source), sourcesContent[i]));
        source = new Link(originalSourcemap, baseSources);
    }
    return sourcemapChain.reduce(linkMap, source);
}
function collapseSourcemaps(bundle, file, map, modules, bundleSourcemapChain, excludeContent) {
    const linkMap = getLinkMap(bundle.graph);
    const moduleSources = modules
        .filter(module => !module.excludeFromSourcemap)
        .map(module => getCollapsedSourcemap(module.id, module.originalCode, module.originalSourcemap, module.sourcemapChain, linkMap));
    // DecodedSourceMap (from magic-string) uses a number[] instead of the more
    // correct SourceMapSegment tuples. Cast it here to gain type safety.
    let source = new Link(map, moduleSources);
    source = bundleSourcemapChain.reduce(linkMap, source);
    let { sources, sourcesContent, names, mappings } = source.traceMappings();
    if (file) {
        const directory = dirname(file);
        sources = sources.map((source) => relative$1(directory, source));
        file = basename(file);
    }
    sourcesContent = (excludeContent ? null : sourcesContent);
    return new SourceMap({ file, sources, sourcesContent, names, mappings });
}
function collapseSourcemap(graph, id, originalCode, originalSourcemap, sourcemapChain) {
    if (!sourcemapChain.length) {
        return originalSourcemap;
    }
    const source = getCollapsedSourcemap(id, originalCode, originalSourcemap, sourcemapChain, getLinkMap(graph));
    const map = source.traceMappings();
    return Object.assign({ version: 3 }, map);
}

const DECONFLICT_IMPORTED_VARIABLES_BY_FORMAT = {
    amd: deconflictImportsOther,
    cjs: deconflictImportsOther,
    es: deconflictImportsEsm,
    iife: deconflictImportsOther,
    system: deconflictImportsEsm,
    umd: deconflictImportsOther
};
function deconflictChunk(modules, dependencies, imports, usedNames, format, interop, preserveModules) {
    for (const module of modules) {
        module.scope.addUsedOutsideNames(usedNames, format);
    }
    deconflictTopLevelVariables(usedNames, modules);
    DECONFLICT_IMPORTED_VARIABLES_BY_FORMAT[format](usedNames, imports, dependencies, interop, preserveModules);
    for (const module of modules) {
        module.scope.deconflict(format);
    }
}
function deconflictImportsEsm(usedNames, imports, _dependencies, interop) {
    for (const variable of imports) {
        const module = variable.module;
        const name = variable.name;
        let proposedName;
        if (module instanceof ExternalModule && (name === '*' || name === 'default')) {
            if (name === 'default' && interop && module.exportsNamespace) {
                proposedName = module.variableName + '__default';
            }
            else {
                proposedName = module.variableName;
            }
        }
        else {
            proposedName = name;
        }
        variable.setRenderNames(null, getSafeName(proposedName, usedNames));
    }
}
function deconflictImportsOther(usedNames, imports, dependencies, interop, preserveModules) {
    for (const chunkOrExternalModule of dependencies) {
        chunkOrExternalModule.variableName = getSafeName(chunkOrExternalModule.variableName, usedNames);
    }
    for (const variable of imports) {
        const module = variable.module;
        if (module instanceof ExternalModule) {
            const name = variable.name;
            if (name === 'default' && interop && (module.exportsNamespace || module.exportsNames)) {
                variable.setRenderNames(null, module.variableName + '__default');
            }
            else if (name === '*' || name === 'default') {
                variable.setRenderNames(null, module.variableName);
            }
            else {
                variable.setRenderNames(module.variableName, null);
            }
        }
        else {
            const chunk = module.chunk;
            if (chunk.exportMode === 'default' || (preserveModules && variable.isNamespace)) {
                variable.setRenderNames(null, chunk.variableName);
            }
            else {
                variable.setRenderNames(chunk.variableName, chunk.getVariableExportName(variable));
            }
        }
    }
}
function deconflictTopLevelVariables(usedNames, modules) {
    for (const module of modules) {
        for (const variable of module.scope.variables.values()) {
            if (variable.included &&
                // this will only happen for exports in some formats
                !(variable.renderBaseName ||
                    (variable instanceof ExportDefaultVariable && variable.getOriginalVariable() !== variable))) {
                variable.setRenderNames(null, getSafeName(variable.name, usedNames));
            }
        }
        const namespace = module.getOrCreateNamespace();
        if (namespace.included) {
            namespace.setRenderNames(null, getSafeName(namespace.name, usedNames));
        }
    }
}

const compareExecIndex = (unitA, unitB) => unitA.execIndex > unitB.execIndex ? 1 : -1;
function sortByExecutionOrder(units) {
    units.sort(compareExecIndex);
}
function analyseModuleExecution(entryModules) {
    let nextExecIndex = 0;
    const cyclePaths = [];
    const analysedModules = {};
    const orderedModules = [];
    const dynamicImports = [];
    const parents = {};
    const analyseModule = (module) => {
        if (analysedModules[module.id])
            return;
        if (module instanceof ExternalModule) {
            module.execIndex = nextExecIndex++;
            analysedModules[module.id] = true;
            return;
        }
        for (const dependency of module.dependencies) {
            if (dependency.id in parents) {
                if (!analysedModules[dependency.id]) {
                    cyclePaths.push(getCyclePath(dependency.id, module.id, parents));
                }
                continue;
            }
            parents[dependency.id] = module.id;
            analyseModule(dependency);
        }
        for (const { resolution } of module.dynamicImports) {
            if (resolution instanceof Module && dynamicImports.indexOf(resolution) === -1) {
                dynamicImports.push(resolution);
            }
        }
        module.execIndex = nextExecIndex++;
        analysedModules[module.id] = true;
        orderedModules.push(module);
    };
    for (const curEntry of entryModules) {
        if (!parents[curEntry.id]) {
            parents[curEntry.id] = null;
            analyseModule(curEntry);
        }
    }
    for (const curEntry of dynamicImports) {
        if (!parents[curEntry.id]) {
            parents[curEntry.id] = null;
            analyseModule(curEntry);
        }
    }
    return { orderedModules, cyclePaths };
}
function getCyclePath(id, parentId, parents) {
    const path = [relativeId(id)];
    let curId = parentId;
    while (curId !== id) {
        path.push(relativeId(curId));
        curId = parents[curId];
        if (!curId)
            break;
    }
    path.push(path[0]);
    path.reverse();
    return path;
}

function guessIndentString(code) {
    const lines = code.split('\n');
    const tabbed = lines.filter(line => /^\t+/.test(line));
    const spaced = lines.filter(line => /^ {2,}/.test(line));
    if (tabbed.length === 0 && spaced.length === 0) {
        return null;
    }
    // More lines tabbed than spaced? Assume tabs, and
    // default to tabs in the case of a tie (or nothing
    // to go on)
    if (tabbed.length >= spaced.length) {
        return '\t';
    }
    // Otherwise, we need to guess the multiple
    const min = spaced.reduce((previous, current) => {
        const numSpaces = /^ +/.exec(current)[0].length;
        return Math.min(numSpaces, previous);
    }, Infinity);
    return new Array(min + 1).join(' ');
}
function getIndentString(modules, options) {
    if (options.indent !== true)
        return options.indent || '';
    for (let i = 0; i < modules.length; i++) {
        const indent = guessIndentString(modules[i].originalCode);
        if (indent !== null)
            return indent;
    }
    return '\t';
}

function decodedSourcemap(map) {
    if (!map)
        return null;
    if (typeof map === 'string') {
        map = JSON.parse(map);
    }
    if (map.mappings === '') {
        return {
            mappings: [],
            names: [],
            sources: [],
            version: 3
        };
    }
    let mappings;
    if (typeof map.mappings === 'string') {
        mappings = decode(map.mappings);
    }
    else {
        mappings = map.mappings;
    }
    return Object.assign(Object.assign({}, map), { mappings });
}

function renderChunk({ chunk, code, options, outputPluginDriver, renderChunk, sourcemapChain }) {
    const renderChunkReducer = (code, result, plugin) => {
        if (result == null)
            return code;
        if (typeof result === 'string')
            result = {
                code: result,
                map: undefined
            };
        // strict null check allows 'null' maps to not be pushed to the chain, while 'undefined' gets the missing map warning
        if (result.map !== null) {
            const map = decodedSourcemap(result.map);
            sourcemapChain.push(map || { missing: true, plugin: plugin.name });
        }
        return result.code;
    };
    let inTransformBundle = false;
    let inRenderChunk = true;
    return outputPluginDriver
        .hookReduceArg0('renderChunk', [code, renderChunk, options], renderChunkReducer)
        .then(code => {
        inRenderChunk = false;
        return outputPluginDriver.hookReduceArg0('transformChunk', [code, options, chunk], renderChunkReducer);
    })
        .then(code => {
        inTransformBundle = true;
        return outputPluginDriver.hookReduceArg0('transformBundle', [code, options, chunk], renderChunkReducer);
    })
        .catch(err => {
        if (inRenderChunk)
            throw err;
        return error(err, {
            code: inTransformBundle ? 'BAD_BUNDLE_TRANSFORMER' : 'BAD_CHUNK_TRANSFORMER',
            message: `Error transforming ${(inTransformBundle ? 'bundle' : 'chunk') +
                (err.plugin ? ` with '${err.plugin}' plugin` : '')}: ${err.message}`,
            plugin: err.plugin
        });
    });
}

function renderNamePattern(pattern, patternName, replacements) {
    if (!isPlainPathFragment(pattern))
        return error(errFailedValidation(`Invalid pattern "${pattern}" for "${patternName}", patterns can be neither absolute nor relative paths and must not contain invalid characters.`));
    return pattern.replace(/\[(\w+)\]/g, (_match, type) => {
        if (!replacements.hasOwnProperty(type)) {
            return error(errFailedValidation(`"[${type}]" is not a valid placeholder in "${patternName}" pattern.`));
        }
        const replacement = replacements[type]();
        if (!isPlainPathFragment(replacement))
            return error(errFailedValidation(`Invalid substitution "${replacement}" for placeholder "[${type}]" in "${patternName}" pattern, can be neither absolute nor relative path.`));
        return replacement;
    });
}
function makeUnique(name, existingNames) {
    const existingNamesLowercase = new Set(Object.keys(existingNames).map(key => key.toLowerCase()));
    if (!existingNamesLowercase.has(name.toLocaleLowerCase()))
        return name;
    const ext = extname(name);
    name = name.substr(0, name.length - ext.length);
    let uniqueName, uniqueIndex = 1;
    while (existingNamesLowercase.has((uniqueName = name + ++uniqueIndex + ext).toLowerCase()))
        ;
    return uniqueName;
}

const NON_ASSET_EXTENSIONS = ['.js', '.jsx', '.ts', '.tsx'];
function getGlobalName(module, globals, graph, hasExports) {
    let globalName;
    if (typeof globals === 'function') {
        globalName = globals(module.id);
    }
    else if (globals) {
        globalName = globals[module.id];
    }
    if (globalName) {
        return globalName;
    }
    if (hasExports) {
        graph.warn({
            code: 'MISSING_GLOBAL_NAME',
            guess: module.variableName,
            message: `No name was provided for external module '${module.id}' in output.globals – guessing '${module.variableName}'`,
            source: module.id
        });
        return module.variableName;
    }
}
function isChunkRendered(chunk) {
    return !chunk.isEmpty || chunk.entryModules.length > 0 || chunk.manualChunkAlias !== null;
}
class Chunk$1 {
    constructor(graph, orderedModules) {
        this.entryModules = [];
        this.exportMode = 'named';
        this.facadeModule = null;
        this.id = null;
        this.indentString = undefined;
        this.manualChunkAlias = null;
        this.usedModules = undefined;
        this.variableName = 'chunk';
        this.dependencies = undefined;
        this.dynamicDependencies = undefined;
        this.exportNames = Object.create(null);
        this.exports = new Set();
        this.fileName = null;
        this.imports = new Set();
        this.name = null;
        this.needsExportsShim = false;
        this.renderedDeclarations = undefined;
        this.renderedHash = undefined;
        this.renderedModuleSources = new Map();
        this.renderedSource = null;
        this.renderedSourceLength = undefined;
        this.sortedExportNames = null;
        this.graph = graph;
        this.orderedModules = orderedModules;
        this.execIndex = orderedModules.length > 0 ? orderedModules[0].execIndex : Infinity;
        this.isEmpty = true;
        for (const module of orderedModules) {
            if (this.isEmpty && module.isIncluded()) {
                this.isEmpty = false;
            }
            if (module.manualChunkAlias) {
                this.manualChunkAlias = module.manualChunkAlias;
            }
            module.chunk = this;
            if (module.isEntryPoint ||
                module.dynamicallyImportedBy.some(module => orderedModules.indexOf(module) === -1)) {
                this.entryModules.push(module);
            }
        }
        const moduleForNaming = this.entryModules[0] || this.orderedModules[this.orderedModules.length - 1];
        if (moduleForNaming) {
            this.variableName = makeLegal(basename(moduleForNaming.chunkName ||
                moduleForNaming.manualChunkAlias ||
                getAliasName(moduleForNaming.id)));
        }
    }
    static generateFacade(graph, facadedModule, facadeName) {
        const chunk = new Chunk$1(graph, []);
        chunk.assignFacadeName(facadeName, facadedModule);
        if (!facadedModule.facadeChunk) {
            facadedModule.facadeChunk = chunk;
        }
        chunk.dependencies = [facadedModule.chunk];
        chunk.dynamicDependencies = [];
        chunk.facadeModule = facadedModule;
        for (const exportName of facadedModule.getAllExportNames()) {
            const tracedVariable = facadedModule.getVariableForExportName(exportName);
            chunk.exports.add(tracedVariable);
            chunk.exportNames[exportName] = tracedVariable;
        }
        return chunk;
    }
    canModuleBeFacade(moduleExportNamesByVariable) {
        for (const exposedVariable of this.exports) {
            if (!moduleExportNamesByVariable.has(exposedVariable)) {
                return false;
            }
        }
        return true;
    }
    generateFacades() {
        const facades = [];
        for (const module of this.entryModules) {
            const requiredFacades = Array.from(module.userChunkNames).map(name => ({
                name
            }));
            if (requiredFacades.length === 0 && module.isUserDefinedEntryPoint) {
                requiredFacades.push({});
            }
            requiredFacades.push(...Array.from(module.chunkFileNames).map(fileName => ({ fileName })));
            if (requiredFacades.length === 0) {
                requiredFacades.push({});
            }
            if (!this.facadeModule) {
                const exportNamesByVariable = module.getExportNamesByVariable();
                if (this.graph.preserveModules || this.canModuleBeFacade(exportNamesByVariable)) {
                    this.facadeModule = module;
                    module.facadeChunk = this;
                    for (const [variable, exportNames] of exportNamesByVariable) {
                        for (const exportName of exportNames) {
                            this.exportNames[exportName] = variable;
                        }
                    }
                    this.assignFacadeName(requiredFacades.shift(), module);
                }
            }
            for (const facadeName of requiredFacades) {
                facades.push(Chunk$1.generateFacade(this.graph, module, facadeName));
            }
        }
        return facades;
    }
    generateId(addons, options, existingNames, includeHash, outputPluginDriver) {
        if (this.fileName !== null) {
            return this.fileName;
        }
        const [pattern, patternName] = this.facadeModule && this.facadeModule.isUserDefinedEntryPoint
            ? [options.entryFileNames || '[name].js', 'output.entryFileNames']
            : [options.chunkFileNames || '[name]-[hash].js', 'output.chunkFileNames'];
        return makeUnique(renderNamePattern(pattern, patternName, {
            format: () => (options.format === 'es' ? 'esm' : options.format),
            hash: () => includeHash
                ? this.computeContentHashWithDependencies(addons, options, existingNames, outputPluginDriver)
                : '[hash]',
            name: () => this.getChunkName()
        }), existingNames);
    }
    generateIdPreserveModules(preserveModulesRelativeDir, options, existingNames) {
        const id = this.orderedModules[0].id;
        const sanitizedId = sanitizeFileName(id);
        let path;
        if (isAbsolute(id)) {
            const extension = extname(id);
            const name = renderNamePattern(options.entryFileNames ||
                (NON_ASSET_EXTENSIONS.includes(extension) ? '[name].js' : '[name][extname].js'), 'output.entryFileNames', {
                ext: () => extension.substr(1),
                extname: () => extension,
                format: () => (options.format === 'es' ? 'esm' : options.format),
                name: () => this.getChunkName()
            });
            path = relative(preserveModulesRelativeDir, `${dirname(sanitizedId)}/${name}`);
        }
        else {
            path = `_virtual/${basename(sanitizedId)}`;
        }
        return makeUnique(normalize(path), existingNames);
    }
    generateInternalExports(options) {
        if (this.facadeModule !== null)
            return;
        const mangle = options.format === 'system' || options.format === 'es' || options.compact;
        let i = 0, safeExportName;
        this.exportNames = Object.create(null);
        this.sortedExportNames = null;
        if (mangle) {
            for (const variable of this.exports) {
                const suggestedName = variable.name[0];
                if (!this.exportNames[suggestedName]) {
                    this.exportNames[suggestedName] = variable;
                }
                else {
                    do {
                        safeExportName = toBase64(++i);
                        // skip past leading number identifiers
                        if (safeExportName.charCodeAt(0) === 49 /* '1' */) {
                            i += 9 * Math.pow(64, (safeExportName.length - 1));
                            safeExportName = toBase64(i);
                        }
                    } while (RESERVED_NAMES[safeExportName] || this.exportNames[safeExportName]);
                    this.exportNames[safeExportName] = variable;
                }
            }
        }
        else {
            for (const variable of this.exports) {
                i = 0;
                safeExportName = variable.name;
                while (this.exportNames[safeExportName]) {
                    safeExportName = variable.name + '$' + ++i;
                }
                this.exportNames[safeExportName] = variable;
            }
        }
    }
    getChunkName() {
        return this.name || (this.name = sanitizeFileName(this.getFallbackChunkName()));
    }
    getDynamicImportIds() {
        return this.dynamicDependencies.map(chunk => chunk.id).filter(Boolean);
    }
    getExportNames() {
        return (this.sortedExportNames || (this.sortedExportNames = Object.keys(this.exportNames).sort()));
    }
    getImportIds() {
        return this.dependencies.map(chunk => chunk.id).filter(Boolean);
    }
    getRenderedHash(outputPluginDriver) {
        if (this.renderedHash)
            return this.renderedHash;
        if (!this.renderedSource)
            return '';
        const hash = createHash();
        const hashAugmentation = this.calculateHashAugmentation(outputPluginDriver);
        hash.update(hashAugmentation);
        hash.update(this.renderedSource.toString());
        hash.update(this.getExportNames()
            .map(exportName => {
            const variable = this.exportNames[exportName];
            return `${relativeId(variable.module.id).replace(/\\/g, '/')}:${variable.name}:${exportName}`;
        })
            .join(','));
        return (this.renderedHash = hash.digest('hex'));
    }
    getRenderedSourceLength() {
        if (this.renderedSourceLength !== undefined)
            return this.renderedSourceLength;
        return (this.renderedSourceLength = this.renderedSource.length());
    }
    getVariableExportName(variable) {
        if (this.graph.preserveModules && variable instanceof NamespaceVariable) {
            return '*';
        }
        for (const exportName of Object.keys(this.exportNames)) {
            if (this.exportNames[exportName] === variable)
                return exportName;
        }
        throw new Error(`Internal Error: Could not find export name for variable ${variable.name}.`);
    }
    link() {
        const dependencies = new Set();
        const dynamicDependencies = new Set();
        for (const module of this.orderedModules) {
            this.addDependenciesToChunk(module.getTransitiveDependencies(), dependencies);
            this.addDependenciesToChunk(module.dynamicDependencies, dynamicDependencies);
            this.setUpChunkImportsAndExportsForModule(module);
        }
        this.dependencies = Array.from(dependencies);
        this.dynamicDependencies = Array.from(dynamicDependencies);
    }
    /*
     * Performs a full merge of another chunk into this chunk
     * chunkList allows updating references in other chunks for the merged chunk to this chunk
     * A new facade will be added to chunkList if tainting exports of either as an entry point
     */
    merge(chunk, chunkList, options, inputBase) {
        if (this.facadeModule !== null || chunk.facadeModule !== null)
            throw new Error('Internal error: Code splitting chunk merges not supported for facades');
        for (const module of chunk.orderedModules) {
            module.chunk = this;
            this.orderedModules.push(module);
        }
        for (const variable of chunk.imports) {
            if (!this.imports.has(variable) && variable.module.chunk !== this) {
                this.imports.add(variable);
            }
        }
        // NB detect when exported variables are orphaned by the merge itself
        // (involves reverse tracing dependents)
        for (const variable of chunk.exports) {
            if (!this.exports.has(variable)) {
                this.exports.add(variable);
            }
        }
        const thisOldExportNames = this.exportNames;
        // regenerate internal names
        this.generateInternalExports(options);
        const updateRenderedDeclaration = (dep, oldExportNames) => {
            if (dep.imports) {
                for (const impt of dep.imports) {
                    impt.imported = this.getVariableExportName(oldExportNames[impt.imported]);
                }
            }
            if (dep.reexports) {
                for (const reexport of dep.reexports) {
                    reexport.imported = this.getVariableExportName(oldExportNames[reexport.imported]);
                }
            }
        };
        const mergeRenderedDeclaration = (into, from) => {
            if (from.imports) {
                if (!into.imports) {
                    into.imports = from.imports;
                }
                else {
                    into.imports = into.imports.concat(from.imports);
                }
            }
            if (from.reexports) {
                if (!into.reexports) {
                    into.reexports = from.reexports;
                }
                else {
                    into.reexports = into.reexports.concat(from.reexports);
                }
            }
            if (!into.exportsNames && from.exportsNames) {
                into.exportsNames = true;
            }
            if (!into.exportsDefault && from.exportsDefault) {
                into.exportsDefault = true;
            }
            into.name = this.variableName;
        };
        // go through the other chunks and update their dependencies
        // also update their import and reexport names in the process
        for (const c of chunkList) {
            let includedDeclaration = undefined;
            for (let i = 0; i < c.dependencies.length; i++) {
                const dep = c.dependencies[i];
                if ((dep === chunk || dep === this) && includedDeclaration) {
                    const duplicateDeclaration = c.renderedDeclarations.dependencies[i];
                    updateRenderedDeclaration(duplicateDeclaration, dep === chunk ? chunk.exportNames : thisOldExportNames);
                    mergeRenderedDeclaration(includedDeclaration, duplicateDeclaration);
                    c.renderedDeclarations.dependencies.splice(i, 1);
                    c.dependencies.splice(i--, 1);
                }
                else if (dep === chunk) {
                    c.dependencies[i] = this;
                    includedDeclaration = c.renderedDeclarations.dependencies[i];
                    updateRenderedDeclaration(includedDeclaration, chunk.exportNames);
                }
                else if (dep === this) {
                    includedDeclaration = c.renderedDeclarations.dependencies[i];
                    updateRenderedDeclaration(includedDeclaration, thisOldExportNames);
                }
            }
        }
        // re-render the merged chunk
        this.preRender(options, inputBase);
    }
    // prerender allows chunk hashes and names to be generated before finalizing
    preRender(options, inputBase) {
        timeStart('render modules', 3);
        const magicString = new Bundle({ separator: options.compact ? '' : '\n\n' });
        this.usedModules = [];
        this.indentString = options.compact ? '' : getIndentString(this.orderedModules, options);
        const n = options.compact ? '' : '\n';
        const _ = options.compact ? '' : ' ';
        const renderOptions = {
            compact: options.compact,
            dynamicImportFunction: options.dynamicImportFunction,
            format: options.format,
            freeze: options.freeze !== false,
            indent: this.indentString,
            namespaceToStringTag: options.namespaceToStringTag === true,
            varOrConst: options.preferConst ? 'const' : 'var'
        };
        // Make sure the direct dependencies of a chunk are present to maintain execution order
        for (const { module } of this.imports) {
            const chunkOrExternal = (module instanceof Module ? module.chunk : module);
            if (this.dependencies.indexOf(chunkOrExternal) === -1) {
                this.dependencies.push(chunkOrExternal);
            }
        }
        // for static and dynamic entry points, inline the execution list to avoid loading latency
        if (options.hoistTransitiveImports !== false &&
            !this.graph.preserveModules &&
            this.facadeModule !== null) {
            for (const dep of this.dependencies) {
                if (dep instanceof Chunk$1)
                    this.inlineChunkDependencies(dep, true);
            }
        }
        // prune empty dependency chunks, inlining their side-effect dependencies
        for (let i = 0; i < this.dependencies.length; i++) {
            const dep = this.dependencies[i];
            if (dep instanceof Chunk$1 && dep.isEmpty) {
                this.dependencies.splice(i--, 1);
                this.inlineChunkDependencies(dep, false);
            }
        }
        sortByExecutionOrder(this.dependencies);
        this.prepareDynamicImports();
        this.setIdentifierRenderResolutions(options);
        let hoistedSource = '';
        const renderedModules = (this.renderedModules = Object.create(null));
        for (const module of this.orderedModules) {
            let renderedLength = 0;
            if (module.isIncluded()) {
                const source = module.render(renderOptions).trim();
                if (options.compact && source.lastLine().indexOf('//') !== -1)
                    source.append('\n');
                const namespace = module.getOrCreateNamespace();
                if (namespace.included || source.length() > 0) {
                    renderedLength = source.length();
                    this.renderedModuleSources.set(module, source);
                    magicString.addSource(source);
                    this.usedModules.push(module);
                    if (namespace.included && !this.graph.preserveModules) {
                        const rendered = namespace.renderBlock(renderOptions);
                        if (namespace.renderFirst())
                            hoistedSource += n + rendered;
                        else
                            magicString.addSource(new MagicString(rendered));
                    }
                }
            }
            const { renderedExports, removedExports } = module.getRenderedExports();
            renderedModules[module.id] = {
                originalLength: module.originalCode.length,
                removedExports,
                renderedExports,
                renderedLength
            };
        }
        if (hoistedSource)
            magicString.prepend(hoistedSource + n + n);
        if (this.needsExportsShim) {
            magicString.prepend(`${n}${renderOptions.varOrConst} ${MISSING_EXPORT_SHIM_VARIABLE}${_}=${_}void 0;${n}${n}`);
        }
        if (options.compact) {
            this.renderedSource = magicString;
        }
        else {
            this.renderedSource = magicString.trim();
        }
        this.renderedSourceLength = undefined;
        this.renderedHash = undefined;
        if (this.isEmpty && this.getExportNames().length === 0 && this.dependencies.length === 0) {
            const chunkName = this.getChunkName();
            this.graph.warn({
                chunkName,
                code: 'EMPTY_BUNDLE',
                message: `Generated an empty chunk: "${chunkName}"`
            });
        }
        this.setExternalRenderPaths(options, inputBase);
        this.renderedDeclarations = {
            dependencies: this.getChunkDependencyDeclarations(options),
            exports: this.exportMode === 'none' ? [] : this.getChunkExportDeclarations()
        };
        timeEnd('render modules', 3);
    }
    render(options, addons, outputChunk, outputPluginDriver) {
        timeStart('render format', 3);
        const format = options.format;
        const finalise = finalisers[format];
        if (options.dynamicImportFunction && format !== 'es') {
            this.graph.warn({
                code: 'INVALID_OPTION',
                message: '"output.dynamicImportFunction" is ignored for formats other than "esm".'
            });
        }
        // populate ids in the rendered declarations only here
        // as chunk ids known only after prerender
        for (let i = 0; i < this.dependencies.length; i++) {
            const dep = this.dependencies[i];
            if (dep instanceof ExternalModule && !dep.renormalizeRenderPath)
                continue;
            const renderedDependency = this.renderedDeclarations.dependencies[i];
            const depId = dep instanceof ExternalModule ? renderedDependency.id : dep.id;
            if (dep instanceof Chunk$1)
                renderedDependency.namedExportsMode = dep.exportMode !== 'default';
            renderedDependency.id = this.getRelativePath(depId);
        }
        this.finaliseDynamicImports(format);
        this.finaliseImportMetas(format, outputPluginDriver);
        const hasExports = this.renderedDeclarations.exports.length !== 0 ||
            this.renderedDeclarations.dependencies.some(dep => (dep.reexports && dep.reexports.length !== 0));
        let usesTopLevelAwait = false;
        const accessedGlobals = new Set();
        for (const module of this.orderedModules) {
            if (module.usesTopLevelAwait) {
                usesTopLevelAwait = true;
            }
            const accessedGlobalVariablesByFormat = module.scope.accessedGlobalVariablesByFormat;
            const accessedGlobalVariables = accessedGlobalVariablesByFormat && accessedGlobalVariablesByFormat.get(format);
            if (accessedGlobalVariables) {
                for (const name of accessedGlobalVariables) {
                    accessedGlobals.add(name);
                }
            }
        }
        if (usesTopLevelAwait && format !== 'es' && format !== 'system') {
            return error({
                code: 'INVALID_TLA_FORMAT',
                message: `Module format ${format} does not support top-level await. Use the "es" or "system" output formats rather.`
            });
        }
        const magicString = finalise(this.renderedSource, {
            accessedGlobals,
            dependencies: this.renderedDeclarations.dependencies,
            exports: this.renderedDeclarations.exports,
            hasExports,
            indentString: this.indentString,
            intro: addons.intro,
            isEntryModuleFacade: this.graph.preserveModules ||
                (this.facadeModule !== null && this.facadeModule.isEntryPoint),
            namedExportsMode: this.exportMode !== 'default',
            outro: addons.outro,
            usesTopLevelAwait,
            varOrConst: options.preferConst ? 'const' : 'var',
            warn: this.graph.warn.bind(this.graph)
        }, options);
        if (addons.banner)
            magicString.prepend(addons.banner);
        if (addons.footer)
            magicString.append(addons.footer);
        const prevCode = magicString.toString();
        timeEnd('render format', 3);
        let map = null;
        const chunkSourcemapChain = [];
        return renderChunk({
            chunk: this,
            code: prevCode,
            options,
            outputPluginDriver,
            renderChunk: outputChunk,
            sourcemapChain: chunkSourcemapChain
        }).then((code) => {
            if (options.sourcemap) {
                timeStart('sourcemap', 3);
                let file;
                if (options.file)
                    file = resolve(options.sourcemapFile || options.file);
                else if (options.dir)
                    file = resolve(options.dir, this.id);
                else
                    file = resolve(this.id);
                const decodedMap = magicString.generateDecodedMap({});
                map = collapseSourcemaps(this, file, decodedMap, this.usedModules, chunkSourcemapChain, options.sourcemapExcludeSources);
                map.sources = map.sources.map(sourcePath => normalize(options.sourcemapPathTransform ? options.sourcemapPathTransform(sourcePath) : sourcePath));
                timeEnd('sourcemap', 3);
            }
            if (options.compact !== true && code[code.length - 1] !== '\n')
                code += '\n';
            return { code, map };
        });
    }
    visitDependencies(handleDependency) {
        const toBeVisited = [this];
        const visited = new Set();
        for (const current of toBeVisited) {
            handleDependency(current);
            if (current instanceof ExternalModule)
                continue;
            for (const dependency of current.dependencies.concat(current.dynamicDependencies)) {
                if (!visited.has(dependency)) {
                    visited.add(dependency);
                    toBeVisited.push(dependency);
                }
            }
        }
    }
    visitStaticDependenciesUntilCondition(isConditionSatisfied) {
        const seen = new Set();
        function visitDep(dep) {
            if (seen.has(dep))
                return undefined;
            seen.add(dep);
            if (dep instanceof Chunk$1) {
                for (const subDep of dep.dependencies) {
                    if (visitDep(subDep))
                        return true;
                }
            }
            return isConditionSatisfied(dep) === true;
        }
        return visitDep(this);
    }
    addDependenciesToChunk(moduleDependencies, chunkDependencies) {
        for (const depModule of moduleDependencies) {
            if (depModule.chunk === this) {
                continue;
            }
            let dependency;
            if (depModule instanceof Module) {
                dependency = depModule.chunk;
            }
            else {
                if (!(depModule.used || depModule.moduleSideEffects)) {
                    continue;
                }
                dependency = depModule;
            }
            chunkDependencies.add(dependency);
        }
    }
    assignFacadeName({ fileName, name }, facadedModule) {
        if (fileName) {
            this.fileName = fileName;
        }
        else {
            this.name = sanitizeFileName(name || facadedModule.chunkName || getAliasName(facadedModule.id));
        }
    }
    calculateHashAugmentation(outputPluginDriver) {
        const facadeModule = this.facadeModule;
        const getChunkName = this.getChunkName.bind(this);
        const preRenderedChunk = {
            dynamicImports: this.getDynamicImportIds(),
            exports: this.getExportNames(),
            facadeModuleId: facadeModule && facadeModule.id,
            imports: this.getImportIds(),
            isDynamicEntry: facadeModule !== null && facadeModule.dynamicallyImportedBy.length > 0,
            isEntry: facadeModule !== null && facadeModule.isEntryPoint,
            modules: this.renderedModules,
            get name() {
                return getChunkName();
            }
        };
        return outputPluginDriver.hookReduceValueSync('augmentChunkHash', '', [preRenderedChunk], (hashAugmentation, pluginHash) => {
            if (pluginHash) {
                hashAugmentation += pluginHash;
            }
            return hashAugmentation;
        });
    }
    computeContentHashWithDependencies(addons, options, existingNames, outputPluginDriver) {
        const hash = createHash();
        hash.update([addons.intro, addons.outro, addons.banner, addons.footer].map(addon => addon || '').join(':'));
        hash.update(options.format);
        this.visitDependencies(dep => {
            if (dep instanceof ExternalModule) {
                hash.update(':' + dep.renderPath);
            }
            else {
                hash.update(dep.getRenderedHash(outputPluginDriver));
                hash.update(dep.generateId(addons, options, existingNames, false, outputPluginDriver));
            }
        });
        return hash.digest('hex').substr(0, 8);
    }
    finaliseDynamicImports(format) {
        for (const [module, code] of this.renderedModuleSources) {
            for (const { node, resolution } of module.dynamicImports) {
                if (!resolution)
                    continue;
                if (resolution instanceof Module) {
                    if (resolution.chunk !== this && isChunkRendered(resolution.chunk)) {
                        const resolutionChunk = resolution.facadeChunk || resolution.chunk;
                        node.renderFinalResolution(code, `'${this.getRelativePath(resolutionChunk.id)}'`, format);
                    }
                }
                else {
                    node.renderFinalResolution(code, resolution instanceof ExternalModule
                        ? `'${resolution.renormalizeRenderPath
                            ? this.getRelativePath(resolution.renderPath)
                            : resolution.id}'`
                        : resolution, format);
                }
            }
        }
    }
    finaliseImportMetas(format, outputPluginDriver) {
        for (const [module, code] of this.renderedModuleSources) {
            for (const importMeta of module.importMetas) {
                importMeta.renderFinalMechanism(code, this.id, format, outputPluginDriver);
            }
        }
    }
    getChunkDependencyDeclarations(options) {
        const reexportDeclarations = new Map();
        for (let exportName of this.getExportNames()) {
            let exportChunk;
            let importName;
            let needsLiveBinding = false;
            if (exportName[0] === '*') {
                needsLiveBinding = options.externalLiveBindings !== false;
                exportChunk = this.graph.moduleById.get(exportName.substr(1));
                importName = exportName = '*';
            }
            else {
                const variable = this.exportNames[exportName];
                const module = variable.module;
                // skip local exports
                if (!module || module.chunk === this)
                    continue;
                if (module instanceof Module) {
                    exportChunk = module.chunk;
                    importName = exportChunk.getVariableExportName(variable);
                    needsLiveBinding = variable.isReassigned;
                }
                else {
                    exportChunk = module;
                    importName = variable.name;
                    needsLiveBinding = options.externalLiveBindings !== false;
                }
            }
            let reexportDeclaration = reexportDeclarations.get(exportChunk);
            if (!reexportDeclaration)
                reexportDeclarations.set(exportChunk, (reexportDeclaration = []));
            reexportDeclaration.push({ imported: importName, reexported: exportName, needsLiveBinding });
        }
        const renderedImports = new Set();
        const dependencies = [];
        for (const dep of this.dependencies) {
            const imports = [];
            for (const variable of this.imports) {
                const renderedVariable = variable instanceof ExportDefaultVariable ? variable.getOriginalVariable() : variable;
                if ((variable.module instanceof Module
                    ? variable.module.chunk === dep
                    : variable.module === dep) &&
                    !renderedImports.has(renderedVariable)) {
                    renderedImports.add(renderedVariable);
                    imports.push({
                        imported: variable.module instanceof ExternalModule
                            ? variable.name
                            : variable.module.chunk.getVariableExportName(variable),
                        local: variable.getName()
                    });
                }
            }
            const reexports = reexportDeclarations.get(dep);
            let exportsNames, exportsDefault;
            let namedExportsMode = true;
            if (dep instanceof ExternalModule) {
                exportsNames = dep.exportsNames || dep.exportsNamespace;
                exportsDefault = 'default' in dep.declarations;
            }
            else {
                exportsNames = true;
                // we don't want any interop patterns to trigger
                exportsDefault = false;
                namedExportsMode = dep.exportMode !== 'default';
            }
            let id = undefined;
            let globalName = undefined;
            if (dep instanceof ExternalModule) {
                id = dep.renderPath;
                if (options.format === 'umd' || options.format === 'iife') {
                    globalName = getGlobalName(dep, options.globals, this.graph, exportsNames || exportsDefault);
                }
            }
            dependencies.push({
                exportsDefault,
                exportsNames,
                globalName,
                id,
                imports: imports.length > 0 ? imports : null,
                isChunk: dep instanceof Chunk$1,
                name: dep.variableName,
                namedExportsMode,
                reexports
            });
        }
        return dependencies;
    }
    getChunkExportDeclarations() {
        const exports = [];
        for (const exportName of this.getExportNames()) {
            if (exportName[0] === '*')
                continue;
            const variable = this.exportNames[exportName];
            const module = variable.module;
            if (module && module.chunk !== this)
                continue;
            let hoisted = false;
            let uninitialized = false;
            if (variable instanceof LocalVariable) {
                if (variable.init === UNDEFINED_EXPRESSION) {
                    uninitialized = true;
                }
                for (const declaration of variable.declarations) {
                    if (declaration.parent instanceof FunctionDeclaration ||
                        (declaration instanceof ExportDefaultDeclaration &&
                            declaration.declaration instanceof FunctionDeclaration)) {
                        hoisted = true;
                        break;
                    }
                }
            }
            else if (variable instanceof GlobalVariable) {
                hoisted = true;
            }
            const localName = variable.getName();
            exports.push({
                exported: exportName === '*' ? localName : exportName,
                hoisted,
                local: localName,
                uninitialized
            });
        }
        return exports;
    }
    getFallbackChunkName() {
        if (this.manualChunkAlias) {
            return this.manualChunkAlias;
        }
        if (this.fileName) {
            return getAliasName(this.fileName);
        }
        return getAliasName(this.orderedModules[this.orderedModules.length - 1].id);
    }
    getRelativePath(targetPath) {
        const relativePath = normalize(relative(dirname(this.id), targetPath));
        return relativePath.startsWith('../') ? relativePath : './' + relativePath;
    }
    inlineChunkDependencies(chunk, deep) {
        for (const dep of chunk.dependencies) {
            if (dep instanceof ExternalModule) {
                if (this.dependencies.indexOf(dep) === -1)
                    this.dependencies.push(dep);
            }
            else {
                if (dep === this || this.dependencies.indexOf(dep) !== -1)
                    continue;
                if (!dep.isEmpty)
                    this.dependencies.push(dep);
                if (deep)
                    this.inlineChunkDependencies(dep, true);
            }
        }
    }
    prepareDynamicImports() {
        for (const module of this.orderedModules) {
            for (const { node, resolution } of module.dynamicImports) {
                if (!node.included)
                    continue;
                if (resolution instanceof Module) {
                    if (resolution.chunk === this) {
                        const namespace = resolution.getOrCreateNamespace();
                        node.setResolution('named', namespace);
                    }
                    else {
                        node.setResolution(resolution.chunk.exportMode);
                    }
                }
                else {
                    node.setResolution('auto');
                }
            }
        }
    }
    setExternalRenderPaths(options, inputBase) {
        for (const dependency of this.dependencies.concat(this.dynamicDependencies)) {
            if (dependency instanceof ExternalModule) {
                dependency.setRenderPath(options, inputBase);
            }
        }
    }
    setIdentifierRenderResolutions(options) {
        for (const exportName of this.getExportNames()) {
            const exportVariable = this.exportNames[exportName];
            if (exportVariable) {
                if (exportVariable instanceof ExportShimVariable) {
                    this.needsExportsShim = true;
                }
                exportVariable.exportName = exportName;
                if (options.format !== 'es' &&
                    options.format !== 'system' &&
                    exportVariable.isReassigned &&
                    !exportVariable.isId &&
                    !(exportVariable instanceof ExportDefaultVariable && exportVariable.hasId)) {
                    exportVariable.setRenderNames('exports', exportName);
                }
                else {
                    exportVariable.setRenderNames(null, null);
                }
            }
        }
        const usedNames = new Set();
        if (this.needsExportsShim) {
            usedNames.add(MISSING_EXPORT_SHIM_VARIABLE);
        }
        if (options.format !== 'es') {
            usedNames.add('exports');
            if (options.format === 'cjs') {
                usedNames
                    .add(INTEROP_DEFAULT_VARIABLE)
                    .add('require')
                    .add('module')
                    .add('__filename')
                    .add('__dirname');
            }
        }
        deconflictChunk(this.orderedModules, this.dependencies, this.imports, usedNames, options.format, options.interop !== false, this.graph.preserveModules);
    }
    setUpChunkImportsAndExportsForModule(module) {
        for (const variable of module.imports) {
            if (variable.module.chunk !== this) {
                this.imports.add(variable);
                if (variable.module instanceof Module) {
                    variable.module.chunk.exports.add(variable);
                }
            }
        }
        if (module.isEntryPoint ||
            module.dynamicallyImportedBy.some(importer => importer.chunk !== this)) {
            const map = module.getExportNamesByVariable();
            for (const exportedVariable of map.keys()) {
                this.exports.add(exportedVariable);
                const exportingModule = exportedVariable.module;
                if (exportingModule && exportingModule.chunk && exportingModule.chunk !== this) {
                    exportingModule.chunk.exports.add(exportedVariable);
                }
            }
        }
        if (module.getOrCreateNamespace().included) {
            for (const reexportName of Object.keys(module.reexportDescriptions)) {
                const reexport = module.reexportDescriptions[reexportName];
                const variable = reexport.module.getVariableForExportName(reexport.localName);
                if (variable.module.chunk !== this) {
                    this.imports.add(variable);
                    if (variable.module instanceof Module) {
                        variable.module.chunk.exports.add(variable);
                    }
                }
            }
        }
        const context = createInclusionContext();
        for (const { node, resolution } of module.dynamicImports) {
            if (node.included && resolution instanceof Module && resolution.chunk === this)
                resolution.getOrCreateNamespace().include(context);
        }
    }
}

/*
 * Given a chunk list, perform optimizations on that chunk list
 * to reduce the mumber of chunks. Mutates the chunks array.
 *
 * Manual chunks (with chunk.chunkAlias already set) are preserved
 * Entry points are carefully preserved as well
 *
 */
function optimizeChunks(chunks, options, CHUNK_GROUPING_SIZE, inputBase) {
    for (let chunkIndex = 0; chunkIndex < chunks.length; chunkIndex++) {
        const mainChunk = chunks[chunkIndex];
        const execGroup = [];
        mainChunk.visitStaticDependenciesUntilCondition(dep => {
            if (dep instanceof Chunk$1) {
                execGroup.push(dep);
            }
        });
        if (execGroup.length < 2) {
            continue;
        }
        let execGroupIndex = 1;
        let seekingFirstMergeCandidate = true;
        let lastChunk = undefined, chunk = execGroup[0], nextChunk = execGroup[1];
        const isMergeCandidate = (chunk) => {
            if (chunk.facadeModule !== null || chunk.manualChunkAlias !== null) {
                return false;
            }
            if (!nextChunk || nextChunk.facadeModule !== null) {
                return false;
            }
            if (chunk.getRenderedSourceLength() > CHUNK_GROUPING_SIZE) {
                return false;
            }
            // if (!chunk.isPure()) continue;
            return true;
        };
        do {
            if (seekingFirstMergeCandidate) {
                if (isMergeCandidate(chunk)) {
                    seekingFirstMergeCandidate = false;
                }
                continue;
            }
            let remainingSize = CHUNK_GROUPING_SIZE - lastChunk.getRenderedSourceLength() - chunk.getRenderedSourceLength();
            if (remainingSize <= 0) {
                if (!isMergeCandidate(chunk)) {
                    seekingFirstMergeCandidate = true;
                }
                continue;
            }
            // if (!chunk.isPure()) continue;
            const chunkDependencies = new Set();
            chunk.visitStaticDependenciesUntilCondition(dep => chunkDependencies.add(dep));
            const ignoreSizeChunks = new Set([chunk, lastChunk]);
            if (lastChunk.visitStaticDependenciesUntilCondition(dep => {
                if (dep === chunk || dep === lastChunk) {
                    return false;
                }
                if (chunkDependencies.has(dep)) {
                    return false;
                }
                if (dep instanceof ExternalModule) {
                    return true;
                }
                remainingSize -= dep.getRenderedSourceLength();
                if (remainingSize <= 0) {
                    return true;
                }
                ignoreSizeChunks.add(dep);
            })) {
                if (!isMergeCandidate(chunk)) {
                    seekingFirstMergeCandidate = true;
                }
                continue;
            }
            if (chunk.visitStaticDependenciesUntilCondition(dep => {
                if (ignoreSizeChunks.has(dep)) {
                    return false;
                }
                if (dep instanceof ExternalModule) {
                    return true;
                }
                remainingSize -= dep.getRenderedSourceLength();
                if (remainingSize <= 0) {
                    return true;
                }
            })) {
                if (!isMergeCandidate(chunk)) {
                    seekingFirstMergeCandidate = true;
                }
                continue;
            }
            // within the size limit -> merge!
            const optimizedChunkIndex = chunks.indexOf(chunk);
            if (optimizedChunkIndex <= chunkIndex)
                chunkIndex--;
            chunks.splice(optimizedChunkIndex, 1);
            lastChunk.merge(chunk, chunks, options, inputBase);
            execGroup.splice(--execGroupIndex, 1);
            chunk = lastChunk;
            // keep going to see if we can merge this with the next again
            if (nextChunk && !isMergeCandidate(nextChunk)) {
                seekingFirstMergeCandidate = true;
            }
        } while (((lastChunk = chunk), (chunk = nextChunk), (nextChunk = execGroup[++execGroupIndex]), chunk));
    }
    return chunks;
}

const skipWhiteSpace = /(?:\s|\/\/.*|\/\*[^]*?\*\/)*/g;
const tt = acorn__default.tokTypes;
var acornExportNsFrom = function (Parser) {
    return class extends Parser {
        parseExport(node, exports) {
            skipWhiteSpace.lastIndex = this.pos;
            const skip = skipWhiteSpace.exec(this.input);
            const next = this.input.charAt(this.pos + skip[0].length);
            if (next !== "*")
                return super.parseExport(node, exports);
            this.next();
            const specifier = this.startNode();
            this.expect(tt.star);
            if (this.eatContextual("as")) {
                node.declaration = null;
                specifier.exported = this.parseIdent(true);
                this.checkExport(exports, specifier.exported.name, this.lastTokStart);
                node.specifiers = [this.finishNode(specifier, "ExportNamespaceSpecifier")];
            }
            this.expectContextual("from");
            if (this.type !== tt.string)
                this.unexpected();
            node.source = this.parseExprAtom();
            this.semicolon();
            return this.finishNode(node, node.specifiers ? "ExportNamedDeclaration" : "ExportAllDeclaration");
        }
    };
};

const tt$1 = acorn__default.tokTypes;
const skipWhiteSpace$1 = /(?:\s|\/\/.*|\/\*[^]*?\*\/)*/g;
const nextTokenIsDot = parser => {
    skipWhiteSpace$1.lastIndex = parser.pos;
    let skip = skipWhiteSpace$1.exec(parser.input);
    let next = parser.pos + skip[0].length;
    return parser.input.slice(next, next + 1) === ".";
};
var acornImportMeta = function (Parser) {
    return class extends Parser {
        parseExprAtom(refDestructuringErrors) {
            if (this.type !== tt$1._import || !nextTokenIsDot(this))
                return super.parseExprAtom(refDestructuringErrors);
            if (!this.options.allowImportExportEverywhere && !this.inModule) {
                this.raise(this.start, "'import' and 'export' may appear only with 'sourceType: module'");
            }
            let node = this.startNode();
            node.meta = this.parseIdent(true);
            this.expect(tt$1.dot);
            node.property = this.parseIdent(true);
            if (node.property.name !== "meta") {
                this.raiseRecoverable(node.property.start, "The only valid meta property for import is import.meta");
            }
            if (this.containsEsc) {
                this.raiseRecoverable(node.property.start, "\"meta\" in import.meta must not contain escape sequences");
            }
            return this.finishNode(node, "MetaProperty");
        }
        parseStatement(context, topLevel, exports) {
            if (this.type !== tt$1._import || !nextTokenIsDot(this)) {
                return super.parseStatement(context, topLevel, exports);
            }
            let node = this.startNode();
            let expr = this.parseExpression();
            return this.parseExpressionStatement(node, expr);
        }
    };
};

class UndefinedVariable extends Variable {
    constructor() {
        super('undefined');
    }
    getLiteralValueAtPath() {
        return undefined;
    }
}

class GlobalScope extends Scope {
    constructor() {
        super();
        this.variables.set('undefined', new UndefinedVariable());
    }
    findVariable(name) {
        let variable = this.variables.get(name);
        if (!variable) {
            variable = new GlobalVariable(name);
            this.variables.set(name, variable);
        }
        return variable;
    }
}

const ANONYMOUS_PLUGIN_PREFIX = 'at position ';
const ANONYMOUS_OUTPUT_PLUGIN_PREFIX = 'at output position ';
function throwPluginError(err, plugin, { hook, id } = {}) {
    if (typeof err === 'string')
        err = { message: err };
    if (err.code && err.code !== Errors.PLUGIN_ERROR) {
        err.pluginCode = err.code;
    }
    err.code = Errors.PLUGIN_ERROR;
    err.plugin = plugin;
    if (hook) {
        err.hook = hook;
    }
    if (id) {
        err.id = id;
    }
    return error(err);
}
const deprecatedHooks = [
    { active: true, deprecated: 'ongenerate', replacement: 'generateBundle' },
    { active: true, deprecated: 'onwrite', replacement: 'generateBundle/writeBundle' },
    { active: true, deprecated: 'transformBundle', replacement: 'renderChunk' },
    { active: true, deprecated: 'transformChunk', replacement: 'renderChunk' },
    { active: false, deprecated: 'resolveAssetUrl', replacement: 'resolveFileUrl' }
];
function warnDeprecatedHooks(plugins, graph) {
    for (const { active, deprecated, replacement } of deprecatedHooks) {
        for (const plugin of plugins) {
            if (deprecated in plugin) {
                graph.warnDeprecation({
                    message: `The "${deprecated}" hook used by plugin ${plugin.name} is deprecated. The "${replacement}" hook should be used instead.`,
                    plugin: plugin.name
                }, active);
            }
        }
    }
}

function createPluginCache(cache) {
    return {
        has(id) {
            const item = cache[id];
            if (!item)
                return false;
            item[0] = 0;
            return true;
        },
        get(id) {
            const item = cache[id];
            if (!item)
                return undefined;
            item[0] = 0;
            return item[1];
        },
        set(id, value) {
            cache[id] = [0, value];
        },
        delete(id) {
            return delete cache[id];
        }
    };
}
function getTrackedPluginCache(pluginCache) {
    const trackedCache = {
        cache: {
            has(id) {
                trackedCache.used = true;
                return pluginCache.has(id);
            },
            get(id) {
                trackedCache.used = true;
                return pluginCache.get(id);
            },
            set(id, value) {
                trackedCache.used = true;
                return pluginCache.set(id, value);
            },
            delete(id) {
                trackedCache.used = true;
                return pluginCache.delete(id);
            }
        },
        used: false
    };
    return trackedCache;
}
const NO_CACHE = {
    has() {
        return false;
    },
    get() {
        return undefined;
    },
    set() { },
    delete() {
        return false;
    }
};
function uncacheablePluginError(pluginName) {
    if (pluginName.startsWith(ANONYMOUS_PLUGIN_PREFIX) ||
        pluginName.startsWith(ANONYMOUS_OUTPUT_PLUGIN_PREFIX)) {
        return error({
            code: 'ANONYMOUS_PLUGIN_CACHE',
            message: 'A plugin is trying to use the Rollup cache but is not declaring a plugin name or cacheKey.'
        });
    }
    return error({
        code: 'DUPLICATE_PLUGIN_NAME',
        message: `The plugin name ${pluginName} is being used twice in the same build. Plugin names must be distinct or provide a cacheKey (please post an issue to the plugin if you are a plugin user).`
    });
}
function getCacheForUncacheablePlugin(pluginName) {
    return {
        has() {
            return uncacheablePluginError(pluginName);
        },
        get() {
            return uncacheablePluginError(pluginName);
        },
        set() {
            return uncacheablePluginError(pluginName);
        },
        delete() {
            return uncacheablePluginError(pluginName);
        }
    };
}

function transform(graph, source, module) {
    const id = module.id;
    const sourcemapChain = [];
    let originalSourcemap = source.map === null ? null : decodedSourcemap(source.map);
    const originalCode = source.code;
    let ast = source.ast;
    const transformDependencies = [];
    const emittedFiles = [];
    let customTransformCache = false;
    let moduleSideEffects = null;
    let syntheticNamedExports = null;
    let trackedPluginCache;
    let curPlugin;
    const curSource = source.code;
    function transformReducer(code, result, plugin) {
        // track which plugins use the custom this.cache to opt-out of transform caching
        if (!customTransformCache && trackedPluginCache.used)
            customTransformCache = true;
        if (customTransformCache) {
            if (result && typeof result === 'object' && Array.isArray(result.dependencies)) {
                for (const dep of result.dependencies) {
                    graph.watchFiles[resolve(dirname(id), dep)] = true;
                }
            }
        }
        else {
            // files emitted by a transform hook need to be emitted again if the hook is skipped
            if (emittedFiles.length)
                module.transformFiles = emittedFiles;
            if (result && typeof result === 'object' && Array.isArray(result.dependencies)) {
                // not great, but a useful way to track this without assuming WeakMap
                if (!curPlugin.warnedTransformDependencies)
                    graph.warnDeprecation(`Returning "dependencies" from the "transform" hook as done by plugin ${plugin.name} is deprecated. The "this.addWatchFile" plugin context function should be used instead.`, true);
                curPlugin.warnedTransformDependencies = true;
                for (const dep of result.dependencies)
                    transformDependencies.push(resolve(dirname(id), dep));
            }
        }
        if (typeof result === 'string') {
            result = {
                ast: undefined,
                code: result,
                map: undefined
            };
        }
        else if (result && typeof result === 'object') {
            if (typeof result.map === 'string') {
                result.map = JSON.parse(result.map);
            }
            if (typeof result.moduleSideEffects === 'boolean') {
                moduleSideEffects = result.moduleSideEffects;
            }
            if (typeof result.syntheticNamedExports === 'boolean') {
                syntheticNamedExports = result.syntheticNamedExports;
            }
        }
        else {
            return code;
        }
        // strict null check allows 'null' maps to not be pushed to the chain, while 'undefined' gets the missing map warning
        if (result.map !== null) {
            const map = decodedSourcemap(result.map);
            sourcemapChain.push(map || { missing: true, plugin: plugin.name });
        }
        ast = result.ast;
        return result.code;
    }
    let setAssetSourceErr;
    return graph.pluginDriver
        .hookReduceArg0('transform', [curSource, id], transformReducer, (pluginContext, plugin) => {
        curPlugin = plugin;
        if (curPlugin.cacheKey)
            customTransformCache = true;
        else
            trackedPluginCache = getTrackedPluginCache(pluginContext.cache);
        return Object.assign(Object.assign({}, pluginContext), { cache: trackedPluginCache ? trackedPluginCache.cache : pluginContext.cache, warn(warning, pos) {
                if (typeof warning === 'string')
                    warning = { message: warning };
                if (pos)
                    augmentCodeLocation(warning, pos, curSource, id);
                warning.id = id;
                warning.hook = 'transform';
                pluginContext.warn(warning);
            },
            error(err, pos) {
                if (typeof err === 'string')
                    err = { message: err };
                if (pos)
                    augmentCodeLocation(err, pos, curSource, id);
                err.id = id;
                err.hook = 'transform';
                return pluginContext.error(err);
            },
            emitAsset(name, source) {
                const emittedFile = { type: 'asset', name, source };
                emittedFiles.push(Object.assign({}, emittedFile));
                return graph.pluginDriver.emitFile(emittedFile);
            },
            emitChunk(id, options) {
                const emittedFile = { type: 'chunk', id, name: options && options.name };
                emittedFiles.push(Object.assign({}, emittedFile));
                return graph.pluginDriver.emitFile(emittedFile);
            },
            emitFile(emittedFile) {
                emittedFiles.push(emittedFile);
                return graph.pluginDriver.emitFile(emittedFile);
            },
            addWatchFile(id) {
                transformDependencies.push(id);
                pluginContext.addWatchFile(id);
            },
            setAssetSource(assetReferenceId, source) {
                pluginContext.setAssetSource(assetReferenceId, source);
                if (!customTransformCache && !setAssetSourceErr) {
                    try {
                        return this.error({
                            code: 'INVALID_SETASSETSOURCE',
                            message: `setAssetSource cannot be called in transform for caching reasons. Use emitFile with a source, or call setAssetSource in another hook.`
                        });
                    }
                    catch (err) {
                        setAssetSourceErr = err;
                    }
                }
            },
            getCombinedSourcemap() {
                const combinedMap = collapseSourcemap(graph, id, originalCode, originalSourcemap, sourcemapChain);
                if (!combinedMap) {
                    const magicString = new MagicString(originalCode);
                    return magicString.generateMap({ includeContent: true, hires: true, source: id });
                }
                if (originalSourcemap !== combinedMap) {
                    originalSourcemap = combinedMap;
                    sourcemapChain.length = 0;
                }
                return new SourceMap(Object.assign(Object.assign({}, combinedMap), { file: null, sourcesContent: combinedMap.sourcesContent }));
            } });
    })
        .catch(err => throwPluginError(err, curPlugin.name, { hook: 'transform', id }))
        .then(code => {
        if (!customTransformCache && setAssetSourceErr)
            throw setAssetSourceErr;
        return {
            ast: ast,
            code,
            customTransformCache,
            moduleSideEffects,
            originalCode,
            originalSourcemap,
            sourcemapChain,
            syntheticNamedExports,
            transformDependencies
        };
    });
}

function normalizeRelativeExternalId(importer, source) {
    return isRelative(source) ? resolve(importer, '..', source) : source;
}
function getIdMatcher(option) {
    if (option === true) {
        return () => true;
    }
    if (typeof option === 'function') {
        return (id, ...args) => (!id.startsWith('\0') && option(id, ...args)) || false;
    }
    if (option) {
        const ids = new Set(Array.isArray(option) ? option : option ? [option] : []);
        return (id => ids.has(id));
    }
    return () => false;
}
function getHasModuleSideEffects(moduleSideEffectsOption, pureExternalModules, graph) {
    if (typeof moduleSideEffectsOption === 'boolean') {
        return () => moduleSideEffectsOption;
    }
    if (moduleSideEffectsOption === 'no-external') {
        return (_id, external) => !external;
    }
    if (typeof moduleSideEffectsOption === 'function') {
        return (id, external) => !id.startsWith('\0') ? moduleSideEffectsOption(id, external) !== false : true;
    }
    if (Array.isArray(moduleSideEffectsOption)) {
        const ids = new Set(moduleSideEffectsOption);
        return id => ids.has(id);
    }
    if (moduleSideEffectsOption) {
        graph.warn(errInvalidOption('treeshake.moduleSideEffects', 'please use one of false, "no-external", a function or an array'));
    }
    const isPureExternalModule = getIdMatcher(pureExternalModules);
    return (id, external) => !(external && isPureExternalModule(id));
}
class ModuleLoader {
    constructor(graph, modulesById, pluginDriver, external, getManualChunk, moduleSideEffects, pureExternalModules) {
        this.indexedEntryModules = [];
        this.latestLoadModulesPromise = Promise.resolve();
        this.manualChunkModules = {};
        this.nextEntryModuleIndex = 0;
        this.loadEntryModule = (unresolvedId, isEntry) => this.pluginDriver.hookFirst('resolveId', [unresolvedId, undefined]).then(resolveIdResult => {
            if (resolveIdResult === false ||
                (resolveIdResult && typeof resolveIdResult === 'object' && resolveIdResult.external)) {
                return error(errEntryCannotBeExternal(unresolvedId));
            }
            const id = resolveIdResult && typeof resolveIdResult === 'object'
                ? resolveIdResult.id
                : resolveIdResult;
            if (typeof id === 'string') {
                return this.fetchModule(id, undefined, true, false, isEntry);
            }
            return error(errUnresolvedEntry(unresolvedId));
        });
        this.graph = graph;
        this.modulesById = modulesById;
        this.pluginDriver = pluginDriver;
        this.isExternal = getIdMatcher(external);
        this.hasModuleSideEffects = getHasModuleSideEffects(moduleSideEffects, pureExternalModules, graph);
        this.getManualChunk = typeof getManualChunk === 'function' ? getManualChunk : () => null;
    }
    addEntryModules(unresolvedEntryModules, isUserDefined) {
        const firstEntryModuleIndex = this.nextEntryModuleIndex;
        this.nextEntryModuleIndex += unresolvedEntryModules.length;
        const loadNewEntryModulesPromise = Promise.all(unresolvedEntryModules.map(({ fileName, id, name }) => this.loadEntryModule(id, true).then(module => {
            if (fileName !== null) {
                module.chunkFileNames.add(fileName);
            }
            else if (name !== null) {
                if (module.chunkName === null) {
                    module.chunkName = name;
                }
                if (isUserDefined) {
                    module.userChunkNames.add(name);
                }
            }
            return module;
        }))).then(entryModules => {
            let moduleIndex = firstEntryModuleIndex;
            for (const entryModule of entryModules) {
                entryModule.isUserDefinedEntryPoint = entryModule.isUserDefinedEntryPoint || isUserDefined;
                const existingIndexModule = this.indexedEntryModules.find(indexedModule => indexedModule.module.id === entryModule.id);
                if (!existingIndexModule) {
                    this.indexedEntryModules.push({ module: entryModule, index: moduleIndex });
                }
                else {
                    existingIndexModule.index = Math.min(existingIndexModule.index, moduleIndex);
                }
                moduleIndex++;
            }
            this.indexedEntryModules.sort(({ index: indexA }, { index: indexB }) => indexA > indexB ? 1 : -1);
            return entryModules;
        });
        return this.awaitLoadModulesPromise(loadNewEntryModulesPromise).then(newEntryModules => ({
            entryModules: this.indexedEntryModules.map(({ module }) => module),
            manualChunkModulesByAlias: this.manualChunkModules,
            newEntryModules
        }));
    }
    addManualChunks(manualChunks) {
        const unresolvedManualChunks = [];
        for (const alias of Object.keys(manualChunks)) {
            const manualChunkIds = manualChunks[alias];
            for (const id of manualChunkIds) {
                unresolvedManualChunks.push({ id, alias });
            }
        }
        const loadNewManualChunkModulesPromise = Promise.all(unresolvedManualChunks.map(({ id }) => this.loadEntryModule(id, false))).then(manualChunkModules => {
            for (let index = 0; index < manualChunkModules.length; index++) {
                this.addModuleToManualChunk(unresolvedManualChunks[index].alias, manualChunkModules[index]);
            }
        });
        return this.awaitLoadModulesPromise(loadNewManualChunkModulesPromise);
    }
    resolveId(source, importer, skip) {
        return __awaiter(this, void 0, void 0, function* () {
            return this.normalizeResolveIdResult(this.isExternal(source, importer, false)
                ? false
                : yield this.pluginDriver.hookFirst('resolveId', [source, importer], null, skip), importer, source);
        });
    }
    addModuleToManualChunk(alias, module) {
        if (module.manualChunkAlias !== null && module.manualChunkAlias !== alias) {
            return error(errCannotAssignModuleToChunk(module.id, alias, module.manualChunkAlias));
        }
        module.manualChunkAlias = alias;
        if (!this.manualChunkModules[alias]) {
            this.manualChunkModules[alias] = [];
        }
        this.manualChunkModules[alias].push(module);
    }
    awaitLoadModulesPromise(loadNewModulesPromise) {
        this.latestLoadModulesPromise = Promise.all([
            loadNewModulesPromise,
            this.latestLoadModulesPromise
        ]);
        const getCombinedPromise = () => {
            const startingPromise = this.latestLoadModulesPromise;
            return startingPromise.then(() => {
                if (this.latestLoadModulesPromise !== startingPromise) {
                    return getCombinedPromise();
                }
            });
        };
        return getCombinedPromise().then(() => loadNewModulesPromise);
    }
    fetchAllDependencies(module) {
        return Promise.all([
            ...Array.from(module.sources).map((source) => __awaiter(this, void 0, void 0, function* () {
                return this.fetchResolvedDependency(source, module.id, (module.resolvedIds[source] =
                    module.resolvedIds[source] ||
                        this.handleResolveId(yield this.resolveId(source, module.id), source, module.id)));
            })),
            ...module.getDynamicImportExpressions().map((specifier, index) => this.resolveDynamicImport(module, specifier, module.id).then(resolvedId => {
                if (resolvedId === null)
                    return;
                const dynamicImport = module.dynamicImports[index];
                if (typeof resolvedId === 'string') {
                    dynamicImport.resolution = resolvedId;
                    return;
                }
                return this.fetchResolvedDependency(relativeId(resolvedId.id), module.id, resolvedId).then(module => {
                    dynamicImport.resolution = module;
                });
            }))
        ]);
    }
    fetchModule(id, importer, moduleSideEffects, syntheticNamedExports, isEntry) {
        const existingModule = this.modulesById.get(id);
        if (existingModule instanceof Module) {
            existingModule.isEntryPoint = existingModule.isEntryPoint || isEntry;
            return Promise.resolve(existingModule);
        }
        const module = new Module(this.graph, id, moduleSideEffects, syntheticNamedExports, isEntry);
        this.modulesById.set(id, module);
        this.graph.watchFiles[id] = true;
        const manualChunkAlias = this.getManualChunk(id);
        if (typeof manualChunkAlias === 'string') {
            this.addModuleToManualChunk(manualChunkAlias, module);
        }
        timeStart('load modules', 3);
        return Promise.resolve(this.pluginDriver.hookFirst('load', [id]))
            .catch((err) => {
            timeEnd('load modules', 3);
            let msg = `Could not load ${id}`;
            if (importer)
                msg += ` (imported by ${importer})`;
            msg += `: ${err.message}`;
            err.message = msg;
            throw err;
        })
            .then(source => {
            timeEnd('load modules', 3);
            if (typeof source === 'string')
                return { code: source };
            if (source && typeof source === 'object' && typeof source.code === 'string')
                return source;
            return error(errBadLoader(id));
        })
            .then(sourceDescription => {
            const cachedModule = this.graph.cachedModules.get(id);
            if (cachedModule &&
                !cachedModule.customTransformCache &&
                cachedModule.originalCode === sourceDescription.code) {
                if (cachedModule.transformFiles) {
                    for (const emittedFile of cachedModule.transformFiles)
                        this.pluginDriver.emitFile(emittedFile);
                }
                return cachedModule;
            }
            if (typeof sourceDescription.moduleSideEffects === 'boolean') {
                module.moduleSideEffects = sourceDescription.moduleSideEffects;
            }
            if (typeof sourceDescription.syntheticNamedExports === 'boolean') {
                module.syntheticNamedExports = sourceDescription.syntheticNamedExports;
            }
            return transform(this.graph, sourceDescription, module);
        })
            .then((source) => {
            module.setSource(source);
            this.modulesById.set(id, module);
            return this.fetchAllDependencies(module).then(() => {
                for (const name in module.exports) {
                    if (name !== 'default') {
                        module.exportsAll[name] = module.id;
                    }
                }
                for (const source of module.exportAllSources) {
                    const id = module.resolvedIds[source].id;
                    const exportAllModule = this.modulesById.get(id);
                    if (exportAllModule instanceof ExternalModule)
                        continue;
                    for (const name in exportAllModule.exportsAll) {
                        if (name in module.exportsAll) {
                            this.graph.warn(errNamespaceConflict(name, module, exportAllModule));
                        }
                        else {
                            module.exportsAll[name] = exportAllModule.exportsAll[name];
                        }
                    }
                }
                return module;
            });
        });
    }
    fetchResolvedDependency(source, importer, resolvedId) {
        if (resolvedId.external) {
            if (!this.modulesById.has(resolvedId.id)) {
                this.modulesById.set(resolvedId.id, new ExternalModule(this.graph, resolvedId.id, resolvedId.moduleSideEffects));
            }
            const externalModule = this.modulesById.get(resolvedId.id);
            if (!(externalModule instanceof ExternalModule)) {
                return error(errInternalIdCannotBeExternal(source, importer));
            }
            return Promise.resolve(externalModule);
        }
        else {
            return this.fetchModule(resolvedId.id, importer, resolvedId.moduleSideEffects, resolvedId.syntheticNamedExports, false);
        }
    }
    handleResolveId(resolvedId, source, importer) {
        if (resolvedId === null) {
            if (isRelative(source)) {
                return error(errUnresolvedImport(source, importer));
            }
            this.graph.warn(errUnresolvedImportTreatedAsExternal(source, importer));
            return {
                external: true,
                id: source,
                moduleSideEffects: this.hasModuleSideEffects(source, true),
                syntheticNamedExports: false
            };
        }
        else {
            if (resolvedId.external && resolvedId.syntheticNamedExports) {
                this.graph.warn(errExternalSyntheticExports(source, importer));
            }
        }
        return resolvedId;
    }
    normalizeResolveIdResult(resolveIdResult, importer, source) {
        let id = '';
        let external = false;
        let moduleSideEffects = null;
        let syntheticNamedExports = false;
        if (resolveIdResult) {
            if (typeof resolveIdResult === 'object') {
                id = resolveIdResult.id;
                if (resolveIdResult.external) {
                    external = true;
                }
                if (typeof resolveIdResult.moduleSideEffects === 'boolean') {
                    moduleSideEffects = resolveIdResult.moduleSideEffects;
                }
                if (typeof resolveIdResult.syntheticNamedExports === 'boolean') {
                    syntheticNamedExports = resolveIdResult.syntheticNamedExports;
                }
            }
            else {
                if (this.isExternal(resolveIdResult, importer, true)) {
                    external = true;
                }
                id = external ? normalizeRelativeExternalId(importer, resolveIdResult) : resolveIdResult;
            }
        }
        else {
            id = normalizeRelativeExternalId(importer, source);
            if (resolveIdResult !== false && !this.isExternal(id, importer, true)) {
                return null;
            }
            external = true;
        }
        return {
            external,
            id,
            moduleSideEffects: typeof moduleSideEffects === 'boolean'
                ? moduleSideEffects
                : this.hasModuleSideEffects(id, external),
            syntheticNamedExports
        };
    }
    resolveDynamicImport(module, specifier, importer) {
        return __awaiter(this, void 0, void 0, function* () {
            // TODO we only should expose the acorn AST here
            const resolution = yield this.pluginDriver.hookFirst('resolveDynamicImport', [
                specifier,
                importer
            ]);
            if (typeof specifier !== 'string') {
                if (typeof resolution === 'string') {
                    return resolution;
                }
                if (!resolution) {
                    return null;
                }
                return Object.assign({ external: false, moduleSideEffects: true }, resolution);
            }
            if (resolution == null) {
                return (module.resolvedIds[specifier] =
                    module.resolvedIds[specifier] ||
                        this.handleResolveId(yield this.resolveId(specifier, module.id), specifier, module.id));
            }
            return this.handleResolveId(this.normalizeResolveIdResult(resolution, importer, specifier), specifier, importer);
        });
    }
}

var BuildPhase;
(function (BuildPhase) {
    BuildPhase[BuildPhase["LOAD_AND_PARSE"] = 0] = "LOAD_AND_PARSE";
    BuildPhase[BuildPhase["ANALYSE"] = 1] = "ANALYSE";
    BuildPhase[BuildPhase["GENERATE"] = 2] = "GENERATE";
})(BuildPhase || (BuildPhase = {}));

const CHAR_CODE_A = 97;
const CHAR_CODE_0 = 48;
function intToHex(num) {
    if (num < 10)
        return String.fromCharCode(CHAR_CODE_0 + num);
    else
        return String.fromCharCode(CHAR_CODE_A + (num - 10));
}
function Uint8ArrayToHexString(buffer) {
    let str = '';
    // hex conversion - 2 chars per 8 bit component
    for (let i = 0; i < buffer.length; i++) {
        const num = buffer[i];
        // big endian conversion, but whatever
        str += intToHex(num >> 4);
        str += intToHex(num & 0xf);
    }
    return str;
}
function randomUint8Array(len) {
    const buffer = new Uint8Array(len);
    for (let i = 0; i < buffer.length; i++)
        buffer[i] = Math.random() * (2 << 8);
    return buffer;
}
function Uint8ArrayXor(to, from) {
    for (let i = 0; i < to.length; i++)
        to[i] = to[i] ^ from[i];
    return to;
}

function assignChunkColouringHashes(entryModules, manualChunkModules) {
    const { dependentEntryPointsByModule, dynamicImportersByModule } = analyzeModuleGraph(entryModules);
    const dynamicDependentEntryPointsByDynamicEntry = getDynamicDependentEntryPoints(dependentEntryPointsByModule, dynamicImportersByModule);
    const staticEntries = new Set(entryModules);
    function addColourToModuleDependencies(entry, colour, dynamicDependentEntryPoints) {
        const manualChunkAlias = entry.manualChunkAlias;
        const modulesToHandle = new Set([entry]);
        for (const module of modulesToHandle) {
            if (manualChunkAlias) {
                module.manualChunkAlias = manualChunkAlias;
                module.entryPointsHash = colour;
            }
            else if (dynamicDependentEntryPoints &&
                areEntryPointsContainedOrDynamicallyDependent(dynamicDependentEntryPoints, dependentEntryPointsByModule.get(module))) {
                continue;
            }
            else {
                Uint8ArrayXor(module.entryPointsHash, colour);
            }
            for (const dependency of module.dependencies) {
                if (!(dependency instanceof ExternalModule || dependency.manualChunkAlias)) {
                    modulesToHandle.add(dependency);
                }
            }
        }
    }
    function areEntryPointsContainedOrDynamicallyDependent(entryPoints, superSet) {
        const entriesToCheck = new Set(entryPoints);
        for (const entry of entriesToCheck) {
            if (!superSet.has(entry)) {
                if (staticEntries.has(entry))
                    return false;
                const dynamicDependentEntryPoints = dynamicDependentEntryPointsByDynamicEntry.get(entry);
                for (const dependentEntry of dynamicDependentEntryPoints) {
                    entriesToCheck.add(dependentEntry);
                }
            }
        }
        return true;
    }
    if (manualChunkModules) {
        for (const chunkName of Object.keys(manualChunkModules)) {
            const entryHash = randomUint8Array(10);
            for (const entry of manualChunkModules[chunkName]) {
                addColourToModuleDependencies(entry, entryHash, null);
            }
        }
    }
    for (const entry of entryModules) {
        if (!entry.manualChunkAlias) {
            const entryHash = randomUint8Array(10);
            addColourToModuleDependencies(entry, entryHash, null);
        }
    }
    for (const entry of dynamicImportersByModule.keys()) {
        if (!entry.manualChunkAlias) {
            const entryHash = randomUint8Array(10);
            addColourToModuleDependencies(entry, entryHash, dynamicDependentEntryPointsByDynamicEntry.get(entry));
        }
    }
}
function analyzeModuleGraph(entryModules) {
    const dynamicImportersByModule = new Map();
    const dependentEntryPointsByModule = new Map();
    const entriesToHandle = new Set(entryModules);
    for (const currentEntry of entriesToHandle) {
        const modulesToHandle = new Set([currentEntry]);
        for (const module of modulesToHandle) {
            getDependentModules(dependentEntryPointsByModule, module).add(currentEntry);
            for (const dependency of module.dependencies) {
                if (!(dependency instanceof ExternalModule)) {
                    modulesToHandle.add(dependency);
                }
            }
            for (const { resolution } of module.dynamicImports) {
                if (resolution instanceof Module &&
                    resolution.dynamicallyImportedBy.length > 0 &&
                    !resolution.manualChunkAlias) {
                    getDependentModules(dynamicImportersByModule, resolution).add(module);
                    entriesToHandle.add(resolution);
                }
            }
        }
    }
    return { dependentEntryPointsByModule, dynamicImportersByModule };
}
function getDependentModules(moduleMap, module) {
    const dependentModules = moduleMap.get(module) || new Set();
    moduleMap.set(module, dependentModules);
    return dependentModules;
}
function getDynamicDependentEntryPoints(dependentEntryPointsByModule, dynamicImportersByModule) {
    const dynamicDependentEntryPointsByDynamicEntry = new Map();
    for (const [dynamicEntry, importers] of dynamicImportersByModule.entries()) {
        const dynamicDependentEntryPoints = getDependentModules(dynamicDependentEntryPointsByDynamicEntry, dynamicEntry);
        for (const importer of importers) {
            for (const entryPoint of dependentEntryPointsByModule.get(importer)) {
                dynamicDependentEntryPoints.add(entryPoint);
            }
        }
    }
    return dynamicDependentEntryPointsByDynamicEntry;
}

const createHash$1 = () => createHash$2('sha256');

function generateAssetFileName(name, source, output) {
    const emittedName = name || 'asset';
    return makeUnique(renderNamePattern(output.assetFileNames, 'output.assetFileNames', {
        hash() {
            const hash = createHash$1();
            hash.update(emittedName);
            hash.update(':');
            hash.update(source);
            return hash.digest('hex').substr(0, 8);
        },
        ext: () => extname(emittedName).substr(1),
        extname: () => extname(emittedName),
        name: () => emittedName.substr(0, emittedName.length - extname(emittedName).length)
    }), output.bundle);
}
function reserveFileNameInBundle(fileName, bundle, graph) {
    if (fileName in bundle) {
        graph.warn(errFileNameConflict(fileName));
    }
    bundle[fileName] = FILE_PLACEHOLDER;
}
const FILE_PLACEHOLDER = {
    type: 'placeholder'
};
function hasValidType(emittedFile) {
    return (emittedFile &&
        (emittedFile.type === 'asset' ||
            emittedFile.type === 'chunk'));
}
function hasValidName(emittedFile) {
    const validatedName = emittedFile.fileName || emittedFile.name;
    return (!validatedName || (typeof validatedName === 'string' && isPlainPathFragment(validatedName)));
}
function getValidSource(source, emittedFile, fileReferenceId) {
    if (typeof source !== 'string' && !Buffer.isBuffer(source)) {
        const assetName = emittedFile.fileName || emittedFile.name || fileReferenceId;
        return error(errFailedValidation(`Could not set source for ${typeof assetName === 'string' ? `asset "${assetName}"` : 'unnamed asset'}, asset source needs to be a string of Buffer.`));
    }
    return source;
}
function getAssetFileName(file, referenceId) {
    if (typeof file.fileName !== 'string') {
        return error(errAssetNotFinalisedForFileName(file.name || referenceId));
    }
    return file.fileName;
}
function getChunkFileName(file) {
    const fileName = file.fileName || (file.module && file.module.facadeChunk.id);
    if (!fileName)
        return error(errChunkNotGeneratedForFileName(file.fileName || file.name));
    return fileName;
}
class FileEmitter {
    constructor(graph, baseFileEmitter) {
        this.output = null;
        this.assertAssetsFinalized = () => {
            for (const [referenceId, emittedFile] of this.filesByReferenceId.entries()) {
                if (emittedFile.type === 'asset' && typeof emittedFile.fileName !== 'string')
                    return error(errNoAssetSourceSet(emittedFile.name || referenceId));
            }
        };
        this.emitFile = (emittedFile) => {
            if (!hasValidType(emittedFile)) {
                return error(errFailedValidation(`Emitted files must be of type "asset" or "chunk", received "${emittedFile &&
                    emittedFile.type}".`));
            }
            if (!hasValidName(emittedFile)) {
                return error(errFailedValidation(`The "fileName" or "name" properties of emitted files must be strings that are neither absolute nor relative paths and do not contain invalid characters, received "${emittedFile.fileName ||
                    emittedFile.name}".`));
            }
            if (emittedFile.type === 'chunk') {
                return this.emitChunk(emittedFile);
            }
            else {
                return this.emitAsset(emittedFile);
            }
        };
        this.getFileName = (fileReferenceId) => {
            const emittedFile = this.filesByReferenceId.get(fileReferenceId);
            if (!emittedFile)
                return error(errFileReferenceIdNotFoundForFilename(fileReferenceId));
            if (emittedFile.type === 'chunk') {
                return getChunkFileName(emittedFile);
            }
            else {
                return getAssetFileName(emittedFile, fileReferenceId);
            }
        };
        this.setAssetSource = (referenceId, requestedSource) => {
            const consumedFile = this.filesByReferenceId.get(referenceId);
            if (!consumedFile)
                return error(errAssetReferenceIdNotFoundForSetSource(referenceId));
            if (consumedFile.type !== 'asset') {
                return error(errFailedValidation(`Asset sources can only be set for emitted assets but "${referenceId}" is an emitted chunk.`));
            }
            if (consumedFile.source !== undefined) {
                return error(errAssetSourceAlreadySet(consumedFile.name || referenceId));
            }
            const source = getValidSource(requestedSource, consumedFile, referenceId);
            if (this.output) {
                this.finalizeAsset(consumedFile, source, referenceId, this.output);
            }
            else {
                consumedFile.source = source;
            }
        };
        this.setOutputBundle = (outputBundle, assetFileNames) => {
            this.output = {
                assetFileNames,
                bundle: outputBundle
            };
            for (const emittedFile of this.filesByReferenceId.values()) {
                if (emittedFile.fileName) {
                    reserveFileNameInBundle(emittedFile.fileName, this.output.bundle, this.graph);
                }
            }
            for (const [referenceId, consumedFile] of this.filesByReferenceId.entries()) {
                if (consumedFile.type === 'asset' && consumedFile.source !== undefined) {
                    this.finalizeAsset(consumedFile, consumedFile.source, referenceId, this.output);
                }
            }
        };
        this.graph = graph;
        this.filesByReferenceId = baseFileEmitter
            ? new Map(baseFileEmitter.filesByReferenceId)
            : new Map();
    }
    assignReferenceId(file, idBase) {
        let referenceId;
        do {
            const hash = createHash$1();
            if (referenceId) {
                hash.update(referenceId);
            }
            else {
                hash.update(idBase);
            }
            referenceId = hash.digest('hex').substr(0, 8);
        } while (this.filesByReferenceId.has(referenceId));
        this.filesByReferenceId.set(referenceId, file);
        return referenceId;
    }
    emitAsset(emittedAsset) {
        const source = typeof emittedAsset.source !== 'undefined'
            ? getValidSource(emittedAsset.source, emittedAsset, null)
            : undefined;
        const consumedAsset = {
            fileName: emittedAsset.fileName,
            name: emittedAsset.name,
            source,
            type: 'asset'
        };
        const referenceId = this.assignReferenceId(consumedAsset, emittedAsset.fileName || emittedAsset.name || emittedAsset.type);
        if (this.output) {
            if (emittedAsset.fileName) {
                reserveFileNameInBundle(emittedAsset.fileName, this.output.bundle, this.graph);
            }
            if (source !== undefined) {
                this.finalizeAsset(consumedAsset, source, referenceId, this.output);
            }
        }
        return referenceId;
    }
    emitChunk(emittedChunk) {
        if (this.graph.phase > BuildPhase.LOAD_AND_PARSE) {
            return error(errInvalidRollupPhaseForChunkEmission());
        }
        if (typeof emittedChunk.id !== 'string') {
            return error(errFailedValidation(`Emitted chunks need to have a valid string id, received "${emittedChunk.id}"`));
        }
        const consumedChunk = {
            fileName: emittedChunk.fileName,
            module: null,
            name: emittedChunk.name || emittedChunk.id,
            type: 'chunk'
        };
        this.graph.moduleLoader
            .addEntryModules([
            {
                fileName: emittedChunk.fileName || null,
                id: emittedChunk.id,
                name: emittedChunk.name || null
            }
        ], false)
            .then(({ newEntryModules: [module] }) => {
            consumedChunk.module = module;
        })
            .catch(() => {
            // Avoid unhandled Promise rejection as the error will be thrown later
            // once module loading has finished
        });
        return this.assignReferenceId(consumedChunk, emittedChunk.id);
    }
    finalizeAsset(consumedFile, source, referenceId, output) {
        const fileName = consumedFile.fileName ||
            this.findExistingAssetFileNameWithSource(output.bundle, source) ||
            generateAssetFileName(consumedFile.name, source, output);
        // We must not modify the original assets to avoid interaction between outputs
        const assetWithFileName = Object.assign(Object.assign({}, consumedFile), { source, fileName });
        this.filesByReferenceId.set(referenceId, assetWithFileName);
        const graph = this.graph;
        output.bundle[fileName] = {
            fileName,
            get isAsset() {
                graph.warnDeprecation('Accessing "isAsset" on files in the bundle is deprecated, please use "type === \'asset\'" instead', false);
                return true;
            },
            source,
            type: 'asset'
        };
    }
    findExistingAssetFileNameWithSource(bundle, source) {
        for (const fileName of Object.keys(bundle)) {
            const outputFile = bundle[fileName];
            if (outputFile.type === 'asset' &&
                (Buffer.isBuffer(source) && Buffer.isBuffer(outputFile.source)
                    ? source.equals(outputFile.source)
                    : source === outputFile.source))
                return fileName;
        }
        return null;
    }
}

function getDeprecatedContextHandler(handler, handlerName, newHandlerName, pluginName, activeDeprecation, graph) {
    let deprecationWarningShown = false;
    return ((...args) => {
        if (!deprecationWarningShown) {
            deprecationWarningShown = true;
            graph.warnDeprecation({
                message: `The "this.${handlerName}" plugin context function used by plugin ${pluginName} is deprecated. The "this.${newHandlerName}" plugin context function should be used instead.`,
                plugin: pluginName
            }, activeDeprecation);
        }
        return handler(...args);
    });
}
function getPluginContexts(pluginCache, graph, fileEmitter, watcher) {
    const existingPluginNames = new Set();
    return (plugin, pidx) => {
        let cacheable = true;
        if (typeof plugin.cacheKey !== 'string') {
            if (plugin.name.startsWith(ANONYMOUS_PLUGIN_PREFIX) ||
                plugin.name.startsWith(ANONYMOUS_OUTPUT_PLUGIN_PREFIX) ||
                existingPluginNames.has(plugin.name)) {
                cacheable = false;
            }
            else {
                existingPluginNames.add(plugin.name);
            }
        }
        let cacheInstance;
        if (!pluginCache) {
            cacheInstance = NO_CACHE;
        }
        else if (cacheable) {
            const cacheKey = plugin.cacheKey || plugin.name;
            cacheInstance = createPluginCache(pluginCache[cacheKey] || (pluginCache[cacheKey] = Object.create(null)));
        }
        else {
            cacheInstance = getCacheForUncacheablePlugin(plugin.name);
        }
        const context = {
            addWatchFile(id) {
                if (graph.phase >= BuildPhase.GENERATE) {
                    return this.error(errInvalidRollupPhaseForAddWatchFile());
                }
                graph.watchFiles[id] = true;
            },
            cache: cacheInstance,
            emitAsset: getDeprecatedContextHandler((name, source) => fileEmitter.emitFile({ type: 'asset', name, source }), 'emitAsset', 'emitFile', plugin.name, false, graph),
            emitChunk: getDeprecatedContextHandler((id, options) => fileEmitter.emitFile({ type: 'chunk', id, name: options && options.name }), 'emitChunk', 'emitFile', plugin.name, false, graph),
            emitFile: fileEmitter.emitFile,
            error(err) {
                return throwPluginError(err, plugin.name);
            },
            getAssetFileName: getDeprecatedContextHandler(fileEmitter.getFileName, 'getAssetFileName', 'getFileName', plugin.name, false, graph),
            getChunkFileName: getDeprecatedContextHandler(fileEmitter.getFileName, 'getChunkFileName', 'getFileName', plugin.name, false, graph),
            getFileName: fileEmitter.getFileName,
            getModuleInfo(moduleId) {
                const foundModule = graph.moduleById.get(moduleId);
                if (foundModule == null) {
                    throw new Error(`Unable to find module ${moduleId}`);
                }
                return {
                    hasModuleSideEffects: foundModule.moduleSideEffects,
                    id: foundModule.id,
                    importedIds: foundModule instanceof ExternalModule
                        ? []
                        : Array.from(foundModule.sources).map(id => foundModule.resolvedIds[id].id),
                    isEntry: foundModule instanceof Module && foundModule.isEntryPoint,
                    isExternal: foundModule instanceof ExternalModule
                };
            },
            isExternal: getDeprecatedContextHandler((id, parentId, isResolved = false) => graph.moduleLoader.isExternal(id, parentId, isResolved), 'isExternal', 'resolve', plugin.name, false, graph),
            meta: {
                rollupVersion: version
            },
            get moduleIds() {
                return graph.moduleById.keys();
            },
            parse: graph.contextParse,
            resolve(source, importer, options) {
                return graph.moduleLoader.resolveId(source, importer, options && options.skipSelf ? pidx : null);
            },
            resolveId: getDeprecatedContextHandler((source, importer) => graph.moduleLoader
                .resolveId(source, importer)
                .then(resolveId => resolveId && resolveId.id), 'resolveId', 'resolve', plugin.name, false, graph),
            setAssetSource: fileEmitter.setAssetSource,
            warn(warning) {
                if (typeof warning === 'string')
                    warning = { message: warning };
                if (warning.code)
                    warning.pluginCode = warning.code;
                warning.code = 'PLUGIN_WARNING';
                warning.plugin = plugin.name;
                graph.warn(warning);
            },
            watcher: watcher
                ? (() => {
                    let deprecationWarningShown = false;
                    function deprecatedWatchListener(event, handler) {
                        if (!deprecationWarningShown) {
                            context.warn({
                                code: 'PLUGIN_WATCHER_DEPRECATED',
                                message: `this.watcher usage is deprecated in plugins. Use the watchChange plugin hook and this.addWatchFile() instead.`
                            });
                            deprecationWarningShown = true;
                        }
                        return watcher.on(event, handler);
                    }
                    return Object.assign(Object.assign({}, watcher), { addListener: deprecatedWatchListener, on: deprecatedWatchListener });
                })()
                : undefined
        };
        return context;
    };
}

class PluginDriver {
    constructor(graph, userPlugins, pluginCache, preserveSymlinks, watcher, basePluginDriver) {
        this.previousHooks = new Set(['options']);
        warnDeprecatedHooks(userPlugins, graph);
        this.graph = graph;
        this.pluginCache = pluginCache;
        this.preserveSymlinks = preserveSymlinks;
        this.watcher = watcher;
        this.fileEmitter = new FileEmitter(graph, basePluginDriver && basePluginDriver.fileEmitter);
        this.emitFile = this.fileEmitter.emitFile;
        this.getFileName = this.fileEmitter.getFileName;
        this.finaliseAssets = this.fileEmitter.assertAssetsFinalized;
        this.setOutputBundle = this.fileEmitter.setOutputBundle;
        this.plugins = userPlugins.concat(basePluginDriver ? basePluginDriver.plugins : [getRollupDefaultPlugin(preserveSymlinks)]);
        this.pluginContexts = this.plugins.map(getPluginContexts(pluginCache, graph, this.fileEmitter, watcher));
        if (basePluginDriver) {
            for (const plugin of userPlugins) {
                for (const hook of basePluginDriver.previousHooks) {
                    if (hook in plugin) {
                        graph.warn(errInputHookInOutputPlugin(plugin.name, hook));
                    }
                }
            }
        }
    }
    createOutputPluginDriver(plugins) {
        return new PluginDriver(this.graph, plugins, this.pluginCache, this.preserveSymlinks, this.watcher, this);
    }
    // chains, first non-null result stops and returns
    hookFirst(hookName, args, replaceContext, skip) {
        let promise = Promise.resolve();
        for (let i = 0; i < this.plugins.length; i++) {
            if (skip === i)
                continue;
            promise = promise.then((result) => {
                if (result != null)
                    return result;
                return this.runHook(hookName, args, i, false, replaceContext);
            });
        }
        return promise;
    }
    // chains synchronously, first non-null result stops and returns
    hookFirstSync(hookName, args, replaceContext) {
        for (let i = 0; i < this.plugins.length; i++) {
            const result = this.runHookSync(hookName, args, i, replaceContext);
            if (result != null)
                return result;
        }
        return null;
    }
    // parallel, ignores returns
    hookParallel(hookName, args, replaceContext) {
        const promises = [];
        for (let i = 0; i < this.plugins.length; i++) {
            const hookPromise = this.runHook(hookName, args, i, false, replaceContext);
            if (!hookPromise)
                continue;
            promises.push(hookPromise);
        }
        return Promise.all(promises).then(() => { });
    }
    // chains, reduces returns of type R, to type T, handling the reduced value as the first hook argument
    hookReduceArg0(hookName, [arg0, ...args], reduce, replaceContext) {
        let promise = Promise.resolve(arg0);
        for (let i = 0; i < this.plugins.length; i++) {
            promise = promise.then(arg0 => {
                const hookPromise = this.runHook(hookName, [arg0, ...args], i, false, replaceContext);
                if (!hookPromise)
                    return arg0;
                return hookPromise.then((result) => reduce.call(this.pluginContexts[i], arg0, result, this.plugins[i]));
            });
        }
        return promise;
    }
    // chains synchronously, reduces returns of type R, to type T, handling the reduced value as the first hook argument
    hookReduceArg0Sync(hookName, [arg0, ...args], reduce, replaceContext) {
        for (let i = 0; i < this.plugins.length; i++) {
            const result = this.runHookSync(hookName, [arg0, ...args], i, replaceContext);
            arg0 = reduce.call(this.pluginContexts[i], arg0, result, this.plugins[i]);
        }
        return arg0;
    }
    // chains, reduces returns of type R, to type T, handling the reduced value separately. permits hooks as values.
    hookReduceValue(hookName, initialValue, args, reduce, replaceContext) {
        let promise = Promise.resolve(initialValue);
        for (let i = 0; i < this.plugins.length; i++) {
            promise = promise.then(value => {
                const hookPromise = this.runHook(hookName, args, i, true, replaceContext);
                if (!hookPromise)
                    return value;
                return hookPromise.then((result) => reduce.call(this.pluginContexts[i], value, result, this.plugins[i]));
            });
        }
        return promise;
    }
    // chains, reduces returns of type R, to type T, handling the reduced value separately. permits hooks as values.
    hookReduceValueSync(hookName, initialValue, args, reduce, replaceContext) {
        let acc = initialValue;
        for (let i = 0; i < this.plugins.length; i++) {
            const result = this.runHookSync(hookName, args, i, replaceContext);
            acc = reduce.call(this.pluginContexts[i], acc, result, this.plugins[i]);
        }
        return acc;
    }
    // chains, ignores returns
    hookSeq(hookName, args, replaceContext) {
        return __awaiter(this, void 0, void 0, function* () {
            let promise = Promise.resolve();
            for (let i = 0; i < this.plugins.length; i++)
                promise = promise.then(() => this.runHook(hookName, args, i, false, replaceContext));
            return promise;
        });
    }
    // chains, ignores returns
    hookSeqSync(hookName, args, replaceContext) {
        for (let i = 0; i < this.plugins.length; i++)
            this.runHookSync(hookName, args, i, replaceContext);
    }
    runHook(hookName, args, pluginIndex, permitValues, hookContext) {
        this.previousHooks.add(hookName);
        const plugin = this.plugins[pluginIndex];
        const hook = plugin[hookName];
        if (!hook)
            return undefined;
        let context = this.pluginContexts[pluginIndex];
        if (hookContext) {
            context = hookContext(context, plugin);
        }
        return Promise.resolve()
            .then(() => {
            // permit values allows values to be returned instead of a functional hook
            if (typeof hook !== 'function') {
                if (permitValues)
                    return hook;
                return error({
                    code: 'INVALID_PLUGIN_HOOK',
                    message: `Error running plugin hook ${hookName} for ${plugin.name}, expected a function hook.`
                });
            }
            return hook.apply(context, args);
        })
            .catch(err => throwPluginError(err, plugin.name, { hook: hookName }));
    }
    runHookSync(hookName, args, pluginIndex, hookContext) {
        this.previousHooks.add(hookName);
        const plugin = this.plugins[pluginIndex];
        let context = this.pluginContexts[pluginIndex];
        const hook = plugin[hookName];
        if (!hook)
            return undefined;
        if (hookContext) {
            context = hookContext(context, plugin);
        }
        try {
            // permit values allows values to be returned instead of a functional hook
            if (typeof hook !== 'function') {
                return error({
                    code: 'INVALID_PLUGIN_HOOK',
                    message: `Error running plugin hook ${hookName} for ${plugin.name}, expected a function hook.`
                });
            }
            return hook.apply(context, args);
        }
        catch (err) {
            return throwPluginError(err, plugin.name, { hook: hookName });
        }
    }
}

function makeOnwarn() {
    const warned = Object.create(null);
    return (warning) => {
        const str = warning.toString();
        if (str in warned)
            return;
        console.error(str);
        warned[str] = true;
    };
}
function normalizeEntryModules(entryModules) {
    if (typeof entryModules === 'string') {
        return [{ fileName: null, name: null, id: entryModules }];
    }
    if (Array.isArray(entryModules)) {
        return entryModules.map(id => ({ fileName: null, name: null, id }));
    }
    return Object.keys(entryModules).map(name => ({
        fileName: null,
        id: entryModules[name],
        name
    }));
}
class Graph {
    constructor(options, watcher) {
        this.moduleById = new Map();
        this.needsTreeshakingPass = false;
        this.phase = BuildPhase.LOAD_AND_PARSE;
        this.watchFiles = Object.create(null);
        this.externalModules = [];
        this.modules = [];
        this.onwarn = options.onwarn || makeOnwarn();
        this.deoptimizationTracker = new PathTracker();
        this.cachedModules = new Map();
        if (options.cache) {
            if (options.cache.modules)
                for (const module of options.cache.modules)
                    this.cachedModules.set(module.id, module);
        }
        if (options.cache !== false) {
            this.pluginCache = (options.cache && options.cache.plugins) || Object.create(null);
            // increment access counter
            for (const name in this.pluginCache) {
                const cache = this.pluginCache[name];
                for (const key of Object.keys(cache))
                    cache[key][0]++;
            }
        }
        this.preserveModules = options.preserveModules;
        this.strictDeprecations = options.strictDeprecations;
        this.cacheExpiry = options.experimentalCacheExpiry;
        if (options.treeshake !== false) {
            this.treeshakingOptions =
                options.treeshake && options.treeshake !== true
                    ? {
                        annotations: options.treeshake.annotations !== false,
                        moduleSideEffects: options.treeshake.moduleSideEffects,
                        propertyReadSideEffects: options.treeshake.propertyReadSideEffects !== false,
                        pureExternalModules: options.treeshake.pureExternalModules,
                        tryCatchDeoptimization: options.treeshake.tryCatchDeoptimization !== false,
                        unknownGlobalSideEffects: options.treeshake.unknownGlobalSideEffects !== false
                    }
                    : {
                        annotations: true,
                        moduleSideEffects: true,
                        propertyReadSideEffects: true,
                        tryCatchDeoptimization: true,
                        unknownGlobalSideEffects: true
                    };
            if (typeof this.treeshakingOptions.pureExternalModules !== 'undefined') {
                this.warnDeprecation(`The "treeshake.pureExternalModules" option is deprecated. The "treeshake.moduleSideEffects" option should be used instead. "treeshake.pureExternalModules: true" is equivalent to "treeshake.moduleSideEffects: 'no-external'"`, false);
            }
        }
        this.contextParse = (code, options = {}) => this.acornParser.parse(code, Object.assign(Object.assign(Object.assign({}, defaultAcornOptions), options), this.acornOptions));
        this.pluginDriver = new PluginDriver(this, options.plugins, this.pluginCache, options.preserveSymlinks === true, watcher);
        if (watcher) {
            const handleChange = (id) => this.pluginDriver.hookSeqSync('watchChange', [id]);
            watcher.on('change', handleChange);
            watcher.once('restart', () => {
                watcher.removeListener('change', handleChange);
            });
        }
        this.shimMissingExports = options.shimMissingExports;
        this.scope = new GlobalScope();
        this.context = String(options.context);
        const optionsModuleContext = options.moduleContext;
        if (typeof optionsModuleContext === 'function') {
            this.getModuleContext = id => optionsModuleContext(id) || this.context;
        }
        else if (typeof optionsModuleContext === 'object') {
            const moduleContext = new Map();
            for (const key in optionsModuleContext) {
                moduleContext.set(resolve(key), optionsModuleContext[key]);
            }
            this.getModuleContext = id => moduleContext.get(id) || this.context;
        }
        else {
            this.getModuleContext = () => this.context;
        }
        this.acornOptions = options.acorn ? Object.assign({}, options.acorn) : {};
        const acornPluginsToInject = [];
        acornPluginsToInject.push(acornImportMeta, acornExportNsFrom);
        this.acornOptions.allowAwaitOutsideFunction = true;
        const acornInjectPlugins = options.acornInjectPlugins;
        acornPluginsToInject.push(...(Array.isArray(acornInjectPlugins)
            ? acornInjectPlugins
            : acornInjectPlugins
                ? [acornInjectPlugins]
                : []));
        this.acornParser = Parser.extend(...acornPluginsToInject);
        this.moduleLoader = new ModuleLoader(this, this.moduleById, this.pluginDriver, options.external, (typeof options.manualChunks === 'function' && options.manualChunks), (this.treeshakingOptions ? this.treeshakingOptions.moduleSideEffects : null), (this.treeshakingOptions ? this.treeshakingOptions.pureExternalModules : false));
    }
    build(entryModules, manualChunks, inlineDynamicImports) {
        // Phase 1 – discovery. We load the entry module and find which
        // modules it imports, and import those, until we have all
        // of the entry module's dependencies
        timeStart('parse modules', 2);
        return Promise.all([
            this.moduleLoader.addEntryModules(normalizeEntryModules(entryModules), true),
            (manualChunks &&
                typeof manualChunks === 'object' &&
                this.moduleLoader.addManualChunks(manualChunks))
        ]).then(([{ entryModules, manualChunkModulesByAlias }]) => {
            if (entryModules.length === 0) {
                throw new Error('You must supply options.input to rollup');
            }
            for (const module of this.moduleById.values()) {
                if (module instanceof Module) {
                    this.modules.push(module);
                }
                else {
                    this.externalModules.push(module);
                }
            }
            timeEnd('parse modules', 2);
            this.phase = BuildPhase.ANALYSE;
            // Phase 2 - linking. We populate the module dependency links and
            // determine the topological execution order for the bundle
            timeStart('analyse dependency graph', 2);
            this.link(entryModules);
            timeEnd('analyse dependency graph', 2);
            // Phase 3 – marking. We include all statements that should be included
            timeStart('mark included statements', 2);
            if (inlineDynamicImports) {
                if (entryModules.length > 1) {
                    throw new Error('Internal Error: can only inline dynamic imports for single-file builds.');
                }
            }
            for (const module of entryModules) {
                module.includeAllExports();
            }
            this.includeMarked(this.modules);
            // check for unused external imports
            for (const externalModule of this.externalModules)
                externalModule.warnUnusedImports();
            timeEnd('mark included statements', 2);
            // Phase 4 – we construct the chunks, working out the optimal chunking using
            // entry point graph colouring, before generating the import and export facades
            timeStart('generate chunks', 2);
            if (!this.preserveModules && !inlineDynamicImports) {
                assignChunkColouringHashes(entryModules, manualChunkModulesByAlias);
            }
            // TODO: there is one special edge case unhandled here and that is that any module
            //       exposed as an unresolvable export * (to a graph external export *,
            //       either as a namespace import reexported or top-level export *)
            //       should be made to be its own entry point module before chunking
            let chunks = [];
            if (this.preserveModules) {
                for (const module of this.modules) {
                    const chunk = new Chunk$1(this, [module]);
                    if (module.isEntryPoint || !chunk.isEmpty) {
                        chunk.entryModules = [module];
                    }
                    chunks.push(chunk);
                }
            }
            else {
                const chunkModules = {};
                for (const module of this.modules) {
                    const entryPointsHashStr = Uint8ArrayToHexString(module.entryPointsHash);
                    const curChunk = chunkModules[entryPointsHashStr];
                    if (curChunk) {
                        curChunk.push(module);
                    }
                    else {
                        chunkModules[entryPointsHashStr] = [module];
                    }
                }
                for (const entryHashSum in chunkModules) {
                    const chunkModulesOrdered = chunkModules[entryHashSum];
                    sortByExecutionOrder(chunkModulesOrdered);
                    const chunk = new Chunk$1(this, chunkModulesOrdered);
                    chunks.push(chunk);
                }
            }
            for (const chunk of chunks) {
                chunk.link();
            }
            chunks = chunks.filter(isChunkRendered);
            const facades = [];
            for (const chunk of chunks) {
                facades.push(...chunk.generateFacades());
            }
            timeEnd('generate chunks', 2);
            this.phase = BuildPhase.GENERATE;
            return chunks.concat(facades);
        });
    }
    getCache() {
        // handle plugin cache eviction
        for (const name in this.pluginCache) {
            const cache = this.pluginCache[name];
            let allDeleted = true;
            for (const key of Object.keys(cache)) {
                if (cache[key][0] >= this.cacheExpiry)
                    delete cache[key];
                else
                    allDeleted = false;
            }
            if (allDeleted)
                delete this.pluginCache[name];
        }
        return {
            modules: this.modules.map(module => module.toJSON()),
            plugins: this.pluginCache
        };
    }
    includeMarked(modules) {
        if (this.treeshakingOptions) {
            let treeshakingPass = 1;
            do {
                timeStart(`treeshaking pass ${treeshakingPass}`, 3);
                this.needsTreeshakingPass = false;
                for (const module of modules) {
                    if (module.isExecuted)
                        module.include();
                }
                timeEnd(`treeshaking pass ${treeshakingPass++}`, 3);
            } while (this.needsTreeshakingPass);
        }
        else {
            // Necessary to properly replace namespace imports
            for (const module of modules)
                module.includeAllInBundle();
        }
    }
    warn(warning) {
        warning.toString = () => {
            let str = '';
            if (warning.plugin)
                str += `(${warning.plugin} plugin) `;
            if (warning.loc)
                str += `${relativeId(warning.loc.file)} (${warning.loc.line}:${warning.loc.column}) `;
            str += warning.message;
            return str;
        };
        this.onwarn(warning);
    }
    warnDeprecation(deprecation, activeDeprecation) {
        if (activeDeprecation || this.strictDeprecations) {
            const warning = errDeprecation(deprecation);
            if (this.strictDeprecations) {
                return error(warning);
            }
            this.warn(warning);
        }
    }
    link(entryModules) {
        for (const module of this.modules) {
            module.linkDependencies();
        }
        const { orderedModules, cyclePaths } = analyseModuleExecution(entryModules);
        for (const cyclePath of cyclePaths) {
            this.warn({
                code: 'CIRCULAR_DEPENDENCY',
                cycle: cyclePath,
                importer: cyclePath[0],
                message: `Circular dependency: ${cyclePath.join(' -> ')}`
            });
        }
        this.modules = orderedModules;
        for (const module of this.modules) {
            module.bindReferences();
        }
        this.warnForMissingExports();
    }
    warnForMissingExports() {
        for (const module of this.modules) {
            for (const importName of Object.keys(module.importDescriptions)) {
                const importDescription = module.importDescriptions[importName];
                if (importDescription.name !== '*' &&
                    !importDescription.module.getVariableForExportName(importDescription.name)) {
                    module.warn({
                        code: 'NON_EXISTENT_EXPORT',
                        message: `Non-existent export '${importDescription.name}' is imported from ${relativeId(importDescription.module.id)}`,
                        name: importDescription.name,
                        source: importDescription.module.id
                    }, importDescription.start);
                }
            }
        }
    }
}

function evalIfFn(strOrFn) {
    switch (typeof strOrFn) {
        case 'function':
            return strOrFn();
        case 'string':
            return strOrFn;
        default:
            return '';
    }
}
const concatSep = (out, next) => (next ? `${out}\n${next}` : out);
const concatDblSep = (out, next) => (next ? `${out}\n\n${next}` : out);
function createAddons(options, outputPluginDriver) {
    return Promise.all([
        outputPluginDriver.hookReduceValue('banner', evalIfFn(options.banner), [], concatSep),
        outputPluginDriver.hookReduceValue('footer', evalIfFn(options.footer), [], concatSep),
        outputPluginDriver.hookReduceValue('intro', evalIfFn(options.intro), [], concatDblSep),
        outputPluginDriver.hookReduceValue('outro', evalIfFn(options.outro), [], concatDblSep)
    ])
        .then(([banner, footer, intro, outro]) => {
        if (intro)
            intro += '\n\n';
        if (outro)
            outro = `\n\n${outro}`;
        if (banner.length)
            banner += '\n';
        if (footer.length)
            footer = '\n' + footer;
        return { intro, outro, banner, footer };
    })
        .catch((err) => {
        return error({
            code: 'ADDON_ERROR',
            message: `Could not retrieve ${err.hook}. Check configuration of plugin ${err.plugin}.
\tError Message: ${err.message}`
        });
    });
}

function assignChunkIds(chunks, inputOptions, outputOptions, inputBase, addons, bundle, outputPluginDriver) {
    const entryChunks = [];
    const otherChunks = [];
    for (const chunk of chunks) {
        (chunk.facadeModule && chunk.facadeModule.isUserDefinedEntryPoint
            ? entryChunks
            : otherChunks).push(chunk);
    }
    // make sure entry chunk names take precedence with regard to deconflicting
    const chunksForNaming = entryChunks.concat(otherChunks);
    for (const chunk of chunksForNaming) {
        if (outputOptions.file) {
            chunk.id = basename(outputOptions.file);
        }
        else if (inputOptions.preserveModules) {
            chunk.id = chunk.generateIdPreserveModules(inputBase, outputOptions, bundle);
        }
        else {
            chunk.id = chunk.generateId(addons, outputOptions, bundle, true, outputPluginDriver);
        }
        bundle[chunk.id] = FILE_PLACEHOLDER;
    }
}

// ported from https://github.com/substack/node-commondir
function commondir(files) {
    if (files.length === 0)
        return '/';
    if (files.length === 1)
        return dirname(files[0]);
    const commonSegments = files.slice(1).reduce((commonSegments, file) => {
        const pathSegements = file.split(/\/+|\\+/);
        let i;
        for (i = 0; commonSegments[i] === pathSegements[i] &&
            i < Math.min(commonSegments.length, pathSegements.length); i++)
            ;
        return commonSegments.slice(0, i);
    }, files[0].split(/\/+|\\+/));
    // Windows correctly handles paths with forward-slashes
    return commonSegments.length > 1 ? commonSegments.join('/') : '/';
}

function getExportMode(chunk, { exports: exportMode, name, format }, facadeModuleId) {
    const exportKeys = chunk.getExportNames();
    if (exportMode === 'default') {
        if (exportKeys.length !== 1 || exportKeys[0] !== 'default') {
            return error(errIncompatibleExportOptionValue('default', exportKeys, facadeModuleId));
        }
    }
    else if (exportMode === 'none' && exportKeys.length) {
        return error(errIncompatibleExportOptionValue('none', exportKeys, facadeModuleId));
    }
    if (!exportMode || exportMode === 'auto') {
        if (exportKeys.length === 0) {
            exportMode = 'none';
        }
        else if (exportKeys.length === 1 && exportKeys[0] === 'default') {
            exportMode = 'default';
        }
        else {
            if (format !== 'es' && exportKeys.indexOf('default') !== -1) {
                chunk.graph.warn(errMixedExport(facadeModuleId, name));
            }
            exportMode = 'named';
        }
    }
    return exportMode;
}

const createGetOption = (config, command) => (name, defaultValue) => command[name] !== undefined
    ? command[name]
    : config[name] !== undefined
        ? config[name]
        : defaultValue;
const normalizeObjectOptionValue = (optionValue) => {
    if (!optionValue) {
        return optionValue;
    }
    if (typeof optionValue !== 'object') {
        return {};
    }
    return optionValue;
};
const getObjectOption = (config, command, name) => {
    const commandOption = normalizeObjectOptionValue(command[name]);
    const configOption = normalizeObjectOptionValue(config[name]);
    if (commandOption !== undefined) {
        return commandOption && configOption ? Object.assign(Object.assign({}, configOption), commandOption) : commandOption;
    }
    return configOption;
};
function ensureArray(items) {
    if (Array.isArray(items)) {
        return items.filter(Boolean);
    }
    if (items) {
        return [items];
    }
    return [];
}
const defaultOnWarn = warning => {
    if (typeof warning === 'string') {
        console.warn(warning);
    }
    else {
        console.warn(warning.message);
    }
};
const getOnWarn = (config, defaultOnWarnHandler = defaultOnWarn) => config.onwarn
    ? warning => config.onwarn(warning, defaultOnWarnHandler)
    : defaultOnWarnHandler;
const getExternal = (config, command) => {
    const configExternal = config.external;
    return typeof configExternal === 'function'
        ? (id, ...rest) => configExternal(id, ...rest) || command.external.indexOf(id) !== -1
        : (typeof config.external === 'string'
            ? [configExternal]
            : Array.isArray(configExternal)
                ? configExternal
                : []).concat(command.external);
};
const commandAliases = {
    c: 'config',
    d: 'dir',
    e: 'external',
    f: 'format',
    g: 'globals',
    h: 'help',
    i: 'input',
    m: 'sourcemap',
    n: 'name',
    o: 'file',
    p: 'plugin',
    v: 'version',
    w: 'watch'
};
function mergeOptions({ config = {}, command: rawCommandOptions = {}, defaultOnWarnHandler }) {
    const command = getCommandOptions(rawCommandOptions);
    const inputOptions = getInputOptions(config, command, defaultOnWarnHandler);
    if (command.output) {
        Object.assign(command, command.output);
    }
    const output = config.output;
    const normalizedOutputOptions = Array.isArray(output) ? output : output ? [output] : [];
    if (normalizedOutputOptions.length === 0)
        normalizedOutputOptions.push({});
    const outputOptions = normalizedOutputOptions.map(singleOutputOptions => getOutputOptions(singleOutputOptions, command));
    const unknownOptionErrors = [];
    const validInputOptions = Object.keys(inputOptions);
    addUnknownOptionErrors(unknownOptionErrors, Object.keys(config), validInputOptions, 'input option', /^output$/);
    const validOutputOptions = Object.keys(outputOptions[0]);
    addUnknownOptionErrors(unknownOptionErrors, outputOptions.reduce((allKeys, options) => allKeys.concat(Object.keys(options)), []), validOutputOptions, 'output option');
    const validCliOutputOptions = validOutputOptions.filter(option => option !== 'sourcemapPathTransform');
    addUnknownOptionErrors(unknownOptionErrors, Object.keys(command), validInputOptions.concat(validCliOutputOptions, Object.keys(commandAliases), 'config', 'environment', 'plugin', 'silent', 'stdin'), 'CLI flag', /^_|output|(config.*)$/);
    return {
        inputOptions,
        optionError: unknownOptionErrors.length > 0 ? unknownOptionErrors.join('\n') : null,
        outputOptions
    };
}
function addUnknownOptionErrors(errors, options, validOptions, optionType, ignoredKeys = /$./) {
    const validOptionSet = new Set(validOptions);
    const unknownOptions = options.filter(key => !validOptionSet.has(key) && !ignoredKeys.test(key));
    if (unknownOptions.length > 0)
        errors.push(`Unknown ${optionType}: ${unknownOptions.join(', ')}. Allowed options: ${Array.from(validOptionSet)
            .sort()
            .join(', ')}`);
}
function getCommandOptions(rawCommandOptions) {
    const external = rawCommandOptions.external && typeof rawCommandOptions.external === 'string'
        ? rawCommandOptions.external.split(',')
        : [];
    return Object.assign(Object.assign({}, rawCommandOptions), { external, globals: typeof rawCommandOptions.globals === 'string'
            ? rawCommandOptions.globals.split(',').reduce((globals, globalDefinition) => {
                const [id, variableName] = globalDefinition.split(':');
                globals[id] = variableName;
                if (external.indexOf(id) === -1) {
                    external.push(id);
                }
                return globals;
            }, Object.create(null))
            : undefined });
}
function getInputOptions(config, command = { external: [], globals: undefined }, defaultOnWarnHandler) {
    const getOption = createGetOption(config, command);
    const inputOptions = {
        acorn: config.acorn,
        acornInjectPlugins: config.acornInjectPlugins,
        cache: getOption('cache'),
        chunkGroupingSize: getOption('chunkGroupingSize', 5000),
        context: getOption('context'),
        experimentalCacheExpiry: getOption('experimentalCacheExpiry', 10),
        experimentalOptimizeChunks: getOption('experimentalOptimizeChunks'),
        external: getExternal(config, command),
        inlineDynamicImports: getOption('inlineDynamicImports', false),
        input: getOption('input', []),
        manualChunks: getOption('manualChunks'),
        moduleContext: config.moduleContext,
        onwarn: getOnWarn(config, defaultOnWarnHandler),
        perf: getOption('perf', false),
        plugins: ensureArray(config.plugins),
        preserveModules: getOption('preserveModules'),
        preserveSymlinks: getOption('preserveSymlinks'),
        shimMissingExports: getOption('shimMissingExports'),
        strictDeprecations: getOption('strictDeprecations', false),
        treeshake: getObjectOption(config, command, 'treeshake'),
        watch: config.watch
    };
    // support rollup({ cache: prevBuildObject })
    if (inputOptions.cache && inputOptions.cache.cache)
        inputOptions.cache = inputOptions.cache.cache;
    return inputOptions;
}
function getOutputOptions(config, command = {}) {
    const getOption = createGetOption(config, command);
    let format = getOption('format');
    // Handle format aliases
    switch (format) {
        case undefined:
        case 'esm':
        case 'module':
            format = 'es';
            break;
        case 'commonjs':
            format = 'cjs';
    }
    return {
        amd: Object.assign(Object.assign({}, config.amd), command.amd),
        assetFileNames: getOption('assetFileNames'),
        banner: getOption('banner'),
        chunkFileNames: getOption('chunkFileNames'),
        compact: getOption('compact', false),
        dir: getOption('dir'),
        dynamicImportFunction: getOption('dynamicImportFunction'),
        entryFileNames: getOption('entryFileNames'),
        esModule: getOption('esModule', true),
        exports: getOption('exports'),
        extend: getOption('extend'),
        externalLiveBindings: getOption('externalLiveBindings', true),
        file: getOption('file'),
        footer: getOption('footer'),
        format,
        freeze: getOption('freeze', true),
        globals: getOption('globals'),
        hoistTransitiveImports: getOption('hoistTransitiveImports', true),
        indent: getOption('indent', true),
        interop: getOption('interop', true),
        intro: getOption('intro'),
        name: getOption('name'),
        namespaceToStringTag: getOption('namespaceToStringTag', false),
        noConflict: getOption('noConflict'),
        outro: getOption('outro'),
        paths: getOption('paths'),
        plugins: ensureArray(config.plugins),
        preferConst: getOption('preferConst'),
        sourcemap: getOption('sourcemap'),
        sourcemapExcludeSources: getOption('sourcemapExcludeSources'),
        sourcemapFile: getOption('sourcemapFile'),
        sourcemapPathTransform: getOption('sourcemapPathTransform'),
        strict: getOption('strict', true)
    };
}

function checkOutputOptions(options) {
    if (options.format === 'es6') {
        return error(errDeprecation({
            message: 'The "es6" output format is deprecated – use "esm" instead',
            url: `https://rollupjs.org/guide/en/#output-format`
        }));
    }
    if (['amd', 'cjs', 'system', 'es', 'iife', 'umd'].indexOf(options.format) < 0) {
        return error({
            message: `You must specify "output.format", which can be one of "amd", "cjs", "system", "esm", "iife" or "umd".`,
            url: `https://rollupjs.org/guide/en/#output-format`
        });
    }
    if (options.exports && !['default', 'named', 'none', 'auto'].includes(options.exports)) {
        return error(errInvalidExportOptionValue(options.exports));
    }
}
function getAbsoluteEntryModulePaths(chunks) {
    const absoluteEntryModulePaths = [];
    for (const chunk of chunks) {
        for (const entryModule of chunk.entryModules) {
            if (isAbsolute(entryModule.id)) {
                absoluteEntryModulePaths.push(entryModule.id);
            }
        }
    }
    return absoluteEntryModulePaths;
}
const throwAsyncGenerateError = {
    get() {
        throw new Error(`bundle.generate(...) now returns a Promise instead of a { code, map } object`);
    }
};
function applyOptionHook(inputOptions, plugin) {
    if (plugin.options)
        return plugin.options.call({ meta: { rollupVersion: version } }, inputOptions) || inputOptions;
    return inputOptions;
}
function normalizePlugins(rawPlugins, anonymousPrefix) {
    const plugins = ensureArray(rawPlugins);
    for (let pluginIndex = 0; pluginIndex < plugins.length; pluginIndex++) {
        const plugin = plugins[pluginIndex];
        if (!plugin.name) {
            plugin.name = `${anonymousPrefix}${pluginIndex + 1}`;
        }
    }
    return plugins;
}
function getInputOptions$1(rawInputOptions) {
    if (!rawInputOptions) {
        throw new Error('You must supply an options object to rollup');
    }
    let { inputOptions, optionError } = mergeOptions({
        config: rawInputOptions
    });
    if (optionError)
        inputOptions.onwarn({ message: optionError, code: 'UNKNOWN_OPTION' });
    inputOptions = inputOptions.plugins.reduce(applyOptionHook, inputOptions);
    inputOptions.plugins = normalizePlugins(inputOptions.plugins, ANONYMOUS_PLUGIN_PREFIX);
    if (inputOptions.inlineDynamicImports) {
        if (inputOptions.preserveModules)
            return error({
                code: 'INVALID_OPTION',
                message: `"preserveModules" does not support the "inlineDynamicImports" option.`
            });
        if (inputOptions.manualChunks)
            return error({
                code: 'INVALID_OPTION',
                message: '"manualChunks" option is not supported for "inlineDynamicImports".'
            });
        if (inputOptions.experimentalOptimizeChunks)
            return error({
                code: 'INVALID_OPTION',
                message: '"experimentalOptimizeChunks" option is not supported for "inlineDynamicImports".'
            });
        if ((inputOptions.input instanceof Array && inputOptions.input.length > 1) ||
            (typeof inputOptions.input === 'object' && Object.keys(inputOptions.input).length > 1))
            return error({
                code: 'INVALID_OPTION',
                message: 'Multiple inputs are not supported for "inlineDynamicImports".'
            });
    }
    else if (inputOptions.preserveModules) {
        if (inputOptions.manualChunks)
            return error({
                code: 'INVALID_OPTION',
                message: '"preserveModules" does not support the "manualChunks" option.'
            });
        if (inputOptions.experimentalOptimizeChunks)
            return error({
                code: 'INVALID_OPTION',
                message: '"preserveModules" does not support the "experimentalOptimizeChunks" option.'
            });
    }
    return inputOptions;
}
let curWatcher;
function setWatcher(watcher) {
    curWatcher = watcher;
}
function assignChunksToBundle(chunks, outputBundle) {
    for (let i = 0; i < chunks.length; i++) {
        const chunk = chunks[i];
        const facadeModule = chunk.facadeModule;
        outputBundle[chunk.id] = {
            code: undefined,
            dynamicImports: chunk.getDynamicImportIds(),
            exports: chunk.getExportNames(),
            facadeModuleId: facadeModule && facadeModule.id,
            fileName: chunk.id,
            imports: chunk.getImportIds(),
            isDynamicEntry: facadeModule !== null && facadeModule.dynamicallyImportedBy.length > 0,
            isEntry: facadeModule !== null && facadeModule.isEntryPoint,
            map: undefined,
            modules: chunk.renderedModules,
            get name() {
                return chunk.getChunkName();
            },
            type: 'chunk'
        };
    }
    return outputBundle;
}
function rollup(rawInputOptions) {
    return __awaiter(this, void 0, void 0, function* () {
        const inputOptions = getInputOptions$1(rawInputOptions);
        initialiseTimers(inputOptions);
        const graph = new Graph(inputOptions, curWatcher);
        curWatcher = undefined;
        // remove the cache option from the memory after graph creation (cache is not used anymore)
        const useCache = rawInputOptions.cache !== false;
        delete inputOptions.cache;
        delete rawInputOptions.cache;
        timeStart('BUILD', 1);
        let chunks;
        try {
            yield graph.pluginDriver.hookParallel('buildStart', [inputOptions]);
            chunks = yield graph.build(inputOptions.input, inputOptions.manualChunks, inputOptions.inlineDynamicImports);
        }
        catch (err) {
            const watchFiles = Object.keys(graph.watchFiles);
            if (watchFiles.length > 0) {
                err.watchFiles = watchFiles;
            }
            yield graph.pluginDriver.hookParallel('buildEnd', [err]);
            throw err;
        }
        yield graph.pluginDriver.hookParallel('buildEnd', []);
        timeEnd('BUILD', 1);
        // ensure we only do one optimization pass per build
        let optimized = false;
        function getOutputOptionsAndPluginDriver(rawOutputOptions) {
            if (!rawOutputOptions) {
                throw new Error('You must supply an options object');
            }
            const outputPluginDriver = graph.pluginDriver.createOutputPluginDriver(normalizePlugins(rawOutputOptions.plugins, ANONYMOUS_OUTPUT_PLUGIN_PREFIX));
            return {
                outputOptions: normalizeOutputOptions(inputOptions, rawOutputOptions, chunks.length > 1, outputPluginDriver),
                outputPluginDriver
            };
        }
        function generate(outputOptions, isWrite, outputPluginDriver) {
            return __awaiter(this, void 0, void 0, function* () {
                timeStart('GENERATE', 1);
                const assetFileNames = outputOptions.assetFileNames || 'assets/[name]-[hash][extname]';
                const inputBase = commondir(getAbsoluteEntryModulePaths(chunks));
                const outputBundleWithPlaceholders = Object.create(null);
                outputPluginDriver.setOutputBundle(outputBundleWithPlaceholders, assetFileNames);
                let outputBundle;
                try {
                    yield outputPluginDriver.hookParallel('renderStart', [outputOptions, inputOptions]);
                    const addons = yield createAddons(outputOptions, outputPluginDriver);
                    for (const chunk of chunks) {
                        if (!inputOptions.preserveModules)
                            chunk.generateInternalExports(outputOptions);
                        if (inputOptions.preserveModules || (chunk.facadeModule && chunk.facadeModule.isEntryPoint))
                            chunk.exportMode = getExportMode(chunk, outputOptions, chunk.facadeModule.id);
                    }
                    for (const chunk of chunks) {
                        chunk.preRender(outputOptions, inputBase);
                    }
                    if (!optimized && inputOptions.experimentalOptimizeChunks) {
                        optimizeChunks(chunks, outputOptions, inputOptions.chunkGroupingSize, inputBase);
                        optimized = true;
                    }
                    assignChunkIds(chunks, inputOptions, outputOptions, inputBase, addons, outputBundleWithPlaceholders, outputPluginDriver);
                    outputBundle = assignChunksToBundle(chunks, outputBundleWithPlaceholders);
                    yield Promise.all(chunks.map(chunk => {
                        const outputChunk = outputBundleWithPlaceholders[chunk.id];
                        return chunk
                            .render(outputOptions, addons, outputChunk, outputPluginDriver)
                            .then(rendered => {
                            outputChunk.code = rendered.code;
                            outputChunk.map = rendered.map;
                            return outputPluginDriver.hookParallel('ongenerate', [
                                Object.assign({ bundle: outputChunk }, outputOptions),
                                outputChunk
                            ]);
                        });
                    }));
                }
                catch (error) {
                    yield outputPluginDriver.hookParallel('renderError', [error]);
                    throw error;
                }
                yield outputPluginDriver.hookSeq('generateBundle', [outputOptions, outputBundle, isWrite]);
                for (const key of Object.keys(outputBundle)) {
                    const file = outputBundle[key];
                    if (!file.type) {
                        graph.warnDeprecation('A plugin is directly adding properties to the bundle object in the "generateBundle" hook. This is deprecated and will be removed in a future Rollup version, please use "this.emitFile" instead.', false);
                        file.type = 'asset';
                    }
                }
                outputPluginDriver.finaliseAssets();
                timeEnd('GENERATE', 1);
                return outputBundle;
            });
        }
        const cache = useCache ? graph.getCache() : undefined;
        const result = {
            cache: cache,
            generate: ((rawOutputOptions) => {
                const { outputOptions, outputPluginDriver } = getOutputOptionsAndPluginDriver(rawOutputOptions);
                const promise = generate(outputOptions, false, outputPluginDriver).then(result => createOutput(result));
                Object.defineProperty(promise, 'code', throwAsyncGenerateError);
                Object.defineProperty(promise, 'map', throwAsyncGenerateError);
                return promise;
            }),
            watchFiles: Object.keys(graph.watchFiles),
            write: ((rawOutputOptions) => {
                const { outputOptions, outputPluginDriver } = getOutputOptionsAndPluginDriver(rawOutputOptions);
                if (!outputOptions.dir && !outputOptions.file) {
                    return error({
                        code: 'MISSING_OPTION',
                        message: 'You must specify "output.file" or "output.dir" for the build.'
                    });
                }
                return generate(outputOptions, true, outputPluginDriver).then((bundle) => __awaiter(this, void 0, void 0, function* () {
                    let chunkCount = 0;
                    for (const fileName of Object.keys(bundle)) {
                        const file = bundle[fileName];
                        if (file.type === 'asset')
                            continue;
                        chunkCount++;
                        if (chunkCount > 1)
                            break;
                    }
                    if (chunkCount > 1) {
                        if (outputOptions.sourcemapFile)
                            return error({
                                code: 'INVALID_OPTION',
                                message: '"output.sourcemapFile" is only supported for single-file builds.'
                            });
                        if (typeof outputOptions.file === 'string')
                            return error({
                                code: 'INVALID_OPTION',
                                message: 'When building multiple chunks, the "output.dir" option must be used, not "output.file".' +
                                    (typeof inputOptions.input !== 'string' ||
                                        inputOptions.inlineDynamicImports === true
                                        ? ''
                                        : ' To inline dynamic imports, set the "inlineDynamicImports" option.')
                            });
                    }
                    yield Promise.all(Object.keys(bundle).map(chunkId => writeOutputFile(result, bundle[chunkId], outputOptions, outputPluginDriver)));
                    yield outputPluginDriver.hookParallel('writeBundle', [bundle]);
                    return createOutput(bundle);
                }));
            })
        };
        if (inputOptions.perf === true)
            result.getTimings = getTimings;
        return result;
    });
}
var SortingFileType;
(function (SortingFileType) {
    SortingFileType[SortingFileType["ENTRY_CHUNK"] = 0] = "ENTRY_CHUNK";
    SortingFileType[SortingFileType["SECONDARY_CHUNK"] = 1] = "SECONDARY_CHUNK";
    SortingFileType[SortingFileType["ASSET"] = 2] = "ASSET";
})(SortingFileType || (SortingFileType = {}));
function getSortingFileType(file) {
    if (file.type === 'asset') {
        return SortingFileType.ASSET;
    }
    if (file.isEntry) {
        return SortingFileType.ENTRY_CHUNK;
    }
    return SortingFileType.SECONDARY_CHUNK;
}
function createOutput(outputBundle) {
    return {
        output: Object.keys(outputBundle)
            .map(fileName => outputBundle[fileName])
            .filter(outputFile => Object.keys(outputFile).length > 0).sort((outputFileA, outputFileB) => {
            const fileTypeA = getSortingFileType(outputFileA);
            const fileTypeB = getSortingFileType(outputFileB);
            if (fileTypeA === fileTypeB)
                return 0;
            return fileTypeA < fileTypeB ? -1 : 1;
        })
    };
}
function writeOutputFile(build, outputFile, outputOptions, outputPluginDriver) {
    const fileName = resolve(outputOptions.dir || dirname(outputOptions.file), outputFile.fileName);
    let writeSourceMapPromise;
    let source;
    if (outputFile.type === 'asset') {
        source = outputFile.source;
    }
    else {
        source = outputFile.code;
        if (outputOptions.sourcemap && outputFile.map) {
            let url;
            if (outputOptions.sourcemap === 'inline') {
                url = outputFile.map.toUrl();
            }
            else {
                url = `${basename(outputFile.fileName)}.map`;
                writeSourceMapPromise = writeFile(`${fileName}.map`, outputFile.map.toString());
            }
            if (outputOptions.sourcemap !== 'hidden') {
                source += `//# ${SOURCEMAPPING_URL}=${url}\n`;
            }
        }
    }
    return writeFile(fileName, source)
        .then(() => writeSourceMapPromise)
        .then(() => outputFile.type === 'chunk' &&
        outputPluginDriver.hookSeq('onwrite', [
            Object.assign({ bundle: build }, outputOptions),
            outputFile
        ]))
        .then(() => { });
}
function normalizeOutputOptions(inputOptions, rawOutputOptions, hasMultipleChunks, outputPluginDriver) {
    const mergedOptions = mergeOptions({
        config: {
            output: Object.assign(Object.assign(Object.assign({}, rawOutputOptions), rawOutputOptions.output), inputOptions.output)
        }
    });
    if (mergedOptions.optionError)
        throw new Error(mergedOptions.optionError);
    // now outputOptions is an array, but rollup.rollup API doesn't support arrays
    const mergedOutputOptions = mergedOptions.outputOptions[0];
    const outputOptionsReducer = (outputOptions, result) => result || outputOptions;
    const outputOptions = outputPluginDriver.hookReduceArg0Sync('outputOptions', [mergedOutputOptions], outputOptionsReducer, pluginContext => {
        const emitError = () => pluginContext.error(errCannotEmitFromOptionsHook());
        return Object.assign(Object.assign({}, pluginContext), { emitFile: emitError, setAssetSource: emitError });
    });
    checkOutputOptions(outputOptions);
    if (typeof outputOptions.file === 'string') {
        if (typeof outputOptions.dir === 'string')
            return error({
                code: 'INVALID_OPTION',
                message: 'You must set either "output.file" for a single-file build or "output.dir" when generating multiple chunks.'
            });
        if (inputOptions.preserveModules) {
            return error({
                code: 'INVALID_OPTION',
                message: 'You must set "output.dir" instead of "output.file" when using the "preserveModules" option.'
            });
        }
        if (typeof inputOptions.input === 'object' && !Array.isArray(inputOptions.input))
            return error({
                code: 'INVALID_OPTION',
                message: 'You must set "output.dir" instead of "output.file" when providing named inputs.'
            });
    }
    if (hasMultipleChunks) {
        if (outputOptions.format === 'umd' || outputOptions.format === 'iife')
            return error({
                code: 'INVALID_OPTION',
                message: 'UMD and IIFE output formats are not supported for code-splitting builds.'
            });
        if (typeof outputOptions.file === 'string')
            return error({
                code: 'INVALID_OPTION',
                message: 'You must set "output.dir" instead of "output.file" when generating multiple chunks.'
            });
    }
    return outputOptions;
}

var utils$1 = createCommonjsModule(function (module, exports) {
    exports.isInteger = num => {
        if (typeof num === 'number') {
            return Number.isInteger(num);
        }
        if (typeof num === 'string' && num.trim() !== '') {
            return Number.isInteger(Number(num));
        }
        return false;
    };
    /**
     * Find a node of the given type
     */
    exports.find = (node, type) => node.nodes.find(node => node.type === type);
    /**
     * Find a node of the given type
     */
    exports.exceedsLimit = (min, max, step = 1, limit) => {
        if (limit === false)
            return false;
        if (!exports.isInteger(min) || !exports.isInteger(max))
            return false;
        return ((Number(max) - Number(min)) / Number(step)) >= limit;
    };
    /**
     * Escape the given node with '\\' before node.value
     */
    exports.escapeNode = (block, n = 0, type) => {
        let node = block.nodes[n];
        if (!node)
            return;
        if ((type && node.type === type) || node.type === 'open' || node.type === 'close') {
            if (node.escaped !== true) {
                node.value = '\\' + node.value;
                node.escaped = true;
            }
        }
    };
    /**
     * Returns true if the given brace node should be enclosed in literal braces
     */
    exports.encloseBrace = node => {
        if (node.type !== 'brace')
            return false;
        if ((node.commas >> 0 + node.ranges >> 0) === 0) {
            node.invalid = true;
            return true;
        }
        return false;
    };
    /**
     * Returns true if a brace node is invalid.
     */
    exports.isInvalidBrace = block => {
        if (block.type !== 'brace')
            return false;
        if (block.invalid === true || block.dollar)
            return true;
        if ((block.commas >> 0 + block.ranges >> 0) === 0) {
            block.invalid = true;
            return true;
        }
        if (block.open !== true || block.close !== true) {
            block.invalid = true;
            return true;
        }
        return false;
    };
    /**
     * Returns true if a node is an open or close node
     */
    exports.isOpenOrClose = node => {
        if (node.type === 'open' || node.type === 'close') {
            return true;
        }
        return node.open === true || node.close === true;
    };
    /**
     * Reduce an array of text nodes.
     */
    exports.reduce = nodes => nodes.reduce((acc, node) => {
        if (node.type === 'text')
            acc.push(node.value);
        if (node.type === 'range')
            node.type = 'text';
        return acc;
    }, []);
    /**
     * Flatten an array
     */
    exports.flatten = (...args) => {
        const result = [];
        const flat = arr => {
            for (let i = 0; i < arr.length; i++) {
                let ele = arr[i];
                Array.isArray(ele) ? flat(ele) : ele !== void 0 && result.push(ele);
            }
            return result;
        };
        flat(args);
        return result;
    };
});

var stringify = (ast, options = {}) => {
    let stringify = (node, parent = {}) => {
        let invalidBlock = options.escapeInvalid && utils$1.isInvalidBrace(parent);
        let invalidNode = node.invalid === true && options.escapeInvalid === true;
        let output = '';
        if (node.value) {
            if ((invalidBlock || invalidNode) && utils$1.isOpenOrClose(node)) {
                return '\\' + node.value;
            }
            return node.value;
        }
        if (node.value) {
            return node.value;
        }
        if (node.nodes) {
            for (let child of node.nodes) {
                output += stringify(child);
            }
        }
        return output;
    };
    return stringify(ast);
};

/*!
 * is-number <https://github.com/jonschlinkert/is-number>
 *
 * Copyright (c) 2014-present, Jon Schlinkert.
 * Released under the MIT License.
 */
var isNumber = function (num) {
    if (typeof num === 'number') {
        return num - num === 0;
    }
    if (typeof num === 'string' && num.trim() !== '') {
        return Number.isFinite ? Number.isFinite(+num) : isFinite(+num);
    }
    return false;
};

const toRegexRange = (min, max, options) => {
    if (isNumber(min) === false) {
        throw new TypeError('toRegexRange: expected the first argument to be a number');
    }
    if (max === void 0 || min === max) {
        return String(min);
    }
    if (isNumber(max) === false) {
        throw new TypeError('toRegexRange: expected the second argument to be a number.');
    }
    let opts = Object.assign({ relaxZeros: true }, options);
    if (typeof opts.strictZeros === 'boolean') {
        opts.relaxZeros = opts.strictZeros === false;
    }
    let relax = String(opts.relaxZeros);
    let shorthand = String(opts.shorthand);
    let capture = String(opts.capture);
    let wrap = String(opts.wrap);
    let cacheKey = min + ':' + max + '=' + relax + shorthand + capture + wrap;
    if (toRegexRange.cache.hasOwnProperty(cacheKey)) {
        return toRegexRange.cache[cacheKey].result;
    }
    let a = Math.min(min, max);
    let b = Math.max(min, max);
    if (Math.abs(a - b) === 1) {
        let result = min + '|' + max;
        if (opts.capture) {
            return `(${result})`;
        }
        if (opts.wrap === false) {
            return result;
        }
        return `(?:${result})`;
    }
    let isPadded = hasPadding(min) || hasPadding(max);
    let state = { min, max, a, b };
    let positives = [];
    let negatives = [];
    if (isPadded) {
        state.isPadded = isPadded;
        state.maxLen = String(state.max).length;
    }
    if (a < 0) {
        let newMin = b < 0 ? Math.abs(b) : 1;
        negatives = splitToPatterns(newMin, Math.abs(a), state, opts);
        a = state.a = 0;
    }
    if (b >= 0) {
        positives = splitToPatterns(a, b, state, opts);
    }
    state.negatives = negatives;
    state.positives = positives;
    state.result = collatePatterns(negatives, positives);
    if (opts.capture === true) {
        state.result = `(${state.result})`;
    }
    else if (opts.wrap !== false && (positives.length + negatives.length) > 1) {
        state.result = `(?:${state.result})`;
    }
    toRegexRange.cache[cacheKey] = state;
    return state.result;
};
function collatePatterns(neg, pos, options) {
    let onlyNegative = filterPatterns(neg, pos, '-', false) || [];
    let onlyPositive = filterPatterns(pos, neg, '', false) || [];
    let intersected = filterPatterns(neg, pos, '-?', true) || [];
    let subpatterns = onlyNegative.concat(intersected).concat(onlyPositive);
    return subpatterns.join('|');
}
function splitToRanges(min, max) {
    let nines = 1;
    let zeros = 1;
    let stop = countNines(min, nines);
    let stops = new Set([max]);
    while (min <= stop && stop <= max) {
        stops.add(stop);
        nines += 1;
        stop = countNines(min, nines);
    }
    stop = countZeros(max + 1, zeros) - 1;
    while (min < stop && stop <= max) {
        stops.add(stop);
        zeros += 1;
        stop = countZeros(max + 1, zeros) - 1;
    }
    stops = [...stops];
    stops.sort(compare);
    return stops;
}
/**
 * Convert a range to a regex pattern
 * @param {Number} `start`
 * @param {Number} `stop`
 * @return {String}
 */
function rangeToPattern(start, stop, options) {
    if (start === stop) {
        return { pattern: start, count: [], digits: 0 };
    }
    let zipped = zip(start, stop);
    let digits = zipped.length;
    let pattern = '';
    let count = 0;
    for (let i = 0; i < digits; i++) {
        let [startDigit, stopDigit] = zipped[i];
        if (startDigit === stopDigit) {
            pattern += startDigit;
        }
        else if (startDigit !== '0' || stopDigit !== '9') {
            pattern += toCharacterClass(startDigit, stopDigit);
        }
        else {
            count++;
        }
    }
    if (count) {
        pattern += options.shorthand === true ? '\\d' : '[0-9]';
    }
    return { pattern, count: [count], digits };
}
function splitToPatterns(min, max, tok, options) {
    let ranges = splitToRanges(min, max);
    let tokens = [];
    let start = min;
    let prev;
    for (let i = 0; i < ranges.length; i++) {
        let max = ranges[i];
        let obj = rangeToPattern(String(start), String(max), options);
        let zeros = '';
        if (!tok.isPadded && prev && prev.pattern === obj.pattern) {
            if (prev.count.length > 1) {
                prev.count.pop();
            }
            prev.count.push(obj.count[0]);
            prev.string = prev.pattern + toQuantifier(prev.count);
            start = max + 1;
            continue;
        }
        if (tok.isPadded) {
            zeros = padZeros(max, tok, options);
        }
        obj.string = zeros + obj.pattern + toQuantifier(obj.count);
        tokens.push(obj);
        start = max + 1;
        prev = obj;
    }
    return tokens;
}
function filterPatterns(arr, comparison, prefix, intersection, options) {
    let result = [];
    for (let ele of arr) {
        let { string } = ele;
        // only push if _both_ are negative...
        if (!intersection && !contains(comparison, 'string', string)) {
            result.push(prefix + string);
        }
        // or _both_ are positive
        if (intersection && contains(comparison, 'string', string)) {
            result.push(prefix + string);
        }
    }
    return result;
}
/**
 * Zip strings
 */
function zip(a, b) {
    let arr = [];
    for (let i = 0; i < a.length; i++)
        arr.push([a[i], b[i]]);
    return arr;
}
function compare(a, b) {
    return a > b ? 1 : b > a ? -1 : 0;
}
function contains(arr, key, val) {
    return arr.some(ele => ele[key] === val);
}
function countNines(min, len) {
    return Number(String(min).slice(0, -len) + '9'.repeat(len));
}
function countZeros(integer, zeros) {
    return integer - (integer % Math.pow(10, zeros));
}
function toQuantifier(digits) {
    let [start = 0, stop = ''] = digits;
    if (stop || start > 1) {
        return `{${start + (stop ? ',' + stop : '')}}`;
    }
    return '';
}
function toCharacterClass(a, b, options) {
    return `[${a}${(b - a === 1) ? '' : '-'}${b}]`;
}
function hasPadding(str) {
    return /^-?(0+)\d/.test(str);
}
function padZeros(value, tok, options) {
    if (!tok.isPadded) {
        return value;
    }
    let diff = Math.abs(tok.maxLen - String(value).length);
    let relax = options.relaxZeros !== false;
    switch (diff) {
        case 0:
            return '';
        case 1:
            return relax ? '0?' : '0';
        case 2:
            return relax ? '0{0,2}' : '00';
        default: {
            return relax ? `0{0,${diff}}` : `0{${diff}}`;
        }
    }
}
/**
 * Cache
 */
toRegexRange.cache = {};
toRegexRange.clearCache = () => (toRegexRange.cache = {});
/**
 * Expose `toRegexRange`
 */
var toRegexRange_1 = toRegexRange;

const isObject$1 = val => val !== null && typeof val === 'object' && !Array.isArray(val);
const transform$1 = toNumber => {
    return value => toNumber === true ? Number(value) : String(value);
};
const isValidValue = value => {
    return typeof value === 'number' || (typeof value === 'string' && value !== '');
};
const isNumber$1 = num => Number.isInteger(+num);
const zeros = input => {
    let value = `${input}`;
    let index = -1;
    if (value[0] === '-')
        value = value.slice(1);
    if (value === '0')
        return false;
    while (value[++index] === '0')
        ;
    return index > 0;
};
const stringify$1 = (start, end, options) => {
    if (typeof start === 'string' || typeof end === 'string') {
        return true;
    }
    return options.stringify === true;
};
const pad = (input, maxLength, toNumber) => {
    if (maxLength > 0) {
        let dash = input[0] === '-' ? '-' : '';
        if (dash)
            input = input.slice(1);
        input = (dash + input.padStart(dash ? maxLength - 1 : maxLength, '0'));
    }
    if (toNumber === false) {
        return String(input);
    }
    return input;
};
const toMaxLen = (input, maxLength) => {
    let negative = input[0] === '-' ? '-' : '';
    if (negative) {
        input = input.slice(1);
        maxLength--;
    }
    while (input.length < maxLength)
        input = '0' + input;
    return negative ? ('-' + input) : input;
};
const toSequence = (parts, options) => {
    parts.negatives.sort((a, b) => a < b ? -1 : a > b ? 1 : 0);
    parts.positives.sort((a, b) => a < b ? -1 : a > b ? 1 : 0);
    let prefix = options.capture ? '' : '?:';
    let positives = '';
    let negatives = '';
    let result;
    if (parts.positives.length) {
        positives = parts.positives.join('|');
    }
    if (parts.negatives.length) {
        negatives = `-(${prefix}${parts.negatives.join('|')})`;
    }
    if (positives && negatives) {
        result = `${positives}|${negatives}`;
    }
    else {
        result = positives || negatives;
    }
    if (options.wrap) {
        return `(${prefix}${result})`;
    }
    return result;
};
const toRange = (a, b, isNumbers, options) => {
    if (isNumbers) {
        return toRegexRange_1(a, b, Object.assign({ wrap: false }, options));
    }
    let start = String.fromCharCode(a);
    if (a === b)
        return start;
    let stop = String.fromCharCode(b);
    return `[${start}-${stop}]`;
};
const toRegex = (start, end, options) => {
    if (Array.isArray(start)) {
        let wrap = options.wrap === true;
        let prefix = options.capture ? '' : '?:';
        return wrap ? `(${prefix}${start.join('|')})` : start.join('|');
    }
    return toRegexRange_1(start, end, options);
};
const rangeError = (...args) => {
    return new RangeError('Invalid range arguments: ' + util.inspect(...args));
};
const invalidRange = (start, end, options) => {
    if (options.strictRanges === true)
        throw rangeError([start, end]);
    return [];
};
const invalidStep = (step, options) => {
    if (options.strictRanges === true) {
        throw new TypeError(`Expected step "${step}" to be a number`);
    }
    return [];
};
const fillNumbers = (start, end, step = 1, options = {}) => {
    let a = Number(start);
    let b = Number(end);
    if (!Number.isInteger(a) || !Number.isInteger(b)) {
        if (options.strictRanges === true)
            throw rangeError([start, end]);
        return [];
    }
    // fix negative zero
    if (a === 0)
        a = 0;
    if (b === 0)
        b = 0;
    let descending = a > b;
    let startString = String(start);
    let endString = String(end);
    let stepString = String(step);
    step = Math.max(Math.abs(step), 1);
    let padded = zeros(startString) || zeros(endString) || zeros(stepString);
    let maxLen = padded ? Math.max(startString.length, endString.length, stepString.length) : 0;
    let toNumber = padded === false && stringify$1(start, end, options) === false;
    let format = options.transform || transform$1(toNumber);
    if (options.toRegex && step === 1) {
        return toRange(toMaxLen(start, maxLen), toMaxLen(end, maxLen), true, options);
    }
    let parts = { negatives: [], positives: [] };
    let push = num => parts[num < 0 ? 'negatives' : 'positives'].push(Math.abs(num));
    let range = [];
    let index = 0;
    while (descending ? a >= b : a <= b) {
        if (options.toRegex === true && step > 1) {
            push(a);
        }
        else {
            range.push(pad(format(a, index), maxLen, toNumber));
        }
        a = descending ? a - step : a + step;
        index++;
    }
    if (options.toRegex === true) {
        return step > 1
            ? toSequence(parts, options)
            : toRegex(range, null, Object.assign({ wrap: false }, options));
    }
    return range;
};
const fillLetters = (start, end, step = 1, options = {}) => {
    if ((!isNumber$1(start) && start.length > 1) || (!isNumber$1(end) && end.length > 1)) {
        return invalidRange(start, end, options);
    }
    let format = options.transform || (val => String.fromCharCode(val));
    let a = `${start}`.charCodeAt(0);
    let b = `${end}`.charCodeAt(0);
    let descending = a > b;
    let min = Math.min(a, b);
    let max = Math.max(a, b);
    if (options.toRegex && step === 1) {
        return toRange(min, max, false, options);
    }
    let range = [];
    let index = 0;
    while (descending ? a >= b : a <= b) {
        range.push(format(a, index));
        a = descending ? a - step : a + step;
        index++;
    }
    if (options.toRegex === true) {
        return toRegex(range, null, { wrap: false, options });
    }
    return range;
};
const fill = (start, end, step, options = {}) => {
    if (end == null && isValidValue(start)) {
        return [start];
    }
    if (!isValidValue(start) || !isValidValue(end)) {
        return invalidRange(start, end, options);
    }
    if (typeof step === 'function') {
        return fill(start, end, 1, { transform: step });
    }
    if (isObject$1(step)) {
        return fill(start, end, 0, step);
    }
    let opts = Object.assign({}, options);
    if (opts.capture === true)
        opts.wrap = true;
    step = step || opts.step || 1;
    if (!isNumber$1(step)) {
        if (step != null && !isObject$1(step))
            return invalidStep(step, opts);
        return fill(start, end, 1, step);
    }
    if (isNumber$1(start) && isNumber$1(end)) {
        return fillNumbers(start, end, step, opts);
    }
    return fillLetters(start, end, Math.max(Math.abs(step), 1), opts);
};
var fillRange = fill;

const compile = (ast, options = {}) => {
    let walk = (node, parent = {}) => {
        let invalidBlock = utils$1.isInvalidBrace(parent);
        let invalidNode = node.invalid === true && options.escapeInvalid === true;
        let invalid = invalidBlock === true || invalidNode === true;
        let prefix = options.escapeInvalid === true ? '\\' : '';
        let output = '';
        if (node.isOpen === true) {
            return prefix + node.value;
        }
        if (node.isClose === true) {
            return prefix + node.value;
        }
        if (node.type === 'open') {
            return invalid ? (prefix + node.value) : '(';
        }
        if (node.type === 'close') {
            return invalid ? (prefix + node.value) : ')';
        }
        if (node.type === 'comma') {
            return node.prev.type === 'comma' ? '' : (invalid ? node.value : '|');
        }
        if (node.value) {
            return node.value;
        }
        if (node.nodes && node.ranges > 0) {
            let args = utils$1.reduce(node.nodes);
            let range = fillRange(...args, Object.assign(Object.assign({}, options), { wrap: false, toRegex: true }));
            if (range.length !== 0) {
                return args.length > 1 && range.length > 1 ? `(${range})` : range;
            }
        }
        if (node.nodes) {
            for (let child of node.nodes) {
                output += walk(child, node);
            }
        }
        return output;
    };
    return walk(ast);
};
var compile_1 = compile;

const append = (queue = '', stash = '', enclose = false) => {
    let result = [];
    queue = [].concat(queue);
    stash = [].concat(stash);
    if (!stash.length)
        return queue;
    if (!queue.length) {
        return enclose ? utils$1.flatten(stash).map(ele => `{${ele}}`) : stash;
    }
    for (let item of queue) {
        if (Array.isArray(item)) {
            for (let value of item) {
                result.push(append(value, stash, enclose));
            }
        }
        else {
            for (let ele of stash) {
                if (enclose === true && typeof ele === 'string')
                    ele = `{${ele}}`;
                result.push(Array.isArray(ele) ? append(item, ele, enclose) : (item + ele));
            }
        }
    }
    return utils$1.flatten(result);
};
const expand = (ast, options = {}) => {
    let rangeLimit = options.rangeLimit === void 0 ? 1000 : options.rangeLimit;
    let walk = (node, parent = {}) => {
        node.queue = [];
        let p = parent;
        let q = parent.queue;
        while (p.type !== 'brace' && p.type !== 'root' && p.parent) {
            p = p.parent;
            q = p.queue;
        }
        if (node.invalid || node.dollar) {
            q.push(append(q.pop(), stringify(node, options)));
            return;
        }
        if (node.type === 'brace' && node.invalid !== true && node.nodes.length === 2) {
            q.push(append(q.pop(), ['{}']));
            return;
        }
        if (node.nodes && node.ranges > 0) {
            let args = utils$1.reduce(node.nodes);
            if (utils$1.exceedsLimit(...args, options.step, rangeLimit)) {
                throw new RangeError('expanded array length exceeds range limit. Use options.rangeLimit to increase or disable the limit.');
            }
            let range = fillRange(...args, options);
            if (range.length === 0) {
                range = stringify(node, options);
            }
            q.push(append(q.pop(), range));
            node.nodes = [];
            return;
        }
        let enclose = utils$1.encloseBrace(node);
        let queue = node.queue;
        let block = node;
        while (block.type !== 'brace' && block.type !== 'root' && block.parent) {
            block = block.parent;
            queue = block.queue;
        }
        for (let i = 0; i < node.nodes.length; i++) {
            let child = node.nodes[i];
            if (child.type === 'comma' && node.type === 'brace') {
                if (i === 1)
                    queue.push('');
                queue.push('');
                continue;
            }
            if (child.type === 'close') {
                q.push(append(q.pop(), queue, enclose));
                continue;
            }
            if (child.value && child.type !== 'open') {
                queue.push(append(queue.pop(), child.value));
                continue;
            }
            if (child.nodes) {
                walk(child, node);
            }
        }
        return queue;
    };
    return utils$1.flatten(walk(ast));
};
var expand_1 = expand;

var constants = {
    MAX_LENGTH: 1024 * 64,
    // Digits
    CHAR_0: '0',
    CHAR_9: '9',
    // Alphabet chars.
    CHAR_UPPERCASE_A: 'A',
    CHAR_LOWERCASE_A: 'a',
    CHAR_UPPERCASE_Z: 'Z',
    CHAR_LOWERCASE_Z: 'z',
    CHAR_LEFT_PARENTHESES: '(',
    CHAR_RIGHT_PARENTHESES: ')',
    CHAR_ASTERISK: '*',
    // Non-alphabetic chars.
    CHAR_AMPERSAND: '&',
    CHAR_AT: '@',
    CHAR_BACKSLASH: '\\',
    CHAR_BACKTICK: '`',
    CHAR_CARRIAGE_RETURN: '\r',
    CHAR_CIRCUMFLEX_ACCENT: '^',
    CHAR_COLON: ':',
    CHAR_COMMA: ',',
    CHAR_DOLLAR: '$',
    CHAR_DOT: '.',
    CHAR_DOUBLE_QUOTE: '"',
    CHAR_EQUAL: '=',
    CHAR_EXCLAMATION_MARK: '!',
    CHAR_FORM_FEED: '\f',
    CHAR_FORWARD_SLASH: '/',
    CHAR_HASH: '#',
    CHAR_HYPHEN_MINUS: '-',
    CHAR_LEFT_ANGLE_BRACKET: '<',
    CHAR_LEFT_CURLY_BRACE: '{',
    CHAR_LEFT_SQUARE_BRACKET: '[',
    CHAR_LINE_FEED: '\n',
    CHAR_NO_BREAK_SPACE: '\u00A0',
    CHAR_PERCENT: '%',
    CHAR_PLUS: '+',
    CHAR_QUESTION_MARK: '?',
    CHAR_RIGHT_ANGLE_BRACKET: '>',
    CHAR_RIGHT_CURLY_BRACE: '}',
    CHAR_RIGHT_SQUARE_BRACKET: ']',
    CHAR_SEMICOLON: ';',
    CHAR_SINGLE_QUOTE: '\'',
    CHAR_SPACE: ' ',
    CHAR_TAB: '\t',
    CHAR_UNDERSCORE: '_',
    CHAR_VERTICAL_LINE: '|',
    CHAR_ZERO_WIDTH_NOBREAK_SPACE: '\uFEFF' /* \uFEFF */
};

/**
 * Constants
 */
const { MAX_LENGTH, CHAR_BACKSLASH, /* \ */ CHAR_BACKTICK, /* ` */ CHAR_COMMA, /* , */ CHAR_DOT, /* . */ CHAR_LEFT_PARENTHESES, /* ( */ CHAR_RIGHT_PARENTHESES, /* ) */ CHAR_LEFT_CURLY_BRACE, /* { */ CHAR_RIGHT_CURLY_BRACE, /* } */ CHAR_LEFT_SQUARE_BRACKET, /* [ */ CHAR_RIGHT_SQUARE_BRACKET, /* ] */ CHAR_DOUBLE_QUOTE, /* " */ CHAR_SINGLE_QUOTE, /* ' */ CHAR_NO_BREAK_SPACE, CHAR_ZERO_WIDTH_NOBREAK_SPACE } = constants;
/**
 * parse
 */
const parse = (input, options = {}) => {
    if (typeof input !== 'string') {
        throw new TypeError('Expected a string');
    }
    let opts = options || {};
    let max = typeof opts.maxLength === 'number' ? Math.min(MAX_LENGTH, opts.maxLength) : MAX_LENGTH;
    if (input.length > max) {
        throw new SyntaxError(`Input length (${input.length}), exceeds max characters (${max})`);
    }
    let ast = { type: 'root', input, nodes: [] };
    let stack = [ast];
    let block = ast;
    let prev = ast;
    let brackets = 0;
    let length = input.length;
    let index = 0;
    let depth = 0;
    let value;
    /**
     * Helpers
     */
    const advance = () => input[index++];
    const push = node => {
        if (node.type === 'text' && prev.type === 'dot') {
            prev.type = 'text';
        }
        if (prev && prev.type === 'text' && node.type === 'text') {
            prev.value += node.value;
            return;
        }
        block.nodes.push(node);
        node.parent = block;
        node.prev = prev;
        prev = node;
        return node;
    };
    push({ type: 'bos' });
    while (index < length) {
        block = stack[stack.length - 1];
        value = advance();
        /**
         * Invalid chars
         */
        if (value === CHAR_ZERO_WIDTH_NOBREAK_SPACE || value === CHAR_NO_BREAK_SPACE) {
            continue;
        }
        /**
         * Escaped chars
         */
        if (value === CHAR_BACKSLASH) {
            push({ type: 'text', value: (options.keepEscaping ? value : '') + advance() });
            continue;
        }
        /**
         * Right square bracket (literal): ']'
         */
        if (value === CHAR_RIGHT_SQUARE_BRACKET) {
            push({ type: 'text', value: '\\' + value });
            continue;
        }
        /**
         * Left square bracket: '['
         */
        if (value === CHAR_LEFT_SQUARE_BRACKET) {
            brackets++;
            let next;
            while (index < length && (next = advance())) {
                value += next;
                if (next === CHAR_LEFT_SQUARE_BRACKET) {
                    brackets++;
                    continue;
                }
                if (next === CHAR_BACKSLASH) {
                    value += advance();
                    continue;
                }
                if (next === CHAR_RIGHT_SQUARE_BRACKET) {
                    brackets--;
                    if (brackets === 0) {
                        break;
                    }
                }
            }
            push({ type: 'text', value });
            continue;
        }
        /**
         * Parentheses
         */
        if (value === CHAR_LEFT_PARENTHESES) {
            block = push({ type: 'paren', nodes: [] });
            stack.push(block);
            push({ type: 'text', value });
            continue;
        }
        if (value === CHAR_RIGHT_PARENTHESES) {
            if (block.type !== 'paren') {
                push({ type: 'text', value });
                continue;
            }
            block = stack.pop();
            push({ type: 'text', value });
            block = stack[stack.length - 1];
            continue;
        }
        /**
         * Quotes: '|"|`
         */
        if (value === CHAR_DOUBLE_QUOTE || value === CHAR_SINGLE_QUOTE || value === CHAR_BACKTICK) {
            let open = value;
            let next;
            if (options.keepQuotes !== true) {
                value = '';
            }
            while (index < length && (next = advance())) {
                if (next === CHAR_BACKSLASH) {
                    value += next + advance();
                    continue;
                }
                if (next === open) {
                    if (options.keepQuotes === true)
                        value += next;
                    break;
                }
                value += next;
            }
            push({ type: 'text', value });
            continue;
        }
        /**
         * Left curly brace: '{'
         */
        if (value === CHAR_LEFT_CURLY_BRACE) {
            depth++;
            let dollar = prev.value && prev.value.slice(-1) === '$' || block.dollar === true;
            let brace = {
                type: 'brace',
                open: true,
                close: false,
                dollar,
                depth,
                commas: 0,
                ranges: 0,
                nodes: []
            };
            block = push(brace);
            stack.push(block);
            push({ type: 'open', value });
            continue;
        }
        /**
         * Right curly brace: '}'
         */
        if (value === CHAR_RIGHT_CURLY_BRACE) {
            if (block.type !== 'brace') {
                push({ type: 'text', value });
                continue;
            }
            let type = 'close';
            block = stack.pop();
            block.close = true;
            push({ type, value });
            depth--;
            block = stack[stack.length - 1];
            continue;
        }
        /**
         * Comma: ','
         */
        if (value === CHAR_COMMA && depth > 0) {
            if (block.ranges > 0) {
                block.ranges = 0;
                let open = block.nodes.shift();
                block.nodes = [open, { type: 'text', value: stringify(block) }];
            }
            push({ type: 'comma', value });
            block.commas++;
            continue;
        }
        /**
         * Dot: '.'
         */
        if (value === CHAR_DOT && depth > 0 && block.commas === 0) {
            let siblings = block.nodes;
            if (depth === 0 || siblings.length === 0) {
                push({ type: 'text', value });
                continue;
            }
            if (prev.type === 'dot') {
                block.range = [];
                prev.value += value;
                prev.type = 'range';
                if (block.nodes.length !== 3 && block.nodes.length !== 5) {
                    block.invalid = true;
                    block.ranges = 0;
                    prev.type = 'text';
                    continue;
                }
                block.ranges++;
                block.args = [];
                continue;
            }
            if (prev.type === 'range') {
                siblings.pop();
                let before = siblings[siblings.length - 1];
                before.value += prev.value + value;
                prev = before;
                block.ranges--;
                continue;
            }
            push({ type: 'dot', value });
            continue;
        }
        /**
         * Text
         */
        push({ type: 'text', value });
    }
    // Mark imbalanced braces and brackets as invalid
    do {
        block = stack.pop();
        if (block.type !== 'root') {
            block.nodes.forEach(node => {
                if (!node.nodes) {
                    if (node.type === 'open')
                        node.isOpen = true;
                    if (node.type === 'close')
                        node.isClose = true;
                    if (!node.nodes)
                        node.type = 'text';
                    node.invalid = true;
                }
            });
            // get the location of the block on parent.nodes (block's siblings)
            let parent = stack[stack.length - 1];
            let index = parent.nodes.indexOf(block);
            // replace the (invalid) block with it's nodes
            parent.nodes.splice(index, 1, ...block.nodes);
        }
    } while (stack.length > 0);
    push({ type: 'eos' });
    return ast;
};
var parse_1 = parse;

/**
 * Expand the given pattern or create a regex-compatible string.
 *
 * ```js
 * const braces = require('braces');
 * console.log(braces('{a,b,c}', { compile: true })); //=> ['(a|b|c)']
 * console.log(braces('{a,b,c}')); //=> ['a', 'b', 'c']
 * ```
 * @param {String} `str`
 * @param {Object} `options`
 * @return {String}
 * @api public
 */
const braces = (input, options = {}) => {
    let output = [];
    if (Array.isArray(input)) {
        for (let pattern of input) {
            let result = braces.create(pattern, options);
            if (Array.isArray(result)) {
                output.push(...result);
            }
            else {
                output.push(result);
            }
        }
    }
    else {
        output = [].concat(braces.create(input, options));
    }
    if (options && options.expand === true && options.nodupes === true) {
        output = [...new Set(output)];
    }
    return output;
};
/**
 * Parse the given `str` with the given `options`.
 *
 * ```js
 * // braces.parse(pattern, [, options]);
 * const ast = braces.parse('a/{b,c}/d');
 * console.log(ast);
 * ```
 * @param {String} pattern Brace pattern to parse
 * @param {Object} options
 * @return {Object} Returns an AST
 * @api public
 */
braces.parse = (input, options = {}) => parse_1(input, options);
/**
 * Creates a braces string from an AST, or an AST node.
 *
 * ```js
 * const braces = require('braces');
 * let ast = braces.parse('foo/{a,b}/bar');
 * console.log(stringify(ast.nodes[2])); //=> '{a,b}'
 * ```
 * @param {String} `input` Brace pattern or AST.
 * @param {Object} `options`
 * @return {Array} Returns an array of expanded values.
 * @api public
 */
braces.stringify = (input, options = {}) => {
    if (typeof input === 'string') {
        return stringify(braces.parse(input, options), options);
    }
    return stringify(input, options);
};
/**
 * Compiles a brace pattern into a regex-compatible, optimized string.
 * This method is called by the main [braces](#braces) function by default.
 *
 * ```js
 * const braces = require('braces');
 * console.log(braces.compile('a/{b,c}/d'));
 * //=> ['a/(b|c)/d']
 * ```
 * @param {String} `input` Brace pattern or AST.
 * @param {Object} `options`
 * @return {Array} Returns an array of expanded values.
 * @api public
 */
braces.compile = (input, options = {}) => {
    if (typeof input === 'string') {
        input = braces.parse(input, options);
    }
    return compile_1(input, options);
};
/**
 * Expands a brace pattern into an array. This method is called by the
 * main [braces](#braces) function when `options.expand` is true. Before
 * using this method it's recommended that you read the [performance notes](#performance))
 * and advantages of using [.compile](#compile) instead.
 *
 * ```js
 * const braces = require('braces');
 * console.log(braces.expand('a/{b,c}/d'));
 * //=> ['a/b/d', 'a/c/d'];
 * ```
 * @param {String} `pattern` Brace pattern
 * @param {Object} `options`
 * @return {Array} Returns an array of expanded values.
 * @api public
 */
braces.expand = (input, options = {}) => {
    if (typeof input === 'string') {
        input = braces.parse(input, options);
    }
    let result = expand_1(input, options);
    // filter out empty strings if specified
    if (options.noempty === true) {
        result = result.filter(Boolean);
    }
    // filter out duplicates if specified
    if (options.nodupes === true) {
        result = [...new Set(result)];
    }
    return result;
};
/**
 * Processes a brace pattern and returns either an expanded array
 * (if `options.expand` is true), a highly optimized regex-compatible string.
 * This method is called by the main [braces](#braces) function.
 *
 * ```js
 * const braces = require('braces');
 * console.log(braces.create('user-{200..300}/project-{a,b,c}-{1..10}'))
 * //=> 'user-(20[0-9]|2[1-9][0-9]|300)/project-(a|b|c)-([1-9]|10)'
 * ```
 * @param {String} `pattern` Brace pattern
 * @param {Object} `options`
 * @return {Array} Returns an array of expanded values.
 * @api public
 */
braces.create = (input, options = {}) => {
    if (input === '' || input.length < 3) {
        return [input];
    }
    return options.expand !== true
        ? braces.compile(input, options)
        : braces.expand(input, options);
};
/**
 * Expose "braces"
 */
var braces_1 = braces;

const WIN_SLASH = '\\\\/';
const WIN_NO_SLASH = `[^${WIN_SLASH}]`;
/**
 * Posix glob regex
 */
const DOT_LITERAL = '\\.';
const PLUS_LITERAL = '\\+';
const QMARK_LITERAL = '\\?';
const SLASH_LITERAL = '\\/';
const ONE_CHAR = '(?=.)';
const QMARK = '[^/]';
const END_ANCHOR = `(?:${SLASH_LITERAL}|$)`;
const START_ANCHOR = `(?:^|${SLASH_LITERAL})`;
const DOTS_SLASH = `${DOT_LITERAL}{1,2}${END_ANCHOR}`;
const NO_DOT = `(?!${DOT_LITERAL})`;
const NO_DOTS = `(?!${START_ANCHOR}${DOTS_SLASH})`;
const NO_DOT_SLASH = `(?!${DOT_LITERAL}{0,1}${END_ANCHOR})`;
const NO_DOTS_SLASH = `(?!${DOTS_SLASH})`;
const QMARK_NO_DOT = `[^.${SLASH_LITERAL}]`;
const STAR = `${QMARK}*?`;
const POSIX_CHARS = {
    DOT_LITERAL,
    PLUS_LITERAL,
    QMARK_LITERAL,
    SLASH_LITERAL,
    ONE_CHAR,
    QMARK,
    END_ANCHOR,
    DOTS_SLASH,
    NO_DOT,
    NO_DOTS,
    NO_DOT_SLASH,
    NO_DOTS_SLASH,
    QMARK_NO_DOT,
    STAR,
    START_ANCHOR
};
/**
 * Windows glob regex
 */
const WINDOWS_CHARS = Object.assign(Object.assign({}, POSIX_CHARS), { SLASH_LITERAL: `[${WIN_SLASH}]`, QMARK: WIN_NO_SLASH, STAR: `${WIN_NO_SLASH}*?`, DOTS_SLASH: `${DOT_LITERAL}{1,2}(?:[${WIN_SLASH}]|$)`, NO_DOT: `(?!${DOT_LITERAL})`, NO_DOTS: `(?!(?:^|[${WIN_SLASH}])${DOT_LITERAL}{1,2}(?:[${WIN_SLASH}]|$))`, NO_DOT_SLASH: `(?!${DOT_LITERAL}{0,1}(?:[${WIN_SLASH}]|$))`, NO_DOTS_SLASH: `(?!${DOT_LITERAL}{1,2}(?:[${WIN_SLASH}]|$))`, QMARK_NO_DOT: `[^.${WIN_SLASH}]`, START_ANCHOR: `(?:^|[${WIN_SLASH}])`, END_ANCHOR: `(?:[${WIN_SLASH}]|$)` });
/**
 * POSIX Bracket Regex
 */
const POSIX_REGEX_SOURCE = {
    alnum: 'a-zA-Z0-9',
    alpha: 'a-zA-Z',
    ascii: '\\x00-\\x7F',
    blank: ' \\t',
    cntrl: '\\x00-\\x1F\\x7F',
    digit: '0-9',
    graph: '\\x21-\\x7E',
    lower: 'a-z',
    print: '\\x20-\\x7E ',
    punct: '\\-!"#$%&\'()\\*+,./:;<=>?@[\\]^_`{|}~',
    space: ' \\t\\r\\n\\v\\f',
    upper: 'A-Z',
    word: 'A-Za-z0-9_',
    xdigit: 'A-Fa-f0-9'
};
var constants$1 = {
    MAX_LENGTH: 1024 * 64,
    POSIX_REGEX_SOURCE,
    // regular expressions
    REGEX_BACKSLASH: /\\(?![*+?^${}(|)[\]])/g,
    REGEX_NON_SPECIAL_CHAR: /^[^@![\].,$*+?^{}()|\\/]+/,
    REGEX_SPECIAL_CHARS: /[-*+?.^${}(|)[\]]/,
    REGEX_SPECIAL_CHARS_BACKREF: /(\\?)((\W)(\3*))/g,
    REGEX_SPECIAL_CHARS_GLOBAL: /([-*+?.^${}(|)[\]])/g,
    REGEX_REMOVE_BACKSLASH: /(?:\[.*?[^\\]\]|\\(?=.))/g,
    // Replace globs with equivalent patterns to reduce parsing time.
    REPLACEMENTS: {
        '***': '*',
        '**/**': '**',
        '**/**/**': '**'
    },
    // Digits
    CHAR_0: 48,
    CHAR_9: 57,
    // Alphabet chars.
    CHAR_UPPERCASE_A: 65,
    CHAR_LOWERCASE_A: 97,
    CHAR_UPPERCASE_Z: 90,
    CHAR_LOWERCASE_Z: 122,
    CHAR_LEFT_PARENTHESES: 40,
    CHAR_RIGHT_PARENTHESES: 41,
    CHAR_ASTERISK: 42,
    // Non-alphabetic chars.
    CHAR_AMPERSAND: 38,
    CHAR_AT: 64,
    CHAR_BACKWARD_SLASH: 92,
    CHAR_CARRIAGE_RETURN: 13,
    CHAR_CIRCUMFLEX_ACCENT: 94,
    CHAR_COLON: 58,
    CHAR_COMMA: 44,
    CHAR_DOT: 46,
    CHAR_DOUBLE_QUOTE: 34,
    CHAR_EQUAL: 61,
    CHAR_EXCLAMATION_MARK: 33,
    CHAR_FORM_FEED: 12,
    CHAR_FORWARD_SLASH: 47,
    CHAR_GRAVE_ACCENT: 96,
    CHAR_HASH: 35,
    CHAR_HYPHEN_MINUS: 45,
    CHAR_LEFT_ANGLE_BRACKET: 60,
    CHAR_LEFT_CURLY_BRACE: 123,
    CHAR_LEFT_SQUARE_BRACKET: 91,
    CHAR_LINE_FEED: 10,
    CHAR_NO_BREAK_SPACE: 160,
    CHAR_PERCENT: 37,
    CHAR_PLUS: 43,
    CHAR_QUESTION_MARK: 63,
    CHAR_RIGHT_ANGLE_BRACKET: 62,
    CHAR_RIGHT_CURLY_BRACE: 125,
    CHAR_RIGHT_SQUARE_BRACKET: 93,
    CHAR_SEMICOLON: 59,
    CHAR_SINGLE_QUOTE: 39,
    CHAR_SPACE: 32,
    CHAR_TAB: 9,
    CHAR_UNDERSCORE: 95,
    CHAR_VERTICAL_LINE: 124,
    CHAR_ZERO_WIDTH_NOBREAK_SPACE: 65279,
    SEP: path.sep,
    /**
     * Create EXTGLOB_CHARS
     */
    extglobChars(chars) {
        return {
            '!': { type: 'negate', open: '(?:(?!(?:', close: `))${chars.STAR})` },
            '?': { type: 'qmark', open: '(?:', close: ')?' },
            '+': { type: 'plus', open: '(?:', close: ')+' },
            '*': { type: 'star', open: '(?:', close: ')*' },
            '@': { type: 'at', open: '(?:', close: ')' }
        };
    },
    /**
     * Create GLOB_CHARS
     */
    globChars(win32) {
        return win32 === true ? WINDOWS_CHARS : POSIX_CHARS;
    }
};

var utils$2 = createCommonjsModule(function (module, exports) {
    const win32 = process.platform === 'win32';
    const { REGEX_SPECIAL_CHARS, REGEX_SPECIAL_CHARS_GLOBAL, REGEX_REMOVE_BACKSLASH } = constants$1;
    exports.isObject = val => val !== null && typeof val === 'object' && !Array.isArray(val);
    exports.hasRegexChars = str => REGEX_SPECIAL_CHARS.test(str);
    exports.isRegexChar = str => str.length === 1 && exports.hasRegexChars(str);
    exports.escapeRegex = str => str.replace(REGEX_SPECIAL_CHARS_GLOBAL, '\\$1');
    exports.toPosixSlashes = str => str.replace(/\\/g, '/');
    exports.removeBackslashes = str => {
        return str.replace(REGEX_REMOVE_BACKSLASH, match => {
            return match === '\\' ? '' : match;
        });
    };
    exports.supportsLookbehinds = () => {
        let segs = process.version.slice(1).split('.');
        if (segs.length === 3 && +segs[0] >= 9 || (+segs[0] === 8 && +segs[1] >= 10)) {
            return true;
        }
        return false;
    };
    exports.isWindows = options => {
        if (options && typeof options.windows === 'boolean') {
            return options.windows;
        }
        return win32 === true || path.sep === '\\';
    };
    exports.escapeLast = (input, char, lastIdx) => {
        let idx = input.lastIndexOf(char, lastIdx);
        if (idx === -1)
            return input;
        if (input[idx - 1] === '\\')
            return exports.escapeLast(input, char, idx - 1);
        return input.slice(0, idx) + '\\' + input.slice(idx);
    };
});

const { CHAR_ASTERISK, /* * */ CHAR_AT, /* @ */ CHAR_BACKWARD_SLASH, /* \ */ CHAR_COMMA: CHAR_COMMA$1, /* , */ CHAR_DOT: CHAR_DOT$1, /* . */ CHAR_EXCLAMATION_MARK, /* ! */ CHAR_FORWARD_SLASH, /* / */ CHAR_LEFT_CURLY_BRACE: CHAR_LEFT_CURLY_BRACE$1, /* { */ CHAR_LEFT_PARENTHESES: CHAR_LEFT_PARENTHESES$1, /* ( */ CHAR_LEFT_SQUARE_BRACKET: CHAR_LEFT_SQUARE_BRACKET$1, /* [ */ CHAR_PLUS, /* + */ CHAR_QUESTION_MARK, /* ? */ CHAR_RIGHT_CURLY_BRACE: CHAR_RIGHT_CURLY_BRACE$1, /* } */ CHAR_RIGHT_PARENTHESES: CHAR_RIGHT_PARENTHESES$1, /* ) */ CHAR_RIGHT_SQUARE_BRACKET: CHAR_RIGHT_SQUARE_BRACKET$1 /* ] */ } = constants$1;
const isPathSeparator = code => {
    return code === CHAR_FORWARD_SLASH || code === CHAR_BACKWARD_SLASH;
};
/**
 * Quickly scans a glob pattern and returns an object with a handful of
 * useful properties, like `isGlob`, `path` (the leading non-glob, if it exists),
 * `glob` (the actual pattern), and `negated` (true if the path starts with `!`).
 *
 * ```js
 * const pm = require('picomatch');
 * console.log(pm.scan('foo/bar/*.js'));
 * { isGlob: true, input: 'foo/bar/*.js', base: 'foo/bar', glob: '*.js' }
 * ```
 * @param {String} `str`
 * @param {Object} `options`
 * @return {Object} Returns an object with tokens and regex source string.
 * @api public
 */
var scan = (input, options) => {
    let opts = options || {};
    let length = input.length - 1;
    let index = -1;
    let start = 0;
    let lastIndex = 0;
    let isGlob = false;
    let backslashes = false;
    let negated = false;
    let braces = 0;
    let prev;
    let code;
    let braceEscaped = false;
    let eos = () => index >= length;
    let advance = () => {
        prev = code;
        return input.charCodeAt(++index);
    };
    while (index < length) {
        code = advance();
        let next;
        if (code === CHAR_BACKWARD_SLASH) {
            backslashes = true;
            next = advance();
            if (next === CHAR_LEFT_CURLY_BRACE$1) {
                braceEscaped = true;
            }
            continue;
        }
        if (braceEscaped === true || code === CHAR_LEFT_CURLY_BRACE$1) {
            braces++;
            while (!eos() && (next = advance())) {
                if (next === CHAR_BACKWARD_SLASH) {
                    backslashes = true;
                    next = advance();
                    continue;
                }
                if (next === CHAR_LEFT_CURLY_BRACE$1) {
                    braces++;
                    continue;
                }
                if (!braceEscaped && next === CHAR_DOT$1 && (next = advance()) === CHAR_DOT$1) {
                    isGlob = true;
                    break;
                }
                if (!braceEscaped && next === CHAR_COMMA$1) {
                    isGlob = true;
                    break;
                }
                if (next === CHAR_RIGHT_CURLY_BRACE$1) {
                    braces--;
                    if (braces === 0) {
                        braceEscaped = false;
                        break;
                    }
                }
            }
        }
        if (code === CHAR_FORWARD_SLASH) {
            if (prev === CHAR_DOT$1 && index === (start + 1)) {
                start += 2;
                continue;
            }
            lastIndex = index + 1;
            continue;
        }
        if (code === CHAR_ASTERISK) {
            isGlob = true;
            break;
        }
        if (code === CHAR_ASTERISK || code === CHAR_QUESTION_MARK) {
            isGlob = true;
            break;
        }
        if (code === CHAR_LEFT_SQUARE_BRACKET$1) {
            while (!eos() && (next = advance())) {
                if (next === CHAR_BACKWARD_SLASH) {
                    backslashes = true;
                    next = advance();
                    continue;
                }
                if (next === CHAR_RIGHT_SQUARE_BRACKET$1) {
                    isGlob = true;
                    break;
                }
            }
        }
        let isExtglobChar = code === CHAR_PLUS
            || code === CHAR_AT
            || code === CHAR_EXCLAMATION_MARK;
        if (isExtglobChar && input.charCodeAt(index + 1) === CHAR_LEFT_PARENTHESES$1) {
            isGlob = true;
            break;
        }
        if (code === CHAR_EXCLAMATION_MARK && index === start) {
            negated = true;
            start++;
            continue;
        }
        if (code === CHAR_LEFT_PARENTHESES$1) {
            while (!eos() && (next = advance())) {
                if (next === CHAR_BACKWARD_SLASH) {
                    backslashes = true;
                    next = advance();
                    continue;
                }
                if (next === CHAR_RIGHT_PARENTHESES$1) {
                    isGlob = true;
                    break;
                }
            }
        }
        if (isGlob) {
            break;
        }
    }
    let prefix = '';
    let orig = input;
    let base = input;
    let glob = '';
    if (start > 0) {
        prefix = input.slice(0, start);
        input = input.slice(start);
        lastIndex -= start;
    }
    if (base && isGlob === true && lastIndex > 0) {
        base = input.slice(0, lastIndex);
        glob = input.slice(lastIndex);
    }
    else if (isGlob === true) {
        base = '';
        glob = input;
    }
    else {
        base = input;
    }
    if (base && base !== '' && base !== '/' && base !== input) {
        if (isPathSeparator(base.charCodeAt(base.length - 1))) {
            base = base.slice(0, -1);
        }
    }
    if (opts.unescape === true) {
        if (glob)
            glob = utils$2.removeBackslashes(glob);
        if (base && backslashes === true) {
            base = utils$2.removeBackslashes(base);
        }
    }
    return { prefix, input: orig, base, glob, negated, isGlob };
};

/**
 * Constants
 */
const { MAX_LENGTH: MAX_LENGTH$1, POSIX_REGEX_SOURCE: POSIX_REGEX_SOURCE$1, REGEX_NON_SPECIAL_CHAR, REGEX_SPECIAL_CHARS_BACKREF, REPLACEMENTS } = constants$1;
/**
 * Helpers
 */
const expandRange = (args, options) => {
    if (typeof options.expandRange === 'function') {
        return options.expandRange(...args, options);
    }
    args.sort();
    let value = `[${args.join('-')}]`;
    return value;
};
const negate = state => {
    let count = 1;
    while (state.peek() === '!' && (state.peek(2) !== '(' || state.peek(3) === '?')) {
        state.advance();
        state.start++;
        count++;
    }
    if (count % 2 === 0) {
        return false;
    }
    state.negated = true;
    state.start++;
    return true;
};
/**
 * Create the message for a syntax error
 */
const syntaxError = (type, char) => {
    return `Missing ${type}: "${char}" - use "\\\\${char}" to match literal characters`;
};
/**
 * Parse the given input string.
 * @param {String} input
 * @param {Object} options
 * @return {Object}
 */
const parse$1 = (input, options) => {
    if (typeof input !== 'string') {
        throw new TypeError('Expected a string');
    }
    input = REPLACEMENTS[input] || input;
    let opts = Object.assign({}, options);
    let max = typeof opts.maxLength === 'number' ? Math.min(MAX_LENGTH$1, opts.maxLength) : MAX_LENGTH$1;
    let len = input.length;
    if (len > max) {
        throw new SyntaxError(`Input length: ${len}, exceeds maximum allowed length: ${max}`);
    }
    let bos = { type: 'bos', value: '', output: opts.prepend || '' };
    let tokens = [bos];
    let capture = opts.capture ? '' : '?:';
    let win32 = utils$2.isWindows(options);
    // create constants based on platform, for windows or posix
    const PLATFORM_CHARS = constants$1.globChars(win32);
    const EXTGLOB_CHARS = constants$1.extglobChars(PLATFORM_CHARS);
    const { DOT_LITERAL, PLUS_LITERAL, SLASH_LITERAL, ONE_CHAR, DOTS_SLASH, NO_DOT, NO_DOT_SLASH, NO_DOTS_SLASH, QMARK, QMARK_NO_DOT, STAR, START_ANCHOR } = PLATFORM_CHARS;
    const globstar = (opts) => {
        return `(${capture}(?:(?!${START_ANCHOR}${opts.dot ? DOTS_SLASH : DOT_LITERAL}).)*?)`;
    };
    let nodot = opts.dot ? '' : NO_DOT;
    let star = opts.bash === true ? globstar(opts) : STAR;
    let qmarkNoDot = opts.dot ? QMARK : QMARK_NO_DOT;
    if (opts.capture) {
        star = `(${star})`;
    }
    // minimatch options support
    if (typeof opts.noext === 'boolean') {
        opts.noextglob = opts.noext;
    }
    let state = {
        index: -1,
        start: 0,
        consumed: '',
        output: '',
        backtrack: false,
        brackets: 0,
        braces: 0,
        parens: 0,
        quotes: 0,
        tokens
    };
    let extglobs = [];
    let stack = [];
    let prev = bos;
    let value;
    /**
     * Tokenizing helpers
     */
    const eos = () => state.index === len - 1;
    const peek = state.peek = (n = 1) => input[state.index + n];
    const advance = state.advance = () => input[++state.index];
    const append = token => {
        state.output += token.output != null ? token.output : token.value;
        state.consumed += token.value || '';
    };
    const increment = type => {
        state[type]++;
        stack.push(type);
    };
    const decrement = type => {
        state[type]--;
        stack.pop();
    };
    /**
     * Push tokens onto the tokens array. This helper speeds up
     * tokenizing by 1) helping us avoid backtracking as much as possible,
     * and 2) helping us avoid creating extra tokens when consecutive
     * characters are plain text. This improves performance and simplifies
     * lookbehinds.
     */
    const push = tok => {
        if (prev.type === 'globstar') {
            let isBrace = state.braces > 0 && (tok.type === 'comma' || tok.type === 'brace');
            let isExtglob = extglobs.length && (tok.type === 'pipe' || tok.type === 'paren');
            if (tok.type !== 'slash' && tok.type !== 'paren' && !isBrace && !isExtglob) {
                state.output = state.output.slice(0, -prev.output.length);
                prev.type = 'star';
                prev.value = '*';
                prev.output = star;
                state.output += prev.output;
            }
        }
        if (extglobs.length && tok.type !== 'paren' && !EXTGLOB_CHARS[tok.value]) {
            extglobs[extglobs.length - 1].inner += tok.value;
        }
        if (tok.value || tok.output)
            append(tok);
        if (prev && prev.type === 'text' && tok.type === 'text') {
            prev.value += tok.value;
            return;
        }
        tok.prev = prev;
        tokens.push(tok);
        prev = tok;
    };
    const extglobOpen = (type, value) => {
        let token = Object.assign(Object.assign({}, EXTGLOB_CHARS[value]), { conditions: 1, inner: '' });
        token.prev = prev;
        token.parens = state.parens;
        token.output = state.output;
        let output = (opts.capture ? '(' : '') + token.open;
        push({ type, value, output: state.output ? '' : ONE_CHAR });
        push({ type: 'paren', extglob: true, value: advance(), output });
        increment('parens');
        extglobs.push(token);
    };
    const extglobClose = token => {
        let output = token.close + (opts.capture ? ')' : '');
        if (token.type === 'negate') {
            let extglobStar = star;
            if (token.inner && token.inner.length > 1 && token.inner.includes('/')) {
                extglobStar = globstar(opts);
            }
            if (extglobStar !== star || eos() || /^\)+$/.test(input.slice(state.index + 1))) {
                output = token.close = ')$))' + extglobStar;
            }
            if (token.prev.type === 'bos' && eos()) {
                state.negatedExtglob = true;
            }
        }
        push({ type: 'paren', extglob: true, value, output });
        decrement('parens');
    };
    if (opts.fastpaths !== false && !/(^[*!]|[/{[()\]}"])/.test(input)) {
        let backslashes = false;
        let output = input.replace(REGEX_SPECIAL_CHARS_BACKREF, (m, esc, chars, first, rest, index) => {
            if (first === '\\') {
                backslashes = true;
                return m;
            }
            if (first === '?') {
                if (esc) {
                    return esc + first + (rest ? QMARK.repeat(rest.length) : '');
                }
                if (index === 0) {
                    return qmarkNoDot + (rest ? QMARK.repeat(rest.length) : '');
                }
                return QMARK.repeat(chars.length);
            }
            if (first === '.') {
                return DOT_LITERAL.repeat(chars.length);
            }
            if (first === '*') {
                if (esc) {
                    return esc + first + (rest ? star : '');
                }
                return star;
            }
            return esc ? m : '\\' + m;
        });
        if (backslashes === true) {
            if (opts.unescape === true) {
                output = output.replace(/\\/g, '');
            }
            else {
                output = output.replace(/\\+/g, m => {
                    return m.length % 2 === 0 ? '\\\\' : (m ? '\\' : '');
                });
            }
        }
        state.output = output;
        return state;
    }
    /**
     * Tokenize input until we reach end-of-string
     */
    while (!eos()) {
        value = advance();
        if (value === '\u0000') {
            continue;
        }
        /**
         * Escaped characters
         */
        if (value === '\\') {
            let next = peek();
            if (next === '/' && opts.bash !== true) {
                continue;
            }
            if (next === '.' || next === ';') {
                continue;
            }
            if (!next) {
                value += '\\';
                push({ type: 'text', value });
                continue;
            }
            // collapse slashes to reduce potential for exploits
            let match = /^\\+/.exec(input.slice(state.index + 1));
            let slashes = 0;
            if (match && match[0].length > 2) {
                slashes = match[0].length;
                state.index += slashes;
                if (slashes % 2 !== 0) {
                    value += '\\';
                }
            }
            if (opts.unescape === true) {
                value = advance() || '';
            }
            else {
                value += advance() || '';
            }
            if (state.brackets === 0) {
                push({ type: 'text', value });
                continue;
            }
        }
        /**
         * If we're inside a regex character class, continue
         * until we reach the closing bracket.
         */
        if (state.brackets > 0 && (value !== ']' || prev.value === '[' || prev.value === '[^')) {
            if (opts.posix !== false && value === ':') {
                let inner = prev.value.slice(1);
                if (inner.includes('[')) {
                    prev.posix = true;
                    if (inner.includes(':')) {
                        let idx = prev.value.lastIndexOf('[');
                        let pre = prev.value.slice(0, idx);
                        let rest = prev.value.slice(idx + 2);
                        let posix = POSIX_REGEX_SOURCE$1[rest];
                        if (posix) {
                            prev.value = pre + posix;
                            state.backtrack = true;
                            advance();
                            if (!bos.output && tokens.indexOf(prev) === 1) {
                                bos.output = ONE_CHAR;
                            }
                            continue;
                        }
                    }
                }
            }
            if ((value === '[' && peek() !== ':') || (value === '-' && peek() === ']')) {
                value = '\\' + value;
            }
            if (value === ']' && (prev.value === '[' || prev.value === '[^')) {
                value = '\\' + value;
            }
            if (opts.posix === true && value === '!' && prev.value === '[') {
                value = '^';
            }
            prev.value += value;
            append({ value });
            continue;
        }
        /**
         * If we're inside a quoted string, continue
         * until we reach the closing double quote.
         */
        if (state.quotes === 1 && value !== '"') {
            value = utils$2.escapeRegex(value);
            prev.value += value;
            append({ value });
            continue;
        }
        /**
         * Double quotes
         */
        if (value === '"') {
            state.quotes = state.quotes === 1 ? 0 : 1;
            if (opts.keepQuotes === true) {
                push({ type: 'text', value });
            }
            continue;
        }
        /**
         * Parentheses
         */
        if (value === '(') {
            push({ type: 'paren', value });
            increment('parens');
            continue;
        }
        if (value === ')') {
            if (state.parens === 0 && opts.strictBrackets === true) {
                throw new SyntaxError(syntaxError('opening', '('));
            }
            let extglob = extglobs[extglobs.length - 1];
            if (extglob && state.parens === extglob.parens + 1) {
                extglobClose(extglobs.pop());
                continue;
            }
            push({ type: 'paren', value, output: state.parens ? ')' : '\\)' });
            decrement('parens');
            continue;
        }
        /**
         * Brackets
         */
        if (value === '[') {
            if (opts.nobracket === true || !input.slice(state.index + 1).includes(']')) {
                if (opts.nobracket !== true && opts.strictBrackets === true) {
                    throw new SyntaxError(syntaxError('closing', ']'));
                }
                value = '\\' + value;
            }
            else {
                increment('brackets');
            }
            push({ type: 'bracket', value });
            continue;
        }
        if (value === ']') {
            if (opts.nobracket === true || (prev && prev.type === 'bracket' && prev.value.length === 1)) {
                push({ type: 'text', value, output: '\\' + value });
                continue;
            }
            if (state.brackets === 0) {
                if (opts.strictBrackets === true) {
                    throw new SyntaxError(syntaxError('opening', '['));
                }
                push({ type: 'text', value, output: '\\' + value });
                continue;
            }
            decrement('brackets');
            let prevValue = prev.value.slice(1);
            if (prev.posix !== true && prevValue[0] === '^' && !prevValue.includes('/')) {
                value = '/' + value;
            }
            prev.value += value;
            append({ value });
            // when literal brackets are explicitly disabled
            // assume we should match with a regex character class
            if (opts.literalBrackets === false || utils$2.hasRegexChars(prevValue)) {
                continue;
            }
            let escaped = utils$2.escapeRegex(prev.value);
            state.output = state.output.slice(0, -prev.value.length);
            // when literal brackets are explicitly enabled
            // assume we should escape the brackets to match literal characters
            if (opts.literalBrackets === true) {
                state.output += escaped;
                prev.value = escaped;
                continue;
            }
            // when the user specifies nothing, try to match both
            prev.value = `(${capture}${escaped}|${prev.value})`;
            state.output += prev.value;
            continue;
        }
        /**
         * Braces
         */
        if (value === '{' && opts.nobrace !== true) {
            push({ type: 'brace', value, output: '(' });
            increment('braces');
            continue;
        }
        if (value === '}') {
            if (opts.nobrace === true || state.braces === 0) {
                push({ type: 'text', value, output: '\\' + value });
                continue;
            }
            let output = ')';
            if (state.dots === true) {
                let arr = tokens.slice();
                let range = [];
                for (let i = arr.length - 1; i >= 0; i--) {
                    tokens.pop();
                    if (arr[i].type === 'brace') {
                        break;
                    }
                    if (arr[i].type !== 'dots') {
                        range.unshift(arr[i].value);
                    }
                }
                output = expandRange(range, opts);
                state.backtrack = true;
            }
            push({ type: 'brace', value, output });
            decrement('braces');
            continue;
        }
        /**
         * Pipes
         */
        if (value === '|') {
            if (extglobs.length > 0) {
                extglobs[extglobs.length - 1].conditions++;
            }
            push({ type: 'text', value });
            continue;
        }
        /**
         * Commas
         */
        if (value === ',') {
            let output = value;
            if (state.braces > 0 && stack[stack.length - 1] === 'braces') {
                output = '|';
            }
            push({ type: 'comma', value, output });
            continue;
        }
        /**
         * Slashes
         */
        if (value === '/') {
            // if the beginning of the glob is "./", advance the start
            // to the current index, and don't add the "./" characters
            // to the state. This greatly simplifies lookbehinds when
            // checking for BOS characters like "!" and "." (not "./")
            if (prev.type === 'dot' && state.index === 1) {
                state.start = state.index + 1;
                state.consumed = '';
                state.output = '';
                tokens.pop();
                prev = bos; // reset "prev" to the first token
                continue;
            }
            push({ type: 'slash', value, output: SLASH_LITERAL });
            continue;
        }
        /**
         * Dots
         */
        if (value === '.') {
            if (state.braces > 0 && prev.type === 'dot') {
                if (prev.value === '.')
                    prev.output = DOT_LITERAL;
                prev.type = 'dots';
                prev.output += value;
                prev.value += value;
                state.dots = true;
                continue;
            }
            push({ type: 'dot', value, output: DOT_LITERAL });
            continue;
        }
        /**
         * Question marks
         */
        if (value === '?') {
            if (prev && prev.type === 'paren') {
                let next = peek();
                let output = value;
                if (next === '<' && !utils$2.supportsLookbehinds()) {
                    throw new Error('Node.js v10 or higher is required for regex lookbehinds');
                }
                if (prev.value === '(' && !/[!=<:]/.test(next) || (next === '<' && !/[!=]/.test(peek(2)))) {
                    output = '\\' + value;
                }
                push({ type: 'text', value, output });
                continue;
            }
            if (opts.noextglob !== true && peek() === '(' && peek(2) !== '?') {
                extglobOpen('qmark', value);
                continue;
            }
            if (opts.dot !== true && (prev.type === 'slash' || prev.type === 'bos')) {
                push({ type: 'qmark', value, output: QMARK_NO_DOT });
                continue;
            }
            push({ type: 'qmark', value, output: QMARK });
            continue;
        }
        /**
         * Exclamation
         */
        if (value === '!') {
            if (opts.noextglob !== true && peek() === '(') {
                if (peek(2) !== '?' || !/[!=<:]/.test(peek(3))) {
                    extglobOpen('negate', value);
                    continue;
                }
            }
            if (opts.nonegate !== true && state.index === 0) {
                negate(state);
                continue;
            }
        }
        /**
         * Plus
         */
        if (value === '+') {
            if (opts.noextglob !== true && peek() === '(' && peek(2) !== '?') {
                extglobOpen('plus', value);
                continue;
            }
            if (prev && (prev.type === 'bracket' || prev.type === 'paren' || prev.type === 'brace')) {
                let output = prev.extglob === true ? '\\' + value : value;
                push({ type: 'plus', value, output });
                continue;
            }
            // use regex behavior inside parens
            if (state.parens > 0 && opts.regex !== false) {
                push({ type: 'plus', value });
                continue;
            }
            push({ type: 'plus', value: PLUS_LITERAL });
            continue;
        }
        /**
         * Plain text
         */
        if (value === '@') {
            if (opts.noextglob !== true && peek() === '(' && peek(2) !== '?') {
                push({ type: 'at', value, output: '' });
                continue;
            }
            push({ type: 'text', value });
            continue;
        }
        /**
         * Plain text
         */
        if (value !== '*') {
            if (value === '$' || value === '^') {
                value = '\\' + value;
            }
            let match = REGEX_NON_SPECIAL_CHAR.exec(input.slice(state.index + 1));
            if (match) {
                value += match[0];
                state.index += match[0].length;
            }
            push({ type: 'text', value });
            continue;
        }
        /**
         * Stars
         */
        if (prev && (prev.type === 'globstar' || prev.star === true)) {
            prev.type = 'star';
            prev.star = true;
            prev.value += value;
            prev.output = star;
            state.backtrack = true;
            state.consumed += value;
            continue;
        }
        if (opts.noextglob !== true && peek() === '(' && peek(2) !== '?') {
            extglobOpen('star', value);
            continue;
        }
        if (prev.type === 'star') {
            if (opts.noglobstar === true) {
                state.consumed += value;
                continue;
            }
            let prior = prev.prev;
            let before = prior.prev;
            let isStart = prior.type === 'slash' || prior.type === 'bos';
            let afterStar = before && (before.type === 'star' || before.type === 'globstar');
            if (opts.bash === true && (!isStart || (!eos() && peek() !== '/'))) {
                push({ type: 'star', value, output: '' });
                continue;
            }
            let isBrace = state.braces > 0 && (prior.type === 'comma' || prior.type === 'brace');
            let isExtglob = extglobs.length && (prior.type === 'pipe' || prior.type === 'paren');
            if (!isStart && prior.type !== 'paren' && !isBrace && !isExtglob) {
                push({ type: 'star', value, output: '' });
                continue;
            }
            // strip consecutive `/**/`
            while (input.slice(state.index + 1, state.index + 4) === '/**') {
                let after = input[state.index + 4];
                if (after && after !== '/') {
                    break;
                }
                state.consumed += '/**';
                state.index += 3;
            }
            if (prior.type === 'bos' && eos()) {
                prev.type = 'globstar';
                prev.value += value;
                prev.output = globstar(opts);
                state.output = prev.output;
                state.consumed += value;
                continue;
            }
            if (prior.type === 'slash' && prior.prev.type !== 'bos' && !afterStar && eos()) {
                state.output = state.output.slice(0, -(prior.output + prev.output).length);
                prior.output = '(?:' + prior.output;
                prev.type = 'globstar';
                prev.output = globstar(opts) + '|$)';
                prev.value += value;
                state.output += prior.output + prev.output;
                state.consumed += value;
                continue;
            }
            let next = peek();
            if (prior.type === 'slash' && prior.prev.type !== 'bos' && next === '/') {
                let end = peek(2) !== void 0 ? '|$' : '';
                state.output = state.output.slice(0, -(prior.output + prev.output).length);
                prior.output = '(?:' + prior.output;
                prev.type = 'globstar';
                prev.output = `${globstar(opts)}${SLASH_LITERAL}|${SLASH_LITERAL}${end})`;
                prev.value += value;
                state.output += prior.output + prev.output;
                state.consumed += value + advance();
                push({ type: 'slash', value, output: '' });
                continue;
            }
            if (prior.type === 'bos' && next === '/') {
                prev.type = 'globstar';
                prev.value += value;
                prev.output = `(?:^|${SLASH_LITERAL}|${globstar(opts)}${SLASH_LITERAL})`;
                state.output = prev.output;
                state.consumed += value + advance();
                push({ type: 'slash', value, output: '' });
                continue;
            }
            // remove single star from output
            state.output = state.output.slice(0, -prev.output.length);
            // reset previous token to globstar
            prev.type = 'globstar';
            prev.output = globstar(opts);
            prev.value += value;
            // reset output with globstar
            state.output += prev.output;
            state.consumed += value;
            continue;
        }
        let token = { type: 'star', value, output: star };
        if (opts.bash === true) {
            token.output = '.*?';
            if (prev.type === 'bos' || prev.type === 'slash') {
                token.output = nodot + token.output;
            }
            push(token);
            continue;
        }
        if (prev && (prev.type === 'bracket' || prev.type === 'paren') && opts.regex === true) {
            token.output = value;
            push(token);
            continue;
        }
        if (state.index === state.start || prev.type === 'slash' || prev.type === 'dot') {
            if (prev.type === 'dot') {
                state.output += NO_DOT_SLASH;
                prev.output += NO_DOT_SLASH;
            }
            else if (opts.dot === true) {
                state.output += NO_DOTS_SLASH;
                prev.output += NO_DOTS_SLASH;
            }
            else {
                state.output += nodot;
                prev.output += nodot;
            }
            if (peek() !== '*') {
                state.output += ONE_CHAR;
                prev.output += ONE_CHAR;
            }
        }
        push(token);
    }
    while (state.brackets > 0) {
        if (opts.strictBrackets === true)
            throw new SyntaxError(syntaxError('closing', ']'));
        state.output = utils$2.escapeLast(state.output, '[');
        decrement('brackets');
    }
    while (state.parens > 0) {
        if (opts.strictBrackets === true)
            throw new SyntaxError(syntaxError('closing', ')'));
        state.output = utils$2.escapeLast(state.output, '(');
        decrement('parens');
    }
    while (state.braces > 0) {
        if (opts.strictBrackets === true)
            throw new SyntaxError(syntaxError('closing', '}'));
        state.output = utils$2.escapeLast(state.output, '{');
        decrement('braces');
    }
    if (opts.strictSlashes !== true && (prev.type === 'star' || prev.type === 'bracket')) {
        push({ type: 'maybe_slash', value: '', output: `${SLASH_LITERAL}?` });
    }
    // rebuild the output if we had to backtrack at any point
    if (state.backtrack === true) {
        state.output = '';
        for (let token of state.tokens) {
            state.output += token.output != null ? token.output : token.value;
            if (token.suffix) {
                state.output += token.suffix;
            }
        }
    }
    return state;
};
/**
 * Fast paths for creating regular expressions for common glob patterns.
 * This can significantly speed up processing and has very little downside
 * impact when none of the fast paths match.
 */
parse$1.fastpaths = (input, options) => {
    let opts = Object.assign({}, options);
    let max = typeof opts.maxLength === 'number' ? Math.min(MAX_LENGTH$1, opts.maxLength) : MAX_LENGTH$1;
    let len = input.length;
    if (len > max) {
        throw new SyntaxError(`Input length: ${len}, exceeds maximum allowed length: ${max}`);
    }
    input = REPLACEMENTS[input] || input;
    let win32 = utils$2.isWindows(options);
    // create constants based on platform, for windows or posix
    const { DOT_LITERAL, SLASH_LITERAL, ONE_CHAR, DOTS_SLASH, NO_DOT, NO_DOTS, NO_DOTS_SLASH, STAR, START_ANCHOR } = constants$1.globChars(win32);
    let capture = opts.capture ? '' : '?:';
    let star = opts.bash === true ? '.*?' : STAR;
    let nodot = opts.dot ? NO_DOTS : NO_DOT;
    let slashDot = opts.dot ? NO_DOTS_SLASH : NO_DOT;
    if (opts.capture) {
        star = `(${star})`;
    }
    const globstar = (opts) => {
        return `(${capture}(?:(?!${START_ANCHOR}${opts.dot ? DOTS_SLASH : DOT_LITERAL}).)*?)`;
    };
    const create = str => {
        switch (str) {
            case '*':
                return `${nodot}${ONE_CHAR}${star}`;
            case '.*':
                return `${DOT_LITERAL}${ONE_CHAR}${star}`;
            case '*.*':
                return `${nodot}${star}${DOT_LITERAL}${ONE_CHAR}${star}`;
            case '*/*':
                return `${nodot}${star}${SLASH_LITERAL}${ONE_CHAR}${slashDot}${star}`;
            case '**':
                return nodot + globstar(opts);
            case '**/*':
                return `(?:${nodot}${globstar(opts)}${SLASH_LITERAL})?${slashDot}${ONE_CHAR}${star}`;
            case '**/*.*':
                return `(?:${nodot}${globstar(opts)}${SLASH_LITERAL})?${slashDot}${star}${DOT_LITERAL}${ONE_CHAR}${star}`;
            case '**/.*':
                return `(?:${nodot}${globstar(opts)}${SLASH_LITERAL})?${DOT_LITERAL}${ONE_CHAR}${star}`;
            default: {
                let match = /^(.*?)\.(\w+)$/.exec(str);
                if (!match)
                    return;
                let source = create(match[1]);
                if (!source)
                    return;
                return source + DOT_LITERAL + match[2];
            }
        }
    };
    let output = create(input);
    if (output && opts.strictSlashes !== true) {
        output += `${SLASH_LITERAL}?`;
    }
    return output;
};
var parse_1$1 = parse$1;

/**
 * Creates a matcher function from one or more glob patterns. The
 * returned function takes a string to match as its first argument,
 * and returns true if the string is a match. The returned matcher
 * function also takes a boolean as the second argument that, when true,
 * returns an object with additional information.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch(glob[, options]);
 *
 * const isMatch = picomatch('*.!(*a)');
 * console.log(isMatch('a.a')); //=> false
 * console.log(isMatch('a.b')); //=> true
 * ```
 * @name picomatch
 * @param {String|Array} `globs` One or more glob patterns.
 * @param {Object=} `options`
 * @return {Function=} Returns a matcher function.
 * @api public
 */
const picomatch = (glob, options, returnState = false) => {
    if (Array.isArray(glob)) {
        let fns = glob.map(input => picomatch(input, options, returnState));
        return str => {
            for (let isMatch of fns) {
                let state = isMatch(str);
                if (state)
                    return state;
            }
            return false;
        };
    }
    if (typeof glob !== 'string' || glob === '') {
        throw new TypeError('Expected pattern to be a non-empty string');
    }
    let opts = options || {};
    let posix = utils$2.isWindows(options);
    let regex = picomatch.makeRe(glob, options, false, true);
    let state = regex.state;
    delete regex.state;
    let isIgnored = () => false;
    if (opts.ignore) {
        let ignoreOpts = Object.assign(Object.assign({}, options), { ignore: null, onMatch: null, onResult: null });
        isIgnored = picomatch(opts.ignore, ignoreOpts, returnState);
    }
    const matcher = (input, returnObject = false) => {
        let { isMatch, match, output } = picomatch.test(input, regex, options, { glob, posix });
        let result = { glob, state, regex, posix, input, output, match, isMatch };
        if (typeof opts.onResult === 'function') {
            opts.onResult(result);
        }
        if (isMatch === false) {
            result.isMatch = false;
            return returnObject ? result : false;
        }
        if (isIgnored(input)) {
            if (typeof opts.onIgnore === 'function') {
                opts.onIgnore(result);
            }
            result.isMatch = false;
            return returnObject ? result : false;
        }
        if (typeof opts.onMatch === 'function') {
            opts.onMatch(result);
        }
        return returnObject ? result : true;
    };
    if (returnState) {
        matcher.state = state;
    }
    return matcher;
};
/**
 * Test `input` with the given `regex`. This is used by the main
 * `picomatch()` function to test the input string.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch.test(input, regex[, options]);
 *
 * console.log(picomatch.test('foo/bar', /^(?:([^/]*?)\/([^/]*?))$/));
 * // { isMatch: true, match: [ 'foo/', 'foo', 'bar' ], output: 'foo/bar' }
 * ```
 * @param {String} `input` String to test.
 * @param {RegExp} `regex`
 * @return {Object} Returns an object with matching info.
 * @api public
 */
picomatch.test = (input, regex, options, { glob, posix } = {}) => {
    if (typeof input !== 'string') {
        throw new TypeError('Expected input to be a string');
    }
    if (input === '') {
        return { isMatch: false, output: '' };
    }
    let opts = options || {};
    let format = opts.format || (posix ? utils$2.toPosixSlashes : null);
    let match = input === glob;
    let output = (match && format) ? format(input) : input;
    if (match === false) {
        output = format ? format(input) : input;
        match = output === glob;
    }
    if (match === false || opts.capture === true) {
        if (opts.matchBase === true || opts.basename === true) {
            match = picomatch.matchBase(input, regex, options, posix);
        }
        else {
            match = regex.exec(output);
        }
    }
    return { isMatch: !!match, match, output };
};
/**
 * Match the basename of a filepath.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch.matchBase(input, glob[, options]);
 * console.log(picomatch.matchBase('foo/bar.js', '*.js'); // true
 * ```
 * @param {String} `input` String to test.
 * @param {RegExp|String} `glob` Glob pattern or regex created by [.makeRe](#makeRe).
 * @return {Boolean}
 * @api public
 */
picomatch.matchBase = (input, glob, options, posix = utils$2.isWindows(options)) => {
    let regex = glob instanceof RegExp ? glob : picomatch.makeRe(glob, options);
    return regex.test(path.basename(input));
};
/**
 * Returns true if **any** of the given glob `patterns` match the specified `string`.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch.isMatch(string, patterns[, options]);
 *
 * console.log(picomatch.isMatch('a.a', ['b.*', '*.a'])); //=> true
 * console.log(picomatch.isMatch('a.a', 'b.*')); //=> false
 * ```
 * @param {String|Array} str The string to test.
 * @param {String|Array} patterns One or more glob patterns to use for matching.
 * @param {Object} [options] See available [options](#options).
 * @return {Boolean} Returns true if any patterns match `str`
 * @api public
 */
picomatch.isMatch = (str, patterns, options) => picomatch(patterns, options)(str);
/**
 * Parse a glob pattern to create the source string for a regular
 * expression.
 *
 * ```js
 * const picomatch = require('picomatch');
 * const result = picomatch.parse(glob[, options]);
 * ```
 * @param {String} `glob`
 * @param {Object} `options`
 * @return {Object} Returns an object with useful properties and output to be used as a regex source string.
 * @api public
 */
picomatch.parse = (glob, options) => parse_1$1(glob, options);
/**
 * Scan a glob pattern to separate the pattern into segments.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch.scan(input[, options]);
 *
 * const result = picomatch.scan('!./foo/*.js');
 * console.log(result);
 * // { prefix: '!./',
 * //   input: '!./foo/*.js',
 * //   base: 'foo',
 * //   glob: '*.js',
 * //   negated: true,
 * //   isGlob: true }
 * ```
 * @param {String} `input` Glob pattern to scan.
 * @param {Object} `options`
 * @return {Object} Returns an object with
 * @api public
 */
picomatch.scan = (input, options) => scan(input, options);
/**
 * Create a regular expression from a glob pattern.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch.makeRe(input[, options]);
 *
 * console.log(picomatch.makeRe('*.js'));
 * //=> /^(?:(?!\.)(?=.)[^/]*?\.js)$/
 * ```
 * @param {String} `input` A glob pattern to convert to regex.
 * @param {Object} `options`
 * @return {RegExp} Returns a regex created from the given pattern.
 * @api public
 */
picomatch.makeRe = (input, options, returnOutput = false, returnState = false) => {
    if (!input || typeof input !== 'string') {
        throw new TypeError('Expected a non-empty string');
    }
    let opts = options || {};
    let prepend = opts.contains ? '' : '^';
    let append = opts.contains ? '' : '$';
    let state = { negated: false, fastpaths: true };
    let prefix = '';
    let output;
    if (input.startsWith('./')) {
        input = input.slice(2);
        prefix = state.prefix = './';
    }
    if (opts.fastpaths !== false && (input[0] === '.' || input[0] === '*')) {
        output = parse_1$1.fastpaths(input, options);
    }
    if (output === void 0) {
        state = picomatch.parse(input, options);
        state.prefix = prefix + (state.prefix || '');
        output = state.output;
    }
    if (returnOutput === true) {
        return output;
    }
    let source = `${prepend}(?:${output})${append}`;
    if (state && state.negated === true) {
        source = `^(?!${source}).*$`;
    }
    let regex = picomatch.toRegex(source, options);
    if (returnState === true) {
        regex.state = state;
    }
    return regex;
};
/**
 * Create a regular expression from the given regex source string.
 *
 * ```js
 * const picomatch = require('picomatch');
 * // picomatch.toRegex(source[, options]);
 *
 * const { output } = picomatch.parse('*.js');
 * console.log(picomatch.toRegex(output));
 * //=> /^(?:(?!\.)(?=.)[^/]*?\.js)$/
 * ```
 * @param {String} `source` Regular expression source string.
 * @param {Object} `options`
 * @return {RegExp}
 * @api public
 */
picomatch.toRegex = (source, options) => {
    try {
        let opts = options || {};
        return new RegExp(source, opts.flags || (opts.nocase ? 'i' : ''));
    }
    catch (err) {
        if (options && options.debug === true)
            throw err;
        return /$^/;
    }
};
/**
 * Picomatch constants.
 * @return {Object}
 */
picomatch.constants = constants$1;
/**
 * Expose "picomatch"
 */
var picomatch_1 = picomatch;

var picomatch$1 = picomatch_1;

const isEmptyString = val => typeof val === 'string' && (val === '' || val === './');
/**
 * Returns an array of strings that match one or more glob patterns.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm(list, patterns[, options]);
 *
 * console.log(mm(['a.js', 'a.txt'], ['*.js']));
 * //=> [ 'a.js' ]
 * ```
 * @param {String|Array<string>} list List of strings to match.
 * @param {String|Array<string>} patterns One or more glob patterns to use for matching.
 * @param {Object} options See available [options](#options)
 * @return {Array} Returns an array of matches
 * @summary false
 * @api public
 */
const micromatch = (list, patterns, options) => {
    patterns = [].concat(patterns);
    list = [].concat(list);
    let omit = new Set();
    let keep = new Set();
    let items = new Set();
    let negatives = 0;
    let onResult = state => {
        items.add(state.output);
        if (options && options.onResult) {
            options.onResult(state);
        }
    };
    for (let i = 0; i < patterns.length; i++) {
        let isMatch = picomatch$1(String(patterns[i]), Object.assign(Object.assign({}, options), { onResult }), true);
        let negated = isMatch.state.negated || isMatch.state.negatedExtglob;
        if (negated)
            negatives++;
        for (let item of list) {
            let matched = isMatch(item, true);
            let match = negated ? !matched.isMatch : matched.isMatch;
            if (!match)
                continue;
            if (negated) {
                omit.add(matched.output);
            }
            else {
                omit.delete(matched.output);
                keep.add(matched.output);
            }
        }
    }
    let result = negatives === patterns.length ? [...items] : [...keep];
    let matches = result.filter(item => !omit.has(item));
    if (options && matches.length === 0) {
        if (options.failglob === true) {
            throw new Error(`No matches found for "${patterns.join(', ')}"`);
        }
        if (options.nonull === true || options.nullglob === true) {
            return options.unescape ? patterns.map(p => p.replace(/\\/g, '')) : patterns;
        }
    }
    return matches;
};
/**
 * Backwards compatibility
 */
micromatch.match = micromatch;
/**
 * Returns a matcher function from the given glob `pattern` and `options`.
 * The returned function takes a string to match as its only argument and returns
 * true if the string is a match.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.matcher(pattern[, options]);
 *
 * const isMatch = mm.matcher('*.!(*a)');
 * console.log(isMatch('a.a')); //=> false
 * console.log(isMatch('a.b')); //=> true
 * ```
 * @param {String} `pattern` Glob pattern
 * @param {Object} `options`
 * @return {Function} Returns a matcher function.
 * @api public
 */
micromatch.matcher = (pattern, options) => picomatch$1(pattern, options);
/**
 * Returns true if **any** of the given glob `patterns` match the specified `string`.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.isMatch(string, patterns[, options]);
 *
 * console.log(mm.isMatch('a.a', ['b.*', '*.a'])); //=> true
 * console.log(mm.isMatch('a.a', 'b.*')); //=> false
 * ```
 * @param {String} str The string to test.
 * @param {String|Array} patterns One or more glob patterns to use for matching.
 * @param {Object} [options] See available [options](#options).
 * @return {Boolean} Returns true if any patterns match `str`
 * @api public
 */
micromatch.isMatch = (str, patterns, options) => picomatch$1(patterns, options)(str);
/**
 * Backwards compatibility
 */
micromatch.any = micromatch.isMatch;
/**
 * Returns a list of strings that _**do not match any**_ of the given `patterns`.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.not(list, patterns[, options]);
 *
 * console.log(mm.not(['a.a', 'b.b', 'c.c'], '*.a'));
 * //=> ['b.b', 'c.c']
 * ```
 * @param {Array} `list` Array of strings to match.
 * @param {String|Array} `patterns` One or more glob pattern to use for matching.
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Array} Returns an array of strings that **do not match** the given patterns.
 * @api public
 */
micromatch.not = (list, patterns, options = {}) => {
    patterns = [].concat(patterns).map(String);
    let result = new Set();
    let items = [];
    let onResult = state => {
        if (options.onResult)
            options.onResult(state);
        items.push(state.output);
    };
    let matches = micromatch(list, patterns, Object.assign(Object.assign({}, options), { onResult }));
    for (let item of items) {
        if (!matches.includes(item)) {
            result.add(item);
        }
    }
    return [...result];
};
/**
 * Returns true if the given `string` contains the given pattern. Similar
 * to [.isMatch](#isMatch) but the pattern can match any part of the string.
 *
 * ```js
 * var mm = require('micromatch');
 * // mm.contains(string, pattern[, options]);
 *
 * console.log(mm.contains('aa/bb/cc', '*b'));
 * //=> true
 * console.log(mm.contains('aa/bb/cc', '*d'));
 * //=> false
 * ```
 * @param {String} `str` The string to match.
 * @param {String|Array} `patterns` Glob pattern to use for matching.
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Boolean} Returns true if the patter matches any part of `str`.
 * @api public
 */
micromatch.contains = (str, pattern, options) => {
    if (typeof str !== 'string') {
        throw new TypeError(`Expected a string: "${util.inspect(str)}"`);
    }
    if (Array.isArray(pattern)) {
        return pattern.some(p => micromatch.contains(str, p, options));
    }
    if (typeof pattern === 'string') {
        if (isEmptyString(str) || isEmptyString(pattern)) {
            return false;
        }
        if (str.includes(pattern) || (str.startsWith('./') && str.slice(2).includes(pattern))) {
            return true;
        }
    }
    return micromatch.isMatch(str, pattern, Object.assign(Object.assign({}, options), { contains: true }));
};
/**
 * Filter the keys of the given object with the given `glob` pattern
 * and `options`. Does not attempt to match nested keys. If you need this feature,
 * use [glob-object][] instead.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.matchKeys(object, patterns[, options]);
 *
 * const obj = { aa: 'a', ab: 'b', ac: 'c' };
 * console.log(mm.matchKeys(obj, '*b'));
 * //=> { ab: 'b' }
 * ```
 * @param {Object} `object` The object with keys to filter.
 * @param {String|Array} `patterns` One or more glob patterns to use for matching.
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Object} Returns an object with only keys that match the given patterns.
 * @api public
 */
micromatch.matchKeys = (obj, patterns, options) => {
    if (!utils$2.isObject(obj)) {
        throw new TypeError('Expected the first argument to be an object');
    }
    let keys = micromatch(Object.keys(obj), patterns, options);
    let res = {};
    for (let key of keys)
        res[key] = obj[key];
    return res;
};
/**
 * Returns true if some of the strings in the given `list` match any of the given glob `patterns`.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.some(list, patterns[, options]);
 *
 * console.log(mm.some(['foo.js', 'bar.js'], ['*.js', '!foo.js']));
 * // true
 * console.log(mm.some(['foo.js'], ['*.js', '!foo.js']));
 * // false
 * ```
 * @param {String|Array} `list` The string or array of strings to test. Returns as soon as the first match is found.
 * @param {String|Array} `patterns` One or more glob patterns to use for matching.
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Boolean} Returns true if any patterns match `str`
 * @api public
 */
micromatch.some = (list, patterns, options) => {
    let items = [].concat(list);
    for (let pattern of [].concat(patterns)) {
        let isMatch = picomatch$1(String(pattern), options);
        if (items.some(item => isMatch(item))) {
            return true;
        }
    }
    return false;
};
/**
 * Returns true if every string in the given `list` matches
 * any of the given glob `patterns`.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.every(list, patterns[, options]);
 *
 * console.log(mm.every('foo.js', ['foo.js']));
 * // true
 * console.log(mm.every(['foo.js', 'bar.js'], ['*.js']));
 * // true
 * console.log(mm.every(['foo.js', 'bar.js'], ['*.js', '!foo.js']));
 * // false
 * console.log(mm.every(['foo.js'], ['*.js', '!foo.js']));
 * // false
 * ```
 * @param {String|Array} `list` The string or array of strings to test.
 * @param {String|Array} `patterns` One or more glob patterns to use for matching.
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Boolean} Returns true if any patterns match `str`
 * @api public
 */
micromatch.every = (list, patterns, options) => {
    let items = [].concat(list);
    for (let pattern of [].concat(patterns)) {
        let isMatch = picomatch$1(String(pattern), options);
        if (!items.every(item => isMatch(item))) {
            return false;
        }
    }
    return true;
};
/**
 * Returns true if **all** of the given `patterns` match
 * the specified string.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.all(string, patterns[, options]);
 *
 * console.log(mm.all('foo.js', ['foo.js']));
 * // true
 *
 * console.log(mm.all('foo.js', ['*.js', '!foo.js']));
 * // false
 *
 * console.log(mm.all('foo.js', ['*.js', 'foo.js']));
 * // true
 *
 * console.log(mm.all('foo.js', ['*.js', 'f*', '*o*', '*o.js']));
 * // true
 * ```
 * @param {String|Array} `str` The string to test.
 * @param {String|Array} `patterns` One or more glob patterns to use for matching.
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Boolean} Returns true if any patterns match `str`
 * @api public
 */
micromatch.all = (str, patterns, options) => {
    if (typeof str !== 'string') {
        throw new TypeError(`Expected a string: "${util.inspect(str)}"`);
    }
    return [].concat(patterns).every(p => picomatch$1(p, options)(str));
};
/**
 * Returns an array of matches captured by `pattern` in `string, or `null` if the pattern did not match.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.capture(pattern, string[, options]);
 *
 * console.log(mm.capture('test/*.js', 'test/foo.js'));
 * //=> ['foo']
 * console.log(mm.capture('test/*.js', 'foo/bar.css'));
 * //=> null
 * ```
 * @param {String} `glob` Glob pattern to use for matching.
 * @param {String} `input` String to match
 * @param {Object} `options` See available [options](#options) for changing how matches are performed
 * @return {Boolean} Returns an array of captures if the input matches the glob pattern, otherwise `null`.
 * @api public
 */
micromatch.capture = (glob, input, options) => {
    let posix = utils$2.isWindows(options);
    let regex = picomatch$1.makeRe(String(glob), Object.assign(Object.assign({}, options), { capture: true }));
    let match = regex.exec(posix ? utils$2.toPosixSlashes(input) : input);
    if (match) {
        return match.slice(1).map(v => v === void 0 ? '' : v);
    }
};
/**
 * Create a regular expression from the given glob `pattern`.
 *
 * ```js
 * const mm = require('micromatch');
 * // mm.makeRe(pattern[, options]);
 *
 * console.log(mm.makeRe('*.js'));
 * //=> /^(?:(\.[\\\/])?(?!\.)(?=.)[^\/]*?\.js)$/
 * ```
 * @param {String} `pattern` A glob pattern to convert to regex.
 * @param {Object} `options`
 * @return {RegExp} Returns a regex created from the given pattern.
 * @api public
 */
micromatch.makeRe = (...args) => picomatch$1.makeRe(...args);
/**
 * Scan a glob pattern to separate the pattern into segments. Used
 * by the [split](#split) method.
 *
 * ```js
 * const mm = require('micromatch');
 * const state = mm.scan(pattern[, options]);
 * ```
 * @param {String} `pattern`
 * @param {Object} `options`
 * @return {Object} Returns an object with
 * @api public
 */
micromatch.scan = (...args) => picomatch$1.scan(...args);
/**
 * Parse a glob pattern to create the source string for a regular
 * expression.
 *
 * ```js
 * const mm = require('micromatch');
 * const state = mm(pattern[, options]);
 * ```
 * @param {String} `glob`
 * @param {Object} `options`
 * @return {Object} Returns an object with useful properties and output to be used as regex source string.
 * @api public
 */
micromatch.parse = (patterns, options) => {
    let res = [];
    for (let pattern of [].concat(patterns || [])) {
        for (let str of braces_1(String(pattern), options)) {
            res.push(picomatch$1.parse(str, options));
        }
    }
    return res;
};
/**
 * Process the given brace `pattern`.
 *
 * ```js
 * const { braces } = require('micromatch');
 * console.log(braces('foo/{a,b,c}/bar'));
 * //=> [ 'foo/(a|b|c)/bar' ]
 *
 * console.log(braces('foo/{a,b,c}/bar', { expand: true }));
 * //=> [ 'foo/a/bar', 'foo/b/bar', 'foo/c/bar' ]
 * ```
 * @param {String} `pattern` String with brace pattern to process.
 * @param {Object} `options` Any [options](#options) to change how expansion is performed. See the [braces][] library for all available options.
 * @return {Array}
 * @api public
 */
micromatch.braces = (pattern, options) => {
    if (typeof pattern !== 'string')
        throw new TypeError('Expected a string');
    if ((options && options.nobrace === true) || !/\{.*\}/.test(pattern)) {
        return [pattern];
    }
    return braces_1(pattern, options);
};
/**
 * Expand braces
 */
micromatch.braceExpand = (pattern, options) => {
    if (typeof pattern !== 'string')
        throw new TypeError('Expected a string');
    return micromatch.braces(pattern, Object.assign(Object.assign({}, options), { expand: true }));
};
/**
 * Expose micromatch
 */
var micromatch_1 = micromatch;

function ensureArray$1(thing) {
    if (Array.isArray(thing))
        return thing;
    if (thing == undefined)
        return [];
    return [thing];
}

function getMatcherString(id, resolutionBase) {
    if (resolutionBase === false) {
        return id;
    }
    return resolve(...(typeof resolutionBase === 'string' ? [resolutionBase, id] : [id]));
}
const createFilter = function createFilter(include, exclude, options) {
    const resolutionBase = options && options.resolve;
    const getMatcher = (id) => {
        return id instanceof RegExp
            ? id
            : {
                test: micromatch_1.matcher(getMatcherString(id, resolutionBase)
                    .split(sep)
                    .join('/'), { dot: true })
            };
    };
    const includeMatchers = ensureArray$1(include).map(getMatcher);
    const excludeMatchers = ensureArray$1(exclude).map(getMatcher);
    return function (id) {
        if (typeof id !== 'string')
            return false;
        if (/\0/.test(id))
            return false;
        id = id.split(sep).join('/');
        for (let i = 0; i < excludeMatchers.length; ++i) {
            const matcher = excludeMatchers[i];
            if (matcher.test(id))
                return false;
        }
        for (let i = 0; i < includeMatchers.length; ++i) {
            const matcher = includeMatchers[i];
            if (matcher.test(id))
                return true;
        }
        return !includeMatchers.length;
    };
};

var modules = {};
var getModule = function (dir) {
    var rootPath = dir ? path.resolve(dir) : process.cwd();
    var rootName = path.join(rootPath, '@root');
    var root = modules[rootName];
    if (!root) {
        root = new module(rootName);
        root.filename = rootName;
        root.paths = module._nodeModulePaths(rootPath);
        modules[rootName] = root;
    }
    return root;
};
var requireRelative = function (requested, relativeTo) {
    var root = getModule(relativeTo);
    return root.require(requested);
};
requireRelative.resolve = function (requested, relativeTo) {
    var root = getModule(relativeTo);
    return module._resolveFilename(requested, root);
};
var requireRelative_1 = requireRelative;

let chokidar;
try {
    chokidar = requireRelative_1('chokidar', process.cwd());
}
catch (err) {
    chokidar = null;
}
var chokidar$1 = chokidar;

const opts = { encoding: 'utf-8', persistent: true };
const watchers = new Map();
function addTask(id, task, chokidarOptions, chokidarOptionsHash, isTransformDependency) {
    if (!watchers.has(chokidarOptionsHash))
        watchers.set(chokidarOptionsHash, new Map());
    const group = watchers.get(chokidarOptionsHash);
    const watcher = group.get(id) || new FileWatcher(id, chokidarOptions, group);
    if (!watcher.fsWatcher) {
        if (isTransformDependency)
            throw new Error(`Transform dependency ${id} does not exist.`);
    }
    else {
        watcher.addTask(task, isTransformDependency);
    }
}
function deleteTask(id, target, chokidarOptionsHash) {
    const group = watchers.get(chokidarOptionsHash);
    const watcher = group.get(id);
    if (watcher)
        watcher.deleteTask(target, group);
}
class FileWatcher {
    constructor(id, chokidarOptions, group) {
        this.id = id;
        this.tasks = new Set();
        this.transformDependencyTasks = new Set();
        let modifiedTime;
        try {
            const stats = statSync(id);
            modifiedTime = +stats.mtime;
        }
        catch (err) {
            if (err.code === 'ENOENT') {
                // can't watch files that don't exist (e.g. injected
                // by plugins somehow)
                return;
            }
            throw err;
        }
        const handleWatchEvent = (event) => {
            if (event === 'rename' || event === 'unlink') {
                this.close();
                group.delete(id);
                this.trigger(id);
                return;
            }
            else {
                let stats;
                try {
                    stats = statSync(id);
                }
                catch (err) {
                    if (err.code === 'ENOENT') {
                        modifiedTime = -1;
                        this.trigger(id);
                        return;
                    }
                    throw err;
                }
                // debounce
                if (+stats.mtime - modifiedTime > 15)
                    this.trigger(id);
            }
        };
        this.fsWatcher = chokidarOptions
            ? chokidar$1.watch(id, chokidarOptions).on('all', handleWatchEvent)
            : watch$1(id, opts, handleWatchEvent);
        group.set(id, this);
    }
    addTask(task, isTransformDependency) {
        if (isTransformDependency)
            this.transformDependencyTasks.add(task);
        else
            this.tasks.add(task);
    }
    close() {
        if (this.fsWatcher)
            this.fsWatcher.close();
    }
    deleteTask(task, group) {
        let deleted = this.tasks.delete(task);
        deleted = this.transformDependencyTasks.delete(task) || deleted;
        if (deleted && this.tasks.size === 0 && this.transformDependencyTasks.size === 0) {
            group.delete(this.id);
            this.close();
        }
    }
    trigger(id) {
        for (const task of this.tasks) {
            task.invalidate(id, false);
        }
        for (const task of this.transformDependencyTasks) {
            task.invalidate(id, true);
        }
    }
}

const DELAY = 200;
class Watcher {
    constructor(configs) {
        this.buildTimeout = null;
        this.invalidatedIds = new Set();
        this.rerun = false;
        this.emitter = new (class extends EventEmitter {
            constructor(close) {
                super();
                this.close = close;
                // Allows more than 10 bundles to be watched without
                // showing the `MaxListenersExceededWarning` to the user.
                this.setMaxListeners(Infinity);
            }
        })(this.close.bind(this));
        this.tasks = (Array.isArray(configs) ? configs : configs ? [configs] : []).map(config => new Task(this, config));
        this.running = true;
        process.nextTick(() => this.run());
    }
    close() {
        if (this.buildTimeout)
            clearTimeout(this.buildTimeout);
        for (const task of this.tasks) {
            task.close();
        }
        this.emitter.removeAllListeners();
    }
    emit(event, value) {
        this.emitter.emit(event, value);
    }
    invalidate(id) {
        if (id) {
            this.invalidatedIds.add(id);
        }
        if (this.running) {
            this.rerun = true;
            return;
        }
        if (this.buildTimeout)
            clearTimeout(this.buildTimeout);
        this.buildTimeout = setTimeout(() => {
            this.buildTimeout = null;
            for (const id of this.invalidatedIds) {
                this.emit('change', id);
            }
            this.invalidatedIds.clear();
            this.emit('restart');
            this.run();
        }, DELAY);
    }
    run() {
        this.running = true;
        this.emit('event', {
            code: 'START'
        });
        let taskPromise = Promise.resolve();
        for (const task of this.tasks)
            taskPromise = taskPromise.then(() => task.run());
        return taskPromise
            .then(() => {
            this.running = false;
            this.emit('event', {
                code: 'END'
            });
        })
            .catch(error => {
            this.running = false;
            this.emit('event', {
                code: 'ERROR',
                error
            });
        })
            .then(() => {
            if (this.rerun) {
                this.rerun = false;
                this.invalidate();
            }
        });
    }
}
class Task {
    constructor(watcher, config) {
        this.cache = { modules: [] };
        this.watchFiles = [];
        this.invalidated = true;
        this.watcher = watcher;
        this.closed = false;
        this.watched = new Set();
        const { inputOptions, outputOptions } = mergeOptions({
            config
        });
        this.inputOptions = inputOptions;
        this.outputs = outputOptions;
        this.outputFiles = this.outputs.map(output => {
            if (output.file || output.dir)
                return path.resolve(output.file || output.dir);
            return undefined;
        });
        const watchOptions = inputOptions.watch || {};
        if ('useChokidar' in watchOptions)
            watchOptions.chokidar = watchOptions.useChokidar;
        let chokidarOptions = 'chokidar' in watchOptions ? watchOptions.chokidar : !!chokidar$1;
        if (chokidarOptions) {
            chokidarOptions = Object.assign(Object.assign({}, (chokidarOptions === true ? {} : chokidarOptions)), { disableGlobbing: true, ignoreInitial: true });
        }
        if (chokidarOptions && !chokidar$1) {
            throw new Error(`watch.chokidar was provided, but chokidar could not be found. Have you installed it?`);
        }
        this.chokidarOptions = chokidarOptions;
        this.chokidarOptionsHash = JSON.stringify(chokidarOptions);
        this.filter = createFilter(watchOptions.include, watchOptions.exclude);
    }
    close() {
        this.closed = true;
        for (const id of this.watched) {
            deleteTask(id, this, this.chokidarOptionsHash);
        }
    }
    invalidate(id, isTransformDependency) {
        this.invalidated = true;
        if (isTransformDependency) {
            for (const module of this.cache.modules) {
                if (module.transformDependencies.indexOf(id) === -1)
                    continue;
                // effective invalidation
                module.originalCode = null;
            }
        }
        this.watcher.invalidate(id);
    }
    run() {
        if (!this.invalidated)
            return;
        this.invalidated = false;
        const options = Object.assign(Object.assign({}, this.inputOptions), { cache: this.cache });
        const start = Date.now();
        this.watcher.emit('event', {
            code: 'BUNDLE_START',
            input: this.inputOptions.input,
            output: this.outputFiles
        });
        setWatcher(this.watcher.emitter);
        return rollup(options)
            .then(result => {
            if (this.closed)
                return undefined;
            this.updateWatchedFiles(result);
            return Promise.all(this.outputs.map(output => result.write(output))).then(() => result);
        })
            .then((result) => {
            this.watcher.emit('event', {
                code: 'BUNDLE_END',
                duration: Date.now() - start,
                input: this.inputOptions.input,
                output: this.outputFiles,
                result
            });
        })
            .catch((error) => {
            if (this.closed)
                return;
            if (Array.isArray(error.watchFiles)) {
                for (const id of error.watchFiles) {
                    this.watchFile(id);
                }
            }
            if (error.id) {
                this.cache.modules = this.cache.modules.filter(module => module.id !== error.id);
            }
            throw error;
        });
    }
    updateWatchedFiles(result) {
        const previouslyWatched = this.watched;
        this.watched = new Set();
        this.watchFiles = result.watchFiles;
        this.cache = result.cache;
        for (const id of this.watchFiles) {
            this.watchFile(id);
        }
        for (const module of this.cache.modules) {
            for (const depId of module.transformDependencies) {
                this.watchFile(depId, true);
            }
        }
        for (const id of previouslyWatched) {
            if (!this.watched.has(id))
                deleteTask(id, this, this.chokidarOptionsHash);
        }
    }
    watchFile(id, isTransformDependency = false) {
        if (!this.filter(id))
            return;
        this.watched.add(id);
        if (this.outputFiles.some(file => file === id)) {
            throw new Error('Cannot import the generated bundle');
        }
        // this is necessary to ensure that any 'renamed' files
        // continue to be watched following an error
        addTask(id, this, this.chokidarOptions, this.chokidarOptionsHash, isTransformDependency);
    }
}
function watch(configs) {
    return new Watcher(configs).emitter;
}

export { version as VERSION, rollup, watch };
