/*
 * Copyright (C) 2008 Apple Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// FIXME: Rename the file.

WebInspector.CPUProfileView = function(profile)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("profile-view");

    this.showSelfTimeAsPercent = true;
    this.showTotalTimeAsPercent = true;
    this.showAverageTimeAsPercent = true;

    var columns = { "self": { title: WebInspector.UIString("Self"), width: "72px", sort: "descending", sortable: true },
                    "total": { title: WebInspector.UIString("Total"), width: "72px", sortable: true },
                    "average": { title: WebInspector.UIString("Average"), width: "72px", sortable: true },
                    "calls": { title: WebInspector.UIString("Calls"), width: "54px", sortable: true },
                    "function": { title: WebInspector.UIString("Function"), disclosure: true, sortable: true } };

    if (Preferences.samplingCPUProfiler) {
        delete columns.average;
        delete columns.calls;
    }

    this.dataGrid = new WebInspector.DataGrid(columns);
    this.dataGrid.addEventListener("sorting changed", this._sortData, this);
    this.dataGrid.element.addEventListener("mousedown", this._mouseDownInDataGrid.bind(this), true);
    this.element.appendChild(this.dataGrid.element);

    this.viewSelectElement = document.createElement("select");
    this.viewSelectElement.className = "status-bar-item";
    this.viewSelectElement.addEventListener("change", this._changeView.bind(this), false);
    this.view = "Heavy";

    var heavyViewOption = document.createElement("option");
    heavyViewOption.label = WebInspector.UIString("Heavy (Bottom Up)");
    var treeViewOption = document.createElement("option");
    treeViewOption.label = WebInspector.UIString("Tree (Top Down)");
    this.viewSelectElement.appendChild(heavyViewOption);
    this.viewSelectElement.appendChild(treeViewOption);

    this.percentButton = new WebInspector.StatusBarButton("", "percent-time-status-bar-item");
    this.percentButton.addEventListener("click", this._percentClicked.bind(this), false);

    this.focusButton = new WebInspector.StatusBarButton(WebInspector.UIString("Focus selected function."), "focus-profile-node-status-bar-item");
    this.focusButton.disabled = true;
    this.focusButton.addEventListener("click", this._focusClicked.bind(this), false);

    this.excludeButton = new WebInspector.StatusBarButton(WebInspector.UIString("Exclude selected function."), "exclude-profile-node-status-bar-item");
    this.excludeButton.disabled = true;
    this.excludeButton.addEventListener("click", this._excludeClicked.bind(this), false);

    this.resetButton = new WebInspector.StatusBarButton(WebInspector.UIString("Restore all functions."), "reset-profile-status-bar-item");
    this.resetButton.visible = false;
    this.resetButton.addEventListener("click", this._resetClicked.bind(this), false);

    this.profile = profile;

    var self = this;
    function profileCallback(profile)
    {
        self.profile.head = profile.head;
        self._assignParentsInProfile();
      
        self.profileDataGridTree = self.bottomUpProfileDataGridTree;
        self.profileDataGridTree.sort(WebInspector.ProfileDataGridTree.propertyComparator("selfTime", false));
     
        self.refresh();
     
        self._updatePercentButton();
    }

    InspectorBackend.getProfile(this.profile.typeId, this.profile.uid, profileCallback);
}

WebInspector.CPUProfileView.prototype = {
    get statusBarItems()
    {
        return [this.viewSelectElement, this.percentButton.element, this.focusButton.element, this.excludeButton.element, this.resetButton.element];
    },

    get profile()
    {
        return this._profile;
    },

    set profile(profile)
    {
        this._profile = profile;
    },

    get bottomUpProfileDataGridTree()
    {
        if (!this._bottomUpProfileDataGridTree)
            this._bottomUpProfileDataGridTree = new WebInspector.BottomUpProfileDataGridTree(this, this.profile.head);
        return this._bottomUpProfileDataGridTree;
    },

    get topDownProfileDataGridTree()
    {
        if (!this._topDownProfileDataGridTree)
            this._topDownProfileDataGridTree = new WebInspector.TopDownProfileDataGridTree(this, this.profile.head);
        return this._topDownProfileDataGridTree;
    },

    get currentTree()
    {
        return this._currentTree;
    },

    set currentTree(tree)
    {
        this._currentTree = tree;
        this.refresh();
    },

    get topDownTree()
    {
        if (!this._topDownTree) {
            this._topDownTree = WebInspector.TopDownTreeFactory.create(this.profile.head);
            this._sortProfile(this._topDownTree);
        }

        return this._topDownTree;
    },

    get bottomUpTree()
    {
        if (!this._bottomUpTree) {
            this._bottomUpTree = WebInspector.BottomUpTreeFactory.create(this.profile.head);
            this._sortProfile(this._bottomUpTree);
        }

        return this._bottomUpTree;
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
        var selectedProfileNode = this.dataGrid.selectedNode ? this.dataGrid.selectedNode.profileNode : null;

        this.dataGrid.removeChildren();

        var children = this.profileDataGridTree.children;
        var count = children.length;

        for (var index = 0; index < count; ++index)
            this.dataGrid.appendChild(children[index]);

        if (selectedProfileNode)
            selectedProfileNode.selected = true;
    },

    refreshVisibleData: function()
    {
        var child = this.dataGrid.children[0];
        while (child) {
            child.refresh();
            child = child.traverseNextNode(false, null, true);
        }
    },

    refreshShowAsPercents: function()
    {
        this._updatePercentButton();
        this.refreshVisibleData();
    },

    searchCanceled: function()
    {
        if (this._searchResults) {
            for (var i = 0; i < this._searchResults.length; ++i) {
                var profileNode = this._searchResults[i].profileNode;

                delete profileNode._searchMatchedSelfColumn;
                delete profileNode._searchMatchedTotalColumn;
                delete profileNode._searchMatchedCallsColumn;
                delete profileNode._searchMatchedFunctionColumn;

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

        var greaterThan = (query.indexOf(">") === 0);
        var lessThan = (query.indexOf("<") === 0);
        var equalTo = (query.indexOf("=") === 0 || ((greaterThan || lessThan) && query.indexOf("=") === 1));
        var percentUnits = (query.lastIndexOf("%") === (query.length - 1));
        var millisecondsUnits = (query.length > 2 && query.lastIndexOf("ms") === (query.length - 2));
        var secondsUnits = (!millisecondsUnits && query.lastIndexOf("s") === (query.length - 1));

        var queryNumber = parseFloat(query);
        if (greaterThan || lessThan || equalTo) {
            if (equalTo && (greaterThan || lessThan))
                queryNumber = parseFloat(query.substring(2));
            else
                queryNumber = parseFloat(query.substring(1));
        }

        var queryNumberMilliseconds = (secondsUnits ? (queryNumber * 1000) : queryNumber);

        // Make equalTo implicitly true if it wasn't specified there is no other operator.
        if (!isNaN(queryNumber) && !(greaterThan || lessThan))
            equalTo = true;

        function matchesQuery(/*ProfileDataGridNode*/ profileDataGridNode)
        {
            delete profileDataGridNode._searchMatchedSelfColumn;
            delete profileDataGridNode._searchMatchedTotalColumn;
            delete profileDataGridNode._searchMatchedAverageColumn;
            delete profileDataGridNode._searchMatchedCallsColumn;
            delete profileDataGridNode._searchMatchedFunctionColumn;

            if (percentUnits) {
                if (lessThan) {
                    if (profileDataGridNode.selfPercent < queryNumber)
                        profileDataGridNode._searchMatchedSelfColumn = true;
                    if (profileDataGridNode.totalPercent < queryNumber)
                        profileDataGridNode._searchMatchedTotalColumn = true;
                    if (profileDataGridNode.averagePercent < queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedAverageColumn = true;
                } else if (greaterThan) {
                    if (profileDataGridNode.selfPercent > queryNumber)
                        profileDataGridNode._searchMatchedSelfColumn = true;
                    if (profileDataGridNode.totalPercent > queryNumber)
                        profileDataGridNode._searchMatchedTotalColumn = true;
                    if (profileDataGridNode.averagePercent < queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedAverageColumn = true;
                }

                if (equalTo) {
                    if (profileDataGridNode.selfPercent == queryNumber)
                        profileDataGridNode._searchMatchedSelfColumn = true;
                    if (profileDataGridNode.totalPercent == queryNumber)
                        profileDataGridNode._searchMatchedTotalColumn = true;
                    if (profileDataGridNode.averagePercent < queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedAverageColumn = true;
                }
            } else if (millisecondsUnits || secondsUnits) {
                if (lessThan) {
                    if (profileDataGridNode.selfTime < queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedSelfColumn = true;
                    if (profileDataGridNode.totalTime < queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedTotalColumn = true;
                    if (profileDataGridNode.averageTime < queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedAverageColumn = true;
                } else if (greaterThan) {
                    if (profileDataGridNode.selfTime > queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedSelfColumn = true;
                    if (profileDataGridNode.totalTime > queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedTotalColumn = true;
                    if (profileDataGridNode.averageTime > queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedAverageColumn = true;
                }

                if (equalTo) {
                    if (profileDataGridNode.selfTime == queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedSelfColumn = true;
                    if (profileDataGridNode.totalTime == queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedTotalColumn = true;
                    if (profileDataGridNode.averageTime == queryNumberMilliseconds)
                        profileDataGridNode._searchMatchedAverageColumn = true;
                }
            } else {
                if (equalTo && profileDataGridNode.numberOfCalls == queryNumber)
                    profileDataGridNode._searchMatchedCallsColumn = true;
                if (greaterThan && profileDataGridNode.numberOfCalls > queryNumber)
                    profileDataGridNode._searchMatchedCallsColumn = true;
                if (lessThan && profileDataGridNode.numberOfCalls < queryNumber)
                    profileDataGridNode._searchMatchedCallsColumn = true;
            }

            if (profileDataGridNode.functionName.hasSubstring(query, true) || profileDataGridNode.url.hasSubstring(query, true))
                profileDataGridNode._searchMatchedFunctionColumn = true;

            if (profileDataGridNode._searchMatchedSelfColumn ||
                profileDataGridNode._searchMatchedTotalColumn ||
                profileDataGridNode._searchMatchedAverageColumn ||
                profileDataGridNode._searchMatchedCallsColumn ||
                profileDataGridNode._searchMatchedFunctionColumn)
            {
                profileDataGridNode.refresh();
                return true;
            }

            return false;
        }

        var current = this.profileDataGridTree.children[0];

        while (current) {
            if (matchesQuery(current)) {
                this._searchResults.push({ profileNode: current });
            }

            current = current.traverseNextNode(false, null, false);
        }

        finishedCallback(this, this._searchResults.length);
    },

    jumpToFirstSearchResult: function()
    {
        if (!this._searchResults || !this._searchResults.length)
            return;
        this._currentSearchResultIndex = 0;
        this._jumpToSearchResult(this._currentSearchResultIndex);
    },

    jumpToLastSearchResult: function()
    {
        if (!this._searchResults || !this._searchResults.length)
            return;
        this._currentSearchResultIndex = (this._searchResults.length - 1);
        this._jumpToSearchResult(this._currentSearchResultIndex);
    },

    jumpToNextSearchResult: function()
    {
        if (!this._searchResults || !this._searchResults.length)
            return;
        if (++this._currentSearchResultIndex >= this._searchResults.length)
            this._currentSearchResultIndex = 0;
        this._jumpToSearchResult(this._currentSearchResultIndex);
    },

    jumpToPreviousSearchResult: function()
    {
        if (!this._searchResults || !this._searchResults.length)
            return;
        if (--this._currentSearchResultIndex < 0)
            this._currentSearchResultIndex = (this._searchResults.length - 1);
        this._jumpToSearchResult(this._currentSearchResultIndex);
    },

    showingFirstSearchResult: function()
    {
        return (this._currentSearchResultIndex === 0);
    },

    showingLastSearchResult: function()
    {
        return (this._searchResults && this._currentSearchResultIndex === (this._searchResults.length - 1));
    },

    _jumpToSearchResult: function(index)
    {
        var searchResult = this._searchResults[index];
        if (!searchResult)
            return;

        var profileNode = searchResult.profileNode;
        profileNode.reveal();
        profileNode.select();
    },

    _changeView: function(event)
    {
        if (!event || !this.profile)
            return;

        if (event.target.selectedIndex == 1 && this.view == "Heavy") {
            this.profileDataGridTree = this.topDownProfileDataGridTree;
            this._sortProfile();
            this.view = "Tree";
        } else if (event.target.selectedIndex == 0 && this.view == "Tree") {
            this.profileDataGridTree = this.bottomUpProfileDataGridTree;
            this._sortProfile();
            this.view = "Heavy";
        }

        if (!this.currentQuery || !this._searchFinishedCallback || !this._searchResults)
            return;

        // The current search needs to be performed again. First negate out previous match
        // count by calling the search finished callback with a negative number of matches.
        // Then perform the search again the with same query and callback.
        this._searchFinishedCallback(this, -this._searchResults.length);
        this.performSearch(this.currentQuery, this._searchFinishedCallback);
    },

    _percentClicked: function(event)
    {
        var currentState = this.showSelfTimeAsPercent && this.showTotalTimeAsPercent && this.showAverageTimeAsPercent;
        this.showSelfTimeAsPercent = !currentState;
        this.showTotalTimeAsPercent = !currentState;
        this.showAverageTimeAsPercent = !currentState;
        this.refreshShowAsPercents();
    },

    _updatePercentButton: function()
    {
        if (this.showSelfTimeAsPercent && this.showTotalTimeAsPercent && this.showAverageTimeAsPercent) {
            this.percentButton.title = WebInspector.UIString("Show absolute total and self times.");
            this.percentButton.toggled = true;
        } else {
            this.percentButton.title = WebInspector.UIString("Show total and self times as percentages.");
            this.percentButton.toggled = false;
        }
    },

    _focusClicked: function(event)
    {
        if (!this.dataGrid.selectedNode)
            return;

        this.resetButton.visible = true;
        this.profileDataGridTree.focus(this.dataGrid.selectedNode);
        this.refresh();
        this.refreshVisibleData();
    },

    _excludeClicked: function(event)
    {
        var selectedNode = this.dataGrid.selectedNode

        if (!selectedNode)
            return;

        selectedNode.deselect();

        this.resetButton.visible = true;
        this.profileDataGridTree.exclude(selectedNode);
        this.refresh();
        this.refreshVisibleData();
    },

    _resetClicked: function(event)
    {
        this.resetButton.visible = false;
        this.profileDataGridTree.restore();
        this.refresh();
        this.refreshVisibleData();
    },

    _dataGridNodeSelected: function(node)
    {
        this.focusButton.disabled = false;
        this.excludeButton.disabled = false;
    },

    _dataGridNodeDeselected: function(node)
    {
        this.focusButton.disabled = true;
        this.excludeButton.disabled = true;
    },

    _sortData: function(event)
    {
        this._sortProfile(this.profile);
    },

    _sortProfile: function()
    {
        var sortAscending = this.dataGrid.sortOrder === "ascending";
        var sortColumnIdentifier = this.dataGrid.sortColumnIdentifier;
        var sortProperty = {
                "average": "averageTime",
                "self": "selfTime",
                "total": "totalTime",
                "calls": "numberOfCalls",
                "function": "functionName"
            }[sortColumnIdentifier];

        this.profileDataGridTree.sort(WebInspector.ProfileDataGridTree.propertyComparator(sortProperty, sortAscending));

        this.refresh();
    },

    _mouseDownInDataGrid: function(event)
    {
        if (event.detail < 2)
            return;

        var cell = event.target.enclosingNodeOrSelfWithNodeName("td");
        if (!cell || (!cell.hasStyleClass("total-column") && !cell.hasStyleClass("self-column") && !cell.hasStyleClass("average-column")))
            return;

        if (cell.hasStyleClass("total-column"))
            this.showTotalTimeAsPercent = !this.showTotalTimeAsPercent;
        else if (cell.hasStyleClass("self-column"))
            this.showSelfTimeAsPercent = !this.showSelfTimeAsPercent;
        else if (cell.hasStyleClass("average-column"))
            this.showAverageTimeAsPercent = !this.showAverageTimeAsPercent;

        this.refreshShowAsPercents();

        event.preventDefault();
        event.stopPropagation();
    },

    _assignParentsInProfile: function()
    {
        var head = this.profile.head;
        head.parent = null;
        head.head = null;
        var nodesToTraverse = [ { parent: head, children: head.children } ];
        while (nodesToTraverse.length > 0) {
            var pair = nodesToTraverse.shift();
            var parent = pair.parent;
            var children = pair.children;
            var length = children.length;
            for (var i = 0; i < length; ++i) {
                children[i].head = head;
                children[i].parent = parent;
                if (children[i].children.length > 0)
                    nodesToTraverse.push({ parent: children[i], children: children[i].children });
            }
        }
    }
}

WebInspector.CPUProfileView.prototype.__proto__ = WebInspector.View.prototype;

WebInspector.CPUProfileType = function()
{
    WebInspector.ProfileType.call(this, WebInspector.CPUProfileType.TypeId, WebInspector.UIString("CPU PROFILES"));
    this._recording = false;
}

WebInspector.CPUProfileType.TypeId = "CPU";

WebInspector.CPUProfileType.prototype = {
    get buttonTooltip()
    {
        return this._recording ? WebInspector.UIString("Stop profiling.") : WebInspector.UIString("Start profiling.");
    },

    get buttonStyle()
    {
        return this._recording ? "record-profile-status-bar-item status-bar-item toggled-on" : "record-profile-status-bar-item status-bar-item";
    },

    buttonClicked: function()
    {
        this._recording = !this._recording;

        if (this._recording)
            InspectorBackend.startProfiling();
        else
            InspectorBackend.stopProfiling();
    },

    get welcomeMessage()
    {
        return WebInspector.UIString("Control CPU profiling by pressing the %s button on the status bar.");
    },

    setRecordingProfile: function(isProfiling)
    {
        this._recording = isProfiling;
    },

    createSidebarTreeElementForProfile: function(profile)
    {
        return new WebInspector.ProfileSidebarTreeElement(profile, WebInspector.UIString("Profile %d"), "profile-sidebar-tree-item");
    },

    createView: function(profile)
    {
        return new WebInspector.CPUProfileView(profile);
    }
}

WebInspector.CPUProfileType.prototype.__proto__ = WebInspector.ProfileType.prototype;
