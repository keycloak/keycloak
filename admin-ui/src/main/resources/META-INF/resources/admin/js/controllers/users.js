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



    $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username}, function(){
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
        $http.post('/auth/rest/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/realm',
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
        $http.delete('/auth/rest/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/realm',
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
        $http.post('/auth/rest/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.name,
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
        $http.delete('/auth/rest/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.name,
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
        $scope.applicationRoles = ApplicationRole.query({realm : realm.realm, userId : user.username, application : $scope.application.name}, function() {
                $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name}, function(){
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
        $scope.searchLoaded = false;
        $scope.currentSearch = $scope.search;

        var params = { realm: realm.realm };
        if ($scope.search) {
            params.search = $scope.search;
        }

        $scope.users = User.query(params, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = params.search;
        });
    };
});



module.controller('UserDetailCtrl', function($scope, realm, user, User, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.user = angular.copy(user);
    $scope.create = !user.username;

    $scope.changed = false; // $scope.create;

    // ID - Name map for required actions. IDs are enum names.
    var userReqActionList = [
        {id: "VERIFY_EMAIL", text: "Verify Email"},
        {id: "UPDATE_PROFILE", text: "Update Profile"},
        {id: "CONFIGURE_TOTP", text: "Configure Totp"},
        {id: "UPDATE_PASSWORD", text: "Update Password"}
    ];

    // Options for the req actions tag selector
    $scope.userReqActionsOptions = {
        'multiple' : true,
        'tags' : userReqActionList
    };

    // Model for the req actions tag selector
    $scope.userActions = [];
    for (var i = 0; i < userReqActionList.length; i++){
        var action = userReqActionList[i];

        if ($scope.user.requiredActions && $scope.user.requiredActions.indexOf(action.id) > -1){
            $scope.userActions.push({id: action.id, text: action.text});
        }
    }

    // Watching ui-select2 model to properly format the req actions for user
    $scope.$watch("userActions", function(newValue, oldValue) {
        $scope.user.requiredActions = [];
        for (var i=0; i < newValue.length; i++){
            var action = newValue[i];
            $scope.user.requiredActions.push(action.id);
        }
    });

    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.create) {
            User.save({
                realm: realm.realm
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);

                $location.url("/realms/" + realm.realm + "/users/" + $scope.user.username);
                Notifications.success("The user has been created.");
            });
        } else {
            User.update({
                realm: realm.realm,
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
        $location.url("/realms/" + realm.realm + "/users");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.user.username, 'user', function() {
            $scope.user.$remove({
                realm : realm.realm,
                userId : $scope.user.username
            }, function() {
                $location.url("/realms/" + realm.realm + "/users");
                Notifications.success("The user has been deleted.");
            });
        });
    };
});

module.controller('UserCredentialsCtrl', function($scope, realm, user, User, UserCredentials, Notifications, Dialog) {
    console.log('UserCredentialsCtrl');

    $scope.realm = realm;
    $scope.user = angular.copy(user);

    $scope.isTotp = false;
    if(!!user.totp){
        $scope.isTotp = user.totp;
    }

    $scope.resetPassword = function() {
        if ($scope.pwdChange) {
            if ($scope.password != $scope.confirmPassword) {
                Notifications.error("Password and confirmation does not match.");
                return;
            }
        }

        Dialog.confirm('Reset password', 'Are you sure you want to reset the users password?', function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.username }, { type : "password", value : $scope.password }, function() {
                Notifications.success("The password has been reset");
                $scope.password = null;
                $scope.confirmPassword = null;
            }, function() {
                Notifications.error("Failed to reset user password");
            });
        }, function() {
            $scope.password = null;
            $scope.confirmPassword = null;
        });
    };

    $scope.removeTotp = function() {
        Dialog.confirm('Remove totp', 'Are you sure you want to remove the users totp configuration?', function() {
            UserCredentials.removeTotp({ realm: realm.realm, userId: user.username }, { }, function() {
                Notifications.success("The users totp configuration has been removed");
                $scope.user.totp = false;
            }, function() {
                Notifications.error("Failed to remove the users totp configuration");
            });
        });
    };

    $scope.resetPasswordEmail = function() {
        Dialog.confirm('Reset password email', 'Are you sure you want to send password reset email to user?', function() {
            UserCredentials.resetPasswordEmail({ realm: realm.realm, userId: user.username }, { }, function() {
                Notifications.success("Password reset email sent to user");
            }, function() {
                Notifications.error("Failed to send password reset mail to user");
            });
        });
    };

    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.userChange = true;
        } else {
            $scope.userChange = false;
        }
    }, true);

    $scope.$watch('password', function() {
        if (!!$scope.password){
            $scope.pwdChange = true;
        } else {
            $scope.pwdChange = false;
        }
    }, true);

    $scope.reset = function() {
        $scope.password = "";
        $scope.confirmPassword = "";

        $scope.user = angular.copy(user);

        $scope.isTotp = false;
        if(!!user.totp){
            $scope.isTotp = user.totp;
        }

        $scope.pwdChange = false;
        $scope.userChange = false;
    };
});

module.controller('RoleMappingCtrl', function($scope, realm, User, users, role, RoleMapping, Notifications) {
    $scope.realm = realm;
    $scope.realmId = realm.realm || realm.realm;
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



