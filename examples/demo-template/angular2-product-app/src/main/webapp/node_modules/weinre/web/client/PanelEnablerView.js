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

WebInspector.PanelEnablerView = function(identifier, headingText, disclaimerText, buttonTitle)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("panel-enabler-view");
    this.element.addStyleClass(identifier);

    this.contentElement = document.createElement("div");
    this.contentElement.className = "panel-enabler-view-content";
    this.element.appendChild(this.contentElement);

    this.imageElement = document.createElement("img");
    this.contentElement.appendChild(this.imageElement);

    this.choicesForm = document.createElement("form");
    this.contentElement.appendChild(this.choicesForm);

    this.headerElement = document.createElement("h1");
    this.headerElement.textContent = headingText;
    this.choicesForm.appendChild(this.headerElement);

    var self = this;
    function enableOption(text, checked) {
        var label = document.createElement("label");
        var option = document.createElement("input");
        option.type = "radio";
        option.name = "enable-option";
        if (checked)
            option.checked = true;
        label.appendChild(option);
        label.appendChild(document.createTextNode(text));
        self.choicesForm.appendChild(label);
        return option;
    };

    this.enabledForSession = enableOption(WebInspector.UIString("Only enable for this session"), true);
    this.enabledAlways = enableOption(WebInspector.UIString("Always enable"));

    this.disclaimerElement = document.createElement("div");
    this.disclaimerElement.className = "panel-enabler-disclaimer";
    this.disclaimerElement.textContent = disclaimerText;
    this.choicesForm.appendChild(this.disclaimerElement);

    this.enableButton = document.createElement("button");
    this.enableButton.setAttribute("type", "button");
    this.enableButton.textContent = buttonTitle;
    this.enableButton.addEventListener("click", this._enableButtonCicked.bind(this), false);
    this.choicesForm.appendChild(this.enableButton);
}

WebInspector.PanelEnablerView.prototype = {
    _enableButtonCicked: function()
    {
        this.dispatchEventToListeners("enable clicked");
    },

    show: function(parentElement)
    {
        WebInspector.View.prototype.show.call(this, parentElement);

        setTimeout(this.resize.bind(this), 0);
    },

    resize: function()
    {
        this.imageElement.removeStyleClass("hidden");

        if (this.element.offsetWidth < (this.choicesForm.offsetWidth + this.imageElement.offsetWidth))
            this.imageElement.addStyleClass("hidden");
    },

    get alwaysEnabled() {
        return this.enabledAlways.checked;
    }
}

WebInspector.PanelEnablerView.prototype.__proto__ = WebInspector.View.prototype;
