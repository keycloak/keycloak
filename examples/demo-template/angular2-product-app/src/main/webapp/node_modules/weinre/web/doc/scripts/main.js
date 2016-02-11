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

//-----------------------------------------------------------------------------
function main() {
//    window.addEventListener("load", addClickToExpandImageHandlers, false);
}

//-----------------------------------------------------------------------------
function addClickToExpandImageHandlers() {
    var elements = document.getElementsByTagName("img")
    for (var i=0; i<elements.length; i++) {
        var element = elements[i]
        if (!hasClass(element, "expand-on-click")) continue

        addClass(element, "width-transition")
        element._original_width_ = element.width
        element._contracted_     = true
        element.width            = element.width / 2

        element.addEventListener("click", clickToExpand, false)
    }
}

//-----------------------------------------------------------------------------
function clickToExpand(event) {
    var element = this

    if (element._contracted_) {
        element.width = element._original_width_
    }
    else {
        element.width = element._original_width_ / 2
    }

    element._contracted_ = ! element._contracted_

//     if (hasClass(element, "contracted")) {
//         removeClass(element, "contracted")
//     }
//
//     else {
//         addClass(element,    "contracted")
//     }
}

//-----------------------------------------------------------------------------
function hasClass(element, className) {
    var classNames = element.className.split(/\s+/)
    for (var i=0; i<classNames.length; i++) {
        if (className == classNames[i]) return true
    }
    return false
}

//-----------------------------------------------------------------------------
function addClass(element, className) {
    if (hasClass(element, className)) return

    element.className += " " + className
}

//-----------------------------------------------------------------------------
function removeClass(element, className) {
    var classNames = element.className.split(/\s+/)
    for (var i=0; i<classNames.length; i++) {
        if (className == classNames[i]) {
            classNames[i] = ""
        }
    }
    element.className = classNames.join(" ")
}

//-----------------------------------------------------------------------------
main()
