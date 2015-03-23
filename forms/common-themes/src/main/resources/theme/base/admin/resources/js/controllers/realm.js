module.controller('GlobalCtrl', function($scope, $http, Auth, WhoAmI, Current, $location, Notifications, ServerInfo) {
    $scope.addMessage = function() {
        Notifications.success("test");
    };

    $scope.authUrl = authUrl;
    $scope.resourceUrl = resourceUrl;
    $scope.auth = Auth;
    $scope.serverInfo = ServerInfo.get();
    $scope.serverInfoUpdate = function() {
        $scope.serverInfo = ServerInfo.get();
    };

    function hasAnyAccess() {
        var realmAccess = Auth.user && Auth.user['realm_access'];
        if (realmAccess) {
            for (var p in realmAccess){
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    WhoAmI.get(function (data) {
        Auth.user = data;
        Auth.loggedIn = true;
        Auth.hasAnyAccess = hasAnyAccess();
    });

    function getAccess(role) {
        if (!Current.realm) {
            return false;
        }

        var realmAccess = Auth.user && Auth.user['realm_access'];
        if (realmAccess) {
            realmAccess = realmAccess[Current.realm.realm];
            if (realmAccess) {
                return realmAccess.indexOf(role) >= 0;
            }
        }
        return false;
    }

    $scope.access = {
        get createRealm() {
            return Auth.user && Auth.user.createRealm;
        },

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

        get viewEvents() {
            return getAccess('view-events') || this.manageClients;
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

        get manageEvents() {
            return getAccess('manage-events');
        }
    }

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

module.controller('RealmCreateCtrl', function($scope, Current, Realm, $upload, $http, WhoAmI, $location, Dialog, Notifications, Auth) {
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
                url: authUrl + '/admin/realms', //upload.php script, node.js route, or servlet url
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


                        WhoAmI.get(function(user) {
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
        console.log('creating new realm **');
        Realm.create(realmCopy, function() {
            Realm.query(function(data) {
                Current.realms = data;

                WhoAmI.get(function(user) {
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
            sslRequired: 'external'
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
    }

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

function genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications, url) {
    $scope.realm = angular.copy(realm);
    $scope.serverInfo = serverInfo;
    $scope.registrationAllowed = $scope.realm.registrationAllowed;

    var oldCopy = angular.copy($scope.realm);

    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
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
            $location.url(url);
            Notifications.success("Your changes have been saved to the realm.");
            $scope.registrationAllowed = $scope.realm.registrationAllowed;
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

}

module.controller('DefenseHeadersCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications, "/realms/" + realm.realm + "/defense/headers");
});

module.controller('RealmLoginSettingsCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications, "/realms/" + realm.realm + "/login-settings");
});

module.controller('RealmThemeCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications, "/realms/" + realm.realm + "/theme-settings");

    $scope.supportedLocalesOptions = {
        'multiple' : true,
        'simple_tags' : true,
        'tags' : ['en', 'de']
    };

    $scope.$watch('realm.supportedLocales', function(oldVal, newVal) {
        if(angular.isUndefined(newVal) || (angular.isArray(newVal) && newVal.length == 0)){
            $scope.realm.defaultLocale = undefined;
        }
    }, true);
});

module.controller('RealmCacheCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $location, Dialog, Notifications, "/realms/" + realm.realm + "/cache-settings");
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
        'tags' : ['password', 'totp', 'cert', 'kerberos']
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
            var appDefaultRoles = ApplicationRole.query({realm: $scope.realm.realm, application: $scope.application.id}, function () {

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
            application: $scope.application.id
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
            application: $scope.application.id
        }, $scope.application, function () {
            Notifications.success("Your changes have been saved to the application.");
        });
    };

});

module.controller('RealmIdentityProviderCtrl', function($scope, $filter, $upload, $http, realm, instance, providerFactory, IdentityProvider, serverInfo, $location, Notifications, Dialog) {
    console.log('RealmIdentityProviderCtrl');

    $scope.realm = angular.copy(realm);

    $scope.initSamlProvider = function() {
        $scope.nameIdFormats = [
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
                name: "Transient"
            },
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
                name: "Persistent"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                name: "Email"

            },
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos",
                name: "Kerberos"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
                name: "X.509 Subject Name"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName",
                name: "Windows Domain Qualified Name"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
                name: "Unspecified"

            }
        ];
        if (instance && instance.alias) {

        } else {
            $scope.identityProvider.config.nameIDPolicyFormat = $scope.nameIdFormats[0].format;
        }
    }

    $scope.hidePassword = true;

    if (instance && instance.alias) {
        $scope.identityProvider = angular.copy(instance);
        $scope.newIdentityProvider = false;
    } else {
        $scope.identityProvider = {};
        $scope.identityProvider.config = {};
        $scope.identityProvider.alias = providerFactory.name;
        $scope.identityProvider.providerId = providerFactory.id;
        $scope.identityProvider.enabled = true;
        $scope.identityProvider.updateProfileFirstLogin = true;
        $scope.identityProvider.authenticateByDefault = false;
        $scope.newIdentityProvider = true;
    }

    $scope.changed = $scope.newIdentityProvider;

    $scope.$watch('identityProvider', function() {
        if (!angular.equals($scope.identityProvider, instance)) {
            $scope.changed = true;
        }
    }, true);


    $scope.serverInfo = serverInfo;

    $scope.allProviders = angular.copy(serverInfo.identityProviders);

    $scope.configuredProviders = angular.copy(realm.identityProviders);

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });


    $scope.files = [];
    $scope.importFile = false;
    $scope.importUrl = false;

    $scope.onFileSelect = function($files) {
        $scope.importFile = true;
        $scope.files = $files;
    };

    $scope.clearFileSelect = function() {
        $scope.importUrl = false;
        $scope.importFile = false;
        $scope.files = null;
    };

    var setConfig = function(data) {
        for (var key in data) {
            $scope.identityProvider.config[key] = data[key];
        }
    }


    $scope.uploadFile = function() {
        if (!$scope.identityProvider.alias) {
            Notifications.error("You must specify an alias");
            return;
        }
        var input = {
            providerId: providerFactory.id
        }
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $scope.files.length; i++) {
            var $file = $scope.files[i];
            $scope.upload = $upload.upload({
                url: authUrl + '/admin/realms/' + realm.realm + '/identity-provider/import',
                // method: POST or PUT,
                // headers: {'headerKey': 'headerValue'}, withCredential: true,
                data: input,
                file: $file
                /* set file formData name for 'Content-Desposition' header. Default: 'file' */
                //fileFormDataName: myFile,
                /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
                //formDataAppender: function(formData, key, val){}
            }).progress(function(evt) {
                console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
            }).success(function(data, status, headers) {
                setConfig(data);
                $scope.clearFileSelect();
                Notifications.success("The IDP metadata has been loaded from file.");
            }).error(function() {
                Notifications.error("The file can not be uploaded. Please verify the file.");
            });
        }
    };

    $scope.importFrom = function() {
        if (!$scope.identityProvider.alias) {
            Notifications.error("You must specify an alias");
            return;
        }
        var input = {
            fromUrl: $scope.fromUrl,
            providerId: providerFactory.id
        }
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/identity-provider/import-config', input)
            .success(function(data, status, headers) {
                setConfig(data);
                $scope.fromUrl = null;
                $scope.importUrl = false;
                Notifications.success("Imported config information from url.");
            }).error(function() {
                Notifications.error("Config can not be imported. Please verify the url.");
            });
    };
    $scope.$watch('fromUrl', function(newVal, oldVal){
        if ($scope.fromUrl && $scope.fromUrl.length > 0) {
            $scope.importUrl = true;
        } else{
            $scope.importUrl = false;
        }
    });

    $scope.$watch('configuredProviders', function(configuredProviders) {
        if (configuredProviders) {
            $scope.configuredProviders = angular.copy(configuredProviders);

            for (var j = 0; j < configuredProviders.length; j++) {
                var configProvidedId = configuredProviders[j].providerId;

                for (var i in $scope.allProviders) {
                    var provider = $scope.allProviders[i];

                    if (provider.groupName == 'Social' && (provider.id == configProvidedId)) {
                        $scope.allProviders.splice(i, 1);
                        break;
                    }
                }
            }
        }
    }, true);

    $scope.callbackUrl = $location.absUrl().replace(/\/admin.*/, "/realms/") + realm.realm + "/broker/" ;

    $scope.addProvider = function(provider) {
        console.log('addProvider');
        $location.url("/create/identity-provider/" + realm.realm + "/" + provider.id);
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.identityProvider.alias, 'provider', function() {
            $scope.identityProvider.$remove({
                realm : realm.realm,
                alias : $scope.identityProvider.alias
            }, function() {
                $location.url("/realms/" + realm.realm + "/identity-provider-settings");
                Notifications.success("The application has been deleted.");
            });
        });
    };

    $scope.save = function() {
        if ($scope.newIdentityProvider) {
            if (!$scope.identityProvider.alias) {
                Notifications.error("You must specify an alias");
                return;
            }
            IdentityProvider.save({
                realm: $scope.realm.realm, alias: ''
            }, $scope.identityProvider, function () {
                $location.url("/realms/" + realm.realm + "/identity-provider-settings");
                Notifications.success("The " + $scope.identityProvider.name + " provider has been created.");
            });
        } else {
            IdentityProvider.update({
                realm: $scope.realm.realm,
                id: $scope.identityProvider.internalId
            }, $scope.identityProvider, function () {
                $location.url("/realms/" + realm.realm + "/identity-provider-settings");
                Notifications.success("The " + $scope.identityProvider.name + " provider has been update.");
            });
        }
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/identity-provider-settings");
    };


    $scope.reset = function() {
        $scope.identityProvider = {};
        $scope.configuredProviders = angular.copy($scope.realm.identityProviders);
    };

    $scope.showPassword = function(flag) {
        $scope.hidePassword = flag;
    };

});

module.controller('RealmIdentityProviderExportCtrl', function(realm, identityProvider, $scope, $http, IdentityProviderExport) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.download = null;
    $scope.exported = "";
    $scope.exportedType = "";

    var url = IdentityProviderExport.url({realm: realm.realm, alias: identityProvider.alias}) ;
    $http.get(url).success(function(data, status, headers, config) {
        $scope.exportedType = headers('Content-Type');
        $scope.exported = data;
    });

    $scope.download = function() {
        var suffix = "txt";
        if ($scope.exportedType == 'application/xml') {
            suffix = 'xml';
        } else if ($scope.exportedType == 'application/json') {
            suffix = 'json';
        }
        saveAs(new Blob([$scope.exported], { type: $scope.exportedType }), 'keycloak.' + suffix);
    }
});

module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, TimeUnit) {
    console.log('RealmTokenDetailCtrl');

    $scope.realm = realm;

    $scope.realm.accessTokenLifespanUnit = TimeUnit.autoUnit(realm.accessTokenLifespan);
    $scope.realm.accessTokenLifespan = TimeUnit.toUnit(realm.accessTokenLifespan, $scope.realm.accessTokenLifespanUnit);
    $scope.$watch('realm.accessTokenLifespanUnit', function(to, from) {
        $scope.realm.accessTokenLifespan = TimeUnit.convert($scope.realm.accessTokenLifespan, from, to);
    });

    $scope.realm.ssoSessionIdleTimeoutUnit = TimeUnit.autoUnit(realm.ssoSessionIdleTimeout);
    $scope.realm.ssoSessionIdleTimeout = TimeUnit.toUnit(realm.ssoSessionIdleTimeout, $scope.realm.ssoSessionIdleTimeoutUnit);
    $scope.$watch('realm.ssoSessionIdleTimeoutUnit', function(to, from) {
        $scope.realm.ssoSessionIdleTimeout = TimeUnit.convert($scope.realm.ssoSessionIdleTimeout, from, to);
    });

    $scope.realm.ssoSessionMaxLifespanUnit = TimeUnit.autoUnit(realm.ssoSessionMaxLifespan);
    $scope.realm.ssoSessionMaxLifespan = TimeUnit.toUnit(realm.ssoSessionMaxLifespan, $scope.realm.ssoSessionMaxLifespanUnit);
    $scope.$watch('realm.ssoSessionMaxLifespanUnit', function(to, from) {
        $scope.realm.ssoSessionMaxLifespan = TimeUnit.convert($scope.realm.ssoSessionMaxLifespan, from, to);
    });

    $scope.realm.accessCodeLifespanUnit = TimeUnit.autoUnit(realm.accessCodeLifespan);
    $scope.realm.accessCodeLifespan = TimeUnit.toUnit(realm.accessCodeLifespan, $scope.realm.accessCodeLifespanUnit);
    $scope.$watch('realm.accessCodeLifespanUnit', function(to, from) {
        $scope.realm.accessCodeLifespan = TimeUnit.convert($scope.realm.accessCodeLifespan, from, to);
    });

    $scope.realm.accessCodeLifespanLoginUnit = TimeUnit.autoUnit(realm.accessCodeLifespanLogin);
    $scope.realm.accessCodeLifespanLogin = TimeUnit.toUnit(realm.accessCodeLifespanLogin, $scope.realm.accessCodeLifespanLoginUnit);
    $scope.$watch('realm.accessCodeLifespanLoginUnit', function(to, from) {
        $scope.realm.accessCodeLifespanLogin = TimeUnit.convert($scope.realm.accessCodeLifespanLogin, from, to);
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
        delete realmCopy["ssoSessionMaxLifespanUnit"];
        delete realmCopy["accessCodeLifespanUnit"];
        delete realmCopy["ssoSessionIdleTimeoutUnit"];
        delete realmCopy["accessCodeLifespanUserActionUnit"];
        delete realmCopy["accessCodeLifespanLoginUnit"];

        realmCopy.accessTokenLifespan = TimeUnit.toSeconds($scope.realm.accessTokenLifespan, $scope.realm.accessTokenLifespanUnit)
        realmCopy.ssoSessionIdleTimeout = TimeUnit.toSeconds($scope.realm.ssoSessionIdleTimeout, $scope.realm.ssoSessionIdleTimeoutUnit)
        realmCopy.ssoSessionMaxLifespan = TimeUnit.toSeconds($scope.realm.ssoSessionMaxLifespan, $scope.realm.ssoSessionMaxLifespanUnit)
        realmCopy.accessCodeLifespan = TimeUnit.toSeconds($scope.realm.accessCodeLifespan, $scope.realm.accessCodeLifespanUnit)
        realmCopy.accessCodeLifespanUserAction = TimeUnit.toSeconds($scope.realm.accessCodeLifespanUserAction, $scope.realm.accessCodeLifespanUserActionUnit)
        realmCopy.accessCodeLifespanLogin = TimeUnit.toSeconds($scope.realm.accessCodeLifespanLogin, $scope.realm.accessCodeLifespanLoginUnit)

        Realm.update(realmCopy, function () {
            $route.reload();
            Notifications.success("The changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $route.reload();
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

module.controller('RealmSessionStatsCtrl', function($scope, realm, stats, RealmApplicationSessionStats, RealmLogoutAll, Notifications) {
    $scope.realm = realm;
    $scope.stats = stats;

    $scope.logoutAll = function() {
        RealmLogoutAll.save({realm : realm.realm}, function (globalReqResult) {
            var successCount = globalReqResult.successRequests ? globalReqResult.successRequests.length : 0;
            var failedCount  = globalReqResult.failedRequests ? globalReqResult.failedRequests.length : 0;

            if (failedCount > 0) {
                var msgStart = successCount>0 ? 'Successfully logout all users under: ' + globalReqResult.successRequests + ' . ' : '';
                Notifications.error(msgStart + 'Failed to logout users under: ' + globalReqResult.failedRequests + '. Verify availability of failed hosts and try again');
            } else {
                window.location.reload();
            }
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
            Notifications.success('Not Before set for realm.');
            reset();
        });
    }
    $scope.pushRevocation = function() {
        RealmPushRevocation.save({ realm: realm.realm}, function (globalReqResult) {
            var successCount = globalReqResult.successRequests ? globalReqResult.successRequests.length : 0;
            var failedCount  = globalReqResult.failedRequests ? globalReqResult.failedRequests.length : 0;

            if (successCount==0 && failedCount==0) {
                Notifications.warn('No push sent. No admin URI configured or no registered cluster nodes available');
            } else if (failedCount > 0) {
                var msgStart = successCount>0 ? 'Successfully push notBefore to: ' + globalReqResult.successRequests + ' . ' : '';
                Notifications.error(msgStart + 'Failed to push notBefore to: ' + globalReqResult.failedRequests + '. Verify availability of failed hosts and try again');
            } else {
                Notifications.success('Successfully push notBefore to all configured applications');
            }
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

                Role.get({ realm: realm.realm, role: role.name }, function(role) {
                    var id = role.id;
                    $location.url("/realms/" + realm.realm + "/roles/" + id);
                    Notifications.success("The role has been created.");
                });
            });
        } else {
            $scope.update();
        }
    };

    $scope.remove = function () {
        Dialog.confirmDelete($scope.role.name, 'role', function () {
            $scope.role.$remove({
                realm: realm.realm,
                role: $scope.role.id
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

module.controller('RealmEventsConfigCtrl', function($scope, eventsConfig, RealmEventsConfig, RealmEvents, realm, serverInfo, $location, Notifications, TimeUnit, Dialog) {
    $scope.realm = realm;

    $scope.eventsConfig = eventsConfig;

    $scope.eventsConfig.expirationUnit = TimeUnit.autoUnit(eventsConfig.eventsExpiration);
    $scope.eventsConfig.eventsExpiration = TimeUnit.toUnit(eventsConfig.eventsExpiration, $scope.eventsConfig.expirationUnit);
    $scope.$watch('eventsConfig.expirationUnit', function(to, from) {
        if ($scope.eventsConfig.eventsExpiration) {
            $scope.eventsConfig.eventsExpiration = TimeUnit.convert($scope.eventsConfig.eventsExpiration, from, to);
        }
    });

    $scope.eventListeners = serverInfo.eventListeners;

    var oldCopy = angular.copy($scope.eventsConfig);
    $scope.changed = false;

    $scope.$watch('eventsConfig', function() {
        if (!angular.equals($scope.eventsConfig, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        $scope.changed = false;

        var copy = angular.copy($scope.eventsConfig)
        delete copy['expirationUnit'];

        copy.eventsExpiration = TimeUnit.toSeconds($scope.eventsConfig.eventsExpiration, $scope.eventsConfig.expirationUnit);

        RealmEventsConfig.update({
            id : realm.realm
        }, copy, function () {
            $location.url("/realms/" + realm.realm + "/events-settings");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.eventsConfig = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.clearEvents = function() {
        Dialog.confirmDelete($scope.realm.realm, 'events', function() {
            RealmEvents.remove({ id : $scope.realm.realm }, function() {
                Notifications.success("The events has been cleared.");
            });
        });
    };
});

module.controller('RealmEventsCtrl', function($scope, RealmEvents, realm) {
    $scope.realm = realm;
    $scope.page = 0;
    
    $scope.eventTypes = [{tag:'LOGIN'}, {tag:'REGISTER'}, {tag:'LOGOUT'}, {tag:'CODE_TO_TOKEN'}, {tag:'REFRESH_TOKEN'},
                         {tag:'LOGIN_ERROR'}, {tag:'REGISTER_ERROR'}, {tag:'LOGOUT_ERROR'}, {tag:'CODE_TO_TOKEN_ERROR'}, {tag:'REFRESH_TOKEN_ERROR'},
                         {tag:'VALIDATE_ACCESS_TOKEN'}, {tag:'VALIDATE_ACCESS_TOKEN_ERROR'}, {tag:'SOCIAL_LINK'}, {tag:'SOCIAL_LINK_ERROR'}, {tag:'REMOVE_FEDERATED_IDENTITY'},
                         {tag:'REMOVE_SOCIAL_LINK_ERROR'}, {tag:'UPDATE_EMAIL'}, {tag:'UPDATE_PROFILE'}, {tag:'UPDATE_PASSWORD'}, {tag:'UPDATE_TOTP'}];

    $scope.query = {
        id : realm.realm,
        max : 5,
        first : 0
    }

    $scope.update = function() {
    	$scope.query.first = 0;
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
           }
        }
        $scope.events = RealmEvents.query($scope.query);
    }
    
    $scope.reset = function() {
    	$scope.query.first = 0;
    	$scope.query.max = 5;
    	$scope.query.type = '';
    	$scope.query.client = '';
    	$scope.query.user = '';
    	$scope.query.dateFrom = '';
    	$scope.query.dateTo = '';
    	
    	$scope.update();
    }
    
    $scope.queryUpdate = function() {
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
           }
        }
        $scope.events = RealmEvents.query($scope.query);
    }
    
    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.queryUpdate();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.queryUpdate();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.queryUpdate();
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
            $location.url("/realms/" + realm.realm + "/defense/brute-force");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };
});


