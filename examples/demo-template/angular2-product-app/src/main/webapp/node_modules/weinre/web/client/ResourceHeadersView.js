/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) IBM Corp. 2009  All rights reserved.
 * Copyright (C) 2010 Google Inc. All rights reserved.
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

WebInspector.ResourceHeadersView = function(resource)
{
    WebInspector.View.call(this);
    this.element.addStyleClass("resource-headers-view");

    this._resource = resource;

    this._headersListElement = document.createElement("ol");
    this._headersListElement.className = "outline-disclosure";
    this.element.appendChild(this._headersListElement);

    this._headersTreeOutline = new TreeOutline(this._headersListElement);
    this._headersTreeOutline.expandTreeElementsWhenArrowing = true;

    this._urlTreeElement = new TreeElement("", null, false);
    this._urlTreeElement.selectable = false;
    this._headersTreeOutline.appendChild(this._urlTreeElement);

    this._requestMethodTreeElement = new TreeElement("", null, false);
    this._requestMethodTreeElement.selectable = false;
    this._headersTreeOutline.appendChild(this._requestMethodTreeElement);

    this._statusCodeTreeElement = new TreeElement("", null, false);
    this._statusCodeTreeElement.selectable = false;
    this._headersTreeOutline.appendChild(this._statusCodeTreeElement);
     
    this._requestHeadersTreeElement = new TreeElement("", null, true);
    this._requestHeadersTreeElement.expanded = true;
    this._requestHeadersTreeElement.selectable = false;
    this._headersTreeOutline.appendChild(this._requestHeadersTreeElement);

    this._decodeHover = WebInspector.UIString("Double-Click to toggle between URL encoded and decoded formats");
    this._decodeRequestParameters = true;

    this._queryStringTreeElement = new TreeElement("", null, true);
    this._queryStringTreeElement.expanded = true;
    this._queryStringTreeElement.selectable = false;
    this._queryStringTreeElement.hidden = true;
    this._headersTreeOutline.appendChild(this._queryStringTreeElement);

    this._formDataTreeElement = new TreeElement("", null, true);
    this._formDataTreeElement.expanded = true;
    this._formDataTreeElement.selectable = false;
    this._formDataTreeElement.hidden = true;
    this._headersTreeOutline.appendChild(this._formDataTreeElement);

    this._requestPayloadTreeElement = new TreeElement(WebInspector.UIString("Request Payload"), null, true);
    this._requestPayloadTreeElement.expanded = true;
    this._requestPayloadTreeElement.selectable = false;
    this._requestPayloadTreeElement.hidden = true;
    this._headersTreeOutline.appendChild(this._requestPayloadTreeElement);

    this._responseHeadersTreeElement = new TreeElement("", null, true);
    this._responseHeadersTreeElement.expanded = true;
    this._responseHeadersTreeElement.selectable = false;
    this._headersTreeOutline.appendChild(this._responseHeadersTreeElement);

    resource.addEventListener("requestHeaders changed", this._refreshRequestHeaders, this);
    resource.addEventListener("responseHeaders changed", this._refreshResponseHeaders, this);
    resource.addEventListener("finished", this._refreshHTTPInformation, this);

    this._refreshURL();
    this._refreshQueryString();
    this._refreshRequestHeaders();
    this._refreshResponseHeaders();
    this._refreshHTTPInformation();
}

WebInspector.ResourceHeadersView.prototype = {

    _refreshURL: function()
    {
        this._urlTreeElement.titleHTML = "<div class=\"header-name\">" + WebInspector.UIString("Request URL") + ":</div>" +
            "<div class=\"header-value source-code\">" + this._resource.url.escapeHTML() + "</div>";
    },

    _refreshQueryString: function()
    {
        var queryParameters = this._resource.queryParameters;
        this._queryStringTreeElement.hidden = !queryParameters;
        if (queryParameters)
            this._refreshParms(WebInspector.UIString("Query String Parameters"), queryParameters, this._queryStringTreeElement);
    },

    _refreshFormData: function()
    {
        this._formDataTreeElement.hidden = true;
        this._requestPayloadTreeElement.hidden = true;

        var formData = this._resource.requestFormData;
        if (!formData)
            return;

        var formParameters = this._resource.formParameters;
        if (formParameters) {
            this._formDataTreeElement.hidden = false;
            this._refreshParms(WebInspector.UIString("Form Data"), formParameters, this._formDataTreeElement);
        } else {
            this._requestPayloadTreeElement.hidden = false;
            this._refreshRequestPayload(formData);
        }
    },

    _refreshRequestPayload: function(formData)
    {
        this._requestPayloadTreeElement.removeChildren();

        var title = "<div class=\"raw-form-data header-value source-code\">" + formData.escapeHTML() + "</div>";
        var parmTreeElement = new TreeElement(null, null, false);
        parmTreeElement.titleHTML = title;
        parmTreeElement.selectable = false;
        this._requestPayloadTreeElement.appendChild(parmTreeElement);
    },

    _refreshParms: function(title, parms, parmsTreeElement)
    {
        parmsTreeElement.removeChildren();

        parmsTreeElement.titleHTML = title + "<span class=\"header-count\">" + WebInspector.UIString(" (%d)", parms.length) + "</span>";

        for (var i = 0; i < parms.length; ++i) {
            var name = parms[i].name;
            var value = parms[i].value;

            var errorDecoding = false;
            if (this._decodeRequestParameters) {
                if (value.indexOf("%") >= 0) {
                    try {
                        value = decodeURIComponent(value);
                    } catch(e) {
                        errorDecoding = true;
                    }
                }
                    
                value = value.replace(/\+/g, " ");
            }

            valueEscaped = value.escapeHTML();
            if (errorDecoding)
                valueEscaped += " <span class=\"error-message\">" + WebInspector.UIString("(unable to decode value)").escapeHTML() + "</span>";

            var title = "<div class=\"header-name\">" + name.escapeHTML() + ":</div>";
            title += "<div class=\"header-value source-code\">" + valueEscaped + "</div>";

            var parmTreeElement = new TreeElement(null, null, false);
            parmTreeElement.titleHTML = title;
            parmTreeElement.selectable = false;
            parmTreeElement.tooltip = this._decodeHover;
            parmTreeElement.ondblclick = this._toggleURLdecoding.bind(this);
            parmsTreeElement.appendChild(parmTreeElement);
        }
    },

    _toggleURLdecoding: function(event)
    {
        this._decodeRequestParameters = !this._decodeRequestParameters;
        this._refreshQueryString();
        this._refreshFormData();
    },

    _getHeaderValue: function(headers, key)
    {
        var lowerKey = key.toLowerCase();
        for (var testKey in headers) {
            if (testKey.toLowerCase() === lowerKey)
                return headers[testKey];
        }
    },

    _refreshRequestHeaders: function()
    {
        var additionalRow = null;
        if (typeof this._resource.webSocketRequestKey3 !== "undefined")
            additionalRow = {header: "(Key3)", value: this._resource.webSocketRequestKey3};
        this._refreshHeaders(WebInspector.UIString("Request Headers"), this._resource.sortedRequestHeaders, additionalRow, this._requestHeadersTreeElement);
        this._refreshFormData();
    },

    _refreshResponseHeaders: function()
    {
        var additionalRow = null;
        if (typeof this._resource.webSocketChallengeResponse !== "undefined")
            additionalRow = {header: "(Challenge Response)", value: this._resource.webSocketChallengeResponse};
        this._refreshHeaders(WebInspector.UIString("Response Headers"), this._resource.sortedResponseHeaders, additionalRow, this._responseHeadersTreeElement);
    },

    _refreshHTTPInformation: function()
    {
        var requestMethodElement = this._requestMethodTreeElement;
        requestMethodElement.hidden = !this._resource.statusCode;
        var statusCodeElement = this._statusCodeTreeElement;
        statusCodeElement.hidden = !this._resource.statusCode;
        var statusCodeImage = "";

        if (this._resource.statusCode) {
            var statusImageSource = "";
            if (this._resource.statusCode < 300 || this._resource.statusCode === 304)
                statusImageSource = "Images/successGreenDot.png";
            else if (this._resource.statusCode < 400)
                statusImageSource = "Images/warningOrangeDot.png";
            else
                statusImageSource = "Images/errorRedDot.png";

            var statusTextEscaped = this._resource.statusCode + " " + this._resource.statusText.escapeHTML();
            statusCodeImage = "<img class=\"resource-status-image\" src=\"" + statusImageSource + "\" title=\"" + statusTextEscaped + "\">";
    
            requestMethodElement.titleHTML = "<div class=\"header-name\">" + WebInspector.UIString("Request Method") + ":</div>" +
                "<div class=\"header-value source-code\">" + this._resource.requestMethod + "</div>";

            statusCodeElement.titleHTML = "<div class=\"header-name\">" + WebInspector.UIString("Status Code") + ":</div>" +
                statusCodeImage + "<div class=\"header-value source-code\">" + statusTextEscaped + "</div>";
        }
    },
    
    _refreshHeaders: function(title, headers, additionalRow, headersTreeElement)
    {
        headersTreeElement.removeChildren();

        var length = headers.length;
        headersTreeElement.titleHTML = title.escapeHTML() + "<span class=\"header-count\">" + WebInspector.UIString(" (%d)", length) + "</span>";
        headersTreeElement.hidden = !length;

        var length = headers.length;
        for (var i = 0; i < length; ++i) {
            var title = "<div class=\"header-name\">" + headers[i].header.escapeHTML() + ":</div>";
            title += "<div class=\"header-value source-code\">" + headers[i].value.escapeHTML() + "</div>"

            var headerTreeElement = new TreeElement(null, null, false);
            headerTreeElement.titleHTML = title;
            headerTreeElement.selectable = false;
            headersTreeElement.appendChild(headerTreeElement);
        }

        if (additionalRow) {
            var title = "<div class=\"header-name\">" + additionalRow.header.escapeHTML() + ":</div>";
            title += "<div class=\"header-value source-code\">" + additionalRow.value.escapeHTML() + "</div>"

            var headerTreeElement = new TreeElement(null, null, false);
            headerTreeElement.titleHTML = title;
            headerTreeElement.selectable = false;
            headersTreeElement.appendChild(headerTreeElement);
        }
    }
}

WebInspector.ResourceHeadersView.prototype.__proto__ = WebInspector.View.prototype;
