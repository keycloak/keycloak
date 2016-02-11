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

WebInspector.Object = function() {
}

WebInspector.Object.prototype = {
    addEventListener: function(eventType, listener, thisObject)
    {
        if (!("_listeners" in this))
            this._listeners = {};
        if (!(eventType in this._listeners))
            this._listeners[eventType] = [];
        this._listeners[eventType].push({ thisObject: thisObject, listener: listener });
    },

    removeEventListener: function(eventType, listener, thisObject)
    {
        if (!("_listeners" in this) || !(eventType in this._listeners))
            return;
        var listeners = this._listeners[eventType];
        for (var i = 0; i < listeners.length; ++i) {
            if (listener && listeners[i].listener === listener && listeners[i].thisObject === thisObject)
                listeners.splice(i, 1);
            else if (!listener && thisObject && listeners[i].thisObject === thisObject)
                listeners.splice(i, 1);
        }

        if (!listeners.length)
            delete this._listeners[eventType];
    },

    removeAllListeners: function()
    {
        delete this._listeners;
    },

    dispatchEventToListeners: function(eventType, eventData)
    {
        if (!("_listeners" in this) || !(eventType in this._listeners))
            return;

        var stoppedPropagation = false;

        function stopPropagation()
        {
            stoppedPropagation = true;
        }

        function preventDefault()
        {
            this.defaultPrevented = true;
        }

        var event = {target: this, type: eventType, data: eventData, defaultPrevented: false};
        event.stopPropagation = stopPropagation;
        event.preventDefault = preventDefault;

        var listeners = this._listeners[eventType].slice(0);
        for (var i = 0; i < listeners.length; ++i) {
            listeners[i].listener.call(listeners[i].thisObject, event);
            if (stoppedPropagation)
                break;
        }

        return event.defaultPrevented;
    }
}
