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

WebInspector.DebuggerModel = function()
{
    this._paused = false;
    this._callFrames = [];
    this._breakpoints = {};
    this._scripts = {};

    InspectorBackend.registerDomainDispatcher("Debugger", new WebInspector.DebuggerDispatcher(this));
}

WebInspector.DebuggerModel.Events = {
    DebuggerPaused: "debugger-paused",
    DebuggerResumed: "debugger-resumed",
    ParsedScriptSource: "parsed-script-source",
    FailedToParseScriptSource: "failed-to-parse-script-source",
    ScriptSourceChanged: "script-source-changed",
    BreakpointAdded: "breakpoint-added",
    BreakpointRemoved: "breakpoint-removed",
    BreakpointResolved: "breakpoint-resolved"
}

WebInspector.DebuggerModel.prototype = {
    enableDebugger: function()
    {
        InspectorBackend.enableDebugger();
        if (this._breakpointsPushedToBackend)
            return;
        var breakpoints = WebInspector.settings.breakpoints;
        for (var i = 0; i < breakpoints.length; ++i) {
            var breakpoint = breakpoints[i];
            if (typeof breakpoint.url !== "string" || typeof breakpoint.lineNumber !== "number" || typeof breakpoint.columnNumber !== "number" ||
                typeof breakpoint.condition !== "string" || typeof breakpoint.enabled !== "boolean")
                continue;
            this.setBreakpoint(breakpoint.url, breakpoint.lineNumber, breakpoint.columnNumber, breakpoint.condition, breakpoint.enabled);
        }
        this._breakpointsPushedToBackend = true;
    },

    disableDebugger: function()
    {
        InspectorBackend.disableDebugger();
    },

    continueToLocation: function(sourceID, lineNumber, columnNumber)
    {
        InspectorBackend.continueToLocation(sourceID, lineNumber, columnNumber);
    },

    setBreakpoint: function(url, lineNumber, columnNumber, condition, enabled)
    {
        function didSetBreakpoint(breakpointsPushedToBackend, breakpointId, locations)
        {
            if (!breakpointId)
                return;
            var breakpoint = new WebInspector.Breakpoint(breakpointId, url, "", lineNumber, columnNumber, condition, enabled);
            breakpoint.locations = locations;
            this._breakpoints[breakpointId] = breakpoint;
            if (breakpointsPushedToBackend)
                this._saveBreakpoints();
            this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.BreakpointAdded, breakpoint);
        }
        InspectorBackend.setJavaScriptBreakpoint(url, lineNumber, columnNumber, condition, enabled, didSetBreakpoint.bind(this, this._breakpointsPushedToBackend));
    },

    setBreakpointBySourceId: function(sourceID, lineNumber, columnNumber, condition, enabled)
    {
        function didSetBreakpoint(breakpointId, actualLineNumber, actualColumnNumber)
        {
            if (!breakpointId)
                return;
            var breakpoint = new WebInspector.Breakpoint(breakpointId, "", sourceID, lineNumber, columnNumber, condition, enabled);
            breakpoint.addLocation(sourceID, actualLineNumber, actualColumnNumber);
            this._breakpoints[breakpointId] = breakpoint;
            this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.BreakpointAdded, breakpoint);
        }
        InspectorBackend.setJavaScriptBreakpointBySourceId(sourceID, lineNumber, columnNumber, condition, enabled, didSetBreakpoint.bind(this));
    },

    removeBreakpoint: function(breakpointId)
    {
        InspectorBackend.removeJavaScriptBreakpoint(breakpointId);
        var breakpoint = this._breakpoints[breakpointId];
        delete this._breakpoints[breakpointId];
        this._saveBreakpoints();
        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.BreakpointRemoved, breakpointId);
    },

    updateBreakpoint: function(breakpointId, condition, enabled)
    {
        var breakpoint = this._breakpoints[breakpointId];
        this.removeBreakpoint(breakpointId);
        if (breakpoint.url)
            this.setBreakpoint(breakpoint.url, breakpoint.lineNumber, breakpoint.columnNumber, condition, enabled);
        else
            this.setBreakpointBySourceId(breakpoint.sourceID, breakpoint.lineNumber, breakpoint.columnNumber, condition, enabled);
    },

    _breakpointResolved: function(breakpointId, sourceID, lineNumber, columnNumber)
    {
        var breakpoint = this._breakpoints[breakpointId];
        if (!breakpoint)
            return;
        breakpoint.addLocation(sourceID, lineNumber, columnNumber);
        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.BreakpointResolved, breakpoint);
    },

    _saveBreakpoints: function()
    {
        var serializedBreakpoints = [];
        for (var id in this._breakpoints) {
            var breakpoint = this._breakpoints[id];
            if (!breakpoint.url)
                continue;
            var serializedBreakpoint = {};
            serializedBreakpoint.url = breakpoint.url;
            serializedBreakpoint.lineNumber = breakpoint.lineNumber;
            serializedBreakpoint.columnNumber = breakpoint.columnNumber;
            serializedBreakpoint.condition = breakpoint.condition;
            serializedBreakpoint.enabled = breakpoint.enabled;
            serializedBreakpoints.push(serializedBreakpoint);
        }
        WebInspector.settings.breakpoints = serializedBreakpoints;
    },

    get breakpoints()
    {
        return this._breakpoints;
    },

    breakpointForId: function(breakpointId)
    {
        return this._breakpoints[breakpointId];
    },

    queryBreakpoints: function(filter)
    {
        var breakpoints = [];
        for (var id in this._breakpoints) {
           var breakpoint = this._breakpoints[id];
           if (filter(breakpoint))
               breakpoints.push(breakpoint);
        }
        return breakpoints;
    },

    reset: function()
    {
        this._paused = false;
        this._callFrames = [];
        for (var id in this._breakpoints) {
            var breakpoint = this._breakpoints[id];
            if (!breakpoint.url)
                this.removeBreakpoint(id);
            else
                breakpoint.locations = [];
        }
        this._scripts = {};
    },

    scriptForSourceID: function(sourceID)
    {
        return this._scripts[sourceID];
    },

    scriptsForURL: function(url)
    {
        return this.queryScripts(function(s) { return s.sourceURL === url; });
    },

    queryScripts: function(filter)
    {
        var scripts = [];
        for (var sourceID in this._scripts) {
            var script = this._scripts[sourceID];
            if (filter(script))
                scripts.push(script);
        }
        return scripts;
    },

    editScriptSource: function(sourceID, scriptSource)
    {
        function didEditScriptSource(success, newBodyOrErrorMessage, callFrames)
        {
            if (success) {
                if (callFrames && callFrames.length)
                    this._callFrames = callFrames;
                this._updateScriptSource(sourceID, newBodyOrErrorMessage);
            } else
                WebInspector.log(newBodyOrErrorMessage, WebInspector.ConsoleMessage.MessageLevel.Warning);
        }
        InspectorBackend.editScriptSource(sourceID, scriptSource, didEditScriptSource.bind(this));
    },

    _updateScriptSource: function(sourceID, scriptSource)
    {
        var script = this._scripts[sourceID];
        var oldSource = script.source;
        script.source = scriptSource;

        // Clear and re-create breakpoints according to text diff.
        var diff = Array.diff(oldSource.split("\n"), script.source.split("\n"));
        for (var id in this._breakpoints) {
            var breakpoint = this._breakpoints[id];
            if (breakpoint.url) {
                if (breakpoint.url !== script.sourceURL)
                    continue;
            } else {
                if (breakpoint.sourceID !== sourceID)
                    continue;
            }
            this.removeBreakpoint(breakpoint.id);
            var lineNumber = breakpoint.lineNumber;
            var newLineNumber = diff.left[lineNumber].row;
            if (newLineNumber === undefined) {
                for (var i = lineNumber - 1; i >= 0; --i) {
                    if (diff.left[i].row === undefined)
                        continue;
                    var shiftedLineNumber = diff.left[i].row + lineNumber - i;
                    if (shiftedLineNumber < diff.right.length) {
                        var originalLineNumber = diff.right[shiftedLineNumber].row;
                        if (originalLineNumber === lineNumber || originalLineNumber === undefined)
                            newLineNumber = shiftedLineNumber;
                    }
                    break;
                }
            }
            if (newLineNumber === undefined)
                continue;
            if (breakpoint.url)
                this.setBreakpoint(breakpoint.url, newLineNumber, breakpoint.columnNumber, breakpoint.condition, breakpoint.enabled);
            else
                this.setBreakpointBySourceId(sourceID, newLineNumber, breakpoint.columnNumber, breakpoint.condition, breakpoint.enabled);
        }

        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.ScriptSourceChanged, { sourceID: sourceID, oldSource: oldSource });
    },

    get callFrames()
    {
        return this._callFrames;
    },

    _pausedScript: function(details)
    {
        this._paused = true;
        this._callFrames = details.callFrames;
        details.breakpoint = this._breakpointForCallFrame(details.callFrames[0]);
        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.DebuggerPaused, details);
    },

    _resumedScript: function()
    {
        this._paused = false;
        this._callFrames = [];
        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.DebuggerResumed);
    },

    _breakpointForCallFrame: function(callFrame)
    {
        function match(location)
        {
            if (location.sourceID != callFrame.sourceID)
                return false;
            return location.lineNumber === callFrame.line && location.columnNumber === callFrame.column;
        }
        for (var id in this._breakpoints) {
            var breakpoint = this._breakpoints[id];
            for (var i = 0; i < breakpoint.locations.length; ++i) {
                if (match(breakpoint.locations[i]))
                    return breakpoint;
            }
        }
    },

    _parsedScriptSource: function(sourceID, sourceURL, lineOffset, columnOffset, length, scriptWorldType)
    {
        var script = new WebInspector.Script(sourceID, sourceURL, "", lineOffset, columnOffset, length, undefined, undefined, scriptWorldType);
        this._scripts[sourceID] = script;
        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.ParsedScriptSource, script);
    },

    _failedToParseScriptSource: function(sourceURL, source, startingLine, errorLine, errorMessage)
    {
        var script = new WebInspector.Script(null, sourceURL, source, startingLine, errorLine, errorMessage, undefined);
        this.dispatchEventToListeners(WebInspector.DebuggerModel.Events.FailedToParseScriptSource, script);
    }
}

WebInspector.DebuggerModel.prototype.__proto__ = WebInspector.Object.prototype;

WebInspector.DebuggerEventTypes = {
    JavaScriptPause: 0,
    JavaScriptBreakpoint: 1,
    NativeBreakpoint: 2
};

WebInspector.DebuggerDispatcher = function(debuggerModel)
{
    this._debuggerModel = debuggerModel;
}

WebInspector.DebuggerDispatcher.prototype = {
    pausedScript: function(details)
    {
        this._debuggerModel._pausedScript(details);
    },

    resumedScript: function()
    {
        this._debuggerModel._resumedScript();
    },

    debuggerWasEnabled: function()
    {
        WebInspector.panels.scripts.debuggerWasEnabled();
    },

    debuggerWasDisabled: function()
    {
        WebInspector.panels.scripts.debuggerWasDisabled();
    },

    parsedScriptSource: function(sourceID, sourceURL, lineOffset, columnOffset, length, scriptWorldType)
    {
        this._debuggerModel._parsedScriptSource(sourceID, sourceURL, lineOffset, columnOffset, length, scriptWorldType);
    },

    failedToParseScriptSource: function(sourceURL, source, startingLine, errorLine, errorMessage)
    {
        this._debuggerModel._failedToParseScriptSource(sourceURL, source, startingLine, errorLine, errorMessage);
    },

    breakpointResolved: function(breakpointId, sourceID, lineNumber, columnNumber)
    {
        this._debuggerModel._breakpointResolved(breakpointId, sourceID, lineNumber, columnNumber);
    },

    didCreateWorker: function()
    {
        var workersPane = WebInspector.panels.scripts.sidebarPanes.workers;
        workersPane.addWorker.apply(workersPane, arguments);
    },

    didDestroyWorker: function()
    {
        var workersPane = WebInspector.panels.scripts.sidebarPanes.workers;
        workersPane.removeWorker.apply(workersPane, arguments);
    }
}
