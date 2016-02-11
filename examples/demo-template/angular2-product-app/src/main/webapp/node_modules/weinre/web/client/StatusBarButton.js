/*
 * Copyright (C) 2009 Google Inc. All rights reserved.
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

WebInspector.StatusBarButton = function(title, className, states)
{
    this.element = document.createElement("button");
    this.element.className = className + " status-bar-item";
    this.element.addEventListener("click", this._clicked.bind(this), false);

    this.glyph = document.createElement("div");
    this.glyph.className = "glyph";
    this.element.appendChild(this.glyph);

    this.glyphShadow = document.createElement("div");
    this.glyphShadow.className = "glyph shadow";
    this.element.appendChild(this.glyphShadow);
    
    this.states = states;
    if (!states)
        this.states = 2;

    if (states == 2)
        this._state = false;
    else
        this._state = 0;
    
    this.title = title;
    this.disabled = false;
    this._visible = true;
}

WebInspector.StatusBarButton.prototype = {
    _clicked: function()
    {
        this.dispatchEventToListeners("click");
    },

    get disabled()
    {
        return this._disabled;
    },

    set disabled(x)
    {
        if (this._disabled === x)
            return;
        this._disabled = x;
        this.element.disabled = x;
    },

    get title()
    {
        return this._title;
    },

    set title(x)
    {
        if (this._title === x)
            return;
        this._title = x;
        this.element.title = x;
    },
    
    get state()
    {
        return this._state;
    },
    
    set state(x)
    {
        if (this._state === x)
            return;
        
        if (this.states === 2) {
            if (x)
                this.element.addStyleClass("toggled-on");
            else
                this.element.removeStyleClass("toggled-on");
        } else {
            if (x !== 0) {
                this.element.removeStyleClass("toggled-" + this._state);
                this.element.addStyleClass("toggled-" + x);
            } else 
                this.element.removeStyleClass("toggled-" + this._state);
        }
        this._state = x;
    },

    get toggled()
    {
        if (this.states !== 2)
            throw("Only used toggled when there are 2 states, otherwise, use state");
        return this.state;
    },

    set toggled(x)
    {
        if (this.states !== 2)
            throw("Only used toggled when there are 2 states, otherwise, use state");
        this.state = x;
    },

    get visible()
    {
        return this._visible;
    },

    set visible(x)
    {
        if (this._visible === x)
            return;

        if (x)
            this.element.removeStyleClass("hidden");
        else
            this.element.addStyleClass("hidden");
        this._visible = x;
    }
}

WebInspector.StatusBarButton.prototype.__proto__ = WebInspector.Object.prototype;
