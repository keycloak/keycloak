'use strict';

var module = angular.module('keycloak.loaders', [ 'keycloak.services', 'ngResource' ]);

module.factory('Loader', function($q) {
	var loader = {};
	loader.get = function(service, id) {
		return function() {
			var i = id && id();
			var delay = $q.defer();
			service.get(i, function(entry) {
				delay.resolve(entry);
			}, function() {
				delay.reject('Unable to fetch ' + i);
			});
			return delay.promise;
		};
	};
	loader.query = function(service, id) {
		return function() {
			var i = id && id();
			var delay = $q.defer();
			service.query(i, function(entry) {
				delay.resolve(entry);
			}, function() {
				delay.reject('Unable to fetch ' + i);
			});
			return delay.promise;
		};
	};
	return loader;
});

module.factory('RealmListLoader', function(Loader, Realm, $q) {
	return Loader.get(Realm);
});

module.factory('ServerInfoLoader', function(Loader, ServerInfo) {
    return function() {
        return ServerInfo.promise;
    };
});

module.factory('RealmLoader', function(Loader, Realm, $route, $q) {
	return Loader.get(Realm, function() {
		return {
			id : $route.current.params.realm
		}
	});
});

module.factory('RealmKeysLoader', function(Loader, RealmKeys, $route, $q) {
    return Loader.get(RealmKeys, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('RealmEventsConfigLoader', function(Loader, RealmEventsConfig, $route, $q) {
    return Loader.get(RealmEventsConfig, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('UserListLoader', function(Loader, User, $route, $q) {
    return Loader.query(User, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RequiredActionsListLoader', function(Loader, RequiredActions, $route, $q) {
    return Loader.query(RequiredActions, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('UnregisteredRequiredActionsListLoader', function(Loader, UnregisteredRequiredActions, $route, $q) {
    return Loader.query(UnregisteredRequiredActions, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RealmSessionStatsLoader', function(Loader, RealmSessionStats, $route, $q) {
    return Loader.get(RealmSessionStats, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RealmClientSessionStatsLoader', function(Loader, RealmClientSessionStats, $route, $q) {
    return Loader.query(RealmClientSessionStats, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientProtocolMapperLoader', function(Loader, ClientProtocolMapper, $route, $q) {
    return Loader.get(ClientProtocolMapper, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client,
            id: $route.current.params.id
        }
    });
});

module.factory('ClientTemplateProtocolMapperLoader', function(Loader, ClientTemplateProtocolMapper, $route, $q) {
    return Loader.get(ClientTemplateProtocolMapper, function() {
        return {
            realm : $route.current.params.realm,
            template : $route.current.params.template,
            id: $route.current.params.id
        }
    });
});

module.factory('UserLoader', function(Loader, User, $route, $q) {
    return Loader.get(User, function() {
        return {
            realm : $route.current.params.realm,
            userId : $route.current.params.user
        }
    });
});

module.factory('ComponentLoader', function(Loader, Components, $route, $q) {
    return Loader.get(Components, function() {
        return {
            realm : $route.current.params.realm,
            componentId: $route.current.params.componentId
        }
    });
});

module.factory('LDAPMapperLoader', function(Loader, Components, $route, $q) {
    return Loader.get(Components, function() {
        return {
            realm : $route.current.params.realm,
            componentId: $route.current.params.mapperId
        }
    });
});

module.factory('ComponentsLoader', function(Loader, Components, $route, $q) {
    var componentsLoader = {};

    componentsLoader.loadComponents = function(parent, componentType) {
        return Loader.query(Components, function() {
            return {
                realm : $route.current.params.realm,
                parent : parent,
                type: componentType
            }
        })();
    };

    return componentsLoader;
});

module.factory('SubComponentTypesLoader', function(Loader, SubComponentTypes, $route, $q) {
    var componentsLoader = {};

    componentsLoader.loadComponents = function(parent, componentType) {
        return Loader.query(SubComponentTypes, function() {
            return {
                realm : $route.current.params.realm,
                componentId : parent,
                type: componentType
            }
        })();
    };

    return componentsLoader;
});

module.factory('UserSessionStatsLoader', function(Loader, UserSessionStats, $route, $q) {
    return Loader.get(UserSessionStats, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});

module.factory('UserSessionsLoader', function(Loader, UserSessions, $route, $q) {
    return Loader.query(UserSessions, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});

module.factory('UserOfflineSessionsLoader', function(Loader, UserOfflineSessions, $route, $q) {
    return Loader.query(UserOfflineSessions, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user,
            client : $route.current.params.client
        }
    });
});

module.factory('UserFederatedIdentityLoader', function(Loader, UserFederatedIdentities, $route, $q) {
    return Loader.query(UserFederatedIdentities, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});

module.factory('UserConsentsLoader', function(Loader, UserConsents, $route, $q) {
    return Loader.query(UserConsents, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});



module.factory('RoleLoader', function(Loader, RoleById, $route, $q) {
    return Loader.get(RoleById, function() {
        return {
            realm : $route.current.params.realm,
            role : $route.current.params.role
        }
    });
});

module.factory('RoleListLoader', function(Loader, Role, $route, $q) {
    return Loader.query(Role, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientRoleLoader', function(Loader, RoleById, $route, $q) {
    return Loader.get(RoleById, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client,
            role : $route.current.params.role
        }
    });
});

module.factory('ClientSessionStatsLoader', function(Loader, ClientSessionStats, $route, $q) {
    return Loader.get(ClientSessionStats, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientSessionCountLoader', function(Loader, ClientSessionCount, $route, $q) {
    return Loader.get(ClientSessionCount, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientOfflineSessionCountLoader', function(Loader, ClientOfflineSessionCount, $route, $q) {
    return Loader.get(ClientOfflineSessionCount, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientClaimsLoader', function(Loader, ClientClaims, $route, $q) {
    return Loader.get(ClientClaims, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientRoleListLoader', function(Loader, ClientRole, $route, $q) {
    return Loader.query(ClientRole, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});



module.factory('ClientLoader', function(Loader, Client, $route, $q) {
    return Loader.get(Client, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientListLoader', function(Loader, Client, $route, $q) {
    return Loader.query(Client, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientTemplateLoader', function(Loader, ClientTemplate, $route, $q) {
    return Loader.get(ClientTemplate, function() {
        return {
            realm : $route.current.params.realm,
            template : $route.current.params.template
        }
    });
});

module.factory('ClientTemplateListLoader', function(Loader, ClientTemplate, $route, $q) {
    return Loader.query(ClientTemplate, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientServiceAccountUserLoader', function(Loader, ClientServiceAccountUser, $route, $q) {
    return Loader.get(ClientServiceAccountUser, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});


module.factory('RoleMappingLoader', function(Loader, RoleMapping, $route, $q) {
	var realm = $route.current.params.realm || $route.current.params.client;

	return Loader.query(RoleMapping, function() {
		return {
			realm : realm,
			role : $route.current.params.role
		}
	});
});

module.factory('IdentityProviderLoader', function(Loader, IdentityProvider, $route, $q) {
    return Loader.get(IdentityProvider, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias
        }
    });
});

module.factory('IdentityProviderFactoryLoader', function(Loader, IdentityProviderFactory, $route, $q) {
    return Loader.get(IdentityProviderFactory, function () {
        return {
            realm: $route.current.params.realm,
            provider_id: $route.current.params.provider_id
        }
    });
});

module.factory('IdentityProviderMapperTypesLoader', function(Loader, IdentityProviderMapperTypes, $route, $q) {
    return Loader.get(IdentityProviderMapperTypes, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias
        }
    });
});

module.factory('IdentityProviderMappersLoader', function(Loader, IdentityProviderMappers, $route, $q) {
    return Loader.query(IdentityProviderMappers, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias
        }
    });
});

module.factory('IdentityProviderMapperLoader', function(Loader, IdentityProviderMapper, $route, $q) {
    return Loader.get(IdentityProviderMapper, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias,
            mapperId: $route.current.params.mapperId
        }
    });
});

module.factory('AuthenticationFlowsLoader', function(Loader, AuthenticationFlows, $route, $q) {
    return Loader.query(AuthenticationFlows, function() {
        return {
            realm : $route.current.params.realm,
            flow: ''
        }
    });
});

module.factory('AuthenticationFormProvidersLoader', function(Loader, AuthenticationFormProviders, $route, $q) {
    return Loader.query(AuthenticationFormProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationFormActionProvidersLoader', function(Loader, AuthenticationFormActionProviders, $route, $q) {
    return Loader.query(AuthenticationFormActionProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticatorProvidersLoader', function(Loader, AuthenticatorProviders, $route, $q) {
    return Loader.query(AuthenticatorProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientAuthenticatorProvidersLoader', function(Loader, ClientAuthenticatorProviders, $route, $q) {
    return Loader.query(ClientAuthenticatorProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationFlowLoader', function(Loader, AuthenticationFlows, $route, $q) {
    return Loader.get(AuthenticationFlows, function() {
        return {
            realm : $route.current.params.realm,
            flow: $route.current.params.flow
        }
    });
});

module.factory('AuthenticationConfigDescriptionLoader', function(Loader, AuthenticationConfigDescription, $route, $q) {
    return Loader.get(AuthenticationConfigDescription, function () {
        return {
            realm: $route.current.params.realm,
            provider: $route.current.params.provider
        }
    });
});

module.factory('PerClientAuthenticationConfigDescriptionLoader', function(Loader, PerClientAuthenticationConfigDescription, $route, $q) {
    return Loader.get(PerClientAuthenticationConfigDescription, function () {
        return {
            realm: $route.current.params.realm
        }
    });
});

module.factory('ExecutionIdLoader', function($route) {
    return function() { return $route.current.params.executionId; };
});

module.factory('AuthenticationConfigLoader', function(Loader, AuthenticationConfig, $route, $q) {
    return Loader.get(AuthenticationConfig, function () {
        return {
            realm: $route.current.params.realm,
            config: $route.current.params.config
        }
    });
});

module.factory('GroupListLoader', function(Loader, Groups, $route, $q) {
    return Loader.query(Groups, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('GroupCountLoader', function(Loader, GroupsCount, $route, $q) {
    return Loader.query(GroupsCount, function() {
        return {
            realm : $route.current.params.realm,
            top : true
        }
    });
});

module.factory('GroupLoader', function(Loader, Group, $route, $q) {
    return Loader.get(Group, function() {
        return {
            realm : $route.current.params.realm,
            groupId : $route.current.params.group
        }
    });
});

module.factory('ClientInitialAccessLoader', function(Loader, ClientInitialAccess, $route) {
    return Loader.query(ClientInitialAccess, function() {
        return {
            realm: $route.current.params.realm
        }
    });
});

module.factory('ClientRegistrationPolicyProvidersLoader', function(Loader, ClientRegistrationPolicyProviders, $route) {
    return Loader.query(ClientRegistrationPolicyProviders, function() {
        return {
            realm: $route.current.params.realm
        }
    });
});






