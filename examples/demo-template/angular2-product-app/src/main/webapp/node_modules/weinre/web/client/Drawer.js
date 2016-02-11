/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
 * Copyright (C) 2009 Joseph Pecoraro
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

WebInspector.Drawer = function()
{
    WebInspector.View.call(this, document.getElementById("drawer"));

    this._savedHeight = 200; // Default.
    this.state = WebInspector.Drawer.State.Hidden;
    this.fullPanel = false;

    this._mainElement = document.getElementById("main");
    this._toolbarElement = document.getElementById("toolbar");
    this._mainStatusBar = document.getElementById("main-status-bar");
    this._mainStatusBar.addEventListener("mousedown", this._startStatusBarDragging.bind(this), true);
    this._viewStatusBar = document.getElementById("other-drawer-status-bar-items");
    this._counters = document.getElementById("counters");
    this._drawerStatusBar = document.getElementById("drawer-status-bar");
}

WebInspector.Drawer.prototype = {
    get visibleView()
    {
        return this._visibleView;
    },

    set visibleView(x)
    {
        if (this._visibleView === x) {
            if (this.visible && this.fullPanel)
                return;
            this.visible = !this.visible;
            return;
        }

        var firstTime = !this._visibleView;
        if (this._visibleView)
            this._visibleView.hide();

        this._visibleView = x;

        if (x && !firstTime) {
            this._safelyRemoveChildren();
            this._viewStatusBar.removeChildren(); // optimize this? call old.detach()
            x.attach(this.element, this._viewStatusBar);
            x.show();
            this.visible = true;
        }
    },

    get savedHeight()
    {
        var height = this._savedHeight || this.element.offsetHeight;
        return Number.constrain(height, Preferences.minConsoleHeight, window.innerHeight - this._mainElement.totalOffsetTop - Preferences.minConsoleHeight);
    },

    showView: function(view)
    {
        if (!this.visible || this.visibleView !== view)
            this.visibleView = view;
    },

    show: function()
    {
        if (this._animating || this.visible)
            return;

        if (this.visibleView)
            this.visibleView.show();

        WebInspector.View.prototype.show.call(this);

        this._animating = true;

        document.body.addStyleClass("drawer-visible");

        var anchoredItems = document.getElementById("anchored-status-bar-items");
        var height = (this.fullPanel ? window.innerHeight - this._toolbarElement.offsetHeight : this.savedHeight);
        var animations = [
            {element: this.element, end: {height: height}},
            {element: this._mainElement, end: {bottom: height}},
            {element: this._mainStatusBar, start: {"padding-left": anchoredItems.offsetWidth - 1}, end: {"padding-left": 0}},
            {element: this._viewStatusBar, start: {opacity: 0}, end: {opacity: 1}}
        ];

        this._drawerStatusBar.insertBefore(anchoredItems, this._drawerStatusBar.firstChild);

        if (this._currentPanelCounters) {
            var oldRight = this._drawerStatusBar.clientWidth - (this._counters.offsetLeft + this._currentPanelCounters.offsetWidth);
            var newRight = WebInspector.Panel.counterRightMargin;
            var rightPadding = (oldRight - newRight);
            animations.push({element: this._currentPanelCounters, start: {"padding-right": rightPadding}, end: {"padding-right": 0}});
            this._currentPanelCounters.parentNode.removeChild(this._currentPanelCounters);
            this._mainStatusBar.appendChild(this._currentPanelCounters);
        }

        function animationFinished()
        {
            if ("updateStatusBarItems" in WebInspector.currentPanel)
                WebInspector.currentPanel.updateStatusBarItems();
            if (this.visibleView.afterShow)
                this.visibleView.afterShow();
            delete this._animating;
            delete this._currentAnimation;
            this.state = (this.fullPanel ? WebInspector.Drawer.State.Full : WebInspector.Drawer.State.Variable);
            if (this._currentPanelCounters)
                this._currentPanelCounters.removeAttribute("style");
        }

        this._currentAnimation = WebInspector.animateStyle(animations, this._animationDuration(), animationFinished.bind(this));
    },

    hide: function()
    {
        if (this._animating || !this.visible)
            return;

        WebInspector.View.prototype.hide.call(this);

        if (this.visibleView)
            this.visibleView.hide();

        this._animating = true;

        if (!this.fullPanel)
            this._savedHeight = this.element.offsetHeight;

        if (this.element === WebInspector.currentFocusElement || this.element.isAncestor(WebInspector.currentFocusElement))
            WebInspector.currentFocusElement = WebInspector.previousFocusElement;

        var anchoredItems = document.getElementById("anchored-status-bar-items");

        // Temporarily set properties and classes to mimic the post-animation values so panels
        // like Elements in their updateStatusBarItems call will size things to fit the final location.
        this._mainStatusBar.style.setProperty("padding-left", (anchoredItems.offsetWidth - 1) + "px");
        document.body.removeStyleClass("drawer-visible");
        if ("updateStatusBarItems" in WebInspector.currentPanel)
            WebInspector.currentPanel.updateStatusBarItems();
        document.body.addStyleClass("drawer-visible");

        var animations = [
            {element: this._mainElement, end: {bottom: 0}},
            {element: this._mainStatusBar, start: {"padding-left": 0}, end: {"padding-left": anchoredItems.offsetWidth - 1}},
            {element: this._viewStatusBar, start: {opacity: 1}, end: {opacity: 0}}
        ];

        if (this._currentPanelCounters) {
            var newRight = this._drawerStatusBar.clientWidth - this._counters.offsetLeft;
            var oldRight = this._mainStatusBar.clientWidth - (this._currentPanelCounters.offsetLeft + this._currentPanelCounters.offsetWidth);
            var rightPadding = (newRight - oldRight);
            animations.push({element: this._currentPanelCounters, start: {"padding-right": 0}, end: {"padding-right": rightPadding}});
        }

        function animationFinished()
        {
            WebInspector.currentPanel.resize();
            this._mainStatusBar.insertBefore(anchoredItems, this._mainStatusBar.firstChild);
            this._mainStatusBar.style.removeProperty("padding-left");

            if (this._currentPanelCounters) {
                this._currentPanelCounters.setAttribute("style", null);
                this._currentPanelCounters.parentNode.removeChild(this._currentPanelCounters);
                this._counters.insertBefore(this._currentPanelCounters, this._counters.firstChild);
            }

            document.body.removeStyleClass("drawer-visible");
            delete this._animating;
            delete this._currentAnimation;
            this.state = WebInspector.Drawer.State.Hidden;
        }

        this._currentAnimation = WebInspector.animateStyle(animations, this._animationDuration(), animationFinished.bind(this));
    },

    resize: function()
    {
        if (this.state === WebInspector.Drawer.State.Hidden)
            return;

        var height;
        if (this.state === WebInspector.Drawer.State.Variable) {
            height = parseInt(this.element.style.height);
            height = Number.constrain(height, Preferences.minConsoleHeight, window.innerHeight - this._mainElement.totalOffsetTop - Preferences.minConsoleHeight);
        } else
            height = window.innerHeight - this._toolbarElement.offsetHeight;

        this._mainElement.style.bottom = height + "px";
        this.element.style.height = height + "px";
    },

    enterPanelMode: function()
    {
        this._cancelAnimationIfNeeded();
        this.fullPanel = true;
        
        if (this.visible) {
            this._savedHeight = this.element.offsetHeight;
            var height = window.innerHeight - this._toolbarElement.offsetHeight;
            this._animateDrawerHeight(height, WebInspector.Drawer.State.Full);
        }
    },

    exitPanelMode: function()
    {
        this._cancelAnimationIfNeeded();
        this.fullPanel = false;

        if (this.visible) {
            // If this animation gets cancelled, we want the state of the drawer to be Variable,
            // so that the new animation can't do an immediate transition between Hidden/Full states.
            this.state = WebInspector.Drawer.State.Variable;
            var height = this.savedHeight;
            this._animateDrawerHeight(height, WebInspector.Drawer.State.Variable);
        }
    },

    immediatelyExitPanelMode: function()
    {
        this.visible = false;
        this.fullPanel = false;
    },

    immediatelyFinishAnimation: function()
    {
        if (this._currentAnimation)
            this._currentAnimation.forceComplete();
    },

    set currentPanelCounters(x)
    {
        if (!x) {
            if (this._currentPanelCounters)
                this._currentPanelCounters.parentElement.removeChild(this._currentPanelCounters);
            delete this._currentPanelCounters;
            return;
        }

        this._currentPanelCounters = x;
        if (this.visible)
            this._mainStatusBar.appendChild(x);
        else
            this._counters.insertBefore(x, this._counters.firstChild);
    },

    _cancelAnimationIfNeeded: function()
    {
        if (this._animating) {
            if (this._currentAnimation)
                this._currentAnimation.cancel();
            delete this._animating;
            delete this._currentAnimation;
        }
    },

    _animateDrawerHeight: function(height, finalState)
    {
        this._animating = true;
        var animations = [
            {element: this.element, end: {height: height}},
            {element: this._mainElement, end: {bottom: height}}
        ];

        function animationFinished()
        {
            delete this._animating;
            delete this._currentAnimation;
            this.state = finalState;
        }

        this._currentAnimation = WebInspector.animateStyle(animations, this._animationDuration(), animationFinished.bind(this));
    },

    _animationDuration: function()
    {
        // Immediate if going between Hidden and Full in full panel mode
        if (this.fullPanel && (this.state === WebInspector.Drawer.State.Hidden || this.state === WebInspector.Drawer.State.Full))
            return 0;

        return (window.event && window.event.shiftKey ? 2000 : 250);
    },

    _safelyRemoveChildren: function()
    {
        var child = this.element.firstChild;
        while (child) {
            if (child.id !== "drawer-status-bar") {
                var moveTo = child.nextSibling;
                this.element.removeChild(child);
                child = moveTo;
            } else
                child = child.nextSibling;
        }
    },

    _startStatusBarDragging: function(event)
    {
        if (!this.visible || event.target !== this._mainStatusBar)
            return;

        WebInspector.elementDragStart(this._mainStatusBar, this._statusBarDragging.bind(this), this._endStatusBarDragging.bind(this), event, "row-resize");

        this._statusBarDragOffset = event.pageY - this.element.totalOffsetTop;

        event.stopPropagation();
    },

    _statusBarDragging: function(event)
    {
        var height = window.innerHeight - event.pageY + this._statusBarDragOffset;
        height = Number.constrain(height, Preferences.minConsoleHeight, window.innerHeight - this._mainElement.totalOffsetTop - Preferences.minConsoleHeight);

        this._mainElement.style.bottom = height + "px";
        this.element.style.height = height + "px";

        event.preventDefault();
        event.stopPropagation();
    },

    _endStatusBarDragging: function(event)
    {
        WebInspector.elementDragEnd(event);

        this._savedHeight = this.element.offsetHeight;
        delete this._statusBarDragOffset;

        event.stopPropagation();
    }
}

WebInspector.Drawer.prototype.__proto__ = WebInspector.View.prototype;

WebInspector.Drawer.State = {
    Hidden: 0,
    Variable: 1,
    Full: 2
};
