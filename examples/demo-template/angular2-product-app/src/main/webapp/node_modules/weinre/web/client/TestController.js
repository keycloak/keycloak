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

WebInspector.TestController = function(callId)
{
    this._callId = callId;
    this._waitUntilDone = false;
    this.results = [];
}

WebInspector.TestController.prototype = {
    waitUntilDone: function()
    {
        this._waitUntilDone = true;
    },

    notifyDone: function(result)
    {
        if (typeof result === "undefined" && this.results.length)
            result = this.results;
        var message = typeof result === "undefined" ? "\"<undefined>\"" : JSON.stringify(result);
        InspectorBackend.didEvaluateForTestInFrontend(this._callId, message);
    },

    runAfterPendingDispatches: function(callback)
    {
        if (WebInspector.pendingDispatches === 0) {
            callback();
            return;
        }
        setTimeout(this.runAfterPendingDispatches.bind(this), 0, callback);
    }
}

WebInspector.evaluateForTestInFrontend = function(callId, script)
{
    var controller = new WebInspector.TestController(callId);
    function invokeMethod()
    {
        try {
            var result;
            if (window[script] && typeof window[script] === "function")
                result = window[script].call(WebInspector, controller);
            else
                result = window.eval(script);

            if (!controller._waitUntilDone)
                controller.notifyDone(result);
        } catch (e) {
            controller.notifyDone(e.toString());
        }
    }
    controller.runAfterPendingDispatches(invokeMethod);
}
