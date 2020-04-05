'use strict';

var module = angular.module('keycloak.services', [ 'ngResource', 'ngRoute' ]);

module.service('Dialog', function($modal) {
    var dialog = {};

    var openDialog = function(title, message, btns, template) {
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
            templateUrl: resourceUrl + template,
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

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
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

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
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

        openDialog(title, message, btns, '/templates/kc-modal.html').then(success, cancel);
    }

    dialog.message = function(title, message, success, cancel) {
        var btns = {
            ok: {
                label: "Ok",
                cssClass: 'btn btn-default'
            }
        }

        openDialog(title, message, btns, '/templates/kc-modal-message.html').then(success, cancel);
    }

    dialog.open = function(title, message, btns, success, cancel) {
        openDialog(title, message, btns, '/templates/kc-modal.html').then(success, cancel);
    }

    return dialog
});

module.service('CopyDialog', function($modal) {
    var dialog = {};
    dialog.open = function (title, suggested, success) {
        var controller = function($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.name = { value: 'Copy of ' + suggested };
            $scope.ok = function () {
                console.log('ok with name: ' + $scope.name);
                $modalInstance.close();
                success($scope.name.value);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
        }
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-copy.html',
            controller: controller,
            resolve: {
                title: function() {
                    return title;
                }
            }
        });
    };
    return dialog;
});

module.factory('Notifications', function($rootScope, $timeout) {
    // time (in ms) the notifications are shown
    var delay = 5000;

    var notifications = {};
    notifications.current = { display: false };
    notifications.current.remove = function() {
        if (notifications.scheduled) {
            $timeout.cancel(notifications.scheduled);
            delete notifications.scheduled;
        }
        delete notifications.current.type;
        delete notifications.current.header;
        delete notifications.current.message;
        notifications.current.display = false;
        console.debug("Remove message");
    }

    $rootScope.notification = notifications.current;

    notifications.message = function(type, header, message) {
        notifications.current.remove();

        notifications.current.type = type;
        notifications.current.header = header;
        notifications.current.message = message;
        notifications.current.display = true;

        notifications.scheduled = $timeout(function() {
            notifications.current.remove();
        }, delay);

        console.debug("Added message");
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


module.factory('ComponentUtils', function() {

    function sortGroups(prop, arr) {
        // sort current elements
        arr.sort(function (a, b) {
            if (a[prop] < b[prop]) { return -1; }
            if (a[prop] > b[prop]) { return 1; }
            return 0;
        });
        // check sub groups
        arr.forEach(function (item, index) {
            if (!!item.subGroups) {
                sortGroups(prop, item.subGroups);
            }
        });
        return arr;
    };

    var utils = {};

    utils.sortGroups = sortGroups;

    utils.findIndexById = function(array, id) {
        for (var i = 0; i < array.length; i++) {
            if (array[i].id === id) return i;
        }
        return -1;
    }

    utils.convertAllMultivaluedStringValuesToList = function(properties, config) {
        if (!properties) {
            return;
        }

        for (var i=0 ; i<properties.length ; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedString') {
                var configProperty = config[prop.name];

                if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                }

                if (typeof configProperty === "string") {
                    configProperty = configProperty.split("##");
                    config[prop.name] = configProperty;
                }
            }
        }
    }

    utils.convertAllListValuesToMultivaluedString = function(properties, config) {
        if (!properties) {
            return;
        }

        for (var i=0 ; i<properties.length ; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedString') {
                var configVal = config[prop.name];

                if (configVal != null) {
                    if (configVal.length > 0) {
                        var lastVal = configVal[configVal.length - 1];
                        if (lastVal === '') {
                            console.log('Remove empty value from config property: ' + prop.name);
                            configVal.splice(configVal.length - 1, 1);
                        }
                    }

                    var attrVals = configVal.join("##");
                    config[prop.name] = attrVals;

                }
            }
        }
    }



    utils.addLastEmptyValueToMultivaluedLists = function(properties, config) {
        if (!properties) {
            return;
        }

        for (var i=0 ; i<properties.length ; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedString') {
                var configProperty = config[prop.name];

                if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                }

                if (configProperty.length == 0 || configProperty[configProperty.length - 1].length > 0) {
                    configProperty.push('');
                }
            }
        }
    }


    utils.removeLastEmptyValue = function(componentConfig) {

        for (var configPropertyName in componentConfig) {
            var configVal = componentConfig[configPropertyName];
            if (configVal && configVal.length > 0) {
                var lastVal = configVal[configVal.length - 1];
                if (lastVal === '') {
                    console.log('Remove empty value from config property: ' + configPropertyName);
                    configVal.splice(configVal.length - 1, 1);
                }
            }
        }
    }

    // Allows you to use ui-select2 with <input> tag.
    // In HTML you will then use property.mvOptions like this:
    // <input ui-select2="prop.mvOptions" ng-model="...
    utils.addMvOptionsToMultivaluedLists = function(properties) {
        if (!properties) return;

        for (var i=0 ; i<properties.length ; i++) {
            var prop = properties[i];
            if (prop.type !== 'MultivaluedList') continue;

            prop.mvOptions = {
                'multiple' : true,
                'simple_tags' : true,
                'tags' : angular.copy(prop.options)
            }
        }

    }

    return utils;
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

module.factory('RealmKeys', function($resource) {
    return $resource(authUrl + '/admin/realms/:id/keys', {
        id : '@realm'
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

module.factory('RealmAdminEvents', function($resource) {
    return $resource(authUrl + '/admin/realms/:id/admin-events', {
        id : '@realm'
    });
});

module.factory('BruteForce', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/attack-detection/brute-force/users', {
        realm : '@realm'
    });
});

module.factory('BruteForceUser', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/attack-detection/brute-force/users/:userId', {
        realm : '@realm',
        userId : '@userId'
    });
});


module.factory('RequiredActions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-actions/:alias', {
        realm : '@realm',
        alias : '@alias'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('RequiredActionRaisePriority', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-actions/:alias/raise-priority', {
        realm : '@realm',
        alias : '@alias'
    });
});

module.factory('RequiredActionLowerPriority', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-actions/:alias/lower-priority', {
        realm : '@realm',
        alias : '@alias'
    });
});

module.factory('UnregisteredRequiredActions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/unregistered-required-actions', {
        realm : '@realm'
    });
});

module.factory('RegisterRequiredAction', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/register-required-action', {
        realm : '@realm'
    });
});

module.factory('RealmLDAPConnectionTester', function($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/testLDAPConnection', {
        realm : '@realm'
    });
});

module.factory('RealmSMTPConnectionTester', function($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/testSMTPConnection', {
        realm : '@realm'
    });
});

module.service('ServerInfo', function($resource, $q, $http) {
    var info = {};
    var delay = $q.defer();

    function copyInfo(data, info) {
        angular.copy(data, info);

        info.listProviderIds = function(spi) {
            var providers = info.providers[spi].providers;
            var ids = Object.keys(providers);
            ids.sort(function(a, b) {
                var s1;
                var s2;

                if (providers[a].order != providers[b].order) {
                    s1 = providers[b].order;
                    s2 = providers[a].order;
                } else {
                    s1 = a;
                    s2 = b;
                }

                if (s1 < s2) {
                    return -1;
                } else if (s1 > s2) {
                    return 1;
                } else {
                    return 0;
                }
            });
            return ids;
        }

        info.featureEnabled = function(provider) {
            return info.profileInfo.disabledFeatures.indexOf(provider) == -1;
        }
    }

    $http.get(authUrl + '/admin/serverinfo').then(function(response) {
        copyInfo(response.data, info);
        delay.resolve(info);
    });

    return {
        get: function() {
            return info;
        },
        reload: function() {
            $http.get(authUrl + '/admin/serverinfo').then(function(response) {
                copyInfo(response.data, info);
            });
        },
        promise: delay.promise
    }
});

module.factory('ClientInitialAccess', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-initial-access/:id', {
        realm : '@realm',
        id : '@id'
    });
});

module.factory('ClientProtocolMapper', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/protocol-mappers/models/:id', {
        realm : '@realm',
        client: '@client',
        id : "@id"
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientScopeProtocolMapper', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/protocol-mappers/models/:id', {
        realm : '@realm',
        clientScope: '@clientScope',
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

module.service('UserSearchState', function() {
    this.isFirstSearch = true;
    this.query = {
        max : 20,
        first : 0
    };
});

module.service('ClientListSearchState', function() {
    this.isFirstSearch = true;
    this.query = {
        max : 20,
        first : 0,
        search: true
    };
});

// Service tracks the last flow selected in Authentication-->Flows tab
module.service('LastFlowSelected', function() {
    this.alias = null;
});

module.service('RealmRoleRemover', function() {
   this.remove = function (role, realm, Dialog, $location, Notifications) {
        Dialog.confirmDelete(role.name, 'role', function () {
            role.$remove({
                realm: realm.realm,
                role: role.id
            }, function () {
                $location.url("/realms/" + realm.realm + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };
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
module.factory('UserOfflineSessions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/offline-sessions/:client', {
        realm : '@realm',
        user : '@user',
        client : '@client'
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

module.factory('UserFederatedIdentities', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/federated-identity', {
        realm : '@realm',
        user : '@user'
    });
});
module.factory('UserFederatedIdentity', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/federated-identity/:provider', {
        realm : '@realm',
        user : '@user',
        provider : '@provider'
    });
});

module.factory('UserConsents', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/consents/:client', {
        realm : '@realm',
        user : '@user',
        client: '@client'
    });
});

module.factory('UserImpersonation', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/impersonation', {
        realm : '@realm',
        user : '@user'
    });
});

module.factory('UserCredentials', function($resource) {
    var credentials = {};

    credentials.getCredentials = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials', {
        realm : '@realm',
        userId : '@userId'
    }).query;

    credentials.getConfiguredUserStorageCredentialTypes = $resource(authUrl + '/admin/realms/:realm/users/:userId/configured-user-storage-credential-types', {
        realm : '@realm',
        userId : '@userId'
    }).query;

    credentials.deleteCredential = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId', {
        realm : '@realm',
        userId : '@userId',
        credentialId : '@credentialId'
    }).delete;

    credentials.updateCredentialLabel = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/userLabel', {
        realm : '@realm',
        userId : '@userId',
        credentialId : '@credentialId'
    }, {
        update : {
            method : 'PUT',
            headers: {
                'Content-Type': 'text/plain;charset=utf-8'
            },
            transformRequest: function(credential, getHeaders) {
                return credential.userLabel;
            }
        }
    }).update;

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

    credentials.disableCredentialTypes = $resource(authUrl + '/admin/realms/:realm/users/:userId/disable-credential-types', {
        realm : '@realm',
        userId : '@userId'
    }, {
        update : {
            method : 'PUT'
        }
    }).update;

    credentials.moveCredentialAfter = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/moveAfter/:newPreviousCredentialId', {
        realm : '@realm',
        userId : '@userId',
        credentialId : '@credentialId',
        newPreviousCredentialId : '@newPreviousCredentialId'
    }, {
        update : {
            method : 'POST'
        }
    }).update;

    credentials.moveToFirst = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/moveToFirst', {
        realm : '@realm',
        userId : '@userId',
        credentialId : '@credentialId'
    }, {
        update : {
            method : 'POST'
        }
    }).update;

    return credentials;
});

module.factory('UserExecuteActionsEmail', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/execute-actions-email', {
        realm : '@realm',
        userId : '@userId',
        lifespan : '@lifespan',
    }, {
        update : {
            method : 'PUT'
        }
    });
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
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients/:client', {
        realm : '@realm',
        userId : '@userId',
        client : "@client"
    });
});

module.factory('AvailableClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients/:client/available', {
        realm : '@realm',
        userId : '@userId',
        client : "@client"
    });
});

module.factory('CompositeClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients/:client/composite', {
        realm : '@realm',
        userId : '@userId',
        client : "@client"
    });
});

module.factory('ClientRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/realm', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientAvailableRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/realm/available', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientCompositeRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/realm/composite', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/clients/:targetClient', {
        realm : '@realm',
        client : '@client',
        targetClient : '@targetClient'
    });
});

module.factory('ClientAvailableClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/clients/:targetClient/available', {
        realm : '@realm',
        client : '@client',
        targetClient : '@targetClient'
    });
});

module.factory('ClientCompositeClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/clients/:targetClient/composite', {
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

module.factory('RealmClearUserCache', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-user-cache', {
        realm : '@realm'
    });
});

module.factory('RealmClearRealmCache', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-realm-cache', {
        realm : '@realm'
    });
});

module.factory('RealmClearKeysCache', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-keys-cache', {
        realm : '@realm'
    });
});

module.factory('RealmSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/session-stats', {
        realm : '@realm'
    });
});

module.factory('RealmClientSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-session-stats', {
        realm : '@realm'
    });
});


module.factory('RoleClientComposites', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role/composites/clients/:client', {
        realm : '@realm',
        role : '@role',
        client : "@client"
    });
});

function clientSelectControl($scope, realm, Client) {
    $scope.clientsUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            Client.query({realm: realm, search: true, clientId: query.term.trim(), max: 20}, function(response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            object.text = object.clientId;
            return object.clientId;
        }
    };
}

function roleControl($scope, $route, realm, role, roles, Client,
            ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
            $http, $location, Notifications, Dialog, ComponentUtils) {
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


    clientSelectControl($scope, $route.current.params.realm, Client);
    
    $scope.selectedClient = null;


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
        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            $scope.selectedRealmRolesToAdd).then(function() {
            for (var i = 0; i < $scope.selectedRealmRolesToAdd.length; i++) {
                var role = $scope.selectedRealmRolesToAdd[i];
                var idx = ComponentUtils.findIndexById($scope.realmRoles, role.id);
                if (idx != -1) {
                    $scope.realmRoles.splice(idx, 1);
                    $scope.realmMappings.push(role);
                }
            }
            $scope.selectedRealmRoles = [];
            $scope.selectedRealmRolesToAdd = [];
            Notifications.success("Role added to composite.");
        });
    };

    $scope.deleteRealmRole = function() {
        $scope.compositeSwitchDisabled=true;
        $scope.selectedRealmMappingsToRemove = JSON.parse('[' + $scope.selectedRealmMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            {data : $scope.selectedRealmMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function() {
            for (var i = 0; i < $scope.selectedRealmMappingsToRemove.length; i++) {
                var role = $scope.selectedRealmMappingsToRemove[i];
                var idx = ComponentUtils.findIndexById($scope.realmMappings, role.id);
                if (idx != -1) {
                    $scope.realmMappings.splice(idx, 1);
                    $scope.realmRoles.push(role);
                }
            }
            $scope.selectedRealmMappings = [];
            $scope.selectedRealmMappingsToRemove = [];
            Notifications.success("Role removed from composite.");
        });
    };

    $scope.addClientRole = function() {
        $scope.compositeSwitchDisabled=true;
        $scope.selectedClientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            $scope.selectedClientRolesToAdd).then(function() {
            for (var i = 0; i < $scope.selectedClientRolesToAdd.length; i++) {
                var role = $scope.selectedClientRolesToAdd[i];
                var idx = ComponentUtils.findIndexById($scope.clientRoles, role.id);
                if (idx != -1) {
                    $scope.clientRoles.splice(idx, 1);
                    $scope.clientMappings.push(role);
                }
            }
            $scope.selectedClientRoles = [];
            $scope.selectedClientRolesToAdd = [];
            Notifications.success("Client role added.");
        });
    };

    $scope.deleteClientRole = function() {
        $scope.compositeSwitchDisabled=true;
        $scope.selectedClientMappingsToRemove = JSON.parse('[' + $scope.selectedClientMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            {data : $scope.selectedClientMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function() {
            for (var i = 0; i < $scope.selectedClientMappingsToRemove.length; i++) {
                var role = $scope.selectedClientMappingsToRemove[i];
                var idx = ComponentUtils.findIndexById($scope.clientMappings, role.id);
                if (idx != -1) {
                    $scope.clientMappings.splice(idx, 1);
                    $scope.clientRoles.push(role);
                }
            }
            $scope.selectedClientMappings = [];
            $scope.selectedClientMappingsToRemove = [];
            Notifications.success("Client role removed.");
        });
    };


    $scope.changeClient = function(client) {
        console.log("selected client: ", client);
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        $scope.clientRoles = ClientRole.query({realm : realm.realm, client : client.id}, function() {
                $scope.clientMappings = RoleClientComposites.query({realm : realm.realm, role : role.id, client : client.id}, function(){
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
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/roles/:role', {
        realm : '@realm',
        client : "@client",
        role : '@role'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientDefaultClientScopes', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/default-client-scopes/:clientScopeId', {
        realm : '@realm',
        client : "@client",
        clientScopeId : '@clientScopeId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientOptionalClientScopes', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/optional-client-scopes/:clientScopeId', {
        realm : '@realm',
        client : "@client",
        clientScopeId : '@clientScopeId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientEvaluateProtocolMappers', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/protocol-mappers?scope=:scopeParam', {
        realm : '@realm',
        client : "@client",
        scopeParam : "@scopeParam"
    });
});

module.factory('ClientEvaluateGrantedRoles', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/scope-mappings/:roleContainer/granted?scope=:scopeParam', {
        realm : '@realm',
        client : "@client",
        roleContainer : "@roleContainer",
        scopeParam : "@scopeParam"
    });
});

module.factory('ClientEvaluateNotGrantedRoles', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/scope-mappings/:roleContainer/not-granted?scope=:scopeParam', {
        realm : '@realm',
        client : "@client",
        roleContainer : "@roleContainer",
        scopeParam : "@scopeParam"
    });
});

module.factory('ClientEvaluateGenerateExampleToken', function($resource) {
    var url = authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/generate-example-access-token?scope=:scopeParam&userId=:userId';
    return {
        url : function(parameters)
        {
            return url
                .replace(':realm', parameters.realm)
                .replace(':client', parameters.client)
                .replace(':scopeParam', parameters.scopeParam)
                .replace(':userId', parameters.userId);
        }
    }
});

module.factory('ClientProtocolMappersByProtocol', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/protocol-mappers/protocol/:protocol', {
        realm : '@realm',
        client : "@client",
        protocol : "@protocol"
    });
});

module.factory('ClientScopeProtocolMappersByProtocol', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/protocol-mappers/protocol/:protocol', {
        realm : '@realm',
        clientScope : "@clientScope",
        protocol : "@protocol"
    });
});

module.factory('ClientScopeRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm', {
        realm : '@realm',
        clientScope : '@clientScope'
    });
});

module.factory('ClientScopeAvailableRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm/available', {
        realm : '@realm',
        clientScope : '@clientScope'
    });
});

module.factory('ClientScopeCompositeRealmScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm/composite', {
        realm : '@realm',
        clientScope : '@clientScope'
    });
});

module.factory('ClientScopeClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/clients/:targetClient', {
        realm : '@realm',
        clientScope : '@clientScope',
        targetClient : '@targetClient'
    });
});

module.factory('ClientScopeAvailableClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/clients/:targetClient/available', {
        realm : '@realm',
        clientScope : '@clientScope',
        targetClient : '@targetClient'
    });
});

module.factory('ClientScopeCompositeClientScopeMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/clients/:targetClient/composite', {
        realm : '@realm',
        clientScope : '@clientScope',
        targetClient : '@targetClient'
    });
});


module.factory('ClientSessionStats', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/session-stats', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientSessionStatsWithUsers', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/session-stats?users=true', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientSessionCount', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/session-count', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientUserSessions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/user-sessions', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientOfflineSessionCount', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/offline-session-count', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientOfflineSessions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/offline-sessions', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('RealmLogoutAll', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/logout-all', {
        realm : '@realm'
    });
});

module.factory('ClientPushRevocation', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/push-revocation', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientClusterNode', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/nodes/:node', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientTestNodesAvailable', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/test-nodes-available', {
        realm : '@realm',
        client : "@client"
    });
});

module.factory('ClientCertificate', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/certificates/:attribute', {
        realm : '@realm',
        client : "@client",
        attribute: "@attribute"
    });
});

module.factory('ClientCertificateGenerate', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/certificates/:attribute/generate', {
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
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/certificates/:attribute/download', {
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
    return $resource(authUrl + '/admin/realms/:realm/clients/:client', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('ClientScope', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope', {
        realm : '@realm',
        clientScope : '@clientScope'
    },  {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('RealmDefaultClientScopes', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/default-default-client-scopes/:clientScopeId', {
        realm : '@realm',
        clientScopeId : '@clientScopeId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('RealmOptionalClientScopes', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/default-optional-client-scopes/:clientScopeId', {
        realm : '@realm',
        clientScopeId : '@clientScopeId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});


module.factory('ClientDescriptionConverter', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-description-converter', {
        realm : '@realm'
    });
});

/*
module.factory('ClientInstallation', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/installation/providers/:provider', {
        realm : '@realm',
        client : '@client',
        provider : '@provider'
    });
});
*/



module.factory('ClientInstallation', function($resource) {
    var url = authUrl + '/admin/realms/:realm/clients/:client/installation/providers/:provider';
    return {
        url : function(parameters)
        {
            return url.replace(':realm', parameters.realm).replace(':client', parameters.client).replace(':provider', parameters.provider);
        }
    }
});

module.factory('ClientInstallationJBoss', function($resource) {
    var url = authUrl + '/admin/realms/:realm/clients/:client/installation/jboss';
    return {
        url : function(parameters)
        {
            return url.replace(':realm', parameters.realm).replace(':client', parameters.client);
        }
    }
});

module.factory('ClientSecret', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/client-secret', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'POST'
        }
    });
});

module.factory('ClientRegistrationAccessToken', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/registration-access-token', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'POST'
        }
    });
});

module.factory('ClientOrigins', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/allowed-origins', {
        realm : '@realm',
        client : '@client'
    },  {
        update : {
            method : 'PUT',
            isArray : true
        }
    });
});

module.factory('ClientServiceAccountUser', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/service-account-user', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('Current', function(Realm, $route, $rootScope) {
    var current = {
        realms: {},
        realm: null
    };

    $rootScope.$on('$routeChangeStart', function() {
        current.realms = Realm.query(null, function(realms) {
            var currentRealm = null;
            if ($route.current.params.realm) {
                for (var i = 0; i < realms.length; i++) {
                    if (realms[i].realm == $route.current.params.realm) {
                        currentRealm =  realms[i];
                    }
                }
            }
            current.realm = currentRealm;
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

    return t;
});

module.factory('TimeUnit2', function() {
    var t = {};

    t.asUnit = function(time) {

        var unit = 'Minutes';

        if (time) {
            if (time == -1) {
                time = -1;
            } else {
                if (time < 60) {
                    time = 60;
                }

                if (time % 60 == 0) {
                    unit = 'Minutes';
                    time = time / 60;
                }
                if (time % 60 == 0) {
                    unit = 'Hours';
                    time = time / 60;
                }
                if (time % 24 == 0) {
                    unit = 'Days'
                    time = time / 24;
                }
            }
        }

        var v = {
            unit: unit,
            time: time,
            toSeconds: function() {
                switch (v.unit) {
                    case 'Minutes':
                        return v.time * 60;
                    case 'Hours':
                        return v.time * 3600;
                    case 'Days':
                        return v.time * 86400;
                }
            }
        }

        return v;
    }

    return t;
});

module.filter('removeSelectedPolicies', function() {
    return function(policies, selectedPolicies) {
        var result = [];
        for(var i in policies) {
            var policy = policies[i];
            var policyAvailable = true;
            for(var j in selectedPolicies) {
                if(policy.id === selectedPolicies[j].id && !policy.multipleSupported) {
                    policyAvailable = false;
                }
            }
            if(policyAvailable) {
                result.push(policy);
            }
        }
        return result;
    }
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

module.factory('AuthenticationFlowExecutions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/executions', {
        realm : '@realm',
        alias : '@alias'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('CreateExecutionFlow', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/executions/flow', {
        realm : '@realm',
        alias : '@alias'
    });
});

module.factory('CreateExecution', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/executions/execution', {
        realm : '@realm',
        alias : '@alias'
    });
});

module.factory('AuthenticationFlows', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:flow', {
        realm : '@realm',
        flow: '@flow'
    });
});

module.factory('AuthenticationFormProviders', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/form-providers', {
        realm : '@realm'
    });
});

module.factory('AuthenticationFormActionProviders', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/form-action-providers', {
        realm : '@realm'
    });
});

module.factory('AuthenticatorProviders', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/authenticator-providers', {
        realm : '@realm'
    });
});

module.factory('ClientAuthenticatorProviders', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/client-authenticator-providers', {
        realm : '@realm'
    });
});


module.factory('AuthenticationFlowsCopy', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/copy', {
        realm : '@realm',
        alias : '@alias'
    });
});
module.factory('AuthenticationConfigDescription', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/config-description/:provider', {
        realm : '@realm',
        provider: '@provider'
    });
});
module.factory('PerClientAuthenticationConfigDescription', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/per-client-config-description', {
        realm : '@realm'
    });
});

module.factory('AuthenticationConfig', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/config/:config', {
        realm : '@realm',
        config: '@config'
    }, {
        update: {
            method : 'PUT'
        }
    });
});
module.factory('AuthenticationExecutionConfig', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/:execution/config', {
        realm : '@realm',
        execution: '@execution'
    });
});

module.factory('AuthenticationExecution', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/:execution', {
        realm : '@realm',
        execution : '@execution'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('AuthenticationExecutionRaisePriority', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/:execution/raise-priority', {
        realm : '@realm',
        execution : '@execution'
    });
});

module.factory('AuthenticationExecutionLowerPriority', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/:execution/lower-priority', {
        realm : '@realm',
        execution : '@execution'
    });
});



module.service('SelectRoleDialog', function($modal) {
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

module.factory('Group', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId', {
        realm : '@realm',
        userId : '@groupId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('GroupChildren', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/children', {
        realm : '@realm',
        groupId : '@groupId'
    });
});

module.factory('GroupsCount', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/count', {
            realm : '@realm'
        },
        {
            query: {
                isArray: false,
                method: 'GET',
                params: {},
                transformResponse: function (data) {
                    return angular.fromJson(data)
                }
            }
        });
});

module.factory('Groups', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups', {
        realm : '@realm'
    })
});

module.factory('GroupRealmRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm', {
        realm : '@realm',
        groupId : '@groupId'
    });
});

module.factory('GroupCompositeRealmRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm/composite', {
        realm : '@realm',
        groupId : '@groupId'
    });
});

module.factory('GroupAvailableRealmRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm/available', {
        realm : '@realm',
        groupId : '@groupId'
    });
});


module.factory('GroupClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/clients/:client', {
        realm : '@realm',
        groupId : '@groupId',
        client : "@client"
    });
});

module.factory('GroupAvailableClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/clients/:client/available', {
        realm : '@realm',
        groupId : '@groupId',
        client : "@client"
    });
});

module.factory('GroupCompositeClientRoleMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/clients/:client/composite', {
        realm : '@realm',
        groupId : '@groupId',
        client : "@client"
    });
});

module.factory('GroupMembership', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/members', {
        realm : '@realm',
        groupId : '@groupId'
    });
});

module.factory('RoleList', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles', {
        realm : '@realm'
    });
});

module.factory('RoleMembership', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles/:role/users', {
        realm : '@realm',
        role : '@role'
    });
});

module.factory('ClientRoleList', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/roles', {
        realm : '@realm',
        client : '@client'
    });
});

module.factory('ClientRoleMembership', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/roles/:role/users', {
        realm : '@realm',
        client : '@client',
        role : '@role'
    });
});

module.factory('UserGroupMembership', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/groups', {
        realm : '@realm',
        userId : '@userId'
    });
});

module.factory('UserGroupMapping', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/groups/:groupId', {
        realm : '@realm',
        userId : '@userId',
        groupId : '@groupId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('DefaultGroups', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/default-groups/:groupId', {
        realm : '@realm',
        groupId : '@groupId'
    }, {
        update : {
            method : 'PUT'
        }
    });
});

module.factory('SubComponentTypes', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/components/:componentId/sub-component-types', {
        realm: '@realm',
        componentId: '@componentId'
    });
});

module.factory('Components', function($resource, ComponentUtils) {
    return $resource(authUrl + '/admin/realms/:realm/components/:componentId', {
        realm : '@realm',
        componentId : '@componentId'
    }, {
        update : {
            method : 'PUT',
            transformRequest: function(componentInstance) {

                if (componentInstance.config) {
                    ComponentUtils.removeLastEmptyValue(componentInstance.config);
                }

                return angular.toJson(componentInstance);
            }
        },
        save : {
            method : 'POST',
            transformRequest: function(componentInstance) {

                if (componentInstance.config) {
                    ComponentUtils.removeLastEmptyValue(componentInstance.config);
                }

                return angular.toJson(componentInstance);
            }
        }
    });
});

module.factory('UserStorageOperations', function($resource) {
    var object = {}
    object.sync = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/sync', {
        realm : '@realm',
        componentId : '@componentId'
    });
    object.removeImportedUsers = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/remove-imported-users', {
        realm : '@realm',
        componentId : '@componentId'
    });
    object.unlinkUsers = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/unlink-users', {
        realm : '@realm',
        componentId : '@componentId'
    });
    object.simpleName = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/name', {
        realm : '@realm',
        componentId : '@componentId'
    });
    return object;
});


module.factory('ClientStorageOperations', function($resource) {
    var object = {}
    object.simpleName = $resource(authUrl + '/admin/realms/:realm/client-storage/:componentId/name', {
        realm : '@realm',
        componentId : '@componentId'
    });
    return object;
});


module.factory('ClientRegistrationPolicyProviders', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-registration-policy/providers', {
        realm : '@realm',
    });
});

module.factory('LDAPMapperSync', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/user-storage/:parentId/mappers/:mapperId/sync', {
        realm : '@realm',
        componentId : '@componentId',
        mapperId: '@mapperId'
    });
});


module.factory('UserGroupMembershipCount', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/groups/count', {
            realm : '@realm',
            userId : '@userId'
        },
        {
            query: {
                isArray: false,
                method: 'GET',
                params: {},
                transformResponse: function (data) {
                    return angular.fromJson(data)
                }
            }
        });
});
