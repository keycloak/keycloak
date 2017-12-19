'use strict';

var auth = {};
var resourceBundle;
var locale = 'en';

var module = angular.module('keycloak', [ 'keycloak.services', 'keycloak.loaders', 'ui.bootstrap', 'ui.select2', 'angularFileUpload', 'angularTreeview', 'pascalprecht.translate', 'ngCookies', 'ngSanitize', 'ui.ace']);
var resourceRequests = 0;
var loadingTimer = -1;

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

    function hasAnyAccess(user) {
        return user && user['realm_access'];
    }

    keycloakAuth.onAuthLogout = function() {
        location.reload();
    }

    keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
        auth.authz = keycloakAuth;

        if (auth.authz.idTokenParsed.locale) {
            locale = auth.authz.idTokenParsed.locale;
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

        loadResourceBundle(function(data) {
            resourceBundle = data;

            auth.refreshPermissions(function () {
                module.factory('Auth', function () {
                    return auth;
                });
                var injector = angular.bootstrap(document, ["keycloak"]);

                injector.get('$translate')('consoleTitle').then(function (consoleTitle) {
                    document.title = consoleTitle;
                });
            });
        });
    }).error(function () {
        window.location.reload();
    });
});

module.factory('authInterceptor', function($q, Auth) {
    return {
        request: function (config) {
            if (!config.url.match(/.html$/)) {
                var deferred = $q.defer();
                if (Auth.authz.token) {
                    Auth.authz.updateToken(5).success(function () {
                        config.headers = config.headers || {};
                        config.headers.Authorization = 'Bearer ' + Auth.authz.token;

                        deferred.resolve(config);
                    }).error(function () {
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
        .when('/realms/:realm/login-settings', {
            templateUrl : resourceUrl + '/partials/realm-login-settings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfo) {
                    return ServerInfo.delay;
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
        .when('/realms/:realm/keys/list', {
            templateUrl : resourceUrl + '/partials/realm-keys-list.html',
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
                clients : function(ClientListLoader) {
                    return ClientListLoader();
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
                },
                groups : function(GroupListLoader) {
                    return GroupListLoader();
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
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
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
                },
                groups : function(GroupListLoader) {
                    return GroupListLoader();
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
        .when('/realms/:realm/clients/:client/mappers', {
            templateUrl : resourceUrl + '/partials/client-mappers.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
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
        .when('/realms/:realm/client-templates/:template/mappers', {
            templateUrl : resourceUrl + '/partials/client-template-mappers.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                template : function(ClientTemplateLoader) {
                    return ClientTemplateLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientTemplateProtocolMapperListCtrl'
        })
        .when('/realms/:realm/client-templates/:template/add-mappers', {
            templateUrl : resourceUrl + '/partials/client-template-mappers-add.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                template : function(ClientTemplateLoader) {
                    return ClientTemplateLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientTemplateAddBuiltinProtocolMapperCtrl'
        })
        .when('/realms/:realm/client-templates/:template/mappers/:id', {
            templateUrl : resourceUrl + '/partials/client-template-protocol-mapper-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                template : function(ClientTemplateLoader) {
                    return ClientTemplateLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mapper : function(ClientTemplateProtocolMapperLoader) {
                    return ClientTemplateProtocolMapperLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }

            },
            controller : 'ClientTemplateProtocolMapperCtrl'
        })
        .when('/create/client-template/:realm/:template/mappers', {
            templateUrl : resourceUrl + '/partials/client-template-protocol-mapper-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                template : function(ClientTemplateLoader) {
                    return ClientTemplateLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientTemplateProtocolMapperCreateCtrl'
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
        .when('/realms/:realm/clients/:client/credentials/client-jwt/:keyType/import/:attribute', {
            templateUrl : resourceUrl + '/partials/client-credentials-jwt-key-import.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                callingContext : function() {
                    return "jwt-credentials";
                }
            },
            controller : 'ClientCertificateImportCtrl'
        })
        .when('/realms/:realm/clients/:client/credentials/client-jwt/:keyType/export/:attribute', {
            templateUrl : resourceUrl + '/partials/client-credentials-jwt-key-export.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                callingContext : function() {
                    return "jwt-credentials";
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
        .when('/realms/:realm/clients/:client/roles', {
            templateUrl : resourceUrl + '/partials/client-role-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                roles : function(ClientRoleListLoader) {
                    return ClientRoleListLoader();
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
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
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
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function() {
                    return {};
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
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientDetailCtrl'
        })
        .when('/create/client-template/:realm', {
            templateUrl : resourceUrl + '/partials/client-template-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
                },
                template : function() {
                    return {};
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientTemplateDetailCtrl'
        })
        .when('/realms/:realm/client-templates/:template', {
            templateUrl : resourceUrl + '/partials/client-template-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
                },
                template : function(ClientTemplateLoader) {
                    return ClientTemplateLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'ClientTemplateDetailCtrl'
        })
        .when('/realms/:realm/client-templates/:template/scope-mappings', {
            templateUrl : resourceUrl + '/partials/client-template-scope-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                template : function(ClientTemplateLoader) {
                    return ClientTemplateLoader();
                },
                clients : function(ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller : 'ClientTemplateScopeMappingCtrl'
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
        .when('/realms/:realm/client-templates', {
            templateUrl : resourceUrl + '/partials/client-template-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                templates : function(ClientTemplateListLoader) {
                    return ClientTemplateListLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller : 'ClientTemplateListCtrl'
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
                serverInfo : function(ServerInfo) {
                    return ServerInfo.delay;
                }
            },
            controller : 'RealmOtpPolicyCtrl'
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

module.directive('kcTabsClientTemplate', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client-template.html'
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
        config[configName] = $scope.client.selected.clientId + "." + $scope.selectedClientRole.role.name;
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

module.controller('ProviderConfigCtrl', function ($modal, $scope, ComponentUtils) {
    $scope.fileNames = {};

    // KEYCLOAK-4463
    $scope.initEditor = function(editor){
        editor.$blockScrolling = Infinity; // suppress warning message
    };

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

module.controller('ComponentConfigCtrl', function ($modal, $scope) {
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
            elem.addClass("password-conceal");
            elem.attr("type","text");
        }
    }
});