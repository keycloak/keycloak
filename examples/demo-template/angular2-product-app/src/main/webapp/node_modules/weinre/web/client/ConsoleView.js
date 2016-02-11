/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2009 Joseph Pecoraro
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

var ExpressionStopCharacters = " =:[({;,!+-*/&|^<>";

WebInspector.ConsoleView = function(drawer)
{
    WebInspector.View.call(this, document.getElementById("console-view"));

    this.messages = [];
    this.drawer = drawer;

    this.clearButton = document.getElementById("clear-console-status-bar-item");
    this.clearButton.title = WebInspector.UIString("Clear console log.");
    this.clearButton.addEventListener("click", this._clearButtonClicked.bind(this), false);

    this.messagesElement = document.getElementById("console-messages");
    this.messagesElement.addEventListener("selectstart", this._messagesSelectStart.bind(this), false);
    this.messagesElement.addEventListener("click", this._messagesClicked.bind(this), true);

    this.promptElement = document.getElementById("console-prompt");
    this.promptElement.setAttribute("contenteditable", "true");
    this.promptElement.className = "source-code";
    this.promptElement.addEventListener("keydown", this._promptKeyDown.bind(this), true);
    this.prompt = new WebInspector.TextPrompt(this.promptElement, this.completions.bind(this), ExpressionStopCharacters + ".");
    this.prompt.history = WebInspector.settings.consoleHistory;

    this.topGroup = new WebInspector.ConsoleGroup(null);
    this.messagesElement.insertBefore(this.topGroup.element, this.promptElement);
    this.currentGroup = this.topGroup;

    this.toggleConsoleButton = document.getElementById("console-status-bar-item");
    this.toggleConsoleButton.title = WebInspector.UIString("Show console.");
    this.toggleConsoleButton.addEventListener("click", this._toggleConsoleButtonClicked.bind(this), false);

    // Will hold the list of filter elements
    this.filterBarElement = document.getElementById("console-filter");

    function createDividerElement() {
        var dividerElement = document.createElement("div");
        dividerElement.addStyleClass("scope-bar-divider");
        this.filterBarElement.appendChild(dividerElement);
    }

    var updateFilterHandler = this._updateFilter.bind(this);
    function createFilterElement(category, label) {
        var categoryElement = document.createElement("li");
        categoryElement.category = category;
        categoryElement.className = category;
        categoryElement.addEventListener("click", updateFilterHandler, false);
        categoryElement.textContent = label;

        this.filterBarElement.appendChild(categoryElement);

        return categoryElement;
    }

    this.allElement = createFilterElement.call(this, "all", WebInspector.UIString("All"));
    createDividerElement.call(this);
    this.errorElement = createFilterElement.call(this, "errors", WebInspector.UIString("Errors"));
    this.warningElement = createFilterElement.call(this, "warnings", WebInspector.UIString("Warnings"));
    this.logElement = createFilterElement.call(this, "logs", WebInspector.UIString("Logs"));

    this.filter(this.allElement, false);
    this._registerShortcuts();

    this.messagesElement.addEventListener("contextmenu", this._handleContextMenuEvent.bind(this), false);

    this._customFormatters = {
        "object": this._formatobject,
        "array":  this._formatarray,
        "node":   this._formatnode,
        "string": this._formatstring
    };

    this._registerConsoleDomainDispatcher();
}

WebInspector.ConsoleView.prototype = {
    _registerConsoleDomainDispatcher: function() {
        var console = this;
        var dispatcher = {
            addConsoleMessage: function(payload)
            {
                var consoleMessage = new WebInspector.ConsoleMessage(
                    payload.source,
                    payload.type,
                    payload.level,
                    payload.line,
                    payload.url,
                    payload.repeatCount,
                    payload.message,
                    payload.parameters,
                    payload.stackTrace,
                    payload.requestId);
                console.addMessage(consoleMessage);
            },

            updateConsoleMessageExpiredCount: function(count)
            {
                var message = String.sprintf(WebInspector.UIString("%d console messages are not shown."), count);
                console.addMessage(WebInspector.ConsoleMessage.createTextMessage(message, WebInspector.ConsoleMessage.MessageLevel.Warning));
            },

            updateConsoleMessageRepeatCount: function(count)
            {
                var msg = console.previousMessage;
                var prevRepeatCount = msg.totalRepeatCount;

                if (!console.commandSincePreviousMessage) {
                    msg.repeatDelta = count - prevRepeatCount;
                    msg.repeatCount = msg.repeatCount + msg.repeatDelta;
                    msg.totalRepeatCount = count;
                    msg._updateRepeatCount();
                    console._incrementErrorWarningCount(msg);
                } else {
                    var msgCopy = new WebInspector.ConsoleMessage(msg.source, msg.type, msg.level, msg.line, msg.url, count - prevRepeatCount, msg._messageText, msg._parameters, msg._stackTrace, msg._requestId);
                    msgCopy.totalRepeatCount = count;
                    msgCopy._formatMessage();
                    console.addMessage(msgCopy);
                }
            },

            consoleMessagesCleared: function()
            {
                console.clearMessages();
            },
        }
        InspectorBackend.registerDomainDispatcher("Console", dispatcher);
    },

    _updateFilter: function(e)
    {
        var isMac = WebInspector.isMac();
        var selectMultiple = false;
        if (isMac && e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey)
            selectMultiple = true;
        if (!isMac && e.ctrlKey && !e.metaKey && !e.altKey && !e.shiftKey)
            selectMultiple = true;

        this.filter(e.target, selectMultiple);
    },

    filter: function(target, selectMultiple)
    {
        function unselectAll()
        {
            this.allElement.removeStyleClass("selected");
            this.errorElement.removeStyleClass("selected");
            this.warningElement.removeStyleClass("selected");
            this.logElement.removeStyleClass("selected");

            this.messagesElement.removeStyleClass("filter-all");
            this.messagesElement.removeStyleClass("filter-errors");
            this.messagesElement.removeStyleClass("filter-warnings");
            this.messagesElement.removeStyleClass("filter-logs");
        }

        var targetFilterClass = "filter-" + target.category;

        if (target.category === "all") {
            if (target.hasStyleClass("selected")) {
                // We can't unselect all, so we break early here
                return;
            }

            unselectAll.call(this);
        } else {
            // Something other than all is being selected, so we want to unselect all
            if (this.allElement.hasStyleClass("selected")) {
                this.allElement.removeStyleClass("selected");
                this.messagesElement.removeStyleClass("filter-all");
            }
        }

        if (!selectMultiple) {
            // If multiple selection is off, we want to unselect everything else
            // and just select ourselves.
            unselectAll.call(this);

            target.addStyleClass("selected");
            this.messagesElement.addStyleClass(targetFilterClass);

            return;
        }

        if (target.hasStyleClass("selected")) {
            // If selectMultiple is turned on, and we were selected, we just
            // want to unselect ourselves.
            target.removeStyleClass("selected");
            this.messagesElement.removeStyleClass(targetFilterClass);
        } else {
            // If selectMultiple is turned on, and we weren't selected, we just
            // want to select ourselves.
            target.addStyleClass("selected");
            this.messagesElement.addStyleClass(targetFilterClass);
        }
    },

    _toggleConsoleButtonClicked: function()
    {
        this.drawer.visibleView = this;
    },

    attach: function(mainElement, statusBarElement)
    {
        mainElement.appendChild(this.element);
        statusBarElement.appendChild(this.clearButton);
        statusBarElement.appendChild(this.filterBarElement);
    },

    show: function()
    {
        this.toggleConsoleButton.addStyleClass("toggled-on");
        this.toggleConsoleButton.title = WebInspector.UIString("Hide console.");
        if (!this.prompt.isCaretInsidePrompt())
            this.prompt.moveCaretToEndOfPrompt();
    },

    afterShow: function()
    {
        WebInspector.currentFocusElement = this.promptElement;
    },

    hide: function()
    {
        this.toggleConsoleButton.removeStyleClass("toggled-on");
        this.toggleConsoleButton.title = WebInspector.UIString("Show console.");
    },

    _scheduleScrollIntoView: function()
    {
        if (this._scrollIntoViewTimer)
            return;

        function scrollIntoView()
        {
            this.promptElement.scrollIntoView(true);
            delete this._scrollIntoViewTimer;
        }
        this._scrollIntoViewTimer = setTimeout(scrollIntoView.bind(this), 20);
    },

    addMessage: function(msg)
    {
        var shouldScrollToLastMessage = this.messagesElement.isScrolledToBottom();

        if (msg instanceof WebInspector.ConsoleMessage && !(msg instanceof WebInspector.ConsoleCommandResult)) {
            this._incrementErrorWarningCount(msg);
            WebInspector.resourceTreeModel.addConsoleMessage(msg);
            WebInspector.panels.scripts.addConsoleMessage(msg);
            this.commandSincePreviousMessage = false;
            this.previousMessage = msg;
        } else if (msg instanceof WebInspector.ConsoleCommand) {
            if (this.previousMessage) {
                this.commandSincePreviousMessage = true;
            }
        }

        this.messages.push(msg);

        if (msg.type === WebInspector.ConsoleMessage.MessageType.EndGroup) {
            var parentGroup = this.currentGroup.parentGroup
            if (parentGroup)
                this.currentGroup = parentGroup;
        } else {
            if (msg.type === WebInspector.ConsoleMessage.MessageType.StartGroup || msg.type === WebInspector.ConsoleMessage.MessageType.StartGroupCollapsed) {
                var group = new WebInspector.ConsoleGroup(this.currentGroup);
                this.currentGroup.messagesElement.appendChild(group.element);
                this.currentGroup = group;
            }

            this.currentGroup.addMessage(msg);
        }

        if (shouldScrollToLastMessage)
            this._scheduleScrollIntoView();
    },

    _incrementErrorWarningCount: function(msg)
    {
        switch (msg.level) {
            case WebInspector.ConsoleMessage.MessageLevel.Warning:
                WebInspector.warnings += msg.repeatDelta;
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Error:
                WebInspector.errors += msg.repeatDelta;
                break;
        }
    },

    requestClearMessages: function()
    {
        InspectorBackend.clearConsoleMessages();
    },

    clearMessages: function()
    {
        WebInspector.resourceTreeModel.clearConsoleMessages();
        WebInspector.panels.scripts.clearConsoleMessages();

        this.messages = [];

        this.currentGroup = this.topGroup;
        this.topGroup.messagesElement.removeChildren();

        WebInspector.errors = 0;
        WebInspector.warnings = 0;

        delete this.commandSincePreviousMessage;
        delete this.previousMessage;
    },

    completions: function(wordRange, bestMatchOnly, completionsReadyCallback)
    {
        // Pass less stop characters to rangeOfWord so the range will be a more complete expression.
        var expressionRange = wordRange.startContainer.rangeOfWord(wordRange.startOffset, ExpressionStopCharacters, this.promptElement, "backward");
        var expressionString = expressionRange.toString();
        var lastIndex = expressionString.length - 1;

        var dotNotation = (expressionString[lastIndex] === ".");
        var bracketNotation = (expressionString[lastIndex] === "[");

        if (dotNotation || bracketNotation)
            expressionString = expressionString.substr(0, lastIndex);

        var prefix = wordRange.toString();
        if (!expressionString && !prefix)
            return;

        var reportCompletions = this._reportCompletions.bind(this, bestMatchOnly, completionsReadyCallback, dotNotation, bracketNotation, prefix);
        // Collect comma separated object properties for the completion.

        var includeCommandLineAPI = (!dotNotation && !bracketNotation);
        var injectedScriptAccess;
        if (WebInspector.panels.scripts && WebInspector.panels.scripts.paused)
            InspectorBackend.getCompletionsOnCallFrame(WebInspector.panels.scripts.selectedCallFrameId(), expressionString, includeCommandLineAPI, reportCompletions);
        else
            InspectorBackend.getCompletions(expressionString, includeCommandLineAPI, reportCompletions);
    },

    _reportCompletions: function(bestMatchOnly, completionsReadyCallback, dotNotation, bracketNotation, prefix, result, isException) {
        if (isException)
            return;

        if (bracketNotation) {
            if (prefix.length && prefix[0] === "'")
                var quoteUsed = "'";
            else
                var quoteUsed = "\"";
        }

        var results = [];
        var properties = Object.keys(result).sort();

        for (var i = 0; i < properties.length; ++i) {
            var property = properties[i];

            if (dotNotation && !/^[a-zA-Z_$][a-zA-Z0-9_$]*$/.test(property))
                continue;

            if (bracketNotation) {
                if (!/^[0-9]+$/.test(property))
                    property = quoteUsed + property.escapeCharacters(quoteUsed + "\\") + quoteUsed;
                property += "]";
            }

            if (property.length < prefix.length)
                continue;
            if (property.indexOf(prefix) !== 0)
                continue;

            results.push(property);
            if (bestMatchOnly)
                break;
        }
        completionsReadyCallback(results);
    },

    _clearButtonClicked: function()
    {
        this.requestClearMessages();
    },

    _handleContextMenuEvent: function(event)
    {
        if (!window.getSelection().isCollapsed) {
            // If there is a selection, we want to show our normal context menu
            // (with Copy, etc.), and not Clear Console.
            return;
        }

        var itemAction = function () {
            WebInspector.settings.monitoringXHREnabled = !WebInspector.settings.monitoringXHREnabled;
            InspectorBackend.setMonitoringXHREnabled(WebInspector.settings.monitoringXHREnabled);
        }.bind(this);
        var contextMenu = new WebInspector.ContextMenu();
        contextMenu.appendCheckboxItem(WebInspector.UIString("XMLHttpRequest logging"), itemAction, WebInspector.settings.monitoringXHREnabled)
        contextMenu.appendItem(WebInspector.UIString("Clear Console"), this.requestClearMessages.bind(this));
        contextMenu.show(event);
    },

    _messagesSelectStart: function(event)
    {
        if (this._selectionTimeout)
            clearTimeout(this._selectionTimeout);

        this.prompt.clearAutoComplete();

        function moveBackIfOutside()
        {
            delete this._selectionTimeout;
            if (!this.prompt.isCaretInsidePrompt() && window.getSelection().isCollapsed)
                this.prompt.moveCaretToEndOfPrompt();
            this.prompt.autoCompleteSoon();
        }

        this._selectionTimeout = setTimeout(moveBackIfOutside.bind(this), 100);
    },

    _messagesClicked: function(event)
    {
        var link = event.target.enclosingNodeOrSelfWithNodeName("a");
        if (!link || !link.representedNode)
            return;

        WebInspector.updateFocusedNode(link.representedNode.id);
        event.stopPropagation();
        event.preventDefault();
    },

    _registerShortcuts: function()
    {
        this._shortcuts = {};

        var shortcut = WebInspector.KeyboardShortcut;
        var shortcutK = shortcut.makeDescriptor("k", WebInspector.KeyboardShortcut.Modifiers.Meta);
        // This case requires a separate bound function as its isMacOnly property should not be shared among different shortcut handlers.
        this._shortcuts[shortcutK.key] = this.requestClearMessages.bind(this);
        this._shortcuts[shortcutK.key].isMacOnly = true;

        var clearConsoleHandler = this.requestClearMessages.bind(this);
        var shortcutL = shortcut.makeDescriptor("l", WebInspector.KeyboardShortcut.Modifiers.Ctrl);
        this._shortcuts[shortcutL.key] = clearConsoleHandler;

        var section = WebInspector.shortcutsHelp.section(WebInspector.UIString("Console"));
        var keys = WebInspector.isMac() ? [ shortcutK.name, shortcutL.name ] : [ shortcutL.name ];
        section.addAlternateKeys(keys, WebInspector.UIString("Clear Console"));

        keys = [
            shortcut.shortcutToString(shortcut.Keys.Tab),
            shortcut.shortcutToString(shortcut.Keys.Tab, shortcut.Modifiers.Shift)
        ];
        section.addRelatedKeys(keys, WebInspector.UIString("Next/previous suggestion"));
        section.addKey(shortcut.shortcutToString(shortcut.Keys.Right), WebInspector.UIString("Accept suggestion"));
        keys = [
            shortcut.shortcutToString(shortcut.Keys.Down),
            shortcut.shortcutToString(shortcut.Keys.Up)
        ];
        section.addRelatedKeys(keys, WebInspector.UIString("Next/previous line"));
        keys = [
            shortcut.shortcutToString("N", shortcut.Modifiers.Alt),
            shortcut.shortcutToString("P", shortcut.Modifiers.Alt)
        ];
        if (WebInspector.isMac())
            section.addRelatedKeys(keys, WebInspector.UIString("Next/previous command"));
        section.addKey(shortcut.shortcutToString(shortcut.Keys.Enter), WebInspector.UIString("Execute command"));
    },

    _promptKeyDown: function(event)
    {
        if (isEnterKey(event)) {
            this._enterKeyPressed(event);
            return;
        }

        var shortcut = WebInspector.KeyboardShortcut.makeKeyFromEvent(event);
        var handler = this._shortcuts[shortcut];
        if (handler) {
            if (!this._shortcuts[shortcut].isMacOnly || WebInspector.isMac()) {
                handler();
                event.preventDefault();
                return;
            }
        }
    },

    evalInInspectedWindow: function(expression, objectGroup, includeCommandLineAPI, callback)
    {
        if (WebInspector.panels.scripts && WebInspector.panels.scripts.paused) {
            WebInspector.panels.scripts.evaluateInSelectedCallFrame(expression, false, objectGroup, includeCommandLineAPI, callback);
            return;
        }

        if (!expression) {
            // There is no expression, so the completion should happen against global properties.
            expression = "this";
        }

        function evalCallback(result)
        {
            callback(WebInspector.RemoteObject.fromPayload(result));
        }
        InspectorBackend.evaluate(expression, objectGroup, includeCommandLineAPI, evalCallback);
    },

    _enterKeyPressed: function(event)
    {
        if (event.altKey || event.ctrlKey || event.shiftKey)
            return;

        event.preventDefault();
        event.stopPropagation();

        this.prompt.clearAutoComplete(true);

        var str = this.prompt.text;
        if (!str.length)
            return;

        var commandMessage = new WebInspector.ConsoleCommand(str);
        this.addMessage(commandMessage);

        var self = this;
        function printResult(result)
        {
            self.prompt.history.push(str);
            self.prompt.historyOffset = 0;
            self.prompt.text = "";

            WebInspector.settings.consoleHistory = self.prompt.history.slice(-30);

            self.addMessage(new WebInspector.ConsoleCommandResult(result, commandMessage));
        }
        this.evalInInspectedWindow(str, "console", true, printResult);
    },

    _format: function(output, forceObjectFormat)
    {
        var isProxy = (output != null && typeof output === "object");
        var type = (forceObjectFormat ? "object" : WebInspector.RemoteObject.type(output));

        var formatter = this._customFormatters[type];
        if (!formatter || !isProxy) {
            formatter = this._formatvalue;
            output = output.description;
        }

        var span = document.createElement("span");
        span.className = "console-formatted-" + type + " source-code";
        formatter.call(this, output, span);
        return span;
    },

    _formatvalue: function(val, elem)
    {
        elem.appendChild(document.createTextNode(val));
    },

    _formatobject: function(obj, elem)
    {
        elem.appendChild(new WebInspector.ObjectPropertiesSection(obj, obj.description, null, true).element);
    },

    _formatnode: function(object, elem)
    {
        function printNode(nodeId)
        {
            if (!nodeId) {
                // Sometimes DOM is loaded after the sync message is being formatted, so we get no
                // nodeId here. So we fall back to object formatting here.
                this._formatobject(object, elem);
                return;
            }
            var treeOutline = new WebInspector.ElementsTreeOutline();
            treeOutline.showInElementsPanelEnabled = true;
            treeOutline.rootDOMNode = WebInspector.domAgent.nodeForId(nodeId);
            treeOutline.element.addStyleClass("outline-disclosure");
            if (!treeOutline.children[0].hasChildren)
                treeOutline.element.addStyleClass("single-node");
            elem.appendChild(treeOutline.element);
        }
        object.pushNodeToFrontend(printNode.bind(this));
    },

    _formatarray: function(arr, elem)
    {
        arr.getOwnProperties(false, this._printArray.bind(this, elem));
    },

    _formatstring: function(output, elem)
    {
        var span = document.createElement("span");
        span.className = "console-formatted-string source-code";
        span.appendChild(WebInspector.linkifyStringAsFragment(output.description));

        // Make black quotes.
        elem.removeStyleClass("console-formatted-string");
        elem.appendChild(document.createTextNode("\""));
        elem.appendChild(span);
        elem.appendChild(document.createTextNode("\""));
    },

    _printArray: function(elem, properties)
    {
        if (!properties)
            return;

        var elements = [];
        for (var i = 0; i < properties.length; ++i) {
            var name = properties[i].name;
            if (name == parseInt(name))
                elements[name] = this._formatAsArrayEntry(properties[i].value);
        }

        elem.appendChild(document.createTextNode("["));
        for (var i = 0; i < elements.length; ++i) {
            var element = elements[i];
            if (element)
                elem.appendChild(element);
            else
                elem.appendChild(document.createTextNode("undefined"))
            if (i < elements.length - 1)
                elem.appendChild(document.createTextNode(", "));
        }
        elem.appendChild(document.createTextNode("]"));
    },

    _formatAsArrayEntry: function(output)
    {
        // Prevent infinite expansion of cross-referencing arrays.
        return this._format(output, WebInspector.RemoteObject.type(output) === "array");
    }
}

WebInspector.ConsoleView.prototype.__proto__ = WebInspector.View.prototype;

WebInspector.ConsoleMessage = function(source, type, level, line, url, repeatCount, message, parameters, stackTrace, requestId)
{
    this.source = source;
    this.type = type;
    this.level = level;
    this.line = line;
    this.url = url;
    this.repeatCount = repeatCount;
    this.repeatDelta = repeatCount;
    this.totalRepeatCount = repeatCount;
    this._messageText = message;
    this._parameters = parameters;
    this._stackTrace = stackTrace;
    this._requestId = requestId;

    if (stackTrace && stackTrace.length) {
        var topCallFrame = stackTrace[0];
        if (!this.url)
            this.url = topCallFrame.scriptName;
        if (!this.line)
            this.line = topCallFrame.lineNumber;
    }

    this._formatMessage();
}

WebInspector.ConsoleMessage.createTextMessage = function(text, level)
{
    level = level || WebInspector.ConsoleMessage.MessageLevel.Log;
    return new WebInspector.ConsoleMessage(WebInspector.ConsoleMessage.MessageSource.JS, WebInspector.ConsoleMessage.MessageType.Log, level, 0, null, 1, null, [text], null);
}

WebInspector.ConsoleMessage.prototype = {
    _formatMessage: function()
    {
        var stackTrace = this._stackTrace;
        var messageText;
        switch (this.type) {
            case WebInspector.ConsoleMessage.MessageType.Trace:
                messageText = document.createTextNode("console.trace()");
                break;
            case WebInspector.ConsoleMessage.MessageType.UncaughtException:
                messageText = document.createTextNode(this._messageText);
                break;
            case WebInspector.ConsoleMessage.MessageType.NetworkError:
                var resource = this._requestId && WebInspector.networkResourceById(this._requestId);
                if (resource) {
                    stackTrace = resource.stackTrace;

                    messageText = document.createElement("span");
                    messageText.appendChild(document.createTextNode(resource.requestMethod + " "));
                    messageText.appendChild(WebInspector.linkifyURLAsNode(resource.url));
                    if (resource.failed)
                        messageText.appendChild(document.createTextNode(" " + resource.localizedFailDescription));
                    else
                        messageText.appendChild(document.createTextNode(" " + resource.statusCode + " (" + resource.statusText + ")"));
                } else
                    messageText = this._format([this._messageText]);
                break;
            case WebInspector.ConsoleMessage.MessageType.Assert:
                var args = [WebInspector.UIString("Assertion failed:")];
                if (this._parameters)
                    args = args.concat(this._parameters);
                messageText = this._format(args);
                break;
            case WebInspector.ConsoleMessage.MessageType.Object:
                var obj = this._parameters ? this._parameters[0] : undefined;
                var args = ["%O", obj];
                messageText = this._format(args);
                break;
            default:
                var args = this._parameters || [this._messageText];
                messageText = this._format(args);
                break;
        }

        this._formattedMessage = document.createElement("span");
        this._formattedMessage.className = "console-message-text source-code";

        if (this.url && this.url !== "undefined") {
            var urlElement = WebInspector.linkifyResourceAsNode(this.url, "scripts", this.line, "console-message-url");
            this._formattedMessage.appendChild(urlElement);
        }

        this._formattedMessage.appendChild(messageText);

        if (this._stackTrace) {
            switch (this.type) {
                case WebInspector.ConsoleMessage.MessageType.Trace:
                case WebInspector.ConsoleMessage.MessageType.UncaughtException:
                case WebInspector.ConsoleMessage.MessageType.NetworkError:
                case WebInspector.ConsoleMessage.MessageType.Assert: {
                    var ol = document.createElement("ol");
                    ol.className = "outline-disclosure";
                    var treeOutline = new TreeOutline(ol);

                    var content = this._formattedMessage;
                    var root = new TreeElement(content, null, true);
                    content.treeElementForTest = root;
                    treeOutline.appendChild(root);
                    if (this.type === WebInspector.ConsoleMessage.MessageType.Trace)
                        root.expand();

                    this._populateStackTraceTreeElement(root);
                    this._formattedMessage = ol;
                }
            }
        }

        // This is used for inline message bubbles in SourceFrames, or other plain-text representations.
        this.message = this._formattedMessage.textContent;
    },

    isErrorOrWarning: function()
    {
        return (this.level === WebInspector.ConsoleMessage.MessageLevel.Warning || this.level === WebInspector.ConsoleMessage.MessageLevel.Error);
    },

    _format: function(parameters)
    {
        // This node is used like a Builder. Values are continually appended onto it.
        var formattedResult = document.createElement("span");
        if (!parameters.length)
            return formattedResult;

        // Formatting code below assumes that parameters are all wrappers whereas frontend console
        // API allows passing arbitrary values as messages (strings, numbers, etc.). Wrap them here.
        for (var i = 0; i < parameters.length; ++i) {
            if (typeof parameters[i] === "object")
                parameters[i] = WebInspector.RemoteObject.fromPayload(parameters[i]);
            else
                parameters[i] = WebInspector.RemoteObject.fromPrimitiveValue(parameters[i]);
        }

        // There can be string log and string eval result. We distinguish between them based on message type.
        var shouldFormatMessage = WebInspector.RemoteObject.type(parameters[0]) === "string" && this.type !== WebInspector.ConsoleMessage.MessageType.Result;

        // Multiple parameters with the first being a format string. Save unused substitutions.
        if (shouldFormatMessage) {
            // Multiple parameters with the first being a format string. Save unused substitutions.
            var result = this._formatWithSubstitutionString(parameters, formattedResult);
            parameters = result.unusedSubstitutions;
            if (parameters.length)
                formattedResult.appendChild(document.createTextNode(" "));
        }

        // Single parameter, or unused substitutions from above.
        for (var i = 0; i < parameters.length; ++i) {
            // Inline strings when formatting.
            if (shouldFormatMessage && parameters[i].type === "string")
                formattedResult.appendChild(document.createTextNode(parameters[i].description));
            else
                formattedResult.appendChild(WebInspector.console._format(parameters[i]));
            if (i < parameters.length - 1)
                formattedResult.appendChild(document.createTextNode(" "));
        }
        return formattedResult;
    },

    _formatWithSubstitutionString: function(parameters, formattedResult)
    {
        var formatters = {}
        for (var i in String.standardFormatters)
            formatters[i] = String.standardFormatters[i];

        function consoleFormatWrapper(force)
        {
            return function(obj) {
                return WebInspector.console._format(obj, force);
            };
        }

        // Firebug uses %o for formatting objects.
        formatters.o = consoleFormatWrapper();
        // Firebug allows both %i and %d for formatting integers.
        formatters.i = formatters.d;
        // Support %O to force object formatting, instead of the type-based %o formatting.
        formatters.O = consoleFormatWrapper(true);

        function append(a, b)
        {
            if (!(b instanceof Node))
                a.appendChild(WebInspector.linkifyStringAsFragment(b.toString()));
            else
                a.appendChild(b);
            return a;
        }

        // String.format does treat formattedResult like a Builder, result is an object.
        return String.format(parameters[0].description, parameters.slice(1), formatters, formattedResult, append);
    },

    toMessageElement: function()
    {
        if (this._element)
            return this._element;

        var element = document.createElement("div");
        element.message = this;
        element.className = "console-message";

        this._element = element;

        switch (this.level) {
            case WebInspector.ConsoleMessage.MessageLevel.Tip:
                element.addStyleClass("console-tip-level");
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Log:
                element.addStyleClass("console-log-level");
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Debug:
                element.addStyleClass("console-debug-level");
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Warning:
                element.addStyleClass("console-warning-level");
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Error:
                element.addStyleClass("console-error-level");
                break;
        }

        if (this.type === WebInspector.ConsoleMessage.MessageType.StartGroup || this.type === WebInspector.ConsoleMessage.MessageType.StartGroupCollapsed)
            element.addStyleClass("console-group-title");

        if (this.elementsTreeOutline) {
            element.addStyleClass("outline-disclosure");
            element.appendChild(this.elementsTreeOutline.element);
            return element;
        }

        element.appendChild(this._formattedMessage);

        if (this.repeatCount > 1)
            this._updateRepeatCount();

        return element;
    },

    _populateStackTraceTreeElement: function(parentTreeElement)
    {
        for (var i = 0; i < this._stackTrace.length; i++) {
            var frame = this._stackTrace[i];

            var content = document.createElement("div");
            var messageTextElement = document.createElement("span");
            messageTextElement.className = "console-message-text source-code";
            var functionName = frame.functionName || WebInspector.UIString("(anonymous function)");
            messageTextElement.appendChild(document.createTextNode(functionName));
            content.appendChild(messageTextElement);

            var urlElement = WebInspector.linkifyResourceAsNode(frame.scriptName, "scripts", frame.lineNumber, "console-message-url");
            content.appendChild(urlElement);

            var treeElement = new TreeElement(content);
            parentTreeElement.appendChild(treeElement);
        }
    },

    _updateRepeatCount: function() {
        if (!this.repeatCountElement) {
            this.repeatCountElement = document.createElement("span");
            this.repeatCountElement.className = "bubble";

            this._element.insertBefore(this.repeatCountElement, this._element.firstChild);
            this._element.addStyleClass("repeated-message");
        }
        this.repeatCountElement.textContent = this.repeatCount;
    },

    toString: function()
    {
        var sourceString;
        switch (this.source) {
            case WebInspector.ConsoleMessage.MessageSource.HTML:
                sourceString = "HTML";
                break;
            case WebInspector.ConsoleMessage.MessageSource.WML:
                sourceString = "WML";
                break;
            case WebInspector.ConsoleMessage.MessageSource.XML:
                sourceString = "XML";
                break;
            case WebInspector.ConsoleMessage.MessageSource.JS:
                sourceString = "JS";
                break;
            case WebInspector.ConsoleMessage.MessageSource.CSS:
                sourceString = "CSS";
                break;
            case WebInspector.ConsoleMessage.MessageSource.Other:
                sourceString = "Other";
                break;
        }

        var typeString;
        switch (this.type) {
            case WebInspector.ConsoleMessage.MessageType.Log:
            case WebInspector.ConsoleMessage.MessageType.UncaughtException:
            case WebInspector.ConsoleMessage.MessageType.NetworkError:
                typeString = "Log";
                break;
            case WebInspector.ConsoleMessage.MessageType.Object:
                typeString = "Object";
                break;
            case WebInspector.ConsoleMessage.MessageType.Trace:
                typeString = "Trace";
                break;
            case WebInspector.ConsoleMessage.MessageType.StartGroupCollapsed:
            case WebInspector.ConsoleMessage.MessageType.StartGroup:
                typeString = "Start Group";
                break;
            case WebInspector.ConsoleMessage.MessageType.EndGroup:
                typeString = "End Group";
                break;
            case WebInspector.ConsoleMessage.MessageType.Assert:
                typeString = "Assert";
                break;
            case WebInspector.ConsoleMessage.MessageType.Result:
                typeString = "Result";
                break;
        }

        var levelString;
        switch (this.level) {
            case WebInspector.ConsoleMessage.MessageLevel.Tip:
                levelString = "Tip";
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Log:
                levelString = "Log";
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Warning:
                levelString = "Warning";
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Debug:
                levelString = "Debug";
                break;
            case WebInspector.ConsoleMessage.MessageLevel.Error:
                levelString = "Error";
                break;
        }

        return sourceString + " " + typeString + " " + levelString + ": " + this._formattedMessage.textContent + "\n" + this.url + " line " + this.line;
    },

    isEqual: function(msg)
    {
        if (!msg)
            return false;

        if (this._stackTrace) {
            if (!msg._stackTrace)
                return false;
            var l = this._stackTrace;
            var r = msg._stackTrace;
            for (var i = 0; i < l.length; i++) {
                if (l[i].scriptName !== r[i].scriptName ||
                    l[i].functionName !== r[i].functionName ||
                    l[i].lineNumber !== r[i].lineNumber ||
                    l[i].column !== r[i].column)
                    return false;
            }
        }

        return (this.source === msg.source)
            && (this.type === msg.type)
            && (this.level === msg.level)
            && (this.line === msg.line)
            && (this.url === msg.url)
            && (this.message === msg.message)
            && (this._requestId === msg._requestId);
    }
}

// Note: Keep these constants in sync with the ones in Console.h
WebInspector.ConsoleMessage.MessageSource = {
    HTML: 0,
    WML: 1,
    XML: 2,
    JS: 3,
    CSS: 4,
    Other: 5
}

WebInspector.ConsoleMessage.MessageType = {
    Log: 0,
    Object: 1,
    Trace: 2,
    StartGroup: 3,
    StartGroupCollapsed: 4,
    EndGroup: 5,
    Assert: 6,
    UncaughtException: 7,
    NetworkError:8,
    Result: 9
}

WebInspector.ConsoleMessage.MessageLevel = {
    Tip: 0,
    Log: 1,
    Warning: 2,
    Error: 3,
    Debug: 4
}

WebInspector.ConsoleCommand = function(command)
{
    this.command = command;
}

WebInspector.ConsoleCommand.prototype = {
    toMessageElement: function()
    {
        var element = document.createElement("div");
        element.command = this;
        element.className = "console-user-command";

        var commandTextElement = document.createElement("span");
        commandTextElement.className = "console-message-text source-code";
        commandTextElement.textContent = this.command;
        element.appendChild(commandTextElement);

        return element;
    }
}

WebInspector.ConsoleCommandResult = function(result, originatingCommand)
{
    var level = (result.isError() ? WebInspector.ConsoleMessage.MessageLevel.Error : WebInspector.ConsoleMessage.MessageLevel.Log);
    this.originatingCommand = originatingCommand;
    WebInspector.ConsoleMessage.call(this, WebInspector.ConsoleMessage.MessageSource.JS, WebInspector.ConsoleMessage.MessageType.Result, level, -1, null, 1, null, [result]);
}

WebInspector.ConsoleCommandResult.prototype = {
    toMessageElement: function()
    {
        var element = WebInspector.ConsoleMessage.prototype.toMessageElement.call(this);
        element.addStyleClass("console-user-command-result");
        return element;
    }
}

WebInspector.ConsoleCommandResult.prototype.__proto__ = WebInspector.ConsoleMessage.prototype;

WebInspector.ConsoleGroup = function(parentGroup)
{
    this.parentGroup = parentGroup;

    var element = document.createElement("div");
    element.className = "console-group";
    element.group = this;
    this.element = element;

    var messagesElement = document.createElement("div");
    messagesElement.className = "console-group-messages";
    element.appendChild(messagesElement);
    this.messagesElement = messagesElement;
}

WebInspector.ConsoleGroup.prototype = {
    addMessage: function(msg)
    {
        var element = msg.toMessageElement();

        if (msg.type === WebInspector.ConsoleMessage.MessageType.StartGroup || msg.type === WebInspector.ConsoleMessage.MessageType.StartGroupCollapsed) {
            this.messagesElement.parentNode.insertBefore(element, this.messagesElement);
            element.addEventListener("click", this._titleClicked.bind(this), false);
            var groupElement = element.enclosingNodeOrSelfWithClass("console-group");
            if (groupElement && msg.type === WebInspector.ConsoleMessage.MessageType.StartGroupCollapsed)
                groupElement.addStyleClass("collapsed");
        } else
            this.messagesElement.appendChild(element);

        if (element.previousSibling && msg.originatingCommand && element.previousSibling.command === msg.originatingCommand)
            element.previousSibling.addStyleClass("console-adjacent-user-command-result");
    },

    _titleClicked: function(event)
    {
        var groupTitleElement = event.target.enclosingNodeOrSelfWithClass("console-group-title");
        if (groupTitleElement) {
            var groupElement = groupTitleElement.enclosingNodeOrSelfWithClass("console-group");
            if (groupElement)
                if (groupElement.hasStyleClass("collapsed"))
                    groupElement.removeStyleClass("collapsed");
                else
                    groupElement.addStyleClass("collapsed");
            groupTitleElement.scrollIntoViewIfNeeded(true);
        }

        event.stopPropagation();
        event.preventDefault();
    }
}

