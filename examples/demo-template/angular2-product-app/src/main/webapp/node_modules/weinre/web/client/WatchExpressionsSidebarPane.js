/*
 * Copyright (C) IBM Corp. 2009  All rights reserved.
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
 *     * Neither the name of IBM Corp. nor the names of its
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

WebInspector.WatchExpressionsSidebarPane = function()
{
    WebInspector.SidebarPane.call(this, WebInspector.UIString("Watch Expressions"));
    this.reset();
}

WebInspector.WatchExpressionsSidebarPane.prototype = {
    reset: function()
    {
        this.bodyElement.removeChildren();

        this.expanded = WebInspector.settings.watchExpressions.length > 0;
        this.section = new WebInspector.WatchExpressionsSection();
        this.bodyElement.appendChild(this.section.element);

        var addElement = document.createElement("button");
        addElement.setAttribute("type", "button");
        addElement.textContent = WebInspector.UIString("Add");
        addElement.addEventListener("click", this.section.addExpression.bind(this.section), false);

        var refreshElement = document.createElement("button");
        refreshElement.setAttribute("type", "button");
        refreshElement.textContent = WebInspector.UIString("Refresh");
        refreshElement.addEventListener("click", this.section.update.bind(this.section), false);

        var centerElement = document.createElement("div");
        centerElement.addStyleClass("watch-expressions-buttons-container");
        centerElement.appendChild(addElement);
        centerElement.appendChild(refreshElement);
        this.bodyElement.appendChild(centerElement);

        this.onexpand = this.refreshExpressions.bind(this);
    },

    refreshExpressions: function()
    {
        if (this.section)
            this.section.update();
    }
}

WebInspector.WatchExpressionsSidebarPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;

WebInspector.WatchExpressionsSection = function()
{
    this._watchObjectGroupId = "watch-group";

    WebInspector.ObjectPropertiesSection.call(this);

    this.watchExpressions = WebInspector.settings.watchExpressions;

    this.headerElement.className = "hidden";
    this.editable = true;
    this.expanded = true;
    this.propertiesElement.addStyleClass("watch-expressions");
}

WebInspector.WatchExpressionsSection.NewWatchExpression = "\xA0";

WebInspector.WatchExpressionsSection.prototype = {
    update: function()
    {
        function appendResult(expression, watchIndex, result)
        {
            var property = new WebInspector.RemoteObjectProperty(expression, result);
            property.watchIndex = watchIndex;

            // To clarify what's going on here: 
            // In the outer function, we calculate the number of properties
            // that we're going to be updating, and set that in the
            // propertyCount variable.  
            // In this function, we test to see when we are processing the 
            // last property, and then call the superclass's updateProperties() 
            // method to get all the properties refreshed at once.
            properties.push(property);
            
            if (properties.length == propertyCount) {
                this.updateProperties(properties, WebInspector.WatchExpressionTreeElement, WebInspector.WatchExpressionsSection.CompareProperties);

                // check to see if we just added a new watch expression,
                // which will always be the last property
                if (this._newExpressionAdded) {
                    delete this._newExpressionAdded;

                    treeElement = this.findAddedTreeElement();
                    if (treeElement)
                        treeElement.startEditing();
                }
            }
        }

        // TODO: pass exact injected script id.
        InspectorBackend.releaseWrapperObjectGroup(0, this._watchObjectGroupId)
        var properties = [];

        // Count the properties, so we known when to call this.updateProperties()
        // in appendResult()
        var propertyCount = 0;
        for (var i = 0; i < this.watchExpressions.length; ++i) {
            if (!this.watchExpressions[i]) 
                continue;
            ++propertyCount;
        }

        // Now process all the expressions, since we have the actual count,
        // which is checked in the appendResult inner function.
        for (var i = 0; i < this.watchExpressions.length; ++i) {
            var expression = this.watchExpressions[i];
            if (!expression)
                continue;

            WebInspector.console.evalInInspectedWindow("(" + expression + ")", this._watchObjectGroupId, false, appendResult.bind(this, expression, i));
        }

        // note this is setting the expansion of the tree, not the section;
        // with no expressions, and expanded tree, we get some extra vertical
        // white space
        // FIXME: should change to use header buttons instead of the buttons
        // at the bottom of the section, then we can add a "No Watch Expressions
        // element when there are no watch expressions, and this issue should
        // go away.
        this.expanded = (propertyCount != 0);
    },

    addExpression: function()
    {
        this._newExpressionAdded = true;
        this.watchExpressions.push(WebInspector.WatchExpressionsSection.NewWatchExpression);
        this.update();
    },

    updateExpression: function(element, value)
    {
        this.watchExpressions[element.property.watchIndex] = value;
        this.saveExpressions();
        this.update();
    },

    findAddedTreeElement: function()
    {
        var children = this.propertiesTreeOutline.children;
        for (var i = 0; i < children.length; ++i)
            if (children[i].property.name === WebInspector.WatchExpressionsSection.NewWatchExpression)
                return children[i];
    },

    saveExpressions: function()
    {
        var toSave = [];
        for (var i = 0; i < this.watchExpressions.length; i++)
            if (this.watchExpressions[i])
                toSave.push(this.watchExpressions[i]);

        WebInspector.settings.watchExpressions = toSave;
        return toSave.length;
    }
}

WebInspector.WatchExpressionsSection.prototype.__proto__ = WebInspector.ObjectPropertiesSection.prototype;

WebInspector.WatchExpressionsSection.CompareProperties = function(propertyA, propertyB) 
{
    if (propertyA.watchIndex == propertyB.watchIndex)
        return 0;
    else if (propertyA.watchIndex < propertyB.watchIndex)
        return -1;
    else
        return 1;
}

WebInspector.WatchExpressionTreeElement = function(property)
{
    WebInspector.ObjectPropertyTreeElement.call(this, property);
}

WebInspector.WatchExpressionTreeElement.prototype = {
    update: function()
    {
        WebInspector.ObjectPropertyTreeElement.prototype.update.call(this);

        if (this.property.value.isError())
            this.valueElement.addStyleClass("watch-expressions-error-level");

        var deleteButton = document.createElement("input");
        deleteButton.type = "button";
        deleteButton.title = WebInspector.UIString("Delete watch expression.");
        deleteButton.addStyleClass("enabled-button");
        deleteButton.addStyleClass("delete-button");
        deleteButton.addEventListener("click", this._deleteButtonClicked.bind(this), false);

        this.listItemElement.insertBefore(deleteButton, this.listItemElement.firstChild);
    },

    _deleteButtonClicked: function()
    {
        this.treeOutline.section.updateExpression(this, null);
    },

    startEditing: function()
    {
        if (WebInspector.isBeingEdited(this.nameElement) || !this.treeOutline.section.editable)
            return;

        this.nameElement.textContent = this.property.name.trim();

        var context = { expanded: this.expanded };

        // collapse temporarily, if required
        this.hasChildren = false;

        this.listItemElement.addStyleClass("editing-sub-part");

        WebInspector.startEditing(this.nameElement, {
            context: context,
            commitHandler: this.editingCommitted.bind(this),
            cancelHandler: this.editingCancelled.bind(this)
        });
    },

    editingCancelled: function(element, context)
    {
        if (!this.nameElement.textContent)
            this.treeOutline.section.updateExpression(this, null);
            
        this.update();
        this.editingEnded(context);
    },

    applyExpression: function(expression, updateInterface)
    {
        expression = expression.trim();

        if (!expression)
            expression = null;

        this.property.name = expression;
        this.treeOutline.section.updateExpression(this, expression);
    }
}

WebInspector.WatchExpressionTreeElement.prototype.__proto__ = WebInspector.ObjectPropertyTreeElement.prototype;
