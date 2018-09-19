function getAccess(Auth, Current, role) {
    if (!Current.realm)return false;
    var realmAccess = Auth.user && Auth.user['realm_access'];
    if (realmAccess) {
        realmAccess = realmAccess[Current.realm.realm];
        if (realmAccess) {
            return realmAccess.indexOf(role) >= 0;
        }
    }
    return false;
}

function getAccessObject(Auth, Current) {
    return {
        get createRealm() {
            return Auth.user && Auth.user.createRealm;
        },

        get queryUsers() {
            return getAccess(Auth, Current, 'query-users') || this.viewUsers;
        },

        get queryGroups() {
            return getAccess(Auth, Current, 'query-groups') || this.viewUsers;
        },

        get queryClients() {
            return getAccess(Auth, Current, 'query-clients') || this.viewClients;
        },

        get viewRealm() {
            return getAccess(Auth, Current, 'view-realm') || getAccess(Auth, Current, 'manage-realm') || this.manageRealm;
        },

        get viewClients() {
            return getAccess(Auth, Current, 'view-clients') || getAccess(Auth, Current, 'manage-clients') || this.manageClients;
        },

        get viewUsers() {
            return getAccess(Auth, Current, 'view-users') || getAccess(Auth, Current, 'manage-users') || this.manageClients;
        },

        get viewEvents() {
            return getAccess(Auth, Current, 'view-events') || getAccess(Auth, Current, 'manage-events') || this.manageClients;
        },

        get viewIdentityProviders() {
            return getAccess(Auth, Current, 'view-identity-providers') || getAccess(Auth, Current, 'manage-identity-providers') || this.manageIdentityProviders;
        },

        get viewAuthorization() {
            return getAccess(Auth, Current, 'view-authorization') || this.manageAuthorization;
        },

        get manageRealm() {
            return getAccess(Auth, Current, 'manage-realm');
        },

        get manageClients() {
            return getAccess(Auth, Current, 'manage-clients');
        },

        get manageUsers() {
            return getAccess(Auth, Current, 'manage-users');
        },

        get manageEvents() {
            return getAccess(Auth, Current, 'manage-events');
        },

        get manageIdentityProviders() {
            return getAccess(Auth, Current, 'manage-identity-providers');
        },

        get manageAuthorization() {
            return getAccess(Auth, Current, 'manage-authorization');
        },

        get impersonation() {
            return getAccess(Auth, Current, 'impersonation');
        }
    };
}


module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications, ServerInfo) {
    $scope.authUrl = authUrl;
    $scope.resourceUrl = resourceUrl;
    $scope.auth = Auth;
    $scope.serverInfo = ServerInfo.get();

    $scope.access = getAccessObject(Auth, Current);

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.fragment = $location.path();
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('HomeCtrl', function(Realm, Auth, Current, $location) {

    Realm.query(null, function(realms) {
        var realm;
        if (realms.length == 1) {
            realm = realms[0];
        } else if (realms.length == 2) {
            if (realms[0].realm == Auth.user.realm) {
                realm = realms[1];
            } else if (realms[1].realm == Auth.user.realm) {
                realm = realms[0];
            }
        }
        if (realm) {
            Current.realms = realms;
            Current.realm = realm;
            var access = getAccessObject(Auth, Current);
            if (access.viewRealm || access.manageRealm) {
                $location.url('/realms/' + realm.realm );
            } else if (access.queryClients) {
                $location.url('/realms/' + realm.realm + "/clients");
            } else if (access.viewIdentityProviders) {
                $location.url('/realms/' + realm.realm + "/identity-provider-settings");
            } else if (access.queryUsers) {
                $location.url('/realms/' + realm.realm + "/users");
            } else if (access.queryGroups) {
                $location.url('/realms/' + realm.realm + "/groups");
            } else if (access.viewEvents) {
                $location.url('/realms/' + realm.realm + "/events");
            }
        } else {
            $location.url('/realms');
        }
    });
});

module.controller('RealmTabCtrl', function(Dialog, $scope, Current, Realm, Notifications, $location) {
    $scope.removeRealm = function() {
        Dialog.confirmDelete(Current.realm.realm, 'realm', function() {
            Realm.remove({ id : Current.realm.realm }, function() {
                Current.realms = Realm.query();
                Notifications.success("The realm has been deleted.");
                $location.url("/");
            });
        });
    };
});

module.controller('ServerInfoCtrl', function($scope, ServerInfo) {
    ServerInfo.reload();

    $scope.serverInfo = ServerInfo.get();

    $scope.$watch($scope.serverInfo, function() {
        $scope.providers = [];
        for(var spi in $scope.serverInfo.providers) {
            var p = angular.copy($scope.serverInfo.providers[spi]);
            p.name = spi;
            $scope.providers.push(p)
        }
    });

    $scope.serverInfoReload = function() {
        ServerInfo.reload();
    }
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
    }
});

module.controller('RealmCreateCtrl', function($scope, Current, Realm, $upload, $http, $location, $route, Dialog, Notifications, Auth, $modal) {
    console.log('RealmCreateCtrl');

    Current.realm = null;

    $scope.realm = {
        enabled: true
    };

    $scope.changed = false;
    $scope.files = [];

    var oldCopy = angular.copy($scope.realm);

    $scope.importFile = function($fileContent){
        $scope.realm = angular.copy(JSON.parse($fileContent));
        $scope.importing = true;
    };

    $scope.viewImportDetails = function() {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'ObjectModalCtrl',
            resolve: {
                object: function () {
                    return $scope.realm;
                }
            }
        })
    };

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.$watch('realm.realm', function() {
	    $scope.realm.id = $scope.realm.realm;
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        Realm.create(realmCopy, function() {
            Notifications.success("The realm has been created.");

            Auth.refreshPermissions(function() {
                $scope.$apply(function() {
                    $location.url("/realms/" + realmCopy.realm);
                });
            });
        });
    };

    $scope.cancel = function() {
        $location.url("/");
    };

    $scope.reset = function() {
        $route.reload();
    }
});

module.controller('ObjectModalCtrl', function($scope, object) {
    $scope.object = object;
});

module.controller('RealmDetailCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $location, $window, Dialog, Notifications, Auth) {
    $scope.createRealm = !realm.realm;
    $scope.serverInfo = serverInfo;
    $scope.realmName = realm.realm;
    $scope.disableRename = realm.realm == masterRealm;

    if (Current.realm == null || Current.realm.realm != realm.realm) {
        for (var i = 0; i < Current.realms.length; i++) {
            if (realm.realm == Current.realms[i].realm) {
                Current.realm = Current.realms[i];
                break;
            }
        }
    }
    for (var i = 0; i < Current.realms.length; i++) {
        if (Current.realms[i].realm == realm.realm) {
            Current.realm = Current.realms[i];
        }
    }
    $scope.realm = angular.copy(realm);

    var oldCopy = angular.copy($scope.realm);

    $scope.changed = $scope.create;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
    $scope.$watch('realmName', function() {
        if (!angular.equals($scope.realmName, oldCopy.realm)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        var realmCopy = angular.copy($scope.realm);
        realmCopy.realm = $scope.realmName;
        $scope.changed = false;
        var nameChanged = !angular.equals($scope.realmName, oldCopy.realm);
        var oldName = oldCopy.realm;
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

            if (nameChanged) {
                console.debug(Auth);
                console.debug(Auth.authz.tokenParsed.iss);

                if (Auth.authz.tokenParsed.iss.endsWith(masterRealm)) {
                    Auth.refreshPermissions(function () {
                        Auth.refreshPermissions(function () {
                            Notifications.success("Your changes have been saved to the realm.");
                            $scope.$apply(function () {
                                $location.url("/realms/" + realmCopy.realm);
                            });
                        });
                    });
                } else {
                    delete Auth.authz.token;
                    delete Auth.authz.refreshToken;

                    var newLocation = $window.location.href.replace('/' + oldName + '/', '/' + realmCopy.realm + '/')
                        .replace('/realms/' + oldName, '/realms/' + realmCopy.realm);
                    window.location.replace(newLocation);
                }
            } else {
                $location.url("/realms/" + realmCopy.realm);
                Notifications.success("Your changes have been saved to the realm.");
            }
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        window.history.back();
    };
});

function genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, url) {
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
            $route.reload();
            Notifications.success("Your changes have been saved to the realm.");
            $scope.registrationAllowed = $scope.realm.registrationAllowed;
        });
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $route.reload();
    };

}

module.controller('DefenseHeadersCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/defense/headers");
});

module.controller('RealmLoginSettingsCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    // KEYCLOAK-5474: Make sure duplicateEmailsAllowed is disabled if loginWithEmailAllowed
    $scope.$watch('realm.loginWithEmailAllowed', function() {
        if ($scope.realm.loginWithEmailAllowed) {
            $scope.realm.duplicateEmailsAllowed = false;
        }
    });
    
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/login-settings");
});

module.controller('RealmOtpPolicyCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    $scope.optionsDigits = [ 6, 8 ];

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/otp-policy");
});


module.controller('RealmThemeCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/theme-settings");

    $scope.supportedLocalesOptions = {
        'multiple' : true,
        'simple_tags' : true,
        'tags' : []
    };
    
    updateSupported();
    
    function localeForTheme(type, name) {
        name = name || 'base';
        for (var i = 0; i < serverInfo.themes[type].length; i++) {
            if (serverInfo.themes[type][i].name == name) {
                return serverInfo.themes[type][i].locales || [];
            }
        }
        return [];
    }

    function updateSupported() {
        if ($scope.realm.internationalizationEnabled) {
            var accountLocales = localeForTheme('account', $scope.realm.accountTheme);
            var loginLocales = localeForTheme('login', $scope.realm.loginTheme);
            var emailLocales = localeForTheme('email', $scope.realm.emailTheme);

            var supportedLocales = [];
            for (var i = 0; i < accountLocales.length; i++) {
                var l = accountLocales[i];
                if (loginLocales.indexOf(l) >= 0 && emailLocales.indexOf(l) >= 0) {
                    supportedLocales.push(l);
                }
            }

            $scope.supportedLocalesOptions.tags = supportedLocales;

            if (!$scope.realm.supportedLocales) {
                $scope.realm.supportedLocales = supportedLocales;
            } else {
                for (var i = 0; i < $scope.realm.supportedLocales.length; i++) {
                    if (supportedLocales.indexOf($scope.realm.supportedLocales[i]) == -1) {
                        $scope.realm.supportedLocales = supportedLocales;
                    }
                }
            }

            if (!$scope.realm.defaultLocale || supportedLocales.indexOf($scope.realm.defaultLocale) == -1) {
                $scope.realm.defaultLocale = 'en';
            }
        }
    }

    $scope.$watch('realm.loginTheme', updateSupported);
    $scope.$watch('realm.accountTheme', updateSupported);
    $scope.$watch('realm.emailTheme', updateSupported);
    $scope.$watch('realm.internationalizationEnabled', updateSupported);
});

module.controller('RealmCacheCtrl', function($scope, realm, RealmClearUserCache, RealmClearRealmCache, RealmClearKeysCache, Notifications) {
    $scope.realm = angular.copy(realm);

    $scope.clearUserCache = function() {
        RealmClearUserCache.save({ realm: realm.realm}, function () {
            Notifications.success("User cache cleared");
        });
    }

    $scope.clearRealmCache = function() {
        RealmClearRealmCache.save({ realm: realm.realm}, function () {
           Notifications.success("Realm cache cleared");
        });
    }

    $scope.clearKeysCache = function() {
        RealmClearKeysCache.save({ realm: realm.realm}, function () {
           Notifications.success("Public keys cache cleared");
        });
    }


});

module.controller('RealmPasswordPolicyCtrl', function($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, serverInfo) {
    var parse = function(policyString) {
        var policies = [];
        if (!policyString || policyString.length == 0){
            return policies;
        }

        var policyArray = policyString.split(" and ");

        for (var i = 0; i < policyArray.length; i ++){
            var policyToken = policyArray[i];
            var id;
            var value;
            if (policyToken.indexOf('(') == -1) {
                id = policyToken.trim();
                value = null;
            } else {
                id = policyToken.substring(0, policyToken.indexOf('('));
                value = policyToken.substring(policyToken.indexOf('(') + 1, policyToken.lastIndexOf(')')).trim();
            }

            for (var j = 0; j < serverInfo.passwordPolicies.length; j++) {
                if (serverInfo.passwordPolicies[j].id == id) {
                    // clone
                    var p = JSON.parse(JSON.stringify(serverInfo.passwordPolicies[j]));
                    
                    p.value = value && value || p.defaultValue;
                    policies.push(p);
                }
            }
        }
        return policies;
    };

    var toString = function(policies) {
        if (!policies || policies.length == 0) {
            return "";
        }
        var policyString = "";
        for (var i = 0; i < policies.length; i++) {
            policyString += policies[i].id + '(' + policies[i].value + ')';
            if (i != policies.length - 1) {
                policyString += ' and ';
            }
        }
        return policyString;
    }

    $scope.realm = realm;
    $scope.serverInfo = serverInfo;

    $scope.changed = false;
    console.log(JSON.stringify(parse(realm.passwordPolicy)));
    $scope.policy = parse(realm.passwordPolicy);
    var oldCopy = angular.copy($scope.policy);

    $scope.$watch('policy', function() {
        $scope.changed = ! angular.equals($scope.policy, oldCopy);
    }, true);

    $scope.addPolicy = function(policy){
        policy.value = policy.defaultValue;
        if (!$scope.policy) {
            $scope.policy = [];
        }
        $scope.policy.push(policy);
    }

    $scope.removePolicy = function(index){
        $scope.policy.splice(index, 1);
    }

    $scope.save = function() {
        $scope.realm.passwordPolicy = toString($scope.policy);
        console.log($scope.realm.passwordPolicy);

        Realm.update($scope.realm, function () {
            $route.reload();
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $route.reload();
    };
});

module.controller('RealmDefaultRolesCtrl', function ($scope, Realm, realm, clients, roles, Notifications, ClientRole, Client) {

    console.log('RealmDefaultRolesCtrl');

    $scope.realm = realm;

    $scope.availableRealmRoles = [];
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmDefRoles = [];

    $scope.clients = angular.copy(clients);
    for (var i = 0; i < clients.length; i++) {
        if (clients[i].name == 'account') {
            $scope.client = $scope.clients[i];
            break;
        }
    }

    $scope.availableClientRoles = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientDefRoles = [];

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

        $scope.selectedRealmRoles = [];

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

        $scope.selectedRealmDefRoles = [];

        // Update/save the realm with new default roles.
        //var realmCopy = angular.copy($scope.realm);
        Realm.update($scope.realm, function () {
            Notifications.success("Realm default roles updated.");
        });
    };

    $scope.changeClient = function () {

        $scope.selectedClientRoles = [];
        $scope.selectedClientDefRoles = [];

        // Populate available roles for selected client
        if ($scope.client) {
            var appDefaultRoles = ClientRole.query({realm: $scope.realm.realm, client: $scope.client.id}, function () {

                if (!$scope.client.hasOwnProperty('defaultRoles') || $scope.client.defaultRoles === null) {
                    $scope.client.defaultRoles = [];
                }

                $scope.availableClientRoles = [];

                for (var i = 0; i < appDefaultRoles.length; i++) {
                    var roleName = appDefaultRoles[i].name;
                    if ($scope.client.defaultRoles.indexOf(roleName) < 0) {
                        $scope.availableClientRoles.push(roleName);
                    }
                }
            });
        } else {
            $scope.availableClientRoles = null;
        }
    };

    $scope.addClientDefaultRole = function () {

        // Remove selected roles from the app available roles and add them to app default roles (move from left to right).
        for (var i = 0; i < $scope.selectedClientRoles.length; i++) {
            var role = $scope.selectedClientRoles[i];

            var idx = $scope.client.defaultRoles.indexOf(role);
            if (idx < 0) {
                $scope.client.defaultRoles.push(role);
            }

            idx = $scope.availableClientRoles.indexOf(role);

            if (idx != -1) {
                $scope.availableClientRoles.splice(idx, 1);
            }
        }

        $scope.selectedClientRoles = [];

        // Update/save the selected client with new default roles.
        Client.update({
            realm: $scope.realm.realm,
            client: $scope.client.id
        }, $scope.client, function () {
            Notifications.success("Your changes have been saved to the client.");
        });
    };

    $scope.rmClientDefaultRole = function () {

        // Remove selected roles from the app default roles and add them to app available roles (move from right to left).
        for (var i = 0; i < $scope.selectedClientDefRoles.length; i++) {
            var role = $scope.selectedClientDefRoles[i];
            var idx = $scope.client.defaultRoles.indexOf(role);
            if (idx != -1) {
                $scope.client.defaultRoles.splice(idx, 1);
            }
            idx = $scope.availableClientRoles.indexOf(role);
            if (idx < 0) {
                $scope.availableClientRoles.push(role);
            }
        }

        $scope.selectedClientDefRoles = [];

        // Update/save the selected client with new default roles.
        Client.update({
            realm: $scope.realm.realm,
            client: $scope.client.id
        }, $scope.client, function () {
            Notifications.success("Your changes have been saved to the client.");
        });
    };

});



module.controller('IdentityProviderTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeIdentityProvider = function() {
        Dialog.confirmDelete($scope.identityProvider.alias, 'provider', function() {
            $scope.identityProvider.$remove({
                realm : Current.realm.realm,
                alias : $scope.identityProvider.alias
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/identity-provider-settings");
                Notifications.success("The identity provider has been deleted.");
            });
        });
    };
});

module.controller('RealmIdentityProviderCtrl', function($scope, $filter, $upload, $http, $route, realm, instance, providerFactory, IdentityProvider, serverInfo, authFlows, $location, Notifications, Dialog) {
    $scope.realm = angular.copy(realm);

    $scope.initSamlProvider = function() {
        $scope.nameIdFormats = [
            /*
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
                name: "Transient"
            },
            */
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
        $scope.signatureAlgorithms = [
            "RSA_SHA1",
            "RSA_SHA256",
            "RSA_SHA512",
            "DSA_SHA1"
        ];
        $scope.xmlKeyNameTranformers = [
            "NONE",
            "KEY_ID",
            "CERT_SUBJECT"
        ];
        if (instance && instance.alias) {

        } else {
            $scope.identityProvider.config.nameIDPolicyFormat = $scope.nameIdFormats[0].format;
            $scope.identityProvider.config.signatureAlgorithm = $scope.signatureAlgorithms[1];
            $scope.identityProvider.config.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
        }
    }

    $scope.hidePassword = true;
    $scope.fromUrl = {
        data: ''
    };

    if (instance && instance.alias) {
        $scope.identityProvider = angular.copy(instance);
        $scope.newIdentityProvider = false;
        for (var i in serverInfo.identityProviders) {
            var provider = serverInfo.identityProviders[i];

            if (provider.id == instance.providerId) {
                $scope.provider = provider;
            }
        }
    } else {
        $scope.identityProvider = {};
        $scope.identityProvider.config = {};
        $scope.identityProvider.alias = providerFactory.id;
        $scope.identityProvider.providerId = providerFactory.id;

        $scope.identityProvider.enabled = true;
        $scope.identityProvider.authenticateByDefault = false;
        $scope.identityProvider.firstBrokerLoginFlowAlias = 'first broker login';
        $scope.identityProvider.config.useJwksUrl = 'true';
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

    removeUsedSocial();
    
    $scope.authFlows = [];
    for (var i=0 ; i<authFlows.length ; i++) {
        if (authFlows[i].providerId == 'basic-flow') {
            $scope.authFlows.push(authFlows[i]);
        }
    }

    $scope.postBrokerAuthFlows = [];
    var emptyFlow = { alias: "" };
    $scope.postBrokerAuthFlows.push(emptyFlow);
    for (var i=0 ; i<$scope.authFlows.length ; i++) {
        $scope.postBrokerAuthFlows.push($scope.authFlows[i]);
    }
    
    if (!$scope.identityProvider.postBrokerLoginFlowAlias) {
        $scope.identityProvider.postBrokerLoginFlowAlias = $scope.postBrokerAuthFlows[0].alias;
    }

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
                url: authUrl + '/admin/realms/' + realm.realm + '/identity-provider/import-config',
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
            }).then(function(response) {
                setConfig(response.data);
                $scope.clearFileSelect();
                Notifications.success("The IDP metadata has been loaded from file.");
            }).catch(function() {
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
            fromUrl: $scope.fromUrl.data,
            providerId: providerFactory.id
        }
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/identity-provider/import-config', input)
            .then(function(response) {
                setConfig(response.data);
                $scope.fromUrl.data = '';
                $scope.importUrl = false;
                Notifications.success("Imported config information from url.");
            }).catch(function() {
                Notifications.error("Config can not be imported. Please verify the url.");
            });
    };
    $scope.$watch('fromUrl.data', function(newVal, oldVal){
        if ($scope.fromUrl.data && $scope.fromUrl.data.length > 0) {
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
                    if (provider.id == configProvidedId) {
                        configuredProviders[j].provider = provider;
                    }
                }
            }
            $scope.configuredProviders = angular.copy(configuredProviders);
        }
    }, true);

    $scope.callbackUrl = encodeURI($location.absUrl().replace(/\/admin.*/, "/realms/") + realm.realm + "/broker/") ;

    $scope.addProvider = function(provider) {
        $location.url("/create/identity-provider/" + realm.realm + "/" + provider.id);
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
                $location.url("/realms/" + realm.realm + "/identity-provider-settings/provider/" + $scope.identityProvider.providerId + "/" + $scope.identityProvider.alias);
                Notifications.success("The " + $scope.identityProvider.alias + " provider has been created.");
            });
        } else {
            IdentityProvider.update({
                realm: $scope.realm.realm,
                alias: $scope.identityProvider.alias
            }, $scope.identityProvider, function () {
                $route.reload();
                Notifications.success("The " + $scope.identityProvider.alias + " provider has been updated.");
            });
        }
    };

    $scope.cancel = function() {
        if ($scope.newIdentityProvider) {
            $location.url("/realms/" + realm.realm + "/identity-provider-settings");
        } else {
            $route.reload();
        }
    };


    $scope.reset = function() {
        $scope.identityProvider = {};
        $scope.configuredProviders = angular.copy($scope.realm.identityProviders);
    };

    $scope.showPassword = function(flag) {
        $scope.hidePassword = flag;
    };

    $scope.removeIdentityProvider = function(identityProvider) {
        Dialog.confirmDelete(identityProvider.alias, 'provider', function() {
            IdentityProvider.remove({
                realm : realm.realm,
                alias : identityProvider.alias
            }, function() {
                $route.reload();
                Notifications.success("The identity provider has been deleted.");
            });
        });
    };
    
    // KEYCLOAK-5932: remove social providers that have already been defined
    function removeUsedSocial() {
        var i = $scope.allProviders.length;
        while (i--) {
            if ($scope.allProviders[i].groupName !== 'Social') continue;
            if ($scope.configuredProviders != null) {
                for (var j = 0; j < $scope.configuredProviders.length; j++) {
                    if ($scope.configuredProviders[j].providerId === $scope.allProviders[i].id) {
                        $scope.allProviders.splice(i, 1);
                        break;
                    }
                }
            }
        }
    };

});

module.controller('RealmIdentityProviderExportCtrl', function(realm, identityProvider, $scope, $http, IdentityProviderExport) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.download = null;
    $scope.exported = "";
    $scope.exportedType = "";

    var url = IdentityProviderExport.url({realm: realm.realm, alias: identityProvider.alias}) ;
    $http.get(url).then(function(response) {
        $scope.exportedType = response.headers('Content-Type');
        $scope.exported = response.data;
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

module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, TimeUnit, TimeUnit2, serverInfo) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.actionTokenProviders = $scope.serverInfo.providers.actionTokenHandler.providers;

    $scope.realm.accessTokenLifespan = TimeUnit2.asUnit(realm.accessTokenLifespan);
    $scope.realm.accessTokenLifespanForImplicitFlow = TimeUnit2.asUnit(realm.accessTokenLifespanForImplicitFlow);
    $scope.realm.ssoSessionIdleTimeout = TimeUnit2.asUnit(realm.ssoSessionIdleTimeout);
    $scope.realm.ssoSessionMaxLifespan = TimeUnit2.asUnit(realm.ssoSessionMaxLifespan);
    $scope.realm.offlineSessionIdleTimeout = TimeUnit2.asUnit(realm.offlineSessionIdleTimeout);
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    $scope.realm.offlineSessionMaxLifespan = TimeUnit2.asUnit(realm.offlineSessionMaxLifespan);
    $scope.realm.accessCodeLifespan = TimeUnit2.asUnit(realm.accessCodeLifespan);
    $scope.realm.accessCodeLifespanLogin = TimeUnit2.asUnit(realm.accessCodeLifespanLogin);
    $scope.realm.accessCodeLifespanUserAction = TimeUnit2.asUnit(realm.accessCodeLifespanUserAction);
    $scope.realm.actionTokenGeneratedByAdminLifespan = TimeUnit2.asUnit(realm.actionTokenGeneratedByAdminLifespan);
    $scope.realm.actionTokenGeneratedByUserLifespan = TimeUnit2.asUnit(realm.actionTokenGeneratedByUserLifespan);
    $scope.realm.attributes = realm.attributes

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    
    var refresh = function() {
        Realm.get($scope.realm.realm, function () {
            $scope.changed = false;
        });
    };

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.$watch('actionLifespanId', function () {
        $scope.actionTokenAttribute = TimeUnit2.asUnit($scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId]);
        //Refresh and disable the button if attribute is empty
        if (!$scope.actionTokenAttribute.toSeconds()) {
            refresh();
        }
    }, true);

    $scope.$watch('actionTokenAttribute', function () {
        if ($scope.actionLifespanId != null && $scope.actionTokenAttribute != null) {
            $scope.changed = true;
            $scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId] = $scope.actionTokenAttribute.toSeconds();
        }
    }, true);

    $scope.changeRevokeRefreshToken = function() {

    };

    $scope.save = function() {
        $scope.realm.accessTokenLifespan = $scope.realm.accessTokenLifespan.toSeconds();
        $scope.realm.accessTokenLifespanForImplicitFlow = $scope.realm.accessTokenLifespanForImplicitFlow.toSeconds();
        $scope.realm.ssoSessionIdleTimeout = $scope.realm.ssoSessionIdleTimeout.toSeconds();
        $scope.realm.ssoSessionMaxLifespan = $scope.realm.ssoSessionMaxLifespan.toSeconds();
        $scope.realm.offlineSessionIdleTimeout = $scope.realm.offlineSessionIdleTimeout.toSeconds();
        // KEYCLOAK-7688 Offline Session Max for Offline Token
        $scope.realm.offlineSessionMaxLifespan = $scope.realm.offlineSessionMaxLifespan.toSeconds();
        $scope.realm.accessCodeLifespan = $scope.realm.accessCodeLifespan.toSeconds();
        $scope.realm.accessCodeLifespanUserAction = $scope.realm.accessCodeLifespanUserAction.toSeconds();
        $scope.realm.accessCodeLifespanLogin = $scope.realm.accessCodeLifespanLogin.toSeconds();
        $scope.realm.actionTokenGeneratedByAdminLifespan = $scope.realm.actionTokenGeneratedByAdminLifespan.toSeconds();
        $scope.realm.actionTokenGeneratedByUserLifespan = $scope.realm.actionTokenGeneratedByUserLifespan.toSeconds();

        Realm.update($scope.realm, function () {
            $route.reload();
            Notifications.success("The changes have been saved to the realm.");
        });
    };
    
    $scope.resetToDefaultToken = function (actionTokenId) {
        $scope.actionTokenAttribute = {};
        delete $scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId];
        //Only for UI effects, resets to the original state
        $scope.actionTokenAttribute.unit = 'Minutes';
    }

    $scope.reset = function() {
        $route.reload();
    };
});

module.controller('ViewKeyCtrl', function($scope, key) {
    $scope.key = key;
});

module.controller('RealmKeysCtrl', function($scope, Realm, realm, $http, $route, $location, Dialog, Notifications, serverInfo, keys, Components, $modal) {
    $scope.realm = angular.copy(realm);
    $scope.keys = keys.keys;
    $scope.active = {};

    Components.query({realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.keys.KeyProvider'
    }, function(data) {
        for (var i = 0; i < keys.keys.length; i++) {
            for (var j = 0; j < data.length; j++) {
                if (keys.keys[i].providerId == data[j].id) {
                    keys.keys[i].provider = data[j];
                }
            }
        }

        for (var t in keys.active) {
            for (var i = 0; i < keys.keys.length; i++) {
                if (keys.active[t] == keys.keys[i].kid) {
                    $scope.active[t] = keys.keys[i];
                }
            }
        }
    });

    $scope.viewKey = function(key) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-key.html',
            controller: 'ViewKeyCtrl',
            resolve: {
                key: function () {
                    return key;
                }
            }
        })
    }
});

module.controller('RealmKeysProvidersCtrl', function($scope, Realm, realm, $http, $route, $location, Dialog, Notifications, serverInfo, Components, $modal) {
    $scope.realm = angular.copy(realm);
    $scope.enableUpload = false;

    $scope.providers = serverInfo.componentTypes['org.keycloak.keys.KeyProvider'];

    Components.query({realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.keys.KeyProvider'
    }, function(data) {
        $scope.instances = data;

        for (var i = 0; i < $scope.instances.length; i++) {
            for (var j = 0; j < $scope.providers.length; j++) {
                if ($scope.providers[j].id === $scope.instances[i].providerId) {
                    $scope.instances[i].provider = $scope.providers[j];
                }
            }
        }
    });

    $scope.addProvider = function(provider) {
        $location.url("/create/keys/" + realm.realm + "/providers/" + provider.id);
    };

    $scope.removeInstance = function(instance) {
        Dialog.confirmDelete(instance.name, 'key provider', function() {
            Components.remove({
                realm : realm.realm,
                componentId : instance.id
            }, function() {
                $route.reload();
                Notifications.success("The provider has been deleted.");
            });
        });
    };
});

module.controller('GenericKeystoreCtrl', function($scope, $location, Notifications, $route, Dialog, realm, serverInfo, instance, providerId, Components) {
    $scope.create = !instance.providerId;
    $scope.realm = realm;

    var providers = serverInfo.componentTypes['org.keycloak.keys.KeyProvider'];
    var providerFactory = null;
    for (var i = 0; i < providers.length; i++) {
        var p = providers[i];
        if (p.id == providerId) {
            $scope.providerFactory = p;
            providerFactory = p;
            break;
        }
    }

    if ($scope.create) {
        $scope.instance = {
            name: providerFactory.id,
            providerId: providerFactory.id,
            providerType: 'org.keycloak.keys.KeyProvider',
            parentId: realm.id,
            config: {
                'priority': ["0"]
            }
        }
    } else {
        $scope.instance = angular.copy(instance);
    }

    if (providerFactory.properties) {
        for (var i = 0; i < providerFactory.properties.length; i++) {
            var configProperty = providerFactory.properties[i];
            if (!$scope.instance.config[configProperty.name]) {
                if (configProperty.defaultValue) {
                    $scope.instance.config[configProperty.name] = [configProperty.defaultValue];
                    if (!$scope.create) {
                        instance.config[configProperty.name] = [configProperty.defaultValue];
                    }
                } else {
                    $scope.instance.config[configProperty.name] = [''];
                    if (!$scope.create) {
                        instance.config[configProperty.name] = [configProperty.defaultValue];
                    }
                }
            }
        }
    }

    $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

    }, true);

    $scope.save = function() {
        $scope.changed = false;
        if ($scope.create) {
            Components.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/keys/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success("The provider has been created.");
            });
        } else {
            Components.update({realm: realm.realm,
                    componentId: instance.id
                },
                $scope.instance,  function () {
                    $route.reload();
                    Notifications.success("The provider has been updated.");
                });
        }
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/keys");
        } else {
            $route.reload();
        }
    };
});

module.controller('RealmSessionStatsCtrl', function($scope, realm, stats, RealmClientSessionStats, RealmLogoutAll, Notifications) {
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
                Notifications.success('Successfully push notBefore to all configured clients');
            }
        });
    }

});


module.controller('RoleTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeRole = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            RoleById.remove({
                realm: realm.realm,
                role: $scope.role.id
            }, function () {
                $route.reload();
                Notifications.success("The role has been deleted.");
            });
        });
    };
});


module.controller('RoleListCtrl', function($scope, $route, Dialog, Notifications, realm, roles, RoleById, filterFilter) {
    $scope.realm = realm;
    $scope.roles = roles;
    $scope.currentPage = 1;
    $scope.currentPageInput = 1;
    $scope.pageSize = 20;
    $scope.numberOfPages = Math.ceil($scope.roles.length/$scope.pageSize);

    $scope.$watch('searchQuery', function (newVal, oldVal) {
        $scope.filtered = filterFilter($scope.roles, {name: newVal});
        $scope.totalItems = $scope.filtered.length;
        $scope.numberOfPages = Math.ceil($scope.totalItems/$scope.pageSize);
        $scope.currentPage = 1;
        $scope.currentPageInput = 1;
    }, true);

    $scope.removeRole = function (role) {
        Dialog.confirmDelete(role.name, 'role', function () {
            RoleById.remove({
                realm: realm.realm,
                role: role.id
            }, function () {
                $route.reload();
                Notifications.success("The role has been deleted.");
            });
        });
    };
});


module.controller('RoleDetailCtrl', function($scope, realm, role, roles, clients,
                                             Role, ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
                                             $http, $location, Dialog, Notifications, RealmRoleRemover) {
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

    $scope.remove = function() {
        RealmRoleRemover.remove($scope.role, realm, Dialog, $location, Notifications);
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/roles");
    };



    roleControl($scope, realm, role, roles, clients,
        ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
        $http, $location, Notifications, Dialog);
});

module.controller('RealmSMTPSettingsCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications, RealmSMTPConnectionTester) {
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

    var initSMTPTest = function() {
        return {
            realm: $scope.realm.realm,
            config: JSON.stringify(realm.smtpServer)
        };
    };

    $scope.testConnection = function() {
        RealmSMTPConnectionTester.send(initSMTPTest(), function() {
            Notifications.success("SMTP connection successful. E-mail was sent!");
        }, function(errorResponse) {
            if (error.data.errorMessage) {
                Notifications.error(error.data.errorMessage);
            } else {
                Notifications.error('Unexpected error during SMTP validation');
            }
        });
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

        obj['port'] = obj['port'] && obj['port'].toString();

        return obj;
    }
});

module.controller('RealmEventsConfigCtrl', function($scope, eventsConfig, RealmEventsConfig, RealmEvents, RealmAdminEvents, realm, serverInfo, $location, Notifications, TimeUnit, Dialog) {
    $scope.realm = realm;

    $scope.eventsConfig = eventsConfig;

    $scope.eventsConfig.expirationUnit = TimeUnit.autoUnit(eventsConfig.eventsExpiration);
    $scope.eventsConfig.eventsExpiration = TimeUnit.toUnit(eventsConfig.eventsExpiration, $scope.eventsConfig.expirationUnit);
    
    $scope.eventListeners = Object.keys(serverInfo.providers.eventsListener.providers);
    
    $scope.eventsConfigSelectOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': $scope.eventListeners
    };
    
    $scope.eventSelectOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['eventType']
    };

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
    
    $scope.clearAdminEvents = function() {
        Dialog.confirmDelete($scope.realm.realm, 'admin-events', function() {
            RealmAdminEvents.remove({ id : $scope.realm.realm }, function() {
                Notifications.success("The admin events has been cleared.");
            });
        });
    };
});

module.controller('RealmEventsCtrl', function($scope, RealmEvents, realm, serverInfo) {
    $scope.realm = realm;
    $scope.page = 0;
    
    $scope.eventSelectOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['eventType']
    };

    $scope.query = {
        id : realm.realm,
        max : 5,
        first : 0
    }
    
    $scope.disablePaste = function(e) {
        e.preventDefault();
        return false;
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

module.controller('RealmAdminEventsCtrl', function($scope, RealmAdminEvents, realm, serverInfo, $modal, $filter) {
    $scope.realm = realm;
    $scope.page = 0;

    $scope.query = {
    	id : realm.realm,
        max : 5,
        first : 0
    };

    $scope.adminEnabledEventOperationsOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['operationType']
    };

    $scope.adminEnabledEventResourceTypesOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['resourceType']
    };
    
    $scope.disablePaste = function(e) {
        e.preventDefault();
        return false;
    }
    
    $scope.update = function() {
    	$scope.query.first = 0;
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
           }
        }
        $scope.events = RealmAdminEvents.query($scope.query);
    };
    
    $scope.reset = function() {
    	$scope.query.first = 0;
    	$scope.query.max = 5;
    	$scope.query.operationTypes = '';
    	$scope.query.resourceTypes = '';
    	$scope.query.resourcePath = '';
    	$scope.query.authRealm = '';
    	$scope.query.authClient = '';
    	$scope.query.authUser = '';
    	$scope.query.authIpAddress = '';
    	$scope.query.dateFrom = '';
    	$scope.query.dateTo = '';
    	
    	$scope.update();
    };
    
    $scope.queryUpdate = function() {
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
           }
        }
        $scope.events = RealmAdminEvents.query($scope.query);
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
    
    $scope.viewRepresentation = function(event) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/realm-events-admin-representation.html',
            controller: 'RealmAdminEventsModalCtrl',
            resolve: {
                event: function () {
                    return event;
                }
            }
        })
    }

    $scope.viewAuth = function(event) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/realm-events-admin-auth.html',
            controller: 'RealmAdminEventsModalCtrl',
            resolve: {
                event: function () {
                    return event;
                }
            }
        })
    }
});

module.controller('RealmAdminEventsModalCtrl', function($scope, $filter, event) {
    $scope.event = event;
});

module.controller('RealmBruteForceCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications, TimeUnit, $route) {
    console.log('RealmBruteForceCtrl');

    $scope.realm = realm;

    $scope.realm.waitIncrementUnit = TimeUnit.autoUnit(realm.waitIncrementSeconds);
    $scope.realm.waitIncrement = TimeUnit.toUnit(realm.waitIncrementSeconds, $scope.realm.waitIncrementUnit);

    $scope.realm.minimumQuickLoginWaitUnit = TimeUnit.autoUnit(realm.minimumQuickLoginWaitSeconds);
    $scope.realm.minimumQuickLoginWait = TimeUnit.toUnit(realm.minimumQuickLoginWaitSeconds, $scope.realm.minimumQuickLoginWaitUnit);

    $scope.realm.maxFailureWaitUnit = TimeUnit.autoUnit(realm.maxFailureWaitSeconds);
    $scope.realm.maxFailureWait = TimeUnit.toUnit(realm.maxFailureWaitSeconds, $scope.realm.maxFailureWaitUnit);

    $scope.realm.maxDeltaTimeUnit = TimeUnit.autoUnit(realm.maxDeltaTimeSeconds);
    $scope.realm.maxDeltaTime = TimeUnit.toUnit(realm.maxDeltaTimeSeconds, $scope.realm.maxDeltaTimeUnit);

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
            oldCopy = angular.copy($scope.realm);
            $location.url("/realms/" + realm.realm + "/defense/brute-force");
            Notifications.success("Your changes have been saved to the realm.");
        });
    };

    $scope.reset = function() {
        $route.reload();
    };
});


module.controller('IdentityProviderMapperListCtrl', function($scope, realm, identityProvider, mapperTypes, mappers) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.mapperTypes = mapperTypes;
    $scope.mappers = mappers;
});

module.controller('IdentityProviderMapperCtrl', function($scope, realm,  identityProvider, mapperTypes, mapper, IdentityProviderMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.create = false;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    $scope.mapperType = mapperTypes[mapper.identityProviderMapper];
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('mapper', function() {
        if (!angular.equals($scope.mapper, mapper)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        IdentityProviderMapper.update({
            realm : realm.realm,
            alias: identityProvider.alias,
            mapperId : mapper.id
        }, $scope.mapper, function() {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/identity-provider-mappers/' + identityProvider.alias + "/mappers/" + mapper.id);
            Notifications.success("Your changes have been saved.");
        });
    };

    $scope.reset = function() {
        $scope.mapper = angular.copy(mapper);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.mapper.name, 'mapper', function() {
            IdentityProviderMapper.remove({ realm: realm.realm, alias: mapper.identityProviderAlias, mapperId : $scope.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/identity-provider-mappers/' + identityProvider.alias + "/mappers");
            });
        });
    };

});

module.controller('IdentityProviderMapperCreateCtrl', function($scope, realm, identityProvider, mapperTypes, IdentityProviderMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.create = true;
    $scope.mapper = { identityProviderAlias: identityProvider.alias, config: {}};
    $scope.mapperTypes = mapperTypes;
    
    // make first type the default
    $scope.mapperType = mapperTypes[Object.keys(mapperTypes)[0]];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.mapper.identityProviderMapper = $scope.mapperType.id;
        IdentityProviderMapper.save({
            realm : realm.realm, alias: identityProvider.alias
        }, $scope.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/identity-provider-mappers/' + identityProvider.alias + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});

module.controller('RealmFlowBindingCtrl', function($scope, flows, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    $scope.flows = [];
    $scope.clientFlows = [];
    for (var i=0 ; i<flows.length ; i++) {
        if (flows[i].providerId == 'client-flow') {
            $scope.clientFlows.push(flows[i]);
        } else {
            $scope.flows.push(flows[i]);
        }
    }

    $scope.profileInfo = serverInfo.profileInfo;

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/flow-bindings");
});


module.controller('CreateFlowCtrl', function($scope, realm,
                                             AuthenticationFlows,
                                             Notifications, $location) {
    console.debug('CreateFlowCtrl');
    $scope.realm = realm;
    $scope.flow = {
        alias: "",
        providerId: "basic-flow",
        description: "",
        topLevel: true,
        builtIn: false
    }

    $scope.save = function() {
        AuthenticationFlows.save({realm: realm.realm, flow: ""}, $scope.flow, function() {
            $location.url("/realms/" + realm.realm + "/authentication/flows/" + $scope.flow.alias);
            Notifications.success("Flow Created.");
        })
    }
    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/authentication/flows");
    };
});

module.controller('CreateExecutionFlowCtrl', function($scope, realm, parentFlow, formProviders,
                                                      CreateExecutionFlow,
                                                      Notifications, $location) {
    $scope.realm = realm;
    $scope.formProviders = formProviders;
    
    var defaultFlowType = parentFlow.providerId == 'client-flow' ? 'client-flow' : 'basic-flow';
    $scope.flow = {
        alias: "",
        type: defaultFlowType,
        description: ""
    }
    $scope.provider = {};
    if (formProviders.length > 0) {
        $scope.provider = formProviders[0];
    }

    $scope.save = function() {
        $scope.flow.provider = $scope.provider.id;
        CreateExecutionFlow.save({realm: realm.realm, alias: parentFlow.alias}, $scope.flow, function() {
            $location.url("/realms/" + realm.realm + "/authentication/flows");
            Notifications.success("Flow Created.");
        })
    }
    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/authentication/flows");
    };
});

module.controller('CreateExecutionCtrl', function($scope, realm, parentFlow, formActionProviders, authenticatorProviders, clientAuthenticatorProviders,
                                                      CreateExecution,
                                                      Notifications, $location) {
    $scope.realm = realm;
    $scope.parentFlow = parentFlow;
    
    if (parentFlow.providerId == 'form-flow') {
        $scope.providers = formActionProviders;
    } else if (parentFlow.providerId == 'client-flow') {
        $scope.providers = clientAuthenticatorProviders;
    } else {
        $scope.providers = authenticatorProviders;
    }

    $scope.provider = {};
    if ($scope.providers.length > 0) {
        $scope.provider = $scope.providers[0];
    }

    $scope.save = function() {
        var execution = {
            provider: $scope.provider.id
        }
        CreateExecution.save({realm: realm.realm, alias: parentFlow.alias}, execution, function() {
            $location.url("/realms/" + realm.realm + "/authentication/flows");
            Notifications.success("Execution Created.");
        })
    }
    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/authentication/flows");
    };
});



module.controller('AuthenticationFlowsCtrl', function($scope, $route, realm, flows, selectedFlow, LastFlowSelected, Dialog,
                                                      AuthenticationFlows, AuthenticationFlowsCopy, AuthenticationFlowExecutions,
                                                      AuthenticationExecution, AuthenticationExecutionRaisePriority, AuthenticationExecutionLowerPriority,
                                                      $modal, Notifications, CopyDialog, $location) {
    $scope.realm = realm;
    $scope.flows = flows;
    
    if (selectedFlow !== null) {
        LastFlowSelected.alias = selectedFlow;
    }
    
    if (selectedFlow === null && LastFlowSelected.alias !== null) {
        selectedFlow = LastFlowSelected.alias;
    }
    
    if (flows.length > 0) {
        $scope.flow = flows[0];
        if (selectedFlow) {
            for (var i = 0; i < flows.length; i++) {
                if (flows[i].alias == selectedFlow) {
                    $scope.flow = flows[i];
                    break;
                }
            }
        }
    }

    $scope.selectFlow = function(flow) {
        $location.url("/realms/" + realm.realm + '/authentication/flows/' + flow.alias);
    };

    var setupForm = function() {
        AuthenticationFlowExecutions.query({realm: realm.realm, alias: $scope.flow.alias}, function(data) {
            $scope.executions = data;
            $scope.choicesmax = 0;
            $scope.levelmax = 0;
            for (var i = 0; i < $scope.executions.length; i++ ) {
                var execution = $scope.executions[i];
                if (execution.requirementChoices.length > $scope.choicesmax) {
                    $scope.choicesmax = execution.requirementChoices.length;
                }
                if (execution.level > $scope.levelmax) {
                    $scope.levelmax = execution.level;
                }
            }
            $scope.levelmaxempties = [];
            for (j = 0; j < $scope.levelmax; j++) {
                $scope.levelmaxempties.push(j);

            }
            for (var i = 0; i < $scope.executions.length; i++ ) {
                var execution = $scope.executions[i];
                execution.empties = [];
                for (j = 0; j < $scope.choicesmax - execution.requirementChoices.length; j++) {
                    execution.empties.push(j);
                }
                execution.preLevels = [];
                for (j = 0; j < execution.level; j++) {
                    execution.preLevels.push(j);
                }
                execution.postLevels = [];
                for (j = execution.level; j < $scope.levelmax; j++) {
                    execution.postLevels.push(j);
                }
            }
        })
    };

    $scope.copyFlow = function() {
        CopyDialog.open('Copy Authentication Flow', $scope.flow.alias, function(name) {
            AuthenticationFlowsCopy.save({realm: realm.realm, alias: $scope.flow.alias}, {
               newName: name
            }, function() {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + name);
                Notifications.success("Flow copied.");
            })
        })
    };

    $scope.deleteFlow = function() {
        Dialog.confirmDelete($scope.flow.alias, 'flow', function() {
            $scope.removeFlow();
        });
    };
    
    $scope.removeFlow = function() {
        console.log('Remove flow:' + $scope.flow.alias);
        if (realm.browserFlow == $scope.flow.alias) {
            Notifications.error("Cannot remove flow, it is currently being used as the browser flow.");

        }  else if (realm.registrationFlow == $scope.flow.alias) {
            Notifications.error("Cannot remove flow, it is currently being used as the registration flow.");

        } else if (realm.directGrantFlow == $scope.flow.alias) {
            Notifications.error("Cannot remove flow, it is currently being used as the direct grant flow.");

        } else if (realm.resetCredentialsFlow == $scope.flow.alias) {
            Notifications.error("Cannot remove flow, it is currently being used as the reset credentials flow.");

        } else if (realm.clientAuthenticationFlow == $scope.flow.alias) {
            Notifications.error("Cannot remove flow, it is currently being used as the client authentication flow.");

        } else if (realm.dockerAuthenticationFlow == $scope.flow.alias) {
            Notifications.error("Cannot remove flow, it is currently being used as the docker authentication flow.");

        } else {
            AuthenticationFlows.remove({realm: realm.realm, flow: $scope.flow.id}, function () {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + flows[0].alias);
                Notifications.success("Flow removed");
            })
        }

    };

    $scope.addFlow = function() {
        $location.url("/realms/" + realm.realm + '/authentication/flows/' + $scope.flow.id + '/create/flow/execution/' + $scope.flow.id);

    }

    $scope.addSubFlow = function(execution) {
        $location.url("/realms/" + realm.realm + '/authentication/flows/' + execution.flowId + '/create/flow/execution/' + $scope.flow.alias);

    }

    $scope.addSubFlowExecution = function(execution) {
        $location.url("/realms/" + realm.realm + '/authentication/flows/' + execution.flowId + '/create/execution/' + $scope.flow.alias);

    }

    $scope.addExecution = function() {
        $location.url("/realms/" + realm.realm + '/authentication/flows/' + $scope.flow.id + '/create/execution/' + $scope.flow.id);

    }

    $scope.createFlow = function() {
        $location.url("/realms/" + realm.realm + '/authentication/create/flow');
    }

    $scope.updateExecution = function(execution) {
        var copy = angular.copy(execution);
        delete copy.empties;
        delete copy.levels;
        delete copy.preLevels;
        delete copy.postLevels;
        AuthenticationFlowExecutions.update({realm: realm.realm, alias: $scope.flow.alias}, copy, function() {
            Notifications.success("Auth requirement updated");
            setupForm();
        });

    };

    $scope.removeExecution = function(execution) {
        console.log('removeExecution: ' + execution.id);
        var exeOrFlow = execution.authenticationFlow ? 'flow' : 'execution';
        Dialog.confirmDelete(execution.displayName, exeOrFlow, function() {
            AuthenticationExecution.remove({realm: realm.realm, execution: execution.id}, function() {
                Notifications.success("The " + exeOrFlow + " was removed.");
                setupForm();
            });
        });
        
    }

    $scope.raisePriority = function(execution) {
        AuthenticationExecutionRaisePriority.save({realm: realm.realm, execution: execution.id}, function() {
            Notifications.success("Priority raised");
            setupForm();
        })
    }

    $scope.lowerPriority = function(execution) {
        AuthenticationExecutionLowerPriority.save({realm: realm.realm, execution: execution.id}, function() {
            Notifications.success("Priority lowered");
            setupForm();
        })
    }

    $scope.setupForm = setupForm;

    if (selectedFlow == null) {
        $scope.selectFlow(flows[0]);
    } else {
        setupForm();
    }
});

module.controller('RequiredActionsCtrl', function($scope, realm, unregisteredRequiredActions,
                                                  $modal, $route,
                                                  RegisterRequiredAction, RequiredActions, RequiredActionRaisePriority, RequiredActionLowerPriority, Notifications) {
    console.log('RequiredActionsCtrl');
    $scope.realm = realm;
    $scope.unregisteredRequiredActions = unregisteredRequiredActions;
    $scope.requiredActions = [];
    var setupRequiredActionsForm = function() {
        console.log('setupRequiredActionsForm');
        RequiredActions.query({realm: realm.realm}, function(data) {
            $scope.requiredActions = [];
            for (var i = 0; i < data.length; i++) {
                $scope.requiredActions.push(data[i]);
            }
        });
    };

    $scope.updateRequiredAction = function(action) {
        RequiredActions.update({realm: realm.realm, alias: action.alias}, action, function() {
            Notifications.success("Required action updated");
            setupRequiredActionsForm();
        });
    }

    $scope.raisePriority = function(action) {
        RequiredActionRaisePriority.save({realm: realm.realm, alias: action.alias}, function() {
            Notifications.success("Required action's priority raised");
            setupRequiredActionsForm();
        })
    }

    $scope.lowerPriority = function(action) {
        RequiredActionLowerPriority.save({realm: realm.realm, alias: action.alias}, function() {
            Notifications.success("Required action's priority lowered");
            setupRequiredActionsForm();
        })
    }

    $scope.register = function() {
        var controller = function($scope, $modalInstance) {
            $scope.unregisteredRequiredActions = unregisteredRequiredActions;
            $scope.selected = {
                selected: $scope.unregisteredRequiredActions[0]
            }
            $scope.ok = function () {
                $modalInstance.close();
                RegisterRequiredAction.save({realm: realm.realm}, $scope.selected.selected, function() {
                    $route.reload();
                });
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
        }
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/unregistered-required-action-selector.html',
            controller: controller,
            resolve: {
            }
        });
    }

    setupRequiredActionsForm();


});

module.controller('AuthenticationConfigCtrl', function($scope, realm, flow, configType, config, AuthenticationConfig, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.flow = flow;
    $scope.configType = configType;
    $scope.create = false;
    $scope.config = angular.copy(config);
    $scope.changed = false;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('config', function() {
        if (!angular.equals($scope.config, config)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        AuthenticationConfig.update({
            realm : realm.realm,
            config : config.id
        }, $scope.config, function() {
            $scope.changed = false;
            config = angular.copy($scope.config);
            $location.url("/realms/" + realm.realm + '/authentication/flows/' + flow.id + '/config/' + configType.providerId + "/" + config.id);
            Notifications.success("Your changes have been saved.");
        });
    };

    $scope.reset = function() {
        $scope.config = angular.copy(config);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.config.alias, 'config', function() {
            AuthenticationConfig.remove({ realm: realm.realm, config : $scope.config.id }, function() {
                Notifications.success("The config has been deleted.");
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + flow.id);
            });
        });
    };

});

module.controller('AuthenticationConfigCreateCtrl', function($scope, realm, flow, configType, execution, AuthenticationExecutionConfig, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.flow = flow;
    $scope.create = true;
    $scope.configType = configType;

    var defaultConfig = {};
    if (configType && Array.isArray(configType.properties)) {
        for(var i = 0; i < configType.properties.length; i++) {
            var property = configType.properties[i];
            if (property && property.name) {
                defaultConfig[property.name] = property.defaultValue;
            }
        }
    }

    $scope.config = { config: defaultConfig};

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        AuthenticationExecutionConfig.save({
            realm : realm.realm,
            execution: execution
        }, $scope.config, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            var url = "/realms/" + realm.realm + '/authentication/flows/' + flow.id + '/config/' + configType.providerId + "/" + id;
            console.log('redirect url: ' + url);
            $location.url(url);
            Notifications.success("Config has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };
});

module.controller('ClientInitialAccessCtrl', function($scope, realm, clientInitialAccess, ClientInitialAccess, Dialog, Notifications, $route, $location) {
    $scope.realm = realm;
    $scope.clientInitialAccess = clientInitialAccess;

    $scope.remove = function(id) {
        Dialog.confirmDelete(id, 'initial access token', function() {
            ClientInitialAccess.remove({ realm: realm.realm, id: id }, function() {
                Notifications.success("The initial access token was deleted.");
                $route.reload();
            });
        });
    }
});

module.controller('ClientInitialAccessCreateCtrl', function($scope, realm, ClientInitialAccess, TimeUnit, Dialog, $location, $translate) {
    $scope.expirationUnit = 'Days';
    $scope.expiration = TimeUnit.toUnit(0, $scope.expirationUnit);
    $scope.count = 1;
    $scope.realm = realm;

    $scope.save = function() {
        var expiration = TimeUnit.toSeconds($scope.expiration, $scope.expirationUnit);
        ClientInitialAccess.save({
            realm: realm.realm
        }, { expiration: expiration, count: $scope.count}, function (data) {
            console.debug(data);
            $scope.id = data.id;
            $scope.token = data.token;
        });
    };

    $scope.cancel = function() {
        $location.url('/realms/' + realm.realm + '/client-registration/client-initial-access');
    };

    $scope.done = function() {
        var btns = {
            ok: {
                label: $translate.instant('continue'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-default'
            }
        }

        var title = $translate.instant('initial-access-token.confirm.title');
        var message = $translate.instant('initial-access-token.confirm.text');
        Dialog.open(title, message, btns, function() {
            $location.url('/realms/' + realm.realm + '/client-registration/client-initial-access');
        });
    };
});

module.controller('ClientRegPoliciesCtrl', function($scope, realm, clientRegistrationPolicyProviders, policies, Dialog, Notifications, Components, $route, $location) {
    $scope.realm = realm;
    $scope.providers = clientRegistrationPolicyProviders;
    $scope.anonPolicies = [];
    $scope.authPolicies = [];
    for (var i=0 ; i<policies.length ; i++) {
        var policy = policies[i];
        if (policy.subType === 'anonymous') {
            $scope.anonPolicies.push(policy);
        } else if (policy.subType === 'authenticated') {
            $scope.authPolicies.push(policy);
        } else {
            throw 'subType is required for clientRegistration policy component!';
        }
    }

    $scope.addProvider = function(authType, provider) {
        console.log('Add provider: authType ' + authType + ', providerId: ' + provider.id);
        $location.url("/realms/" + realm.realm + "/client-registration/client-reg-policies/create/" + authType + '/' + provider.id);
    };

    $scope.getInstanceLink = function(instance) {
        return "/realms/" + realm.realm + "/client-registration/client-reg-policies/" + instance.providerId + "/" + instance.id;
    }

    $scope.removeInstance = function(instance) {
        Dialog.confirmDelete(instance.name, 'client registration policy', function() {
            Components.remove({
                realm : realm.realm,
                componentId : instance.id
            }, function() {
                $route.reload();
                Notifications.success("The policy has been deleted.");
            });
        });
    };

});

module.controller('ClientRegPolicyDetailCtrl', function($scope, realm, clientRegistrationPolicyProviders, instance, Dialog, Notifications, Components, ComponentUtils, $route, $location) {
    $scope.realm = realm;
    $scope.instance = instance;
    $scope.providerTypes = clientRegistrationPolicyProviders;

    for (var i=0 ; i<$scope.providerTypes.length ; i++) {
        var providerType = $scope.providerTypes[i];
        if (providerType.id === instance.providerId) {
            $scope.providerType = providerType;
            break;
        }
    }

    $scope.create = !$scope.instance.name;

    function toDefaultValue(configProperty) {
        if (configProperty.type === 'MultivaluedString' || configProperty.type === 'MultivaluedList') {
            if (configProperty.defaultValue) {
                return configProperty.defaultValue;
            } else {
                return [];
            }
        }

        if (configProperty.defaultValue) {
            return [ configProperty.defaultValue ];
        } else {
            return [ '' ];
        }
    }

    if ($scope.create) {
        $scope.instance.name = $scope.instance.providerId;
        $scope.instance.parentId = realm.id;
        $scope.instance.config = {};

        if ($scope.providerType.properties) {

            for (var i = 0; i < $scope.providerType.properties.length; i++) {
                var configProperty = $scope.providerType.properties[i];
                $scope.instance.config[configProperty.name] = toDefaultValue(configProperty);
            }
        }
    }

    if ($scope.providerType.properties) {
        ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.providerType.properties, $scope.instance.config);
        ComponentUtils.addMvOptionsToMultivaluedLists($scope.providerType.properties);
    }

    var oldCopy = angular.copy($scope.instance);
    $scope.changed = false;

    $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
    
    $scope.reset = function() {
        $route.reload();
    };

    $scope.save = function() {
        $scope.changed = false;
        if ($scope.create) {
            Components.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/client-registration/client-reg-policies/" + $scope.instance.providerId + "/" + id);
                Notifications.success("The policy has been created.");
            });
        } else {
            Components.update({realm: realm.realm,
                    componentId: instance.id
                },
                $scope.instance,  function () {
                    $route.reload();
                    Notifications.success("The policy has been updated.");
                });
        }
    };

});

module.controller('RealmImportCtrl', function($scope, realm, $route, 
                                              Notifications, $modal, $resource) {
    $scope.rawContent = {};
    $scope.fileContent = {
        enabled: true
    };
    $scope.changed = false;
    $scope.files = [];
    $scope.realm = realm;
    $scope.overwrite = false;
    $scope.skip = false;
    $scope.importUsers = false;
    $scope.importGroups = false;
    $scope.importClients = false;
    $scope.importIdentityProviders = false;
    $scope.importRealmRoles = false;
    $scope.importClientRoles = false;
    $scope.ifResourceExists='FAIL';
    $scope.isMultiRealm = false;
    $scope.results = {};
    $scope.currentPage = 0;
    var pageSize = 15;
    
    var oldCopy = angular.copy($scope.fileContent);

    $scope.importFile = function($fileContent){
        var parsed;
        try {
            parsed = JSON.parse($fileContent);
        } catch (e) {
            Notifications.error('Unable to parse JSON file.');
            return;
        }
        
        $scope.rawContent = angular.copy(parsed);
        if (($scope.rawContent instanceof Array) && ($scope.rawContent.length > 0)) {
            if ($scope.rawContent.length > 1) $scope.isMultiRealm = true;
            $scope.fileContent = $scope.rawContent[0];
        } else {
            $scope.fileContent = $scope.rawContent;
        }
        
        $scope.importing = true;
        setOnOffSwitchDefaults();
        $scope.results = {};
        if (!$scope.hasResources()) {
            $scope.nothingToImport();
        }
    };

    $scope.hasResults = function() {
        return (Object.keys($scope.results).length > 0) &&
                ($scope.results.results !== undefined) &&
                ($scope.results.results.length > 0);
    }
    
    $scope.resultsPage = function() {
        if (!$scope.hasResults()) return {};
        return $scope.results.results.slice(startIndex(), endIndex());
    }
    
    function startIndex() {
        return pageSize * $scope.currentPage;
    }
    
    function endIndex() {
        var length = $scope.results.results.length;
        var endIndex = startIndex() + pageSize;
        if (endIndex > length) endIndex = length;
        return endIndex;
    }
    
    function setOnOffSwitchDefaults() {
        $scope.importUsers = $scope.hasArray('users');
        $scope.importGroups = $scope.hasArray('groups');
        $scope.importClients = $scope.hasArray('clients');
        $scope.importIdentityProviders = $scope.hasArray('identityProviders');
        $scope.importRealmRoles = $scope.hasRealmRoles();
        $scope.importClientRoles = $scope.hasClientRoles();
    }
    
    $scope.setFirstPage = function() {
        $scope.currentPage = 0;
    }
    
    $scope.setNextPage = function() {
        $scope.currentPage++;
    }
    
    $scope.setPreviousPage = function() {
        $scope.currentPage--;
    }
    
    $scope.hasNext = function() {
        if (!$scope.hasResults()) return false;
        var length = $scope.results.results.length;
        //console.log('length=' + length);
        var endIndex = startIndex() + pageSize;
        //console.log('endIndex=' + endIndex);
        return length > endIndex;
    }
    
    $scope.hasPrevious = function() {
        if (!$scope.hasResults()) return false;
        return $scope.currentPage > 0;
    }
    
    $scope.viewImportDetails = function() {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'ObjectModalCtrl',
            resolve: {
                object: function () {
                    return $scope.fileContent;
                }
            }
        })
    };
    
    $scope.hasArray = function(section) {
        return ($scope.fileContent !== 'undefined') &&
               ($scope.fileContent.hasOwnProperty(section)) &&
               ($scope.fileContent[section] instanceof Array) &&
               ($scope.fileContent[section].length > 0);
    }
    
    $scope.hasRealmRoles = function() {
        return $scope.hasRoles() &&
               ($scope.fileContent.roles.hasOwnProperty('realm')) &&
               ($scope.fileContent.roles.realm instanceof Array) &&
               ($scope.fileContent.roles.realm.length > 0);
    }
    
    $scope.hasRoles = function() {
        return ($scope.fileContent !== 'undefined') &&
               ($scope.fileContent.hasOwnProperty('roles')) &&
               ($scope.fileContent.roles !== 'undefined');
    }
    
    $scope.hasClientRoles = function() {
        return $scope.hasRoles() &&
               ($scope.fileContent.roles.hasOwnProperty('client')) &&
               (Object.keys($scope.fileContent.roles.client).length > 0);
    }
    
    $scope.itemCount = function(section) {
        if (!$scope.importing) return 0;
        if ($scope.hasRealmRoles() && (section === 'roles.realm')) return $scope.fileContent.roles.realm.length;
        if ($scope.hasClientRoles() && (section === 'roles.client')) return clientRolesCount($scope.fileContent.roles.client);
        
        if (!$scope.fileContent.hasOwnProperty(section)) return 0;
        
        return $scope.fileContent[section].length;
    }
    
    clientRolesCount = function(clientRoles) {
        var total = 0;
        for (var clientName in clientRoles) {
            total += clientRoles[clientName].length;
        }
        return total;
    }
    
    $scope.hasResources = function() {
        return ($scope.importUsers && $scope.hasArray('users')) ||
               ($scope.importGroups && $scope.hasArray('groups')) ||
               ($scope.importClients && $scope.hasArray('clients')) ||
               ($scope.importIdentityProviders && $scope.hasArray('identityProviders')) ||
               ($scope.importRealmRoles && $scope.hasRealmRoles()) ||
               ($scope.importClientRoles && $scope.hasClientRoles());
    }
    
    $scope.nothingToImport = function() {
        Notifications.error('No resources specified to import.');
    }
    
    $scope.$watch('fileContent', function() {
        if (!angular.equals($scope.fileContent, oldCopy)) {
            $scope.changed = true;
        }
        setOnOffSwitchDefaults();
    }, true);
    
    $scope.successMessage = function() {
        var message = $scope.results.added + ' records added. ';
        if ($scope.ifResourceExists === 'SKIP') {
            message += $scope.results.skipped + ' records skipped.'
        }
        if ($scope.ifResourceExists === 'OVERWRITE') {
            message += $scope.results.overwritten + ' records overwritten.';
        }
        return message;
    }
    
    $scope.save = function() {
        var json = angular.copy($scope.fileContent);
        json.ifResourceExists = $scope.ifResourceExists;
        if (!$scope.importUsers) delete json.users;
        if (!$scope.importGroups) delete json.groups;
        if (!$scope.importIdentityProviders) delete json.identityProviders;
        if (!$scope.importClients) delete json.clients;
        
        if (json.hasOwnProperty('roles')) {
            if (!$scope.importRealmRoles) delete json.roles.realm;
            if (!$scope.importClientRoles) delete json.roles.client;
        }
        
        var importFile = $resource(authUrl + '/admin/realms/' + realm.realm + '/partialImport');
        $scope.results = importFile.save(json, function() {
            Notifications.success($scope.successMessage());
        }, function(error) {
            if (error.data.errorMessage) {
                Notifications.error(error.data.errorMessage);
            } else {
                Notifications.error('Unexpected error during import');
            }
        });
    };
    
    $scope.reset = function() {
        $route.reload();
    }

});

module.controller('RealmExportCtrl', function($scope, realm, $http,
                                              $httpParamSerializer, Notifications, Dialog) {
    $scope.realm = realm;
    $scope.exportGroupsAndRoles = false;
    $scope.exportClients = false;

    $scope.export = function() {
        if ($scope.exportGroupsAndRoles || $scope.exportClients) {
            Dialog.confirm('Export', 'This operation may make server unresponsive for a while.\n\nAre you sure you want to proceed?', download);
        } else {
            download();
        }
    }

    function download() {
        var exportUrl = authUrl + '/admin/realms/' + realm.realm + '/partial-export';
        var params = {};
        if ($scope.exportGroupsAndRoles) {
            params['exportGroupsAndRoles'] = true;
        }
        if ($scope.exportClients) {
            params['exportClients'] = true;
        }
        if (Object.keys(params).length > 0) {
            exportUrl += '?' + $httpParamSerializer(params);
        }
        $http.post(exportUrl)
            .then(function(response) {
                var download = angular.fromJson(response.data);
                download = angular.toJson(download, true);
                saveAs(new Blob([download], { type: 'application/json' }), 'realm-export.json');
            }).catch(function() {
                Notifications.error("Sorry, something went wrong.");
            });
    }
});
