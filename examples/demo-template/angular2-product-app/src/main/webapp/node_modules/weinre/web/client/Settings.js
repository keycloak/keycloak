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


var Preferences = {
    canEditScriptSource: false,
    maxInlineTextChildLength: 80,
    minConsoleHeight: 75,
    minSidebarWidth: 100,
    minElementsSidebarWidth: 200,
    minScriptsSidebarWidth: 200,
    styleRulesExpandedState: {},
    showMissingLocalizedStrings: false,
    samplingCPUProfiler: false,
    showColorNicknames: true,
    debuggerAlwaysEnabled: false,
    profilerAlwaysEnabled: false,
    onlineDetectionEnabled: true,
    nativeInstrumentationEnabled: false,
    resourceExportEnabled: false,
    useDataURLForResourceImageIcons: true,
    showTimingTab: false,
    showCookiesTab: false,
    debugMode: false,
    heapProfilerPresent: false,
    detailedHeapProfiles: false
}

WebInspector.Settings = function()
{
    this.installApplicationSetting("colorFormat", "hex");
    this.installApplicationSetting("consoleHistory", []);
    this.installApplicationSetting("debuggerEnabled", false);
    this.installApplicationSetting("profilerEnabled", false);
    this.installApplicationSetting("eventListenersFilter", "all");
    this.installApplicationSetting("lastActivePanel", "elements");
    this.installApplicationSetting("lastViewedScriptFile", "application");
    this.installApplicationSetting("monitoringXHREnabled", false);
    this.installApplicationSetting("pauseOnExceptionState", WebInspector.ScriptsPanel.PauseOnExceptionsState.DontPauseOnExceptions);
    this.installApplicationSetting("resourcesLargeRows", true);
    this.installApplicationSetting("resourcesSortOptions", {timeOption: "responseTime", sizeOption: "transferSize"});
    this.installApplicationSetting("resourceViewTab", "content");
    this.installApplicationSetting("showInheritedComputedStyleProperties", false);
    this.installApplicationSetting("showUserAgentStyles", true);
    this.installApplicationSetting("watchExpressions", []);
    this.installApplicationSetting("breakpoints", []);

    this.installProjectSetting("nativeBreakpoints", []);
}

WebInspector.Settings.Events = {
    ProjectChanged: "project-changed"
}

WebInspector.Settings.prototype = {
    installApplicationSetting: function(key, defaultValue)
    {
        if (key in this)
            return;

        Object.defineProperty(this, key,{
            get : this._get.bind(this, key, defaultValue),
            set : this._set.bind(this, key)});
    },

    installProjectSetting: function(key, defaultValue)
    {
        Object.defineProperty(this, key,{
            get : this._getProjectSetting.bind(this, key, defaultValue),
            set : this._setProjectSetting.bind(this, key)});

    },

    inspectedURLChanged: function(url)
    {
        var fragmentIndex = url.indexOf("#");
        if (fragmentIndex !== -1)
            url = url.substring(0, fragmentIndex);
        this._projectId = url;
        this.dispatchEventToListeners(WebInspector.Settings.Events.ProjectChanged);
    },

    get projectId()
    {
        return this._projectId;
    },

    findSettingForAllProjects: function(key)
    {
        var result = {};
        var regexp = "^" + key + ":(.*)";
        for (var i = 0; i < window.localStorage.length; ++i) {
            var fullKey =  window.localStorage.key(i);
            var match = fullKey.match(regexp);
            if (!match)
                continue;
            try {
                result[match[1]] = JSON.parse(window.localStorage[fullKey]);
            } catch(e) {
                window.localStorage.removeItem(fullKey);
            }
        }
        return result;
    },

    _get: function(key, defaultValue)
    {
        if (key in window.localStorage) {
            try {
                return JSON.parse(window.localStorage[key]);
            } catch(e) {
                window.localStorage.removeItem(key);
            }
        }
        return defaultValue;
    },

    _set: function(key, value)
    {
        window.localStorage[key] = JSON.stringify(value);
    },

    _getProjectSetting: function(key, defaultValue)
    {
        return this._get(this._formatProjectKey(key), defaultValue);
    },

    _setProjectSetting: function(key, value)
    {
        return this._set(this._formatProjectKey(key), value);
    },

    _formatProjectKey: function(key)
    {
        return key + ":" + this._projectId;
    }
}

WebInspector.Settings.prototype.__proto__ = WebInspector.Object.prototype;
