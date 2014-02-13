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
    if (!$scope.create) {
        $scope.oauth= angular.copy(oauth);
    } else {
        $scope.oauth = {};
        $scope.oauth.webOrigins = [];
        $scope.oauth.redirectUris = [];
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
        if ($scope.create) {
            OAuthClient.save({
                realm: realm.realm
            }, $scope.oauth, function (data, headers) {
                $scope.changed = false;
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/oauth-clients/" + id);
                Notifications.success("The oauth client has been created.");
            });
        } else {
            OAuthClient.update({
                realm : realm.realm,
                id : oauth.id
            }, $scope.oauth, function() {
                $scope.changed = false;
                oauth = angular.copy($scope.oauth);
                Notifications.success("Your changes have been saved to the oauth client.");
            });
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
        Dialog.confirmDelete($scope.oauth.name, 'oauth', function() {
            $scope.oauth.$remove({
                realm : realm.realm,
                id : $scope.oauth.id
            }, function() {
                $location.url("/realms/" + realm.realm + "/oauth-clients");
                Notifications.success("The oauth client has been deleted.");
            });
        });
    };


});

module.controller('OAuthClientScopeMappingCtrl', function($scope, $http, realm, oauth, roles, applications, OAuthClientRealmScopeMapping, OAuthClientApplicationScopeMapping, ApplicationRole) {
    $scope.realm = realm;
    $scope.oauth = oauth;
    $scope.realmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.applications = applications;
    $scope.applicationRoles = [];
    $scope.selectedApplicationRoles = [];
    $scope.selectedApplicationMappings = [];
    $scope.applicationMappings = [];



    $scope.realmMappings = OAuthClientRealmScopeMapping.query({realm : realm.realm, oauth : oauth.id}, function(){
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
        $http.post('/auth/rest/admin/realms/' + realm.realm + '/oauth-clients/' + oauth.id + '/scope-mappings/realm',
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
        $http.delete('/auth/rest/admin/realms/' + realm.realm + '/oauth-clients/' + oauth.id +  '/scope-mappings/realm',
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
        $http.post('/auth/rest/admin/realms/' + realm.realm + '/oauth-clients/' + oauth.id +  '/scope-mappings/applications/' + $scope.targetApp.name,
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
        $http.delete('/auth/rest/admin/realms/' + realm.realm + '/oauth-clients/' + oauth.id +  '/scope-mappings/applications/' + $scope.targetApp.name,
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
                    $scope.applicationMappings = OAuthClientApplicationScopeMapping.query({realm : realm.realm, oauth : oauth.id, targetApp : $scope.targetApp.name}, function(){
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
            $scope.targetApp = null;
        }
    };



});


module.controller('OAuthClientInstallationCtrl', function($scope, realm, installation, oauth, OAuthClientInstallation, $routeParams) {
    $scope.realm = realm;
    $scope.oauth = oauth;
    $scope.installation = installation;
    $scope.download = OAuthClientInstallation.url({ realm: $routeParams.realm, oauth: $routeParams.oauth });
});
