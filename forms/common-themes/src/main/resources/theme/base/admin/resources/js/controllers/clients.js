Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

module.controller('ClientTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeClient = function() {
        Dialog.confirmDelete($scope.client.clientId, 'client', function() {
            $scope.client.$remove({
                realm : Current.realm.realm,
                client : $scope.client.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/clients");
                Notifications.success("The client has been deleted.");
            });
        });
    };
});

module.controller('ClientRoleListCtrl', function($scope, $location, realm, client, roles) {
    $scope.realm = realm;
    $scope.roles = roles;
    $scope.client = client;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ClientCredentialsCtrl', function($scope, $location, realm, client, clientAuthenticatorProviders, clientConfigProperties, Client) {
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    $scope.clientAuthenticatorProviders = clientAuthenticatorProviders;

    var updateCurrentPartial = function(val) {
        $scope.clientAuthenticatorConfigPartial;
        switch(val) {
            case 'client-secret':
                $scope.clientAuthenticatorConfigPartial = 'client-credentials-secret.html';
                break;
            case 'client-jwt':
                $scope.clientAuthenticatorConfigPartial = 'client-credentials-jwt.html';
                break;
            default:
                $scope.currentAuthenticatorConfigProperties = clientConfigProperties[val];
                $scope.clientAuthenticatorConfigPartial = 'client-credentials-generic.html';
                break;
        }
    };

    updateCurrentPartial(client.clientAuthenticatorType);

    $scope.$watch('client.clientAuthenticatorType', function() {
        if (!angular.equals($scope.client.clientAuthenticatorType, client.clientAuthenticatorType)) {

            Client.update({
                realm : realm.realm,
                client : client.id
            }, $scope.client, function() {
                $scope.changed = false;
                client = angular.copy($scope.client);
                updateCurrentPartial(client.clientAuthenticatorType)
            });

        }
    }, true);

});

module.controller('ClientSecretCtrl', function($scope, $location, ClientSecret, Notifications) {
    var secret = ClientSecret.get({ realm : $scope.realm.realm, client : $scope.client.id },
        function() {
            $scope.secret = secret.value;
        }
    );

    $scope.changePassword = function() {
        var secret = ClientSecret.update({ realm : $scope.realm.realm, client : $scope.client.id },
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

    $scope.cancel = function() {
        $location.url("/realms/" + $scope.realm.realm + "/clients/" + $scope.client.id + "/credentials");
    };
});

module.controller('ClientSignedJWTCtrl', function($scope, $location, ClientCertificate) {
    var signingKeyInfo = ClientCertificate.get({ realm : $scope.realm.realm, client : $scope.client.id, attribute: 'jwt.credential' },
        function() {
            $scope.signingKeyInfo = signingKeyInfo;
        }
    );

    $scope.importCertificate = function() {
        $location.url("/realms/" + $scope.realm.realm + "/clients/" + $scope.client.id + "/credentials/client-jwt/Signing/import/jwt.credential");
    };

    $scope.generateSigningKey = function() {
        $location.url("/realms/" + $scope.realm.realm + "/clients/" + $scope.client.id + "/credentials/client-jwt/Signing/export/jwt.credential");
    };

    $scope.cancel = function() {
        $location.url("/realms/" + $scope.realm.realm + "/clients/" + $scope.client.id + "/credentials");
    };
});

module.controller('ClientGenericCredentialsCtrl', function($scope, $location, Client, Notifications) {

    console.log('ClientGenericCredentialsCtrl invoked');

    $scope.clientCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch('client', function() {
        if (!angular.equals($scope.client, $scope.clientCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {

        Client.update({
            realm : $scope.realm.realm,
            client : $scope.client.id
        }, $scope.client, function() {
            $scope.changed = false;
            $scope.clientCopy = angular.copy($scope.client);
            Notifications.success("Client authentication configuration has been saved to the client.");
        });
    };

    $scope.reset = function() {
        $scope.client = angular.copy($scope.clientCopy);
        $scope.changed = false;
    };
});

module.controller('ClientIdentityProviderCtrl', function($scope, $location, $route, realm, client, Client, $location, Notifications) {
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    var length = 0;

    if ($scope.client.identityProviders) {
        length = $scope.client.identityProviders.length;

        for (i = 0; i < $scope.client.identityProviders.length; i++) {
            var clientProvider = $scope.client.identityProviders[i];
            if (clientProvider.retrieveToken) {
                clientProvider.retrieveToken = clientProvider.retrieveToken.toString();
            }
        }

    } else {
        $scope.client.identityProviders = [];
    }

    $scope.identityProviders = [];
    var providersMissingInClient = [];

    for (j = 0; j < realm.identityProviders.length; j++) {
        var identityProvider = realm.identityProviders[j];
        var clientProvider = null;

        for (i = 0; i < $scope.client.identityProviders.length; i++) {
            clientProvider = $scope.client.identityProviders[i];

            if (clientProvider) {

                if (clientProvider.id == identityProvider.id) {
                    $scope.identityProviders[i] = {};
                    $scope.identityProviders[i].identityProvider = identityProvider;
                    $scope.identityProviders[i].retrieveToken = clientProvider.retrieveToken;
                    break;
                }

                clientProvider = null;
            }
        }

        if (clientProvider == null) {
            providersMissingInClient.push(identityProvider);
        }
    }

    for (j = 0; j < providersMissingInClient.length; j++) {
        var identityProvider = providersMissingInClient[j];

        var currentProvider = {};
        currentProvider.identityProvider = identityProvider;
        currentProvider.retrieveToken = "false";
        $scope.identityProviders.push(currentProvider);

        var currentClientProvider = {};
        currentClientProvider.id = identityProvider.id;
        currentClientProvider.retrieveToken = "false";
        $scope.client.identityProviders.push(currentClientProvider);
    }

    var oldCopy = angular.copy($scope.client);

    $scope.save = function() {

        Client.update({
            realm : realm.realm,
            client : client.id
        }, $scope.client, function() {
            $scope.changed = false;
            $route.reload();
            Notifications.success("Your changes have been saved to the client.");
        });
    };

    $scope.reset = function() {
        $scope.client = angular.copy(oldCopy);
        $scope.changed = false;
    };

    $scope.$watch('client', function() {
        if (!angular.equals($scope.client, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
});

module.controller('ClientSamlKeyCtrl', function($scope, $location, $http, $upload, realm, client,
                                                         ClientCertificate, ClientCertificateGenerate,
                                                         ClientCertificateDownload, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    var signingKeyInfo = ClientCertificate.get({ realm : realm.realm, client : client.id, attribute: 'saml.signing' },
        function() {
            $scope.signingKeyInfo = signingKeyInfo;
        }
    );

    $scope.generateSigningKey = function() {
        var keyInfo = ClientCertificateGenerate.generate({ realm : realm.realm, client : client.id, attribute: 'saml.signing' },
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
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/saml/Signing/import/saml.signing");
    };

    $scope.exportSigningKey = function() {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/saml/Signing/export/saml.signing");
    };

    var encryptionKeyInfo = ClientCertificate.get({ realm : realm.realm, client : client.id, attribute: 'saml.encryption' },
        function() {
            $scope.encryptionKeyInfo = encryptionKeyInfo;
        }
    );

    $scope.generateEncryptionKey = function() {
        var keyInfo = ClientCertificateGenerate.generate({ realm : realm.realm, client : client.id, attribute: 'saml.encryption' },
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
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/saml/Encryption/import/saml.encryption");
    };

    $scope.exportEncryptionKey = function() {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/saml/Encryption/export/saml.encryption");
    };


    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ClientCertificateImportCtrl', function($scope, $location, $http, $upload, realm, client, callingContext, $routeParams,
                                                         ClientCertificate, ClientCertificateGenerate,
                                                         ClientCertificateDownload, Notifications) {

    console.log("callingContext: " + callingContext);

    var keyType = $routeParams.keyType;
    var attribute = $routeParams.attribute;
    $scope.realm = realm;
    $scope.client = client;
    $scope.keyType = keyType;

    if (callingContext == 'saml') {
        var uploadUrl = authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/certificates/' + attribute + '/upload';
        var redirectLocation = "/realms/" + realm.realm + "/clients/" + client.id + "/saml/keys";
    } else if (callingContext == 'jwt-credentials') {
        var uploadUrl = authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/certificates/' + attribute + '/upload-certificate';
        var redirectLocation = "/realms/" + realm.realm + "/clients/" + client.id + "/credentials";
    }

    $scope.files = [];

    $scope.onFileSelect = function($files) {
        $scope.files = $files;
    };

    $scope.cancel = function() {
        $location.url(redirectLocation);
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
                url: uploadUrl,
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
            }).success(function(data, status, headers) {
                Notifications.success("Keystore uploaded successfully.");
                $location.url(redirectLocation);
            }).error(function(data) {
                var errorMsg = data['error_description'] ? data['error_description'] : 'The key store can not be uploaded. Please verify the file.';
                Notifications.error(errorMsg);
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

module.controller('ClientCertificateExportCtrl', function($scope, $location, $http, $upload, realm, client, callingContext, $routeParams,
                                                         ClientCertificate, ClientCertificateGenerate,
                                                         ClientCertificateDownload, Notifications) {
    var keyType = $routeParams.keyType;
    var attribute = $routeParams.attribute;
    $scope.realm = realm;
    $scope.client = client;
    $scope.keyType = keyType;

    if (callingContext == 'saml') {
        var downloadUrl = authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/certificates/' + attribute + '/download';
        var realmCertificate = true;
    } else if (callingContext == 'jwt-credentials') {
        var downloadUrl = authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/certificates/' + attribute + '/generate-and-download'
        var realmCertificate = false;
    }

    var jks = {
        keyAlias: client.clientId,
        realmAlias: realm.realm,
        realmCertificate: realmCertificate
    };

    $scope.keyFormats = [
        "JKS",
        "PKCS12"
    ];

    var keyInfo = ClientCertificate.get({ realm : realm.realm, client : client.id, attribute: attribute },
        function() {
            $scope.keyInfo = keyInfo;
        }
    );
    $scope.jks = jks;
    $scope.jks.format = $scope.keyFormats[0];

    $scope.download = function() {
        $http({
            url: downloadUrl,
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

            if (callingContext == 'jwt-credentials') {
                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/credentials");
                Notifications.success("New keypair and certificate generated successfully. Download keystore file")
            }

            saveAs(blob, 'keystore' + ext);
        }).error(function(data) {
            var errorMsg = 'Error downloading';
            try {
                var error = JSON.parse(String.fromCharCode.apply(null, new Uint8Array(data)));
                errorMsg = error['error_description'] ? error['error_description'] : errorMsg;
            } catch (err) {
            }
            Notifications.error(errorMsg);
        });
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/credentials");
    }
});

module.controller('ClientSessionsCtrl', function($scope, realm, sessionCount, client,
                                                      ClientUserSessions) {
    $scope.realm = realm;
    $scope.count = sessionCount.count;
    $scope.sessions = [];
    $scope.client = client;

    $scope.page = 0;

    $scope.query = {
        realm : realm.realm,
        client: $scope.client.id,
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
        ClientUserSessions.query($scope.query, function(updated) {
            $scope.sessions = updated;
        })
    };
});

module.controller('ClientOfflineSessionsCtrl', function($scope, realm, offlineSessionCount, client,
                                                      ClientOfflineSessions) {
    $scope.realm = realm;
    $scope.count = offlineSessionCount.count;
    $scope.sessions = [];
    $scope.client = client;

    $scope.page = 0;

    $scope.query = {
        realm : realm.realm,
        client: $scope.client.id,
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
        ClientOfflineSessions.query($scope.query, function(updated) {
            $scope.sessions = updated;
        })
    };
});

module.controller('ClientRoleDetailCtrl', function($scope, realm, client, role, roles, clients,
                                                        Role, ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
                                                        $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.save = function() {
        if ($scope.create) {
            ClientRole.save({
                realm: realm.realm,
                client : client.id
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                ClientRole.get({ realm: realm.realm, client : client.id, role: role.name }, function(role) {
                    var id = role.id;
                    $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/roles/" + id);
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
                client : client.id,
                role : $scope.role.id
            }, function() {
                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/roles");
                Notifications.success("The role has been deleted.");
            });
        });
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/roles");
    };


    roleControl($scope, realm, role, roles, clients,
        ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
        $http, $location, Notifications, Dialog);

});

module.controller('ClientImportCtrl', function($scope, $location, $upload, realm, serverInfo, Notifications) {

    $scope.realm = realm;

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
                url: authUrl + '/admin/realms/' + realm.realm + '/client-importers/' + $scope.configFormat.id + '/upload',
                // method: POST or PUT,
                // headers: {'headerKey': 'headerValue'}, withCredential: true,
                data: {myObj: ""},
                file: $file
                /* set file formData name for 'Content-Desposition' header. Default: 'file' */
                //fileFormDataName: myFile,
                /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
                //formDataAppender: function(formData, key, val){}
            }).success(function(data, status, headers) {
                Notifications.success("Uploaded successfully.");
                $location.url("/realms/" + realm.realm + "/clients");
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


module.controller('ClientListCtrl', function($scope, realm, clients, Client, serverInfo, $route, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.clients = clients;

    $scope.removeClient = function(client) {
        Dialog.confirmDelete(client.clientId, 'client', function() {
            Client.remove({
                realm : realm.realm,
                client : client.id
            }, function() {
                $route.reload();
                Notifications.success("The client has been deleted.");
            });
        });
    };
});

module.controller('ClientInstallationCtrl', function($scope, realm, client, ClientInstallation,ClientInstallationJBoss, $http, $routeParams) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.installation = null;
    $scope.download = null;
    $scope.configFormat = null;
    $scope.filename = null;

    $scope.configFormats = [
        "Keycloak JSON",
        "Wildfly/EAP Subsystem XML"
    ];

    $scope.changeFormat = function() {
        if ($scope.configFormat == "Keycloak JSON") {
            $scope.filename = 'keycloak.json';

            var url = ClientInstallation.url({ realm: $routeParams.realm, client: $routeParams.client });
            $http.get(url).success(function(data) {
                var tmp = angular.fromJson(data);
                $scope.installation = angular.toJson(tmp, true);
                $scope.type = 'application/json';
            })
        } else if ($scope.configFormat == "Wildfly/EAP Subsystem XML") {
            $scope.filename = 'keycloak.xml';

            var url = ClientInstallationJBoss.url({ realm: $routeParams.realm, client: $routeParams.client });
            $http.get(url).success(function(data) {
                $scope.installation = data;
                $scope.type = 'text/xml';
            })
        }

        console.debug($scope.filename);
    };

    $scope.download = function() {
        saveAs(new Blob([$scope.installation], { type: $scope.type }), $scope.filename);
    }
});

module.controller('ClientDetailCtrl', function($scope, realm, client, $route, serverInfo, Client, ClientDescriptionConverter, $location, $modal, Dialog, Notifications) {
    $scope.accessTypes = [
        "confidential",
        "public",
        "bearer-only"
    ];

    $scope.protocols = Object.keys(serverInfo.providers['login-protocol'].providers).sort();

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

    $scope.canonicalization = [
        {name: "EXCLUSIVE", value:  "http://www.w3.org/2001/10/xml-exc-c14n#"  },
        {name: "EXCLUSIVE_WITH_COMMENTS", value: "http://www.w3.org/2001/10/xml-exc-c14n#WithComments"},
        {name: "INCLUSIVE", value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315" },
        {name: "INCLUSIVE_WITH_COMMENTS", value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments"}
    ];

    $scope.realm = realm;
    $scope.create = !client.clientId;
    $scope.samlAuthnStatement = false;
    $scope.samlMultiValuedRoles = false;
    $scope.samlServerSignature = false;
    $scope.samlAssertionSignature = false;
    $scope.samlClientSignature = false;
    $scope.samlEncrypt = false;
    $scope.samlForcePostBinding = false;
    $scope.samlForceNameIdFormat = false;

    function updateProperties() {
        if (!$scope.client.attributes) {
            $scope.client.attributes = {};
        }
        $scope.accessType = $scope.accessTypes[0];
        if ($scope.client.bearerOnly) {
            $scope.accessType = $scope.accessTypes[2];
        } else if ($scope.client.publicClient) {
            $scope.accessType = $scope.accessTypes[1];
        }
        if ($scope.client.protocol) {
            $scope.protocol = $scope.protocols[$scope.protocols.indexOf($scope.client.protocol)];
        } else {
            $scope.protocol = $scope.protocols[0];
        }
        if ($scope.client.attributes['saml.signature.algorithm'] == 'RSA_SHA1') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[0];
        } else if ($scope.client.attributes['saml.signature.algorithm'] == 'RSA_SHA256') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[1];
        } else if ($scope.client.attributes['saml.signature.algorithm'] == 'RSA_SHA512') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[2];
        } else if ($scope.client.attributes['saml.signature.algorithm'] == 'DSA_SHA1') {
            $scope.signatureAlgorithm = $scope.signatureAlgorithms[3];
        }
        if ($scope.client.attributes['saml_name_id_format'] == 'username') {
            $scope.nameIdFormat = $scope.nameIdFormats[0];
        } else if ($scope.client.attributes['saml_name_id_format'] == 'email') {
            $scope.nameIdFormat = $scope.nameIdFormats[1];
        } else if ($scope.client.attributes['saml_name_id_format'] == 'transient') {
            $scope.nameIdFormat = $scope.nameIdFormats[2];
        } else if ($scope.client.attributes['saml_name_id_format'] == 'persistent') {
            $scope.nameIdFormat = $scope.nameIdFormats[3];
        }
    }

    if (!$scope.create) {
        $scope.client = angular.copy(client);
        updateProperties();
    } else {
        $scope.client = { enabled: true, attributes: {}};
        $scope.client.attributes['saml_signature_canonicalization_method'] = $scope.canonicalization[0].value;
        $scope.client.redirectUris = [];
        $scope.accessType = $scope.accessTypes[0];
        $scope.protocol = $scope.protocols[0];
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[1];
        $scope.nameIdFormat = $scope.nameIdFormats[0];
        $scope.samlAuthnStatement = true;
        $scope.samlForceNameIdFormat = false;
    }

    if ($scope.client.attributes["saml.server.signature"]) {
        if ($scope.client.attributes["saml.server.signature"] == "true") {
            $scope.samlServerSignature = true;
        } else {
            $scope.samlServerSignature = false;

        }
    }
    if ($scope.client.attributes["saml.assertion.signature"]) {
        if ($scope.client.attributes["saml.assertion.signature"] == "true") {
            $scope.samlAssertionSignature = true;
        } else {
            $scope.samlAssertionSignature = false;
        }
    }
    if ($scope.client.attributes["saml.client.signature"]) {
        if ($scope.client.attributes["saml.client.signature"] == "true") {
            $scope.samlClientSignature = true;
        } else {
            $scope.samlClientSignature = false;
        }
    }
    if ($scope.client.attributes["saml.encrypt"]) {
        if ($scope.client.attributes["saml.encrypt"] == "true") {
            $scope.samlEncrypt = true;
        } else {
            $scope.samlEncrypt = false;
        }
    }
    if ($scope.client.attributes["saml.authnstatement"]) {
        if ($scope.client.attributes["saml.authnstatement"] == "true") {
            $scope.samlAuthnStatement = true;
        } else {
            $scope.samlAuthnStatement = false;
        }
    }
    if ($scope.client.attributes["saml_force_name_id_format"]) {
        if ($scope.client.attributes["saml_force_name_id_format"] == "true") {
            $scope.samlForceNameIdFormat = true;
        } else {
            $scope.samlForceNameIdFormat = false;
        }
    }
    if ($scope.client.attributes["saml.multivalued.roles"]) {
        if ($scope.client.attributes["saml.multivalued.roles"] == "true") {
            $scope.samlMultiValuedRoles = true;
        } else {
            $scope.samlMultiValuedRoles = false;
        }
    }
    if ($scope.client.attributes["saml.force.post.binding"]) {
        if ($scope.client.attributes["saml.force.post.binding"] == "true") {
            $scope.samlForcePostBinding = true;
        } else {
            $scope.samlForcePostBinding = false;
        }
    }

    $scope.importFile = function(fileContent){
        console.debug(fileContent);
        ClientDescriptionConverter.save({
            realm: realm.realm
        }, fileContent, function (data) {
            $scope.client = data;
            updateProperties();
            $scope.importing = true;
        });
    };

    $scope.viewImportDetails = function() {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'JsonModalCtrl',
            resolve: {
                object: function () {
                    return $scope.client;
                }
            }
        })
    };

    $scope.switchChange = function() {
        $scope.changed = true;
    }

    $scope.changeAccessType = function() {
        if ($scope.accessType == "confidential") {
            $scope.client.bearerOnly = false;
            $scope.client.publicClient = false;
        } else if ($scope.accessType == "public") {
            $scope.client.bearerOnly = false;
            $scope.client.publicClient = true;
        } else if ($scope.accessType == "bearer-only") {
            $scope.client.bearerOnly = true;
            $scope.client.publicClient = false;
        }
    };

    $scope.changeProtocol = function() {
        if ($scope.protocol == "openid-connect") {
            $scope.client.protocol = "openid-connect";
        } else if ($scope.protocol == "saml") {
            $scope.client.protocol = "saml";
        }
    };

    $scope.changeAlgorithm = function() {
        $scope.client.attributes['saml.signature.algorithm'] = $scope.signatureAlgorithm;
    };

    $scope.changeNameIdFormat = function() {
        $scope.client.attributes['saml_name_id_format'] = $scope.nameIdFormat;
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    function isChanged() {
        if (!angular.equals($scope.client, client)) {
            return true;
        }
        if ($scope.newRedirectUri && $scope.newRedirectUri.length > 0) {
            return true;
        }
        if ($scope.newWebOrigin && $scope.newWebOrigin.length > 0) {
            return true;
        }
        return false;
    }

    $scope.$watch('client', function() {
        $scope.changed = isChanged();
    }, true);

    $scope.$watch('newRedirectUri', function() {
        $scope.changed = isChanged();
    }, true);


    $scope.$watch('newWebOrigin', function() {
        $scope.changed = isChanged();
    }, true);

    $scope.deleteWebOrigin = function(index) {
        $scope.client.webOrigins.splice(index, 1);
    }
    $scope.addWebOrigin = function() {
        $scope.client.webOrigins.push($scope.newWebOrigin);
        $scope.newWebOrigin = "";
    }
    $scope.deleteRedirectUri = function(index) {
        $scope.client.redirectUris.splice(index, 1);
    }

    $scope.addRedirectUri = function() {
        $scope.client.redirectUris.push($scope.newRedirectUri);
        $scope.newRedirectUri = "";
    }

    $scope.save = function() {
        if ($scope.newRedirectUri && $scope.newRedirectUri.length > 0) {
            $scope.addRedirectUri();
        }

        if ($scope.newWebOrigin && $scope.newWebOrigin.length > 0) {
            $scope.addWebOrigin();
        }

        if ($scope.samlServerSignature == true) {
            $scope.client.attributes["saml.server.signature"] = "true";
        } else {
            $scope.client.attributes["saml.server.signature"] = "false";

        }
        if ($scope.samlAssertionSignature == true) {
            $scope.client.attributes["saml.assertion.signature"] = "true";
        } else {
            $scope.client.attributes["saml.assertion.signature"] = "false";
        }
        if ($scope.samlClientSignature == true) {
            $scope.client.attributes["saml.client.signature"] = "true";
        } else {
            $scope.client.attributes["saml.client.signature"] = "false";

        }
        if ($scope.samlEncrypt == true) {
            $scope.client.attributes["saml.encrypt"] = "true";
        } else {
            $scope.client.attributes["saml.encrypt"] = "false";

        }
        if ($scope.samlAuthnStatement == true) {
            $scope.client.attributes["saml.authnstatement"] = "true";
        } else {
            $scope.client.attributes["saml.authnstatement"] = "false";

        }
        if ($scope.samlForceNameIdFormat == true) {
            $scope.client.attributes["saml_force_name_id_format"] = "true";
        } else {
            $scope.client.attributes["saml_force_name_id_format"] = "false";

        }
        if ($scope.samlMultiValuedRoles == true) {
            $scope.client.attributes["saml.multivalued.roles"] = "true";
        } else {
            $scope.client.attributes["saml.multivalued.roles"] = "false";

        }
        if ($scope.samlForcePostBinding == true) {
            $scope.client.attributes["saml.force.post.binding"] = "true";
        } else {
            $scope.client.attributes["saml.force.post.binding"] = "false";

        }

        $scope.client.protocol = $scope.protocol;
        $scope.client.attributes['saml.signature.algorithm'] = $scope.signatureAlgorithm;
        $scope.client.attributes['saml_name_id_format'] = $scope.nameIdFormat;

        if ($scope.client.protocol != 'saml' && !$scope.client.bearerOnly && !$scope.client.directGrantsOnly && (!$scope.client.redirectUris || $scope.client.redirectUris.length == 0)) {
            Notifications.error("You must specify at least one redirect uri");
        } else {
            if ($scope.create) {
                Client.save({
                    realm: realm.realm,
                    client: ''
                }, $scope.client, function (data, headers) {
                    $scope.changed = false;
                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.realm + "/clients/" + id);
                    Notifications.success("The client has been created.");
                });
            } else {
                Client.update({
                    realm : realm.realm,
                    client : client.id
                }, $scope.client, function() {
                    $scope.changed = false;
                    client = angular.copy($scope.client);
                    $location.url("/realms/" + realm.realm + "/clients/" + client.id);
                    Notifications.success("Your changes have been saved to the client.");
                });
            }
        }
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/clients");
    };
});

module.controller('ClientScopeMappingCtrl', function($scope, $http, realm, client, clients, Notifications,
                                                          Client,
                                                          ClientRealmScopeMapping, ClientClientScopeMapping, ClientRole,
                                                          ClientAvailableRealmScopeMapping, ClientAvailableClientScopeMapping,
                                                          ClientCompositeRealmScopeMapping, ClientCompositeClientScopeMapping) {
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clients = clients;
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];


    $scope.changeFullScopeAllowed = function() {
        Client.update({
            realm : realm.realm,
            client : client.id
        }, $scope.client, function() {
            $scope.changed = false;
            client = angular.copy($scope.client);
            updateRealmRoles();
            Notifications.success("Scope mappings updated.");
        });
    }



    function updateRealmRoles() {
        $scope.realmRoles = ClientAvailableRealmScopeMapping.query({realm : realm.realm, client : client.id});
        $scope.realmMappings = ClientRealmScopeMapping.query({realm : realm.realm, client : client.id});
        $scope.realmComposite = ClientCompositeRealmScopeMapping.query({realm : realm.realm, client : client.id});
    }

    function updateClientRoles() {
        if ($scope.targetClient) {
            $scope.clientRoles = ClientAvailableClientScopeMapping.query({realm : realm.realm, client : client.id, targetClient : $scope.targetClient.id});
            $scope.clientMappings = ClientClientScopeMapping.query({realm : realm.realm, client : client.id, targetClient : $scope.targetClient.id});
            $scope.clientComposite = ClientCompositeClientScopeMapping.query({realm : realm.realm, client : client.id, targetClient : $scope.targetClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
    }

    $scope.changeClient = function() {
        updateClientRoles();
    };

    $scope.addRealmRole = function() {
        var roles = $scope.selectedRealmRoles;
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/scope-mappings/realm',
            roles).success(function() {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        var roles = $scope.selectedRealmMappings;
        $scope.selectedRealmMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/realm',
            {data : roles, headers : {"content-type" : "application/json"}}).success(function () {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        var roles = $scope.selectedClientRoles;
        $scope.selectedClientRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/clients/' + $scope.targetClient.id,
                roles).success(function () {
                updateClientRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        var roles = $scope.selectedClientMappings;
        $scope.selectedClientMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/clients/' + $scope.targetClient.id,
            {data : roles, headers : {"content-type" : "application/json"}}).success(function () {
                updateClientRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    updateRealmRoles();
});

module.controller('ClientRevocationCtrl', function($scope, realm, client, Client, ClientPushRevocation, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;

    var setNotBefore = function() {
        if ($scope.client.notBefore == 0) {
            $scope.notBefore = "None";
        } else {
            $scope.notBefore = new Date($scope.client.notBefore * 1000);
        }
    };

    setNotBefore();

    var refresh = function() {
        Client.get({ realm : realm.realm, client: $scope.client.id }, function(updated) {
            $scope.client = updated;
            setNotBefore();
        })

    };

    $scope.clear = function() {
        $scope.client.notBefore = 0;
        Client.update({ realm : realm.realm, client: client.id}, $scope.client, function () {
            $scope.notBefore = "None";
            Notifications.success('Not Before cleared for client.');
            refresh();
        });
    }
    $scope.setNotBeforeNow = function() {
        $scope.client.notBefore = new Date().getTime()/1000;
        Client.update({ realm : realm.realm, client: $scope.client.id}, $scope.client, function () {
            Notifications.success('Not Before set for client.');
            refresh();
        });
    }
    $scope.pushRevocation = function() {
        ClientPushRevocation.save({realm : realm.realm, client: $scope.client.id}, function (globalReqResult) {
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

module.controller('ClientClusteringCtrl', function($scope, client, Client, ClientTestNodesAvailable, ClientClusterNode, realm, $location, $route, Dialog, Notifications, TimeUnit) {
    $scope.client = client;
    $scope.realm = realm;

    var oldCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch('client', function() {
        if (!angular.equals($scope.client, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.client.nodeReRegistrationTimeoutUnit = TimeUnit.autoUnit(client.nodeReRegistrationTimeout);
    $scope.client.nodeReRegistrationTimeout = TimeUnit.toUnit(client.nodeReRegistrationTimeout, $scope.client.nodeReRegistrationTimeoutUnit);
    $scope.$watch('client.nodeReRegistrationTimeoutUnit', function(to, from) {
        $scope.client.nodeReRegistrationTimeout = TimeUnit.convert($scope.client.nodeReRegistrationTimeout, from, to);
    });

    $scope.save = function() {
        var clientCopy = angular.copy($scope.client);
        delete clientCopy['nodeReRegistrationTimeoutUnit'];
        clientCopy.nodeReRegistrationTimeout = TimeUnit.toSeconds($scope.client.nodeReRegistrationTimeout, $scope.client.nodeReRegistrationTimeoutUnit)
        Client.update({ realm : realm.realm, client : client.id }, clientCopy, function () {
            $route.reload();
            Notifications.success('Your changes have been saved to the client.');
        });
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.testNodesAvailable = function() {
        ClientTestNodesAvailable.get({ realm : realm.realm, client : client.id }, function(globalReqResult) {
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

    if (client.registeredNodes) {
        var nodeRegistrations = [];
        for (node in client.registeredNodes) {
            reg = {
                host: node,
                lastRegistration: new Date(client.registeredNodes[node] * 1000)
            }
            nodeRegistrations.push(reg);
        }

        $scope.nodeRegistrations = nodeRegistrations;
    };

    $scope.removeNode = function(node) {
        Dialog.confirmDelete(node.host, 'node', function() {
            ClientClusterNode.remove({ realm : realm.realm, client : client.id , node: node.host }, function() {
                Notifications.success('Node ' + node.host + ' unregistered successfully.');
                $route.reload();
            });
        });
    };
});

module.controller('ClientClusteringNodeCtrl', function($scope, client, Client, ClientClusterNode, realm, $location, $routeParams, Notifications) {
    $scope.client = client;
    $scope.realm = realm;
    $scope.create = !$routeParams.node;

    $scope.save = function() {
        ClientClusterNode.save({ realm : realm.realm, client : client.id , node: $scope.node.host }, function() {
            Notifications.success('Node ' + $scope.node.host + ' registered successfully.');
            $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/clustering');
        });
    }

    $scope.unregisterNode = function() {
        ClientClusterNode.remove({ realm : realm.realm, client : client.id , node: $scope.node.host }, function() {
            Notifications.success('Node ' + $scope.node.host + ' unregistered successfully.');
            $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/clustering');
        });
    }

    if ($scope.create) {
        $scope.node = {}
        $scope.registered = false;
    } else {
        var lastRegTime = client.registeredNodes[$routeParams.node];

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

module.controller('AddBuiltinProtocolMapperCtrl', function($scope, realm, client, serverInfo,
                                                            ClientProtocolMappersByProtocol,
                                                            $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;




    var updateMappers = function() {
        var clientMappers = ClientProtocolMappersByProtocol.query({realm : realm.realm, client : client.id, protocol : client.protocol}, function() {
            var builtinMappers = serverInfo.builtinProtocolMappers[client.protocol];
            for (var i = 0; i < clientMappers.length; i++) {
                for (var j = 0; j < builtinMappers.length; j++) {
                    if (builtinMappers[j].name == clientMappers[i].name
                        && builtinMappers[j].protocolMapper == clientMappers[i].protocolMapper) {
                        builtinMappers.splice(j, 1);
                        break;
                    }
                }
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
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/protocol-mappers/add-models',
                   toAdd).success(function() {
                Notifications.success("Mappers added");
                $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/mappers');
            }).error(function() {
                Notifications.error("Error adding mappers");
                $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/mappers');
            });
    };

});

module.controller('ClientProtocolMapperListCtrl', function($scope, realm, client, serverInfo,
                                                           ClientProtocolMappersByProtocol, ClientProtocolMapper,
                                                           $route, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.removeMapper = function(mapper) {
        console.debug(mapper);
        Dialog.confirmDelete(mapper.name, 'mapper', function() {
            ClientProtocolMapper.remove({ realm: realm.realm, client: client.id, id : mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $route.reload();
            });
        });
    };

    var updateMappers = function() {
        $scope.mappers = ClientProtocolMappersByProtocol.query({realm : realm.realm, client : client.id, protocol : client.protocol});
    };

    updateMappers();
});

module.controller('ClientProtocolMapperCtrl', function($scope, realm, serverInfo, client, mapper, ClientProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.create = false;
    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }
    $scope.protocol = client.protocol;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    $scope.boolval = true;
    $scope.boolvalId = 'boolval';

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
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
        ClientProtocolMapper.update({
            realm : realm.realm,
            client: client.id,
            id : mapper.id
        }, $scope.mapper, function() {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/clients/' + client.id + "/mappers/" + mapper.id);
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
            ClientProtocolMapper.remove({ realm: realm.realm, client: client.id, id : $scope.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/clients/' + client.id + "/mappers");
            });
        });
    };

});

module.controller('ClientProtocolMapperCreateCtrl', function($scope, realm, serverInfo, client, ClientProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.create = true;
    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }
    var protocol = client.protocol;
    $scope.protocol = protocol;
    $scope.mapper = { protocol :  client.protocol, config: {}};
    $scope.mapperTypes = serverInfo.protocolMapperTypes[protocol];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.mapper.protocolMapper = $scope.mapperType.id;
        ClientProtocolMapper.save({
            realm : realm.realm, client: client.id
        }, $scope.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/clients/' + client.id + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});



