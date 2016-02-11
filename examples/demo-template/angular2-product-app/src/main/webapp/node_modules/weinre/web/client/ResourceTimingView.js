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

WebInspector.ResourceTimingView = function(resource)
{
    WebInspector.View.call(this);
    this.element.addStyleClass("resource-timing-view");

    this._resource = resource;

    resource.addEventListener("timing changed", this._refresh, this);
}

WebInspector.ResourceTimingView.prototype = {
    show: function(parentElement)
    {
        if (!this._resource.timing) {
            if (!this._emptyMsgElement) {
                this._emptyMsgElement = document.createElement("div");
                this._emptyMsgElement.className = "storage-empty-view";
                this._emptyMsgElement.textContent = WebInspector.UIString("This request has no detailed timing info.");
                this.element.appendChild(this._emptyMsgElement);
            }
            WebInspector.View.prototype.show.call(this, parentElement);
            return;
        }

        if (this._emptyMsgElement)
            this._emptyMsgElement.parentElement.removeChild(this._emptyMsgElement);

        this._refresh();
        WebInspector.View.prototype.show.call(this, parentElement);
    },

    _refresh: function()
    {
        if (this._tableElement)
            this._tableElement.parentElement.removeChild(this._tableElement);

        this._tableElement = WebInspector.ResourceTimingView.createTimingTable(this._resource);
        this.element.appendChild(this._tableElement);
    }
}

WebInspector.ResourceTimingView.createTimingTable = function(resource)
{
    var tableElement = document.createElement("table");
    var rows = [];

    function addRow(title, className, start, end, color)
    {
        var row = {};
        row.title = title;
        row.className = className;
        row.start = start;
        row.end = end;
        rows.push(row);
    }

    if (resource.timing.proxyStart !== -1)
        addRow(WebInspector.UIString("Proxy"), "proxy", resource.timing.proxyStart, resource.timing.proxyEnd);

    if (resource.timing.dnsStart !== -1)
        addRow(WebInspector.UIString("DNS Lookup"), "dns", resource.timing.dnsStart, resource.timing.dnsEnd);

    if (resource.timing.connectStart !== -1) {
        if (resource.connectionReused)
            addRow(WebInspector.UIString("Blocking"), "connecting", resource.timing.connectStart, resource.timing.connectEnd);
        else {
            var connectStart = resource.timing.connectStart;
            // Connection includes DNS, subtract it here.
            if (resource.timing.dnsStart !== -1)
                connectStart += resource.timing.dnsEnd - resource.timing.dnsStart;
            addRow(WebInspector.UIString("Connecting"), "connecting", connectStart, resource.timing.connectEnd);
        }
    }

    if (resource.timing.sslStart !== -1)
        addRow(WebInspector.UIString("SSL"), "ssl", resource.timing.sslStart, resource.timing.sslEnd);

    var sendStart = resource.timing.sendStart;
    if (resource.timing.sslStart !== -1)
        sendStart += resource.timing.sslEnd - resource.timing.sslStart;
    
    addRow(WebInspector.UIString("Sending"), "sending", resource.timing.sendStart, resource.timing.sendEnd);
    addRow(WebInspector.UIString("Waiting"), "waiting", resource.timing.sendEnd, resource.timing.receiveHeadersEnd);
    addRow(WebInspector.UIString("Receiving"), "receiving", (resource.responseReceivedTime - resource.timing.requestTime) * 1000, (resource.endTime - resource.timing.requestTime) * 1000);

    var chartWidth = 200;
    var total = (resource.endTime - resource.timing.requestTime) * 1000;
    var scale = chartWidth / total;

    for (var i = 0; i < rows.length; ++i) {
        var tr = document.createElement("tr");
        tableElement.appendChild(tr);

        var td = document.createElement("td");
        td.textContent = rows[i].title;
        tr.appendChild(td);

        td = document.createElement("td");
        td.width = chartWidth + "px";

        var row = document.createElement("div");
        row.className = "network-timing-row";
        td.appendChild(row);

        var bar = document.createElement("span");
        bar.className = "network-timing-bar " + rows[i].className;
        bar.style.left = scale * rows[i].start + "px";
        bar.style.right = scale * (total - rows[i].end) + "px";
        bar.style.backgroundColor = rows[i].color;
        bar.textContent = "\u200B"; // Important for 0-time items to have 0 width.
        row.appendChild(bar);

        var title = document.createElement("span");
        title.className = "network-timing-bar-title";
        if (total - rows[i].end < rows[i].start)
            title.style.right = (scale * (total - rows[i].end) + 3) + "px";
        else
            title.style.left = (scale * rows[i].start + 3) + "px";
        title.textContent = Number.millisToString(rows[i].end - rows[i].start);
        row.appendChild(title);

        tr.appendChild(td);
    }
    return tableElement;
}

WebInspector.ResourceTimingView.prototype.__proto__ = WebInspector.View.prototype;
