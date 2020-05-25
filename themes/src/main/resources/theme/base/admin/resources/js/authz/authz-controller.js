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

var Resources = {
    delete: function(ResourceServerResource, realm, client, $scope, AuthzDialog, $location, Notifications, $route) {
        ResourceServerResource.permissions({
            realm : realm,
            client : client.id,
            rsrid : $scope.resource._id
        }, function (permissions) {
            var msg = "";

            if (permissions.length > 0 && !$scope.deleteConsent) {
                msg = "<p>This resource is referenced in some permissions:</p>";
                msg += "<ul>";
                for (i = 0; i < permissions.length; i++) {
                    msg+= "<li><strong>" + permissions[i].name + "</strong></li>";
                }
                msg += "</ul>";
                msg += "<p>If you remove this resource, the permissions above will be affected and will not be associated with this resource anymore.</p>";
            }

            AuthzDialog.confirmDeleteWithMsg($scope.resource.name, "Resource", msg, function() {
                ResourceServerResource.delete({realm : realm, client : $scope.client.id, rsrid : $scope.resource._id}, null, function() {
                    $location.url("/realms/" + realm + "/clients/" + $scope.client.id + "/authz/resource-server/resource");
                    $route.reload();
                    Notifications.success("The resource has been deleted.");
                });
            });
        });
    }
}

var Policies = {
    delete: function(service, realm, client, $scope, AuthzDialog, $location, Notifications, $route, isPermission) {
        var msg = "";

        service.dependentPolicies({
            realm : realm,
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

            AuthzDialog.confirmDeleteWithMsg($scope.policy.name, isPermission ? "Permission" : "Policy", msg, function() {
                service.delete({realm : realm, client : $scope.client.id, id : $scope.policy.id}, null, function() {
                    if (isPermission) {
                        $location.url("/realms/" + realm + "/clients/" + $scope.client.id + "/authz/resource-server/permission");
                        Notifications.success("The permission has been deleted.");
                    } else {
                        $location.url("/realms/" + realm + "/clients/" + $scope.client.id + "/authz/resource-server/policy");
                        Notifications.success("The policy has been deleted.");
                    }
                    $route.reload();
                });
            });
        });
    }
}

module.controller('ResourceServerResourceCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerResource, client, AuthzDialog, Notifications, viewState) {
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
            viewState.state = {};
            viewState.state.previousUrl = '/realms/' + $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/resource';
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

    $scope.showDetails = function(item, event) {
        if (event.target.localName == 'a' || event.target.localName == 'button') {
            return;
        }

        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.resources.length; i++) {
                $scope.loadDetails($scope.resources[i]);
            }
        }
    };

    $scope.delete = function(resource) {
        $scope.resource = resource;
        Resources.delete(ResourceServerResource, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route);
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
            resource.attributes = {};
            resource.uris = [];

            $scope.resource = angular.copy(resource);

            $scope.$watch('resource', function() {
                if (!angular.equals($scope.resource, resource)) {
                    $scope.changed = true;
                }
            }, true);

            $scope.$watch('newUri', function() {
                if ($scope.newUri && $scope.newUri.length > 0) {
                    $scope.changed = true;
                }
            }, true);

            $scope.save = function() {
                if ($scope.newUri && $scope.newUri.length > 0) {
                    $scope.addUri();
                }

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

            $scope.reset = function() {
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

                if (!data.attributes) {
                    data.attributes = {};
                }

                $scope.resource = angular.copy(data);
                $scope.changed = false;

                $scope.originalResource = angular.copy($scope.resource);

                $scope.$watch('resource', function() {
                    if (!angular.equals($scope.resource, data)) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.$watch('newUri', function() {
                    if ($scope.newUri && $scope.newUri.length > 0) {
                        $scope.changed = true;
                    }
                }, true);

                $scope.save = function() {
                    if ($scope.newUri && $scope.newUri.length > 0) {
                        $scope.addUri();
                    }

                    for (i = 0; i < $scope.resource.scopes.length; i++) {
                        delete $scope.resource.scopes[i].text;
                    }

                    var keys = Object.keys($scope.resource.attributes);

                    for (var k = 0; k < keys.length; k++) {
                        var key = keys[k];
                        var value = $scope.resource.attributes[key];
                        var values = value.toString().split(',');

                        $scope.resource.attributes[key] = [];

                        for (j = 0; j < values.length; j++) {
                            $scope.resource.attributes[key].push(values[j]);
                        }
                    }
                    $instance.checkNameAvailability(function () {
                        ResourceServerResource.update({realm : realm.realm, client : $scope.client.id, rsrid : $scope.resource._id}, $scope.resource, function() {
                            $route.reload();
                            Notifications.success("The resource has been updated.");
                        });
                    });
                }

                $scope.remove = function() {
                    Resources.delete(ResourceServerResource, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route);
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
        if (!$scope.resource.name || $scope.resource.name.trim().length == 0) {
            return;
        }
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

    $scope.addAttribute = function() {
        $scope.resource.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
    }

    $scope.removeAttribute = function(key) {
        delete $scope.resource.attributes[key];
    }

    $scope.addUri = function() {
        $scope.resource.uris.push($scope.newUri);
        $scope.newUri = "";
    }

    $scope.deleteUri = function(index) {
        $scope.resource.uris.splice(index, 1);
    }
});

var Scopes = {
    delete: function(ResourceServerScope, realm, client, $scope, AuthzDialog, $location, Notifications, $route) {
        ResourceServerScope.permissions({
            realm : realm,
            client : client.id,
            id : $scope.scope.id
        }, function (permissions) {
            var msg = "";

            if (permissions.length > 0 && !$scope.deleteConsent) {
                msg = "<p>This scope is referenced in some permissions:</p>";
                msg += "<ul>";
                for (i = 0; i < permissions.length; i++) {
                    msg+= "<li><strong>" + permissions[i].name + "</strong></li>";
                }
                msg += "</ul>";
                msg += "<p>If you remove this scope, the permissions above will be affected and will not be associated with this scope anymore.</p>";
            }

            AuthzDialog.confirmDeleteWithMsg($scope.scope.name, "Scope", msg, function() {
                ResourceServerScope.delete({realm : realm, client : $scope.client.id, id : $scope.scope.id}, null, function() {
                    $location.url("/realms/" + realm + "/clients/" + $scope.client.id + "/authz/resource-server/scope");
                    $route.reload();
                    Notifications.success("The scope has been deleted.");
                });
            });
        });
    }
}

module.controller('ResourceServerScopeCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerScope,client, AuthzDialog, Notifications, viewState) {
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
            viewState.state = {};
            viewState.state.previousUrl = '/realms/' + $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/scope';
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

    $scope.showDetails = function(item, event) {
        if (event.target.localName == 'a' || event.target.localName == 'button') {
            return;
        }
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.scopes.length; i++) {
                $scope.loadDetails($scope.scopes[i]);
            }
        }
    };

    $scope.delete = function(scope) {
        $scope.scope = scope;
        Scopes.delete(ResourceServerScope, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route);
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

            $scope.reset = function() {
                $location.url("/realms/" + realm.realm + "/clients/" + $scope.client.id + "/authz/resource-server/scope/");
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
                    Scopes.delete(ResourceServerScope, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route);
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
        if (!$scope.scope.name || $scope.scope.name.trim().length == 0) {
            return;
        }
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

module.controller('ResourceServerPolicyCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerPolicy, PolicyProvider, client, AuthzDialog, Notifications, KcStrings) {
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
        if (KcStrings.endsWith(policyType.type, '.js')) {
            ResourceServerPolicy.save({realm : realm.realm, client : client.id, type: policyType.type}, {name: policyType.name, type: policyType.type}, function(data) {
                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/");
                Notifications.success("The policy has been created.");
            });
        } else {
            $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + policyType.type + "/create");
        }
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

    $scope.showDetails = function(item, event) {
        if (event.target.localName == 'a' || event.target.localName == 'button') {
            return;
        }
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.policies.length; i++) {
                $scope.loadDetails($scope.policies[i]);
            }
        }
    };

    $scope.delete = function(policy) {
        $scope.policy = policy;
        Policies.delete(ResourceServerPolicy, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route, false);
    };
});

module.controller('ResourceServerPermissionCtrl', function($scope, $http, $route, $location, realm, ResourceServer, ResourceServerPermission, PolicyProvider, client, AuthzDialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.policyProviders = [];

    $scope.query = {
        realm: realm.realm,
        client : client.id,
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

        ResourceServerPermission.query($scope.query, function(data) {
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

        ResourceServerPermission.associatedPolicies({
            realm : $route.current.params.realm,
            client : client.id,
            id : policy.id
        }, function(response) {
            policy.associatedPolicies = response;
            policy.details.loaded = true;
        });
    }

    $scope.showDetails = function(item, event) {
        if (event.target.localName == 'a' || event.target.localName == 'button') {
            return;
        }
        if (item) {
            $scope.loadDetails(item);
        } else {
            for (i = 0; i < $scope.policies.length; i++) {
                $scope.loadDetails($scope.policies[i]);
            }
        }
    };

    $scope.delete = function(policy) {
        $scope.policy = policy;
        Policies.delete(ResourceServerPermission, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route, true);
    };
});

module.controller('ResourceServerPolicyResourceDetailCtrl', function($scope, $route, $location, realm, client, PolicyController, ResourceServerPermission, ResourceServerResource, policyViewState) {
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
                    ResourceServerPermission.searchPolicies($scope.query, function(response) {
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
                if ($scope.applyToResourceTypeFlag) {
                    $scope.selectedResource = null;
                } else {
                    $scope.policy.resourceType = null;
                }
            }
        },

        onInitUpdate : function(policy) {
            if (!policy.resourceType) {
                $scope.selectedResource = {};
                ResourceServerPermission.resources({
                    realm: $route.current.params.realm,
                    client: client.id,
                    id: policy.id
                }, function (resources) {
                    resources[0].text = resources[0].name;
                    $scope.selectedResource = resources[0];
                    var copy = angular.copy($scope.selectedResource);
                    $scope.$watch('selectedResource', function() {
                        if (!angular.equals($scope.selectedResource, copy)) {
                            $scope.changed = true;
                        }
                    }, true);
                });
            } else {
                $scope.applyToResourceTypeFlag = true;
            }

            ResourceServerPermission.associatedPolicies({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(policies) {
                $scope.selectedPolicies = [];
                for (i = 0; i < policies.length; i++) {
                    policies[i].text = policies[i].name;
                    $scope.selectedPolicies.push(policies[i]);
                }
                var copy = angular.copy($scope.selectedPolicies);
                $scope.$watch('selectedPolicies', function() {
                    if (!angular.equals($scope.selectedPolicies, copy)) {
                        $scope.changed = true;
                    }
                }, true);
            });
        },

        onUpdate : function() {
            if ($scope.selectedResource && $scope.selectedResource._id) {
                $scope.policy.resources = [];
                $scope.policy.resources.push($scope.selectedResource._id);
            } else {
                $scope.policy.resources = [];
            }

            var policies = [];

            for (i = 0; i < $scope.selectedPolicies.length; i++) {
                policies.push($scope.selectedPolicies[i].id);
            }

            $scope.policy.policies = policies;
            delete $scope.policy.config;
        },

        onInitCreate : function(newPolicy) {
            policyViewState.state.previousPage.name = 'authz-add-resource-permission';
            $scope.selectedResource = null;
            var copy = angular.copy($scope.selectedResource);
            $scope.$watch('selectedResource', function() {
                if (!angular.equals($scope.selectedResource, copy)) {
                    $scope.changed = true;
                }
            }, true);

            $scope.selectedPolicies = null;
            var copy = angular.copy($scope.selectedPolicies);
            $scope.$watch('selectedPolicies', function() {
                if (!angular.equals($scope.selectedPolicies, copy)) {
                    $scope.changed = true;
                }
            }, true);

            var resourceId = $location.search()['rsrid'];

            if (resourceId) {
                ResourceServerResource.get({
                    realm : $route.current.params.realm,
                    client : client.id,
                    rsrid : resourceId
                }, function(data) {
                    data.text = data.name;
                    $scope.selectedResource = data;
                });
            }
        },

        onCreate : function() {
            if ($scope.selectedResource && $scope.selectedResource._id) {
                $scope.policy.resources = [];
                $scope.policy.resources.push($scope.selectedResource._id);
            } else {
                delete $scope.policy.resources
            }

            var policies = [];

            if ($scope.selectedPolicies) {
                for (i = 0; i < $scope.selectedPolicies.length; i++) {
                    policies.push($scope.selectedPolicies[i].id);
                }
            }

            $scope.policy.policies = policies;
            delete $scope.policy.config;
        },

        onSaveState : function(policy) {
            policyViewState.state.selectedResource = $scope.selectedResource;
            policyViewState.state.applyToResourceTypeFlag = $scope.applyToResourceTypeFlag;
        },

        onRestoreState : function(policy) {
            $scope.selectedResource = policyViewState.state.selectedResource;
            $scope.applyToResourceTypeFlag = policyViewState.state.applyToResourceTypeFlag;
            policy.resourceType = policyViewState.state.policy.resourceType;
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyScopeDetailCtrl', function($scope, $route, $location, realm, client, PolicyController, ResourceServerPolicy, ResourceServerResource, ResourceServerScope, policyViewState) {
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
                $scope.selectedScopes = null;
                if ($scope.selectedResource) {
                    ResourceServerResource.scopes({
                        realm: $route.current.params.realm,
                        client: client.id,
                        rsrid: $scope.selectedResource._id
                    }, function (data) {
                        $scope.resourceScopes = data;
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
                                $scope.selectedResource = resource[0];
                                var copy = angular.copy($scope.selectedResource);
                                $scope.$watch('selectedResource', function() {
                                    if (!angular.equals($scope.selectedResource, copy)) {
                                        $scope.changed = true;
                                    }
                                }, true);
                                ResourceServerResource.scopes({
                                    realm: $route.current.params.realm,
                                    client: client.id,
                                    rsrid: resource[0]._id
                                }, function (scopes) {
                                    $scope.resourceScopes = scopes;
                                });
                            });
                        });
                    }

                    ResourceServerPolicy.scopes({
                        realm : $route.current.params.realm,
                        client : client.id,
                        id : policy.id
                    }, function(scopes) {
                        $scope.selectedScopes = [];
                        for (i = 0; i < scopes.length; i++) {
                            scopes[i].text = scopes[i].name;
                            $scope.selectedScopes.push(scopes[i].id);
                        }
                        var copy = angular.copy($scope.selectedScopes);
                        $scope.$watch('selectedScopes', function() {
                            if (!angular.equals($scope.selectedScopes, copy)) {
                                $scope.changed = true;
                            }
                        }, true);
                    });
                } else {
                    $scope.selectedResource = null;
                    var copy = angular.copy($scope.selectedResource);
                    $scope.$watch('selectedResource', function() {
                        if (!angular.equals($scope.selectedResource, copy)) {
                            $scope.changed = true;
                        }
                    }, true);
                    ResourceServerPolicy.scopes({
                        realm : $route.current.params.realm,
                        client : client.id,
                        id : policy.id
                    }, function(scopes) {
                        $scope.selectedScopes = [];
                        for (i = 0; i < scopes.length; i++) {
                            scopes[i].text = scopes[i].name;
                            $scope.selectedScopes.push(scopes[i]);
                        }
                        var copy = angular.copy($scope.selectedScopes);
                        $scope.$watch('selectedScopes', function() {
                            if (!angular.equals($scope.selectedScopes, copy)) {
                                $scope.changed = true;
                            }
                        }, true);
                    });
                }
            });

            ResourceServerPolicy.associatedPolicies({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(policies) {
                $scope.selectedPolicies = [];
                for (i = 0; i < policies.length; i++) {
                    policies[i].text = policies[i].name;
                    $scope.selectedPolicies.push(policies[i]);
                }
                var copy = angular.copy($scope.selectedPolicies);
                $scope.$watch('selectedPolicies', function() {
                    if (!angular.equals($scope.selectedPolicies, copy)) {
                        $scope.changed = true;
                    }
                }, true);
            });
        },

        onUpdate : function() {
            if ($scope.selectedResource != null) {
                $scope.policy.resources = [$scope.selectedResource._id];
            } else {
                $scope.policy.resources = [];
            }

            var scopes = [];

            for (i = 0; i < $scope.selectedScopes.length; i++) {
                if ($scope.selectedScopes[i].id) {
                    scopes.push($scope.selectedScopes[i].id);
                } else {
                    scopes.push($scope.selectedScopes[i]);
                }
            }

            $scope.policy.scopes = scopes;

            var policies = [];

            if ($scope.selectedPolicies) {
                for (i = 0; i < $scope.selectedPolicies.length; i++) {
                    policies.push($scope.selectedPolicies[i].id);
                }
            }

            $scope.policy.policies = policies;
            delete $scope.policy.config;
        },

        onInitCreate : function(newPolicy) {
            policyViewState.state.previousPage.name = 'authz-add-scope-permission';
            var scopeId = $location.search()['scpid'];

            if (scopeId) {
                ResourceServerScope.get({
                    realm: $route.current.params.realm,
                    client: client.id,
                    id: scopeId,
                }, function (data) {
                    data.text = data.name;
                    if (!$scope.policy.scopes) {
                        $scope.selectedScopes = [];
                    }
                    $scope.selectedScopes.push(data);
                });
            }
        },

        onCreate : function() {
            if ($scope.selectedResource != null) {
                $scope.policy.resources = [$scope.selectedResource._id];
            }

            var scopes = [];

            for (i = 0; i < $scope.selectedScopes.length; i++) {
                if ($scope.selectedScopes[i].id) {
                    scopes.push($scope.selectedScopes[i].id);
                } else {
                    scopes.push($scope.selectedScopes[i]);
                }
            }

            $scope.policy.scopes = scopes;

            var policies = [];

            if ($scope.selectedPolicies) {
                for (i = 0; i < $scope.selectedPolicies.length; i++) {
                    policies.push($scope.selectedPolicies[i].id);
                }
            }

            $scope.policy.policies = policies;
            delete $scope.policy.config;
        },

        onSaveState : function(policy) {
            policyViewState.state.selectedScopes = $scope.selectedScopes;
            policyViewState.state.selectedResource = $scope.selectedResource;
            policyViewState.state.resourceScopes = $scope.resourceScopes;
        },

        onRestoreState : function(policy) {
            $scope.selectedScopes = policyViewState.state.selectedScopes;
            $scope.selectedResource = policyViewState.state.selectedResource;
            $scope.resourceScopes = policyViewState.state.resourceScopes;
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

            if (policy.users) {
                var users = policy.users;

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
                } else {
                    $scope.changed = false;
                }
            }, true);
        },

        onUpdate : function() {
            var users = [];

            for (i = 0; i < $scope.selectedUsers.length; i++) {
                users.push($scope.selectedUsers[i].id);
            }

            $scope.policy.users = users;
            delete $scope.policy.config;
        },

        onCreate : function() {
            var users = [];

            for (i = 0; i < $scope.selectedUsers.length; i++) {
                users.push($scope.selectedUsers[i].id);
            }

            $scope.policy.users = users;
            delete $scope.policy.config;
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyClientDetailCtrl', function($scope, $route, realm, client, PolicyController, Client) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "client";
        },

        onInit : function() {
            clientSelectControl($scope, $route.current.params.realm, Client);

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

            $scope.removeFromList = function(client) {
                var index = $scope.selectedClients.indexOf(client);
                if (index != -1) {
                    $scope.selectedClients.splice(index, 1);
                }
            }
        },

        onInitUpdate : function(policy) {
            var selectedClients = [];

            if (policy.clients) {
                var clients = policy.clients;

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
                } else {
                    $scope.changed = false;
                }
            }, true);
        },

        onUpdate : function() {
            var clients = [];

            for (var i = 0; i < $scope.selectedClients.length; i++) {
                clients.push($scope.selectedClients[i].id);
            }

            $scope.policy.clients = clients;
            delete $scope.policy.config;
        },

        onInitCreate : function() {
            var selectedClients = [];

            $scope.$watch('selectedClients', function() {
                if (!angular.equals($scope.selectedClients, selectedClients)) {
                    $scope.changed = true;
                }
            }, true);
        },

        onCreate : function() {
            var clients = [];

            for (var i = 0; i < $scope.selectedClients.length; i++) {
                clients.push($scope.selectedClients[i].id);
            }

            $scope.policy.clients = clients;
            delete $scope.policy.config;
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

            if (policy.roles) {
                var roles = policy.roles;

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
                } else {
                    $scope.changed = false;
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

            $scope.policy.roles = roles;
            delete $scope.policy.config;
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

            $scope.policy.roles = roles;
            delete $scope.policy.config;
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

module.controller('ResourceServerPolicyGroupDetailCtrl', function($scope, $route, realm, client, Client, Groups, Group, PolicyController, Notifications, $translate) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "group";
        },

        onInit : function() {
            $scope.tree = [];

            Groups.query({realm: $route.current.params.realm}, function(groups) {
                $scope.groups = groups;
                $scope.groupList = [
                    {"id" : "realm", "name": $translate.instant('groups'),
                                "subGroups" : groups}
                ];
            });

            var isLeaf = function(node) {
                return node.id != "realm" && (!node.subGroups || node.subGroups.length == 0);
            }

            $scope.getGroupClass = function(node) {
                if (node.id == "realm") {
                    return 'pficon pficon-users';
                }
                if (isLeaf(node)) {
                    return 'normal';
                }
                if (node.subGroups.length && node.collapsed) return 'collapsed';
                if (node.subGroups.length && !node.collapsed) return 'expanded';
                return 'collapsed';

            }

            $scope.getSelectedClass = function(node) {
                if (node.selected) {
                    return 'selected';
                } else if ($scope.cutNode && $scope.cutNode.id == node.id) {
                    return 'cut';
                }
                return undefined;
            }

            $scope.selectGroup = function(group) {
                for (i = 0; i < $scope.selectedGroups.length; i++) {
                    if ($scope.selectedGroups[i].id == group.id) {
                        return
                    }
                }
                if (group.id == "realm") {
                    Notifications.error("You must choose a group");
                    return;
                }
                $scope.selectedGroups.push({id: group.id, path: group.path});
                $scope.changed = true;
            }

            $scope.extendChildren = function(group) {
                $scope.changed = true;
            }

            $scope.removeFromList = function(group) {
                var index = $scope.selectedGroups.indexOf(group);
                if (index != -1) {
                    $scope.selectedGroups.splice(index, 1);
                    $scope.changed = true;
                }
            }
        },

        onInitCreate : function(policy) {
            var selectedGroups = [];

            $scope.selectedGroups = angular.copy(selectedGroups);

            $scope.$watch('selectedGroups', function() {
                if (!angular.equals($scope.selectedGroups, selectedGroups)) {
                    $scope.changed = true;
                } else {
                    $scope.changed = PolicyController.isNewAssociatedPolicy();
                }
            }, true);
        },

        onInitUpdate : function(policy) {
            $scope.selectedGroups = policy.groups;

            angular.forEach($scope.selectedGroups, function(group, index){
               Group.get({realm: $route.current.params.realm, groupId: group.id}, function (existing) {
                   group.path = existing.path;
               });
            });

            $scope.$watch('selectedGroups', function() {
                if (!$scope.changed) {
                    return;
                }
                if (!angular.equals($scope.selectedGroups, selectedGroups)) {
                    $scope.changed = true;
                } else {
                    $scope.changed = false;
                }
            }, true);
        },

        onUpdate : function() {
            $scope.policy.groups = $scope.selectedGroups;
            delete $scope.policy.config;
        },

        onCreate : function() {
            $scope.policy.groups = $scope.selectedGroups;
            delete $scope.policy.config;
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyJSDetailCtrl', function($scope, $route, $location, realm, PolicyController, client, serverInfo) {
    PolicyController.onInit({
        getPolicyType : function() {
            return "js";
        },

        onInit : function() {
            $scope.readOnly = !serverInfo.featureEnabled('UPLOAD_SCRIPTS');
            $scope.initEditor = function(editor){
                editor.$blockScrolling = Infinity;
                editor.setReadOnly($scope.readOnly);
                var session = editor.getSession();
                session.setMode('ace/mode/javascript');
            };
        },

        onInitUpdate : function(policy) {

        },

        onUpdate : function() {
            delete $scope.policy.config;
        },

        onInitCreate : function(newPolicy) {
        },

        onCreate : function() {
            delete $scope.policy.config;
        }
    }, realm, client, $scope);
});

module.controller('ResourceServerPolicyTimeDetailCtrl', function($scope, $route, $location, realm, PolicyController, client) {

    function clearEmptyStrings() {
        if ($scope.policy.notBefore != undefined && $scope.policy.notBefore.trim() == '') {
            $scope.policy.notBefore = null;
        }
        if ($scope.policy.notOnOrAfter != undefined && $scope.policy.notOnOrAfter.trim() == '') {
            $scope.policy.notOnOrAfter = null;
        }
    }

    PolicyController.onInit({
        getPolicyType : function() {
            return "time";
        },

        onInit : function() {

        },

        onInitUpdate : function(policy) {
            if (policy.dayMonth) {
                policy.dayMonth = parseInt(policy.dayMonth);
            }
            if (policy.dayMonthEnd) {
                policy.dayMonthEnd = parseInt(policy.dayMonthEnd);
            }
            if (policy.month) {
                policy.month = parseInt(policy.month);
            }
            if (policy.monthEnd) {
                policy.monthEnd = parseInt(policy.monthEnd);
            }
            if (policy.year) {
                policy.year = parseInt(policy.year);
            }
            if (policy.yearEnd) {
                policy.yearEnd = parseInt(policy.yearEnd);
            }
            if (policy.hour) {
                policy.hour = parseInt(policy.hour);
            }
            if (policy.hourEnd) {
                policy.hourEnd = parseInt(policy.hourEnd);
            }
            if (policy.minute) {
                policy.minute = parseInt(policy.minute);
            }
            if (policy.minuteEnd) {
                policy.minuteEnd = parseInt(policy.minuteEnd);
            }
        },

        onUpdate : function() {
            clearEmptyStrings();
            delete $scope.policy.config;
        },

        onInitCreate : function(newPolicy) {
        },

        onCreate : function() {
            clearEmptyStrings();
            delete $scope.policy.config;
        }
    }, realm, client, $scope);

    $scope.isRequired = function () {
        var policy = $scope.policy;

        if (!policy) {
            return true;
        }

        if (policy.notOnOrAfter || policy.notBefore
            || policy.dayMonth
            || policy.month
            || policy.year
            || policy.hour
            || policy.minute) {
            return false;
        }
        return true;
    }
});

module.controller('ResourceServerPolicyAggregateDetailCtrl', function($scope, $route, $location, realm, PolicyController, ResourceServerPolicy, client, PolicyProvider, policyViewState) {
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
            ResourceServerPolicy.associatedPolicies({
                realm : $route.current.params.realm,
                client : client.id,
                id : policy.id
            }, function(policies) {
                $scope.selectedPolicies = [];
                for (i = 0; i < policies.length; i++) {
                    policies[i].text = policies[i].name;
                    $scope.selectedPolicies.push(policies[i]);
                }
                var copy = angular.copy($scope.selectedPolicies);
                $scope.$watch('selectedPolicies', function() {
                    if (!angular.equals($scope.selectedPolicies, copy)) {
                        $scope.changed = true;
                    }
                }, true);
            });
        },

        onUpdate : function() {
            var policies = [];

            for (i = 0; i < $scope.selectedPolicies.length; i++) {
                policies.push($scope.selectedPolicies[i].id);
            }

            $scope.policy.policies = policies;
            delete $scope.policy.config;
        },

        onInitCreate : function(newPolicy) {
            policyViewState.state.previousPage.name = 'authz-add-aggregated-policy';
        },

        onCreate : function() {
            var policies = [];

            for (i = 0; i < $scope.selectedPolicies.length; i++) {
                policies.push($scope.selectedPolicies[i].id);
            }

            $scope.policy.policies = policies;
            delete $scope.policy.config;
        }
    }, realm, client, $scope);
});

module.service("PolicyController", function($http, $route, $location, ResourceServer, ResourceServerPolicy, ResourceServerPermission, AuthzDialog, Notifications, policyViewState, PolicyProvider, viewState) {

    var PolicyController = {};

    PolicyController.isNewAssociatedPolicy = function() {
        return $route.current.params['new_policy'] != null;
    }

    PolicyController.isBackNewAssociatedPolicy = function() {
        return $route.current.params['back'] != null;
    }

    PolicyController.onInit = function(delegate, realm, client, $scope) {
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

        if ((!policyViewState.state || !PolicyController.isBackNewAssociatedPolicy()) && !PolicyController.isNewAssociatedPolicy()) {
            policyViewState.state = {};
        }

        if (!policyViewState.state.previousPage) {
            policyViewState.state.previousPage = {};
        }

        $scope.policyViewState = policyViewState;

        $scope.addPolicy = function(policyType) {
            policyViewState.state.policy = $scope.policy;

            if (delegate.onSaveState) {
               delegate.onSaveState($scope.policy);
            }

            if ($scope.selectedPolicies) {
                policyViewState.state.selectedPolicies = $scope.selectedPolicies;
            }
            var previousUrl = window.location.href.substring(window.location.href.indexOf('/realms'));

            if (previousUrl.indexOf('back=true') == -1) {
                previousUrl = previousUrl + (previousUrl.indexOf('?') == -1 ? '?' : '&') + 'back=true';
            }
            policyViewState.state.previousUrl = previousUrl;
            $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + policyType.type + "/create?new_policy=true");
        }

        $scope.detailPolicy = function(policy) {
            policyViewState.state.policy = $scope.policy;
            if (delegate.onSaveState) {
               delegate.onSaveState($scope.policy);
            }
            if ($scope.selectedPolicies) {
                policyViewState.state.selectedPolicies = $scope.selectedPolicies;
            }
            var previousUrl = window.location.href.substring(window.location.href.indexOf('/realms'));

            if (previousUrl.indexOf('back=true') == -1) {
                previousUrl = previousUrl + (previousUrl.indexOf('?') == -1 ? '?' : '&') + 'back=true';
            }
            policyViewState.state.previousUrl = previousUrl;
            $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + policy.type + "/" + policy.id + "?new_policy=true");
        }

        $scope.removePolicy = function(list, policy) {
            for (i = 0; i < angular.copy(list).length; i++) {
                if (policy.id == list[i].id) {
                    list.splice(i, 1);
                }
            }
        }

        $scope.selectPolicy = function(policy) {
            if (!policy || !policy.id) {
                return;
            }

            if (!$scope.selectedPolicies) {
                $scope.selectedPolicies = [];
            }

            $scope.selectedPolicy = null;

            for (i = 0; i < $scope.selectedPolicies.length; i++) {
                if ($scope.selectedPolicies[i].id == policy.id) {
                    return;
                }
            }

            $scope.selectedPolicies.push(policy);
        }

        $scope.createNewPolicy = function() {
            $scope.showNewPolicy = true;
        }

        $scope.cancelCreateNewPolicy = function() {
            $scope.showNewPolicy = false;
        }

        $scope.historyBackOnSaveOrCancel = PolicyController.isNewAssociatedPolicy();

        if (!delegate.isPermission) {
            delegate.isPermission = function () {
                return false;
            }
        }

        var service = ResourceServerPolicy;

        if (delegate.isPermission()) {
            service = ResourceServerPermission;
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

                var policy = {};

                policy.type = delegate.getPolicyType();
                policy.config = {};
                policy.logic = 'POSITIVE';
                policy.decisionStrategy = 'UNANIMOUS';

                $scope.changed = $scope.historyBackOnSaveOrCancel || PolicyController.isBackNewAssociatedPolicy();

                if (viewState.state != null && viewState.state.previousUrl != null) {
                    $scope.previousUrl = viewState.state.previousUrl;
                    policyViewState.state.rootUrl = $scope.previousUrl;
                    viewState.state = {};
                }

                $scope.policy = angular.copy(policy);

                $scope.$watch('policy', function() {
                    if (!angular.equals($scope.policy, policy)) {
                        $scope.changed = true;
                    }
                }, true);

                if (PolicyController.isBackNewAssociatedPolicy()) {
                    if (delegate.onRestoreState) {
                        delegate.onRestoreState($scope.policy);
                    }
                    $instance.restoreState($scope);
                } else if (delegate.onInitCreate) {
                   delegate.onInitCreate(policy);
                }

                $scope.save = function() {
                    $instance.checkNameAvailability(function () {
                        if (delegate.onCreate) {
                            delegate.onCreate();
                        }
                        service.save({realm : realm.realm, client : client.id, type: $scope.policy.type}, $scope.policy, function(data) {
                            if (delegate.isPermission()) {
                                if ($scope.historyBackOnSaveOrCancel || policyViewState.state.rootUrl != null) {
                                    if (policyViewState.state.rootUrl != null) {
                                        $location.url(policyViewState.state.rootUrl);
                                    } else {
                                        policyViewState.state.newPolicyName = $scope.policy.name;
                                        $location.url(policyViewState.state.previousUrl);
                                    }
                                } else {
                                    $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/" + $scope.policy.type + "/" + data.id);
                                }
                                Notifications.success("The permission has been created.");
                            } else {
                                if ($scope.historyBackOnSaveOrCancel) {
                                    policyViewState.state.newPolicyName = $scope.policy.name;
                                    $location.url(policyViewState.state.previousUrl);
                                } else {
                                    $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + $scope.policy.type + "/" + data.id);
                                }
                                Notifications.success("The policy has been created.");
                            }
                        });
                    });
                }

                $scope.reset = function() {
                    if (delegate.isPermission()) {
                        if ($scope.historyBackOnSaveOrCancel || policyViewState.state.rootUrl != null) {
                            if (policyViewState.state.rootUrl != null) {
                                $location.url(policyViewState.state.rootUrl);
                            } else {
                                $location.url(policyViewState.state.previousUrl);
                            }
                        } else {
                            $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/");
                        }
                    } else {
                        if ($scope.historyBackOnSaveOrCancel) {
                            $location.url(policyViewState.state.previousUrl);
                        } else {
                            $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/");
                        }
                    }
                }
            } else {
                service.get({
                    realm: realm.realm,
                    client : client.id,
                    type: delegate.getPolicyType(),
                    id: $route.current.params.id
                }, function(data) {
                    $scope.originalPolicy = data;
                    var policy = angular.copy(data);

                    $scope.changed = $scope.historyBackOnSaveOrCancel || PolicyController.isBackNewAssociatedPolicy();
                    $scope.policy = angular.copy(policy);

                    if (PolicyController.isBackNewAssociatedPolicy()) {
                        if (delegate.onRestoreState) {
                            delegate.onRestoreState($scope.policy);
                        }
                        $instance.restoreState($scope);
                    } else if (delegate.onInitUpdate) {
                        delegate.onInitUpdate($scope.policy);
                    }

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
                            service.update({realm : realm.realm, client : client.id, type: $scope.policy.type, id : $scope.policy.id}, $scope.policy, function() {
                                if (delegate.isPermission()) {
                                    if ($scope.historyBackOnSaveOrCancel) {
                                        $location.url(policyViewState.state.previousUrl);
                                    } else {
                                        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/permission/" + $scope.policy.type + "/" + $scope.policy.id);
                                    }
                                    $route.reload();
                                    Notifications.success("The permission has been updated.");
                                } else {
                                    if ($scope.historyBackOnSaveOrCancel) {
                                        $location.url(policyViewState.state.previousUrl);
                                    } else {
                                        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/authz/resource-server/policy/" + $scope.policy.type + "/" + $scope.policy.id);
                                    }
                                    $route.reload();
                                    Notifications.success("The policy has been updated.");
                                }
                            });
                        });
                    }

                    $scope.reset = function() {
                        if ($scope.historyBackOnSaveOrCancel) {
                            $location.url(policyViewState.state.previousUrl);
                        } else {
                            var freshPolicy = angular.copy(data);

                            if (delegate.onInitUpdate) {
                                delegate.onInitUpdate(freshPolicy);
                            }

                            $scope.policy = angular.copy(freshPolicy);
                            $scope.changed = false;
                        }
                    }
                });

                $scope.remove = function() {
                    Policies.delete(ResourceServerPolicy, $route.current.params.realm, client, $scope, AuthzDialog, $location, Notifications, $route, delegate.isPermission());
                }
            }
        });

        $scope.checkNewNameAvailability = function () {
            $instance.checkNameAvailability(function () {});
        }

        this.checkNameAvailability = function (onSuccess) {
            if (!$scope.policy.name || $scope.policy.name.trim().length == 0) {
                return;
            }
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

        this.restoreState = function($scope) {
            $scope.policy.name = policyViewState.state.policy.name;
            $scope.policy.description = policyViewState.state.policy.description;
            $scope.policy.decisionStrategy = policyViewState.state.policy.decisionStrategy;
            $scope.policy.logic = policyViewState.state.policy.logic;
            $scope.selectedPolicies = policyViewState.state.selectedPolicies;

            if (!$scope.selectedPolicies) {
                $scope.selectedPolicies = [];
            }

            $scope.changed = true;
            var previousPage = policyViewState.state.previousPage;

            if (policyViewState.state.newPolicyName) {
                ResourceServerPolicy.query({
                   realm: realm.realm,
                   client : client.id,
                   permission: false,
                   name: policyViewState.state.newPolicyName,
                   max : 20,
                   first : 0
               }, function(response) {
                    for (i = 0; i < response.length; i++) {
                        if (response[i].name == policyViewState.state.newPolicyName) {
                            response[i].text = response[i].name;
                            $scope.selectedPolicies.push(response[i]);
                        }
                    }

                    var rootUrl = policyViewState.state.rootUrl;
                    policyViewState.state = {};
                    policyViewState.state.previousPage = previousPage;
                    policyViewState.state.rootUrl = rootUrl;
                });
            } else {
                var rootUrl = policyViewState.state.rootUrl;
                policyViewState.state = {};
                policyViewState.state.previousPage = previousPage;
                policyViewState.state.rootUrl = rootUrl;
            }
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
                , $scope.authzRequest).then(function(response) {
                    $scope.evaluationResult = response.data;
                    $scope.showResultTab();
                });
    }

    $scope.entitlements = function() {
        $scope.authzRequest.entitlements = true;
        $http.post(authUrl + '/admin/realms/'+ $route.current.params.realm + '/clients/' + client.id + '/authz/resource-server/policy/evaluate'
            , $scope.authzRequest).then(function(response) {
            $scope.evaluationResult = response.data;
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

    $scope.reset = function() {
        $scope.authzRequest = angular.copy(authzRequest);
        $scope.changed = false;
    }
});

getManageClientId = function(realm) {
    if (realm.realm == masterRealm) {
        return 'master-realm';
    } else {
        return 'realm-management';
    }
}

module.controller('RealmRolePermissionsCtrl', function($scope, $http, $route, $location, realm, role, RoleManagementPermissions, Client, Notifications, Dialog, RealmRoleRemover) {
    console.log('RealmRolePermissionsCtrl');
    $scope.role = role;
    $scope.realm = realm;
    
    $scope.remove = function() {
        RealmRoleRemover.remove($scope.role, realm, Dialog, $location, Notifications);
    };
    
    RoleManagementPermissions.get({realm: realm.realm, role: role.id}, function(data) {
        $scope.permissions = data;
        $scope.$watch('permissions.enabled', function(newVal, oldVal) {
            if (newVal != oldVal) {
                var param = {enabled: $scope.permissions.enabled};
                $scope.permissions= RoleManagementPermissions.update({realm: realm.realm, role:role.id}, param);
            }
        }, true);
    });
    Client.query({realm: realm.realm, clientId: getManageClientId(realm)}, function(data) {
        $scope.realmManagementClientId = data[0].id;
    });
});
module.controller('ClientRolePermissionsCtrl', function($scope, $http, $route, $location, realm, client, role, Client, RoleManagementPermissions, Client, Notifications) {
    console.log('RealmRolePermissionsCtrl');
    $scope.client = client;
    $scope.role = role;
    $scope.realm = realm;
    RoleManagementPermissions.get({realm: realm.realm, role: role.id}, function(data) {
        $scope.permissions = data;
        $scope.$watch('permissions.enabled', function(newVal, oldVal) {
            if (newVal != oldVal) {
                var param = {enabled: $scope.permissions.enabled};
                $scope.permissions = RoleManagementPermissions.update({realm: realm.realm, role:role.id}, param);
            }
        }, true);
    });
    Client.query({realm: realm.realm, clientId: getManageClientId(realm)}, function(data) {
        $scope.realmManagementClientId = data[0].id;
    });
});

module.controller('UsersPermissionsCtrl', function($scope, $http, $route, $location, realm, UsersManagementPermissions, Client, Notifications) {
    console.log('UsersPermissionsCtrl');
    $scope.realm = realm;
    var first = true;
    UsersManagementPermissions.get({realm: realm.realm}, function(data) {
        $scope.permissions = data;
        $scope.$watch('permissions.enabled', function(newVal, oldVal) {
            if (newVal != oldVal) {
                var param = {enabled: $scope.permissions.enabled};
                $scope.permissions = UsersManagementPermissions.update({realm: realm.realm}, param);

            }
        }, true);
    });
    Client.query({realm: realm.realm, clientId: getManageClientId(realm)}, function(data) {
        $scope.realmManagementClientId = data[0].id;
    });




});

module.controller('ClientPermissionsCtrl', function($scope, $http, $route, $location, realm, client, Client, ClientManagementPermissions, Notifications) {
    $scope.client = client;
    $scope.realm = realm;
    ClientManagementPermissions.get({realm: realm.realm, client: client.id}, function(data) {
        $scope.permissions = data;
        $scope.$watch('permissions.enabled', function(newVal, oldVal) {
            if (newVal != oldVal) {
                var param = {enabled: $scope.permissions.enabled};
                $scope.permissions = ClientManagementPermissions.update({realm: realm.realm, client: client.id}, param);
            }
        }, true);
    });
    Client.query({realm: realm.realm, clientId: getManageClientId(realm)}, function(data) {
        $scope.realmManagementClientId = data[0].id;
    });
});

module.controller('IdentityProviderPermissionCtrl', function($scope, $http, $route, $location, realm, identityProvider, Client, IdentityProviderManagementPermissions, Notifications) {
    $scope.identityProvider = identityProvider;
    $scope.realm = realm;
    IdentityProviderManagementPermissions.get({realm: realm.realm, alias: identityProvider.alias}, function(data) {
        $scope.permissions = data;
        $scope.$watch('permissions.enabled', function(newVal, oldVal) {
            if (newVal != oldVal) {
                var param = {enabled: $scope.permissions.enabled};
                $scope.permissions = IdentityProviderManagementPermissions.update({realm: realm.realm, alias: identityProvider.alias}, param);
            }
        }, true);
    });
    Client.query({realm: realm.realm, clientId: getManageClientId(realm)}, function(data) {
        $scope.realmManagementClientId = data[0].id;
    });
});

module.controller('GroupPermissionsCtrl', function($scope, $http, $route, $location, realm, group, GroupManagementPermissions, Client, Notifications) {
    $scope.group = group;
    $scope.realm = realm;
    Client.query({realm: realm.realm, clientId: getManageClientId(realm)}, function(data) {
        $scope.realmManagementClientId = data[0].id;
    });
    GroupManagementPermissions.get({realm: realm.realm, group: group.id}, function(data) {
        $scope.permissions = data;
        $scope.$watch('permissions.enabled', function(newVal, oldVal) {
            if (newVal != oldVal) {
                var param = {enabled: $scope.permissions.enabled};
                $scope.permissions = GroupManagementPermissions.update({realm: realm.realm, group: group.id}, param);
            }
        }, true);
    });
});