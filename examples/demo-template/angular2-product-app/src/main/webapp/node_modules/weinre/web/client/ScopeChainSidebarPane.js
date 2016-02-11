/*
 * Copyright (C) 2008 Apple Inc. All Rights Reserved.
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

WebInspector.ScopeChainSidebarPane = function()
{
    WebInspector.SidebarPane.call(this, WebInspector.UIString("Scope Variables"));
    this._sections = [];
    this._expandedSections = {};
    this._expandedProperties = [];
}

WebInspector.ScopeChainSidebarPane.prototype = {
    update: function(callFrame)
    {
        this.bodyElement.removeChildren();

        if (!callFrame) {
            var infoElement = document.createElement("div");
            infoElement.className = "info";
            infoElement.textContent = WebInspector.UIString("Not Paused");
            this.bodyElement.appendChild(infoElement);
            return;
        }

        for (var i = 0; i < this._sections.length; ++i) {
            var section = this._sections[i];
            if (!section.title)
                continue;
            if (section.expanded)
                this._expandedSections[section.title] = true;
            else
                delete this._expandedSections[section.title];
        }

        this._sections = [];

        var foundLocalScope = false;
        var scopeChain = callFrame.scopeChain;
        for (var i = 0; i < scopeChain.length; ++i) {
            var scopeObjectProxy = scopeChain[i];
            var title = null;
            var subtitle = scopeObjectProxy.description;
            var emptyPlaceholder = null;
            var extraProperties = null;

            if (scopeObjectProxy.isLocal) {
                foundLocalScope = true;
                title = WebInspector.UIString("Local");
                emptyPlaceholder = WebInspector.UIString("No Variables");
                subtitle = null;
                if (scopeObjectProxy.thisObject)
                    extraProperties = [ new WebInspector.RemoteObjectProperty("this", WebInspector.RemoteObject.fromPayload(scopeObjectProxy.thisObject)) ];
            } else if (scopeObjectProxy.isClosure) {
                title = WebInspector.UIString("Closure");
                emptyPlaceholder = WebInspector.UIString("No Variables");
                subtitle = null;
            } else if (i === (scopeChain.length - 1))
                title = WebInspector.UIString("Global");
            else if (scopeObjectProxy.isElement)
                title = WebInspector.UIString("Event Target");
            else if (scopeObjectProxy.isDocument)
                title = WebInspector.UIString("Event Document");
            else if (scopeObjectProxy.isWithBlock)
                title = WebInspector.UIString("With Block");

            if (!title || title === subtitle)
                subtitle = null;

            var section = new WebInspector.ObjectPropertiesSection(WebInspector.RemoteObject.fromPayload(scopeObjectProxy), title, subtitle, emptyPlaceholder, true, extraProperties, WebInspector.ScopeVariableTreeElement);
            section.editInSelectedCallFrameWhenPaused = true;
            section.pane = this;

            if (!foundLocalScope || scopeObjectProxy.isLocal || title in this._expandedSections)
                section.expanded = true;

            this._sections.push(section);
            this.bodyElement.appendChild(section.element);
        }
    }
}

WebInspector.ScopeChainSidebarPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;

WebInspector.ScopeVariableTreeElement = function(property)
{
    WebInspector.ObjectPropertyTreeElement.call(this, property);
}

WebInspector.ScopeVariableTreeElement.prototype = {
    onattach: function()
    {
        WebInspector.ObjectPropertyTreeElement.prototype.onattach.call(this);
        if (this.hasChildren && this.propertyIdentifier in this.treeOutline.section.pane._expandedProperties)
            this.expand();
    },

    onexpand: function()
    {
        this.treeOutline.section.pane._expandedProperties[this.propertyIdentifier] = true;
    },

    oncollapse: function()
    {
        delete this.treeOutline.section.pane._expandedProperties[this.propertyIdentifier];
    },

    get propertyIdentifier()
    {
        if ("_propertyIdentifier" in this)
            return this._propertyIdentifier;
        var section = this.treeOutline.section;
        this._propertyIdentifier = section.title + ":" + (section.subtitle ? section.subtitle + ":" : "") + this.propertyPath;
        return this._propertyIdentifier;
    },

    get propertyPath()
    {
        if ("_propertyPath" in this)
            return this._propertyPath;

        var current = this;
        var result;

        do {
            if (result)
                result = current.property.name + "." + result;
            else
                result = current.property.name;
            current = current.parent;
        } while (current && !current.root);

        this._propertyPath = result;
        return result;
    }
}

WebInspector.ScopeVariableTreeElement.prototype.__proto__ = WebInspector.ObjectPropertyTreeElement.prototype;
