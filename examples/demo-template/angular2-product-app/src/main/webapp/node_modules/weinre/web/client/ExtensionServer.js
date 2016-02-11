/*
 * Copyright (C) 2011 Google Inc. All rights reserved.
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

WebInspector.ExtensionServer = function()
{
    this._clientObjects = {};
    this._handlers = {};
    this._subscribers = {};
    this._extraHeaders = {};
    this._status = new WebInspector.ExtensionStatus();

    this._registerHandler("addRequestHeaders", this._onAddRequestHeaders.bind(this));
    this._registerHandler("addAuditCategory", this._onAddAuditCategory.bind(this));
    this._registerHandler("addAuditResult", this._onAddAuditResult.bind(this));
    this._registerHandler("createPanel", this._onCreatePanel.bind(this));
    this._registerHandler("createSidebarPane", this._onCreateSidebar.bind(this));
    this._registerHandler("createWatchExpressionSidebarPane", this._onCreateWatchExpressionSidebarPane.bind(this));
    this._registerHandler("evaluateOnInspectedPage", this._onEvaluateOnInspectedPage.bind(this));
    this._registerHandler("getHAR", this._onGetHAR.bind(this));
    this._registerHandler("getResourceContent", this._onGetResourceContent.bind(this));
    this._registerHandler("log", this._onLog.bind(this));
    this._registerHandler("reload", this._onReload.bind(this));
    this._registerHandler("setSidebarHeight", this._onSetSidebarHeight.bind(this));
    this._registerHandler("setWatchSidebarContent", this._onSetWatchSidebarContent.bind(this));
    this._registerHandler("stopAuditCategoryRun", this._onStopAuditCategoryRun.bind(this));
    this._registerHandler("subscribe", this._onSubscribe.bind(this));
    this._registerHandler("unsubscribe", this._onUnsubscribe.bind(this));

    window.addEventListener("message", this._onWindowMessage.bind(this), false);
}

WebInspector.ExtensionServer.prototype = {
    notifyPanelShown: function(panelName)
    {
        this._postNotification("panel-shown-" + panelName);
    },

    notifyObjectSelected: function(panelId, objectId)
    {
        this._postNotification("panel-objectSelected-" + panelId, objectId);
    },

    notifySearchAction: function(panelId, action, searchString)
    {
        this._postNotification("panel-search-" + panelId, action, searchString);
    },

    notifyPageLoaded: function(milliseconds)
    {
        this._postNotification("inspectedPageLoaded", milliseconds);
    },

    notifyPageDOMContentLoaded: function(milliseconds)
    {
        this._postNotification("inspectedPageDOMContentLoaded", milliseconds);
    },

    notifyInspectedURLChanged: function()
    {
        this._postNotification("inspectedURLChanged");
    },

    notifyInspectorReset: function()
    {
        this._postNotification("reset");
    },

    notifyExtensionWatchSidebarUpdated: function(id)
    {
        this._postNotification("watch-sidebar-updated-" + id);
    },

    startAuditRun: function(category, auditRun)
    {
        this._clientObjects[auditRun.id] = auditRun;
        this._postNotification("audit-started-" + category.id, auditRun.id);
    },

    stopAuditRun: function(auditRun)
    {
        delete this._clientObjects[auditRun.id];
    },

    _notifyResourceFinished: function(event)
    {
        var resource = event.data;
        this._postNotification("resource-finished", resource.identifier, (new WebInspector.HAREntry(resource)).build());
    },

    _postNotification: function(type, details)
    {
        var subscribers = this._subscribers[type];
        if (!subscribers)
            return;
        var message = {
            command: "notify-" + type,
            arguments: Array.prototype.slice.call(arguments, 1)
        };
        for (var i = 0; i < subscribers.length; ++i)
            subscribers[i].postMessage(message);
    },

    _onSubscribe: function(message, port)
    {
        var subscribers = this._subscribers[message.type];
        if (subscribers)
            subscribers.push(port);
        else
            this._subscribers[message.type] = [ port ];
    },

    _onUnsubscribe: function(message, port)
    {
        var subscribers = this._subscribers[message.type];
        if (!subscribers)
            return;
        subscribers.remove(port);
        if (!subscribers.length)
            delete this._subscribers[message.type];
    },

    _onAddRequestHeaders: function(message)
    {
        var id = message.extensionId;
        if (typeof id !== "string")
            return this._status.E_BADARGTYPE("extensionId", typeof id, "string");
        var extensionHeaders = this._extraHeaders[id];
        if (!extensionHeaders) {
            extensionHeaders = {};
            this._extraHeaders[id] = extensionHeaders;
        }
        for (name in message.headers)
            extensionHeaders[name] = message.headers[name];
        var allHeaders = {};
        for (extension in this._extraHeaders) {
            var headers = this._extraHeaders[extension];
            for (name in headers) {
                if (typeof headers[name] === "string")
                    allHeaders[name] = headers[name];
            }
        }
        InspectorBackend.setExtraHeaders(allHeaders);
    },

    _onCreatePanel: function(message, port)
    {
        var id = message.id;
        // The ids are generated on the client API side and must be unique, so the check below
        // shouldn't be hit unless someone is bypassing the API.
        if (id in this._clientObjects || id in WebInspector.panels)
            return this._status.E_EXISTS(id);
        var panel = new WebInspector.ExtensionPanel(id, message.title, message.icon);
        this._clientObjects[id] = panel;

        var toolbarElement = document.getElementById("toolbar");
        var lastToolbarItem = WebInspector.panelOrder[WebInspector.panelOrder.length - 1].toolbarItem;
        WebInspector.addPanelToolbarIcon(toolbarElement, panel, lastToolbarItem);
        WebInspector.panels[id] = panel;
        var iframe = this._createClientIframe(panel.element, message.url);
        iframe.style.height = "100%";
        return this._status.OK();
    },

    _onCreateSidebar: function(message)
    {
        var sidebar = this._createSidebar(message, WebInspector.SidebarPane);
        if (sidebar.isError)
            return sidebar;
        this._createClientIframe(sidebar.bodyElement, message.url);
        return this._status.OK();
    },

    _onCreateWatchExpressionSidebarPane: function(message)
    {
        var sidebar = this._createSidebar(message, WebInspector.ExtensionWatchSidebarPane);
        return sidebar.isError ? sidebar : this._status.OK();
    },

    _createSidebar: function(message, constructor)
    {
        var panel = WebInspector.panels[message.panel];
        if (!panel)
            return this._status.E_NOTFOUND(message.panel);
        if (!panel.sidebarElement || !panel.sidebarPanes)
            return this._status.E_NOTSUPPORTED();
        var id = message.id;
        var sidebar = new constructor(message.title, message.id);
        this._clientObjects[id] = sidebar;
        panel.sidebarPanes[id] = sidebar;
        panel.sidebarElement.appendChild(sidebar.element);

        return sidebar;
    },

    _createClientIframe: function(parent, url, requestId, port)
    {
        var iframe = document.createElement("iframe");
        iframe.src = url;
        iframe.style.width = "100%";
        parent.appendChild(iframe);
        return iframe;
    },

    _onSetSidebarHeight: function(message)
    {
        var sidebar = this._clientObjects[message.id];
        if (!sidebar)
            return this._status.E_NOTFOUND(message.id);
        sidebar.bodyElement.firstChild.style.height = message.height;
    },

    _onSetWatchSidebarContent: function(message)
    {
        var sidebar = this._clientObjects[message.id];
        if (!sidebar)
            return this._status.E_NOTFOUND(message.id);
        if (message.evaluateOnPage)
            sidebar.setExpression(message.expression, message.rootTitle);
        else
            sidebar.setObject(message.expression, message.rootTitle);
    },

    _onLog: function(message)
    {
        WebInspector.log(message.message);
    },

    _onReload: function(message)
    {
        if (typeof message.userAgent === "string")
            InspectorBackend.setUserAgentOverride(message.userAgent);

        InspectorBackend.reloadPage(false);
        return this._status.OK();
    },

    _onEvaluateOnInspectedPage: function(message, port)
    {
        function callback(resultPayload)
        {
            var resultObject = WebInspector.RemoteObject.fromPayload(resultPayload);
            var result = {};
            if (resultObject.isError())
                result.isException = true;
            result.value = resultObject.description;
            this._dispatchCallback(message.requestId, port, result);
        }
        var evalExpression = "JSON.stringify(eval(unescape('" + escape(message.expression) + "')));";
        InspectorBackend.evaluate(evalExpression, "none", true, callback.bind(this));
    },

    _onRevealAndSelect: function(message)
    {
        if (message.panelId === "resources" && type === "resource")
            return this._onRevealAndSelectResource(message);
        else
            return this._status.E_NOTSUPPORTED(message.panelId, message.type);
    },

    _onRevealAndSelectResource: function(message)
    {
        var id = message.id;
        var resource = null;

        resource = WebInspector.networkResourceById(id) || WebInspector.resourceForURL(id);
        if (!resource)
            return this._status.E_NOTFOUND(typeof id + ": " + id);

        WebInspector.panels.resources.showResource(resource, message.line);
        WebInspector.showPanel("resources");
    },

    _dispatchCallback: function(requestId, port, result)
    {
        port.postMessage({ command: "callback", requestId: requestId, result: result });
    },

    _onGetHAR: function(request)
    {
        var harLog = new WebInspector.HARLog();
        harLog.includeResourceIds = true;
        return harLog.build();
    },

    _onGetResourceContent: function(message, port)
    {
        function onContentAvailable(content, encoded)
        {
            var response = {
                encoding: encoded ? "base64" : "",
                content: content
            };
            this._dispatchCallback(message.requestId, port, response);
        }
        var resource = WebInspector.networkResourceById(message.id);
        if (!resource)
            return this._status.E_NOTFOUND(message.id);
        resource.requestContent(onContentAvailable.bind(this));
    },

    _onAddAuditCategory: function(request)
    {
        var category = new WebInspector.ExtensionAuditCategory(request.id, request.displayName, request.resultCount);
        if (WebInspector.panels.audits.getCategory(category.id))
            return this._status.E_EXISTS(category.id);
        this._clientObjects[request.id] = category;
        WebInspector.panels.audits.addCategory(category);
    },

    _onAddAuditResult: function(request)
    {
        var auditResult = this._clientObjects[request.resultId];
        if (!auditResult)
            return this._status.E_NOTFOUND(request.resultId);
        try {
            auditResult.addResult(request.displayName, request.description, request.severity, request.details);
        } catch (e) {
            return e;
        }
        return this._status.OK();
    },

    _onStopAuditCategoryRun: function(request)
    {
        var auditRun = this._clientObjects[request.resultId];
        if (!auditRun)
            return this._status.E_NOTFOUND(request.resultId);
        auditRun.cancel();
    },

    initExtensions: function()
    {
        // The networkManager is normally created after the ExtensionServer is constructed, but before initExtensions() is called.
        WebInspector.networkManager.addEventListener(WebInspector.NetworkManager.EventTypes.ResourceFinished, this._notifyResourceFinished, this);

        InspectorExtensionRegistry.getExtensionsAsync();
    },

    _addExtensions: function(extensions)
    {
        // See ExtensionAPI.js and ExtensionCommon.js for details.
        InspectorFrontendHost.setExtensionAPI(this._buildExtensionAPIInjectedScript());
        for (var i = 0; i < extensions.length; ++i) {
            var extension = extensions[i];
            try {
                if (!extension.startPage)
                    return;
                var iframe = document.createElement("iframe");
                iframe.src = extension.startPage;
                iframe.style.display = "none";
                document.body.appendChild(iframe);
            } catch (e) {
                console.error("Failed to initialize extension " + extension.startPage + ":" + e);
            }
        }
    },

    _buildExtensionAPIInjectedScript: function()
    {
        var resourceTypes = {};
        var resourceTypeProperties = Object.getOwnPropertyNames(WebInspector.Resource.Type);
        for (var i = 0; i < resourceTypeProperties.length; ++i) {
             var propName = resourceTypeProperties[i];
             var propValue = WebInspector.Resource.Type[propName];
             if (typeof propValue === "number")
                 resourceTypes[propName] = WebInspector.Resource.Type.toString(propValue);
        }
        var platformAPI = WebInspector.buildPlatformExtensionAPI ? WebInspector.buildPlatformExtensionAPI() : "";
        return "(function(){ " +
            "var apiPrivate = {};" +
            "(" + WebInspector.commonExtensionSymbols.toString() + ")(apiPrivate);" +
            "(" + WebInspector.injectedExtensionAPI.toString() + ").apply(this, arguments);" +
            platformAPI +
            "})";
    },

    _onWindowMessage: function(event)
    {
        if (event.data !== "registerExtension")
            return;
        var port = event.ports[0];
        port.addEventListener("message", this._onmessage.bind(this), false);
        port.start();
    },

    _onmessage: function(event)
    {
        var request = event.data;
        var result;

        if (request.command in this._handlers)
            result = this._handlers[request.command](request, event.target);
        else
            result = this._status.E_NOTSUPPORTED(request.command);

        if (result && request.requestId)
            this._dispatchCallback(request.requestId, event.target, result);
    },

    _registerHandler: function(command, callback)
    {
        this._handlers[command] = callback;
    }
}

WebInspector.ExtensionServer._statuses =
{
    OK: "",
    E_EXISTS: "Object already exists: %s",
    E_BADARG: "Invalid argument %s: %s",
    E_BADARGTYPE: "Invalid type for argument %s: got %s, expected %s",
    E_NOTFOUND: "Object not found: %s",
    E_NOTSUPPORTED: "Object does not support requested operation: %s",
}

WebInspector.ExtensionStatus = function()
{
    function makeStatus(code)
    {
        var description = WebInspector.ExtensionServer._statuses[code] || code;
        var details = Array.prototype.slice.call(arguments, 1);
        var status = { code: code, description: description, details: details };
        if (code !== "OK") {
            status.isError = true;
            console.log("Extension server error: " + String.vsprintf(description, details));
        }
        return status;
    }
    for (status in WebInspector.ExtensionServer._statuses)
        this[status] = makeStatus.bind(null, status);
}

WebInspector.addExtensions = function(extensions)
{
    WebInspector.extensionServer._addExtensions(extensions);
}

WebInspector.extensionServer = new WebInspector.ExtensionServer();
