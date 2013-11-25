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
        $scope.fragment = $location.path();
        $scope.path = $location.path().substring(1).split("/");
    });

    $http.get('/auth-server/rest/saas/admin/realms').success(function(data) {
        Current.realms = data;
        if (data.length > 0) {
            Current.realm = data[0];
            //$location.url("/realms/" + Current.realm.id);
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
                    Notifications.success("The realm has been created.");
                    $scope.social = $scope.realm.social;
                    $scope.registrationAllowed = $scope.realm.registrationAllowed;
                });
            });
        } else {
            console.log('updating realm...');
            $scope.changed = false;
            Realm.update(realmCopy, function () {
                var id = realmCopy.id;
                var data = Realm.query(function () {
                    Current.realms = data;
                    for (var i = 0; i < Current.realms.length; i++) {
                        if (Current.realms[i].id == id) {
                            Current.realm = Current.realms[i];
                            oldCopy = angular.copy($scope.realm);
                        }
                    }
                });
                $location.url("/realms/" + id);
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
            Realm.remove($scope.realm, function() {
                Current.realms = Realm.get();
                $location.url("/realms");
                Notifications.success("The realm has been deleted.");
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
        requiredOAuthClientCredentials : realm.requiredOAuthClientCredentials,
        registrationAllowed : realm.registrationAllowed
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
        var realmCopy = angular.copy($scope.realm);
        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.id + "/required-credentials");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };
});

module.controller('RealmRegistrationCtrl', function ($scope, Realm, realm, applications, roles, Notifications, ApplicationRole, Application) {

    console.log('RealmRegistrationCtrl');

    $scope.realm = realm;

    $scope.availableRealmRoles = [];
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmDefRoles = [];

    $scope.applications = angular.copy(applications);

    $scope.availableAppRoles = [];
    $scope.selectedAppRoles = [];
    $scope.selectedAppDefRoles = [];

    if (!$scope.realm.hasOwnProperty('defaultRoles') || $scope.realm.defaultRoles === null) {
        $scope.realm.defaultRoles = [];
    }

    // Populate available roles. Available roles are neither already assigned or system roles.
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
        var appDefaultRoles = ApplicationRole.query({realm: $scope.realm.id, application: $scope.application.id}, function () {

            if (!$scope.application.hasOwnProperty('defaultRoles') || $scope.application.defaultRoles === null) {
                $scope.application.defaultRoles = [];
            }

            $scope.availableAppRoles = [];

            for (var i = 0; i < appDefaultRoles.length; i++) {
                var roleName = appDefaultRoles[i].name;

                if (systemRoles.indexOf(roleName) < 0 && $scope.application.defaultRoles.indexOf(roleName) < 0) {
                    $scope.availableAppRoles.push(roleName);
                }
            }
        });
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
            realm: $scope.realm.id,
            id: $scope.application.id
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
            realm: $scope.realm.id,
            id: $scope.application.id
        }, $scope.application, function () {
            Notifications.success("Your changes have been saved to the application.");
        });
    };

});

module.controller('RealmSocialCtrl', function($scope, realm, Realm, $location, Notifications) {
    console.log('RealmSocialCtrl');

    $scope.realm = { id : realm.id, realm : realm.realm, social : realm.social, registrationAllowed : realm.registrationAllowed,
        tokenLifespan : realm.tokenLifespan,  accessCodeLifespan : realm.accessCodeLifespan };

    if (!realm["socialProviders"]){
        $scope.realm["socialProviders"] = {};
    } else {
        $scope.realm["socialProviders"] = realm.socialProviders;
    }

    // Hardcoded provider list in form of map providerId:providerName
    $scope.allProviders = { google:"Google", facebook:"Facebook", twitter:"Twitter" };
    $scope.availableProviders = [];

    for (var provider in $scope.allProviders){
        $scope.availableProviders.push(provider);
    }

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    $scope.callbackUrl = $location.absUrl().replace(/\/admin.*/, "/rest/social/callback");

    // To get rid of the "undefined" option in the provider select list
    // Setting the 1st option from the list (if the list is not empty)
    var selectFirstProvider = function(){
        if ($scope.unsetProviders.length > 0){
            $scope.newProviderId = $scope.unsetProviders[0];
        } else {
            $scope.newProviderId = null;
        }
    }

    // Fill in configured providers
    var initSocial = function() {
        // postSaveProviders is used for remembering providers which were already validated after pressing the save button
        // thanks to this it's easy to distinguish between newly added fields and those already tried to be saved
        $scope.postSaveProviders = [];
        $scope.unsetProviders = [];
        $scope.configuredProviders = [];

        for (var providerConfig in $scope.realm.socialProviders){
            // Get the provider ID which is before the '.' (i.e. google in google.key or google.secret)
            if ($scope.realm.socialProviders.hasOwnProperty(providerConfig)){
                var pId = providerConfig.split('.')[0];
                if ($scope.configuredProviders.indexOf(pId) < 0){
                    $scope.configuredProviders.push(pId);
                }
            }
        }

        // If no providers are already configured, you can add any of them
        if ($scope.configuredProviders.length == 0){
            $scope.unsetProviders = $scope.availableProviders.slice(0);
        } else {
            for (var i = 0; i < $scope.availableProviders.length; i++){
                var providerId = $scope.availableProviders[i];
                if ($scope.configuredProviders.indexOf(providerId) < 0){
                    $scope.unsetProviders.push(providerId);
                }
            }
        }

        selectFirstProvider();
    };

    initSocial();

    $scope.addProvider = function() {
        if ($scope.availableProviders.indexOf($scope.newProviderId) > -1){
            $scope.realm.socialProviders[$scope.newProviderId+".key"]="";
            $scope.realm.socialProviders[$scope.newProviderId+".secret"]="";
            $scope.configuredProviders.push($scope.newProviderId);
            $scope.unsetProviders.remove($scope.unsetProviders.indexOf($scope.newProviderId));
            selectFirstProvider();
        }
    };

    $scope.removeProvider = function(pId) {
        delete $scope.realm.socialProviders[pId+".key"];
        delete $scope.realm.socialProviders[pId+".secret"];
        $scope.configuredProviders.remove($scope.configuredProviders.indexOf(pId));

        // Removing from postSaveProviders, so the empty fields are not red if the provider is added to the list again
        var rId = $scope.postSaveProviders.indexOf(pId);
        if (rId > -1){
            $scope.postSaveProviders.remove(rId)
        }

        $scope.unsetProviders.push(pId);
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
            $location.url("/realms/" + realm.id + "/social-settings");
            Notifications.success("Saved changes to realm");
            oldCopy = realmCopy;
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
        // Initialize lists of configured and unset providers again
        initSocial();
    };

    $scope.openHelp = function(pId) {
        $scope.helpPId = pId;
        $scope.providerHelpModal = true;
    };

    $scope.closeHelp = function() {
        $scope.providerHelpModal = false;
    };

});

module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmTokenDetailCtrl');

    $scope.realm = { id : realm.id, realm : realm.realm, social : realm.social, registrationAllowed : realm.registrationAllowed,
        tokenLifespan : realm.tokenLifespan,  accessCodeLifespan : realm.accessCodeLifespan,
        accessCodeLifespanUserAction : realm.accessCodeLifespanUserAction  };

    $scope.realm.tokenLifespanUnit = 'Seconds';
    $scope.realm.accessCodeLifespanUnit = 'Seconds';
    $scope.realm.accessCodeLifespanUserActionUnit = 'Seconds';

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        delete realmCopy["tokenLifespanUnit"];
        delete realmCopy["accessCodeLifespanUnit"];
        delete realmCopy["accessCodeLifespanUserActionUnit"];
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
        if ($scope.realm.accessCodeLifespanUserActionUnit == 'Minutes') {
            realmCopy.accessCodeLifespanUserAction = $scope.realm.accessCodeLifespanUserAction * 60;
        } else if ($scope.realm.accessCodeLifespanUserActionUnit == 'Hours') {
            realmCopy.accessCodeLifespanUserAction = $scope.realm.accessCodeLifespanUserAction * 60 * 60;
        } else if ($scope.realm.accessCodeLifespanUserActionUnit == 'Days') {
            realmCopy.accessCodeLifespanUserAction = $scope.realm.accessCodeLifespanUserAction * 60 * 60 * 24;
        }
        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.id + "/token-settings");
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
                Realm.update({ id: realm.id, publicKey : 'GENERATE' }, function () {
                Notifications.success('New keys generated for realm.');
                Realm.get({ id : realm.id }, function(updated) {
                    $scope.realm = updated;
                })
            });
        });
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
                Notifications.success("The role has been created.");
            });
        } else {
            Role.update({
                realm : realm.id,
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
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.id,
                roleId : $scope.role.id
            }, function() {
                $location.url("/realms/" + realm.id + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };
});

module.controller('RealmSMTPSettingsCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications) {
    $scope.realm = {
        id : realm.id, realm : realm.realm, social : realm.social, registrationAllowed : realm.registrationAllowed,
        smtpServer: realm.smtpServer
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
        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $location.url("/realms/" + realm.id + "/smtp-settings");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };
});