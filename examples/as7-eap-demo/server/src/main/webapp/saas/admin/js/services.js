'use strict';

var module = angular.module('keycloak.services', [ 'ngResource' ]);

module.service('Auth', function($resource, $http, $location, $routeParams) {
	var auth = {
		loggedIn : keycloakCookieLoggedIn
	};
	auth.user = {
		userId : null,
		displayName : null
	};
    auth.logout = function() {
        auth.user = {
            userId : null,
            displayName : null
        };
        auth.loggedIn = false;
        $http.get('/auth-server/rest/saas/logout-cookie');
    };
    if (!auth.loggedIn) {
        return auth;
    }
    $http.get('/auth-server/rest/saas/whoami').success(function(data, status) {
        auth.user = data;
        //alert(data.userId);
    })
        .error(function(data, status) {
            alert("Failed!");
        });
	return auth;
});

module.service('Dialog', function($dialog) {
	var dialog = {};
	dialog.confirmDelete = function(name, type, success) {
		var title = 'Delete ' + name;
		var msg = 'Are you sure you want to permanently delete this ' + type + '?';
		var btns = [ {
			result : 'cancel',
			label : 'Cancel'
		}, {
			result : 'ok',
			label : 'Delete this ' + type,
			cssClass : 'btn-primary'
		} ];

		$dialog.messageBox(title, msg, btns).open().then(function(result) {
			if (result == "ok") {
				success();
			}
		});
	}
	return dialog
});

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

	notifications.message = function(type, message) {
		$rootScope.notification = {
			type : type,
			message : message
		};

		schedulePop();
	}

	notifications.info = function(message) {
		notifications.message("info", message);
	};

	notifications.success = function(message) {
		notifications.message("success", message);
	};

	notifications.error = function(message) {
		notifications.message("error", message);
	};

	notifications.warn = function(message) {
		notifications.message("warn", message);
	};

	return notifications;
});

module.factory('Provider', function($resource) {
	return $resource('/ejs-identity/api/admin/providers');
});

module.factory('Realm', function($resource) {
	return $resource('/auth-server/rest/saas/admin/realms/:id', {
		id : '@id'
	}, {
		update : {
			method : 'PUT'
		}
	});
});

module.factory('RoleMapping', function($resource) {
	return $resource('/keycloak-server/ui/api/roles/:realm/:role/:userId', {
		realm : '@realm',
		role : '@role',
		userId : '@userId'
	}, {
		save : {
			method : 'PUT'
		}
	});
});

module.factory('User', function($resource) {
	return $resource('/keycloak-server/ui/api/realms/:realm/users/:userId', {
		realm : '@realm',
		userId : '@userId'
	}, {
		save : {
			method : 'PUT'
		}
	});
});

module.factory('Role', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/roles/:roleId', {
        realm : '@realm',
        roleId : '@roleId'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('Application', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/resources/:id', {
        realm : '@realm',
        id : '@id'
    },  {
        update : {
            method : 'PUT'
        }
    });
});


module.factory('Current', function($resource) {
    return {
        realm : null,
        realms : {},
        application : null,
        applications : {}
    };
});