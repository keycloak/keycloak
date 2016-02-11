/*
 * Copyright (C) 2009 Apple Inc.  All rights reserved.
 * Copyright (C) 2009 Joseph Pecoraro
 * Copyright (C) 2010 Google Inc. All rights reserved.
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

WebInspector.CookiesTable = function(cookieDomain, expandable, deleteCallback)
{
    this._cookieDomain = cookieDomain;

    var columns = { 0: {}, 1: {}, 2: {}, 3: {}, 4: {}, 5: {}, 6: {}, 7: {} };
    columns[0].title = WebInspector.UIString("Name");
    columns[0].sortable = true;
    columns[0].disclosure = expandable;
    columns[0].width = "24%";
    columns[1].title = WebInspector.UIString("Value");
    columns[1].sortable = true;
    columns[1].width = "34%";
    columns[2].title = WebInspector.UIString("Domain");
    columns[2].sortable = true;
    columns[2].width = "7%";
    columns[3].title = WebInspector.UIString("Path");
    columns[3].sortable = true;
    columns[3].width = "7%";
    columns[4].title = WebInspector.UIString("Expires");
    columns[4].sortable = true;
    columns[4].width = "7%";
    columns[5].title = WebInspector.UIString("Size");
    columns[5].aligned = "right";
    columns[5].sortable = true;
    columns[5].width = "7%";
    columns[6].title = WebInspector.UIString("HTTP");
    columns[6].aligned = "centered";
    columns[6].sortable = true;
    columns[6].width = "7%";
    columns[7].title = WebInspector.UIString("Secure");
    columns[7].aligned = "centered";
    columns[7].sortable = true;
    columns[7].width = "7%";

    this._dataGrid = new WebInspector.DataGrid(columns, null, deleteCallback ? this._onDeleteFromGrid.bind(this) : null);
    this._dataGrid.addEventListener("sorting changed", this._rebuildTable, this);

    this.element = this._dataGrid.element;
    this._data = [];
    this._deleteCallback = deleteCallback;
}

WebInspector.CookiesTable.prototype = {
    updateWidths: function()
    {
        if (this._dataGrid)
            this._dataGrid.updateWidths();
    },

    setCookies: function(cookies)
    {
        this._data = [{cookies: cookies}];
        this._rebuildTable();
    },

    addCookiesFolder: function(folderName, cookies)
    {
        this._data.push({cookies: cookies, folderName: folderName});
        this._rebuildTable();
    },

    get selectedCookie()
    {
        var node = this._dataGrid.selectedNode;
        return node ? node.cookie : null;
    },

    _rebuildTable: function()
    {
        this._dataGrid.removeChildren();
        for (var i = 0; i < this._data.length; ++i) {
            var item = this._data[i];
            if (item.folderName) {
                var groupData = [ item.folderName, "", "", "", "", this._totalSize(item.cookies), "", "" ];
                var groupNode = new WebInspector.DataGridNode(groupData);
                groupNode.selectable = true;
                this._dataGrid.appendChild(groupNode);
                groupNode.element.addStyleClass("row-group");
                this._populateNode(groupNode, item.cookies);
                groupNode.expand();
            } else
                this._populateNode(this._dataGrid, item.cookies);
        }
    },

    _populateNode: function(parentNode, cookies)
    {
        var selectedCookie = this.selectedCookie;
        parentNode.removeChildren();
        if (!cookies)
            return;

        this._sortCookies(cookies);
        for (var i = 0; i < cookies.length; ++i) {
            var cookieNode = this._createGridNode(cookies[i]);
            parentNode.appendChild(cookieNode);
            if (selectedCookie === cookies[i])
                cookieNode.selected = true;
        }
    },

    _totalSize: function(cookies)
    {
        var totalSize = 0;
        for (var i = 0; cookies && i < cookies.length; ++i)
            totalSize += cookies[i].size;
        return totalSize;
    },

    _sortCookies: function(cookies)
    {
        var sortDirection = this._dataGrid.sortOrder === "ascending" ? 1 : -1;

        function localeCompare(field, cookie1, cookie2)
        {
            return sortDirection * (cookie1[field] + "").localeCompare(cookie2[field] + "")
        }

        function numberCompare(field, cookie1, cookie2)
        {
            return sortDirection * (cookie1[field] - cookie2[field]);
        }

        function expiresCompare(cookie1, cookie2)
        {
            if (cookie1.session !== cookie2.session)
                return sortDirection * (cookie1.session ? 1 : -1);

            if (cookie1.session)
                return 0;

            return sortDirection * (cookie1.expires - cookie2.expires);
        }

        var comparator;
        switch (parseInt(this._dataGrid.sortColumnIdentifier)) {
            case 0: comparator = localeCompare.bind(this, "name"); break;
            case 1: comparator = localeCompare.bind(this, "value"); break;
            case 2: comparator = localeCompare.bind(this, "domain"); break;
            case 3: comparator = localeCompare.bind(this, "path"); break;
            case 4: comparator = expiresCompare; break;
            case 5: comparator = numberCompare.bind(this, "size"); break;
            case 6: comparator = localeCompare.bind(this, "httpOnly"); break;
            case 7: comparator = localeCompare.bind(this, "secure"); break;
            default: localeCompare.bind(this, "name");
        }

        cookies.sort(comparator);
    },

    _createGridNode: function(cookie)
    {
        var data = {};
        data[0] = cookie.name;
        data[1] = cookie.value;
        data[2] = cookie.domain || "";
        data[3] = cookie.path || "";
        data[4] = cookie.type === WebInspector.Cookie.Type.Request ? "" :
            (cookie.session ? WebInspector.UIString("Session") : new Date(cookie.expires).toGMTString());
        data[5] = cookie.size;
        var checkmark = "\u2713";
        data[6] = (cookie.httpOnly ? checkmark : "");
        data[7] = (cookie.secure ? checkmark : "");

        var node = new WebInspector.DataGridNode(data);
        node.cookie = cookie;
        node.selectable = true;
        return node;
    },

    _onDeleteFromGrid: function(node)
    {
        this._deleteCallback(node.cookie);
    }
}
