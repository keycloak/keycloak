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

WebInspector.Popover = function(contentElement)
{
    this.element = document.createElement("div");
    this.element.className = "popover";

    this._popupArrowElement = document.createElement("div");
    this._popupArrowElement.className = "arrow";
    this.element.appendChild(this._popupArrowElement);

    this.contentElement = contentElement;
    this._contentDiv = document.createElement("div");
    this._contentDiv.className = "content";
}

WebInspector.Popover.prototype = {
    show: function(anchor, preferredWidth, preferredHeight)
    {
        // This should not happen, but we hide previous popup to be on the safe side.
        if (WebInspector.Popover._popoverElement)
            document.body.removeChild(WebInspector.Popover._popoverElement);
        WebInspector.Popover._popoverElement = this.element;

        // Temporarily attach in order to measure preferred dimensions.
        this.contentElement.positionAt(0, 0);
        document.body.appendChild(this.contentElement);
        var preferredWidth = preferredWidth || this.contentElement.offsetWidth;
        var preferredHeight = preferredHeight || this.contentElement.offsetHeight;

        this._contentDiv.appendChild(this.contentElement);
        this.element.appendChild(this._contentDiv);
        document.body.appendChild(this.element);
        this._positionElement(anchor, preferredWidth, preferredHeight);
    },

    hide: function()
    {
        if (WebInspector.Popover._popoverElement) {
            delete WebInspector.Popover._popoverElement;
            document.body.removeChild(this.element);
        }
    },

    _positionElement: function(anchorElement, preferredWidth, preferredHeight)
    {
        var borderWidth = 25;
        var scrollerWidth = 11;
        var arrowHeight = 15;
        var arrowOffset = 10;
        var borderRadius = 10;

        // Skinny tooltips are not pretty, their arrow location is not nice.
        preferredWidth = Math.max(preferredWidth, 50);
        var totalWidth = window.innerWidth;
        var totalHeight = window.innerHeight;

        var anchorBox = {x: anchorElement.totalOffsetLeft, y: anchorElement.totalOffsetTop, width: anchorElement.offsetWidth, height: anchorElement.offsetHeight};
        while (anchorElement !== document.body) {
            if (anchorElement.scrollLeft)
                anchorBox.x -= anchorElement.scrollLeft;
            if (anchorElement.scrollTop)
                anchorBox.y -= anchorElement.scrollTop;
            anchorElement = anchorElement.parentElement;
        }

        var newElementPosition = { x: 0, y: 0, width: preferredWidth + scrollerWidth, height: preferredHeight };

        var verticalAlignment;
        var roomAbove = anchorBox.y;
        var roomBelow = totalHeight - anchorBox.y - anchorBox.height;

        if (roomAbove > roomBelow) {
            // Positioning above the anchor.
            if (anchorBox.y > newElementPosition.height + arrowHeight + borderRadius)
                newElementPosition.y = anchorBox.y - newElementPosition.height - arrowHeight;
            else {
                newElementPosition.y = borderRadius * 2;
                newElementPosition.height = anchorBox.y - borderRadius * 2 - arrowHeight;
            }
            verticalAlignment = "bottom";
        } else {
            // Positioning below the anchor.
            newElementPosition.y = anchorBox.y + anchorBox.height + arrowHeight;
            if (newElementPosition.y + newElementPosition.height + arrowHeight - borderWidth >= totalHeight)
                newElementPosition.height = totalHeight - anchorBox.y - anchorBox.height - borderRadius * 2 - arrowHeight;
            // Align arrow.
            verticalAlignment = "top";
        }

        var horizontalAlignment;
        if (anchorBox.x + newElementPosition.width < totalWidth) {
            newElementPosition.x = Math.max(borderRadius, anchorBox.x - borderRadius - arrowOffset);
            horizontalAlignment = "left";
        } else if (newElementPosition.width + borderRadius * 2 < totalWidth) {
            newElementPosition.x = totalWidth - newElementPosition.width - borderRadius;
            horizontalAlignment = "right";
            // Position arrow accurately.
            var arrowRightPosition = Math.max(0, totalWidth - anchorBox.x - anchorBox.width - borderRadius - arrowOffset);
            arrowRightPosition += anchorBox.width / 2;
            this._popupArrowElement.style.right = arrowRightPosition + "px";
        } else {
            newElementPosition.x = borderRadius;
            newElementPosition.width = totalWidth - borderRadius * 2;
            newElementPosition.height += scrollerWidth;
            horizontalAlignment = "left";
            if (verticalAlignment === "bottom")
                newElementPosition.y -= scrollerWidth;
            // Position arrow accurately.
            this._popupArrowElement.style.left = Math.max(0, anchorBox.x - borderRadius * 2 - arrowOffset) + "px";
            this._popupArrowElement.style.left += anchorBox.width / 2;
        }

        this.element.className = "popover " + verticalAlignment + "-" + horizontalAlignment + "-arrow";
        this.element.positionAt(newElementPosition.x - borderWidth, newElementPosition.y - borderWidth);
        this.element.style.width = newElementPosition.width + borderWidth * 2 + "px";
        this.element.style.height = newElementPosition.height + borderWidth * 2 + "px";
    }
}

WebInspector.PopoverHelper = function(panelElement, getAnchor, showPopup, showOnClick, onHide)
{
    this._panelElement = panelElement;
    this._getAnchor = getAnchor;
    this._showPopup = showPopup;
    this._showOnClick = showOnClick;
    this._onHide = onHide;
    panelElement.addEventListener("mousedown", this._mouseDown.bind(this), false);
    panelElement.addEventListener("mousemove", this._mouseMove.bind(this), false);
    this.setTimeout(1000);
}

WebInspector.PopoverHelper.prototype = {
    setTimeout: function(timeout)
    {
         this._timeout = timeout;
    },

    _mouseDown: function(event)
    {
        this._killHidePopupTimer();
        this._handleMouseAction(event, true);
    },

    _mouseMove: function(event)
    {
        // Pretend that nothing has happened.
        if (this._hoverElement === event.target || (this._hoverElement && this._hoverElement.isAncestor(event.target)))
            return;

        // User has 500ms (this._timeout / 2) to reach the popup.
        if (this._popup && !this._hidePopupTimer) {
            var self = this;
            function doHide()
            {
                self._hidePopup();
                delete self._hidePopupTimer;
            }
            this._hidePopupTimer = setTimeout(doHide, this._timeout / 2);
        }

        this._handleMouseAction(event);
    },

    _handleMouseAction: function(event, isMouseDown)
    {
        this._resetHoverTimer();

        this._hoverElement = this._getAnchor(event.target);
        if (!this._hoverElement)
            return;

        var toolTipDelay = isMouseDown ? 0 : (this._popup ? this._timeout * 0.6 : this._timeout);
        this._hoverTimer = setTimeout(this._mouseHover.bind(this, this._hoverElement), toolTipDelay);
    },

    _resetHoverTimer: function()
    {
        if (this._hoverTimer) {
            clearTimeout(this._hoverTimer);
            delete this._hoverTimer;
        }
    },

    hidePopup: function()
    {
        this._resetHoverTimer();
        this._hidePopup();
    },

    _hidePopup: function()
    {
        if (!this._popup)
            return;

        if (this._onHide)
            this._onHide();

        this._popup.hide();
        delete this._popup;
    },

    _mouseHover: function(element)
    {
        delete this._hoverTimer;

        this._popup = this._showPopup(element);
        if (this._popup)
            this._popup.contentElement.addEventListener("mousemove", this._killHidePopupTimer.bind(this), true);
    },

    _killHidePopupTimer: function()
    {
        if (this._hidePopupTimer) {
            clearTimeout(this._hidePopupTimer);
            delete this._hidePopupTimer;

            // We know that we reached the popup, but we might have moved over other elements.
            // Discard pending command.
            this._resetHoverTimer();
        }
    }
}
