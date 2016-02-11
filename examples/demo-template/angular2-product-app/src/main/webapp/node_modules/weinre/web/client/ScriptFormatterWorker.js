/*
 * Copyright (C) 2011 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

var parse = loadModule("parse-js.js");
var process = loadModule("process.js");

onmessage = function(event) {
    var source = event.data;
    var formattedSource = beautify(source);
    var mapping = buildMapping(source, formattedSource);
    postMessage({ formattedSource: formattedSource, mapping: mapping });
};

function beautify(source)
{
    var ast = parse.parse(source);
    var beautifyOptions = {
        indent_level: 4,
        indent_start: 0,
        quote_keys: false,
        space_colon: false
    };
    return process.gen_code(ast, beautifyOptions);
}

function buildMapping(source, formattedSource)
{
    var mapping = { original: [], formatted: [] };
    var lastCodePosition = 0;
    var regexp = /[\$\.\w]+|{|}/g;
    while (true) {
        var match = regexp.exec(formattedSource);
        if (!match)
            break;
        var position = source.indexOf(match[0], lastCodePosition);
        if (position === -1)
            continue;
        mapping.original.push(position);
        mapping.formatted.push(match.index);
        lastCodePosition = position + match[0].length;
    }
    return mapping;
}

function loadModule(src)
{
    var request = new XMLHttpRequest();
    request.open("GET", src, false);
    request.send();

    var exports = {};
    eval(request.responseText);
    return exports;
}

function require()
{
    return parse;
}
