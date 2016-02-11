/*
 * Copyright (C) 2008 Apple Inc. All Rights Reserved.
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

WebInspector.WelcomeView = function(identifier, headingText, instructionsText)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("panel-enabler-view");
    this.element.addStyleClass(identifier);
    this.element.addStyleClass("welcome");

    this.contentElement = document.createElement("div");
    this.contentElement.className = "panel-enabler-view-content";
    this.element.appendChild(this.contentElement);

    this.alignerElement = document.createElement("div");
    this.alignerElement.className = "welcome-instructions-aligner";
    this.contentElement.appendChild(this.alignerElement);

    this.instructionsElement = document.createElement("div");
    this.instructionsElement.className = "instructions";
    this.contentElement.appendChild(this.instructionsElement);

    this.headerElement = document.createElement("h1");
    this.headerElement.textContent = headingText;
    this.instructionsElement.appendChild(this.headerElement);

    if (instructionsText)
        this.addMessage(instructionsText);
}

WebInspector.WelcomeView.prototype = {
    addMessage: function(message)
    {
        var messageElement = document.createElement("div");
        messageElement.className = "message";
        if (typeof message == "string")
            // Message text can contain <br> tags for better text balancing, so we
            // put it into elements using 'innerHTML', not 'textContent'.
            messageElement.innerHTML = message;
        else
            messageElement.appendChild(message);
        this.instructionsElement.appendChild(messageElement);
    }
}

WebInspector.WelcomeView.prototype.__proto__ = WebInspector.View.prototype;
