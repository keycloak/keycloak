var Keycloak = function (options) {
    options = options || {};

    if (!(this instanceof Keycloak)) {
        return new Keycloak(options);
    }

    var kc = this;

    if (!options.url) {
        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            if (scripts[i].src.match(/.*keycloak\.js/)) {
                options.url = scripts[i].src.substr(0, scripts[i].src.indexOf('/auth/js/keycloak.js'));
                break;
            }
        }
    }

    if (!options.url) {
        throw 'url missing';
    }

    if (!options.realm) {
        throw 'realm missing';
    }

    if (!options.clientId) {
        throw 'clientId missing';
    }

    kc.init = function (successCallback, errorCallback) {
        if (window.oauth.callback) {
            processCallback(successCallback, errorCallback);
        } else if (options.token) {
            kc.setToken(options.token, successCallback);
        } else if (options.onload) {
            switch (options.onload) {
                case 'login-required' :
                    window.location = kc.createLoginUrl(true);
                    break;
                case 'check-sso' :
                    window.location = kc.createLoginUrl(false);
                    break;
            }
        }
    }

    kc.login = function () {
        window.location.href = kc.createLoginUrl(true);
    }

    kc.logout = function () {
        kc.setToken(undefined);
        window.location.href = kc.createLogoutUrl();
    }

    kc.hasRealmRole = function (role) {
        var access = kc.realmAccess;
        return access && access.roles.indexOf(role) >= 0 || false;
    }

    kc.hasResourceRole = function (role, resource) {
        if (!kc.resourceAccess) {
            return false;
        }

        var access = kc.resourceAccess[resource || options.clientId];
        return access && access.roles.indexOf(role) >= 0 || false;
    }

    kc.loadUserProfile = function (success, error) {
        var url = kc.getRealmUrl() + '/account';
        var req = new XMLHttpRequest();
        req.open('GET', url, true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Authorization', 'bearer ' + kc.token);

        req.onreadystatechange = function () {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    kc.profile = JSON.parse(req.responseText);
                    success && success(kc.profile)
                } else {
                    var response = { status: req.status, statusText: req.status };
                    if (req.responseText) {
                        response.data = JSON.parse(req.responseText);
                    }
                    error && error(response);
                }
            }
        }

        req.send();
    }

    /**
     * checks to make sure token is valid.  If it is, it calls successCallback with no parameters.
     * If it isn't valid, it tries to refresh the access token.  On successful refresh, it calls successCallback.
     *
     * @param successCallback
     * @param errorCallback
     */
    kc.onValidAccessToken = function(successCallback, errorCallback) {
        if (!kc.tokenParsed) {
            console.log('no token');
            errorCallback();
            return;
        }
        var currTime = new Date().getTime() / 1000;
        if (currTime > kc.tokenParsed['exp']) {
            if (!kc.refreshToken) {
                console.log('no refresh token');
                errorCallback();
                return;
            }
            console.log('calling refresh');
            var params = 'grant_type=refresh_token&' + 'refresh_token=' + kc.refreshToken;
            var url = kc.getRealmUrl() + '/tokens/refresh';

            var req = new XMLHttpRequest();
            req.open('POST', url, true, options.clientId, options.clientSecret);
            req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        console.log('Refresh Success');
                        var tokenResponse = JSON.parse(req.responseText);
                        kc.refreshToken = tokenResponse['refresh_token'];
                        kc.setToken(tokenResponse['access_token'], successCallback);
                    } else {
                        console.log('error on refresh HTTP invoke: ' + req.status);
                        errorCallback && errorCallback({ authenticated: false, status: req.status, statusText: req.statusText });
                    }
                }
            };
            req.send(params);
        } else {
            console.log('Token is still valid');
            successCallback();
        }

    }

    kc.getRealmUrl = function() {
        return options.url + '/auth/rest/realms/' + encodeURIComponent(options.realm);
    }

    function processCallback(successCallback, errorCallback) {
        var code = window.oauth.code;
        var error = window.oauth.error;
        var prompt = window.oauth.prompt;

        if (code) {
            var params = 'code=' + code;
            var url = kc.getRealmUrl() + '/tokens/access/codes';

            var req = new XMLHttpRequest();
            req.open('POST', url, true);
            req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

            if (options.clientId && options.clientSecret) {
                req.setRequestHeader('Authorization', 'Basic ' + btoa(options.clientId + ':' + options.clientSecret));
            } else {
                params += '&client_id=' + encodeURIComponent(options.clientId);
            }

            req.withCredentials = true;

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        var tokenResponse = JSON.parse(req.responseText);
                        kc.refreshToken = tokenResponse['refresh_token'];
                        kc.setToken(tokenResponse['access_token'], successCallback);
                    } else {
                        errorCallback && errorCallback({ authenticated: false, status: req.status, statusText: req.statusText });
                    }
                }
            };

            req.send(params);
        } else if (error) {
            if (prompt != 'none') {
                setTimeout(function() {
                    errorCallback && errorCallback({  authenticated: false, error: error })
                }, 0);
            }
        }
    }

    kc.setToken = function(token, successCallback) {
        if (token) {
            window.oauth.token = token;
            kc.token = token;
            kc.tokenParsed = JSON.parse(atob(token.split('.')[1]));
            kc.authenticated = true;
            kc.subject = kc.tokenParsed.sub;
            kc.realmAccess = kc.tokenParsed.realm_access;
            kc.resourceAccess = kc.tokenParsed.resource_access;

            for (var i = 0; i < idTokenProperties.length; i++) {
                var n = idTokenProperties[i];
                if (kc.tokenParsed[n]) {
                    if (!kc.idToken) {
                        kc.idToken = {};
                    }
                    kc.idToken[n] = kc.tokenParsed[n];
                }
            }

            setTimeout(function() {
                successCallback && successCallback({ authenticated: kc.authenticated, subject: kc.subject });
            }, 0);
        } else {
            delete window.oauth.token;
            delete kc.token;
        }
    }

    kc.createLoginUrl = function(prompt) {
        var state = createUUID();

        sessionStorage.oauthState = state;
        var url = kc.getRealmUrl()
            + '/tokens/login'
            + '?client_id=' + encodeURIComponent(options.clientId)
            + '&redirect_uri=' + getEncodedRedirectUri()
            + '&state=' + encodeURIComponent(state)
            + '&response_type=code';

        if (prompt == false) {
            url += '&prompt=none';
        }

        return url;
    }

    kc.createLogoutUrl = function() {
        var url = kc.getRealmUrl()
            + '/tokens/logout'
            + '?redirect_uri=' + getEncodedRedirectUri();
        return url;
    }

    function getEncodedRedirectUri() {
        var url;
        if (options.redirectUri) {
            url = options.redirectUri;
        } else {
            url = (location.protocol + '//' + location.hostname + (location.port && (':' + location.port)) + location.pathname);
            if (location.hash) {
                url += '?redirect_fragment=' + encodeURIComponent(location.hash.substring(1));
            }
        }
        return encodeURI(url);
    }

    function createUUID() {
        var s = [];
        var hexDigits = '0123456789abcdef';
        for (var i = 0; i < 36; i++) {
            s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
        }
        s[14] = '4';
        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
        s[8] = s[13] = s[18] = s[23] = '-';
        var uuid = s.join('');
        return uuid;
    }
    
    var idTokenProperties = [
        "name", 
        "given_name", 
        "family_name", 
        "middle_name", 
        "nickname", 
        "preferred_username", 
        "profile", 
        "picture", 
        "website", 
        "email", 
        "email_verified", 
        "gender", 
        "birthdate", 
        "zoneinfo", 
        "locale", 
        "phone_number", 
        "phone_number_verified", 
        "address", 
        "updated_at", 
        "formatted", 
        "street_address", 
        "locality", 
        "region", 
        "postal_code", 
        "country", 
        "claims_locales"
    ]
}

window.oauth = (function () {
    var oauth = {};

    var params = window.location.search.substring(1).split('&');
    for (var i = 0; i < params.length; i++) {
        var p = params[i].split('=');
        switch (decodeURIComponent(p[0])) {
            case 'code':
                oauth.code = p[1];
                break;
            case 'error':
                oauth.error = p[1];
                break;
            case 'state':
                oauth.state = decodeURIComponent(p[1]);
                break;
            case 'redirect_fragment':
                oauth.fragment = decodeURIComponent(p[1]);
                break;
            case 'prompt':
                oauth.prompt = p[1];
                break;
        }
    }

    if (oauth.state && oauth.state == sessionStorage.oauthState) {
        oauth.callback = true;
        delete sessionStorage.oauthState;
    } else {
        oauth.callback = false;
    }

    if (oauth.callback) {
        window.history.replaceState({}, null, location.protocol + '//' + location.host + location.pathname + (oauth.fragment ? '#' + oauth.fragment : ''));
    } else if (oauth.fragment) {
        window.history.replaceState({}, null, location.protocol + '//' + location.host + location.pathname + (oauth.fragment ? '#' + oauth.fragment : ''));
    }

    return oauth;
}());