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
        deep: false,
        max : 20,
        first : 0
    };

    $scope.listSizes = [5, 10, 20];

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

        ResourceServerResource.query($scope.query, function(response) {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            $scope.resources = response;
            if ($scope.detailsFilter) {
                $scope.showDetails();
            }
        });
    };

    $scope.loadDetails = function (resource) {
        if (resource.details) {
            resource.details.loaded = !resource.details.loaded;
            return;
        }

        resource.details = {loaded: false};

        ResourceServerResource.scopes({
            realm : $route.current.params.realm,
            client : client.id,
            rsrid : resource._id
        }, function(response) {
            resource.scopes = response;
            ResourceServerResource.permissions({
                realm : $route.current.params.realm,
                client : client.id,
                rsrid : resource._id
            }, function(response) {
                resource.policies = response;
                resource.details.loaded = true;
            });
        });
    }

    $scope.showDetails = function(item) {
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.resources.length; i++) {
                $scope.loadDetails($scope.resources[i]);
            }
        }
    };
});

module.controller('ResourceServerResourceDetailCtrl', function($scope, $http, $route, $location, realm, ResourceServer, client, ResourceServerResource, ResourceServerScope, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    $scope.scopesUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            $scope.query = {
                realm: realm.realm,
                client : client.id,
                name: query.term.trim(),
                deep: false,
                max : 20,
                first : 0
            };
            ResourceServerScope.query($scope.query, function(response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            return object.name;
        },
        formatSelection: function(object, container, query) {
            return object.name;
        }
    };

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
                for (i = 0; i < $scope.resource.scopes.length; i++) {
                    delete $scope.resource.scopes[i].text;
                }
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

                $scope.resource = angular.copy(data);
                $scope.changed = false;

                $scope.originalResource = angular.copy($scope.resource);

                $scope.$watch('resource', function() {
                    if (!angular.equals($scope.resource, data)) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.save = function() {
                    for (i = 0; i < $scope.resource.scopes.length; i++) {
                        delete $scope.resource.scopes[i].text;
                    }
                    $instance.checkNameAvailability(function () {
                        ResourceServerResource.update({realm : realm.realm, client : $scope.client.id, rsrid : $scope.resource._id}, $scope.resource, function() {
                            $route.reload();
                            Notifications.success("The resource has been updated.");
                        });
                    });
                }

                $scope.remove = function() {
                    ResourceServerResource.permissions({
                        realm : $route.current.params.realm,
                        client : client.id,
                        rsrid : $scope.resource._id
                    }, function (permissions) {
                        var msg = "";

                        if (permissions.length > 0 && !$scope.deleteConsent) {
                            msg = "<p>This resource is referenced in some policies:</p>";
                            msg += "<ul>";
                            for (i = 0; i < permissions.length; i++) {
                                msg+= "<li><strong>" + permissions[i].name + "</strong></li>";
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
        deep: false,
        max : 20,
        first : 0
    };

    $scope.listSizes = [5, 10, 20];

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

    $scope.searchQuery = function(detailsFilter) {
        $scope.searchLoaded = false;

        ResourceServerScope.query($scope.query, function(response) {
            $scope.scopes = response;
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            if ($scope.detailsFilter) {
                $scope.showDetails();
            }
        });
    };

    $scope.loadDetails = function (scope) {
        if (scope.details) {
            scope.details.loaded = !scope.details.loaded;
            return;
        }

        scope.details = {loaded: false};

        ResourceServerScope.resources({
            realm : $route.current.params.realm,
            client : client.id,
            id : scope.id
        }, function(response) {
            scope.resources = response;
            ResourceServerScope.permissions({
                realm : $route.current.params.realm,
                client : client.id,
                id : scope.id
            }, function(response) {
                scope.policies = response;
                scope.details.loaded = true;
            });
        });
    }

    $scope.showDetails = function(item) {
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.scopes.length; i++) {
                $scope.loadDetails($scope.scopes[i]);
            }
        }
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
                    ResourceServerScope.permissions({
                        realm : $route.current.params.realm,
                        client : client.id,
                        id : $scope.scope.id
                    }, function (permissions) {
                        var msg = "";

                        if (permissions.length > 0 && !$scope.deleteConsent) {
                            msg = "<p>This scope is referenced in some policies:</p>";
                            msg += "<ul>";
                            for (i = 0; i < permissions.length; i++) {
                                msg+= "<li><strong>" + permissions[i].name + "</strong></li>";
                            }
                            msg += "</ul>";
                            msg += "<p>If you remove this scope, the policies above will be affected and will not be associated with this scope anymore.</p>";
                        }

                        AuthzDialog.confirmDeleteWithMsg($scope.scope.name, "Scope", msg, function() {
                            ResourceServerScope.delete({realm : realm.realm, client : $scope.client.id, id : $scope.scope.id}, null, function() {
                                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/scope");
                                Notifications.success("The scope has been deleted.");
                            });
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
        max: 20,
        first : 0
    };

    $scope.listSizes = [5, 10, 20];

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
            $scope.policies = data;
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            if ($scope.detailsFilter) {
                $scope.showDetails();
            }
        });
    };

    $scope.loadDetails = function (policy) {
        if (policy.details) {
            policy.details.loaded = !policy.details.loaded;
            return;
        }

        policy.details = {loaded: false};

        ResourceServerPolicy.dependentPolicies({
            realm : $route.current.params.realm,
            client : client.id,
            id : policy.id
        }, function(response) {
            policy.dependentPolicies = response;
            policy.details.loaded = true;
        });
    }

    $scope.showDetails = function(item) {
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.policies.length; i++) {
                $scope.loadDetails($scope.policies[i]);
            }
        }
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

    $scope.listSizes = [5, 10, 20];

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
            $scope.policies = data;
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            if ($scope.detailsFilter) {
                $scope.showDetails();
            }
        });
    };

    $scope.loadDetails = function (policy) {
        if (policy.details) {
            policy.details.loaded = !policy.details.loaded;
            return;
        }

        policy.details = {loaded: false};

        ResourceServerPolicy.associatedPolicies({
            realm : $route.current.params.realm,
            client : client.id,
            id : policy.id
        }, function(response) {
            policy.associatedPolicies = response;
            policy.details.loaded = true;
        });
    }

    $scope.showDetails = function(item) {
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.policies.length; i++) {
                $scope.loadDetails($scope.policies[i]);
            }
        }
    };
});

module.controller('ResourceServerPolicyDroolsDetailCtrl', function($scope, $http, $route, realm, client, PolicyController) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "rules";
        },

        onInit : function() {
            $scope.drools = {};

            $scope.resolveModules = function(policy) {
                if (!policy) {
                    policy = $scope.policy;
                }

                $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/rules/resolveModules'
                        , policy).success(function(data) {
                            $scope.drools.moduleNames = data;
                            $scope.resolveSessions();
                        });
            }

            $scope.resolveSessions = function() {
                $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/rules/resolveSessions'
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
            $scope.resourcesUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                id: function(resource){ return resource._id; },
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    $scope.query = {
                        realm: realm.realm,
                        client : client.id,
                        name: query.term.trim(),
                        deep: false,
                        max : 20,
                        first : 0
                    };
                    ResourceServerResource.query($scope.query, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    object.text = object.name;
                    return object.name;
                }
            };

            $scope.policiesUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    $scope.query = {
                        realm: realm.realm,
                        client : client.id,
                        permission: false,
                        name: query.term.trim(),
                        max : 20,
                        first : 0
                    };
                    ResourceServerPolicy.query($scope.query, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    object.text = object.name;
                    return object.name;
                }
            };

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
            policy.config.resources = {};
            ResourceServerPolicy.resources({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(resources) {
                resources[0].text = resources[0].name;
                $scope.policy.config.resources = resources[0];
            });

            policy.config.applyPolicies = [];
            ResourceServerPolicy.associatedPolicies({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(policies) {
                for (i = 0; i < policies.length; i++) {
                    policies[i].text = policies[i].name;
                    $scope.policy.config.applyPolicies.push(policies[i]);
                }
            });
        },

        onUpdate : function() {
            if ($scope.policy.config.resources && $scope.policy.config.resources._id) {
                $scope.policy.config.resources = JSON.stringify([$scope.policy.config.resources._id]);
            } else {
                delete $scope.policy.config.resources
            }

            var policies = [];

            for (i = 0; i < $scope.policy.config.applyPolicies.length; i++) {
                policies.push($scope.policy.config.applyPolicies[i].id);
            }

            $scope.policy.config.applyPolicies = JSON.stringify(policies);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.decisionStrategy = 'UNANIMOUS';
            newPolicy.config = {};
            newPolicy.config.resources = null;

            var resourceId = $location.search()['rsrid'];

            if (resourceId) {
                ResourceServerResource.get({
                    realm : $route.current.params.realm,
                    client : client.id,
                    rsrid : resourceId
                }, function(data) {
                    data.text = data.name;
                    $scope.policy.config.resources = data;
                });
            }
        },

        onCreate : function() {
            if ($scope.policy.config.resources && $scope.policy.config.resources._id) {
                $scope.policy.config.resources = JSON.stringify([$scope.policy.config.resources._id]);
            } else {
                delete $scope.policy.config.resources
            }

            var policies = [];

            for (i = 0; i < $scope.policy.config.applyPolicies.length; i++) {
                policies.push($scope.policy.config.applyPolicies[i].id);
            }

            $scope.policy.config.applyPolicies = JSON.stringify(policies);
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
            $scope.scopesUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    $scope.query = {
                        realm: realm.realm,
                        client : client.id,
                        name: query.term.trim(),
                        deep: false,
                        max : 20,
                        first : 0
                    };
                    ResourceServerScope.query($scope.query, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    object.text = object.name;
                    return object.name;
                }
            };

            $scope.resourcesUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                id: function(resource){ return resource._id; },
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    $scope.query = {
                        realm: realm.realm,
                        client : client.id,
                        name: query.term.trim(),
                        deep: false,
                        max : 20,
                        first : 0
                    };
                    ResourceServerResource.query($scope.query, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    object.text = object.name;
                    return object.name;
                }
            };

            $scope.policiesUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    $scope.query = {
                        realm: realm.realm,
                        client : client.id,
                        permission: false,
                        name: query.term.trim(),
                        max : 20,
                        first : 0
                    };
                    ResourceServerPolicy.query($scope.query, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    object.text = object.name;
                    return object.name;
                }
            };

            $scope.selectResource = function() {
                $scope.policy.config.scopes = null;
                if ($scope.policy.config.resources) {
                    ResourceServerResource.scopes({
                        realm: $route.current.params.realm,
                        client: client.id,
                        rsrid: $scope.policy.config.resources._id
                    }, function (data) {
                        $scope.policy.config.resources.scopes = data;
                    });
                }
            }
        },

        onInitUpdate : function(policy) {
            ResourceServerPolicy.resources({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(resources) {
                if (resources.length > 0) {
                    for (i = 0; i < resources.length; i++) {
                        ResourceServerResource.get({
                            realm: $route.current.params.realm,
                            client: client.id,
                            rsrid: resources[0]._id,
                        }, function (resource) {
                            ResourceServerResource.query({
                                realm: $route.current.params.realm,
                                client: client.id,
                                _id: resource._id,
                                deep: false
                            }, function (resource) {
                                resource[0].text = resource[0].name;
                                $scope.policy.config.resources = resource[0];
                                ResourceServerResource.scopes({
                                    realm: $route.current.params.realm,
                                    client: client.id,
                                    rsrid: resource[0]._id
                                }, function (scopes) {
                                    $scope.policy.config.resources.scopes = scopes;
                                });
                                ResourceServerPolicy.scopes({
                                    realm: $route.current.params.realm,
                                    client: client.id,
                                    id: policy.id
                                }, function (scopes) {
                                    $scope.policy.config.scopes = [];
                                    for (i = 0; i < scopes.length; i++) {
                                        $scope.policy.config.scopes.push(scopes[i].id);
                                    }
                                });
                            });
                        });
                    }
                } else {
                    $scope.policy.config.resources = null;
                    ResourceServerPolicy.scopes({
                        realm : $route.current.params.realm,
                        client : client.id,
                        id : policy.id
                    }, function(scopes) {
                        $scope.policy.config.scopes = [];
                        for (i = 0; i < scopes.length; i++) {
                            scopes[i].text = scopes[i].name;
                            $scope.policy.config.scopes.push(scopes[i]);
                        }
                    });
                }
            });

            policy.config.applyPolicies = [];
            ResourceServerPolicy.associatedPolicies({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(policies) {
                for (i = 0; i < policies.length; i++) {
                    policies[i].text = policies[i].name;
                    $scope.policy.config.applyPolicies.push(policies[i]);
                }
            });
        },

        onUpdate : function() {
            if ($scope.policy.config.resources != null) {
                $scope.policy.config.resources = JSON.stringify([$scope.policy.config.resources._id]);
            } else {
                $scope.policy.config.resources = JSON.stringify([""]);
            }

            var scopes = [];

            for (i = 0; i < $scope.policy.config.scopes.length; i++) {
                if ($scope.policy.config.scopes[i].id) {
                    scopes.push($scope.policy.config.scopes[i].id);
                } else {
                    scopes.push($scope.policy.config.scopes[i]);
                }
            }

            $scope.policy.config.scopes = JSON.stringify(scopes);

            var policies = [];

            for (i = 0; i < $scope.policy.config.applyPolicies.length; i++) {
                policies.push($scope.policy.config.applyPolicies[i].id);
            }

            $scope.policy.config.applyPolicies = JSON.stringify(policies);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.decisionStrategy = 'UNANIMOUS';
            newPolicy.config = {};
            newPolicy.config.resources = null;

            var scopeId = $location.search()['scpid'];

            if (scopeId) {
                ResourceServerScope.get({
                    realm: $route.current.params.realm,
                    client: client.id,
                    id: scopeId,
                }, function (data) {
                    data.text = data.name;
                    if (!$scope.policy.config.scopes) {
                        $scope.policy.config.scopes = [];
                    }
                    $scope.policy.config.scopes.push(data);
                });
            }
        },

        onCreate : function() {
            if ($scope.policy.config.resources != null) {
                $scope.policy.config.resources = JSON.stringify([$scope.policy.config.resources._id]);
            }

            var scopes = [];

            for (i = 0; i < $scope.policy.config.scopes.length; i++) {
                if ($scope.policy.config.scopes[i].id) {
                    scopes.push($scope.policy.config.scopes[i].id);
                } else {
                    scopes.push($scope.policy.config.scopes[i]);
                }
            }

            $scope.policy.config.scopes = JSON.stringify(scopes);

            var policies = [];

            for (i = 0; i < $scope.policy.config.applyPolicies.length; i++) {
                policies.push($scope.policy.config.applyPolicies[i].id);
            }

            $scope.policy.config.applyPolicies = JSON.stringify(policies);
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

            $scope.removeFromList = function(list, user) {
                for (i = 0; i < angular.copy(list).length; i++) {
                    if (user == list[i]) {
                        list.splice(i, 1);
                    }
                }
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

module.controller('ResourceServerPolicyClientDetailCtrl', function($scope, $route, realm, client, PolicyController, Client) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "client";
        },

        onInit : function() {
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
                    Client.query({realm: $route.current.params.realm, search: query.term.trim(), max: 20}, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    return object.clientId;
                }
            };

            $scope.selectedClients = [];

            $scope.selectClient = function(client) {
                if (!client || !client.id) {
                    return;
                }

                $scope.selectedClient = null;

                for (var i = 0; i < $scope.selectedClients.length; i++) {
                    if ($scope.selectedClients[i].id == client.id) {
                        return;
                    }
                }

                $scope.selectedClients.push(client);
            }

            $scope.removeFromList = function(list, index) {
                list.splice(index, 1);
            }
        },

        onInitUpdate : function(policy) {
            var selectedClients = [];

            if (policy.config.clients) {
                var clients = eval(policy.config.clients);

                for (var i = 0; i < clients.length; i++) {
                    Client.get({realm: $route.current.params.realm, client: clients[i]}, function(data) {
                        selectedClients.push(data);
                        $scope.selectedClients = angular.copy(selectedClients);
                    });
                }
            }

            $scope.$watch('selectedClients', function() {
                if (!angular.equals($scope.selectedClients, selectedClients)) {
                    $scope.changed = true;
                }
            }, true);
        },

        onUpdate : function() {
            var clients = [];

            for (var i = 0; i < $scope.selectedClients.length; i++) {
                clients.push($scope.selectedClients[i].id);
            }

            $scope.policy.config.clients = JSON.stringify(clients);
        },

        onCreate : function() {
            var clients = [];

            for (var i = 0; i < $scope.selectedClients.length; i++) {
                clients.push($scope.selectedClients[i].id);
            }

            $scope.policy.config.clients = JSON.stringify(clients);
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
            if (policy.config.dayMonth) {
                policy.config.dayMonth = parseInt(policy.config.dayMonth);
            }
            if (policy.config.dayMonthEnd) {
                policy.config.dayMonthEnd = parseInt(policy.config.dayMonthEnd);
            }
            if (policy.config.month) {
                policy.config.month = parseInt(policy.config.month);
            }
            if (policy.config.monthEnd) {
                policy.config.monthEnd = parseInt(policy.config.monthEnd);
            }
            if (policy.config.year) {
                policy.config.year = parseInt(policy.config.year);
            }
            if (policy.config.yearEnd) {
                policy.config.yearEnd = parseInt(policy.config.yearEnd);
            }
            if (policy.config.hour) {
                policy.config.hour = parseInt(policy.config.hour);
            }
            if (policy.config.hourEnd) {
                policy.config.hourEnd = parseInt(policy.config.hourEnd);
            }
            if (policy.config.minute) {
                policy.config.minute = parseInt(policy.config.minute);
            }
            if (policy.config.minuteEnd) {
                policy.config.minuteEnd = parseInt(policy.config.minuteEnd);
            }
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

    $scope.isRequired = function () {
        var policy = $scope.policy;
        if (policy.config.noa || policy.config.nbf
            || policy.config.dayMonth
            || policy.config.month
            || policy.config.year
            || policy.config.hour
            || policy.config.minute) {
            return false;
        }
        return true;
    }
});

module.controller('ResourceServerPolicyAggregateDetailCtrl', function($scope, $route, $location, realm, PolicyController, ResourceServerPolicy, client) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "aggregate";
        },

        onInit : function() {
            $scope.policiesUiSelect = {
                minimumInputLength: 1,
                delay: 500,
                allowClear: true,
                query: function (query) {
                    var data = {results: []};
                    if ('' == query.term.trim()) {
                        query.callback(data);
                        return;
                    }
                    $scope.query = {
                        realm: realm.realm,
                        client : client.id,
                        permission: false,
                        name: query.term.trim(),
                        max : 20,
                        first : 0
                    };
                    ResourceServerPolicy.query($scope.query, function(response) {
                        data.results = response;
                        query.callback(data);
                    });
                },
                formatResult: function(object, container, query) {
                    object.text = object.name;
                    return object.name;
                }
            };
        },

        onInitUpdate : function(policy) {
            policy.config.applyPolicies = [];
            ResourceServerPolicy.associatedPolicies({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(policies) {
                for (i = 0; i < policies.length; i++) {
                    policies[i].text = policies[i].name;
                    $scope.policy.config.applyPolicies.push(policies[i]);
                }
            });
        },

        onUpdate : function() {
            var policies = [];

            for (i = 0; i < $scope.policy.config.applyPolicies.length; i++) {
                policies.push($scope.policy.config.applyPolicies[i].id);
            }

            $scope.policy.config.applyPolicies = JSON.stringify(policies);
        },

        onInitCreate : function(newPolicy) {
            newPolicy.config = {};
            newPolicy.decisionStrategy = 'UNANIMOUS';
        },

        onCreate : function() {
            var policies = [];

            for (i = 0; i < $scope.policy.config.applyPolicies.length; i++) {
                policies.push($scope.policy.config.applyPolicies[i].id);
            }

            $scope.policy.config.applyPolicies = JSON.stringify(policies);
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
                    realm: realm.realm,
                    client : client.id,
                    id: $route.current.params.id
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

                    ResourceServerPolicy.dependentPolicies({
                        realm : $route.current.params.realm,
                        client : client.id,
                        id : $scope.policy.id
                    }, function (dependentPolicies) {
                        if (dependentPolicies.length > 0 && !$scope.deleteConsent) {
                            msg = "<p>This policy is being used by other policies:</p>";
                            msg += "<ul>";
                            for (i = 0; i < dependentPolicies.length; i++) {
                                msg+= "<li><strong>" + dependentPolicies[i].name + "</strong></li>";
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
    $scope.resultUrl = resourceUrl + '/partials/authz/policy/resource-server-policy-evaluate-result.html';

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
        delete $scope.newResource;
        $scope.authzRequest.resources = [];
    }

    $scope.addResource = function() {
        var resource = angular.copy($scope.newResource);

        if (!resource) {
            resource = {};
        }

        delete resource.text;

        if (!$scope.newScopes || (resource._id != null && $scope.newScopes.length > 0 && $scope.newScopes[0].id)) {
            $scope.newScopes = [];
        }

        var scopes = [];

        for (i = 0; i < $scope.newScopes.length; i++) {
            if ($scope.newScopes[i].name) {
                scopes.push($scope.newScopes[i].name);
            } else {
                scopes.push($scope.newScopes[i]);
            }
        }

        resource.scopes = scopes;

        $scope.authzRequest.resources.push(resource);

        delete $scope.newResource;
        delete $scope.newScopes;
    }

    $scope.removeResource = function(index) {
        $scope.authzRequest.resources.splice(index, 1);
    }

    $scope.resolveScopes = function() {
        if ($scope.newResource._id) {
            $scope.newResource.scopes = [];
            $scope.scopes = [];
            ResourceServerResource.scopes({
                realm: $route.current.params.realm,
                client: client.id,
                rsrid: $scope.newResource._id
            }, function (data) {
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
            if (!$scope.newScopes || ($scope.newResource._id != null && $scope.newScopes.length > 0 && $scope.newScopes[0].id)) {
                $scope.newScopes = [];
            }

            var scopes = angular.copy($scope.newScopes);

            for (i = 0; i < scopes.length; i++) {
                delete scopes[i].text;
            }

            $scope.authzRequest.resources[0].scopes = scopes;
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

    $scope.resourcesUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        id: function(resource){ return resource._id; },
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            $scope.query = {
                realm: realm.realm,
                client : client.id,
                name: query.term.trim(),
                deep: false,
                max : 20,
                first : 0
            };
            ResourceServerResource.query($scope.query, function(response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            object.text = object.name;
            return object.name;
        }
    };

    $scope.scopesUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            $scope.query = {
                realm: realm.realm,
                client : client.id,
                name: query.term.trim(),
                deep: false,
                max : 20,
                first : 0
            };
            ResourceServerScope.query($scope.query, function(response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            object.text = object.name;
            return object.name;
        }
    };

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