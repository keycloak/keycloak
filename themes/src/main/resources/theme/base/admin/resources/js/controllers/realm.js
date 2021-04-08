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


module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications, ServerInfo, RealmSpecificLocalizationTexts) {
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

    $scope.$watch(function() {
        return Current.realm;
    }, function() {
        if(Current.realm !== null && currentRealm !== Current.realm.id) {
            currentRealm = Current.realm.id;
            translateProvider.translations(locale, resourceBundle);
            RealmSpecificLocalizationTexts.get({id: Current.realm.realm, locale: locale}, function (localizationTexts) {
                translateProvider.translations(locale, localizationTexts.toJSON());
            })
        }
    })
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
    $scope.authServerUrl = authServerUrl;

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
    $scope.realm.attributes['userProfileEnabled'] = $scope.realm.attributes['userProfileEnabled'] == 'true';

    var oldCopy = angular.copy($scope.realm);
    $scope.realmCopy = oldCopy;

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
                        $scope.realmCopy = oldCopy;
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

module.controller('RealmWebAuthnPolicyCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, $location, Dialog, Notifications) {

    $scope.deleteAcceptableAaguid = function(index) {
        $scope.realm.webAuthnPolicyAcceptableAaguids.splice(index, 1);
    };

    $scope.addAcceptableAaguid = function() {
        $scope.realm.webAuthnPolicyAcceptableAaguids.push($scope.newAcceptableAaguid);
        $scope.newAcceptableAaguid = "";
    };

    // Just for case the user fill particular URL with disabled WebAuthn feature.
    $scope.redirectIfWebAuthnDisabled = function () {
        if (!serverInfo.featureEnabled('WEB_AUTHN')) {
            $location.url("/realms/" + $scope.realm.realm + "/authentication");
        }
    };

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/webauthn-policy");
});

module.controller('RealmWebAuthnPasswordlessPolicyCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, $location, Dialog, Notifications) {

    $scope.deleteAcceptableAaguid = function(index) {
        $scope.realm.webAuthnPolicyPasswordlessAcceptableAaguids.splice(index, 1);
    };

    $scope.addAcceptableAaguid = function() {
        $scope.realm.webAuthnPolicyPasswordlessAcceptableAaguids.push($scope.newAcceptableAaguid);
        $scope.newAcceptableAaguid = "";
    };

    // Just for case the user fill particular URL with disabled WebAuthn feature.
    $scope.redirectIfWebAuthnDisabled = function () {
        if (!serverInfo.featureEnabled('WEB_AUTHN')) {
            $location.url("/realms/" + $scope.realm.realm + "/authentication");
        }
    };

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/webauthn-policy-passwordless");
});

module.controller('RealmCibaPolicyCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, $location, Dialog, Notifications) {

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/ciba-policy");
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

module.controller('RealmLocalizationCtrl', function($scope, Current, $location, Realm, realm, serverInfo, Notifications, RealmSpecificLocales, realmSpecificLocales, RealmSpecificLocalizationTexts, RealmSpecificLocalizationText, Dialog, $translate){
    $scope.realm = realm;
    $scope.realmSpecificLocales = realmSpecificLocales;
    $scope.newLocale = null;
    $scope.selectedRealmSpecificLocales = null;
    $scope.localizationTexts = null;

    $scope.createLocale = function() {
        if(!$scope.newLocale) {
            Notifications.error($translate.instant('missing-locale'));
            return;
        }
        $scope.realmSpecificLocales.push($scope.newLocale)
        $scope.selectedRealmSpecificLocales = $scope.newLocale;
        $scope.newLocale = null;
        $location.url('/create/localization/' + realm.realm + '/' + $scope.selectedRealmSpecificLocales);
    }

    $scope.$watch(function() {
        return $scope.selectedRealmSpecificLocales;
    }, function() {
        if($scope.selectedRealmSpecificLocales != null) {
            $scope.updateRealmSpecificLocalizationTexts();
        }
    })

    $scope.updateRealmSpecificLocales = function() {
        RealmSpecificLocales.get({id: realm.realm}, function (updated) {
            $scope.realmSpecificLocales = updated;
        })
    }

    $scope.updateRealmSpecificLocalizationTexts = function() {
        RealmSpecificLocalizationTexts.get({id: realm.realm, locale: $scope.selectedRealmSpecificLocales }, function (updated) {
            $scope.localizationTexts = updated;
        })
    }

    $scope.removeLocalizationText = function(key) {
        Dialog.confirmDelete(key, 'localization text', function() {
            RealmSpecificLocalizationText.remove({
                realm: realm.realm,
                locale: $scope.selectedRealmSpecificLocales,
                key: key
            }, function () {
                $scope.updateRealmSpecificLocalizationTexts();
                Notifications.success($translate.instant('localization-text.remove.success'));
            });
        });
    }
});

module.controller('RealmLocalizationUploadCtrl', function($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, $upload, $translate){
    $scope.realm = realm;
    $scope.locale = null;
    $scope.files = [];

    $scope.onFileSelect = function($files) {
        $scope.files = $files;
    };

    $scope.reset = function() {
        $scope.locale = null;
        $scope.files = null;
    };

    $scope.save = function() {

        if(!$scope.files || $scope.files.length === 0) {
            Notifications.error($translate.instant('missing-file'));
            return;
        }
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $scope.files.length; i++) {
            var $file = $scope.files[i];
            $scope.upload = $upload.upload({
                url: authUrl + '/admin/realms/' + realm.realm + '/localization/' + $scope.locale,
                file: $file
            }).then(function(response) {
                $scope.reset();
                Notifications.success($translate.instant('localization-file.upload.success'));
            }).catch(function() {
                Notifications.error($translate.instant('localization-file.upload.error'));
            });
        }
    };

});

module.controller('RealmLocalizationDetailCtrl', function($scope, Current, $location, Realm, realm, Notifications, locale, key, RealmSpecificLocalizationText, localizationText, $translate){
    $scope.realm = realm;
    $scope.locale = locale;
    $scope.key = key;
    $scope.value = ((localizationText)? localizationText.content : null);

    $scope.create = !key;

    $scope.save = function() {
        if ($scope.create) {
            RealmSpecificLocalizationText.save({
                realm: realm.realm,
                locale: $scope.locale,
                key: $scope.key
            }, $scope.value, function (data, headers) {
                $location.url("/realms/" + realm.realm + "/localization");
                Notifications.success($translate.instant('localization-text.create.success'));
            });
        } else {
            RealmSpecificLocalizationText.save({
                realm: realm.realm,
                locale: $scope.locale,
                key: $scope.key
            }, $scope.value, function (data, headers) {
                $location.url("/realms/" + realm.realm + "/localization");
                Notifications.success($translate.instant('localization-text.update.success'));
            });
        }
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/localization");
    };

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

module.controller('RealmDefaultRolesCtrl', function ($scope, $route, realm, roles, Notifications, ClientRole, Client, RoleRealmComposites, RoleClientComposites, ComponentUtils, $http) {

    console.log('RealmDefaultRolesCtrl');

    $scope.realm = realm;
    $scope.availableRealmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmDefRoles = [];

    $scope.availableClientRoles = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientDefRoles = [];

    for (var j = 0; j < $scope.availableRealmRoles.length; j++) {
        if ($scope.availableRealmRoles[j].id === realm.defaultRole.id) {
            var realmRole = $scope.availableRealmRoles[j];
            var idx = $scope.availableRealmRoles.indexOf(realmRole);
            $scope.availableRealmRoles.splice(idx, 1);
            break;
        }
    }

    $scope.realmMappings = RoleRealmComposites.query({realm : realm.realm, role : realm.defaultRole.id}, function(){
        for (var i = 0; i < $scope.realmMappings.length; i++) {
            var role = $scope.realmMappings[i];
            for (var j = 0; j < $scope.availableRealmRoles.length; j++) {
                var realmRole = $scope.availableRealmRoles[j];
                if (realmRole.id === role.id) {
                    var idx = $scope.availableRealmRoles.indexOf(realmRole);
                    if (idx !== -1) {
                        $scope.availableRealmRoles.splice(idx, 1);
                        break;
                    }
                }
            }
        }
    });

    $scope.addRealmDefaultRole = function () {

        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + realm.defaultRole.id + '/composites',
            $scope.selectedRealmRolesToAdd).then(function() {
            // Remove selected roles from the Available roles and add them to realm default roles (move from left to right).
            for (var i = 0; i < $scope.selectedRealmRolesToAdd.length; i++) {
                var selectedRole = $scope.selectedRealmRolesToAdd[i];
                var index = ComponentUtils.findIndexById($scope.availableRealmRoles, selectedRole.id);
                if (index > -1) {
                    $scope.availableRealmRoles.splice(index, 1);
                    $scope.realmMappings.push(selectedRole);
                }
            }

            $scope.selectedRealmRoles = [];
            $scope.selectedRealmRolesToAdd = [];
            Notifications.success("Default roles updated.");
        });
    };

    $scope.deleteRealmDefaultRole = function () {

        $scope.selectedClientRolesToRemove = JSON.parse('[' + $scope.selectedRealmDefRoles + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + realm.defaultRole.id + '/composites',
            {data : $scope.selectedClientRolesToRemove, headers : {"content-type" : "application/json"}}).then(function() {
            // Remove selected roles from the realm default roles and add them to available roles (move from right to left).
            for (var i = 0; i < $scope.selectedClientRolesToRemove.length; i++) {
                var selectedRole = $scope.selectedClientRolesToRemove[i];
                var index = ComponentUtils.findIndexById($scope.realmMappings, selectedRole.id);
                if (index > -1) {
                    $scope.realmMappings.splice(index, 1);
                    $scope.availableRealmRoles.push(selectedRole);
                }
            }

            $scope.selectedRealmDefRoles = [];
            $scope.selectedClientRolesToRemove = [];
            Notifications.success("Default roles updated.");
        });
    };

    $scope.changeClient = function (client) {
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        $scope.selectedClientRoles = [];
        $scope.selectedClientDefRoles = [];

        // Populate available roles for selected client
        if ($scope.selectedClient) {
            $scope.availableClientRoles = ClientRole.query({realm: realm.realm, client: client.id}, function () {
                $scope.clientMappings = RoleClientComposites.query({realm : realm.realm, role : realm.defaultRole.id, client : client.id}, function(){
                    for (var i = 0; i < $scope.clientMappings.length; i++) {
                        var role = $scope.clientMappings[i];
                        for (var j = 0; j < $scope.availableClientRoles.length; j++) {
                            var clientRole = $scope.availableClientRoles[j];
                            if (clientRole.id === role.id) {
                                var idx = $scope.availableClientRoles.indexOf(clientRole);
                                if (idx !== -1) {
                                    $scope.availableClientRoles.splice(idx, 1);
                                    break;
                                }
                            }
                        }
                    }
                });
                for (var j = 0; j < $scope.availableClientRoles.length; j++) {
                    if ($scope.availableClientRoles[j] === realm.defaultRole.id) {
                        var clientRole = $scope.availableClientRoles[j];
                        var idx = $scope.availableClientRoles.indexof(clientRole);
                        $scope.availableClientRoles.splice(idx, 1);
                        break;
                    }
                }
            });
        } else {
            $scope.availableClientRoles = null;
        }
    };

    $scope.addClientDefaultRole = function () {

        $scope.selectedClientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + realm.defaultRole.id + '/composites',
            $scope.selectedClientRolesToAdd).then(function() {
            // Remove selected roles from the app available roles and add them to app default roles (move from left to right).
            for (var i = 0; i < $scope.selectedClientRolesToAdd.length; i++) {
                var selectedRole = $scope.selectedClientRolesToAdd[i];

                var index = ComponentUtils.findIndexById($scope.availableClientRoles, selectedRole.id);
                if (index > -1) {
                    $scope.availableClientRoles.splice(index, 1);
                    $scope.clientMappings.push(selectedRole);
                }
            }

            $scope.selectedClientRoles = [];
            $scope.selectedClientRolesToAdd = [];
            Notifications.success("Default roles updated.");
        });
    };

    $scope.rmClientDefaultRole = function () {

        $scope.selectedClientRolesToRemove = JSON.parse('[' + $scope.selectedClientDefRoles + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + realm.defaultRole.id + '/composites',
            {data : $scope.selectedClientRolesToRemove, headers : {"content-type" : "application/json"}}).then(function() {
            // Remove selected roles from the realm default roles and add them to available roles (move from right to left).
            for (var i = 0; i < $scope.selectedClientRolesToRemove.length; i++) {
                var selectedRole = $scope.selectedClientRolesToRemove[i];
                var index = ComponentUtils.findIndexById($scope.clientMappings, selectedRole.id);
                if (index > -1) {
                    $scope.clientMappings.splice(index, 1);
                    $scope.availableClientRoles.push(selectedRole);
                }
            }

            $scope.selectedClientDefRoles = [];
            $scope.selectedClientRolesToRemove = [];
            Notifications.success("Default roles updated.");
        });
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
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
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
                name: "Persistent"

            },
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
                name: "Transient"
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
            "RSA_SHA256_MGF1",
            "RSA_SHA512",
            "RSA_SHA512_MGF1",
            "DSA_SHA1"
        ];
        $scope.xmlKeyNameTranformers = [
            "NONE",
            "KEY_ID",
            "CERT_SUBJECT"
        ];
        $scope.principalTypes = [
            {
                type: "SUBJECT",
                name: "Subject NameID"

            },
            {
                type: "ATTRIBUTE",
                name: "Attribute [Name]"

            },
            {
                type: "FRIENDLY_ATTRIBUTE",
                name: "Attribute [Friendly Name]"

            }
        ];
        if (instance && instance.alias) {

        } else {
            $scope.identityProvider.config.nameIDPolicyFormat = $scope.nameIdFormats[0].format;
            $scope.identityProvider.config.principalType = $scope.principalTypes[0].type;
            $scope.identityProvider.config.signatureAlgorithm = $scope.signatureAlgorithms[1];
            $scope.identityProvider.config.xmlSigKeyInfoKeyNameTransformer = $scope.xmlKeyNameTranformers[1];			
            $scope.identityProvider.config.allowCreate = 'true';
        }
        $scope.identityProvider.config.entityId = $scope.identityProvider.config.entityId || (authUrl + '/realms/' + realm.realm);
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
        $scope.identityProvider.config.syncMode = 'IMPORT';
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
    	if (data["enabledFromMetadata"] !== undefined ) {
             $scope.identityProvider.enabled = data["enabledFromMetadata"] == "true";
             delete data["enabledFromMetadata"];
        }
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

    $scope.callbackUrl = authServerUrl + "/realms/" + realm.realm + "/broker/";

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

    if (instance && instance.alias) {
        try { $scope.authnContextClassRefs = JSON.parse($scope.identityProvider.config.authnContextClassRefs || '[]'); } catch (e) { $scope.authnContextClassRefs = []; }
        try { $scope.authnContextDeclRefs = JSON.parse($scope.identityProvider.config.authnContextDeclRefs || '[]'); } catch (e) { $scope.authnContextDeclRefs = []; }
    } else {
        $scope.authnContextClassRefs = [];
        $scope.authnContextDeclRefs = [];
    }

    $scope.deleteAuthnContextClassRef = function(index) {
        $scope.authnContextClassRefs.splice(index, 1);
        $scope.identityProvider.config.authnContextClassRefs = JSON.stringify($scope.authnContextClassRefs);
    };

    $scope.addAuthnContextClassRef = function() {
        $scope.authnContextClassRefs.push($scope.newAuthnContextClassRef);
        $scope.identityProvider.config.authnContextClassRefs = JSON.stringify($scope.authnContextClassRefs);
        $scope.newAuthnContextClassRef = "";
    };

    $scope.deleteAuthnContextDeclRef = function(index) {
        $scope.authnContextDeclRefs.splice(index, 1);
        $scope.identityProvider.config.authnContextDeclRefs = JSON.stringify($scope.authnContextDeclRefs);
    };

    $scope.addAuthnContextDeclRef = function() {
        $scope.authnContextDeclRefs.push($scope.newAuthnContextDeclRef);
        $scope.identityProvider.config.authnContextDeclRefs = JSON.stringify($scope.authnContextDeclRefs);
        $scope.newAuthnContextDeclRef = "";
    };
});

module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, TimeUnit, TimeUnit2, serverInfo) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.actionTokenProviders = $scope.serverInfo.providers.actionTokenHandler.providers;

    $scope.realm.accessTokenLifespan = TimeUnit2.asUnit(realm.accessTokenLifespan);
    $scope.realm.accessTokenLifespanForImplicitFlow = TimeUnit2.asUnit(realm.accessTokenLifespanForImplicitFlow);
    $scope.realm.ssoSessionIdleTimeout = TimeUnit2.asUnit(realm.ssoSessionIdleTimeout);
    $scope.realm.ssoSessionMaxLifespan = TimeUnit2.asUnit(realm.ssoSessionMaxLifespan);
    $scope.realm.ssoSessionIdleTimeoutRememberMe = TimeUnit2.asUnit(realm.ssoSessionIdleTimeoutRememberMe);
    $scope.realm.ssoSessionMaxLifespanRememberMe = TimeUnit2.asUnit(realm.ssoSessionMaxLifespanRememberMe);
    $scope.realm.offlineSessionIdleTimeout = TimeUnit2.asUnit(realm.offlineSessionIdleTimeout);
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    $scope.realm.offlineSessionMaxLifespan = TimeUnit2.asUnit(realm.offlineSessionMaxLifespan);
    $scope.realm.clientSessionIdleTimeout = TimeUnit2.asUnit(realm.clientSessionIdleTimeout);
    $scope.realm.clientSessionMaxLifespan = TimeUnit2.asUnit(realm.clientSessionMaxLifespan);
    $scope.realm.clientOfflineSessionIdleTimeout = TimeUnit2.asUnit(realm.clientOfflineSessionIdleTimeout);
    $scope.realm.clientOfflineSessionMaxLifespan = TimeUnit2.asUnit(realm.clientOfflineSessionMaxLifespan);
    $scope.realm.accessCodeLifespan = TimeUnit2.asUnit(realm.accessCodeLifespan);
    $scope.realm.accessCodeLifespanLogin = TimeUnit2.asUnit(realm.accessCodeLifespanLogin);
    $scope.realm.accessCodeLifespanUserAction = TimeUnit2.asUnit(realm.accessCodeLifespanUserAction);
    $scope.realm.actionTokenGeneratedByAdminLifespan = TimeUnit2.asUnit(realm.actionTokenGeneratedByAdminLifespan);
    $scope.realm.actionTokenGeneratedByUserLifespan = TimeUnit2.asUnit(realm.actionTokenGeneratedByUserLifespan);
    $scope.realm.oauth2DeviceCodeLifespan = TimeUnit2.asUnit(realm.oauth2DeviceCodeLifespan);
    $scope.requestUriLifespan = TimeUnit2.asUnit(realm.attributes.parRequestUriLifespan);
    $scope.realm.attributes = realm.attributes

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    
    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
    
    $scope.$watch('requestUriLifespan', function () {
        $scope.changed = true;
    }, true);
    
    $scope.$watch('actionLifespanId', function () {
        // changedActionLifespanId signals other watchers that we were merely 
        // changing the dropdown and we should not enable 'save' button
        if ($scope.actionTokenAttribute && $scope.actionTokenAttribute.hasOwnProperty('time')) {
            $scope.changedActionLifespanId = true;
        }
        
        $scope.actionTokenAttribute = TimeUnit2.asUnit($scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId]);
    }, true);

    $scope.$watch('actionTokenAttribute', function () {
        if ($scope.actionLifespanId === null) return;
        
        if ($scope.changedActionLifespanId) {
            $scope.changedActionLifespanId = false;
            return;
        } else {
            $scope.changed = true;
        }
        
        if ($scope.actionTokenAttribute !== null) {
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
        $scope.realm.ssoSessionIdleTimeoutRememberMe = $scope.realm.ssoSessionIdleTimeoutRememberMe.toSeconds();
        $scope.realm.ssoSessionMaxLifespanRememberMe = $scope.realm.ssoSessionMaxLifespanRememberMe.toSeconds();
        $scope.realm.offlineSessionIdleTimeout = $scope.realm.offlineSessionIdleTimeout.toSeconds();
        // KEYCLOAK-7688 Offline Session Max for Offline Token
        $scope.realm.offlineSessionMaxLifespan = $scope.realm.offlineSessionMaxLifespan.toSeconds();
        $scope.realm.clientSessionIdleTimeout = $scope.realm.clientSessionIdleTimeout.toSeconds();
        $scope.realm.clientSessionMaxLifespan = $scope.realm.clientSessionMaxLifespan.toSeconds();
        $scope.realm.clientOfflineSessionIdleTimeout = $scope.realm.clientOfflineSessionIdleTimeout.toSeconds();
        $scope.realm.clientOfflineSessionMaxLifespan = $scope.realm.clientOfflineSessionMaxLifespan.toSeconds();
        $scope.realm.accessCodeLifespan = $scope.realm.accessCodeLifespan.toSeconds();
        $scope.realm.accessCodeLifespanUserAction = $scope.realm.accessCodeLifespanUserAction.toSeconds();
        $scope.realm.accessCodeLifespanLogin = $scope.realm.accessCodeLifespanLogin.toSeconds();
        $scope.realm.actionTokenGeneratedByAdminLifespan = $scope.realm.actionTokenGeneratedByAdminLifespan.toSeconds();
        $scope.realm.actionTokenGeneratedByUserLifespan = $scope.realm.actionTokenGeneratedByUserLifespan.toSeconds();
        $scope.realm.oauth2DeviceCodeLifespan = $scope.realm.oauth2DeviceCodeLifespan.toSeconds();
        $scope.realm.attributes.parRequestUriLifespan = $scope.requestUriLifespan.toSeconds().toString();

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

module.controller('RealmUserProfileCtrl', function($scope, Realm, realm, clientScopes, $http, $location, $route, UserProfile, Dialog, Notifications, serverInfo) {
    $scope.realm = realm;
    $scope.validatorProviders = serverInfo.componentTypes['org.keycloak.validate.Validator'];

    $scope.isShowAttributes = true;

    UserProfile.get({realm: realm.realm}, function(config) {
        $scope.config = config;
        $scope.rawConfig = angular.toJson(config, true);
    });

    $scope.isShowAttributes = true;

    $scope.showAttributes = function() {
        $route.reload();
    }

    $scope.showJsonEditor = function() {
        $scope.isShowAttributes = false;
        delete $scope.currentAttribute;
    }

    $scope.isRequiredRoles = {
        minimumInputLength: 0,
        delay: 500,
        allowClear: true,
        id: function(e) { return e; },
        query: function (query) {
            var expectedRoles = ['user', 'admin'];
            var roles = [];

            if ('' == query.term.trim()) {
                roles = expectedRoles;
            } else {
                for (var i = 0; i < expectedRoles.length; i++) {
                    if (expectedRoles[i].indexOf(query.term.trim()) != -1) {
                        roles.push(expectedRoles[i]);
                    }
                }
            }

            query.callback({results: roles});
        },
        formatResult: function(object, container, query) {
            return object;
        },
        formatSelection: function(object, container, query) {
            return object;
        }
    };

    $scope.isRequiredScopes = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var scopes = [];

            if ('' == query.term.trim()) {
                scopes = clientScopes;
            } else {
                for (var i = 0; i < clientScopes.length; i++) {
                    if (clientScopes[i].name.indexOf(query.term.trim()) != -1) {
                        scopes.push(clientScopes[i]);
                    }
                }
            }

            query.callback({results: scopes});
        },
        formatResult: function(object, container, query) {
            return object.name;
        },
        formatSelection: function(object, container, query) {
            return object.name;
        }
    };

    $scope.selectorByScopeSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var scopes = [];

            if ('' == query.term.trim()) {
                scopes = clientScopes;
            } else {
                for (var i = 0; i < clientScopes.length; i++) {
                    if (clientScopes[i].name.indexOf(query.term.trim()) != -1) {
                        scopes.push(clientScopes[i]);
                    }
                }
            }

            query.callback({results: scopes});
        },
        formatResult: function(object, container, query) {
            return object.name;
        },
        formatSelection: function(object, container, query) {
            return object.name;
        }
    };

    $scope.attributeSelected = false;

    $scope.showListing = function() {
        return !$scope.attributeSelected && $scope.currentAttribute == null && $scope.isShowAttributes;
    }

    $scope.create = function() {
        $scope.isCreate = true;
        $scope.currentAttribute = {
            selector: {
                scopes: []
            },
            required: {
                roles: [],
                scopes: []
            },
            permissions: {
                view: [],
                edit: []
            }
        };
    };

	$scope.isNotUsernameOrEmail = function(attributeName) {
		return attributeName != "username" && attributeName != "email";
	}; 

	$scope.guiOrderUp = function(index) {
		$scope.moveAttribute(index, index - 1);
	};

	$scope.guiOrderDown = function(index) {
		$scope.moveAttribute(index, index + 1);
	};
	
	$scope.moveAttribute = function(old_index, new_index){
    	$scope.config.attributes.splice(new_index, 0, $scope.config.attributes.splice(old_index, 1)[0]);
		$scope.save();
	}

    $scope.removeAttribute = function(attribute) {
        Dialog.confirmDelete(attribute.name, 'attribute', function() {
            let newAttributes = [];

            for (var v of $scope.config.attributes) {
                if (v != attribute) {
                    newAttributes.push(v);
                }
            }

            $scope.config.attributes = newAttributes;
            $scope.save();
        });
    };

    $scope.addAnnotation = function() {
        if (!$scope.currentAttribute.annotations) {
            $scope.currentAttribute.annotations = {};
        }
        $scope.currentAttribute.annotations[$scope.newAnnotation.key] = $scope.newAnnotation.value;
        delete $scope.newAnnotation;
    }

    $scope.removeAnnotation = function(key) {
        delete $scope.currentAttribute.annotations[key];
    }

    $scope.edit = function(attribute) {
        if (attribute.permissions == null) {
            attribute.permissions = {
                view: [],
                edit: []
            };
        }

        if (attribute.selector == null) {
            attribute.selector = {
                scopes: []
            };
        }

        if (attribute.required) {
            if (attribute.required.roles) {
                $scope.requiredRoles = attribute.required.roles;
            }
            if (attribute.required.scopes) {
                for (var i = 0; i < attribute.required.scopes.length; i++) {
                    $scope.requiredScopes.push({
                        id: attribute.required.scopes[i],
                        name: attribute.required.scopes[i]
                    });
                }
            }
        }

        if (attribute.selector && attribute.selector.scopes) {
            for (var i = 0; i < attribute.selector.scopes.length; i++) {
                $scope.selectorByScope.push({
                    id: attribute.selector.scopes[i],
                    name: attribute.selector.scopes[i]
                });
            }
        }

        $scope.isRequired = attribute.required != null;
        $scope.canUserView = attribute.permissions.view.includes('user');
        $scope.canAdminView = attribute.permissions.view.includes('admin');
        $scope.canUserEdit = attribute.permissions.edit.includes('user');
        $scope.canAdminEdit = attribute.permissions.edit.includes('admin');
        $scope.currentAttribute = attribute;
        $scope.attributeSelected = true;
    };

    $scope.$watch('isRequired', function() {
        if ($scope.isRequired) {
            $scope.currentAttribute.required = {
                roles: [],
                scopes: []
            };
        } else if ($scope.currentAttribute) {
            delete $scope.currentAttribute.required;
        }
    }, true);

    handlePermission = function(permission, role, allowed) {
        let attribute = $scope.currentAttribute;

        if (attribute && attribute.permissions) {
            let roles = [];

            for (let r of attribute.permissions[permission]) {
                if (r != role) {
                    roles.push(r);
                }
            }

            if (allowed) {
                roles.push(role);
            }

            attribute.permissions[permission] = roles;
        }
    }

    $scope.$watch('canUserView', function() {
        handlePermission('view', 'user', $scope.canUserView);
    }, true);

    $scope.$watch('canAdminView', function() {
        handlePermission('view', 'admin', $scope.canAdminView);
    }, true);

    $scope.$watch('canUserEdit', function() {
        handlePermission('edit', 'user', $scope.canUserEdit);
    }, true);

    $scope.$watch('canAdminEdit', function() {
        handlePermission('edit', 'admin', $scope.canAdminEdit);
    }, true);

    $scope.addValidator = function(validator) {
        if ($scope.currentAttribute.validations == null) {
            $scope.currentAttribute.validations = {};
        }

        let config = {};

        for (let key in validator.config) {
            let values = validator.config[key];

            for (let k in values) {
                config[key] = values[k];
            }
        }

        $scope.currentAttribute.validations[validator.id] = config;

        delete $scope.newValidator;
    };

    $scope.selectValidator = function(validator) {
        validator.config = {};
    };

    $scope.cancelAddValidator = function() {
        delete $scope.newValidator;
    };

    $scope.removeValidator = function(id) {
        let newValidators = {};

        for (let v in $scope.currentAttribute.validations) {
            if (v != id) {
                newValidators[v] = $scope.currentAttribute.validations[v];
            }
        }

        if (newValidators.length == 0) {
            delete $scope.currentAttribute.validations;
            return;
        }

        $scope.currentAttribute.validations = newValidators;
    };

    $scope.save = function() {
        if (!$scope.isShowAttributes) {
            $scope.config = JSON.parse($scope.rawConfig);
        }

        if ($scope.currentAttribute) {
            if ($scope.isRequired) {
                $scope.currentAttribute.required.roles = $scope.requiredRoles;

                for (var i = 0; i < $scope.requiredScopes.length; i++) {
                    $scope.currentAttribute.required.scopes.push($scope.requiredScopes[i].name);
                }
            }

            $scope.currentAttribute.selector = {scopes: []};

            for (var i = 0; i < $scope.selectorByScope.length; i++) {
                $scope.currentAttribute.selector.scopes.push($scope.selectorByScope[i].name);
            }

            if ($scope.isCreate) {
                $scope.config['attributes'].push($scope.currentAttribute);
            }
        }

        UserProfile.update({realm: realm.realm},
            $scope.config,  function () {
                $scope.attributeSelected = false;
                delete $scope.currentAttribute;
                delete $scope.isCreate;
                delete $scope.isRequired;
                delete $scope.canUserView;
                delete $scope.canAdminView;
                delete $scope.canUserEdit;
                delete $scope.canAdminEdit;
                $route.reload();
                Notifications.success("User Profile configuration has been saved.");
            });
    };

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


module.controller('RoleListCtrl', function($scope, $route, Dialog, Notifications, realm, RoleList, RoleById, filterFilter) {
    $scope.realm = realm;
    $scope.roles = [];
    $scope.defaultRoleName = realm.defaultRole.name;

    $scope.query = {
        realm: realm.realm,
        search : null,
        max : 20,
        first : 0
    }

    $scope.$watch('query.search', function (newVal, oldVal) {
        if($scope.query.search && $scope.query.search.length >= 3) {
            $scope.firstPage();
        }
    }, true);

    $scope.firstPage = function() {
        $scope.query.first = 0;
        $scope.searchQuery();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.searchQuery();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.searchQuery();
    }

    $scope.searchQuery = function() {
        $scope.searchLoaded = false;

        $scope.roles = RoleList.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.searchQuery();
    
    $scope.determineEditLink = function(role) {
        return role.name === $scope.defaultRoleName ? "/realms/" + $scope.realm.realm + "/default-roles" : "/realms/" + $scope.realm.realm + "/roles/" + role.id;
    }

    $scope.removeRole = function (role) {
        if (role.name === $scope.defaultRoleName) return;

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


module.controller('RoleDetailCtrl', function($scope, realm, role, roles, Client, $route,
                                             Role, ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
                                             $http, $location, Dialog, Notifications, RealmRoleRemover, ComponentUtils) {
    $scope.realm = realm;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.save = function() {
        convertAttributeValuesToLists();
        console.log('save');
        if ($scope.create) {
            Role.save({
                realm: realm.realm
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                convertAttributeValuesToString($scope.role);
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

    $scope.addAttribute = function() {
        $scope.role.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
    }

    $scope.removeAttribute = function(key) {
        delete $scope.role.attributes[key];
    }

    function convertAttributeValuesToLists() {
        var attrs = $scope.role.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                var attrVals = attrs[attribute].split("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    function convertAttributeValuesToString(role) {
        var attrs = role.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                var attrVals = attrs[attribute].join("##");
                attrs[attribute] = attrVals;
                console.log("attribute" + attrVals)
            }
        }
    }

    roleControl($scope, $route, realm, role, roles, Client,
        ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
        $http, $location, Notifications, Dialog, ComponentUtils);
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

    $scope.testConnection = function() {
        RealmSMTPConnectionTester.save({realm: realm.realm}, realm.smtpServer, function() {
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

module.controller('IdentityProviderMapperCtrl', function ($scope, realm, identityProvider, mapperTypes, mapper, IdentityProviderMapper, Notifications, Dialog, ComponentUtils, $location) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.create = false;
    $scope.changed = false;
    $scope.mapperType = mapperTypes[mapper.identityProviderMapper];

    ComponentUtils.convertAllMultivaluedStringValuesToList($scope.mapperType.properties, mapper.config);
    ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.mapperType.properties, mapper.config);

    $scope.mapper = angular.copy(mapper);

    $scope.$watch(function () {
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
        let mapperCopy = angular.copy($scope.mapper);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.mapperType.properties, mapperCopy.config);

        IdentityProviderMapper.update({
            realm : realm.realm,
            alias : identityProvider.alias,
            mapperId : mapper.id
        }, mapperCopy, function () {
            $scope.changed = false;
            ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.mapperType.properties, $scope.mapper.config);
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

module.controller('IdentityProviderMapperCreateCtrl', function ($scope, realm, identityProvider, mapperTypes, IdentityProviderMapper, Notifications, Dialog, ComponentUtils, $location) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.create = true;
    $scope.mapper = { identityProviderAlias: identityProvider.alias, config: {}};
    $scope.mapperTypes = mapperTypes;
    
    // make first type the default
    $scope.mapperType = mapperTypes[Object.keys(mapperTypes)[0]];
    $scope.mapper.config.syncMode = 'INHERIT';

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function () {
        $scope.mapper.identityProviderMapper = $scope.mapperType.id;
        let copyMapper = angular.copy($scope.mapper);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.mapperType.properties, copyMapper.config);

        IdentityProviderMapper.save({
            realm : realm.realm,
            alias : identityProvider.alias
        }, copyMapper, function (data, headers) {
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
                                                      AuthenticationFlows, AuthenticationFlowsCopy, AuthenticationFlowsUpdate, AuthenticationFlowExecutions,
                                                      AuthenticationExecution, AuthenticationExecutionRaisePriority, AuthenticationExecutionLowerPriority,
                                                      $modal, Notifications, CopyDialog, UpdateDialog, $location) {
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

    $scope.editFlow = function(flow) {
        var copy = angular.copy(flow);
        UpdateDialog.open('Update Authentication Flow', copy.alias, copy.description, function(name, desc) {
            copy.alias = name;
            copy.description = desc;
            AuthenticationFlowsUpdate.update({realm: realm.realm, flow: flow.id}, copy, function() {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + name);
                Notifications.success("Flow updated");
            });
        })
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

    $scope.editExecutionFlow = function(execution) {
        var copy = angular.copy(execution);
        delete copy.empties;
        delete copy.levels;
        delete copy.preLevels;
        delete copy.postLevels;
        UpdateDialog.open('Update Execution Flow', copy.displayName, copy.description, function(name, desc) {
            copy.displayName = name;
            copy.description = desc;
            AuthenticationFlowExecutions.update({realm: realm.realm, alias: $scope.flow.alias}, copy, function() {
                Notifications.success("Execution Flow updated");
                setupForm();
            });
        })
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

module.controller('AuthenticationConfigCtrl', function($scope, realm, flow, configType, config, AuthenticationConfig, Notifications,
                                              Dialog, $location, ComponentUtils) {
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
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllListValuesToMultivaluedString(configType.properties, configCopy.config);

        AuthenticationConfig.update({
            realm : realm.realm,
            config : config.id
        }, configCopy, function() {
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

module.controller('AuthenticationConfigCreateCtrl', function($scope, realm, flow, configType, execution, AuthenticationExecutionConfig,
                                                    Notifications, Dialog, $location, ComponentUtils) {
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
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllListValuesToMultivaluedString(configType.properties, configCopy.config);

        AuthenticationExecutionConfig.save({
            realm : realm.realm,
            execution: execution
        }, configCopy, function(data, headers) {
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

module.controller('ClientRegPolicyDetailCtrl', function ($scope, realm, clientRegistrationPolicyProviders, instance, Dialog, Notifications, Components, ComponentUtils, $route, $location, $translate) {
    $scope.realm = realm;
    $scope.instance = instance;
    $scope.providerTypes = clientRegistrationPolicyProviders;

    for (let i = 0; i < $scope.providerTypes.length; i++) {
        let providerType = $scope.providerTypes[i];
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

    $translate($scope.instance.providerId + ".label")
        .then((translatedValue) => {
            $scope.headerTitle = translatedValue;
        }).catch(() => {
            $scope.headerTitle = $scope.instance.providerId;
    });

    if ($scope.create) {
        $scope.instance.name = "";
        $scope.instance.parentId = realm.id;
        $scope.instance.config = {};

        if ($scope.providerType.properties) {

            for (let i = 0; i < $scope.providerType.properties.length; i++) {
                let configProperty = $scope.providerType.properties[i];
                $scope.instance.config[configProperty.name] = toDefaultValue(configProperty);
            }
        }
    }

    if ($scope.providerType.properties) {
        ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.providerType.properties, $scope.instance.config);
        ComponentUtils.addMvOptionsToMultivaluedLists($scope.providerType.properties);
    }

    let oldCopy = angular.copy($scope.instance);
    $scope.changed = false;

    $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
    
    $scope.reset = function() {
        $scope.create ? window.history.back() : $route.reload();
    };

    $scope.hasValidValues = () => $scope.changed && $scope.instance.name;

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

module.controller('ClientPoliciesProfilesListCtrl', function($scope, realm, clientProfiles, ClientPoliciesProfiles, Dialog, Notifications, $route, $location) {
    console.log('ClientPoliciesProfilesListCtrl');
    $scope.realm = realm;
    $scope.clientProfiles = clientProfiles;

    $scope.removeClientProfile = function(clientProfile) {
        Dialog.confirmDelete(clientProfile.name, 'client profile', function() {
            console.log("Deleting client profile from the JSON: " + clientProfile.name);

            for (var i = 0; i < $scope.clientProfiles.profiles.length; i++) {
                var currentProfile = $scope.clientProfiles.profiles[i];
                if (currentProfile.name === clientProfile.name) {
                    $scope.clientProfiles.profiles.splice(i, 1);
                    break;
                }
            }

            ClientPoliciesProfiles.update({
                realm: realm.realm,
            }, $scope.clientProfiles, function () {
                $route.reload();
                Notifications.success("The client profile was deleted.");
            }, function (errorResponse) {
                $route.reload();
                var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
                Notifications.error('Failed to delete client profile: ' + errDetails);
            });
        });
    };

});

module.controller('ClientPoliciesProfilesJsonCtrl', function($scope, realm, clientProfiles, ClientPoliciesProfiles, Dialog, Notifications, $route, $location) {
    console.log('ClientPoliciesProfilesJsonCtrl');
    $scope.realm = realm;
    $scope.clientProfilesString = angular.toJson(clientProfiles, true);

    $scope.save = function() {
        var clientProfilesObj = null;
        try {
            clientProfilesObj = angular.fromJson($scope.clientProfilesString);
        } catch (e) {
            Notifications.error("Provided JSON is incorrect: " + e.message);
            console.log(e);
            return;
        }
        var clientProfilesCompressed = angular.toJson(clientProfilesObj, false);

        ClientPoliciesProfiles.update({
            realm: realm.realm,
        }, clientProfilesCompressed,  function () {
            $route.reload();
            Notifications.success("The client profiles configuration was updated.");
        }, function(errorResponse) {
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            Notifications.error("Failed to update client profiles: " + errDetails);
            console.log("Error response when updating client profiles JSON: Status: " + errorResponse.status +
                    ", statusText: " + errorResponse.statusText + ", data: " + JSON.stringify(errorResponse.data));
        });
    };

    $scope.reset = function() {
        $route.reload();
    };

});

module.controller('ClientPoliciesProfilesEditCtrl', function($scope, realm, clientProfiles, ClientPoliciesProfiles, Dialog, Notifications, $route, $location) {
    var targetProfileName = $route.current.params.profileName;
    $scope.createNew = targetProfileName == null;
    if ($scope.createNew) {
        console.log('ClientPoliciesProfilesEditCtrl: creating new profile');
    } else {
        console.log('ClientPoliciesProfilesEditCtrl: updating profile ' + targetProfileName);
    }

    $scope.realm = realm;
    $scope.editedProfile = null;

    function getProfileByName(profilesArray) {
        if (!profilesArray) return null;
        for (var i=0 ; i < profilesArray.length ; i++) {
            var currentProfile = profilesArray[i];
            if (targetProfileName === currentProfile.name) {
                return currentProfile;
            }
        }
    }

    if ($scope.createNew) {
        $scope.editedProfile = {
            name: "",
            executors: []
        };
    } else {
        var globalProfile = false;
        $scope.editedProfile = getProfileByName(clientProfiles.profiles);
        if (!$scope.editedProfile) {
            $scope.editedProfile = getProfileByName(clientProfiles.globalProfiles);
            globalProfile = true;
        }

        if ($scope.editedProfile == null) {
            console.log("Profile of name " + targetProfileName + " not found");
            throw 'Profile not found';
        }
    }

    // needs to be a function because when this controller runs, the permissions might not be loaded yet
    $scope.isReadOnly = function() {
        return !$scope.access.manageRealm || globalProfile;
    }

    $scope.removeExecutor = function(executorIndex) {
        Dialog.confirmDelete($scope.editedProfile.executors[executorIndex].executor, 'executor', function() {
            console.log("remove executor of index " + executorIndex);

            // Delete executor
            $scope.editedProfile.executors.splice(executorIndex, 1);

            ClientPoliciesProfiles.update({
                realm: realm.realm,
            }, clientProfiles, function () {
                Notifications.success("The executor was deleted.");
            }, function (errorResponse) {
                $route.reload();
                var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
                Notifications.error('Failed to delete executor: ' + errDetails);
            });
        });
    }

    $scope.save = function() {
        if (!$scope.editedProfile.name || $scope.editedProfile.name === '') {
            Notifications.error('Name must be provided');
            return;
        }

        if ($scope.createNew) {
            clientProfiles.profiles.push($scope.editedProfile);
        }

        ClientPoliciesProfiles.update({
            realm: realm.realm,
        }, clientProfiles,  function () {
            if ($scope.createNew) {
                Notifications.success("The client profile was created.");
                $location.url('/realms/' + realm.realm + '/client-policies/profiles-update/' + $scope.editedProfile.name);
            } else {
                Notifications.success("The client profile was updated.");
                $location.url('/realms/' + realm.realm + '/client-policies/profiles');
            }
        }, function(errorResponse) {
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            if ($scope.createNew) {
                Notifications.error('Failed to create client profile: ' + errDetails);
            } else {
                Notifications.error('Failed to update client profile: ' + errDetails);
            }
        });

    };

    $scope.back = function() {
        $location.url('/realms/' + realm.realm + '/client-policies/profiles');
    };

});

module.controller('ClientPoliciesProfilesEditExecutorCtrl', function($scope, realm, serverInfo, clientProfiles, ClientPoliciesProfiles, ComponentUtils, Dialog, Notifications, $route, $location) {
    var updatedExecutorIndex = $route.current.params.executorIndex;
    var targetProfileName = $route.current.params.profileName;
    $scope.createNew = updatedExecutorIndex == null;
    if ($scope.createNew) {
        console.log('ClientPoliciesProfilesEditExecutorCtrl: adding executor to profile ' + targetProfileName);
    } else {
        console.log('ClientPoliciesProfilesEditExecutorCtrl: updating executor with index ' + updatedExecutorIndex + ' of profile ' + targetProfileName);
    }
    $scope.realm = realm;

    function getProfileByName(profilesArray) {
        if (!profilesArray) return null;
        for (var i=0 ; i < profilesArray.length ; i++) {
            var currentProfile = profilesArray[i];
            if (targetProfileName === currentProfile.name) {
                return currentProfile;
            }
        }
    }

    var globalProfile = false;
    $scope.editedProfile = getProfileByName(clientProfiles.profiles);
    if (!$scope.editedProfile) {
        $scope.editedProfile = getProfileByName(clientProfiles.globalProfiles);
        globalProfile = true;
    }
    if ($scope.editedProfile == null) {
        throw 'Client profile of specified name not found';
    }

    // needs to be a function because when this controller runs, the permissions might not be loaded yet
    $scope.isReadOnly = function() {
        return !$scope.access.manageRealm || globalProfile;
    }

    $scope.executorTypes = serverInfo.componentTypes['org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider'];

    for (var j=0 ; j < $scope.executorTypes.length ; j++) {
        var currExecutorType = $scope.executorTypes[j];
        if (currExecutorType.properties) {
            console.log("Adjusting executorType: " + currExecutorType.id);
            ComponentUtils.addMvOptionsToMultivaluedLists(currExecutorType.properties);
        }
    }

    function getExecutorByIndex(clientProfile, executorIndex) {
        if (clientProfile.executors.length <= executorIndex) {
            console.error('Client profile does not have executor of specified index');
            $location.path('/notfound');
            return null;
        } else {
            return clientProfile.executors[executorIndex];
        }
    }

    if ($scope.createNew) {
        // make first type the default
        $scope.executorType = $scope.executorTypes[0];
        var oldExecutorType = $scope.executorType;
        initConfig();

        $scope.$watch('executorType', function() {
            if (!angular.equals($scope.executorType, oldExecutorType)) {
                oldExecutorType = $scope.executorType;
                initConfig();
            }
        }, true);
    } else {
        var exec = getExecutorByIndex($scope.editedProfile, updatedExecutorIndex);
        if (exec) {
            // a failsafe in case the configuration was deleted entirely (or set to null) in the JSON view
            if (!exec.configuration) {
                exec.configuration = {}
            }

            $scope.executor = {
                config: exec.configuration
            };

            $scope.executorType = null;
            for (var j=0 ; j < $scope.executorTypes.length ; j++) {
                var currentExType = $scope.executorTypes[j];
                if (exec.executor === currentExType.id) {
                    $scope.executorType = currentExType;
                    break;
                }
            }

            for (var j=0 ; j < $scope.executorType.properties.length ; j++) {
                // Convert boolean properties from the configuration to strings as expected by the kc-provider-config directive
                var currentProperty = $scope.executorType.properties[j];
                if (currentProperty.type === 'boolean') {
                    $scope.executor.config[currentProperty.name] = ($scope.executor.config[currentProperty.name]) ? "true" : "false";
                }

                // a workaround for select2 to prevent displaying empty boxes
                var configProperty = $scope.executor.config[$scope.executorType.properties[j].name];
                if (Array.isArray(configProperty) && configProperty.length === 0) {
                    $scope.executor.config[$scope.executorType.properties[j].name] = null
                }

            }
        }

    }

    function toDefaultValue(configProperty) {
        if (configProperty.type === 'boolean') {
            return (configProperty.defaultValue) ? "true" : "false";
        }

        if (configProperty.defaultValue !== undefined) {
            if ((configProperty.type === 'MultivaluedString' || configProperty.type === 'MultivaluedList') && !Array.isArray(configProperty.defaultValue)) {
                return [configProperty.defaultValue]
            }
            return configProperty.defaultValue;
        } else {
            return null;
        }
    }

    function initConfig() {
        console.log("Initialized config now. ConfigType is: " + $scope.executorType.id);
        $scope.executor = {
            config: {}
        };

       for (let i = 0; i < $scope.executorType.properties.length; i++) {
           let configProperty = $scope.executorType.properties[i];
           $scope.executor.config[configProperty.name] = toDefaultValue(configProperty);
       }
    }

    $scope.save = function() {
        console.log("save: " + $scope.executorType.id);

        var executorName = $scope.executorType.id;
        if (!$scope.editedProfile.executors) {
            $scope.editedProfile.executors = [];
        }

        ComponentUtils.removeLastEmptyValue($scope.executor.config);

        // Convert String properties required by the kc-provider-config directive back to booleans
        for (var j=0 ; j < $scope.executorType.properties.length ; j++) {
            var currentProperty = $scope.executorType.properties[j];
            if (currentProperty.type === 'boolean') {
                $scope.executor.config[currentProperty.name] = ($scope.executor.config[currentProperty.name] === "true") ? true : false;
            }
        }

        if ($scope.createNew) {
            var selectedExecutor = {
                executor: $scope.executorType.id,
                configuration: $scope.executor.config
            };
            $scope.editedProfile.executors.push(selectedExecutor);
        } else {
            var currentExecutor = getExecutorByIndex($scope.editedProfile, updatedExecutorIndex);
            if (currentExecutor) {
                currentExecutor.configuration = $scope.executor.config;
            }
         }

        ClientPoliciesProfiles.update({
            realm: realm.realm,
        }, clientProfiles,  function () {
            if ($scope.createNew) {
                Notifications.success("Executor created successfully");
            } else {
                Notifications.success("Executor updated successfully");
            }
            $location.url('/realms/' + realm.realm + '/client-policies/profiles-update/' + $scope.editedProfile.name);
        }, function(errorResponse) {
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            if ($scope.createNew) {
                Notifications.error('Failed to create executor: ' + errDetails);
            } else {
                Notifications.error('Failed to update executor: ' + errDetails);
            }
        });

    };

    $scope.cancel = function() {
        $location.url('/realms/' + realm.realm + '/client-policies/profiles-update/' + $scope.editedProfile.name);
    };

});

module.controller('ClientPoliciesListCtrl', function($scope, realm, clientPolicies, ClientPolicies, Dialog, Notifications, $route, $location) {
    console.log('ClientPoliciesListCtrl');
    $scope.realm = realm;
    $scope.clientPolicies = clientPolicies;

    $scope.removeClientPolicy = function(clientPolicy) {
        Dialog.confirmDelete(clientPolicy.name, 'client policy', function() {
            console.log("Deleting client policy from the JSON: " + clientPolicy.name);

            for (var i = 0; i < $scope.clientPolicies.policies.length; i++) {
                var currentPolicy = $scope.clientPolicies.policies[i];
                if (currentPolicy.name === clientPolicy.name) {
                    $scope.clientPolicies.policies.splice(i, 1);
                    break;
                }
            }

            ClientPolicies.update({
                realm: realm.realm,
            }, $scope.clientPolicies, function () {
                $route.reload();
                Notifications.success("The client policy was deleted.");
            }, function (errorResponse) {
                $route.reload();
                var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
                Notifications.error('Failed to delete client policy: ' + errDetails);
            });
        });
    };

});

module.controller('ClientPoliciesJsonCtrl', function($scope, realm, clientPolicies, Dialog, Notifications, ClientPolicies, $route, $location) {
    console.log('ClientPoliciesJsonCtrl');
    $scope.realm = realm;
    $scope.clientPoliciesString = angular.toJson(clientPolicies, true);

    $scope.save = function() {
        var clientPoliciesObj = null;
        try {
            var clientPoliciesObj = angular.fromJson($scope.clientPoliciesString);
        } catch (e) {
            Notifications.error("Provided JSON is incorrect: " + e.message);
            console.log(e);
            return;
        }
        var clientPoliciesCompressed = angular.toJson(clientPoliciesObj, false);

        ClientPolicies.update({
            realm: realm.realm,
        }, clientPoliciesCompressed,  function () {
            $route.reload();
            Notifications.success("The client policies configuration was updated.");
        }, function(errorResponse) {
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            Notifications.error("Failed to update client policies: " + errDetails);
            console.log("Error response when updating client policies JSON: Status: " + errorResponse.status +
                    ", statusText: " + errorResponse.statusText + ", data: " + JSON.stringify(errorResponse.data));
        });
    };

    $scope.reset = function() {
        $route.reload();
    };
});

module.controller('ClientPoliciesEditCtrl', function($scope, realm, clientProfiles, clientPolicies, ClientPolicies, Dialog, Notifications, $route, $location) {
    var targetPolicyName = $route.current.params.policyName;
    $scope.createNew = targetPolicyName == null;
    if ($scope.createNew) {
        console.log('ClientPoliciesEditCtrl: creating new policy');
    } else {
        console.log('ClientPoliciesEditCtrl: updating policy ' + targetPolicyName);
    }

    $scope.realm = realm;
    $scope.clientPolicies = clientPolicies;
    $scope.clientProfiles = clientProfiles;
    $scope.editedPolicy = null;

    if ($scope.createNew) {
        $scope.editedPolicy = {
            name: "",
            enabled: true,
            profiles: [],
            conditions: []
        };
    } else {
        for (var i=0 ; i < $scope.clientPolicies.policies.length ; i++) {
            var currentPolicy = $scope.clientPolicies.policies[i];
            if (targetPolicyName === currentPolicy.name) {
                $scope.editedPolicy = currentPolicy;
                break;
            }
        }

        if ($scope.editedPolicy == null) {
            console.log("Policy of name " + targetPolicyName + " not found");
            throw 'Policy not found';
        }
    }

    // needs to be a function because when this controller runs, the permissions might not be loaded yet
    $scope.isReadOnly = function() {
        return !$scope.access.manageRealm;
    }

    $scope.availableProfiles = [];
    var allClientProfiles = clientProfiles.profiles;
    if (clientProfiles.globalProfiles) {
        allClientProfiles = allClientProfiles.concat(clientProfiles.globalProfiles);
    }
    for (var k=0 ; k<allClientProfiles.length ; k++) {
        var profileName = allClientProfiles[k].name;
        if (!$scope.editedPolicy.profiles || !$scope.editedPolicy.profiles.includes(profileName)) {
            $scope.availableProfiles.push(profileName);
        }
    }

    $scope.removeCondition = function(conditionIndex) {
        Dialog.confirmDelete($scope.editedPolicy.conditions[conditionIndex].condition, 'condition', function() {
            console.log("remove condition of index " + conditionIndex);

            // Delete condition
            $scope.editedPolicy.conditions.splice(conditionIndex, 1);

            ClientPolicies.update({
                realm: realm.realm,
            }, $scope.clientPolicies, function () {
                Notifications.success("The condition was deleted.");
            }, function (errorResponse) {
                $route.reload();
                var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
                Notifications.error('Failed to delete condition: ' + errDetails);
            });
        });
    }

    $scope.save = function() {
        if (!$scope.editedPolicy.name || $scope.editedPolicy.name === '') {
            Notifications.error('Name must be provided');
            return;
        }

        if ($scope.createNew) {
            $scope.clientPolicies.policies.push($scope.editedPolicy);
        }

        ClientPolicies.update({
            realm: realm.realm,
        }, $scope.clientPolicies,  function () {
            if ($scope.createNew) {
                Notifications.success("The client policy was created.");
                $location.url('/realms/' + realm.realm + '/client-policies/policies-update/' + $scope.editedPolicy.name);
            } else {
                Notifications.success("The client policy was updated.");
                $location.url('/realms/' + realm.realm + '/client-policies/policies');
            }
        }, function(errorResponse) {
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            if ($scope.createNew) {
                Notifications.error('Failed to create client policy: ' + errDetails);
            } else {
                Notifications.error('Failed to update client policy: ' + errDetails);
            }
        });

    };

    $scope.back = function() {
        $location.url('/realms/' + realm.realm + '/client-policies/policies');
    };


    function moveProfileAndUpdatePolicy(arrayFrom, arrayTo, profileName, notificationsMessage) {
        for (var i=0 ; i<arrayFrom.length ; i++) {
            if (arrayFrom[i] === profileName) {
                arrayFrom.splice(i, 1);
                arrayTo.push(profileName);
                break;
            }
        }

        ClientPolicies.update({
            realm: realm.realm,
        }, $scope.clientPolicies,  function () {
            Notifications.success(notificationsMessage);
        }, function(errorResponse) {
            $route.reload();
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            Notifications.error('Failed to update profiles of the policy: ' + errDetails);
        });
    }

    $scope.addProfile = function(profileName) {
        console.log("addProfile: " + profileName);
        moveProfileAndUpdatePolicy($scope.availableProfiles, $scope.editedPolicy.profiles, profileName, "Profile added to the policy");
    };

    $scope.removeProfile = function(profileName) {
        console.log("removeProfile: " + profileName);
        moveProfileAndUpdatePolicy( $scope.editedPolicy.profiles, $scope.availableProfiles, profileName, "Profile removed from the policy");
    }

});

module.controller('ClientPoliciesEditConditionCtrl', function($scope, realm, serverInfo, clientPolicies, ClientPolicies, Components, ComponentUtils, Dialog, Notifications, $route, $location) {
    var updatedConditionIndex = $route.current.params.conditionIndex;
    var targetPolicyName = $route.current.params.policyName;
    $scope.createNew = updatedConditionIndex == null;
    if ($scope.createNew) {
        console.log('ClientPoliciesEditConditionCtrl: adding condition to policy ' + targetPolicyName);
    } else {
        console.log('ClientPoliciesEditConditionCtrl: updating condition with index ' + updatedConditionIndex + ' of policy ' + targetPolicyName);
    }
    $scope.realm = realm;

    $scope.editedPolicy = null;
    for (var i=0 ; i < clientPolicies.policies.length ; i++) {
        var currentPolicy = clientPolicies.policies[i];
        if (targetPolicyName === currentPolicy.name) {
            $scope.editedPolicy = currentPolicy;
            break;
        }
    }
    if ($scope.editedPolicy == null) {
        throw 'Client policy of specified name not found';
    }

    // needs to be a function because when this controller runs, the permissions might not be loaded yet
    $scope.isReadOnly = function() {
        return !$scope.access.manageRealm;
    }

    $scope.conditionTypes = serverInfo.componentTypes['org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider'];

    for (var j=0 ; j < $scope.conditionTypes.length ; j++) {
        var currConditionType = $scope.conditionTypes[j];
        if (currConditionType.properties) {
            console.log("Adjusting conditionType: " + currConditionType.id);
            ComponentUtils.addMvOptionsToMultivaluedLists(currConditionType.properties);
        }
    }

    function getConditionByIndex(clientPolicy, conditionIndex) {
        if (clientPolicy.conditions.length <= conditionIndex) {
            console.error('Client policy does not have condition of specified index');
            $location.path('/notfound');
            return null;
        } else {
            return clientPolicy.conditions[conditionIndex];
        }
    }

    if ($scope.createNew) {
        // make first type the default
        $scope.conditionType = $scope.conditionTypes[0];
        var oldConditionType = $scope.conditionType;
        initConfig();

        $scope.$watch('conditionType', function() {
            if (!angular.equals($scope.conditionType, oldConditionType)) {
                oldConditionType = $scope.conditionType;
                initConfig();
            }
        }, true);
    } else {
        var cond = getConditionByIndex($scope.editedPolicy, updatedConditionIndex);
        if (cond) {
            // a failsafe in case the configuration was deleted entirely (or set to null) in the JSON view
            if (!cond.configuration) {
                cond.configuration = {}
            }

            $scope.condition = {
                config: cond.configuration
            };

            $scope.conditionType = null;
            for (var j=0 ; j < $scope.conditionTypes.length ; j++) {
                var currentCndType = $scope.conditionTypes[j];
                if (cond.condition === currentCndType.id) {
                    $scope.conditionType = currentCndType;
                    break;
                }
            }

            for (var j=0 ; j < $scope.conditionType.properties.length ; j++) {
                // Convert boolean properties from the configuration to strings as expected by the kc-provider-config directive
                var currentProperty = $scope.conditionType.properties[j];
                if (currentProperty.type === 'boolean') {
                    $scope.condition.config[currentProperty.name] = ($scope.condition.config[currentProperty.name]) ? "true" : "false";
                }

                // a workaround for select2 to prevent displaying empty boxes
                var configProperty = $scope.condition.config[$scope.conditionType.properties[j].name];
                if (Array.isArray(configProperty) && configProperty.length === 0) {
                    $scope.condition.config[$scope.conditionType.properties[j].name] = null
                }

            }
        }

    }

    function toDefaultValue(configProperty) {
        if (configProperty.type === 'boolean') {
            return (configProperty.defaultValue) ? "true" : "false";
        }

        if (configProperty.defaultValue !== undefined) {
            if ((configProperty.type === 'MultivaluedString' || configProperty.type === 'MultivaluedList') && !Array.isArray(configProperty.defaultValue)) {
                return [configProperty.defaultValue]
            }
            return configProperty.defaultValue;
        } else {
            return null;
        }
    }

    function initConfig() {
        console.log("Initialized config now. ConfigType is: " + $scope.conditionType.id);
        $scope.condition = {
            config: {}
        };

       for (let i = 0; i < $scope.conditionType.properties.length; i++) {
           let configProperty = $scope.conditionType.properties[i];
           $scope.condition.config[configProperty.name] = toDefaultValue(configProperty);
       }
    }


    $scope.save = function() {
        console.log("save: " + $scope.conditionType.id);

        var conditionName = $scope.conditionType.id;
        if (!$scope.editedPolicy.conditions) {
            $scope.editedPolicy.conditions = [];
        }

        ComponentUtils.removeLastEmptyValue($scope.condition.config);

        // Convert String properties required by the kc-provider-config directive back to booleans
        for (var j=0 ; j < $scope.conditionType.properties.length ; j++) {
            var currentProperty = $scope.conditionType.properties[j];
            if (currentProperty.type === 'boolean') {
                $scope.condition.config[currentProperty.name] = ($scope.condition.config[currentProperty.name] === "true") ? true : false;
            }
        }

        var selectedCondition;
        if ($scope.createNew) {
            var selectedCondition = {
                condition: $scope.conditionType.id,
                configuration: $scope.condition.config
            };
            $scope.editedPolicy.conditions.push(selectedCondition);
        } else {
            var currentCondition = getConditionByIndex($scope.editedPolicy, updatedConditionIndex);
            if (currentCondition) {
                currentCondition.configuration = $scope.condition.config;
            }
        }

        ClientPolicies.update({
            realm: realm.realm,
        }, clientPolicies,  function () {
            if ($scope.createNew) {
                Notifications.success("Condition created successfully");
            } else {
                Notifications.success("Condition updated successfully");
            }
            $location.url('/realms/' + realm.realm + '/client-policies/policies-update/' + $scope.editedPolicy.name);
        }, function(errorResponse) {
            var errDetails = (!errorResponse.data.errorMessage) ? "unknown error, please see the server log" : errorResponse.data.errorMessage
            if ($scope.createNew) {
                Notifications.error('Failed to create condition: ' + errDetails);
            } else {
                Notifications.error('Failed to update condition: ' + errDetails);
            }
        });

    };

    $scope.cancel = function() {
        $location.url('/realms/' + realm.realm + '/client-policies/policies-update/' + $scope.editedPolicy.name);
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
