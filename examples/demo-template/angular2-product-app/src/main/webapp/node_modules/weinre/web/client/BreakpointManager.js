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

WebInspector.BreakpointManager = function()
{
    this._stickyBreakpoints = {};
    var breakpoints = WebInspector.settings.findSettingForAllProjects("nativeBreakpoints");
    for (var projectId in breakpoints)
        this._stickyBreakpoints[projectId] = this._validateBreakpoints(breakpoints[projectId]);

    this._breakpoints = {};
    this._domBreakpointsRestored = false;

    WebInspector.settings.addEventListener(WebInspector.Settings.Events.ProjectChanged, this._projectChanged, this);
    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerPaused, this._debuggerPaused, this);
    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerResumed, this._debuggerResumed, this);
}

WebInspector.BreakpointManager.BreakpointTypes = {
    DOM: "DOM",
    EventListener: "EventListener",
    XHR: "XHR"
}

WebInspector.BreakpointManager.Events = {
    DOMBreakpointAdded: "dom-breakpoint-added",
    EventListenerBreakpointAdded: "event-listener-breakpoint-added",
    XHRBreakpointAdded: "xhr-breakpoint-added",
    ProjectChanged: "project-changed"
}

WebInspector.BreakpointManager.prototype = {
    createDOMBreakpoint: function(nodeId, type)
    {
        this._createDOMBreakpoint(nodeId, type, true, false);
    },

    _createDOMBreakpoint: function(nodeId, type, enabled, restored)
    {
        var node = WebInspector.domAgent.nodeForId(nodeId);
        if (!node)
            return;

        var breakpointId = this._createDOMBreakpointId(nodeId, type);
        if (breakpointId in this._breakpoints)
            return;

        var breakpoint = new WebInspector.DOMBreakpoint(node, type);
        this._setBreakpoint(breakpointId, breakpoint, enabled, restored);
        if (enabled && restored)
            breakpoint._enable();

        breakpoint.view = new WebInspector.DOMBreakpointView(this, breakpointId, enabled, node, type);
        this.dispatchEventToListeners(WebInspector.BreakpointManager.Events.DOMBreakpointAdded, breakpoint.view);
    },

    createEventListenerBreakpoint: function(eventName)
    {
        this._createEventListenerBreakpoint(eventName, true, false);
    },

    _createEventListenerBreakpoint: function(eventName, enabled, restored)
    {
        var breakpointId = this._createEventListenerBreakpointId(eventName);
        if (breakpointId in this._breakpoints)
            return;

        var breakpoint = new WebInspector.EventListenerBreakpoint(eventName);
        this._setBreakpoint(breakpointId, breakpoint, enabled, restored);

        breakpoint.view = new WebInspector.EventListenerBreakpointView(this, breakpointId, enabled, eventName);
        this.dispatchEventToListeners(WebInspector.BreakpointManager.Events.EventListenerBreakpointAdded, breakpoint.view);
    },

    createXHRBreakpoint: function(url)
    {
        this._createXHRBreakpoint(url, true, false);
    },

    _createXHRBreakpoint: function(url, enabled, restored)
    {
        var breakpointId = this._createXHRBreakpointId(url);
        if (breakpointId in this._breakpoints)
            return;

        var breakpoint = new WebInspector.XHRBreakpoint(url);
        this._setBreakpoint(breakpointId, breakpoint, enabled, restored);

        breakpoint.view = new WebInspector.XHRBreakpointView(this, breakpointId, enabled, url);
        this.dispatchEventToListeners(WebInspector.BreakpointManager.Events.XHRBreakpointAdded, breakpoint.view);
    },

    _setBreakpoint: function(breakpointId, breakpoint, enabled, restored)
    {
        this._breakpoints[breakpointId] = breakpoint;
        breakpoint.enabled = enabled;
        if (restored)
            return;
        if (enabled)
            breakpoint._enable();
        this._saveBreakpoints();
    },

    _setBreakpointEnabled: function(breakpointId, enabled)
    {
        var breakpoint = this._breakpoints[breakpointId];
        if (breakpoint.enabled === enabled)
            return;
        if (enabled)
            breakpoint._enable();
        else
            breakpoint._disable();
        breakpoint.enabled = enabled;
        this._saveBreakpoints();
    },

    _removeBreakpoint: function(breakpointId)
    {
        var breakpoint = this._breakpoints[breakpointId];
        if (breakpoint.enabled)
            breakpoint._disable();
        delete this._breakpoints[breakpointId];
        this._saveBreakpoints();
    },

    breakpointViewForEventData: function(eventData)
    {
        var breakpointId;
        if (eventData.breakpointType === WebInspector.BreakpointManager.BreakpointTypes.DOM)
            breakpointId = this._createDOMBreakpointId(eventData.nodeId, eventData.type);
        else if (eventData.breakpointType === WebInspector.BreakpointManager.BreakpointTypes.EventListener)
            breakpointId = this._createEventListenerBreakpointId(eventData.eventName);
        else if (eventData.breakpointType === WebInspector.BreakpointManager.BreakpointTypes.XHR)
            breakpointId = this._createXHRBreakpointId(eventData.breakpointURL);
        else
            return;

        var breakpoint = this._breakpoints[breakpointId];
        if (breakpoint)
            return breakpoint.view;
    },

    _debuggerPaused: function(event)
    {
        var eventType = event.data.eventType;
        var eventData = event.data.eventData;

        if (eventType !== WebInspector.DebuggerEventTypes.NativeBreakpoint)
            return;

        var breakpointView = this.breakpointViewForEventData(eventData);
        if (!breakpointView)
            return;

        breakpointView.hit = true;
        this._lastHitBreakpointView = breakpointView;
    },

    _debuggerResumed: function(event)
    {
        if (!this._lastHitBreakpointView)
            return;
        this._lastHitBreakpointView.hit = false;
        delete this._lastHitBreakpointView;
    },

    _projectChanged: function(event)
    {
        this._breakpoints = {};
        this._domBreakpointsRestored = false;
        this.dispatchEventToListeners(WebInspector.BreakpointManager.Events.ProjectChanged);

        var breakpoints = this._stickyBreakpoints[WebInspector.settings.projectId] || [];
        for (var i = 0; i < breakpoints.length; ++i) {
            var breakpoint = breakpoints[i];
            if (breakpoint.type === WebInspector.BreakpointManager.BreakpointTypes.EventListener)
                this._createEventListenerBreakpoint(breakpoint.condition.eventName, breakpoint.enabled, true);
            else if (breakpoint.type === WebInspector.BreakpointManager.BreakpointTypes.XHR)
                this._createXHRBreakpoint(breakpoint.condition.url, breakpoint.enabled, true);
        }

        if (!this._breakpointsPushedToFrontend) {
            InspectorBackend.setAllBrowserBreakpoints(this._stickyBreakpoints);
            this._breakpointsPushedToFrontend = true;
        }
    },

    restoreDOMBreakpoints: function()
    {
        function didPushNodeByPathToFrontend(path, nodeId)
        {
            pathToNodeId[path] = nodeId;
            pendingCalls -= 1;
            if (pendingCalls)
                return;
            for (var i = 0; i < breakpoints.length; ++i) {
                var breakpoint = breakpoints[i];
                if (breakpoint.type !== WebInspector.BreakpointManager.BreakpointTypes.DOM)
                    continue;
                var nodeId = pathToNodeId[breakpoint.condition.path];
                if (nodeId)
                    this._createDOMBreakpoint(nodeId, breakpoint.condition.type, breakpoint.enabled, true);
            }
            this._domBreakpointsRestored = true;
            this._saveBreakpoints();
        }

        var breakpoints = this._stickyBreakpoints[WebInspector.settings.projectId] || [];
        var pathToNodeId = {};
        var pendingCalls = 0;
        for (var i = 0; i < breakpoints.length; ++i) {
            if (breakpoints[i].type !== WebInspector.BreakpointManager.BreakpointTypes.DOM)
                continue;
            var path = breakpoints[i].condition.path;
            if (path in pathToNodeId)
                continue;
            pathToNodeId[path] = 0;
            pendingCalls += 1;
            InspectorBackend.pushNodeByPathToFrontend(path, didPushNodeByPathToFrontend.bind(this, path));
        }
        if (!pendingCalls)
            this._domBreakpointsRestored = true;
    },

    _saveBreakpoints: function()
    {
        var breakpoints = [];
        for (var breakpointId in this._breakpoints) {
            var breakpoint = this._breakpoints[breakpointId];
            var persistentBreakpoint = breakpoint._serializeToJSON();
            persistentBreakpoint.enabled = breakpoint.enabled;
            breakpoints.push(persistentBreakpoint);
        }
        if (!this._domBreakpointsRestored) {
            var stickyBreakpoints = this._stickyBreakpoints[WebInspector.settings.projectId] || [];
            for (var i = 0; i < stickyBreakpoints.length; ++i) {
                if (stickyBreakpoints[i].type === WebInspector.BreakpointManager.BreakpointTypes.DOM)
                    breakpoints.push(stickyBreakpoints[i]);
            }
        }
        WebInspector.settings.nativeBreakpoints = breakpoints;

        this._stickyBreakpoints[WebInspector.settings.projectId] = breakpoints;
        InspectorBackend.setAllBrowserBreakpoints(this._stickyBreakpoints);
    },

    _validateBreakpoints: function(persistentBreakpoints)
    {
        var breakpoints = [];
        var breakpointsSet = {};
        for (var i = 0; i < persistentBreakpoints.length; ++i) {
            var breakpoint = persistentBreakpoints[i];
            if (!("type" in breakpoint && "enabled" in breakpoint && "condition" in breakpoint))
                continue;
            var id = breakpoint.type + ":";
            var condition = breakpoint.condition;
            if (breakpoint.type === WebInspector.BreakpointManager.BreakpointTypes.DOM) {
                if (typeof condition.path !== "string" || typeof condition.type !== "number")
                    continue;
                id += condition.path + ":" + condition.type;
            } else if (breakpoint.type === WebInspector.BreakpointManager.BreakpointTypes.EventListener) {
                if (typeof condition.eventName !== "string")
                    continue;
                id += condition.eventName;
            } else if (breakpoint.type === WebInspector.BreakpointManager.BreakpointTypes.XHR) {
                if (typeof condition.url !== "string")
                    continue;
                id += condition.url;
            } else
                continue;
            if (id in breakpointsSet)
                continue;
            breakpointsSet[id] = true;
            breakpoints.push(breakpoint);
        }
        return breakpoints;
    },

    _createDOMBreakpointId: function(nodeId, type)
    {
        return "dom:" + nodeId + ":" + type;
    },

    _createEventListenerBreakpointId: function(eventName)
    {
        return "eventListner:" + eventName;
    },

    _createXHRBreakpointId: function(url)
    {
        return "xhr:" + url;
    }
}

WebInspector.BreakpointManager.prototype.__proto__ = WebInspector.Object.prototype;

WebInspector.DOMBreakpoint = function(node, type)
{
    this._nodeId = node.id;
    this._path = node.path();
    this._type = type;
}

WebInspector.DOMBreakpoint.prototype = {
    _enable: function()
    {
        InspectorBackend.setDOMBreakpoint(this._nodeId, this._type);
    },

    _disable: function()
    {
        InspectorBackend.removeDOMBreakpoint(this._nodeId, this._type);
    },

    _serializeToJSON: function()
    {
        var type = WebInspector.BreakpointManager.BreakpointTypes.DOM;
        return { type: type, condition: { path: this._path, type: this._type } };
    }
}

WebInspector.EventListenerBreakpoint = function(eventName)
{
    this._eventName = eventName;
}

WebInspector.EventListenerBreakpoint.prototype = {
    _enable: function()
    {
        InspectorBackend.setEventListenerBreakpoint(this._eventName);
    },

    _disable: function()
    {
        InspectorBackend.removeEventListenerBreakpoint(this._eventName);
    },

    _serializeToJSON: function()
    {
        var type = WebInspector.BreakpointManager.BreakpointTypes.EventListener;
        return { type: type, condition: { eventName: this._eventName } };
    }
}

WebInspector.XHRBreakpoint = function(url)
{
    this._url = url;
}

WebInspector.XHRBreakpoint.prototype = {
    _enable: function()
    {
        InspectorBackend.setXHRBreakpoint(this._url);
    },

    _disable: function()
    {
        InspectorBackend.removeXHRBreakpoint(this._url);
    },

    _serializeToJSON: function()
    {
        var type = WebInspector.BreakpointManager.BreakpointTypes.XHR;
        return { type: type, condition: { url: this._url } };
    }
}



WebInspector.NativeBreakpointView = function(manager, id, enabled)
{
    this._manager = manager;
    this._id = id;
    this._enabled = enabled;
    this._hit = false;
}

WebInspector.NativeBreakpointView.prototype = {
    get enabled()
    {
        return this._enabled;
    },

    set enabled(enabled)
    {
        this._manager._setBreakpointEnabled(this._id, enabled);
        this._enabled = enabled;
        this.dispatchEventToListeners("enable-changed");
    },

    get hit()
    {
        return this._hit;
    },

    set hit(hit)
    {
        this._hit = hit;
        this.dispatchEventToListeners("hit-state-changed");
    },

    remove: function()
    {
        this._manager._removeBreakpoint(this._id);
        this._onRemove();
        this.dispatchEventToListeners("removed");
    },

    _compare: function(x, y)
    {
        if (x !== y)
            return x < y ? -1 : 1;
        return 0;
    },

    _onRemove: function()
    {
    }
}

WebInspector.NativeBreakpointView.prototype.__proto__ = WebInspector.Object.prototype;

WebInspector.DOMBreakpointView = function(manager, id, enabled, node, type)
{
    WebInspector.NativeBreakpointView.call(this, manager, id, enabled);
    this._node = node;
    this._nodeId = node.id;
    this._type = type;
    node.breakpoints[this._type] = this;
}

WebInspector.DOMBreakpointView.prototype = {
    compareTo: function(other)
    {
        return this._compare(this._type, other._type);
    },

    populateLabelElement: function(element)
    {
        // FIXME: this should belong to the view, not the manager.
        var linkifiedNode = WebInspector.panels.elements.linkifyNodeById(this._nodeId);
        linkifiedNode.addStyleClass("monospace");
        element.appendChild(linkifiedNode);
        var description = document.createElement("div");
        description.className = "source-text";
        description.textContent = WebInspector.domBreakpointTypeLabel(this._type);
        element.appendChild(description);
    },

    populateStatusMessageElement: function(element, eventData)
    {
        var substitutions = [WebInspector.domBreakpointTypeLabel(this._type), WebInspector.panels.elements.linkifyNodeById(this._nodeId)];
        var formatters = {
            s: function(substitution)
            {
                return substitution;
            }
        };
        function append(a, b)
        {
            if (typeof b === "string")
                b = document.createTextNode(b);
            element.appendChild(b);
        }
        if (this._type === WebInspector.DOMBreakpointTypes.SubtreeModified) {
            var targetNode = WebInspector.panels.elements.linkifyNodeById(eventData.targetNodeId);
            if (eventData.insertion) {
                if (eventData.targetNodeId !== this._nodeId)
                    WebInspector.formatLocalized("Paused on a \"%s\" breakpoint set on %s, because a new child was added to its descendant %s.", substitutions.concat(targetNode), formatters, "", append);
                else
                    WebInspector.formatLocalized("Paused on a \"%s\" breakpoint set on %s, because a new child was added to that node.", substitutions, formatters, "", append);
            } else
                WebInspector.formatLocalized("Paused on a \"%s\" breakpoint set on %s, because its descendant %s was removed.", substitutions.concat(targetNode), formatters, "", append);
        } else
            WebInspector.formatLocalized("Paused on a \"%s\" breakpoint set on %s.", substitutions, formatters, "", append);
    },

    _onRemove: function()
    {
        delete this._node.breakpoints[this._type];
    }
}

WebInspector.DOMBreakpointView.prototype.__proto__ = WebInspector.NativeBreakpointView.prototype;

WebInspector.EventListenerBreakpointView = function(manager, id, enabled, eventName)
{
    WebInspector.NativeBreakpointView.call(this, manager, id, enabled);
    this._eventName = eventName;
}

WebInspector.EventListenerBreakpointView.eventNameForUI = function(eventName)
{
    if (!WebInspector.EventListenerBreakpointView._eventNamesForUI) {
        WebInspector.EventListenerBreakpointView._eventNamesForUI = {
            "instrumentation:setTimer": WebInspector.UIString("Set Timer"),
            "instrumentation:clearTimer": WebInspector.UIString("Clear Timer"),
            "instrumentation:timerFired": WebInspector.UIString("Timer Fired")
        };
    }
    return WebInspector.EventListenerBreakpointView._eventNamesForUI[eventName] || eventName.substring(eventName.indexOf(":") + 1);
}

WebInspector.EventListenerBreakpointView.prototype = {
    get eventName()
    {
        return this._eventName;
    },

    compareTo: function(other)
    {
        return this._compare(this._eventName, other._eventName);
    },

    populateLabelElement: function(element)
    {
        element.appendChild(document.createTextNode(this._uiEventName()));
    },

    populateStatusMessageElement: function(element, eventData)
    {
        var status = WebInspector.UIString("Paused on a \"%s\" Event Listener.", this._uiEventName());
        element.appendChild(document.createTextNode(status));
    },

    _uiEventName: function()
    {
        return WebInspector.EventListenerBreakpointView.eventNameForUI(this._eventName);
    }
}

WebInspector.EventListenerBreakpointView.prototype.__proto__ = WebInspector.NativeBreakpointView.prototype;

WebInspector.XHRBreakpointView = function(manager, id, enabled, url)
{
    WebInspector.NativeBreakpointView.call(this, manager, id, enabled);
    this._url = url;
}

WebInspector.XHRBreakpointView.prototype = {
    compareTo: function(other)
    {
        return this._compare(this._url, other._url);
    },

    populateEditElement: function(element)
    {
        element.textContent = this._url;
    },

    populateLabelElement: function(element)
    {
        var label;
        if (!this._url.length)
            label = WebInspector.UIString("Any XHR");
        else
            label = WebInspector.UIString("URL contains \"%s\"", this._url);
        element.appendChild(document.createTextNode(label));
        element.addStyleClass("cursor-auto");
    },

    populateStatusMessageElement: function(element)
    {
        var status = WebInspector.UIString("Paused on a XMLHttpRequest.");
        element.appendChild(document.createTextNode(status));
    }
}

WebInspector.XHRBreakpointView.prototype.__proto__ = WebInspector.NativeBreakpointView.prototype;

WebInspector.DOMBreakpointTypes = {
    SubtreeModified: 0,
    AttributeModified: 1,
    NodeRemoved: 2
};

WebInspector.domBreakpointTypeLabel = function(type)
{
    if (!WebInspector._DOMBreakpointTypeLabels) {
        WebInspector._DOMBreakpointTypeLabels = {};
        WebInspector._DOMBreakpointTypeLabels[WebInspector.DOMBreakpointTypes.SubtreeModified] = WebInspector.UIString("Subtree Modified");
        WebInspector._DOMBreakpointTypeLabels[WebInspector.DOMBreakpointTypes.AttributeModified] = WebInspector.UIString("Attribute Modified");
        WebInspector._DOMBreakpointTypeLabels[WebInspector.DOMBreakpointTypes.NodeRemoved] = WebInspector.UIString("Node Removed");
    }
    return WebInspector._DOMBreakpointTypeLabels[type];
}

WebInspector.domBreakpointTypeContextMenuLabel = function(type)
{
    if (!WebInspector._DOMBreakpointTypeContextMenuLabels) {
        WebInspector._DOMBreakpointTypeContextMenuLabels = {};
        WebInspector._DOMBreakpointTypeContextMenuLabels[WebInspector.DOMBreakpointTypes.SubtreeModified] = WebInspector.UIString("Break on Subtree Modifications");
        WebInspector._DOMBreakpointTypeContextMenuLabels[WebInspector.DOMBreakpointTypes.AttributeModified] = WebInspector.UIString("Break on Attributes Modifications");
        WebInspector._DOMBreakpointTypeContextMenuLabels[WebInspector.DOMBreakpointTypes.NodeRemoved] = WebInspector.UIString("Break on Node Removal");
    }
    return WebInspector._DOMBreakpointTypeContextMenuLabels[type];
}
