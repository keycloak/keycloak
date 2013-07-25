'use strict';

var module = angular.module('keycloak.services', [ 'ngResource' ]);

module.factory('Notifications', function($rootScope, $timeout) {
	var notifications = {};

	var scheduled = null;
	var schedulePop = function() {
		if (scheduled) {
			$timeout.cancel(scheduled);
		}

		scheduled = $timeout(function() {
			$rootScope.notification = null;
			scheduled = null;
		}, 3000);
	};

	if (!$rootScope.notifications) {
		$rootScope.notifications = [];
	}

	notifications.success = function(message) {
		$rootScope.notification = {
			type : "success",
			message : message
		};

		schedulePop();
	};

	return notifications;
});

module.factory('Application', function($resource) {
	return $resource('/keycloak-server/ui/api/applications/:id', {
		id : '@id'
	}, {
		update : {
			method : 'PUT'
		}
	});
});

module.factory('ApplicationListLoader', function(Application, $q) {
	return function() {
		var delay = $q.defer();
		Application.query(function(applications) {
			delay.resolve(applications);
		}, function() {
			delay.reject('Unable to fetch applications');
		});
		return delay.promise;
	};
});

module.factory('ApplicationLoader', function(Application, $route, $q) {
	return function() {
		var id = $route.current.params.application;
		var delay = $q.defer();
		Application.get({
			id : id
		}, function(application) {
			delay.resolve(application);
		}, function() {
			delay.reject('Unable to fetch application ' + id);
		});
		return delay.promise;
	};
});

module.factory('Provider', function($resource) {
	return $resource('/ejs-identity/api/admin/providers');
});

module.factory('ProviderListLoader', function(Provider, $q) {
	return function() {
		var delay = $q.defer();
		Provider.query(function(providers) {
			delay.resolve(providers);
		}, function() {
			delay.reject('Unable to fetch providers');
		});
		return delay.promise;
	};
});

module.factory('Realm', function($resource) {
	return $resource('/keycloak-server/ui/api/realms/:id', {
		id : '@id'
	}, {
		update : {
			method : 'PUT'
		}
	});
});

module.factory('RealmListLoader', function(Realm, $q) {
	return function() {
		var delay = $q.defer();
		Realm.query(function(realms) {
			delay.resolve(realms);
		}, function() {
			delay.reject('Unable to fetch realms');
		});
		return delay.promise;
	};
});

module.factory('RealmLoader', function(Realm, $route, $q) {
	return function() {
		var id = $route.current.params.realm;
		var delay = $q.defer();
		Realm.get({
			id : id
		}, function(realm) {
			delay.resolve(realm);
		}, function() {
			delay.reject('Unable to fetch realm ' + name);
		});
		return delay.promise;
	};
});

module.factory('User', function($resource) {
	return $resource('/ejs-identity/api/im/:realm/users/:id', {
		realm : '@realm',
		id : '@id'
	}, {
		save : {
			method : 'PUT'
		}
	});
});

module.factory('UserListLoader', function(User, $route, $q) {
	return function() {
		var delay = $q.defer();
		User.query({
			realmKey : $route.current.params.realmKey
		}, function(users) {
			delay.resolve(users);
		}, function() {
			delay.reject('Unable to fetch users');
		});
		return delay.promise;
	};
});

module.factory('UserLoader', function(User, $route, $q) {
	return function() {
		var name = $route.current.params.user;
		if (name == 'new') {
			return {};
		} else {
			var delay = $q.defer();
			User.get({
				realm : $route.current.params.realm,
				name : name
			}, function(user) {
				delay.resolve(user);
			}, function() {
				delay.reject('Unable to fetch user ' + name);
			});
			return delay.promise;
		}
	};
});

module.service('Auth', function($resource, $http, $location, $routeParams) {
	var auth = {
		loggedIn : true
	};
	auth.user = {
		userId : 'test',
		displayName : 'Test User'
	};
	return auth;
});