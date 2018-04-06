module.factory('ResourceServer', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server', {
        realm : '@realm',
        client: '@client'
    }, {
        'update' : {method : 'PUT'},
        'import' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/import', method : 'POST'},
        'settings' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/settings', method : 'GET'}
    });
});

module.factory('ResourceServerResource', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/resource/:rsrid', {
        realm : '@realm',
        client: '@client',
        rsrid : '@rsrid'
    }, {
        'update' : {method : 'PUT'},
        'search' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/resource/search', method : 'GET'},
        'scopes' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/resource/:rsrid/scopes', method : 'GET', isArray: true},
        'permissions' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/resource/:rsrid/permissions', method : 'GET', isArray: true}
    });
});

module.factory('ResourceServerScope', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/scope/:id', {
        realm : '@realm',
        client: '@client',
        id : '@id'
    }, {
        'update' : {method : 'PUT'},
        'search' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/scope/search', method : 'GET'},
        'resources' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/scope/:id/resources', method : 'GET', isArray: true},
        'permissions' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/scope/:id/permissions', method : 'GET', isArray: true},
    });
});

module.factory('ResourceServerPolicy', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:type/:id', {
        realm : '@realm',
        client: '@client',
        id : '@id',
        type: '@type'
    }, {
        'update' : {method : 'PUT'},
        'search' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/search', method : 'GET'},
        'associatedPolicies' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:id/associatedPolicies', method : 'GET', isArray: true},
        'dependentPolicies' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:id/dependentPolicies', method : 'GET', isArray: true},
        'scopes' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:id/scopes', method : 'GET', isArray: true},
        'resources' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:id/resources', method : 'GET', isArray: true}
    });
});

module.factory('ResourceServerPermission', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/permission/:type/:id', {
        realm : '@realm',
        client: '@client',
        type: '@type',
        id : '@id'
    }, {
        'update' : {method : 'PUT'},
        'search' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/permission/search', method : 'GET'},
        'searchPolicies' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy', method : 'GET', isArray: true},
        'associatedPolicies' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:id/associatedPolicies', method : 'GET', isArray: true},
        'dependentPolicies' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/:id/dependentPolicies', method : 'GET', isArray: true},
        'scopes' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/permission/:id/scopes', method : 'GET', isArray: true},
        'resources' : {url: authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/permission/:id/resources', method : 'GET', isArray: true}
    });
});

module.factory('PolicyProvider', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/authz/resource-server/policy/providers', {
        realm : '@realm',
        client: '@client'
    });
});

module.service('AuthzDialog', function($modal) {
    var dialog = {};

    var openDialog = function(title, message, btns, template) {
        var controller = function($scope, $modalInstance, $sce, title, message, btns) {
            $scope.title = title;
            $scope.message = $sce.trustAsHtml(message);
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

    dialog.confirmDeleteWithMsg = function(name, type, msg, success) {
        var title = 'Delete ' + type;
        msg += 'Are you sure you want to permanently delete the ' + type + ' <strong>' + name + '</strong> ?';
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

        openDialog(title, msg, btns, '/templates/authz/kc-authz-modal.html').then(success);
    };

    dialog.confirmDelete = function(name, type, success) {
        var title = 'Delete ' + type;
        var msg = 'Are you sure you want to permanently delete the ' + type + ' <strong>' + name + '</strong> ?';
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

        openDialog(title, msg, btns, '/templates/authz/kc-authz-modal.html').then(success);
    }

    return dialog;
});

module.factory('RoleManagementPermissions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role/management/permissions', {
        realm : '@realm',
        role : '@role'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('UsersManagementPermissions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-management-permissions', {
        realm : '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientManagementPermissions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/management/permissions', {
        realm : '@realm',
        client : '@client'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('IdentityProviderManagementPermissions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/management/permissions', {
        realm : '@realm',
        alias : '@alias'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('GroupManagementPermissions', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:group/management/permissions', {
        realm : '@realm',
        group : '@group'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('policyState', [function () {
    return {
        model: {
            state: {}
        }
    };
}]);

