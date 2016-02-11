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

WebInspector.SourceFrameContent = function(text, mapping, scriptRanges)
{
    this._text = text;
    this._mapping = mapping;
    this._scriptRanges = scriptRanges;
}

WebInspector.SourceFrameContent.prototype = {
    get text()
    {
        return this._text;
    },

    get scriptRanges()
    {
        return this._scriptRanges;
    },

    sourceFrameLineNumberToActualLocation: function(lineNumber)
    {
        // Script content may start right after <script> tag without new line (e.g. "<script>function f()...").
        // In that case, column number should be equal to script column offset.
        var columnNumber = 0;
        for (var i = 0; i < this._scriptRanges.length; ++i) {
            var scriptRange = this._scriptRanges[i];
            if (scriptRange.start.lineNumber < lineNumber)
                continue;
            if (scriptRange.start.lineNumber === lineNumber)
                columnNumber = scriptRange.start.columnNumber;
            break;
        }
        var location = this._mapping.sourceLocationToActualLocation(lineNumber, columnNumber);
        location.sourceID = this._sourceIDForSourceFrameLineNumber(lineNumber);
        return location;
    },

    actualLocationToSourceFrameLineNumber: function(lineNumber, columnNumber)
    {
        return this._mapping.actualLocationToSourceLocation(lineNumber, columnNumber).lineNumber;
    },

    _sourceIDForSourceFrameLineNumber: function(lineNumber)
    {
        for (var i = 0; i < this._scriptRanges.length; ++i) {
            var scriptRange = this._scriptRanges[i];
            if (lineNumber < scriptRange.start.lineNumber)
                return;
            if (lineNumber > scriptRange.end.lineNumber)
                continue;
            if (lineNumber === scriptRange.end.lineNumber && !scriptRange.end.columnNumber)
                continue;
            return scriptRange.sourceID;
        }
    }
}


WebInspector.SourceMapping = function()
{
}

WebInspector.SourceMapping.prototype = {
    actualLocationToSourceLocation: function(lineNumber, columnNumber)
    {
        // Should be implemented by subclasses.
    },

    sourceLocationToActualLocation: function(lineNumber, columnNumber)
    {
        // Should be implemented by subclasses.
    }
}


WebInspector.IdenticalSourceMapping = function()
{
    WebInspector.SourceMapping.call(this);
}

WebInspector.IdenticalSourceMapping.prototype = {
    actualLocationToSourceLocation: function(lineNumber, columnNumber)
    {
        return { lineNumber: lineNumber, columnNumber: columnNumber};
    },

    sourceLocationToActualLocation: function(lineNumber, columnNumber)
    {
        return { lineNumber: lineNumber, columnNumber: columnNumber};
    }
}

WebInspector.IdenticalSourceMapping.prototype.__proto__ = WebInspector.SourceMapping.prototype;
