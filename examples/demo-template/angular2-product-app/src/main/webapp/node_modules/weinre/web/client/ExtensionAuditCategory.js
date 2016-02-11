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

WebInspector.ExtensionAuditCategory = function(id, displayName, ruleCount)
{
    this._id = id;
    this._displayName = displayName;
    this._ruleCount  = ruleCount;
}

WebInspector.ExtensionAuditCategory.prototype = {
    // AuditCategory interface
    get id()
    {
        return this._id;
    },

    get displayName()
    {
        return this._displayName;
    },

    get ruleCount()
    {
        return this._ruleCount;
    },

    run: function(resources, callback)
    {
        new WebInspector.ExtensionAuditCategoryResults(this, callback);
    }
}

WebInspector.ExtensionAuditCategoryResults = function(category, callback)
{
    this._category = category;
    this._pendingRules = category.ruleCount;
    this._ruleCompletionCallback = callback;

    this.id = category.id + "-" + ++WebInspector.ExtensionAuditCategoryResults._lastId;
    WebInspector.extensionServer.startAuditRun(category, this);
}

WebInspector.ExtensionAuditCategoryResults.prototype = {
    get complete()
    {
        return !this._pendingRules;
    },

    cancel: function()
    {
        while (!this.complete)
            this._addResult(null);
    },

    addResult: function(displayName, description, severity, details)
    {
        var result = new WebInspector.AuditRuleResult(displayName);
        result.addChild(description);
        result.severity = severity;
        if (details)
            this._addNode(result, details);
        this._addResult(result);
    },

    _addNode: function(parent, node)
    {
        var addedNode = parent.addChild(node.contents, node.expanded);
        if (node.children) {
            for (var i = 0; i < node.children.length; ++i)
                this._addNode(addedNode, node.children[i]);
        }
    },

    _addResult: function(result)
    {
        this._ruleCompletionCallback(result);
        this._pendingRules--;
        if (!this._pendingRules)
            WebInspector.extensionServer.stopAuditRun(this);
    }
}

WebInspector.ExtensionAuditCategoryResults._lastId = 0;
