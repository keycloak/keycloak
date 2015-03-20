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

module.factory('RealmApplicationSessionStatsLoader', function(Loader, RealmApplicationSessionStats, $route, $q) {
    return Loader.query(RealmApplicationSessionStats, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ApplicationProtocolMapperLoader', function(Loader, ApplicationProtocolMapper, $route, $q) {
    return Loader.get(ApplicationProtocolMapper, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application,
            id: $route.current.params.id
        }
    });
});

module.factory('OAuthClientProtocolMapperLoader', function(Loader, OAuthClientProtocolMapper, $route, $q) {
    return Loader.get(OAuthClientProtocolMapper, function() {
        return {
            realm : $route.current.params.realm,
            oauth : $route.current.params.oauth,
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

module.factory('ApplicationRoleLoader', function(Loader, RoleById, $route, $q) {
    return Loader.get(RoleById, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application,
            role : $route.current.params.role
        }
    });
});

module.factory('ApplicationSessionStatsLoader', function(Loader, ApplicationSessionStats, $route, $q) {
    return Loader.get(ApplicationSessionStats, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application
        }
    });
});

module.factory('ApplicationSessionCountLoader', function(Loader, ApplicationSessionCount, $route, $q) {
    return Loader.get(ApplicationSessionCount, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application
        }
    });
});

module.factory('ApplicationClaimsLoader', function(Loader, ApplicationClaims, $route, $q) {
    return Loader.get(ApplicationClaims, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application
        }
    });
});

module.factory('ApplicationInstallationLoader', function(Loader, ApplicationInstallation, $route, $q) {
    return Loader.get(ApplicationInstallation, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application
        }
    });
});

module.factory('ApplicationRoleListLoader', function(Loader, ApplicationRole, $route, $q) {
    return Loader.query(ApplicationRole, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application
        }
    });
});



module.factory('ApplicationLoader', function(Loader, Application, $route, $q) {
    return Loader.get(Application, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application
        }
    });
});

module.factory('ApplicationListLoader', function(Loader, Application, $route, $q) {
    return Loader.query(Application, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});


module.factory('RoleMappingLoader', function(Loader, RoleMapping, $route, $q) {
	var realm = $route.current.params.realm || $route.current.params.application;

	return Loader.query(RoleMapping, function() {
		return {
			realm : realm,
			role : $route.current.params.role
		}
	});
});

module.factory('OAuthClientLoader', function(Loader, OAuthClient, $route, $q) {
    return Loader.get(OAuthClient, function() {
        return {
            realm : $route.current.params.realm,
            oauth : $route.current.params.oauth
        }
    });
});

module.factory('OAuthClientClaimsLoader', function(Loader, OAuthClientClaims, $route, $q) {
    return Loader.get(OAuthClientClaims, function() {
        return {
            realm : $route.current.params.realm,
            oauth : $route.current.params.oauth
        }
    });
});


module.factory('OAuthClientListLoader', function(Loader, OAuthClient, $route, $q) {
    return Loader.query(OAuthClient, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('OAuthClientInstallationLoader', function(Loader, OAuthClientInstallation, $route, $q) {
    return Loader.get(OAuthClientInstallation, function() {
        return {
            realm : $route.current.params.realm,
            oauth : $route.current.params.oauth
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