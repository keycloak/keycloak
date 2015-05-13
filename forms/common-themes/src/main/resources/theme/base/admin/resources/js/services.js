'use strict';

var module = angular.module('keycloak.services', [ 'ngResource', 'ngRoute' ]);

module.service('Dialog', function($modal) {
	var dialog = {};

    var openDialog = function(title, message, btns) {
        var controller = function($scope, $modalInstance, title, message, btns) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;

            $scope.ok = function () {
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
        };

        return $modal.open({
            templateUrl: resourceUrl + '/templates/kc-modal.html',
            controller: controller,
            resolve: {
                title: function() {
                    return title;
                },
                message: function() {
                    return message;
                },
                btns: function() {
                    return btns;
                }
            }
        }).result;
    }

	var escapeHtml = function(str) {
		var div = document.createElement('div');
		div.appendChild(document.createTextNode(str));
		return div.innerHTML;
	};

	dialog.confirmDelete = function(name, type, success) {
		var title = 'Delete ' + escapeHtml(type.charAt(0).toUpperCase() + type.slice(1));
		var msg = 'Are you sure you want to permanently delete the ' + type + ' ' + name + '?';
        var btns = {
            ok: {
                label: 'Delete',
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-default'
            }
        }

        openDialog(title, msg, btns).then(success);
	}

    dialog.confirmGenerateKeys = function(name, type, success) {
        var title = 'Generate new keys for realm';
        var msg = 'Are you sure you want to permanently generate new keys for ' + name + '?';
        var btns = {
            ok: {
                label: 'Generate Keys',
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-default'
            }
        }

        openDialog(title, msg, btns).then(success);
    }

    dialog.confirm = function(title, message, success, cancel) {
        var btns = {
            ok: {
                label: title,
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-default'
            }
        }

        openDialog(title, message, btns).then(success, cancel);
    }

	return dialog
});

module.factory('Notifications', function($rootScope, $timeout) {
	// time (in ms) the notifications are shown
	var delay = 5000;

	var notifications = {};

	var scheduled = null;
	var schedulePop = function() {
		if (scheduled) {
			$timeout.cancel(scheduled);
		}

		scheduled = $timeout(function() {
			$rootScope.notification = null;
			scheduled = null;
		}, delay);
	};

	if (!$rootScope.notifications) {
		$rootScope.notifications = [];
	}

	notifications.message = function(type, header, message) {
		$rootScope.notification = {
			type : type,
			header: header,
			message : message
		};

		schedulePop();
	}

	notifications.info = function(message) {
		notifications.message("info", "Info!", message);
	};

	notifications.success = function(message) {
		notifications.message("success", "Success!", message);
	};

	notifications.error = function(message) {
		notifications.message("danger", "Error!", message);
	};

	notifications.warn = function(message) {
		notifications.message("warning", "Warning!", message);
	};

	return notifications;
});

module.factory('WhoAmI', function($resource) {
    return $resource(consoleBaseUrl + '/whoami');
});

module.factory('Realm', function($resource) {
	return $resource(authUrl + '/admin/realms/:id', {
		id : '@realm'
	}, {
		update : {
			method : 'PUT'
		},
        create : {
            method : 'POST',
            params : { id : ''}
        }

    });
});

module.factory('RealmEventsConfig', function($resource) {
    return $resource(authUrl + '/admin/realms/:id/events/config', {
        id : '@realm'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('RealmEvents', function($resource) {
    return $resource(authUrl + '/admin/realms/:id/events', {
        id : '@realm'
    });
});

module.factory('RealmLDAPConnectionTester', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/testLDAPConnection');
});

module.factory('ServerInfo', function($resource) {
    return $resource(authUrl + '/admin/serverinfo');
});



module.factory('ClientProtocolMapper', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/protocol-mappers/models/:id', {
        realm : '@realm',
        client: '@client',
        id : "@id"
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('User', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId', {
        realm : '@realm',
        userId : '@userId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('UserFederationInstances', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/user-federation/instances/:instance', {
        realm : '@realm',
        instance : '@instance'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('UserFederationProviders', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/user-federation/providers/:provider', {
        realm : '@realm',
        provider : "@provider"
    });
});

module.factory('UserFederationSync', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/user-federation/sync/:provider');
});


module.factory('UserSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/session-stats', {
        realm : '@realm',
        user : '@user'
    });
});
module.factory('UserSessions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/sessions', {
        realm : '@realm',
        user : '@user'
    });
});

module.factory('UserSessionLogout', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/sessions/:session', {
        realm : '@realm',
        session : '@session'
    });
});

module.factory('UserLogout', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/logout', {
        realm : '@realm',
        user : '@user'
    });
});
module.factory('UserFederatedIdentity', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/federated-identity', {
        realm : '@realm',
        user : '@user'
    });
});
module.factory('UserConsents', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/consents/:client', {
        realm : '@realm',
        user : '@user',
        client: '@client'
    });
});

module.factory('UserCredentials', function($resource) {
    var credentials = {};

    credentials.resetPassword = $resource(authUrl + '/admin/realms/:realm/users/:userId/reset-password', {
        realm : '@realm',
        userId : '@userId'
    }, {
        update : {
            method : 'PUT'
        }
    }).update;

    credentials.removeTotp = $resource(authUrl + '/admin/realms/:realm/users/:userId/remove-totp', {
        realm : '@realm',
        userId : '@userId'
    }, {
        update : {
            method : 'PUT'
        }
    }).update;

    credentials.resetPasswordEmail = $resource(authUrl + '/admin/realms/:realm/users/:userId/reset-password-email', {
        realm : '@realm',
        userId : '@userId'
    }, {
        update : {
            method : 'PUT'
        }
    }).update;

    return credentials;
});

module.factory('RealmRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm', {
        realm : '@realm',
        userId : '@userId'
    });
});

module.factory('CompositeRealmRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm/composite', {
        realm : '@realm',
        userId : '@userId'
    });
});

module.factory('AvailableRealmRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm/available', {
        realm : '@realm',
        userId : '@userId'
    });
});


module.factory('ClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients-by-id/:client', {
        realm : '@realm',
        userId : '@userId',
        client : "@client"
    });
});

module.factory('AvailableClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients-by-id/:client/available', {
        realm : '@realm',
        userId : '@userId',
        client : "@client"
    });
});

module.factory('CompositeClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients-by-id/:client/composite', {
        realm : '@realm',
        userId : '@userId',
        client : "@client"
    });
});

module.factory('ClientRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/scope-mappings/realm', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientAvailableRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/scope-mappings/realm/available', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientCompositeRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/scope-mappings/realm/composite', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/scope-mappings/clients-by-id/:targetClient', {
        realm : '@realm',
        client : '@client',
        targetClient : '@targetClient'
    });
});

module.factory('ClientAvailableClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/scope-mappings/clients-by-id/:targetClient/available', {
        realm : '@realm',
        client : '@client',
        targetClient : '@targetClient'
    });
});

module.factory('ClientCompositeClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/scope-mappings/clients-by-id/:targetClient/composite', {
        realm : '@realm',
        client : '@client',
        targetClient : '@targetClient'
    });
});



module.factory('RealmRoles', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles', {
        realm : '@realm'
    });
});

module.factory('RoleRealmComposites', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role/composites/realm', {
        realm : '@realm',
        role : '@role'
    });
});

module.factory('RealmPushRevocation', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/push-revocation', {
        realm : '@realm'
    });
});

module.factory('RealmSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/session-stats', {
        realm : '@realm'
    });
});

module.factory('RealmClientSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-by-id-session-stats', {
        realm : '@realm'
    });
});


module.factory('RoleClientComposites', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role/composites/clients-by-id/:client', {
        realm : '@realm',
        role : '@role',
        client : "@client"
    });
});


function roleControl($scope, realm, role, roles, clients,
                     ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
                     $http, $location, Notifications, Dialog) {

    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('role', function () {
        if (!angular.equals($scope.role, role)) {
            $scope.changed = true;
        }
    }, true);

    $scope.update = function () {
        RoleById.update({
            realm: realm.realm,
            role: role.id
        }, $scope.role, function () {
            $scope.changed = false;
            role = angular.copy($scope.role);
            Notifications.success("Your changes have been saved to the role.");
        });
    };

    $scope.reset = function () {
        $scope.role = angular.copy(role);
        $scope.changed = false;
    };

    if (!role.id) return;

    $scope.compositeSwitch = role.composite;
    $scope.compositeSwitchDisabled = role.composite;
    $scope.realmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clients = clients;
    $scope.clientRoles = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];

    for (var j = 0; j < $scope.realmRoles.length; j++) {
        if ($scope.realmRoles[j].id == role.id) {
            var realmRole = $scope.realmRoles[j];
            var idx = $scope.realmRoles.indexOf(realmRole);
            $scope.realmRoles.splice(idx, 1);
            break;
        }
    }


    $scope.realmMappings = RoleRealmComposites.query({realm : realm.realm, role : role.id}, function(){
        for (var i = 0; i < $scope.realmMappings.length; i++) {
            var role = $scope.realmMappings[i];
            for (var j = 0; j < $scope.realmRoles.length; j++) {
                var realmRole = $scope.realmRoles[j];
                if (realmRole.id == role.id) {
                    var idx = $scope.realmRoles.indexOf(realmRole);
                    if (idx != -1) {
                        $scope.realmRoles.splice(idx, 1);
                        break;
                    }
                }
            }
        }
    });

    $scope.addRealmRole = function() {
        $scope.compositeSwitchDisabled=true;
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
                $scope.selectedRealmRoles).success(function() {
                for (var i = 0; i < $scope.selectedRealmRoles.length; i++) {
                    var role = $scope.selectedRealmRoles[i];
                    var idx = $scope.realmRoles.indexOf($scope.selectedRealmRoles[i]);
                    if (idx != -1) {
                        $scope.realmRoles.splice(idx, 1);
                        $scope.realmMappings.push(role);
                    }
                }
                $scope.selectRealmRoles = [];
            });
    };

    $scope.deleteRealmRole = function() {
        $scope.compositeSwitchDisabled=true;
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function() {
                for (var i = 0; i < $scope.selectedRealmMappings.length; i++) {
                    var role = $scope.selectedRealmMappings[i];
                    var idx = $scope.realmMappings.indexOf($scope.selectedRealmMappings[i]);
                    if (idx != -1) {
                        $scope.realmMappings.splice(idx, 1);
                        $scope.realmRoles.push(role);
                    }
                }
                $scope.selectedRealmMappings = [];
            });
    };

    $scope.addClientRole = function() {
        $scope.compositeSwitchDisabled=true;
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
                $scope.selectedClientRoles).success(function() {
                for (var i = 0; i < $scope.selectedClientRoles.length; i++) {
                    var role = $scope.selectedClientRoles[i];
                    var idx = $scope.clientRoles.indexOf($scope.selectedClientRoles[i]);
                    if (idx != -1) {
                        $scope.clientRoles.splice(idx, 1);
                        $scope.clientMappings.push(role);
                    }
                }
                $scope.selectedClientRoles = [];
            });
    };

    $scope.deleteClientRole = function() {
        $scope.compositeSwitchDisabled=true;
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            {data : $scope.selectedClientMappings, headers : {"content-type" : "application/json"}}).success(function() {
                for (var i = 0; i < $scope.selectedClientMappings.length; i++) {
                    var role = $scope.selectedClientMappings[i];
                    var idx = $scope.clientMappings.indexOf($scope.selectedClientMappings[i]);
                    if (idx != -1) {
                        $scope.clientMappings.splice(idx, 1);
                        $scope.clientRoles.push(role);
                    }
                }
                $scope.selectedClientMappings = [];
            });
    };


    $scope.changeClient = function() {
        $scope.clientRoles = ClientRole.query({realm : realm.realm, client : $scope.compositeClient.id}, function() {
                $scope.clientMappings = RoleClientComposites.query({realm : realm.realm, role : role.id, client : $scope.compositeClient.id}, function(){
                    for (var i = 0; i < $scope.clientMappings.length; i++) {
                        var role = $scope.clientMappings[i];
                        for (var j = 0; j < $scope.clientRoles.length; j++) {
                            var realmRole = $scope.clientRoles[j];
                            if (realmRole.id == role.id) {
                                var idx = $scope.clientRoles.indexOf(realmRole);
                                if (idx != -1) {
                                    $scope.clientRoles.splice(idx, 1);
                                    break;
                                }
                            }
                        }
                    }
                });
                for (var j = 0; j < $scope.clientRoles.length; j++) {
                    if ($scope.clientRoles[j] == role.id) {
                        var appRole = $scope.clientRoles[j];
                        var idx = $scope.clientRoles.indexof(appRole);
                        $scope.clientRoles.splice(idx, 1);
                        break;
                    }
                }
            }
        );
    };




}


module.factory('Role', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles/:role', {
        realm : '@realm',
        role : '@role'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('RoleById', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role', {
        realm : '@realm',
        role : '@role'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientRole', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/roles/:role', {
        realm : '@realm',
        client : "@client",
        role : '@role'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientClaims', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/claims', {
        realm : '@realm',
        client : "@client"
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientProtocolMappersByProtocol', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/protocol-mappers/protocol/:protocol', {
        realm : '@realm',
        client : "@client",
        protocol : "@protocol"
    });
});

module.factory('ClientSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/session-stats', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientSessionStatsWithUsers', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/session-stats?users=true', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientSessionCount', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/session-count', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientUserSessions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/user-sessions', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientLogoutAll', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/logout-all', {
        realm : '@realm',
        client : "@client"
    });
});
module.factory('ClientLogoutUser', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/logout-user/:user', {
        realm : '@realm',
        client : "@client",
        user : "@user"
    });
});
module.factory('RealmLogoutAll', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/logout-all', {
        realm : '@realm'
    });
});

module.factory('ClientPushRevocation', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/push-revocation', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientClusterNode', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/nodes/:node', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientTestNodesAvailable', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/test-nodes-available', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientCertificate', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/certificates/:attribute', {
            realm : '@realm',
            client : "@client",
            attribute: "@attribute"
        });
});

module.factory('ClientCertificateGenerate', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/certificates/:attribute/generate', {
            realm : '@realm',
            client : "@client",
            attribute: "@attribute"
        },
        {
            generate : {
                method : 'POST'
            }
        });
});

module.factory('ClientCertificateDownload', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/certificates/:attribute/download', {
        realm : '@realm',
        client : "@client",
        attribute: "@attribute"
    },
        {
            download : {
                method : 'POST',
                responseType: 'arraybuffer'
            }
        });
});

module.factory('Client', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientInstallation', function($resource) {
    var url = authUrl + '/admin/realms/:realm/clients-by-id/:client/installation/json';
    return {
        url : function(parameters)
        {
            return url.replace(':realm', parameters.realm).replace(':client', parameters.client);
        }
    }
});
module.factory('ClientInstallationJBoss', function($resource) {
    var url = authUrl + '/admin/realms/:realm/clients-by-id/:client/installation/jboss';
    return {
        url : function(parameters)
     {
        return url.replace(':realm', parameters.realm).replace(':client', parameters.client);
    }
    }
});

module.factory('ClientCredentials', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/client-secret', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'POST'
        }
    });
});

module.factory('ClientOrigins', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-by-id/:client/allowed-origins', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'PUT',
            isArray : true
        }
    });
});

module.factory('Current', function(Realm, $route, $rootScope) {
    var current = {};

    current.realms = {};
    current.realm = null;

    $rootScope.$on('$routeChangeStart', function() {
        current.realm = null;
        current.realms = Realm.query(null, function(realms) {
            if ($route.current.params.realm) {
                for (var i = 0; i < realms.length; i++) {
                    if (realms[i].realm == $route.current.params.realm) {
                        current.realm =  realms[i];
                    }
                }
            }
        });
    });

    return current;
});

module.factory('TimeUnit', function() {
    var t = {};

    t.autoUnit = function(time) {
        if (!time) {
            return 'Hours';
        }

        var unit = 'Seconds';
        if (time % 60 == 0) {
            unit = 'Minutes';
            time  = time / 60;
        }
        if (time % 60 == 0) {
            unit = 'Hours';
            time = time / 60;
        }
        if (time % 24 == 0) {
            unit = 'Days'
            time = time / 24;
        }
        return unit;
    }

    t.toSeconds = function(time, unit) {
        switch (unit) {
            case 'Seconds': return time;
            case 'Minutes': return time * 60;
            case 'Hours': return time * 3600;
            case 'Days': return time * 86400;
            default: throw 'invalid unit ' + unit;
        }
    }

    t.toUnit = function(time, unit) {
        switch (unit) {
            case 'Seconds': return time;
            case 'Minutes': return Math.ceil(time / 60);
            case 'Hours': return Math.ceil(time / 3600);
            case 'Days': return Math.ceil(time / 86400);
            default: throw 'invalid unit ' + unit;
        }
    }

    t.convert = function(time, from, to) {
        var seconds = t.toSeconds(time, from);
        return t.toUnit(seconds, to);
    }

    return t;
});


module.factory('PasswordPolicy', function() {
    var p = {};

    p.policyMessages = {
        hashIterations: 	"Number of hashing iterations.  Default is 1.  Recommended is 50000.",
        length:         	"Minimal password length (integer type). Default value is 8.",
        digits:         	"Minimal number (integer type) of digits in password. Default value is 1.",
        lowerCase:      	"Minimal number (integer type) of lowercase characters in password. Default value is 1.",
        upperCase:      	"Minimal number (integer type) of uppercase characters in password. Default value is 1.",
        specialChars:   	"Minimal number (integer type) of special characters in password. Default value is 1.",
        notUsername:    	"Block passwords that are equal to the username",
        regexPatterns:  	"Block passwords that do not match all of the regex patterns (string type).",
        passwordHistory:  	"Block passwords that are equal to previous passwords. Default value is 3.",
        forceExpiredPasswordChange:  	"Force password change when password credential is expired. Default value is 365 days."
    }

    p.allPolicies = [
        { name: 'hashIterations', value: 1 },
        { name: 'length', value: 8 },
        { name: 'digits', value: 1 },
        { name: 'lowerCase', value: 1 },
        { name: 'upperCase', value: 1 },
        { name: 'specialChars', value: 1 },
        { name: 'notUsername', value: 1 },
        { name: 'regexPatterns', value: ''},
        { name: 'passwordHistory', value: 3 },
        { name: 'forceExpiredPasswordChange', value: 365 }
    ];

    p.parse = function(policyString) {
        var policies = [];
        var re, policyEntry;

        if (!policyString || policyString.length == 0){
            return policies;
        }

        var policyArray = policyString.split(" and ");

        for (var i = 0; i < policyArray.length; i ++){
            var policyToken = policyArray[i];
            
            if(policyToken.indexOf('regexPatterns') === 0) {
            	re = /(\w+)\((.*)\)/;
            	policyEntry = re.exec(policyToken);
                if (null !== policyEntry) {
                	policies.push({ name: policyEntry[1], value: policyEntry[2] });
                }
            } else {
            	re = /(\w+)\(*(\d*)\)*/;
            	policyEntry = re.exec(policyToken);
                if (null !== policyEntry) {
                	policies.push({ name: policyEntry[1], value: parseInt(policyEntry[2]) });
                }
            }
        }
        return policies;
    };

    p.toString = function(policies) {
        if (!policies || policies.length == 0) {
            return "";
        }

        var policyString = "";

        for (var i in policies){
            policyString += policies[i].name;
            if ( policies[i].value ){
                policyString += '(' + policies[i].value + ')';
            }
            policyString += " and ";
        }

        policyString = policyString.substring(0, policyString.length - 5);

        return policyString;
    };

    return p;
});

module.factory('IdentityProvider', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias', {
        realm : '@realm',
        alias : '@alias'
    }, {
        update: {
            method : 'PUT'
        }
    });
});

module.factory('IdentityProviderExport', function($resource) {
    var url = authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/export';
    return {
        url : function(parameters)
        {
            return url.replace(':realm', parameters.realm).replace(':alias', parameters.alias);
        }
    }
});

module.factory('IdentityProviderFactory', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/providers/:provider_id', {
        realm : '@realm',
        provider_id : '@provider_id'
    });
});

module.factory('IdentityProviderMapperTypes', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/mapper-types', {
        realm : '@realm',
        alias : '@alias'
    });
});

module.factory('IdentityProviderMappers', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/mappers', {
        realm : '@realm',
        alias : '@alias'
    });
});

module.factory('IdentityProviderMapper', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/mappers/:mapperId', {
        realm : '@realm',
        alias : '@alias',
        mapperId: '@mapperId'
    }, {
        update: {
            method : 'PUT'
        }
    });
});

