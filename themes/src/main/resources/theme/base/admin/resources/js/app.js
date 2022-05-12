'use strict';

var auth = {};
var resourceBundle;
var locale = 'en';

var module = angular.module('keycloak', [ 'keycloak.services', 'keycloak.loaders', 'ui.bootstrap', 'ui.select2', 'angularFileUpload', 'angularTreeview', 'pascalprecht.translate', 'ngCookies', 'ngSanitize']);
var resourceRequests = 0;
var loadingTimer = -1;
var translateProvider = null;
var currentRealm = null;

angular.element(document).ready(function () {
    var keycloakAuth = new Keycloak(consoleBaseUrl + 'config');

    function whoAmI(success, error) {
        var req = new XMLHttpRequest();
        req.open('GET', consoleBaseUrl + 'whoami', true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Authorization', 'bearer ' + keycloakAuth.token);

        req.onreadystatechange = function () {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    var data = JSON.parse(req.responseText);
                    success(data);
                } else {
                    error();
                }
            }
        }

        req.send();
    }

    function loadResourceBundle(success, error) {
        var req = new XMLHttpRequest();
        req.open('GET', consoleBaseUrl + 'messages.json?lang=' + locale, true);
        req.setRequestHeader('Accept', 'application/json');

        req.onreadystatechange = function () {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    var data = JSON.parse(req.responseText);
                    success && success(data);
                } else {
                    error && error();
                }
            }
        }

        req.send();
    }

    function loadSelect2Localization() {
        // 'en' is the built-in default and does not have to be loaded.
        var supportedLocales = ['ar', 'az', 'bg', 'ca', 'cs', 'da', 'de', 'el', 'es', 'et', 'eu', 'fa', 'fi', 'fr',
            'gl', 'he', 'hr', 'hu', 'id', 'is', 'it', 'ja', 'ka', 'ko', 'lt', 'lv', 'mk', 'ms', 'nl', 'no', 'pl',
            'pt-BR', 'pt-PT', 'ro', 'rs', 'ru', 'sk', 'sv', 'th', 'tr', 'ug-CN', 'uk', 'vi', 'zh-CN', 'zh-TW'];
        if (supportedLocales.indexOf(locale) == -1) return;
        var select2JsUrl;
        var allScriptElements = document.getElementsByTagName('script');
        for (var i = 0, n = allScriptElements.length; i < n; i++) {
            var src = allScriptElements[i].getAttribute('src');
            if (src && src.match(/\/select2\/select2\.js$/)) {
                select2JsUrl = src;
                break;
            }
        }
        if (!select2JsUrl) return;
        var scriptElement = document.createElement('script');
        scriptElement.src = select2JsUrl.replace(/\/select2\/select2\.js$/, '/select2/select2_locale_'+locale+'.js');
        scriptElement.type = 'text/javascript';
        document.getElementsByTagName('head')[0].appendChild(scriptElement);
    }

    function hasAnyAccess(user) {
        return user && user['realm_access'];
    }

    keycloakAuth.onAuthLogout = function() {
        location.reload();
    }

    auth.refreshPermissions = function(success, error) {
        whoAmI(function(data) {
            auth.user = data;
            auth.loggedIn = true;
            auth.hasAnyAccess = hasAnyAccess(data);

            success();
        }, function() {
            error();
        });
    };

    module.factory('Auth', function () {
        return auth;
    });

    keycloakAuth.init({ onLoad: 'login-required', pkceMethod: 'S256' }).then(function () {
        auth.authz = keycloakAuth;

        whoAmI(function(data) {
            auth.user = data;
            auth.loggedIn = true;
            auth.hasAnyAccess = hasAnyAccess(data);
            locale = auth.user.locale || locale;

            loadResourceBundle(function(data) {
                resourceBundle = data;

                var injector = angular.bootstrap(document, ["keycloak"]);

                injector.get('$translate')('consoleTitle').then(function (consoleTitle) {
                    document.title = consoleTitle;
                });
            });
        });

        loadSelect2Localization();
    }).catch(function () {
        window.location.reload();
    });
});

module.factory('authInterceptor', function($q, Auth) {
    return {
        request: function (config) {
            if (!config.url.match(/.html$/)) {
                var deferred = $q.defer();
                if (Auth.authz.token) {
                    Auth.authz.updateToken(5).then(function () {
                        config.headers = config.headers || {};
                        config.headers.Authorization = 'Bearer ' + Auth.authz.token;

                        deferred.resolve(config);
                    }).catch(function () {
                        location.reload();
                    });
                }
                return deferred.promise;
            } else {
                return config;
            }
        }
    };
});

module.config(['$translateProvider', function($translateProvider) {
    translateProvider = $translateProvider;
    $translateProvider.useSanitizeValueStrategy('sanitizeParameters');
    $translateProvider.preferredLanguage(locale);
    $translateProvider.translations(locale, resourceBundle);
}]);

// Change for upgrade to AngularJS 1.6
// See https://github.com/angular/angular.js/commit/aa077e81129c740041438688dff2e8d20c3d7b52
module.config(['$locationProvider', function($locationProvider) {
  $locationProvider.hashPrefix('');
}]);

module.config([ '$routeProvider', function($routeProvider) {
    $routeProvider
        .when('/create/realm', {
            templateUrl : resourceUrl + '/partials/realm-create.html',
            resolve : {

            },
            controller : 'RealmCreateCtrl'
        })
        .when('/realms/:realm', {
            templateUrl : resourceUrl + '/partials/realm-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmDetailCtrl'
        })
        .when('/realms/:realm/localization', {
            templateUrl : resourceUrl + '/partials/realm-localization.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                realmSpecificLocales : function(RealmSpecificLocalesLoader) {
                    return RealmSpecificLocalesLoader();
                }
            },
            controller : 'RealmLocalizationCtrl'
        })
        .when('/realms/:realm/localization/upload', {
            templateUrl : resourceUrl + '/partials/realm-localization-upload.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmLocalizationUploadCtrl'
        })
        .when('/realms/:realm/login-settings', {
            templateUrl : resourceUrl + '/partials/realm-login-settings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmLoginSettingsCtrl'
        })
        .when('/realms/:realm/theme-settings', {
            templateUrl : resourceUrl + '/partials/realm-theme-settings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmThemeCtrl'
        })
        .when('/realms/:realm/cache-settings', {
            templateUrl : resourceUrl + '/partials/realm-cache-settings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmCacheCtrl'
        })
        .when('/realms', {
            templateUrl : resourceUrl + '/partials/realm-list.html',
            controller : 'RealmListCtrl'
        })
        .when('/realms/:realm/token-settings', {
            templateUrl : resourceUrl + '/partials/realm-tokens.html',
            resolve : {
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmTokenDetailCtrl'
        })
        .when('/realms/:realm/user-profile', {
            templateUrl : resourceUrl + '/partials/realm-user-profile.html',
            resolve : {
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScopes : function(ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
            },
            controller : 'RealmUserProfileCtrl'
        })
        .when('/realms/:realm/client-registration/client-initial-access', {
            templateUrl : resourceUrl + '/partials/client-initial-access.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientInitialAccess : function(ClientInitialAccessLoader) {
                    return ClientInitialAccessLoader();
                }
            },
            controller : 'ClientInitialAccessCtrl'
        })
        .when('/realms/:realm/client-registration/client-initial-access/create', {
            templateUrl : resourceUrl + '/partials/client-initial-access-create.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'ClientInitialAccessCreateCtrl'
        })
        .when('/realms/:realm/client-registration/client-reg-policies', {
            templateUrl : resourceUrl + '/partials/client-reg-policies.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                policies : function(ComponentsLoader) {
                    return ComponentsLoader.loadComponents(null, 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy');
                },
                clientRegistrationPolicyProviders : function(ClientRegistrationPolicyProvidersLoader) {
                    return ClientRegistrationPolicyProvidersLoader();
                }
            },
            controller : 'ClientRegPoliciesCtrl'
        })
        .when('/realms/:realm/client-registration/client-reg-policies/create/:componentType/:providerId', {
            templateUrl : resourceUrl + '/partials/client-reg-policy-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function($route) {
                    return {
                        providerType: 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy',
                        subType: $route.current.params.componentType,
                        providerId: $route.current.params.providerId
                    };
                },
                clientRegistrationPolicyProviders : function(ClientRegistrationPolicyProvidersLoader) {
                    return ClientRegistrationPolicyProvidersLoader();
                }
            },
            controller : 'ClientRegPolicyDetailCtrl'
        })
        .when('/realms/:realm/client-registration/client-reg-policies/:provider/:componentId', {
            templateUrl : resourceUrl + '/partials/client-reg-policy-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function(ComponentLoader) {
                    return ComponentLoader();
                },
                clientRegistrationPolicyProviders : function(ClientRegistrationPolicyProvidersLoader) {
                    return ClientRegistrationPolicyProvidersLoader();
                }
            },
            controller : 'ClientRegPolicyDetailCtrl'
        })
        .when('/realms/:realm/client-policies/profiles', {
            templateUrl : resourceUrl + '/partials/client-policies-profiles-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('true');
                },
            },
            controller : 'ClientPoliciesProfilesListCtrl'
        })
        .when('/realms/:realm/client-policies/profiles-json', {
            templateUrl : resourceUrl + '/partials/client-policies-profiles-json.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('true');
                }
            },
            controller : 'ClientPoliciesProfilesJsonCtrl'
        })
        .when('/realms/:realm/client-policies/profiles-create', {
            templateUrl : resourceUrl + '/partials/client-policies-profiles-edit.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('false');
                }
            },
            controller : 'ClientPoliciesProfilesEditCtrl'
        })
        .when('/realms/:realm/client-policies/profiles-update/:profileName', {
            templateUrl : resourceUrl + '/partials/client-policies-profiles-edit.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('true');
                }
            },
            controller : 'ClientPoliciesProfilesEditCtrl'
        })
        .when('/realms/:realm/client-policies/profiles-update/:profileName/create-executor', {
            templateUrl : resourceUrl + '/partials/client-policies-profiles-edit-executor.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('false');
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientPoliciesProfilesEditExecutorCtrl'
        })
        .when('/realms/:realm/client-policies/profiles-update/:profileName/update-executor/:executorIndex', {
            templateUrl : resourceUrl + '/partials/client-policies-profiles-edit-executor.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('true');
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientPoliciesProfilesEditExecutorCtrl'
        })
        .when('/realms/:realm/client-policies/policies', {
            templateUrl : resourceUrl + '/partials/client-policies-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientPolicies : function(ClientPoliciesLoader) {
                    return ClientPoliciesLoader();
                }
            },
            controller : 'ClientPoliciesListCtrl'
        })
        .when('/realms/:realm/client-policies/policies-json', {
            templateUrl : resourceUrl + '/partials/client-policies-json.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientPolicies : function(ClientPoliciesLoader) {
                    return ClientPoliciesLoader();
                }
            },
            controller : 'ClientPoliciesJsonCtrl'
        })
        .when('/realms/:realm/client-policies/policy-create', {
            templateUrl : resourceUrl + '/partials/client-policies-policy-edit.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('true');
                },
                clientPolicies : function(ClientPoliciesLoader) {
                    return ClientPoliciesLoader();
                }
            },
            controller : 'ClientPoliciesEditCtrl'
        })
        .when('/realms/:realm/client-policies/policies-update/:policyName', {
            templateUrl : resourceUrl + '/partials/client-policies-policy-edit.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientProfiles : function(ClientPoliciesProfilesLoader) {
                    return ClientPoliciesProfilesLoader.loadClientProfiles('true');
                },
                clientPolicies : function(ClientPoliciesLoader) {
                    return ClientPoliciesLoader();
                }
            },
            controller : 'ClientPoliciesEditCtrl'
        })
        .when('/realms/:realm/client-policies/policies-update/:policyName/create-condition', {
            templateUrl : resourceUrl + '/partials/client-policies-policy-edit-condition.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientPolicies : function(ClientPoliciesLoader) {
                    return ClientPoliciesLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientPoliciesEditConditionCtrl'
        })
        .when('/realms/:realm/client-policies/policies-update/:policyName/update-condition/:conditionIndex', {
            templateUrl : resourceUrl + '/partials/client-policies-policy-edit-condition.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientPolicies : function(ClientPoliciesLoader) {
                    return ClientPoliciesLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientPoliciesEditConditionCtrl'
        })
        .when('/realms/:realm/keys', {
            templateUrl : resourceUrl + '/partials/realm-keys.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                keys: function(RealmKeysLoader) {
                    return RealmKeysLoader();
                }
            },
            controller : 'RealmKeysCtrl'
        })
        .when('/realms/:realm/keys/passive', {
            templateUrl : resourceUrl + '/partials/realm-keys-passive.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                keys: function(RealmKeysLoader) {
                    return RealmKeysLoader();
                }
            },
            controller : 'RealmKeysCtrl'
        })
        .when('/realms/:realm/keys/disabled', {
                    templateUrl : resourceUrl + '/partials/realm-keys-disabled.html',
                    resolve : {
                        realm : function(RealmLoader) {
                            return RealmLoader();
                        },
                        serverInfo : function(ServerInfoLoader) {
                            return ServerInfoLoader();
                        },
                        keys: function(RealmKeysLoader) {
                            return RealmKeysLoader();
                        }
                    },
                    controller : 'RealmKeysCtrl'
                })
        .when('/realms/:realm/keys/providers', {
            templateUrl : resourceUrl + '/partials/realm-keys-providers.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmKeysProvidersCtrl'
        })
        .when('/create/keys/:realm/providers/:provider', {
            templateUrl : resourceUrl + '/partials/realm-keys-generic.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function() {
                    return {
                    };
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericKeystoreCtrl'
        })
        .when('/realms/:realm/keys/providers/:provider/:componentId', {
            templateUrl : resourceUrl + '/partials/realm-keys-generic.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function(ComponentLoader) {
                    return ComponentLoader();
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericKeystoreCtrl'
        })
        .when('/realms/:realm/identity-provider-settings', {
            templateUrl : resourceUrl + '/partials/realm-identity-provider.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance : function(IdentityProviderLoader) {
                    return {};
                },
                providerFactory : function(IdentityProviderFactoryLoader) {
                    return {};
                },
                authFlows : function(AuthenticationFlowsLoader) {
                    return {};
                }
            },
            controller : 'RealmIdentityProviderCtrl'
        })
        .when('/create/identity-provider/:realm/:provider_id', {
            templateUrl : function(params){ return resourceUrl + '/partials/realm-identity-provider-' + params.provider_id + '.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance : function(IdentityProviderLoader) {
                    return {};
                },
                providerFactory : function(IdentityProviderFactoryLoader) {
                    return new IdentityProviderFactoryLoader();
                },
                authFlows : function(AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                }
            },
            controller : 'RealmIdentityProviderCtrl'
        })
        .when('/realms/:realm/identity-provider-settings/provider/:provider_id/:alias', {
            templateUrl : function(params){ return resourceUrl + '/partials/realm-identity-provider-' + params.provider_id + '.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance : function(IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                providerFactory : function(IdentityProviderFactoryLoader) {
                    return IdentityProviderFactoryLoader();
                },
                authFlows : function(AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                }
            },
            controller : 'RealmIdentityProviderCtrl'
        })
        .when('/realms/:realm/identity-provider-settings/provider/:provider_id/:alias/export', {
            templateUrl : resourceUrl + '/partials/realm-identity-provider-export.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                identityProvider : function(IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                providerFactory : function(IdentityProviderFactoryLoader) {
                    return IdentityProviderFactoryLoader();
                }
            },
            controller : 'RealmIdentityProviderExportCtrl'
        })
        .when('/realms/:realm/identity-provider-mappers/:alias/mappers', {
            templateUrl : function(params){ return resourceUrl + '/partials/identity-provider-mappers.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                identityProvider : function(IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                mapperTypes : function(IdentityProviderMapperTypesLoader) {
                    return IdentityProviderMapperTypesLoader();
                },
                mappers : function(IdentityProviderMappersLoader) {
                    return IdentityProviderMappersLoader();
                }
            },
            controller : 'IdentityProviderMapperListCtrl'
        })
        .when('/realms/:realm/identity-provider-mappers/:alias/mappers/:mapperId', {
            templateUrl : function(params){ return resourceUrl + '/partials/identity-provider-mapper-detail.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                identityProvider : function(IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                mapperTypes : function(IdentityProviderMapperTypesLoader) {
                    return IdentityProviderMapperTypesLoader();
                },
                mapper : function(IdentityProviderMapperLoader) {
                    return IdentityProviderMapperLoader();
                }
            },
            controller : 'IdentityProviderMapperCtrl'
        })
        .when('/create/identity-provider-mappers/:realm/:alias', {
            templateUrl : function(params){ return resourceUrl + '/partials/identity-provider-mapper-detail.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                identityProvider : function(IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                mapperTypes : function(IdentityProviderMapperTypesLoader) {
                    return IdentityProviderMapperTypesLoader();
                }
            },
            controller : 'IdentityProviderMapperCreateCtrl'
        })

        .when('/realms/:realm/default-roles', {
            templateUrl : resourceUrl + '/partials/realm-default-roles.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                }
            },
            controller : 'RealmDefaultRolesCtrl'
        })
        .when('/realms/:realm/smtp-settings', {
            templateUrl : resourceUrl + '/partials/realm-smtp.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmSMTPSettingsCtrl'
        })
        .when('/realms/:realm/events', {
            templateUrl : resourceUrl + '/partials/realm-events.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmEventsCtrl'
        })
        .when('/realms/:realm/admin-events', {
            templateUrl : resourceUrl + '/partials/realm-events-admin.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmAdminEventsCtrl'
        })
        .when('/realms/:realm/events-settings', {
            templateUrl : resourceUrl + '/partials/realm-events-config.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig : function(RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                }
            },
            controller : 'RealmEventsConfigCtrl'
        })
        .when('/realms/:realm/partial-import', {
            templateUrl : resourceUrl + '/partials/partial-import.html',
            resolve : {
                resourceName : function() { return 'users'},
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmImportCtrl'
        })
        .when('/realms/:realm/partial-export', {
            templateUrl : resourceUrl + '/partials/partial-export.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmExportCtrl'
        })
        .when('/create/user/:realm', {
            templateUrl : resourceUrl + '/partials/user-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function() {
                    return {};
                }
            },
            controller : 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user', {
            templateUrl : resourceUrl + '/partials/user-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user/user-attributes', {
            templateUrl : resourceUrl + '/partials/user-attributes.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user/user-credentials', {
            templateUrl : resourceUrl + '/partials/user-credentials.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'UserCredentialsCtrl'
        })
        .when('/realms/:realm/users/:user/role-mappings', {
            templateUrl : resourceUrl + '/partials/role-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function() {
                    return {};
                }
            },
            controller : 'UserRoleMappingCtrl'
        })
        .when('/realms/:realm/users/:user/groups', {
            templateUrl : resourceUrl + '/partials/user-group-membership.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'UserGroupMembershipCtrl'
        })
        .when('/realms/:realm/users/:user/sessions', {
            templateUrl : resourceUrl + '/partials/user-sessions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                sessions : function(UserSessionsLoader) {
                    return UserSessionsLoader();
                }
            },
            controller : 'UserSessionsCtrl'
        })
        .when('/realms/:realm/users/:user/federated-identity', {
            templateUrl : resourceUrl + '/partials/user-federated-identity-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                federatedIdentities : function(UserFederatedIdentityLoader) {
                    return UserFederatedIdentityLoader();
                }
            },
            controller : 'UserFederatedIdentityCtrl'
        })
        .when('/create/federated-identity/:realm/:user', {
            templateUrl : resourceUrl + '/partials/user-federated-identity-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                federatedIdentities : function(UserFederatedIdentityLoader) {
                    return UserFederatedIdentityLoader();
                }
            },
            controller : 'UserFederatedIdentityAddCtrl'
        })
        .when('/realms/:realm/users/:user/consents', {
            templateUrl : resourceUrl + '/partials/user-consents.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                userConsents : function(UserConsentsLoader) {
                    return UserConsentsLoader();
                }
            },
            controller : 'UserConsentsCtrl'
        })
        .when('/realms/:realm/users/:user/offline-sessions/:client', {
            templateUrl : resourceUrl + '/partials/user-offline-sessions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                offlineSessions : function(UserOfflineSessionsLoader) {
                    return UserOfflineSessionsLoader();
                }
            },
            controller : 'UserOfflineSessionsCtrl'
        })
        .when('/realms/:realm/users', {
            templateUrl : resourceUrl + '/partials/user-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'UserListCtrl'
        })

        .when('/create/role/:realm', {
            templateUrl : resourceUrl + '/partials/role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                role : function() {
                    return {};
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role', {
            templateUrl : resourceUrl + '/partials/role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                role : function(RoleLoader) {
                    return RoleLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role/role-attributes', {
            templateUrl : resourceUrl + '/partials/role-attributes.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                role : function(RoleLoader) {
                    return RoleLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role/users', {
        	templateUrl : resourceUrl + '/partials/realm-role-users.html',
        	resolve : {
        		realm : function(RealmLoader) {
        			return RealmLoader();
        		},
        		role : function(RoleLoader) {
        			return RoleLoader();
        		}
        	},
        	controller : 'RoleMembersCtrl'
        })
        .when('/realms/:realm/roles', {
            templateUrl : resourceUrl + '/partials/role-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RoleListCtrl'
        })
        .when('/realms/:realm/groups', {
            templateUrl : resourceUrl + '/partials/group-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'GroupListCtrl'
        })
        .when('/create/group/:realm/parent/:parentId', {
            templateUrl : resourceUrl + '/partials/create-group.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                parentId : function($route) {
                    return $route.current.params.parentId;
                }
            },
            controller : 'GroupCreateCtrl'
        })
        .when('/realms/:realm/groups/:group', {
            templateUrl : resourceUrl + '/partials/group-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                group : function(GroupLoader) {
                    return GroupLoader();
                }
            },
            controller : 'GroupDetailCtrl'
        })
        .when('/realms/:realm/groups/:group/attributes', {
            templateUrl : resourceUrl + '/partials/group-attributes.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                group : function(GroupLoader) {
                    return GroupLoader();
                }
            },
            controller : 'GroupDetailCtrl'
        })
        .when('/realms/:realm/groups/:group/members', {
            templateUrl : resourceUrl + '/partials/group-members.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                group : function(GroupLoader) {
                    return GroupLoader();
                }
            },
            controller : 'GroupMembersCtrl'
        })
        .when('/realms/:realm/groups/:group/role-mappings', {
            templateUrl : resourceUrl + '/partials/group-role-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                group : function(GroupLoader) {
                    return GroupLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function() {
                    return {};
                }
            },
            controller : 'GroupRoleMappingCtrl'
        })
        .when('/realms/:realm/default-groups', {
            templateUrl : resourceUrl + '/partials/default-groups.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'DefaultGroupsCtrl'
        })


        .when('/create/role/:realm/clients/:client', {
            templateUrl : resourceUrl + '/partials/client-role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                role : function() {
                    return {};
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientRoleDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/roles/:role', {
            templateUrl : resourceUrl + '/partials/client-role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                role : function(ClientRoleLoader) {
                    return ClientRoleLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientRoleDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/roles/:role/role-attributes', {
            templateUrl : resourceUrl + '/partials/client-role-attributes.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                role : function(ClientRoleLoader) {
                    return ClientRoleLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientRoleDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/roles/:role/users', {
            templateUrl : resourceUrl + '/partials/client-role-users.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                role : function(ClientRoleLoader) {
                    return ClientRoleLoader();
                }
            },
            controller : 'ClientRoleMembersCtrl'
        })
        .when('/realms/:realm/clients/:client/mappers', {
            templateUrl : resourceUrl + '/partials/client-mappers.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientProtocolMapperListCtrl'
        })
        .when('/realms/:realm/clients/:client/add-mappers', {
            templateUrl : resourceUrl + '/partials/client-mappers-add.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'AddBuiltinProtocolMapperCtrl'
        })
        .when('/realms/:realm/clients/:client/mappers/:id', {
            templateUrl : resourceUrl + '/partials/client-protocol-mapper-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mapper : function(ClientProtocolMapperLoader) {
                    return ClientProtocolMapperLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }

            },
            controller : 'ClientProtocolMapperCtrl'
        })
        .when('/create/client/:realm/:client/mappers', {
            templateUrl : resourceUrl + '/partials/client-protocol-mapper-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientProtocolMapperCreateCtrl'
        })
        .when('/realms/:realm/clients/:client/client-scopes/setup-scopes', {
            templateUrl : resourceUrl + '/partials/client-scopes-setup.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                clientScopes : function(ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                clientDefaultClientScopes : function(ClientDefaultClientScopesLoader) {
                    return ClientDefaultClientScopesLoader();
                },
                clientOptionalClientScopes : function(ClientOptionalClientScopesLoader) {
                    return ClientOptionalClientScopesLoader();
                }
            },
            controller : 'ClientClientScopesSetupCtrl'
        })
        .when('/realms/:realm/clients/:client/client-scopes/evaluate-scopes', {
            templateUrl : resourceUrl + '/partials/client-scopes-evaluate.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                clientScopes : function(ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                clientDefaultClientScopes : function(ClientDefaultClientScopesLoader) {
                    return ClientDefaultClientScopesLoader();
                },
                clientOptionalClientScopes : function(ClientOptionalClientScopesLoader) {
                    return ClientOptionalClientScopesLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientClientScopesEvaluateCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/mappers', {
            templateUrl : resourceUrl + '/partials/client-scope-mappers.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScope : function(ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientScopeProtocolMapperListCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/add-mappers', {
            templateUrl : resourceUrl + '/partials/client-scope-mappers-add.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScope : function(ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientScopeAddBuiltinProtocolMapperCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/mappers/:id', {
            templateUrl : resourceUrl + '/partials/client-scope-protocol-mapper-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScope : function(ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mapper : function(ClientScopeProtocolMapperLoader) {
                    return ClientScopeProtocolMapperLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }

            },
            controller : 'ClientScopeProtocolMapperCtrl'
        })
        .when('/create/client-scope/:realm/:clientScope/mappers', {
            templateUrl : resourceUrl + '/partials/client-scope-protocol-mapper-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                clientScope : function(ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientScopeProtocolMapperCreateCtrl'
        })
        .when('/realms/:realm/clients/:client/sessions', {
            templateUrl : resourceUrl + '/partials/client-sessions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                sessionCount : function(ClientSessionCountLoader) {
                    return ClientSessionCountLoader();
                }
            },
            controller : 'ClientSessionsCtrl'
        })
        .when('/realms/:realm/clients/:client/offline-access', {
            templateUrl : resourceUrl + '/partials/client-offline-sessions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                offlineSessionCount : function(ClientOfflineSessionCountLoader) {
                    return ClientOfflineSessionCountLoader();
                }
            },
            controller : 'ClientOfflineSessionsCtrl'
        })
        .when('/realms/:realm/clients/:client/credentials', {
            templateUrl : resourceUrl + '/partials/client-credentials.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                clientAuthenticatorProviders : function(ClientAuthenticatorProvidersLoader) {
                    return ClientAuthenticatorProvidersLoader();
                },
                clientConfigProperties: function(PerClientAuthenticationConfigDescriptionLoader) {
                    return PerClientAuthenticationConfigDescriptionLoader();
                }
            },
            controller : 'ClientCredentialsCtrl'
        })
        .when('/realms/:realm/clients/:client/oidc/:keyType/import/:attribute', {
            templateUrl : resourceUrl + '/partials/client-oidc-key-import.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                callingContext : function() {
                    return "oidc";
                }
            },
            controller : 'ClientCertificateImportCtrl'
        })
        .when('/realms/:realm/clients/:client/oidc/:keyType/export/:attribute', {
            templateUrl : resourceUrl + '/partials/client-oidc-key-export.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                callingContext : function() {
                    return "oidc";
                }
            },
            controller : 'ClientCertificateExportCtrl'
        })
        .when('/realms/:realm/clients/:client/identity-provider', {
            templateUrl : resourceUrl + '/partials/client-identity-provider.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientIdentityProviderCtrl'
        })
        .when('/realms/:realm/clients/:client/clustering', {
            templateUrl : resourceUrl + '/partials/client-clustering.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientClusteringCtrl'
        })
        .when('/register-node/realms/:realm/clients/:client/clustering', {
            templateUrl : resourceUrl + '/partials/client-clustering-node.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientClusteringNodeCtrl'
        })
        .when('/realms/:realm/clients/:client/clustering/:node', {
            templateUrl : resourceUrl + '/partials/client-clustering-node.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientClusteringNodeCtrl'
        })
        .when('/realms/:realm/clients/:client/saml/keys', {
            templateUrl : resourceUrl + '/partials/client-saml-keys.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientSamlKeyCtrl'
        })
        .when('/realms/:realm/clients/:client/saml/:keyType/import/:attribute', {
            templateUrl : resourceUrl + '/partials/client-saml-key-import.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                callingContext : function() {
                    return "saml";
                }
            },
            controller : 'ClientCertificateImportCtrl'
        })
        .when('/realms/:realm/clients/:client/saml/:keyType/export/:attribute', {
            templateUrl : resourceUrl + '/partials/client-saml-key-export.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                callingContext : function() {
                    return "saml";
                }
            },
            controller : 'ClientCertificateExportCtrl'
        })
        .when('/realms/:realm/clients/:client/oidc/keys', {
            templateUrl : resourceUrl + '/partials/client-oidc-keys.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientOidcKeyCtrl'
        })
        .when('/realms/:realm/clients/:client/roles', {
            templateUrl : resourceUrl + '/partials/client-role-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientRoleListCtrl'
        })
        .when('/realms/:realm/clients/:client/revocation', {
            templateUrl : resourceUrl + '/partials/client-revocation.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientRevocationCtrl'
        })
        .when('/realms/:realm/clients/:client/scope-mappings', {
            templateUrl : resourceUrl + '/partials/client-scope-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientScopeMappingCtrl'
        })
        .when('/realms/:realm/clients/:client/installation', {
            templateUrl : resourceUrl + '/partials/client-installation.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientInstallationCtrl'
        })
        .when('/realms/:realm/clients/:client/service-account-roles', {
            templateUrl : resourceUrl + '/partials/client-service-account-roles.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(ClientServiceAccountUserLoader) {
                    return ClientServiceAccountUserLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'UserRoleMappingCtrl'
        })
        .when('/create/client/:realm', {
            templateUrl : resourceUrl + '/partials/create-client.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function() {
                    return {};
                },
                flows : function(AuthenticationFlowsLoader) {
                     return AuthenticationFlowsLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'CreateClientCtrl'
        })
        .when('/realms/:realm/clients/:client', {
            templateUrl : resourceUrl + '/partials/client-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                flows : function(AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientDetailCtrl'
        })
        .when('/create/client-scope/:realm', {
            templateUrl : resourceUrl + '/partials/client-scope-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScope : function() {
                    return {};
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientScopeDetailCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope', {
            templateUrl : resourceUrl + '/partials/client-scope-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScope : function(ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientScopeDetailCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/scope-mappings', {
            templateUrl : resourceUrl + '/partials/client-scope-scope-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScope : function(ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientScopeScopeMappingCtrl'
        })
        .when('/realms/:realm/clients', {
            templateUrl : resourceUrl + '/partials/client-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller : 'ClientListCtrl'
        })
        .when('/realms/:realm/client-scopes', {
            templateUrl : resourceUrl + '/partials/client-scope-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScopes : function(ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller : 'ClientScopeListCtrl'
        })
        .when('/realms/:realm/default-client-scopes', {
            templateUrl : resourceUrl + '/partials/client-scopes-realm-default.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                clientScopes : function(ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                realmDefaultClientScopes : function(RealmDefaultClientScopesLoader) {
                    return RealmDefaultClientScopesLoader();
                },
                realmOptionalClientScopes : function(RealmOptionalClientScopesLoader) {
                    return RealmOptionalClientScopesLoader();
                }
            },
            controller : 'ClientScopesRealmDefaultCtrl'
        })
        .when('/import/client/:realm', {
            templateUrl : resourceUrl + '/partials/client-import.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientImportCtrl'
        })
       .when('/realms/:realm/client-stores', {
            templateUrl : resourceUrl + '/partials/client-storage-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientStoresCtrl'
        })
        .when('/realms/:realm/client-storage/providers/:provider/:componentId', {
            templateUrl : resourceUrl + '/partials/client-storage-generic.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function(ComponentLoader) {
                    return ComponentLoader();
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericClientStorageCtrl'
        })
        .when('/create/client-storage/:realm/providers/:provider', {
             templateUrl : resourceUrl + '/partials/client-storage-generic.html',
             resolve : {
                 realm : function(RealmLoader) {
                     return RealmLoader();
                 },
                 instance : function() {
                     return {

                     };
                 },
                 providerId : function($route) {
                     return $route.current.params.provider;
                 },
                 serverInfo : function(ServerInfoLoader) {
                     return ServerInfoLoader();
                 }
             },
             controller : 'GenericClientStorageCtrl'
         })
       .when('/', {
            templateUrl : resourceUrl + '/partials/home.html',
            controller : 'HomeCtrl'
        })
        .when('/mocks/:realm', {
            templateUrl : resourceUrl + '/partials/realm-detail_mock.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmDetailCtrl'
        })
        .when('/realms/:realm/sessions/revocation', {
            templateUrl : resourceUrl + '/partials/session-revocation.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmRevocationCtrl'
        })
         .when('/realms/:realm/sessions/realm', {
            templateUrl : resourceUrl + '/partials/session-realm.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                stats : function(RealmClientSessionStatsLoader) {
                    return RealmClientSessionStatsLoader();
                }
            },
            controller : 'RealmSessionStatsCtrl'
        })
        .when('/create/user-storage/:realm/providers/ldap', {
            templateUrl : resourceUrl + '/partials/user-storage-ldap.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function() {
                    return {

                    };
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'LDAPUserStorageCtrl'
        })
        .when('/create/user-storage/:realm/providers/kerberos', {
            templateUrl : resourceUrl + '/partials/user-storage-kerberos.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function() {
                    return {

                    };
                },
                providerId : function($route) {
                    return "kerberos";
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericUserStorageCtrl'
        })
        .when('/create/user-storage/:realm/providers/:provider', {
            templateUrl : resourceUrl + '/partials/user-storage-generic.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function() {
                    return {

                    };
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericUserStorageCtrl'
        })
        .when('/realms/:realm/user-storage/providers/ldap/:componentId', {
            templateUrl : resourceUrl + '/partials/user-storage-ldap.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function(ComponentLoader) {
                    return ComponentLoader();
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'LDAPUserStorageCtrl'
        })
        .when('/realms/:realm/user-storage/providers/kerberos/:componentId', {
            templateUrl : resourceUrl + '/partials/user-storage-kerberos.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function(ComponentLoader) {
                    return ComponentLoader();
                },
                providerId : function($route) {
                    return "kerberos";
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericUserStorageCtrl'
        })
        .when('/realms/:realm/user-storage/providers/:provider/:componentId', {
            templateUrl : resourceUrl + '/partials/user-storage-generic.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                instance : function(ComponentLoader) {
                    return ComponentLoader();
                },
                providerId : function($route) {
                    return $route.current.params.provider;
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'GenericUserStorageCtrl'
        })
        .when('/realms/:realm/ldap-mappers/:componentId', {
            templateUrl : function(params){ return resourceUrl + '/partials/user-storage-ldap-mappers.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                provider : function(ComponentLoader) {
                    return ComponentLoader();
                },
                mappers : function(ComponentsLoader, $route) {
                    return ComponentsLoader.loadComponents($route.current.params.componentId, 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper');
                }
            },
            controller : 'LDAPMapperListCtrl'
        })
        .when('/create/ldap-mappers/:realm/:componentId', {
            templateUrl : function(params){ return resourceUrl + '/partials/user-storage-ldap-mapper-detail.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                provider : function(ComponentLoader) {
                    return ComponentLoader();
                },
                mapperTypes : function(SubComponentTypesLoader, $route) {
                    return SubComponentTypesLoader.loadComponents($route.current.params.componentId, 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper');
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'LDAPMapperCreateCtrl'
        })
        .when('/realms/:realm/ldap-mappers/:componentId/mappers/:mapperId', {
            templateUrl : function(params){ return resourceUrl + '/partials/user-storage-ldap-mapper-detail.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                provider : function(ComponentLoader) {
                    return ComponentLoader();
                },
                mapperTypes : function(SubComponentTypesLoader, $route) {
                    return SubComponentTypesLoader.loadComponents($route.current.params.componentId, 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper');
                },
                mapper : function(LDAPMapperLoader) {
                    return LDAPMapperLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'LDAPMapperCtrl'
        })
        .when('/realms/:realm/user-federation', {
            templateUrl : resourceUrl + '/partials/user-federation.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'UserFederationCtrl'
        })
        .when('/realms/:realm/defense/headers', {
            templateUrl : resourceUrl + '/partials/defense-headers.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller : 'DefenseHeadersCtrl'
        })
        .when('/realms/:realm/defense/brute-force', {
            templateUrl : resourceUrl + '/partials/brute-force.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmBruteForceCtrl'
        })
        .when('/realms/:realm/protocols', {
            templateUrl : resourceUrl + '/partials/protocol-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller : 'ProtocolListCtrl'
        })
        .when('/realms/:realm/authentication/flows', {
            templateUrl : resourceUrl + '/partials/authentication-flows.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                flows : function(AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                selectedFlow : function() {
                    return null;
                }
            },
            controller : 'AuthenticationFlowsCtrl'
        })
        .when('/realms/:realm/authentication/flow-bindings', {
            templateUrl : resourceUrl + '/partials/authentication-flow-bindings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                flows : function(AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmFlowBindingCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow', {
            templateUrl : resourceUrl + '/partials/authentication-flows.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                flows : function(AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                selectedFlow : function($route) {
                    return $route.current.params.flow;
                }
            },
            controller : 'AuthenticationFlowsCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow/create/execution/:topFlow', {
            templateUrl : resourceUrl + '/partials/create-execution.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                topFlow: function($route) {
                    return $route.current.params.topFlow;
                },
                parentFlow : function(AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                formActionProviders : function(AuthenticationFormActionProvidersLoader) {
                    return AuthenticationFormActionProvidersLoader();
                },
                authenticatorProviders : function(AuthenticatorProvidersLoader) {
                    return AuthenticatorProvidersLoader();
                },
                clientAuthenticatorProviders : function(ClientAuthenticatorProvidersLoader) {
                    return ClientAuthenticatorProvidersLoader();
                }
            },
            controller : 'CreateExecutionCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow/create/flow/execution/:topFlow', {
            templateUrl : resourceUrl + '/partials/create-flow-execution.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                topFlow: function($route) {
                    return $route.current.params.topFlow;
                },
                parentFlow : function(AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                formProviders : function(AuthenticationFormProvidersLoader) {
                    return AuthenticationFormProvidersLoader();
                }
            },
            controller : 'CreateExecutionFlowCtrl'
        })
        .when('/realms/:realm/authentication/create/flow', {
            templateUrl : resourceUrl + '/partials/create-flow.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'CreateFlowCtrl'
        })
        .when('/realms/:realm/authentication/required-actions', {
            templateUrl : resourceUrl + '/partials/required-actions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                unregisteredRequiredActions : function(UnregisteredRequiredActionsListLoader) {
                    return UnregisteredRequiredActionsListLoader();
                }
            },
            controller : 'RequiredActionsCtrl'
        })
        .when('/realms/:realm/authentication/password-policy', {
            templateUrl : resourceUrl + '/partials/password-policy.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmPasswordPolicyCtrl'
        })
        .when('/realms/:realm/authentication/otp-policy', {
            templateUrl : resourceUrl + '/partials/otp-policy.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmOtpPolicyCtrl'
        })
        .when('/realms/:realm/authentication/webauthn-policy', {
            templateUrl : resourceUrl + '/partials/webauthn-policy.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmWebAuthnPolicyCtrl'
        })
        .when('/realms/:realm/authentication/webauthn-policy-passwordless', {
            templateUrl : resourceUrl + '/partials/webauthn-policy-passwordless.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmWebAuthnPasswordlessPolicyCtrl'
        })
        .when('/realms/:realm/authentication/ciba-policy', {
            templateUrl : resourceUrl + '/partials/ciba-policy.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmCibaPolicyCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow/config/:provider/:config', {
            templateUrl : resourceUrl + '/partials/authenticator-config.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                flow : function(AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                configType : function(AuthenticationConfigDescriptionLoader) {
                    return AuthenticationConfigDescriptionLoader();
                },
                config : function(AuthenticationConfigLoader) {
                    return AuthenticationConfigLoader();
                }
            },
            controller : 'AuthenticationConfigCtrl'
        })
        .when('/create/authentication/:realm/flows/:flow/execution/:executionId/provider/:provider', {
            templateUrl : resourceUrl + '/partials/authenticator-config.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                flow : function(AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                configType : function(AuthenticationConfigDescriptionLoader) {
                    return AuthenticationConfigDescriptionLoader();
                },
                execution : function(ExecutionIdLoader) {
                    return ExecutionIdLoader();
                }
            },
            controller : 'AuthenticationConfigCreateCtrl'
        })
        .when('/create/localization/:realm/:locale', {
            templateUrl : resourceUrl + '/partials/realm-localization-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                locale: function($route) {
                    return $route.current.params.locale;
                },
                key: function() {
                    return null
                },
                localizationText : function() {
                    return null;
                }
            },
            controller : 'RealmLocalizationDetailCtrl'
        })
        .when('/realms/:realm/localization/:locale/:key', {
            templateUrl : resourceUrl + '/partials/realm-localization-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                locale: function($route) {
                    return $route.current.params.locale;
                },
                key: function($route) {
                    return $route.current.params.key;
                },
                localizationText : function(RealmSpecificlocalizationTextLoader) {
                    return RealmSpecificlocalizationTextLoader();
                }
            },
            controller : 'RealmLocalizationDetailCtrl'
        })
        .when('/server-info', {
            templateUrl : resourceUrl + '/partials/server-info.html',
            resolve : {
            	serverInfo : function(ServerInfoLoader) {
                return ServerInfoLoader();
            	}
            },
            controller : 'ServerInfoCtrl'
        })
        .when('/server-info/providers', {
            templateUrl : resourceUrl + '/partials/server-info-providers.html',
            resolve : {
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ServerInfoCtrl'
        })
        .when('/logout', {
            templateUrl : resourceUrl + '/partials/home.html',
            controller : 'LogoutCtrl'
        })
        .when('/notfound', {
            templateUrl : resourceUrl + '/partials/notfound.html'
        })
        .when('/forbidden', {
            templateUrl : resourceUrl + '/partials/forbidden.html'
        })
        .otherwise({
            templateUrl : resourceUrl + '/partials/pagenotfound.html'
        });
} ]);

module.config(function($httpProvider) {
    $httpProvider.interceptors.push('errorInterceptor');

    var spinnerFunction = function(data, headersGetter) {
        if (resourceRequests == 0) {
            loadingTimer = window.setTimeout(function() {
                $('#loading').show();
                loadingTimer = -1;
            }, 500);
        }
        resourceRequests++;
        return data;
    };
    $httpProvider.defaults.transformRequest.push(spinnerFunction);

    $httpProvider.interceptors.push('spinnerInterceptor');
    $httpProvider.interceptors.push('authInterceptor');

});

module.factory('spinnerInterceptor', function($q, $window, $rootScope, $location) {
    return {
        response: function(response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if(loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }
            return response;
        },
        responseError: function(response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if(loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }

            return $q.reject(response);
        }
    };
});

module.factory('errorInterceptor', function($q, $window, $rootScope, $location, Notifications, Auth) {
    return {
        response: function(response) {
            return response;
        },
        responseError: function(response) {
            if (response.status == 401) {
                Auth.authz.logout();
            } else if (response.status == 403) {
                $location.path('/forbidden');
            } else if (response.status == 404) {
                $location.path('/notfound');
            } else if (response.status) {
                if (response.data && response.data.errorMessage) {
                    Notifications.error(response.data.errorMessage);
                } else if (response.data && response.data.errors) {
                    var messages = "Multiple errors found: ";

                    for (var i = 0; i < response.data.errors.length; i++) {
                        messages+=response.data.errors[i].errorMessage + " ";
                    }

                    Notifications.error(messages);
                } else if (response.data && response.data.error_description) {
                    Notifications.error(response.data.error_description);
                } else {
                    Notifications.error("An unexpected server error has occurred");
                }
            } else {
                Notifications.error("No response from server.");
            }
            return $q.reject(response);
        }
    };
});

// collapsable form fieldsets
module.directive('collapsable', function() {
    return function(scope, element, attrs) {
        element.click(function() {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('kc-icon-collapse').toggleClass('kc-icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

// collapsable form fieldsets
module.directive('uncollapsed', function() {
    return function(scope, element, attrs) {
        element.prepend('<i class="toggle-class fa fa-angle-down"></i> ');
        element.click(function() {
            $(this).find('.toggle-class').toggleClass('fa-angle-down').toggleClass('fa-angle-right');
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

// collapsable form fieldsets
module.directive('collapsed', function() {
    return function(scope, element, attrs) {
        element.prepend('<i class="toggle-class fa fa-angle-right"></i> ');
        element.parent().find('.form-group').toggleClass('hidden');
        element.click(function() {
            $(this).find('.toggle-class').toggleClass('fa-angle-down').toggleClass('fa-angle-right');
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

/**
 * Directive for presenting an ON-OFF switch for checkbox.
 * Usage: <input ng-model="mmm" name="nnn" id="iii" onoffswitch [on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitch', function() {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '@',
            id: '@',
            ngModel: '=',
            ngDisabled: '=',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        // TODO - The same code acts differently when put into the templateURL. Find why and move the code there.
        //templateUrl: "templates/kc-switch.html",
        template: "<span><div class='onoffswitch' tabindex='0'><input type='checkbox' ng-model='ngModel' ng-disabled='ngDisabled' class='onoffswitch-checkbox' name='{{name}}' id='{{id}}'><label for='{{id}}' class='onoffswitch-label'><span class='onoffswitch-inner'><span class='onoffswitch-active'>{{kcOnText}}</span><span class='onoffswitch-inactive'>{{kcOffText}}</span></span><span class='onoffswitch-switch'></span></label></div></span>",
        compile: function(element, attrs) {
            /*
            We don't want to propagate basic attributes to the root element of directive. Id should be passed to the
            input element only to achieve proper label binding (and validity).
            */
            element.removeAttr('name');
            element.removeAttr('id');

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown', function(e){
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    }
});

/**
 * Directive for presenting an ON-OFF switch for checkbox. The directive expects the value to be string 'true' or 'false', not boolean true/false
 * This directive provides some additional capabilities to the default onoffswitch such as:
 *
 * - Dynamic values for id and name attributes. Useful if you need to use this directive inside a ng-repeat
 * - Specific scope to specify the value. Instead of just true or false.
 *
 * Usage: <input ng-model="mmm" name="nnn" id="iii" kc-onoffswitch-model [on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitchstring', function() {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '=',
            id: '=',
            value: '=',
            ngModel: '=',
            ngDisabled: '=',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        // TODO - The same code acts differently when put into the templateURL. Find why and move the code there.
        //templateUrl: "templates/kc-switch.html",
        template: '<span><div class="onoffswitch" tabindex="0"><input type="checkbox" ng-true-value="\'true\'" ng-false-value="\'false\'" ng-model="ngModel" ng-disabled="ngDisabled" class="onoffswitch-checkbox" name="kc{{name}}" id="kc{{id}}"><label for="kc{{id}}" class="onoffswitch-label"><span class="onoffswitch-inner"><span class="onoffswitch-active">{{kcOnText}}</span><span class="onoffswitch-inactive">{{kcOffText}}</span></span><span class="onoffswitch-switch"></span></label></div></span>',
        compile: function(element, attrs) {

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown click', function(e){
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    }
});

/**
 * Directive for presenting an ON-OFF switch for checkbox. The directive expects the true-value or false-value to be string like 'true' or 'false', not boolean true/false.
 * This directive provides some additional capabilities to the default onoffswitch such as:
 *
 * - Specific scope to specify the value. Instead of just 'true' or 'false' you can use any other values. For example: true-value="'foo'" false-value="'bar'" .
 * But 'true'/'false' are defaults if true-value and false-value are not specified
 *
 * Usage: <input ng-model="mmm" name="nnn" id="iii" onoffswitchvalue [ true-value="'true'" false-value="'false'" on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitchvalue', function() {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '@',
            id: '@',
            trueValue: '@',
            falseValue: '@',
            ngModel: '=',
            ngDisabled: '=',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        // TODO - The same code acts differently when put into the templateURL. Find why and move the code there.
        //templateUrl: "templates/kc-switch.html",
        template: "<span><div class='onoffswitch' tabindex='0'><input type='checkbox' ng-true-value='{{trueValue}}' ng-false-value='{{falseValue}}' ng-model='ngModel' ng-disabled='ngDisabled' class='onoffswitch-checkbox' name='{{name}}' id='{{id}}'><label for='{{id}}' class='onoffswitch-label'><span class='onoffswitch-inner'><span class='onoffswitch-active'>{{kcOnText}}</span><span class='onoffswitch-inactive'>{{kcOffText}}</span></span><span class='onoffswitch-switch'></span></label></div></span>",
        compile: function(element, attrs) {
            /*
             We don't want to propagate basic attributes to the root element of directive. Id should be passed to the
             input element only to achieve proper label binding (and validity).
             */
            element.removeAttr('name');
            element.removeAttr('id');

            if (!attrs.trueValue) { attrs.trueValue = "'true'"; }
            if (!attrs.falseValue) { attrs.falseValue = "'false'"; }

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown', function(e){
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    }
});

module.directive('kcInput', function() {
    var d = {
        scope : true,
        replace : false,
        link : function(scope, element, attrs) {
            var form = element.children('form');
            var label = element.children('label');
            var input = element.children('input');

            var id = form.attr('name') + '.' + input.attr('name');

            element.attr('class', 'control-group');

            label.attr('class', 'control-label');
            label.attr('for', id);

            input.wrap('<div class="controls"/>');
            input.attr('id', id);

            if (!input.attr('placeHolder')) {
                input.attr('placeHolder', label.text());
            }

            if (input.attr('required')) {
                label.append(' <span class="required">*</span>');
            }
        }
    };
    return d;
});

module.directive('kcEnter', function() {
    return function(scope, element, attrs) {
        element.bind("keydown keypress", function(event) {
            if (event.which === 13) {
                scope.$apply(function() {
                    scope.$eval(attrs.kcEnter);
                });

                event.preventDefault();
            }
        });
    };
});

// Don't allow URI reserved characters
module.directive('kcNoReservedChars', function (Notifications, $translate) {
    return function($scope, element) {
        element.bind("keypress", function(event) {
            var keyPressed = String.fromCharCode(event.which || event.keyCode || 0);
            
            // ] and ' can not be used inside a character set on POSIX and GNU
            if (keyPressed.match('[:/?#[@!$&()*+,;=]') || keyPressed === ']' || keyPressed === '\'') {
                event.preventDefault();
                $scope.$apply(function() {
                    Notifications.warn($translate.instant('key-not-allowed-here', {character: keyPressed}));
                });
            }
        });
    };
});

module.directive('kcSave', function ($compile, $timeout, Notifications) {
    var clickDelay = 500; // 500 ms

    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-primary");
            elem.attr("type","submit");

            var disabled = false;
            elem.on('click', function(evt) {
                if ($scope.hasOwnProperty("changed") && !$scope.changed) return;

                // KEYCLOAK-4121: Prevent double form submission
                if (disabled) {
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    return;
                } else {
                    disabled = true;
                    $timeout(function () { disabled = false; }, clickDelay, false);
                }

                $scope.$apply(function() {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        var ngValid = form.find('.ng-valid');
                        if ($scope[form.attr('name')].$valid) {
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');
                            $scope['save']();
                        } else {
                            Notifications.error("Missing or invalid field(s). Please verify the fields in red.")
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');

                            var ngInvalid = form.find('.ng-invalid');
                            //ngInvalid.addClass('error');
                            ngInvalid.parent().addClass('has-error');
                        }
                    }
                });
            })
        }
    }
});

module.directive('kcReset', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-default");
            elem.attr("type","submit");
            elem.bind('click', function() {
                $scope.$apply(function() {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        form.find('.ng-valid').removeClass('error');
                        form.find('.ng-invalid').removeClass('error');
                        $scope['reset']();
                    }
                })
            })
        }
    }
});

module.directive('kcCancel', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-default");
            elem.attr("type","submit");
        }
    }
});

module.directive('kcDelete', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-danger");
            elem.attr("type","submit");
        }
    }
});


module.directive('kcDropdown', function ($compile, Notifications) {
    return {
        scope: {
            kcOptions: '=',
            kcModel: '=',
            id: "=",
            kcPlaceholder: '@'
        },
        restrict: 'EA',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-select.html',
        link: function(scope, element, attr) {
            scope.updateModel = function(item) {
                scope.kcModel = item;
            };
        }
    }
});

module.directive('kcReadOnly', function() {
    var disabled = {};

    var d = {
        replace : false,
        link : function(scope, element, attrs) {
            var disable = function(i, e) {
                if (!e.disabled) {
                    disabled[e.tagName + i] = true;
                    e.disabled = true;
                }
            }

            var enable = function(i, e) {
                if (disabled[e.tagName + i]) {
                    e.disabled = false;
                    delete disabled[i];
                }
            }

            var filterIgnored = function(i, e){
                return !e.attributes['kc-read-only-ignore'];
            }

            scope.$watch(attrs.kcReadOnly, function(readOnly) {
                if (readOnly) {
                    element.find('input').filter(filterIgnored).each(disable);
                    element.find('button').filter(filterIgnored).each(disable);
                    element.find('select').filter(filterIgnored).each(disable);
                    element.find('textarea').filter(filterIgnored).each(disable);
                } else {
                    element.find('input').filter(filterIgnored).each(enable);
                    element.find('input').filter(filterIgnored).each(enable);
                    element.find('button').filter(filterIgnored).each(enable);
                    element.find('select').filter(filterIgnored).each(enable);
                    element.find('textarea').filter(filterIgnored).each(enable);
                }
            });
        }
    };
    return d;
});

module.directive('kcMenu', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-menu.html'
    }
});

module.directive('kcTabsRealm', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-realm.html'
    }
});

module.directive('kcTabsAuthentication', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-authentication.html'
    }
});

module.directive('kcTabsRole', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-role.html'
    }
});

module.directive('kcTabsClientRole', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client-role.html'
    }
});

module.directive('kcTabsUser', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-user.html'
    }
});

module.directive('kcTabsUsers', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-users.html'
    }
});

module.directive('kcTabsClients', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-clients.html'
    }
});

module.directive('kcTabsGroup', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-group.html'
    }
});

module.directive('kcTabsGroupList', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-group-list.html'
    }
});

module.directive('kcTabsClient', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client.html'
    }
});

module.directive('kcTabsClientScope', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client-scope.html'
    }
});

module.directive('kcNavigationUser', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-navigation-user.html'
    }
});

module.directive('kcTabsIdentityProvider', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-identity-provider.html'
    }
});

module.directive('kcTabsUserFederation', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-user-federation.html'
    }
});

module.directive('kcTabsLdap', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-ldap.html'
    }
});

module.controller('RoleSelectorModalCtrl', function($scope, realm, config, configName, RealmRoles, Client, ClientRole, $modalInstance) {
    $scope.selectedRealmRole = {
        role: undefined
    };
    $scope.selectedClientRole = {
        role: undefined
    };
    $scope.client = {
        selected: undefined
    };

    $scope.selectRealmRole = function() {
        config[configName] = $scope.selectedRealmRole.role.name;
        $modalInstance.close();
    }

    $scope.selectClientRole = function() {
        config[configName] = $scope.selectedClient.clientId + "." + $scope.selectedClientRole.role.name;
        $modalInstance.close();
    }

    $scope.cancel = function() {
        $modalInstance.dismiss();
    }

    clientSelectControl($scope, realm.realm, Client);
    
    $scope.selectedClient = null;

    $scope.changeClient = function(client) {
        $scope.selectedClient = client;
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }
        if ($scope.selectedClient) {
            ClientRole.query({realm: realm.realm, client: $scope.selectedClient.id}, function (data) {
                $scope.clientRoles = data;
             });
        } else {
            console.log('selected client was null');
            $scope.clientRoles = null;
        }

        $scope.selectedClient = client;
    }

    RealmRoles.query({realm: realm.realm}, function(data) {
        $scope.realmRoles = data;
    })
});

module.controller('GroupSelectorModalCtrl', function($scope, $q, realm, config, configName, GroupsCount, Groups, Group, GroupChildren, Notifications, Dialog, ComponentUtils, $modalInstance, $translate) {
    $scope.realm = realm;
    $scope.groupList = [
        {
            "id" : "realm",
            "name": $translate.instant('groups'),
            "subGroups" : []
        }
    ];
    $scope.groupSelector = {
        searchCriteria: undefined,
        currentPage: 1,
        pageSize: 20,
        numberOfPages: 1
    };
    $scope.groupSelector.currentPageInput = $scope.groupSelector.currentPage;

    var refreshGroups = function (search) {
        console.log('refreshGroups');
        $scope.groupSelector.currentPageInput = $scope.groupSelector.currentPage;

        var first = ($scope.groupSelector.currentPage * $scope.groupSelector.pageSize) - $scope.groupSelector.pageSize;
        console.log('first:' + first);
        var queryParams = {
            realm : realm.realm,
            first : first,
            max : $scope.groupSelector.pageSize
        };
        var countParams = {
            realm : realm.realm,
            top : 'true'
        };

        if(angular.isDefined(search) && search !== '') {
            queryParams.search = search;
            countParams.search = search;
        }

        var promiseGetGroups = $q.defer();
        Groups.query(queryParams, function(entry) {
            promiseGetGroups.resolve(entry);
        }, function() {
            promiseGetGroups.reject($translate.instant('group.fetch.fail', {params: queryParams}));
        });
        promiseGetGroups.promise.then(function(groups) {
            $scope.groupList = [
                {
                    "id" : "realm",
                    "name": $translate.instant('groups'),
                    "subGroups": ComponentUtils.sortGroups('name', groups)
                }
            ];
            if (angular.isDefined(search) && search !== '') {
                // Add highlight for concrete text match
                setTimeout(function () {
                    document.querySelectorAll('span').forEach(function (element) {
                        if (element.textContent.indexOf(search) != -1) {
                            angular.element(element).addClass('highlight');
                        }
                    });
                }, 500);
            }
        }, function (failed) {
            Notifications.error(failed);
        });

        var promiseCount = $q.defer();
        console.log('countParams: realm[' + countParams.realm);
        GroupsCount.query(countParams, function(entry) {
            promiseCount.resolve(entry);
        }, function() {
            promiseCount.reject($translate.instant('group.fetch.fail', {params: countParams}));
        });
        promiseCount.promise.then(function(entry) {
            if(angular.isDefined(entry.count) && entry.count > $scope.groupSelector.pageSize) {
                $scope.groupSelector.numberOfPages = Math.ceil(entry.count/$scope.groupSelector.pageSize);
            } else {
                $scope.groupSelector.numberOfPages = 1;
            }
        }, function (failed) {
            Notifications.error(failed);
        });
    };

    refreshGroups();

    $scope.$watch('groupSelector.currentPage', function(newValue, oldValue) {
        if(parseInt(newValue, 10) !== oldValue) {
            refreshGroups($scope.groupSelector.searchCriteria);
        }
    });

    $scope.clearSearch = function() {
        $scope.groupSelector.searchCriteria = '';
        if (parseInt($scope.groupSelector.currentPage, 10) === 1) {
            refreshGroups();
        } else {
            $scope.groupSelector.currentPage = 1;
        }
    };

    $scope.searchGroup = function() {
        if (parseInt($scope.groupSelector.currentPage, 10) === 1) {
            refreshGroups($scope.groupSelector.searchCriteria);
        } else {
            $scope.groupSelector.currentPage = 1;
        }
    };

    $scope.selectGroup = function(selected) {
        if(!selected || selected.id === "realm") return;

        config[configName] = selected.path;
        $modalInstance.close();
    }

    $scope.edit = $scope.selectGroup;

    $scope.cancel = function() {
        $modalInstance.dismiss();
    }

    var isLeaf = function(node) {
        return node.id !== "realm" && (!node.subGroups || node.subGroups.length === 0);
    };

    $scope.getGroupClass = function(node) {
        if (node.id === "realm") {
            return 'pficon pficon-users';
        }
        if (isLeaf(node)) {
            return 'normal';
        }
        if (node.subGroups.length && node.collapsed) return 'collapsed';
        if (node.subGroups.length && !node.collapsed) return 'expanded';
        return 'collapsed';

    };

    $scope.getSelectedClass = function(node) {
        if (node.selected) {
            return 'selected';
        }
        return undefined;
    }
});


module.controller('ProviderConfigCtrl', function ($modal, $scope, $route, ComponentUtils, Client) {
    clientSelectControl($scope, $route.current.params.realm, Client);
    $scope.fileNames = {};
    $scope.newMapEntries = {};
    var cachedMaps = {};
    var cachedParsedMaps = {};
    var focusMapValueId = null;

    // KEYCLOAK-4463
    $scope.initEditor = function(editor){
        editor.$blockScrolling = Infinity; // suppress warning message
    };

    $scope.initSelectedClient = function(configName, config) {
        if(config[configName]) {
            $scope.selectedClient = null;
            Client.query({realm: $route.current.params.realm, search: false, clientId: config[configName], max: 1}, function(data) {
                if(data.length > 0) {
                    $scope.selectedClient = angular.copy(data[0]);
                    $scope.selectedClient.text = $scope.selectedClient.clientId;
                }
            });
        }   
    }

    $scope.openRoleSelector = function (configName, config) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/role-selector.html',
            controller: 'RoleSelectorModalCtrl',
            resolve: {
                realm: function () {
                    return $scope.realm;
                },
                config: function () {
                    return config;
                },
                configName: function () {
                    return configName;
                }
            }
        })
    }

    $scope.openGroupSelector = function (configName, config) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/group-selector.html',
            controller: 'GroupSelectorModalCtrl',
            resolve: {
                realm: function () {
                    return $scope.realm;
                },
                config: function () {
                    return config;
                },
                configName: function () {
                    return configName;
                }
            }
        })
    }

    $scope.changeClient = function(configName, config, client, multivalued) {
        if (!client || !client.id) {
            config[configName] = null;
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        if (multivalued) {
            config[configName][0] = client.clientId;
        } else {
            config[configName] = client.clientId;
        }
    };

    ComponentUtils.convertAllMultivaluedStringValuesToList($scope.properties, $scope.config);

    ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.properties, $scope.config);

    $scope.addValueToMultivalued = function(optionName) {
        var configProperty = $scope.config[optionName];
        var lastIndex = configProperty.length - 1;
        var lastValue = configProperty[lastIndex];
        console.log("Option=" + optionName + ", lastIndex=" + lastIndex + ", lastValue=" + lastValue);

        if (lastValue.length > 0) {
            configProperty.push('');
        }
    }

    $scope.deleteValueFromMultivalued = function(optionName, index) {
        $scope.config[optionName].splice(index, 1);
    }

    $scope.uploadFile = function($files, optionName, config) {
        var reader = new FileReader();
        reader.onload = function(e) {
            $scope.$apply(function() {
                config[optionName][0] = e.target.result;
            });
        };
        reader.readAsText($files[0]);
        $scope.fileNames[optionName] = $files[0].name;
    }

    $scope.addMapEntry = function(optionName) {
        $scope.removeMapEntry(optionName, $scope.newMapEntries[optionName].key)

        var parsedMap = JSON.parse($scope.config[optionName]);
        parsedMap.push($scope.newMapEntries[optionName]);
        $scope.config[optionName] = JSON.stringify(parsedMap);

        delete $scope.newMapEntries[optionName];
    }

    $scope.removeMapEntry = function(optionName, key) {
        var parsedMap = JSON.parse($scope.config[optionName]);

        for(var i = parsedMap.length - 1; i >= 0; i--) {
            if(parsedMap[i]['key'] === key) {
                parsedMap.splice(i, 1);
            }
        }

        $scope.config[optionName] = JSON.stringify(parsedMap);
    }

    $scope.updateMapEntry = function(optionName, key, value) {
        var parsedMap = JSON.parse($scope.config[optionName]);

        for(var i = parsedMap.length - 1; i >= 0; i--) {
            if(parsedMap[i]['key'] === key) {
                parsedMap[i]['value'] = value;
            }
        }
        $scope.config[optionName] = JSON.stringify(parsedMap);

        focusMapValueId = "mapValue-" + optionName + "-" + key;
    }

    $scope.jsonParseMap = function(optionName) {

        if(cachedParsedMaps[optionName] === undefined) {
            cachedMaps[optionName] = "[]";
            cachedParsedMaps[optionName] = [];

            if(!$scope.config.hasOwnProperty(optionName)){
                $scope.config[optionName]=cachedMaps[optionName];
            } else {
                cachedMaps[optionName] = $scope.config[optionName];
                cachedParsedMaps[optionName] = JSON.parse(cachedMaps[optionName]);
            }
        }

        var mapChanged = $scope.config[optionName] !== cachedMaps[optionName];

        if(mapChanged){
            cachedMaps[optionName] = $scope.config[optionName];
            cachedParsedMaps[optionName] = JSON.parse(cachedMaps[optionName]);
        }

        if(!mapChanged && focusMapValueId !== null){
            document.getElementById(focusMapValueId).focus();
            focusMapValueId = null;
        }

        return cachedParsedMaps[optionName];
    }
});

module.directive('kcProviderConfig', function ($modal) {
    return {
        scope: {
            config: '=',
            properties: '=',
            realm: '=',
            clients: '=',
            configName: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'ProviderConfigCtrl',
        templateUrl: resourceUrl + '/templates/kc-provider-config.html'
    }
});

module.controller('ComponentRoleSelectorModalCtrl', function($scope, realm, config, configName, RealmRoles, Client, ClientRole, $modalInstance) {
    $scope.selectedRealmRole = {
        role: undefined
    };
    $scope.selectedClientRole = {
        role: undefined
    };
    $scope.client = {
        selected: undefined
    };

    $scope.selectRealmRole = function() {
        config[configName][0] = $scope.selectedRealmRole.role.name;
        $modalInstance.close();
    }

    $scope.selectClientRole = function() {
        config[configName][0] = $scope.client.selected.clientId + "." + $scope.selectedClientRole.role.name;
        $modalInstance.close();
    }

    $scope.cancel = function() {
        $modalInstance.dismiss();
    }

    $scope.changeClient = function() {
        if ($scope.client.selected) {
            ClientRole.query({realm: realm.realm, client: $scope.client.selected.id}, function (data) {
                $scope.clientRoles = data;
            });
        } else {
            console.log('selected client was null');
            $scope.clientRoles = null;
        }

    }
    RealmRoles.query({realm: realm.realm}, function(data) {
        $scope.realmRoles = data;
    })
    Client.query({realm: realm.realm}, function(data) {
        $scope.clients = data;
        if (data.length > 0) {
            $scope.client.selected = data[0];
            $scope.changeClient();
        }
    })
});

module.controller('ComponentConfigCtrl', function ($modal, $scope, $route, Client) {

    $scope.initSelectedClient = function(configName, config) {
        if(config[configName]) {
            $scope.selectedClient = null;
            Client.query({realm: $route.current.params.realm, search: false, clientId: config[configName], max: 1}, function(data) {
                if(data.length > 0) {
                    $scope.selectedClient = angular.copy(data[0]);
                    $scope.selectedClient.text = $scope.selectedClient.clientId;
                }
            });
        }   
    }

    $scope.changeClient = function(configName, config, client) {
        if (!client || !client.id) {
            config[configName] = null;
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        config[configName] = client.clientId;
    };


    $scope.openRoleSelector = function (configName, config) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/component-role-selector.html',
            controller: 'ComponentRoleSelectorModalCtrl',
            resolve: {
                realm: function () {
                    return $scope.realm;
                },
                config: function () {
                    return config;
                },
                configName: function () {
                    return configName;
                }
            }
        })
    }
});
module.directive('kcComponentConfig', function ($modal) {
    return {
        scope: {
            config: '=',
            properties: '=',
            realm: '=',
            clients: '=',
            configName: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'ComponentConfigCtrl',
        templateUrl: resourceUrl + '/templates/kc-component-config.html'
    }
});

/*
*  Used to select the element (invoke $(elem).select()) on specified action list.
*  Usages kc-select-action="click mouseover"
*  When used in the textarea element, this will select/highlight the textarea content on specified action (i.e. click).
*/
module.directive('kcSelectAction', function ($compile, Notifications) {
    return {
        restrict: 'A',
        compile: function (elem, attrs) {

            var events = attrs.kcSelectAction.split(" ");

            for(var i=0; i < events.length; i++){

                elem.bind(events[i], function(){
                    elem.select();
                });
            }
        }
    }
});

module.filter('remove', function() {
    return function(input, remove, attribute) {
        if (!input || !remove) {
            return input;
        }

        var out = [];
        for ( var i = 0; i < input.length; i++) {
            var e = input[i];

            if (Array.isArray(remove)) {
                for (var j = 0; j < remove.length; j++) {
                    if (attribute) {
                        if (remove[j][attribute] == e[attribute]) {
                            e = null;
                            break;
                        }
                    } else {
                        if (remove[j] == e) {
                            e = null;
                            break;
                        }
                    }
                }
            } else {
                if (attribute) {
                    if (remove[attribute] == e[attribute]) {
                        e = null;
                    }
                } else {
                    if (remove == e) {
                        e = null;
                    }
                }
            }

            if (e != null) {
                out.push(e);
            }
        }

        return out;
    };
});

module.filter('capitalize', function() {
    return function(input) {
        if (!input) {
            return;
        }
        var splittedWords = input.split(/\s+/);
        for (var i=0; i<splittedWords.length ; i++) {
            splittedWords[i] = splittedWords[i].charAt(0).toUpperCase() + splittedWords[i].slice(1);
        };
        return splittedWords.join(" ");
    };
});

/*
 * Guarantees a deterministic property iteration order.
 * See: http://www.2ality.com/2015/10/property-traversal-order-es6.html
 */
module.filter('toOrderedMapSortedByKey', function(){
   return function(input){

       if(!input){
           return input;
       }

       var keys = Object.keys(input);

       if(keys.length <= 1){
           return input;
       }

       keys.sort();

       var result = {};
       for (var i = 0; i < keys.length; i++) {
           result[keys[i]] = input[keys[i]];
       }

       return result;
   };
});

module.directive('kcSidebarResize', function ($window) {
    return function (scope, element) {
        function resize() {
            var navBar = angular.element(document.getElementsByClassName('navbar-pf')).height();
            var container = angular.element(document.getElementById("view").getElementsByTagName("div")[0]).height();
            var height = Math.max(container, window.innerHeight - navBar - 3);

            element[0].style['min-height'] = height + 'px';
        }

        resize();

        var w = angular.element($window);
        scope.$watch(function () {
            return {
                'h': window.innerHeight,
                'w': window.innerWidth
            };
        }, function () {
            resize();
        }, true);
        w.bind('resize', function () {
            scope.$apply();
        });
    }
});



module.directive('kcTooltip', function($compile) {
        return {
            restrict: 'E',
            replace: false,
            terminal: true,
            priority: 1000,
            link: function link(scope,element, attrs) {
                var angularElement = angular.element(element[0]);
                var tooltip = angularElement.text();
                angularElement.text('');
                element.addClass('hidden');

                var label = angular.element(element.parent().children()[0]);
                label.append(' <i class="fa fa-question-circle text-muted" tooltip="' + tooltip + '" tooltip-placement="right" tooltip-trigger="mouseover mouseout"></i>');

                $compile(label)(scope);
            }
        };
});

module.directive( 'kcOpen', function ( $location ) {
    return function ( scope, element, attrs ) {
        var path;

        attrs.$observe( 'kcOpen', function (val) {
            path = val;
        });

        element.bind( 'click', function () {
            scope.$apply( function () {
                $location.path(path);
            });
        });
    };
});

module.directive('kcOnReadFile', function ($parse) {
    console.debug('kcOnReadFile');
    return {
        restrict: 'A',
        scope: false,
        link: function(scope, element, attrs) {
            var fn = $parse(attrs.kcOnReadFile);

            element.on('change', function(onChangeEvent) {
                var reader = new FileReader();

                reader.onload = function(onLoadEvent) {
                    scope.$apply(function() {
                        fn(scope, {$fileContent:onLoadEvent.target.result});
                    });
                };

                reader.readAsText((onChangeEvent.srcElement || onChangeEvent.target).files[0]);
            });
        }
    };
});

module.controller('PagingCtrl', function ($scope) {
    $scope.currentPageInput = 1;

    $scope.firstPage = function() {
        if (!$scope.hasPrevious()) return;
        $scope.currentPage = 1;
        $scope.currentPageInput = 1;
    };

    $scope.lastPage = function() {
        if (!$scope.hasNext()) return;
        $scope.currentPage = $scope.numberOfPages;
        $scope.currentPageInput = $scope.numberOfPages;
    };

    $scope.previousPage = function() {
        if (!$scope.hasPrevious()) return;
        $scope.currentPage--;
        $scope.currentPageInput = $scope.currentPage;
    };

    $scope.nextPage = function() {
        if (!$scope.hasNext()) return;
        $scope.currentPage++;
        $scope.currentPageInput = $scope.currentPage;
    };

    $scope.hasNext = function() {
        return $scope.currentPage < $scope.numberOfPages;
    };

    $scope.hasPrevious = function() {
        return $scope.currentPage > 1;
    };
});

// Provides a component for injection with utility methods for manipulating strings
module.factory('KcStrings', function () {
    var instance = {};
    
    // some IE versions do not support string.endsWith method, this method should be used as an alternative for cross-browser compatibility
    instance.endsWith = function(source, suffix) {
        return source.indexOf(suffix, source.length - suffix.length) !== -1;
    };
    
    return instance;
});

module.directive('kcPaging', function () {
    return {
        scope: {
            currentPage: '=',
            currentPageInput: '=',
            numberOfPages: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'PagingCtrl',
        templateUrl: resourceUrl + '/templates/kc-paging.html'
    }
});

// Tests the page number input from currentPageInput to see
// if it represents a valid page.  If so, the current page is changed.
module.directive('kcValidPage', function() {
   return {
       require: 'ngModel',
       link: function(scope, element, attrs, ctrl) {
           ctrl.$validators.inRange = function(modelValue, viewValue) {
               if (viewValue >= 1 && viewValue <= scope.numberOfPages) {
                   scope.currentPage = viewValue;
               }

               return true;
           }
       }
   }
});

// Directive to parse/format strings into numbers
module.directive('stringToNumber', function() {
    return {
        require: 'ngModel',
        link: function(scope, element, attrs, ngModel) {
            ngModel.$parsers.push(function(value) {
                return (typeof value === 'undefined' || value === null)? '' : '' + value;
            });
            ngModel.$formatters.push(function(value) {
                return parseFloat(value);
            });
        }
    };
});

// filter used for paged tables
module.filter('startFrom', function () {
    return function (input, start) {
        if (input) {
            start = +start;
            return input.slice(start);
        }
        return [];
    };
});


module.directive('kcPassword', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            function toggleMask(evt) {
                if(elem.hasClass('password-conceal')) {
                    view();
                } else {
                    conceal();
                }
            }

            function view() {
                elem.removeClass('password-conceal');

                var t = elem.next().children().first();
                t.addClass('fa-eye-slash');
                t.removeClass('fa-eye');
            }

            function conceal() {
                elem.addClass('password-conceal');

                var t = elem.next().children().first();
                t.removeClass('fa-eye-slash');
                t.addClass('fa-eye');
            }

            elem.addClass("password-conceal");
            elem.attr("type","text");
            elem.attr("autocomplete", "off");

            var p = elem.parent();

            var inputGroup = $('<div class="input-group"></div>');
            var eye = $('<span class="input-group-addon btn btn-default"><span class="fa fa-eye"></span></span>')
                        .on('click', toggleMask);

            $scope.$watch(attr.ngModel, function(v) {
                if (v && v == '**********') {
                    elem.next().addClass('disabled')
                } else if (v && v.indexOf('${v') == 0) {
                    elem.next().addClass('disabled')
                    view();
                } else {
                    elem.next().removeClass('disabled')
                }
            })

            elem.detach().appendTo(inputGroup);
            inputGroup.append(eye);
            p.append(inputGroup);
        }
    }
});


module.filter('resolveClientRootUrl', function() {
    return function(input) {
        if (!input) {
            return;
        }
        return input.replace("${authBaseUrl}", authServerUrl).replace("${authAdminUrl}", authUrl);
    };
});
