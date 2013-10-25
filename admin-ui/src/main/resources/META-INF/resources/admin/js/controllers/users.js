module.controller('UserRoleMappingCtrl', function($scope, $http, realm, user, roles, applications, RealmRoleMapping, ApplicationRoleMapping, ApplicationRole) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.realmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.applications = applications;
    $scope.applicationRoles = [];
    $scope.selectedApplicationRoles = [];
    $scope.selectedApplicationMappings = [];
    $scope.applicationMappings = [];



    $scope.realmMappings = RealmRoleMapping.query({realm : realm.id, userId : user.username}, function(){
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
        $http.post('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/realm',
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
        $http.delete('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/realm',
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

    $scope.addApplicationRole = function() {
        $http.post('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.id,
                $scope.selectedApplicationRoles).success(function() {
                for (var i = 0; i < $scope.selectedApplicationRoles.length; i++) {
                    var role = $scope.selectedApplicationRoles[i];
                    var idx = $scope.applicationRoles.indexOf($scope.selectedApplicationRoles[i]);
                    if (idx != -1) {
                        $scope.applicationRoles.splice(idx, 1);
                        $scope.applicationMappings.push(role);
                    }
                }
                $scope.selectedApplicationRoles = [];
            });
    };

    $scope.deleteApplicationRole = function() {
        $http.delete('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.id,
            {data : $scope.selectedApplicationMappings, headers : {"content-type" : "application/json"}}).success(function() {
                for (var i = 0; i < $scope.selectedApplicationMappings.length; i++) {
                    var role = $scope.selectedApplicationMappings[i];
                    var idx = $scope.applicationMappings.indexOf($scope.selectedApplicationMappings[i]);
                    if (idx != -1) {
                        $scope.applicationMappings.splice(idx, 1);
                        $scope.applicationRoles.push(role);
                    }
                }
                $scope.selectedApplicationMappings = [];
            });
    };


    $scope.changeApplication = function() {
        $scope.applicationRoles = ApplicationRole.query({realm : realm.id, userId : user.username, application : $scope.application.id}, function() {
                $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.id, userId : user.username, application : $scope.application.id}, function(){
                    for (var i = 0; i < $scope.applicationMappings.length; i++) {
                        var role = $scope.applicationMappings[i];
                        for (var j = 0; j < $scope.applicationRoles.length; j++) {
                            var realmRole = $scope.applicationRoles[j];
                            if (realmRole.id == role.id) {
                                var idx = $scope.applicationRoles.indexOf(realmRole);
                                if (idx != -1) {
                                    $scope.applicationRoles.splice(idx, 1);
                                    break;
                                }
                            }
                        }
                    }
                });

            }
        );
    };



});

module.controller('UserListCtrl', function($scope, realm, User) {
    $scope.realm = realm;

    $scope.searchQuery = function() {
        console.log('search: ' + $scope.search);
        var parameters = { search : $scope.search };
        parameters["realm"] = realm.id;
        $scope.users = User.query(parameters);
    };
});



module.controller('UserDetailCtrl', function($scope, realm, user, User, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.user = angular.copy(user);
    $scope.create = !user.username;

    $scope.changed = false; // $scope.create;

    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.create) {
            User.save({
                realm: realm.id
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);

                $location.url("/realms/" + realm.id + "/users/" + $scope.user.username);
                Notifications.success("The user has been created.");
            });
        } else {
            User.update({
                realm: realm.id,
                userId: $scope.user.username
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);
                Notifications.success("Your changes have been saved to the user.");
            });

        }
    };

    $scope.reset = function() {
        $scope.user = angular.copy(user);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/users");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.user.username, 'user', function() {
            $scope.user.$remove({
                realm : realm.id,
                userId : $scope.user.username
            }, function() {
                $location.url("/realms/" + realm.id + "/users");
                Notifications.success("The user has been deleted.");
            });
        });
    };
});

module.controller('RoleMappingCtrl', function($scope, realm, User, users, role, RoleMapping, Notifications) {
    $scope.realm = realm;
    $scope.realmId = realm.realm || realm.id;
    $scope.allUsers = User.query({ realm : $scope.realmId });
    $scope.users = users;
    $scope.role = role;

    $scope.addUser = function() {
        var user = $scope.newUser;
        $scope.newUser = null;

        for ( var i = 0; i < $scope.allUsers.length; i++) {
            if ($scope.allUsers[i].userId == user) {
                user = $scope.allUsers[i];
                RoleMapping.save({
                    realm : $scope.realmId,
                    role : role
                }, user, function() {
                    $scope.users = RoleMapping.query({
                        realm : $scope.realmId,
                        role : role
                    });
                    Notifications.success("The role mapping has been added for the user.");
                });
            }
        }
    }

    $scope.removeUser = function(userId) {
        for (var i = 0; i < $scope.users.length; i++) {
            var user = $scope.users[i];
            if ($scope.users[i].userId == userId) {
                RoleMapping.delete({
                    realm : $scope.realmId,
                    role : role
                }, user, function() {
                    $scope.users = RoleMapping.query({
                        realm : $scope.realmId,
                        role : role
                    });

                    Notifications.success("The role mapping has been removed for the user.");
                });
            }
        }
    }
});



