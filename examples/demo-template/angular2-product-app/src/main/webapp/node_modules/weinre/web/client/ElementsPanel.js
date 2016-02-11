/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2008 Matt Lilek <webkit@mattlilek.com>
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

WebInspector.ElementsPanel = function()
{
    WebInspector.Panel.call(this, "elements");

    this.contentElement = document.createElement("div");
    this.contentElement.id = "elements-content";
    this.contentElement.className = "outline-disclosure source-code";

    this.treeOutline = new WebInspector.ElementsTreeOutline();
    this.treeOutline.panel = this;
    this.treeOutline.includeRootDOMNode = false;
    this.treeOutline.selectEnabled = true;

    this.treeOutline.focusedNodeChanged = function(forceUpdate)
    {
        if (this.panel.visible && WebInspector.currentFocusElement !== document.getElementById("search"))
            WebInspector.currentFocusElement = this.element;

        this.panel.updateBreadcrumb(forceUpdate);

        for (var pane in this.panel.sidebarPanes)
           this.panel.sidebarPanes[pane].needsUpdate = true;

        this.panel.updateStyles(true);
        this.panel.updateMetrics();
        this.panel.updateProperties();
        this.panel.updateEventListeners();

        if (this._focusedDOMNode) {
            InspectorBackend.addInspectedNode(this._focusedDOMNode.id);
            WebInspector.extensionServer.notifyObjectSelected(this.panel.name);
        }
    };

    this.contentElement.appendChild(this.treeOutline.element);

    this.crumbsElement = document.createElement("div");
    this.crumbsElement.className = "crumbs";
    this.crumbsElement.addEventListener("mousemove", this._mouseMovedInCrumbs.bind(this), false);
    this.crumbsElement.addEventListener("mouseout", this._mouseMovedOutOfCrumbs.bind(this), false);

    this.sidebarPanes = {};
    this.sidebarPanes.computedStyle = new WebInspector.ComputedStyleSidebarPane();
    this.sidebarPanes.styles = new WebInspector.StylesSidebarPane(this.sidebarPanes.computedStyle);
    this.sidebarPanes.metrics = new WebInspector.MetricsSidebarPane();
    this.sidebarPanes.properties = new WebInspector.PropertiesSidebarPane();
    if (Preferences.nativeInstrumentationEnabled)
        this.sidebarPanes.domBreakpoints = WebInspector.createDOMBreakpointsSidebarPane();
    this.sidebarPanes.eventListeners = new WebInspector.EventListenersSidebarPane();

    this.sidebarPanes.styles.onexpand = this.updateStyles.bind(this);
    this.sidebarPanes.metrics.onexpand = this.updateMetrics.bind(this);
    this.sidebarPanes.properties.onexpand = this.updateProperties.bind(this);
    this.sidebarPanes.eventListeners.onexpand = this.updateEventListeners.bind(this);

    this.sidebarPanes.styles.expanded = true;

    this.sidebarPanes.styles.addEventListener("style edited", this._stylesPaneEdited, this);
    this.sidebarPanes.styles.addEventListener("style property toggled", this._stylesPaneEdited, this);
    this.sidebarPanes.metrics.addEventListener("metrics edited", this._metricsPaneEdited, this);
    WebInspector.cssModel.addEventListener("stylesheet changed", this._styleSheetChanged, this);

    this.sidebarElement = document.createElement("div");
    this.sidebarElement.id = "elements-sidebar";

    for (var pane in this.sidebarPanes)
        this.sidebarElement.appendChild(this.sidebarPanes[pane].element);

    this.sidebarResizeElement = document.createElement("div");
    this.sidebarResizeElement.className = "sidebar-resizer-vertical";
    this.sidebarResizeElement.addEventListener("mousedown", this.rightSidebarResizerDragStart.bind(this), false);

    this._nodeSearchButton = new WebInspector.StatusBarButton(WebInspector.UIString("Select an element in the page to inspect it."), "node-search-status-bar-item");
    this._nodeSearchButton.addEventListener("click", this.toggleSearchingForNode.bind(this), false);

    this.element.appendChild(this.contentElement);
    this.element.appendChild(this.sidebarElement);
    this.element.appendChild(this.sidebarResizeElement);

    this._registerShortcuts();

    this.reset();
}

WebInspector.ElementsPanel.prototype = {
    get toolbarItemLabel()
    {
        return WebInspector.UIString("Elements");
    },

    get statusBarItems()
    {
        return [this._nodeSearchButton.element, this.crumbsElement];
    },

    get defaultFocusedElement()
    {
        return this.treeOutline.element;
    },

    updateStatusBarItems: function()
    {
        this.updateBreadcrumbSizes();
    },

    show: function()
    {
        WebInspector.Panel.prototype.show.call(this);
        this.sidebarResizeElement.style.right = (this.sidebarElement.offsetWidth - 3) + "px";
        this.updateBreadcrumb();
        this.treeOutline.updateSelection();
        if (this.recentlyModifiedNodes.length)
            this.updateModifiedNodes();
    },

    hide: function()
    {
        WebInspector.Panel.prototype.hide.call(this);

        WebInspector.highlightDOMNode(0);
        this.setSearchingForNode(false);
    },

    resize: function()
    {
        this.treeOutline.updateSelection();
        this.updateBreadcrumbSizes();
    },

    reset: function()
    {
        if (this.focusedDOMNode)
            this._selectedPathOnReset = this.focusedDOMNode.path();

        this.rootDOMNode = null;
        this.focusedDOMNode = null;

        WebInspector.highlightDOMNode(0);

        this.recentlyModifiedNodes = [];

        delete this.currentQuery;
    },

    setDocument: function(inspectedRootDocument)
    {
        this.reset();
        this.searchCanceled();

        if (!inspectedRootDocument)
            return;

        inspectedRootDocument.addEventListener("DOMNodeInserted", this._nodeInserted.bind(this));
        inspectedRootDocument.addEventListener("DOMNodeRemoved", this._nodeRemoved.bind(this));
        inspectedRootDocument.addEventListener("DOMAttrModified", this._attributesUpdated.bind(this));
        inspectedRootDocument.addEventListener("DOMCharacterDataModified", this._characterDataModified.bind(this));

        this.rootDOMNode = inspectedRootDocument;

        function selectNode(candidateFocusNode)
        {
            if (!candidateFocusNode)
                candidateFocusNode = inspectedRootDocument.body || inspectedRootDocument.documentElement;

            if (!candidateFocusNode)
                return;

            this.focusedDOMNode = candidateFocusNode;
            if (this.treeOutline.selectedTreeElement)
                this.treeOutline.selectedTreeElement.expand();
        }

        function selectLastSelectedNode(nodeId)
        {
            if (this.focusedDOMNode) {
                // Focused node has been explicitly set while reaching out for the last selected node.
                return;
            }
            var node = nodeId ? WebInspector.domAgent.nodeForId(nodeId) : 0;
            selectNode.call(this, node);
        }

        if (this._selectedPathOnReset)
            InspectorBackend.pushNodeByPathToFrontend(this._selectedPathOnReset, selectLastSelectedNode.bind(this));
        else
            selectNode.call(this);
        delete this._selectedPathOnReset;
    },

    searchCanceled: function()
    {
        delete this._searchQuery;
        this._hideSearchHighlights();

        WebInspector.updateSearchMatchesCount(0, this);

        this._currentSearchResultIndex = 0;
        this._searchResults = [];
        InspectorBackend.searchCanceled();
    },

    performSearch: function(query)
    {
        // Call searchCanceled since it will reset everything we need before doing a new search.
        this.searchCanceled();

        var whitespaceTrimmedQuery = query.trim();
        if (!whitespaceTrimmedQuery.length)
            return;

        this._updatedMatchCountOnce = false;
        this._matchesCountUpdateTimeout = null;
        this._searchQuery = query;

        InspectorBackend.performSearch(whitespaceTrimmedQuery, false);
    },

    populateHrefContextMenu: function(contextMenu, event, anchorElement)
    {
        if (!anchorElement.href)
            return false;

        var resourceURL = WebInspector.resourceURLForRelatedNode(this.focusedDOMNode, anchorElement.href);
        if (!resourceURL)
            return false;

        // Add resource-related actions.
        contextMenu.appendItem(WebInspector.openLinkExternallyLabel(), WebInspector.openResource.bind(null, resourceURL, false));
        if (WebInspector.resourceForURL(resourceURL))
            contextMenu.appendItem(WebInspector.UIString("Open Link in Resources Panel"), WebInspector.openResource.bind(null, resourceURL, true));
        return true;
    },

    switchToAndFocus: function(node)
    {
        // Reset search restore.
        WebInspector.cancelSearch();
        WebInspector.currentPanel = this;
        this.focusedDOMNode = node;
    },

    _updateMatchesCount: function()
    {
        WebInspector.updateSearchMatchesCount(this._searchResults.length, this);
        this._matchesCountUpdateTimeout = null;
        this._updatedMatchCountOnce = true;
    },

    _updateMatchesCountSoon: function()
    {
        if (!this._updatedMatchCountOnce)
            return this._updateMatchesCount();
        if (this._matchesCountUpdateTimeout)
            return;
        // Update the matches count every half-second so it doesn't feel twitchy.
        this._matchesCountUpdateTimeout = setTimeout(this._updateMatchesCount.bind(this), 500);
    },

    addNodesToSearchResult: function(nodeIds)
    {
        if (!nodeIds.length)
            return;

        for (var i = 0; i < nodeIds.length; ++i) {
            var nodeId = nodeIds[i];
            var node = WebInspector.domAgent.nodeForId(nodeId);
            if (!node)
                continue;

            this._currentSearchResultIndex = 0;
            this._searchResults.push(node);
        }
        this._highlightCurrentSearchResult();
        this._updateMatchesCountSoon();
    },

    jumpToNextSearchResult: function()
    {
        if (!this._searchResults || !this._searchResults.length)
            return;

        if (++this._currentSearchResultIndex >= this._searchResults.length)
            this._currentSearchResultIndex = 0;
        this._highlightCurrentSearchResult();
    },

    jumpToPreviousSearchResult: function()
    {
        if (!this._searchResults || !this._searchResults.length)
            return;

        if (--this._currentSearchResultIndex < 0)
            this._currentSearchResultIndex = (this._searchResults.length - 1);
        this._highlightCurrentSearchResult();
    },

    _highlightCurrentSearchResult: function()
    {
        this._hideSearchHighlights();
        var node = this._searchResults[this._currentSearchResultIndex];
        var treeElement = this.treeOutline.findTreeElement(node);
        if (treeElement) {
            treeElement.highlightSearchResults(this._searchQuery);
            treeElement.reveal();
        }
    },

    _hideSearchHighlights: function(node)
    {
        for (var i = 0; this._searchResults && i < this._searchResults.length; ++i) {
            var node = this._searchResults[i];
            var treeElement = this.treeOutline.findTreeElement(node);
            if (treeElement)
                treeElement.highlightSearchResults(null);
        }
    },

    renameSelector: function(oldIdentifier, newIdentifier, oldSelector, newSelector)
    {
        // TODO: Implement Shifting the oldSelector, and its contents to a newSelector
    },

    get rootDOMNode()
    {
        return this.treeOutline.rootDOMNode;
    },

    set rootDOMNode(x)
    {
        this.treeOutline.rootDOMNode = x;
    },

    get focusedDOMNode()
    {
        return this.treeOutline.focusedDOMNode;
    },

    set focusedDOMNode(x)
    {
        this.treeOutline.focusedDOMNode = x;
    },

    _attributesUpdated: function(event)
    {
        this.recentlyModifiedNodes.push({node: event.target, updated: true});
        if (this.visible)
            this._updateModifiedNodesSoon();
    },

    _characterDataModified: function(event)
    {
        this.recentlyModifiedNodes.push({node: event.target, updated: true});
        if (this.visible)
            this._updateModifiedNodesSoon();
    },

    _nodeInserted: function(event)
    {
        this.recentlyModifiedNodes.push({node: event.target, parent: event.relatedNode, inserted: true});
        if (this.visible)
            this._updateModifiedNodesSoon();
    },

    _nodeRemoved: function(event)
    {
        this.recentlyModifiedNodes.push({node: event.target, parent: event.relatedNode, removed: true});
        if (this.visible)
            this._updateModifiedNodesSoon();
    },

    _updateModifiedNodesSoon: function()
    {
        if ("_updateModifiedNodesTimeout" in this)
            return;
        this._updateModifiedNodesTimeout = setTimeout(this.updateModifiedNodes.bind(this), 0);
    },

    updateModifiedNodes: function()
    {
        if ("_updateModifiedNodesTimeout" in this) {
            clearTimeout(this._updateModifiedNodesTimeout);
            delete this._updateModifiedNodesTimeout;
        }

        var updatedParentTreeElements = [];
        var updateBreadcrumbs = false;

        for (var i = 0; i < this.recentlyModifiedNodes.length; ++i) {
            var replaced = this.recentlyModifiedNodes[i].replaced;
            var parent = this.recentlyModifiedNodes[i].parent;
            var node = this.recentlyModifiedNodes[i].node;

            if (this.recentlyModifiedNodes[i].updated) {
                var nodeItem = this.treeOutline.findTreeElement(node);
                if (nodeItem)
                    nodeItem.updateTitle();
                continue;
            }
            
            if (!parent)
                continue;

            var parentNodeItem = this.treeOutline.findTreeElement(parent);
            if (parentNodeItem && !parentNodeItem.alreadyUpdatedChildren) {
                parentNodeItem.updateChildren(replaced);
                parentNodeItem.alreadyUpdatedChildren = true;
                updatedParentTreeElements.push(parentNodeItem);
            }

            if (!updateBreadcrumbs && (this.focusedDOMNode === parent || isAncestorNode(this.focusedDOMNode, parent)))
                updateBreadcrumbs = true;
        }

        for (var i = 0; i < updatedParentTreeElements.length; ++i)
            delete updatedParentTreeElements[i].alreadyUpdatedChildren;

        this.recentlyModifiedNodes = [];

        if (updateBreadcrumbs)
            this.updateBreadcrumb(true);
    },

    _stylesPaneEdited: function()
    {
        // Once styles are edited, the Metrics pane should be updated.
        this.sidebarPanes.metrics.needsUpdate = true;
        this.updateMetrics();
    },

    _metricsPaneEdited: function()
    {
        // Once metrics are edited, the Styles pane should be updated.
        this.sidebarPanes.styles.needsUpdate = true;
        this.updateStyles(true);
    },

    _styleSheetChanged: function()
    {
        this._metricsPaneEdited();
        this._stylesPaneEdited();
    },

    _mouseMovedInCrumbs: function(event)
    {
        var nodeUnderMouse = document.elementFromPoint(event.pageX, event.pageY);
        var crumbElement = nodeUnderMouse.enclosingNodeOrSelfWithClass("crumb");

        WebInspector.highlightDOMNode(crumbElement ? crumbElement.representedObject.id : 0);

        if ("_mouseOutOfCrumbsTimeout" in this) {
            clearTimeout(this._mouseOutOfCrumbsTimeout);
            delete this._mouseOutOfCrumbsTimeout;
        }
    },

    _mouseMovedOutOfCrumbs: function(event)
    {
        var nodeUnderMouse = document.elementFromPoint(event.pageX, event.pageY);
        if (nodeUnderMouse && nodeUnderMouse.isDescendant(this.crumbsElement))
            return;

        WebInspector.highlightDOMNode(0);

        this._mouseOutOfCrumbsTimeout = setTimeout(this.updateBreadcrumbSizes.bind(this), 1000);
    },

    updateBreadcrumb: function(forceUpdate)
    {
        if (!this.visible)
            return;

        var crumbs = this.crumbsElement;

        var handled = false;
        var foundRoot = false;
        var crumb = crumbs.firstChild;
        while (crumb) {
            if (crumb.representedObject === this.rootDOMNode)
                foundRoot = true;

            if (foundRoot)
                crumb.addStyleClass("dimmed");
            else
                crumb.removeStyleClass("dimmed");

            if (crumb.representedObject === this.focusedDOMNode) {
                crumb.addStyleClass("selected");
                handled = true;
            } else {
                crumb.removeStyleClass("selected");
            }

            crumb = crumb.nextSibling;
        }

        if (handled && !forceUpdate) {
            // We don't need to rebuild the crumbs, but we need to adjust sizes
            // to reflect the new focused or root node.
            this.updateBreadcrumbSizes();
            return;
        }

        crumbs.removeChildren();

        var panel = this;

        function selectCrumbFunction(event)
        {
            var crumb = event.currentTarget;
            if (crumb.hasStyleClass("collapsed")) {
                // Clicking a collapsed crumb will expose the hidden crumbs.
                if (crumb === panel.crumbsElement.firstChild) {
                    // If the focused crumb is the first child, pick the farthest crumb
                    // that is still hidden. This allows the user to expose every crumb.
                    var currentCrumb = crumb;
                    while (currentCrumb) {
                        var hidden = currentCrumb.hasStyleClass("hidden");
                        var collapsed = currentCrumb.hasStyleClass("collapsed");
                        if (!hidden && !collapsed)
                            break;
                        crumb = currentCrumb;
                        currentCrumb = currentCrumb.nextSibling;
                    }
                }

                panel.updateBreadcrumbSizes(crumb);
            } else {
                // Clicking a dimmed crumb or double clicking (event.detail >= 2)
                // will change the root node in addition to the focused node.
                if (event.detail >= 2 || crumb.hasStyleClass("dimmed"))
                    panel.rootDOMNode = crumb.representedObject.parentNode;
                panel.focusedDOMNode = crumb.representedObject;
            }

            event.preventDefault();
        }

        foundRoot = false;
        for (var current = this.focusedDOMNode; current; current = current.parentNode) {
            if (current.nodeType === Node.DOCUMENT_NODE)
                continue;

            if (current === this.rootDOMNode)
                foundRoot = true;

            var crumb = document.createElement("span");
            crumb.className = "crumb";
            crumb.representedObject = current;
            crumb.addEventListener("mousedown", selectCrumbFunction, false);

            var crumbTitle;
            switch (current.nodeType) {
                case Node.ELEMENT_NODE:
                    this.decorateNodeLabel(current, crumb);
                    break;

                case Node.TEXT_NODE:
                    if (isNodeWhitespace.call(current))
                        crumbTitle = WebInspector.UIString("(whitespace)");
                    else
                        crumbTitle = WebInspector.UIString("(text)");
                    break

                case Node.COMMENT_NODE:
                    crumbTitle = "<!-->";
                    break;

                case Node.DOCUMENT_TYPE_NODE:
                    crumbTitle = "<!DOCTYPE>";
                    break;

                default:
                    crumbTitle = this.treeOutline.nodeNameToCorrectCase(current.nodeName);
            }

            if (!crumb.childNodes.length) {
                var nameElement = document.createElement("span");
                nameElement.textContent = crumbTitle;
                crumb.appendChild(nameElement);
                crumb.title = crumbTitle;
            }

            if (foundRoot)
                crumb.addStyleClass("dimmed");
            if (current === this.focusedDOMNode)
                crumb.addStyleClass("selected");
            if (!crumbs.childNodes.length)
                crumb.addStyleClass("end");

            crumbs.appendChild(crumb);
        }

        if (crumbs.hasChildNodes())
            crumbs.lastChild.addStyleClass("start");

        this.updateBreadcrumbSizes();
    },

    decorateNodeLabel: function(node, parentElement)
    {
        var title = this.treeOutline.nodeNameToCorrectCase(node.nodeName);

        var nameElement = document.createElement("span");
        nameElement.textContent = title;
        parentElement.appendChild(nameElement);

        var idAttribute = node.getAttribute("id");
        if (idAttribute) {
            var idElement = document.createElement("span");
            parentElement.appendChild(idElement);

            var part = "#" + idAttribute;
            title += part;
            idElement.appendChild(document.createTextNode(part));

            // Mark the name as extra, since the ID is more important.
            nameElement.className = "extra";
        }

        var classAttribute = node.getAttribute("class");
        if (classAttribute) {
            var classes = classAttribute.split(/\s+/);
            var foundClasses = {};

            if (classes.length) {
                var classesElement = document.createElement("span");
                classesElement.className = "extra";
                parentElement.appendChild(classesElement);

                for (var i = 0; i < classes.length; ++i) {
                    var className = classes[i];
                    if (className && !(className in foundClasses)) {
                        var part = "." + className;
                        title += part;
                        classesElement.appendChild(document.createTextNode(part));
                        foundClasses[className] = true;
                    }
                }
            }
        }
        parentElement.title = title;
    },

    linkifyNodeReference: function(node)
    {
        var link = document.createElement("span");
        link.className = "node-link";
        this.decorateNodeLabel(node, link);
        WebInspector.wireElementWithDOMNode(link, node.id);
        return link;
    },

    linkifyNodeById: function(nodeId)
    {
        var node = WebInspector.domAgent.nodeForId(nodeId);
        if (!node)
            return document.createTextNode(WebInspector.UIString("<node>"));
        return this.linkifyNodeReference(node);
    },

    updateBreadcrumbSizes: function(focusedCrumb)
    {
        if (!this.visible)
            return;

        if (document.body.offsetWidth <= 0) {
            // The stylesheet hasn't loaded yet or the window is closed,
            // so we can't calculate what is need. Return early.
            return;
        }

        var crumbs = this.crumbsElement;
        if (!crumbs.childNodes.length || crumbs.offsetWidth <= 0)
            return; // No crumbs, do nothing.

        // A Zero index is the right most child crumb in the breadcrumb.
        var selectedIndex = 0;
        var focusedIndex = 0;
        var selectedCrumb;

        var i = 0;
        var crumb = crumbs.firstChild;
        while (crumb) {
            // Find the selected crumb and index. 
            if (!selectedCrumb && crumb.hasStyleClass("selected")) {
                selectedCrumb = crumb;
                selectedIndex = i;
            }

            // Find the focused crumb index. 
            if (crumb === focusedCrumb)
                focusedIndex = i;

            // Remove any styles that affect size before
            // deciding to shorten any crumbs.
            if (crumb !== crumbs.lastChild)
                crumb.removeStyleClass("start");
            if (crumb !== crumbs.firstChild)
                crumb.removeStyleClass("end");

            crumb.removeStyleClass("compact");
            crumb.removeStyleClass("collapsed");
            crumb.removeStyleClass("hidden");

            crumb = crumb.nextSibling;
            ++i;
        }

        // Restore the start and end crumb classes in case they got removed in coalesceCollapsedCrumbs().
        // The order of the crumbs in the document is opposite of the visual order.
        crumbs.firstChild.addStyleClass("end");
        crumbs.lastChild.addStyleClass("start");

        function crumbsAreSmallerThanContainer()
        {
            var rightPadding = 20;
            var errorWarningElement = document.getElementById("error-warning-count");
            if (!WebInspector.drawer.visible && errorWarningElement)
                rightPadding += errorWarningElement.offsetWidth;
            return ((crumbs.totalOffsetLeft + crumbs.offsetWidth + rightPadding) < window.innerWidth);
        }

        if (crumbsAreSmallerThanContainer())
            return; // No need to compact the crumbs, they all fit at full size.

        var BothSides = 0;
        var AncestorSide = -1;
        var ChildSide = 1;

        function makeCrumbsSmaller(shrinkingFunction, direction, significantCrumb)
        {
            if (!significantCrumb)
                significantCrumb = (focusedCrumb || selectedCrumb);

            if (significantCrumb === selectedCrumb)
                var significantIndex = selectedIndex;
            else if (significantCrumb === focusedCrumb)
                var significantIndex = focusedIndex;
            else {
                var significantIndex = 0;
                for (var i = 0; i < crumbs.childNodes.length; ++i) {
                    if (crumbs.childNodes[i] === significantCrumb) {
                        significantIndex = i;
                        break;
                    }
                }
            }

            function shrinkCrumbAtIndex(index)
            {
                var shrinkCrumb = crumbs.childNodes[index];
                if (shrinkCrumb && shrinkCrumb !== significantCrumb)
                    shrinkingFunction(shrinkCrumb);
                if (crumbsAreSmallerThanContainer())
                    return true; // No need to compact the crumbs more.
                return false;
            }

            // Shrink crumbs one at a time by applying the shrinkingFunction until the crumbs
            // fit in the container or we run out of crumbs to shrink.
            if (direction) {
                // Crumbs are shrunk on only one side (based on direction) of the signifcant crumb.
                var index = (direction > 0 ? 0 : crumbs.childNodes.length - 1);
                while (index !== significantIndex) {
                    if (shrinkCrumbAtIndex(index))
                        return true;
                    index += (direction > 0 ? 1 : -1);
                }
            } else {
                // Crumbs are shrunk in order of descending distance from the signifcant crumb,
                // with a tie going to child crumbs.
                var startIndex = 0;
                var endIndex = crumbs.childNodes.length - 1;
                while (startIndex != significantIndex || endIndex != significantIndex) {
                    var startDistance = significantIndex - startIndex;
                    var endDistance = endIndex - significantIndex;
                    if (startDistance >= endDistance)
                        var index = startIndex++;
                    else
                        var index = endIndex--;
                    if (shrinkCrumbAtIndex(index))
                        return true;
                }
            }

            // We are not small enough yet, return false so the caller knows.
            return false;
        }

        function coalesceCollapsedCrumbs()
        {
            var crumb = crumbs.firstChild;
            var collapsedRun = false;
            var newStartNeeded = false;
            var newEndNeeded = false;
            while (crumb) {
                var hidden = crumb.hasStyleClass("hidden");
                if (!hidden) {
                    var collapsed = crumb.hasStyleClass("collapsed"); 
                    if (collapsedRun && collapsed) {
                        crumb.addStyleClass("hidden");
                        crumb.removeStyleClass("compact");
                        crumb.removeStyleClass("collapsed");

                        if (crumb.hasStyleClass("start")) {
                            crumb.removeStyleClass("start");
                            newStartNeeded = true;
                        }

                        if (crumb.hasStyleClass("end")) {
                            crumb.removeStyleClass("end");
                            newEndNeeded = true;
                        }

                        continue;
                    }

                    collapsedRun = collapsed;

                    if (newEndNeeded) {
                        newEndNeeded = false;
                        crumb.addStyleClass("end");
                    }
                } else
                    collapsedRun = true;
                crumb = crumb.nextSibling;
            }

            if (newStartNeeded) {
                crumb = crumbs.lastChild;
                while (crumb) {
                    if (!crumb.hasStyleClass("hidden")) {
                        crumb.addStyleClass("start");
                        break;
                    }
                    crumb = crumb.previousSibling;
                }
            }
        }

        function compact(crumb)
        {
            if (crumb.hasStyleClass("hidden"))
                return;
            crumb.addStyleClass("compact");
        }

        function collapse(crumb, dontCoalesce)
        {
            if (crumb.hasStyleClass("hidden"))
                return;
            crumb.addStyleClass("collapsed");
            crumb.removeStyleClass("compact");
            if (!dontCoalesce)
                coalesceCollapsedCrumbs();
        }

        function compactDimmed(crumb)
        {
            if (crumb.hasStyleClass("dimmed"))
                compact(crumb);
        }

        function collapseDimmed(crumb)
        {
            if (crumb.hasStyleClass("dimmed"))
                collapse(crumb);
        }

        if (!focusedCrumb) {
            // When not focused on a crumb we can be biased and collapse less important
            // crumbs that the user might not care much about.

            // Compact child crumbs.
            if (makeCrumbsSmaller(compact, ChildSide))
                return;

            // Collapse child crumbs.
            if (makeCrumbsSmaller(collapse, ChildSide))
                return;

            // Compact dimmed ancestor crumbs.
            if (makeCrumbsSmaller(compactDimmed, AncestorSide))
                return;

            // Collapse dimmed ancestor crumbs.
            if (makeCrumbsSmaller(collapseDimmed, AncestorSide))
                return;
        }

        // Compact ancestor crumbs, or from both sides if focused.
        if (makeCrumbsSmaller(compact, (focusedCrumb ? BothSides : AncestorSide)))
            return;

        // Collapse ancestor crumbs, or from both sides if focused.
        if (makeCrumbsSmaller(collapse, (focusedCrumb ? BothSides : AncestorSide)))
            return;

        if (!selectedCrumb)
            return;

        // Compact the selected crumb.
        compact(selectedCrumb);
        if (crumbsAreSmallerThanContainer())
            return;

        // Collapse the selected crumb as a last resort. Pass true to prevent coalescing.
        collapse(selectedCrumb, true);
    },

    updateStyles: function(forceUpdate)
    {
        var stylesSidebarPane = this.sidebarPanes.styles;
        var computedStylePane = this.sidebarPanes.computedStyle;
        if ((!stylesSidebarPane.expanded && !computedStylePane.expanded) || !stylesSidebarPane.needsUpdate)
            return;

        stylesSidebarPane.update(this.focusedDOMNode, null, forceUpdate);
        stylesSidebarPane.needsUpdate = false;
    },

    updateMetrics: function()
    {
        var metricsSidebarPane = this.sidebarPanes.metrics;
        if (!metricsSidebarPane.expanded || !metricsSidebarPane.needsUpdate)
            return;

        metricsSidebarPane.update(this.focusedDOMNode);
        metricsSidebarPane.needsUpdate = false;
    },

    updateProperties: function()
    {
        var propertiesSidebarPane = this.sidebarPanes.properties;
        if (!propertiesSidebarPane.expanded || !propertiesSidebarPane.needsUpdate)
            return;

        propertiesSidebarPane.update(this.focusedDOMNode);
        propertiesSidebarPane.needsUpdate = false;
    },

    updateEventListeners: function()
    {
        var eventListenersSidebarPane = this.sidebarPanes.eventListeners;
        if (!eventListenersSidebarPane.expanded || !eventListenersSidebarPane.needsUpdate)
            return;

        eventListenersSidebarPane.update(this.focusedDOMNode);
        eventListenersSidebarPane.needsUpdate = false;
    },

    _registerShortcuts: function()
    {
        var shortcut = WebInspector.KeyboardShortcut;
        var section = WebInspector.shortcutsHelp.section(WebInspector.UIString("Elements Panel"));
        var keys = [
            shortcut.shortcutToString(shortcut.Keys.Up),
            shortcut.shortcutToString(shortcut.Keys.Down)
        ];
        section.addRelatedKeys(keys, WebInspector.UIString("Navigate elements"));
        var keys = [
            shortcut.shortcutToString(shortcut.Keys.Right),
            shortcut.shortcutToString(shortcut.Keys.Left)
        ];
        section.addRelatedKeys(keys, WebInspector.UIString("Expand/collapse"));
        section.addKey(shortcut.shortcutToString(shortcut.Keys.Enter), WebInspector.UIString("Edit attribute"));

        this.sidebarPanes.styles.registerShortcuts();
    },

    handleShortcut: function(event)
    {
        // Cmd/Control + Shift + C should be a shortcut to clicking the Node Search Button.
        // This shortcut matches Firebug.
        if (event.keyIdentifier === "U+0043") {     // C key
            if (WebInspector.isMac())
                var isNodeSearchKey = event.metaKey && !event.ctrlKey && !event.altKey && event.shiftKey;
            else
                var isNodeSearchKey = event.ctrlKey && !event.metaKey && !event.altKey && event.shiftKey;

            if (isNodeSearchKey) {
                this.toggleSearchingForNode();
                event.handled = true;
                return;
            }
        }
    },

    handleCopyEvent: function(event)
    {
        // Don't prevent the normal copy if the user has a selection.
        if (!window.getSelection().isCollapsed)
            return;
        event.clipboardData.clearData();
        event.preventDefault();
        InspectorBackend.copyNode(this.focusedDOMNode.id);
    },

    rightSidebarResizerDragStart: function(event)
    {
        WebInspector.elementDragStart(this.sidebarElement, this.rightSidebarResizerDrag.bind(this), this.rightSidebarResizerDragEnd.bind(this), event, "col-resize");
    },

    rightSidebarResizerDragEnd: function(event)
    {
        WebInspector.elementDragEnd(event);
        this.saveSidebarWidth();
    },

    rightSidebarResizerDrag: function(event)
    {
        var x = event.pageX;
        var newWidth = Number.constrain(window.innerWidth - x, Preferences.minElementsSidebarWidth, window.innerWidth * 0.66);
        this.setSidebarWidth(newWidth);
        event.preventDefault();
    },

    setSidebarWidth: function(newWidth)
    {
        this.sidebarElement.style.width = newWidth + "px";
        this.contentElement.style.right = newWidth + "px";
        this.sidebarResizeElement.style.right = (newWidth - 3) + "px";
        this.treeOutline.updateSelection();
    },

    updateFocusedNode: function(nodeId)
    {
        var node = WebInspector.domAgent.nodeForId(nodeId);
        if (!node)
            return;

        this.focusedDOMNode = node;
        this._nodeSearchButton.toggled = false;
    },

    _setSearchingForNode: function(enabled)
    {
        this._nodeSearchButton.toggled = enabled;
    },

    setSearchingForNode: function(enabled)
    {
        InspectorBackend.setSearchingForNode(enabled, this._setSearchingForNode.bind(this));
    },

    toggleSearchingForNode: function()
    {
        this.setSearchingForNode(!this._nodeSearchButton.toggled);
    },

    elementsToRestoreScrollPositionsFor: function()
    {
        return [ this.contentElement, this.sidebarElement ];
    }
}

WebInspector.ElementsPanel.prototype.__proto__ = WebInspector.Panel.prototype;
