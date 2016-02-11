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

WebInspector.CSSStyleModel = function()
{
}

WebInspector.CSSStyleModel.parseRuleArrayPayload = function(ruleArray)
{
    var result = [];
    for (var i = 0; i < ruleArray.length; ++i)
        result.push(WebInspector.CSSRule.parsePayload(ruleArray[i]));
    return result;
}

WebInspector.CSSStyleModel.prototype = {
    getStylesAsync: function(nodeId, userCallback)
    {
        function callback(userCallback, payload)
        {
            if (!payload) {
                if (userCallback)
                    userCallback(null);
                return;
            }

            var result = {};
            if ("inlineStyle" in payload)
                result.inlineStyle = WebInspector.CSSStyleDeclaration.parsePayload(payload.inlineStyle);

            result.computedStyle = WebInspector.CSSStyleDeclaration.parsePayload(payload.computedStyle);
            result.matchedCSSRules = WebInspector.CSSStyleModel.parseRuleArrayPayload(payload.matchedCSSRules);

            result.styleAttributes = {};
            for (var name in payload.styleAttributes)
                result.styleAttributes[name] = WebInspector.CSSStyleDeclaration.parsePayload(payload.styleAttributes[name]);

            result.pseudoElements = [];
            for (var i = 0; i < payload.pseudoElements.length; ++i) {
                var entryPayload = payload.pseudoElements[i];
                result.pseudoElements.push({ pseudoId: entryPayload.pseudoId, rules: WebInspector.CSSStyleModel.parseRuleArrayPayload(entryPayload.rules) });
            }

            result.inherited = [];
            for (var i = 0; i < payload.inherited.length; ++i) {
                var entryPayload = payload.inherited[i];
                var entry = {};
                if ("inlineStyle" in entryPayload)
                    entry.inlineStyle = WebInspector.CSSStyleDeclaration.parsePayload(entryPayload.inlineStyle);
                if ("matchedCSSRules" in entryPayload)
                    entry.matchedCSSRules = WebInspector.CSSStyleModel.parseRuleArrayPayload(entryPayload.matchedCSSRules);
                result.inherited.push(entry);
            }

            if (userCallback)
                userCallback(result);
        }

        InspectorBackend.getStylesForNode(nodeId, callback.bind(null, userCallback));
    },

    getComputedStyleAsync: function(nodeId, userCallback)
    {
        function callback(userCallback, stylePayload)
        {
            if (!stylePayload)
                userCallback(null);
            else
                userCallback(WebInspector.CSSStyleDeclaration.parsePayload(stylePayload));
        }

        InspectorBackend.getComputedStyleForNode(nodeId, callback.bind(null, userCallback));
    },

    getInlineStyleAsync: function(nodeId, userCallback)
    {
        function callback(userCallback, stylePayload)
        {
            if (!stylePayload)
                userCallback(null);
            else
                userCallback(WebInspector.CSSStyleDeclaration.parsePayload(stylePayload));
        }

        InspectorBackend.getInlineStyleForNode(nodeId, callback.bind(null, userCallback));
    },

    setRuleSelector: function(ruleId, nodeId, newSelector, successCallback, failureCallback)
    {
        function checkAffectsCallback(nodeId, successCallback, rulePayload, selectedNodeIds)
        {
            var doesAffectSelectedNode = (selectedNodeIds.indexOf(nodeId) >= 0);
            var rule = WebInspector.CSSRule.parsePayload(rulePayload);
            successCallback(rule, doesAffectSelectedNode);
            this._styleSheetChanged(rule.id.styleSheetId, true);
        }

        function callback(nodeId, successCallback, failureCallback, newSelector, rulePayload)
        {
            if (!rulePayload)
                failureCallback();
            else
                InspectorBackend.querySelectorAll(nodeId, newSelector, checkAffectsCallback.bind(this, nodeId, successCallback, rulePayload));
        }

        InspectorBackend.setRuleSelector(ruleId, newSelector, callback.bind(this, nodeId, successCallback, failureCallback));
    },

    addRule: function(nodeId, selector, successCallback, failureCallback)
    {
        function checkAffectsCallback(nodeId, successCallback, rulePayload, selectedNodeIds)
        {
            var doesAffectSelectedNode = (selectedNodeIds.indexOf(nodeId) >= 0);
            var rule = WebInspector.CSSRule.parsePayload(rulePayload);
            successCallback(rule, doesAffectSelectedNode);
            this._styleSheetChanged(rule.id.styleSheetId, true);
        }

        function callback(successCallback, failureCallback, selector, rulePayload)
        {
            if (!rulePayload) {
                // Invalid syntax for a selector
                failureCallback();
            } else
                InspectorBackend.querySelectorAll(nodeId, selector, checkAffectsCallback.bind(this, nodeId, successCallback, rulePayload));
        }

        InspectorBackend.addRule(nodeId, selector, callback.bind(this, successCallback, failureCallback, selector));
    },

    _styleSheetChanged: function(styleSheetId, majorChange)
    {
        if (!majorChange || !styleSheetId)
            return;

        function callback(href, content)
        {
            var resource = WebInspector.resourceForURL(href);
            if (resource && resource.type === WebInspector.Resource.Type.Stylesheet)
                resource.setContent(content, this._onRevert.bind(this, styleSheetId));
        }
        InspectorBackend.getStyleSheetText(styleSheetId, callback.bind(this));
    },

    _onRevert: function(styleSheetId, contentToRevertTo)
    {
        function callback(success)
        {
            this._styleSheetChanged(styleSheetId, true);
            this.dispatchEventToListeners("stylesheet changed");
        }
        InspectorBackend.setStyleSheetText(styleSheetId, contentToRevertTo, callback.bind(this));
    }
}

WebInspector.CSSStyleModel.prototype.__proto__ = WebInspector.Object.prototype;

WebInspector.CSSStyleDeclaration = function(payload)
{
    this.id = payload.styleId;
    this.properties = payload.properties;
    this._shorthandValues = payload.shorthandValues;
    this._livePropertyMap = {}; // LIVE properties (source-based or style-based) : { name -> CSSProperty }
    this._allProperties = []; // ALL properties: [ CSSProperty ]
    this._longhandProperties = {}; // shorthandName -> [ CSSProperty ]
    this.__disabledProperties = {}; // DISABLED properties: { index -> CSSProperty }
    var payloadPropertyCount = payload.cssProperties.length;

    var propertyIndex = 0;
    for (var i = 0; i < payloadPropertyCount; ++i) {
        var property = new WebInspector.CSSProperty.parsePayload(this, i, payload.cssProperties[i]);
        this._allProperties.push(property);
        if (property.disabled)
            this.__disabledProperties[i] = property;
        if (!property.active && !property.styleBased)
            continue;
        var name = property.name;
        this[propertyIndex] = name;
        this._livePropertyMap[name] = property;

        // Index longhand properties.
        if (property.shorthand) { // only for parsed
            var longhands = this._longhandProperties[property.shorthand];
            if (!longhands) {
                longhands = [];
                this._longhandProperties[property.shorthand] = longhands;
            }
            longhands.push(property);
        }
        ++propertyIndex;
    }
    this.length = propertyIndex;
    if ("cssText" in payload)
        this.cssText = payload.cssText;
}

WebInspector.CSSStyleDeclaration.parsePayload = function(payload)
{
    return new WebInspector.CSSStyleDeclaration(payload);
}

WebInspector.CSSStyleDeclaration.prototype = {
    get allProperties()
    {
        return this._allProperties;
    },

    getLiveProperty: function(name)
    {
        return this._livePropertyMap[name];
    },

    getPropertyValue: function(name)
    {
        var property = this._livePropertyMap[name];
        return property ? property.value : "";
    },

    getPropertyPriority: function(name)
    {
        var property = this._livePropertyMap[name];
        return property ? property.priority : "";
    },

    getPropertyShorthand: function(name)
    {
        var property = this._livePropertyMap[name];
        return property ? property.shorthand : "";
    },

    isPropertyImplicit: function(name)
    {
        var property = this._livePropertyMap[name];
        return property ? property.implicit : "";
    },

    styleTextWithShorthands: function()
    {
        var cssText = "";
        var foundProperties = {};
        for (var i = 0; i < this.length; ++i) {
            var individualProperty = this[i];
            var shorthandProperty = this.getPropertyShorthand(individualProperty);
            var propertyName = (shorthandProperty || individualProperty);

            if (propertyName in foundProperties)
                continue;

            if (shorthandProperty) {
                var value = this.getShorthandValue(shorthandProperty);
                var priority = this.getShorthandPriority(shorthandProperty);
            } else {
                var value = this.getPropertyValue(individualProperty);
                var priority = this.getPropertyPriority(individualProperty);
            }

            foundProperties[propertyName] = true;

            cssText += propertyName + ": " + value;
            if (priority)
                cssText += " !" + priority;
            cssText += "; ";
        }

        return cssText;
    },

    getLonghandProperties: function(name)
    {
        return this._longhandProperties[name] || [];
    },

    getShorthandValue: function(shorthandProperty)
    {
        var property = this.getLiveProperty(shorthandProperty);
        return property ? property.value : this._shorthandValues[shorthandProperty];
    },

    getShorthandPriority: function(shorthandProperty)
    {
        var priority = this.getPropertyPriority(shorthandProperty);
        if (priority)
            return priority;

        var longhands = this._longhandProperties[shorthandProperty];
        return longhands ? this.getPropertyPriority(longhands[0]) : null;
    },

    propertyAt: function(index)
    {
        return (index < this.allProperties.length) ? this.allProperties[index] : null;
    },

    pastLastSourcePropertyIndex: function()
    {
        for (var i = this.allProperties.length - 1; i >= 0; --i) {
            var property = this.allProperties[i];
            if (property.active || property.disabled)
                return i + 1;
        }
        return 0;
    },

    newBlankProperty: function()
    {
        return new WebInspector.CSSProperty(this, this.pastLastSourcePropertyIndex(), "", "", "", "active", true, false, false, "");
    },

    insertPropertyAt: function(index, name, value, userCallback)
    {
        function callback(userCallback, payload)
        {
            if (!userCallback)
                return;

            if (!payload)
                userCallback(null);
            else {
                userCallback(WebInspector.CSSStyleDeclaration.parsePayload(payload));
                WebInspector.cssModel._styleSheetChanged(this.id.styleSheetId, true);
            }
        }

        InspectorBackend.setPropertyText(this.id, index, name + ": " + value + ";", false, callback.bind(null, userCallback));
    },

    appendProperty: function(name, value, userCallback)
    {
        this.insertPropertyAt(this.allProperties.length, name, value, userCallback);
    }
}

WebInspector.CSSRule = function(payload)
{
    this.id = payload.ruleId;
    this.selectorText = payload.selectorText;
    this.sourceLine = payload.sourceLine;
    this.sourceURL = payload.sourceURL;
    this.origin = payload.origin;
    this.style = WebInspector.CSSStyleDeclaration.parsePayload(payload.style);
    this.style.parentRule = this;
    this.selectorRange = payload.selectorRange;
}

WebInspector.CSSRule.parsePayload = function(payload)
{
    return new WebInspector.CSSRule(payload);
}

WebInspector.CSSRule.prototype = {
    get isUserAgent()
    {
        return this.origin === "user-agent";
    },

    get isUser()
    {
        return this.origin === "user";
    },

    get isViaInspector()
    {
        return this.origin === "inspector";
    },

    get isRegular()
    {
        return this.origin === "";
    }
}

WebInspector.CSSProperty = function(ownerStyle, index, name, value, priority, status, parsedOk, implicit, shorthand, text)
{
    this.ownerStyle = ownerStyle;
    this.index = index;
    this.name = name;
    this.value = value;
    this.priority = priority;
    this.status = status;
    this.parsedOk = parsedOk;
    this.implicit = implicit;
    this.shorthand = shorthand;
    this.text = text;
}

WebInspector.CSSProperty.parsePayload = function(ownerStyle, index, payload)
{
    var result = new WebInspector.CSSProperty(
        ownerStyle, index, payload.name, payload.value, payload.priority, payload.status, payload.parsedOk, payload.implicit, payload.shorthandName, payload.text);
    return result;
}

WebInspector.CSSProperty.prototype = {
    get propertyText()
    {
        if (this.text !== undefined)
            return this.text;

        if (this.name === "")
            return "";
        return this.name + ": " + this.value + (this.priority ? " !" + this.priority : "") + ";";
    },

    get isLive()
    {
        return this.active || this.styleBased;
    },

    get active()
    {
        return this.status === "active";
    },

    get styleBased()
    {
        return this.status === "style";
    },

    get inactive()
    {
        return this.status === "inactive";
    },

    get disabled()
    {
        return this.status === "disabled";
    },

    // Replaces "propertyName: propertyValue [!important];" in the stylesheet by an arbitrary propertyText.
    setText: function(propertyText, majorChange, userCallback)
    {
        function enabledCallback(style)
        {
            if (style)
                WebInspector.cssModel._styleSheetChanged(style.id.styleSheetId, majorChange);
            if (userCallback)
                userCallback(style);
        }

        function callback(stylePayload)
        {
            if (stylePayload) {
                this.text = propertyText;
                var style = WebInspector.CSSStyleDeclaration.parsePayload(stylePayload);
                var newProperty = style.allProperties[this.index];

                if (newProperty && this.disabled && !propertyText.match(/^\s*$/)) {
                    newProperty.setDisabled(false, enabledCallback);
                    return;
                } else
                    WebInspector.cssModel._styleSheetChanged(style.id.styleSheetId, majorChange);
                if (userCallback)
                    userCallback(style);
            } else {
                if (userCallback)
                    userCallback(null);
            }
        }

        if (!this.ownerStyle)
            throw "No ownerStyle for property";

        // An index past all the properties adds a new property to the style.
        InspectorBackend.setPropertyText(this.ownerStyle.id, this.index, propertyText, this.index < this.ownerStyle.pastLastSourcePropertyIndex(), callback.bind(this));
    },

    setValue: function(newValue, userCallback)
    {
        var text = this.name + ": " + newValue + (this.priority ? " !" + this.priority : "") + ";"
        this.setText(text, userCallback);
    },

    setDisabled: function(disabled, userCallback)
    {
        if (!this.ownerStyle && userCallback)
            userCallback(null);
        if (disabled === this.disabled && userCallback)
            userCallback(this.ownerStyle);

        function callback(stylePayload)
        {
            if (!userCallback)
                return;
            if (!stylePayload)
                userCallback(null);
            else {
                var style = WebInspector.CSSStyleDeclaration.parsePayload(stylePayload);
                userCallback(style);
                WebInspector.cssModel._styleSheetChanged(this.ownerStyle.id.styleSheetId, false);
            }
        }

        InspectorBackend.toggleProperty(this.ownerStyle.id, this.index, disabled, callback.bind(this));
    }
}

WebInspector.CSSStyleSheet = function(payload)
{
    this.id = payload.styleSheetId;
    this.sourceURL = payload.sourceURL;
    this.title = payload.title;
    this.disabled = payload.disabled;
    this.rules = [];
    this.styles = {};
    for (var i = 0; i < payload.rules.length; ++i) {
        var rule = WebInspector.CSSRule.parsePayload(payload.rules[i]);
        this.rules.push(rule);
        if (rule.style)
            this.styles[rule.style.id] = rule.style;
    }
    if ("text" in payload)
        this._text = payload.text;
}

WebInspector.CSSStyleSheet.createForId = function(styleSheetId, userCallback)
{
    function callback(styleSheetPayload)
    {
        if (!styleSheetPayload)
            userCallback(null);
        else
            userCallback(new WebInspector.CSSStyleSheet(styleSheetPayload));
    }
    InspectorBackend.getStyleSheet(styleSheetId, callback.bind(this));
}

WebInspector.CSSStyleSheet.prototype = {
    getText: function()
    {
        return this._text;
    },

    setText: function(newText, userCallback)
    {
        function callback(styleSheetPayload)
        {
            if (!styleSheetPayload)
                userCallback(null);
            else {
                userCallback(new WebInspector.CSSStyleSheet(styleSheetPayload));
                WebInspector.cssModel._styleSheetChanged(this.id, true);
            }
        }

        InspectorBackend.setStyleSheetText(this.id, newText, callback.bind(this));
    }
}
