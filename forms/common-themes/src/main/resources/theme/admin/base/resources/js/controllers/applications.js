module.controller('ApplicationRoleListCtrl', function($scope, $location, realm, application, roles) {
    $scope.realm = realm;
    $scope.roles = roles;
    $scope.application = application;

    for (var i = 0; i < roles.length; i++) {
        console.log("role.id: " + roles[i].id + " role.name: " + roles[i].name);
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationCredentialsCtrl', function($scope, $location, realm, application, ApplicationCredentials, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    var secret = ApplicationCredentials.get({ realm : realm.realm, application : application.id },
        function() {
            $scope.secret = secret.value;
        }
    );

    $scope.changePassword = function() {
        var secret = ApplicationCredentials.update({ realm : realm.realm, application : application.id },
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

module.controller('ApplicationCertificateCtrl', function($scope, $location, $http, realm, application,
                                                         ApplicationCertificate, ApplicationCertificateGenerate,
                                                         ApplicationCertificateDownload, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    var jks = {
        keyAlias: application.name,
        realmAlias: realm.realm
    };

    $scope.jks = jks;

    var keyInfo = ApplicationCertificate.get({ realm : realm.realm, application : application.id },
        function() {
            $scope.keyInfo = keyInfo;
        }
    );

    $scope.generate = function() {
        var keyInfo = ApplicationCertificateGenerate.generate({ realm : realm.realm, application : application.id },
            function() {
                Notifications.success('Client keypair and cert has been changed.');
                $scope.keyInfo = keyInfo;
            },
            function() {
                Notifications.error("Client keypair and cert was not changed due to a problem.");
            }
        );
    };

    $scope.downloadJKS = function() {
        $http({
            url: authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id + '/certificates/download',
            method: 'POST',
            responseType: 'arraybuffer',
            data: $scope.jks,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/octet-stream'
            }
        }).success(function(data){
            var blob = new Blob([data], {
                type: 'application/octet-stream'
            });
            saveAs(blob, 'keystore' + '.jks');
        }).error(function(){
            Notifications.error("Error downloading.");
        });
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationSessionsCtrl', function($scope, realm, sessionCount, application,
                                                      ApplicationUserSessions) {
    $scope.realm = realm;
    $scope.count = sessionCount.count;
    $scope.sessions = [];
    $scope.application = application;

    $scope.page = 0;

    $scope.query = {
        realm : realm.realm,
        application: $scope.application.id,
        max : 5,
        first : 0
    }

    $scope.firstPage = function() {
        $scope.query.first = 0;
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.loadUsers();
    }

    $scope.previousPage = function() {
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.loadUsers();
    }

    $scope.nextPage = function() {
        $scope.query.first += parseInt($scope.query.max);
        $scope.loadUsers();
    }

    $scope.toDate = function(val) {
        return new Date(val);
    };

    $scope.loadUsers = function() {
        ApplicationUserSessions.query($scope.query, function(updated) {
            $scope.sessions = updated;
        })
    };
});

module.controller('ApplicationClaimsCtrl', function($scope, realm, application, claims,
                                                        ApplicationClaims,
                                                        $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.claims = angular.copy(claims);

    $scope.changed = false;

    $scope.$watch('claims', function () {
        if (!angular.equals($scope.claims, claims)) {
            $scope.changed = true;
        }
    }, true);


    $scope.save = function () {
        ApplicationClaims.update({
            realm: realm.realm,
            application: application.id
        }, $scope.claims, function () {
            $scope.changed = false;
            claims = angular.copy($scope.claims);

            Notifications.success("Your claim changes have been saved.");
        });
    };

    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/claims");
    };

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
                application : application.id
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/roles/" + id);
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
                application : application.id,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/roles");
    };


    roleControl($scope, realm, role, roles, applications,
        ApplicationRole, RoleById, RoleRealmComposites, RoleApplicationComposites,
        $http, $location, Notifications, Dialog);

});

module.controller('ApplicationListCtrl', function($scope, realm, applications, Application, $location) {
    $scope.realm = realm;
    $scope.applications = applications;
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationInstallationCtrl', function($scope, realm, application, ApplicationInstallation,ApplicationInstallationJBoss, $http, $routeParams) {
    console.log('ApplicationInstallationCtrl');
    $scope.realm = realm;
    $scope.application = application;
    $scope.installation = null;
    $scope.download = null;
    $scope.configFormat = null;

    $scope.configFormats = [
        "keycloak.json",
        "Wildfly/JBoss Subsystem XML"
    ];

    $scope.changeFormat = function() {
        if ($scope.configFormat == "keycloak.json") {
            var url = ApplicationInstallation.url({ realm: $routeParams.realm, application: $routeParams.application });
            $http.get(url).success(function(data) {
                var tmp = angular.fromJson(data);
                $scope.installation = angular.toJson(tmp, true);
                $scope.type = 'application/json';
            })
        } else if ($scope.configFormat == "Wildfly/JBoss Subsystem XML") {
            var url = ApplicationInstallationJBoss.url({ realm: $routeParams.realm, application: $routeParams.application });
            $http.get(url).success(function(data) {
                $scope.installation = data;
                $scope.type = 'text/xml';
            })
        }
    };

    $scope.download = function() {
        saveAs(new Blob([$scope.installation], { type: $scope.type }), 'keycloak.json');
    }
});

module.controller('ApplicationDetailCtrl', function($scope, realm, application, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.accessTypes = [
        "confidential",
        "public",
        "bearer-only"
    ];

    $scope.protocols = [
        "openid-connect",
        "saml"
    ];

    $scope.realm = realm;
    $scope.create = !application.name;
    $scope.samlServerSignature = false;
    $scope.samlClientSignature = false;
    $scope.samlEncrypt = false;
    if (!$scope.create) {
        if (!application.attributes) {
            application.attributes = {};
        }
        $scope.application= angular.copy(application);
        $scope.accessType = $scope.accessTypes[0];
        if (application.bearerOnly) {
            $scope.accessType = $scope.accessTypes[2];
        } else if (application.publicClient) {
            $scope.accessType = $scope.accessTypes[1];
        }
        if (application.protocol == 'openid-connect') {
            $scope.protocol = $scope.protocols[0];
        } else if (application.protocol == 'saml') {
            $scope.protocol = $scope.protocols[1];
        } else { // protocol could be null due to older keycloak installs
            $scope.protocol = $scope.protocols[0];
        }
    } else {
        $scope.application = { enabled: true, attributes: {}};
        $scope.application.redirectUris = [];
        $scope.accessType = $scope.accessTypes[0];
        $scope.protocol = $scope.protocols[0];
    }

    if ($scope.application.attributes["samlServerSignature"]) {
        if ($scope.application.attributes["samlServerSignature"] == "true") {
            $scope.samlServerSignature = true;
        }
    }
    if ($scope.application.attributes["samlClientSignature"]) {
        if ($scope.application.attributes["samlClientSignature"] == "true") {
            $scope.samlClientSignature = true;
        }
    }
    if ($scope.application.attributes["samlEncrypt"]) {
        if ($scope.application.attributes["samlEncrypt"] == "true") {
            $scope.samlEncrypt = true;
        }
    }

    $scope.switchChange = function() {
        $scope.changed = true;
    }

    $scope.changeAccessType = function() {
        if ($scope.accessType == "confidential") {
            $scope.application.bearerOnly = false;
            $scope.application.publicClient = false;
        } else if ($scope.accessType == "public") {
            $scope.application.bearerOnly = false;
            $scope.application.publicClient = true;
        } else if ($scope.accessType == "bearer-only") {
            $scope.application.bearerOnly = true;
            $scope.application.publicClient = false;
        }
    };

    $scope.changeProtocol = function() {
        if ($scope.protocol == "openid-connect") {
            $scope.application.protocol = "openid-connect";
        } else if ($scope.accessType == "saml") {
            $scope.application.protocol = "saml";
        }
    };

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
        if ($scope.samlServerSignature == true) {
            $scope.application.attributes["samlServerSignature"] = "true";
        } else {
            $scope.application.attributes["samlServerSignature"] = "false";

        }
        if ($scope.samlClientSignature == true) {
            $scope.application.attributes["samlClientSignature"] = "true";
        } else {
            $scope.application.attributes["samlClientSignature"] = "false";

        }
        if ($scope.samlEncrypt == true) {
            $scope.application.attributes["samlEncrypt"] = "true";
        } else {
            $scope.application.attributes["samlEncrypt"] = "false";

        }

        $scope.application.protocol = $scope.protocol;

        if (!$scope.application.bearerOnly && (!$scope.application.redirectUris || $scope.application.redirectUris.length == 0)) {
            Notifications.error("You must specify at least one redirect uri");
        } else {
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
                    application : application.id
                }, $scope.application, function() {
                    $scope.changed = false;
                    application = angular.copy($scope.application);
                    $location.url("/realms/" + realm.realm + "/applications/" + application.id);
                    Notifications.success("Your changes have been saved to the application.");
                });
            }
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
                application : $scope.application.id
            }, function() {
                $location.url("/realms/" + realm.realm + "/applications");
                Notifications.success("The application has been deleted.");
            });
        });
    };


});

module.controller('ApplicationScopeMappingCtrl', function($scope, $http, realm, application, applications, Notifications,
                                                          Application,
                                                          ApplicationRealmScopeMapping, ApplicationApplicationScopeMapping, ApplicationRole,
                                                          ApplicationAvailableRealmScopeMapping, ApplicationAvailableApplicationScopeMapping,
                                                          ApplicationCompositeRealmScopeMapping, ApplicationCompositeApplicationScopeMapping) {
    $scope.realm = realm;
    $scope.application = angular.copy(application);
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
        Application.update({
            realm : realm.realm,
            application : application.id
        }, $scope.application, function() {
            $scope.changed = false;
            application = angular.copy($scope.application);
            updateRealmRoles();
            Notifications.success("Scope mappings updated.");
        });
    }



    function updateRealmRoles() {
        $scope.realmRoles = ApplicationAvailableRealmScopeMapping.query({realm : realm.realm, application : application.id});
        $scope.realmMappings = ApplicationRealmScopeMapping.query({realm : realm.realm, application : application.id});
        $scope.realmComposite = ApplicationCompositeRealmScopeMapping.query({realm : realm.realm, application : application.id});
    }

    function updateAppRoles() {
        if ($scope.targetApp) {
            console.debug($scope.targetApp.name);
            $scope.applicationRoles = ApplicationAvailableApplicationScopeMapping.query({realm : realm.realm, application : application.id, targetApp : $scope.targetApp.id});
            $scope.applicationMappings = ApplicationApplicationScopeMapping.query({realm : realm.realm, application : application.id, targetApp : $scope.targetApp.id});
            $scope.applicationComposite = ApplicationCompositeApplicationScopeMapping.query({realm : realm.realm, application : application.id, targetApp : $scope.targetApp.id});
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
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id + '/scope-mappings/realm',
                $scope.selectedRealmRoles).success(function() {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id +  '/scope-mappings/realm',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function () {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.addApplicationRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id +  '/scope-mappings/applications-by-id/' + $scope.targetApp.id,
                $scope.selectedApplicationRoles).success(function () {
                updateAppRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteApplicationRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id +  '/scope-mappings/applications-by-id/' + $scope.targetApp.id,
            {data : $scope.selectedApplicationMappings, headers : {"content-type" : "application/json"}}).success(function () {
                updateAppRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    updateRealmRoles();
});

module.controller('ApplicationRevocationCtrl', function($scope, realm, application, Application, ApplicationPushRevocation, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;

    var setNotBefore = function() {
        if ($scope.application.notBefore == 0) {
            $scope.notBefore = "None";
        } else {
            $scope.notBefore = new Date($scope.application.notBefore * 1000);
        }
    };

    setNotBefore();

    var refresh = function() {
        Application.get({ realm : realm.realm, application: $scope.application.id }, function(updated) {
            $scope.application = updated;
            setNotBefore();
        })

    };

    $scope.clear = function() {
        $scope.application.notBefore = 0;
        Application.update({ realm : realm.realm, application: application.id}, $scope.application, function () {
            $scope.notBefore = "None";
            Notifications.success('Not Before cleared for application.');
            refresh();
        });
    }
    $scope.setNotBeforeNow = function() {
        $scope.application.notBefore = new Date().getTime()/1000;
        Application.update({ realm : realm.realm, application: $scope.application.id}, $scope.application, function () {
            Notifications.success('Not Before cleared for application.');
            refresh();
        });
    }
    $scope.pushRevocation = function() {
        ApplicationPushRevocation.save({realm : realm.realm, application: $scope.application.id}, function () {
            Notifications.success('Push sent for application.');
        });
    }

});

