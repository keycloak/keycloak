
function kcAuthChecker() {
    var authChecker = this;

    // How often to check if KEYCLOAK_SESSION cookie was added in the other browser tab?
    var checkIntervalMillisecs = 2000;

    // 1 - unknown state or KEYCLOAK_SESSION present since beginning, 2 - cookie KEYCLOAK_SESSION not present, 3 - cookie KEYCLOAK_SESSION present after it
    // was not present before (possible to move here from state 2, but not from 1. Reason are "re-authentication" scenarios where we want login screens displayed even if SSO cookie exists)
    var sessionCookieState = 1;

     function setupTimer(authSessionId, tabId, loginRestartUrl) {
        setTimeout(function() {
            authChecker.checkCookiesAndSetTimer(authSessionId, tabId, loginRestartUrl);
        }, checkIntervalMillisecs);
    }

    authChecker.checkCookiesAndSetTimer = function(authSessionId, tabId, loginRestartUrl) {
        var sessionCookie = getCookieByName("KEYCLOAK_SESSION");
        if (sessionCookie) {
            if (sessionCookieState == 2) {
                sessionCookieState = 3;
                console.log("kcAuthChecker: Cookie KEYCLOAK_SESSION added");
            }
        } else {
            if (sessionCookieState == 1) {
                sessionCookieState = 2;
                console.log("kcAuthChecker: Cookie KEYCLOAK_SESSION not present");
                document.kcAuthCheckerReady = true; // For tests
            }
        }

        if (sessionCookieState == 3) {
            checkAuthSession(authSessionId, tabId, loginRestartUrl);
        } else {
            setupTimer(authSessionId, tabId, loginRestartUrl);
        }
    }

    function checkAuthSession(authSessionId, tabId, loginRestartUrl) {
        var authCookieStr = getCookieByName("KC_AUTH_STATE");
        if (authCookieStr) {
            var authCookie = JSON.parse(authCookieStr);
            if (authSessionId === authCookie.authSessionId && authCookie.remainingTabs.indexOf(tabId) > -1) {
                loginRestartUrl = htmlDecode(loginRestartUrl);
                window.location.href = loginRestartUrl;
            } else {
                console.log("kcAuthChecker: Cookie KC_AUTH_STATE present, but value does not match with authentication session parameters in current browser.");
            }
        } else {
            console.log("kcAuthChecker: Cookie KC_AUTH_STATE not present");
        }
    }

    function htmlDecode(input) {
        var doc = new DOMParser().parseFromString(input, "text/html");
        return doc.documentElement.textContent;
    }

    function getCookieByName(name) {
        const cookies = {};
        var cookiesVal = document.cookie.split(";");

        cookiesVal.forEach(cookieFunc);

        function cookieFunc(cookieVal) {
            var cookieParsed = cookieVal.split("=");
            if (cookieParsed.length == 2) {
                cookies[cookieParsed[0].trim()] = cookieParsed[1].trim();
            }
        }

        return cookies[name];
    }

}
