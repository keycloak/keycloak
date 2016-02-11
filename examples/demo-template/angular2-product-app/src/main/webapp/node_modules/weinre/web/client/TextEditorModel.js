/*
 * Copyright (C) 2009 Google Inc. All rights reserved.
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

WebInspector.TextRange = function(startLine, startColumn, endLine, endColumn)
{
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
}

WebInspector.TextRange.prototype = {
    isEmpty: function()
    {
        return this.startLine === this.endLine && this.startColumn === this.endColumn;
    },

    get linesCount()
    {
        return this.endLine - this.startLine;
    },

    clone: function()
    {
        return new WebInspector.TextRange(this.startLine, this.startColumn, this.endLine, this.endColumn); 
    }
}

WebInspector.TextEditorModel = function()
{
    this._lines = [""];
    this._attributes = [];
    this._undoStack = [];
    this._noPunctuationRegex = /[^ !%&()*+,-.:;<=>?\[\]\^{|}~]+/;
}

WebInspector.TextEditorModel.prototype = {
    set changeListener(changeListener)
    {
        this._changeListener = changeListener;
    },

    get linesCount()
    {
        return this._lines.length;
    },

    line: function(lineNumber)
    {
        if (lineNumber >= this._lines.length)
            throw "Out of bounds:" + lineNumber;
        return this._lines[lineNumber];
    },

    lineLength: function(lineNumber)
    {
        return this._lines[lineNumber].length;
    },

    setText: function(range, text)
    {
        if (!range)
            range = new WebInspector.TextRange(0, 0, this._lines.length - 1, this._lines[this._lines.length - 1].length);
        var command = this._pushUndoableCommand(range, text);
        var newRange = this._innerSetText(range, text);
        command.range = newRange.clone();

        if (this._changeListener)
            this._changeListener(range, newRange, command.text, text);
        return newRange;
    },

    set replaceTabsWithSpaces(replaceTabsWithSpaces)
    {
        this._replaceTabsWithSpaces = replaceTabsWithSpaces;
    },

    _innerSetText: function(range, text)
    {
        this._eraseRange(range);
        if (text === "")
            return new WebInspector.TextRange(range.startLine, range.startColumn, range.startLine, range.startColumn);

        var newLines = text.split("\n");
        this._replaceTabsIfNeeded(newLines);

        var prefix = this._lines[range.startLine].substring(0, range.startColumn);
        var prefixArguments = this._arguments
        var suffix = this._lines[range.startLine].substring(range.startColumn);

        var postCaret = prefix.length;
        // Insert text.
        if (newLines.length === 1) {
            this._setLine(range.startLine, prefix + newLines[0] + suffix);
            postCaret += newLines[0].length;
        } else {
            this._setLine(range.startLine, prefix + newLines[0]);
            for (var i = 1; i < newLines.length; ++i)
                this._insertLine(range.startLine + i, newLines[i]);
            this._setLine(range.startLine + newLines.length - 1, newLines[newLines.length - 1] + suffix);
            postCaret = newLines[newLines.length - 1].length;
        }
        return new WebInspector.TextRange(range.startLine, range.startColumn,
                                          range.startLine + newLines.length - 1, postCaret);
    },

    _replaceTabsIfNeeded: function(lines)
    {
        if (!this._replaceTabsWithSpaces)
            return;
        var spaces = [ "    ", "   ", "  ", " "];
        for (var i = 0; i < lines.length; ++i) {
            var line = lines[i];
            var index = line.indexOf("\t");
            while (index !== -1) {
                line = line.substring(0, index) + spaces[index % 4] + line.substring(index + 1);
                index = line.indexOf("\t", index + 1);
            }
            lines[i] = line;
        }
    },

    _eraseRange: function(range)
    {
        if (range.isEmpty())
            return;

        var prefix = this._lines[range.startLine].substring(0, range.startColumn);
        var suffix = this._lines[range.endLine].substring(range.endColumn);

        if (range.endLine > range.startLine)
            this._removeLines(range.startLine + 1, range.endLine - range.startLine);
        this._setLine(range.startLine, prefix + suffix);
    },

    _setLine: function(lineNumber, text)
    {
        this._lines[lineNumber] = text;
    },

    _removeLines: function(fromLine, count)
    {
        this._lines.splice(fromLine, count);
        this._attributes.splice(fromLine, count);
    },

    _insertLine: function(lineNumber, text)
    {
        this._lines.splice(lineNumber, 0, text);
        this._attributes.splice(lineNumber, 0, {});
    },

    wordRange: function(lineNumber, column)
    {
        return new WebInspector.TextRange(lineNumber, this.wordStart(lineNumber, column, true), lineNumber, this.wordEnd(lineNumber, column, true));
    },

    wordStart: function(lineNumber, column, gapless)
    {
        var line = this._lines[lineNumber];
        var prefix = line.substring(0, column).split("").reverse().join("");
        var prefixMatch = this._noPunctuationRegex.exec(prefix);
        return prefixMatch && (!gapless || prefixMatch.index === 0) ? column - prefixMatch.index - prefixMatch[0].length : column;
    },

    wordEnd: function(lineNumber, column, gapless)
    {
        var line = this._lines[lineNumber];
        var suffix = line.substring(column);
        var suffixMatch = this._noPunctuationRegex.exec(suffix);
        return suffixMatch && (!gapless || suffixMatch.index === 0) ? column + suffixMatch.index + suffixMatch[0].length : column;
    },

    copyRange: function(range)
    {
        if (!range)
            range = new WebInspector.TextRange(0, 0, this._lines.length - 1, this._lines[this._lines.length - 1].length);

        var clip = [];
        if (range.startLine === range.endLine) {
            clip.push(this._lines[range.startLine].substring(range.startColumn, range.endColumn));
            return clip.join("\n");
        }
        clip.push(this._lines[range.startLine].substring(range.startColumn));
        for (var i = range.startLine + 1; i < range.endLine; ++i)
            clip.push(this._lines[i]);
        clip.push(this._lines[range.endLine].substring(0, range.endColumn));
        return clip.join("\n");
    },

    setAttribute: function(line, name, value)
    {
        var attrs = this._attributes[line];
        if (!attrs) {
            attrs = {};
            this._attributes[line] = attrs;
        }
        attrs[name] = value;
    },

    getAttribute: function(line, name)
    {
        var attrs = this._attributes[line];
        return attrs ? attrs[name] : null;
    },

    removeAttribute: function(line, name)
    {
        var attrs = this._attributes[line];
        if (attrs)
            delete attrs[name];
    },

    _pushUndoableCommand: function(range, text)
    {
        var command = {
            text: this.copyRange(range),
            startLine: range.startLine,
            startColumn: range.startColumn,
            endLine: range.startLine,
            endColumn: range.startColumn
        };
        if (this._inUndo)
            this._redoStack.push(command);
        else {
            if (!this._inRedo)
                this._redoStack = [];
            this._undoStack.push(command);
        }
        return command;
    },

    undo: function()
    {
        this._markRedoableState();

        this._inUndo = true;
        var range = this._doUndo(this._undoStack);
        delete this._inUndo;

        return range;
    },

    redo: function()
    {
        this.markUndoableState();

        this._inRedo = true;
        var range = this._doUndo(this._redoStack);
        delete this._inRedo;

        return range;
    },

    _doUndo: function(stack)
    {
        var range = null;
        for (var i = stack.length - 1; i >= 0; --i) {
            var command = stack[i];
            stack.length = i;

            range = this.setText(command.range, command.text);
            if (i > 0 && stack[i - 1].explicit)
                return range;
        }
        return range;
    },

    markUndoableState: function()
    {
        if (this._undoStack.length)
            this._undoStack[this._undoStack.length - 1].explicit = true;
    },

    _markRedoableState: function()
    {
        if (this._redoStack.length)
            this._redoStack[this._redoStack.length - 1].explicit = true;
    },

    resetUndoStack: function()
    {
        this._undoStack = [];
    }
}
