var Keycloak = function (options) {
    options = options || {};

    if (!(this instanceof Keycloak)) {
        return new Keycloak(options);
    }

    var instance = this;

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

    if (!options.clientSecret) {
        throw 'clientSecret missing';
    }

    this.init = function (successCallback, errorCallback) {
        if (window.oauth.callback) {
            delete sessionStorage.oauthToken;
            processCallback(successCallback, errorCallback);
        } else if (options.token) {
            setToken(options.token, successCallback);
        } else if (sessionStorage.oauthToken) {
            setToken(sessionStorage.oauthToken, successCallback);
        } else if (options.onload) {
            switch (options.onload) {
                case 'login-required' :
                    window.location = createLoginUrl(true);
                    break;
                case 'check-sso' :
                    window.location = createLoginUrl(false);
                    break;
            }
        }
    }

    this.login = function () {
        window.location.href = createLoginUrl(true);
    }

    this.logout = function () {
        setToken(undefined);
        window.location.href = createLogoutUrl();
    }

    this.hasRealmRole = function (role) {
        var access = this.realmAccess;
        return access && access.roles.indexOf(role) >= 0 || false;
    }

    this.hasResourceRole = function (role, resource) {
        if (!this.resourceAccess) {
            return false;
        }

        var access = this.resourceAccess[resource || options.clientId];
        return access && access.roles.indexOf(role) >= 0 || false;
    }

    this.loadUserProfile = function (success, error) {
        var url = getRealmUrl() + '/account';
        var req = new XMLHttpRequest();
        req.open('GET', url, true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Authorization', 'bearer ' + this.token);

        req.onreadystatechange = function () {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    instance.profile = JSON.parse(req.responseText);
                    success && success(instance.profile)
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

    function getRealmUrl() {
        return options.url + '/auth/rest/realms/' + encodeURIComponent(options.realm);
    }

    function processCallback(successCallback, errorCallback) {
        var code = window.oauth.code;
        var error = window.oauth.error;
        var prompt = window.oauth.prompt;

        if (code) {
            var params = 'code=' + code + '&client_id=' + encodeURIComponent(options.clientId) + '&password=' + encodeURIComponent(options.clientSecret);
            var url = getRealmUrl() + '/tokens/access/codes';

            var req = new XMLHttpRequest();
            req.open('POST', url, true);
            req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        setToken(JSON.parse(req.responseText)['access_token'], successCallback);
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

    function setToken(token, successCallback) {
        if (token) {
            sessionStorage.oauthToken = token;
            window.oauth.token = token;
            instance.token = token;

            instance.tokenParsed = JSON.parse(atob(token.split('.')[1]));
            instance.authenticated = true;
            instance.username = instance.tokenParsed.sub;
            instance.realmAccess = instance.tokenParsed.realm_access;
            instance.resourceAccess = instance.tokenParsed.resource_access;

            setTimeout(function() {
                successCallback && successCallback({ authenticated: instance.authenticated, username: instance.username });
            }, 0);
        } else {
            delete sessionStorage.oauthToken;
            delete window.oauth.token;
            delete instance.token;
        }
    }

    function createLoginUrl(prompt) {
        var state = createUUID();

        sessionStorage.oauthState = state;
        var url = getRealmUrl()
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

    function createLogoutUrl() {
        var url = getRealmUrl()
            + '/tokens/logout'
            + '?redirect_uri=' + getEncodedRedirectUri();
        return url;
    }

    function getEncodedRedirectUri() {
        var url = (location.protocol + '//' + location.hostname + (location.port && (':' + location.port)) + location.pathname);
        if (location.hash) {
            url += '?redirect_fragment=' + encodeURIComponent(location.hash.substring(1));
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