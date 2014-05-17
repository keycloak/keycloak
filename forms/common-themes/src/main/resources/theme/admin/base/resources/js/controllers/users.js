module.controller('UserRoleMappingCtrl', function($scope, $http, realm, user, applications, RealmRoleMapping,
                                                  ApplicationRoleMapping, AvailableRealmRoleMapping, AvailableApplicationRoleMapping,
                                                  CompositeRealmRoleMapping, CompositeApplicationRoleMapping) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.applications = applications;
    $scope.applicationRoles = [];
    $scope.applicationComposite = [];
    $scope.selectedApplicationRoles = [];
    $scope.selectedApplicationMappings = [];
    $scope.applicationMappings = [];
    $scope.dummymodel = [];

    $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username});
    $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.username});
    $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.username});

    $scope.addRealmRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/realm',
                $scope.selectedRealmRoles).success(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.application) {
                    console.log('load available');
                    $scope.applicationComposite = CompositeApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                    $scope.applicationRoles = AvailableApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                    $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                    $scope.selectedApplicationRoles = [];
                    $scope.selectedApplicationMappings = [];
                }
            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/realm',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.application) {
                    console.log('load available');
                    $scope.applicationComposite = CompositeApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                    $scope.applicationRoles = AvailableApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                    $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                    $scope.selectedApplicationRoles = [];
                    $scope.selectedApplicationMappings = [];
                }
            });
    };

    $scope.addApplicationRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.name,
                $scope.selectedApplicationRoles).success(function() {
                $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                $scope.applicationRoles = AvailableApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                $scope.applicationComposite = CompositeApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                $scope.selectedApplicationRoles = [];
                $scope.selectedApplicationMappings = [];
            });
    };

    $scope.deleteApplicationRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.name,
            {data : $scope.selectedApplicationMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                $scope.applicationRoles = AvailableApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                $scope.applicationComposite = CompositeApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
                $scope.selectedApplicationRoles = [];
                $scope.selectedApplicationMappings = [];
            });
    };


    $scope.changeApplication = function() {
        console.log('changeApplication');
        if ($scope.application) {
            console.log('load available');
            $scope.applicationComposite = CompositeApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
            $scope.applicationRoles = AvailableApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
            $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.realm, userId : user.username, application : $scope.application.name});
        } else {
            $scope.applicationRoles = null;
            $scope.applicationMappings = null;
            $scope.applicationComposite = null;
        }
        $scope.selectedApplicationRoles = [];
        $scope.selectedApplicationMappings = [];
    };



});

module.controller('UserSessionsCtrl', function($scope, realm, user, sessions, UserSessions, UserLogout, UserSessionLogout, Notifications) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.sessions = sessions;

    $scope.logoutAll = function() {
        UserLogout.save({realm : realm.realm, user: user.username}, function () {
            Notifications.success('Logged out user in all applications');
            UserSessions.get({realm: realm.realm, user: user.username}, function(updated) {
                $scope.sessions = updated;
            })
        });
    };

    $scope.logoutSession = function(sessionId) {
        console.log('here in logoutSession');
        UserSessionLogout.delete({realm : realm.realm, session: sessionId}, function() {
            Notifications.success('Logged out session');
            UserSessions.get({realm: realm.realm, user: user.username}, function(updated) {
                $scope.sessions = updated;
            })
        });
    }
});

module.controller('UserSocialCtrl', function($scope, realm, user, socialLinks) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.socialLinks = socialLinks;
    console.log('showing social links of user');
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

    if ($scope.create) {
        $scope.user.enabled = true;
    }

    $scope.changed = false; // $scope.create;

    // ID - Name map for required actions. IDs are enum names.
    $scope.userReqActionList = [
        {id: "VERIFY_EMAIL", text: "Verify Email"},
        {id: "UPDATE_PROFILE", text: "Update Profile"},
        {id: "CONFIGURE_TOTP", text: "Configure Totp"},
        {id: "UPDATE_PASSWORD", text: "Update Password"}
    ];

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
