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


WebInspector.ResourceTreeModel = function()
{
    this.reset();
}

WebInspector.ResourceTreeModel.prototype = {
    reset: function()
    {
        this._resourcesByURL = {};
        this._resourcesByFrameId = {};
        this._subframes = {};
        if (WebInspector.panels)
            WebInspector.panels.resources.clear();
    },

    addOrUpdateFrame: function(frame)
    {
        var tmpResource = new WebInspector.Resource(null, frame.url);
        WebInspector.panels.resources.addOrUpdateFrame(frame.parentId, frame.id, frame.name, tmpResource.displayName);
        var subframes = this._subframes[frame.parentId];
        if (!subframes) {
            subframes = {};
            this._subframes[frame.parentId || 0] = subframes;
        }
        subframes[frame.id] = true;
    },

    didCommitLoadForFrame: function(frame, loader)
    {
        // frame.parentId === 0 is when main frame navigation happens.
        this._clearChildFramesAndResources(frame.parentId ? frame.id : 0, loader.loaderId);

        this.addOrUpdateFrame(frame);

        var resourcesForFrame = this._resourcesByFrameId[frame.id];
        for (var i = 0; resourcesForFrame && i < resourcesForFrame.length; ++i)
            WebInspector.panels.resources.addResourceToFrame(frame.id, resourcesForFrame[i]);
    },

    frameDetachedFromParent: function(frameId)
    {
        this._clearChildFramesAndResources(frameId, 0);
        WebInspector.panels.resources.removeFrame(frameId);
    },

    addResourceToFrame: function(frameId, resource)
    {
        var resourcesForFrame = this._resourcesByFrameId[frameId];
        if (!resourcesForFrame) {
            resourcesForFrame = [];
            this._resourcesByFrameId[frameId] = resourcesForFrame;
        }
        resourcesForFrame.push(resource);
        this._bindResourceURL(resource);

        WebInspector.panels.resources.addResourceToFrame(frameId, resource);
    },

    forAllResources: function(callback)
    {
        this._callForFrameResources(0, callback);
    },

    addConsoleMessage: function(msg)
    {
        var resource = this.resourceForURL(msg.url);
        if (!resource)
            return;

        switch (msg.level) {
        case WebInspector.ConsoleMessage.MessageLevel.Warning:
            resource.warnings += msg.repeatDelta;
            break;
        case WebInspector.ConsoleMessage.MessageLevel.Error:
            resource.errors += msg.repeatDelta;
            break;
        }

        var view = WebInspector.ResourceView.resourceViewForResource(resource);
        if (view.addMessage)
            view.addMessage(msg);
    },

    clearConsoleMessages: function()
    {
        function callback(resource)
        {
            resource.clearErrorsAndWarnings();
        }
        this.forAllResources(callback);
    },

    resourceForURL: function(url)
    {
        // FIXME: receive frameId here.
        var entry = this._resourcesByURL[url];
        if (entry instanceof Array)
            return entry[0];
        return entry;
    },

    _bindResourceURL: function(resource)
    {
        var resourceForURL = this._resourcesByURL[resource.url];
        if (!resourceForURL)
            this._resourcesByURL[resource.url] = resource;
        else if (resourceForURL instanceof Array)
            resourceForURL.push(resource);
        else
            this._resourcesByURL[resource.url] = [resourceForURL, resource];
    },

    _clearChildFramesAndResources: function(frameId, loaderId)
    {
        WebInspector.panels.resources.removeResourcesFromFrame(frameId);

        this._clearResources(frameId, loaderId);
        var subframes = this._subframes[frameId];
        if (!subframes)
            return;

        for (var childFrameId in subframes) {
            WebInspector.panels.resources.removeFrame(childFrameId);
            this._clearChildFramesAndResources(childFrameId, loaderId);
        }
        delete this._subframes[frameId];
    },

    _clearResources: function(frameId, loaderToPreserveId)
    {
        var resourcesForFrame = this._resourcesByFrameId[frameId];
        if (!resourcesForFrame)
            return;

        var preservedResourcesForFrame = [];
        for (var i = 0; i < resourcesForFrame.length; ++i) {
            var resource = resourcesForFrame[i];
            if (resource.loader.loaderId === loaderToPreserveId) {
                preservedResourcesForFrame.push(resource);
                continue;
            }
            this._unbindResourceURL(resource);
        }

        delete this._resourcesByFrameId[frameId];
        if (preservedResourcesForFrame.length)
            this._resourcesByFrameId[frameId] = preservedResourcesForFrame;
    },

    _callForFrameResources: function(frameId, callback)
    {
        var resources = this._resourcesByFrameId[frameId];
        for (var i = 0; resources && i < resources.length; ++i) {
            if (callback(resources[i]))
                return true;
        }
        
        var frames = this._subframes[frameId];
        if (frames) {
            for (var id in frames) {
                if (this._callForFrameResources(id, callback))
                    return true;
            }
        }
        return false;
    },

    _unbindResourceURL: function(resource)
    {
        var resourceForURL = this._resourcesByURL[resource.url];
        if (!resourceForURL)
            return;

        if (resourceForURL instanceof Array) {
            resourceForURL.remove(resource, true);
            if (resourceForURL.length === 1)
                this._resourcesByURL[resource.url] = resourceForURL[0];
            return;
        }

        delete this._resourcesByURL[resource.url];
    }
}
