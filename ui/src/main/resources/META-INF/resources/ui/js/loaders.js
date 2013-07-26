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

module.factory('ApplicationListLoader', function(Loader, Application, $q) {
	return Loader.query(Application);
});

module.factory('ApplicationLoader', function(Loader, Application, $route, $q) {
	return Loader.get(Application, function() {
		return {
			id : $route.current.params.application
		}
	});
});

module.factory('RealmListLoader', function(Loader, Realm, $q) {
	return Loader.query(Realm);
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

module.factory('RoleMappingLoader', function(Loader, RoleMapping, $route, $q) {
	var realm = $route.current.params.realm || $route.current.params.application;

	return Loader.query(RoleMapping, function() {
		return {
			realm : realm,
			role : $route.current.params.role
		}
	});
});