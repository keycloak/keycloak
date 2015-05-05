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
	}
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
	}
	return loader;
});

module.factory('RealmListLoader', function(Loader, Realm, $q) {
	return Loader.get(Realm);
});

module.factory('ServerInfoLoader', function(Loader, ServerInfo, $q) {
    return Loader.get(ServerInfo);
});

module.factory('RealmLoader', function(Loader, Realm, $route, $q) {
	return Loader.get(Realm, function() {
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

module.factory('UserLoader', function(Loader, User, $route, $q) {
    return Loader.get(User, function() {
        return {
            realm : $route.current.params.realm,
            userId : $route.current.params.user
        }
    });
});

module.factory('UserFederationInstanceLoader', function(Loader, UserFederationInstances, $route, $q) {
    return Loader.get(UserFederationInstances, function() {
        return {
            realm : $route.current.params.realm,
            instance: $route.current.params.instance
        }
    });
});

module.factory('UserFederationFactoryLoader', function(Loader, UserFederationProviders, $route, $q) {
    return Loader.get(UserFederationProviders, function() {
        return {
            realm : $route.current.params.realm,
            provider: $route.current.params.provider
        }
    });
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

module.factory('UserFederatedIdentityLoader', function(Loader, UserFederatedIdentity, $route, $q) {
    return Loader.query(UserFederatedIdentity, function() {
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

module.factory('ClientClaimsLoader', function(Loader, ClientClaims, $route, $q) {
    return Loader.get(ClientClaims, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientInstallationLoader', function(Loader, ClientInstallation, $route, $q) {
    return Loader.get(ClientInstallation, function() {
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

