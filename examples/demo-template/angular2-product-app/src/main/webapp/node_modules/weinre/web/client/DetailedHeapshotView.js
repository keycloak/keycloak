/*
 * Copyright (C) 2011 Google Inc. All rights reserved.
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

WebInspector.DetailedHeapshotView = function(parent, profile)
{
    WebInspector.View.call(this);

    this.element.addStyleClass("heap-snapshot-view");

    this.parent = parent;
    this.profile = profile;
}

WebInspector.DetailedHeapshotView.prototype = {
    get profile()
    {
        return this._profile;
    },

    set profile(profile)
    {
        this._profile = profile;
    }
};

WebInspector.DetailedHeapshotView.prototype.__proto__ = WebInspector.View.prototype;

WebInspector.DetailedHeapshotProfileType = function()
{
    WebInspector.ProfileType.call(this, WebInspector.HeapSnapshotProfileType.TypeId, WebInspector.UIString("HEAP SNAPSHOTS"));
}

WebInspector.DetailedHeapshotProfileType.prototype = {
    get buttonTooltip()
    {
        return WebInspector.UIString("Take heap snapshot.");
    },

    get buttonStyle()
    {
        return "heap-snapshot-status-bar-item status-bar-item";
    },

    buttonClicked: function()
    {
        WebInspector.panels.profiles.takeHeapSnapshot(true);
    },

    get welcomeMessage()
    {
        return WebInspector.UIString("Get a heap snapshot by pressing the %s button on the status bar.");
    },

    createSidebarTreeElementForProfile: function(profile)
    {
        return new WebInspector.ProfileSidebarTreeElement(profile, WebInspector.UIString("Snapshot %d"), "heap-snapshot-sidebar-tree-item");
    },

    createView: function(profile)
    {
        return new WebInspector.DetailedHeapshotView(WebInspector.panels.profiles, profile);
    }
}

WebInspector.DetailedHeapshotProfileType.prototype.__proto__ = WebInspector.ProfileType.prototype;
