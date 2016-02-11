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

WebInspector.GoToLineDialog = function(view)
{
    this._element = document.createElement("div");
    this._element.className = "go-to-line-dialog";
    this._element.addEventListener("keydown", this._onKeyDown.bind(this), false);
    this._closeKeys = [
        WebInspector.KeyboardShortcut.Keys.Enter.code,
        WebInspector.KeyboardShortcut.Keys.Esc.code,
    ];

    var dialogWindow = this._element;

    dialogWindow.createChild("label").innerText = WebInspector.UIString("Go to line: ");

    this._input = dialogWindow.createChild("input");
    this._input.setAttribute("type", "text");
    this._input.setAttribute("size", 6);
    var linesCount = view.textModel.linesCount;
    if (linesCount)
        this._input.setAttribute("title", WebInspector.UIString("1 - %d", linesCount));
    var blurHandler = this._onBlur.bind(this);
    this._input.addEventListener("blur", blurHandler, false);
    

    var go = dialogWindow.createChild("button");
    go.innerText = WebInspector.UIString("Go");
    go.addEventListener("click", this._onClick.bind(this), false);
    go.addEventListener("mousedown", function(e) {
        // Ok button click will close the dialog, removing onBlur listener
        // to let click event be handled.
        this._input.removeEventListener("blur", blurHandler, false);
    }.bind(this), false);

    this._view = view;
    view.element.appendChild(this._element);

    this._previousFocusElement = WebInspector.currentFocusElement;
    WebInspector.currentFocusElement = this._input;
    this._input.select();
}

WebInspector.GoToLineDialog.show = function(sourceView)
{
    if (this._instance)
        return;
    this._instance = new WebInspector.GoToLineDialog(sourceView);
}

WebInspector.GoToLineDialog.prototype = {
    _hide: function()
    {
        if (this._isHiding)
            return;
        this._isHiding = true;

        WebInspector.currentFocusElement = this._previousFocusElement;
        WebInspector.GoToLineDialog._instance = null;
        this._element.parentElement.removeChild(this._element);
    },

    _onBlur: function(event)
    {
        this._hide();
    },

    _onKeyDown: function(event)
    {
        if (event.keyCode === WebInspector.KeyboardShortcut.Keys.Tab.code) {
            event.preventDefault();
            return;
        }

        if (event.keyCode === WebInspector.KeyboardShortcut.Keys.Enter.code)
            this._highlightSelectedLine();

        if (this._closeKeys.indexOf(event.keyCode) >= 0) {
            this._hide();
            event.stopPropagation();
        }
    },

    _onClick: function(event)
    {
        this._highlightSelectedLine();
        this._hide();
    },

    _highlightSelectedLine: function()
    {
        var value = this._input.value;
        var lineNumber = parseInt(value, 10);
        if (!isNaN(lineNumber) && lineNumber > 0) {
            lineNumber = Math.min(lineNumber, this._view.textModel.linesCount);
            this._view.highlightLine(lineNumber);
        }
    }
};
