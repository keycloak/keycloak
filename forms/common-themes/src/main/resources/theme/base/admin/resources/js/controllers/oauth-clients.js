module.controller('OAuthClientClaimsCtrl', function($scope, realm, oauth, claims,
                                                    OAuthClientClaims,
                                                    $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.oauth = oauth;
    $scope.claims = angular.copy(claims);

    $scope.changed = false;

    $scope.$watch('claims', function () {
        if (!angular.equals($scope.claims, claims)) {
            $scope.changed = true;
        }
    }, true);


    $scope.save = function () {
        OAuthClientClaims.update({
            realm: realm.realm,
            oauth: oauth.id
        }, $scope.claims, function () {
            $scope.changed = false;
            claims = angular.copy($scope.claims);

            Notifications.success("Your claim changes have been saved.");
        });
    };

    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + "/oauth-clients/" + oauth.id + "/claims");
    };

});

module.controller('OAuthClientCredentialsCtrl', function($scope, $location, realm, oauth, OAuthClientCredentials, Notifications) {
    $scope.realm = realm;
    $scope.oauth = oauth;

    var secret = OAuthClientCredentials.get({ realm : realm.realm, oauth : oauth.id },
        function() {
            $scope.secret = secret.value;
        }
    );

    $scope.changePassword = function() {
        var secret = OAuthClientCredentials.update({ realm : realm.realm,  oauth : oauth.id  },
            function() {
                Notifications.success('The secret has been changed.');
                $scope.secret = secret.value;
            },
            function() {
                Notifications.error("The secret was not changed due to a problem.");
                $scope.secret = "error";
            }
        );
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

});

module.controller('OAuthClientListCtrl', function($scope, realm, oauthClients, OAuthClient, $location) {
    $scope.realm = realm;
    $scope.oauthClients = oauthClients;
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('OAuthClientDetailCtrl', function($scope, realm, oauth, OAuthClient, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.create = !oauth.id;

    $scope.accessTypes = [
        "confidential",
        "public"
    ];

    $scope.changeAccessType = function() {
        if ($scope.accessType == "confidential") {
            $scope.oauth.publicClient = false;
        } else if ($scope.accessType == "public") {
            $scope.oauth.publicClient = true;
        }
    };


    if (!$scope.create) {
        $scope.oauth= angular.copy(oauth);
        $scope.accessType = $scope.accessTypes[0];
        if (oauth.publicClient) {
            $scope.accessType = $scope.accessTypes[1];
        }
    } else {
        $scope.oauth = { enabled: true };
        $scope.oauth.webOrigins = [];
        $scope.oauth.redirectUris = [];
        $scope.accessType = $scope.accessTypes[0];
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('oauth', function() {
        if (!angular.equals($scope.oauth, oauth)) {
            $scope.changed = true;
        }
    }, true);

    $scope.deleteWebOrigin = function(index) {
        $scope.oauth.webOrigins.splice(index, 1);
    }
    $scope.addWebOrigin = function() {
        $scope.oauth.webOrigins.push($scope.newWebOrigin);
        $scope.newWebOrigin = "";
    }
    $scope.deleteRedirectUri = function(index) {
        $scope.oauth.redirectUris.splice(index, 1);
    }
    $scope.addRedirectUri = function() {
        $scope.oauth.redirectUris.push($scope.newRedirectUri);
        $scope.newRedirectUri = "";
    }

    $scope.save = function() {
        if (!$scope.oauth.directGrantsOnly && (!$scope.oauth.redirectUris || $scope.oauth.redirectUris.length == 0)) {
            Notifications.error("You must specify at least one redirect uri");
        } else {
            if ($scope.create) {
                OAuthClient.save({
                    realm: realm.realm
                }, $scope.oauth, function (data, headers) {
                    $scope.changed = false;
                    var l = headers().location;
                    var name = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.realm + "/oauth-clients/" + name);
                    Notifications.success("The oauth client has been created.");
                });
            } else {
                OAuthClient.update({
                    realm : realm.realm,
                    oauth : oauth.id
                }, $scope.oauth, function() {
                    $scope.changed = false;
                    oauth = angular.copy($scope.oauth);
                    $location.url("/realms/" + realm.realm + "/oauth-clients/" + oauth.id);
                    Notifications.success("Your changes have been saved to the oauth client.");
                });
            }
        }
    };

    $scope.reset = function() {
        $scope.oauth = angular.copy(oauth);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/oauth-clients");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.oauth.id, 'oauth', function() {
            $scope.oauth.$remove({
                realm : realm.realm,
                oauth : $scope.oauth.id
            }, function() {
                $location.url("/realms/" + realm.realm + "/oauth-clients");
                Notifications.success("The oauth client has been deleted.");
            });
        });
    };


});

module.controller('OAuthClientScopeMappingCtrl', function($scope, $http, realm, oauth, applications, Notifications,
                                                          OAuthClient,
                                                          OAuthClientRealmScopeMapping, OAuthClientApplicationScopeMapping, ApplicationRole,
                                                          OAuthClientAvailableRealmScopeMapping, OAuthClientAvailableApplicationScopeMapping,
                                                          OAuthClientCompositeRealmScopeMapping, OAuthClientCompositeApplicationScopeMapping) {
    $scope.realm = realm;
    $scope.oauth = angular.copy(oauth);
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

    $scope.changeFullScopeAllowed = function() {
        console.log('change full scope');
        OAuthClient.update({
            realm : realm.realm,
            oauth : oauth.id
        }, $scope.oauth, function() {
            $scope.changed = false;
            oauth = angular.copy($scope.oauth);
            Notifications.success("Scope mappings updated.");
        });

    }


    function updateRealmRoles() {
        $scope.realmRoles = OAuthClientAvailableRealmScopeMapping.query({realm : realm.realm, oauth : oauth.id});
        $scope.realmMappings = OAuthClientRealmScopeMapping.query({realm : realm.realm, oauth : oauth.id});
        $scope.realmComposite = OAuthClientCompositeRealmScopeMapping.query({realm : realm.realm, oauth : oauth.id});
    }

    function updateAppRoles() {
        if ($scope.targetApp) {
            console.debug($scope.targetApp.name);
            $scope.applicationRoles = OAuthClientAvailableApplicationScopeMapping.query({realm : realm.realm, oauth : oauth.id, targetApp : $scope.targetApp.id});
            $scope.applicationMappings = OAuthClientApplicationScopeMapping.query({realm : realm.realm, oauth : oauth.id, targetApp : $scope.targetApp.id});
            $scope.applicationComposite = OAuthClientCompositeApplicationScopeMapping.query({realm : realm.realm, oauth : oauth.id, targetApp : $scope.targetApp.id});
        } else {
            $scope.applicationRoles = null;
            $scope.applicationMappings = null;
            $scope.applicationComposite = null;
        }
    }

    $scope.changeApplication = function() {
        updateAppRoles();
    };

    $scope.addRealmRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/oauth-clients-by-id/' + oauth.id + '/scope-mappings/realm',
            $scope.selectedRealmRoles).success(function () {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/oauth-clients-by-id/' + oauth.id +  '/scope-mappings/realm',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function () {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");

            });
    };

    $scope.addApplicationRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/oauth-clients-by-id/' + oauth.id +  '/scope-mappings/applications-by-id/' + $scope.targetApp.id,
            $scope.selectedApplicationRoles).success(function () {
                updateAppRoles();
                Notifications.success("Scope mappings updated.");

            });
    };

    $scope.deleteApplicationRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/oauth-clients-by-id/' + oauth.id +  '/scope-mappings/applications-by-id/' + $scope.targetApp.id,
            {data : $scope.selectedApplicationMappings, headers : {"content-type" : "application/json"}}).success(function () {
                updateAppRoles();
                Notifications.success("Scope mappings updated.");

            });
    };

    updateRealmRoles();
});

module.controller('OAuthClientInstallationCtrl', function($scope, realm, installation, oauth, OAuthClientInstallation, $routeParams) {
    $scope.realm = realm;
    $scope.oauth = oauth;
    $scope.installation = installation;

    $scope.download = function() {
        saveAs(new Blob([angular.toJson($scope.installation, true)], { type: 'application/json' }), 'keycloak.json');
    }
});

module.controller('OAuthClientRevocationCtrl', function($scope, realm, oauth, OAuthClient, $location, Dialog, Notifications) {
    $scope.oauth = oauth;
    $scope.realm = realm;
    var setNotBefore = function() {
        if ($scope.oauth.notBefore == 0) {
            $scope.notBefore = "None";
        } else {
            $scope.notBefore = new Date($scope.oauth.notBefore * 1000);
        }
    };

    setNotBefore();

    var refresh = function() {
        OAuthClient.get({ realm : realm.realm, oauth: $scope.oauth.id }, function(updated) {
            $scope.oauth = updated;
            setNotBefore();
        })

    };

    $scope.clear = function() {
        $scope.oauth.notBefore = 0;
        OAuthClient.update({ realm : realm.realm, oauth: $scope.oauth.id}, $scope.oauth, function () {
            $scope.notBefore = "None";
            Notifications.success('Not Before cleared for application.');
            refresh();
        });
    }
    $scope.setNotBeforeNow = function() {
        $scope.oauth.notBefore = new Date().getTime()/1000;
        OAuthClient.update({ realm : realm.realm, oauth: $scope.oauth.id}, $scope.oauth, function () {
            Notifications.success('Not Before cleared for application.');
            refresh();
        });
    }
});

module.controller('OAuthClientIdentityProviderCtrl', function($scope, $route, realm, oauth, OAuthClient, $location, Notifications) {
    $scope.realm = realm;
    $scope.oauth = angular.copy(oauth);
    var length = 0;

    if ($scope.oauth.identityProviders) {
        length = $scope.oauth.identityProviders.length;
    } else {
        $scope.oauth.identityProviders = new Array(realm.identityProviders.length);
    }

    for (j = length; j < realm.identityProviders.length; j++) {
        $scope.oauth.identityProviders[j] = {};
    }

    $scope.identityProviders = [];

    for (j = 0; j < realm.identityProviders.length; j++) {
        var identityProvider = realm.identityProviders[j];
        var match = false;
        var applicationProvider;

        for (i = 0; i < $scope.oauth.identityProviders.length; i++) {
            applicationProvider = $scope.oauth.identityProviders[i];

            if (applicationProvider) {
                if (applicationProvider.retrieveToken) {
                    applicationProvider.retrieveToken = applicationProvider.retrieveToken.toString();
                } else {
                    applicationProvider.retrieveToken = false.toString();
                }

                if (applicationProvider.id == identityProvider.id) {
                    $scope.identityProviders[i] = {};
                    $scope.identityProviders[i].identityProvider = identityProvider;
                    $scope.identityProviders[i].retrieveToken = applicationProvider.retrieveToken.toString();
                    break;
                }

                applicationProvider = null;
            }
        }

        if (applicationProvider == null) {
            var length = $scope.identityProviders.length + $scope.oauth.identityProviders.length;

            $scope.identityProviders[length] = {};
            $scope.identityProviders[length].identityProvider = identityProvider;
            $scope.identityProviders[length].retrieveToken = false.toString();
        }
    }

    $scope.identityProviders = $scope.identityProviders.filter(function(n){ return n != undefined });

    var oldCopy = angular.copy($scope.oauth);

    $scope.save = function() {
        var selectedProviders = [];

        for (i = 0; i < $scope.oauth.identityProviders.length; i++) {
            var appProvider = $scope.oauth.identityProviders[i];

            if (appProvider.id != null && appProvider.id != false) {
                selectedProviders[selectedProviders.length] = appProvider;
            }
        }

        $scope.oauth.identityProviders = selectedProviders;

        OAuthClient.update({
            realm : realm.realm,
            oauth : oauth.id
        }, $scope.oauth, function() {
            $scope.changed = false;
            $route.reload();
            Notifications.success("Your changes have been saved to the application.");
        });
    };

    $scope.reset = function() {
        $scope.oauth = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.$watch('oauth', function() {
        if (!angular.equals($scope.oauth, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
});

module.controller('OAuthClientProtocolMapperListCtrl', function($scope, realm, oauth, serverInfo,
                                                                OAuthClientProtocolMappersByProtocol,
                                                                $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.oauth = oauth;
    if (oauth.protocol == null) {
        oauth.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[oauth.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;


    var updateMappers = function() {
        $scope.mappers = OAuthClientProtocolMappersByProtocol.query({realm : realm.realm, oauth : oauth.id, protocol : oauth.protocol});
    };

    updateMappers();
});

module.controller('OAuthClientAddBuiltinProtocolMapperCtrl', function($scope, realm, oauth, serverInfo,
                                                           OAuthClientProtocolMappersByProtocol,
                                                           $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.oauth = oauth;
    if (oauth.protocol == null) {
        oauth.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[oauth.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;




    var updateMappers = function() {
        var appMappers = OAuthClientProtocolMappersByProtocol.query({realm : realm.realm, oauth : oauth.id, protocol : oauth.protocol}, function() {
            var builtinMappers = serverInfo.builtinProtocolMappers[oauth.protocol];
            for (var i = 0; i < appMappers.length; i++) {
                for (var j = 0; j < builtinMappers.length; j++) {
                    if (builtinMappers[j].name == appMappers[i].name
                        && builtinMappers[j].protocolMapper == appMappers[i].protocolMapper) {
                        console.log('removing: ' + builtinMappers[j].name);
                        builtinMappers.splice(j, 1);
                        break;
                    }
                }
            }
            for (var j = 0; j < builtinMappers.length; j++) {
                console.log('builtin left: ' + builtinMappers[j].name);
            }
            $scope.mappers = builtinMappers;
            for (var i = 0; i < $scope.mappers.length; i++) {
                $scope.mappers[i].isChecked = false;
            }


        });
    };

    updateMappers();

    $scope.add = function() {
        var toAdd = [];
        for (var i = 0; i < $scope.mappers.length; i++) {
            if ($scope.mappers[i].isChecked) {
                delete $scope.mappers[i].isChecked;
                toAdd.push($scope.mappers[i]);
            }
        }
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/oauth-clients-by-id/' + oauth.id + '/protocol-mappers/add-models',
            toAdd).success(function() {
                Notifications.success("Mappers added");
                $location.url('/realms/' + realm.realm + '/oauth-clients/' + oauth.id +  '/mappers');
            }).error(function() {
                Notifications.error("Error adding mappers");
                $location.url('/realms/' + realm.realm + '/oauth-clients/' + oauth.id +  '/mappers');
            });
    };

});

module.controller('OAuthClientProtocolMapperCtrl', function($scope, realm, serverInfo, oauth, mapper, OAuthClientProtocolMapper, Notifications, Dialog, $location) {
    if (oauth.protocol == null) {
        oauth.protocol = 'openid-connect';
    }
    $scope.realm = realm;
    $scope.oauth = oauth;
    $scope.create = false;
    var protocol = oauth.protocol;
    $scope.protocol = oauth.protocol;
    $scope.mapper = angular.copy(mapper);
    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    var protocolMappers = serverInfo.protocolMapperTypes[protocol];
    for (var i = 0; i < protocolMappers.length; i++) {
        if (protocolMappers[i].id == mapper.protocolMapper) {
            $scope.mapperType = protocolMappers[i];
        }
    }
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
        OAuthClientProtocolMapper.update({
            realm : realm.realm,
            oauth: oauth.id,
            id : mapper.id
        }, $scope.mapper, function() {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/oauth-clients/' + oauth.id + "/mappers/" + mapper.id);
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
            OAuthClientProtocolMapper.remove({ realm: realm.realm, oauth: oauth.id, id : $scope.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/oauth-clients/' + oauth.id + "/mappers");
            });
        });
    };

});

module.controller('OAuthClientProtocolMapperCreateCtrl', function($scope, realm, serverInfo, oauth, OAuthClientProtocolMapper, Notifications, Dialog, $location) {
    if (oauth.protocol == null) {
        oauth.protocol = 'openid-connect';
    }
    $scope.realm = realm;
    $scope.oauth = oauth;
    $scope.create = true;
    var protocol = oauth.protocol;
    $scope.protocol = protocol;
    $scope.mapper = { protocol :  oauth.protocol, config: {}};
    $scope.mapperTypes = serverInfo.protocolMapperTypes[protocol];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.mapper.protocolMapper = $scope.mapperType.id;
        OAuthClientProtocolMapper.save({
            realm : realm.realm, oauth: oauth.id
        }, $scope.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/oauth-clients/' + oauth.id + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});


