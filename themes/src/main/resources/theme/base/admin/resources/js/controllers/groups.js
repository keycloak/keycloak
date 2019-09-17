module.controller('GroupListCtrl', function($scope, $route, $q, realm, Groups, GroupsCount, Group, GroupChildren, Notifications, $location, Dialog, ComponentUtils) {
    $scope.realm = realm;
    $scope.groupList = [
        {
            "id" : "realm",
            "name": "Groups",
            "subGroups" : []
        }
    ];

    $scope.searchCriteria = '';
    $scope.currentPage = 1;
    $scope.currentPageInput = $scope.currentPage;
    $scope.pageSize = 20;
    $scope.numberOfPages = 1;
    $scope.tree = [];

    var refreshGroups = function (search) {
        console.log('refreshGroups');
        $scope.currentPageInput = $scope.currentPage;

        var first = ($scope.currentPage * $scope.pageSize) - $scope.pageSize;
        console.log('first:' + first);
        var queryParams = {
            realm : realm.realm,
            first : first,
            max : $scope.pageSize
        };
        var countParams = {
            realm : realm.realm,
            top : 'true'
        };

        if(angular.isDefined(search) && search !== '') {
            queryParams.search = search;
            countParams.search = search;
        }

        var promiseGetGroups = $q.defer();
        Groups.query(queryParams, function(entry) {
            promiseGetGroups.resolve(entry);
        }, function() {
            promiseGetGroups.reject('Unable to fetch ' + queryParams);
        });
        promiseGetGroups.promise.then(function(groups) {
            $scope.groupList = [
                {
                    "id" : "realm",
                    "name": "Groups",
                    "subGroups": ComponentUtils.sortGroups('name', groups)
                }
            ];
            if (angular.isDefined(search) && search !== '') {
                // Add highlight for concrete text match
                setTimeout(function () {
                    document.querySelectorAll('span').forEach(function (element) {
                        if (element.textContent.indexOf(search) != -1) {
                            angular.element(element).addClass('highlight');
                        }
                    });
                }, 500);
            }
        }, function (failed) {
            Notifications.error(failed);
        });

        var promiseCount = $q.defer();
        console.log('countParams: realm[' + countParams.realm);
        GroupsCount.query(countParams, function(entry) {
            promiseCount.resolve(entry);
        }, function() {
            promiseCount.reject('Unable to fetch ' + countParams);
        });
        promiseCount.promise.then(function(entry) {
            if(angular.isDefined(entry.count) && entry.count > $scope.pageSize) {
                $scope.numberOfPages = Math.ceil(entry.count/$scope.pageSize);
            } else {
                $scope.numberOfPages = 1;
            }
        }, function (failed) {
            Notifications.error(failed);
        });
    };
    refreshGroups();

    $scope.$watch('currentPage', function(newValue, oldValue) {
        if(parseInt(newValue, 10) !== oldValue) {
            refreshGroups($scope.searchCriteria);
        }
    });

    $scope.clearSearch = function() {
        $scope.searchCriteria = '';
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshGroups();
        } else {
            $scope.currentPage = 1;
        }
    };

    $scope.searchGroup = function() {
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshGroups($scope.searchCriteria);
        } else {
            $scope.currentPage = 1;
        }
    };

    $scope.edit = function(selected) {
        if (selected.id === 'realm') return;
        $location.url("/realms/" + realm.realm + "/groups/" + selected.id);
    };

    $scope.cut = function(selected) {
        $scope.cutNode = selected;
    };

    $scope.isDisabled = function() {
        if (!$scope.tree.currentNode) return true;
        return $scope.tree.currentNode.id === 'realm';
    };

    $scope.paste = function(selected) {
        if (selected === null) return;
        if ($scope.cutNode === null) return;
        if (selected.id === $scope.cutNode.id) return;
        if (selected.id === 'realm') {
            Groups.save({realm: realm.realm}, {id:$scope.cutNode.id}, function() {
                $route.reload();
                Notifications.success("Group moved.");

            });

        } else {
            GroupChildren.save({realm: realm.realm, groupId: selected.id}, {id:$scope.cutNode.id}, function() {
                $route.reload();
                Notifications.success("Group moved.");

            });

        }

    };

    $scope.remove = function(selected) {
        if (selected === null) return;
        Dialog.confirmDelete(selected.name, 'group', function() {
            Group.remove({ realm: realm.realm, groupId : selected.id }, function() {
                $route.reload();
                Notifications.success("The group has been deleted.");
            });
        });

    };

    $scope.createGroup = function(selected) {
        var parent = 'realm';
        if (selected) {
            parent = selected.id;
        }
        $location.url("/create/group/" + realm.realm + '/parent/' + parent);

    };
    var isLeaf = function(node) {
        return node.id !== "realm" && (!node.subGroups || node.subGroups.length === 0);
    };

    $scope.getGroupClass = function(node) {
        if (node.id === "realm") {
            return 'pficon pficon-users';
        }
        if (isLeaf(node)) {
            return 'normal';
        }
        if (node.subGroups.length && node.collapsed) return 'collapsed';
        if (node.subGroups.length && !node.collapsed) return 'expanded';
        return 'collapsed';

    };

    $scope.getSelectedClass = function(node) {
        if (node.selected) {
            return 'selected';
        } else if ($scope.cutNode && $scope.cutNode.id === node.id) {
            return 'cut';
        }
        return undefined;
    }

});

module.controller('GroupCreateCtrl', function($scope, $route, realm, parentId, Groups, Group, GroupChildren, Notifications, $location) {
    $scope.realm = realm;
    $scope.group = {};
    $scope.save = function() {
        console.log('save!!!');
        if (parentId === 'realm') {
            console.log('realm');
            Groups.save({realm: realm.realm}, $scope.group, function(data, headers) {
                var l = headers().location;


                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/groups/" + id);
                Notifications.success("Group Created.");
            })

        } else {
            GroupChildren.save({realm: realm.realm, groupId: parentId}, $scope.group, function(data, headers) {
                var l = headers().location;


                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/groups/" + id);
                Notifications.success("Group Created.");
            })

        }

    };
    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/groups");
    };
});

module.controller('GroupTabCtrl', function(Dialog, $scope, Current, Group, Notifications, $location) {
    $scope.removeGroup = function() {
        Dialog.confirmDelete($scope.group.name, 'group', function() {
            Group.remove({
                realm : Current.realm.realm,
                groupId : $scope.group.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/groups");
                Notifications.success("The group has been deleted.");
            });
        });
    };
});

module.controller('GroupDetailCtrl', function(Dialog, $scope, realm, group, Group, Notifications, $location) {
    $scope.realm = realm;

    if (!group.attributes) {
        group.attributes = {}
    }
    convertAttributeValuesToString(group);


    $scope.group = angular.copy(group);

    $scope.changed = false; // $scope.create;
    $scope.$watch('group', function() {
        if (!angular.equals($scope.group, group)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        convertAttributeValuesToLists();

        Group.update({
            realm: realm.realm,
            groupId: $scope.group.id
        }, $scope.group, function () {
            $scope.changed = false;
            convertAttributeValuesToString($scope.group);
            group = angular.copy($scope.group);
            Notifications.success("Your changes have been saved to the group.");
        });
    };

    function convertAttributeValuesToLists() {
        var attrs = $scope.group.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                attrs[attribute] = attrs[attribute].split("##");
            }
        }
    }

    function convertAttributeValuesToString(group) {
        var attrs = group.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                attrs[attribute] = attrs[attribute].join("##");
            }
        }
    }

    $scope.reset = function() {
        $scope.group = angular.copy(group);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/groups");
    };

    $scope.addAttribute = function() {
        $scope.group.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
    }

    $scope.removeAttribute = function(key) {
        delete $scope.group.attributes[key];
    }
});

module.controller('GroupRoleMappingCtrl', function($scope, $http, $route, realm, group, clients, client, Client, Notifications, GroupRealmRoleMapping,
                                                   GroupClientRoleMapping, GroupAvailableRealmRoleMapping, GroupAvailableClientRoleMapping,
                                                   GroupCompositeRealmRoleMapping, GroupCompositeClientRoleMapping) {
    $scope.realm = realm;
    $scope.group = group;
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clients = clients;
    $scope.client = client;
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];

    $scope.realmMappings = GroupRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
    $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
    $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});

    $scope.addRealmRole = function() {
        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm',
            $scope.selectedRealmRolesToAdd).then(function() {
            $scope.realmMappings = GroupRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.selectedRealmMappings = [];
            $scope.selectRealmRoles = [];
            if ($scope.selectedClient) {
                console.log('load available');
                $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
                $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
                $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
            }
            $scope.selectedRealmRolesToAdd = [];
            Notifications.success("Role mappings updated.");

        });
    };

    $scope.deleteRealmRole = function() {
        $scope.selectedRealmMappingsToRemove = JSON.parse('[' + $scope.selectedRealmMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm',
            {data : $scope.selectedRealmMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function() {
            $scope.realmMappings = GroupRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.selectedRealmMappings = [];
            $scope.selectRealmRoles = [];
            if ($scope.selectedClient) {
                console.log('load available');
                $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
                $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
                $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
            }
            $scope.selectedRealmMappingsToRemove = [];
            Notifications.success("Role mappings updated.");
        });
    };

    $scope.addClientRole = function() {
        $scope.selectedClientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/clients/' + $scope.selectedClient.id,
            $scope.selectedClientRolesToAdd).then(function() {
            $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.selectedClientRoles = [];
            $scope.selectedClientMappings = [];
            $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.selectedClientRolesToAdd = [];
            Notifications.success("Role mappings updated.");
        });
    };

    $scope.deleteClientRole = function() {
        $scope.selectedClientMappingsToRemove = JSON.parse('[' + $scope.selectedClientMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/clients/' + $scope.selectedClient.id,
            {data : $scope.selectedClientMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function() {
            $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.selectedClientRoles = [];
            $scope.selectedClientMappings = [];
            $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
            $scope.selectedClientMappingsToRemove = [];
            Notifications.success("Role mappings updated.");
        });
    };


    $scope.changeClient = function(client) {
        $scope.selectedClient = client;
        if (!client || !client.id) {
            $scope.selectedClient = null;
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
            return;
        }
        if ($scope.selectedClient) {
            $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
            $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.selectedClient.id});
        }
        $scope.selectedClientRoles = [];
        $scope.selectedClientMappings = [];
    };

    clientSelectControl($scope, $route.current.params.realm, Client);

});

module.controller('GroupMembersCtrl', function($scope, realm, group, GroupMembership) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.group = group;

    $scope.query = {
        realm: realm.realm,
        groupId: group.id,
        max : 5,
        first : 0
    };


    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.searchQuery();
    };

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.searchQuery();
    };

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.searchQuery();
    };

    $scope.searchQuery = function() {
        console.log("query.search: " + $scope.query.search);
        $scope.searchLoaded = false;

        $scope.users = GroupMembership.query($scope.query, function() {
            console.log('search loaded');
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.searchQuery();

});

module.controller('DefaultGroupsCtrl', function($scope, $q, realm, Groups, GroupsCount, DefaultGroups, Notifications) {
    $scope.realm = realm;
    $scope.groupList = [];
    $scope.selectedGroup = null;
    $scope.tree = [];

    $scope.searchCriteria = '';
    $scope.currentPage = 1;
    $scope.currentPageInput = $scope.currentPage;
    $scope.pageSize = 20;
    $scope.numberOfPages = 1;

    var refreshDefaultGroups = function () {
        DefaultGroups.query({realm: realm.realm}, function(data) {
            $scope.defaultGroups = data;
        });
    }

    var refreshAvailableGroups = function (search) {
        var first = ($scope.currentPage * $scope.pageSize) - $scope.pageSize;
        $scope.currentPageInput = $scope.currentPage;
        var queryParams = {
            realm : realm.realm,
            first : first,
            max : $scope.pageSize
        };
        var countParams = {
            realm : realm.realm,
            top : 'true'
        };

        if(angular.isDefined(search) && search !== '') {
            queryParams.search = search;
            countParams.search = search;
        }

        var promiseGetGroups = $q.defer();
        Groups.query(queryParams, function(entry) {
            promiseGetGroups.resolve(entry);
        }, function() {
            promiseGetGroups.reject('Unable to fetch ' + queryParams);
        });
        promiseGetGroups.promise.then(function(groups) {
            $scope.groupList = groups;
        }, function (failed) {
            Notifications.success(failed);
        });

        var promiseCount = $q.defer();
        GroupsCount.query(countParams, function(entry) {
            promiseCount.resolve(entry);
        }, function() {
            promiseCount.reject('Unable to fetch ' + countParams);
        });
        promiseCount.promise.then(function(entry) {
            if(angular.isDefined(entry.count) && entry.count > $scope.pageSize) {
                $scope.numberOfPages = Math.ceil(entry.count/$scope.pageSize);
            }
        }, function (failed) {
            Notifications.success(failed);
        });
    };

    refreshAvailableGroups();

    $scope.$watch('currentPage', function(newValue, oldValue) {
        if(parseInt(newValue, 10) !== parseInt(oldValue, 10)) {
            refreshAvailableGroups($scope.searchCriteria);
        }
    });

    $scope.clearSearch = function() {
        $scope.searchCriteria = '';
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshAvailableGroups();
        } else {
            $scope.currentPage = 1;
        }
    };

    $scope.searchGroup = function() {
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshAvailableGroups($scope.searchCriteria);
        } else {
            $scope.currentPage = 1;
        }
    };

    refreshDefaultGroups();

    $scope.addDefaultGroup = function() {
        if (!$scope.tree.currentNode) {
            Notifications.error('Please select a group to add');
            return;
        }

        DefaultGroups.update({realm: realm.realm, groupId: $scope.tree.currentNode.id}, function() {
            refreshDefaultGroups();
            Notifications.success('Added default group');
        });

    };

    $scope.removeDefaultGroup = function() {
        DefaultGroups.remove({realm: realm.realm, groupId: $scope.selectedGroup.id}, function() {
            refreshDefaultGroups();
            Notifications.success('Removed default group');
        });

    };

    var isLeaf = function(node) {
        return node.id !== "realm" && (!node.subGroups || node.subGroups.length === 0);
    };

    $scope.getGroupClass = function(node) {
        if (node.id === "realm") {
            return 'pficon pficon-users';
        }
        if (isLeaf(node)) {
            return 'normal';
        }
        if (node.subGroups.length && node.collapsed) return 'collapsed';
        if (node.subGroups.length && !node.collapsed) return 'expanded';
        return 'collapsed';

    };

    $scope.getSelectedClass = function(node) {
        if (node.selected) {
            return 'selected';
        } else if ($scope.cutNode && $scope.cutNode.id === node.id) {
            return 'cut';
        }
        return undefined;
    }

});
