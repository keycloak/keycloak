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

;(function(){

window.addEventListener("load", onLoad, false)

var Interfaces = {}
var Indent     = "<span class='indent'>   </span>"
var Indent2    = "<span class='indent'>      </span>"

var e_interfaceList
var e_interfaceName
var e_interfaceBody
var e_showIdl
var e_showJavaScript
var e_showJava

var NativeTypes = "int any number boolean string void".split(" ")

var IDLTools = require("weinre/common/IDLTools")

if (!window.localStorage) {
    window.localStorage = {
        getItem: function() {},
        setItem: function() {}
    }
}

//-----------------------------------------------------------------------------
function onLoad() {
    e_interfaceList  = document.getElementById("interface-list")
    e_interfaceName  = document.getElementById("interface-name")
    e_interfaceBody  = document.getElementById("interface-body")
    e_showIdl        = document.getElementById("show-Idl")
    e_showJavaScript = document.getElementById("show-JavaScript")
    e_showJava       = document.getElementById("show-Java")

    setUpShowCheckBoxes()
    populateInterfacesList(IDLTools.getIDLsMatching(/.*/))
}

//-----------------------------------------------------------------------------
function setUpShowCheckBoxes() {
    setUpShowCheckBox(e_showIdl,        "show-Idl")
    setUpShowCheckBox(e_showJavaScript, "show-JavaScript")
    setUpShowCheckBox(e_showJava,       "show-Java")
}

//-----------------------------------------------------------------------------
function setUpShowCheckBox(element, key) {
    var value = localStorage.getItem(key)
    if (null == value)
        value = true
    else
        value = (value == "true")

    element.checked    = value
    element.storageKey = key

    element.addEventListener("click", el_showCheckBoxClicked, false)
}

//-----------------------------------------------------------------------------
function el_showCheckBoxClicked(event) {
    var element = event.target

    localStorage.setItem(element.storageKey, element.checked)

    reapplyDisplayStyle("." + element.storageKey, element.checked)
}

//-----------------------------------------------------------------------------
function reapplyDisplayStyles() {
    reapplyDisplayStyle(".show-Idl",        e_showIdl.checked)
    reapplyDisplayStyle(".show-JavaScript", e_showJavaScript.checked)
    reapplyDisplayStyle(".show-Java",       e_showJava.checked)
}

//-----------------------------------------------------------------------------
function reapplyDisplayStyle(className, value) {
    value = value ? "block" : "none"
    ;[].slice.call(document.querySelectorAll(className)).forEach(function(element) {
        element.style.display = value
    })
}

//-----------------------------------------------------------------------------
function populateInterfacesList(intfs) {
    e_interfaceList.innerHTML = ""

    fixedIntfs = []
    intfs.forEach(function(intf){
        fixedIntfs.push(intf.name)
        Interfaces[intf.name] = intf

        if (!intf.methods)    intf.methods    = []
        if (!intf.attributes) intf.attributes = []
    })

    intfs = fixedIntfs

    intfs.sort()
    intfs.forEach(function(intf){
        var a  = document.createElement("a")
        a.href          = "#"
        a.innerHTML     = intf
        a.interfaceName = intf
        a.addEventListener("click", el_interfaceClicked, false)

        var li = document.createElement("li")
        li.appendChild(a)
        e_interfaceList.appendChild(li)
    })
}

//-----------------------------------------------------------------------------
function el_interfaceClicked(event) {
    event.preventDefault()

    showInterface(event.target.interfaceName)
}

//-----------------------------------------------------------------------------
function showInterface(interfaceName) {
    var intf = Interfaces[interfaceName]

    e_interfaceName.innerHTML = interfaceName

    var html = []

    showInterfaceIdl(intf, html)
    showInterfaceJavaScript(intf, html)
    showInterfaceJava(intf, html)

    e_interfaceBody.innerHTML = html.join("\n")

    reapplyDisplayStyles()
}

window.showInterface = showInterface

//-----------------------------------------------------------------------------
function showInterfaceIdl(intf, html) {
    html.push("<div class='show-Idl'><h3>IDL</h3><pre>")
    html.push("interface {")

    intf.methods.forEach(function(method){
        showInterfaceIdlMethod(method, html)
    })

    if (intf.attributes.length > 0) html.push("<table>")
    intf.attributes.forEach(function(attribute){
        showInterfaceIdlAttribute(attribute, html)
    })
    if (intf.attributes.length > 0) html.push("</table>")

    html.push("};")
    html.push("</pre></div>")
}

//-----------------------------------------------------------------------------
function showInterfaceIdlMethod(method, html) {
    var line = "\n   "

    var allParameters = method.parameters.concat(method.callbackParameters)

    line += getIdlType(method.returns)
    line += " <span class='methodName'>" + method.name + "</span> (" + getIdlParameterList(allParameters) + ");"
    html.push(line)
}

//-----------------------------------------------------------------------------
function getIdlParameterList(parameters) {
    var result = []

    if (parameters.length == 0) return "";

    result.push("<table>")
    parameters.forEach(function(parameter, index, list){
        var comma = (index == list.length-1) ? "" : ","
        result.push("<tr>")
        result.push("<td>" + Indent2 + (parameter.out ? "out" : "in"))
        result.push("<td>" + getIdlType(parameter.type))
        result.push("<td>" + "<span class='parameterName tdIndent'>" + parameter.name + comma + "</span>")
    })

    result.push("</table>")
    return result.join("\n") + Indent
}

//-----------------------------------------------------------------------------
function getIdlType(type) {
    var result


    if (-1 == NativeTypes.indexOf(type.name)) {
        result = "<a href='javascript:showInterface(\"" + type.name + "\"); void(0);'>" + type.name + "</a>"
    }
    else {
        result = type.name
    }

    for (var i=0; i<type.rank; i++) {
        result += "[]"
    }

    return "<span class='type'>" + result + "</span>"
}

//-----------------------------------------------------------------------------
IDL2Java = {
    string: "String",
    any:    "Object",
    int:    "Long",
    boolean: "Boolean",
    "":     "?"
}

//-----------------------------------------------------------------------------
function getJavaType(type) {
    var result


    if (-1 == NativeTypes.indexOf(type.name)) {
        result = "<a href='javascript:showInterface(\"" + type.name + "\"); void(0);'>" + type.name + "</a>"
    }
    else {
        result = IDL2Java[type.name]
        if (!result) {
            result = "?" + type.name + "?"
            console.log("Unable to translate IDL type to Java: " + type.name)
        }
    }

    for (var i=0; i<type.rank; i++) {
        result += "[]"
    }

    return "<span class='type'>" + result + "</span>"
}


//-----------------------------------------------------------------------------
function showInterfaceIdlAttribute(attribute, html) {
    var line = "<tr><td>" + Indent + "attribute "

    line += getIdlType(attribute.type)
    line += "<td><span class='attributeName tdIndent'>" + attribute.name + "</span>;"
    html.push(line)

}

//-----------------------------------------------------------------------------
function showInterfaceJavaScript(intf, html) {
    html.push("<div class='show-JavaScript'><h3>JavaScript</h3><pre>")

    var line = ""

    line += "\n//-----------------------------------------------------------------------------"
    line += "\n<span class='interfaceName'>class " + intf.name + "</span>"
    html.push(line)

    intf.methods.forEach(function(method){
        showInterfaceJavaScriptMethod(intf, method, html)
    })

    html.push("</pre></div>")
}

//-----------------------------------------------------------------------------
function showInterfaceJavaScriptMethod(intf, method, html) {
    var line = ""

    line += "\n//-----------------------------------------------------------------------------"
    line += "\n<span class='methodName'>method " + method.name + "</span>(" + getJavaScriptParameterListSimple(method.parameters, method.returns) + ")"
    line += "\n    // callback: function(" + getJavaScriptCallbackParameterListSimple(method.callbackParameters) + ")"
    line += "\n    Weinre.notImplemented(arguments.callee.signature)"
    line += "\n"
    html.push(line)
}

//-----------------------------------------------------------------------------
function getJavaScriptParameterList(parameters, returnType) {
    var result = []

    result.push("<table>")
    parameters.forEach(function(parameter){
        result.push("<tr>")
        result.push("<td>" + Indent2 + "<span class='parameterName'>" + parameter.name + ",</span>")
        result.push("<td><span class='tdIndent'>// " + getIdlType(parameter.type) + "</span>")
    })

    result.push("<tr>")
    result.push("<td>" + Indent2 + "<span class='parameterName'>callback</span>")
    result.push("<td><span class='tdIndent'>// function(error, " + getIdlType(returnType) + ")</span>")

    result.push("</table>")
    return result.join("\n") + Indent
}

//-----------------------------------------------------------------------------
function getJavaScriptParameterListSimple(parameters, returnType) {
    var result = []

    parameters.forEach(function(parameter){
        if (parameter.out) return
        result.push("<span class='type'>/*" + getIdlType(parameter.type) + "*/ </span><span class='parameterName'>" + parameter.name + "</span>")
    })

    result.push("<span class='parameterName'>callback</span>")
    return result.join(", ")
}

//-----------------------------------------------------------------------------
function getJavaScriptCallbackParameterListSimple(parameters) {
    var result = []

    parameters.forEach(function(parameter){
        if (!parameter.out) return
        result.push("/*" + getIdlType(parameter.type) + "*/ "+ parameter.name)
    })

    return result.join(", ")
}

//-----------------------------------------------------------------------------
function showInterfaceJava(intf, html) {
    html.push("<div class='show-Java'><h3>Java</h3><pre>")

    intf.methods.forEach(function(method){
        showInterfaceJavaMethod(intf, method, html)
    })

    html.push("</pre></div>")
}

//-----------------------------------------------------------------------------
function showInterfaceJavaMethod(intf, method, html) {
    var line = ""

    line += "\n    /**"
    line += "\n     * "
    line += "\n     */"
    line += "\n    <span class='methodName'>public void " + method.name + "</span>(" + getJavaParameterListSimple(method.parameters, method.returns) + ") throws IOException {"
    line += "\n        Main.warn(getClass().getName() + \"." + method.name + "() not implemented\");"
    line += "\n"
    line += "\n        channel.sendCallback(\"" + intf.name + "\", callbackId" + getJavaCallbackParameterListSimple(method.callbackParameters) + ");"
    line += "\n    }"
    line += "\n"
    html.push(line)
}

//-----------------------------------------------------------------------------
function getJavaParameterList(parameters, returnType) {
    var result = []

    result.push("<table>")
    parameters.forEach(function(parameter){
        result.push("<tr>")
        result.push("<td>" + Indent2 + "<span class='parameterName'>" + parameter.name + ",</span>")
        result.push("<td><span class='tdIndent'>// " + getIdlType(parameter.type) + "</span>")
    })

    result.push("<tr>")
    result.push("<td>" + Indent2 + "<span class='parameterName'>callback</span>")
    result.push("<td><span class='tdIndent'>// function(error, " + getIdlType(returnType) + ")</span>")

    result.push("</table>")
    return result.join("\n") + Indent
}

//-----------------------------------------------------------------------------
function getJavaParameterListSimple(parameters, returnType) {
    var result = []

    result.push("<span class='type'>Channel</span> <span class='parameterName'>channel</span>")

    parameters.forEach(function(parameter){
        if (parameter.out) return
        result.push("<span class='type'>" + getJavaType(parameter.type) + " </span><span class='parameterName'>" + parameter.name + "</span>")
    })

    result.push("<span class='type'>String</span> <span class='parameterName'>callbackId</span>")
    return result.join(", ")
}

//-----------------------------------------------------------------------------
function getJavaCallbackParameterListSimple(parameters) {
    var result = []

    parameters.forEach(function(parameter){
        if (!parameter.out) return
        result.push("/*" + getJavaType(parameter.type)  + " " + parameter.name + "*/ (Object) null")
    })

    result = result.join(", ")

    if (result != "") result = ", " + result

    return result
}

//-----------------------------------------------------------------------------
function toArray(arrayLike) {
    return [].slice.call(arrayLike)
}

//-----------------------------------------------------------------------------
ExBreak = new Error("breaks out of loops")

//-----------------------------------------------------------------------------
function exBreak() {
    throw ExBreak
}

//-----------------------------------------------------------------------------
function allowExBreak(func) {
    try {
        func.call()
    }
    catch(e) {
        if (e == ExBreak) return
        throw e
    }
}

//-----------------------------------------------------------------------------
})();

