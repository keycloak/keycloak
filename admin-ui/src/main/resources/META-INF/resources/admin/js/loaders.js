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

module.factory('RealmLoader', function(Loader, Realm, $route, $q) {
	return Loader.get(Realm, function() {
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

module.factory('UserLoader', function(Loader, User, $route, $q) {
	return Loader.get(User, function() {
		return {
			realm : $route.current.params.realm,
			userId : $route.current.params.user
		}
	});
});

module.factory('RoleLoader', function(Loader, Role, $route, $q) {
    return Loader.get(Role, function() {
        return {
            realm : $route.current.params.realm,
            roleId : $route.current.params.role
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

module.factory('ApplicationRoleLoader', function(Loader, ApplicationRole, $route, $q) {
    return Loader.get(ApplicationRole, function() {
        return {
            realm : $route.current.params.realm,
            application : $route.current.params.application,
            roleId : $route.current.params.role
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
            id : $route.current.params.application
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
            id : $route.current.params.oauth
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
