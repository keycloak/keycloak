/*
 * Copyright (C) 2009 Apple Inc.  All rights reserved.
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

WebInspector.CookieItemsView = function(treeElement, cookieDomain)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("storage-view");

    this._deleteButton = new WebInspector.StatusBarButton(WebInspector.UIString("Delete"), "delete-storage-status-bar-item");
    this._deleteButton.visible = false;
    this._deleteButton.addEventListener("click", this._deleteButtonClicked.bind(this), false);

    this._refreshButton = new WebInspector.StatusBarButton(WebInspector.UIString("Refresh"), "refresh-storage-status-bar-item");
    this._refreshButton.addEventListener("click", this._refreshButtonClicked.bind(this), false);

    this._treeElement = treeElement;
    this._cookieDomain = cookieDomain;

    this._emptyMsgElement = document.createElement("div");
    this._emptyMsgElement.className = "storage-empty-view";
    this._emptyMsgElement.textContent = WebInspector.UIString("This site has no cookies.");
    this.element.appendChild(this._emptyMsgElement);
}

WebInspector.CookieItemsView.prototype = {
    get statusBarItems()
    {
        return [this._refreshButton.element, this._deleteButton.element];
    },

    show: function(parentElement)
    {
        WebInspector.View.prototype.show.call(this, parentElement);
        this._update();
    },

    hide: function()
    {
        WebInspector.View.prototype.hide.call(this);
        this._deleteButton.visible = false;
    },

    resize: function()
    {
        if (this._cookiesTable)
            this._cookiesTable.updateWidths();
    },

    _update: function()
    {
        WebInspector.Cookies.getCookiesAsync(this._updateWithCookies.bind(this));
    },

    _updateWithCookies: function(allCookies, isAdvanced)
    {
        this._cookies = isAdvanced ? this._filterCookiesForDomain(allCookies) : allCookies;

        if (!this._cookies.length) {
            // Nothing to show.
            this._emptyMsgElement.removeStyleClass("hidden");
            this._deleteButton.visible = false;
            if (this._cookiesTable)
                this._cookiesTable.element.addStyleClass("hidden");
            return;
        }

        if (!this._cookiesTable) {
            this._cookiesTable = isAdvanced ? new WebInspector.CookiesTable(this._cookieDomain, false, this._deleteCookie.bind(this)) : new WebInspector.SimpleCookiesTable();
            this.element.appendChild(this._cookiesTable.element);
        }

        this._cookiesTable.setCookies(this._cookies);
        this._cookiesTable.element.removeStyleClass("hidden");
        this._emptyMsgElement.addStyleClass("hidden");
        if (isAdvanced) {
            this._treeElement.subtitle = String.sprintf(WebInspector.UIString("%d cookies (%s)"), this._cookies.length,
                Number.bytesToString(this._totalSize));
            this._deleteButton.visible = true;
        }
        this._cookiesTable.updateWidths();
    },

    _filterCookiesForDomain: function(allCookies)
    {
        var cookies = [];
        var resourceURLsForDocumentURL = [];
        this._totalSize = 0;

        function populateResourcesForDocuments(resource)
        {
            var url = resource.documentURL.asParsedURL();
            if (url && url.host == this._cookieDomain)
                resourceURLsForDocumentURL.push(resource.url);
        }
        WebInspector.forAllResources(populateResourcesForDocuments.bind(this));

        for (var i = 0; i < allCookies.length; ++i) {
            var pushed = false;
            var size = allCookies[i].size;
            for (var j = 0; j < resourceURLsForDocumentURL.length; ++j) {
                var resourceURL = resourceURLsForDocumentURL[j];
                if (WebInspector.Cookies.cookieMatchesResourceURL(allCookies[i], resourceURL)) {
                    this._totalSize += size;
                    if (!pushed) {
                        pushed = true;
                        cookies.push(allCookies[i]);
                    }
                }
            }
        }
        return cookies;
    },

    _deleteCookie: function(cookie)
    {
        InspectorBackend.deleteCookie(cookie.name, this._cookieDomain);
        this._update();
    },

    _deleteButtonClicked: function()
    {
        if (this._cookiesTable.selectedCookie)
            this._deleteCookie(this._cookiesTable.selectedCookie);
    },

    _refreshButtonClicked: function(event)
    {
        this._update();
    }
}

WebInspector.CookieItemsView.prototype.__proto__ = WebInspector.View.prototype;

WebInspector.SimpleCookiesTable = function()
{
    this.element = document.createElement("div");
    var columns = {};
    columns[0] = {};
    columns[1] = {};
    columns[0].title = WebInspector.UIString("Name");
    columns[1].title = WebInspector.UIString("Value");

    this._dataGrid = new WebInspector.DataGrid(columns);
    this._dataGrid.autoSizeColumns(20, 80);
    this.element.appendChild(this._dataGrid.element);
    this._dataGrid.updateWidths();
}

WebInspector.SimpleCookiesTable.prototype = {
    setCookies: function(cookies)
    {
        this._dataGrid.removeChildren();
        var addedCookies = {};
        for (var i = 0; i < cookies.length; ++i) {
            if (addedCookies[cookies[i].name])
                continue;
            addedCookies[cookies[i].name] = true;
            var data = {};
            data[0] = cookies[i].name;
            data[1] = cookies[i].value;

            var node = new WebInspector.DataGridNode(data, false);
            node.selectable = true;
            this._dataGrid.appendChild(node);
        }
        this._dataGrid.children[0].selected = true;
    },

    resize: function()
    {
        if (this._dataGrid)
            this._dataGrid.updateWidths();
    }
}
