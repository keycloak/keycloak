module.controller('GroupListCtrl', function($scope, $route, realm, groups, Groups, Group, GroupChildren, Notifications, $location, Dialog) {
    $scope.realm = realm;
    $scope.groupList = [
        {"id" : "realm", "name": "Groups",
            "subGroups" : groups}
    ];

    $scope.tree = [];

    $scope.edit = function(selected) {
        if (selected.id == 'realm') return;
        $location.url("/realms/" + realm.realm + "/groups/" + selected.id);
    }

    $scope.cut = function(selected) {
        $scope.cutNode = selected;
    }

    $scope.isDisabled = function() {
        if (!$scope.tree.currentNode) return true;
        return $scope.tree.currentNode.id == 'realm';
    }

    $scope.paste = function(selected) {
        if (selected == null) return;
        if ($scope.cutNode == null) return;
        if (selected.id == $scope.cutNode.id) return;
        if (selected.id == 'realm') {
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

    }

    $scope.remove = function(selected) {
        if (selected == null) return;
        Dialog.confirmDelete(selected.name, 'group', function() {
            Group.remove({ realm: realm.realm, groupId : selected.id }, function() {
                $route.reload();
                Notifications.success("The group has been deleted.");
            });
        });

    }

    $scope.createGroup = function(selected) {
        var parent = 'realm';
        if (selected) {
            parent = selected.id;
        }
        $location.url("/create/group/" + realm.realm + '/parent/' + parent);

    }
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

});

module.controller('GroupCreateCtrl', function($scope, $route, realm, parentId, Groups, Group, GroupChildren, Notifications, $location) {
    $scope.realm = realm;
    $scope.group = {};
    $scope.save = function() {
        console.log('save!!!');
        if (parentId == 'realm') {
            console.log('realm')
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

    }
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
                var attrVals = attrs[attribute].split("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    function convertAttributeValuesToString(group) {
        var attrs = group.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                var attrVals = attrs[attribute].join("##");
                attrs[attribute] = attrVals;
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

module.controller('GroupRoleMappingCtrl', function($scope, $http, realm, group, clients, client, Notifications, GroupRealmRoleMapping,
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
        var roles = $scope.selectedRealmRoles;
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm',
            roles).success(function() {
                $scope.realmMappings = GroupRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.targetClient) {
                    console.log('load available');
                    $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                    $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                    $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success("Role mappings updated.");

            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.realmMappings = GroupRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.targetClient) {
                    console.log('load available');
                    $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                    $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                    $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success("Role mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/clients/' + $scope.targetClient.id,
            $scope.selectedClientRoles).success(function() {
                $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                Notifications.success("Role mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/clients/' + $scope.targetClient.id,
            {data : $scope.selectedClientMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                $scope.realmComposite = GroupCompositeRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                $scope.realmRoles = GroupAvailableRealmRoleMapping.query({realm : realm.realm, groupId : group.id});
                Notifications.success("Role mappings updated.");
            });
    };


    $scope.changeClient = function() {
        if ($scope.targetClient) {
            $scope.clientComposite = GroupCompositeClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
            $scope.clientRoles = GroupAvailableClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
            $scope.clientMappings = GroupClientRoleMapping.query({realm : realm.realm, groupId : group.id, client : $scope.targetClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
        $scope.selectedClientRoles = [];
        $scope.selectedClientMappings = [];
    };



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

module.controller('DefaultGroupsCtrl', function($scope, $route, realm, groups, DefaultGroups, Notifications, $location, Dialog) {
    $scope.realm = realm;
    $scope.groupList = groups;
    $scope.selectedGroup = null;
    $scope.tree = [];

    DefaultGroups.query({realm: realm.realm}, function(data) {
        $scope.defaultGroups = data;

    });

    $scope.addDefaultGroup = function() {
        if (!$scope.tree.currentNode) {
            Notifications.error('Please select a group to add');
            return;
        };

        DefaultGroups.update({realm: realm.realm, groupId: $scope.tree.currentNode.id}, function() {
            Notifications.success('Added default group');
            $route.reload();
        });

    };

    $scope.removeDefaultGroup = function() {
        DefaultGroups.remove({realm: realm.realm, groupId: $scope.selectedGroup.id}, function() {
            Notifications.success('Removed default group');
            $route.reload();
        });

    };

    var isLeaf = function(node) {
        return node.id != "realm" && (!node.subGroups || node.subGroups.length == 0);
    };

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

});

