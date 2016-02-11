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

WebInspector.NetworkManager = function(resourceTreeModel)
{
    WebInspector.Object.call(this);
    this._resourceTreeModel = resourceTreeModel;
    this._dispatcher = new WebInspector.NetworkDispatcher(resourceTreeModel, this);
    InspectorBackend.cachedResources(this._processCachedResources.bind(this));
}

WebInspector.NetworkManager.EventTypes = {
    ResourceStarted: "ResourceStarted",
    ResourceUpdated: "ResourceUpdated",
    ResourceFinished: "ResourceFinished",
    MainResourceCommitLoad: "MainResourceCommitLoad"
}

WebInspector.NetworkManager.prototype = {
    reset: function()
    {
        WebInspector.panels.network.clear();
        this._resourceTreeModel.reset();
        InspectorBackend.cachedResources(this._processCachedResources.bind(this));
    },

    requestContent: function(resource, base64Encode, callback)
    {
        function callbackWrapper(success, content)
        {
            callback(success ? content : null);
        }
        InspectorBackend.resourceContent(resource.loader.frameId, resource.url, base64Encode, callbackWrapper);
    },

    _processCachedResources: function(mainFramePayload)
    {
        var mainResource = this._dispatcher._addFramesRecursively(mainFramePayload);
        WebInspector.mainResource = mainResource;
        mainResource.isMainResource = true;
    },

    inflightResourceForURL: function(url)
    {
        return this._dispatcher._inflightResourcesByURL[url];
    }
}

WebInspector.NetworkManager.prototype.__proto__ = WebInspector.Object.prototype;

WebInspector.NetworkDispatcher = function(resourceTreeModel, manager)
{
    this._manager = manager;
    this._inflightResourcesById = {};
    this._inflightResourcesByURL = {};
    this._resourceTreeModel = resourceTreeModel;
    this._lastIdentifierForCachedResource = 0;
    InspectorBackend.registerDomainDispatcher("Network", this);
}

WebInspector.NetworkDispatcher.prototype = {
    _updateResourceWithRequest: function(resource, request)
    {
        resource.requestMethod = request.httpMethod;
        resource.requestHeaders = request.httpHeaderFields;
        resource.requestFormData = request.requestFormData;
    },

    _updateResourceWithResponse: function(resource, response)
    {
        if (resource.isNull)
            return;

        resource.mimeType = response.mimeType;
        resource.expectedContentLength = response.expectedContentLength;
        resource.textEncodingName = response.textEncodingName;
        resource.suggestedFilename = response.suggestedFilename;
        resource.statusCode = response.httpStatusCode;
        resource.statusText = response.httpStatusText;

        resource.responseHeaders = response.httpHeaderFields;
        resource.connectionReused = response.connectionReused;
        resource.connectionID = response.connectionID;

        if (response.wasCached)
            resource.cached = true;
        else
            resource.timing = response.timing;

        if (response.loadInfo) {
            if (response.loadInfo.httpStatusCode)
                resource.statusCode = response.loadInfo.httpStatusCode;
            if (response.loadInfo.httpStatusText)
                resource.statusText = response.loadInfo.httpStatusText;
            resource.requestHeaders = response.loadInfo.requestHeaders;
            resource.responseHeaders = response.loadInfo.responseHeaders;
        }
    },

    _updateResourceWithCachedResource: function(resource, cachedResource)
    {
        resource.type = WebInspector.Resource.Type[cachedResource.type];
        resource.resourceSize = cachedResource.encodedSize;
        this._updateResourceWithResponse(resource, cachedResource.response);
    },

    identifierForInitialRequest: function(identifier, url, loader, callStack)
    {
        this._startResource(this._createResource(identifier, url, loader, callStack));
    },

    willSendRequest: function(identifier, time, request, redirectResponse)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        // Redirect may have empty URL and we'd like to not crash with invalid HashMap entry.
        // See http/tests/misc/will-send-request-returns-null-on-redirect.html
        var isRedirect = !redirectResponse.isNull && request.url.length;
        if (isRedirect) {
            this.didReceiveResponse(identifier, time, "Other", redirectResponse);
            resource = this._appendRedirect(resource.identifier, time, request.url);
        }

        this._updateResourceWithRequest(resource, request);
        resource.startTime = time;

        if (isRedirect)
            this._startResource(resource);
        else
            this._updateResource(resource);
    },

    markResourceAsCached: function(identifier)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        resource.cached = true;
        this._updateResource(resource);
    },

    didReceiveResponse: function(identifier, time, resourceType, response)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        resource.responseReceivedTime = time;
        resource.type = WebInspector.Resource.Type[resourceType];

        this._updateResourceWithResponse(resource, response);

        this._updateResource(resource);
        this._resourceTreeModel.addResourceToFrame(resource.loader.frameId, resource);
    },

    didReceiveContentLength: function(identifier, time, lengthReceived)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        resource.resourceSize += lengthReceived;
        resource.endTime = time;

        this._updateResource(resource);
    },

    didFinishLoading: function(identifier, finishTime)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        this._finishResource(resource, finishTime);
    },

    didFailLoading: function(identifier, time, localizedDescription)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        resource.failed = true;
        resource.localizedFailDescription = localizedDescription;
        this._finishResource(resource, time);
    },

    didLoadResourceFromMemoryCache: function(time, cachedResource)
    {
        var resource = this._createResource("cached:" + ++this._lastIdentifierForCachedResource, cachedResource.url, cachedResource.loader);
        this._updateResourceWithCachedResource(resource, cachedResource);
        resource.cached = true;
        resource.requestMethod = "GET";
        this._startResource(resource);
        resource.startTime = resource.responseReceivedTime = time;
        this._finishResource(resource, time);
        this._resourceTreeModel.addResourceToFrame(resource.loader.frameId, resource);
    },

    frameDetachedFromParent: function(frameId)
    {
        this._resourceTreeModel.frameDetachedFromParent(frameId);
    },

    setInitialContent: function(identifier, sourceString, type)
    {
        var resource = WebInspector.networkResourceById(identifier);
        if (!resource)
            return;

        resource.type = WebInspector.Resource.Type[type];
        resource.setInitialContent(sourceString);
        this._updateResource(resource);
    },

    didCommitLoadForFrame: function(frame, loader)
    {
        this._resourceTreeModel.didCommitLoadForFrame(frame, loader);
        if (!frame.parentId) {
            var mainResource = this._resourceTreeModel.resourceForURL(frame.url);
            if (mainResource) {
                WebInspector.mainResource = mainResource;
                mainResource.isMainResource = true;
                this._dispatchEventToListeners(WebInspector.NetworkManager.EventTypes.MainResourceCommitLoad, mainResource);
            }
        }
    },

    didCreateWebSocket: function(identifier, requestURL)
    {
        var resource = this._createResource(identifier, requestURL);
        resource.type = WebInspector.Resource.Type.WebSocket;
        this._startResource(resource);
    },

    willSendWebSocketHandshakeRequest: function(identifier, time, request)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        resource.requestMethod = "GET";
        resource.requestHeaders = request.webSocketHeaderFields;
        resource.webSocketRequestKey3 = request.webSocketRequestKey3;
        resource.startTime = time;

        this._updateResource(resource);
    },

    didReceiveWebSocketHandshakeResponse: function(identifier, time, response)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;

        resource.statusCode = response.statusCode;
        resource.statusText = response.statusText;
        resource.responseHeaders = response.webSocketHeaderFields;
        resource.webSocketChallengeResponse = response.webSocketChallengeResponse;
        resource.responseReceivedTime = time;

        this._updateResource(resource);
    },

    didCloseWebSocket: function(identifier, time)
    {
        var resource = this._inflightResourcesById[identifier];
        if (!resource)
            return;
        this._finishResource(resource, time);
    },

    _appendRedirect: function(identifier, time, redirectURL)
    {
        var originalResource = this._inflightResourcesById[identifier];
        var previousRedirects = originalResource.redirects || [];
        originalResource.identifier = "redirected:" + identifier + "." + previousRedirects.length;
        delete originalResource.redirects;
        this._finishResource(originalResource, time);
        var newResource = this._createResource(identifier, redirectURL, originalResource.loader, originalResource.stackTrace);
        newResource.redirects = previousRedirects.concat(originalResource);
        return newResource;
    },

    _startResource: function(resource)
    {
        this._inflightResourcesById[resource.identifier] = resource;
        this._inflightResourcesByURL[resource.url] = resource;
        this._dispatchEventToListeners(WebInspector.NetworkManager.EventTypes.ResourceStarted, resource);
    },

    _updateResource: function(resource)
    {
        this._dispatchEventToListeners(WebInspector.NetworkManager.EventTypes.ResourceUpdated, resource);
    },

    _finishResource: function(resource, finishTime)
    {
        resource.endTime = finishTime;
        resource.finished = true;
        this._dispatchEventToListeners(WebInspector.NetworkManager.EventTypes.ResourceFinished, resource);
        delete this._inflightResourcesById[resource.identifier];
        delete this._inflightResourcesByURL[resource.url];
    },

    _addFramesRecursively: function(framePayload)
    {
        var frameResource = this._createResource(null, framePayload.resource.url, framePayload.resource.loader);
        this._updateResourceWithRequest(frameResource, framePayload.resource.request);
        this._updateResourceWithResponse(frameResource, framePayload.resource.response);
        frameResource.type = WebInspector.Resource.Type["Document"];
        frameResource.finished = true;

        this._resourceTreeModel.addOrUpdateFrame(framePayload);
        this._resourceTreeModel.addResourceToFrame(framePayload.id, frameResource);

        for (var i = 0; framePayload.children && i < framePayload.children.length; ++i)
            this._addFramesRecursively(framePayload.children[i]);

        if (!framePayload.subresources)
            return;

        for (var i = 0; i < framePayload.subresources.length; ++i) {
            var cachedResource = framePayload.subresources[i];
            var resource = this._createResource(null, cachedResource.url, cachedResource.loader);
            this._updateResourceWithCachedResource(resource, cachedResource);
            resource.finished = true;
            this._resourceTreeModel.addResourceToFrame(framePayload.id, resource);
        }
        return frameResource;
    },

    _dispatchEventToListeners: function(eventType, resource)
    {
        this._manager.dispatchEventToListeners(eventType, resource);
    },

    _createResource: function(identifier, url, loader, stackTrace)
    {
        var resource = new WebInspector.Resource(identifier, url);
        resource.loader = loader;
        if (loader)
            resource.documentURL = loader.url;
        resource.stackTrace = stackTrace;
        return resource;
    }
}
