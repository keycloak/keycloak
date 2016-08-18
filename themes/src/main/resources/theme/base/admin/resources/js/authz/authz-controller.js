module.controller('ResourceServerCtrl', function($scope, realm, ResourceServer) {
    $scope.realm = realm;

    ResourceServer.query({realm : realm.realm}, function (data) {
        $scope.servers = data;
    });
});

module.controller('ResourceServerDetailCtrl', function($scope, $http, $route, $location, $upload, $modal, realm, ResourceServer, client, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = angular.copy(data);
        $scope.changed = false;

        $scope.$watch('server', function() {
            if (!angular.equals($scope.server, data)) {
                $scope.changed = true;
            }
        }, true);

        $scope.save = function() {
            ResourceServer.update({realm : realm.realm, client : $scope.server.clientId}, $scope.server, function() {
                $route.reload();
                Notifications.success("The resource server has been created.");
            });
        }

        $scope.reset = function() {
            $route.reload();
        }

        $scope.export = function() {
            $scope.exportSettings = true;
            ResourceServer.settings({
                realm : $route.current.params.realm,
                client : client.id
            }, function(data) {
                var tmp = angular.fromJson(data);
                $scope.settings = angular.toJson(tmp, true);
            })
        }

        $scope.downloadSettings = function() {
            saveAs(new Blob([$scope.settings], { type: 'application/json' }), $scope.server.name + "-authz-config.json");
        }

        $scope.cancelExport = function() {
            delete $scope.settings
        }

        $scope.onFileSelect = function($fileContent) {
            $scope.server = angular.copy(JSON.parse($fileContent));
            $scope.importing = true;
        };

        $scope.viewImportDetails = function() {
            $modal.open({
                templateUrl: resourceUrl + '/partials/modal/view-object.html',
                controller: 'ObjectModalCtrl',
                resolve: {
                    object: function () {
                        return $scope.server;
                    }
                }
            })
        };

        $scope.import = function () {
            ResourceServer.import({realm : realm.realm, client : client.id}, $scope.server, function() {
                $route.reload();
                Notifications.success("The resource server has been updated.");
            });
        }
    });
});

module.controller('ResourceServerResourceCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerResource, client) {
    $scope.realm = realm;
    $scope.client = client;

    $scope.query = {
        realm: realm.realm,
        client : client.id,
        max : 20,
        first : 0
    };

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;

        $scope.createPolicy = function(resource) {
            $location.path('/realms/' + $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/permission/resource/create').search({rsrid: resource._id});
        }

        $scope.searchQuery();
    });

    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.searchQuery();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.searchQuery();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.searchQuery();
    }

    $scope.searchQuery = function() {
        $scope.searchLoaded = false;

        $scope.resources = ResourceServerResource.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
});

module.controller('ResourceServerResourceDetailCtrl', function($scope, $http, $route, $location, realm, ResourceServer, client, ResourceServerResource, ResourceServerScope, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
        $scope.scopes = data;
    });

    var $instance = this;

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;

        var resourceId = $route.current.params.rsrid;

        if (!resourceId) {
            $scope.create = true;
            $scope.changed = false;

            var resource = {};
            resource.scopes = [];

            $scope.resource = angular.copy(resource);

            $scope.$watch('resource', function() {
                if (!angular.equals($scope.resource, resource)) {
                    $scope.changed = true;
                }
            }, true);

            $scope.save = function() {
                $instance.checkNameAvailability(function () {
                    ResourceServerResource.save({realm : realm.realm, client : $scope.client.id}, $scope.resource, function(data) {
                        $location.url("/realms/" + realm.realm + "/clients/" + $scope.client.id + "/authz/resource-server/resource/" + data._id);
                        Notifications.success("The resource has been created.");
                    });
                });
            }

            $scope.cancel = function() {
                $location.url("/realms/" + realm.realm + "/clients/" + $scope.client.id + "/authz/resource-server/resource/");
            }
        } else {
            ResourceServerResource.get({
                realm : $route.current.params.realm,
                client : client.id,
                rsrid : $route.current.params.rsrid,
            }, function(data) {
                if (!data.scopes) {
                    data.scopes = [];
                }

                if (!data.policies) {
                    data.policies = [];
                }

                $scope.resource = angular.copy(data);
                $scope.changed = false;

                for (i = 0; i < $scope.resource.scopes.length; i++) {
                    $scope.resource.scopes[i] = $scope.resource.scopes[i].name;
                }

                $scope.originalResource = angular.copy($scope.resource);

                $scope.$watch('resource', function() {
                    if (!angular.equals($scope.resource, data)) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.save = function() {
                    $instance.checkNameAvailability(function () {
                        ResourceServerResource.update({realm : realm.realm, client : $scope.client.id, rsrid : $scope.resource._id}, $scope.resource, function() {
                            $route.reload();
                            Notifications.success("The resource has been updated.");
                        });
                    });
                }

                $scope.remove = function() {
                    var msg = "";

                    if ($scope.resource.policies.length > 0) {
                        msg = "<p>This resource is referenced in some policies:</p>";
                        msg += "<ul>";
                        for (i = 0; i < $scope.resource.policies.length; i++) {
                            msg+= "<li><strong>" + $scope.resource.policies[i].name + "</strong></li>";
                        }
                        msg += "</ul>";
                        msg += "<p>If you remove this resource, the policies above will be affected and will not be associated with this resource anymore.</p>";
                    }

                    AuthzDialog.confirmDeleteWithMsg($scope.resource.name, "Resource", msg, function() {
                        ResourceServerResource.delete({realm : realm.realm, client : $scope.client.id, rsrid : $scope.resource._id}, null, function() {
                            $location.url("/realms/" + realm.realm + "/clients/" + $scope.client.id + "/authz/resource-server/resource");
                            Notifications.success("The resource has been deleted.");
                        });
                    });
                }

                $scope.reset = function() {
                    $route.reload();
                }
            });
        }
    });

    $scope.checkNewNameAvailability = function () {
        $instance.checkNameAvailability(function () {});
    }

    this.checkNameAvailability = function (onSuccess) {
        ResourceServerResource.search({
            realm : $route.current.params.realm,
            client : client.id,
            rsrid : $route.current.params.rsrid,
            name: $scope.resource.name
        }, function(data) {
            if (data && data._id && data._id != $scope.resource._id) {
                Notifications.error("Name already in use by another resource, please choose another one.");
            } else {
                onSuccess();
            }
        });
    }
});

module.controller('ResourceServerScopeCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerScope, client) {
    $scope.realm = realm;
    $scope.client = client;

    $scope.query = {
        realm: realm.realm,
        client : client.id,
        max : 20,
        first : 0
    };

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;

        $scope.createPolicy = function(scope) {
            $location.path('/realms/' + $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/permission/scope/create').search({scpid: scope.id});
        }

        $scope.searchQuery();
    });

    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.searchQuery();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.searchQuery();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.searchQuery();
    }

    $scope.searchQuery = function() {
        $scope.searchLoaded = false;

        $scope.scopes = ResourceServerScope.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
});

module.controller('ResourceServerScopeDetailCtrl', function($scope, $http, $route, $location, realm, ResourceServer, client, ResourceServerScope, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    var $instance = this;

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;

        var scopeId = $route.current.params.id;

        if (!scopeId) {
            $scope.create = true;
            $scope.changed = false;

            var scope = {};

            $scope.scope = angular.copy(scope);

            $scope.$watch('scope', function() {
                if (!angular.equals($scope.scope, scope)) {
                    $scope.changed = true;
                }
            }, true);

            $scope.save = function() {
                $instance.checkNameAvailability(function () {
                    ResourceServerScope.save({realm : realm.realm, client : $scope.client.id}, $scope.scope, function(data) {
                        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/scope/" + data.id);
                        Notifications.success("The scope has been created.");
                    });
                });
            }
        } else {
            ResourceServerScope.get({
                realm : $route.current.params.realm,
                client : client.id,
                id : $route.current.params.id,
            }, function(data) {
                $scope.scope = angular.copy(data);
                $scope.changed = false;

                $scope.$watch('scope', function() {
                    if (!angular.equals($scope.scope, data)) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.originalScope = angular.copy($scope.scope);

                $scope.save = function() {
                    $instance.checkNameAvailability(function () {
                        ResourceServerScope.update({realm : realm.realm, client : $scope.client.id, id : $scope.scope.id}, $scope.scope, function() {
                            $scope.changed = false;
                            Notifications.success("The scope has been updated.");
                        });
                    });
                }

                $scope.remove = function() {
                    var msg = "";

                    if ($scope.scope.policies.length > 0) {
                        msg = "<p>This resource is referenced in some policies:</p>";
                        msg += "<ul>";
                        for (i = 0; i < $scope.scope.policies.length; i++) {
                            msg+= "<li><strong>" + $scope.scope.policies[i].name + "</strong></li>";
                        }
                        msg += "</ul>";
                        msg += "<p>If you remove this resource, the policies above will be affected and will not be associated with this resource anymore.</p>";
                    }

                    AuthzDialog.confirmDeleteWithMsg($scope.scope.name, "Scope", msg, function() {
                        ResourceServerScope.delete({realm : realm.realm, client : $scope.client.id, id : $scope.scope.id}, null, function() {
                            $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/scope");
                            Notifications.success("The scope has been deleted.");
                        });
                    });
                }

                $scope.reset = function() {
                    $route.reload();
                }
            });
        }
    });

    $scope.checkNewNameAvailability = function () {
        $instance.checkNameAvailability(function () {});
    }

    this.checkNameAvailability = function (onSuccess) {
        ResourceServerScope.search({
            realm : $route.current.params.realm,
            client : client.id,
            name: $scope.scope.name
        }, function(data) {
            if (data && data.id && data.id != $scope.scope.id) {
                Notifications.error("Name already in use by another scope, please choose another one.");
            } else {
                onSuccess();
            }
        });
    }
});

module.controller('ResourceServerPolicyCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerPolicy, PolicyProvider, client) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.policyProviders = [];

    $scope.query = {
        realm: realm.realm,
        client : client.id,
        permission: false,
        max : 20,
        first : 0
    };

    PolicyProvider.query({
        realm : $route.current.params.realm,
        client : client.id
    }, function (data) {
        for (i = 0; i < data.length; i++) {
            if (data[i].type != 'resource' && data[i].type != 'scope') {
                $scope.policyProviders.push(data[i]);
            }
        }
    });

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;
        $scope.searchQuery();
    });

    $scope.addPolicy = function(policyType) {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + policyType.type + "/create");
    }

    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.searchQuery();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.searchQuery();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.searchQuery();
    }

    $scope.searchQuery = function() {
        $scope.searchLoaded = false;

        ResourceServerPolicy.query($scope.query, function(data) {
            $scope.policies = [];

            for (i = 0; i < data.length; i++) {
                if (data[i].type != 'resource' && data[i].type != 'scope') {
                    $scope.policies.push(data[i]);
                }
            }

            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
});

module.controller('ResourceServerPermissionCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerPolicy, PolicyProvider, client) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.policyProviders = [];

    $scope.query = {
        realm: realm.realm,
        client : client.id,
        permission: true,
        max : 20,
        first : 0
    };

    PolicyProvider.query({
        realm : $route.current.params.realm,
        client : client.id
    }, function (data) {
        for (i = 0; i < data.length; i++) {
            if (data[i].type == 'resource' || data[i].type == 'scope') {
                $scope.policyProviders.push(data[i]);
            }
        }
    });

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;
        $scope.searchQuery();
    });

    $scope.addPolicy = function(policyType) {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/" + policyType.type + "/create");
    }

    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.searchQuery();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.searchQuery();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.searchQuery();
    }

    $scope.searchQuery = function() {
        $scope.searchLoaded = false;

        ResourceServerPolicy.query($scope.query, function(data) {
            $scope.policies = [];

            for (i = 0; i < data.length; i++) {
                if (data[i].type == 'resource' || data[i].type == 'scope') {
                    $scope.policies.push(data[i]);
                }
            }

            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
});

module.controller('ResourceServerPolicyDroolsDetailCtrl', function($scope, $http, $route, realm, client, PolicyController) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "drools";
        },

        onInit : function() {
            $scope.drools = {};

            $scope.resolveModules = function(policy) {
                if (!policy) {
                    policy = $scope.policy;
                }

                $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/drools/resolveModules'
                        , policy).success(function(data) {
                            $scope.drools.moduleNames = data;
                            $scope.resolveSessions();
                        });
            }

            $scope.resolveSessions = function() {
                $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/drools/resolveSessions'
                        , $scope.policy).success(function(data) {
                            $scope.drools.moduleSessions = data;
                        });
            }
        },

        onInitUpdate : function(policy) {
            policy.config.scannerPeriod = parseInt(policy.config.scannerPeriod);
            $scope.resolveModules(policy);
        },

        onUpdate : function() {
            $scope.policy.config.resources = JSON.stringify($scope.policy.config.resources);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.config.scannerPeriod = 1;
            newPolicy.config.scannerPeriodUnit = 'Hours';
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyResourceDetailCtrl', function($scope, $route, $location, realm, client, PolicyController, ResourceServerPolicy, ResourceServerResource) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "resource";
        },

        isPermission : function() {
            return true;
        },

        onInit : function() {
            ResourceServerResource.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.resources = data;
            });

            ResourceServerPolicy.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.policies = [];

                for (i = 0; i < data.length; i++) {
                    if (data[i].type != 'resource' && data[i].type != 'scope') {
                        $scope.policies.push(data[i]);
                    }
                }
            });

            $scope.applyToResourceType = function() {
                if ($scope.policy.config.default) {
                    $scope.policy.config.resources = [];
                } else {
                    $scope.policy.config.defaultResourceType = null;
                }
            }
        },

        onInitUpdate : function(policy) {
            policy.config.default = eval(policy.config.default);
            policy.config.resources = eval(policy.config.resources);
            policy.config.applyPolicies = eval(policy.config.applyPolicies);
        },

        onUpdate : function() {
            $scope.policy.config.resources = JSON.stringify($scope.policy.config.resources);
            $scope.policy.config.applyPolicies = JSON.stringify($scope.policy.config.applyPolicies);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.decisionStrategy = 'UNANIMOUS';
            newPolicy.config = {};
            newPolicy.config.resources = '';

            var resourceId = $location.search()['rsrid'];

            if (resourceId) {
                newPolicy.config.resources = [resourceId];
            }
        },

        onCreate : function() {
            $scope.policy.config.resources = JSON.stringify($scope.policy.config.resources);
            $scope.policy.config.applyPolicies = JSON.stringify($scope.policy.config.applyPolicies);
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyScopeDetailCtrl', function($scope, $route, $location, realm, client, PolicyController, ResourceServerPolicy, ResourceServerResource, ResourceServerScope) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "scope";
        },

        isPermission : function() {
            return true;
        },

        onInit : function() {
            ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.scopes = data;
            });

            ResourceServerResource.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.resources = data;
            });

            ResourceServerPolicy.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.policies = [];

                for (i = 0; i < data.length; i++) {
                    if (data[i].type != 'resource' && data[i].type != 'scope') {
                        $scope.policies.push(data[i]);
                    }
                }
            });

            $scope.resolveScopes = function(policy, keepScopes) {
                if (!keepScopes) {
                    policy.config.scopes = [];
                }

                if (!policy) {
                    policy = $scope.policy;
                }

                if (policy.config.resources != null) {
                    ResourceServerResource.get({
                        realm : $route.current.params.realm,
                        client : client.id,
                        rsrid : policy.config.resources
                    }, function(data) {
                        $scope.scopes = data.scopes;
                    });
                } else {
                    ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
                        $scope.scopes = data;
                    });
                }
            }
        },

        onInitUpdate : function(policy) {
            if (policy.config.resources) {
                policy.config.resources = eval(policy.config.resources);

                if (policy.config.resources.length > 0) {
                    policy.config.resources = policy.config.resources[0];
                } else {
                    policy.config.resources = null;
                }
            }

            $scope.resolveScopes(policy, true);

            policy.config.applyPolicies = eval(policy.config.applyPolicies);
            policy.config.scopes = eval(policy.config.scopes);
        },

        onUpdate : function() {
            if ($scope.policy.config.resources != null) {
                var resources = undefined;

                if ($scope.policy.config.resources.length != 0) {
                    resources = JSON.stringify([$scope.policy.config.resources])
                }

                $scope.policy.config.resources = resources;
            }

            $scope.policy.config.scopes = JSON.stringify($scope.policy.config.scopes);
            $scope.policy.config.applyPolicies = JSON.stringify($scope.policy.config.applyPolicies);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.decisionStrategy = 'UNANIMOUS';
            newPolicy.config = {};
            newPolicy.config.resources = '';

            var scopeId = $location.search()['scpid'];

            if (scopeId) {
                newPolicy.config.scopes = [scopeId];
            }
        },

        onCreate : function() {
            if ($scope.policy.config.resources != null) {
                var resources = undefined;

                if ($scope.policy.config.resources.length != 0) {
                    resources = JSON.stringify([$scope.policy.config.resources])
                }

                $scope.policy.config.resources = resources;
            }
            $scope.policy.config.scopes = JSON.stringify($scope.policy.config.scopes);
            $scope.policy.config.applyPolicies = JSON.stringify($scope.policy.config.applyPolicies);
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyUserDetailCtrl', function($scope, $route, realm, client, PolicyController, User) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "user";
        },

        onInit : function() {
            $scope.usersUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    User.query({realm: $route.current.params.realm, search: query.term.trim(), max: 20}, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    return object.username;
                }
            };

            $scope.selectedUsers = [];

            $scope.selectUser = function(user) {
                if (!user || !user.id) {
                    return;
                }

                $scope.selectedUser = null;

                for (i = 0; i < $scope.selectedUsers.length; i++) {
                    if ($scope.selectedUsers[i].id == user.id) {
                        return;
                    }
                }

                $scope.selectedUsers.push(user);
            }

            $scope.removeFromList = function(list, index) {
                list.splice(index, 1);
            }
        },

        onInitUpdate : function(policy) {
            var selectedUsers = [];

            if (policy.config.users) {
                var users = eval(policy.config.users);

                for (i = 0; i < users.length; i++) {
                    User.get({realm: $route.current.params.realm, userId: users[i]}, function(data) {
                        selectedUsers.push(data);
                        $scope.selectedUsers = angular.copy(selectedUsers);
                    });
                }
            }

            $scope.$watch('selectedUsers', function() {
                if (!angular.equals($scope.selectedUsers, selectedUsers)) {
                    $scope.changed = true;
                }
            }, true);
        },

        onUpdate : function() {
            var users = [];

            for (i = 0; i < $scope.selectedUsers.length; i++) {
                users.push($scope.selectedUsers[i].id);
            }

            $scope.policy.config.users = JSON.stringify(users);
        },

        onCreate : function() {
            var users = [];

            for (i = 0; i < $scope.selectedUsers.length; i++) {
                users.push($scope.selectedUsers[i].id);
            }

            $scope.policy.config.users = JSON.stringify(users);
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyRoleDetailCtrl', function($scope, $route, realm, client, Client, ClientRole, PolicyController, Role, RoleById) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "role";
        },

        onInit : function() {
            Role.query({realm: $route.current.params.realm}, function(data) {
                $scope.roles = data;
            });

            Client.query({realm: $route.current.params.realm}, function (data) {
                $scope.clients = data;
            });

            $scope.selectedRoles = [];

            $scope.selectRole = function(role) {
                if (!role || !role.id) {
                    return;
                }

                $scope.selectedRole = null;

                for (i = 0; i < $scope.selectedRoles.length; i++) {
                    if ($scope.selectedRoles[i].id == role.id) {
                        return;
                    }
                }

                $scope.selectedRoles.push(role);

                var clientRoles = [];

                if ($scope.clientRoles) {
                    for (i = 0; i < $scope.clientRoles.length; i++) {
                        if ($scope.clientRoles[i].id != role.id) {
                            clientRoles.push($scope.clientRoles[i]);
                        }
                    }
                    $scope.clientRoles = clientRoles;
                }
            }

            $scope.removeFromList = function(role) {
                if ($scope.clientRoles && $scope.selectedClient && $scope.selectedClient.id == role.containerId) {
                    $scope.clientRoles.push(role);
                }
                var index = $scope.selectedRoles.indexOf(role);
                if (index != -1) {
                    $scope.selectedRoles.splice(index, 1);
                }
            }

            $scope.selectClient = function() {
                if (!$scope.selectedClient) {
                    $scope.clientRoles = [];
                    return;
                }
                ClientRole.query({realm: $route.current.params.realm, client: $scope.selectedClient.id}, function(data) {
                    var roles = [];

                    for (j = 0; j < data.length; j++) {
                        var defined = false;

                        for (i = 0; i < $scope.selectedRoles.length; i++) {
                            if ($scope.selectedRoles[i].id == data[j].id) {
                                defined = true;
                                break;
                            }
                        }

                        if (!defined) {
                            data[j].container = {};
                            data[j].container.name = $scope.selectedClient.clientId;
                            roles.push(data[j]);
                        }
                    }
                    $scope.clientRoles = roles;
                });
            }
        },

        onInitUpdate : function(policy) {
            var selectedRoles = [];

            if (policy.config.roles) {
                var roles = eval(policy.config.roles);

                for (i = 0; i < roles.length; i++) {
                    RoleById.get({realm: $route.current.params.realm, role: roles[i].id}, function(data) {
                        for (i = 0; i < roles.length; i++) {
                            if (roles[i].id == data.id) {
                                data.required = roles[i].required ? true : false;
                            }
                        }
                        for (i = 0; i < $scope.clients.length; i++) {
                            if ($scope.clients[i].id == data.containerId) {
                                data.container = {};
                                data.container.name = $scope.clients[i].clientId;
                            }
                        }
                        selectedRoles.push(data);
                        $scope.selectedRoles = angular.copy(selectedRoles);
                    });
                }
            }

            $scope.$watch('selectedRoles', function() {
                if (!angular.equals($scope.selectedRoles, selectedRoles)) {
                    $scope.changed = true;
                }
            }, true);
        },

        onUpdate : function() {
            var roles = [];

            for (i = 0; i < $scope.selectedRoles.length; i++) {
                var role = {};
                role.id = $scope.selectedRoles[i].id;
                if ($scope.selectedRoles[i].required) {
                    role.required = $scope.selectedRoles[i].required;
                }
                roles.push(role);
            }

            $scope.policy.config.roles = JSON.stringify(roles);
        },

        onCreate : function() {
            var roles = [];

            for (i = 0; i < $scope.selectedRoles.length; i++) {
                var role = {};
                role.id = $scope.selectedRoles[i].id;
                if ($scope.selectedRoles[i].required) {
                    role.required = $scope.selectedRoles[i].required;
                }
                roles.push(role);
            }

            $scope.policy.config.roles = JSON.stringify(roles);
        }
    }, realm, client, $scope);
    
    $scope.hasRealmRole = function () {
        for (i = 0; i < $scope.selectedRoles.length; i++) {
            if (!$scope.selectedRoles[i].clientRole) {
                return true;
            }
        }
        return false;
    }

    $scope.hasClientRole = function () {
        for (i = 0; i < $scope.selectedRoles.length; i++) {
            if ($scope.selectedRoles[i].clientRole) {
                return true;
            }
        }
        return false;
    }
});

module.controller('ResourceServerPolicyJSDetailCtrl', function($scope, $route, $location, realm, PolicyController, client) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "js";
        },

        onInit : function() {
            $scope.initEditor = function(editor){
                var session = editor.getSession();

                session.setMode('ace/mode/javascript');
            };
        },

        onInitUpdate : function(policy) {

        },

        onUpdate : function() {

        },

        onInitCreate : function(newPolicy) {
            newPolicy.config = {};
        },

        onCreate : function() {

        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyTimeDetailCtrl', function($scope, $route, $location, realm, PolicyController, client) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "time";
        },

        onInit : function() {
        },

        onInitUpdate : function(policy) {

        },

        onUpdate : function() {

        },

        onInitCreate : function(newPolicy) {
            newPolicy.config.expirationTime = 1;
            newPolicy.config.expirationUnit = 'Minutes';
        },

        onCreate : function() {

        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyAggregateDetailCtrl', function($scope, $route, $location, realm, PolicyController, ResourceServerPolicy, client) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "aggregate";
        },

        onInit : function() {
            ResourceServerPolicy.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.policies = [];

                for (i = 0; i < data.length; i++) {
                    if (data[i].type != 'resource' && data[i].type != 'scope') {
                        $scope.policies.push(data[i]);
                    }
                }
            });
        },

        onInitUpdate : function(policy) {
            policy.config.applyPolicies = eval(policy.config.applyPolicies);
        },

        onUpdate : function() {
            $scope.policy.config.applyPolicies = JSON.stringify($scope.policy.config.applyPolicies);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.config = {};
            newPolicy.decisionStrategy = 'UNANIMOUS';
        },

        onCreate : function() {
            $scope.policy.config.applyPolicies = JSON.stringify($scope.policy.config.applyPolicies);
        }
    }, realm, client, $scope);
});

module.service("PolicyController", function($http, $route, $location, ResourceServer, ResourceServerPolicy, AuthzDialog, Notifications) {

    var PolicyController = {};

    PolicyController.onInit = function(delegate, realm, client, $scope) {
        if (!delegate.isPermission) {
            delegate.isPermission = function () {
                return false;
            }
        }

        $scope.realm = realm;
        $scope.client = client;

        $scope.decisionStrategies = ['AFFIRMATIVE', 'UNANIMOUS', 'CONSENSUS'];
        $scope.logics = ['POSITIVE', 'NEGATIVE'];

        delegate.onInit();

        var $instance = this;

        ResourceServer.get({
            realm : $route.current.params.realm,
            client : client.id
        }, function(data) {
            $scope.server = data;

            var policyId = $route.current.params.id;

            if (!policyId) {
                $scope.create = true;
                $scope.changed = false;

                var policy = {};

                policy.type = delegate.getPolicyType();
                policy.config = {};
                policy.logic = 'POSITIVE';

                if (delegate.onInitCreate) {
                    delegate.onInitCreate(policy);
                }

                $scope.policy = angular.copy(policy);

                $scope.$watch('policy', function() {
                    if (!angular.equals($scope.policy, policy)) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.save = function() {
                    $instance.checkNameAvailability(function () {
                        if (delegate.onCreate) {
                            delegate.onCreate();
                        }
                        ResourceServerPolicy.save({realm : realm.realm, client : client.id}, $scope.policy, function(data) {
                            if (delegate.isPermission()) {
                                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/" + $scope.policy.type + "/" + data.id);
                                Notifications.success("The permission has been created.");
                            } else {
                                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + $scope.policy.type + "/" + data.id);
                                Notifications.success("The policy has been created.");
                            }
                        });
                    });
                }

                $scope.cancel = function() {
                    if (delegate.isPermission()) {
                        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/");
                    } else {
                        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/");
                    }
                }
            } else {
                ResourceServerPolicy.get({
                    realm : $route.current.params.realm,
                    client : client.id,
                    id : $route.current.params.id,
                }, function(data) {
                    $scope.originalPolicy = data;
                    var policy = angular.copy(data);

                    if (delegate.onInitUpdate) {
                        delegate.onInitUpdate(policy);
                    }

                    $scope.policy = angular.copy(policy);
                    $scope.changed = false;

                    $scope.$watch('policy', function() {
                        if (!angular.equals($scope.policy, policy)) {
                            $scope.changed = true;
                        }
                    }, true);

                    $scope.save = function() {
                        $instance.checkNameAvailability(function () {
                            if (delegate.onUpdate) {
                                delegate.onUpdate();
                            }
                            ResourceServerPolicy.update({realm : realm.realm, client : client.id, id : $scope.policy.id}, $scope.policy, function() {
                                $route.reload();
                                if (delegate.isPermission()) {
                                    Notifications.success("The permission has been updated.");
                                } else {
                                    Notifications.success("The policy has been updated.");
                                }
                            });
                        });
                    }

                    $scope.reset = function() {
                        var freshPolicy = angular.copy(data);

                        if (delegate.onInitUpdate) {
                            delegate.onInitUpdate(freshPolicy);
                        }

                        $scope.policy = angular.copy(freshPolicy);
                        $scope.changed = false;
                    }
                });

                $scope.remove = function() {
                    var msg = "";

                    if ($scope.policy.dependentPolicies.length > 0) {
                        msg = "<p>This policy is being used by other policies:</p>";
                        msg += "<ul>";
                        for (i = 0; i < $scope.policy.dependentPolicies.length; i++) {
                            msg+= "<li><strong>" + $scope.policy.dependentPolicies[i].name + "</strong></li>";
                        }
                        msg += "</ul>";
                        msg += "<p>If you remove this policy, the policies above will be affected and will not be associated with this policy anymore.</p>";
                    }

                    AuthzDialog.confirmDeleteWithMsg($scope.policy.name, "Policy", msg, function() {
                        ResourceServerPolicy.delete({realm : $scope.realm.realm, client : $scope.client.id, id : $scope.policy.id}, null, function() {
                            if (delegate.isPermission()) {
                                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission");
                                Notifications.success("The permission has been deleted.");
                            } else {
                                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy");
                                Notifications.success("The policy has been deleted.");
                            }
                        });
                    });
                }
            }
        });

        $scope.checkNewNameAvailability = function () {
            $instance.checkNameAvailability(function () {});
        }

        this.checkNameAvailability = function (onSuccess) {
            ResourceServerPolicy.search({
                realm: $route.current.params.realm,
                client: client.id,
                name: $scope.policy.name
            }, function(data) {
                if (data && data.id && data.id != $scope.policy.id) {
                    Notifications.error("Name already in use by another policy or permission, please choose another one.");
                } else {
                    onSuccess();
                }
            });
        }
    }

    return PolicyController;
});

module.controller('PolicyEvaluateCtrl', function($scope, $http, $route, $location, realm, clients, roles, ResourceServer, client, ResourceServerResource, ResourceServerScope, User, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.clients = clients;
    $scope.roles = roles;
    $scope.authzRequest = {};
    $scope.authzRequest.resources = [];
    $scope.authzRequest.context = {};
    $scope.authzRequest.context.attributes = {};
    $scope.authzRequest.roleIds = [];
    $scope.newResource = {};
    $scope.resultUrl = resourceUrl + '/partials/authz/policy/resource-server-policy-evaluate-result.html';

    ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
        $scope.scopes = data;
    });

    $scope.addContextAttribute = function() {
        if (!$scope.newContextAttribute.value || $scope.newContextAttribute.value == '') {
            Notifications.error("You must provide a value to a context attribute.");
            return;
        }

        $scope.authzRequest.context.attributes[$scope.newContextAttribute.key] = $scope.newContextAttribute.value;
        delete $scope.newContextAttribute;
    }

    $scope.removeContextAttribute = function(key) {
        delete $scope.authzRequest.context.attributes[key];
    }

    $scope.getContextAttribute = function(key) {
        for (i = 0; i < $scope.defaultContextAttributes.length; i++) {
            if ($scope.defaultContextAttributes[i].key == key) {
                return $scope.defaultContextAttributes[i];
            }
        }

        return $scope.authzRequest.context.attributes[key];
    }

    $scope.getContextAttributeName = function(key) {
        var attribute = $scope.getContextAttribute(key);

        if (!attribute.name) {
            return key;
        }

        return attribute.name;
    }

    $scope.defaultContextAttributes = [
        {
            key : "custom",
            name : "Custom Attribute...",
            custom: true
        },
        {
            key : "kc.identity.authc.method",
            name : "Authentication Method",
            values: [
                {
                    key : "pwd",
                    name : "Password"
                },
                {
                    key : "otp",
                    name : "One-Time Password"
                },
                {
                    key : "kbr",
                    name : "Kerberos"
                }
            ]
        },
        {
            key : "kc.realm.name",
            name : "Realm"
        },
        {
            key : "kc.time.date_time",
            name : "Date/Time (MM/dd/yyyy hh:mm:ss)"
        },
        {
            key : "kc.client.network.ip_address",
            name : "Client IPv4 Address"
        },
        {
            key : "kc.client.network.host",
            name : "Client Host"
        },
        {
            key : "kc.client.user_agent",
            name : "Client/User Agent"
        }
    ];

    $scope.isDefaultContextAttribute = function() {
        if (!$scope.newContextAttribute) {
            return true;
        }

        if ($scope.newContextAttribute.custom) {
            return false;
        }

        if (!$scope.getContextAttribute($scope.newContextAttribute.key).custom) {
            return true;
        }

        return false;
    }

    $scope.selectDefaultContextAttribute = function() {
        $scope.newContextAttribute = angular.copy($scope.newContextAttribute);
    }

    $scope.setApplyToResourceType = function() {
        if ($scope.applyResourceType) {
            ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.scopes = data;
            });
        }

        delete $scope.newResource;
        $scope.authzRequest.resources = [];
    }

    $scope.addResource = function() {
        var resource = {};

        resource.id = $scope.newResource._id;

        for (i = 0; i < $scope.resources.length; i++) {
            if ($scope.resources[i]._id == resource.id) {
                resource.name = $scope.resources[i].name;
                break;
            }
        }

        resource.scopes = $scope.newResource.scopes;

        $scope.authzRequest.resources.push(resource);

        delete $scope.newResource;
    }

    $scope.removeResource = function(index) {
        $scope.authzRequest.resources.splice(index, 1);
    }

    $scope.resolveScopes = function() {
        if ($scope.newResource._id) {
            $scope.newResource.scopes = [];
            $scope.scopes = [];
            ResourceServerResource.get({
                realm: $route.current.params.realm,
                client : client.id,
                rsrid: $scope.newResource._id
            }, function (data) {
                $scope.scopes = data.scopes;
                if (data.typedScopes) {
                    for (i=0;i<data.typedScopes.length;i++) {
                        $scope.scopes.push(data.typedScopes[i]);
                    }
                }
            });
        } else {
            ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.scopes = data;
            });
        }
    }

    $scope.reevaluate = function() {
        if ($scope.authzRequest.entitlements) {
            $scope.entitlements();
        } else {
            $scope.save();
        }
    }

    $scope.showAuthzData = function() {
        $scope.showRpt = true;
    }

    $scope.save = function() {
        $scope.authzRequest.entitlements = false;
        if ($scope.applyResourceType) {
            if (!$scope.newResource) {
                $scope.newResource = {};
            }
            $scope.authzRequest.resources[0].scopes = $scope.newResource.scopes;
        }

        $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/evaluate'
                , $scope.authzRequest).success(function(data) {
                    $scope.evaluationResult = data;
                    $scope.showResultTab();
                });
    }

    $scope.entitlements = function() {
        $scope.authzRequest.entitlements = true;
        $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/evaluate'
            , $scope.authzRequest).success(function(data) {
            $scope.evaluationResult = data;
            $scope.showResultTab();
        });
    }

    $scope.showResultTab = function() {
        $scope.showResult = true;
        $scope.showRpt = false;
    }

    $scope.showRequestTab = function() {
        $scope.showResult = false;
        $scope.showRpt = false;
    }

    $scope.usersUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            User.query({realm: $route.current.params.realm, search: query.term.trim(), max: 20}, function(response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            object.text = object.username;
            return object.username;
        }
    };

    ResourceServerResource.query({realm : realm.realm, client : client.id}, function (data) {
        $scope.resources = data;
    });

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;
    });

    $scope.selectUser = function(user) {
        if (!user || !user.id) {
            $scope.selectedUser = null;
            $scope.authzRequest.userId = '';
            return;
        }

        $scope.authzRequest.userId = user.id;
    }

});