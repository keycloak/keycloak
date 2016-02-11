/*
 * Copyright (C) 2009 Google Inc. All rights reserved.
 * Copyright (C) 2010 Apple Inc. All rights reserved.
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

WebInspector.TextViewer = function(textModel, platform, url)
{
    this._textModel = textModel;
    this._textModel.changeListener = this._textChanged.bind(this);

    this.element = document.createElement("div");
    this.element.className = "text-editor monospace";

    var syncScrollListener = this._syncScroll.bind(this);
    var syncDecorationsForLineListener = this._syncDecorationsForLine.bind(this);
    this._mainPanel = new WebInspector.TextEditorMainPanel(this._textModel, url, syncScrollListener, syncDecorationsForLineListener);
    this._gutterPanel = new WebInspector.TextEditorGutterPanel(this._textModel, syncDecorationsForLineListener);
    this.element.appendChild(this._mainPanel.element);
    this.element.appendChild(this._gutterPanel.element);
}

WebInspector.TextViewer.prototype = {
    set mimeType(mimeType)
    {
        this._mainPanel.mimeType = mimeType;
    },

    set readOnly(readOnly)
    {
        this._mainPanel.readOnly = readOnly;
    },

    get textModel()
    {
        return this._textModel;
    },

    revealLine: function(lineNumber)
    {
        this._mainPanel.revealLine(lineNumber);
    },

    addDecoration: function(lineNumber, decoration)
    {
        this._mainPanel.addDecoration(lineNumber, decoration);
        this._gutterPanel.addDecoration(lineNumber, decoration);
    },

    removeDecoration: function(lineNumber, decoration)
    {
        this._mainPanel.removeDecoration(lineNumber, decoration);
        this._gutterPanel.removeDecoration(lineNumber, decoration);
    },

    markAndRevealRange: function(range)
    {
        this._mainPanel.markAndRevealRange(range);
    },

    highlightLine: function(lineNumber)
    {
        this._mainPanel.highlightLine(lineNumber);
    },

    clearLineHighlight: function()
    {
        this._mainPanel.clearLineHighlight();
    },

    freeCachedElements: function()
    {
        this._mainPanel.freeCachedElements();
        this._gutterPanel.freeCachedElements();
    },

    editLine: function(lineRow, callback)
    {
        this._mainPanel.editLine(lineRow, callback);
    },

    get scrollTop()
    {
        return this._mainPanel.element.scrollTop;
    },

    set scrollTop(scrollTop)
    {
        this._mainPanel.element.scrollTop = scrollTop;
    },

    get scrollLeft()
    {
        return this._mainPanel.element.scrollLeft;
    },

    set scrollLeft(scrollLeft)
    {
        this._mainPanel.element.scrollLeft = scrollLeft;
    },

    beginUpdates: function()
    {
        this._mainPanel.beginUpdates();
        this._gutterPanel.beginUpdates();
    },

    endUpdates: function()
    {
        this._mainPanel.endUpdates();
        this._gutterPanel.endUpdates();
    },

    resize: function()
    {
        this._mainPanel.resize();
        this._gutterPanel.resize();
        this._updatePanelOffsets();
    },

    // WebInspector.TextModel listener
    _textChanged: function(oldRange, newRange, oldText, newText)
    {
        this._mainPanel.textChanged();
        this._gutterPanel.textChanged();
        this._updatePanelOffsets();
    },

    _updatePanelOffsets: function()
    {
        var lineNumbersWidth = this._gutterPanel.element.offsetWidth;
        if (lineNumbersWidth)
            this._mainPanel.element.style.setProperty("left", lineNumbersWidth + "px");
        else
            this._mainPanel.element.style.removeProperty("left"); // Use default value set in CSS.
    },

    _syncScroll: function()
    {
        // Async call due to performance reasons.
        setTimeout(function() {
            var mainElement = this._mainPanel.element;
            var gutterElement = this._gutterPanel.element;

            // Handle horizontal scroll bar at the bottom of the main panel.
            if (gutterElement.offsetHeight > mainElement.clientHeight)
                gutterElement.style.setProperty("padding-bottom", (gutterElement.offsetHeight - mainElement.clientHeight) + "px");
            else
                gutterElement.style.removeProperty("padding-bottom");

            gutterElement.scrollTop = mainElement.scrollTop;
        }.bind(this), 0);
    },

    _syncDecorationsForLine: function(lineNumber)
    {
        if (lineNumber >= this._textModel.linesCount)
            return;

        var mainChunk = this._mainPanel.makeLineAChunk(lineNumber);
        var gutterChunk = this._gutterPanel.makeLineAChunk(lineNumber);
        var height = mainChunk.height;
        if (height)
            gutterChunk.element.style.setProperty("height", height + "px");
        else
            gutterChunk.element.style.removeProperty("height");
    }
}

WebInspector.TextEditorChunkedPanel = function(textModel)
{
    this._textModel = textModel;

    this._defaultChunkSize = 50;
    this._paintCoalescingLevel = 0;
    this._domUpdateCoalescingLevel = 0;
}

WebInspector.TextEditorChunkedPanel.prototype = {
    get textModel()
    {
        return this._textModel;
    },

    revealLine: function(lineNumber)
    {
        if (lineNumber >= this._textModel.linesCount)
            return;

        var chunk = this.makeLineAChunk(lineNumber);
        chunk.element.scrollIntoViewIfNeeded();
    },

    addDecoration: function(lineNumber, decoration)
    {
        var chunk = this.makeLineAChunk(lineNumber);
        chunk.addDecoration(decoration);
    },

    removeDecoration: function(lineNumber, decoration)
    {
        var chunk = this.makeLineAChunk(lineNumber);
        chunk.removeDecoration(decoration);
    },

    textChanged: function(oldRange, newRange, oldText, newText)
    {
        this._buildChunks();
    },

    _buildChunks: function()
    {
        this.beginDomUpdates();

        this.element.removeChildren();

        this._textChunks = [];
        for (var i = 0; i < this._textModel.linesCount; i += this._defaultChunkSize) {
            var chunk = this._createNewChunk(i, i + this._defaultChunkSize);
            this._textChunks.push(chunk);
            this.element.appendChild(chunk.element);
        }

        this._repaintAll();

        this.endDomUpdates();
    },

    makeLineAChunk: function(lineNumber)
    {
        if (!this._textChunks)
            this._buildChunks();

        var chunkNumber = this._chunkNumberForLine(lineNumber);
        var oldChunk = this._textChunks[chunkNumber];
        if (oldChunk.linesCount === 1)
            return oldChunk;

        this.beginDomUpdates();

        var wasExpanded = oldChunk.expanded;
        oldChunk.expanded = false;

        var insertIndex = chunkNumber + 1;

        // Prefix chunk.
        if (lineNumber > oldChunk.startLine) {
            var prefixChunk = this._createNewChunk(oldChunk.startLine, lineNumber);
            this._textChunks.splice(insertIndex++, 0, prefixChunk);
            this.element.insertBefore(prefixChunk.element, oldChunk.element);
        }

        // Line chunk.
        var lineChunk = this._createNewChunk(lineNumber, lineNumber + 1);
        this._textChunks.splice(insertIndex++, 0, lineChunk);
        this.element.insertBefore(lineChunk.element, oldChunk.element);

        // Suffix chunk.
        if (oldChunk.startLine + oldChunk.linesCount > lineNumber + 1) {
            var suffixChunk = this._createNewChunk(lineNumber + 1, oldChunk.startLine + oldChunk.linesCount);
            this._textChunks.splice(insertIndex, 0, suffixChunk);
            this.element.insertBefore(suffixChunk.element, oldChunk.element);
        }

        // Remove enclosing chunk.
        this._textChunks.splice(chunkNumber, 1);
        this.element.removeChild(oldChunk.element);

        if (wasExpanded) {
            if (prefixChunk)
                prefixChunk.expanded = true;
            lineChunk.expanded = true;
            if (suffixChunk)
                suffixChunk.expanded = true;
        }

        this.endDomUpdates();

        return lineChunk;
    },

    _scroll: function()
    {
        this._scheduleRepaintAll();
        if (this._syncScrollListener)
            this._syncScrollListener();
    },

    _scheduleRepaintAll: function()
    {
        if (this._repaintAllTimer)
            clearTimeout(this._repaintAllTimer);
        this._repaintAllTimer = setTimeout(this._repaintAll.bind(this), 50);
    },

    beginUpdates: function()
    {
        this._paintCoalescingLevel++;
    },

    endUpdates: function()
    {
        this._paintCoalescingLevel--;
        if (!this._paintCoalescingLevel)
            this._repaintAll();
    },

    beginDomUpdates: function()
    {
        this._domUpdateCoalescingLevel++;
    },

    endDomUpdates: function()
    {
        this._domUpdateCoalescingLevel--;
    },

    _chunkNumberForLine: function(lineNumber)
    {
        for (var i = 0; i < this._textChunks.length; ++i) {
            var line = this._textChunks[i].startLine;
            if (lineNumber >= line && lineNumber < line + this._textChunks[i].linesCount)
                return i;
        }
        return this._textChunks.length - 1;
    },

    _chunkForLine: function(lineNumber)
    {
        return this._textChunks[this._chunkNumberForLine(lineNumber)];
    },

    _repaintAll: function()
    {
        delete this._repaintAllTimer;

        if (this._paintCoalescingLevel || this._dirtyLines)
            return;

        if (!this._textChunks)
            this._buildChunks();

        var visibleFrom = this.element.scrollTop;
        var visibleTo = this.element.scrollTop + this.element.clientHeight;

        var offset = 0;
        var fromIndex = -1;
        var toIndex = 0;
        for (var i = 0; i < this._textChunks.length; ++i) {
            var chunk = this._textChunks[i];
            var chunkHeight = chunk.height;
            if (offset + chunkHeight > visibleFrom && offset < visibleTo) {
                if (fromIndex === -1)
                    fromIndex = i;
                toIndex = i + 1;
            } else {
                if (offset >= visibleTo)
                    break;
            }
            offset += chunkHeight;
        }

        if (toIndex)
            this._expandChunks(fromIndex, toIndex);
    },

    _totalHeight: function(firstElement, lastElement)
    {
        lastElement = (lastElement || firstElement).nextElementSibling;
        if (lastElement)
            return lastElement.offsetTop - firstElement.offsetTop;
        else if (firstElement.offsetParent)
            return firstElement.offsetParent.scrollHeight - firstElement.offsetTop;
        return firstElement.offsetHeight;
    },
    
    resize: function()
    {
        this._repaintAll();
    }
}

WebInspector.TextEditorGutterPanel = function(textModel, syncDecorationsForLineListener)
{
    WebInspector.TextEditorChunkedPanel.call(this, textModel);

    this._syncDecorationsForLineListener = syncDecorationsForLineListener;

    this.element = document.createElement("div");
    this.element.className = "text-editor-lines";

    this.element.addEventListener("scroll", this._scroll.bind(this), false);

    this.freeCachedElements();
    this._buildChunks();
}

WebInspector.TextEditorGutterPanel.prototype = {
    freeCachedElements: function()
    {
        this._cachedRows = [];
    },

    _createNewChunk: function(startLine, endLine)
    {
        return new WebInspector.TextEditorGutterChunk(this, startLine, endLine);
    },

    _expandChunks: function(fromIndex, toIndex)
    {
        for (var i = 0; i < this._textChunks.length; ++i) {
            this._textChunks[i].expanded = (fromIndex <= i && i < toIndex);
        }
    }
}

WebInspector.TextEditorGutterPanel.prototype.__proto__ = WebInspector.TextEditorChunkedPanel.prototype;

WebInspector.TextEditorGutterChunk = function(textViewer, startLine, endLine)
{
    this._textViewer = textViewer;
    this._textModel = textViewer._textModel;

    this.startLine = startLine;
    endLine = Math.min(this._textModel.linesCount, endLine);
    this.linesCount = endLine - startLine;

    this._expanded = false;

    this.element = document.createElement("div");
    this.element.lineNumber = startLine;
    this.element.className = "webkit-line-number";

    if (this.linesCount === 1) {
        // Single line chunks are typically created for decorations. Host line number in
        // the sub-element in order to allow flexible border / margin management.
        var innerSpan = document.createElement("span");
        innerSpan.className = "webkit-line-number-inner";
        innerSpan.textContent = startLine + 1;
        var outerSpan = document.createElement("div");
        outerSpan.className = "webkit-line-number-outer";
        outerSpan.appendChild(innerSpan);
        this.element.appendChild(outerSpan);
    } else {
        var lineNumbers = [];
        for (var i = startLine; i < endLine; ++i) {
            lineNumbers.push(i + 1);
        }
        this.element.textContent = lineNumbers.join("\n");
    }
}

WebInspector.TextEditorGutterChunk.prototype = {
    addDecoration: function(decoration)
    {
        if (typeof decoration === "string") {
            this.element.addStyleClass(decoration);
        }
    },

    removeDecoration: function(decoration)
    {
        if (typeof decoration === "string") {
            this.element.removeStyleClass(decoration);
        }
    },

    get expanded()
    {
        return this._expanded;
    },

    set expanded(expanded)
    {
        if (this.linesCount === 1)
            this._textViewer._syncDecorationsForLineListener(this.startLine);

        if (this._expanded === expanded)
            return;

        this._expanded = expanded;

        if (this.linesCount === 1)
            return;

        this._textViewer.beginDomUpdates();

        if (expanded) {
            this._expandedLineRows = [];
            var parentElement = this.element.parentElement;
            for (var i = this.startLine; i < this.startLine + this.linesCount; ++i) {
                var lineRow = this._createRow(i);
                parentElement.insertBefore(lineRow, this.element);
                this._expandedLineRows.push(lineRow);
            }
            parentElement.removeChild(this.element);
        } else {
            var elementInserted = false;
            for (var i = 0; i < this._expandedLineRows.length; ++i) {
                var lineRow = this._expandedLineRows[i];
                var parentElement = lineRow.parentElement;
                if (parentElement) {
                    if (!elementInserted) {
                        elementInserted = true;
                        parentElement.insertBefore(this.element, lineRow);
                    }
                    parentElement.removeChild(lineRow);
                }
                this._textViewer._cachedRows.push(lineRow);
            }
            delete this._expandedLineRows;
        }

        this._textViewer.endDomUpdates();
    },

    get height()
    {
        if (!this._expandedLineRows)
            return this._textViewer._totalHeight(this.element);
        return this._textViewer._totalHeight(this._expandedLineRows[0], this._expandedLineRows[this._expandedLineRows.length - 1]);
    },

    _createRow: function(lineNumber)
    {
        var lineRow = this._textViewer._cachedRows.pop() || document.createElement("div");
        lineRow.lineNumber = lineNumber;
        lineRow.className = "webkit-line-number";
        lineRow.textContent = lineNumber + 1;
        return lineRow;
    }
}

WebInspector.TextEditorMainPanel = function(textModel, url, syncScrollListener, syncDecorationsForLineListener)
{
    WebInspector.TextEditorChunkedPanel.call(this, textModel);

    this._syncScrollListener = syncScrollListener;
    this._syncDecorationsForLineListener = syncDecorationsForLineListener;

    this._url = url;
    this._highlighter = new WebInspector.TextEditorHighlighter(textModel, this._highlightDataReady.bind(this));
    this._readOnly = true;

    this.element = document.createElement("div");
    this.element.className = "text-editor-contents";
    this.element.tabIndex = 0;

    this.element.addEventListener("scroll", this._scroll.bind(this), false);

    // FIXME: Remove old live editing functionality and Preferences.sourceEditorEnabled flag.
    if (!Preferences.sourceEditorEnabled)
        this.element.addEventListener("keydown", this._handleKeyDown.bind(this), false);

    var handleDOMUpdates = this._handleDOMUpdates.bind(this);
    this.element.addEventListener("DOMCharacterDataModified", handleDOMUpdates, false);
    this.element.addEventListener("DOMNodeInserted", handleDOMUpdates, false);
    this.element.addEventListener("DOMNodeRemoved", handleDOMUpdates, false);
    // For some reasons, in a few corner cases the events above are not able to catch the editings.
    // To workaround that we also listen to a more general event as a backup.
    this.element.addEventListener("DOMSubtreeModified", this._handleDOMSubtreeModified.bind(this), false);

    this.freeCachedElements();
    this._buildChunks();
}

WebInspector.TextEditorMainPanel.prototype = {
    set mimeType(mimeType)
    {
        this._highlighter.mimeType = mimeType;
    },

    set readOnly(readOnly)
    {
        // FIXME: Remove the Preferences.sourceEditorEnabled flag.
        if (!Preferences.sourceEditorEnabled)
            return;

        this._readOnly = readOnly;
        if (this._readOnly)
            this.element.removeStyleClass("text-editor-editable");
        else
            this.element.addStyleClass("text-editor-editable");
    },

    markAndRevealRange: function(range)
    {
        if (this._rangeToMark) {
            var markedLine = this._rangeToMark.startLine;
            this._rangeToMark = null;
            this._paintLines(markedLine, markedLine + 1);
        }

        if (range) {
            this._rangeToMark = range;
            this.revealLine(range.startLine);
            this._paintLines(range.startLine, range.startLine + 1);
            if (this._markedRangeElement)
                this._markedRangeElement.scrollIntoViewIfNeeded();
        }
        delete this._markedRangeElement;
    },

    highlightLine: function(lineNumber)
    {
        this.clearLineHighlight();
        this._highlightedLine = lineNumber;
        this.revealLine(lineNumber);
        this.addDecoration(lineNumber, "webkit-highlighted-line");
    },

    clearLineHighlight: function()
    {
        if (typeof this._highlightedLine === "number") {
            this.removeDecoration(this._highlightedLine, "webkit-highlighted-line");
            delete this._highlightedLine;
        }
    },

    freeCachedElements: function()
    {
        this._cachedSpans = [];
        this._cachedTextNodes = [];
        this._cachedRows = [];
    },

    _handleKeyDown: function()
    {
        if (this._editingLine || event.metaKey || event.shiftKey || event.ctrlKey || event.altKey)
            return;

        var scrollValue = 0;
        if (event.keyCode === WebInspector.KeyboardShortcut.Keys.Up.code)
            scrollValue = -1;
        else if (event.keyCode == WebInspector.KeyboardShortcut.Keys.Down.code)
            scrollValue = 1;

        if (scrollValue) {
            event.preventDefault();
            event.stopPropagation();
            this.element.scrollByLines(scrollValue);
            return;
        }

        scrollValue = 0;
        if (event.keyCode === WebInspector.KeyboardShortcut.Keys.Left.code)
            scrollValue = -40;
        else if (event.keyCode == WebInspector.KeyboardShortcut.Keys.Right.code)
            scrollValue = 40;

        if (scrollValue) {
            event.preventDefault();
            event.stopPropagation();
            this.element.scrollLeft += scrollValue;
        }
    },

    editLine: function(lineRow, callback)
    {
        var oldContent = lineRow.innerHTML;
        function finishEditing(committed, e, newContent)
        {
            if (committed)
                callback(newContent);
            lineRow.innerHTML = oldContent;
            delete this._editingLine;
        }
        this._editingLine = WebInspector.startEditing(lineRow, {
            context: null,
            commitHandler: finishEditing.bind(this, true),
            cancelHandler: finishEditing.bind(this, false),
            multiline: true
        });
    },

    _buildChunks: function()
    {
        this._highlighter.reset();
        for (var i = 0; i < this._textModel.linesCount; ++i)
            this._textModel.removeAttribute(i, "highlight");

        WebInspector.TextEditorChunkedPanel.prototype._buildChunks.call(this);
    },

    _createNewChunk: function(startLine, endLine)
    {
        return new WebInspector.TextEditorMainChunk(this, startLine, endLine);
    },

    _expandChunks: function(fromIndex, toIndex)
    {
        var lastChunk = this._textChunks[toIndex - 1];
        var lastVisibleLine = lastChunk.startLine + lastChunk.linesCount;

        var selection = this._getSelection();

        this._muteHighlightListener = true;
        this._highlighter.highlight(lastVisibleLine);
        delete this._muteHighlightListener;

        for (var i = 0; i < this._textChunks.length; ++i) {
            this._textChunks[i].expanded = (fromIndex <= i && i < toIndex);
        }

        this._restoreSelection(selection);
    },

    _highlightDataReady: function(fromLine, toLine)
    {
        if (this._muteHighlightListener || this._dirtyLines)
            return;
        this._paintLines(fromLine, toLine, true /*restoreSelection*/);
    },

    _paintLines: function(fromLine, toLine, restoreSelection)
    {
        var selection;
        var chunk = this._chunkForLine(fromLine);
        for (var i = fromLine; i < toLine; ++i) {
            if (i >= chunk.startLine + chunk.linesCount)
                chunk = this._chunkForLine(i);
            var lineRow = chunk.getExpandedLineRow(i);
            if (!lineRow)
                continue;
            if (restoreSelection && !selection)
                selection = this._getSelection();
            this._paintLine(lineRow, i);
        }
        if (restoreSelection)
            this._restoreSelection(selection);
    },

    _paintLine: function(lineRow, lineNumber)
    {
        this.beginDomUpdates();
        try {
            var highlight = this._textModel.getAttribute(lineNumber, "highlight");
            if (!highlight) {
                if (this._rangeToMark && this._rangeToMark.startLine === lineNumber)
                    this._markedRangeElement = highlightSearchResult(lineRow, this._rangeToMark.startColumn, this._rangeToMark.endColumn - this._rangeToMark.startColumn);
                return;
            }

            lineRow.removeChildren();
            var line = this._textModel.line(lineNumber);
            if (!line)
                lineRow.appendChild(document.createElement("br"));

            var plainTextStart = -1;
            for (var j = 0; j < line.length;) {
                if (j > 1000) {
                    // This line is too long - do not waste cycles on minified js highlighting.
                    if (plainTextStart === -1)
                        plainTextStart = j;
                    break;
                }
                var attribute = highlight[j];
                if (!attribute || !attribute.tokenType) {
                    if (plainTextStart === -1)
                        plainTextStart = j;
                    j++;
                } else {
                    if (plainTextStart !== -1) {
                        this._appendTextNode(lineRow, line.substring(plainTextStart, j));
                        plainTextStart = -1;
                    }
                    this._appendSpan(lineRow, line.substring(j, j + attribute.length), attribute.tokenType);
                    j += attribute.length;
                }
            }
            if (plainTextStart !== -1)
                this._appendTextNode(lineRow, line.substring(plainTextStart, line.length));
            if (this._rangeToMark && this._rangeToMark.startLine === lineNumber)
                this._markedRangeElement = highlightSearchResult(lineRow, this._rangeToMark.startColumn, this._rangeToMark.endColumn - this._rangeToMark.startColumn);
            if (lineRow.decorationsElement)
                lineRow.appendChild(lineRow.decorationsElement);
        } finally {
            this.endDomUpdates();
        }
    },

    _releaseLinesHighlight: function(lineRow)
    {
        if (!lineRow)
            return;
        if ("spans" in lineRow) {
            var spans = lineRow.spans;
            for (var j = 0; j < spans.length; ++j)
                this._cachedSpans.push(spans[j]);
            delete lineRow.spans;
        }
        if ("textNodes" in lineRow) {
            var textNodes = lineRow.textNodes;
            for (var j = 0; j < textNodes.length; ++j)
                this._cachedTextNodes.push(textNodes[j]);
            delete lineRow.textNodes;
        }
        this._cachedRows.push(lineRow);
    },

    _getSelection: function()
    {
        var selection = window.getSelection();
        if (!selection.rangeCount)
            return null;
        var selectionRange = selection.getRangeAt(0);
        // Selection may be outside of the viewer.
        if (!this.element.isAncestor(selectionRange.startContainer) || !this.element.isAncestor(selectionRange.endContainer))
            return null;
        var start = this._selectionToPosition(selectionRange.startContainer, selectionRange.startOffset);
        var end = selectionRange.collapsed ? start : this._selectionToPosition(selectionRange.endContainer, selectionRange.endOffset);
        if (selection.anchorNode === selectionRange.startContainer && selection.anchorOffset === selectionRange.startOffset)
            return new WebInspector.TextRange(start.line, start.column, end.line, end.column);
        else
            return new WebInspector.TextRange(end.line, end.column, start.line, start.column);
    },

    _restoreSelection: function(range)
    {
        if (!range)
            return;
        var start = this._positionToSelection(range.startLine, range.startColumn);
        var end = range.isEmpty() ? start : this._positionToSelection(range.endLine, range.endColumn);
        window.getSelection().setBaseAndExtent(start.container, start.offset, end.container, end.offset);
    },

    _selectionToPosition: function(container, offset)
    {
        if (container === this.element && offset === 0)
            return { line: 0, column: 0 };
        if (container === this.element && offset === 1)
            return { line: this._textModel.linesCount - 1, column: this._textModel.lineLength(this._textModel.linesCount - 1) };

        var lineRow = container.enclosingNodeOrSelfWithNodeName("DIV");
        var lineNumber = lineRow.lineNumber;
        if (container === lineRow && offset === 0)
            return { line: lineNumber, column: 0 };

        // This may be chunk and chunks may contain \n.
        var column = 0;
        var node = lineRow.traverseNextTextNode(lineRow);
        while (node && node !== container) {
            var text = node.textContent;
            for (var i = 0; i < text.length; ++i) {
                if (text.charAt(i) === "\n") {
                    lineNumber++;
                    column = 0;
                } else
                    column++;
            }
            node = node.traverseNextTextNode(lineRow);
        }

        if (node === container && offset) {
            var text = node.textContent;
            for (var i = 0; i < offset; ++i) {
                if (text.charAt(i) === "\n") {
                    lineNumber++;
                    column = 0;
                } else
                    column++;
            }
        }
        return { line: lineNumber, column: column };
    },

    _positionToSelection: function(line, column)
    {
        var chunk = this._chunkForLine(line);
        var lineRow = chunk.getExpandedLineRow(line);
        if (lineRow)
            var rangeBoundary = lineRow.rangeBoundaryForOffset(column);
        else {
            var offset = column;
            for (var i = chunk.startLine; i < line; ++i)
                offset += this._textModel.lineLength(i) + 1; // \n
            lineRow = chunk.element;
            if (lineRow.firstChild)
                var rangeBoundary = { container: lineRow.firstChild, offset: offset };
            else
                var rangeBoundary = { container: lineRow, offset: 0 };
        }
        return rangeBoundary;
    },

    _appendSpan: function(element, content, className)
    {
        if (className === "html-resource-link" || className === "html-external-link") {
            element.appendChild(this._createLink(content, className === "html-external-link"));
            return;
        }

        var span = this._cachedSpans.pop() || document.createElement("span");
        span.className = "webkit-" + className;
        span.textContent = content;
        element.appendChild(span);
        if (!("spans" in element))
            element.spans = [];
        element.spans.push(span);
    },

    _appendTextNode: function(element, text)
    {
        var textNode = this._cachedTextNodes.pop();
        if (textNode)
            textNode.nodeValue = text;
        else
            textNode = document.createTextNode(text);
        element.appendChild(textNode);
        if (!("textNodes" in element))
            element.textNodes = [];
        element.textNodes.push(textNode);
    },

    _createLink: function(content, isExternal)
    {
        var quote = content.charAt(0);
        if (content.length > 1 && (quote === "\"" ||   quote === "'"))
            content = content.substring(1, content.length - 1);
        else
            quote = null;

        var a = WebInspector.linkifyURLAsNode(this._rewriteHref(content), content, null, isExternal);
        var span = document.createElement("span");
        span.className = "webkit-html-attribute-value";
        if (quote)
            span.appendChild(document.createTextNode(quote));
        span.appendChild(a);
        if (quote)
            span.appendChild(document.createTextNode(quote));
        return span;
    },

    _rewriteHref: function(hrefValue, isExternal)
    {
        if (!this._url || !hrefValue || hrefValue.indexOf("://") > 0)
            return hrefValue;
        return WebInspector.completeURL(this._url, hrefValue);
    },

    textChanged: function(oldRange, newRange, oldText, newText)
    {
        // FIXME: Update only that part of the editor that has just been changed.
        this._buildChunks();
    },

    _handleDOMUpdates: function(e)
    {
        if (this._domUpdateCoalescingLevel)
            return;

        var target = e.target;
        if (target === this.element)
            return;

        var lineRow = target.enclosingNodeOrSelfWithClass("webkit-line-content");
        if (!lineRow)
            return;

        if (lineRow.decorationsElement && lineRow.decorationsElement.isAncestor(target)) {
            if (this._syncDecorationsForLineListener) {
                // Wait until this event is processed and only then sync the sizes. This is necessary in
                // case of the DOMNodeRemoved event, because it is dispatched before the removal takes place.
                setTimeout(function() {
                    this._syncDecorationsForLineListener(lineRow.lineNumber);
                }.bind(this), 0);
            }
            return;
        }

        if (this._readOnly)
            return;

        if (target === lineRow && (e.type === "DOMNodeInserted" || e.type === "DOMNodeRemoved")) {
            // The "lineNumber" (if any) is no longer valid for a line being removed or inserted.
            delete lineRow.lineNumber;
        }

        var startLine = 0;
        for (var row = lineRow; row; row = row.previousSibling) {
            if (typeof row.lineNumber === "number") {
                startLine = row.lineNumber;
                break;
            }
        }

        var endLine = this._textModel.linesCount;
        for (var row = lineRow.nextSibling; row; row = row.nextSibling) {
            if (typeof row.lineNumber === "number") {
                endLine = row.lineNumber;
                break;
            }
        }

        if (this._dirtyLines) {
            this._dirtyLines.start = Math.min(this._dirtyLines.start, startLine);
            this._dirtyLines.end = Math.max(this._dirtyLines.end, endLine);
        } else {
            this._dirtyLines = { start: startLine, end: endLine };
            setTimeout(this._applyDomUpdates.bind(this), 0);
        }
    },

    _handleDOMSubtreeModified: function(e)
    {
        if (this._domUpdateCoalescingLevel || this._readOnly || e.target !== this.element)
            return;

        // Proceed only when other events failed to catch the DOM updates, otherwise it is not necessary.
        if (this._dirtyLines)
            return;

        var selection = this._getSelection();
        if (!selection)
            return;

        var startLine = Math.min(selection.startLine, selection.endLine);
        var endLine = Math.max(selection.startLine, selection.endLine) + 1;
        endLine = Math.min(this._textModel.linesCount, endLine);

        this._dirtyLines = { start: startLine, end: endLine };
        setTimeout(this._applyDomUpdates.bind(this), 0);
    },

    _applyDomUpdates: function()
    {
        if (!this._dirtyLines)
            return;

        var dirtyLines = this._dirtyLines;
        delete this._dirtyLines;

        // Check if the editor had been set readOnly by the moment when this async callback got executed.
        if (this._readOnly)
            return;

        // FIXME: DELETE DECORATIONS IN THE INVOLVED CHUNKS IF ANY! SYNC THE GUTTER ALSO.

        // FIXME: DELETE MARKED AND HIGHLIGHTED LINES (INVALIDATE SEARCH RESULTS)! this._markedRangeElement
        
        var firstChunkNumber = this._chunkNumberForLine(dirtyLines.start);
        var startLine = this._textChunks[firstChunkNumber].startLine;
        var endLine = this._textModel.linesCount;

        // Collect lines.
        var firstLineRow;
        if (firstChunkNumber) {
            var chunk = this._textChunks[firstChunkNumber - 1];
            firstLineRow = chunk.expanded ? chunk.getExpandedLineRow(chunk.startLine + chunk.linesCount - 1) : chunk.element;
            firstLineRow = firstLineRow.nextSibling;
        } else {
            firstLineRow = this.element.firstChild;
        }

        var lines = [];
        for (var lineRow = firstLineRow; lineRow; lineRow = lineRow.nextSibling) {
            if (typeof lineRow.lineNumber === "number" && lineRow.lineNumber >= dirtyLines.end) {
                endLine = lineRow.lineNumber;
                break;
            }
            // Update with the newest lineNumber, so that the call to the _getSelection method below should work.
            lineRow.lineNumber = startLine + lines.length;
            this._collectLinesFromDiv(lines, lineRow);
        }

        // Try to decrease the range being replaced if possible.
        var startOffset = 0;
        while (startLine < dirtyLines.start && startOffset < lines.length) {
            if (this._textModel.line(startLine) !== lines[startOffset])
                break;
            ++startOffset;
            ++startLine;
        }

        var endOffset = lines.length;
        while (endLine > dirtyLines.end && endOffset > startOffset) {
            if (this._textModel.line(endLine - 1) !== lines[endOffset - 1])
                break;
            --endOffset;
            --endLine;
        }

        lines = lines.slice(startOffset, endOffset);

        var selection = this._getSelection();
        this.beginUpdates();

        if (lines.length === 0 && endLine < this._textModel.linesCount) {
            var range = new WebInspector.TextRange(startLine, 0, endLine, 0);
            var newRange = this._textModel.setText(range, '');
        } else {
            var range = new WebInspector.TextRange(startLine, 0, endLine - 1, this._textModel.lineLength(endLine - 1));
            var newRange = this._textModel.setText(range, lines.join("\n"));
        }

        this.endUpdates();
        this._restoreSelection(selection);
    },

    _collectLinesFromDiv: function(lines, element)
    {
        var textContents = [];
        var node = element.traverseNextNode(element);
        while (node) {
            if (node.nodeName.toLowerCase() === "br")
                textContents.push("\n");
            else if (node.nodeType === Node.TEXT_NODE)
                textContents.push(node.textContent);
            node = node.traverseNextNode(element);
        }

        var textContent = textContents.join('');
        // The last \n (if any) does not "count" in a DIV.
        textContent = textContent.replace(/\n$/, '');

        textContents = textContent.split("\n");
        for (var i = 0; i < textContents.length; ++i)
            lines.push(textContents[i]);
    }
}

WebInspector.TextEditorMainPanel.prototype.__proto__ = WebInspector.TextEditorChunkedPanel.prototype;

WebInspector.TextEditorMainChunk = function(textViewer, startLine, endLine)
{
    this._textViewer = textViewer;
    this._textModel = textViewer._textModel;

    this.element = document.createElement("div");
    this.element.lineNumber = startLine;
    this.element.className = "webkit-line-content";

    this.startLine = startLine;
    endLine = Math.min(this._textModel.linesCount, endLine);
    this.linesCount = endLine - startLine;

    this._expanded = false;

    var lines = [];
    for (var i = startLine; i < endLine; ++i) {
        lines.push(this._textModel.line(i));
    }

    this.element.textContent = lines.join("\n");

    // The last empty line will get swallowed otherwise.
    if (!lines[lines.length - 1])
        this.element.appendChild(document.createElement("br"));
}

WebInspector.TextEditorMainChunk.prototype = {
    addDecoration: function(decoration)
    {
        if (typeof decoration === "string") {
            this.element.addStyleClass(decoration);
            return;
        }
        this._textViewer.beginDomUpdates();
        if (!this.element.decorationsElement) {
            this.element.decorationsElement = document.createElement("div");
            this.element.decorationsElement.className = "webkit-line-decorations";
            this.element.appendChild(this.element.decorationsElement);
        }
        this.element.decorationsElement.appendChild(decoration);
        this._textViewer.endDomUpdates();
    },

    removeDecoration: function(decoration)
    {
        if (typeof decoration === "string") {
            this.element.removeStyleClass(decoration);
            return;
        }
        if (!this.element.decorationsElement)
            return;
        this._textViewer.beginDomUpdates();
        this.element.decorationsElement.removeChild(decoration);
        this._textViewer.endDomUpdates();
    },

    get expanded()
    {
        return this._expanded;
    },

    set expanded(expanded)
    {
        if (this._expanded === expanded)
            return;

        this._expanded = expanded;

        if (this.linesCount === 1) {
            if (expanded)
                this._textViewer._paintLine(this.element, this.startLine);
            return;
        }

        this._textViewer.beginDomUpdates();

        if (expanded) {
            this._expandedLineRows = [];
            var parentElement = this.element.parentElement;
            for (var i = this.startLine; i < this.startLine + this.linesCount; ++i) {
                var lineRow = this._createRow(i);
                parentElement.insertBefore(lineRow, this.element);
                this._expandedLineRows.push(lineRow);
                this._textViewer._paintLine(lineRow, i);
            }
            parentElement.removeChild(this.element);
        } else {
            var elementInserted = false;
            for (var i = 0; i < this._expandedLineRows.length; ++i) {
                var lineRow = this._expandedLineRows[i];
                var parentElement = lineRow.parentElement;
                if (parentElement) {
                    if (!elementInserted) {
                        elementInserted = true;
                        parentElement.insertBefore(this.element, lineRow);
                    }
                    parentElement.removeChild(lineRow);
                }
                this._textViewer._releaseLinesHighlight(lineRow);
            }
            delete this._expandedLineRows;
        }

        this._textViewer.endDomUpdates();
    },

    get height()
    {
        if (!this._expandedLineRows)
            return this._textViewer._totalHeight(this.element);
        return this._textViewer._totalHeight(this._expandedLineRows[0], this._expandedLineRows[this._expandedLineRows.length - 1]);
    },

    _createRow: function(lineNumber)
    {
        var lineRow = this._textViewer._cachedRows.pop() || document.createElement("div");
        lineRow.lineNumber = lineNumber;
        lineRow.className = "webkit-line-content";
        lineRow.textContent = this._textModel.line(lineNumber);
        if (!lineRow.textContent)
            lineRow.appendChild(document.createElement("br"));
        return lineRow;
    },

    getExpandedLineRow: function(lineNumber)
    {
        if (!this._expanded || lineNumber < this.startLine || lineNumber >= this.startLine + this.linesCount)
            return null;
        if (!this._expandedLineRows)
            return this.element;
        return this._expandedLineRows[lineNumber - this.startLine];
    }
}
