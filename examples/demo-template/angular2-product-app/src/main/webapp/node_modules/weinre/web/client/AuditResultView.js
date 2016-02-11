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

WebInspector.AuditResultView = function(categoryResults)
{
    WebInspector.View.call(this);
    this.element.className = "audit-result-view";

    function categorySorter(a, b) {
        return (a.title || "").localeCompare(b.title || "");
    }
    categoryResults.sort(categorySorter);
    for (var i = 0; i < categoryResults.length; ++i)
        this.element.appendChild(new WebInspector.AuditCategoryResultPane(categoryResults[i]).element);
}

WebInspector.AuditResultView.prototype.__proto__ = WebInspector.View.prototype;


WebInspector.AuditCategoryResultPane = function(categoryResult)
{
    WebInspector.SidebarPane.call(this, categoryResult.title);
    var treeOutlineElement = document.createElement("ol");
    this.bodyElement.addStyleClass("audit-result-tree");
    this.bodyElement.appendChild(treeOutlineElement);

    this._treeOutline = new TreeOutline(treeOutlineElement);
    this._treeOutline.expandTreeElementsWhenArrowing = true;

    function ruleSorter(a, b)
    {
        var result = WebInspector.AuditRule.SeverityOrder[a.severity || 0] - WebInspector.AuditRule.SeverityOrder[b.severity || 0];
        if (!result)
            result = (a.value || "").localeCompare(b.value || "");
        return result;
    }

    categoryResult.ruleResults.sort(ruleSorter);

    for (var i = 0; i < categoryResult.ruleResults.length; ++i) {
        var ruleResult = categoryResult.ruleResults[i];
        var treeElement = this._appendResult(this._treeOutline, ruleResult);
        treeElement.listItemElement.addStyleClass("audit-result");

        if (ruleResult.severity) {
            var severityElement = document.createElement("img");
            severityElement.className = "severity-" + ruleResult.severity;
            treeElement.listItemElement.appendChild(severityElement);
        }
    }
    this.expand();
}

WebInspector.AuditCategoryResultPane.prototype = {
    _appendResult: function(parentTreeElement, result)
    {
        var title = "";
        
        if (typeof result.value === "string") {
            title = result.value;
            if (result.violationCount)
                title = String.sprintf("%s (%d)", title, result.violationCount);
        }

        var treeElement = new TreeElement(null, null, !!result.children);
        treeElement.titleHTML = title;
        parentTreeElement.appendChild(treeElement);

        if (result.className)
            treeElement.listItemElement.addStyleClass(result.className);
        if (typeof result.value !== "string")
            treeElement.listItemElement.appendChild(WebInspector.applyFormatters(result.value));

        if (result.children) {
            for (var i = 0; i < result.children.length; ++i)
                this._appendResult(treeElement, result.children[i]);
        }
        if (result.expanded) {
            treeElement.listItemElement.removeStyleClass("parent");
            treeElement.listItemElement.addStyleClass("parent-expanded");
            treeElement.expand();
        }
        return treeElement;
    }
}

WebInspector.AuditCategoryResultPane.prototype.__proto__ = WebInspector.SidebarPane.prototype;
