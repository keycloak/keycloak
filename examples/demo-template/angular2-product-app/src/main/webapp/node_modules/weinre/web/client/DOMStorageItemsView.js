/*
 * Copyright (C) 2008 Nokia Inc.  All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY
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

WebInspector.DOMStorageItemsView = function(domStorage)
{
    WebInspector.View.call(this);

    this.domStorage = domStorage;

    this.element.addStyleClass("storage-view");
    this.element.addStyleClass("table");

    this.deleteButton = new WebInspector.StatusBarButton(WebInspector.UIString("Delete"), "delete-storage-status-bar-item");
    this.deleteButton.visible = false;
    this.deleteButton.addEventListener("click", this._deleteButtonClicked.bind(this), false);

    this.refreshButton = new WebInspector.StatusBarButton(WebInspector.UIString("Refresh"), "refresh-storage-status-bar-item");
    this.refreshButton.addEventListener("click", this._refreshButtonClicked.bind(this), false);
}

WebInspector.DOMStorageItemsView.prototype = {
    get statusBarItems()
    {
        return [this.refreshButton.element, this.deleteButton.element];
    },

    show: function(parentElement)
    {
        WebInspector.View.prototype.show.call(this, parentElement);
        this.update();
    },

    hide: function()
    {
        WebInspector.View.prototype.hide.call(this);
        this.deleteButton.visible = false;
    },

    update: function()
    {
        this.element.removeChildren();
        var callback = this._showDOMStorageEntries.bind(this);
        this.domStorage.getEntries(callback);
    },

    _showDOMStorageEntries: function(entries)
    {
        this._dataGrid = this._dataGridForDOMStorageEntries(entries);
        this.element.appendChild(this._dataGrid.element);
        this._dataGrid.autoSizeColumns(10);
        this.deleteButton.visible = true;
    },

    resize: function()
    {
        if (this._dataGrid)
            this._dataGrid.updateWidths();
    },

    _dataGridForDOMStorageEntries: function(entries)
    {
        var columns = {};
        columns[0] = {};
        columns[1] = {};
        columns[0].title = WebInspector.UIString("Key");
        columns[1].title = WebInspector.UIString("Value");

        var nodes = [];

        var keys = [];
        var length = entries.length;
        for (var i = 0; i < entries.length; i++) {
            var data = {};

            var key = entries[i][0];
            data[0] = key;
            var value = entries[i][1];
            data[1] = value;
            var node = new WebInspector.DataGridNode(data, false);
            node.selectable = true;
            nodes.push(node);
            keys.push(key);
        }

        var dataGrid = new WebInspector.DataGrid(columns, this._editingCallback.bind(this), this._deleteCallback.bind(this));
        var length = nodes.length;
        for (var i = 0; i < length; ++i)
            dataGrid.appendChild(nodes[i]);
        dataGrid.addCreationNode(false);
        if (length > 0)
            nodes[0].selected = true;
        return dataGrid;
    },

    _deleteButtonClicked: function(event)
    {
        if (!this._dataGrid || !this._dataGrid.selectedNode)
            return;

        this._deleteCallback(this._dataGrid.selectedNode);
    },

    _refreshButtonClicked: function(event)
    {
        this.update();
    },
    
    _editingCallback: function(editingNode, columnIdentifier, oldText, newText)
    {
        var domStorage = this.domStorage;
        if (columnIdentifier === 0) {
            if (oldText)
                domStorage.removeItem(oldText);

            domStorage.setItem(newText, editingNode.data[1]);
        } else {
            domStorage.setItem(editingNode.data[0], newText);
        }
        
        this.update();
    },
    
    _deleteCallback: function(node)
    {
        if (!node || node.isCreationNode)
            return;

        if (this.domStorage)
            this.domStorage.removeItem(node.data[0]);
            
        this.update();
    }
}

WebInspector.DOMStorageItemsView.prototype.__proto__ = WebInspector.View.prototype;
