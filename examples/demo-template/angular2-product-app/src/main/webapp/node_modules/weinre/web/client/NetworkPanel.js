/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2008, 2009 Anthony Ricaud <rik@webkit.org>
 * Copyright (C) 2011 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

WebInspector.NetworkPanel = function()
{
    WebInspector.Panel.call(this, "network");

    this.createSidebar();
    this.sidebarElement.className = "network-sidebar";

    this._resources = [];
    this._resourcesById = {};
    this._resourcesByURL = {};
    this._staleResources = [];
    this._resourceGridNodes = {};
    this._mainResourceLoadTime = -1;
    this._mainResourceDOMContentTime = -1;
    this._hiddenCategories = {};

    this._categories = WebInspector.resourceCategories;

    this.containerElement = document.createElement("div");
    this.containerElement.id = "network-container";
    this.sidebarElement.appendChild(this.containerElement);

    this._viewsContainerElement = document.createElement("div");
    this._viewsContainerElement.id = "network-views";
    this._viewsContainerElement.className = "hidden";
    this.element.appendChild(this._viewsContainerElement);

    this._closeButtonElement = document.createElement("button");
    this._closeButtonElement.id = "network-close-button";
    this._closeButtonElement.addEventListener("click", this._toggleGridMode.bind(this), false);
    this._viewsContainerElement.appendChild(this._closeButtonElement);

    this._createSortingFunctions();
    this._createTable();
    this._createTimelineGrid();
    this._createStatusbarButtons();
    this._createFilterStatusBarItems();
    this._createSummaryBar();

    if (!WebInspector.settings.resourcesLargeRows)
        this._setLargerResources(WebInspector.settings.resourcesLargeRows);

    this._popoverHelper = new WebInspector.PopoverHelper(this.element, this._getPopoverAnchor.bind(this), this._showPopover.bind(this), true);
    // Enable faster hint.
    this._popoverHelper.setTimeout(100);

    this.calculator = new WebInspector.NetworkTransferTimeCalculator();
    this._filter(this._filterAllElement, false);

    this._toggleGridMode();

    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.ResourceStarted, this._onResourceStarted, this);
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.ResourceUpdated, this._onResourceUpdated, this);
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.ResourceFinished, this._onResourceUpdated, this);
    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.MainResourceCommitLoad, this._onMainResourceCommitLoad, this);
}

WebInspector.NetworkPanel.prototype = {
    get toolbarItemLabel()
    {
        return WebInspector.UIString("Network");
    },

    get statusBarItems()
    {
        return [this._largerResourcesButton.element, this._preserveLogToggle.element, this._clearButton.element, this._filterBarElement];
    },

    isCategoryVisible: function(categoryName)
    {
        return true;
    },

    elementsToRestoreScrollPositionsFor: function()
    {
        return [this.containerElement, this._dataGrid.scrollContainer];
    },

    resize: function()
    {
        WebInspector.Panel.prototype.resize.call(this);
        this._dataGrid.updateWidths();
        this._positionSummaryBar();
    },

    updateSidebarWidth: function(width)
    {
        if (!this._viewingResourceMode)
            return;
        WebInspector.Panel.prototype.updateSidebarWidth.call(this, width);
    },

    updateMainViewWidth: function(width)
    {
        this._viewsContainerElement.style.left = width + "px";
    },

    handleShortcut: function(event)
    {
        if (this._viewingResourceMode && event.keyCode === WebInspector.KeyboardShortcut.Keys.Esc.code) {
            this._toggleGridMode();
            event.handled = true;
        }
    },

    _positionSummaryBar: function()
    {
        // Position the total bar.

        var fillerRow = this._dataGrid.dataTableBody.lastChild;
        if (this._summaryBarElement.parentElement !== this.element && fillerRow.offsetHeight > 0) {
            // Glue status to bottom.
            if (this._summaryBarRowNode) {
                this._dataGrid.removeChild(this._summaryBarRowNode);
                delete this._summaryBarRowNode;
            }
            this._summaryBarElement.addStyleClass("network-summary-bar-bottom");
            this.element.appendChild(this._summaryBarElement);
            this._dataGrid.element.style.bottom = "20px";
            return;
        }

        if (!this._summaryBarRowNode && !fillerRow.offsetHeight) {
            // Glue status to table.
            this._summaryBarRowNode = new WebInspector.NetworkTotalGridNode(this._summaryBarElement);
            this._summaryBarElement.removeStyleClass("network-summary-bar-bottom");
            this._dataGrid.appendChild(this._summaryBarRowNode);
            this._dataGrid.element.style.bottom = 0;
            this._sortItems();
        }
        this._updateOffscreenRows();
    },

    _resetSummaryBar: function()
    {
        delete this._summaryBarRowNode;
        this._summaryBarElement.parentElement.removeChild(this._summaryBarElement);
        this._updateSummaryBar();
    },

    _createTimelineGrid: function()
    {
        this._timelineGrid = new WebInspector.TimelineGrid();
        this._timelineGrid.element.addStyleClass("network-timeline-grid");
        this._dataGrid.element.appendChild(this._timelineGrid.element);
    },

    _createTable: function()
    {
        var columns = {name: {}, method: {}, status: {}, type: {}, size: {}, time: {}, timeline: {}};
        columns.name.titleDOMFragment = this._makeHeaderFragment(WebInspector.UIString("Name"), WebInspector.UIString("Path"));
        columns.name.sortable = true;
        columns.name.width = "20%";
        columns.name.disclosure = true;

        columns.method.title = WebInspector.UIString("Method");
        columns.method.sortable = true;
        columns.method.width = "7%";

        columns.status.titleDOMFragment = this._makeHeaderFragment(WebInspector.UIString("Status"), WebInspector.UIString("Text"));
        columns.status.sortable = true;
        columns.status.width = "8%";

        columns.type.title = WebInspector.UIString("Type");
        columns.type.sortable = true;
        columns.type.width = "10%";

        columns.size.titleDOMFragment = this._makeHeaderFragment(WebInspector.UIString("Size"), WebInspector.UIString("Transfer"));
        columns.size.sortable = true;
        columns.size.width = "10%";
        columns.size.aligned = "right";

        columns.time.titleDOMFragment = this._makeHeaderFragment(WebInspector.UIString("Time"), WebInspector.UIString("Latency"));
        columns.time.sortable = true;
        columns.time.width = "10%";
        columns.time.aligned = "right";

        columns.timeline.title = "";
        columns.timeline.sortable = false;
        columns.timeline.width = "37%";
        columns.timeline.sort = "ascending";

        this._dataGrid = new WebInspector.DataGrid(columns);
        this._dataGrid.element.addEventListener("contextmenu", this._contextMenu.bind(this), true);
        this.containerElement.appendChild(this._dataGrid.element);
        this._dataGrid.addEventListener("sorting changed", this._sortItems, this);
        this._dataGrid.addEventListener("width changed", this._updateDividersIfNeeded, this);
        this._dataGrid.scrollContainer.addEventListener("scroll", this._updateOffscreenRows.bind(this));

        this._patchTimelineHeader();
    },

    _makeHeaderFragment: function(title, subtitle)
    {
        var fragment = document.createDocumentFragment();
        fragment.appendChild(document.createTextNode(title));
        var subtitleDiv = document.createElement("div");
        subtitleDiv.className = "network-header-subtitle";
        subtitleDiv.textContent = subtitle;
        fragment.appendChild(subtitleDiv);
        return fragment;
    },

    _patchTimelineHeader: function()
    {
        var timelineSorting = document.createElement("select");

        var option = document.createElement("option");
        option.value = "startTime";
        option.label = WebInspector.UIString("Timeline");
        timelineSorting.appendChild(option);

        option = document.createElement("option");
        option.value = "startTime";
        option.label = WebInspector.UIString("Start Time");
        timelineSorting.appendChild(option);

        option = document.createElement("option");
        option.value = "responseTime";
        option.label = WebInspector.UIString("Response Time");
        timelineSorting.appendChild(option);

        option = document.createElement("option");
        option.value = "endTime";
        option.label = WebInspector.UIString("End Time");
        timelineSorting.appendChild(option);

        option = document.createElement("option");
        option.value = "duration";
        option.label = WebInspector.UIString("Duration");
        timelineSorting.appendChild(option);

        option = document.createElement("option");
        option.value = "latency";
        option.label = WebInspector.UIString("Latency");
        timelineSorting.appendChild(option);

        var header = this._dataGrid.headerTableHeader("timeline");
        header.replaceChild(timelineSorting, header.firstChild);

        timelineSorting.addEventListener("click", function(event) { event.stopPropagation() }, false);
        timelineSorting.addEventListener("change", this._sortByTimeline.bind(this), false);
        this._timelineSortSelector = timelineSorting;
    },

    _createSortingFunctions: function()
    {
        this._sortingFunctions = {};
        this._sortingFunctions.name = WebInspector.NetworkDataGridNode.NameComparator;
        this._sortingFunctions.method = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "method", false);
        this._sortingFunctions.status = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "statusCode", false);
        this._sortingFunctions.type = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "mimeType", false);
        this._sortingFunctions.size = WebInspector.NetworkDataGridNode.SizeComparator;
        this._sortingFunctions.time = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "duration", false);
        this._sortingFunctions.timeline = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "startTime", false);
        this._sortingFunctions.startTime = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "startTime", false);
        this._sortingFunctions.endTime = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "endTime", false);
        this._sortingFunctions.responseTime = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "responseReceivedTime", false);
        this._sortingFunctions.duration = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "duration", true);
        this._sortingFunctions.latency = WebInspector.NetworkDataGridNode.ResourcePropertyComparator.bind(null, "latency", true);

        var timeCalculator = new WebInspector.NetworkTransferTimeCalculator();
        var durationCalculator = new WebInspector.NetworkTransferDurationCalculator();

        this._calculators = {};
        this._calculators.timeline = timeCalculator;
        this._calculators.startTime = timeCalculator;
        this._calculators.endTime = timeCalculator;
        this._calculators.responseTime = timeCalculator;
        this._calculators.duration = durationCalculator;
        this._calculators.latency = durationCalculator;
    },

    _sortItems: function()
    {
        var columnIdentifier = this._dataGrid.sortColumnIdentifier;
        if (columnIdentifier === "timeline") {
            this._sortByTimeline();
            return;
        }
        var sortingFunction = this._sortingFunctions[columnIdentifier];
        if (!sortingFunction)
            return;

        this._dataGrid.sortNodes(sortingFunction, this._dataGrid.sortOrder === "descending");
        this._timelineSortSelector.selectedIndex = 0;
        this._updateOffscreenRows();
    },

    _sortByTimeline: function()
    {
        var selectedIndex = this._timelineSortSelector.selectedIndex;
        if (!selectedIndex)
            selectedIndex = 1; // Sort by start time by default.
        var selectedOption = this._timelineSortSelector[selectedIndex];
        var value = selectedOption.value;

        var sortingFunction = this._sortingFunctions[value];
        this._dataGrid.sortNodes(sortingFunction);
        this.calculator = this._calculators[value];
        if (this.calculator.startAtZero)
            this._timelineGrid.hideEventDividers();
        else
            this._timelineGrid.showEventDividers();
        this._dataGrid.markColumnAsSortedBy("timeline", "ascending");
        this._updateOffscreenRows();
    },

    _createFilterStatusBarItems: function()
    {
        var filterBarElement = document.createElement("div");
        filterBarElement.className = "scope-bar status-bar-item";
        filterBarElement.id = "network-filter";

        function createFilterElement(category, label)
        {
            var categoryElement = document.createElement("li");
            categoryElement.category = category;
            categoryElement.className = category;
            categoryElement.appendChild(document.createTextNode(label));
            categoryElement.addEventListener("click", this._updateFilter.bind(this), false);
            filterBarElement.appendChild(categoryElement);

            return categoryElement;
        }

        this._filterAllElement = createFilterElement.call(this, "all", WebInspector.UIString("All"));

        // Add a divider
        var dividerElement = document.createElement("div");
        dividerElement.addStyleClass("scope-bar-divider");
        filterBarElement.appendChild(dividerElement);

        for (var category in this._categories)
            createFilterElement.call(this, category, this._categories[category].title);
        this._filterBarElement = filterBarElement;
    },

    _createSummaryBar: function()
    {
        this._summaryBarElement = document.createElement("div");
        this._summaryBarElement.className = "network-summary-bar";
        this.containerElement.appendChild(this._summaryBarElement);
    },

    _updateSummaryBar: function()
    {
        this._positionSummaryBar(); // Grid is growing.
        var numRequests = this._resources.length;

        if (!numRequests) {
            if (this._summaryBarElement._isDisplayingWarning)
                return;
            this._summaryBarElement._isDisplayingWarning = true;

            var img = document.createElement("img");
            img.src = "Images/warningIcon.png";
            this._summaryBarElement.removeChildren();
            this._summaryBarElement.appendChild(img);
            this._summaryBarElement.appendChild(document.createTextNode(" "));
            this._summaryBarElement.appendChild(document.createTextNode(
                WebInspector.UIString("No requests captured. Reload the page to see detailed information on the network activity.")));
            return;
        }
        delete this._summaryBarElement._isDisplayingWarning;

        var transferSize = 0;
        var baseTime = -1;
        var maxTime = -1;
        for (var i = 0; i < this._resources.length; ++i) {
            var resource = this._resources[i];
            transferSize += (resource.cached || !resource.transferSize) ? 0 : resource.transferSize;
            if (resource.isMainResource)
                baseTime = resource.startTime;
            if (resource.endTime > maxTime)
                maxTime = resource.endTime;
        }
        var text = String.sprintf(WebInspector.UIString("%d requests"), numRequests);
        text += "  \u2758  " + String.sprintf(WebInspector.UIString("%s transferred"), Number.bytesToString(transferSize));
        if (baseTime !== -1 && this._mainResourceLoadTime !== -1 && this._mainResourceDOMContentTime !== -1 && this._mainResourceDOMContentTime > baseTime) {
            text += "  \u2758  " + String.sprintf(WebInspector.UIString("%s (onload: %s, DOMContentLoaded: %s)"),
                        Number.secondsToString(maxTime - baseTime),
                        Number.secondsToString(this._mainResourceLoadTime - baseTime),
                        Number.secondsToString(this._mainResourceDOMContentTime - baseTime));
        }
        this._summaryBarElement.textContent = text;
    },

    _showCategory: function(category)
    {
        this._dataGrid.element.addStyleClass("filter-" + category);
        delete this._hiddenCategories[category];
    },

    _hideCategory: function(category)
    {
        this._dataGrid.element.removeStyleClass("filter-" + category);
        this._hiddenCategories[category] = true;
    },

    _updateFilter: function(e)
    {
        var isMac = WebInspector.isMac();
        var selectMultiple = false;
        if (isMac && e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey)
            selectMultiple = true;
        if (!isMac && e.ctrlKey && !e.metaKey && !e.altKey && !e.shiftKey)
            selectMultiple = true;

        this._filter(e.target, selectMultiple);
        this._positionSummaryBar();
    },

    _filter: function(target, selectMultiple)
    {
        function unselectAll()
        {
            for (var i = 0; i < this._filterBarElement.childNodes.length; ++i) {
                var child = this._filterBarElement.childNodes[i];
                if (!child.category)
                    continue;

                child.removeStyleClass("selected");
                this._hideCategory(child.category);
            }
        }

        if (target.category === this._filterAllElement) {
            if (target.hasStyleClass("selected")) {
                // We can't unselect All, so we break early here
                return;
            }

            // If All wasn't selected, and now is, unselect everything else.
            unselectAll.call(this);
        } else {
            // Something other than All is being selected, so we want to unselect All.
            if (this._filterAllElement.hasStyleClass("selected")) {
                this._filterAllElement.removeStyleClass("selected");
                this._hideCategory("all");
            }
        }

        if (!selectMultiple) {
            // If multiple selection is off, we want to unselect everything else
            // and just select ourselves.
            unselectAll.call(this);

            target.addStyleClass("selected");
            this._showCategory(target.category);
            return;
        }

        if (target.hasStyleClass("selected")) {
            // If selectMultiple is turned on, and we were selected, we just
            // want to unselect ourselves.
            target.removeStyleClass("selected");
            this._hideCategory(target.category);
        } else {
            // If selectMultiple is turned on, and we weren't selected, we just
            // want to select ourselves.
            target.addStyleClass("selected");
            this._showCategory(target.category);
        }
        this._updateOffscreenRows();
    },

    _scheduleRefresh: function()
    {
        if (this._needsRefresh)
            return;

        this._needsRefresh = true;

        if (this.visible && !("_refreshTimeout" in this))
            this._refreshTimeout = setTimeout(this.refresh.bind(this), 500);
    },

    _updateDividersIfNeeded: function(force)
    {
        var timelineColumn = this._dataGrid.columns.timeline;
        for (var i = 0; i < this._dataGrid.resizers.length; ++i) {
            if (timelineColumn.ordinal === this._dataGrid.resizers[i].rightNeighboringColumnID) {
                // Position timline grid location.
                this._timelineGrid.element.style.left = this._dataGrid.resizers[i].style.left;
                this._timelineGrid.element.style.right = "18px";
            }
        }

        var proceed = true;
        if (!this.visible) {
            this._scheduleRefresh();
            proceed = false;
        } else
            proceed = this._timelineGrid.updateDividers(force, this.calculator);

        if (!proceed)
            return;

        if (this.calculator.startAtZero || !this.calculator.computePercentageFromEventTime) {
            // If our current sorting method starts at zero, that means it shows all
            // resources starting at the same point, and so onLoad event and DOMContent
            // event lines really wouldn't make much sense here, so don't render them.
            // Additionally, if the calculator doesn't have the computePercentageFromEventTime
            // function defined, we are probably sorting by size, and event times aren't relevant
            // in this case.
            return;
        }

        this._timelineGrid.removeEventDividers();
        if (this._mainResourceLoadTime !== -1) {
            var percent = this.calculator.computePercentageFromEventTime(this._mainResourceLoadTime);

            var loadDivider = document.createElement("div");
            loadDivider.className = "network-event-divider network-red-divider";

            var loadDividerPadding = document.createElement("div");
            loadDividerPadding.className = "network-event-divider-padding";
            loadDividerPadding.title = WebInspector.UIString("Load event fired");
            loadDividerPadding.appendChild(loadDivider);
            loadDividerPadding.style.left = percent + "%";
            this._timelineGrid.addEventDivider(loadDividerPadding);
        }

        if (this._mainResourceDOMContentTime !== -1) {
            var percent = this.calculator.computePercentageFromEventTime(this._mainResourceDOMContentTime);

            var domContentDivider = document.createElement("div");
            domContentDivider.className = "network-event-divider network-blue-divider";

            var domContentDividerPadding = document.createElement("div");
            domContentDividerPadding.className = "network-event-divider-padding";
            domContentDividerPadding.title = WebInspector.UIString("DOMContent event fired");
            domContentDividerPadding.appendChild(domContentDivider);
            domContentDividerPadding.style.left = percent + "%";
            this._timelineGrid.addEventDivider(domContentDividerPadding);
        }
    },

    _refreshIfNeeded: function()
    {
        if (this._needsRefresh)
            this.refresh();
    },

    _invalidateAllItems: function()
    {
        this._staleResources = this._resources.slice();
    },

    get calculator()
    {
        return this._calculator;
    },

    set calculator(x)
    {
        if (!x || this._calculator === x)
            return;

        this._calculator = x;
        this._calculator.reset();

        this._invalidateAllItems();
        this.refresh();
    },

    _resourceGridNode: function(resource)
    {
        return this._resourceGridNodes[resource.identifier];
    },

    revealAndSelectItem: function(resource)
    {
        var node = this._resourceGridNode(resource);
        if (node) {
            node.reveal();
            node.select(true);
        }
    },

    addEventDivider: function(divider)
    {
        this._timelineGrid.addEventDivider(divider);
    },

    _createStatusbarButtons: function()
    {
        this._preserveLogToggle = new WebInspector.StatusBarButton(WebInspector.UIString("Preserve Log upon Navigation"), "record-profile-status-bar-item");
        this._preserveLogToggle.addEventListener("click", this._onPreserveLogClicked.bind(this), false);

        this._clearButton = new WebInspector.StatusBarButton(WebInspector.UIString("Clear"), "clear-status-bar-item");
        this._clearButton.addEventListener("click", this._reset.bind(this), false);

        this._largerResourcesButton = new WebInspector.StatusBarButton(WebInspector.UIString("Use small resource rows."), "network-larger-resources-status-bar-item");
        this._largerResourcesButton.toggled = WebInspector.settings.resourcesLargeRows;
        this._largerResourcesButton.addEventListener("click", this._toggleLargerResources.bind(this), false);
    },

    set mainResourceLoadTime(x)
    {
        if (this._mainResourceLoadTime === x)
            return;

        this._mainResourceLoadTime = x || -1;
        // Update the dividers to draw the new line
        this._updateDividersIfNeeded(true);
    },

    set mainResourceDOMContentTime(x)
    {
        if (this._mainResourceDOMContentTime === x)
            return;

        this._mainResourceDOMContentTime = x || -1;
        this._updateDividersIfNeeded(true);
    },

    show: function()
    {
        WebInspector.Panel.prototype.show.call(this);
        this._refreshIfNeeded();

        if (this.visibleView)
            this.visibleView.show(this._viewsContainerElement);

        this._dataGrid.updateWidths();
        this._positionSummaryBar();
    },

    hide: function()
    {
        WebInspector.Panel.prototype.hide.call(this);
        this._popoverHelper.hidePopup();
    },

    get searchableViews()
    {
        var views = [];
        return views;
    },

    searchMatchFound: function(view, matches)
    {
        this._resourceGridNode(view.resource).searchMatches = matches;
    },

    searchCanceled: function(startingNewSearch)
    {
        WebInspector.Panel.prototype.searchCanceled.call(this, startingNewSearch);

        if (startingNewSearch || !this._resources)
            return;
    },

    performSearch: function(query)
    {
        WebInspector.Panel.prototype.performSearch.call(this, query);
    },

    refresh: function()
    {
        this._needsRefresh = false;
        if ("_refreshTimeout" in this) {
            clearTimeout(this._refreshTimeout);
            delete this._refreshTimeout;
        }

        var wasScrolledToLastRow = this._dataGrid.isScrolledToLastRow();
        var staleItemsLength = this._staleResources.length;
        var boundariesChanged = false;

        for (var i = 0; i < staleItemsLength; ++i) {
            var resource = this._staleResources[i];
            var node = this._resourceGridNode(resource);
            if (!node) {
                // Create the timeline tree element and graph.
                node = new WebInspector.NetworkDataGridNode(this, resource);
                this._resourceGridNodes[resource.identifier] = node;
                this._dataGrid.appendChild(node);
            }
            node.refreshResource();

            if (this.calculator.updateBoundaries(resource))
                boundariesChanged = true;
        }

        if (boundariesChanged) {
            // The boundaries changed, so all item graphs are stale.
            this._invalidateAllItems();
            staleItemsLength = this._staleResources.length;
        }

        for (var i = 0; i < staleItemsLength; ++i)
            this._resourceGridNode(this._staleResources[i]).refreshGraph(this.calculator);

        this._staleResources = [];
        this._sortItems();
        this._updateSummaryBar();
        this._dataGrid.updateWidths();

        if (wasScrolledToLastRow)
            this._dataGrid.scrollToLastRow();
    },

    _onPreserveLogClicked: function(e)
    {
        this._preserveLogToggle.toggled = !this._preserveLogToggle.toggled;
    },

    _reset: function()
    {
        this._popoverHelper.hidePopup();
        this._closeVisibleResource();

        this._toggleGridMode();

        // Begin reset timeline
        if (this._calculator)
            this._calculator.reset();

        this._resources = [];
        this._resourcesById = {};
        this._resourcesByURL = {};
        this._staleResources = [];
        this._resourceGridNodes = {};

        this._dataGrid.removeChildren();
        delete this._summaryBarRowNode;
        this._updateDividersIfNeeded(true);
        // End reset timeline.

        this._mainResourceLoadTime = -1;
        this._mainResourceDOMContentTime = -1;

        this._viewsContainerElement.removeChildren();
        this._viewsContainerElement.appendChild(this._closeButtonElement);
        this._resetSummaryBar();
    },

    get resources()
    {
        return this._resources;
    },

    resourceById: function(id)
    {
        return this._resourcesById[id];
    },

    _onResourceStarted: function(event)
    {
        this._appendResource(event.data);
    },

    _appendResource: function(resource)
    {
        this._resources.push(resource);
        this._resourcesById[resource.identifier] = resource;
        this._resourcesByURL[resource.url] = resource;

        // Pull all the redirects of the main resource upon commit load.
        if (resource.redirects) {
            for (var i = 0; i < resource.redirects.length; ++i)
                this._refreshResource(resource.redirects[i]);
        }

        this._refreshResource(resource);
    },

    _onResourceUpdated: function(event)
    {
        this._refreshResource(event.data);
    },

    _refreshResource: function(resource)
    {
        this._staleResources.push(resource);
        this._scheduleRefresh();

        var oldView = WebInspector.ResourceView.existingResourceViewForResource(resource);
        if (!oldView)
            return;

        if (WebInspector.ResourceView.resourceViewTypeMatchesResource(resource))
            return;

        var newView = WebInspector.ResourceView.recreateResourceView(resource);
        if (this.visibleView === oldView)
            this.visibleView = newView;
    },

    clear: function()
    {
        if (this._preserveLogToggle.toggled)
            return;
        this._reset();
    },

    _onMainResourceCommitLoad: function()
    {
        if (this._preserveLogToggle.toggled)
            return;

        this._reset();
        // Now resurrect the main resource along with all redirects that lead to it.
        var resourcesToAppend = (WebInspector.mainResource.redirects || []).concat(WebInspector.mainResource);
        resourcesToAppend.forEach(this._appendResource, this);
    },

    canShowSourceLine: function(url, line)
    {
        return !!this._resourcesByURL[url];
    },

    showSourceLine: function(url, line)
    {
        this._showResource(this._resourcesByURL[url], line);
    },

    _showResource: function(resource, line)
    {
        if (!resource)
            return;

        this._popoverHelper.hidePopup();

        this._toggleViewingResourceMode();

        if (this.visibleView) {
            this.visibleView.detach();
            delete this.visibleView;
        }

        var view = new WebInspector.NetworkItemView(resource);
        view.show(this._viewsContainerElement);
        this.visibleView = view;

        this.updateSidebarWidth();
    },

    _closeVisibleResource: function()
    {
        this.element.removeStyleClass("viewing-resource");

        if (this.visibleView) {
            this.visibleView.detach();
            delete this.visibleView;
        }

        if (this._lastSelectedGraphTreeElement)
            this._lastSelectedGraphTreeElement.select(true);

        this.updateSidebarWidth();
    },

    _toggleLargerResources: function()
    {
        WebInspector.settings.resourcesLargeRows = !WebInspector.settings.resourcesLargeRows;
        this._setLargerResources(WebInspector.settings.resourcesLargeRows);
    },

    _setLargerResources: function(enabled)
    {
        this._largerResourcesButton.toggled = enabled;
        if (!enabled) {
            this._largerResourcesButton.title = WebInspector.UIString("Use large resource rows.");
            this._dataGrid.element.addStyleClass("small");
            this._timelineGrid.element.addStyleClass("small");
            this._viewsContainerElement.addStyleClass("small");
        } else {
            this._largerResourcesButton.title = WebInspector.UIString("Use small resource rows.");
            this._dataGrid.element.removeStyleClass("small");
            this._timelineGrid.element.removeStyleClass("small");
            this._viewsContainerElement.removeStyleClass("small");
        }
        this._positionSummaryBar();
    },

    _getPopoverAnchor: function(element)
    {
        var anchor = element.enclosingNodeOrSelfWithClass("network-graph-bar") || element.enclosingNodeOrSelfWithClass("network-graph-label");
        if (!anchor)
            return null;
        var resource = anchor.parentElement.resource;
        return resource && resource.timing ? anchor : null;
    },

    _showPopover: function(anchor)
    {
        var resource = anchor.parentElement.resource;
        var tableElement = WebInspector.ResourceTimingView.createTimingTable(resource);
        var popover = new WebInspector.Popover(tableElement);
        popover.show(anchor);
        return popover;
    },

    _toggleGridMode: function()
    {
        if (this._viewingResourceMode) {
            this._viewingResourceMode = false;
            this.element.removeStyleClass("viewing-resource");
            this._dataGrid.element.removeStyleClass("viewing-resource-mode");
            this._viewsContainerElement.addStyleClass("hidden");
            this.sidebarElement.style.right = 0;
            this.sidebarElement.style.removeProperty("width");
            if (this._dataGrid.selectedNode)
                this._dataGrid.selectedNode.selected = false;
        }

        if (this._briefGrid) {
            this._dataGrid.element.removeStyleClass("full-grid-mode");
            this._dataGrid.element.addStyleClass("brief-grid-mode");

            this._dataGrid.hideColumn("method");
            this._dataGrid.hideColumn("status");
            this._dataGrid.hideColumn("type");
            this._dataGrid.hideColumn("size");
            this._dataGrid.hideColumn("time");

            var widths = {};
            widths.name = 20;
            widths.timeline = 80;
        } else {
            this._dataGrid.element.addStyleClass("full-grid-mode");
            this._dataGrid.element.removeStyleClass("brief-grid-mode");

            this._dataGrid.showColumn("method");
            this._dataGrid.showColumn("status");
            this._dataGrid.showColumn("type");
            this._dataGrid.showColumn("size");
            this._dataGrid.showColumn("time");

            var widths = {};
            widths.name = 20;
            widths.method = 7;
            widths.status = 8;
            widths.type = 10;
            widths.size = 10;
            widths.time = 10;
            widths.timeline = 37;
        }

        this._dataGrid.showColumn("timeline");
        this._dataGrid.applyColumnWidthsMap(widths);

    },

    _toggleViewingResourceMode: function()
    {
        if (this._viewingResourceMode)
            return;
        this._viewingResourceMode = true;
        this._preservedColumnWidths = this._dataGrid.columnWidthsMap();

        this.element.addStyleClass("viewing-resource");
        this._dataGrid.element.addStyleClass("viewing-resource-mode");
        this._dataGrid.element.removeStyleClass("full-grid-mode");
        this._dataGrid.element.removeStyleClass("brief-grid-mode");

        this._dataGrid.hideColumn("method");
        this._dataGrid.hideColumn("status");
        this._dataGrid.hideColumn("type");
        this._dataGrid.hideColumn("size");
        this._dataGrid.hideColumn("time");
        this._dataGrid.hideColumn("timeline");

        this._viewsContainerElement.removeStyleClass("hidden");
        this.updateSidebarWidth(200);

        var widths = {};
        widths.name = 100;
        this._dataGrid.applyColumnWidthsMap(widths);
    },

    _contextMenu: function(event)
    {
        // createBlobURL is enabled conditionally, do not expose resource export if it's not available.
        if (typeof window.webkitURL.createObjectURL !== "function" || !Preferences.resourceExportEnabled)
            return;

        var contextMenu = new WebInspector.ContextMenu();
        var gridNode = this._dataGrid.dataGridNodeFromNode(event.target);
        var resource = gridNode && gridNode._resource;
        if (resource)
            contextMenu.appendItem(WebInspector.UIString("Export to HAR"), this._exportResource.bind(this, resource));
        contextMenu.appendItem(WebInspector.UIString("Export all to HAR"), this._exportAll.bind(this));
        contextMenu.show(event);
    },

    _exportAll: function()
    {
        var harArchive = {
            log: (new WebInspector.HARLog()).build()
        }
        InspectorFrontendHost.copyText(JSON.stringify(harArchive));
    },

    _exportResource: function(resource)
    {
        var har = (new WebInspector.HAREntry(resource)).build();
        InspectorFrontendHost.copyText(JSON.stringify(har));
    },

    _updateOffscreenRows: function(e)
    {
        var dataTableBody = this._dataGrid.dataTableBody;
        var rows = dataTableBody.children;
        var recordsCount = rows.length;
        if (recordsCount < 2)
            return;  // Filler row only.

        var visibleTop = this._dataGrid.scrollContainer.scrollTop;
        var visibleBottom = visibleTop + this._dataGrid.scrollContainer.offsetHeight;

        var rowHeight = 0;

        // Filler is at recordsCount - 1.
        var unfilteredRowIndex = 0;
        for (var i = 0; i < recordsCount - 1; ++i) {
            var row = rows[i];
            // Don't touch summaty - quit instead.
            if (this._summaryBarRowNode && row === this._summaryBarRowNode.element)
                break;

            var dataGridNode = this._dataGrid.dataGridNodeFromNode(row);
            if (dataGridNode.isFilteredOut()) {
                row.removeStyleClass("offscreen");
                continue;
            }

            if (!rowHeight)
                rowHeight = row.offsetHeight;

            var rowIsVisible = unfilteredRowIndex * rowHeight < visibleBottom && (unfilteredRowIndex + 1) * rowHeight > visibleTop;
            if (rowIsVisible !== row.rowIsVisible) {
                if (rowIsVisible)
                    row.removeStyleClass("offscreen");
                else
                    row.addStyleClass("offscreen");
                row.rowIsVisible = rowIsVisible;
            }
            unfilteredRowIndex++;
        }
    }
}

WebInspector.NetworkPanel.prototype.__proto__ = WebInspector.Panel.prototype;

WebInspector.NetworkBaseCalculator = function()
{
}

WebInspector.NetworkBaseCalculator.prototype = {
    computeSummaryValues: function(items)
    {
        var total = 0;
        var categoryValues = {};

        var itemsLength = items.length;
        for (var i = 0; i < itemsLength; ++i) {
            var item = items[i];
            var value = this._value(item);
            if (typeof value === "undefined")
                continue;
            if (!(item.category.name in categoryValues))
                categoryValues[item.category.name] = 0;
            categoryValues[item.category.name] += value;
            total += value;
        }

        return {categoryValues: categoryValues, total: total};
    },

    computeBarGraphPercentages: function(item)
    {
        return {start: 0, middle: 0, end: (this._value(item) / this.boundarySpan) * 100};
    },

    computeBarGraphLabels: function(item)
    {
        var label = this.formatValue(this._value(item));
        return {left: label, right: label, tooltip: label};
    },

    get boundarySpan()
    {
        return this.maximumBoundary - this.minimumBoundary;
    },

    updateBoundaries: function(item)
    {
        this.minimumBoundary = 0;

        var value = this._value(item);
        if (typeof this.maximumBoundary === "undefined" || value > this.maximumBoundary) {
            this.maximumBoundary = value;
            return true;
        }
        return false;
    },

    reset: function()
    {
        delete this.minimumBoundary;
        delete this.maximumBoundary;
    },

    _value: function(item)
    {
        return 0;
    },

    formatValue: function(value)
    {
        return value.toString();
    }
}

WebInspector.NetworkTimeCalculator = function(startAtZero)
{
    WebInspector.NetworkBaseCalculator.call(this);
    this.startAtZero = startAtZero;
}

WebInspector.NetworkTimeCalculator.prototype = {
    computeSummaryValues: function(resources)
    {
        var resourcesByCategory = {};
        var resourcesLength = resources.length;
        for (var i = 0; i < resourcesLength; ++i) {
            var resource = resources[i];
            if (!(resource.category.name in resourcesByCategory))
                resourcesByCategory[resource.category.name] = [];
            resourcesByCategory[resource.category.name].push(resource);
        }

        var earliestStart;
        var latestEnd;
        var categoryValues = {};
        for (var category in resourcesByCategory) {
            resourcesByCategory[category].sort(WebInspector.Resource.CompareByTime);
            categoryValues[category] = 0;

            var segment = {start: -1, end: -1};

            var categoryResources = resourcesByCategory[category];
            var resourcesLength = categoryResources.length;
            for (var i = 0; i < resourcesLength; ++i) {
                var resource = categoryResources[i];
                if (resource.startTime === -1 || resource.endTime === -1)
                    continue;

                if (typeof earliestStart === "undefined")
                    earliestStart = resource.startTime;
                else
                    earliestStart = Math.min(earliestStart, resource.startTime);

                if (typeof latestEnd === "undefined")
                    latestEnd = resource.endTime;
                else
                    latestEnd = Math.max(latestEnd, resource.endTime);

                if (resource.startTime <= segment.end) {
                    segment.end = Math.max(segment.end, resource.endTime);
                    continue;
                }

                categoryValues[category] += segment.end - segment.start;

                segment.start = resource.startTime;
                segment.end = resource.endTime;
            }

            // Add the last segment
            categoryValues[category] += segment.end - segment.start;
        }

        return {categoryValues: categoryValues, total: latestEnd - earliestStart};
    },

    computeBarGraphPercentages: function(resource)
    {
        if (resource.startTime !== -1)
            var start = ((resource.startTime - this.minimumBoundary) / this.boundarySpan) * 100;
        else
            var start = 0;

        if (resource.responseReceivedTime !== -1)
            var middle = ((resource.responseReceivedTime - this.minimumBoundary) / this.boundarySpan) * 100;
        else
            var middle = (this.startAtZero ? start : 100);

        if (resource.endTime !== -1)
            var end = ((resource.endTime - this.minimumBoundary) / this.boundarySpan) * 100;
        else
            var end = (this.startAtZero ? middle : 100);

        if (this.startAtZero) {
            end -= start;
            middle -= start;
            start = 0;
        }

        return {start: start, middle: middle, end: end};
    },

    computePercentageFromEventTime: function(eventTime)
    {
        // This function computes a percentage in terms of the total loading time
        // of a specific event. If startAtZero is set, then this is useless, and we
        // want to return 0.
        if (eventTime !== -1 && !this.startAtZero)
            return ((eventTime - this.minimumBoundary) / this.boundarySpan) * 100;

        return 0;
    },

    computeBarGraphLabels: function(resource)
    {
        var rightLabel = "";
        if (resource.responseReceivedTime !== -1 && resource.endTime !== -1)
            rightLabel = this.formatValue(resource.endTime - resource.responseReceivedTime);

        var hasLatency = resource.latency > 0;
        if (hasLatency)
            var leftLabel = this.formatValue(resource.latency);
        else
            var leftLabel = rightLabel;

        if (resource.timing)
            return {left: leftLabel, right: rightLabel};

        if (hasLatency && rightLabel) {
            var total = this.formatValue(resource.duration);
            var tooltip = WebInspector.UIString("%s latency, %s download (%s total)", leftLabel, rightLabel, total);
        } else if (hasLatency)
            var tooltip = WebInspector.UIString("%s latency", leftLabel);
        else if (rightLabel)
            var tooltip = WebInspector.UIString("%s download", rightLabel);

        if (resource.cached)
            tooltip = WebInspector.UIString("%s (from cache)", tooltip);
        return {left: leftLabel, right: rightLabel, tooltip: tooltip};
    },

    updateBoundaries: function(resource)
    {
        var didChange = false;

        var lowerBound;
        if (this.startAtZero)
            lowerBound = 0;
        else
            lowerBound = this._lowerBound(resource);

        if (lowerBound !== -1 && (typeof this.minimumBoundary === "undefined" || lowerBound < this.minimumBoundary)) {
            this.minimumBoundary = lowerBound;
            didChange = true;
        }

        var upperBound = this._upperBound(resource);
        if (upperBound !== -1 && (typeof this.maximumBoundary === "undefined" || upperBound > this.maximumBoundary)) {
            this.maximumBoundary = upperBound;
            didChange = true;
        }

        return didChange;
    },

    formatValue: function(value)
    {
        return Number.secondsToString(value);
    },

    _lowerBound: function(resource)
    {
        return 0;
    },

    _upperBound: function(resource)
    {
        return 0;
    }
}

WebInspector.NetworkTimeCalculator.prototype.__proto__ = WebInspector.NetworkBaseCalculator.prototype;

WebInspector.NetworkTransferTimeCalculator = function()
{
    WebInspector.NetworkTimeCalculator.call(this, false);
}

WebInspector.NetworkTransferTimeCalculator.prototype = {
    formatValue: function(value)
    {
        return Number.secondsToString(value);
    },

    _lowerBound: function(resource)
    {
        return resource.startTime;
    },

    _upperBound: function(resource)
    {
        return resource.endTime;
    }
}

WebInspector.NetworkTransferTimeCalculator.prototype.__proto__ = WebInspector.NetworkTimeCalculator.prototype;

WebInspector.NetworkTransferDurationCalculator = function()
{
    WebInspector.NetworkTimeCalculator.call(this, true);
}

WebInspector.NetworkTransferDurationCalculator.prototype = {
    formatValue: function(value)
    {
        return Number.secondsToString(value);
    },

    _upperBound: function(resource)
    {
        return resource.duration;
    }
}

WebInspector.NetworkTransferDurationCalculator.prototype.__proto__ = WebInspector.NetworkTimeCalculator.prototype;

WebInspector.NetworkDataGridNode = function(panel, resource)
{
    WebInspector.DataGridNode.call(this, {});
    this._panel = panel;
    this._resource = resource;
}

WebInspector.NetworkDataGridNode.prototype = {
    createCells: function()
    {
        this._nameCell = this._createDivInTD("name");
        this._methodCell = this._createDivInTD("method");
        this._statusCell = this._createDivInTD("status");
        this._typeCell = this._createDivInTD("type");
        this._sizeCell = this._createDivInTD("size");
        this._timeCell = this._createDivInTD("time");
        this._createTimelineCell();
        this._nameCell.addEventListener("click", this.select.bind(this), false);
        this._nameCell.addEventListener("dblclick", this._openInNewTab.bind(this), false);
    },

    isFilteredOut: function()
    {
        if (!this._panel._hiddenCategories.all)
            return false;
        return this._resource.category.name in this._panel._hiddenCategories;
    },

    select: function()
    {
        this._panel._showResource(this._resource);
        WebInspector.DataGridNode.prototype.select.apply(this, arguments);
    },

    _openInNewTab: function()
    {
        InspectorBackend.openInInspectedWindow(this._resource.url);
    },

    get selectable()
    {
        if (!this._panel._viewingResourceMode)
            return false;
        return !this.isFilteredOut();
    },

    _createDivInTD: function(columnIdentifier)
    {
        var td = document.createElement("td");
        td.className = columnIdentifier + "-column";
        var div = document.createElement("div");
        td.appendChild(div);
        this._element.appendChild(td);
        return div;
    },

    _createTimelineCell: function()
    {
        this._graphElement = document.createElement("div");
        this._graphElement.className = "network-graph-side";

        this._barAreaElement = document.createElement("div");
        //    this._barAreaElement.className = "network-graph-bar-area hidden";
        this._barAreaElement.className = "network-graph-bar-area";
        this._barAreaElement.resource = this._resource;
        this._graphElement.appendChild(this._barAreaElement);

        this._barLeftElement = document.createElement("div");
        this._barLeftElement.className = "network-graph-bar waiting";
        this._barAreaElement.appendChild(this._barLeftElement);

        this._barRightElement = document.createElement("div");
        this._barRightElement.className = "network-graph-bar";
        this._barAreaElement.appendChild(this._barRightElement);


        this._labelLeftElement = document.createElement("div");
        this._labelLeftElement.className = "network-graph-label waiting";
        this._barAreaElement.appendChild(this._labelLeftElement);

        this._labelRightElement = document.createElement("div");
        this._labelRightElement.className = "network-graph-label";
        this._barAreaElement.appendChild(this._labelRightElement);

        this._graphElement.addEventListener("mouseover", this._refreshLabelPositions.bind(this), false);

        this._timelineCell = document.createElement("td");
        this._timelineCell.className = "timeline-column";
        this._element.appendChild(this._timelineCell);
        this._timelineCell.appendChild(this._graphElement);
    },

    refreshResource: function()
    {
        this._refreshNameCell();

        this._methodCell.textContent = this._resource.requestMethod;

        this._refreshStatusCell();

        if (this._resource.mimeType) {
            this._typeCell.removeStyleClass("network-dim-cell");
            this._typeCell.textContent = this._resource.mimeType;
        } else {
            this._typeCell.addStyleClass("network-dim-cell");
            this._typeCell.textContent = WebInspector.UIString("Pending");
        }

        this._refreshSizeCell();
        this._refreshTimeCell();

        if (this._resource.cached)
            this._graphElement.addStyleClass("resource-cached");

        this._element.addStyleClass("network-item");
        if (!this._element.hasStyleClass("network-category-" + this._resource.category.name)) {
            this._element.removeMatchingStyleClasses("network-category-\\w+");
            this._element.addStyleClass("network-category-" + this._resource.category.name);
        }
    },

    _refreshNameCell: function()
    {
        this._nameCell.removeChildren();

        if (this._resource.category === WebInspector.resourceCategories.images) {
            var previewImage = document.createElement("img");
            previewImage.className = "image-network-icon-preview";
            this._resource.populateImageSource(previewImage);

            var iconElement = document.createElement("div");
            iconElement.className = "icon";
            iconElement.appendChild(previewImage);
        } else {
            var iconElement = document.createElement("img");
            iconElement.className = "icon";
        }
        this._nameCell.appendChild(iconElement);
        this._nameCell.appendChild(document.createTextNode(this._fileName()));


        var subtitle = this._resource.displayDomain;

        if (this._resource.path && this._resource.lastPathComponent) {
            var lastPathComponentIndex = this._resource.path.lastIndexOf("/" + this._resource.lastPathComponent);
            if (lastPathComponentIndex != -1)
                subtitle += this._resource.path.substring(0, lastPathComponentIndex);
        }

        this._appendSubtitle(this._nameCell, subtitle);
        this._nameCell.title = this._resource.url;
    },

    _fileName: function()
    {
        var fileName = this._resource.displayName;
        if (this._resource.queryString)
            fileName += "?" + this._resource.queryString;
        return fileName;
    },

    _refreshStatusCell: function()
    {
        this._statusCell.removeChildren();

        var fromCache = this._resource.cached;
        if (fromCache) {
            this._statusCell.textContent = WebInspector.UIString("(from cache)");
            this._statusCell.addStyleClass("network-dim-cell");
            return;
        }

        this._statusCell.removeStyleClass("network-dim-cell");
        if (this._resource.statusCode) {
            this._statusCell.appendChild(document.createTextNode(this._resource.statusCode));
            this._statusCell.removeStyleClass("network-dim-cell");
            this._appendSubtitle(this._statusCell, this._resource.statusText);
            this._statusCell.title = this._resource.statusCode + " " + this._resource.statusText;
        } else {
            this._statusCell.addStyleClass("network-dim-cell");
            this._statusCell.textContent = WebInspector.UIString("Pending");
        }
    },

    _refreshSizeCell: function()
    {
        var resourceSize = typeof this._resource.resourceSize === "number" ? Number.bytesToString(this._resource.resourceSize) : "?";
        var transferSize = typeof this._resource.transferSize === "number" ? Number.bytesToString(this._resource.transferSize) : "?";
        var fromCache = this._resource.cached;
        this._sizeCell.textContent = !fromCache ? resourceSize : WebInspector.UIString("(from cache)");
        if (fromCache)
            this._sizeCell.addStyleClass("network-dim-cell");
        else
            this._sizeCell.removeStyleClass("network-dim-cell");
        if (!fromCache)
            this._appendSubtitle(this._sizeCell, transferSize);
    },

    _refreshTimeCell: function()
    {
        if (this._resource.duration > 0) {
            this._timeCell.removeStyleClass("network-dim-cell");
            this._timeCell.textContent = Number.secondsToString(this._resource.duration);
            this._appendSubtitle(this._timeCell, Number.secondsToString(this._resource.latency));
        } else {
            this._timeCell.addStyleClass("network-dim-cell");
            this._timeCell.textContent = WebInspector.UIString("Pending");
        }
    },

    _appendSubtitle: function(cellElement, subtitleText)
    {
        var subtitleElement = document.createElement("div");
        subtitleElement.className = "network-cell-subtitle";
        subtitleElement.textContent = subtitleText;
        cellElement.appendChild(subtitleElement);
    },

    refreshGraph: function(calculator)
    {
        var percentages = calculator.computeBarGraphPercentages(this._resource);
        this._percentages = percentages;

        this._barAreaElement.removeStyleClass("hidden");

        if (!this._graphElement.hasStyleClass("network-category-" + this._resource.category.name)) {
            this._graphElement.removeMatchingStyleClasses("network-category-\\w+");
            this._graphElement.addStyleClass("network-category-" + this._resource.category.name);
        }

        this._barLeftElement.style.setProperty("left", percentages.start + "%");
        this._barRightElement.style.setProperty("right", (100 - percentages.end) + "%");

        this._barLeftElement.style.setProperty("right", (100 - percentages.end) + "%");
        this._barRightElement.style.setProperty("left", percentages.middle + "%");

        var labels = calculator.computeBarGraphLabels(this._resource);
        this._labelLeftElement.textContent = labels.left;
        this._labelRightElement.textContent = labels.right;

        var tooltip = (labels.tooltip || "");
        this._barLeftElement.title = tooltip;
        this._labelLeftElement.title = tooltip;
        this._labelRightElement.title = tooltip;
        this._barRightElement.title = tooltip;
    },

    _refreshLabelPositions: function()
    {
        if (!this._percentages)
            return;
        this._labelLeftElement.style.removeProperty("left");
        this._labelLeftElement.style.removeProperty("right");
        this._labelLeftElement.removeStyleClass("before");
        this._labelLeftElement.removeStyleClass("hidden");

        this._labelRightElement.style.removeProperty("left");
        this._labelRightElement.style.removeProperty("right");
        this._labelRightElement.removeStyleClass("after");
        this._labelRightElement.removeStyleClass("hidden");

        var labelPadding = 10;
        var barRightElementOffsetWidth = this._barRightElement.offsetWidth;
        var barLeftElementOffsetWidth = this._barLeftElement.offsetWidth;

        if (this._barLeftElement) {
            var leftBarWidth = barLeftElementOffsetWidth - labelPadding;
            var rightBarWidth = (barRightElementOffsetWidth - barLeftElementOffsetWidth) - labelPadding;
        } else {
            var leftBarWidth = (barLeftElementOffsetWidth - barRightElementOffsetWidth) - labelPadding;
            var rightBarWidth = barRightElementOffsetWidth - labelPadding;
        }

        var labelLeftElementOffsetWidth = this._labelLeftElement.offsetWidth;
        var labelRightElementOffsetWidth = this._labelRightElement.offsetWidth;

        var labelBefore = (labelLeftElementOffsetWidth > leftBarWidth);
        var labelAfter = (labelRightElementOffsetWidth > rightBarWidth);
        var graphElementOffsetWidth = this._graphElement.offsetWidth;

        if (labelBefore && (graphElementOffsetWidth * (this._percentages.start / 100)) < (labelLeftElementOffsetWidth + 10))
            var leftHidden = true;

        if (labelAfter && (graphElementOffsetWidth * ((100 - this._percentages.end) / 100)) < (labelRightElementOffsetWidth + 10))
            var rightHidden = true;

        if (barLeftElementOffsetWidth == barRightElementOffsetWidth) {
            // The left/right label data are the same, so a before/after label can be replaced by an on-bar label.
            if (labelBefore && !labelAfter)
                leftHidden = true;
            else if (labelAfter && !labelBefore)
                rightHidden = true;
        }

        if (labelBefore) {
            if (leftHidden)
                this._labelLeftElement.addStyleClass("hidden");
            this._labelLeftElement.style.setProperty("right", (100 - this._percentages.start) + "%");
            this._labelLeftElement.addStyleClass("before");
        } else {
            this._labelLeftElement.style.setProperty("left", this._percentages.start + "%");
            this._labelLeftElement.style.setProperty("right", (100 - this._percentages.middle) + "%");
        }

        if (labelAfter) {
            if (rightHidden)
                this._labelRightElement.addStyleClass("hidden");
            this._labelRightElement.style.setProperty("left", this._percentages.end + "%");
            this._labelRightElement.addStyleClass("after");
        } else {
            this._labelRightElement.style.setProperty("left", this._percentages.middle + "%");
            this._labelRightElement.style.setProperty("right", (100 - this._percentages.end) + "%");
        }
    }
}

WebInspector.NetworkDataGridNode.NameComparator = function(a, b)
{
    var aFileName = a._resource.displayName + (a._resource.queryString ? a._resource.queryString : "");
    var bFileName = b._resource.displayName + (b._resource.queryString ? b._resource.queryString : "");
    if (aFileName > bFileName)
        return 1;
    if (bFileName > aFileName)
        return -1;
    return 0;
}

WebInspector.NetworkDataGridNode.SizeComparator = function(a, b)
{
    if (b._resource.cached && !a._resource.cached)
        return 1;
    if (a._resource.cached && !b._resource.cached)
        return -1;

    if (a._resource.resourceSize === b._resource.resourceSize)
        return 0;

    return a._resource.resourceSize - b._resource.resourceSize;
}

WebInspector.NetworkDataGridNode.ResourcePropertyComparator = function(propertyName, revert, a, b)
{
    var aValue = a._resource[propertyName];
    var bValue = b._resource[propertyName];
    if (aValue > bValue)
        return revert ? -1 : 1;
    if (bValue > aValue)
        return revert ? 1 : -1;
    return 0;
}

WebInspector.NetworkDataGridNode.prototype.__proto__ = WebInspector.DataGridNode.prototype;

WebInspector.NetworkTotalGridNode = function(element)
{
    this._summaryBarElement = element;
    WebInspector.DataGridNode.call(this, {summaryRow: true});
}

WebInspector.NetworkTotalGridNode.prototype = {
    isFilteredOut: function()
    {
        return false;
    },

    get selectable()
    {
        return false;
    },

    createCells: function()
    {
        var td = document.createElement("td");
        td.setAttribute("colspan", 7);
        td.className = "network-summary";
        td.appendChild(this._summaryBarElement);
        this._element.appendChild(td);
    }
}

WebInspector.NetworkTotalGridNode.prototype.__proto__ = WebInspector.DataGridNode.prototype;
