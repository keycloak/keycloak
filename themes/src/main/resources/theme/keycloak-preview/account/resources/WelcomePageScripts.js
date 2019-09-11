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
    if (!isWelcomePage()) {
        document.getElementById("welcomeScreen").style.display = 'none';
        document.getElementById("main_react_container").style.display = 'block';
    } else {
        document.getElementById("welcomeScreen").style.display = 'block';
        document.getElementById("main_react_container").style.display = 'none';
    }
};

var toggleLocaleDropdown = function () {
    var localeDropdownList = document.getElementById("landing-locale-dropdown-list");
    if (localeDropdownList.hasAttribute("hidden")) {
        localeDropdownList.removeAttribute("hidden");
        document.getElementById("landing-locale-dropdown-button").setAttribute("aria-expanded", true);
    } else {
        localeDropdownList.setAttribute("hidden", true);
        document.getElementById("landing-locale-dropdown-button").setAttribute("aria-expanded", false);
    }
};

var toggleMobileDropdown = function () {
    var mobileDropdown = document.getElementById("mobileDropdown");
    var mobileKebab = document.getElementById("mobileKebab");
    var mobileKebabButton = document.getElementById("mobileKebabButton");
    if (mobileDropdown.style.display === 'none') {
        mobileDropdown.style.display = 'block';
        mobileKebab.classList.add("pf-m-expanded");
        mobileKebabButton.setAttribute("aria-expanded", "true");
    } else {
        mobileDropdown.style.display = 'none';
        mobileKebab.classList.remove("pf-m-expanded");
        mobileKebabButton.setAttribute("aria-expanded", "false");
    }
};

var toggleMobileChooseLocale = function() {
    var mobileLocaleSelectedIcon = document.getElementById("mobileLocaleSelectedIcon");
    var isDropdownClosed = mobileLocaleSelectedIcon.classList.contains("fa-angle-right");
    var mobileLocaleSeperator = document.getElementById("mobileLocaleSeperator");
    
    if (isDropdownClosed) {
        mobileLocaleSelectedIcon.classList.remove("fa-angle-right");
        mobileLocaleSelectedIcon.classList.add("fa-angle-down");
        mobileLocaleSeperator.style.display = 'block';
    } else {
        mobileLocaleSelectedIcon.classList.add("fa-angle-right");
        mobileLocaleSelectedIcon.classList.remove("fa-angle-down");
        mobileLocaleSeperator.style.display = 'none';
    }
    
    for (var i=0; i < availableLocales.length; i++) {
        if (locale === availableLocales[i].locale) continue; // don't unhide current locale
        var mobileLocaleSelection = document.getElementById("mobile-locale-" + availableLocales[i].locale);
        if (isDropdownClosed) {
            mobileLocaleSelection.style.display= 'inline';
        } else {
            mobileLocaleSelection.style.display= 'none';
        }
    }
    
    toggleMobileDropdown();
}

var loadjs = function (url, loadListener) {
    const script = document.createElement("script");
    script.src = resourceUrl + url;
    if (loadListener)
        script.addEventListener("load", loadListener);
    document.head.appendChild(script);
};


