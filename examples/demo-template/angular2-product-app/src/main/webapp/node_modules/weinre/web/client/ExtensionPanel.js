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

WebInspector.ExtensionPanel = function(id, label, iconURL, options)
{
    this.toolbarItemLabel = label;
    this._addStyleRule(".toolbar-item." + id + " .toolbar-icon", "background-image: url(" + iconURL + ");");
    WebInspector.Panel.call(this, id);
}

WebInspector.ExtensionPanel.prototype = {
    get defaultFocusedElement()
    {
        return this.sidebarTreeElement || this.element;
    },

    updateMainViewWidth: function(width)
    {
        this.bodyElement.style.left = width + "px";
        this.resize();
    },

    searchCanceled: function(startingNewSearch)
    {
        WebInspector.extensionServer.notifySearchAction(this._id, "cancelSearch");
        WebInspector.Panel.prototype.searchCanceled.apply(this, arguments);
    },

    performSearch: function(query)
    {
        WebInspector.extensionServer.notifySearchAction(this._id, "performSearch", query);
        WebInspector.Panel.prototype.performSearch.apply(this, arguments);
    },

    jumpToNextSearchResult: function()
    {
        WebInspector.extensionServer.notifySearchAction(this._id, "nextSearchResult");
        WebInspector.Panel.prototype.jumpToNextSearchResult.call(this);
    },

    jumpToPreviousSearchResult: function()
    {
        WebInspector.extensionServer.notifySearchAction(this._id, "previousSearchResult");
        WebInspector.Panel.prototype.jumpToPreviousSearchResult.call(this);
    },

    _addStyleRule: function(selector, body)
    {
        var style = document.createElement("style");
        style.textContent = selector + " { " + body + " }";
        document.head.appendChild(style);
    }
}

WebInspector.ExtensionPanel.prototype.__proto__ = WebInspector.Panel.prototype;

WebInspector.ExtensionWatchSidebarPane = function(title, id)
{
    WebInspector.SidebarPane.call(this, title);
    this._id = id;
}

WebInspector.ExtensionWatchSidebarPane.prototype = {
    setObject: function(object, title)
    {
        this._setObject(WebInspector.RemoteObject.fromLocalObject(object), title);
    },

    setExpression: function(expression, title)
    {
        InspectorBackend.evaluate(expression, "extension-watch", false, this._onEvaluate.bind(this, title));
    },

    _onEvaluate: function(title, result)
    {
        this._setObject(WebInspector.RemoteObject.fromPayload(result), title);
    },

    _setObject: function(object, title)
    {
        this.bodyElement.removeChildren();
        var section = new WebInspector.ObjectPropertiesSection(object, title, null, true);
        if (!title)
            section.headerElement.addStyleClass("hidden");
        section.expanded = true;
        this.bodyElement.appendChild(section.element);
        WebInspector.extensionServer.notifyExtensionWatchSidebarUpdated(this._id);
    }
}

WebInspector.ExtensionWatchSidebarPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;
