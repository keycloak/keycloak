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

WebInspector.WorkersSidebarPane = function()
{
    WebInspector.SidebarPane.call(this, WebInspector.UIString("Workers"));
    
    this._workers = {};

    this._enableWorkersCheckbox = new WebInspector.Checkbox(
        WebInspector.UIString("Debug"),
        "sidebar-pane-subtitle",
        WebInspector.UIString("Allow debugging workers. Enabling this option will replace native workers with the iframe-based JavaScript implementation"));
    this.titleElement.insertBefore(this._enableWorkersCheckbox.element, this.titleElement.firstChild);

    this._enableWorkersCheckbox.addEventListener(this._onTriggerInstrument.bind(this));
    this._enableWorkersCheckbox.checked = false;

    this._listElement = document.createElement("ol");
    this._listElement.className = "workers-list";

    this.bodyElement.appendChild(this._listElement);
    this._treeOutline = new TreeOutline(this._listElement);
}

WebInspector.WorkersSidebarPane.prototype = {
    addWorker: function(id, url, isShared)
    {
        if (id in this._workers) 
            return;
        var worker = new WebInspector.Worker(id, url, isShared);
        this._workers[id] = worker;

        var title = WebInspector.linkifyURL(url, WebInspector.displayNameForURL(url), "worker-item", true, url);
        var treeElement = new TreeElement(null, worker, false);
        treeElement.titleHTML = title;
        this._treeOutline.appendChild(treeElement);
    },

    removeWorker: function(id)
    {
        if (id in this._workers) {
            this._treeOutline.removeChild(this._treeOutline.findTreeElement(this._workers[id]));
            delete this._workers[id];
        }
    },

    setInstrumentation: function(enabled)
    {
        InspectorBackend.removeAllScriptsToEvaluateOnLoad();
        if (enabled)
            InspectorBackend.addScriptToEvaluateOnLoad("(" + InjectedFakeWorker + ")");
    },

    reset: function()
    {
        this.setInstrumentation(this._enableWorkersCheckbox.checked);
        this._treeOutline.removeChildren();
        this._workers = {};
    },

    _onTriggerInstrument: function(event)
    {
        this.setInstrumentation(this._enableWorkersCheckbox.checked);
    }
};

WebInspector.WorkersSidebarPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;

WebInspector.Worker = function(id, url, shared)
{
    this.id = id;
    this.url = url;
    this.shared = shared;
}
