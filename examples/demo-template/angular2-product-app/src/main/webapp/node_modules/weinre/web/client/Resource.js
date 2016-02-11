/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
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
WebInspector.Resource = function(identifier, url)
{
    this.identifier = identifier;
    this.url = url;
    this._startTime = -1;
    this._endTime = -1;
    this._category = WebInspector.resourceCategories.other;
    this._pendingContentCallbacks = [];
    this._responseHeadersSize = 0;
}

// Keep these in sync with WebCore::InspectorResource::Type
WebInspector.Resource.Type = {
    Document:   0,
    Stylesheet: 1,
    Image:      2,
    Font:       3,
    Script:     4,
    XHR:        5,
    WebSocket:  7,
    Other:      8,

    isTextType: function(type)
    {
        return (type === this.Document) || (type === this.Stylesheet) || (type === this.Script) || (type === this.XHR);
    },

    toUIString: function(type)
    {
        switch (type) {
            case this.Document:
                return WebInspector.UIString("Document");
            case this.Stylesheet:
                return WebInspector.UIString("Stylesheet");
            case this.Image:
                return WebInspector.UIString("Image");
            case this.Font:
                return WebInspector.UIString("Font");
            case this.Script:
                return WebInspector.UIString("Script");
            case this.XHR:
                return WebInspector.UIString("XHR");
            case this.WebSocket:
                return WebInspector.UIString("WebSocket");
            case this.Other:
            default:
                return WebInspector.UIString("Other");
        }
    },

    // Returns locale-independent string identifier of resource type (primarily for use in extension API).
    // The IDs need to be kept in sync with webInspector.resoureces.Types object in ExtensionAPI.js.
    toString: function(type)
    {
        switch (type) {
            case this.Document:
                return "document";
            case this.Stylesheet:
                return "stylesheet";
            case this.Image:
                return "image";
            case this.Font:
                return "font";
            case this.Script:
                return "script";
            case this.XHR:
                return "xhr";
            case this.WebSocket:
                return "websocket";
            case this.Other:
            default:
                return "other";
        }
    }
}

WebInspector.Resource.prototype = {
    get url()
    {
        return this._url;
    },

    set url(x)
    {
        if (this._url === x)
            return;

        this._url = x;
        delete this._parsedQueryParameters;

        var parsedURL = x.asParsedURL();
        this.domain = parsedURL ? parsedURL.host : "";
        this.path = parsedURL ? parsedURL.path : "";
        this.lastPathComponent = "";
        if (parsedURL && parsedURL.path) {
            // First cut the query params.
            var path = parsedURL.path;
            var indexOfQuery = path.indexOf("?");
            if (indexOfQuery !== -1)
                path = path.substring(0, indexOfQuery);

            // Then take last path component.
            var lastSlashIndex = path.lastIndexOf("/");
            if (lastSlashIndex !== -1)
                this.lastPathComponent = path.substring(lastSlashIndex + 1);
        }
        this.lastPathComponentLowerCase = this.lastPathComponent.toLowerCase();
    },

    get documentURL()
    {
        return this._documentURL;
    },

    set documentURL(x)
    {
        this._documentURL = x;
    },

    get displayName()
    {
        if (this._displayName)
            return this._displayName;
        this._displayName = this.lastPathComponent;
        if (!this._displayName)
            this._displayName = this.displayDomain;
        if (!this._displayName && this.url)
            this._displayName = this.url.trimURL(WebInspector.mainResource ? WebInspector.mainResource.domain : "");
        if (this._displayName === "/")
            this._displayName = this.url;
        return this._displayName;
    },

    get displayDomain()
    {
        // WebInspector.Database calls this, so don't access more than this.domain.
        if (this.domain && (!WebInspector.mainResource || (WebInspector.mainResource && this.domain !== WebInspector.mainResource.domain)))
            return this.domain;
        return "";
    },

    get startTime()
    {
        return this._startTime || -1;
    },

    set startTime(x)
    {
        this._startTime = x;
    },

    get responseReceivedTime()
    {
        return this._responseReceivedTime || -1;
    },

    set responseReceivedTime(x)
    {
        this._responseReceivedTime = x;
    },

    get endTime()
    {
        return this._endTime || -1;
    },

    set endTime(x)
    {
        if (this.timing && this.timing.requestTime) {
            // Check against accurate responseReceivedTime.
            this._endTime = Math.max(x, this.responseReceivedTime);
        } else {
            // Prefer endTime since it might be from the network stack.
            this._endTime = x;
            if (this._responseReceivedTime > x)
                this._responseReceivedTime = x;
        }
    },

    get duration()
    {
        if (this._endTime === -1 || this._startTime === -1)
            return -1;
        return this._endTime - this._startTime;
    },

    get latency()
    {
        if (this._responseReceivedTime === -1 || this._startTime === -1)
            return -1;
        return this._responseReceivedTime - this._startTime;
    },

    get receiveDuration()
    {
        if (this._endTime === -1 || this._responseReceivedTime === -1)
            return -1;
        return this._endTime - this._responseReceivedTime;
    },

    get resourceSize()
    {
        return this._resourceSize || 0;
    },

    set resourceSize(x)
    {
        this._resourceSize = x;
    },

    get transferSize()
    {
        if (this.cached)
            return 0;
        if (this.statusCode === 304) // Not modified
            return this._responseHeadersSize;
        // FIXME: We prefer using Content-Length over resourceSize as
        // resourceSize may differ from actual transfer size if platform's
        // network stack performed decoding (e.g. gzip decompression).
        // The Content-Length, though, is expected to come from raw
        // response headers and will reflect actual transfer length.
        // This won't work for chunked content encoding, so fall back to
        // resourceSize when we don't have Content-Length. This still won't
        // work for chunks with non-trivial encodings. We need a way to
        // get actaul transfer size from the network stack.
        var bodySize = Number(this.responseHeaders["Content-Length"] || this.resourceSize);
        return this._responseHeadersSize + bodySize;
    },

    get expectedContentLength()
    {
        return this._expectedContentLength || 0;
    },

    set expectedContentLength(x)
    {
        this._expectedContentLength = x;
    },

    get finished()
    {
        return this._finished;
    },

    set finished(x)
    {
        if (this._finished === x)
            return;

        this._finished = x;

        if (x) {
            this._checkWarnings();
            this.dispatchEventToListeners("finished");
            if (this._pendingContentCallbacks.length)
                this._innerRequestContent();
        }
    },

    get failed()
    {
        return this._failed;
    },

    set failed(x)
    {
        this._failed = x;
    },

    get category()
    {
        return this._category;
    },

    set category(x)
    {
        this._category = x;
    },

    get cached()
    {
        return this._cached;
    },

    set cached(x)
    {
        this._cached = x;
        if (x)
            delete this._timing;
    },

    get timing()
    {
        return this._timing;
    },

    set timing(x)
    {
        if (x && !this._cached) {
            // Take startTime and responseReceivedTime from timing data for better accuracy.
            // Timing's requestTime is a baseline in seconds, rest of the numbers there are ticks in millis.
            this._startTime = x.requestTime;
            this._responseReceivedTime = x.requestTime + x.receiveHeadersEnd / 1000.0;

            this._timing = x;
            this.dispatchEventToListeners("timing changed");
        }
    },

    get mimeType()
    {
        return this._mimeType;
    },

    set mimeType(x)
    {
        this._mimeType = x;
    },

    get type()
    {
        return this._type;
    },

    set type(x)
    {
        if (this._type === x)
            return;

        this._type = x;

        switch (x) {
            case WebInspector.Resource.Type.Document:
                this.category = WebInspector.resourceCategories.documents;
                break;
            case WebInspector.Resource.Type.Stylesheet:
                this.category = WebInspector.resourceCategories.stylesheets;
                break;
            case WebInspector.Resource.Type.Script:
                this.category = WebInspector.resourceCategories.scripts;
                break;
            case WebInspector.Resource.Type.Image:
                this.category = WebInspector.resourceCategories.images;
                break;
            case WebInspector.Resource.Type.Font:
                this.category = WebInspector.resourceCategories.fonts;
                break;
            case WebInspector.Resource.Type.XHR:
                this.category = WebInspector.resourceCategories.xhr;
                break;
            case WebInspector.Resource.Type.WebSocket:
                this.category = WebInspector.resourceCategories.websockets;
                break;
            case WebInspector.Resource.Type.Other:
            default:
                this.category = WebInspector.resourceCategories.other;
                break;
        }
    },

    get requestHeaders()
    {
        return this._requestHeaders || {};
    },

    set requestHeaders(x)
    {
        this._requestHeaders = x;
        delete this._sortedRequestHeaders;
        delete this._requestCookies;

        this.dispatchEventToListeners("requestHeaders changed");
    },

    get sortedRequestHeaders()
    {
        if (this._sortedRequestHeaders !== undefined)
            return this._sortedRequestHeaders;

        this._sortedRequestHeaders = [];
        for (var key in this.requestHeaders)
            this._sortedRequestHeaders.push({header: key, value: this.requestHeaders[key]});
        this._sortedRequestHeaders.sort(function(a,b) { return a.header.localeCompare(b.header) });

        return this._sortedRequestHeaders;
    },

    requestHeaderValue: function(headerName)
    {
        return this._headerValue(this.requestHeaders, headerName);
    },

    get requestCookies()
    {
        if (!this._requestCookies)
            this._requestCookies = WebInspector.CookieParser.parseCookie(this.requestHeaderValue("Cookie"));
        return this._requestCookies;
    },

    get requestFormData()
    {
        return this._requestFormData;
    },

    set requestFormData(x)
    {
        this._requestFormData = x;
        delete this._parsedFormParameters;
    },

    get responseHeaders()
    {
        return this._responseHeaders || {};
    },

    set responseHeaders(x)
    {
        this._responseHeaders = x;
        // FIXME: we should take actual headers size from network stack, when possible.
        this._responseHeadersSize = this._headersSize(x);
        delete this._sortedResponseHeaders;
        delete this._responseCookies;

        this.dispatchEventToListeners("responseHeaders changed");
    },

    get sortedResponseHeaders()
    {
        if (this._sortedResponseHeaders !== undefined)
            return this._sortedResponseHeaders;

        this._sortedResponseHeaders = [];
        for (var key in this.responseHeaders)
            this._sortedResponseHeaders.push({header: key, value: this.responseHeaders[key]});
        this._sortedResponseHeaders.sort(function(a,b) { return a.header.localeCompare(b.header) });

        return this._sortedResponseHeaders;
    },

    responseHeaderValue: function(headerName)
    {
        return this._headerValue(this.responseHeaders, headerName);
    },

    get responseCookies()
    {
        if (!this._responseCookies)
            this._responseCookies = WebInspector.CookieParser.parseSetCookie(this.responseHeaderValue("Set-Cookie"));
        return this._responseCookies;
    },

    get queryParameters()
    {
        if (this._parsedQueryParameters)
            return this._parsedQueryParameters;
        var queryString = this.url.split("?", 2)[1];
        if (!queryString)
            return;
        this._parsedQueryParameters = this._parseParameters(queryString);
        return this._parsedQueryParameters;
    },

    get formParameters()
    {
        if (this._parsedFormParameters)
            return this._parsedFormParameters;
        if (!this.requestFormData)
            return;
        var requestContentType = this.requestHeaderValue("Content-Type");
        if (!requestContentType || !requestContentType.match(/^application\/x-www-form-urlencoded\s*(;.*)?$/i))
            return;
        this._parsedFormParameters = this._parseParameters(this.requestFormData);
        return this._parsedFormParameters;
    },

    _parseParameters: function(queryString)
    {
        function parseNameValue(pair)
        {
            var parameter = {};
            var splitPair = pair.split("=", 2);

            parameter.name = splitPair[0];
            if (splitPair.length === 1)
                parameter.value = "";
            else
                parameter.value = splitPair[1];
            return parameter;
        }
        return queryString.split("&").map(parseNameValue);
    },

    _headerValue: function(headers, headerName)
    {
        headerName = headerName.toLowerCase();
        for (var header in headers) {
            if (header.toLowerCase() === headerName)
                return headers[header];
        }
    },

    _headersSize: function(headers)
    {
        var size = 0;
        for (var header in headers)
            size += header.length + headers[header].length + 3; // _typical_ overhead per herader is ": ".length + "\n".length.
        return size;
    },

    get errors()
    {
        return this._errors || 0;
    },

    set errors(x)
    {
        this._errors = x;
        this.dispatchEventToListeners("errors-warnings-updated");
    },

    get warnings()
    {
        return this._warnings || 0;
    },

    set warnings(x)
    {
        this._warnings = x;
        this.dispatchEventToListeners("errors-warnings-updated");
    },

    clearErrorsAndWarnings: function()
    {
        this._warnings = 0;
        this._errors = 0;
        this.dispatchEventToListeners("errors-warnings-updated");
    },

    _mimeTypeIsConsistentWithType: function()
    {
        // If status is an error, content is likely to be of an inconsistent type,
        // as it's going to be an error message. We do not want to emit a warning
        // for this, though, as this will already be reported as resource loading failure.
        if (this.statusCode >= 400)
            return true;

        if (typeof this.type === "undefined"
            || this.type === WebInspector.Resource.Type.Other
            || this.type === WebInspector.Resource.Type.XHR
            || this.type === WebInspector.Resource.Type.WebSocket)
            return true;

        if (!this.mimeType)
            return true; // Might be not known for cached resources with null responses.

        if (this.mimeType in WebInspector.MIMETypes)
            return this.type in WebInspector.MIMETypes[this.mimeType];

        return false;
    },

    _checkWarnings: function()
    {
        for (var warning in WebInspector.Warnings)
            this._checkWarning(WebInspector.Warnings[warning]);
    },

    _checkWarning: function(warning)
    {
        var msg;
        switch (warning.id) {
            case WebInspector.Warnings.IncorrectMIMEType.id:
                if (!this._mimeTypeIsConsistentWithType())
                    msg = new WebInspector.ConsoleMessage(WebInspector.ConsoleMessage.MessageSource.Other,
                        WebInspector.ConsoleMessage.MessageType.Log,
                        WebInspector.ConsoleMessage.MessageLevel.Warning,
                        -1,
                        this.url,
                        1,
                        String.sprintf(WebInspector.Warnings.IncorrectMIMEType.message, WebInspector.Resource.Type.toUIString(this.type), this.mimeType),
                        null,
                        null);
                break;
        }

        if (msg)
            WebInspector.console.addMessage(msg);
    },

    get content()
    {
        return this._content;
    },

    get contentTimestamp()
    {
        return this._contentTimestamp;
    },

    setInitialContent: function(content)
    {
        this._content = content;
    },

    isLocallyModified: function()
    {
        return !!this._baseRevision;
    },

    setContent: function(newContent, onRevert)
    {
        var revisionResource = new WebInspector.Resource(null, this.url);
        revisionResource.type = this.type;
        revisionResource.loader = this.loader;
        revisionResource.timestamp = this.timestamp;
        revisionResource._content = this._content;
        revisionResource._actualResource = this;
        revisionResource._fireOnRevert = onRevert;

        if (this.finished)
            revisionResource.finished = true;
        else {
            function finished()
            {
                this.removeEventListener("finished", finished);
                revisionResource.finished = true;
            }
            this.addEventListener("finished", finished.bind(this));
        }

        if (!this._baseRevision)
            this._baseRevision = revisionResource;
        else
            revisionResource._baseRevision = this._baseRevision;

        var data = { revision: revisionResource };
        this._content = newContent;
        this.timestamp = new Date();
        this.dispatchEventToListeners("content-changed", data);
    },

    revertToThis: function()
    {
        if (!this._actualResource || !this._fireOnRevert)
            return;

        function callback(content)
        {
            if (content)
                this._fireOnRevert(content);
        }
        this.requestContent(callback.bind(this));
    },

    get baseRevision()
    {
        return this._baseRevision;
    },

    requestContent: function(callback)
    {
        // We do not support content retrieval for WebSockets at the moment.
        // Since WebSockets are potentially long-living, fail requests immediately
        // to prevent caller blocking until resource is marked as finished.
        if (this.type === WebInspector.Resource.Type.WebSocket) {
            callback(null, null);
            return;
        }
        if (typeof this._content !== "undefined") {
            callback(this.content, this._contentEncoded);
            return;
        }
        this._pendingContentCallbacks.push(callback);
        if (this.finished)
            this._innerRequestContent();
    },

    populateImageSource: function(image)
    {
        function onResourceContent()
        {
            image.src = this._contentURL();
        }

        if (Preferences.useDataURLForResourceImageIcons)
            this.requestContent(onResourceContent.bind(this));
        else
            image.src = this.url;
    },

    _contentURL: function()
    {
        var maxDataUrlSize = 1024 * 1024;
        // If resource content is not available or won't fit a data URL, fall back to using original URL.
        if (this._content == null || this._content.length > maxDataUrlSize)
            return this.url;

        return "data:" + this.mimeType + (this._contentEncoded ? ";base64," : ",") + this._content;
    },

    _innerRequestContent: function()
    {
        if (this._contentRequested)
            return;
        this._contentRequested = true;
        this._contentEncoded = !WebInspector.Resource.Type.isTextType(this.type);

        function onResourceContent(data)
        {
            this._content = data;
            var callbacks = this._pendingContentCallbacks.slice();
            for (var i = 0; i < callbacks.length; ++i)
                callbacks[i](this._content, this._contentEncoded);
            this._pendingContentCallbacks.length = 0;
            delete this._contentRequested;
        }
        WebInspector.networkManager.requestContent(this, this._contentEncoded, onResourceContent.bind(this));
    }
}

WebInspector.Resource.prototype.__proto__ = WebInspector.Object.prototype;
