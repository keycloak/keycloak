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

    function randomString(len) {
        var charSet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        var randomString = '';
        for (var i = 0; i < len; i++) {
            var randomPoz = Math.floor(Math.random() * charSet.length);
            randomString += charSet.substring(randomPoz,randomPoz+1);
        }
        return randomString;
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

        ApplicationCredentials.update({ realm : realm.realm, application : application.name }, creds,
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

        ApplicationCredentials.update({ realm : realm.realm, application : application.name }, creds,
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

module.controller('ApplicationSessionsCtrl', function($scope, $location, realm, application) {
    $scope.realm = realm;
    $scope.application = application;
});

module.controller('ApplicationRoleDetailCtrl', function($scope, realm, application, role, roles, applications,
                                                        Role, ApplicationRole, RoleById, RoleRealmComposites, RoleApplicationComposites,
                                                        $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.save = function() {
        if ($scope.create) {
            ApplicationRole.save({
                realm: realm.realm,
                application : application.name
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/applications/" + application.name + "/roles/" + id);
                Notifications.success("The role has been created.");
            });
        } else {
            $scope.update();
        }
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.realm,
                application : application.name,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.realm + "/applications/" + application.name + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/applications/" + application.name + "/roles");
    };


    roleControl($scope, realm, role, roles, applications,
        ApplicationRole, RoleById, RoleRealmComposites, RoleApplicationComposites,
        $http, $location, Notifications, Dialog);

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

module.controller('ApplicationInstallationCtrl', function($scope, realm, installation, application, ApplicationInstallation, $routeParams) {
    console.log('ApplicationInstallationCtrl');
    $scope.realm = realm;
    $scope.application = application;
    $scope.installation = installation;
    $scope.download = ApplicationInstallation.url({ realm: $routeParams.realm, application: $routeParams.application });
});

module.controller('ApplicationDetailCtrl', function($scope, realm, application, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.realm = realm;
    $scope.create = !application.name;
    if (!$scope.create) {
        $scope.application= angular.copy(application);
    } else {
        $scope.application = {};
        $scope.application.webOrigins = [];
        $scope.application.redirectUris = [];
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('application', function() {
        if (!angular.equals($scope.application, application)) {
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
        if ($scope.create) {
            Application.save({
                realm: realm.realm,
                application: ''
            }, $scope.application, function (data, headers) {
                $scope.changed = false;
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/applications/" + id);
                Notifications.success("The application has been created.");
            });
        } else {
            Application.update({
                realm : realm.realm,
                application : application.name
            }, $scope.application, function() {
                $scope.changed = false;
                application = angular.copy($scope.application);
                Notifications.success("Your changes have been saved to the application.");
            });
        }
    };

    $scope.reset = function() {
        $scope.application = angular.copy(application);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/applications");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.application.name, 'application', function() {
            $scope.application.$remove({
                realm : realm.realm,
                application : $scope.application.name
            }, function() {
                $location.url("/realms/" + realm.realm + "/applications");
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



    $scope.realmMappings = ApplicationRealmScopeMapping.query({realm : realm.realm, application : application.name}, function(){
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
        $http.post('/auth/rest/admin/realms/' + realm.realm + '/applications/' + application.name + '/scope-mappings/realm',
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
        $http.delete('/auth/rest/admin/realms/' + realm.realm + '/applications/' + application.name +  '/scope-mappings/realm',
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
        $http.post('/auth/rest/admin/realms/' + realm.realm + '/applications/' + application.name +  '/scope-mappings/applications/' + $scope.targetApp.name,
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
        $http.delete('/auth/rest/admin/realms/' + realm.realm + '/applications/' + application.name +  '/scope-mappings/applications/' + $scope.targetApp.name,
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
        if ($scope.targetApp) {
            $scope.applicationRoles = ApplicationRole.query({realm : realm.realm, application : $scope.targetApp.name}, function() {
                    $scope.applicationMappings = ApplicationApplicationScopeMapping.query({realm : realm.realm, application : application.name, targetApp : $scope.targetApp.name}, function(){
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
        } else {
            $scope.applicationRoles = null;
        }
    };



});
