/*
 * Copyright (C) 2008 Apple Inc. All Rights Reserved.
 * Copyright (C) 2009 Joseph Pecoraro
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

WebInspector.ObjectPropertiesSection = function(object, title, subtitle, emptyPlaceholder, ignoreHasOwnProperty, extraProperties, treeElementConstructor)
{
    this.emptyPlaceholder = (emptyPlaceholder || WebInspector.UIString("No Properties"));
    this.object = object;
    this.ignoreHasOwnProperty = ignoreHasOwnProperty;
    this.extraProperties = extraProperties;
    this.treeElementConstructor = treeElementConstructor || WebInspector.ObjectPropertyTreeElement;
    this.editable = true;

    WebInspector.PropertiesSection.call(this, title, subtitle);
}

WebInspector.ObjectPropertiesSection.prototype = {
    onpopulate: function()
    {
        this.update();
    },

    update: function()
    {
        var self = this;
        var callback = function(properties) {
            if (!properties)
                return;
            self.updateProperties(properties);
        };
        this.object.getProperties(this.ignoreHasOwnProperty, true, callback);
    },

    updateProperties: function(properties, rootTreeElementConstructor, rootPropertyComparer)
    {
        if (!rootTreeElementConstructor)
            rootTreeElementConstructor = this.treeElementConstructor;
            
        if (!rootPropertyComparer)
            rootPropertyComparer = WebInspector.ObjectPropertiesSection.CompareProperties;
            
        if (this.extraProperties)
            for (var i = 0; i < this.extraProperties.length; ++i)
                properties.push(this.extraProperties[i]);
                
        properties.sort(rootPropertyComparer);

        this.propertiesTreeOutline.removeChildren();

        for (var i = 0; i < properties.length; ++i) {
            properties[i].parentObject = this.object;
            this.propertiesTreeOutline.appendChild(new rootTreeElementConstructor(properties[i]));
        }

        if (!this.propertiesTreeOutline.children.length) {
            var title = "<div class=\"info\">" + this.emptyPlaceholder + "</div>";
            var infoElement = new TreeElement(null, null, false);
            infoElement.titleHTML = title;
            this.propertiesTreeOutline.appendChild(infoElement);
        }
        this.propertiesForTest = properties;
    }
}

WebInspector.ObjectPropertiesSection.prototype.__proto__ = WebInspector.PropertiesSection.prototype;

WebInspector.ObjectPropertiesSection.CompareProperties = function(propertyA, propertyB) 
{
    var a = propertyA.name;
    var b = propertyB.name;
    if (a === "__proto__")
        return 1;
    if (b === "__proto__")
        return -1;

    // if used elsewhere make sure to
    //  - convert a and b to strings (not needed here, properties are all strings)
    //  - check if a == b (not needed here, no two properties can be the same)

    var diff = 0;
    var chunk = /^\d+|^\D+/;
    var chunka, chunkb, anum, bnum;
    while (diff === 0) {
        if (!a && b)
            return -1;
        if (!b && a)
            return 1;
        chunka = a.match(chunk)[0];
        chunkb = b.match(chunk)[0];
        anum = !isNaN(chunka);
        bnum = !isNaN(chunkb);
        if (anum && !bnum)
            return -1;
        if (bnum && !anum)
            return 1;
        if (anum && bnum) {
            diff = chunka - chunkb;
            if (diff === 0 && chunka.length !== chunkb.length) {
                if (!+chunka && !+chunkb) // chunks are strings of all 0s (special case)
                    return chunka.length - chunkb.length;
                else
                    return chunkb.length - chunka.length;
            }
        } else if (chunka !== chunkb)
            return (chunka < chunkb) ? -1 : 1;
        a = a.substring(chunka.length);
        b = b.substring(chunkb.length);
    }
    return diff;
}

WebInspector.ObjectPropertyTreeElement = function(property)
{
    this.property = property;

    // Pass an empty title, the title gets made later in onattach.
    TreeElement.call(this, "", null, false);
}

WebInspector.ObjectPropertyTreeElement.prototype = {
    onpopulate: function()
    {
        if (this.children.length && !this.shouldRefreshChildren)
            return;

        var callback = function(properties) {
            this.removeChildren();
            if (!properties)
                return;

            properties.sort(WebInspector.ObjectPropertiesSection.CompareProperties);
            for (var i = 0; i < properties.length; ++i) {
                this.appendChild(new this.treeOutline.section.treeElementConstructor(properties[i]));
            }
        };
        this.property.value.getOwnProperties(true, callback.bind(this));
    },

    ondblclick: function(event)
    {
        this.startEditing();
    },

    onattach: function()
    {
        this.update();
    },

    update: function()
    {
        this.nameElement = document.createElement("span");
        this.nameElement.className = "name";
        this.nameElement.textContent = this.property.name;

        var separatorElement = document.createElement("span");
        separatorElement.className = "separator";
        separatorElement.textContent = ": ";
        
        this.valueElement = document.createElement("span");
        this.valueElement.className = "value";

        var description = this.property.value.description;
        // Render \n as a nice unicode cr symbol.
        if (this.property.value.type === "string" && typeof description === "string")
            description = description.replace(/\n/g, "\u21B5");
        this.valueElement.textContent = description;

        if (this.property.isGetter)
            this.valueElement.addStyleClass("dimmed");
        if (this.property.value.isError())
            this.valueElement.addStyleClass("error");
        if (this.property.value.type)
            this.valueElement.addStyleClass("console-formatted-" + this.property.value.type);
        if (this.property.value.type === "node")
            this.valueElement.addEventListener("contextmenu", this._contextMenuEventFired.bind(this), false);

        this.listItemElement.removeChildren();

        this.listItemElement.appendChild(this.nameElement);
        this.listItemElement.appendChild(separatorElement);
        this.listItemElement.appendChild(this.valueElement);
        this.hasChildren = this.property.value.hasChildren;
    },

    _contextMenuEventFired: function()
    {
        function selectNode(nodeId)
        {
            if (nodeId) {
                WebInspector.panels.elements.switchToAndFocus(WebInspector.domAgent.nodeForId(nodeId));
            }
        }

        function revealElement()
        {
            this.property.value.pushNodeToFrontend(selectNode);
        }

        var contextMenu = new WebInspector.ContextMenu();
        contextMenu.appendItem(WebInspector.UIString("Reveal in Elements Panel"), revealElement.bind(this));
        contextMenu.show(event);
    },

    updateSiblings: function()
    {
        if (this.parent.root)
            this.treeOutline.section.update();
        else
            this.parent.shouldRefreshChildren = true;
    },

    startEditing: function()
    {
        if (WebInspector.isBeingEdited(this.valueElement) || !this.treeOutline.section.editable)
            return;

        var context = { expanded: this.expanded };

        // Lie about our children to prevent expanding on double click and to collapse subproperties.
        this.hasChildren = false;

        this.listItemElement.addStyleClass("editing-sub-part");

        WebInspector.startEditing(this.valueElement, {
            context: context,
            commitHandler: this.editingCommitted.bind(this),
            cancelHandler: this.editingCancelled.bind(this)
        });
    },

    editingEnded: function(context)
    {
        this.listItemElement.scrollLeft = 0;
        this.listItemElement.removeStyleClass("editing-sub-part");
        if (context.expanded)
            this.expand();
    },

    editingCancelled: function(element, context)
    {
        this.update();
        this.editingEnded(context);
    },

    editingCommitted: function(element, userInput, previousContent, context)
    {
        if (userInput === previousContent)
            return this.editingCancelled(element, context); // nothing changed, so cancel

        this.applyExpression(userInput, true);

        this.editingEnded(context);
    },

    applyExpression: function(expression, updateInterface)
    {
        expression = expression.trim();
        var expressionLength = expression.length;
        var self = this;
        var callback = function(success) {
            if (!updateInterface)
                return;

            if (!success)
                self.update();

            if (!expressionLength) {
                // The property was deleted, so remove this tree element.
                self.parent.removeChild(this);
            } else {
                // Call updateSiblings since their value might be based on the value that just changed.
                self.updateSiblings();
            }
        };
        this.property.parentObject.setPropertyValue(this.property.name, expression.trim(), callback);
    }
}

WebInspector.ObjectPropertyTreeElement.prototype.__proto__ = TreeElement.prototype;
