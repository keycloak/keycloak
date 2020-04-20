/* 
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var isWelcomePage = function () {
    var winHash = window.location.hash;
    return winHash.indexOf('#/app') !== 0;
};

var toggleReact = function () {
    var welcomeScreen = document.getElementById("welcomeScreen");
    var spinnerScreen = document.getElementById("spinner_screen");
    var reactScreen = document.getElementById("main_react_container");

    if (!isWelcomePage() && !isReactLoading) {
        if (welcomeScreen) welcomeScreen.style.display = 'none';
        if (spinnerScreen) spinnerScreen.style.display = 'none';
        if (reactScreen) reactScreen.style.display = 'block';
        if (reactScreen) reactScreen.style.height = '100%';
    } else if (!isWelcomePage() && isReactLoading) {
        loadPatternFly();
        if (welcomeScreen) welcomeScreen.style.display = 'none';
        if (reactScreen) reactScreen.style.display = 'none';
        if (spinnerScreen) spinnerScreen.style.display = 'block';
        if (spinnerScreen) spinnerScreen.style.height = '100%';
    } else {
        loadPatternFly();
        if (reactScreen) reactScreen.style.display = 'none';
        if (spinnerScreen) spinnerScreen.style.display = 'none';
        if (welcomeScreen) welcomeScreen.style.display = 'block';
        if (welcomeScreen) welcomeScreen.style.height = '100%';
    }
};

var patternFlyHasLoaded = false;
var loadPatternFly = function () {
    if (patternFlyHasLoaded) return;
    const link = document.createElement("link");
    link.rel="stylesheet";
    link.href=resourceUrl + "/node_modules/@patternfly/patternfly/patternfly.min.css";
    document.head.appendChild(link);
    patternFlyHasLoaded = true;
}

var loadjs = function (url, loadListener) {
    const script = document.createElement("script");
    script.src = resourceUrl + url;
    if (loadListener)
        script.addEventListener("load", loadListener);
    document.head.appendChild(script);
};


