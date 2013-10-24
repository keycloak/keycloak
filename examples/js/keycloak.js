window.keycloak = (function () {
    var kc = {};
    var config = {
        baseUrl: null,
        clientId: null,
        clientSecret: null,
        realm: null,
        redirectUri: null
    };

    kc.init = function (c) {
        for (var prop in config) {
            if (c[prop]) {
                config[prop] = c[prop];
            }

            if (!config[prop]) {
                throw new Error(prop + 'not defined');
            }
        }

        if (!processCallback()) {
            window.location.href = getLoginUrl() + '&prompt=none';
        }
    }

    kc.login = function () {
        window.location.href = getLoginUrl();
    }

    return kc;

    function getLoginUrl(fragment) {
        var state = createUUID();
        if (fragment) {
            state += '#' + fragment;
        }
        sessionStorage.state = state;
        var url = config.baseUrl + '/rest/realms/' + encodeURIComponent(config.realm) + '/tokens/login?response_type=code&client_id='
            + encodeURIComponent(config.clientId) + '&redirect_uri=' + encodeURIComponent(config.redirectUri) + '&state=' + encodeURIComponent(state);
        return url;
    }

    function parseToken(token) {
        return JSON.parse(atob(token.split('.')[1]));
    }

    function processCallback() {
        var code = getQueryParam('code');
        var error = getQueryParam('error');
        var state = getQueryParam('state');

        if (!(code || error)) {
            return false;
        }

        if (state != sessionStorage.state) {
            console.error('Invalid state');
            return true;
        }

        if (code) {
            console.info('Received code');

            var clientId = encodeURIComponent(config.clientId);
            var clientSecret = encodeURIComponent(config.clientSecret);
            var realm = encodeURIComponent(config.realm);

            var params = 'code=' + code + '&client_id=' + clientId + '&password=' + clientSecret;
            var url = config.baseUrl + '/rest/realms/' + realm + '/tokens/access/codes'

            var http = new XMLHttpRequest();
            http.open('POST', url, false);
            http.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

            http.send(params);
            if (http.status == 200) {
                kc.token = JSON.parse(http.responseText)['access_token'];
                kc.tokenParsed = parseToken(kc.token);
                kc.authenticated = true;
                kc.user = kc.tokenParsed.prn;

                console.info('Authenticated');
            }

            updateLocation(state);
            return true;
        } else if (error) {
            console.info('Error ' + error);
            updateLocation(state);
            return true;
        }
    }

    function updateLocation(state) {
        var fragment = '';
        if (state && state.indexOf('#') != -1) {
            fragment = state.substr(state.indexOf('#'));
        }

        window.history.replaceState({}, document.title, location.protocol + "//" + location.host + location.pathname + fragment);
    }

    function getQueryParam(name) {
        var params = window.location.search.substring(1).split('&');
        for (var i = 0; i < params.length; i++) {
            var p = params[i].split('=');
            if (decodeURIComponent(p[0]) == name) {
                return p[1];
            }
        }
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
})();