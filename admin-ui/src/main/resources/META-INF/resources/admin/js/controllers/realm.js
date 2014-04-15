module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications) {
    $scope.addMessage = function() {
        Notifications.success("test");
    };

    $scope.authUrl = authUrl;

    $scope.auth = Auth;
    $http.get(authUrl + '/rest/admin/whoami').success(function(data, status) {
        Auth.user = data;
        Auth.loggedIn = true;

        function getAccess(role) {
            if (!Current.realm) {
                return false;
            }

            var realmAccess = Auth.user['realm_access'];
            if (realmAccess) {
                realmAccess = realmAccess[Current.realm.realm];
                if (realmAccess) {
                    return realmAccess.indexOf(role) >= 0;
                }
            }
            return false;
        }

        $scope.access = {
            createRealm: data.createRealm,

            get viewRealm() {
                return getAccess('view-realm') || this.manageRealm;
            },

            get viewApplications() {
                return getAccess('view-applications') || this.manageApplications;
            },

            get viewClients() {
                return getAccess('view-clients') || this.manageClients;
            },

            get viewUsers() {
                return getAccess('view-users') || this.manageClients;
            },

            get viewAudit() {
                return getAccess('view-audit') || this.manageClients;
            },

            get manageRealm() {
                return getAccess('manage-realm');
            },

            get manageApplications() {
                return getAccess('manage-applications');
            },

            get manageClients() {
                return getAccess('manage-clients');
            },

            get manageUsers() {
                return getAccess('manage-users');
            },

            get manageAudit() {
                return getAccess('manage-audit');
            }
        }
    })
        .error(function(data, status) {
            Auth.loggedIn = false;
        });

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.fragment = $location.path();
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('HomeCtrl', function(Realm, Auth, $location) {
    Realm.query(null, function(realms) {
        var realm;
        if (realms.length == 1) {
            realm = realms[0].realm;
        } else if (realms.length == 2) {
            if (realms[0].realm == Auth.user.realm) {
                realm = realms[1].realm;
            } else if (realms[1].realm == Auth.user.realm) {
                realm = realms[0].realm;
            }
        }
        if (realm) {
            $location.url('/realms/' + realm);
        } else {
            $location.url('/realms');
        }
    });
});

module.controller('RealmListCtrl', function($scope, Realm, Current) {
    $scope.realms = Realm.query();
    Current.realms = $scope.realms;
});

module.controller('RealmDropdownCtrl', function($scope, Realm, Current, Auth, $location) {
//    Current.realms = Realm.get();
    $scope.current = Current;

    $scope.changeRealm = function(selectedRealm) {
        $location.url("/realms/" + selectedRealm);
    };

    $scope.showNav = function() {
        var show = Current.realms.length > 0;
        return Auth.loggedIn && show;
    }
    $scope.refresh = function() {
         Current.refresh();
    }
});

module.controller('RealmCreateCtrl', function($scope, Current, Realm, $upload, $http, $location, Dialog, Notifications, Auth) {
    console.log('RealmCreateCtrl');

    Current.realm = null;

    $scope.realm = {
        enabled: true
    };

    $scope.changed = false;
    $scope.files = [];

    var oldCopy = angular.copy($scope.realm);

    $scope.onFileSelect = function($files) {
        $scope.files = $files;
    };

    $scope.clearFileSelect = function() {
        $scope.files = null;
    }

    $scope.uploadFile = function() {
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $scope.files.length; i++) {
            var $file = $scope.files[i];
            $scope.upload = $upload.upload({
                url: authUrl + '/rest/admin/realms', //upload.php script, node.js route, or servlet url
                // method: POST or PUT,
                // headers: {'headerKey': 'headerValue'}, withCredential: true,
                data: {myObj: ""},
                file: $file
                /* set file formData name for 'Content-Desposition' header. Default: 'file' */
                //fileFormDataName: myFile,
                /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
                //formDataAppender: function(formData, key, val){}
            }).progress(function(evt) {
                    console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
                }).success(function(data, status, headers) {
                    Realm.query(function(data) {
                        Current.realms = data;

                        $http.get(authUrl + '/rest/admin/whoami').success(function(user) {
                            Auth.user = user;

                            Notifications.success("The realm has been uploaded.");

                            var location = headers('Location');
                            if (location) {
                                $location.url("/realms/" + location.substring(location.lastIndexOf('/') + 1));
                            } else {
                                $location.url("/realms");
                            }
                        });
                    });
                })
            .error(function() {
                    Notifications.error("The realm can not be uploaded. Please verify the file.");

                });
            //.then(success, error, progress);
        }
    };

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        var ssl = window.location.protocol == 'https:';
        realmCopy.sslNotRequired = !ssl;
        console.log('creating new realm **');
        Realm.create(realmCopy, function() {
            Realm.query(function(data) {
                Current.realms = data;

                $http.get(authUrl + '/rest/admin/whoami').success(function(user) {
                    Auth.user = user;

                    $location.url("/realms/" + realmCopy.realm);
                    Notifications.success("The realm has been created.");
                });
            });
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };
});


module.controller('RealmDetailCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications) {
    $scope.createRealm = !realm.realm;
    $scope.serverInfo = serverInfo;

    console.log('RealmDetailCtrl');

    if ($scope.createRealm) {
        $scope.realm = {
            enabled: true,
            requireSsl: true
        };
    } else {
        if (Current.realm == null || Current.realm.realm != realm.realm) {
            for (var i = 0; i < Current.realms.length; i++) {
                if (realm.realm == Current.realms[i].realm) {
                    Current.realm = Current.realms[i];
                    break;
                }
            }
        }
        console.log('realm name: ' + realm.realm);
        for (var i = 0; i < Current.realms.length; i++) {
            console.log('checking Current.realm:' + Current.realms[i].realm);
            if (Current.realms[i].realm == realm.realm) {
                Current.realm = Current.realms[i];
            }
        }
        /*
        if (Current.realm == null || Current.realm.realm != realm.realm) {
            console.log('should be unreachable');
            console.log('Why? ' + Current.realms.length + ' ' + Current.realm);
            return;
        }
        */
        $scope.realm = angular.copy(realm);
        $scope.realm.requireSsl = !realm.sslNotRequired;
    }

    $scope.social = $scope.realm.social;
    $scope.registrationAllowed = $scope.realm.registrationAllowed;

    var oldCopy = angular.copy($scope.realm);



    $scope.changed = $scope.create;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        realmCopy.sslNotRequired = !realmCopy.requireSsl;
        delete realmCopy["requireSsl"];
        if ($scope.createRealm) {
            Realm.save(realmCopy, function(data, headers) {
                console.log('creating new realm');
                var data = Realm.query(function() {
                    Current.realms = data;
                    for (var i = 0; i < Current.realms.length; i++) {
                        if (Current.realms[i].realm == realmCopy.realm) {
                            Current.realm = Current.realms[i];
                        }
                    }
                    $location.url("/realms/" + realmCopy.realm);
                    Notifications.success("The realm has been created.");
                    $scope.social = $scope.realm.social;
                    $scope.registrationAllowed = $scope.realm.registrationAllowed;
                });
            });
        } else {
            console.log('updating realm...');
            $scope.changed = false;
            console.log('oldCopy.realm - ' + oldCopy.realm);
            Realm.update({ id : oldCopy.realm}, realmCopy, function () {
                var data = Realm.query(function () {
                    Current.realms = data;
                    for (var i = 0; i < Current.realms.length; i++) {
                        if (Current.realms[i].realm == realmCopy.realm) {
                            Current.realm = Current.realms[i];
                            oldCopy = angular.copy($scope.realm);
                        }
                    }
                });
                $location.url("/realms/" + realmCopy.realm);
                Notifications.success("Your changes have been saved to the realm.");
                $scope.social = $scope.realm.social;
                $scope.registrationAllowed = $scope.realm.registrationAllowed;
            });
        }
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.realm.realm, 'realm', function() {
            Realm.remove({ id : $scope.realm.realm }, function() {
                Current.realms = Realm.query();
                Notifications.success("The realm has been deleted.");
                $location.url("/");
            });
        });
    };
});

module.controller('RealmRequiredCredentialsCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications, PasswordPolicy) {
    console.log('RealmRequiredCredentialsCtrl');

    $scope.realm = realm;

    var oldCopy = angular.copy($scope.realm);

    $scope.allPolicies = PasswordPolicy.allPolicies;
    $scope.policyMessages = PasswordPolicy.policyMessages;

    $scope.policy = PasswordPolicy.parse(realm.passwordPolicy);
    var oldPolicy = angular.copy($scope.policy);

    $scope.addPolicy = function(policy){
        if (!$scope.policy) {
            $scope.policy = [];
        }
        $scope.policy.push(policy);
    }

    $scope.removePolicy = function(index){
        $scope.policy.splice(index, 1);
    }

    $scope.userCredentialOptions = {
        'multiple' : true,
        'simple_tags' : true,
        'tags' : ['password', 'totp', 'cert']
    };

    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.$watch('policy', function(oldVal, newVal) {
        if (!angular.equals($scope.policy, oldPolicy)) {
            $scope.realm.passwordPolicy = PasswordPolicy.toString($scope.policy);
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        $scope.changed = false;

        Realm.update($scope.realm, function () {
            $location.url("/realms/" + realm.realm + "/required-credentials");
            Notifications.success("Your changes have been saved to the realm.");
            oldCopy = angular.copy($scope.realm);
            oldPolicy = angular.copy($scope.policy);
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.policy = angular.copy(oldPolicy);
        $scope.changed = false;
    };
});

module.controller('RealmDefaultRolesCtrl', function ($scope, Realm, realm, applications, roles, Notifications, ApplicationRole, Application) {

    console.log('RealmDefaultRolesCtrl');

    $scope.realm = realm;

    $scope.availableRealmRoles = [];
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmDefRoles = [];

    $scope.applications = angular.copy(applications);
    for (var i = 0; i < applications.length; i++) {
        if (applications[i].name == 'account') {
            $scope.application = $scope.applications[i];
            break;
        }
    }

    $scope.availableAppRoles = [];
    $scope.selectedAppRoles = [];
    $scope.selectedAppDefRoles = [];

    if (!$scope.realm.hasOwnProperty('defaultRoles') || $scope.realm.defaultRoles === null) {
        $scope.realm.defaultRoles = [];
    }

    // Populate available roles. Available roles are neither already assigned
    for (var i = 0; i < roles.length; i++) {
        var item = roles[i].name;

        if ($scope.realm.defaultRoles.indexOf(item) < 0) {
            $scope.availableRealmRoles.push(item);
        }
    }

    $scope.addRealmDefaultRole = function () {

        // Remove selected roles from the Available roles and add them to realm default roles (move from left to right).
        for (var i = 0; i < $scope.selectedRealmRoles.length; i++) {
            var selectedRole = $scope.selectedRealmRoles[i];

            $scope.realm.defaultRoles.push(selectedRole);

            var index = $scope.availableRealmRoles.indexOf(selectedRole);
            if (index > -1) {
                $scope.availableRealmRoles.splice(index, 1);
            }
        }

        // Update/save the realm with new default roles.
        Realm.update($scope.realm, function () {
            Notifications.success("Realm default roles updated.");
        });
    };

    $scope.deleteRealmDefaultRole = function () {

        // Remove selected roles from the realm default roles and add them to available roles (move from right to left).
        for (var i = 0; i < $scope.selectedRealmDefRoles.length; i++) {
            $scope.availableRealmRoles.push($scope.selectedRealmDefRoles[i]);

            var index = $scope.realm.defaultRoles.indexOf($scope.selectedRealmDefRoles[i]);
            if (index > -1) {
                $scope.realm.defaultRoles.splice(index, 1);
            }
        }

        // Update/save the realm with new default roles.
        //var realmCopy = angular.copy($scope.realm);
        Realm.update($scope.realm, function () {
            Notifications.success("Realm default roles updated.");
        });
    };

    $scope.changeApplication = function () {

        $scope.selectedAppRoles = [];
        $scope.selectedAppDefRoles = [];

        // Populate available roles for selected application
        if ($scope.application) {
            var appDefaultRoles = ApplicationRole.query({realm: $scope.realm.realm, application: $scope.application.name}, function () {

                if (!$scope.application.hasOwnProperty('defaultRoles') || $scope.application.defaultRoles === null) {
                    $scope.application.defaultRoles = [];
                }

                $scope.availableAppRoles = [];

                for (var i = 0; i < appDefaultRoles.length; i++) {
                    var roleName = appDefaultRoles[i].name;
                    if ($scope.application.defaultRoles.indexOf(roleName) < 0) {
                        $scope.availableAppRoles.push(roleName);
                    }
                }
            });
        } else {
            $scope.availableAppRoles = null;
        }
    };

    $scope.addAppDefaultRole = function () {

        // Remove selected roles from the app available roles and add them to app default roles (move from left to right).
        for (var i = 0; i < $scope.selectedAppRoles.length; i++) {
            var role = $scope.selectedAppRoles[i];

            var idx = $scope.application.defaultRoles.indexOf(role);
            if (idx < 0) {
                $scope.application.defaultRoles.push(role);
            }

            idx = $scope.availableAppRoles.indexOf(role);

            if (idx != -1) {
                $scope.availableAppRoles.splice(idx, 1);
            }
        }

        // Update/save the selected application with new default roles.
        Application.update({
            realm: $scope.realm.realm,
            application: $scope.application.name
        }, $scope.application, function () {
            Notifications.success("Your changes have been saved to the application.");
        });
    };

    $scope.rmAppDefaultRole = function () {

        // Remove selected roles from the app default roles and add them to app available roles (move from right to left).
        for (var i = 0; i < $scope.selectedAppDefRoles.length; i++) {
            var role = $scope.selectedAppDefRoles[i];
            var idx = $scope.application.defaultRoles.indexOf(role);
            if (idx != -1) {
                $scope.application.defaultRoles.splice(idx, 1);
            }
            idx = $scope.availableAppRoles.indexOf(role);
            if (idx < 0) {
                $scope.availableAppRoles.push(role);
            }
        }

        // Update/save the selected application with new default roles.
        Application.update({
            realm: $scope.realm.realm,
            application: $scope.application.name
        }, $scope.application, function () {
            Notifications.success("Your changes have been saved to the application.");
        });
    };

});

module.controller('RealmSocialCtrl', function($scope, realm, Realm, serverInfo, $location, Notifications) {
    console.log('RealmSocialCtrl');

    $scope.realm = angular.copy(realm);
    $scope.serverInfo = serverInfo;

    $scope.allProviders = serverInfo.socialProviders;
    $scope.configuredProviders = [];

    $scope.$watch('realm.socialProviders', function(socialProviders) {
        $scope.configuredProviders = [];
         for (var providerConfig in socialProviders) {
             var i = providerConfig.split('.');
             if (i.length == 2 && i[1] == 'key') {
                 $scope.configuredProviders.push(i[0]);
             }
         }
    }, true);

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    $scope.callbackUrl = $location.absUrl().replace(/\/admin.*/, "/rest/social/callback");

    $scope.addProvider = function(pId) {
        if (!$scope.realm.socialProviders) {
            $scope.realm.socialProviders = {};
        }

        $scope.realm.socialProviders[pId + ".key"] = "";
        $scope.realm.socialProviders[pId + ".secret"] = "";
    };

    $scope.removeProvider = function(pId) {
        delete $scope.realm.socialProviders[pId+".key"];
        delete $scope.realm.socialProviders[pId+".secret"];
    };

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        realmCopy.social = true;
        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.realm + "/social-settings");
            Notifications.success("Saved changes to realm");
            oldCopy = realmCopy;
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };

});

module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications, TimeUnit) {
    console.log('RealmTokenDetailCtrl');

    $scope.realm = realm;

    $scope.realm.accessTokenLifespanUnit = TimeUnit.autoUnit(realm.accessTokenLifespan);
    $scope.realm.accessTokenLifespan = TimeUnit.toUnit(realm.accessTokenLifespan, $scope.realm.accessTokenLifespanUnit);
    $scope.$watch('realm.accessTokenLifespanUnit', function(to, from) {
        $scope.realm.accessTokenLifespan = TimeUnit.convert($scope.realm.accessTokenLifespan, from, to);
    });

    $scope.realm.centralLoginLifespanUnit = TimeUnit.autoUnit(realm.centralLoginLifespan);
    $scope.realm.centralLoginLifespan = TimeUnit.toUnit(realm.centralLoginLifespan, $scope.realm.centralLoginLifespanUnit);
    $scope.$watch('realm.centralLoginLifespanUnit', function(to, from) {
        $scope.realm.centralLoginLifespan = TimeUnit.convert($scope.realm.centralLoginLifespan, from, to);
    });

    $scope.realm.refreshTokenLifespanUnit = TimeUnit.autoUnit(realm.refreshTokenLifespan);
    $scope.realm.refreshTokenLifespan = TimeUnit.toUnit(realm.refreshTokenLifespan, $scope.realm.refreshTokenLifespanUnit);
    $scope.$watch('realm.refreshTokenLifespanUnit', function(to, from) {
        $scope.realm.refreshTokenLifespan = TimeUnit.convert($scope.realm.refreshTokenLifespan, from, to);
    });

    $scope.realm.accessCodeLifespanUnit = TimeUnit.autoUnit(realm.accessCodeLifespan);
    $scope.realm.accessCodeLifespan = TimeUnit.toUnit(realm.accessCodeLifespan, $scope.realm.accessCodeLifespanUnit);
    $scope.$watch('realm.accessCodeLifespanUnit', function(to, from) {
        $scope.realm.accessCodeLifespan = TimeUnit.convert($scope.realm.accessCodeLifespan, from, to);
    });

    $scope.realm.accessCodeLifespanUserActionUnit = TimeUnit.autoUnit(realm.accessCodeLifespanUserAction);
    $scope.realm.accessCodeLifespanUserAction = TimeUnit.toUnit(realm.accessCodeLifespanUserAction, $scope.realm.accessCodeLifespanUserActionUnit);
    $scope.$watch('realm.accessCodeLifespanUserActionUnit', function(to, from) {
        $scope.realm.accessCodeLifespanUserAction = TimeUnit.convert($scope.realm.accessCodeLifespanUserAction, from, to);
    });


    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        delete realmCopy["accessTokenLifespanUnit"];
        delete realmCopy["refreshTokenLifespanUnit"];
        delete realmCopy["accessCodeLifespanUnit"];
        delete realmCopy["centralLoginLifespanUnit"];
        delete realmCopy["accessCodeLifespanUserActionUnit"];

        realmCopy.accessTokenLifespan = TimeUnit.toSeconds($scope.realm.accessTokenLifespan, $scope.realm.accessTokenLifespanUnit)
        realmCopy.centralLoginLifespan = TimeUnit.toSeconds($scope.realm.centralLoginLifespan, $scope.realm.centralLoginLifespanUnit)
        realmCopy.refreshTokenLifespan = TimeUnit.toSeconds($scope.realm.refreshTokenLifespan, $scope.realm.refreshTokenLifespanUnit)
        realmCopy.accessCodeLifespan = TimeUnit.toSeconds($scope.realm.accessCodeLifespan, $scope.realm.accessCodeLifespanUnit)
        realmCopy.accessCodeLifespanUserAction = TimeUnit.toSeconds($scope.realm.accessCodeLifespanUserAction, $scope.realm.accessCodeLifespanUserActionUnit)

        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.realm + "/token-settings");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };
});

module.controller('RealmKeysDetailCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    $scope.realm = realm;

    $scope.generate = function() {
        Dialog.confirmGenerateKeys($scope.realm.realm, 'realm', function() {
                Realm.update({ realm: realm.realm, publicKey : 'GENERATE' }, function () {
                Notifications.success('New keys generated for realm.');
                Realm.get({ id : realm.realm }, function(updated) {
                    $scope.realm = updated;
                })
            });
        });
    };
});

module.controller('RealmSessionStatsCtrl', function($scope, realm, stats, RealmSessionStats, RealmLogoutAll, Notifications) {
    $scope.realm = realm;
    $scope.stats = stats;

    console.log(stats);

    $scope.logoutAll = function() {
        RealmLogoutAll.save({realm : realm.realm}, function () {
            Notifications.success('Logged out all users');
            RealmSessionStats.get({realm: realm.realm}, function(updated) {
                Notifications.success('Logged out all users');
                $scope.stats = updated;
            })
        });
    };


});


module.controller('RealmRevocationCtrl', function($scope, Realm, RealmPushRevocation, realm, $http, $location, Dialog, Notifications) {
    $scope.realm = angular.copy(realm);

    var setNotBefore = function() {
        if ($scope.realm.notBefore == 0) {
            $scope.notBefore = "None";
        } else {
            $scope.notBefore = new Date($scope.realm.notBefore * 1000);
        }
    };

    setNotBefore();

    var reset = function() {
        Realm.get({ id : realm.realm }, function(updated) {
            $scope.realm = updated;
            setNotBefore();
        })

    };

    $scope.clear = function() {
        Realm.update({ realm: realm.realm, notBefore : 0 }, function () {
            $scope.notBefore = "None";
            Notifications.success('Not Before cleared for realm.');
            reset();
        });
    }
    $scope.setNotBeforeNow = function() {
        Realm.update({ realm: realm.realm, notBefore : new Date().getTime()/1000}, function () {
            Notifications.success('Not Before cleared for realm.');
            reset();
        });
    }
    $scope.pushRevocation = function() {
        RealmPushRevocation.save({ realm: realm.realm}, function () {
            Notifications.success('Push sent for realm.');
        });
    }

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


module.controller('RoleDetailCtrl', function($scope, realm, role, roles, applications,
                                             Role, ApplicationRole, RoleById, RoleRealmComposites, RoleApplicationComposites,
                                             $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.save = function() {
        console.log('save');
        if ($scope.create) {
            Role.save({
                realm: realm.realm
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/roles/" + id);
                Notifications.success("The role has been created.");
            });
        } else {
            $scope.update();
        }
    };

    $scope.remove = function () {
        Dialog.confirmDelete($scope.role.name, 'role', function () {
            $scope.role.$remove({
                realm: realm.realm,
                role: $scope.role.name
            }, function () {
                $location.url("/realms/" + realm.realm + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/roles");
    };



    roleControl($scope, realm, role, roles, applications,
        ApplicationRole, RoleById, RoleRealmComposites, RoleApplicationComposites,
        $http, $location, Notifications, Dialog);
});

module.controller('RealmSMTPSettingsCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmSMTPSettingsCtrl');

    var booleanSmtpAtts = ["auth","ssl","starttls"];

    $scope.realm = realm;

    if ($scope.realm.smtpServer) {
        $scope.realm.smtpServer = typeObject($scope.realm.smtpServer);
    };

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        realmCopy['smtpServer'] = detypeObject(realmCopy.smtpServer);
        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.realm + "/smtp-settings");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };

    /* Convert string attributes containing a boolean to actual boolean type + convert an integer string (port) to integer. */
    function typeObject(obj){
        for (var att in obj){
            if (booleanSmtpAtts.indexOf(att) < 0)
                continue;
            if (obj[att] === "true"){
                obj[att] = true;
            } else if (obj[att] === "false"){
                obj[att] = false;
            }
        }

        obj['port'] = parseInt(obj['port']);

        return obj;
    }

    /* Convert all non-string values to strings to invert changes caused by the typeObject function. */
    function detypeObject(obj){
        for (var att in obj){
            if (booleanSmtpAtts.indexOf(att) < 0)
                continue;
            if (obj[att] === true){
                obj[att] = "true";
            } else if (obj[att] === false){
                obj[att] = "false"
            }
        }

        obj['port'] = obj['port'].toString();

        return obj;
    }
});

module.controller('RealmLdapSettingsCtrl', function($scope, $location, Notifications, Realm, realm) {
    console.log('RealmLdapSettingsCtrl');

    $scope.realm = realm;

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.realm + "/ldap-settings");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };
});

module.controller('RealmAuthSettingsCtrl', function($scope, realm) {
    console.log('RealmAuthSettingsCtrl');

    $scope.realm = realm;
    $scope.authenticationProviders = realm.authenticationProviders;
});

module.controller('RealmAuthSettingsDetailCtrl', function($scope, $routeParams, $location, Notifications, Dialog, Realm, realm, serverInfo) {
    console.log('RealmAuthSettingsDetailCtrl');

    $scope.realm = realm;
    $scope.availableProviders = serverInfo.authProviders;
    $scope.availableProviderNames = Object.keys(serverInfo.authProviders);

    $scope.create = !$routeParams.index;
    $scope.changed = false;

    if ($scope.create) {
        $scope.authProvider = {
            passwordUpdateSupported: true,
            config: {}
        };

        $scope.authProviderOptionNames = [];
    } else {
        $scope.authProvider = realm.authenticationProviders[ $routeParams.index ];
        if (!$scope.authProvider.config) {
            $scope.authProvider.config = {};
        }

        $scope.authProviderOptionNames = serverInfo.authProviders[ $scope.authProvider.providerName ];
        $scope.authProviderIndex = $routeParams.index;
    }

    var oldCopy = angular.copy($scope.authProvider);
    $scope.$watch('authProvider', function() {
        if (!angular.equals($scope.authProvider, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.changeAuthProvider = function() {
        console.log('RealmAuthSettingsDetailCtrl: provider changed to ' + $scope.authProvider.providerName);
        $scope.authProviderOptionNames = serverInfo.authProviders[ $scope.authProvider.providerName ];
    }

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/auth-settings");
    }

    $scope.reset = function() {
        $scope.authProvider = angular.copy(oldCopy);
        $scope.changed = false;
    }

    $scope.save = function() {
        if (!$scope.authProvider.providerName) {
            console.log('RealmAuthSettingsDetailCtrl: no provider selected. Skip creation');
            return;
        }

        console.log('RealmAuthSettingsDetailCtrl: creating provider ' + $scope.authProvider.providerName);
        var realmCopy = angular.copy($scope.realm);
        if (!realmCopy.authenticationProviders) {
            realmCopy.authenticationProviders = [];
        }

        if ($scope.create) {
            realmCopy.authenticationProviders.push($scope.authProvider);
        } else {
            realmCopy.authenticationProviders[ $scope.authProviderIndex ] = $scope.authProvider;
        }

        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.realm + "/auth-settings");
            Notifications.success("Authentication provider has been saved.");
        });
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.realm.authenticationProviders.providerName, 'authentication Provider', function() {
            console.log('RealmAuthSettingsDetailCtrl: deleting provider ' + $scope.authProvider.providerName);

            var realmCopy = angular.copy($scope.realm);
            realmCopy.authenticationProviders.splice($scope.authProviderIndex, 1);

            $scope.changed = false;
            Realm.update(realmCopy, function () {
                $location.url("/realms/" + realm.realm + "/auth-settings");
                Notifications.success("Authentication provider has been deleted.");
            });
        });
    };
});

module.controller('RealmAuditCtrl', function($scope, auditConfig, RealmAudit, RealmAuditEvents, realm, serverInfo, $location, Notifications, TimeUnit, Dialog) {
    $scope.realm = realm;

    $scope.auditConfig = auditConfig;

    $scope.auditConfig.expirationUnit = TimeUnit.autoUnit(auditConfig.auditExpiration);
    $scope.auditConfig.auditExpiration = TimeUnit.toUnit(auditConfig.auditExpiration, $scope.auditConfig.expirationUnit);
    $scope.$watch('auditConfig.expirationUnit', function(to, from) {
        if ($scope.auditConfig.auditExpiration) {
            $scope.auditConfig.auditExpiration = TimeUnit.convert($scope.auditConfig.auditExpiration, from, to);
        }
    });

    $scope.auditListeners = serverInfo.auditListeners;

    var oldCopy = angular.copy($scope.auditConfig);
    $scope.changed = false;

    $scope.$watch('auditConfig', function() {
        if (!angular.equals($scope.auditConfig, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        $scope.changed = false;

        var copy = angular.copy($scope.auditConfig)
        delete copy['expirationUnit'];

        copy.auditExpiration = TimeUnit.toSeconds($scope.auditConfig.auditExpiration, $scope.auditConfig.expirationUnit);

        RealmAudit.update({
            id : realm.realm
        }, copy, function () {
            $location.url("/realms/" + realm.realm + "/audit-settings");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.auditConfig = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.clearAudit = function() {
        Dialog.confirmDelete($scope.realm.realm, 'audit events', function() {
            RealmAuditEvents.remove({ id : $scope.realm.realm }, function() {
                Notifications.success("The audit events has been cleared.");
            });
        });
    };
});

module.controller('RealmAuditEventsCtrl', function($scope, RealmAuditEvents, realm) {
    $scope.realm = realm;
    $scope.page = 0;

    $scope.query = {
        id : realm.realm,
        max : 5,
        first : 0
    }

    $scope.update = function() {
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
           }
        }
        $scope.events = RealmAuditEvents.query($scope.query);
    }

    $scope.firstPage = function() {
        $scope.query.first = 0;
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.update();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.update();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.update();
    }

    $scope.update();
});

module.controller('RealmBruteForceCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications, TimeUnit) {
    console.log('RealmBruteForceCtrl');

    $scope.realm = realm;

    $scope.realm.waitIncrementUnit = TimeUnit.autoUnit(realm.waitIncrementSeconds);
    $scope.realm.waitIncrement = TimeUnit.toUnit(realm.waitIncrementSeconds, $scope.realm.waitIncrementUnit);
    $scope.$watch('realm.waitIncrementUnit', function(to, from) {
        $scope.realm.waitIncrement = TimeUnit.convert($scope.realm.waitIncrement, from, to);
    });

    $scope.realm.minimumQuickLoginWaitUnit = TimeUnit.autoUnit(realm.minimumQuickLoginWaitSeconds);
    $scope.realm.minimumQuickLoginWait = TimeUnit.toUnit(realm.minimumQuickLoginWaitSeconds, $scope.realm.minimumQuickLoginWaitUnit);
    $scope.$watch('realm.minimumQuickLoginWaitUnit', function(to, from) {
        $scope.realm.minimumQuickLoginWait = TimeUnit.convert($scope.realm.minimumQuickLoginWait, from, to);
    });

    $scope.realm.maxFailureWaitUnit = TimeUnit.autoUnit(realm.maxFailureWaitSeconds);
    $scope.realm.maxFailureWait = TimeUnit.toUnit(realm.maxFailureWaitSeconds, $scope.realm.maxFailureWaitUnit);
    $scope.$watch('realm.maxFailureWaitUnit', function(to, from) {
        $scope.realm.maxFailureWait = TimeUnit.convert($scope.realm.maxFailureWait, from, to);
    });

    $scope.realm.maxDeltaTimeUnit = TimeUnit.autoUnit(realm.maxDeltaTimeSeconds);
    $scope.realm.maxDeltaTime = TimeUnit.toUnit(realm.maxDeltaTimeSeconds, $scope.realm.maxDeltaTimeUnit);
    $scope.$watch('realm.maxDeltaTimeUnit', function(to, from) {
        $scope.realm.maxDeltaTime = TimeUnit.convert($scope.realm.maxDeltaTime, from, to);
    });

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        delete realmCopy["waitIncrementUnit"];
        delete realmCopy["waitIncrement"];
        delete realmCopy["minimumQuickLoginWaitUnit"];
        delete realmCopy["minimumQuickLoginWait"];
        delete realmCopy["maxFailureWaitUnit"];
        delete realmCopy["maxFailureWait"];
        delete realmCopy["maxDeltaTimeUnit"];
        delete realmCopy["maxDeltaTime"];

        realmCopy.waitIncrementSeconds = TimeUnit.toSeconds($scope.realm.waitIncrement, $scope.realm.waitIncrementUnit)
        realmCopy.minimumQuickLoginWaitSeconds = TimeUnit.toSeconds($scope.realm.minimumQuickLoginWait, $scope.realm.minimumQuickLoginWaitUnit)
        realmCopy.maxFailureWaitSeconds = TimeUnit.toSeconds($scope.realm.maxFailureWait, $scope.realm.maxFailureWaitUnit)
        realmCopy.maxDeltaTimeSeconds = TimeUnit.toSeconds($scope.realm.maxDeltaTime, $scope.realm.maxDeltaTimeUnit)

        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.realm + "/sessions/brute-force");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };
});


