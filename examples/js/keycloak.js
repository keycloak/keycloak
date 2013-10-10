window.keycloak = (function () {
    var kc = {};
    var config = {
        baseUrl: null,
        clientId: null,
        clientSecret: null,
        realm: null
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

        var token = getTokenFromCode();
        if (token) {
            var t = parseToken(token);
            kc.user = t.prn;
            kc.authenticated = true;
            kc.token = token;
        } else {
            kc.authenticated = false;
        }
    }

    kc.login = function () {
        var clientId = encodeURIComponent(config.clientId);
        var redirectUri = encodeURIComponent(window.location.href);
        var state = encodeURIComponent(createUUID());
        var realm = encodeURIComponent(config.realm);
        var url = config.baseUrl + '/rest/realms/' + realm + '/tokens/login?response_type=code&client_id=' + clientId + '&redirect_uri=' + redirectUri
            + '&state=' + state;

        sessionStorage.state = state;

        window.location.href = url;
    }

    return kc;

    function parseToken(token) {
        return JSON.parse(atob(token.split('.')[1]));
    }

    function getTokenFromCode() {
        var code = getQueryParam('code');
        var state = getQueryParam('state');
        if (code && state === sessionStorage.state) {
            window.history.replaceState({}, document.title, location.protocol + "//" + location.host + location.pathname);

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
                return JSON.parse(http.responseText)['access_token'];
            }
        }
        return undefined;
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