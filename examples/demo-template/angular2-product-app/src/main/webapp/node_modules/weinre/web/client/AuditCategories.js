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

WebInspector.AuditCategories.PagePerformance = function() {
    WebInspector.AuditCategory.call(this, WebInspector.AuditCategories.PagePerformance.AuditCategoryName);
}

WebInspector.AuditCategories.PagePerformance.AuditCategoryName = "Web Page Performance";

WebInspector.AuditCategories.PagePerformance.prototype = {
    initialize: function()
    {
        this.addRule(new WebInspector.AuditRules.UnusedCssRule(), WebInspector.AuditRule.Severity.Warning);
        this.addRule(new WebInspector.AuditRules.CssInHeadRule(), WebInspector.AuditRule.Severity.Severe);
        this.addRule(new WebInspector.AuditRules.StylesScriptsOrderRule(), WebInspector.AuditRule.Severity.Severe);
    }
}

WebInspector.AuditCategories.PagePerformance.prototype.__proto__ = WebInspector.AuditCategory.prototype;

WebInspector.AuditCategories.NetworkUtilization = function() {
    WebInspector.AuditCategory.call(this, WebInspector.AuditCategories.NetworkUtilization.AuditCategoryName);
}

WebInspector.AuditCategories.NetworkUtilization.AuditCategoryName = "Network Utilization";

WebInspector.AuditCategories.NetworkUtilization.prototype = {
    initialize: function()
    {
        this.addRule(new WebInspector.AuditRules.GzipRule(), WebInspector.AuditRule.Severity.Severe);
        this.addRule(new WebInspector.AuditRules.ImageDimensionsRule(), WebInspector.AuditRule.Severity.Warning);
        this.addRule(new WebInspector.AuditRules.CookieSizeRule(400), WebInspector.AuditRule.Severity.Warning);
        this.addRule(new WebInspector.AuditRules.StaticCookielessRule(5), WebInspector.AuditRule.Severity.Warning);
        this.addRule(new WebInspector.AuditRules.CombineJsResourcesRule(2), WebInspector.AuditRule.Severity.Severe);
        this.addRule(new WebInspector.AuditRules.CombineCssResourcesRule(2), WebInspector.AuditRule.Severity.Severe);
        this.addRule(new WebInspector.AuditRules.MinimizeDnsLookupsRule(4), WebInspector.AuditRule.Severity.Warning);
        this.addRule(new WebInspector.AuditRules.ParallelizeDownloadRule(4, 10, 0.5), WebInspector.AuditRule.Severity.Warning);
        this.addRule(new WebInspector.AuditRules.BrowserCacheControlRule(), WebInspector.AuditRule.Severity.Severe);
        this.addRule(new WebInspector.AuditRules.ProxyCacheControlRule(), WebInspector.AuditRule.Severity.Warning);
    }
}

WebInspector.AuditCategories.NetworkUtilization.prototype.__proto__ = WebInspector.AuditCategory.prototype;
