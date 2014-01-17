module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications) {
    $scope.addMessage = function() {
        Notifications.success("test");
    };

    $scope.auth = Auth;
    $http.get('/auth/rest/admin/whoami').success(function(data, status) {
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
});

module.controller('HomeCtrl', function(Realm, $location) {
    Realm.query(null, function(realms) {
        var realm;
        if (realms.length == 1) {
            realm = realms[0].realm;
        } else if (realms.length == 2) {
            if (realms[0].realm == 'keycloak-admin') {
                realm = realms[1].realm;
            } else if (realms[1].realm == 'administration') {
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
    $scope.changeRealm = function() {
        $location.url("/realms/" + $scope.current.realm.realm);
    };
    $scope.showNav = function() {
        var show = Current.realms.length > 0;
        return Auth.loggedIn && show;
    }
    $scope.refresh = function() {
         Current.refresh();
    }
});

module.controller('RealmCreateCtrl', function($scope, Current, Realm, $upload, $http, $location, Dialog, Notifications) {
    console.log('RealmCreateCtrl');

    $scope.realm = {
        enabled: true
    };

    $scope.changed = false;
    $scope.files = [];

    var oldCopy = angular.copy($scope.realm);

    $scope.onFileSelect = function($files) {
        $scope.files = $files;
    };

    $scope.changeFileSelect = function() {
        $scope.files = null;
        document.getElementById('import-file').click();
    }

    $scope.uploadFile = function() {
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $scope.files.length; i++) {
            var $file = $scope.files[i];
            $scope.upload = $upload.upload({
                url: '/auth/rest/admin/realms', //upload.php script, node.js route, or servlet url
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
                }).success(function(data, status, headers, config) {
                    Notifications.success("The realm has been uploaded.");
                    $location.url("/realms");
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
        Realm.create(realmCopy, function(data, headers) {
            var data = Realm.query(function() {
                Current.realms = data;
                for (var i = 0; i < Current.realms.length; i++) {
                    if (Current.realms[i].realm == realmCopy.realm) {
                        Current.realm = Current.realms[i];
                    }
                }
                $location.url("/realms/" + realmCopy.realm);
                Notifications.success("The realm has been created.");
            });
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };
});


module.controller('RealmDetailCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications) {
    $scope.createRealm = !realm.realm;

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

module.controller('RealmRequiredCredentialsCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmRequiredCredentialsCtrl');

    $scope.realm = {
        id : realm.realm, realm : realm.realm, social : realm.social,
        requiredCredentials : realm.requiredCredentials,
        requiredApplicationCredentials : realm.requiredApplicationCredentials,
        requiredOAuthClientCredentials : realm.requiredOAuthClientCredentials,
        registrationAllowed : realm.registrationAllowed
    };

    if (realm.hasOwnProperty('passwordPolicy')){
        $scope.realm['passwordPolicy'] = realm.passwordPolicy;
    } else {
        $scope.realm['passwordPolicy'] = "";
        realm['passwordPolicy'] = "";
    }

    var oldCopy = angular.copy($scope.realm);

    /* Map used in the table when hovering over (i) icon */
    $scope.policyMessages = {
        length:         "Minimal password length (integer type). Default value is 8.",
        digits:         "Minimal number (integer type) of digits in password. Default value is 1.",
        lowerCase:      "Minimal number (integer type) of lowercase characters in password. Default value is 1.",
        upperCase:      "Minimal number (integer type) of uppercase characters in password. Default value is 1.",
        specialChars:   "Minimal number (integer type) of special characters in password. Default value is 1."
    }

    // $scope.policy is an object representing passwordPolicy string
    $scope.policy = {};
    // All available policies
    $scope.allPolicies = ['length', 'digits', 'lowerCase', 'upperCase', 'specialChars'];
    // List of configured policies
    $scope.configuredPolicies = [];
    // List of not configured policies
    $scope.availablePolicies = $scope.allPolicies.slice(0);

    $scope.addPolicy = function(){
        $scope.policy[$scope.newPolicyId] = "";
        updateConfigured();
    }

    $scope.removePolicy = function(pId){
        delete $scope.policy[pId];
        updateConfigured();
    }

    // Updating lists of configured and non-configured policies based on the $scope.policy object
    var updateConfigured = function(){

        for (var i = 0; i < $scope.allPolicies.length; i++){

            var policy = $scope.allPolicies[i];

            if($scope.policy.hasOwnProperty(policy)){

                var ind = $scope.configuredPolicies.indexOf(policy);

                if(ind < 0){
                    $scope.configuredPolicies.push(policy);
                }

                ind = $scope.availablePolicies.indexOf(policy);
                if(ind > -1){
                    $scope.availablePolicies.splice(ind, 1);
                }
            } else {

                var ind = $scope.configuredPolicies.indexOf(policy);

                if(ind > -1){
                    $scope.configuredPolicies.splice(ind, 1);
                }

                ind = $scope.availablePolicies.indexOf(policy);
                if(ind < 0){
                    $scope.availablePolicies.push(policy);
                }
            }
        }

        if ($scope.availablePolicies.length > 0){
            $scope.newPolicyId = $scope.availablePolicies[0];
        }
    }

    // Creating object from policy string
    var evaluatePolicy = function(policyString){

        var policyObject = {};

        if (!policyString || policyString.length == 0){
            return policyObject;
        }

        var policyArray = policyString.split(" and ");

        for (var i = 0; i < policyArray.length; i ++){
            var policyToken = policyArray[i];
            var re = /(\w+)\(*(\d*)\)*/;

            var policyEntry = re.exec(policyToken);
            policyObject[policyEntry[1]] = policyEntry[2];
        }

        return policyObject;
    }

    // Creating policy string based on policy object
    var generatePolicy = function(policyObject){
        var policyString = "";

        for (var key in policyObject){
            policyString += key;
            var value = policyObject[key];
            if ( value != ""){
                policyString += "("+value+")";
            }
            policyString += " and ";
        }

        policyString = policyString.substring(0, policyString.length - 5);

        return policyString;
    }

    $scope.policy = evaluatePolicy(realm.passwordPolicy);
    updateConfigured();

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

    $scope.$watch('policy', function() {
        $scope.realm.passwordPolicy = generatePolicy($scope.policy);
        if ($scope.realm.passwordPolicy != realm.passwordPolicy){
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        $scope.changed = false;

        Realm.update($scope.realm, function () {
            $location.url("/realms/" + realm.realm + "/required-credentials");
            Notifications.success("Your changes have been saved to the realm.");
            oldCopy = angular.copy($scope.realm);
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);

        $scope.configuredPolicies = [];
        $scope.availablePolicies = $scope.allPolicies.slice(0);
        $scope.policy = evaluatePolicy(oldCopy.passwordPolicy);
        updateConfigured();

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
            id: $scope.application.name
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
            id: $scope.application.name
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
            $scope.unsetProviders.splice($scope.unsetProviders.indexOf($scope.newProviderId),1);
            selectFirstProvider();
        }
    };

    $scope.removeProvider = function(pId) {
        delete $scope.realm.socialProviders[pId+".key"];
        delete $scope.realm.socialProviders[pId+".secret"];
        $scope.configuredProviders.splice($scope.configuredProviders.indexOf(pId),1);

        // Removing from postSaveProviders, so the empty fields are not red if the provider is added to the list again
        var rId = $scope.postSaveProviders.indexOf(pId);
        if (rId > -1){
            $scope.postSaveProviders.splice(rId,1)
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
            $location.url("/realms/" + realm.realm + "/social-settings");
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

});

module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications, TimeUnit) {
    console.log('RealmTokenDetailCtrl');

    $scope.realm = { id : realm.id, realm : realm.realm, social : realm.social, registrationAllowed : realm.registrationAllowed };

    $scope.realm.tokenLifespanUnit = TimeUnit.autoUnit(realm.tokenLifespan);
    $scope.realm.tokenLifespan = TimeUnit.toUnit(realm.tokenLifespan, $scope.realm.tokenLifespanUnit);
    $scope.$watch('realm.tokenLifespanUnit', function(to, from) {
        $scope.realm.tokenLifespan = TimeUnit.convert($scope.realm.tokenLifespan, from, to);
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
        delete realmCopy["tokenLifespanUnit"];
        delete realmCopy["accessCodeLifespanUnit"];
        delete realmCopy["accessCodeLifespanUserActionUnit"];

        realmCopy.tokenLifespan = TimeUnit.toSeconds($scope.realm.tokenLifespan, $scope.realm.tokenLifespanUnit)
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
                Realm.update({ id: realm.realm, publicKey : 'GENERATE' }, function () {
                Notifications.success('New keys generated for realm.');
                Realm.get({ id : realm.realm }, function(updated) {
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
            Role.update({
                realm : realm.realm,
                role : role.name
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
        $location.url("/realms/" + realm.realm + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.realm,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.realm + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };
});

module.controller('RealmSMTPSettingsCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmSMTPSettingsCtrl');

    var booleanSmtpAtts = ["auth","ssl","starttls"];

    $scope.realm = {
        id : realm.id, realm : realm.realm, social : realm.social, registrationAllowed : realm.registrationAllowed,
        smtpServer: typeObject(realm.smtpServer)
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