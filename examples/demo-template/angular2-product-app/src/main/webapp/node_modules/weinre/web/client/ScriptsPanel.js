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

WebInspector.ScriptsPanel = function()
{
    WebInspector.Panel.call(this, "scripts");

    this.topStatusBar = document.createElement("div");
    this.topStatusBar.className = "status-bar";
    this.topStatusBar.id = "scripts-status-bar";
    this.element.appendChild(this.topStatusBar);

    this.backButton = document.createElement("button");
    this.backButton.className = "status-bar-item";
    this.backButton.id = "scripts-back";
    this.backButton.title = WebInspector.UIString("Show the previous script resource.");
    this.backButton.disabled = true;
    this.backButton.appendChild(document.createElement("img"));
    this.backButton.addEventListener("click", this._goBack.bind(this), false);
    this.topStatusBar.appendChild(this.backButton);

    this.forwardButton = document.createElement("button");
    this.forwardButton.className = "status-bar-item";
    this.forwardButton.id = "scripts-forward";
    this.forwardButton.title = WebInspector.UIString("Show the next script resource.");
    this.forwardButton.disabled = true;
    this.forwardButton.appendChild(document.createElement("img"));
    this.forwardButton.addEventListener("click", this._goForward.bind(this), false);
    this.topStatusBar.appendChild(this.forwardButton);

    this.filesSelectElement = document.createElement("select");
    this.filesSelectElement.className = "status-bar-item";
    this.filesSelectElement.id = "scripts-files";
    this.filesSelectElement.addEventListener("change", this._changeVisibleFile.bind(this), false);
    this.topStatusBar.appendChild(this.filesSelectElement);

    this.functionsSelectElement = document.createElement("select");
    this.functionsSelectElement.className = "status-bar-item";
    this.functionsSelectElement.id = "scripts-functions";

    // FIXME: append the functions select element to the top status bar when it is implemented.
    // this.topStatusBar.appendChild(this.functionsSelectElement);

    this.formatButton = document.createElement("button");
    this.formatButton.className = "status-bar-item";
    this.formatButton.id = "format-script";
    this.formatButton.title = WebInspector.UIString("Format script.");
    this.formatButton.appendChild(document.createElement("img"));
    this.formatButton.addEventListener("click", this._formatScript.bind(this), false);
    if (Preferences.debugMode)
        this.topStatusBar.appendChild(this.formatButton);

    this.sidebarButtonsElement = document.createElement("div");
    this.sidebarButtonsElement.id = "scripts-sidebar-buttons";
    this.topStatusBar.appendChild(this.sidebarButtonsElement);

    this.pauseButton = document.createElement("button");
    this.pauseButton.className = "status-bar-item";
    this.pauseButton.id = "scripts-pause";
    this.pauseButton.title = WebInspector.UIString("Pause script execution.");
    this.pauseButton.disabled = true;
    this.pauseButton.appendChild(document.createElement("img"));
    this.pauseButton.addEventListener("click", this._togglePause.bind(this), false);
    this.sidebarButtonsElement.appendChild(this.pauseButton);

    this.stepOverButton = document.createElement("button");
    this.stepOverButton.className = "status-bar-item";
    this.stepOverButton.id = "scripts-step-over";
    this.stepOverButton.title = WebInspector.UIString("Step over next function call.");
    this.stepOverButton.disabled = true;
    this.stepOverButton.addEventListener("click", this._stepOverClicked.bind(this), false);
    this.stepOverButton.appendChild(document.createElement("img"));
    this.sidebarButtonsElement.appendChild(this.stepOverButton);

    this.stepIntoButton = document.createElement("button");
    this.stepIntoButton.className = "status-bar-item";
    this.stepIntoButton.id = "scripts-step-into";
    this.stepIntoButton.title = WebInspector.UIString("Step into next function call.");
    this.stepIntoButton.disabled = true;
    this.stepIntoButton.addEventListener("click", this._stepIntoClicked.bind(this), false);
    this.stepIntoButton.appendChild(document.createElement("img"));
    this.sidebarButtonsElement.appendChild(this.stepIntoButton);

    this.stepOutButton = document.createElement("button");
    this.stepOutButton.className = "status-bar-item";
    this.stepOutButton.id = "scripts-step-out";
    this.stepOutButton.title = WebInspector.UIString("Step out of current function.");
    this.stepOutButton.disabled = true;
    this.stepOutButton.addEventListener("click", this._stepOutClicked.bind(this), false);
    this.stepOutButton.appendChild(document.createElement("img"));
    this.sidebarButtonsElement.appendChild(this.stepOutButton);

    this.toggleBreakpointsButton = new WebInspector.StatusBarButton(WebInspector.UIString("Deactivate all breakpoints."), "toggle-breakpoints");
    this.toggleBreakpointsButton.toggled = true;
    this.toggleBreakpointsButton.addEventListener("click", this.toggleBreakpointsClicked.bind(this), false);
    this.sidebarButtonsElement.appendChild(this.toggleBreakpointsButton.element);

    this.debuggerStatusElement = document.createElement("div");
    this.debuggerStatusElement.id = "scripts-debugger-status";
    this.sidebarButtonsElement.appendChild(this.debuggerStatusElement);

    this.viewsContainerElement = document.createElement("div");
    this.viewsContainerElement.id = "script-resource-views";

    this.sidebarElement = document.createElement("div");
    this.sidebarElement.id = "scripts-sidebar";

    this.sidebarResizeElement = document.createElement("div");
    this.sidebarResizeElement.className = "sidebar-resizer-vertical";
    this.sidebarResizeElement.addEventListener("mousedown", this._startSidebarResizeDrag.bind(this), false);

    this.sidebarResizeWidgetElement = document.createElement("div");
    this.sidebarResizeWidgetElement.id = "scripts-sidebar-resizer-widget";
    this.sidebarResizeWidgetElement.addEventListener("mousedown", this._startSidebarResizeDrag.bind(this), false);
    this.topStatusBar.appendChild(this.sidebarResizeWidgetElement);

    this.sidebarPanes = {};
    this.sidebarPanes.watchExpressions = new WebInspector.WatchExpressionsSidebarPane();
    this.sidebarPanes.callstack = new WebInspector.CallStackSidebarPane();
    this.sidebarPanes.scopechain = new WebInspector.ScopeChainSidebarPane();
    this.sidebarPanes.jsBreakpoints = new WebInspector.JavaScriptBreakpointsSidebarPane();
    if (Preferences.nativeInstrumentationEnabled) {
        this.sidebarPanes.domBreakpoints = WebInspector.createDOMBreakpointsSidebarPane();
        this.sidebarPanes.xhrBreakpoints = WebInspector.createXHRBreakpointsSidebarPane();
        this.sidebarPanes.eventListenerBreakpoints = new WebInspector.EventListenerBreakpointsSidebarPane();
    }

    this.sidebarPanes.workers = new WebInspector.WorkersSidebarPane();

    for (var pane in this.sidebarPanes)
        this.sidebarElement.appendChild(this.sidebarPanes[pane].element);

    this.sidebarPanes.callstack.expanded = true;
    this.sidebarPanes.callstack.addEventListener("call frame selected", this._callFrameSelected, this);

    this.sidebarPanes.scopechain.expanded = true;
    this.sidebarPanes.jsBreakpoints.expanded = true;

    var panelEnablerHeading = WebInspector.UIString("You need to enable debugging before you can use the Scripts panel.");
    var panelEnablerDisclaimer = WebInspector.UIString("Enabling debugging will make scripts run slower.");
    var panelEnablerButton = WebInspector.UIString("Enable Debugging");

    this.panelEnablerView = new WebInspector.PanelEnablerView("scripts", panelEnablerHeading, panelEnablerDisclaimer, panelEnablerButton);
    this.panelEnablerView.addEventListener("enable clicked", this._enableDebugging, this);

    this.element.appendChild(this.panelEnablerView.element);
    this.element.appendChild(this.viewsContainerElement);
    this.element.appendChild(this.sidebarElement);
    this.element.appendChild(this.sidebarResizeElement);

    this.enableToggleButton = new WebInspector.StatusBarButton("", "enable-toggle-status-bar-item");
    this.enableToggleButton.addEventListener("click", this._toggleDebugging.bind(this), false);
    if (Preferences.debuggerAlwaysEnabled)
        this.enableToggleButton.element.addStyleClass("hidden");

    this._pauseOnExceptionButton = new WebInspector.StatusBarButton("", "scripts-pause-on-exceptions-status-bar-item", 3);
    this._pauseOnExceptionButton.addEventListener("click", this._togglePauseOnExceptions.bind(this), false);

    this._registerShortcuts();

    this._debuggerEnabled = Preferences.debuggerAlwaysEnabled;

    this.reset();

    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.ParsedScriptSource, this._parsedScriptSource, this);
    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.FailedToParseScriptSource, this._failedToParseScriptSource, this);
    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.ScriptSourceChanged, this._scriptSourceChanged, this);
    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerPaused, this._debuggerPaused, this);
    WebInspector.debuggerModel.addEventListener(WebInspector.DebuggerModel.Events.DebuggerResumed, this._debuggerResumed, this);
}

// Keep these in sync with WebCore::ScriptDebugServer
WebInspector.ScriptsPanel.PauseOnExceptionsState = {
    DontPauseOnExceptions : 0,
    PauseOnAllExceptions : 1,
    PauseOnUncaughtExceptions: 2
};

WebInspector.ScriptsPanel.prototype = {
    get toolbarItemLabel()
    {
        return WebInspector.UIString("Scripts");
    },

    get statusBarItems()
    {
        return [this.enableToggleButton.element, this._pauseOnExceptionButton.element];
    },

    get defaultFocusedElement()
    {
        return this.filesSelectElement;
    },

    get paused()
    {
        return this._paused;
    },

    show: function()
    {
        WebInspector.Panel.prototype.show.call(this);
        this.sidebarResizeElement.style.right = (this.sidebarElement.offsetWidth - 3) + "px";

        if (this.visibleView)
            this.visibleView.show(this.viewsContainerElement);
    },

    hide: function()
    {
        if (this.visibleView)
            this.visibleView.hide();
        WebInspector.Panel.prototype.hide.call(this);
    },

    get breakpointsActivated()
    {
        return this.toggleBreakpointsButton.toggled;
    },

    _parsedScriptSource: function(event)
    {
        this._addScript(event.data);
    },

    _failedToParseScriptSource: function(event)
    {
        this._addScript(event.data);
    },

    _scriptSourceChanged: function(event)
    {
        var sourceID = event.data.sourceID;
        var oldSource = event.data.oldSource;

        var oldView, newView;
        var script = WebInspector.debuggerModel.scriptForSourceID(sourceID);
        if (script.resource) {
            oldView = this._urlToSourceFrame[script.resource.url];
            delete this._urlToSourceFrame[script.resource.url];
            newView = this._sourceFrameForResource(script.resource);
            var revertHandle = WebInspector.debuggerModel.editScriptSource.bind(WebInspector.debuggerModel, sourceID, oldSource);
            script.resource.setContent(script.source, revertHandle);
        } else {
            var oldView = script._sourceFrame;
            delete script._sourceFrame;
            newView = this._sourceFrameForScript(script);
        }
        newView.scrollTop = oldView.scrollTop;

        if (this.visibleView === oldView)
            this.visibleView = newView;

        var callFrames = WebInspector.debuggerModel.callFrames;
        if (callFrames.length)
            this._debuggerPaused({ data: { callFrames: callFrames } });
    },

    _addScript: function(script)
    {
        var resource = WebInspector.networkManager.inflightResourceForURL(script.sourceURL) || WebInspector.resourceForURL(script.sourceURL);
        if (resource) {
            if (resource.finished) {
                // Resource is finished, bind the script right away.
                script.resource = resource;
            } else {
                // Resource is not finished, bind the script later.
                if (!resource._scriptsPendingResourceLoad) {
                    resource._scriptsPendingResourceLoad = [];
                    resource.addEventListener("finished", this._resourceLoadingFinished, this);
                }
                resource._scriptsPendingResourceLoad.push(script);
            }
        }
        this._addScriptToFilesMenu(script);
    },

    _resourceLoadingFinished: function(e)
    {
        var resource = e.target;

        var visible = false;
        var select = this.filesSelectElement;
        for (var i = 0; i < resource._scriptsPendingResourceLoad.length; ++i) {
            // Bind script to resource.
            var script = resource._scriptsPendingResourceLoad[i];
            script.resource = resource;

            if (select.options[select.selectedIndex] === script.filesSelectOption)
                visible = true;

            // Remove script from the files list.
            script.filesSelectOption.parentElement.removeChild(script.filesSelectOption);
        }
        // Adding first script will add resource.
        this._addScriptToFilesMenu(resource._scriptsPendingResourceLoad[0]);
        delete resource._scriptsPendingResourceLoad;

        if (visible)
            this._showScriptOrResource(resource, { initialLoad: true });
    },

    addConsoleMessage: function(message)
    {
        this._messages.push(message);
        var sourceFrame = this._urlToSourceFrame[message.url];
        if (sourceFrame)
            sourceFrame.addMessage(message);
    },

    clearConsoleMessages: function()
    {
        this._messages = [];
        for (var url in this._urlToSourceFrame)
            this._urlToSourceFrame[url].clearMessages();
    },

    selectedCallFrameId: function()
    {
        var selectedCallFrame = this.sidebarPanes.callstack.selectedCallFrame;
        if (!selectedCallFrame)
            return null;
        return selectedCallFrame.id;
    },

    evaluateInSelectedCallFrame: function(code, updateInterface, objectGroup, includeCommandLineAPI, callback)
    {
        var selectedCallFrame = this.sidebarPanes.callstack.selectedCallFrame;
        if (!this._paused || !selectedCallFrame)
            return;

        if (typeof updateInterface === "undefined")
            updateInterface = true;

        function updatingCallbackWrapper(result)
        {
            if (result) {
                callback(WebInspector.RemoteObject.fromPayload(result));
                if (updateInterface)
                    this.sidebarPanes.scopechain.update(selectedCallFrame);
            }
        }
        InspectorBackend.evaluateOnCallFrame(selectedCallFrame.id, code, objectGroup, includeCommandLineAPI, updatingCallbackWrapper.bind(this));
    },

    _debuggerPaused: function(event)
    {
        var callFrames = event.data.callFrames;

        this._paused = true;
        this._waitingToPause = false;
        this._stepping = false;

        this._updateDebuggerButtons();

        WebInspector.currentPanel = this;

        this.sidebarPanes.callstack.update(event.data);
        this.sidebarPanes.callstack.selectedCallFrame = callFrames[0];

        window.focus();
        InspectorFrontendHost.bringToFront();
    },

    _debuggerResumed: function()
    {
        this._paused = false;
        this._waitingToPause = false;
        this._stepping = false;

        this._clearInterface();
    },

    debuggerWasEnabled: function()
    {
        this._setPauseOnExceptions(WebInspector.settings.pauseOnExceptionState);

        if (this._debuggerEnabled)
            return;
        this._debuggerEnabled = true;
        this.reset(true);
    },

    debuggerWasDisabled: function()
    {
        if (!this._debuggerEnabled)
            return;

        this._debuggerEnabled = false;
        this.reset(true);
    },

    reset: function(preserveItems)
    {
        this.visibleView = null;

        delete this.currentQuery;
        this.searchCanceled();

        this._debuggerResumed();

        this._backForwardList = [];
        this._currentBackForwardIndex = -1;
        this._updateBackAndForwardButtons();

        this._urlToSourceFrame = {};
        this._messages = [];
        this._resourceForURLInFilesSelect = {};
        this.filesSelectElement.removeChildren();
        this.functionsSelectElement.removeChildren();
        this.viewsContainerElement.removeChildren();

        this.sidebarPanes.watchExpressions.refreshExpressions();
        if (!preserveItems)
            this.sidebarPanes.workers.reset();
    },

    get visibleView()
    {
        return this._visibleView;
    },

    set visibleView(x)
    {
        if (this._visibleView === x)
            return;

        if (this._visibleView)
            this._visibleView.hide();

        this._visibleView = x;

        if (x)
            x.show(this.viewsContainerElement);
    },

    canShowSourceLine: function(url, line)
    {
        if (!this._debuggerEnabled)
            return false;
        return !!this._scriptOrResourceForURLAndLine(url, line);
    },

    showSourceLine: function(url, line)
    {
        var scriptOrResource = this._scriptOrResourceForURLAndLine(url, line);
        this._showScriptOrResource(scriptOrResource, {line: line, shouldHighlightLine: true});
    },

    _scriptOrResourceForURLAndLine: function(url, line)
    {
        var scripts = WebInspector.debuggerModel.scriptsForURL(url);
        for (var i = 0; i < scripts.length; ++i) {
            var script = scripts[i];
            if (script.resource)
                return script.resource;
            if (script.startingLine <= line && script.startingLine + script.linesCount > line)
                return script;
        }
        return null;
    },

    showView: function(view)
    {
        if (!view)
            return;
        this._showScriptOrResource(view.resource || view.script);
    },

    handleShortcut: function(event)
    {
        var shortcut = WebInspector.KeyboardShortcut.makeKeyFromEvent(event);
        var handler = this._shortcuts[shortcut];
        if (handler) {
            handler(event);
            event.handled = true;
        } else
            this.sidebarPanes.callstack.handleShortcut(event);
    },

    _sourceFrameForScriptOrResource: function(scriptOrResource)
    {
        if (scriptOrResource instanceof WebInspector.Resource)
            return this._sourceFrameForResource(scriptOrResource);
        return this._sourceFrameForScript(scriptOrResource);
    },

    _sourceFrameForResource: function(resource)
    {
        var sourceFrame = this._urlToSourceFrame[resource.url];
        if (sourceFrame)
            return sourceFrame;
        var contentProvider = new WebInspector.SourceFrameContentProviderForResource(resource);
        var isScript = resource.type === WebInspector.Resource.Type.Script;
        sourceFrame = new WebInspector.SourceFrame(contentProvider, resource.url, isScript);
        for (var i = 0; i < this._messages.length; ++i) {
            var message = this._messages[i];
            if (this._messages[i].url === resource.url)
                sourceFrame.addMessage(message);
        }
        this._urlToSourceFrame[resource.url] = sourceFrame;
        return sourceFrame;
    },

    _sourceFrameForScript: function(script)
    {
        if (script._sourceFrame)
            return script._sourceFrame;
        var contentProvider = new WebInspector.SourceFrameContentProviderForScript(script);
        script._sourceFrame = new WebInspector.SourceFrame(contentProvider, script.sourceURL, true);
        return script._sourceFrame;
    },

    _showScriptOrResource: function(scriptOrResource, options)
    {
        // options = {line:, shouldHighlightLine:, fromBackForwardAction:, initialLoad:}
        options = options || {};

        if (!scriptOrResource)
            return;

        var view = this._sourceFrameForScriptOrResource(scriptOrResource);
        if (!view)
            return;

        var url = scriptOrResource.url || scriptOrResource.sourceURL;
        if (url && !options.initialLoad)
            WebInspector.settings.lastViewedScriptFile = url;

        if (!options.fromBackForwardAction) {
            var oldIndex = this._currentBackForwardIndex;
            if (oldIndex >= 0)
                this._backForwardList.splice(oldIndex + 1, this._backForwardList.length - oldIndex);

            // Check for a previous entry of the same object in _backForwardList.
            // If one is found, remove it and update _currentBackForwardIndex to match.
            var previousEntryIndex = this._backForwardList.indexOf(scriptOrResource);
            if (previousEntryIndex !== -1) {
                this._backForwardList.splice(previousEntryIndex, 1);
                --this._currentBackForwardIndex;
            }

            this._backForwardList.push(scriptOrResource);
            ++this._currentBackForwardIndex;

            this._updateBackAndForwardButtons();
        }

        this.visibleView = view;

        if (options.line) {
            if (view.revealLine)
                view.revealLine(options.line);
            if (view.highlightLine && options.shouldHighlightLine)
                view.highlightLine(options.line);
        }

        var option;
        if (scriptOrResource instanceof WebInspector.Script) {
            option = scriptOrResource.filesSelectOption;

            // hasn't been added yet - happens for stepping in evals,
            // so use the force option to force the script into the menu.
            if (!option) {
                this._addScriptToFilesMenu(scriptOrResource, true);
                option = scriptOrResource.filesSelectOption;
            }

            console.assert(option);
        } else
            option = scriptOrResource.filesSelectOption;

        if (option)
            this.filesSelectElement.selectedIndex = option.index;
    },

    _addScriptToFilesMenu: function(script, force)
    {
        if (!script.sourceURL && !force)
            return;

        if (script.resource) {
            if (this._resourceForURLInFilesSelect[script.resource.url])
                return;
            this._resourceForURLInFilesSelect[script.resource.url] = script.resource;
        }

        var displayName = script.sourceURL ? WebInspector.displayNameForURL(script.sourceURL) : WebInspector.UIString("(program)");

        var select = this.filesSelectElement;
        var option = document.createElement("option");
        option.representedObject = script.resource || script;
        option.url = displayName;
        option.startingLine = script.startingLine;
        option.text = script.resource || script.startingLine === 1 ? displayName : String.sprintf("%s:%d", displayName, script.startingLine);

        function optionCompare(a, b)
        {
            if (a.url < b.url)
                return -1;
            else if (a.url > b.url)
                return 1;

            if (typeof a.startingLine !== "number")
                return -1;
            if (typeof b.startingLine !== "number")
                return -1;
            return a.startingLine - b.startingLine;
        }

        var insertionIndex = insertionIndexForObjectInListSortedByFunction(option, select.childNodes, optionCompare);
        if (insertionIndex < 0)
            select.appendChild(option);
        else
            select.insertBefore(option, select.childNodes.item(insertionIndex));

        if (script.resource)
            script.resource.filesSelectOption = option;
        else
            script.filesSelectOption = option;

        if (select.options[select.selectedIndex] === option) {
            // Call _showScriptOrResource if the option we just appended ended up being selected.
            // This will happen for the first item added to the menu.
            this._showScriptOrResource(option.representedObject, {initialLoad: true});
        } else {
            // If not first item, check to see if this was the last viewed
            var url = option.representedObject.url || option.representedObject.sourceURL;
            var lastURL = WebInspector.settings.lastViewedScriptFile;
            if (url && url === lastURL) {
                // For resources containing multiple <script> tags, we first report them separately and
                // then glue them all together. They all share url and there is no need to show them all one
                // by one.
                var isResource = !!option.representedObject.url;
                if (isResource || !this.visibleView || !this.visibleView.script || this.visibleView.script.sourceURL !== url)
                    this._showScriptOrResource(option.representedObject, {initialLoad: true});
            }
        }

        if (script.worldType === WebInspector.Script.WorldType.EXTENSIONS_WORLD)
            script.filesSelectOption.addStyleClass("extension-script");
    },

    _clearCurrentExecutionLine: function()
    {
        if (this._executionSourceFrame)
            this._executionSourceFrame.clearExecutionLocation();
        delete this._executionSourceFrame;
    },

    _callFrameSelected: function()
    {
        this._clearCurrentExecutionLine();

        var callStackPane = this.sidebarPanes.callstack;
        var currentFrame = callStackPane.selectedCallFrame;
        if (!currentFrame)
            return;

        this.sidebarPanes.scopechain.update(currentFrame);
        this.sidebarPanes.watchExpressions.refreshExpressions();

        var script = WebInspector.debuggerModel.scriptForSourceID(currentFrame.sourceID);
        var scriptOrResource = script.resource || script;
        this._showScriptOrResource(scriptOrResource, {line: currentFrame.line});

        this._executionSourceFrame = this._sourceFrameForScriptOrResource(scriptOrResource);
        if (this._executionSourceFrame)
            this._executionSourceFrame.setExecutionLocation(currentFrame.line, currentFrame.column);
    },

    _changeVisibleFile: function(event)
    {
        var select = this.filesSelectElement;
        this._showScriptOrResource(select.options[select.selectedIndex].representedObject);
    },

    _startSidebarResizeDrag: function(event)
    {
        WebInspector.elementDragStart(this.sidebarElement, this._sidebarResizeDrag.bind(this), this._endSidebarResizeDrag.bind(this), event, "col-resize");

        if (event.target === this.sidebarResizeWidgetElement)
            this._dragOffset = (event.target.offsetWidth - (event.pageX - event.target.totalOffsetLeft));
        else
            this._dragOffset = 0;
    },

    _endSidebarResizeDrag: function(event)
    {
        WebInspector.elementDragEnd(event);
        delete this._dragOffset;
        this.saveSidebarWidth();
    },

    _sidebarResizeDrag: function(event)
    {
        var x = event.pageX + this._dragOffset;
        var newWidth = Number.constrain(window.innerWidth - x, Preferences.minScriptsSidebarWidth, window.innerWidth * 0.66);
        this.setSidebarWidth(newWidth);
        event.preventDefault();
    },

    setSidebarWidth: function(newWidth)
    {
        this.sidebarElement.style.width = newWidth + "px";
        this.sidebarButtonsElement.style.width = newWidth + "px";
        this.viewsContainerElement.style.right = newWidth + "px";
        this.sidebarResizeWidgetElement.style.right = newWidth + "px";
        this.sidebarResizeElement.style.right = (newWidth - 3) + "px";

        this.resize();
    },

    _setPauseOnExceptions: function(pauseOnExceptionsState)
    {
        function callback(pauseOnExceptionsState)
        {
            if (pauseOnExceptionsState == WebInspector.ScriptsPanel.PauseOnExceptionsState.DontPauseOnExceptions)
                this._pauseOnExceptionButton.title = WebInspector.UIString("Don't pause on exceptions.\nClick to Pause on all exceptions.");
            else if (pauseOnExceptionsState == WebInspector.ScriptsPanel.PauseOnExceptionsState.PauseOnAllExceptions)
                this._pauseOnExceptionButton.title = WebInspector.UIString("Pause on all exceptions.\nClick to Pause on uncaught exceptions.");
            else if (pauseOnExceptionsState == WebInspector.ScriptsPanel.PauseOnExceptionsState.PauseOnUncaughtExceptions)
                this._pauseOnExceptionButton.title = WebInspector.UIString("Pause on uncaught exceptions.\nClick to Not pause on exceptions.");

            this._pauseOnExceptionButton.state = pauseOnExceptionsState;
            WebInspector.settings.pauseOnExceptionState = pauseOnExceptionsState;
        }
        InspectorBackend.setPauseOnExceptionsState(pauseOnExceptionsState, callback.bind(this));
    },

    _updateDebuggerButtons: function()
    {
        if (this._debuggerEnabled) {
            this.enableToggleButton.title = WebInspector.UIString("Debugging enabled. Click to disable.");
            this.enableToggleButton.toggled = true;
            this._pauseOnExceptionButton.visible = true;
            this.panelEnablerView.visible = false;
        } else {
            this.enableToggleButton.title = WebInspector.UIString("Debugging disabled. Click to enable.");
            this.enableToggleButton.toggled = false;
            this._pauseOnExceptionButton.visible = false;
            this.panelEnablerView.visible = true;
        }

        if (this._paused) {
            this.pauseButton.addStyleClass("paused");

            this.pauseButton.disabled = false;
            this.stepOverButton.disabled = false;
            this.stepIntoButton.disabled = false;
            this.stepOutButton.disabled = false;

            this.debuggerStatusElement.textContent = WebInspector.UIString("Paused");
        } else {
            this.pauseButton.removeStyleClass("paused");

            this.pauseButton.disabled = this._waitingToPause;
            this.stepOverButton.disabled = true;
            this.stepIntoButton.disabled = true;
            this.stepOutButton.disabled = true;

            if (this._waitingToPause)
                this.debuggerStatusElement.textContent = WebInspector.UIString("Pausing");
            else if (this._stepping)
                this.debuggerStatusElement.textContent = WebInspector.UIString("Stepping");
            else
                this.debuggerStatusElement.textContent = "";
        }
    },

    _updateBackAndForwardButtons: function()
    {
        this.backButton.disabled = this._currentBackForwardIndex <= 0;
        this.forwardButton.disabled = this._currentBackForwardIndex >= (this._backForwardList.length - 1);
    },

    _clearInterface: function()
    {
        this.sidebarPanes.callstack.update(null);
        this.sidebarPanes.scopechain.update(null);

        this._clearCurrentExecutionLine();
        this._updateDebuggerButtons();
    },

    _goBack: function()
    {
        if (this._currentBackForwardIndex <= 0) {
            console.error("Can't go back from index " + this._currentBackForwardIndex);
            return;
        }

        this._showScriptOrResource(this._backForwardList[--this._currentBackForwardIndex], {fromBackForwardAction: true});
        this._updateBackAndForwardButtons();
    },

    _goForward: function()
    {
        if (this._currentBackForwardIndex >= this._backForwardList.length - 1) {
            console.error("Can't go forward from index " + this._currentBackForwardIndex);
            return;
        }

        this._showScriptOrResource(this._backForwardList[++this._currentBackForwardIndex], {fromBackForwardAction: true});
        this._updateBackAndForwardButtons();
    },

    _formatScript: function()
    {
        if (this.visibleView)
            this.visibleView.formatSource();
    },

    _enableDebugging: function()
    {
        if (this._debuggerEnabled)
            return;
        this._toggleDebugging(this.panelEnablerView.alwaysEnabled);
    },

    _toggleDebugging: function(optionalAlways)
    {
        this._paused = false;
        this._waitingToPause = false;
        this._stepping = false;

        if (this._debuggerEnabled) {
            WebInspector.settings.debuggerEnabled = false;
            WebInspector.debuggerModel.disableDebugger();
        } else {
            WebInspector.settings.debuggerEnabled = !!optionalAlways;
            WebInspector.debuggerModel.enableDebugger();
        }
    },

    _togglePauseOnExceptions: function()
    {
        this._setPauseOnExceptions((this._pauseOnExceptionButton.state + 1) % this._pauseOnExceptionButton.states);
    },

    _togglePause: function()
    {
        if (this._paused) {
            this._paused = false;
            this._waitingToPause = false;
            InspectorBackend.resume();
        } else {
            this._stepping = false;
            this._waitingToPause = true;
            InspectorBackend.pause();
        }

        this._clearInterface();
    },

    _stepOverClicked: function()
    {
        this._paused = false;
        this._stepping = true;

        this._clearInterface();

        InspectorBackend.stepOver();
    },

    _stepIntoClicked: function()
    {
        this._paused = false;
        this._stepping = true;

        this._clearInterface();

        InspectorBackend.stepInto();
    },

    _stepOutClicked: function()
    {
        this._paused = false;
        this._stepping = true;

        this._clearInterface();

        InspectorBackend.stepOut();
    },

    toggleBreakpointsClicked: function()
    {
        this.toggleBreakpointsButton.toggled = !this.toggleBreakpointsButton.toggled;
        if (this.toggleBreakpointsButton.toggled) {
            InspectorBackend.activateBreakpoints();
            this.toggleBreakpointsButton.title = WebInspector.UIString("Deactivate all breakpoints.");
            document.getElementById("main-panels").removeStyleClass("breakpoints-deactivated");
        } else {
            InspectorBackend.deactivateBreakpoints();
            this.toggleBreakpointsButton.title = WebInspector.UIString("Activate all breakpoints.");
            document.getElementById("main-panels").addStyleClass("breakpoints-deactivated");
        }
    },

    elementsToRestoreScrollPositionsFor: function()
    {
        return [ this.sidebarElement ];
    },

    _registerShortcuts: function()
    {
        var section = WebInspector.shortcutsHelp.section(WebInspector.UIString("Scripts Panel"));
        var handler, shortcut1, shortcut2;
        var platformSpecificModifier = WebInspector.KeyboardShortcut.Modifiers.CtrlOrMeta;

        this._shortcuts = {};

        // Continue.
        handler = this.pauseButton.click.bind(this.pauseButton);
        shortcut1 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F8);
        this._shortcuts[shortcut1.key] = handler;
        shortcut2 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Slash, platformSpecificModifier);
        this._shortcuts[shortcut2.key] = handler;
        section.addAlternateKeys([ shortcut1.name, shortcut2.name ], WebInspector.UIString("Continue"));

        // Step over.
        handler = this.stepOverButton.click.bind(this.stepOverButton);
        shortcut1 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F10);
        this._shortcuts[shortcut1.key] = handler;
        shortcut2 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.SingleQuote, platformSpecificModifier);
        this._shortcuts[shortcut2.key] = handler;
        section.addAlternateKeys([ shortcut1.name, shortcut2.name ], WebInspector.UIString("Step over"));

        // Step into.
        handler = this.stepIntoButton.click.bind(this.stepIntoButton);
        shortcut1 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F11);
        this._shortcuts[shortcut1.key] = handler;
        shortcut2 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Semicolon, platformSpecificModifier);
        this._shortcuts[shortcut2.key] = handler;
        section.addAlternateKeys([ shortcut1.name, shortcut2.name ], WebInspector.UIString("Step into"));

        // Step out.
        handler = this.stepOutButton.click.bind(this.stepOutButton);
        shortcut1 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.F11, WebInspector.KeyboardShortcut.Modifiers.Shift);
        this._shortcuts[shortcut1.key] = handler;
        shortcut2 = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Semicolon, WebInspector.KeyboardShortcut.Modifiers.Shift, platformSpecificModifier);
        this._shortcuts[shortcut2.key] = handler;
        section.addAlternateKeys([ shortcut1.name, shortcut2.name ], WebInspector.UIString("Step out"));

        var isMac = WebInspector.isMac();
        if (isMac)
            shortcut1 = WebInspector.KeyboardShortcut.makeDescriptor("l", WebInspector.KeyboardShortcut.Modifiers.Meta);
        else
            shortcut1 = WebInspector.KeyboardShortcut.makeDescriptor("g", WebInspector.KeyboardShortcut.Modifiers.Ctrl);
        this._shortcuts[shortcut1.key] = this.showGoToLineDialog.bind(this);
        section.addAlternateKeys([ shortcut1.name ], WebInspector.UIString("Go to Line"));
        this.sidebarPanes.callstack.registerShortcuts(section);
    },

    searchCanceled: function()
    {
        WebInspector.updateSearchMatchesCount(0, this);

        if (this._searchView)
            this._searchView.searchCanceled();

        delete this._searchView;
        delete this._searchQuery;
    },

    performSearch: function(query)
    {
        if (!this.visibleView)
            return;

        // Call searchCanceled since it will reset everything we need before doing a new search.
        this.searchCanceled();

        this._searchView = this.visibleView;
        this._searchQuery = query;

        function finishedCallback(view, searchMatches)
        {
            if (!searchMatches)
                return;

            WebInspector.updateSearchMatchesCount(searchMatches, this);
            view.jumpToFirstSearchResult();
        }

        this._searchView.performSearch(query, finishedCallback.bind(this));
    },

    jumpToNextSearchResult: function()
    {
        if (!this._searchView)
            return;

        if (this._searchView !== this.visibleView) {
            this.performSearch(this._searchQuery);
            return;
        }

        if (this._searchView.showingLastSearchResult())
            this._searchView.jumpToFirstSearchResult();
        else
            this._searchView.jumpToNextSearchResult();
    },

    jumpToPreviousSearchResult: function()
    {
        if (!this._searchView)
            return;

        if (this._searchView !== this.visibleView) {
            this.performSearch(this._searchQuery);
            if (this._searchView)
                this._searchView.jumpToLastSearchResult();
            return;
        }

        if (this._searchView.showingFirstSearchResult())
            this._searchView.jumpToLastSearchResult();
        else
            this._searchView.jumpToPreviousSearchResult();
    },

    showGoToLineDialog: function(e)
    {
         var view = this.visibleView;
         if (view)
             WebInspector.GoToLineDialog.show(view);
    }
}

WebInspector.ScriptsPanel.prototype.__proto__ = WebInspector.Panel.prototype;


WebInspector.SourceFrameContentProviderForScript = function(script)
{
    WebInspector.SourceFrameContentProvider.call(this);
    this._script = script;
}

WebInspector.SourceFrameContentProviderForScript.prototype = {
    requestContent: function(callback)
    {
        function didRequestSource(source)
        {
            if (source) {
                var prefix = "";
                for (var i = 0; i < this._script.lineOffset; ++i)
                    prefix += "\n";
                source = prefix + source;
            } else
                source = WebInspector.UIString("<source is not available>");

            var sourceMapping = new WebInspector.IdenticalSourceMapping();
            var scriptRanges = WebInspector.ScriptFormatter.findScriptRanges(source.lineEndings(), [this._script]);
            callback("text/javascript", new WebInspector.SourceFrameContent(source, sourceMapping, scriptRanges));
        }
        this._script.requestSource(didRequestSource.bind(this));
    }
}

WebInspector.SourceFrameContentProviderForScript.prototype.__proto__ = WebInspector.SourceFrameContentProvider.prototype;
