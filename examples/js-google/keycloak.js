window.keycloak = (function () {
    var kc = {};
    var config = {
        clientId: null,
        clientSecret: null
    };

    kc.init = function (c) {
        for (var prop in config) {
            if (c[prop]) {
                config[prop] = c[prop];
            }

            if (!config[prop]) {
                throw new Error(prop + ' not defined');
            }
        }

        loadToken();

        if (kc.token) {
            kc.user = kc.tokenInfo.user_id;
            kc.authenticated = true;
        } else {
            kc.authenticated = false;
            kc.user = null;
        }
    }

    kc.login = function () {
        var clientId = encodeURIComponent(config.clientId);
        var redirectUri = encodeURIComponent(window.location.href);
        var state = encodeURIComponent(createUUID());
        var scope = encodeURIComponent('https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/plus.login');
        var url = 'https://accounts.google.com/o/oauth2/auth?response_type=token&client_id=' + clientId + '&redirect_uri=' + redirectUri
            + '&state=' + state + '&scope=' + scope;

        sessionStorage.state = state;

        window.location.href = url;
    }

    function parseToken(token) {
        return JSON.parse(atob(token.split('.')[1]));
    }

    kc.profile = function(header) {
        var url = 'https://www.googleapis.com/oauth2/v1/userinfo'

        if (!header) {
            url = url + '?access_token=' + kc.token;
        }

         var http = new XMLHttpRequest();
         http.open('GET', url, false);
        if (header) {
        http.setRequestHeader('Authorization', 'Bearer ' + kc.token);
        }       

            http.send();
            if (http.status == 200) {
                return JSON.parse(http.responseText);
            }
    }

    kc.contacts = function(header) {
        var url = 'https://www.googleapis.com/plus/v1/people/me';

        if (!header) {
            url = url + '?access_token=' + kc.token;
        }

         var http = new XMLHttpRequest();
         http.open('GET', url, false);
        if (header) {
        http.setRequestHeader('Authorization', 'Bearer ' + kc.token);
        }       

            http.send();
            if (http.status == 200) {
                return http.responseText;
            }
    }

    return kc;

    function loadToken() {
        var params = {}
        var queryString = location.hash.substring(1)
        var regex = /([^&=]+)=([^&]*)/g, m;
        while (m = regex.exec(queryString)) {
            params[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
        }

        var token = params['access_token'];
        var state = params['state'];

        if (token && state === sessionStorage.state) {
            window.history.replaceState({}, document.title, location.protocol + "//" + location.host + location.pathname);

                kc.token = token;

            var url = 'https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=' + token;

            var http = new XMLHttpRequest();
            http.open('GET', url, false);

            http.send();
            if (http.status == 200) {
                kc.tokenInfo = JSON.parse(http.responseText);
            }
        }
        return undefined;
    }

    function getQueryParam(name) {
    console.debug(window.location.hash);
        var params = window.location.hash.substring(1).split('&');
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
