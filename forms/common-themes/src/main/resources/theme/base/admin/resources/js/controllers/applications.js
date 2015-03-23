Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

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

module.controller('ApplicationIdentityProviderCtrl', function($scope, $location, $route, realm, application, Application, $location, Notifications) {
    $scope.realm = realm;
    $scope.application = angular.copy(application);
    var length = 0;

    if ($scope.application.identityProviders) {
        length = $scope.application.identityProviders.length;

        for (i = 0; i < $scope.application.identityProviders.length; i++) {
            var applicationProvider = $scope.application.identityProviders[i];
            if (applicationProvider.retrieveToken) {
                applicationProvider.retrieveToken = applicationProvider.retrieveToken.toString();
            }
        }

    } else {
        $scope.application.identityProviders = [];
    }

    $scope.identityProviders = [];
    var providersMissingInApp = [];

    for (j = 0; j < realm.identityProviders.length; j++) {
        var identityProvider = realm.identityProviders[j];
        var applicationProvider = null;

        for (i = 0; i < $scope.application.identityProviders.length; i++) {
            applicationProvider = $scope.application.identityProviders[i];

            if (applicationProvider) {

                if (applicationProvider.id == identityProvider.id) {
                    $scope.identityProviders[i] = {};
                    $scope.identityProviders[i].identityProvider = identityProvider;
                    $scope.identityProviders[i].retrieveToken = applicationProvider.retrieveToken;
                    break;
                }

                applicationProvider = null;
            }
        }

        if (applicationProvider == null) {
            providersMissingInApp.push(identityProvider);
        }
    }

    for (j = 0; j < providersMissingInApp.length; j++) {
        var identityProvider = providersMissingInApp[j];

        var currentProvider = {};
        currentProvider.identityProvider = identityProvider;
        currentProvider.retrieveToken = "false";
        $scope.identityProviders.push(currentProvider);

        var currentAppProvider = {};
        currentAppProvider.id = identityProvider.id;
        currentAppProvider.retrieveToken = "false";
        $scope.application.identityProviders.push(currentAppProvider);
    }

    var oldCopy = angular.copy($scope.application);

    $scope.save = function() {

        Application.update({
            realm : realm.realm,
            application : application.id
        }, $scope.application, function() {
            $scope.changed = false;
            $route.reload();
            Notifications.success("Your changes have been saved to the application.");
        });
    };

    $scope.reset = function() {
        $scope.application = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.$watch('application', function() {
        if (!angular.equals($scope.application, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
});

module.controller('ApplicationSamlKeyCtrl', function($scope, $location, $http, $upload, realm, application,
                                                         ApplicationCertificate, ApplicationCertificateGenerate,
                                                         ApplicationCertificateDownload, Notifications) {
    $scope.realm = realm;
    $scope.application = application;

    var signingKeyInfo = ApplicationCertificate.get({ realm : realm.realm, application : application.id, attribute: 'saml.signing' },
        function() {
            $scope.signingKeyInfo = signingKeyInfo;
        }
    );

    $scope.generateSigningKey = function() {
        var keyInfo = ApplicationCertificateGenerate.generate({ realm : realm.realm, application : application.id, attribute: 'saml.signing' },
            function() {
                Notifications.success('Signing key has been regenerated.');
                $scope.signingKeyInfo = keyInfo;
            },
            function() {
                Notifications.error("Signing key was not regenerated.");
            }
        );
    };

    $scope.importSigningKey = function() {
        $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/saml/Signing/import/saml.signing");
    };

    $scope.exportSigningKey = function() {
        $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/saml/Signing/export/saml.signing");
    };

    var encryptionKeyInfo = ApplicationCertificate.get({ realm : realm.realm, application : application.id, attribute: 'saml.encryption' },
        function() {
            $scope.encryptionKeyInfo = encryptionKeyInfo;
        }
    );

    $scope.generateEncryptionKey = function() {
        var keyInfo = ApplicationCertificateGenerate.generate({ realm : realm.realm, application : application.id, attribute: 'saml.encryption' },
            function() {
                Notifications.success('Encryption key has been regenerated.');
                $scope.encryptionKeyInfo = keyInfo;
            },
            function() {
                Notifications.error("Encryption key was not regenerated.");
            }
        );
    };

    $scope.importEncryptionKey = function() {
        $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/saml/Encryption/import/saml.encryption");
    };

    $scope.exportEncryptionKey = function() {
        $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/saml/Encryption/export/saml.encryption");
    };


    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationCertificateImportCtrl', function($scope, $location, $http, $upload, realm, application, $routeParams,
                                                         ApplicationCertificate, ApplicationCertificateGenerate,
                                                         ApplicationCertificateDownload, Notifications) {

    var keyType = $routeParams.keyType;
    var attribute = $routeParams.attribute;
    $scope.realm = realm;
    $scope.application = application;
    $scope.keyType = keyType;

    $scope.files = [];

    $scope.onFileSelect = function($files) {
        $scope.files = $files;
    };

    $scope.clearFileSelect = function() {
        $scope.files = null;
    }

    $scope.keyFormats = [
        "JKS",
        "PKCS12"
    ];

    $scope.uploadKeyFormat = $scope.keyFormats[0];

    $scope.uploadFile = function() {
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $scope.files.length; i++) {
            var $file = $scope.files[i];
            $scope.upload = $upload.upload({
                url: authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id + '/certificates/' + attribute + '/upload',
                // method: POST or PUT,
                // headers: {'headerKey': 'headerValue'}, withCredential: true,
                data: {keystoreFormat: $scope.uploadKeyFormat,
                    keyAlias: $scope.uploadKeyAlias,
                    keyPassword: $scope.uploadKeyPassword,
                    storePassword: $scope.uploadStorePassword
                },
                file: $file
                /* set file formData name for 'Content-Desposition' header. Default: 'file' */
                //fileFormDataName: myFile,
                /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
                //formDataAppender: function(formData, key, val){}
            }).progress(function(evt) {
                console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
            }).success(function(data, status, headers) {
                Notifications.success("Keystore uploaded successfully.");
                $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/saml/keys");
            })
                .error(function() {
                    Notifications.error("The key store can not be uploaded. Please verify the file.");

                });
            //.then(success, error, progress);
        }
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationCertificateExportCtrl', function($scope, $location, $http, $upload, realm, application, $routeParams,
                                                         ApplicationCertificate, ApplicationCertificateGenerate,
                                                         ApplicationCertificateDownload, Notifications) {
    var keyType = $routeParams.keyType;
    var attribute = $routeParams.attribute;
    $scope.realm = realm;
    $scope.application = application;
    $scope.keyType = keyType;
    var jks = {
        keyAlias: application.name,
        realmAlias: realm.realm
    };

    $scope.keyFormats = [
        "JKS",
        "PKCS12"
    ];

    var keyInfo = ApplicationCertificate.get({ realm : realm.realm, application : application.id, attribute: attribute },
        function() {
            $scope.keyInfo = keyInfo;
        }
    );
    $scope.jks = jks;
    $scope.jks.format = $scope.keyFormats[0];

    $scope.download = function() {
        $http({
            url: authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id + '/certificates/' + attribute + '/download',
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
            var ext = ".jks";
            if ($scope.jks.format == 'PKCS12') ext = ".p12";
            saveAs(blob, 'keystore' + ext);
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

                ApplicationRole.get({ realm: realm.realm, application : application.id, role: role.name }, function(role) {
                    var id = role.id;
                    $location.url("/realms/" + realm.realm + "/applications/" + application.id + "/roles/" + id);
                    Notifications.success("The role has been created.");
                });
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
                role : $scope.role.id
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

module.controller('ApplicationImportCtrl', function($scope, $location, $upload, realm, serverInfo, Notifications) {

    $scope.realm = realm;
    $scope.configFormats = serverInfo.applicationImporters;
    $scope.configFormat = null;

    $scope.files = [];

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
                url: authUrl + '/admin/realms/' + realm.realm + '/application-importers/' + $scope.configFormat.id + '/upload',
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
                Notifications.success("Uploaded successfully.");
                $location.url("/realms/" + realm.realm + "/applications");
            })
                .error(function() {
                    Notifications.error("The file can not be uploaded. Please verify the file.");

                });
            //.then(success, error, progress);
        }
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});


module.controller('ApplicationListCtrl', function($scope, realm, applications, Application, serverInfo, $location) {
    $scope.realm = realm;
    $scope.applications = applications;
    $scope.importButton = serverInfo.applicationImporters.length > 0;
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

module.controller('ApplicationDetailCtrl', function($scope, realm, application, serverInfo, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.accessTypes = [
        "confidential",
        "public",
        "bearer-only"
    ];

    $scope.protocols = serverInfo.protocols;

    $scope.signatureAlgorithms = [
        "RSA_SHA1",
        "RSA_SHA256",
        "RSA_SHA512",
        "DSA_SHA1"
    ];
    $scope.nameIdFormats = [
        "username",
        "email",
        "transient",
        "persistent"
    ];

    $scope.realm = realm;
    $scope.create = !application.name;
    $scope.samlAuthnStatement = false;
    $scope.samlMultiValuedRoles = false;
    $scope.samlServerSignature = false;
    $scope.samlAssertionSignature = false;
    $scope.samlClientSignature = false;
    $scope.samlEncrypt = false;
    $scope.samlForcePostBinding = false;
    $scope.samlForceNameIdFormat = false;
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
        if (application.protocol) {
            $scope.protocol = $scope.protocols[$scope.protocols.indexOf(application.protocol)];
        } else {
            $scope.protocol = $scope.protocols[0];
        }
        if (application.attributes['saml.signature.algorithm'] == 'RSA_SHA1') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[0];
        } else if (application.attributes['saml.signature.algorithm'] == 'RSA_SHA256') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[1];
        } else if (application.attributes['saml.signature.algorithm'] == 'RSA_SHA512') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[2];
        } else if (application.attributes['saml.signature.algorithm'] == 'DSA_SHA1') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[3];
        }
        if (application.attributes['saml_name_id_format'] == 'unspecified') {
            $scope.nameIdFormat = $scope.nameIdFormats[0];
        } else if (application.attributes['saml_name_id_format'] == 'email') {
            $scope.nameIdFormat = $scope.nameIdFormats[1];
        } else if (application.attributes['saml_name_id_format'] == 'transient') {
            $scope.nameIdFormat = $scope.nameIdFormats[2];
        } else if (application.attributes['saml_name_id_format'] == 'persistent') {
            $scope.nameIdFormat = $scope.nameIdFormats[3];
        }

    } else {
        $scope.application = { enabled: true, attributes: {}};
        $scope.application.redirectUris = [];
        $scope.accessType = $scope.accessTypes[0];
        $scope.protocol = $scope.protocols[0];
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[1];
        $scope.nameIdFormat = $scope.nameIdFormats[0];
        $scope.samlAuthnStatement = true;
        $scope.samlForceNameIdFormat = false;
    }

    if ($scope.application.attributes["saml.server.signature"]) {
        if ($scope.application.attributes["saml.server.signature"] == "true") {
            $scope.samlServerSignature = true;
        } else {
            $scope.samlServerSignature = false;

        }
    }
    if ($scope.application.attributes["saml.assertion.signature"]) {
        if ($scope.application.attributes["saml.assertion.signature"] == "true") {
            $scope.samlAssertionSignature = true;
        } else {
            $scope.samlAssertionSignature = false;
        }
    }
    if ($scope.application.attributes["saml.client.signature"]) {
        if ($scope.application.attributes["saml.client.signature"] == "true") {
            $scope.samlClientSignature = true;
        } else {
            $scope.samlClientSignature = false;
        }
    }
    if ($scope.application.attributes["saml.encrypt"]) {
        if ($scope.application.attributes["saml.encrypt"] == "true") {
            $scope.samlEncrypt = true;
        } else {
            $scope.samlEncrypt = false;
        }
    }
    if ($scope.application.attributes["saml.authnstatement"]) {
        if ($scope.application.attributes["saml.authnstatement"] == "true") {
            $scope.samlAuthnStatement = true;
        } else {
            $scope.samlAuthnStatement = false;
        }
    }
    if ($scope.application.attributes["saml_force_name_id_format"]) {
        if ($scope.application.attributes["saml_force_name_id_format"] == "true") {
            $scope.samlForceNameIdFormat = true;
        } else {
            $scope.samlForceNameIdFormat = false;
        }
    }
    if ($scope.application.attributes["saml.multivalued.roles"]) {
        if ($scope.application.attributes["saml.multivalued.roles"] == "true") {
            $scope.samlMultiValuedRoles = true;
        } else {
            $scope.samlMultiValuedRoles = false;
        }
    }
    if ($scope.application.attributes["saml.force.post.binding"]) {
        if ($scope.application.attributes["saml.force.post.binding"] == "true") {
            $scope.samlForcePostBinding = true;
        } else {
            $scope.samlForcePostBinding = false;
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

    $scope.changeAlgorithm = function() {
        $scope.application.attributes['saml.signature.algorithm'] = $scope.signatureAlgorithm;
    };

    $scope.changeNameIdFormat = function() {
        $scope.application.attributes['saml_name_id_format'] = $scope.nameIdFormat;
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
            $scope.application.attributes["saml.server.signature"] = "true";
        } else {
            $scope.application.attributes["saml.server.signature"] = "false";

        }
        if ($scope.samlAssertionSignature == true) {
            $scope.application.attributes["saml.assertion.signature"] = "true";
        } else {
            $scope.application.attributes["saml.assertion.signature"] = "false";
        }
        if ($scope.samlClientSignature == true) {
            $scope.application.attributes["saml.client.signature"] = "true";
        } else {
            $scope.application.attributes["saml.client.signature"] = "false";

        }
        if ($scope.samlEncrypt == true) {
            $scope.application.attributes["saml.encrypt"] = "true";
        } else {
            $scope.application.attributes["saml.encrypt"] = "false";

        }
        if ($scope.samlAuthnStatement == true) {
            $scope.application.attributes["saml.authnstatement"] = "true";
        } else {
            $scope.application.attributes["saml.authnstatement"] = "false";

        }
        if ($scope.samlForceNameIdFormat == true) {
            $scope.application.attributes["saml_force_name_id_format"] = "true";
        } else {
            $scope.application.attributes["saml_force_name_id_format"] = "false";

        }
        if ($scope.samlMultiValuedRoles == true) {
            $scope.application.attributes["saml.multivalued.roles"] = "true";
        } else {
            $scope.application.attributes["saml.multivalued.roles"] = "false";

        }
        if ($scope.samlForcePostBinding == true) {
            $scope.application.attributes["saml.force.post.binding"] = "true";
        } else {
            $scope.application.attributes["saml.force.post.binding"] = "false";

        }

        $scope.application.protocol = $scope.protocol;
        $scope.application.attributes['saml.signature.algorithm'] = $scope.signatureAlgorithm;
        $scope.application.attributes['saml_name_id_format'] = $scope.nameIdFormat;

        if ($scope.application.protocol != 'saml' && !$scope.application.bearerOnly && (!$scope.application.redirectUris || $scope.application.redirectUris.length == 0)) {
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
            Notifications.success('Not Before set for application.');
            refresh();
        });
    }
    $scope.pushRevocation = function() {
        ApplicationPushRevocation.save({realm : realm.realm, application: $scope.application.id}, function (globalReqResult) {
            var successCount = globalReqResult.successRequests ? globalReqResult.successRequests.length : 0;
            var failedCount  = globalReqResult.failedRequests ? globalReqResult.failedRequests.length : 0;

            if (successCount==0 && failedCount==0) {
                Notifications.warn('No push sent. No admin URI configured or no registered cluster nodes available');
            } else if (failedCount > 0) {
                var msgStart = successCount>0 ? 'Successfully push notBefore to: ' + globalReqResult.successRequests + ' . ' : '';
                Notifications.error(msgStart + 'Failed to push notBefore to: ' + globalReqResult.failedRequests + '. Verify availability of failed hosts and try again');
            } else {
                Notifications.success('Successfully push notBefore to: ' + globalReqResult.successRequests);
            }
        });
    }

});

module.controller('ApplicationClusteringCtrl', function($scope, application, Application, ApplicationTestNodesAvailable, realm, $location, $route, Notifications, TimeUnit) {
    $scope.application = application;
    $scope.realm = realm;

    var oldCopy = angular.copy($scope.application);
    $scope.changed = false;

    $scope.$watch('application', function() {
        if (!angular.equals($scope.application, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.application.nodeReRegistrationTimeoutUnit = TimeUnit.autoUnit(application.nodeReRegistrationTimeout);
    $scope.application.nodeReRegistrationTimeout = TimeUnit.toUnit(application.nodeReRegistrationTimeout, $scope.application.nodeReRegistrationTimeoutUnit);
    $scope.$watch('application.nodeReRegistrationTimeoutUnit', function(to, from) {
        $scope.application.nodeReRegistrationTimeout = TimeUnit.convert($scope.application.nodeReRegistrationTimeout, from, to);
    });

    $scope.save = function() {
        var appCopy = angular.copy($scope.application);
        delete appCopy['nodeReRegistrationTimeoutUnit'];
        appCopy.nodeReRegistrationTimeout = TimeUnit.toSeconds($scope.application.nodeReRegistrationTimeout, $scope.application.nodeReRegistrationTimeoutUnit)
        Application.update({ realm : realm.realm, application : application.id }, appCopy, function () {
            $route.reload();
            Notifications.success('Your changes have been saved to the application.');
        });
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.testNodesAvailable = function() {
        console.log('testNodesAvailable');
        ApplicationTestNodesAvailable.get({ realm : realm.realm, application : application.id }, function(globalReqResult) {
            $route.reload();

            var successCount = globalReqResult.successRequests ? globalReqResult.successRequests.length : 0;
            var failedCount  = globalReqResult.failedRequests ? globalReqResult.failedRequests.length : 0;

            if (successCount==0 && failedCount==0) {
                Notifications.warn('No requests sent. No admin URI configured or no registered cluster nodes available');
            } else if (failedCount > 0) {
                var msgStart = successCount>0 ? 'Successfully verify availability for ' + globalReqResult.successRequests + ' . ' : '';
                Notifications.error(msgStart + 'Failed to verify availability for: ' + globalReqResult.failedRequests + '. Fix or unregister failed cluster nodes and try again');
            } else {
                Notifications.success('Successfully sent requests to: ' + globalReqResult.successRequests);
            }
        });
    };

    if (application.registeredNodes) {
        var nodeRegistrations = [];
        for (node in application.registeredNodes) {
            reg = {
                host: node,
                lastRegistration: new Date(application.registeredNodes[node] * 1000)
            }
            nodeRegistrations.push(reg);
        }

        $scope.nodeRegistrations = nodeRegistrations;
    };
});

module.controller('ApplicationClusteringNodeCtrl', function($scope, application, Application, ApplicationClusterNode, realm, $location, $routeParams, Notifications) {
    $scope.application = application;
    $scope.realm = realm;
    $scope.create = !$routeParams.node;

    $scope.save = function() {
        console.log('registerNode: ' + $scope.node.host);
        ApplicationClusterNode.save({ realm : realm.realm, application : application.id , node: $scope.node.host }, function() {
            Notifications.success('Node ' + $scope.node.host + ' registered successfully.');
            $location.url('/realms/' + realm.realm + '/applications/' + application.id +  '/clustering');
        });
    }

    $scope.unregisterNode = function() {
        console.log('unregisterNode: ' + $scope.node.host);
        ApplicationClusterNode.remove({ realm : realm.realm, application : application.id , node: $scope.node.host }, function() {
            Notifications.success('Node ' + $scope.node.host + ' unregistered successfully.');
            $location.url('/realms/' + realm.realm + '/applications/' + application.id +  '/clustering');
        });
    }

    if ($scope.create) {
        $scope.node = {}
        $scope.registered = false;
    } else {
        var lastRegTime = application.registeredNodes[$routeParams.node];

        if (lastRegTime) {
            $scope.registered = true;
            $scope.node = {
                host: $routeParams.node,
                lastRegistration: new Date(lastRegTime * 1000)
            }

        } else {
            $scope.registered = false;
            $scope.node = {
                host: $routeParams.node
            }
        }
    }
});

module.controller('ApplicationProtocolMapperListCtrl', function($scope, realm, application, serverInfo,
                                                    ApplicationProtocolMappersByProtocol,
                                                    $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    if (application.protocol == null) {
        application.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[application.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;


    var updateMappers = function() {
        $scope.mappers = ApplicationProtocolMappersByProtocol.query({realm : realm.realm, application : application.id, protocol : application.protocol});
    };

    updateMappers();
});

module.controller('AddBuiltinProtocolMapperCtrl', function($scope, realm, application, serverInfo,
                                                            ApplicationProtocolMappersByProtocol,
                                                            $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    if (application.protocol == null) {
        application.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[application.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;




    var updateMappers = function() {
        var appMappers = ApplicationProtocolMappersByProtocol.query({realm : realm.realm, application : application.id, protocol : application.protocol}, function() {
            var builtinMappers = serverInfo.builtinProtocolMappers[application.protocol];
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
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/applications-by-id/' + application.id + '/protocol-mappers/add-models',
                   toAdd).success(function() {
                Notifications.success("Mappers added");
                $location.url('/realms/' + realm.realm + '/applications/' + application.id +  '/mappers');
            }).error(function() {
                Notifications.error("Error adding mappers");
                $location.url('/realms/' + realm.realm + '/applications/' + application.id +  '/mappers');
            });
    };

});

module.controller('ApplicationProtocolMapperCtrl', function($scope, realm, serverInfo, application, mapper, ApplicationProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.create = false;
    if (application.protocol == null) {
        application.protocol = 'openid-connect';
    }
    $scope.protocol = application.protocol;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    $scope.boolval = true;
    $scope.boolvalId = 'boolval';

    var protocolMappers = serverInfo.protocolMapperTypes[application.protocol];
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
        ApplicationProtocolMapper.update({
            realm : realm.realm,
            application: application.id,
            id : mapper.id
        }, $scope.mapper, function() {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/applications/' + application.id + "/mappers/" + mapper.id);
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
            ApplicationProtocolMapper.remove({ realm: realm.realm, application: application.id, id : $scope.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/applications/' + application.id + "/mappers");
            });
        });
    };

});

module.controller('ApplicationProtocolMapperCreateCtrl', function($scope, realm, serverInfo, application, ApplicationProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.create = true;
    if (application.protocol == null) {
        application.protocol = 'openid-connect';
    }
    var protocol = application.protocol;
    $scope.protocol = protocol;
    $scope.mapper = { protocol :  application.protocol, config: {}};
    $scope.mapperTypes = serverInfo.protocolMapperTypes[protocol];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.mapper.protocolMapper = $scope.mapperType.id;
        ApplicationProtocolMapper.save({
            realm : realm.realm, application: application.id
        }, $scope.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/applications/' + application.id + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});




