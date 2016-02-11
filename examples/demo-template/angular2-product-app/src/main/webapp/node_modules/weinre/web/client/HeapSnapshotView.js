/*
 * Copyright (C) 2010 Google Inc. All rights reserved.
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

WebInspector.HeapSnapshotView = function(parent, profile)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("heap-snapshot-view");

    this.parent = parent;
    this.parent.addEventListener("profile added", this._updateBaseOptions, this);

    this.showCountAsPercent = false;
    this.showSizeAsPercent = false;
    this.showCountDeltaAsPercent = false;
    this.showSizeDeltaAsPercent = false;

    this.categories = {
        code: new WebInspector.ResourceCategory("code", WebInspector.UIString("Code"), "rgb(255,121,0)"),
        data: new WebInspector.ResourceCategory("data", WebInspector.UIString("Objects"), "rgb(47,102,236)")
    };

    var summaryContainer = document.createElement("div");
    summaryContainer.id = "heap-snapshot-summary-container";

    this.countsSummaryBar = new WebInspector.SummaryBar(this.categories);
    this.countsSummaryBar.element.className = "heap-snapshot-summary";
    this.countsSummaryBar.calculator = new WebInspector.HeapSummaryCountCalculator();
    var countsLabel = document.createElement("div");
    countsLabel.className = "heap-snapshot-summary-label";
    countsLabel.textContent = WebInspector.UIString("Count");
    this.countsSummaryBar.element.appendChild(countsLabel);
    summaryContainer.appendChild(this.countsSummaryBar.element);

    this.sizesSummaryBar = new WebInspector.SummaryBar(this.categories);
    this.sizesSummaryBar.element.className = "heap-snapshot-summary";
    this.sizesSummaryBar.calculator = new WebInspector.HeapSummarySizeCalculator();
    var sizesLabel = document.createElement("label");
    sizesLabel.className = "heap-snapshot-summary-label";
    sizesLabel.textContent = WebInspector.UIString("Size");
    this.sizesSummaryBar.element.appendChild(sizesLabel);
    summaryContainer.appendChild(this.sizesSummaryBar.element);

    this.element.appendChild(summaryContainer);

    var columns = {
        cons: { title: WebInspector.UIString("Constructor"), disclosure: true, sortable: true },
        count: { title: WebInspector.UIString("Count"), width: "54px", sortable: true },
        size: { title: WebInspector.UIString("Size"), width: "72px", sort: "descending", sortable: true },
        // \xb1 is a "plus-minus" sign.
        countDelta: { title: WebInspector.UIString("\xb1 Count"), width: "72px", sortable: true },
        sizeDelta: { title: WebInspector.UIString("\xb1 Size"), width: "72px", sortable: true }
    };

    this.dataGrid = new WebInspector.DataGrid(columns);
    this.dataGrid.addEventListener("sorting changed", this._sortData, this);
    this.dataGrid.element.addEventListener("mousedown", this._mouseDownInDataGrid.bind(this), true);
    this.element.appendChild(this.dataGrid.element);

    this.profile = profile;

    this.baseSelectElement = document.createElement("select");
    this.baseSelectElement.className = "status-bar-item";
    this.baseSelectElement.addEventListener("change", this._changeBase.bind(this), false);
    this._updateBaseOptions();

    this.percentButton = new WebInspector.StatusBarButton("", "percent-time-status-bar-item status-bar-item");
    this.percentButton.addEventListener("click", this._percentClicked.bind(this), false);

    this._loadProfile(this.profile, profileCallback.bind(this));

    function profileCallback(profile)
    {
        var list = this._profiles();
        var profileIndex;
        for (var i = 0; i < list.length; ++i)
            if (list[i].uid === profile.uid) {
                profileIndex = i;
                break;
            }
        if (profileIndex > 0)
            this.baseSelectElement.selectedIndex = profileIndex - 1;
        else
            this.baseSelectElement.selectedIndex = profileIndex;
        this._resetDataGridList(resetCompleted.bind(this));
    }

    function resetCompleted()
    {
        this.refresh();
        this._updatePercentButton();
    }
}

WebInspector.HeapSnapshotView.prototype = {
    get statusBarItems()
    {
        return [this.baseSelectElement, this.percentButton.element];
    },

    get profile()
    {
        return this._profile;
    },

    set profile(profile)
    {
        this._profile = profile;
    },

    show: function(parentElement)
    {
        WebInspector.View.prototype.show.call(this, parentElement);
        this.dataGrid.updateWidths();
    },

    hide: function()
    {
        WebInspector.View.prototype.hide.call(this);
        this._currentSearchResultIndex = -1;
    },

    resize: function()
    {
        if (this.dataGrid)
            this.dataGrid.updateWidths();
    },

    refresh: function()
    {
        this.dataGrid.removeChildren();

        var children = this.snapshotDataGridList.children;
        var count = children.length;
        for (var index = 0; index < count; ++index)
            this.dataGrid.appendChild(children[index]);

        this._updateSummaryGraph();
    },

    refreshShowAsPercents: function()
    {
        this._updatePercentButton();
        this.refreshVisibleData();
    },

    _deleteSearchMatchedFlags: function(node)
    {
        delete node._searchMatchedConsColumn;
        delete node._searchMatchedCountColumn;
        delete node._searchMatchedSizeColumn;
        delete node._searchMatchedCountDeltaColumn;
        delete node._searchMatchedSizeDeltaColumn;
    },

    searchCanceled: function()
    {
        if (this._searchResults) {
            for (var i = 0; i < this._searchResults.length; ++i) {
                var profileNode = this._searchResults[i].profileNode;
                this._deleteSearchMatchedFlags(profileNode);
                profileNode.refresh();
            }
        }

        delete this._searchFinishedCallback;
        this._currentSearchResultIndex = -1;
        this._searchResults = [];
    },

    performSearch: function(query, finishedCallback)
    {
        // Call searchCanceled since it will reset everything we need before doing a new search.
        this.searchCanceled();

        query = query.trim();

        if (!query.length)
            return;

        this._searchFinishedCallback = finishedCallback;

        var helper = WebInspector.HeapSnapshotView.SearchHelper;

        var operationAndNumber = helper.parseOperationAndNumber(query);
        var operation = operationAndNumber[0];
        var queryNumber = operationAndNumber[1];

        var percentUnits = helper.percents.test(query);
        var megaBytesUnits = helper.megaBytes.test(query);
        var kiloBytesUnits = helper.kiloBytes.test(query);
        var bytesUnits = helper.bytes.test(query);

        var queryNumberBytes = (megaBytesUnits ? (queryNumber * 1024 * 1024) : (kiloBytesUnits ? (queryNumber * 1024) : queryNumber));

        function matchesQuery(heapSnapshotDataGridNode)
        {
            WebInspector.HeapSnapshotView.prototype._deleteSearchMatchedFlags(heapSnapshotDataGridNode);

            if (percentUnits) {
                heapSnapshotDataGridNode._searchMatchedCountColumn = operation(heapSnapshotDataGridNode.countPercent, queryNumber);
                heapSnapshotDataGridNode._searchMatchedSizeColumn = operation(heapSnapshotDataGridNode.sizePercent, queryNumber);
                heapSnapshotDataGridNode._searchMatchedCountDeltaColumn = operation(heapSnapshotDataGridNode.countDeltaPercent, queryNumber);
                heapSnapshotDataGridNode._searchMatchedSizeDeltaColumn = operation(heapSnapshotDataGridNode.sizeDeltaPercent, queryNumber);
            } else if (megaBytesUnits || kiloBytesUnits || bytesUnits) {
                heapSnapshotDataGridNode._searchMatchedSizeColumn = operation(heapSnapshotDataGridNode.size, queryNumberBytes);
                heapSnapshotDataGridNode._searchMatchedSizeDeltaColumn = operation(heapSnapshotDataGridNode.sizeDelta, queryNumberBytes);
            } else {
                heapSnapshotDataGridNode._searchMatchedCountColumn = operation(heapSnapshotDataGridNode.count, queryNumber);
                heapSnapshotDataGridNode._searchMatchedCountDeltaColumn = operation(heapSnapshotDataGridNode.countDelta, queryNumber);
            }

            if (heapSnapshotDataGridNode.constructorName.hasSubstring(query, true))
                heapSnapshotDataGridNode._searchMatchedConsColumn = true;

            if (heapSnapshotDataGridNode._searchMatchedConsColumn ||
                heapSnapshotDataGridNode._searchMatchedCountColumn ||
                heapSnapshotDataGridNode._searchMatchedSizeColumn ||
                heapSnapshotDataGridNode._searchMatchedCountDeltaColumn ||
                heapSnapshotDataGridNode._searchMatchedSizeDeltaColumn) {
                heapSnapshotDataGridNode.refresh();
                return true;
            }

            return false;
        }

        var current = this.snapshotDataGridList.children[0];
        var depth = 0;
        var info = {};

        // The second and subsequent levels of heap snapshot nodes represent retainers,
        // so recursive expansion will be infinite, since a graph is being traversed.
        // So default to a recursion cap of 2 levels.
        var maxDepth = 2;

        while (current) {
            if (matchesQuery(current))
                this._searchResults.push({ profileNode: current });
            current = current.traverseNextNode(false, null, (depth >= maxDepth), info);
            depth += info.depthChange;
        }

        finishedCallback(this, this._searchResults.length);
    },

    // FIXME: move these methods to a superclass, inherit both views from it.
    jumpToFirstSearchResult: WebInspector.CPUProfileView.prototype.jumpToFirstSearchResult,
    jumpToLastSearchResult: WebInspector.CPUProfileView.prototype.jumpToLastSearchResult,
    jumpToNextSearchResult: WebInspector.CPUProfileView.prototype.jumpToNextSearchResult,
    jumpToPreviousSearchResult: WebInspector.CPUProfileView.prototype.jumpToPreviousSearchResult,
    showingFirstSearchResult: WebInspector.CPUProfileView.prototype.showingFirstSearchResult,
    showingLastSearchResult: WebInspector.CPUProfileView.prototype.showingLastSearchResult,
    _jumpToSearchResult: WebInspector.CPUProfileView.prototype._jumpToSearchResult,

    refreshVisibleData: function()
    {
        var child = this.dataGrid.children[0];
        while (child) {
            child.refresh();
            child = child.traverseNextNode(false, null, true);
        }
        this._updateSummaryGraph();
    },

    _changeBase: function()
    {
        if (this.baseSnapshot.uid === this._profiles()[this.baseSelectElement.selectedIndex].uid)
            return;

        this._resetDataGridList(resetCompleted.bind(this));

        function resetCompleted() {
            this.refresh();

            if (!this.currentQuery || !this._searchFinishedCallback || !this._searchResults)
                return;

            // The current search needs to be performed again. First negate out previous match
            // count by calling the search finished callback with a negative number of matches.
            // Then perform the search again with the same query and callback.
            this._searchFinishedCallback(this, -this._searchResults.length);
            this.performSearch(this.currentQuery, this._searchFinishedCallback);
        }
    },

    _createSnapshotDataGridList: function()
    {
        if (this._snapshotDataGridList)
          delete this._snapshotDataGridList;

        this._snapshotDataGridList = new WebInspector.HeapSnapshotDataGridList(this, this.baseSnapshot.entries, this.profile.entries);
        return this._snapshotDataGridList;
    },

    _profiles: function()
    {
        return WebInspector.panels.profiles.getProfiles(WebInspector.HeapSnapshotProfileType.TypeId);
    },

    _loadProfile: function(profile, callback)
    {
        WebInspector.panels.profiles.loadHeapSnapshot(profile.uid, callback);
    },

    processLoadedSnapshot: function(profile, loadedSnapshot)
    {
        var snapshot = WebInspector.HeapSnapshotView.prototype._convertSnapshot(loadedSnapshot);
        profile.children = snapshot.children;
        profile.entries = snapshot.entries;
        profile.lowlevels = snapshot.lowlevels;
        WebInspector.HeapSnapshotView.prototype._prepareProfile(profile);
    },

    _mouseDownInDataGrid: function(event)
    {
        if (event.detail < 2)
            return;

        var cell = event.target.enclosingNodeOrSelfWithNodeName("td");
        if (!cell || (!cell.hasStyleClass("count-column") && !cell.hasStyleClass("size-column") && !cell.hasStyleClass("countDelta-column") && !cell.hasStyleClass("sizeDelta-column")))
            return;

        if (cell.hasStyleClass("count-column"))
            this.showCountAsPercent = !this.showCountAsPercent;
        else if (cell.hasStyleClass("size-column"))
            this.showSizeAsPercent = !this.showSizeAsPercent;
        else if (cell.hasStyleClass("countDelta-column"))
            this.showCountDeltaAsPercent = !this.showCountDeltaAsPercent;
        else if (cell.hasStyleClass("sizeDelta-column"))
            this.showSizeDeltaAsPercent = !this.showSizeDeltaAsPercent;

        this.refreshShowAsPercents();

        event.preventDefault();
        event.stopPropagation();
    },

    get _isShowingAsPercent()
    {
        return this.showCountAsPercent && this.showSizeAsPercent && this.showCountDeltaAsPercent && this.showSizeDeltaAsPercent;
    },

    _percentClicked: function(event)
    {
        var currentState = this._isShowingAsPercent;
        this.showCountAsPercent = !currentState;
        this.showSizeAsPercent = !currentState;
        this.showCountDeltaAsPercent = !currentState;
        this.showSizeDeltaAsPercent = !currentState;
        this.refreshShowAsPercents();
    },

    _convertSnapshot: function(loadedSnapshot)
    {
        var snapshot = new WebInspector.HeapSnapshot(loadedSnapshot);
        var result = {lowlevels: {}, entries: {}, children: {}};
        var rootEdgesIter = snapshot.rootNode.edges;
        for (var iter = rootEdgesIter; iter.hasNext(); iter.next()) {
            var node = iter.edge.node;
            if (node.isHidden)
                result.lowlevels[node.name] = {count: node.instancesCount, size: node.selfSize, type: node.name};
            else if (node.instancesCount)
                result.entries[node.name] = {constructorName: node.name, count: node.instancesCount, size: node.selfSize};
            else {
                var entry = {constructorName: node.name};
                for (var innerIter = node.edges; innerIter.hasNext(); innerIter.next()) {
                    var edge = innerIter.edge;
                    entry[edge.nodeIndex] = {constructorName: edge.node.name, count: edge.name};
                }
                result.children[rootEdgesIter.edge.nodeIndex] = entry;
            }
        }
        return result;
    },

    _prepareProfile: function(profile)
    {
        for (var profileEntry in profile.entries)
            profile.entries[profileEntry].retainers = {};
        profile.clusters = {};

        for (var addr in profile.children) {
            var retainer = profile.children[addr];
            var retainerId = retainer.constructorName + ":" + addr;
            for (var childAddr in retainer) {
                if (childAddr === "constructorName") continue;
                var item = retainer[childAddr];
                var itemId = item.constructorName + ":" + childAddr;
                if ((item.constructorName === "Object" || item.constructorName === "Array")) {
                    if (!(itemId in profile.clusters))
                        profile.clusters[itemId] = { constructorName: itemId, retainers: {} };
                    mergeRetainers(profile.clusters[itemId], item);
                }
                mergeRetainers(profile.entries[item.constructorName], item);
            }
        }

        function mergeRetainers(entry, item)
        {
            if (!(retainer.constructorName in entry.retainers))
               entry.retainers[retainer.constructorName] = { constructorName: retainer.constructorName, count: 0, clusters: {} };
            var retainerEntry = entry.retainers[retainer.constructorName];
            retainerEntry.count += item.count;
            if (retainer.constructorName === "Object" || retainer.constructorName === "Array")
                retainerEntry.clusters[retainerId] = true;
        }
    },

    _resetDataGridList: function(callback)
    {
        this._loadProfile(this._profiles()[this.baseSelectElement.selectedIndex], profileLoaded.bind(this));

        function profileLoaded(profile)
        {
            this.baseSnapshot = profile;
            var lastComparator = WebInspector.HeapSnapshotDataGridList.propertyComparator("size", false);
            if (this.snapshotDataGridList)
                lastComparator = this.snapshotDataGridList.lastComparator;
            this.snapshotDataGridList = this._createSnapshotDataGridList();
            this.snapshotDataGridList.sort(lastComparator, true);
            callback();
        }
    },

    _sortData: function()
    {
        var sortAscending = this.dataGrid.sortOrder === "ascending";
        var sortColumnIdentifier = this.dataGrid.sortColumnIdentifier;
        var sortProperty = {
            cons: ["constructorName", null],
            count: ["count", "constructorName"],
            size: ["size", "constructorName"],
            countDelta: [this.showCountDeltaAsPercent ? "countDeltaPercent" : "countDelta", "constructorName"],
            sizeDelta: [this.showSizeDeltaAsPercent ? "sizeDeltaPercent" : "sizeDelta", "constructorName"]
        }[sortColumnIdentifier];

        this.snapshotDataGridList.sort(WebInspector.HeapSnapshotDataGridList.propertyComparator(sortProperty[0], sortProperty[1], sortAscending));

        this.refresh();
    },

    _updateBaseOptions: function()
    {
        var list = this._profiles();
        // We're assuming that snapshots can only be added.
        if (this.baseSelectElement.length === list.length)
            return;

        for (var i = this.baseSelectElement.length, n = list.length; i < n; ++i) {
            var baseOption = document.createElement("option");
            var title = list[i].title;
            if (!title.indexOf(UserInitiatedProfileName))
                title = WebInspector.UIString("Snapshot %d", title.substring(UserInitiatedProfileName.length + 1));
            baseOption.label = WebInspector.UIString("Compared to %s", title);
            this.baseSelectElement.appendChild(baseOption);
        }
    },

    _updatePercentButton: function()
    {
        if (this._isShowingAsPercent) {
            this.percentButton.title = WebInspector.UIString("Show absolute counts and sizes.");
            this.percentButton.toggled = true;
        } else {
            this.percentButton.title = WebInspector.UIString("Show counts and sizes as percentages.");
            this.percentButton.toggled = false;
        }
    },

    _updateSummaryGraph: function()
    {
        this.countsSummaryBar.calculator.showAsPercent = this._isShowingAsPercent;
        this.countsSummaryBar.update(this.profile.lowlevels);

        this.sizesSummaryBar.calculator.showAsPercent = this._isShowingAsPercent;
        this.sizesSummaryBar.update(this.profile.lowlevels);
    }
};

WebInspector.HeapSnapshotView.prototype.__proto__ = WebInspector.View.prototype;

WebInspector.HeapSnapshotView.SearchHelper = {
    // In comparators, we assume that a value from a node is passed as the first parameter.
    operations: {
        LESS: function (a, b) { return a !== null && a < b; },
        LESS_OR_EQUAL: function (a, b) { return a !== null && a <= b; },
        EQUAL: function (a, b) { return a !== null && a === b; },
        GREATER_OR_EQUAL: function (a, b) { return a !== null && a >= b; },
        GREATER: function (a, b) { return a !== null && a > b; }
    },

    operationParsers: {
        LESS: /^<(\d+)/,
        LESS_OR_EQUAL: /^<=(\d+)/,
        GREATER_OR_EQUAL: /^>=(\d+)/,
        GREATER: /^>(\d+)/
    },

    parseOperationAndNumber: function(query)
    {
        var operations = WebInspector.HeapSnapshotView.SearchHelper.operations;
        var parsers = WebInspector.HeapSnapshotView.SearchHelper.operationParsers;
        for (var operation in parsers) {
            var match = query.match(parsers[operation]);
            if (match !== null)
                return [operations[operation], parseFloat(match[1])];
        }
        return [operations.EQUAL, parseFloat(query)];
    },

    percents: /%$/,

    megaBytes: /MB$/i,

    kiloBytes: /KB$/i,

    bytes: /B$/i
}

WebInspector.HeapSummaryCalculator = function(lowLevelField)
{
    this.total = 1;
    this.lowLevelField = lowLevelField;
}

WebInspector.HeapSummaryCalculator.prototype = {
    computeSummaryValues: function(lowLevels)
    {
        var highLevels = { data: 0, code: 0 };
        this.total = 0;
        for (var item in lowLevels) {
            var highItem = this._highFromLow(item);
            if (highItem) {
                var value = lowLevels[item][this.lowLevelField];
                highLevels[highItem] += value;
                this.total += value;
            }
        }
        var result = { categoryValues: highLevels };
        if (!this.showAsPercent)
            result.total = this.total;
        return result;
    },

    formatValue: function(value)
    {
        if (this.showAsPercent)
            return WebInspector.UIString("%.2f%%", value / this.total * 100.0);
        else
            return this._valueToString(value);
    },

    get showAsPercent()
    {
        return this._showAsPercent;
    },

    set showAsPercent(x)
    {
        this._showAsPercent = x;
    }
}

WebInspector.HeapSummaryCountCalculator = function()
{
    WebInspector.HeapSummaryCalculator.call(this, "count");
}

WebInspector.HeapSummaryCountCalculator.prototype = {
    _highFromLow: function(type)
    {
        if (type === "CODE_TYPE" || type === "SHARED_FUNCTION_INFO_TYPE" || type === "SCRIPT_TYPE") return "code";
        if (type === "STRING_TYPE" || type === "HEAP_NUMBER_TYPE" || type.match(/^JS_/)) return "data";
        return null;
    },

    _valueToString: function(value)
    {
        return value.toString();
    }
}

WebInspector.HeapSummaryCountCalculator.prototype.__proto__ = WebInspector.HeapSummaryCalculator.prototype;

WebInspector.HeapSummarySizeCalculator = function()
{
    WebInspector.HeapSummaryCalculator.call(this, "size");
}

WebInspector.HeapSummarySizeCalculator.prototype = {
    _highFromLow: function(type)
    {
        if (type === "CODE_TYPE" || type === "SHARED_FUNCTION_INFO_TYPE" || type === "SCRIPT_TYPE")
            return "code";
        if (type === "STRING_TYPE" || type === "HEAP_NUMBER_TYPE" || type.match(/^JS_/) || type.match(/_ARRAY_TYPE$/))
            return "data";
        return null;
    },

    _valueToString: Number.bytesToString
}

WebInspector.HeapSummarySizeCalculator.prototype.__proto__ = WebInspector.HeapSummaryCalculator.prototype;

WebInspector.HeapSnapshotDataGridNodeWithRetainers = function(owningTree)
{
    this.tree = owningTree;

    WebInspector.DataGridNode.call(this, null, this._hasRetainers);

    this.addEventListener("populate", this._populate, this);
};

WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype = {
    isEmptySet: function(set)
    {
        for (var x in set)
            return false;
        return true;
    },

    get _hasRetainers()
    {
        return !this.isEmptySet(this.retainers);
    },

    get _parent()
    {
        // For top-level nodes, return owning tree as a parent, not data grid.
        return this.parent !== this.dataGrid ? this.parent : this.tree;
    },

    _populate: function(event)
    {
        function appendDiffEntry(baseItem, snapshotItem)
        {
            this.appendChild(new WebInspector.HeapSnapshotDataGridRetainerNode(this.snapshotView, baseItem, snapshotItem, this.tree));
        }

        this.produceDiff(this.baseRetainers, this.retainers, appendDiffEntry.bind(this));

        if (this._parent) {
            var currentComparator = this._parent.lastComparator;
            if (currentComparator)
                this.sort(currentComparator, true);
        }

        this.removeEventListener("populate", this._populate, this);
    },

    produceDiff: function(baseEntries, currentEntries, callback)
    {
        for (var item in currentEntries)
            callback(baseEntries[item], currentEntries[item]);

        for (item in baseEntries) {
            if (!(item in currentEntries))
                callback(baseEntries[item], null);
        }
    },

    sort: function(comparator, force) {
        if (!force && this.lastComparator === comparator)
            return;

        this.children.sort(comparator);
        var childCount = this.children.length;
        for (var childIndex = 0; childIndex < childCount; ++childIndex)
            this.children[childIndex]._recalculateSiblings(childIndex);
        for (var i = 0; i < this.children.length; ++i) {
            var child = this.children[i];
            if (!force && (!child.expanded || child.lastComparator === comparator))
                continue;
            child.sort(comparator, force);
        }
        this.lastComparator = comparator;
    },

    signForDelta: function(delta) {
        if (delta === 0)
            return "";
        if (delta > 0)
            return "+";
        else
            return "\u2212";  // Math minus sign, same width as plus.
    },

    showDeltaAsPercent: function(value)
    {
        if (value === Number.POSITIVE_INFINITY)
            return WebInspector.UIString("new");
        else if (value === Number.NEGATIVE_INFINITY)
            return WebInspector.UIString("deleted");
        if (value > 1000.0)
            return WebInspector.UIString("%s >1000%%", this.signForDelta(value));
        return WebInspector.UIString("%s%.2f%%", this.signForDelta(value), Math.abs(value));
    },

    getTotalCount: function()
    {
        if (!this._count) {
            this._count = 0;
            for (var i = 0, n = this.children.length; i < n; ++i)
                this._count += this.children[i].count;
        }
        return this._count;
    },

    getTotalSize: function()
    {
        if (!this._size) {
            this._size = 0;
            for (var i = 0, n = this.children.length; i < n; ++i)
                this._size += this.children[i].size;
        }
        return this._size;
    },

    get countPercent()
    {
        return this.count / this._parent.getTotalCount() * 100.0;
    },

    get sizePercent()
    {
        return this.size / this._parent.getTotalSize() * 100.0;
    },

    get countDeltaPercent()
    {
        if (this.baseCount > 0) {
            if (this.count > 0)
                return this.countDelta / this.baseCount * 100.0;
            else
                return Number.NEGATIVE_INFINITY;
        } else
            return Number.POSITIVE_INFINITY;
    },

    get sizeDeltaPercent()
    {
        if (this.baseSize > 0) {
            if (this.size > 0)
                return this.sizeDelta / this.baseSize * 100.0;
            else
                return Number.NEGATIVE_INFINITY;
        } else
            return Number.POSITIVE_INFINITY;
    },

    get data()
    {
        var data = {};

        data["cons"] = this.constructorName;

        if (this.snapshotView.showCountAsPercent)
            data["count"] = WebInspector.UIString("%.2f%%", this.countPercent);
        else
            data["count"] = this.count;

        if (this.size !== null) {
            if (this.snapshotView.showSizeAsPercent)
                data["size"] = WebInspector.UIString("%.2f%%", this.sizePercent);
            else
                data["size"] = Number.bytesToString(this.size);
        } else
            data["size"] = "";

        if (this.snapshotView.showCountDeltaAsPercent)
            data["countDelta"] = this.showDeltaAsPercent(this.countDeltaPercent);
        else
            data["countDelta"] = WebInspector.UIString("%s%d", this.signForDelta(this.countDelta), Math.abs(this.countDelta));

        if (this.sizeDelta !== null) {
            if (this.snapshotView.showSizeDeltaAsPercent)
                data["sizeDelta"] = this.showDeltaAsPercent(this.sizeDeltaPercent);
            else
                data["sizeDelta"] = WebInspector.UIString("%s%s", this.signForDelta(this.sizeDelta), Number.bytesToString(Math.abs(this.sizeDelta)));
        } else
            data["sizeDelta"] = "";

        return data;
    },

    createCell: function(columnIdentifier)
    {
        var cell = WebInspector.DataGridNode.prototype.createCell.call(this, columnIdentifier);

        if ((columnIdentifier === "cons" && this._searchMatchedConsColumn) ||
            (columnIdentifier === "count" && this._searchMatchedCountColumn) ||
            (columnIdentifier === "size" && this._searchMatchedSizeColumn) ||
            (columnIdentifier === "countDelta" && this._searchMatchedCountDeltaColumn) ||
            (columnIdentifier === "sizeDelta" && this._searchMatchedSizeDeltaColumn))
            cell.addStyleClass("highlight");

        return cell;
    }
};

WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype.__proto__ = WebInspector.DataGridNode.prototype;

WebInspector.HeapSnapshotDataGridNode = function(snapshotView, baseEntry, snapshotEntry, owningTree)
{
    this.snapshotView = snapshotView;

    if (!snapshotEntry)
        snapshotEntry = { constructorName: baseEntry.constructorName, count: 0, size: 0, retainers: {} };
    this.constructorName = snapshotEntry.constructorName;
    this.count = snapshotEntry.count;
    this.size = snapshotEntry.size;
    this.retainers = snapshotEntry.retainers;

    if (!baseEntry)
        baseEntry = { count: 0, size: 0, retainers: {} };
    this.baseCount = baseEntry.count;
    this.countDelta = this.count - this.baseCount;
    this.baseSize = baseEntry.size;
    this.sizeDelta = this.size - this.baseSize;
    this.baseRetainers = baseEntry.retainers;

    WebInspector.HeapSnapshotDataGridNodeWithRetainers.call(this, owningTree);
};

WebInspector.HeapSnapshotDataGridNode.prototype.__proto__ = WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype;

WebInspector.HeapSnapshotDataGridList = function(snapshotView, baseEntries, snapshotEntries)
{
    this.tree = this;
    this.snapshotView = snapshotView;
    this.children = [];
    this.lastComparator = null;
    this.populateChildren(baseEntries, snapshotEntries);
};

WebInspector.HeapSnapshotDataGridList.prototype = {
    appendChild: function(child)
    {
        this.insertChild(child, this.children.length);
    },

    insertChild: function(child, index)
    {
        this.children.splice(index, 0, child);
    },

    removeChildren: function()
    {
        this.children = [];
    },

    populateChildren: function(baseEntries, snapshotEntries)
    {
        function appendListEntry(baseItem, snapshotItem)
        {
            this.appendChild(new WebInspector.HeapSnapshotDataGridNode(this.snapshotView, baseItem, snapshotItem, this));
        }
        this.produceDiff(baseEntries, snapshotEntries, appendListEntry.bind(this));
    },

    produceDiff: WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype.produceDiff,
    sort: WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype.sort,
    getTotalCount: WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype.getTotalCount,
    getTotalSize: WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype.getTotalSize
};

WebInspector.HeapSnapshotDataGridList.propertyComparators = [{}, {}];

WebInspector.HeapSnapshotDataGridList.propertyComparator = function(property, property2, isAscending)
{
    var propertyHash = property + "#" + property2;
    var comparator = this.propertyComparators[(isAscending ? 1 : 0)][propertyHash];
    if (!comparator) {
        comparator = function(lhs, rhs) {
            var l = lhs[property], r = rhs[property];
            var result = 0;
            if (l !== null && r !== null) {
                result = l < r ? -1 : (l > r ? 1 : 0);
            }
            if (result !== 0 || property2 === null) {
                return isAscending ? result : -result;
            } else {
                l = lhs[property2];
                r = rhs[property2];
                return l < r ? -1 : (l > r ? 1 : 0);
            }
        };
        this.propertyComparators[(isAscending ? 1 : 0)][propertyHash] = comparator;
    }
    return comparator;
};

WebInspector.HeapSnapshotDataGridRetainerNode = function(snapshotView, baseEntry, snapshotEntry, owningTree)
{
    this.snapshotView = snapshotView;

    if (!snapshotEntry)
        snapshotEntry = { constructorName: baseEntry.constructorName, count: 0, clusters: {} };
    this.constructorName = snapshotEntry.constructorName;
    this.count = snapshotEntry.count;
    this.retainers = this._calculateRetainers(this.snapshotView.profile, snapshotEntry.clusters);

    if (!baseEntry)
        baseEntry = { count: 0, clusters: {} };
    this.baseCount = baseEntry.count;
    this.countDelta = this.count - this.baseCount;
    this.baseRetainers = this._calculateRetainers(this.snapshotView.baseSnapshot, baseEntry.clusters);

    this.size = null;
    this.sizeDelta = null;

    WebInspector.HeapSnapshotDataGridNodeWithRetainers.call(this, owningTree);
}

WebInspector.HeapSnapshotDataGridRetainerNode.prototype = {
    get sizePercent()
    {
        return null;
    },

    get sizeDeltaPercent()
    {
        return null;
    },

    _calculateRetainers: function(snapshot, clusters)
    {
        var retainers = {};
        if (this.isEmptySet(clusters)) {
            if (this.constructorName in snapshot.entries)
                return snapshot.entries[this.constructorName].retainers;
        } else {
            // In case when an entry is retained by clusters, we need to gather up the list
            // of retainers by merging retainers of every cluster.
            // E.g. having such a tree:
            //   A
            //     Object:1  10
            //       X       3
            //       Y       4
            //     Object:2  5
            //       X       6
            //
            // will result in a following retainers list: X 9, Y 4.
            for (var clusterName in clusters) {
                if (clusterName in snapshot.clusters) {
                    var clusterRetainers = snapshot.clusters[clusterName].retainers;
                    for (var clusterRetainer in clusterRetainers) {
                        var clusterRetainerEntry = clusterRetainers[clusterRetainer];
                        if (!(clusterRetainer in retainers))
                            retainers[clusterRetainer] = { constructorName: clusterRetainerEntry.constructorName, count: 0, clusters: {} };
                        retainers[clusterRetainer].count += clusterRetainerEntry.count;
                        for (var clusterRetainerCluster in clusterRetainerEntry.clusters)
                            retainers[clusterRetainer].clusters[clusterRetainerCluster] = true;
                    }
                }
            }
        }
        return retainers;
    }
};

WebInspector.HeapSnapshotDataGridRetainerNode.prototype.__proto__ = WebInspector.HeapSnapshotDataGridNodeWithRetainers.prototype;


WebInspector.HeapSnapshotProfileType = function()
{
    WebInspector.ProfileType.call(this, WebInspector.HeapSnapshotProfileType.TypeId, WebInspector.UIString("HEAP SNAPSHOTS"));
}

WebInspector.HeapSnapshotProfileType.TypeId = "HEAP";

WebInspector.HeapSnapshotProfileType.prototype = {
    get buttonTooltip()
    {
        return WebInspector.UIString("Take heap snapshot.");
    },

    get buttonStyle()
    {
        return "heap-snapshot-status-bar-item status-bar-item";
    },

    buttonClicked: function()
    {
        InspectorBackend.takeHeapSnapshot(false);
    },

    get welcomeMessage()
    {
        return WebInspector.UIString("Get a heap snapshot by pressing the %s button on the status bar.");
    },

    createSidebarTreeElementForProfile: function(profile)
    {
        return new WebInspector.ProfileSidebarTreeElement(profile, WebInspector.UIString("Snapshot %d"), "heap-snapshot-sidebar-tree-item");
    },

    createView: function(profile)
    {
        return new WebInspector.HeapSnapshotView(WebInspector.panels.profiles, profile);
    }
}

WebInspector.HeapSnapshotProfileType.prototype.__proto__ = WebInspector.ProfileType.prototype;
