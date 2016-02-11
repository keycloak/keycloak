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

WebInspector.CallStackSidebarPane = function()
{
    WebInspector.SidebarPane.call(this, WebInspector.UIString("Call Stack"));
}

WebInspector.CallStackSidebarPane.prototype = {
    update: function(details)
    {
        this.bodyElement.removeChildren();

        this.placards = [];
        delete this._selectedCallFrame;

        if (!details) {
            var infoElement = document.createElement("div");
            infoElement.className = "info";
            infoElement.textContent = WebInspector.UIString("Not Paused");
            this.bodyElement.appendChild(infoElement);
            return;
        }

        var callFrames = details.callFrames;
        var title;
        var subtitle;
        var script;

        for (var i = 0; i < callFrames.length; ++i) {
            var callFrame = callFrames[i];
            switch (callFrame.type) {
            case "function":
                title = callFrame.functionName || WebInspector.UIString("(anonymous function)");
                break;
            case "program":
                title = WebInspector.UIString("(program)");
                break;
            }

            script = WebInspector.debuggerModel.scriptForSourceID(callFrame.sourceID);
            if (script)
                subtitle = WebInspector.displayNameForURL(script.sourceURL);
            else
                subtitle = WebInspector.UIString("(internal script)");

            if (callFrame.line > 0) {
                if (subtitle)
                    subtitle += ":" + callFrame.line;
                else
                    subtitle = WebInspector.UIString("line %d", callFrame.line);
            }

            var placard = new WebInspector.Placard(title, subtitle);
            placard.callFrame = callFrame;

            placard.element.addEventListener("click", this._placardSelected.bind(this), false);

            this.placards.push(placard);
            this.bodyElement.appendChild(placard.element);
        }

        if (details.breakpoint)
            this._scriptBreakpointHit();
        else if (details.eventType === WebInspector.DebuggerEventTypes.NativeBreakpoint)
            this._nativeBreakpointHit(details.eventData);
    },

    get selectedCallFrame()
    {
        return this._selectedCallFrame;
    },

    set selectedCallFrame(x)
    {
        this._selectedCallFrame = x;

        for (var i = 0; i < this.placards.length; ++i) {
            var placard = this.placards[i];
            placard.selected = (placard.callFrame === this._selectedCallFrame);
        }

        this.dispatchEventToListeners("call frame selected");
    },

    handleShortcut: function(event)
    {
        var shortcut = WebInspector.KeyboardShortcut.makeKeyFromEvent(event);
        var handler = this._shortcuts[shortcut];
        if (handler) {
            handler(event);
            event.handled = true;
        }
    },

    _selectNextCallFrameOnStack: function()
    {
        var index = this._selectedCallFrameIndex();
        if (index == -1)
            return;
        this._selectedPlacardByIndex(index + 1);
    },

    _selectPreviousCallFrameOnStack: function()
    {
        var index = this._selectedCallFrameIndex();
        if (index == -1)
            return;
        this._selectedPlacardByIndex(index - 1);
    },

    _selectedPlacardByIndex: function(index)
    {
        if (index < 0 || index >= this.placards.length)
            return;
        var placard = this.placards[index];
        this.selectedCallFrame = placard.callFrame
    },

    _selectedCallFrameIndex: function()
    {
        if (!this._selectedCallFrame)
            return -1;
        for (var i = 0; i < this.placards.length; ++i) {
            var placard = this.placards[i];
            if (placard.callFrame === this._selectedCallFrame)
                return i;
        }
        return -1;
    },

    _placardSelected: function(event)
    {
        var placardElement = event.target.enclosingNodeOrSelfWithClass("placard");
        this.selectedCallFrame = placardElement.placard.callFrame;
    },

    registerShortcuts: function(section)
    {
        this._shortcuts = {};

        var nextCallFrame = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Period,
            WebInspector.KeyboardShortcut.Modifiers.Ctrl);
        this._shortcuts[nextCallFrame.key] = this._selectNextCallFrameOnStack.bind(this);

        var prevCallFrame = WebInspector.KeyboardShortcut.makeDescriptor(WebInspector.KeyboardShortcut.Keys.Comma,
            WebInspector.KeyboardShortcut.Modifiers.Ctrl);
        this._shortcuts[prevCallFrame.key] = this._selectPreviousCallFrameOnStack.bind(this);

        section.addRelatedKeys([ nextCallFrame.name, prevCallFrame.name ], WebInspector.UIString("Next/previous call frame"));
    },

    _scriptBreakpointHit: function()
    {
        var statusMessageElement = document.createElement("div");
        statusMessageElement.className = "info";
        statusMessageElement.appendChild(document.createTextNode(WebInspector.UIString("Paused on a JavaScript breakpoint.")));
        this.bodyElement.appendChild(statusMessageElement);
    },

    _nativeBreakpointHit: function(eventData)
    {
        var breakpoint = WebInspector.breakpointManager.breakpointViewForEventData(eventData);
        if (!breakpoint)
            return;

        var statusMessageElement = document.createElement("div");
        statusMessageElement.className = "info";
        breakpoint.populateStatusMessageElement(statusMessageElement, eventData);
        this.bodyElement.appendChild(statusMessageElement);
    }
}

WebInspector.CallStackSidebarPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;
