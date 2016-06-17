module.controller('ResourceServerCtrl', function($scope, realm, ResourceServer) {
    $scope.realm = realm;

    ResourceServer.query({realm : realm.realm}, function (data) {
        $scope.servers = data;
    });
});

module.controller('ResourceServerDetailCtrl', function($scope, $http, $route, $location, $upload, realm, ResourceServer, client, AuthzDialog, Notifications) {
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
            $scope.server = angular.copy(data);
            $scope.changed = false;
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

        $scope.onFileSelect = function($files) {
            $scope.files = $files;
        };

        $scope.clearFileSelect = function() {
            $scope.files = null;
        }

        $scope.uploadFile = function() {
            //$files: an array of files selected, each file has name, size, and type.
            for (var i = 0; i < $scope.files.length; i++) {
                var $file = $scope.files[i];
                $scope.upload = $upload.upload({
                    url: authUrl + '/admin/realms/' + $route.current.params.realm  + '/clients/' + client.id + '/authz/resource-server', //upload.php script, node.js route, or servlet url
                    // method: POST or PUT,
                    // headers: {'headerKey': 'headerValue'}, withCredential: true,
                    data: {myObj: ""},
                    file: $file
                    /* set file formData name for 'Content-Desposition' header. Default: 'file' */
                    //fileFormDataName: myFile,
                    /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
                    //formDataAppender: function(formData, key, val){}
                }).progress(function(evt) {
                    console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
                }).success(function(data, status, headers) {
                    $route.reload();
                    Notifications.success("The resource server has been updated.");
                }).error(function() {
                    Notifications.error("The resource server can not be uploaded. Please verify the file.");
                });
            }
        };
    });
});

module.controller('ResourceServerResourceCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerResource, client) {
    $scope.realm = realm;
    $scope.client = client;

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;

        $scope.createPolicy = function(resource) {
            $location.path('/realms/' + $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/permission/resource/create').search({rsrid: resource._id});
        }

        ResourceServerResource.query({realm : realm.realm, client : client.id}, function (data) {
            $scope.resources = data;
        });
    });
});

module.controller('ResourceServerResourceDetailCtrl', function($scope, $http, $route, $location, realm, ResourceServer, client, ResourceServerResource, ResourceServerScope, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
        $scope.scopes = data;
    });

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
                ResourceServerResource.save({realm : realm.realm, client : $scope.client.id}, $scope.resource, function(data) {
                    $location.url("/realms/" + realm.realm + "/clients/" + $scope.client.id + "/authz/resource-server/resource/" + data._id);
                    Notifications.success("The resource has been created.");
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
                $scope.resource = angular.copy(data);
                $scope.changed = false;

                for (i = 0; i < $scope.resource.scopes.length; i++) {
                    $scope.resource.scopes[i] = $scope.resource.scopes[i].name;
                }

                $scope.$watch('resource', function() {
                    if (!angular.equals($scope.resource, data)) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.save = function() {
                    ResourceServerResource.update({realm : realm.realm, client : $scope.client.id, rsrid : $scope.resource._id}, $scope.resource, function() {
                        $route.reload();
                        Notifications.success("The resource has been updated.");
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
                    $scope.resource = angular.copy(data);
                    $scope.changed = false;
                }
            });
        }
    });
});

module.controller('ResourceServerScopeCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerScope, client) {
    $scope.realm = realm;
    $scope.client = client;

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;

        ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
            $scope.scopes = data;
        });
    });
});

module.controller('ResourceServerScopeDetailCtrl', function($scope, $http, $route, $location, realm, ResourceServer, client, ResourceServerScope, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

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

            $scope.resource = angular.copy(scope);

            $scope.$watch('scope', function() {
                if (!angular.equals($scope.scope, scope)) {
                    $scope.changed = true;
                }
            }, true);

            $scope.save = function() {
                ResourceServerScope.save({realm : realm.realm, client : $scope.client.id}, $scope.scope, function(data) {
                    $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/scope/" + data.id);
                    Notifications.success("The scope has been created.");
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

                $scope.save = function() {
                    ResourceServerScope.update({realm : realm.realm, client : $scope.client.id, id : $scope.scope.id}, $scope.scope, function() {
                        $scope.changed = false;
                        Notifications.success("The scope has been updated.");
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
                    $scope.scope = angular.copy(data);
                    $scope.changed = false;
                }
            });
        }
    });
});

module.controller('ResourceServerPolicyCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerPolicy, PolicyProvider, client) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.policyProviders = [];

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

        ResourceServerPolicy.query({realm : realm.realm, client : client.id}, function (data) {
            $scope.policies = [];

            for (i = 0; i < data.length; i++) {
                if (data[i].type != 'resource' && data[i].type != 'scope') {
                    $scope.policies.push(data[i]);
                }
            }
        });
    });

    $scope.addPolicy = function(policyType) {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + policyType.type + "/create");
    }
});

module.controller('ResourceServerPermissionCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerPolicy, PolicyProvider, client) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.policyProviders = [];

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

        ResourceServerPolicy.query({realm : realm.realm, client : client.id}, function (data) {
            $scope.policies = [];

            for (i = 0; i < data.length; i++) {
                if (data[i].type == 'resource' || data[i].type == 'scope') {
                    $scope.policies.push(data[i]);
                }
            }
        });
    });

    $scope.addPolicy = function(policyType) {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/" + policyType.type + "/create");
    }
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

module.controller('ResourceServerPolicyScopeDetailCtrl', function($scope, $route, realm, client, PolicyController, ResourceServerPolicy, ResourceServerResource, ResourceServerScope) {
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
            User.query({realm: $route.current.params.realm}, function(data) {
                $scope.users = data;
            });

            $scope.selectedUsers = [];

            $scope.selectUser = function(user) {
                if (!user || !user.id) {
                    return;
                }

                $scope.selectedUser = {};
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

module.controller('ResourceServerPolicyRoleDetailCtrl', function($scope, $route, realm, client, PolicyController, Role, RoleById) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "role";
        },

        onInit : function() {
            Role.query({realm: $route.current.params.realm}, function(data) {
                $scope.roles = data;
            });

            $scope.selectedRoles = [];

            $scope.selectRole = function(role) {
                if (!role || !role.id) {
                    return;
                }

                $scope.selectedRole = {};
                $scope.selectedRoles.push(role);
            }

            $scope.removeFromList = function(list, index) {
                list.splice(index, 1);
            }
        },

        onInitUpdate : function(policy) {
            var selectedRoles = [];

            if (policy.config.roles) {
                var roles = eval(policy.config.roles);

                for (i = 0; i < roles.length; i++) {
                    RoleById.get({realm: $route.current.params.realm, role: roles[i]}, function(data) {
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
                roles.push($scope.selectedRoles[i].id);
            }

            $scope.policy.config.roles = JSON.stringify(roles);
        },

        onCreate : function() {
            var roles = [];

            for (i = 0; i < $scope.selectedRoles.length; i++) {
                roles.push($scope.selectedRoles[i].id);
            }

            $scope.policy.config.roles = JSON.stringify(roles);
        }
    }, realm, client, $scope);
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
            key : "kc.authz.context.authc.method",
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
            key : "kc.authz.context.authc.realm",
            name : "Realm"
        },
        {
            key : "kc.authz.context.time.date_time",
            name : "Date/Time (MM/dd/yyyy hh:mm:ss)"
        },
        {
            key : "kc.authz.context.client.network.ip_address",
            name : "Client IPv4 Address"
        },
        {
            key : "kc.authz.context.client.network.host",
            name : "Client Host"
        },
        {
            key : "kc.authz.context.client.user_agent",
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
            });
        } else {
            ResourceServerScope.query({realm : realm.realm, client : client.id}, function (data) {
                $scope.scopes = data;
            });
        }
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
    }

    $scope.showRequestTab = function() {
        $scope.showResult = false;
    }

    User.query({realm: $route.current.params.realm}, function(data) {
        $scope.users = data;
    });

    ResourceServerResource.query({realm : realm.realm, client : client.id}, function (data) {
        $scope.resources = data;
    });

    ResourceServer.get({
        realm : $route.current.params.realm,
        client : client.id
    }, function(data) {
        $scope.server = data;
    });
});