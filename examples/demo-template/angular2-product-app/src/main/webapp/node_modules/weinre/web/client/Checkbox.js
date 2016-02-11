/*
 * Copyright (C) 2010 Google Inc. All Rights Reserved.
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

WebInspector.Checkbox = function(label, className, tooltip)
{
    this.element = document.createElement('label');
    this._inputElement = document.createElement('input');
    this._inputElement.type = "checkbox";

    this.element.className = className;
    this.element.appendChild(this._inputElement);
    this.element.appendChild(document.createTextNode(label));
    if (tooltip)
        this.element.title = tooltip;
}

WebInspector.Checkbox.prototype = {
    set checked(checked)
    {
        this._inputElement.checked = checked;
    },

    get checked()
    {
        return this._inputElement.checked;
    },

    addEventListener: function(listener)
    {
        function listenerWrapper(event)
        {
            if (listener)
                listener(event);
            event.stopPropagation();
            return true;
        }

        this._inputElement.addEventListener("click", listenerWrapper, false);
        this.element.addEventListener("click", listenerWrapper, false);
    }
}
