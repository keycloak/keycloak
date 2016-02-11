/*
 * Copyright (C) 2007, 2008, 2010 Apple Inc.  All rights reserved.
 * Copyright (C) 2009 Joseph Pecoraro
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

WebInspector.ResourcesPanel = function(database)
{
    WebInspector.Panel.call(this, "resources");

    WebInspector.settings.installApplicationSetting("resourcesLastSelectedItem", {});

    this.createSidebar();
    this.sidebarElement.addStyleClass("outline-disclosure filter-all children small");
    this.sidebarTreeElement.removeStyleClass("sidebar-tree");

    this.resourcesListTreeElement = new WebInspector.StorageCategoryTreeElement(this, WebInspector.UIString("Frames"), "Frames", "frame-storage-tree-item");
    this.sidebarTree.appendChild(this.resourcesListTreeElement);
    this._treeElementForFrameId = {};

    this.databasesListTreeElement = new WebInspector.StorageCategoryTreeElement(this, WebInspector.UIString("Databases"), "Databases", "database-storage-tree-item");
    this.sidebarTree.appendChild(this.databasesListTreeElement);

    this.localStorageListTreeElement = new WebInspector.StorageCategoryTreeElement(this, WebInspector.UIString("Local Storage"), "LocalStorage", "domstorage-storage-tree-item local-storage");
    this.sidebarTree.appendChild(this.localStorageListTreeElement);

    this.sessionStorageListTreeElement = new WebInspector.StorageCategoryTreeElement(this, WebInspector.UIString("Session Storage"), "SessionStorage", "domstorage-storage-tree-item session-storage");
    this.sidebarTree.appendChild(this.sessionStorageListTreeElement);

    this.cookieListTreeElement = new WebInspector.StorageCategoryTreeElement(this, WebInspector.UIString("Cookies"), "Cookies", "cookie-storage-tree-item");
    this.sidebarTree.appendChild(this.cookieListTreeElement);

    this.applicationCacheListTreeElement = new WebInspector.StorageCategoryTreeElement(this, WebInspector.UIString("Application Cache"), "ApplicationCache", "application-cache-storage-tree-item");
    this.sidebarTree.appendChild(this.applicationCacheListTreeElement);

    this.storageViews = document.createElement("div");
    this.storageViews.id = "storage-views";
    this.storageViews.className = "diff-container";
    this.element.appendChild(this.storageViews);

    this.storageViewStatusBarItemsContainer = document.createElement("div");
    this.storageViewStatusBarItemsContainer.className = "status-bar-items";

    this._databases = [];
    this._domStorage = [];
    this._cookieViews = {};
    this._origins = {};
    this._domains = {};

    this.sidebarElement.addEventListener("mousemove", this._onmousemove.bind(this), false);
    this.sidebarElement.addEventListener("mouseout", this._onmouseout.bind(this), false);

    WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.ResourceUpdated, this._refreshResource, this);
}

WebInspector.ResourcesPanel.prototype = {
    get toolbarItemLabel()
    {
        return WebInspector.UIString("Resources");
    },

    get statusBarItems()
    {
        return [this.storageViewStatusBarItemsContainer];
    },

    elementsToRestoreScrollPositionsFor: function()
    {
        return [this.sidebarElement];
    },

    show: function()
    {
        WebInspector.Panel.prototype.show.call(this);

        if (this.visibleView && this.visibleView.resource)
            this._showResourceView(this.visibleView.resource);

        this._initDefaultSelection();
    },

    loadEventFired: function()
    {
        this._initDefaultSelection();
    },

    _initDefaultSelection: function()
    {
        if (this._initializedDefaultSelection)
            return;

        this._initializedDefaultSelection = true;
        var itemURL = WebInspector.settings.resourcesLastSelectedItem;
        if (itemURL) {
            for (var treeElement = this.sidebarTree.children[0]; treeElement; treeElement = treeElement.traverseNextTreeElement(false, this.sidebarTree, true)) {
                if (treeElement.itemURL === itemURL) {
                    treeElement.select();
                    treeElement.reveal();
                    return;
                }
            }
        }

        if (WebInspector.mainResource && this.resourcesListTreeElement && this.resourcesListTreeElement.expanded)
            this.showResource(WebInspector.mainResource);
    },

    reset: function()
    {
        delete this._initializedDefaultSelection;
        this._origins = {};
        this._domains = {};
        for (var i = 0; i < this._databases.length; ++i) {
            var database = this._databases[i];
            delete database._tableViews;
            delete database._queryView;
        }
        this._databases = [];

        var domStorageLength = this._domStorage.length;
        for (var i = 0; i < this._domStorage.length; ++i) {
            var domStorage = this._domStorage[i];
            delete domStorage._domStorageView;
        }
        this._domStorage = [];

        this._cookieViews = {};
        
        this._applicationCacheView = null;
        delete this._cachedApplicationCacheViewStatus;

        this.databasesListTreeElement.removeChildren();
        this.localStorageListTreeElement.removeChildren();
        this.sessionStorageListTreeElement.removeChildren();
        this.cookieListTreeElement.removeChildren();
        this.applicationCacheListTreeElement.removeChildren();
        this.storageViews.removeChildren();

        this.storageViewStatusBarItemsContainer.removeChildren();

        if (this.sidebarTree.selectedTreeElement)
            this.sidebarTree.selectedTreeElement.deselect();
    },

    clear: function()
    {
        this.resourcesListTreeElement.removeChildren();
        this._treeElementForFrameId = {};
        this.reset();
    },

    addOrUpdateFrame: function(parentFrameId, frameId, title, subtitle)
    {
        var frameTreeElement = this._treeElementForFrameId[frameId];
        if (frameTreeElement) {
            frameTreeElement.setTitles(title, subtitle);
            return;
        }

        var parentTreeElement = parentFrameId ? this._treeElementForFrameId[parentFrameId] : this.resourcesListTreeElement;
        if (!parentTreeElement) {
            console.warning("No frame with id:" + parentFrameId + " to route " + displayName + " to.")
            return;
        }

        var frameTreeElement = new WebInspector.FrameTreeElement(this, frameId, title, subtitle);
        this._treeElementForFrameId[frameId] = frameTreeElement;

        // Insert in the alphabetical order, first frames, then resources.
        var children = parentTreeElement.children;
        for (var i = 0; i < children.length; ++i) {
            var child = children[i];
            if (!(child instanceof WebInspector.FrameTreeElement)) {
                parentTreeElement.insertChild(frameTreeElement, i);
                return;
            }
            if (child.displayName.localeCompare(frameTreeElement.displayName) > 0) {
                parentTreeElement.insertChild(frameTreeElement, i);
                return;
            }
        }
        parentTreeElement.appendChild(frameTreeElement);
    },

    removeFrame: function(frameId)
    {
        var frameTreeElement = this._treeElementForFrameId[frameId];
        if (!frameTreeElement)
            return;
        delete this._treeElementForFrameId[frameId];
        if (frameTreeElement.parent)
            frameTreeElement.parent.removeChild(frameTreeElement);
    },

    addResourceToFrame: function(frameId, resource)
    {
        this.addDocumentURL(resource.documentURL);

        if (resource.statusCode >= 301 && resource.statusCode <= 303)
            return;

        var frameTreeElement = this._treeElementForFrameId[frameId];
        if (!frameTreeElement) {
            // This is a frame's main resource, it will be retained
            // and re-added by the resource manager;
            return;
        }

        var resourceTreeElement = new WebInspector.FrameResourceTreeElement(this, resource);

        // Insert in the alphabetical order, first frames, then resources. Document resource goes first.
        var children = frameTreeElement.children;
        for (var i = 0; i < children.length; ++i) {
            var child = children[i];
            if (!(child instanceof WebInspector.FrameResourceTreeElement))
                continue;

            if (resource.type === WebInspector.Resource.Type.Document ||
                    (child._resource.type !== WebInspector.Resource.Type.Document && child._resource.displayName.localeCompare(resource.displayName) > 0)) {
                frameTreeElement.insertChild(resourceTreeElement, i);
                return;
            }
        }
        frameTreeElement.appendChild(resourceTreeElement);
    },

    removeResourcesFromFrame: function(frameId)
    {
        var frameTreeElement = this._treeElementForFrameId[frameId];
        if (frameTreeElement)
            frameTreeElement.removeChildren();
    },

    _refreshResource: function(event)
    {
        var resource = event.data;
        // FIXME: do not add XHR in the first place based on the native instrumentation.
        if (resource.type === WebInspector.Resource.Type.XHR) {
            var resourceTreeElement = this._findTreeElementForResource(resource);
            if (resourceTreeElement)
                resourceTreeElement.parent.removeChild(resourceTreeElement);
        }
    },

    addDatabase: function(database)
    {
        this._databases.push(database);

        var databaseTreeElement = new WebInspector.DatabaseTreeElement(this, database);
        database._databasesTreeElement = databaseTreeElement;
        this.databasesListTreeElement.appendChild(databaseTreeElement);
    },

    addDocumentURL: function(url)
    {
        var parsedURL = url.asParsedURL();
        if (!parsedURL)
            return;

        var domain = parsedURL.host;
        if (!this._domains[domain]) {
            this._domains[domain] = true;

            var cookieDomainTreeElement = new WebInspector.CookieTreeElement(this, domain);
            this.cookieListTreeElement.appendChild(cookieDomainTreeElement);

            var applicationCacheTreeElement = new WebInspector.ApplicationCacheTreeElement(this, domain);
            this.applicationCacheListTreeElement.appendChild(applicationCacheTreeElement);
        }
    },

    addDOMStorage: function(domStorage)
    {
        this._domStorage.push(domStorage);
        var domStorageTreeElement = new WebInspector.DOMStorageTreeElement(this, domStorage, (domStorage.isLocalStorage ? "local-storage" : "session-storage"));
        domStorage._domStorageTreeElement = domStorageTreeElement;
        if (domStorage.isLocalStorage)
            this.localStorageListTreeElement.appendChild(domStorageTreeElement);
        else
            this.sessionStorageListTreeElement.appendChild(domStorageTreeElement);
    },

    selectDatabase: function(databaseId)
    {
        var database;
        for (var i = 0, len = this._databases.length; i < len; ++i) {
            database = this._databases[i];
            if (database.id === databaseId) {
                this.showDatabase(database);
                database._databasesTreeElement.select();
                return;
            }
        }
    },

    selectDOMStorage: function(storageId)
    {
        var domStorage = this._domStorageForId(storageId);
        if (domStorage) {
            this.showDOMStorage(domStorage);
            domStorage._domStorageTreeElement.select();
        }
    },

    canShowSourceLine: function(url, line)
    {
        return !!WebInspector.resourceForURL(url);
    },

    showSourceLine: function(url, line)
    {
        var resource = WebInspector.resourceForURL(url);
        if (resource.type === WebInspector.Resource.Type.XHR) {
            // Show XHRs in the network panel only.
            if (WebInspector.panels.network && WebInspector.panels.network.canShowSourceLine(url, line)) {
                WebInspector.currentPanel = WebInspector.panels.network;
                WebInspector.panels.network.showSourceLine(url, line);
            }
            return;
        }
        this.showResource(WebInspector.resourceForURL(url), line);
    },

    showResource: function(resource, line)
    {
        var resourceTreeElement = this._findTreeElementForResource(resource);
        if (resourceTreeElement) {
            resourceTreeElement.reveal();
            resourceTreeElement.select();
        }

        if (line) {
            var view = WebInspector.ResourceView.resourceViewForResource(resource);
            if (view.revealLine)
                view.revealLine(line);
            if (view.highlightLine)
                view.highlightLine(line);
        }
        return true;
    },

    _showResourceView: function(resource)
    {
        var view = WebInspector.ResourceView.resourceViewForResource(resource);

        // Consider rendering diff markup here.
        if (resource.baseRevision && view instanceof WebInspector.SourceFrame) {
            function callback(baseContent)
            {
                if (baseContent)
                    this._applyDiffMarkup(view, baseContent, resource.content);
            }
            resource.baseRevision.requestContent(callback.bind(this));
        }
        this._innerShowView(view);
    },

    _applyDiffMarkup: function(view, baseContent, newContent) {
        var oldLines = baseContent.split("\n");
        var newLines = newContent.split("\n");

        var diff = Array.diff(oldLines, newLines);

        var diffData = {};
        diffData.added = [];
        diffData.removed = [];
        diffData.changed = [];

        var offset = 0;
        var right = diff.right;
        for (var i = 0; i < right.length; ++i) {
            if (typeof right[i] === "string") {
                if (right.length > i + 1 && right[i + 1].row === i + 1 - offset)
                    diffData.changed.push(i);
                else {
                    diffData.added.push(i);
                    offset++;
                }
            } else
                offset = i - right[i].row;
        }
        view.markDiff(diffData);
    },

    showDatabase: function(database, tableName)
    {
        if (!database)
            return;
            
        var view;
        if (tableName) {
            if (!("_tableViews" in database))
                database._tableViews = {};
            view = database._tableViews[tableName];
            if (!view) {
                view = new WebInspector.DatabaseTableView(database, tableName);
                database._tableViews[tableName] = view;
            }
        } else {
            view = database._queryView;
            if (!view) {
                view = new WebInspector.DatabaseQueryView(database);
                database._queryView = view;
            }
        }

        this._innerShowView(view);
    },

    showDOMStorage: function(domStorage)
    {
        if (!domStorage)
            return;

        var view;
        view = domStorage._domStorageView;
        if (!view) {
            view = new WebInspector.DOMStorageItemsView(domStorage);
            domStorage._domStorageView = view;
        }

        this._innerShowView(view);
    },

    showCookies: function(treeElement, cookieDomain)
    {
        var view = this._cookieViews[cookieDomain];
        if (!view) {
            view = new WebInspector.CookieItemsView(treeElement, cookieDomain);
            this._cookieViews[cookieDomain] = view;
        }

        this._innerShowView(view);
    },

    showApplicationCache: function(treeElement, appcacheDomain)
    {
        var view = this._applicationCacheView;
        if (!view) {
            view = new WebInspector.ApplicationCacheItemsView(treeElement, appcacheDomain);
            this._applicationCacheView = view;
        }

        this._innerShowView(view);

        if ("_cachedApplicationCacheViewStatus" in this)
            this._applicationCacheView.updateStatus(this._cachedApplicationCacheViewStatus);
    },

    showCategoryView: function(categoryName)
    {
        if (!this._categoryView)
            this._categoryView = new WebInspector.StorageCategoryView();
        this._categoryView.setText(categoryName);
        this._innerShowView(this._categoryView);
    },

    _innerShowView: function(view)
    {
        if (this.visibleView)
            this.visibleView.hide();

        view.show(this.storageViews);
        this.visibleView = view;

        this.storageViewStatusBarItemsContainer.removeChildren();
        var statusBarItems = view.statusBarItems || [];
        for (var i = 0; i < statusBarItems.length; ++i)
            this.storageViewStatusBarItemsContainer.appendChild(statusBarItems[i]);
    },

    closeVisibleView: function()
    {
        if (this.visibleView)
            this.visibleView.hide();
        delete this.visibleView;
    },

    updateDatabaseTables: function(database)
    {
        if (!database || !database._databasesTreeElement)
            return;

        database._databasesTreeElement.shouldRefreshChildren = true;

        if (!("_tableViews" in database))
            return;

        var tableNamesHash = {};
        var self = this;
        function tableNamesCallback(tableNames)
        {
            var tableNamesLength = tableNames.length;
            for (var i = 0; i < tableNamesLength; ++i)
                tableNamesHash[tableNames[i]] = true;

            for (var tableName in database._tableViews) {
                if (!(tableName in tableNamesHash)) {
                    if (self.visibleView === database._tableViews[tableName])
                        self.closeVisibleView();
                    delete database._tableViews[tableName];
                }
            }
        }
        database.getTableNames(tableNamesCallback);
    },

    dataGridForResult: function(columnNames, values)
    {
        var numColumns = columnNames.length;
        if (!numColumns)
            return null;

        var columns = {};

        for (var i = 0; i < columnNames.length; ++i) {
            var column = {};
            column.width = columnNames[i].length;
            column.title = columnNames[i];
            column.sortable = true;

            columns[columnNames[i]] = column;
        }

        var nodes = [];
        for (var i = 0; i < values.length / numColumns; ++i) {
            var data = {};
            for (var j = 0; j < columnNames.length; ++j)
                data[columnNames[j]] = values[numColumns * i + j];

            var node = new WebInspector.DataGridNode(data, false);
            node.selectable = false;
            nodes.push(node);
        }

        var dataGrid = new WebInspector.DataGrid(columns);
        var length = nodes.length;
        for (var i = 0; i < length; ++i)
            dataGrid.appendChild(nodes[i]);

        dataGrid.addEventListener("sorting changed", this._sortDataGrid.bind(this, dataGrid), this);
        return dataGrid;
    },

    _sortDataGrid: function(dataGrid)
    {
        var nodes = dataGrid.children.slice();
        var sortColumnIdentifier = dataGrid.sortColumnIdentifier;
        var sortDirection = dataGrid.sortOrder === "ascending" ? 1 : -1;
        var columnIsNumeric = true;

        for (var i = 0; i < nodes.length; i++) {
            if (isNaN(Number(nodes[i].data[sortColumnIdentifier])))
                columnIsNumeric = false;
        }

        function comparator(dataGridNode1, dataGridNode2)
        {
            var item1 = dataGridNode1.data[sortColumnIdentifier];
            var item2 = dataGridNode2.data[sortColumnIdentifier];

            var comparison;
            if (columnIsNumeric) {
                // Sort numbers based on comparing their values rather than a lexicographical comparison.
                var number1 = parseFloat(item1);
                var number2 = parseFloat(item2);
                comparison = number1 < number2 ? -1 : (number1 > number2 ? 1 : 0);
            } else
                comparison = item1 < item2 ? -1 : (item1 > item2 ? 1 : 0);

            return sortDirection * comparison;
        }

        nodes.sort(comparator);
        dataGrid.removeChildren();
        for (var i = 0; i < nodes.length; i++)
            dataGrid.appendChild(nodes[i]);
    },

    updateDOMStorage: function(storageId)
    {
        var domStorage = this._domStorageForId(storageId);
        if (!domStorage)
            return;

        var view = domStorage._domStorageView;
        if (this.visibleView && view === this.visibleView)
            domStorage._domStorageView.update();
    },

    updateApplicationCacheStatus: function(status)
    {
        this._cachedApplicationCacheViewStatus = status;
        if (this._applicationCacheView && this._applicationCacheView === this.visibleView)
            this._applicationCacheView.updateStatus(status);
    },

    updateNetworkState: function(isNowOnline)
    {
        if (this._applicationCacheView && this._applicationCacheView === this.visibleView)
            this._applicationCacheView.updateNetworkState(isNowOnline);
    },

    updateManifest: function(manifest)
    {
        if (this._applicationCacheView && this._applicationCacheView === this.visibleView)
            this._applicationCacheView.updateManifest(manifest);
    },

    _domStorageForId: function(storageId)
    {
        if (!this._domStorage)
            return null;
        var domStorageLength = this._domStorage.length;
        for (var i = 0; i < domStorageLength; ++i) {
            var domStorage = this._domStorage[i];
            if (domStorage.id == storageId)
                return domStorage;
        }
        return null;
    },

    updateMainViewWidth: function(width)
    {
        this.storageViews.style.left = width + "px";
        this.storageViewStatusBarItemsContainer.style.left = width + "px";
        this.resize();
    },

    get searchableViews()
    {
        var views = [];

        var visibleView = this.visibleView;
        if (visibleView.performSearch)
            views.push(visibleView);

        function callback(resourceTreeElement)
        {
            var resource = resourceTreeElement._resource;
            var resourceView = WebInspector.ResourceView.resourceViewForResource(resource);
            if (resourceView.performSearch && resourceView !== visibleView)
                views.push(resourceView);
        }
        this._forAllResourceTreeElements(callback);
        return views;
    },

    _forAllResourceTreeElements: function(callback)
    {
        var stop = false;
        for (var treeElement = this.resourcesListTreeElement; !stop && treeElement; treeElement = treeElement.traverseNextTreeElement(false, this.resourcesListTreeElement, true)) {
            if (treeElement instanceof WebInspector.FrameResourceTreeElement)
                stop = callback(treeElement);
        }
    },

    searchMatchFound: function(view, matches)
    {
        if (!view.resource)
            return;
        var treeElement = this._findTreeElementForResource(view.resource);
        if (treeElement)
            treeElement.searchMatchFound(matches);
    },

    _findTreeElementForResource: function(resource)
    {
        function isAncestor(ancestor, object)
        {
            // Redirects, XHRs do not belong to the tree, it is fine to silently return false here.
            return false;
        }

        function getParent(object)
        {
            // Redirects, XHRs do not belong to the tree, it is fine to silently return false here.
            return null;
        }

        return this.sidebarTree.findTreeElement(resource, isAncestor, getParent);
    },

    searchCanceled: function(startingNewSearch)
    {
        WebInspector.Panel.prototype.searchCanceled.call(this, startingNewSearch);

        if (startingNewSearch)
            return;

        function callback(resourceTreeElement)
        {
            resourceTreeElement._errorsWarningsUpdated();
        }
        this._forAllResourceTreeElements(callback);
    },

    performSearch: function(query)
    {
        function callback(resourceTreeElement)
        {
            resourceTreeElement._resetBubble();
        }
        this._forAllResourceTreeElements(callback);
        WebInspector.Panel.prototype.performSearch.call(this, query);
    },

    showView: function(view)
    {
        if (view)
            this.showResource(view.resource);
    },

    _onmousemove: function(event)
    {
        var nodeUnderMouse = document.elementFromPoint(event.pageX, event.pageY);
        if (!nodeUnderMouse)
            return;

        var listNode = nodeUnderMouse.enclosingNodeOrSelfWithNodeName("li");
        if (!listNode)
            return;

        var element = listNode.treeElement;
        if (this._previousHoveredElement === element)
            return;

        if (this._previousHoveredElement) {
            this._previousHoveredElement.hovered = false;
            delete this._previousHoveredElement;
        }

        if (element instanceof WebInspector.FrameTreeElement) {
            this._previousHoveredElement = element;
            element.hovered = true;
        }
    },

    _onmouseout: function(event)
    {
        if (this._previousHoveredElement) {
            this._previousHoveredElement.hovered = false;
            delete this._previousHoveredElement;
        }
    }
}

WebInspector.ResourcesPanel.prototype.__proto__ = WebInspector.Panel.prototype;

WebInspector.BaseStorageTreeElement = function(storagePanel, representedObject, title, iconClass, hasChildren)
{
    TreeElement.call(this, "", representedObject, hasChildren);
    this._storagePanel = storagePanel;
    this._titleText = title;
    this._iconClass = iconClass;
}

WebInspector.BaseStorageTreeElement.prototype = {
    onattach: function()
    {
        this.listItemElement.removeChildren();
        this.listItemElement.addStyleClass(this._iconClass);

        var selectionElement = document.createElement("div");
        selectionElement.className = "selection";
        this.listItemElement.appendChild(selectionElement);

        this.imageElement = document.createElement("img");
        this.imageElement.className = "icon";
        this.listItemElement.appendChild(this.imageElement);

        this.titleElement = document.createElement("div");
        this.titleElement.className = "base-storage-tree-element-title";
        this.titleElement.textContent = this._titleText;
        this.listItemElement.appendChild(this.titleElement);
    },

    onselect: function()
    {
        var itemURL = this.itemURL;
        if (itemURL)
            WebInspector.settings.resourcesLastSelectedItem = itemURL;
    },

    onreveal: function()
    {
        if (this.listItemElement)
            this.listItemElement.scrollIntoViewIfNeeded(false);
    },

    get titleText()
    {
        return this._titleText;
    },

    set titleText(titleText)
    {
        this._titleText = titleText;
        if (this.titleElement)
            this.titleElement.textContent = this._titleText;
    },

    isEventWithinDisclosureTriangle: function()
    {
        // Override it since we use margin-left in place of treeoutline's text-indent.
        // Hence we need to take padding into consideration. This all is needed for leading
        // icons in the tree.
        var paddingLeft = 14;
        var left = this.listItemElement.totalOffsetLeft + paddingLeft;
        return event.pageX >= left && event.pageX <= left + this.arrowToggleWidth && this.hasChildren;
    }
}

WebInspector.BaseStorageTreeElement.prototype.__proto__ = TreeElement.prototype;

WebInspector.StorageCategoryTreeElement = function(storagePanel, categoryName, settingsKey, iconClass)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, categoryName, iconClass, true);
    this._expandedSettingKey = "resources" + settingsKey + "Expanded";
    WebInspector.settings.installApplicationSetting(this._expandedSettingKey, settingsKey === "Frames");
    this._categoryName = categoryName;
}

WebInspector.StorageCategoryTreeElement.prototype = {
    get itemURL()
    {
        return "category://" + this._categoryName;
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showCategoryView(this._categoryName);
    },

    onattach: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onattach.call(this);
        if (WebInspector.settings[this._expandedSettingKey])
            this.expand();
    },

    onexpand: function()
    {
        WebInspector.settings[this._expandedSettingKey] = true;
    },

    oncollapse: function()
    {
        WebInspector.settings[this._expandedSettingKey] = false;
    }
}
WebInspector.StorageCategoryTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.FrameTreeElement = function(storagePanel, frameId, title, subtitle)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, "", "frame-storage-tree-item");
    this._frameId = frameId;
    this.setTitles(title, subtitle);
}

WebInspector.FrameTreeElement.prototype = {
    get itemURL()
    {
        return "frame://" + encodeURI(this._displayName);
    },

    onattach: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onattach.call(this);
        if (this._titleToSetOnAttach || this._subtitleToSetOnAttach) {
            this.setTitles(this._titleToSetOnAttach, this._subtitleToSetOnAttach);
            delete this._titleToSetOnAttach;
            delete this._subtitleToSetOnAttach;
        }
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showCategoryView(this._displayName);

        this.listItemElement.removeStyleClass("hovered");
        InspectorBackend.hideFrameHighlight();
    },

    get displayName()
    {
        return this._displayName;
    },

    setTitles: function(title, subtitle)
    {
        this._displayName = "";
        if (this.parent) {
            if (title) {
                this.titleElement.textContent = title;
                this._displayName = title;
            }
            if (subtitle) {
                var subtitleElement = document.createElement("span");
                subtitleElement.className = "base-storage-tree-element-subtitle";
                subtitleElement.textContent = "(" + subtitle + ")";
                this._displayName += " (" + subtitle + ")";
                this.titleElement.appendChild(subtitleElement);
            }
        } else {
            this._titleToSetOnAttach = title;
            this._subtitleToSetOnAttach = subtitle;
        }
    },

    set hovered(hovered)
    {
        if (hovered) {
            this.listItemElement.addStyleClass("hovered");
            InspectorBackend.highlightFrame(this._frameId);
        } else {
            this.listItemElement.removeStyleClass("hovered");
            InspectorBackend.hideFrameHighlight();
        }
    }
}
WebInspector.FrameTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.FrameResourceTreeElement = function(storagePanel, resource)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, resource, resource.displayName, "resource-sidebar-tree-item resources-category-" + resource.category.name);
    this._resource = resource;
    this._resource.addEventListener("errors-warnings-updated", this._errorsWarningsUpdated, this);
    this._resource.addEventListener("content-changed", this._contentChanged, this);
    this.tooltip = resource.url;
}

WebInspector.FrameResourceTreeElement.prototype = {
    get itemURL()
    {
        return this._resource.url;
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel._showResourceView(this._resource);
    },

    ondblclick: function(event)
    {
        InspectorBackend.openInInspectedWindow(this._resource.url);
    },

    onattach: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onattach.call(this);

        if (this._resource.category === WebInspector.resourceCategories.images) {
            var previewImage = document.createElement("img");
            previewImage.className = "image-resource-icon-preview";
            this._resource.populateImageSource(previewImage);

            var iconElement = document.createElement("div");
            iconElement.className = "icon";
            iconElement.appendChild(previewImage);
            this.listItemElement.replaceChild(iconElement, this.imageElement);
        }

        this._statusElement = document.createElement("div");
        this._statusElement.className = "status";
        this.listItemElement.insertBefore(this._statusElement, this.titleElement);

        this.listItemElement.draggable = true;
        this.listItemElement.addEventListener("dragstart", this._ondragstart.bind(this), false);
    },

    _ondragstart: function(event)
    {
        event.dataTransfer.setData("text/plain", this._resource.content);
        event.dataTransfer.effectAllowed = "copy";
        return true;
    },

    _setBubbleText: function(x)
    {
        if (!this._bubbleElement) {
            this._bubbleElement = document.createElement("div");
            this._bubbleElement.className = "bubble";
            this._statusElement.appendChild(this._bubbleElement);
        }

        this._bubbleElement.textContent = x;
    },

    _resetBubble: function()
    {
        if (this._bubbleElement) {
            this._bubbleElement.textContent = "";
            this._bubbleElement.removeStyleClass("search-matches");
            this._bubbleElement.removeStyleClass("warning");
            this._bubbleElement.removeStyleClass("error");
        }
    },

    searchMatchFound: function(matches)
    {
        this._resetBubble();

        this._setBubbleText(matches);
        this._bubbleElement.addStyleClass("search-matches");

        // Expand, do not scroll into view.
        var currentAncestor = this.parent;
        while (currentAncestor && !currentAncestor.root) {
            if (!currentAncestor.expanded)
                currentAncestor.expand();
            currentAncestor = currentAncestor.parent;
        }
    },

    _errorsWarningsUpdated: function()
    {
        // FIXME: move to the SourceFrame.
        if (!this._resource.warnings && !this._resource.errors) {
            var view = WebInspector.ResourceView.existingResourceViewForResource(this._resource);
            if (view && view.clearMessages)
                view.clearMessages();
        }

        if (this._storagePanel.currentQuery)
            return;

        this._resetBubble();

        if (this._resource.warnings || this._resource.errors)
            this._setBubbleText(this._resource.warnings + this._resource.errors);

        if (this._resource.warnings)
            this._bubbleElement.addStyleClass("warning");

        if (this._resource.errors)
            this._bubbleElement.addStyleClass("error");
    },

    _contentChanged: function(event)
    {
        this.insertChild(new WebInspector.ResourceRevisionTreeElement(this._storagePanel, event.data.revision), 0);
        var oldView = WebInspector.ResourceView.existingResourceViewForResource(this._resource);
        if (oldView) {
            var newView = WebInspector.ResourceView.recreateResourceView(this._resource);
            if (oldView === this._storagePanel.visibleView)
                this._storagePanel.visibleView = newView;
        }
    }
}

WebInspector.FrameResourceTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.DatabaseTreeElement = function(storagePanel, database)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, database.name, "database-storage-tree-item", true);
    this._database = database;
}

WebInspector.DatabaseTreeElement.prototype = {
    get itemURL()
    {
        return "database://" + encodeURI(this._database.name);
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showDatabase(this._database);
    },

    oncollapse: function()
    {
        // Request a refresh after every collapse so the next
        // expand will have an updated table list.
        this.shouldRefreshChildren = true;
    },

    onpopulate: function()
    {
        this.removeChildren();

        function tableNamesCallback(tableNames)
        {
            var tableNamesLength = tableNames.length;
            for (var i = 0; i < tableNamesLength; ++i)
                this.appendChild(new WebInspector.DatabaseTableTreeElement(this._storagePanel, this._database, tableNames[i]));
        }
        this._database.getTableNames(tableNamesCallback.bind(this));
    }

}
WebInspector.DatabaseTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.DatabaseTableTreeElement = function(storagePanel, database, tableName)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, tableName, "database-storage-tree-item");
    this._database = database;
    this._tableName = tableName;
}

WebInspector.DatabaseTableTreeElement.prototype = {
    get itemURL()
    {
        return "database://" + encodeURI(this._database.name) + "/" + encodeURI(this._tableName);
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showDatabase(this._database, this._tableName);
    }
}
WebInspector.DatabaseTableTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.DOMStorageTreeElement = function(storagePanel, domStorage, className)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, domStorage.domain ? domStorage.domain : WebInspector.UIString("Local Files"), "domstorage-storage-tree-item " + className);
    this._domStorage = domStorage;
}

WebInspector.DOMStorageTreeElement.prototype = {
    get itemURL()
    {
        return "storage://" + this._domStorage.domain + "/" + (this._domStorage.isLocalStorage ? "local" : "session");
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showDOMStorage(this._domStorage);
    }
}
WebInspector.DOMStorageTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.CookieTreeElement = function(storagePanel, cookieDomain)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, cookieDomain ? cookieDomain : WebInspector.UIString("Local Files"), "cookie-storage-tree-item");
    this._cookieDomain = cookieDomain;
}

WebInspector.CookieTreeElement.prototype = {
    get itemURL()
    {
        return "cookies://" + this._cookieDomain;
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showCookies(this, this._cookieDomain);
    }
}
WebInspector.CookieTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.ApplicationCacheTreeElement = function(storagePanel, appcacheDomain)
{
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, appcacheDomain ? appcacheDomain : WebInspector.UIString("Local Files"), "application-cache-storage-tree-item");
    this._appcacheDomain = appcacheDomain;
}

WebInspector.ApplicationCacheTreeElement.prototype = {
    get itemURL()
    {
        return "appcache://" + this._appcacheDomain;
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel.showApplicationCache(this, this._appcacheDomain);
    }
}
WebInspector.ApplicationCacheTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.ResourceRevisionTreeElement = function(storagePanel, revision)
{
    var title = revision.timestamp ? revision.timestamp.toLocaleTimeString() : WebInspector.UIString("(original)");
    WebInspector.BaseStorageTreeElement.call(this, storagePanel, null, title, "resource-sidebar-tree-item resources-category-" + revision.category.name);
    if (revision.timestamp)
        this.tooltip = revision.timestamp.toLocaleString();
    this._resource = revision;
}

WebInspector.ResourceRevisionTreeElement.prototype = {
    onattach: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onattach.call(this);
        this.listItemElement.draggable = true;
        this.listItemElement.addEventListener("dragstart", this._ondragstart.bind(this), false);
        this.listItemElement.addEventListener("contextmenu", this._handleContextMenuEvent.bind(this), true);
    },

    onselect: function()
    {
        WebInspector.BaseStorageTreeElement.prototype.onselect.call(this);
        this._storagePanel._showResourceView(this._resource);
    },

    _ondragstart: function(event)
    {
        event.dataTransfer.setData("text/plain", this._resource.content);
        event.dataTransfer.effectAllowed = "copy";
        return true;
    },

    _handleContextMenuEvent: function(event)
    {
        var contextMenu = new WebInspector.ContextMenu();
        contextMenu.appendItem(WebInspector.UIString("Revert to this revision"), this._resource.revertToThis.bind(this._resource));
        contextMenu.show(event);
    }
}

WebInspector.ResourceRevisionTreeElement.prototype.__proto__ = WebInspector.BaseStorageTreeElement.prototype;

WebInspector.StorageCategoryView = function()
{
    WebInspector.View.call(this);

    this.element.addStyleClass("storage-view");

    this._emptyMsgElement = document.createElement("div");
    this._emptyMsgElement.className = "storage-empty-view";
    this.element.appendChild(this._emptyMsgElement);
}

WebInspector.StorageCategoryView.prototype = {
    setText: function(text)
    {
        this._emptyMsgElement.textContent = text;
    }
}

WebInspector.StorageCategoryView.prototype.__proto__ = WebInspector.View.prototype;
