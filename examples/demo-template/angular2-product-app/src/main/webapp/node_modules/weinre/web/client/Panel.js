/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
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

WebInspector.Panel = function(name)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("panel");
    this.element.addStyleClass(name);
    this._panelName = name;

    WebInspector.settings.installApplicationSetting(this._sidebarWidthSettingName(), undefined);
}

// Should by in sync with style declarations.
WebInspector.Panel.counterRightMargin = 25;

WebInspector.Panel.prototype = {
    get toolbarItem()
    {
        if (this._toolbarItem)
            return this._toolbarItem;

        // Sample toolbar item as markup:
        // <button class="toolbar-item resources toggleable">
        // <div class="toolbar-icon"></div>
        // <div class="toolbar-label">Resources</div>
        // </button>

        this._toolbarItem = document.createElement("button");
        this._toolbarItem.className = "toolbar-item toggleable";
        this._toolbarItem.panel = this;

        this._toolbarItem.addStyleClass(this._panelName);

        var iconElement = document.createElement("div");
        iconElement.className = "toolbar-icon";
        this._toolbarItem.appendChild(iconElement);

        if ("toolbarItemLabel" in this) {
            var labelElement = document.createElement("div");
            labelElement.className = "toolbar-label";
            labelElement.textContent = this.toolbarItemLabel;
            this._toolbarItem.appendChild(labelElement);
        }

        return this._toolbarItem;
    },

    get name()
    {
        return this._panelName;
    },

    show: function()
    {
        WebInspector.View.prototype.show.call(this);

        var statusBarItems = this.statusBarItems;
        if (statusBarItems) {
            this._statusBarItemContainer = document.createElement("div");
            for (var i = 0; i < statusBarItems.length; ++i)
                this._statusBarItemContainer.appendChild(statusBarItems[i]);
            document.getElementById("main-status-bar").appendChild(this._statusBarItemContainer);
        }

        if ("_toolbarItem" in this)
            this._toolbarItem.addStyleClass("toggled-on");

        WebInspector.currentFocusElement = this.defaultFocusedElement;

        this.restoreSidebarWidth();
        this._restoreScrollPositions();
    },

    hide: function()
    {
        this._storeScrollPositions();
        WebInspector.View.prototype.hide.call(this);

        if (this._statusBarItemContainer && this._statusBarItemContainer.parentNode)
            this._statusBarItemContainer.parentNode.removeChild(this._statusBarItemContainer);
        delete this._statusBarItemContainer;
        if ("_toolbarItem" in this)
            this._toolbarItem.removeStyleClass("toggled-on");
    },

    get defaultFocusedElement()
    {
        return this.sidebarTreeElement || this.element;
    },

    attach: function()
    {
        if (!this.element.parentNode)
            document.getElementById("main-panels").appendChild(this.element);
    },

    searchCanceled: function()
    {
        if (this._searchResults) {
            for (var i = 0; i < this._searchResults.length; ++i) {
                var view = this._searchResults[i];
                if (view.searchCanceled)
                    view.searchCanceled();
                delete view.currentQuery;
            }
        }

        WebInspector.updateSearchMatchesCount(0, this);

        if (this._currentSearchChunkIntervalIdentifier) {
            clearInterval(this._currentSearchChunkIntervalIdentifier);
            delete this._currentSearchChunkIntervalIdentifier;
        }

        this._totalSearchMatches = 0;
        this._currentSearchResultIndex = 0;
        this._searchResults = [];
    },

    performSearch: function(query)
    {
        // Call searchCanceled since it will reset everything we need before doing a new search.
        this.searchCanceled(true);

        var searchableViews = this.searchableViews;
        if (!searchableViews || !searchableViews.length)
            return;

        var parentElement = this.viewsContainerElement;
        var visibleView = this.visibleView;
        var sortFuction = this.searchResultsSortFunction;

        var matchesCountUpdateTimeout = null;

        function updateMatchesCount()
        {
            WebInspector.updateSearchMatchesCount(this._totalSearchMatches, this);
            matchesCountUpdateTimeout = null;
        }

        function updateMatchesCountSoon()
        {
            if (matchesCountUpdateTimeout)
                return;
            // Update the matches count every half-second so it doesn't feel twitchy.
            matchesCountUpdateTimeout = setTimeout(updateMatchesCount.bind(this), 500);
        }

        function finishedCallback(view, searchMatches)
        {
            if (!searchMatches)
                return;

            this._totalSearchMatches += searchMatches;
            this._searchResults.push(view);

            if (sortFuction)
                this._searchResults.sort(sortFuction);

            if (this.searchMatchFound)
                this.searchMatchFound(view, searchMatches);

            updateMatchesCountSoon.call(this);

            if (view === visibleView)
                view.jumpToFirstSearchResult();
        }

        var i = 0;
        var panel = this;
        var boundFinishedCallback = finishedCallback.bind(this);
        var chunkIntervalIdentifier = null;

        // Split up the work into chunks so we don't block the
        // UI thread while processing.

        function processChunk()
        {
            var view = searchableViews[i];

            if (++i >= searchableViews.length) {
                if (panel._currentSearchChunkIntervalIdentifier === chunkIntervalIdentifier)
                    delete panel._currentSearchChunkIntervalIdentifier;
                clearInterval(chunkIntervalIdentifier);
            }

            if (!view)
                return;

            if (view.element.parentNode !== parentElement && view.element.parentNode && parentElement)
                view.detach();

            view.currentQuery = query;
            view.performSearch(query, boundFinishedCallback);
        }

        processChunk();

        chunkIntervalIdentifier = setInterval(processChunk, 25);
        this._currentSearchChunkIntervalIdentifier = chunkIntervalIdentifier;
    },

    jumpToNextSearchResult: function()
    {
        if (!this.showView || !this._searchResults || !this._searchResults.length)
            return;

        var showFirstResult = false;

        this._currentSearchResultIndex = this._searchResults.indexOf(this.visibleView);
        if (this._currentSearchResultIndex === -1) {
            this._currentSearchResultIndex = 0;
            showFirstResult = true;
        }

        var currentView = this._searchResults[this._currentSearchResultIndex];

        if (currentView.showingLastSearchResult()) {
            if (++this._currentSearchResultIndex >= this._searchResults.length)
                this._currentSearchResultIndex = 0;
            currentView = this._searchResults[this._currentSearchResultIndex];
            showFirstResult = true;
        }

        if (currentView !== this.visibleView) {
            this.showView(currentView);
            WebInspector.focusSearchField();
        }

        if (showFirstResult)
            currentView.jumpToFirstSearchResult();
        else
            currentView.jumpToNextSearchResult();
    },

    jumpToPreviousSearchResult: function()
    {
        if (!this.showView || !this._searchResults || !this._searchResults.length)
            return;

        var showLastResult = false;

        this._currentSearchResultIndex = this._searchResults.indexOf(this.visibleView);
        if (this._currentSearchResultIndex === -1) {
            this._currentSearchResultIndex = 0;
            showLastResult = true;
        }

        var currentView = this._searchResults[this._currentSearchResultIndex];

        if (currentView.showingFirstSearchResult()) {
            if (--this._currentSearchResultIndex < 0)
                this._currentSearchResultIndex = (this._searchResults.length - 1);
            currentView = this._searchResults[this._currentSearchResultIndex];
            showLastResult = true;
        }

        if (currentView !== this.visibleView) {
            this.showView(currentView);
            WebInspector.focusSearchField();
        }

        if (showLastResult)
            currentView.jumpToLastSearchResult();
        else
            currentView.jumpToPreviousSearchResult();
    },

    createSidebar: function(parentElement, resizerParentElement)
    {
        if (this.sidebarElement)
            return;

        if (!parentElement)
            parentElement = this.element;

        if (!resizerParentElement)
            resizerParentElement = parentElement;

        this.sidebarElement = document.createElement("div");
        this.sidebarElement.className = "sidebar";
        parentElement.appendChild(this.sidebarElement);

        this.sidebarResizeElement = document.createElement("div");
        this.sidebarResizeElement.className = "sidebar-resizer-vertical";
        this.sidebarResizeElement.addEventListener("mousedown", this._startSidebarDragging.bind(this), false);
        resizerParentElement.appendChild(this.sidebarResizeElement);

        this.sidebarTreeElement = document.createElement("ol");
        this.sidebarTreeElement.className = "sidebar-tree";
        this.sidebarElement.appendChild(this.sidebarTreeElement);

        this.sidebarTree = new TreeOutline(this.sidebarTreeElement);
        this.sidebarTree.panel = this;
    },

    _sidebarWidthSettingName: function()
    {
        return this._panelName + "SidebarWidth";
    },

    _startSidebarDragging: function(event)
    {
        WebInspector.elementDragStart(this.sidebarResizeElement, this._sidebarDragging.bind(this), this._endSidebarDragging.bind(this), event, "col-resize");
    },

    _sidebarDragging: function(event)
    {
        this.updateSidebarWidth(event.pageX);

        event.preventDefault();
    },

    _endSidebarDragging: function(event)
    {
        WebInspector.elementDragEnd(event);
        this.saveSidebarWidth();
    },

    updateSidebarWidth: function(width)
    {
        if (!this.sidebarElement)
            return;

        if (this.sidebarElement.offsetWidth <= 0) {
            // The stylesheet hasn't loaded yet or the window is closed,
            // so we can't calculate what is need. Return early.
            return;
        }

        if (!("_currentSidebarWidth" in this))
            this._currentSidebarWidth = this.sidebarElement.offsetWidth;

        if (typeof width === "undefined")
            width = this._currentSidebarWidth;

        width = Number.constrain(width, Preferences.minSidebarWidth, window.innerWidth / 2);

        this._currentSidebarWidth = width;
        this.setSidebarWidth(width);

        this.updateMainViewWidth(width);
    },

    setSidebarWidth: function(width)
    {
        this.sidebarElement.style.width = width + "px";
        this.sidebarResizeElement.style.left = (width - 3) + "px";
    },

    restoreSidebarWidth: function()
    {
        var sidebarWidth = WebInspector.settings[this._sidebarWidthSettingName()];
        this.updateSidebarWidth(sidebarWidth);
    },

    saveSidebarWidth: function()
    {
        if (!this.sidebarElement)
            return;
        WebInspector.settings[this._sidebarWidthSettingName()] = this.sidebarElement.offsetWidth;
    },

    updateMainViewWidth: function(width)
    {
        // Should be implemented by ancestors.
    },

    resize: function()
    {
        var visibleView = this.visibleView;
        if (visibleView && "resize" in visibleView)
            visibleView.resize();
    },

    canShowSourceLine: function(url, line)
    {
        return false;
    },

    showSourceLine: function(url, line)
    {
        return false;
    },

    elementsToRestoreScrollPositionsFor: function()
    {
        return [];
    },

    _storeScrollPositions: function()
    {
        var elements = this.elementsToRestoreScrollPositionsFor();
        for (var i = 0; i < elements.length; ++i) {
            var container = elements[i];
            container._scrollTop = container.scrollTop;
        }
    },

    _restoreScrollPositions: function()
    {
        var elements = this.elementsToRestoreScrollPositionsFor();
        for (var i = 0; i < elements.length; ++i) {
            var container = elements[i];
            if (container._scrollTop)
                container.scrollTop = container._scrollTop;
        }
    }
}

WebInspector.Panel.prototype.__proto__ = WebInspector.View.prototype;
