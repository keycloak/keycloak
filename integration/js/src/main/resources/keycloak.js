var Keycloak = function (config) {
    if (!(this instanceof Keycloak)) {
        return new Keycloak(config);
    }

    var kc = this;
    var adapter;

    kc.init = function (init) {
        kc.authenticated = false;

        if (window.Cordova) {
            adapter = loadAdapter('cordova');
        } else {
            adapter = loadAdapter();
        }

        var promise = createPromise();

        var initPromise = createPromise();
        initPromise.promise.success(function() {
            kc.onReady && kc.onReady(kc.authenticated);
            promise.setSuccess(kc.authenticated);
        }).error(function() {
            promise.setError();
        });

        var configPromise = loadConfig(config);

        function processInit() {
            var callback = parseCallback(window.location.href);
            if (callback) {
                window.history.replaceState({}, null, location.protocol + '//' + location.host + location.pathname + (callback.fragment ? '#' + callback.fragment : ''));
                processCallback(callback, initPromise);
                return;
            } else if (init) {
                if (init.code || init.error) {
                    processCallback(init, initPromise);
                    return;
                } else if (init.token || init.refreshToken) {
                    setToken(init.token, init.refreshToken);
                    initPromise.setSuccess();
                } else if (init == 'login-required') {
                    var p = kc.login();
                    if (p) {
                        p.success(function() {
                            initPromise.setSuccess();
                        }).error(function() {
                            initPromise.setError();
                        });
                    };
                } else if (init == 'check-sso') {
                    var p = kc.login({ prompt: 'none' });
                    if (p) {
                        p.success(function() {
                            initPromise.setSuccess();
                        }).error(function() {
                            initPromise.setSuccess();
                        });
                    };
                } else {
                    throw 'invalid init: ' + init;
                }
            } else {
                initPromise.setSuccess();
            }
        }

        configPromise.success(processInit);
        configPromise.error(function() {
            promise.setError();
        });

        return promise.promise;
    }

    kc.login = function (options) {
        return adapter.login(options);
    }

    kc.createLoginUrl = function(options) {
        var state = createUUID();

        var redirectUri = adapter.redirectUri(options);
        if (options && options.prompt) {
            if (redirectUri.indexOf('?') == -1) {
                redirectUri += '?prompt=' + options.prompt;
            } else {
                redirectUri += '&prompt=' + options.prompt;
            }
        }

        sessionStorage.oauthState = state;

        var url = getRealmUrl()
            + '/tokens/login'
            + '?client_id=' + encodeURIComponent(kc.clientId)
            + '&redirect_uri=' + encodeURIComponent(redirectUri)
            + '&state=' + encodeURIComponent(state)
            + '&response_type=code';

        if (options && options.prompt) {
            url += '&prompt=' + options.prompt;
        }

        return url;
    }

    kc.logout = function(options) {
        return adapter.logout(options);
    }

    kc.createLogoutUrl = function(options) {
        var url = getRealmUrl()
            + '/tokens/logout'
            + '?redirect_uri=' + encodeURIComponent(adapter.redirectUri(options));

        return url;
    }

    kc.createAccountUrl = function(options) {
        var url = getRealmUrl()
            + '/account'
            + '?referrer=' + kc.clientId
            + '&referrer_uri=' + encodeURIComponent(adapter.redirectUri(options));

        return url;
    }

    kc.accountManagement = function() {
        return adapter.accountManagement();
    }

    kc.hasRealmRole = function (role) {
        var access = kc.realmAccess;
        return access && access.roles.indexOf(role) >= 0 || false;
    }

    kc.hasResourceRole = function(role, resource) {
        if (!kc.resourceAccess) {
            return false;
        }

        var access = kc.resourceAccess[resource || kc.clientId];
        return access && access.roles.indexOf(role) >= 0 || false;
    }

    kc.loadUserProfile = function() {
        var url = getRealmUrl() + '/account';
        var req = new XMLHttpRequest();
        req.open('GET', url, true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Authorization', 'bearer ' + kc.token);

        var promise = createPromise();

        req.onreadystatechange = function () {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    kc.profile = JSON.parse(req.responseText);
                    promise.setSuccess(kc.profile);
                } else {
                    promise.setError();
                }
            }
        }

        req.send();

        return promise.promise;
    }

    kc.isTokenExpired = function(minValidity) {
        if (!kc.tokenParsed || !kc.refreshToken) {
            throw 'Not authenticated';
        }

        var expiresIn = kc.tokenParsed['exp'] - (new Date().getTime() / 1000);
        if (minValidity) {
            expiresIn -= minValidity;
        }

        return expiresIn < 0;
    }

    kc.updateToken = function(minValidity) {
        if (!kc.tokenParsed || !kc.refreshToken) {
            throw 'Not authenticated';
        }

        var promise = createPromise();

        if (minValidity) {
            if (!kc.isTokenExpired(minValidity)) {
                promise.setSuccess(false);
                return promise.promise;
            }
        }

        var params = 'grant_type=refresh_token&' + 'refresh_token=' + kc.refreshToken;
        var url = getRealmUrl() + '/tokens/refresh';

        var req = new XMLHttpRequest();
        req.open('POST', url, true);
        req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

        if (kc.clientId && kc.clientSecret) {
            req.setRequestHeader('Authorization', 'Basic ' + btoa(kc.clientId + ':' + kc.clientSecret));
        } else {
            params += '&client_id=' + encodeURIComponent(kc.clientId);
        }

        req.onreadystatechange = function() {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    var tokenResponse = JSON.parse(req.responseText);
                    setToken(tokenResponse['access_token'], tokenResponse['refresh_token']);
                    kc.onAuthRefreshSuccess && kc.onAuthRefreshSuccess();
                    promise.setSuccess(true);
                } else {
                    kc.onAuthRefreshError && kc.onAuthRefreshError();
                    promise.setError();
                }
            }
        };

        req.send(params);

        return promise.promise;
    }

    function getRealmUrl() {
        return kc.authServerUrl + '/realms/' + encodeURIComponent(kc.realm);
    }

    function processCallback(oauth, promise) {
        var code = oauth.code;
        var error = oauth.error;
        var prompt = oauth.prompt;

        if (code) {
            var params = 'code=' + code;
            var url = getRealmUrl() + '/tokens/access/codes';

            var req = new XMLHttpRequest();
            req.open('POST', url, true);
            req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

            if (kc.clientId && kc.clientSecret) {
                req.setRequestHeader('Authorization', 'Basic ' + btoa(kc.clientId + ':' + kc.clientSecret));
            } else {
                params += '&client_id=' + encodeURIComponent(kc.clientId);
            }

            req.withCredentials = true;

            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        var tokenResponse = JSON.parse(req.responseText);
                        setToken(tokenResponse['access_token'], tokenResponse['refresh_token']);
                        kc.onAuthSuccess && kc.onAuthSuccess();
                        promise && promise.setSuccess();
                    } else {
                        kc.onAuthError && kc.onAuthError();
                        promise && promise.setError();
                    }
                }
            };

            req.send(params);
        } else if (error) {
            if (prompt != 'none') {
                kc.onAuthError && kc.onAuthError();
                promise && promise.setError();
            } else {
                promise && promise.setSuccess();
            }
        }
    }

    function loadConfig(url) {
        var promise = createPromise();
        var configUrl;

        if (!config) {
            configUrl = 'keycloak.json';
        } else if (typeof config === 'string') {
            configUrl = config;
        }

        if (configUrl) {
            var req = new XMLHttpRequest();
            req.open('GET', configUrl, true);
            req.setRequestHeader('Accept', 'application/json');

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        var config = JSON.parse(req.responseText);

                        kc.authServerUrl = config['auth-server-url'];
                        kc.realm = config['realm'];
                        kc.clientId = config['resource'];

                        promise.setSuccess();
                    } else {
                        promise.setError();
                    }
                }
            };

            req.send();
        } else {
            if (!config['url']) {
                var scripts = document.getElementsByTagName('script');
                for (var i = 0; i < scripts.length; i++) {
                    if (scripts[i].src.match(/.*keycloak\.js/)) {
                        config.url = scripts[i].src.substr(0, scripts[i].src.indexOf('/js/keycloak.js'));
                        break;
                    }
                }
            }

            if (!config.realm) {
                throw 'realm missing';
            }

            if (!config.clientId) {
                throw 'clientId missing';
            }

            kc.authServerUrl = config.url;
            kc.realm = config.realm;
            kc.clientId = config.clientId;

            promise.setSuccess();
        }

        return promise.promise;
    }

    function clearToken() {
        setToken(null, null);
        kc.onAuthLogout && kc.onAuthLogout();
    }

    function setToken(token, refreshToken) {
        if (token) {
            kc.token = token;
            kc.tokenParsed = JSON.parse(decodeURIComponent(escape(window.atob( token.split('.')[1] ))));
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
        } else {
            delete kc.token;
            delete kc.tokenParsed;
            delete kc.subject;
            delete kc.realmAccess;
            delete kc.resourceAccess;
            delete kc.idToken;

            kc.authenticated = false;
        }

        if (refreshToken) {
            kc.refreshToken = refreshToken;
            kc.refreshTokenParsed = JSON.parse(atob(refreshToken.split('.')[1]));
        } else {
            delete kc.refreshToken;
            delete kc.refreshTokenParsed;
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

    function parseCallback(url) {
        if (url.indexOf('?') != -1) {
            var oauth = {};

            var params = url.split('?')[1].split('&');
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

            if ((oauth.code || oauth.error) && oauth.state && oauth.state == sessionStorage.oauthState) {
                delete sessionStorage.oauthState;
                return oauth;
            }
        }
    }

    function createPromise() {
        var p = {
            setSuccess: function(result) {
                p.success = true;
                p.result = result;
                if (p.successCallback) {
                    p.successCallback(result);
                }
            },

            setError: function(result) {
                p.error = true;
                p.result = result;
                if (p.errorCallback) {
                    p.errorCallback(result);
                }
            },

            promise: {
                success: function(callback) {
                    if (p.success) {
                        callback(p.result);
                    } else if (!p.error) {
                        p.successCallback = callback;
                    }
                    return p.promise;
                },
                error: function(callback) {
                    if (p.error) {
                        callback(p.result);
                    } else if (!p.success) {
                        p.errorCallback = callback;
                    }
                    return p.promise;
                }
            }
        }
        return p;
    }

    function loadAdapter(type) {
        if (!type || type == 'default') {
            return {
                login: function(options) {
                    window.location.href = kc.createLoginUrl(options);
                },

                logout: function(options) {
                    window.location.href = kc.createLogoutUrl(options);
                },

                accountManagement : function() {
                    window.location.href = kc.createAccountUrl();
                },

                redirectUri: function(options) {
                    if (options && options.redirectUri) {
                        return options.redirectUri;
                    } else if (kc.redirectUri) {
                        return kc.redirectUri;
                    } else {
                        var url = (location.protocol + '//' + location.hostname + (location.port && (':' + location.port)) + location.pathname);
                        if (location.hash) {
                            url += '?redirect_fragment=' + encodeURIComponent(location.hash.substring(1));
                        }
                        return url;
                    }
                }
            };
        }

        if (type == 'cordova') {
            console.debug('Enabling Cordova support');

            return {
                login: function(options) {
                    var promise = createPromise();

                    var o = 'location=no';
                    if (options && options.prompt == 'none') {
                        o += ',hidden=yes';
                    }

                    var loginUrl = kc.createLoginUrl(options);
                    var ref = window.open(loginUrl, '_blank', o);

                    var callback;
                    var error;

                    ref.addEventListener('loadstart', function(event) {
                        if (event.url.indexOf('http://localhost') == 0) {
                            callback = parseCallback(event.url);
                            ref.close();
                        }
                    });

                    ref.addEventListener('loaderror', function(event) {
                        if (event.url.indexOf('http://localhost') != 0) {
                            error = true;
                            ref.close();
                        }
                    });

                    ref.addEventListener('exit', function(event) {
                        if (error || !callback) {
                            promise.setError();
                        } else {
                            processCallback(callback, promise);
                        }
                    });

                    return promise.promise;
                },

                logout: function(options) {
                    var promise = createPromise();

                    var logoutUrl = kc.createLogoutUrl(options);
                    var ref = window.open(logoutUrl, '_blank', 'location=no,hidden=yes');

                    var error;

                    ref.addEventListener('loadstart', function(event) {
                        if (event.url.indexOf('http://localhost') == 0) {
                            ref.close();
                        }
                    });

                    ref.addEventListener('loaderror', function(event) {
                        if (event.url.indexOf('http://localhost') != 0) {
                            error = true;
                            ref.close();
                        }
                    });

                    ref.addEventListener('exit', function(event) {
                        if (error) {
                            promise.setError();
                        } else {
                            clearToken();
                            promise.setSuccess();
                        }
                    });

                    return promise.promise;
                },

                accountManagement : function() {
                    var accountUrl = kc.createAccountUrl();
                    var ref = window.open(accountUrl, '_blank', 'location=no');
                    ref.addEventListener('loadstart', function(event) {
                        if (event.url.indexOf('http://localhost') == 0) {
                            ref.close();
                        }
                    });
                },

                redirectUri: function(options) {
                    return 'http://localhost';
                }
            }
        }

        throw 'invalid adapter type: ' + type;
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
