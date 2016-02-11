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

// See http://groups.google.com/group/http-archive-specification/web/har-1-2-spec
// for HAR specification.

WebInspector.HAREntry = function(resource)
{
    this._resource = resource;
}

WebInspector.HAREntry.prototype = {
    build: function()
    {
        return {
            pageref: this._resource.documentURL,
            startedDateTime: new Date(this._resource.startTime * 1000),
            time: WebInspector.HAREntry._toMilliseconds(this._resource.duration),
            request: this._buildRequest(),
            response: this._buildResponse(),
            // cache: {...}, -- Not supproted yet.
            timings: this._buildTimings()
        };
    },

    _buildRequest: function()
    {
        var res = {
            method: this._resource.requestMethod,
            url: this._resource.url,
            // httpVersion: "HTTP/1.1" -- Not available.
            headers: this._buildHeaders(this._resource.requestHeaders),
            headersSize: -1, // Not available.
            bodySize: -1 // Not available.
        };
        if (this._resource.queryParameters)
            res.queryString = this._buildParameters(this._resource.queryParameters);
        if (this._resource.requestFormData)
            res.postData = this._buildPostData();
        if (this._resource.requestCookies)
            res.cookies = this._buildCookies(this._resource.requestCookies);
        return res;
    },

    _buildResponse: function()
    {
        var res = {
            status: this._resource.statusCode,
            statusText: this._resource.statusText,
            // "httpVersion": "HTTP/1.1" -- Not available.
            headers: this._buildHeaders(this._resource.responseHeaders),
            content: this._buildContent(),
            redirectURL: this._resource.responseHeaderValue("Location") || "",
            headersSize: -1, // Not available.
            bodySize: this._resource.resourceSize
        };
        if (this._resource.responseCookies)
            res.cookies = this._buildCookies(this._resource.responseCookies);
        return res;
    },

    _buildContent: function()
    {
        return {
            size: this._resource.resourceSize,
            // compression: 0, -- Not available.
            mimeType: this._resource.mimeType,
            // text: -- Not available.
        };
    },

    _buildTimings: function()
    {
        var waitForConnection = this._interval("connectStart", "connectEnd");
        var blocked;
        var connect;
        var dns = this._interval("dnsStart", "dnsEnd");
        var send = this._interval("sendStart", "sendEnd");
        var ssl = this._interval("sslStart", "sslEnd");

        if (ssl !== -1 && send !== -1)
            send -= ssl;

        if (this._resource.connectionReused) {
            connect = -1;
            blocked = waitForConnection;
        } else {
            blocked = 0;
            connect = waitForConnection;
            if (dns !== -1)
                connect -= dns;
        }

        return {
            blocked: blocked,
            dns: dns,
            connect: connect,
            send: send,
            wait: this._interval("sendEnd", "receiveHeadersEnd"),
            receive: WebInspector.HAREntry._toMilliseconds(this._resource.receiveDuration),
            ssl: ssl
        };
    },

    _buildHeaders: function(headers)
    {
        var result = [];
        for (var name in headers)
            result.push({ name: name, value: headers[name] });
        return result;
    },

    _buildPostData: function()
    {
        var res = {
            mimeType: this._resource.requestHeaderValue("Content-Type"),
            text: this._resource.requestFormData
        };
        if (this._resource.formParameters)
           res.params = this._buildParameters(this._resource.formParameters);
        return res;
    },

    _buildParameters: function(parameters)
    {
        return parameters.slice();
    },

    _buildCookies: function(cookies)
    {
        return cookies.map(this._buildCookie.bind(this));
    },

    _buildCookie: function(cookie)
    {
        
        return {
            name: cookie.name,
            value: cookie.value,
            path: cookie.path,
            domain: cookie.domain,
            expires: cookie.expires(new Date(this._resource.startTime * 1000)),
            httpOnly: cookie.httpOnly,
            secure: cookie.secure
        };
    },

    _interval: function(start, end)
    {
        var timing = this._resource.timing;
        if (!timing)
            return -1;
        var startTime = timing[start];
        return typeof startTime !== "number" || startTime === -1 ? -1 : Math.round(timing[end] - startTime);
    }
}

WebInspector.HAREntry._toMilliseconds = function(time)
{
    return time === -1 ? -1 : Math.round(time * 1000);
}

WebInspector.HARLog = function()
{
    this.includeResourceIds = false;
}

WebInspector.HARLog.prototype = {
    build: function()
    {
        var webKitVersion = /AppleWebKit\/([^ ]+)/.exec(window.navigator.userAgent);
        
        return {
            version: "1.2",
            creator: {
                name: "WebInspector",
                version: webKitVersion ? webKitVersion[1] : "n/a"
            },
            pages: this._buildPages(),
            entries: WebInspector.networkResources.map(this._convertResource.bind(this))
        }
    },

    _buildPages: function()
    {
        return [
            {
                startedDateTime: new Date(WebInspector.mainResource.startTime * 1000),
                id: WebInspector.mainResource.documentURL,
                title: "",
                pageTimings: this.buildMainResourceTimings()
            }
        ];
    },

    buildMainResourceTimings: function()
    {
        return {
             onContentLoad: this._pageEventTime(WebInspector.mainResourceDOMContentTime),
             onLoad: this._pageEventTime(WebInspector.mainResourceLoadTime),
        }
    },

    _convertResource: function(resource)
    {
        var entry = (new WebInspector.HAREntry(resource)).build();
        if (this.includeResourceIds)
            entry._resourceId = resource.identifier;
        return entry;
    },

    _pageEventTime: function(time)
    {
        var startTime = WebInspector.mainResource.startTime;
        if (time === -1 || startTime === -1)
            return -1;
        return WebInspector.HAREntry._toMilliseconds(time - startTime);
    }
}
