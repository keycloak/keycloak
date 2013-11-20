module.controller('ApplicationRoleListCtrl', function($scope, $location, realm, application, roles) {
    $scope.realm = realm;
    $scope.roles = roles;
    $scope.application = application;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationCredentialsCtrl', function($scope, $location, realm, application, ApplicationCredentials, Notifications) {
    $scope.realm = realm;
    $scope.application = application;

    var required = realm.requiredApplicationCredentials;

    for (var i = 0; i < required.length; i++) {
        if (required[i] == 'password') {
            $scope.passwordRequired = true;
        } else if (required[i] == 'totp') {
            $scope.totpRequired = true;
        } else if (required[i] == 'cert') {
            $scope.certRequired = true;
        }
    }

    $scope.generateTotp = function() {
        $scope.totp = randomString(5) + '-' + randomString(5) + '-' + randomString(5);
    }

    $scope.changePassword = function() {
        if ($scope.password != $scope.confirmPassword) {
            Notifications.error("Password and confirmation does not match.");
            $scope.password = "";
            $scope.confirmPassword = "";
            return;
        }
        var creds = [
            {
                type : "password",
                value : $scope.password
            }
        ];

        ApplicationCredentials.update({ realm : realm.id, application : application.id }, creds,
            function() {
                Notifications.success('The password has been changed.');
                $scope.password = null;
                $scope.confirmPassword = null;
            },
            function() {
                Notifications.error("The password was not changed due to a problem.");
                $scope.password = null;
                $scope.confirmPassword = null;
            }
        );
    };

    $scope.changeTotp = function() {
        var creds = [
            {
                type : "totp",
                value : $scope.totp
            }
        ];

        ApplicationCredentials.update({ realm : realm.id, application : application.id }, creds,
            function() {
                Notifications.success('The totp was changed.');
                $scope.totp = null;
            },
            function() {
                Notifications.error("The totp was not changed due to a problem.");
                $scope.totp = null;
            }
        );
    };
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});





module.controller('ApplicationRoleDetailCtrl', function($scope, realm, application, role, ApplicationRole, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('role', function() {
        if (!angular.equals($scope.role, role)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.create) {
            ApplicationRole.save({
                realm: realm.id,
                application : application.id
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.id + "/applications/" + application.id + "/roles/" + id);
                Notifications.success("The role has been created.");

            });
        } else {
            ApplicationRole.update({
                realm : realm.id,
                application : application.id,
                roleId : role.id
            }, $scope.role, function() {
                $scope.changed = false;
                role = angular.copy($scope.role);
                Notifications.success("Your changes have been saved to the role.");
            });
        }
    };

    $scope.reset = function() {
        $scope.role = angular.copy(role);
        $scope.changed = false;
        $scope.roleForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/applications/" + application.id + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.id,
                application : application.id,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.id + "/applications/" + application.id + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };
});

module.controller('ApplicationListCtrl', function($scope, realm, applications, Application, $location) {
    console.log('ApplicationListCtrl');
    $scope.realm = realm;
    $scope.applications = applications;
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationDetailCtrl', function($scope, realm, application, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.realm = realm;
    $scope.create = !application.id;
    if (!$scope.create) {
        $scope.application= angular.copy(application);
    } else {
        $scope.application = {};
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('application', function() {
        console.log('watch application');
        if (!angular.equals($scope.application, application)) {
            console.log('application changed');
            $scope.changed = true;
        }
    }, true);

    $scope.deleteWebOrigin = function(index) {
        $scope.application.webOrigins.splice(index, 1);
    }
    $scope.addWebOrigin = function() {
        $scope.application.webOrigins.push($scope.newWebOrigin);
        $scope.newWebOrigin = "";
    }
    $scope.deleteRedirectUri = function(index) {
        $scope.application.redirectUris.splice(index, 1);
    }
    $scope.addRedirectUri = function() {
        $scope.application.redirectUris.push($scope.newRedirectUri);
        $scope.newRedirectUri = "";
    }

    $scope.save = function() {
        if ($scope.applicationForm.$valid) {

            if ($scope.create) {
                Application.save({
                    realm: realm.id
                }, $scope.application, function (data, headers) {
                    $scope.changed = false;
                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.id + "/applications/" + id);
                    Notifications.success("The application has been created.");
                });
            } else {
                Application.update({
                    realm : realm.id,
                    id : application.id
                }, $scope.application, function() {
                    $scope.changed = false;
                    application = angular.copy($scope.application);
                    Notifications.success("Your changes have been saved to the application.");
                });
            }

        } else {
            $scope.applicationForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.application = angular.copy(application);
        $scope.changed = false;
        $scope.applicationForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/applications");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.application.name, 'application', function() {
            $scope.application.$remove({
                realm : realm.id,
                id : $scope.application.id
            }, function() {
                $location.url("/realms/" + realm.id + "/applications");
                Notifications.success("The application has been deleted.");
            });
        });
    };


});

module.controller('ApplicationScopeMappingCtrl', function($scope, $http, realm, application, roles, applications, ApplicationRealmScopeMapping, ApplicationApplicationScopeMapping, ApplicationRole) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.realmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.applications = applications;
    $scope.applicationRoles = [];
    $scope.selectedApplicationRoles = [];
    $scope.selectedApplicationMappings = [];
    $scope.applicationMappings = [];



    $scope.realmMappings = ApplicationRealmScopeMapping.query({realm : realm.id, application : application.id}, function(){
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
        $http.post('/auth-server/rest/saas/admin/realms/' + realm.id + '/applications/' + application.id + '/scope-mappings/realm',
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
        $http.delete('/auth-server/rest/saas/admin/realms/' + realm.id + '/applications/' + application.id +  '/scope-mappings/realm',
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
        $http.post('/auth-server/rest/saas/admin/realms/' + realm.id + '/applications/' + application.id +  '/scope-mappings/applications/' + $scope.targetApp.id,
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
        $http.delete('/auth-server/rest/saas/admin/realms/' + realm.id + '/applications/' + application.id +  '/scope-mappings/applications/' + $scope.targetApp.id,
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
        $scope.applicationRoles = ApplicationRole.query({realm : realm.id, application : $scope.targetApp.id}, function() {
                $scope.applicationMappings = ApplicationApplicationScopeMapping.query({realm : realm.id, application : application.id, targetApp : $scope.targetApp.id}, function(){
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
