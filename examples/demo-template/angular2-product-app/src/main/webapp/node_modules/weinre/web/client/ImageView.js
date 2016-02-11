/*
 * Copyright (C) 2007, 2008 Apple Inc.  All rights reserved.
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

WebInspector.ImageView = function(resource)
{
    WebInspector.ResourceView.call(this, resource);

    this.element.addStyleClass("image");
}

WebInspector.ImageView.prototype = {
    hasContent: function()
    {
        return true;
    },

    show: function(parentElement)
    {
        WebInspector.ResourceView.prototype.show.call(this, parentElement);
        this._createContentIfNeeded();
    },

    _createContentIfNeeded: function()
    {
        if (this._container)
            return;

        var imageContainer = document.createElement("div");
        imageContainer.className = "image";
        this.element.appendChild(imageContainer);

        var imagePreviewElement = document.createElement("img");
        imagePreviewElement.addStyleClass("resource-image-view");
        imageContainer.appendChild(imagePreviewElement);

        this._container = document.createElement("div");
        this._container.className = "info";
        this.element.appendChild(this._container);

        var imageNameElement = document.createElement("h1");
        imageNameElement.className = "title";
        imageNameElement.textContent = this.resource.displayName;
        this._container.appendChild(imageNameElement);

        var infoListElement = document.createElement("dl");
        infoListElement.className = "infoList";

        this.resource.populateImageSource(imagePreviewElement);

        function onImageLoad()
        {
            var content = this.resource.content;
            if (content)
                var resourceSize = this._base64ToSize(content);
            else
                var resourceSize = this.resource.resourceSize;

            var imageProperties = [
                { name: WebInspector.UIString("Dimensions"), value: WebInspector.UIString("%d × %d", imagePreviewElement.naturalWidth, imagePreviewElement.naturalHeight) },
                { name: WebInspector.UIString("File size"), value: Number.bytesToString(resourceSize) },
                { name: WebInspector.UIString("MIME type"), value: this.resource.mimeType }
            ];
    
            infoListElement.removeChildren();
            for (var i = 0; i < imageProperties.length; ++i) {
                var dt = document.createElement("dt");
                dt.textContent = imageProperties[i].name;
                infoListElement.appendChild(dt);
                var dd = document.createElement("dd");
                dd.textContent = imageProperties[i].value;
                infoListElement.appendChild(dd);
            }
            var dt = document.createElement("dt");
            dt.textContent = WebInspector.UIString("URL");
            infoListElement.appendChild(dt);
            var dd = document.createElement("dd");
            dd.appendChild(WebInspector.linkifyURLAsNode(this.resource.url));
            infoListElement.appendChild(dd);

            this._container.appendChild(infoListElement);
        }
        imagePreviewElement.addEventListener("load", onImageLoad.bind(this), false);
    },

    _base64ToSize: function(content)
    {
        if (!content.length)
            return 0;
        var size = (content.length || 0) * 3 / 4;
        if (content.length > 0 && content[content.length - 1] === "=")
            size--;
        if (content.length > 1 && content[content.length - 2] === "=")
            size--;
        return size;
    }
}

WebInspector.ImageView.prototype.__proto__ = WebInspector.ResourceView.prototype;
