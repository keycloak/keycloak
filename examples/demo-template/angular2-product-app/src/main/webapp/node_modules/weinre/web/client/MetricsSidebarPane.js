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

WebInspector.MetricsSidebarPane = function()
{
    WebInspector.SidebarPane.call(this, WebInspector.UIString("Metrics"));
    this._inlineStyleId = null;
}

WebInspector.MetricsSidebarPane.prototype = {
    update: function(node)
    {
        if (node)
            this.node = node;
        else
            node = this.node;

        if (!node || !node.ownerDocument.defaultView || node.nodeType !== Node.ELEMENT_NODE) {
            this.bodyElement.removeChildren();
            return;
        }

        var self = this;
        var callback = function(style) {
            if (!style)
                return;
            self._update(style);
        };
        WebInspector.cssModel.getComputedStyleAsync(node.id, callback);

        var inlineStyleCallback = function(style) {
            if (!style)
                return;
            self.inlineStyle = style;
        };
        WebInspector.cssModel.getInlineStyleAsync(node.id, inlineStyleCallback);
    },

    _update: function(style)
    {
        // Updating with computed style.
        var metricsElement = document.createElement("div");
        metricsElement.className = "metrics";

        function createBoxPartElement(style, name, side, suffix)
        {
            var propertyName = (name !== "position" ? name + "-" : "") + side + suffix;
            var value = style.getPropertyValue(propertyName);
            if (value === "" || (name !== "position" && value === "0px"))
                value = "\u2012";
            else if (name === "position" && value === "auto")
                value = "\u2012";
            value = value.replace(/px$/, "");

            var element = document.createElement("div");
            element.className = side;
            element.textContent = value;
            element.addEventListener("dblclick", this.startEditing.bind(this, element, name, propertyName), false);
            return element;
        }

        // Display types for which margin is ignored.
        var noMarginDisplayType = {
            "table-cell": true,
            "table-column": true,
            "table-column-group": true,
            "table-footer-group": true,
            "table-header-group": true,
            "table-row": true,
            "table-row-group": true
        };

        // Display types for which padding is ignored.
        var noPaddingDisplayType = {
            "table-column": true,
            "table-column-group": true,
            "table-footer-group": true,
            "table-header-group": true,
            "table-row": true,
            "table-row-group": true
        };

        // Position types for which top, left, bottom and right are ignored.
        var noPositionType = {
            "static": true
        };

        var boxes = ["content", "padding", "border", "margin", "position"];
        var boxLabels = [WebInspector.UIString("content"), WebInspector.UIString("padding"), WebInspector.UIString("border"), WebInspector.UIString("margin"), WebInspector.UIString("position")];
        var previousBox;
        for (var i = 0; i < boxes.length; ++i) {
            var name = boxes[i];

            if (name === "margin" && noMarginDisplayType[style.getPropertyValue("display")])
                continue;
            if (name === "padding" && noPaddingDisplayType[style.getPropertyValue("display")])
                continue;
            if (name === "position" && noPositionType[style.getPropertyValue("position")])
                continue;

            var boxElement = document.createElement("div");
            boxElement.className = name;

            if (name === "content") {
                var width = style.getPropertyValue("width").replace(/px$/, "");
                var widthElement = document.createElement("span");
                widthElement.textContent = width;
                widthElement.addEventListener("dblclick", this.startEditing.bind(this, widthElement, "width", "width"), false);

                var height = style.getPropertyValue("height").replace(/px$/, "");
                var heightElement = document.createElement("span");
                heightElement.textContent = height;
                heightElement.addEventListener("dblclick", this.startEditing.bind(this, heightElement, "height", "height"), false);

                boxElement.appendChild(widthElement);
                boxElement.appendChild(document.createTextNode(" \u00D7 "));
                boxElement.appendChild(heightElement);
            } else {
                var suffix = (name === "border" ? "-width" : "");

                var labelElement = document.createElement("div");
                labelElement.className = "label";
                labelElement.textContent = boxLabels[i];
                boxElement.appendChild(labelElement);

                boxElement.appendChild(createBoxPartElement.call(this, style, name, "top", suffix));
                boxElement.appendChild(document.createElement("br"));
                boxElement.appendChild(createBoxPartElement.call(this, style, name, "left", suffix));

                if (previousBox)
                    boxElement.appendChild(previousBox);

                boxElement.appendChild(createBoxPartElement.call(this, style, name, "right", suffix));
                boxElement.appendChild(document.createElement("br"));
                boxElement.appendChild(createBoxPartElement.call(this, style, name, "bottom", suffix));
            }

            previousBox = boxElement;
        }

        metricsElement.appendChild(previousBox);
        this.bodyElement.removeChildren();
        this.bodyElement.appendChild(metricsElement);
    },

    startEditing: function(targetElement, box, styleProperty)
    {
        if (WebInspector.isBeingEdited(targetElement))
            return;

        var context = { box: box, styleProperty: styleProperty };

        WebInspector.startEditing(targetElement, {
            context: context,
            commitHandler: this.editingCommitted.bind(this),
            cancelHandler: this.editingCancelled.bind(this)
        });
    },

    editingCancelled: function(element, context)
    {
        this.update();
    },

    editingCommitted: function(element, userInput, previousContent, context)
    {
        if (!this.inlineStyle) {
            // Element has no renderer.
            return this.editingCancelled(element, context); // nothing changed, so cancel
        }

        if (userInput === previousContent)
            return this.editingCancelled(element, context); // nothing changed, so cancel

        if (context.box !== "position" && (!userInput || userInput === "\u2012"))
            userInput = "0px";
        else if (context.box === "position" && (!userInput || userInput === "\u2012"))
            userInput = "auto";

        // Append a "px" unit if the user input was just a number.
        if (/^\d+$/.test(userInput))
            userInput += "px";

        var self = this;
        var callback = function(style) {
            if (!style)
                return;
            self.inlineStyle = style;
            self.dispatchEventToListeners("metrics edited");
            self.update();
        };

        function setEnabledValueCallback(context, style)
        {
            var property = style.getLiveProperty(context.styleProperty);
            if (!property)
                style.appendProperty(context.styleProperty, userInput, callback);
             else
                property.setValue(userInput, callback);
        }

        var allProperties = this.inlineStyle.allProperties;
        for (var i = 0; i < allProperties.length; ++i) {
            var property = allProperties[i];
            if (property.name !== context.styleProperty || property.inactive)
                continue;
            if (property.disabled)
                property.setDisabled(false, setEnabledValueCallback.bind(null, context));
            else
                property.setValue(userInput, callback);
            return;
        }

        this.inlineStyle.appendProperty(context.styleProperty, userInput, callback);
    }
}

WebInspector.MetricsSidebarPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;
