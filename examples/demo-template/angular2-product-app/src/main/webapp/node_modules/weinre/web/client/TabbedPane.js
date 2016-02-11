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

WebInspector.TabbedPane = function(element)
{
    this.element = element || document.createElement("div");
    this.element.addStyleClass("tabbed-pane");
    this._tabsElement = this.element.createChild("div", "tabbed-pane-header");
    this._contentElement = this.element.createChild("div", "tabbed-pane-content");
    this._tabs = {};
}

WebInspector.TabbedPane.prototype = {
    appendTab: function(id, tabTitle, view)
    {
        var tabElement = document.createElement("li");
        tabElement.textContent = tabTitle;
        tabElement.addEventListener("click", this.selectTab.bind(this, id, true), false);

        this._tabsElement.appendChild(tabElement);
        this._contentElement.appendChild(view.element);

        this._tabs[id] = { tabElement: tabElement, view: view }
    },

    selectTab: function(id, userGesture)
    {
        if (!(id in this._tabs))
            return false;

        if (this._currentTab) {
            this._hideTab(this._currentTab)
            delete this._currentTab;
        }

        var tab = this._tabs[id];
        this._showTab(tab);
        this._currentTab = tab;
        if (userGesture) {
            var event = {tabId: id};
            this.dispatchEventToListeners("tab-selected", event);
        }
        return true;
    },

    _showTab: function(tab)
    {
        tab.tabElement.addStyleClass("selected");
        tab.view.show(this._contentElement);
    },

    _hideTab: function(tab)
    {
        tab.tabElement.removeStyleClass("selected");
        tab.view.visible = false;
    }
}

WebInspector.TabbedPane.prototype.__proto__ = WebInspector.Object.prototype;
