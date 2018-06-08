/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function( window, undefined ) {

    var Keycloak = function (config) {
        if (!(this instanceof Keycloak)) {
            return new Keycloak(config);
        }

        var kc = this;
        var adapter;
        var refreshQueue = [];
        var callbackStorage;

        var loginIframe = {
            enable: true,
            callbackList: [],
            interval: 5
        };

        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            if ((scripts[i].src.indexOf('keycloak.js') !== -1 || scripts[i].src.indexOf('keycloak.min.js') !== -1) && scripts[i].src.indexOf('version=') !== -1) {
                kc.iframeVersion = scripts[i].src.substring(scripts[i].src.indexOf('version=') + 8).split('&')[0];
            }
        }

        var useNonce = true;
        
        kc.init = function (initOptions) {
            kc.authenticated = false;

            callbackStorage = createCallbackStorage();

            if (initOptions && initOptions.adapter === 'cordova') {
                adapter = loadAdapter('cordova');
            } else if (initOptions && initOptions.adapter === 'default') {
                adapter = loadAdapter();
            } else if (initOptions && typeof initOptions.adapter === "object") {
                adapter = initOptions.adapter;
            } else {
                if (window.Cordova || window.cordova) {
                    adapter = loadAdapter('cordova');
                } else {
                    adapter = loadAdapter();
                }
            }

            if (initOptions) {
                if (typeof initOptions.useNonce !== 'undefined') {
                    useNonce = initOptions.useNonce;
                }

                if (typeof initOptions.checkLoginIframe !== 'undefined') {
                    loginIframe.enable = initOptions.checkLoginIframe;
                }

                if (initOptions.checkLoginIframeInterval) {
                    loginIframe.interval = initOptions.checkLoginIframeInterval;
                }

                if (initOptions.onLoad === 'login-required') {
                    kc.loginRequired = true;
                }

                if (initOptions.responseMode) {
                    if (initOptions.responseMode === 'query' || initOptions.responseMode === 'fragment') {
                        kc.responseMode = initOptions.responseMode;
                    } else {
                        throw 'Invalid value for responseMode';
                    }
                }

                if (initOptions.flow) {
                    switch (initOptions.flow) {
                        case 'standard':
                            kc.responseType = 'code';
                            break;
                        case 'implicit':
                            kc.responseType = 'id_token token';
                            break;
                        case 'hybrid':
                            kc.responseType = 'code id_token token';
                            break;
                        default:
                            throw 'Invalid value for flow';
                    }
                    kc.flow = initOptions.flow;
                }

                if (initOptions.timeSkew != null) {
                    kc.timeSkew = initOptions.timeSkew;
                }
            }

            if (!kc.responseMode) {
                kc.responseMode = 'fragment';
            }
            if (!kc.responseType) {
                kc.responseType = 'code';
                kc.flow = 'standard';
            }

            var promise = createPromise();

            var initPromise = createPromise();
            initPromise.promise.success(function() {
                kc.onReady && kc.onReady(kc.authenticated);
                promise.setSuccess(kc.authenticated);
            }).error(function(errorData) {
                promise.setError(errorData);
            });

            var configPromise = loadConfig(config);

            function onLoad() {
                var doLogin = function(prompt) {
                    if (!prompt) {
                        options.prompt = 'none';
                    }
                    kc.login(options).success(function () {
                        initPromise.setSuccess();
                    }).error(function () {
                        initPromise.setError();
                    });
                }

                var options = {};
                switch (initOptions.onLoad) {
                    case 'check-sso':
                        if (loginIframe.enable) {
                            setupCheckLoginIframe().success(function() {
                                checkLoginIframe().success(function () {
                                    doLogin(false);
                                }).error(function () {
                                    initPromise.setSuccess();
                                });
                            });
                        } else {
                            doLogin(false);
                        }
                        break;
                    case 'login-required':
                        doLogin(true);
                        break;
                    default:
                        throw 'Invalid value for onLoad';
                }
            }

            function processInit() {
                var callback = parseCallback(window.location.href);

                if (callback) {
                    window.history.replaceState({}, null, callback.newUrl);
                }

                if (callback && callback.valid) {
                    return setupCheckLoginIframe().success(function() {
                        processCallback(callback, initPromise);
                    }).error(function (e) {
                        initPromise.setError();
                    });
                } else if (initOptions) {
                    if (initOptions.token && initOptions.refreshToken) {
                        setToken(initOptions.token, initOptions.refreshToken, initOptions.idToken);

                        if (loginIframe.enable) {
                            setupCheckLoginIframe().success(function() {
                                checkLoginIframe().success(function () {
                                    kc.onAuthSuccess && kc.onAuthSuccess();
                                    initPromise.setSuccess();
                                }).error(function () {
                                    setToken(null, null, null);
                                    initPromise.setSuccess();
                                });
                            });
                        } else {
                            kc.updateToken(-1).success(function() {
                                kc.onAuthSuccess && kc.onAuthSuccess();
                                initPromise.setSuccess();
                            }).error(function() {
                                kc.onAuthError && kc.onAuthError();
                                if (initOptions.onLoad) {
                                    onLoad();
                                } else {
                                    initPromise.setError();
                                }
                            });
                        }
                    } else if (initOptions.onLoad) {
                        onLoad();
                    } else {
                        initPromise.setSuccess();
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
            var nonce = createUUID();

            var redirectUri = adapter.redirectUri(options);

            var callbackState = {
                state: state,
                nonce: nonce,
                redirectUri: encodeURIComponent(redirectUri)
            }

            if (options && options.prompt) {
                callbackState.prompt = options.prompt;
            }

            callbackStorage.add(callbackState);

            var baseUrl;
            if (options && options.action == 'register') {
                baseUrl = kc.endpoints.register();
            } else {
                baseUrl = kc.endpoints.authorize();
            }

            var scope;
            if (options && options.scope) {
                if (options.scope.indexOf("openid") != -1) {
                    scope = options.scope;
                } else {
                    scope = "openid " + options.scope;
                }
            } else {
                scope = "openid";
            }

            var url = baseUrl
                + '?client_id=' + encodeURIComponent(kc.clientId)
                + '&redirect_uri=' + encodeURIComponent(redirectUri)
                + '&state=' + encodeURIComponent(state)
                + '&response_mode=' + encodeURIComponent(kc.responseMode)
                + '&response_type=' + encodeURIComponent(kc.responseType)
                + '&scope=' + encodeURIComponent(scope);
                if (useNonce) {
                    url = url + '&nonce=' + encodeURIComponent(nonce);
                }

            if (options && options.prompt) {
                url += '&prompt=' + encodeURIComponent(options.prompt);
            }

            if (options && options.maxAge) {
                url += '&max_age=' + encodeURIComponent(options.maxAge);
            }

            if (options && options.loginHint) {
                url += '&login_hint=' + encodeURIComponent(options.loginHint);
            }

            if (options && options.idpHint) {
                url += '&kc_idp_hint=' + encodeURIComponent(options.idpHint);
            }

            if (options && options.locale) {
                url += '&ui_locales=' + encodeURIComponent(options.locale);
            }
            
            if (options && options.kcLocale) {
                url += '&kc_locale=' + encodeURIComponent(options.kcLocale);
            }

            return url;
        }

        kc.logout = function(options) {
            return adapter.logout(options);
        }

        kc.createLogoutUrl = function(options) {
            var url = kc.endpoints.logout()
                + '?redirect_uri=' + encodeURIComponent(adapter.redirectUri(options, false));

            return url;
        }

        kc.register = function (options) {
            return adapter.register(options);
        }

        kc.createRegisterUrl = function(options) {
            if (!options) {
                options = {};
            }
            options.action = 'register';
            return kc.createLoginUrl(options);
        }

        kc.createAccountUrl = function(options) {
            var realm = getRealmUrl();
            var url = undefined;
            if (typeof realm !== 'undefined') {
                url = realm
                + '/account'
                + '?referrer=' + encodeURIComponent(kc.clientId)
                + '&referrer_uri=' + encodeURIComponent(adapter.redirectUri(options));
            }
            return url;
        }

        kc.accountManagement = function() {
            return adapter.accountManagement();
        }

        kc.hasRealmRole = function (role) {
            var access = kc.realmAccess;
            return !!access && access.roles.indexOf(role) >= 0;
        }

        kc.hasResourceRole = function(role, resource) {
            if (!kc.resourceAccess) {
                return false;
            }

            var access = kc.resourceAccess[resource || kc.clientId];
            return !!access && access.roles.indexOf(role) >= 0;
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

        kc.loadUserInfo = function() {
            var url = kc.endpoints.userinfo();
            var req = new XMLHttpRequest();
            req.open('GET', url, true);
            req.setRequestHeader('Accept', 'application/json');
            req.setRequestHeader('Authorization', 'bearer ' + kc.token);

            var promise = createPromise();

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        kc.userInfo = JSON.parse(req.responseText);
                        promise.setSuccess(kc.userInfo);
                    } else {
                        promise.setError();
                    }
                }
            }

            req.send();

            return promise.promise;
        }

        kc.isTokenExpired = function(minValidity) {
            if (!kc.tokenParsed || (!kc.refreshToken && kc.flow != 'implicit' )) {
                throw 'Not authenticated';
            }

            if (kc.timeSkew == null) {
                console.info('[KEYCLOAK] Unable to determine if token is expired as timeskew is not set');
                return true;
            }

            var expiresIn = kc.tokenParsed['exp'] - Math.ceil(new Date().getTime() / 1000) + kc.timeSkew;
            if (minValidity) {
                expiresIn -= minValidity;
            }
            return expiresIn < 0;
        }

        kc.updateToken = function(minValidity) {
            var promise = createPromise();

            if (!kc.refreshToken) {
                promise.setError();
                return promise.promise;
            }

            minValidity = minValidity || 5;

            var exec = function() {
                var refreshToken = false;
                if (minValidity == -1) {
                    refreshToken = true;
                    console.info('[KEYCLOAK] Refreshing token: forced refresh');
                } else if (!kc.tokenParsed || kc.isTokenExpired(minValidity)) {
                    refreshToken = true;
                    console.info('[KEYCLOAK] Refreshing token: token expired');
                }

                if (!refreshToken) {
                    promise.setSuccess(false);
                } else {
                    var params = 'grant_type=refresh_token&' + 'refresh_token=' + kc.refreshToken;
                    var url = kc.endpoints.token();

                    refreshQueue.push(promise);

                    if (refreshQueue.length == 1) {
                        var req = new XMLHttpRequest();
                        req.open('POST', url, true);
                        req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
                        req.withCredentials = true;

                        if (kc.clientId && kc.clientSecret) {
                            req.setRequestHeader('Authorization', 'Basic ' + btoa(kc.clientId + ':' + kc.clientSecret));
                        } else {
                            params += '&client_id=' + encodeURIComponent(kc.clientId);
                        }

                        var timeLocal = new Date().getTime();

                        req.onreadystatechange = function () {
                            if (req.readyState == 4) {
                                if (req.status == 200) {
                                    console.info('[KEYCLOAK] Token refreshed');

                                    timeLocal = (timeLocal + new Date().getTime()) / 2;

                                    var tokenResponse = JSON.parse(req.responseText);

                                    setToken(tokenResponse['access_token'], tokenResponse['refresh_token'], tokenResponse['id_token'], timeLocal);

                                    kc.onAuthRefreshSuccess && kc.onAuthRefreshSuccess();
                                    for (var p = refreshQueue.pop(); p != null; p = refreshQueue.pop()) {
                                        p.setSuccess(true);
                                    }
                                } else {
                                    console.warn('[KEYCLOAK] Failed to refresh token');

                                    if (req.status == 400) {
                                        kc.clearToken();
                                    }

                                    kc.onAuthRefreshError && kc.onAuthRefreshError();
                                    for (var p = refreshQueue.pop(); p != null; p = refreshQueue.pop()) {
                                        p.setError(true);
                                    }
                                }
                            }
                        };

                        req.send(params);
                    }
                }
            }

            if (loginIframe.enable) {
                var iframePromise = checkLoginIframe();
                iframePromise.success(function() {
                    exec();
                }).error(function() {
                    promise.setError();
                });
            } else {
                exec();
            }

            return promise.promise;
        }

        kc.clearToken = function() {
            if (kc.token) {
                setToken(null, null, null);
                kc.onAuthLogout && kc.onAuthLogout();
                if (kc.loginRequired) {
                    kc.login();
                }
            }
        }

        function getRealmUrl() {
            if (typeof kc.authServerUrl !== 'undefined') {
                if (kc.authServerUrl.charAt(kc.authServerUrl.length - 1) == '/') {
                    return kc.authServerUrl + 'realms/' + encodeURIComponent(kc.realm);
                } else {
                    return kc.authServerUrl + '/realms/' + encodeURIComponent(kc.realm);
                }
            } else {
            	return undefined;
            }
        }

        function getOrigin() {
            if (!window.location.origin) {
                return window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
            } else {
                return window.location.origin;
            }
        }

        function processCallback(oauth, promise) {
            var code = oauth.code;
            var error = oauth.error;
            var prompt = oauth.prompt;

            var timeLocal = new Date().getTime();

            if (error) {
                if (prompt != 'none') {
                    var errorData = { error: error, error_description: oauth.error_description };
                    kc.onAuthError && kc.onAuthError(errorData);
                    promise && promise.setError(errorData);
                } else {
                    promise && promise.setSuccess();
                }
                return;
            } else if ((kc.flow != 'standard') && (oauth.access_token || oauth.id_token)) {
                authSuccess(oauth.access_token, null, oauth.id_token, true);
            }

            if ((kc.flow != 'implicit') && code) {
                var params = 'code=' + code + '&grant_type=authorization_code';
                var url = kc.endpoints.token();

                var req = new XMLHttpRequest();
                req.open('POST', url, true);
                req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

                if (kc.clientId && kc.clientSecret) {
                    req.setRequestHeader('Authorization', 'Basic ' + btoa(kc.clientId + ':' + kc.clientSecret));
                } else {
                    params += '&client_id=' + encodeURIComponent(kc.clientId);
                }

                params += '&redirect_uri=' + oauth.redirectUri;

                req.withCredentials = true;

                req.onreadystatechange = function() {
                    if (req.readyState == 4) {
                        if (req.status == 200) {

                            var tokenResponse = JSON.parse(req.responseText);
                            authSuccess(tokenResponse['access_token'], tokenResponse['refresh_token'], tokenResponse['id_token'], kc.flow === 'standard');
                        } else {
                            kc.onAuthError && kc.onAuthError();
                            promise && promise.setError();
                        }
                    }
                };

                req.send(params);
            }

            function authSuccess(accessToken, refreshToken, idToken, fulfillPromise) {
                timeLocal = (timeLocal + new Date().getTime()) / 2;

                setToken(accessToken, refreshToken, idToken, timeLocal);

                if (useNonce && ((kc.tokenParsed && kc.tokenParsed.nonce != oauth.storedNonce) ||
                    (kc.refreshTokenParsed && kc.refreshTokenParsed.nonce != oauth.storedNonce) ||
                    (kc.idTokenParsed && kc.idTokenParsed.nonce != oauth.storedNonce))) {

                    console.info('[KEYCLOAK] Invalid nonce, clearing token');
                    kc.clearToken();
                    promise && promise.setError();
                } else {
                    if (fulfillPromise) {
                        kc.onAuthSuccess && kc.onAuthSuccess();
                        promise && promise.setSuccess();
                    }
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

            function setupOidcEndoints(oidcConfiguration) {
                if (! oidcConfiguration) {
                    kc.endpoints = {
                        authorize: function() {
                            return getRealmUrl() + '/protocol/openid-connect/auth';
                        },
                        token: function() {
                            return getRealmUrl() + '/protocol/openid-connect/token';
                        },
                        logout: function() {
                            return getRealmUrl() + '/protocol/openid-connect/logout';
                        },
                        checkSessionIframe: function() {
                            var src = getRealmUrl() + '/protocol/openid-connect/login-status-iframe.html';
                            if (kc.iframeVersion) {
                              src = src + '?version=' + kc.iframeVersion;
                            }
                            return src;
                        },
                        register: function() {
                            return getRealmUrl() + '/protocol/openid-connect/registrations';
                        },
                        userinfo: function() {
                            return getRealmUrl() + '/protocol/openid-connect/userinfo';
                        }
                    };
                } else {
                    kc.endpoints = {
                        authorize: function() {
                            return oidcConfiguration.authorization_endpoint;
                        },
                        token: function() {
                            return oidcConfiguration.token_endpoint;
                        },
                        logout: function() {
                            if (!oidcConfiguration.end_session_endpoint) {
                                throw "Not supported by the OIDC server";
                            }
                            return oidcConfiguration.end_session_endpoint;
                        },
                        checkSessionIframe: function() {
                            if (!oidcConfiguration.check_session_iframe) {
                                throw "Not supported by the OIDC server";
                            }
                            return oidcConfiguration.check_session_iframe;
                        },
                        register: function() {
                            throw 'Redirection to "Register user" page not supported in standard OIDC mode';
                        },
                        userinfo: function() {
                            if (!oidcConfiguration.userinfo_endpoint) {
                                throw "Not supported by the OIDC server";
                            }
                            return oidcConfiguration.userinfo_endpoint;
                        }
                    }
                }
            }

            if (configUrl) {
                var req = new XMLHttpRequest();
                req.open('GET', configUrl, true);
                req.setRequestHeader('Accept', 'application/json');

                req.onreadystatechange = function () {
                    if (req.readyState == 4) {
                        if (req.status == 200 || fileLoaded(req)) {
                            var config = JSON.parse(req.responseText);

                            kc.authServerUrl = config['auth-server-url'];
                            kc.realm = config['realm'];
                            kc.clientId = config['resource'];
                            kc.clientSecret = (config['credentials'] || {})['secret'];
                            setupOidcEndoints(null);
                            promise.setSuccess();
                        } else {
                            promise.setError();
                        }
                    }
                };

                req.send();
            } else {
                if (!config.clientId) {
                    throw 'clientId missing';
                }

                kc.clientId = config.clientId;
                kc.clientSecret = (config.credentials || {}).secret;

                var oidcProvider = config['oidcProvider'];
                if (!oidcProvider) {
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

                    kc.authServerUrl = config.url;
                    kc.realm = config.realm;
                    setupOidcEndoints(null);
                    promise.setSuccess();
                } else {
                    if (typeof oidcProvider === 'string') {
                        var oidcProviderConfigUrl;
                        if (oidcProvider.charAt(oidcProvider.length - 1) == '/') {
                            oidcProviderConfigUrl = oidcProvider + '.well-known/openid-configuration';
                        } else {
                            oidcProviderConfigUrl = oidcProvider + '/.well-known/openid-configuration';
                        }
                        var req = new XMLHttpRequest();
                        req.open('GET', oidcProviderConfigUrl, true);
                        req.setRequestHeader('Accept', 'application/json');

                        req.onreadystatechange = function () {
                            if (req.readyState == 4) {
                                if (req.status == 200 || fileLoaded(req)) {
                                    var oidcProviderConfig = JSON.parse(req.responseText);
                                    setupOidcEndoints(oidcProviderConfig);
                                    promise.setSuccess();
                                } else {
                                    promise.setError();
                                }
                            }
                        };

                        req.send();
                    } else {
                        setupOidcEndoints(oidcProvider);
                        promise.setSuccess();
                    }
                }
            }

            return promise.promise;
        }

        function fileLoaded(xhr) {
            return xhr.status == 0 && xhr.responseText && xhr.responseURL.startsWith('file:');
        }

        function setToken(token, refreshToken, idToken, timeLocal) {
            if (kc.tokenTimeoutHandle) {
                clearTimeout(kc.tokenTimeoutHandle);
                kc.tokenTimeoutHandle = null;
            }

            if (refreshToken) {
                kc.refreshToken = refreshToken;
                kc.refreshTokenParsed = decodeToken(refreshToken);
            } else {
                delete kc.refreshToken;
                delete kc.refreshTokenParsed;
            }

            if (idToken) {
                kc.idToken = idToken;
                kc.idTokenParsed = decodeToken(idToken);
            } else {
                delete kc.idToken;
                delete kc.idTokenParsed;
            }

            if (token) {
                kc.token = token;
                kc.tokenParsed = decodeToken(token);
                kc.sessionId = kc.tokenParsed.session_state;
                kc.authenticated = true;
                kc.subject = kc.tokenParsed.sub;
                kc.realmAccess = kc.tokenParsed.realm_access;
                kc.resourceAccess = kc.tokenParsed.resource_access;

                if (timeLocal) {
                    kc.timeSkew = Math.floor(timeLocal / 1000) - kc.tokenParsed.iat;
                }

                if (kc.timeSkew != null) {
                    console.info('[KEYCLOAK] Estimated time difference between browser and server is ' + kc.timeSkew + ' seconds');

                    if (kc.onTokenExpired) {
                        var expiresIn = (kc.tokenParsed['exp'] - (new Date().getTime() / 1000) + kc.timeSkew) * 1000;
                        console.info('[KEYCLOAK] Token expires in ' + Math.round(expiresIn / 1000) + ' s');
                        if (expiresIn <= 0) {
                            kc.onTokenExpired();
                        } else {
                            kc.tokenTimeoutHandle = setTimeout(kc.onTokenExpired, expiresIn);
                        }
                    }
                }
            } else {
                delete kc.token;
                delete kc.tokenParsed;
                delete kc.subject;
                delete kc.realmAccess;
                delete kc.resourceAccess;

                kc.authenticated = false;
            }
        }

        function decodeToken(str) {
            str = str.split('.')[1];

            str = str.replace('/-/g', '+');
            str = str.replace('/_/g', '/');
            switch (str.length % 4)
            {
                case 0:
                    break;
                case 2:
                    str += '==';
                    break;
                case 3:
                    str += '=';
                    break;
                default:
                    throw 'Invalid token';
            }

            str = (str + '===').slice(0, str.length + (str.length % 4));
            str = str.replace(/-/g, '+').replace(/_/g, '/');

            str = decodeURIComponent(escape(atob(str)));

            str = JSON.parse(str);
            return str;
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

        kc.callback_id = 0;

        function createCallbackId() {
            var id = '<id: ' + (kc.callback_id++) + (Math.random()) + '>';
            return id;

        }

        function parseCallback(url) {
            var oauth = parseCallbackUrl(url);
            if (!oauth) {
                return;
            }

            var oauthState = callbackStorage.get(oauth.state);

            if (oauthState) {
                oauth.valid = true;
                oauth.redirectUri = oauthState.redirectUri;
                oauth.storedNonce = oauthState.nonce;
                oauth.prompt = oauthState.prompt;
            }

            return oauth;
        }

        function parseCallbackUrl(url) {
            var supportedParams;
            switch (kc.flow) {
                case 'standard':
                    supportedParams = ['code', 'state', 'session_state'];
                    break;
                case 'implicit':
                    supportedParams = ['access_token', 'id_token', 'state', 'session_state'];
                    break;
                case 'hybrid':
                    supportedParams = ['access_token', 'id_token', 'code', 'state', 'session_state'];
                    break;
            }

            supportedParams.push('error');
            supportedParams.push('error_description');
            supportedParams.push('error_uri');

            var queryIndex = url.indexOf('?');
            var fragmentIndex = url.indexOf('#');

            var newUrl;
            var parsed;

            if (kc.responseMode === 'query' && queryIndex !== -1) {
                newUrl = url.substring(0, queryIndex);
                parsed = parseCallbackParams(url.substring(queryIndex + 1, fragmentIndex !== -1 ? fragmentIndex : url.length), supportedParams);
                if (parsed.paramsString !== '') {
                    newUrl += '?' + parsed.paramsString;
                }
                if (fragmentIndex !== -1) {
                    newUrl += url.substring(fragmentIndex);
                }
            } else if (kc.responseMode === 'fragment' && fragmentIndex !== -1) {
                newUrl = url.substring(0, fragmentIndex);
                parsed = parseCallbackParams(url.substring(fragmentIndex + 1), supportedParams);
                if (parsed.paramsString !== '') {
                    newUrl += '#' + parsed.paramsString;
                }
            }

            if (parsed && parsed.oauthParams) {
                if (kc.flow === 'standard' || kc.flow === 'hybrid') {
                    if ((parsed.oauthParams.code || parsed.oauthParams.error) && parsed.oauthParams.state) {
                        parsed.oauthParams.newUrl = newUrl;
                        return parsed.oauthParams;
                    }
                } else if (kc.flow === 'implicit') {
                    if ((parsed.oauthParams.access_token || parsed.oauthParams.error) && parsed.oauthParams.state) {
                        parsed.oauthParams.newUrl = newUrl;
                        return parsed.oauthParams;
                    }
                }
            }
        }

        function parseCallbackParams(paramsString, supportedParams) {
            var p = paramsString.split('&');
            var result = {
                paramsString: '',
                oauthParams: {}
            }
            for (var i = 0; i < p.length; i++) {
                var t = p[i].split('=');
                if (supportedParams.indexOf(t[0]) !== -1) {
                    result.oauthParams[t[0]] = t[1];
                } else {
                    if (result.paramsString !== '') {
                        result.paramsString += '&';
                    }
                    result.paramsString += p[i];
                }
            }
            return result;
        }

        function createPromise() {
            if (typeof Promise === "function") {
                return createNativePromise();
            } else {
                return createLegacyPromise();
            }
        }

        function createNativePromise() {
            // Need to create a native Promise which also preserves the
            // interface of the custom promise type previously used by the API
            var p = {
                setSuccess: function(result) {
                    p.success = true;
                    p.resolve(result);
                },

                setError: function(result) {
                    p.success = false;
                    p.reject(result);
                }
            };
            p.promise = new Promise(function(resolve, reject) {
                p.resolve = resolve;
                p.reject = reject;
            });
            p.promise.success = function(callback) {
                p.promise.then(callback);
                return p.promise;
            }
            p.promise.error = function(callback) {
                p.promise.catch(callback);
                return p.promise;
            }
            return p;
        }

        function createLegacyPromise() {
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

        function setupCheckLoginIframe() {
            var promise = createPromise();

            if (!loginIframe.enable) {
                promise.setSuccess();
                return promise.promise;
            }

            if (loginIframe.iframe) {
                promise.setSuccess();
                return promise.promise;
            }

            var iframe = document.createElement('iframe');
            loginIframe.iframe = iframe;

            iframe.onload = function() {
                var authUrl = kc.endpoints.authorize();
                if (authUrl.charAt(0) === '/') {
                    loginIframe.iframeOrigin = getOrigin();
                } else {
                    loginIframe.iframeOrigin = authUrl.substring(0, authUrl.indexOf('/', 8));
                }
                promise.setSuccess();

                setTimeout(check, loginIframe.interval * 1000);
            }

            var src = kc.endpoints.checkSessionIframe();
            iframe.setAttribute('src', src );
            iframe.setAttribute('title', 'keycloak-session-iframe' );
            iframe.style.display = 'none';
            document.body.appendChild(iframe);

            var messageCallback = function(event) {
                if ((event.origin !== loginIframe.iframeOrigin) || (loginIframe.iframe.contentWindow !== event.source)) {
                    return;
                }

                if (!(event.data == 'unchanged' || event.data == 'changed' || event.data == 'error')) {
                    return;
                }


                if (event.data != 'unchanged') {
                    kc.clearToken();
                }

                var callbacks = loginIframe.callbackList.splice(0, loginIframe.callbackList.length);

                for (var i = callbacks.length - 1; i >= 0; --i) {
                    var promise = callbacks[i];
                    if (event.data == 'unchanged') {
                        promise.setSuccess();
                    } else {
                        promise.setError();
                    }
                }
            };

            window.addEventListener('message', messageCallback, false);

            var check = function() {
                checkLoginIframe();
                if (kc.token) {
                    setTimeout(check, loginIframe.interval * 1000);
                }
            };

            return promise.promise;
        }

        function checkLoginIframe() {
            var promise = createPromise();

            if (loginIframe.iframe && loginIframe.iframeOrigin ) {
                var msg = kc.clientId + ' ' + kc.sessionId;
                loginIframe.callbackList.push(promise);
                var origin = loginIframe.iframeOrigin;
                if (loginIframe.callbackList.length == 1) {
                    loginIframe.iframe.contentWindow.postMessage(msg, origin);
                }
            } else {
                promise.setSuccess();
            }

            return promise.promise;
        }

        function loadAdapter(type) {
            if (!type || type == 'default') {
                return {
                    login: function(options) {
                        window.location.href = kc.createLoginUrl(options);
                        return createPromise().promise;
                    },

                    logout: function(options) {
                        window.location.href = kc.createLogoutUrl(options);
                        return createPromise().promise;
                    },

                    register: function(options) {
                        window.location.href = kc.createRegisterUrl(options);
                        return createPromise().promise;
                    },

                    accountManagement : function() {
                        var accountUrl = kc.createAccountUrl();
                        if (typeof accountUrl !== 'undefined') {
                            window.location.href = accountUrl;
                        } else {
                            throw "Not supported by the OIDC server";
                        }
                        return createPromise().promise;
                    },

                    redirectUri: function(options, encodeHash) {
                        if (arguments.length == 1) {
                            encodeHash = true;
                        }

                        if (options && options.redirectUri) {
                            return options.redirectUri;
                        } else if (kc.redirectUri) {
                            return kc.redirectUri;
                        } else {
                            return location.href;
                        }
                    }
                };
            }

            if (type == 'cordova') {
                loginIframe.enable = false;
                var cordovaOpenWindowWrapper = function(loginUrl, target, options) {
                    if (window.cordova && window.cordova.InAppBrowser) {
                        // Use inappbrowser for IOS and Android if available
                        return window.cordova.InAppBrowser.open(loginUrl, target, options);
                    } else {
                        return window.open(loginUrl, target, options);
                    }
                };
                return {
                    login: function(options) {
                        var promise = createPromise();

                        var o = 'location=no';
                        if (options && options.prompt == 'none') {
                            o += ',hidden=yes';
                        }

                        var loginUrl = kc.createLoginUrl(options);
                        var ref = cordovaOpenWindowWrapper(loginUrl, '_blank', o);
                        var completed = false;

                        ref.addEventListener('loadstart', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                var callback = parseCallback(event.url);
                                processCallback(callback, promise);
                                ref.close();
                                completed = true;
                            }
                        });

                        ref.addEventListener('loaderror', function(event) {
                            if (!completed) {
                                if (event.url.indexOf('http://localhost') == 0) {
                                    var callback = parseCallback(event.url);
                                    processCallback(callback, promise);
                                    ref.close();
                                    completed = true;
                                } else {
                                    promise.setError();
                                    ref.close();
                                }
                            }
                        });

                        return promise.promise;
                    },

                    logout: function(options) {
                        var promise = createPromise();

                        var logoutUrl = kc.createLogoutUrl(options);
                        var ref = cordovaOpenWindowWrapper(logoutUrl, '_blank', 'location=no,hidden=yes');

                        var error;

                        ref.addEventListener('loadstart', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                ref.close();
                            }
                        });

                        ref.addEventListener('loaderror', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                ref.close();
                            } else {
                                error = true;
                                ref.close();
                            }
                        });

                        ref.addEventListener('exit', function(event) {
                            if (error) {
                                promise.setError();
                            } else {
                                kc.clearToken();
                                promise.setSuccess();
                            }
                        });

                        return promise.promise;
                    },

                    register : function() {
                        var registerUrl = kc.createRegisterUrl();
                        var ref = cordovaOpenWindowWrapper(registerUrl, '_blank', 'location=no');
                        ref.addEventListener('loadstart', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                ref.close();
                            }
                        });
                    },

                    accountManagement : function() {
                        var accountUrl = kc.createAccountUrl();
                        if (typeof accountUrl !== 'undefined') {
                            var ref = cordovaOpenWindowWrapper(accountUrl, '_blank', 'location=no');
                            ref.addEventListener('loadstart', function(event) {
                                if (event.url.indexOf('http://localhost') == 0) {
                                    ref.close();
                                }
                            });
                        } else {
                            throw "Not supported by the OIDC server";
                        }
                    },

                    redirectUri: function(options) {
                        return 'http://localhost';
                    }
                }
            }

            throw 'invalid adapter type: ' + type;
        }

        var LocalStorage = function() {
            if (!(this instanceof LocalStorage)) {
                return new LocalStorage();
            }

            localStorage.setItem('kc-test', 'test');
            localStorage.removeItem('kc-test');

            var cs = this;

            function clearExpired() {
                var time = new Date().getTime();
                for (var i = 0; i < localStorage.length; i++)  {
                    var key = localStorage.key(i);
                    if (key && key.indexOf('kc-callback-') == 0) {
                        var value = localStorage.getItem(key);
                        if (value) {
                            try {
                                var expires = JSON.parse(value).expires;
                                if (!expires || expires < time) {
                                    localStorage.removeItem(key);
                                }
                            } catch (err) {
                                localStorage.removeItem(key);
                            }
                        }
                    }
                }
            }

            cs.get = function(state) {
                if (!state) {
                    return;
                }

                var key = 'kc-callback-' + state;
                var value = localStorage.getItem(key);
                if (value) {
                    localStorage.removeItem(key);
                    value = JSON.parse(value);
                }

                clearExpired();
                return value;
            };

            cs.add = function(state) {
                clearExpired();

                var key = 'kc-callback-' + state.state;
                state.expires = new Date().getTime() + (60 * 60 * 1000);
                localStorage.setItem(key, JSON.stringify(state));
            };
        };

        var CookieStorage = function() {
            if (!(this instanceof CookieStorage)) {
                return new CookieStorage();
            }

            var cs = this;

            cs.get = function(state) {
                if (!state) {
                    return;
                }

                var value = getCookie('kc-callback-' + state);
                setCookie('kc-callback-' + state, '', cookieExpiration(-100));
                if (value) {
                    return JSON.parse(value);
                }
            };

            cs.add = function(state) {
                setCookie('kc-callback-' + state.state, JSON.stringify(state), cookieExpiration(60));
            };

            cs.removeItem = function(key) {
                setCookie(key, '', cookieExpiration(-100));
            };

            var cookieExpiration = function (minutes) {
                var exp = new Date();
                exp.setTime(exp.getTime() + (minutes*60*1000));
                return exp;
            };

            var getCookie = function (key) {
                var name = key + '=';
                var ca = document.cookie.split(';');
                for (var i = 0; i < ca.length; i++) {
                    var c = ca[i];
                    while (c.charAt(0) == ' ') {
                        c = c.substring(1);
                    }
                    if (c.indexOf(name) == 0) {
                        return c.substring(name.length, c.length);
                    }
                }
                return '';
            };

            var setCookie = function (key, value, expirationDate) {
                var cookie = key + '=' + value + '; '
                    + 'expires=' + expirationDate.toUTCString() + '; ';
                document.cookie = cookie;
            }
        };

        function createCallbackStorage() {
            try {
                return new LocalStorage();
            } catch (err) {
            }

            return new CookieStorage();
        }
    }

    if ( typeof module === "object" && module && typeof module.exports === "object" ) {
        module.exports = Keycloak;
    } else {
        window.Keycloak = Keycloak;

        if ( typeof define === "function" && define.amd ) {
            define( "keycloak", [], function () { return Keycloak; } );
        }
    }
})( window );
