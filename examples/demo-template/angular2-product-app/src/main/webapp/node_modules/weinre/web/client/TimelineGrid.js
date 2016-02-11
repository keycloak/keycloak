/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2008, 2009 Anthony Ricaud <rik@webkit.org>
 * Copyright (C) 2009 Google Inc. All rights reserved.
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

WebInspector.TimelineGrid = function()
{
    this.element = document.createElement("div");

    this._itemsGraphsElement = document.createElement("div");
    this._itemsGraphsElement.id = "resources-graphs";
    this.element.appendChild(this._itemsGraphsElement);

    this._dividersElement = document.createElement("div");
    this._dividersElement.className = "resources-dividers";
    this.element.appendChild(this._dividersElement);

    this._eventDividersElement = document.createElement("div");
    this._eventDividersElement.className = "resources-event-dividers";
    this.element.appendChild(this._eventDividersElement);

    this._dividersLabelBarElement = document.createElement("div");
    this._dividersLabelBarElement.className = "resources-dividers-label-bar";
    this.element.appendChild(this._dividersLabelBarElement);
}

WebInspector.TimelineGrid.prototype = {
    get itemsGraphsElement()
    {
        return this._itemsGraphsElement;
    },

    
    updateDividers: function(force, calculator, paddingLeft)
    {
        var dividerCount = Math.round(this._dividersElement.offsetWidth / 64);
        var slice = calculator.boundarySpan / dividerCount;
        if (!force && this._currentDividerSlice === slice)
            return false;

        if (typeof paddingLeft !== "number")
            paddingLeft = 0;
        this._currentDividerSlice = slice;

        // Reuse divider elements and labels.
        var divider = this._dividersElement.firstChild;
        var dividerLabelBar = this._dividersLabelBarElement.firstChild;

        var dividersLabelBarElementClientWidth = this._dividersLabelBarElement.clientWidth;
        var clientWidth = dividersLabelBarElementClientWidth - paddingLeft;
        for (var i = paddingLeft ? 0 : 1; i <= dividerCount; ++i) {
            if (!divider) {
                divider = document.createElement("div");
                divider.className = "resources-divider";
                this._dividersElement.appendChild(divider);

                dividerLabelBar = document.createElement("div");
                dividerLabelBar.className = "resources-divider";
                var label = document.createElement("div");
                label.className = "resources-divider-label";
                dividerLabelBar._labelElement = label;
                dividerLabelBar.appendChild(label);
                this._dividersLabelBarElement.appendChild(dividerLabelBar);
                dividersLabelBarElementClientWidth = this._dividersLabelBarElement.clientWidth;
            }

            if (i === (paddingLeft ? 0 : 1)) {
                divider.addStyleClass("first");
                dividerLabelBar.addStyleClass("first");
            } else {
                divider.removeStyleClass("first");
                dividerLabelBar.removeStyleClass("first");
            }

            if (i === dividerCount) {
                divider.addStyleClass("last");
                dividerLabelBar.addStyleClass("last");
            } else {
                divider.removeStyleClass("last");
                dividerLabelBar.removeStyleClass("last");
            }

            var left = paddingLeft + clientWidth * (i / dividerCount);
            var percentLeft = 100 * left / dividersLabelBarElementClientWidth;
            this._setDividerAndBarLeft(divider, dividerLabelBar, percentLeft);

            if (!isNaN(slice))
                dividerLabelBar._labelElement.textContent = calculator.formatValue(slice * i);
            else
                dividerLabelBar._labelElement.textContent = "";

            divider = divider.nextSibling;
            dividerLabelBar = dividerLabelBar.nextSibling;
        }

        // Remove extras.
        while (divider) {
            var nextDivider = divider.nextSibling;
            this._dividersElement.removeChild(divider);
            divider = nextDivider;
        }
        while (dividerLabelBar) {
            var nextDivider = dividerLabelBar.nextSibling;
            this._dividersLabelBarElement.removeChild(dividerLabelBar);
            dividerLabelBar = nextDivider;
        }
        return true;
    },

    _setDividerAndBarLeft: function(divider, dividerLabelBar, percentLeft)
    {
        var percentStyleLeft = parseFloat(divider.style.left);
        if (!isNaN(percentStyleLeft) && Math.abs(percentStyleLeft - percentLeft) < 0.1)
            return;
        divider.style.left = percentLeft + "%";
        dividerLabelBar.style.left = percentLeft + "%";
    },

    addEventDivider: function(divider)
    {
        this._eventDividersElement.appendChild(divider);
    },

    addEventDividers: function(dividers)
    {
        this.element.removeChild(this._eventDividersElement);
        for (var i = 0; i < dividers.length; ++i)
            if (dividers[i])
                this._eventDividersElement.appendChild(dividers[i]);
        this.element.appendChild(this._eventDividersElement);
    },

    removeEventDividers: function()
    {
        this._eventDividersElement.removeChildren();
    },

    hideEventDividers: function()
    {
        this._eventDividersElement.addStyleClass("hidden");
    },

    showEventDividers: function()
    {
        this._eventDividersElement.removeStyleClass("hidden");
    },

    setScrollAndDividerTop: function(scrollTop, dividersTop)
    {
        this._dividersElement.style.top = scrollTop + "px";
        this._eventDividersElement.style.top = scrollTop + "px";
        this._dividersLabelBarElement.style.top = dividersTop + "px";
    }
}
