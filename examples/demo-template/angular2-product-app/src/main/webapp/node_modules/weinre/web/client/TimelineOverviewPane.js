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

WebInspector.TimelineOverviewPane = function(categories)
{
    this._categories = categories;

    this.statusBarFilters = document.createElement("div");
    this.statusBarFilters.className = "status-bar-items";
    for (var categoryName in this._categories) {
        var category = this._categories[categoryName];
        this.statusBarFilters.appendChild(this._createTimelineCategoryStatusBarCheckbox(category, this._onCheckboxClicked.bind(this, category)));
    }

    this._overviewGrid = new WebInspector.TimelineGrid();
    this._overviewGrid.element.id = "timeline-overview-grid";
    this._overviewGrid.itemsGraphsElement.id = "timeline-overview-timelines";
    this._overviewGrid.element.addEventListener("mousedown", this._dragWindow.bind(this), true);

    this._heapGraph = new WebInspector.HeapGraph();
    this._heapGraph.element.id = "timeline-overview-memory";
    this._overviewGrid.element.insertBefore(this._heapGraph.element, this._overviewGrid.itemsGraphsElement);

    this.element = this._overviewGrid.element;

    this._categoryGraphs = {};
    var i = 0;
    for (var category in this._categories) {
        var categoryGraph = new WebInspector.TimelineCategoryGraph(this._categories[category], i++ % 2);
        this._categoryGraphs[category] = categoryGraph;
        this._overviewGrid.itemsGraphsElement.appendChild(categoryGraph.graphElement);
    }
    this._overviewGrid.setScrollAndDividerTop(0, 0);

    this._overviewWindowElement = document.createElement("div");
    this._overviewWindowElement.id = "timeline-overview-window";
    this._overviewGrid.element.appendChild(this._overviewWindowElement);

    this._overviewWindowBordersElement = document.createElement("div");
    this._overviewWindowBordersElement.className = "timeline-overview-window-rulers";
    this._overviewGrid.element.appendChild(this._overviewWindowBordersElement);

    var overviewDividersBackground = document.createElement("div");
    overviewDividersBackground.className = "timeline-overview-dividers-background";
    this._overviewGrid.element.appendChild(overviewDividersBackground);

    this._leftResizeElement = document.createElement("div");
    this._leftResizeElement.className = "timeline-window-resizer";
    this._leftResizeElement.style.left = 0;
    this._overviewGrid.element.appendChild(this._leftResizeElement);

    this._rightResizeElement = document.createElement("div");
    this._rightResizeElement.className = "timeline-window-resizer timeline-window-resizer-right";
    this._rightResizeElement.style.right = 0;
    this._overviewGrid.element.appendChild(this._rightResizeElement);

    this._overviewCalculator = new WebInspector.TimelineOverviewCalculator();

    this.windowLeft = 0.0;
    this.windowRight = 1.0;
}

WebInspector.TimelineOverviewPane.minSelectableSize = 12;

WebInspector.TimelineOverviewPane.prototype = {
    showTimelines: function(event) {
        this._heapGraph.hide();
        this._overviewGrid.itemsGraphsElement.removeStyleClass("hidden");
    },

    showMemoryGraph: function(records) {
        this._heapGraph.show();
        this._heapGraph.update(records);
        this._overviewGrid.itemsGraphsElement.addStyleClass("hidden");
    },

    _onCheckboxClicked: function (category, event) {
        if (event.target.checked)
            category.hidden = false;
        else
            category.hidden = true;
        this._categoryGraphs[category.name].dimmed = !event.target.checked;
        this.dispatchEventToListeners("filter changed");
    },

    _forAllRecords: function(recordsArray, callback)
    {
        if (!recordsArray)
            return;
        for (var i = 0; i < recordsArray.length; ++i) {
            callback(recordsArray[i]);
            this._forAllRecords(recordsArray[i].children, callback);
        }
    },

    update: function(records, showShortEvents)
    {
        this._showShortEvents = showShortEvents;
        // Clear summary bars.
        var timelines = {};
        for (var category in this._categories) {
            timelines[category] = [];
            this._categoryGraphs[category].clearChunks();
        }

        // Create sparse arrays with 101 cells each to fill with chunks for a given category.
        this._overviewCalculator.reset();
        this._forAllRecords(records, this._overviewCalculator.updateBoundaries.bind(this._overviewCalculator));

        function markTimeline(record)
        {
            if (!(this._showShortEvents || record.isLong()))
                return;
            var percentages = this._overviewCalculator.computeBarGraphPercentages(record);

            var end = Math.round(percentages.end);
            var categoryName = record.category.name;
            for (var j = Math.round(percentages.start); j <= end; ++j)
                timelines[categoryName][j] = true;
        }
        this._forAllRecords(records, markTimeline.bind(this));

        // Convert sparse arrays to continuous segments, render graphs for each.
        for (var category in this._categories) {
            var timeline = timelines[category];
            window.timelineSaved = timeline;
            var chunkStart = -1;
            for (var j = 0; j < 101; ++j) {
                if (timeline[j]) {
                    if (chunkStart === -1)
                        chunkStart = j;
                } else {
                    if (chunkStart !== -1) {
                        this._categoryGraphs[category].addChunk(chunkStart, j);
                        chunkStart = -1;
                    }
                }
            }
            if (chunkStart !== -1) {
                this._categoryGraphs[category].addChunk(chunkStart, 100);
                chunkStart = -1;
            }
        }

        this._heapGraph.setSize(this._overviewGrid.element.offsetWidth, 60);
        if (this._heapGraph.visible)
            this._heapGraph.update(records);

        this._overviewGrid.updateDividers(true, this._overviewCalculator);
    },

    updateEventDividers: function(records, dividerConstructor)
    {
        this._overviewGrid.removeEventDividers();
        var dividers = [];
        for (var i = 0; i < records.length; ++i) {
            var record = records[i];
            var positions = this._overviewCalculator.computeBarGraphPercentages(record);
            var dividerPosition = Math.round(positions.start * 10);
            if (dividers[dividerPosition])
                continue;
            var divider = dividerConstructor(record);
            divider.style.left = positions.start + "%";
            dividers[dividerPosition] = divider;
        }
        this._overviewGrid.addEventDividers(dividers);
    },

    updateMainViewWidth: function(width, records)
    {
        this._overviewGrid.element.style.left = width + "px";
        this.statusBarFilters.style.left = Math.max(155, width) + "px";
    },

    reset: function()
    {
        this.windowLeft = 0.0;
        this.windowRight = 1.0;
        this._overviewWindowElement.style.left = "0%";
        this._overviewWindowElement.style.width = "100%";
        this._overviewWindowBordersElement.style.left = "0%";
        this._overviewWindowBordersElement.style.right = "0%";
        this._leftResizeElement.style.left = "0%";
        this._rightResizeElement.style.left = "100%";
        this._overviewCalculator.reset();
        this._overviewGrid.updateDividers(true, this._overviewCalculator);
    },

    _resizeWindow: function(resizeElement, event)
    {
        WebInspector.elementDragStart(resizeElement, this._windowResizeDragging.bind(this, resizeElement), this._endWindowDragging.bind(this), event, "col-resize");
    },

    _windowResizeDragging: function(resizeElement, event)
    {
        if (resizeElement === this._leftResizeElement)
            this._resizeWindowLeft(event.pageX - this._overviewGrid.element.offsetLeft);
        else
            this._resizeWindowRight(event.pageX - this._overviewGrid.element.offsetLeft);
        event.preventDefault();
    },

    _dragWindow: function(event)
    {
        var node = event.target;
        while (node) {
            if (node === this._overviewGrid._dividersLabelBarElement) {
                WebInspector.elementDragStart(this._overviewWindowElement, this._windowDragging.bind(this, event.pageX,
                    this._leftResizeElement.offsetLeft, this._rightResizeElement.offsetLeft), this._endWindowDragging.bind(this), event, "ew-resize");
                break;
            } else if (node === this._overviewGrid.element) {
                var position = event.pageX - this._overviewGrid.element.offsetLeft;
                this._overviewWindowSelector = new WebInspector.TimelinePanel.WindowSelector(this._overviewGrid.element, position, event);
                WebInspector.elementDragStart(null, this._windowSelectorDragging.bind(this), this._endWindowSelectorDragging.bind(this), event, "col-resize");
                break;
            } else if (node === this._leftResizeElement || node === this._rightResizeElement) {
                this._resizeWindow(node, event);
                break;
            }
            node = node.parentNode;
        }
    },

    _windowSelectorDragging: function(event)
    {
        this._overviewWindowSelector._updatePosition(event.pageX - this._overviewGrid.element.offsetLeft);
        event.preventDefault();
    },

    _endWindowSelectorDragging: function(event)
    {
        WebInspector.elementDragEnd(event);
        var window = this._overviewWindowSelector._close(event.pageX - this._overviewGrid.element.offsetLeft);
        delete this._overviewWindowSelector;
        if (window.end - window.start < WebInspector.TimelineOverviewPane.minSelectableSize)
            if (this._overviewGrid.itemsGraphsElement.offsetWidth - window.end > WebInspector.TimelineOverviewPane.minSelectableSize)
                window.end = window.start + WebInspector.TimelineOverviewPane.minSelectableSize;
            else
                window.start = window.end - WebInspector.TimelineOverviewPane.minSelectableSize;
        this._setWindowPosition(window.start, window.end);
    },

    _windowDragging: function(startX, windowLeft, windowRight, event)
    {
        var delta = event.pageX - startX;
        var start = windowLeft + delta;
        var end = windowRight + delta;
        var windowSize = windowRight - windowLeft;

        if (start < 0) {
            start = 0;
            end = windowSize;
        }

        if (end > this._overviewGrid.element.clientWidth) {
            end = this._overviewGrid.element.clientWidth;
            start = end - windowSize;
        }
        this._setWindowPosition(start, end);

        event.preventDefault();
    },

    _resizeWindowLeft: function(start)
    {
        // Glue to edge.
        if (start < 10)
            start = 0;
        else if (start > this._rightResizeElement.offsetLeft -  4)
            start = this._rightResizeElement.offsetLeft - 4;
        this._setWindowPosition(start, null);
    },

    _resizeWindowRight: function(end)
    {
        // Glue to edge.
        if (end > this._overviewGrid.element.clientWidth - 10)
            end = this._overviewGrid.element.clientWidth;
        else if (end < this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.minSelectableSize)
            end = this._leftResizeElement.offsetLeft + WebInspector.TimelineOverviewPane.minSelectableSize;
        this._setWindowPosition(null, end);
    },

    _setWindowPosition: function(start, end)
    {
        var rulerAdjustment = 1 / this._overviewGrid.element.clientWidth;
        if (typeof start === "number") {
            this.windowLeft = start / this._overviewGrid.element.clientWidth;
            this._leftResizeElement.style.left = this.windowLeft * 100 + "%";
            this._overviewWindowElement.style.left = this.windowLeft * 100 + "%";
            this._overviewWindowBordersElement.style.left = (this.windowLeft - rulerAdjustment) * 100 + "%";
        }
        if (typeof end === "number") {
            this.windowRight = end / this._overviewGrid.element.clientWidth;
            this._rightResizeElement.style.left = this.windowRight * 100 + "%";
        }
        this._overviewWindowElement.style.width = (this.windowRight - this.windowLeft) * 100 + "%";
        this._overviewWindowBordersElement.style.right = (1 - this.windowRight + 2 * rulerAdjustment) * 100 + "%";
        this.dispatchEventToListeners("window changed");
    },

    _endWindowDragging: function(event)
    {
        WebInspector.elementDragEnd(event);
    },

    _createTimelineCategoryStatusBarCheckbox: function(category, onCheckboxClicked)
    {
        var labelContainer = document.createElement("div");
        labelContainer.addStyleClass("timeline-category-statusbar-item");
        labelContainer.addStyleClass("timeline-category-" + category.name);
        labelContainer.addStyleClass("status-bar-item");

        var label = document.createElement("label");
        var checkElement = document.createElement("input");
        checkElement.type = "checkbox";
        checkElement.className = "timeline-category-checkbox";
        checkElement.checked = true;
        checkElement.addEventListener("click", onCheckboxClicked);
        label.appendChild(checkElement);

        var typeElement = document.createElement("span");
        typeElement.className = "type";
        typeElement.textContent = category.title;
        label.appendChild(typeElement);

        labelContainer.appendChild(label);
        return labelContainer;
    }

}

WebInspector.TimelineOverviewPane.prototype.__proto__ = WebInspector.Object.prototype;


WebInspector.TimelineOverviewCalculator = function()
{
}

WebInspector.TimelineOverviewCalculator.prototype = {
    computeBarGraphPercentages: function(record)
    {
        var start = (record.startTime - this.minimumBoundary) / this.boundarySpan * 100;
        var end = (record.endTime - this.minimumBoundary) / this.boundarySpan * 100;
        return {start: start, end: end};
    },

    reset: function()
    {
        delete this.minimumBoundary;
        delete this.maximumBoundary;
    },

    updateBoundaries: function(record)
    {
        if (typeof this.minimumBoundary === "undefined" || record.startTime < this.minimumBoundary) {
            this.minimumBoundary = record.startTime;
            return true;
        }
        if (typeof this.maximumBoundary === "undefined" || record.endTime > this.maximumBoundary) {
            this.maximumBoundary = record.endTime;
            return true;
        }
        return false;
    },

    get boundarySpan()
    {
        return this.maximumBoundary - this.minimumBoundary;
    },

    formatValue: function(value)
    {
        return Number.secondsToString(value);
    }
}


WebInspector.TimelineCategoryGraph = function(category, isEven)
{
    this._category = category;

    this._graphElement = document.createElement("div");
    this._graphElement.className = "timeline-graph-side timeline-overview-graph-side" + (isEven ? " even" : "");

    this._barAreaElement = document.createElement("div");
    this._barAreaElement.className = "timeline-graph-bar-area timeline-category-" + category.name;
    this._graphElement.appendChild(this._barAreaElement);
}

WebInspector.TimelineCategoryGraph.prototype = {
    get graphElement()
    {
        return this._graphElement;
    },

    addChunk: function(start, end)
    {
        var chunk = document.createElement("div");
        chunk.className = "timeline-graph-bar";
        this._barAreaElement.appendChild(chunk);
        chunk.style.setProperty("left", start + "%");
        chunk.style.setProperty("width", (end - start) + "%");
    },

    clearChunks: function()
    {
        this._barAreaElement.removeChildren();
    },

    set dimmed(dimmed)
    {
        if (dimmed)
            this._barAreaElement.removeStyleClass("timeline-category-" + this._category.name);
        else
            this._barAreaElement.addStyleClass("timeline-category-" + this._category.name);
    }
}

WebInspector.TimelinePanel.WindowSelector = function(parent, position, event)
{
    this._startPosition = position;
    this._width = parent.offsetWidth;
    this._windowSelector = document.createElement("div");
    this._windowSelector.className = "timeline-window-selector";
    this._windowSelector.style.left = this._startPosition + "px";
    this._windowSelector.style.right = this._width - this._startPosition +  + "px";
    parent.appendChild(this._windowSelector);
}

WebInspector.TimelinePanel.WindowSelector.prototype = {
    _createSelectorElement: function(parent, left, width, height)
    {
        var selectorElement = document.createElement("div");
        selectorElement.className = "timeline-window-selector";
        selectorElement.style.left = left + "px";
        selectorElement.style.width = width + "px";
        selectorElement.style.top = "0px";
        selectorElement.style.height = height + "px";
        parent.appendChild(selectorElement);
        return selectorElement;
    },

    _close: function(position)
    {
        position = Math.max(0, Math.min(position, this._width));
        this._windowSelector.parentNode.removeChild(this._windowSelector);
        return this._startPosition < position ? {start: this._startPosition, end: position} : {start: position, end: this._startPosition};
    },

    _updatePosition: function(position)
    {
        position = Math.max(0, Math.min(position, this._width));
        if (position < this._startPosition) {
            this._windowSelector.style.left = position + "px";
            this._windowSelector.style.right = this._width - this._startPosition + "px";
        } else {
            this._windowSelector.style.left = this._startPosition + "px";
            this._windowSelector.style.right = this._width - position + "px";
        }
    }
}

WebInspector.HeapGraph = function() {
    this._canvas = document.createElement("canvas");

    this._maxHeapSizeLabel = document.createElement("div");
    this._maxHeapSizeLabel.addStyleClass("memory-graph-label");

    this._element = document.createElement("div");
    this._element.addStyleClass("hidden");
    this._element.appendChild(this._canvas);
    this._element.appendChild(this._maxHeapSizeLabel);
}

WebInspector.HeapGraph.prototype = {
    get element() {
    //    return this._canvas;
        return this._element;
    },

    get visible() {
        return !this.element.hasStyleClass("hidden");
    },

    show: function() {
        this.element.removeStyleClass("hidden");
    },

    hide: function() {
        this.element.addStyleClass("hidden");
    },

    setSize: function(w, h) {
        this._canvas.width = w;
        this._canvas.height = h - 5;
    },

    update: function(records)
    {
        if (!records.length)
            return;

        var maxTotalHeapSize = 0;
        var minTime;
        var maxTime;
        this._forAllRecords(records, function(r) {
            if (r.totalHeapSize && r.totalHeapSize > maxTotalHeapSize)
                maxTotalHeapSize = r.totalHeapSize;

            if (typeof minTime === "undefined" || r.startTime < minTime)
                minTime = r.startTime;
            if (typeof maxTime === "undefined" || r.endTime > maxTime)
                maxTime = r.endTime;
        });

        var width = this._canvas.width;
        var height = this._canvas.height;
        var xFactor = width / (maxTime - minTime);
        var yFactor = height / maxTotalHeapSize;

        var histogram = new Array(width);
        this._forAllRecords(records, function(r) {
            if (!r.usedHeapSize)
                return;
             var x = Math.round((r.endTime - minTime) * xFactor);
             var y = Math.round(r.usedHeapSize * yFactor);
             histogram[x] = Math.max(histogram[x] || 0, y);
        });

        var ctx = this._canvas.getContext("2d");
        this._clear(ctx);

        // +1 so that the border always fit into the canvas area.
        height = height + 1;

        ctx.beginPath();
        var initialY = 0;
        for (var k = 0; k < histogram.length; k++) {
            if (histogram[k]) {
                initialY = histogram[k];
                break;
            }
        }
        ctx.moveTo(0, height - initialY);

        for (var x = 0; x < histogram.length; x++) {
             if (!histogram[x])
                 continue;
             ctx.lineTo(x, height - histogram[x]);
        }

        ctx.lineWidth = 0.5;
        ctx.strokeStyle = "rgba(20,0,0,0.8)";
        ctx.stroke();

        ctx.fillStyle = "rgba(214,225,254, 0.8);";
        ctx.lineTo(width, 60);
        ctx.lineTo(0, 60);
        ctx.lineTo(0, height - initialY);
        ctx.fill();
        ctx.closePath();

        this._maxHeapSizeLabel.textContent = Number.bytesToString(maxTotalHeapSize);
    },

    _clear: function(ctx) {
        ctx.fillStyle = "rgba(255,255,255,0.8)";
        ctx.fillRect(0, 0, this._canvas.width, this._canvas.height);
    },

    _forAllRecords: WebInspector.TimelineOverviewPane.prototype._forAllRecords
}
