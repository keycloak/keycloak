/*
 * Copyright (C) 2007 Apple Inc.  All rights reserved.
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

WebInspector.SidebarPane = function(title)
{
    this.element = document.createElement("div");
    this.element.className = "pane";

    this.titleElement = document.createElement("div");
    this.titleElement.className = "title";
    this.titleElement.tabIndex = 0;
    this.titleElement.addEventListener("click", this.toggleExpanded.bind(this), false);
    this.titleElement.addEventListener("keydown", this._onTitleKeyDown.bind(this), false);

    this.bodyElement = document.createElement("div");
    this.bodyElement.className = "body";

    this.element.appendChild(this.titleElement);
    this.element.appendChild(this.bodyElement);

    this.title = title;
    this.growbarVisible = false;
    this.expanded = false;
}

WebInspector.SidebarPane.prototype = {
    get title()
    {
        return this._title;
    },

    set title(x)
    {
        if (this._title === x)
            return;
        this._title = x;
        this.titleElement.textContent = x;
    },

    get growbarVisible()
    {
        return this._growbarVisible;
    },

    set growbarVisible(x)
    {
        if (this._growbarVisible === x)
            return;

        this._growbarVisible = x;

        if (x && !this._growbarElement) {
            this._growbarElement = document.createElement("div");
            this._growbarElement.className = "growbar";
            this.element.appendChild(this._growbarElement);
        } else if (!x && this._growbarElement) {
            if (this._growbarElement.parentNode)
                this._growbarElement.parentNode(this._growbarElement);
            delete this._growbarElement;
        }
    },

    get expanded()
    {
        return this._expanded;
    },

    set expanded(x)
    {
        if (x)
            this.expand();
        else
            this.collapse();
    },

    expand: function()
    {
        if (this._expanded)
            return;
        this._expanded = true;
        this.element.addStyleClass("expanded");
        if (this.onexpand)
            this.onexpand(this);
    },

    collapse: function()
    {
        if (!this._expanded)
            return;
        this._expanded = false;
        this.element.removeStyleClass("expanded");
        if (this.oncollapse)
            this.oncollapse(this);
    },

    toggleExpanded: function()
    {
        this.expanded = !this.expanded;
    },

    _onTitleKeyDown: function(event)
    {
        if (isEnterKey(event) || event.keyCode === WebInspector.KeyboardShortcut.Keys.Space.code)
            this.toggleExpanded();
    }
}

WebInspector.SidebarPane.prototype.__proto__ = WebInspector.Object.prototype;
