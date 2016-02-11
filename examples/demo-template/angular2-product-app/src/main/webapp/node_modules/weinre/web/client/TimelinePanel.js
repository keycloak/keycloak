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

WebInspector.TimelinePanel = function()
{
    WebInspector.Panel.call(this, "timeline");

    this.element.appendChild(this._createTopPane());
    this.element.tabIndex = 0;

    this._sidebarBackgroundElement = document.createElement("div");
    this._sidebarBackgroundElement.className = "sidebar timeline-sidebar-background";
    this.element.appendChild(this._sidebarBackgroundElement);

    this._containerElement = document.createElement("div");
    this._containerElement.id = "timeline-container";
    this._containerElement.addEventListener("scroll", this._onScroll.bind(this), false);
    this.element.appendChild(this._containerElement);

    this.createSidebar(this._containerElement, this._containerElement);
    var itemsTreeElement = new WebInspector.SidebarSectionTreeElement(WebInspector.UIString("RECORDS"), {}, true);
    itemsTreeElement.expanded = true;
    this.sidebarTree.appendChild(itemsTreeElement);

    this._sidebarListElement = document.createElement("div");
    this.sidebarElement.appendChild(this._sidebarListElement);

    this._containerContentElement = document.createElement("div");
    this._containerContentElement.id = "resources-container-content";
    this._containerElement.appendChild(this._containerContentElement);

    this._timelineGrid = new WebInspector.TimelineGrid();
    this._itemsGraphsElement = this._timelineGrid.itemsGraphsElement;
    this._itemsGraphsElement.id = "timeline-graphs";
    this._containerContentElement.appendChild(this._timelineGrid.element);

    this._topGapElement = document.createElement("div");
    this._topGapElement.className = "timeline-gap";
    this._itemsGraphsElement.appendChild(this._topGapElement);

    this._graphRowsElement = document.createElement("div");
    this._itemsGraphsElement.appendChild(this._graphRowsElement);

    this._bottomGapElement = document.createElement("div");
    this._bottomGapElement.className = "timeline-gap";
    this._itemsGraphsElement.appendChild(this._bottomGapElement);

    this._expandElements = document.createElement("div");
    this._expandElements.id = "orphan-expand-elements";
    this._itemsGraphsElement.appendChild(this._expandElements);

    this._rootRecord = this._createRootRecord();
    this._sendRequestRecords = {};
    this._scheduledResourceRequests = {};
    this._timerRecords = {};

    this._calculator = new WebInspector.TimelineCalculator();
    this._calculator._showShortEvents = false;
    var shortRecordThresholdTitle = Number.secondsToString(WebInspector.TimelinePanel.shortRecordThreshold);
    this._showShortRecordsTitleText = WebInspector.UIString("Show the records that are shorter than %s", shortRecordThresholdTitle);
    this._hideShortRecordsTitleText = WebInspector.UIString("Hide the records that are shorter than %s", shortRecordThresholdTitle);
    this._createStatusbarButtons();

    this._boundariesAreValid = true;
    this._scrollTop = 0;

    this._popoverHelper = new WebInspector.PopoverHelper(this._containerElement, this._getPopoverAnchor.bind(this), this._showPopover.bind(this), true);

    // Disable short events filter by default.
    this.toggleFilterButton.toggled = true;
    this._calculator._showShortEvents = this.toggleFilterButton.toggled;
    this._markTimelineRecords = [];
    this._expandOffset = 15;

    InspectorBackend.registerDomainDispatcher("Timeline", new WebInspector.TimelineDispatcher(this));
}

// Define row height, should be in sync with styles for timeline graphs.
WebInspector.TimelinePanel.rowHeight = 18;
WebInspector.TimelinePanel.shortRecordThreshold = 0.015;

WebInspector.TimelinePanel.prototype = {
    _createTopPane: function() {
        var topPaneElement = document.createElement("div");
        topPaneElement.id = "timeline-overview-panel";

        this._topPaneSidebarElement = document.createElement("div");
        this._topPaneSidebarElement.id = "timeline-overview-sidebar";

        var overviewTreeElement = document.createElement("ol");
        overviewTreeElement.className = "sidebar-tree";
        this._topPaneSidebarElement.appendChild(overviewTreeElement);
        topPaneElement.appendChild(this._topPaneSidebarElement);

        var topPaneSidebarTree = new TreeOutline(overviewTreeElement);
        var timelinesOverviewItem = new WebInspector.SidebarTreeElement("resources-time-graph-sidebar-item", WebInspector.UIString("Timelines"));
        topPaneSidebarTree.appendChild(timelinesOverviewItem);
        timelinesOverviewItem.onselect = this._timelinesOverviewItemSelected.bind(this);
        timelinesOverviewItem.select(true);

        var memoryOverviewItem = new WebInspector.SidebarTreeElement("resources-size-graph-sidebar-item", WebInspector.UIString("Memory"));
        topPaneSidebarTree.appendChild(memoryOverviewItem);
        memoryOverviewItem.onselect = this._memoryOverviewItemSelected.bind(this);

        this._overviewPane = new WebInspector.TimelineOverviewPane(this.categories);
        this._overviewPane.addEventListener("window changed", this._windowChanged, this);
        this._overviewPane.addEventListener("filter changed", this._refresh, this);
        topPaneElement.appendChild(this._overviewPane.element);

        var separatorElement = document.createElement("div");
        separatorElement.id = "timeline-overview-separator";
        topPaneElement.appendChild(separatorElement);
        return topPaneElement;
    },

    get toolbarItemLabel()
    {
        return WebInspector.UIString("Timeline");
    },

    get statusBarItems()
    {
        return [this.toggleFilterButton.element, this.toggleTimelineButton.element, this.clearButton.element, this._overviewPane.statusBarFilters];
    },

    get categories()
    {
        if (!this._categories) {
            this._categories = {
                loading: new WebInspector.TimelineCategory("loading", WebInspector.UIString("Loading"), "rgb(47,102,236)"),
                scripting: new WebInspector.TimelineCategory("scripting", WebInspector.UIString("Scripting"), "rgb(157,231,119)"),
                rendering: new WebInspector.TimelineCategory("rendering", WebInspector.UIString("Rendering"), "rgb(164,60,255)")
            };
        }
        return this._categories;
    },

    get defaultFocusedElement()
    {
        return this.element;
    },

    get _recordStyles()
    {
        if (!this._recordStylesArray) {
            var recordTypes = WebInspector.TimelineAgent.RecordType;
            var recordStyles = {};
            recordStyles[recordTypes.EventDispatch] = { title: WebInspector.UIString("Event"), category: this.categories.scripting };
            recordStyles[recordTypes.Layout] = { title: WebInspector.UIString("Layout"), category: this.categories.rendering };
            recordStyles[recordTypes.RecalculateStyles] = { title: WebInspector.UIString("Recalculate Style"), category: this.categories.rendering };
            recordStyles[recordTypes.Paint] = { title: WebInspector.UIString("Paint"), category: this.categories.rendering };
            recordStyles[recordTypes.ParseHTML] = { title: WebInspector.UIString("Parse"), category: this.categories.loading };
            recordStyles[recordTypes.TimerInstall] = { title: WebInspector.UIString("Install Timer"), category: this.categories.scripting };
            recordStyles[recordTypes.TimerRemove] = { title: WebInspector.UIString("Remove Timer"), category: this.categories.scripting };
            recordStyles[recordTypes.TimerFire] = { title: WebInspector.UIString("Timer Fired"), category: this.categories.scripting };
            recordStyles[recordTypes.XHRReadyStateChange] = { title: WebInspector.UIString("XHR Ready State Change"), category: this.categories.scripting };
            recordStyles[recordTypes.XHRLoad] = { title: WebInspector.UIString("XHR Load"), category: this.categories.scripting };
            recordStyles[recordTypes.EvaluateScript] = { title: WebInspector.UIString("Evaluate Script"), category: this.categories.scripting };
            recordStyles[recordTypes.MarkTimeline] = { title: WebInspector.UIString("Mark"), category: this.categories.scripting };
            recordStyles[recordTypes.ResourceSendRequest] = { title: WebInspector.UIString("Send Request"), category: this.categories.loading };
            recordStyles[recordTypes.ResourceReceiveResponse] = { title: WebInspector.UIString("Receive Response"), category: this.categories.loading };
            recordStyles[recordTypes.ResourceFinish] = { title: WebInspector.UIString("Finish Loading"), category: this.categories.loading };
            recordStyles[recordTypes.FunctionCall] = { title: WebInspector.UIString("Function Call"), category: this.categories.scripting };
            recordStyles[recordTypes.ResourceReceiveData] = { title: WebInspector.UIString("Receive Data"), category: this.categories.loading };
            recordStyles[recordTypes.GCEvent] = { title: WebInspector.UIString("GC Event"), category: this.categories.scripting };
            recordStyles[recordTypes.MarkDOMContentEventType] = { title: WebInspector.UIString("DOMContent event"), category: this.categories.scripting };
            recordStyles[recordTypes.MarkLoadEventType] = { title: WebInspector.UIString("Load event"), category: this.categories.scripting };
            recordStyles[recordTypes.ScheduleResourceRequest] = { title: WebInspector.UIString("Schedule Request"), category: this.categories.loading };
            this._recordStylesArray = recordStyles;
        }
        return this._recordStylesArray;
    },

    _createStatusbarButtons: function()
    {
        this.toggleTimelineButton = new WebInspector.StatusBarButton(WebInspector.UIString("Record"), "record-profile-status-bar-item");
        this.toggleTimelineButton.addEventListener("click", this._toggleTimelineButtonClicked.bind(this), false);

        this.clearButton = new WebInspector.StatusBarButton(WebInspector.UIString("Clear"), "clear-status-bar-item");
        this.clearButton.addEventListener("click", this._clearPanel.bind(this), false);

        this.toggleFilterButton = new WebInspector.StatusBarButton(this._hideShortRecordsTitleText, "timeline-filter-status-bar-item");
        this.toggleFilterButton.addEventListener("click", this._toggleFilterButtonClicked.bind(this), false);

        this.recordsCounter = document.createElement("span");
        this.recordsCounter.className = "timeline-records-counter";
    },

    _updateRecordsCounter: function()
    {
        this.recordsCounter.textContent = WebInspector.UIString("%d of %d captured records are visible", this._rootRecord._visibleRecordsCount, this._rootRecord._allRecordsCount);
    },

    _updateEventDividers: function()
    {
        this._timelineGrid.removeEventDividers();
        var clientWidth = this._graphRowsElement.offsetWidth - this._expandOffset;
        var dividers = [];
        for (var i = 0; i < this._markTimelineRecords.length; ++i) {
            var record = this._markTimelineRecords[i];
            var positions = this._calculator.computeBarGraphWindowPosition(record, clientWidth);
            var dividerPosition = Math.round(positions.left);
            if (dividerPosition < 0 || dividerPosition >= clientWidth || dividers[dividerPosition])
                continue;
            var divider = this._createEventDivider(record);
            divider.style.left = (dividerPosition + this._expandOffset) + "px";
            dividers[dividerPosition] = divider;
        }
        this._timelineGrid.addEventDividers(dividers);
        this._overviewPane.updateEventDividers(this._markTimelineRecords, this._createEventDivider.bind(this));
    },

    _createEventDivider: function(record)
    {
        var eventDivider = document.createElement("div");
        eventDivider.className = "resources-event-divider";
        var recordTypes = WebInspector.TimelineAgent.RecordType;

        var eventDividerPadding = document.createElement("div");
        eventDividerPadding.className = "resources-event-divider-padding";
        eventDividerPadding.title = record.title;

        if (record.type === recordTypes.MarkDOMContentEventType)
            eventDivider.className += " resources-blue-divider";
        else if (record.type === recordTypes.MarkLoadEventType)
            eventDivider.className += " resources-red-divider";
        else if (record.type === recordTypes.MarkTimeline) {
            eventDivider.className += " resources-orange-divider";
            eventDividerPadding.title = record.data.message;
        }
        eventDividerPadding.appendChild(eventDivider);
        return eventDividerPadding;
    },

    _timelinesOverviewItemSelected: function(event) {
        this._overviewPane.showTimelines();
    },

    _memoryOverviewItemSelected: function(event) {
        this._overviewPane.showMemoryGraph(this._rootRecord.children);
    },

    _toggleTimelineButtonClicked: function()
    {
        if (this.toggleTimelineButton.toggled)
            InspectorBackend.stopTimelineProfiler();
        else {
            this._clearPanel();
            InspectorBackend.startTimelineProfiler();
        }
    },

    _toggleFilterButtonClicked: function()
    {
        this.toggleFilterButton.toggled = !this.toggleFilterButton.toggled;
        this._calculator._showShortEvents = this.toggleFilterButton.toggled;
        this.toggleFilterButton.element.title = this._calculator._showShortEvents ? this._hideShortRecordsTitleText : this._showShortRecordsTitleText;
        this._scheduleRefresh(true);
    },

    _timelineProfilerWasStarted: function()
    {
        this.toggleTimelineButton.toggled = true;
    },

    _timelineProfilerWasStopped: function()
    {
        this.toggleTimelineButton.toggled = false;
    },

    _addRecordToTimeline: function(record)
    {
        if (record.type == WebInspector.TimelineAgent.RecordType.ResourceSendRequest) {
            var isMainResource = (record.data.identifier === WebInspector.mainResource.identifier);
            if (isMainResource && this._mainResourceIdentifier !== record.data.identifier) {
                // We are loading new main resource -> clear the panel. Check above is necessary since
                // there may be several resource loads with main resource marker upon redirects, redirects are reported with
                // the original identifier.
                this._mainResourceIdentifier = record.data.identifier;
                this._clearPanel();
            }
        }
        this._innerAddRecordToTimeline(record, this._rootRecord);
        this._scheduleRefresh();
    },

    _findParentRecord: function(record)
    {
        var recordTypes = WebInspector.TimelineAgent.RecordType;
        var parentRecord;
        if (record.type === recordTypes.ResourceReceiveResponse ||
            record.type === recordTypes.ResourceFinish ||
            record.type === recordTypes.ResourceReceiveData)
            parentRecord = this._sendRequestRecords[record.data.identifier];
        else if (record.type === recordTypes.TimerFire)
            parentRecord = this._timerRecords[record.data.timerId];
        else if (record.type === recordTypes.ResourceSendRequest)
            parentRecord = this._scheduledResourceRequests[record.data.url];
        return parentRecord;
    },

    _innerAddRecordToTimeline: function(record, parentRecord)
    {
        var connectedToOldRecord = false;
        var recordTypes = WebInspector.TimelineAgent.RecordType;
        if (record.type === recordTypes.MarkDOMContentEventType || record.type === recordTypes.MarkLoadEventType)
            parentRecord = null; // No bar entry for load events.
        else if (parentRecord === this._rootRecord) {
            var newParentRecord = this._findParentRecord(record);
            if (newParentRecord) {
                parentRecord = newParentRecord;
                connectedToOldRecord = true;
            }
        }

        if (record.type == recordTypes.TimerFire && record.children && record.children.length) {
            var childRecord = record.children[0];
            if (childRecord.type === recordTypes.FunctionCall) {
                record.data.scriptName = childRecord.data.scriptName;
                record.data.scriptLine = childRecord.data.scriptLine;
                record.children.shift();
                record.children = childRecord.children.concat(record.children);
            }
        }

        var formattedRecord = new WebInspector.TimelinePanel.FormattedRecord(record, parentRecord, this);

        if (record.type === recordTypes.MarkDOMContentEventType || record.type === recordTypes.MarkLoadEventType) {
            this._markTimelineRecords.push(formattedRecord);
            return;
        }

        ++this._rootRecord._allRecordsCount;
        formattedRecord.collapsed = (parentRecord === this._rootRecord);

        var childrenCount = record.children ? record.children.length : 0;
        for (var i = 0; i < childrenCount; ++i)
            this._innerAddRecordToTimeline(record.children[i], formattedRecord);

        formattedRecord._calculateAggregatedStats(this.categories);

        if (connectedToOldRecord) {
            var record = formattedRecord;
            do {
                var parent = record.parent;
                parent._cpuTime += formattedRecord._cpuTime;
                if (parent._lastChildEndTime < record._lastChildEndTime)
                    parent._lastChildEndTime = record._lastChildEndTime;
                for (var category in formattedRecord._aggregatedStats)
                    parent._aggregatedStats[category] += formattedRecord._aggregatedStats[category];
                record = parent;
            } while (record.parent);
        } else
            if (parentRecord !== this._rootRecord)
                parentRecord._selfTime -= formattedRecord.endTime - formattedRecord.startTime;

        // Keep bar entry for mark timeline since nesting might be interesting to the user.
        if (record.type === recordTypes.MarkTimeline)
            this._markTimelineRecords.push(formattedRecord);
    },

    setSidebarWidth: function(width)
    {
        WebInspector.Panel.prototype.setSidebarWidth.call(this, width);
        this._sidebarBackgroundElement.style.width = width + "px";
        this._topPaneSidebarElement.style.width = width + "px";
    },

    updateMainViewWidth: function(width)
    {
        this._containerContentElement.style.left = width + "px";
        this._scheduleRefresh();
        this._overviewPane.updateMainViewWidth(width);
    },

    resize: function()
    {
        this._closeRecordDetails();
        this._scheduleRefresh();
    },

    _createRootRecord: function()
    {
        var rootRecord = {};
        rootRecord.children = [];
        rootRecord._visibleRecordsCount = 0;
        rootRecord._allRecordsCount = 0;
        rootRecord._aggregatedStats = {};
        return rootRecord;
    },

    _clearPanel: function()
    {
        this._markTimelineRecords = [];
        this._sendRequestRecords = {};
        this._scheduledResourceRequests = {};
        this._timerRecords = {};
        this._rootRecord = this._createRootRecord();
        this._boundariesAreValid = false;
        this._overviewPane.reset();
        this._adjustScrollPosition(0);
        this._refresh();
        this._closeRecordDetails();
    },

    show: function()
    {
        WebInspector.Panel.prototype.show.call(this);
        if (typeof this._scrollTop === "number")
            this._containerElement.scrollTop = this._scrollTop;
        this._refresh();
        WebInspector.drawer.currentPanelCounters = this.recordsCounter;
    },

    hide: function()
    {
        WebInspector.Panel.prototype.hide.call(this);
        this._closeRecordDetails();
        WebInspector.drawer.currentPanelCounters = null;
    },

    _onScroll: function(event)
    {
        this._closeRecordDetails();
        var scrollTop = this._containerElement.scrollTop;
        var dividersTop = Math.max(0, scrollTop);
        this._timelineGrid.setScrollAndDividerTop(scrollTop, dividersTop);
        this._scheduleRefresh(true);
    },

    _windowChanged: function()
    {
        this._closeRecordDetails();
        this._scheduleRefresh();
    },

    _scheduleRefresh: function(preserveBoundaries)
    {
        this._closeRecordDetails();
        this._boundariesAreValid &= preserveBoundaries;

        if (!this.visible)
            return;

        if (preserveBoundaries)
            this._refresh();
        else
            if (!this._refreshTimeout)
                this._refreshTimeout = setTimeout(this._refresh.bind(this), 100);
    },

    _refresh: function()
    {
        if (this._refreshTimeout) {
            clearTimeout(this._refreshTimeout);
            delete this._refreshTimeout;
        }

        this._overviewPane.update(this._rootRecord.children, this._calculator._showShortEvents);
        this._refreshRecords(!this._boundariesAreValid);
        this._updateRecordsCounter();
        if(!this._boundariesAreValid)
            this._updateEventDividers();
        this._boundariesAreValid = true;
    },

    _updateBoundaries: function()
    {
        this._calculator.reset();
        this._calculator.windowLeft = this._overviewPane.windowLeft;
        this._calculator.windowRight = this._overviewPane.windowRight;

        for (var i = 0; i < this._rootRecord.children.length; ++i)
            this._calculator.updateBoundaries(this._rootRecord.children[i]);

        this._calculator.calculateWindow();
    },

    _addToRecordsWindow: function(record, recordsWindow, parentIsCollapsed)
    {
        if (!this._calculator._showShortEvents && !record.isLong())
            return;
        var percentages = this._calculator.computeBarGraphPercentages(record);
        if (percentages.start < 100 && percentages.endWithChildren >= 0 && !record.category.hidden) {
            ++this._rootRecord._visibleRecordsCount;
            ++record.parent._invisibleChildrenCount;
            if (!parentIsCollapsed)
                recordsWindow.push(record);
        }

        var index = recordsWindow.length;
        record._invisibleChildrenCount = 0;
        for (var i = 0; i < record.children.length; ++i)
            this._addToRecordsWindow(record.children[i], recordsWindow, parentIsCollapsed || record.collapsed);
        record._visibleChildrenCount = recordsWindow.length - index;
    },

    _filterRecords: function()
    {
        var recordsInWindow = [];
        this._rootRecord._visibleRecordsCount = 0;
        for (var i = 0; i < this._rootRecord.children.length; ++i)
            this._addToRecordsWindow(this._rootRecord.children[i], recordsInWindow);
        return recordsInWindow;
    },

    _refreshRecords: function(updateBoundaries)
    {
        if (updateBoundaries)
            this._updateBoundaries();

        var recordsInWindow = this._filterRecords();

        // Calculate the visible area.
        this._scrollTop = this._containerElement.scrollTop;
        var visibleTop = this._scrollTop;
        var visibleBottom = visibleTop + this._containerElement.clientHeight;

        var rowHeight = WebInspector.TimelinePanel.rowHeight;

        // Convert visible area to visible indexes. Always include top-level record for a visible nested record.
        var startIndex = Math.max(0, Math.min(Math.floor(visibleTop / rowHeight) - 1, recordsInWindow.length - 1));
        var endIndex = Math.min(recordsInWindow.length, Math.ceil(visibleBottom / rowHeight));

        // Resize gaps first.
        var top = (startIndex * rowHeight) + "px";
        this._topGapElement.style.height = top;
        this.sidebarElement.style.top = top;
        this.sidebarResizeElement.style.top = top;
        this._bottomGapElement.style.height = (recordsInWindow.length - endIndex) * rowHeight + "px";

        // Update visible rows.
        var listRowElement = this._sidebarListElement.firstChild;
        var width = this._graphRowsElement.offsetWidth;
        this._itemsGraphsElement.removeChild(this._graphRowsElement);
        var graphRowElement = this._graphRowsElement.firstChild;
        var scheduleRefreshCallback = this._scheduleRefresh.bind(this, true);
        this._itemsGraphsElement.removeChild(this._expandElements);
        this._expandElements.removeChildren();

        for (var i = 0; i < endIndex; ++i) {
            var record = recordsInWindow[i];
            var isEven = !(i % 2);

            if (i < startIndex) {
                var lastChildIndex = i + record._visibleChildrenCount;
                if (lastChildIndex >= startIndex && lastChildIndex < endIndex) {
                    var expandElement = new WebInspector.TimelineExpandableElement(this._expandElements);
                    expandElement._update(record, i, this._calculator.computeBarGraphWindowPosition(record, width - this._expandOffset));
                }
            } else {
                if (!listRowElement) {
                    listRowElement = new WebInspector.TimelineRecordListRow().element;
                    this._sidebarListElement.appendChild(listRowElement);
                }
                if (!graphRowElement) {
                    graphRowElement = new WebInspector.TimelineRecordGraphRow(this._itemsGraphsElement, scheduleRefreshCallback, rowHeight).element;
                    this._graphRowsElement.appendChild(graphRowElement);
                }

                listRowElement.row.update(record, isEven, this._calculator, visibleTop);
                graphRowElement.row.update(record, isEven, this._calculator, width, this._expandOffset, i);

                listRowElement = listRowElement.nextSibling;
                graphRowElement = graphRowElement.nextSibling;
            }
        }

        // Remove extra rows.
        while (listRowElement) {
            var nextElement = listRowElement.nextSibling;
            listRowElement.row.dispose();
            listRowElement = nextElement;
        }
        while (graphRowElement) {
            var nextElement = graphRowElement.nextSibling;
            graphRowElement.row.dispose();
            graphRowElement = nextElement;
        }

        this._itemsGraphsElement.insertBefore(this._graphRowsElement, this._bottomGapElement);
        this._itemsGraphsElement.appendChild(this._expandElements);
        this.sidebarResizeElement.style.height = this.sidebarElement.clientHeight + "px";
        // Reserve some room for expand / collapse controls to the left for records that start at 0ms.
        var timelinePaddingLeft = this._calculator.windowLeft === 0 ? this._expandOffset : 0;
        if (updateBoundaries)
            this._timelineGrid.updateDividers(true, this._calculator, timelinePaddingLeft);
        this._adjustScrollPosition((recordsInWindow.length + 1) * rowHeight);
    },

    _adjustScrollPosition: function(totalHeight)
    {
        // Prevent the container from being scrolled off the end.
        if ((this._containerElement.scrollTop + this._containerElement.offsetHeight) > totalHeight + 1)
            this._containerElement.scrollTop = (totalHeight - this._containerElement.offsetHeight);
    },

    _getPopoverAnchor: function(element)
    {
        return element.enclosingNodeOrSelfWithClass("timeline-graph-bar") || element.enclosingNodeOrSelfWithClass("timeline-tree-item");
    },

    _showPopover: function(anchor)
    {
        var record = anchor.row._record;
        var popover = new WebInspector.Popover(record._generatePopupContent(this._calculator, this.categories));
        popover.show(anchor);
        return popover;
    },

    _closeRecordDetails: function()
    {
        this._popoverHelper.hidePopup();
    }
}

WebInspector.TimelinePanel.prototype.__proto__ = WebInspector.Panel.prototype;

WebInspector.TimelineDispatcher = function(timelinePanel)
{
    this._timelinePanel = timelinePanel;
}

WebInspector.TimelineDispatcher.prototype = {
    timelineProfilerWasStarted: function()
    {
        this._timelinePanel._timelineProfilerWasStarted();
    },

    timelineProfilerWasStopped: function()
    {
        this._timelinePanel._timelineProfilerWasStopped();
    },

    addRecordToTimeline: function(record)
    {
        this._timelinePanel._addRecordToTimeline(record);
    }
}

WebInspector.TimelineCategory = function(name, title, color)
{
    this.name = name;
    this.title = title;
    this.color = color;
}

WebInspector.TimelineCalculator = function()
{
    this.reset();
    this.windowLeft = 0.0;
    this.windowRight = 1.0;
}

WebInspector.TimelineCalculator.prototype = {
    computeBarGraphPercentages: function(record)
    {
        var start = (record.startTime - this.minimumBoundary) / this.boundarySpan * 100;
        var end = (record.startTime + record._selfTime - this.minimumBoundary) / this.boundarySpan * 100;
        var endWithChildren = (record._lastChildEndTime - this.minimumBoundary) / this.boundarySpan * 100;
        var cpuWidth = record._cpuTime / this.boundarySpan * 100;
        return {start: start, end: end, endWithChildren: endWithChildren, cpuWidth: cpuWidth};
    },

    computeBarGraphWindowPosition: function(record, clientWidth)
    {
        var minWidth = 5;
        var borderWidth = 4;
        var workingArea = clientWidth - minWidth - borderWidth;
        var percentages = this.computeBarGraphPercentages(record);
        var left = percentages.start / 100 * workingArea;
        var width = (percentages.end - percentages.start) / 100 * workingArea + minWidth;
        var widthWithChildren =  (percentages.endWithChildren - percentages.start) / 100 * workingArea;
        var cpuWidth = percentages.cpuWidth / 100 * workingArea + minWidth;
        if (percentages.endWithChildren > percentages.end)
            widthWithChildren += borderWidth + minWidth;
        return {left: left, width: width, widthWithChildren: widthWithChildren, cpuWidth: cpuWidth};
    },

    calculateWindow: function()
    {
        this.minimumBoundary = this._absoluteMinimumBoundary + this.windowLeft * (this._absoluteMaximumBoundary - this._absoluteMinimumBoundary);
        this.maximumBoundary = this._absoluteMinimumBoundary + this.windowRight * (this._absoluteMaximumBoundary - this._absoluteMinimumBoundary);
        this.boundarySpan = this.maximumBoundary - this.minimumBoundary;
    },

    reset: function()
    {
        this._absoluteMinimumBoundary = -1;
        this._absoluteMaximumBoundary = -1;
    },

    updateBoundaries: function(record)
    {
        var lowerBound = record.startTime;
        if (this._absoluteMinimumBoundary === -1 || lowerBound < this._absoluteMinimumBoundary)
            this._absoluteMinimumBoundary = lowerBound;

        var minimumTimeFrame = 0.1;
        var minimumDeltaForZeroSizeEvents = 0.01;
        var upperBound = Math.max(record._lastChildEndTime + minimumDeltaForZeroSizeEvents, lowerBound + minimumTimeFrame);
        if (this._absoluteMaximumBoundary === -1 || upperBound > this._absoluteMaximumBoundary)
            this._absoluteMaximumBoundary = upperBound;
    },

    formatValue: function(value)
    {
        return Number.secondsToString(value + this.minimumBoundary - this._absoluteMinimumBoundary);
    }
}


WebInspector.TimelineRecordListRow = function()
{
    this.element = document.createElement("div");
    this.element.row = this;
    this.element.style.cursor = "pointer";
    var iconElement = document.createElement("span");
    iconElement.className = "timeline-tree-icon";
    this.element.appendChild(iconElement);

    this._typeElement = document.createElement("span");
    this._typeElement.className = "type";
    this.element.appendChild(this._typeElement);

    var separatorElement = document.createElement("span");
    separatorElement.className = "separator";
    separatorElement.textContent = " ";

    this._dataElement = document.createElement("span");
    this._dataElement.className = "data dimmed";

    this.element.appendChild(separatorElement);
    this.element.appendChild(this._dataElement);
}

WebInspector.TimelineRecordListRow.prototype = {
    update: function(record, isEven, calculator, offset)
    {
        this._record = record;
        this._calculator = calculator;
        this._offset = offset;

        this.element.className = "timeline-tree-item timeline-category-" + record.category.name + (isEven ? " even" : "");
        this._typeElement.textContent = record.title;

        if (this._dataElement.firstChild)
            this._dataElement.removeChildren();
        if (record.details) {
            var detailsContainer = document.createElement("span");
            if (typeof record.details === "object") {
                detailsContainer.appendChild(document.createTextNode("("));
                detailsContainer.appendChild(record.details);
                detailsContainer.appendChild(document.createTextNode(")"));
            } else
                detailsContainer.textContent = "(" + record.details + ")";
            this._dataElement.appendChild(detailsContainer);
        }
    },

    dispose: function()
    {
        this.element.parentElement.removeChild(this.element);
    }
}

WebInspector.TimelineRecordGraphRow = function(graphContainer, scheduleRefresh)
{
    this.element = document.createElement("div");
    this.element.row = this;

    this._barAreaElement = document.createElement("div");
    this._barAreaElement.className = "timeline-graph-bar-area";
    this.element.appendChild(this._barAreaElement);

    this._barWithChildrenElement = document.createElement("div");
    this._barWithChildrenElement.className = "timeline-graph-bar with-children";
    this._barWithChildrenElement.row = this;
    this._barAreaElement.appendChild(this._barWithChildrenElement);

    this._barCpuElement = document.createElement("div");
    this._barCpuElement.className = "timeline-graph-bar cpu"
    this._barCpuElement.row = this;
    this._barAreaElement.appendChild(this._barCpuElement);

    this._barElement = document.createElement("div");
    this._barElement.className = "timeline-graph-bar";
    this._barElement.row = this;
    this._barAreaElement.appendChild(this._barElement);

    this._expandElement = new WebInspector.TimelineExpandableElement(graphContainer);
    this._expandElement._element.addEventListener("click", this._onClick.bind(this));

    this._scheduleRefresh = scheduleRefresh;
}

WebInspector.TimelineRecordGraphRow.prototype = {
    update: function(record, isEven, calculator, clientWidth, expandOffset, index)
    {
        this._record = record;
        this.element.className = "timeline-graph-side timeline-category-" + record.category.name + (isEven ? " even" : "");
        var barPosition = calculator.computeBarGraphWindowPosition(record, clientWidth - expandOffset);
        this._barWithChildrenElement.style.left = barPosition.left + expandOffset + "px";
        this._barWithChildrenElement.style.width = barPosition.widthWithChildren + "px";
        this._barElement.style.left = barPosition.left + expandOffset + "px";
        this._barElement.style.width =  barPosition.width + "px";
        this._barCpuElement.style.left = barPosition.left + expandOffset + "px";
        this._barCpuElement.style.width = barPosition.cpuWidth + "px";
        this._expandElement._update(record, index, barPosition);
    },

    _onClick: function(event)
    {
        this._record.collapsed = !this._record.collapsed;
        this._scheduleRefresh();
    },

    dispose: function()
    {
        this.element.parentElement.removeChild(this.element);
        this._expandElement._dispose();
    }
}

WebInspector.TimelinePanel.FormattedRecord = function(record, parentRecord, panel)
{
    var recordTypes = WebInspector.TimelineAgent.RecordType;
    var style = panel._recordStyles[record.type];

    this.parent = parentRecord;
    if (parentRecord)
        parentRecord.children.push(this);
    this.category = style.category;
    this.title = style.title;
    this.startTime = record.startTime / 1000;
    this.data = record.data;
    this.type = record.type;
    this.endTime = (typeof record.endTime !== "undefined") ? record.endTime / 1000 : this.startTime;
    this._selfTime = this.endTime - this.startTime;
    this._lastChildEndTime = this.endTime;
    this.originalRecordForTests = record;
    if (record.stackTrace && record.stackTrace.length)
        this.stackTrace = record.stackTrace;
    this.totalHeapSize = record.totalHeapSize;
    this.usedHeapSize = record.usedHeapSize;

    // Make resource receive record last since request was sent; make finish record last since response received.
    if (record.type === recordTypes.ResourceSendRequest) {
        panel._sendRequestRecords[record.data.identifier] = this;
    } else if (record.type === recordTypes.ScheduleResourceRequest) {
        panel._scheduledResourceRequests[record.data.url] = this;
    } else if (record.type === recordTypes.ResourceReceiveResponse) {
        var sendRequestRecord = panel._sendRequestRecords[record.data.identifier];
        if (sendRequestRecord) { // False if we started instrumentation in the middle of request.
            record.data.url = sendRequestRecord.data.url;
            // Now that we have resource in the collection, recalculate details in order to display short url.
            sendRequestRecord.details = this._getRecordDetails(sendRequestRecord, panel._sendRequestRecords);
            if (sendRequestRecord.parent !== panel._rootRecord && sendRequestRecord.parent.type === recordTypes.ScheduleResourceRequest)
                sendRequestRecord.parent.details = this._getRecordDetails(sendRequestRecord, panel._sendRequestRecords);
        }
    } else if (record.type === recordTypes.ResourceReceiveData) {
        var sendRequestRecord = panel._sendRequestRecords[record.data.identifier];
        if (sendRequestRecord) // False for main resource.
            record.data.url = sendRequestRecord.data.url;
    } else if (record.type === recordTypes.ResourceFinish) {
        var sendRequestRecord = panel._sendRequestRecords[record.data.identifier];
        if (sendRequestRecord) // False for main resource.
            record.data.url = sendRequestRecord.data.url;
    } else if (record.type === recordTypes.TimerInstall) {
        this.timeout = record.data.timeout;
        this.singleShot = record.data.singleShot;
        panel._timerRecords[record.data.timerId] = this;
    } else if (record.type === recordTypes.TimerFire) {
        var timerInstalledRecord = panel._timerRecords[record.data.timerId];
        if (timerInstalledRecord) {
            this.callSiteStackTrace = timerInstalledRecord.stackTrace;
            this.timeout = timerInstalledRecord.timeout;
            this.singleShot = timerInstalledRecord.singleShot;
        }
    }
    this.details = this._getRecordDetails(record, panel._sendRequestRecords);
}

WebInspector.TimelinePanel.FormattedRecord.prototype = {
    isLong: function()
    {
        return (this._lastChildEndTime - this.startTime) > WebInspector.TimelinePanel.shortRecordThreshold;
    },

    get children()
    {
        if (!this._children)
            this._children = [];
        return this._children;
    },

    _generateAggregatedInfo: function()
    {
        var cell = document.createElement("span");
        cell.className = "timeline-aggregated-info";
        for (var index in this._aggregatedStats) {
            var label = document.createElement("div");
            label.className = "timeline-aggregated-category timeline-" + index;
            cell.appendChild(label);
            var text = document.createElement("span");
            text.textContent = Number.secondsToString(this._aggregatedStats[index] + 0.0001);
            cell.appendChild(text);
        }
        return cell;
    },

    _generatePopupContent: function(calculator, categories)
    {
        var contentHelper = new WebInspector.TimelinePanel.PopupContentHelper(this.title);

        if (this._children && this._children.length) {
            contentHelper._appendTextRow(WebInspector.UIString("Self Time"), Number.secondsToString(this._selfTime + 0.0001));
            contentHelper._appendElementRow(WebInspector.UIString("Aggregated Time"), this._generateAggregatedInfo());
        }
        var text = WebInspector.UIString("%s (at %s)", Number.secondsToString(this._lastChildEndTime - this.startTime),
            calculator.formatValue(this.startTime - calculator.minimumBoundary));
        contentHelper._appendTextRow(WebInspector.UIString("Duration"), text);

        var recordTypes = WebInspector.TimelineAgent.RecordType;

        switch (this.type) {
            case recordTypes.GCEvent:
                contentHelper._appendTextRow(WebInspector.UIString("Collected"), Number.bytesToString(this.data.usedHeapSizeDelta));
                break;
            case recordTypes.TimerInstall:
            case recordTypes.TimerFire:
            case recordTypes.TimerRemove:
                contentHelper._appendTextRow(WebInspector.UIString("Timer ID"), this.data.timerId);
                if (typeof this.timeout === "number") {
                    contentHelper._appendTextRow(WebInspector.UIString("Timeout"), Number.secondsToString(this.timeout / 1000));
                    contentHelper._appendTextRow(WebInspector.UIString("Repeats"), !this.singleShot);
                }
                break;
            case recordTypes.FunctionCall:
                contentHelper._appendLinkRow(WebInspector.UIString("Location"), this.data.scriptName, this.data.scriptLine);
                break;
            case recordTypes.ScheduleResourceRequest:
            case recordTypes.ResourceSendRequest:
            case recordTypes.ResourceReceiveResponse:
            case recordTypes.ResourceReceiveData:
            case recordTypes.ResourceFinish:
                contentHelper._appendLinkRow(WebInspector.UIString("Resource"), this.data.url);
                if (this.data.requestMethod)
                    contentHelper._appendTextRow(WebInspector.UIString("Request Method"), this.data.requestMethod);
                if (typeof this.data.statusCode === "number")
                    contentHelper._appendTextRow(WebInspector.UIString("Status Code"), this.data.statusCode);
                if (this.data.mimeType)
                    contentHelper._appendTextRow(WebInspector.UIString("MIME Type"), this.data.mimeType);
                if (typeof this.data.expectedContentLength === "number" && this.data.expectedContentLength !== -1)
                    contentHelper._appendTextRow(WebInspector.UIString("Expected Content Length"), this.data.expectedContentLength);
                break;
            case recordTypes.EvaluateScript:
                if (this.data && this.data.url)
                    contentHelper._appendLinkRow(WebInspector.UIString("Script"), this.data.url, this.data.lineNumber);
                break;
            case recordTypes.Paint:
                contentHelper._appendTextRow(WebInspector.UIString("Location"), WebInspector.UIString("(%d, %d)", this.data.x, this.data.y));
                contentHelper._appendTextRow(WebInspector.UIString("Dimensions"), WebInspector.UIString("%d × %d", this.data.width, this.data.height));
            case recordTypes.RecalculateStyles: // We don't want to see default details.
                break;
            default:
                if (this.details)
                    contentHelper._appendTextRow(WebInspector.UIString("Details"), this.details);
                break;
        }

        if (this.data.scriptName && this.type !== recordTypes.FunctionCall)
            contentHelper._appendLinkRow(WebInspector.UIString("Function Call"), this.data.scriptName, this.data.scriptLine);

        if (this.usedHeapSize)
            contentHelper._appendTextRow(WebInspector.UIString("Used Heap Size"), WebInspector.UIString("%s of %s", Number.bytesToString(this.usedHeapSize), Number.bytesToString(this.totalHeapSize)));

        if (this.callSiteStackTrace && this.callSiteStackTrace.length)
            contentHelper._appendStackTrace(WebInspector.UIString("Call Site stack"), this.callSiteStackTrace);

        if (this.stackTrace)
            contentHelper._appendStackTrace(WebInspector.UIString("Call Stack"), this.stackTrace);

        return contentHelper._contentTable;
    },

    _getRecordDetails: function(record, sendRequestRecords)
    {
        switch (record.type) {
            case WebInspector.TimelineAgent.RecordType.GCEvent:
                return WebInspector.UIString("%s collected", Number.bytesToString(record.data.usedHeapSizeDelta));
            case WebInspector.TimelineAgent.RecordType.TimerFire:
                return record.data.scriptName ? WebInspector.linkifyResourceAsNode(record.data.scriptName, "scripts", record.data.scriptLine, "", "") : record.data.timerId;
            case WebInspector.TimelineAgent.RecordType.FunctionCall:
                return record.data.scriptName ? WebInspector.linkifyResourceAsNode(record.data.scriptName, "scripts", record.data.scriptLine, "", "") : null;
            case WebInspector.TimelineAgent.RecordType.EventDispatch:
                return record.data ? record.data.type : null;
            case WebInspector.TimelineAgent.RecordType.Paint:
                return record.data.width + "\u2009\u00d7\u2009" + record.data.height;
            case WebInspector.TimelineAgent.RecordType.TimerInstall:
            case WebInspector.TimelineAgent.RecordType.TimerRemove:
                return this.stackTrace ? WebInspector.linkifyResourceAsNode(this.stackTrace[0].scriptName, "scripts", this.stackTrace[0].lineNumber, "", "") : record.data.timerId;
            case WebInspector.TimelineAgent.RecordType.ParseHTML:
            case WebInspector.TimelineAgent.RecordType.RecalculateStyles:
                return this.stackTrace ? WebInspector.linkifyResourceAsNode(this.stackTrace[0].scriptName, "scripts", this.stackTrace[0].lineNumber, "", "") : null;
            case WebInspector.TimelineAgent.RecordType.EvaluateScript:
                return record.data.url ? WebInspector.linkifyResourceAsNode(record.data.url, "scripts", record.data.lineNumber, "", "") : null;
            case WebInspector.TimelineAgent.RecordType.XHRReadyStateChange:
            case WebInspector.TimelineAgent.RecordType.XHRLoad:
            case WebInspector.TimelineAgent.RecordType.ScheduleResourceRequest:
            case WebInspector.TimelineAgent.RecordType.ResourceSendRequest:
            case WebInspector.TimelineAgent.RecordType.ResourceReceiveData:
            case WebInspector.TimelineAgent.RecordType.ResourceReceiveResponse:
            case WebInspector.TimelineAgent.RecordType.ResourceFinish:
                return WebInspector.displayNameForURL(record.data.url);
            case WebInspector.TimelineAgent.RecordType.MarkTimeline:
                return record.data.message;
            default:
                return null;
        }
    },

    _calculateAggregatedStats: function(categories)
    {
        this._aggregatedStats = {};
        for (var category in categories)
            this._aggregatedStats[category] = 0;
        this._cpuTime = this._selfTime;

        if (this._children) {
            for (var index = this._children.length; index; --index) {
                var child = this._children[index - 1];
                this._aggregatedStats[child.category.name] += child._selfTime;
                for (var category in categories)
                    this._aggregatedStats[category] += child._aggregatedStats[category];
            }
            for (var category in this._aggregatedStats)
                this._cpuTime += this._aggregatedStats[category];
        }
    }
}

WebInspector.TimelinePanel.PopupContentHelper = function(title)
{
    this._contentTable = document.createElement("table");;
    var titleCell = this._createCell(WebInspector.UIString("%s - Details", title), "timeline-details-title");
    titleCell.colSpan = 2;
    var titleRow = document.createElement("tr");
    titleRow.appendChild(titleCell);
    this._contentTable.appendChild(titleRow);
}

WebInspector.TimelinePanel.PopupContentHelper.prototype = {
    _createCell: function(content, styleName)
    {
        var text = document.createElement("label");
        text.appendChild(document.createTextNode(content));
        var cell = document.createElement("td");
        cell.className = "timeline-details";
        if (styleName)
            cell.className += " " + styleName;
        cell.textContent = content;
        return cell;
    },

    _appendTextRow: function(title, content)
    {
        var row = document.createElement("tr");
        row.appendChild(this._createCell(title, "timeline-details-row-title"));
        row.appendChild(this._createCell(content, "timeline-details-row-data"));
        this._contentTable.appendChild(row);
    },

    _appendElementRow: function(title, content, titleStyle)
    {
        var row = document.createElement("tr");
        var titleCell = this._createCell(title, "timeline-details-row-title");
        if (titleStyle)
            titleCell.addStyleClass(titleStyle);
        row.appendChild(titleCell);
        var cell = document.createElement("td");
        cell.className = "timeline-details";
        cell.appendChild(content);
        row.appendChild(cell);
        this._contentTable.appendChild(row);
    },

    _appendLinkRow: function(title, scriptName, scriptLine)
    {
        var link = WebInspector.linkifyResourceAsNode(scriptName, "scripts", scriptLine, "timeline-details");
        this._appendElementRow(title, link);
    },

    _appendStackTrace: function(title, stackTrace)
    {
        this._appendTextRow("", "");
        var framesTable = document.createElement("table");
        for (var i = 0; i < stackTrace.length; ++i) {
            var stackFrame = stackTrace[i];
            var row = document.createElement("tr");
            row.className = "timeline-details";
            row.appendChild(this._createCell(stackFrame.functionName ? stackFrame.functionName : WebInspector.UIString("(anonymous function)"), "timeline-function-name"));
            row.appendChild(this._createCell(" @ "));
            var linkCell = document.createElement("td");
            linkCell.appendChild(WebInspector.linkifyResourceAsNode(stackFrame.scriptName, "scripts", stackFrame.lineNumber, "timeline-details"));
            row.appendChild(linkCell);
            framesTable.appendChild(row);
        }
        this._appendElementRow(title, framesTable, "timeline-stacktrace-title");
    }
}

WebInspector.TimelineExpandableElement = function(container)
{
    this._element = document.createElement("div");
    this._element.className = "timeline-expandable";

    var leftBorder = document.createElement("div");
    leftBorder.className = "timeline-expandable-left";
    this._element.appendChild(leftBorder);

    container.appendChild(this._element);
}

WebInspector.TimelineExpandableElement.prototype = {
    _update: function(record, index, barPosition)
    {
        var rowHeight = WebInspector.TimelinePanel.rowHeight;
        if (record._visibleChildrenCount || record._invisibleChildrenCount) {
            this._element.style.top = index * rowHeight + "px";
            this._element.style.left = barPosition.left + "px";
            this._element.style.width = Math.max(12, barPosition.width + 25) + "px";
            if (!record.collapsed) {
                this._element.style.height = (record._visibleChildrenCount + 1) * rowHeight + "px";
                this._element.addStyleClass("timeline-expandable-expanded");
                this._element.removeStyleClass("timeline-expandable-collapsed");
            } else {
                this._element.style.height = rowHeight + "px";
                this._element.addStyleClass("timeline-expandable-collapsed");
                this._element.removeStyleClass("timeline-expandable-expanded");
            }
            this._element.removeStyleClass("hidden");
        } else
            this._element.addStyleClass("hidden");
    },

    _dispose: function()
    {
        this._element.parentElement.removeChild(this._element);
    }
}
