/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

var weinre_protocol = location.protocol
var weinre_host     = location.hostname
var weinre_port     = location.port
var weinre_pathname = location.pathname
var weinre_id       = "anonymous"

function doReplacements() {
    var hash = location.href.split("#")[1]
    if (hash) {
        weinre_id = hash
    }

    replaceURL("url-client-ui",              buildHttpURL("client/#" + weinre_id))
    replaceURL("url-interfaces",             buildHttpURL("interfaces/interfaces.html"))
    replaceURL("url-target-demo",            buildHttpURL("demo/weinre-demo.html#" + weinre_id))
    replaceURL("url-target-demo-min",        buildHttpURL("demo/weinre-demo-min.html#" + weinre_id))
    replaceURL("url-target-script",          buildHttpURL("target/target-script-min.js#" + weinre_id))
    replaceURL("url-target-bookmarklet",     getTargetBookmarklet(), "weinre target debug")
    replaceURL("url-target-documentation",   buildHttpURL("doc/"))

    replaceText("version-weinre",    Weinre.Versions.weinre)
    replaceText("version-build",     Weinre.Versions.build)

    replaceText("target-bookmarklet-src-pre",       getTargetBookmarklet())
    replaceText("target-bookmarklet-src-text-area", getTargetBookmarklet())

    replaceText("url-target-script-raw",  buildHttpURL("target/target-script-min.js#" + weinre_id))
}

doReplacements();

window.onhashchange = doReplacements;

//---------------------------------------------------------------------
function buildHttpURL(uri) {
    var port     = weinre_port
    var pathname = weinre_pathname

    if (pathname == "/index.html") pathname = "/"

    if (weinre_protocol == "file:") {
        return uri
    }

    else if (weinre_protocol == "http:") {
        if (port != "") port = ":" + port

        return weinre_protocol + "//" + weinre_host + port + pathname + uri
    }

    else if (weinre_protocol == "https:") {
        if (port != "") port = ":" + port

        return weinre_protocol + "//" + weinre_host + port + pathname + uri
    }
}

//-----------------------------------------------------------------------------
function targetBookmarkletFunction(e){
    e.setAttribute("src","???");
    document.getElementsByTagName("body")[0].appendChild(e);
}

//-----------------------------------------------------------------------------
function getTargetBookmarklet() {
    var script = targetBookmarkletFunction.toString();
    script = script.replace(/\n/g,   "")
    script = script.replace("targetBookmarkletFunction","")
    script = script.replace(/\s*/g, "")
    script = script.replace("???", buildHttpURL("target/target-script-min.js#" + weinre_id))
    script = "(" + script + ')(document.createElement("script"));void(0);'
    return 'javascript:' + script
}

//---------------------------------------------------------------------
function replaceURL(id, url, linkText) {
    if (!linkText) linkText = url
    replaceText(id, "<a href='" + url + "'>" + linkText + "</a>");
}

//---------------------------------------------------------------------
function replaceText(id, text) {
    var element = document.getElementById(id)
    if (null == element) {
//      alert("error: can't find element with id '" + id + "'")
        return
    }

    element.innerHTML = text
}
