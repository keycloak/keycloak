'use strict';

var module = angular.module('keycloak.services', [ 'ngResource' ]);

module.service('Auth', function() {
	var auth = {
		loggedIn : false
	};
	auth.user = {
		userId : null,
		displayName : null
	};
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

module.factory('Realm', function($resource) {
	return $resource('/auth-server/rest/saas/admin/realms/:id', {
		id : '@id'
	}, {
		update : {
			method : 'PUT'
		}
	});
});

module.factory('User', function($resource) {
	return $resource('/auth-server/rest/saas/admin/realms/:realm/users/:userId', {
		realm : '@realm',
		userId : '@userId'
	}, {
		update : {
			method : 'PUT'
		}
	});
});

module.factory('RealmRoleMapping', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/users/:userId/role-mappings/realm', {
        realm : '@realm',
        userId : '@userId'
    });
});

module.factory('ApplicationRoleMapping', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/users/:userId/role-mappings/applications/:application', {
        realm : '@realm',
        userId : '@userId',
        application : "@application"
    });
});

module.factory('ApplicationRealmScopeMapping', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/applications/:application/scope-mappings/realm', {
        realm : '@realm',
        application : '@application'
    });
});

module.factory('ApplicationApplicationScopeMapping', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/applications/:application/scope-mappings/applications/:targetApp', {
        realm : '@realm',
        application : '@application',
        targetApp : '@targetApp'
    });
});



module.factory('RealmRoles', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/roles', {
        realm : '@realm'
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

module.factory('ApplicationRole', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/applications/:application/roles/:roleId', {
        realm : '@realm',
        application : "@application",
        roleId : '@roleId'
    },  {
        update : {
            method : 'PUT'
        }
    });
});


module.factory('Application', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/applications/:id', {
        realm : '@realm',
        id : '@id'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ApplicationCredentials', function($resource) {
    return $resource('/auth-server/rest/saas/admin/realms/:realm/applications/:application/credentials', {
        realm : '@realm',
        application : '@application'
    },  {
        update : {
            method : 'PUT',
            isArray : true
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