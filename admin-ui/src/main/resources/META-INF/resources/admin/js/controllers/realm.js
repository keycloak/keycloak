module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications) {
    $scope.addMessage = function() {
        Notifications.success("test");
    };

    $scope.auth = Auth;
    $http.get('/auth-server/rest/saas/whoami').success(function(data, status) {
        Auth.user = data;
        Auth.loggedIn = true;
    })
        .error(function(data, status) {
            Auth.loggedIn = false;
        });

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $http.get('/auth-server/rest/saas/admin/realms').success(function(data) {
        Current.realms = data;
        if (data.length > 0) {
            Current.realm = data[0];
            $location.url("/realms/" + Current.realm.id);
        }
    });
});

module.controller('RealmListCtrl', function($scope, Realm, Current) {
    $scope.realms = Realm.get();
    Current.realms = $scope.realms;
});

module.controller('RealmDropdownCtrl', function($scope, Realm, Current, Auth, $location) {
//    Current.realms = Realm.get();
    $scope.current = Current;
    $scope.changeRealm = function() {
        $location.url("/realms/" + $scope.current.realm.id);
    };
    $scope.showNav = function() {
        var show = Current.realms.length > 0;
        console.log('Show dropdown? ' + show);
        return Auth.loggedIn && show;
    }
});

module.controller('RealmDetailCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications) {
    $scope.createRealm = !realm.id;

    console.log('RealmDetailCtrl');

    if ($scope.createRealm) {
        $scope.realm = {
            enabled: true,
            requireSsl: true,
            cookieLoginAllowed: true
        };
    } else {
        if (Current.realm == null || Current.realm.id != realm.id) {
            for (var i = 0; i < Current.realms.length; i++) {
                if (realm.id == Current.realms[i].id) {
                    Current.realm = Current.realms[i];
                    break;
                }
            }
        }
        if (Current.realm == null || Current.realm.id != realm.id) {
            console.log('should be unreachable');
            console.log('Why? ' + Current.realms.length + ' ' + Current.realm);
            return;
        }
        $scope.realm = angular.copy(realm);
        $scope.realm.requireSsl = !realm.sslNotRequired;
    }

    var oldCopy = angular.copy($scope.realm);



    $scope.changed = $scope.create;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.realmForm.$valid) {
            var realmCopy = angular.copy($scope.realm);
            realmCopy.sslNotRequired = !realmCopy.requireSsl;
            delete realmCopy["requireSsl"];
            if ($scope.createRealm) {
                Realm.save(realmCopy, function(data, headers) {
                    console.log('creating new realm');
                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    var data = Realm.query(function() {
                        Current.realms = data;
                        for (var i = 0; i < Current.realms.length; i++) {
                            if (Current.realms[i].id == id) {
                                Current.realm = Current.realms[i];
                            }
                        }
                        $location.url("/realms/" + id);
                        Notifications.success("Created realm");
                    });
                });
            } else {
                console.log('updating realm...');
                $scope.changed = false;
                Realm.update(realmCopy, function() {
                    var id = realmCopy.id;
                    var data = Realm.query(function() {
                        Current.realms = data;
                        for (var i = 0; i < Current.realms.length; i++) {
                            if (Current.realms[i].id == id) {
                                Current.realm = Current.realms[i];
                                oldCopy = angular.copy($scope.realm);
                            }
                        }
                    });
                    $location.url("/realms/" + id);
                    Notifications.success("Saved changes to realm");
                });
            }
        } else {
            $scope.realmForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
        $scope.realmForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.realm.name, 'realm', function() {
            Realm.remove($scope.realm, function() {
                Current.realms = Realm.get();
                $location.url("/realms");
                Notifications.success("Deleted realm");
            });
        });
    };
});

module.controller('RealmRequiredCredentialsCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmRequiredCredentialsCtrl');

    $scope.realm = {
        id : realm.id, realm : realm.realm, social : realm.social,
        requiredCredentials : realm.requiredCredentials,
        requiredApplicationCredentials : realm.requiredApplicationCredentials,
        requiredOAuthClientCredentials : realm.requiredOAuthClientCredentials
    };

    $scope.userCredentialOptions = {
        'multiple' : true,
        'simple_tags' : true,
        'tags' : ['password', 'totp', 'cert']
    };

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.realmForm.$valid) {
            var realmCopy = angular.copy($scope.realm);
            $scope.changed = false;
            Realm.update(realmCopy, function () {
                $location.url("/realms/" + realm.id + "/required-credentials");
                Notifications.success("Saved changes to realm");
            });
        } else {
            $scope.realmForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
        $scope.realmForm.showErrors = false;
    };
});


module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmTokenDetailCtrl');

    $scope.realm = { id : realm.id, realm : realm.realm, social : realm.social, tokenLifespan : realm.tokenLifespan,  accessCodeLifespan : realm.accessCodeLifespan };
    $scope.realm.tokenLifespanUnit = 'Seconds';
    $scope.realm.accessCodeLifespanUnit = 'Seconds';

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.realmForm.$valid) {
            var realmCopy = angular.copy($scope.realm);
            delete realmCopy["tokenLifespanUnit"];
            delete realmCopy["accessCodeLifespanUnit"];
            if ($scope.realm.tokenLifespanUnit == 'Minutes') {
                realmCopy.tokenLifespan = $scope.realm.tokenLifespan * 60;
            } else if ($scope.realm.tokenLifespanUnit == 'Hours') {
                realmCopy.tokenLifespan = $scope.realm.tokenLifespan * 60 * 60;
            } else if ($scope.realm.tokenLifespanUnit == 'Days') {
                realmCopy.tokenLifespan = $scope.realm.tokenLifespan * 60 * 60 * 24;
            }
            if ($scope.realm.accessCodeLifespanUnit == 'Minutes') {
                realmCopy.accessCodeLifespan = $scope.realm.accessCodeLifespan * 60;
            } else if ($scope.realm.accessCodeLifespanUnit == 'Hours') {
                realmCopy.accessCodeLifespan = $scope.realm.accessCodeLifespan * 60 * 60;
            } else if ($scope.realm.accessCodeLifespanUnit == 'Days') {
                realmCopy.accessCodeLifespan = $scope.realm.accessCodeLifespan * 60 * 60 * 24;
            }
            $scope.changed = false;
            Realm.update(realmCopy, function () {
                $location.url("/realms/" + realm.id + "/token-settings");
                Notifications.success("Saved changes to realm");
            });
        } else {
            $scope.realmForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
        $scope.realmForm.showErrors = false;
    };
});

module.controller('RoleListCtrl', function($scope, $location, realm, roles) {
    $scope.realm = realm;
    $scope.roles = roles;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('RoleDetailCtrl', function($scope, realm, role, Role, $location, Dialog, Notifications) {
    $scope.realm = realm;
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
            Role.save({
                realm: realm.id
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.id + "/roles/" + id);
                Notifications.success("Created role");

            });
        } else {
            Role.update({
                realm : realm.id,
                roleId : role.id
            }, $scope.role, function() {
                $scope.changed = false;
                role = angular.copy($scope.role);
                Notifications.success("Saved changes to role");
            });
        }
    };

    $scope.reset = function() {
        $scope.role = angular.copy(role);
        $scope.changed = false;
        $scope.roleForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.id,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.id + "/roles");
                Notifications.success("Deleted role");
            });
        });
    };
});
