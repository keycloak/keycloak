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

// Ideally, we would rely on platform support for parsing a cookie, since
// this would save us from any potential inconsistency. However, exposing
// platform cookie parsing logic would require quite a bit of additional 
// plumbing, and at least some platforms lack support for parsing Cookie,
// which is in a format slightly different from Set-Cookie and is normally 
// only required on the server side.

WebInspector.CookieParser = function()
{
}

WebInspector.CookieParser.prototype = {
    get cookies()
    {
        return this._cookies;
    },

    parseCookie: function(cookieHeader)
    {
        if (!this._initialize(cookieHeader))
            return;

        for (var kv = this._extractKeyValue(); kv; kv = this._extractKeyValue()) {
            if (kv.key.charAt(0) === "$" && this._lastCookie)
                this._lastCookie.addAttribute(kv.key.slice(1), kv.value);
            else if (kv.key.toLowerCase() !== "$version" && typeof kv.value === "string")
                this._addCookie(kv, WebInspector.Cookie.Type.Request);
            this._advanceAndCheckCookieDelimiter();
        }
        this._flushCookie();
        return this._cookies;
    },

    parseSetCookie: function(setCookieHeader)
    {
        if (!this._initialize(setCookieHeader))
            return;
        for (var kv = this._extractKeyValue(); kv; kv = this._extractKeyValue()) {
            if (this._lastCookie)
                this._lastCookie.addAttribute(kv.key, kv.value);
            else 
                this._addCookie(kv, WebInspector.Cookie.Type.Response);
            if (this._advanceAndCheckCookieDelimiter())
                this._flushCookie();
        }
        this._flushCookie();
        return this._cookies;
    },

    _initialize: function(headerValue)
    {
        this._input = headerValue;
        if (typeof headerValue !== "string")
            return false;
        this._cookies = [];
        this._lastCookie = null;
        this._originalInputLength = this._input.length;
        return true;
    },

    _flushCookie: function()
    {
        if (this._lastCookie)
            this._lastCookie.size = this._originalInputLength - this._input.length - this._lastCookiePosition;
        this._lastCookie = null;
    },

    _extractKeyValue: function()
    {
        if (!this._input || !this._input.length)
            return null;
        // Note: RFCs offer an option for quoted values that may contain commas and semicolons.
        // Many browsers/platforms do not support this, however (see http://webkit.org/b/16699
        // and http://crbug.com/12361). The logic below matches latest versions of IE, Firefox,
        // Chrome and Safari on some old platforms. The latest version of Safari supports quoted
        // cookie values, though. 
        var keyValueMatch = /^[ \t]*([^\s=;]+)[ \t]*(?:=[ \t]*([^;\n]*))?/.exec(this._input);
        if (!keyValueMatch) {
            console.log("Failed parsing cookie header before: " + this._input);
            return null;
        }

        var result = {
            key: keyValueMatch[1],
            value: keyValueMatch[2] && keyValueMatch[2].trim(),
            position: this._originalInputLength - this._input.length
        };
        this._input = this._input.slice(keyValueMatch[0].length);
        return result;
    },

    _advanceAndCheckCookieDelimiter: function()
    {
        var match = /^\s*[\n;]\s*/.exec(this._input);
        if (!match)
            return false;
        this._input = this._input.slice(match[0].length);
        return match[0].match("\n") !== null;
    },

    _addCookie: function(keyValue, type)
    {
        if (this._lastCookie)
            this._lastCookie.size = keyValue.position - this._lastCookiePosition;
        // Mozilla bug 169091: Mozilla, IE and Chrome treat single token (w/o "=") as
        // specifying a value for a cookie with empty name.
        this._lastCookie = keyValue.value ? new WebInspector.Cookie(keyValue.key, keyValue.value, type) :
            new WebInspector.Cookie("", keyValue.key, type);
        this._lastCookiePosition = keyValue.position;
        this._cookies.push(this._lastCookie);
    }
};

WebInspector.CookieParser.parseCookie = function(header)
{
    return (new WebInspector.CookieParser()).parseCookie(header);
}

WebInspector.CookieParser.parseSetCookie = function(header)
{
    return (new WebInspector.CookieParser()).parseSetCookie(header);
}

WebInspector.Cookie = function(name, value, type)
{
    this.name = name;
    this.value = value;
    this.type = type;
    this._attributes = {};
}

WebInspector.Cookie.prototype = {
    get httpOnly()
    {
        return "httponly" in this._attributes;
    },

    get secure()
    {
        return "secure" in this._attributes;
    },

    get session()
    {
        // RFC 2965 suggests using Discard attribute to mark session cookies, but this does not seem to be widely used.
        // Check for absence of explicity max-age or expiry date instead.
        return  !("expries" in this._attributes || "max-age" in this._attributes);
    },

    get path()
    {
        return this._attributes.path;
    },

    get domain()
    {
        return this._attributes.domain;
    },

    expires: function(requestDate)
    {
        return this._attributes.expires ? new Date(this._attributes.expires) :
            (this._attributes["max-age"] ? new Date(requestDate.getTime() + 1000 * this._attributes["max-age"]) : null);
    },

    get attributes()
    {
        return this._attributes;
    },

    addAttribute: function(key, value)
    {
        this._attributes[key.toLowerCase()] = value;
    }
}

WebInspector.Cookie.Type = {
    Request: 0,
    Response: 1
};
