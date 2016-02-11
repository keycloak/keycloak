/*
 * Copyright (C) 2009 280 North Inc. All Rights Reserved.
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

WebInspector.ProfileDataGridNode = function(profileView, profileNode, owningTree, hasChildren)
{
    this.profileView = profileView;
    this.profileNode = profileNode;

    WebInspector.DataGridNode.call(this, null, hasChildren);

    this.addEventListener("populate", this._populate, this);

    this.tree = owningTree;

    this.childrenByCallUID = {};
    this.lastComparator = null;

    this.callUID = profileNode.callUID;
    this.selfTime = profileNode.selfTime;
    this.totalTime = profileNode.totalTime;
    this.functionName = profileNode.functionName;
    this.numberOfCalls = profileNode.numberOfCalls;
    this.url = profileNode.url;
}

WebInspector.ProfileDataGridNode.prototype = {
    get data()
    {
        function formatMilliseconds(time)
        {
            return Number.secondsToString(time / 1000, !Preferences.samplingCPUProfiler);
        }

        var data = {};

        data["function"] = this.functionName;
        data["calls"] = this.numberOfCalls;

        if (this.profileView.showSelfTimeAsPercent)
            data["self"] = WebInspector.UIString("%.2f%%", this.selfPercent);
        else
            data["self"] = formatMilliseconds(this.selfTime);

        if (this.profileView.showTotalTimeAsPercent)
            data["total"] = WebInspector.UIString("%.2f%%", this.totalPercent);
        else
            data["total"] = formatMilliseconds(this.totalTime);

        if (this.profileView.showAverageTimeAsPercent)
            data["average"] = WebInspector.UIString("%.2f%%", this.averagePercent);
        else
            data["average"] = formatMilliseconds(this.averageTime);

        return data;
    },

    createCell: function(columnIdentifier)
    {
        var cell = WebInspector.DataGridNode.prototype.createCell.call(this, columnIdentifier);

        if (columnIdentifier === "self" && this._searchMatchedSelfColumn)
            cell.addStyleClass("highlight");
        else if (columnIdentifier === "total" && this._searchMatchedTotalColumn)
            cell.addStyleClass("highlight");
        else if (columnIdentifier === "average" && this._searchMatchedAverageColumn)
            cell.addStyleClass("highlight");
        else if (columnIdentifier === "calls" && this._searchMatchedCallsColumn)
            cell.addStyleClass("highlight");

        if (columnIdentifier !== "function")
            return cell;

        if (this.profileNode._searchMatchedFunctionColumn)
            cell.addStyleClass("highlight");

        if (this.profileNode.url) {
            var lineNumber;
            if (this.profileNode.lineNumber > 0)
                lineNumber = this.profileNode.lineNumber;
            var urlElement = WebInspector.linkifyResourceAsNode(this.profileNode.url, "scripts", lineNumber, "profile-node-file");
            cell.insertBefore(urlElement, cell.firstChild);
        }

        return cell;
    },

    select: function(supressSelectedEvent)
    {
        WebInspector.DataGridNode.prototype.select.call(this, supressSelectedEvent);
        this.profileView._dataGridNodeSelected(this);
    },

    deselect: function(supressDeselectedEvent)
    {
        WebInspector.DataGridNode.prototype.deselect.call(this, supressDeselectedEvent);
        this.profileView._dataGridNodeDeselected(this);
    },

    sort: function(/*Function*/ comparator, /*Boolean*/ force)
    {
        var gridNodeGroups = [[this]];

        for (var gridNodeGroupIndex = 0; gridNodeGroupIndex < gridNodeGroups.length; ++gridNodeGroupIndex) {
            var gridNodes = gridNodeGroups[gridNodeGroupIndex];
            var count = gridNodes.length;

            for (var index = 0; index < count; ++index) {
                var gridNode = gridNodes[index];

                // If the grid node is collapsed, then don't sort children (save operation for later).
                // If the grid node has the same sorting as previously, then there is no point in sorting it again.
                if (!force && (!gridNode.expanded || gridNode.lastComparator === comparator)) {
                    if (gridNode.children.length)
                        gridNode.shouldRefreshChildren = true;
                    continue;
                }

                gridNode.lastComparator = comparator;

                var children = gridNode.children;
                var childCount = children.length;

                if (childCount) {
                    children.sort(comparator);

                    for (var childIndex = 0; childIndex < childCount; ++childIndex)
                        children[childIndex]._recalculateSiblings(childIndex);

                    gridNodeGroups.push(children);
                }
            }
        }
    },

    insertChild: function(/*ProfileDataGridNode*/ profileDataGridNode, index)
    {
        WebInspector.DataGridNode.prototype.insertChild.call(this, profileDataGridNode, index);

        this.childrenByCallUID[profileDataGridNode.callUID] = profileDataGridNode;
    },

    removeChild: function(/*ProfileDataGridNode*/ profileDataGridNode)
    {
        WebInspector.DataGridNode.prototype.removeChild.call(this, profileDataGridNode);

        delete this.childrenByCallUID[profileDataGridNode.callUID];
    },

    removeChildren: function(/*ProfileDataGridNode*/ profileDataGridNode)
    {
        WebInspector.DataGridNode.prototype.removeChildren.call(this);

        this.childrenByCallUID = {};
    },

    findChild: function(/*Node*/ node)
    {
        if (!node)
            return null;
        return this.childrenByCallUID[node.callUID];
    },

    get averageTime()
    {
        return this.selfTime / Math.max(1, this.numberOfCalls);
    },

    get averagePercent()
    {
        return this.averageTime / this.tree.totalTime * 100.0;
    },

    get selfPercent()
    {
        return this.selfTime / this.tree.totalTime * 100.0;
    },

    get totalPercent()
    {
        return this.totalTime / this.tree.totalTime * 100.0;
    },

    get _parent()
    {
        return this.parent !== this.dataGrid ? this.parent : this.tree;
    },

    _populate: function(event)
    {
        this._sharedPopulate();

        if (this._parent) {
            var currentComparator = this._parent.lastComparator;

            if (currentComparator)
                this.sort(currentComparator, true);
        }

        if (this.removeEventListener)
            this.removeEventListener("populate", this._populate, this);
    },

    // When focusing and collapsing we modify lots of nodes in the tree.
    // This allows us to restore them all to their original state when we revert.
    _save: function()
    {
        if (this._savedChildren)
            return;

        this._savedSelfTime = this.selfTime;
        this._savedTotalTime = this.totalTime;
        this._savedNumberOfCalls = this.numberOfCalls;

        this._savedChildren = this.children.slice();
    },

    // When focusing and collapsing we modify lots of nodes in the tree.
    // This allows us to restore them all to their original state when we revert.
    _restore: function()
    {
        if (!this._savedChildren)
            return;

        this.selfTime = this._savedSelfTime;
        this.totalTime = this._savedTotalTime;
        this.numberOfCalls = this._savedNumberOfCalls;

        this.removeChildren();

        var children = this._savedChildren;
        var count = children.length;

        for (var index = 0; index < count; ++index) {
            children[index]._restore();
            this.appendChild(children[index]);
        }
    },

    _merge: function(child, shouldAbsorb)
    {
        this.selfTime += child.selfTime;

        if (!shouldAbsorb) {
            this.totalTime += child.totalTime;
            this.numberOfCalls += child.numberOfCalls;
        }

        var children = this.children.slice();

        this.removeChildren();

        var count = children.length;

        for (var index = 0; index < count; ++index) {
            if (!shouldAbsorb || children[index] !== child)
                this.appendChild(children[index]);
        }

        children = child.children.slice();
        count = children.length;

        for (var index = 0; index < count; ++index) {
            var orphanedChild = children[index],
                existingChild = this.childrenByCallUID[orphanedChild.callUID];

            if (existingChild)
                existingChild._merge(orphanedChild, false);
            else
                this.appendChild(orphanedChild);
        }
    }
}

WebInspector.ProfileDataGridNode.prototype.__proto__ = WebInspector.DataGridNode.prototype;

WebInspector.ProfileDataGridTree = function(profileView, profileNode)
{
    this.tree = this;
    this.children = [];

    this.profileView = profileView;

    this.totalTime = profileNode.totalTime;
    this.lastComparator = null;

    this.childrenByCallUID = {};
}

WebInspector.ProfileDataGridTree.prototype = {
    get expanded()
    {
        return true;
    },

    appendChild: function(child)
    {
        this.insertChild(child, this.children.length);
    },

    insertChild: function(child, index)
    {
        this.children.splice(index, 0, child);
        this.childrenByCallUID[child.callUID] = child;
    },

    removeChildren: function()
    {
        this.children = [];
        this.childrenByCallUID = {};
    },

    findChild: WebInspector.ProfileDataGridNode.prototype.findChild,
    sort: WebInspector.ProfileDataGridNode.prototype.sort,

    _save: function()
    {
        if (this._savedChildren)
            return;

        this._savedTotalTime = this.totalTime;
        this._savedChildren = this.children.slice();
    },

    restore: function()
    {
        if (!this._savedChildren)
            return;

        this.children = this._savedChildren;
        this.totalTime = this._savedTotalTime;

        var children = this.children;
        var count = children.length;

        for (var index = 0; index < count; ++index)
            children[index]._restore();

        this._savedChildren = null;
    }
}

WebInspector.ProfileDataGridTree.propertyComparators = [{}, {}];

WebInspector.ProfileDataGridTree.propertyComparator = function(/*String*/ property, /*Boolean*/ isAscending)
{
    var comparator = this.propertyComparators[(isAscending ? 1 : 0)][property];

    if (!comparator) {
        if (isAscending) {
            comparator = function(lhs, rhs)
            {
                if (lhs[property] < rhs[property])
                    return -1;

                if (lhs[property] > rhs[property])
                    return 1;

                return 0;
            }
        } else {
            comparator = function(lhs, rhs)
            {
                if (lhs[property] > rhs[property])
                    return -1;

                if (lhs[property] < rhs[property])
                    return 1;

                return 0;
            }
        }

        this.propertyComparators[(isAscending ? 1 : 0)][property] = comparator;
    }

    return comparator;
}
