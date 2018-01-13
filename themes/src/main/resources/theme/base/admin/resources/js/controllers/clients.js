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

module.controller('ClientRoleListCtrl', function($scope, $location, realm, client, roles, $route, RoleById, Notifications, Dialog) {
    $scope.realm = realm;
    $scope.roles = roles;
    $scope.client = client;

    $scope.removeRole = function(role) {
        Dialog.confirmDelete(role.name, 'role', function() {
            RoleById.remove({
                realm: realm.realm,
                role: role.id
            }, function () {
                $route.reload();
                Notifications.success("The role has been deleted.");
            });
        });
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ClientCredentialsCtrl', function($scope, $location, realm, client, clientAuthenticatorProviders, clientConfigProperties, Client, ClientRegistrationAccessToken, Notifications) {
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

    $scope.regenerateRegistrationAccessToken = function() {
        var secret = ClientRegistrationAccessToken.update({ realm : $scope.realm.realm, client : $scope.client.id },
            function(data) {
                Notifications.success('The registration access token has been updated.');
                $scope.client['registrationAccessToken'] = data.registrationAccessToken;
            },
            function() {
                Notifications.error('Failed to update the registration access token');
            }
        );
    };
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

module.controller('ClientSignedJWTCtrl', function($scope, $location, Client, ClientCertificate, Notifications, $route) {
    var signingKeyInfo = ClientCertificate.get({ realm : $scope.realm.realm, client : $scope.client.id, attribute: 'jwt.credential' },
        function() {
            $scope.signingKeyInfo = signingKeyInfo;
        }
    );

    console.log('ClientSignedJWTCtrl invoked');

    $scope.clientCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch('client', function() {
        if (!angular.equals($scope.client, $scope.clientCopy)) {
            $scope.changed = true;
        }
    }, true);

    if ($scope.client.attributes["use.jwks.url"]) {
        if ($scope.client.attributes["use.jwks.url"] == "true") {
            $scope.useJwksUrl = true;
        } else {
            $scope.useJwksUrl = false;
        }
    }

    $scope.switchChange = function() {
        $scope.changed = true;
    }

    $scope.save = function() {

        if ($scope.useJwksUrl == true) {
            $scope.client.attributes["use.jwks.url"] = "true";
        } else {
            $scope.client.attributes["use.jwks.url"] = "false";
        }

        Client.update({
            realm : $scope.realm.realm,
            client : $scope.client.id
        }, $scope.client, function() {
            $scope.changed = false;
            $scope.clientCopy = angular.copy($scope.client);
            Notifications.success("Client authentication configuration has been saved to the client.");
        });
    };

    $scope.importCertificate = function() {
        $location.url("/realms/" + $scope.realm.realm + "/clients/" + $scope.client.id + "/credentials/client-jwt/Signing/import/jwt.credential");
    };

    $scope.generateSigningKey = function() {
        $location.url("/realms/" + $scope.realm.realm + "/clients/" + $scope.client.id + "/credentials/client-jwt/Signing/export/jwt.credential");
    };

    $scope.reset = function() {
        $route.reload();
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
        "PKCS12",
        "Certificate PEM"
    ];

    if (callingContext == 'jwt-credentials') {
        $scope.keyFormats.push('Public Key PEM');
        $scope.keyFormats.push('JSON Web Key Set');
    }

    $scope.hideKeystoreSettings = function() {
        return $scope.uploadKeyFormat == 'Certificate PEM' || $scope.uploadKeyFormat == 'Public Key PEM' || $scope.uploadKeyFormat == 'JSON Web Key Set';
    }

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
            }).then(function(data, status, headers) {
                Notifications.success("Keystore uploaded successfully.");
                $location.url(redirectLocation);
            })
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
        }).then(function(response){
            var blob = new Blob([response.data], {
                type: 'application/octet-stream'
            });
            var ext = ".jks";
            if ($scope.jks.format == 'PKCS12') ext = ".p12";

            if (callingContext == 'jwt-credentials') {
                $location.url("/realms/" + realm.realm + "/clients/" + client.id + "/credentials");
                Notifications.success("New keypair and certificate generated successfully. Download keystore file")
            }

            saveAs(blob, 'keystore' + ext);
        }).catch(function(response) {
            var errorMsg = 'Error downloading';
            try {
                var error = JSON.parse(String.fromCharCode.apply(null, new Uint8Array(response.data)));
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


module.controller('ClientListCtrl', function($scope, realm, Client, serverInfo, $route, Dialog, Notifications, filterFilter) {
    $scope.realm = realm;
    $scope.clients = [];
    $scope.currentPage = 1;
    $scope.currentPageInput = 1;
    $scope.numberOfPages = 1;
    $scope.pageSize = 20;
    
    Client.query({realm: realm.realm, viewableOnly: true}).$promise.then(function(clients) {
        $scope.numberOfPages = Math.ceil(clients.length/$scope.pageSize);
        $scope.clients = clients;
    });
    
    $scope.$watch('search', function (newVal, oldVal) {
        $scope.filtered = filterFilter($scope.clients, newVal);
        $scope.totalItems = $scope.filtered.length;
        $scope.numberOfPages = Math.ceil($scope.totalItems/$scope.pageSize);
        $scope.currentPage = 1;
        $scope.currentPageInput = 1;
  }, true);
  
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

    $scope.exportClient = function(client) {
        var clientCopy = angular.copy(client);
        delete clientCopy.id;

        for (var i = 0; i < clientCopy.protocolMappers.length; i++) {
            delete clientCopy.protocolMappers[i].id;
        }

        saveAs(new Blob([angular.toJson(clientCopy, 4)], { type: 'application/json' }), clientCopy.clientId + '.json');
    }
});

module.controller('ClientInstallationCtrl', function($scope, realm, client, serverInfo, ClientInstallation,$http, $routeParams) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.installation = null;
    $scope.download = null;
    $scope.configFormat = null;
    $scope.filename = null;

    var protocol = client.protocol;
    if (!protocol) protocol = 'openid-connect';
    $scope.configFormats = serverInfo.clientInstallations[protocol];
    console.log('configFormats.length: ' + $scope.configFormats.length);

    $scope.changeFormat = function() {
        var url = ClientInstallation.url({ realm: $routeParams.realm, client: $routeParams.client, provider: $scope.configFormat.id });
        if ($scope.configFormat.mediaType == 'application/zip') {
            $http({
                url: url,
                method: 'GET',
                responseType: 'arraybuffer',
                cache: false
            }).then(function(response) {
                var installation = response.data;
                $scope.installation = installation;
                }
            );
        } else {
            $http.get(url).then(function (response) {
                var installation = response.data;
                if ($scope.configFormat.mediaType == 'application/json') {
                    installation = angular.fromJson(response.data);
                    installation = angular.toJson(installation, true);
                }
                $scope.installation = installation;
            });
        }

    };
    $scope.download = function() {
        saveAs(new Blob([$scope.installation], { type: $scope.configFormat.mediaType }), $scope.configFormat.filename);
    }
});

module.controller('ClientDetailCtrl', function($scope, realm, client, templates, $route, serverInfo, Client, ClientDescriptionConverter, $location, $modal, Dialog, Notifications) {



    $scope.accessTypes = [
        "confidential",
        "public",
        "bearer-only"
    ];

    $scope.protocols = serverInfo.listProviderIds('login-protocol');

    $scope.templates = [ {name:'NONE'}];
    for (var i = 0; i < templates.length; i++) {
        var template = templates[i];
        $scope.templates.push(template);
    }

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
    $scope.xmlKeyNameTranformers = [
        "NONE",
        "KEY_ID",
        "CERT_SUBJECT"
    ];

    $scope.canonicalization = [
        {name: "EXCLUSIVE", value:  "http://www.w3.org/2001/10/xml-exc-c14n#"  },
        {name: "EXCLUSIVE_WITH_COMMENTS", value: "http://www.w3.org/2001/10/xml-exc-c14n#WithComments"},
        {name: "INCLUSIVE", value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315" },
        {name: "INCLUSIVE_WITH_COMMENTS", value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments"}
    ];

    $scope.oidcSignatureAlgorithms = [
        "unsigned",
        "RS256"
    ];

    $scope.requestObjectSignatureAlgorithms = [
        "any",
        "none",
        "RS256"
    ];

    $scope.realm = realm;
    $scope.samlAuthnStatement = false;
    $scope.samlOneTimeUseCondition = false;
    $scope.samlMultiValuedRoles = false;
    $scope.samlServerSignature = false;
    $scope.samlServerSignatureEnableKeyInfoExtension = false;
    $scope.samlAssertionSignature = false;
    $scope.samlClientSignature = false;
    $scope.samlEncrypt = false;
    $scope.samlForcePostBinding = false;
    $scope.samlForceNameIdFormat = false;
    $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
    $scope.disableAuthorizationTab = !client.authorizationServicesEnabled;
    $scope.disableServiceAccountRolesTab = !client.serviceAccountsEnabled;
    $scope.disableCredentialsTab = client.publicClient;

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
        if ($scope.client.attributes["saml.server.signature"]) {
            if ($scope.client.attributes["saml.server.signature"] == "true") {
                $scope.samlServerSignature = true;
            } else {
                $scope.samlServerSignature = false;

            }
        }
        if ($scope.client.attributes["saml.server.signature.keyinfo.ext"]) {
            if ($scope.client.attributes["saml.server.signature.keyinfo.ext"] == "true") {
                $scope.samlServerSignatureEnableKeyInfoExtension = true;
            } else {
                $scope.samlServerSignatureEnableKeyInfoExtension = false;
            }
        }
        if ($scope.client.attributes['saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer'] === 'NONE') {
            $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[0];
        } else if ($scope.client.attributes['saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer'] === 'KEY_ID') {
            $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
        } else if ($scope.client.attributes['saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer'] === 'CERT_SUBJECT') {
            $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[2];
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
         if ($scope.client.attributes["saml.onetimeuse.condition"]) {
                    if ($scope.client.attributes["saml.onetimeuse.condition"] == "true") {
                        $scope.samlOneTimeUseCondition = true;
                    } else {
                        $scope.samlOneTimeUseCondition = false;
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

        var attrVal1 = $scope.client.attributes['user.info.response.signature.alg'];
        $scope.userInfoSignedResponseAlg = attrVal1==null ? 'unsigned' : attrVal1;

        var attrVal2 = $scope.client.attributes['request.object.signature.alg'];
         $scope.requestObjectSignatureAlg = attrVal2==null ? 'any' : attrVal2;
    }

    if (!$scope.create) {
        $scope.client = client;
        updateProperties();

        $scope.clientEdit = angular.copy(client);
    }


    $scope.importFile = function(fileContent){
        console.debug(fileContent);
        ClientDescriptionConverter.save({
            realm: realm.realm
        }, fileContent, function (data) {
            $scope.client = data;
            updateProperties();
            $scope.importing = true;

            $scope.clientEdit = angular.copy(client);
        });
    };

    $scope.viewImportDetails = function() {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'ObjectModalCtrl',
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
            $scope.clientEdit.bearerOnly = false;
            $scope.clientEdit.publicClient = false;
        } else if ($scope.accessType == "public") {
            $scope.clientEdit.bearerOnly = false;
            $scope.clientEdit.publicClient = true;
        } else if ($scope.accessType == "bearer-only") {
            $scope.clientEdit.bearerOnly = true;
            $scope.clientEdit.publicClient = false;
        }
    };

    $scope.changeProtocol = function() {
        if ($scope.protocol == "openid-connect") {
            $scope.clientEdit.protocol = "openid-connect";
        } else if ($scope.protocol == "saml") {
            $scope.clientEdit.protocol = "saml";
        }
    };

    $scope.changeAlgorithm = function() {
        $scope.clientEdit.attributes['saml.signature.algorithm'] = $scope.signatureAlgorithm;
    };

    $scope.changeNameIdFormat = function() {
        $scope.clientEdit.attributes['saml_name_id_format'] = $scope.nameIdFormat;
    };

    $scope.changeSamlSigKeyNameTranformer = function() {
        $scope.clientEdit.attributes['saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer'] = $scope.samlXmlKeyNameTranformer;
    };

    $scope.changeUserInfoSignedResponseAlg = function() {
        if ($scope.userInfoSignedResponseAlg === 'unsigned') {
            $scope.clientEdit.attributes['user.info.response.signature.alg'] = null;
        } else {
            $scope.clientEdit.attributes['user.info.response.signature.alg'] = $scope.userInfoSignedResponseAlg;
        }
    };

    $scope.changeRequestObjectSignatureAlg = function() {
        if ($scope.requestObjectSignatureAlg === 'any') {
            $scope.clientEdit.attributes['request.object.signature.alg'] = null;
        } else {
            $scope.clientEdit.attributes['request.object.signature.alg'] = $scope.requestObjectSignatureAlg;
        }
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    function isChanged() {
        if (!angular.equals($scope.client, $scope.clientEdit)) {
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

    function configureAuthorizationServices() {
        if ($scope.clientEdit.authorizationServicesEnabled) {
            if ($scope.accessType == 'public') {
                $scope.accessType = 'confidential';
            }
            $scope.clientEdit.publicClient = false;
            $scope.clientEdit.serviceAccountsEnabled = true;
        } else if ($scope.clientEdit.bearerOnly) {
            $scope.clientEdit.serviceAccountsEnabled = false;
        }
        if ($scope.client.authorizationServicesEnabled && !$scope.clientEdit.authorizationServicesEnabled) {
            Dialog.confirm("Disable Authorization Settings", "Are you sure you want to disable authorization ? Once you save your changes, all authorization settings associated with this client will be removed. This operation can not be reverted.", function () {
            }, function () {
                $scope.clientEdit.authorizationServicesEnabled = true;
            });
        }
    }

    $scope.$watch('clientEdit', function() {
        $scope.changed = isChanged();
        configureAuthorizationServices();
    }, true);

    $scope.$watch('newRedirectUri', function() {
        $scope.changed = isChanged();
    }, true);


    $scope.$watch('newWebOrigin', function() {
        $scope.changed = isChanged();
    }, true);

    $scope.deleteWebOrigin = function(index) {
        $scope.clientEdit.webOrigins.splice(index, 1);
    }
    $scope.addWebOrigin = function() {
        $scope.clientEdit.webOrigins.push($scope.newWebOrigin);
        $scope.newWebOrigin = "";
    }
    $scope.deleteRedirectUri = function(index) {
        $scope.clientEdit.redirectUris.splice(index, 1);
    }

    $scope.addRedirectUri = function() {
        $scope.clientEdit.redirectUris.push($scope.newRedirectUri);
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
            $scope.clientEdit.attributes["saml.server.signature"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.server.signature"] = "false";
        }
        if ($scope.samlServerSignatureEnableKeyInfoExtension == true) {
            $scope.clientEdit.attributes["saml.server.signature.keyinfo.ext"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.server.signature.keyinfo.ext"] = "false";
        }
        if ($scope.samlAssertionSignature == true) {
            $scope.clientEdit.attributes["saml.assertion.signature"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.assertion.signature"] = "false";
        }
        if ($scope.samlClientSignature == true) {
            $scope.clientEdit.attributes["saml.client.signature"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.client.signature"] = "false";

        }
        if ($scope.samlEncrypt == true) {
            $scope.clientEdit.attributes["saml.encrypt"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.encrypt"] = "false";

        }
        if ($scope.samlAuthnStatement == true) {
            $scope.clientEdit.attributes["saml.authnstatement"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.authnstatement"] = "false";

        }
        if ($scope.samlOneTimeUseCondition == true) {
                    $scope.clientEdit.attributes["saml.onetimeuse.condition"] = "true";
                } else {
                    $scope.clientEdit.attributes["saml.onetimeuse.condition"] = "false";

                }
        if ($scope.samlForceNameIdFormat == true) {
            $scope.clientEdit.attributes["saml_force_name_id_format"] = "true";
        } else {
            $scope.clientEdit.attributes["saml_force_name_id_format"] = "false";

        }
        if ($scope.samlMultiValuedRoles == true) {
            $scope.clientEdit.attributes["saml.multivalued.roles"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.multivalued.roles"] = "false";

        }
        if ($scope.samlForcePostBinding == true) {
            $scope.clientEdit.attributes["saml.force.post.binding"] = "true";
        } else {
            $scope.clientEdit.attributes["saml.force.post.binding"] = "false";

        }

        $scope.clientEdit.protocol = $scope.protocol;
        $scope.clientEdit.attributes['saml.signature.algorithm'] = $scope.signatureAlgorithm;
        $scope.clientEdit.attributes['saml_name_id_format'] = $scope.nameIdFormat;

        if ($scope.clientEdit.protocol != 'saml' && !$scope.clientEdit.bearerOnly && ($scope.clientEdit.standardFlowEnabled || $scope.clientEdit.implicitFlowEnabled) && (!$scope.clientEdit.redirectUris || $scope.clientEdit.redirectUris.length == 0)) {
            Notifications.error("You must specify at least one redirect uri");
        } else {
            Client.update({
                realm : realm.realm,
                client : client.id
            }, $scope.clientEdit, function() {
                $route.reload();
                Notifications.success("Your changes have been saved to the client.");
            });
        }
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/clients");
    };
});

module.controller('CreateClientCtrl', function($scope, realm, client, templates, $route, serverInfo, Client, ClientDescriptionConverter, $location, $modal, Dialog, Notifications) {
    $scope.protocols = serverInfo.listProviderIds('login-protocol');
    $scope.create = true;
    $scope.templates = [ {name:'NONE'}];
    var templateNameMap = new Object();
    for (var i = 0; i < templates.length; i++) {
        var template = templates[i];
        templateNameMap[template.name] = template;
        $scope.templates.push(template);
    }

    $scope.realm = realm;

    $scope.client = {
        enabled: true,
        attributes: {}
    };
    $scope.client.redirectUris = [];
    $scope.protocol = $scope.protocols[0];


    $scope.importFile = function(fileContent){
        console.debug(fileContent);
        ClientDescriptionConverter.save({
            realm: realm.realm
        }, fileContent, function (data) {
            $scope.client = data;
            if (data.protocol) {
                $scope.protocol = data.protocol;
            }
            $scope.importing = true;
        });
    };

    $scope.viewImportDetails = function() {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'ObjectModalCtrl',
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

    $scope.changeTemplate = function() {
        if ($scope.client.clientTemplate == 'NONE') {
            $scope.protocol = 'openid-connect';
            $scope.client.protocol = 'openid-connect';
            $scope.client.clientTemplate = null;

        } else {
            var template = templateNameMap[$scope.client.clientTemplate];
            $scope.protocol = template.protocol;
            $scope.client.protocol = template.protocol;
        }
    }
    $scope.changeProtocol = function() {
        if ($scope.protocol == "openid-connect") {
            $scope.client.protocol = "openid-connect";
        } else if ($scope.protocol == "saml") {
            $scope.client.protocol = "saml";
        }
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
        return false;
    }

    $scope.$watch('client', function() {
        $scope.changed = isChanged();
    }, true);


    $scope.save = function() {
        $scope.client.protocol = $scope.protocol;

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
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/clients");
    };
});

module.controller('ClientScopeMappingCtrl', function($scope, $http, realm, client, clients, templates, Notifications,
                                                          Client, ClientTemplate,
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

    if (client.clientTemplate) {
        for (var i = 0; i < templates.length; i++) {
            if (templates[i].name == client.clientTemplate) {
                ClientTemplate.get({realm: realm.realm, template: templates[i].id}, function(data) {
                    $scope.template = data;
                });
                break;
            }
        }

    }

    $scope.hideRoleSelector = function() {
       return ($scope.client.useTemplateScope && $scope.template && template.fullScopeAllowed)
               || (!$scope.template && $scope.client.fullScopeAllowed);
    }

    $scope.changeFlag = function() {
        console.log('changeFlag');
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
            roles).then(function() {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        var roles = $scope.selectedRealmMappings;
        $scope.selectedRealmMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/realm',
            {data : roles, headers : {"content-type" : "application/json"}}).then(function () {
                updateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        var roles = $scope.selectedClientRoles;
        $scope.selectedClientRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/clients/' + $scope.targetClient.id,
                roles).then(function () {
                updateClientRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        var roles = $scope.selectedClientMappings;
        $scope.selectedClientMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/clients/' + $scope.targetClient.id,
            {data : roles, headers : {"content-type" : "application/json"}}).then(function () {
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

module.controller('ClientClusteringNodeCtrl', function($scope, client, Client, ClientClusterNode, realm,
                                                       $location, $routeParams, Notifications, Dialog) {
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
        Dialog.confirmDelete($scope.node.host, 'node', function() {
            ClientClusterNode.remove({ realm : realm.realm, client : client.id , node: $scope.node.host }, function() {
                Notifications.success('Node ' + $scope.node.host + ' unregistered successfully.');
                $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/clustering');
            });
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
                   toAdd).then(function() {
                Notifications.success("Mappers added");
                $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/mappers');
            }).catch(function() {
                Notifications.error("Error adding mappers");
                $location.url('/realms/' + realm.realm + '/clients/' + client.id +  '/mappers');
            });
    };

});

module.controller('ClientProtocolMapperListCtrl', function($scope, realm, client, templates, serverInfo,
                                                           Client,
                                                           ClientProtocolMappersByProtocol, ClientProtocolMapper,
                                                           $route, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }
    if (client.clientTemplate) {
        for (var i = 0; i < templates.length; i++) {
            if (client.clientTemplate == templates[i].name) {
                $scope.template = templates[i];
                break;
            }
        }
    }
    $scope.changeFlag = function() {
        Client.update({
            realm : realm.realm,
            client : client.id
        }, $scope.client, function() {
            $scope.changed = false;
            client = angular.copy($scope.client);
            Notifications.success("Client updated.");
        });
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

module.controller('ClientProtocolMapperCtrl', function($scope, realm, serverInfo, client, clients, mapper, ClientProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.clients = clients;

    /*
    $scope.client = client;
    $scope.create = false;
    $scope.protocol = client.protocol;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    */

    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }

    $scope.model = {
        realm: realm,
        client: client,
        create: false,
        protocol: client.protocol,
        mapper: angular.copy(mapper),
        changed: false
    };

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    for (var i = 0; i < protocolMappers.length; i++) {
        if (protocolMappers[i].id === mapper.protocolMapper) {
            $scope.model.mapperType = protocolMappers[i];
        }
    }
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('model.mapper', function() {
        if (!angular.equals($scope.model.mapper, mapper)) {
            $scope.model.changed = true;
        }
    }, true);

    $scope.save = function() {
        ClientProtocolMapper.update({
            realm : realm.realm,
            client: client.id,
            id : $scope.model.mapper.id
        }, $scope.model.mapper, function() {
            $scope.model.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/clients/' + client.id + "/mappers/" + $scope.model.mapper.id);
            Notifications.success("Your changes have been saved.");
        });
    };

    $scope.reset = function() {
        $scope.model.mapper = angular.copy(mapper);
        $scope.model.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.model.mapper.name, 'mapper', function() {
            ClientProtocolMapper.remove({ realm: realm.realm, client: client.id, id : $scope.model.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/clients/' + client.id + "/mappers");
            });
        });
    };

});

module.controller('ClientProtocolMapperCreateCtrl', function($scope, realm, serverInfo, client, clients, ClientProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.clients = clients;

    if (client.protocol == null) {
        client.protocol = 'openid-connect';
    }
    var protocol = client.protocol;
    /*
    $scope.client = client;
    $scope.create = true;
    $scope.protocol = protocol;
    $scope.mapper = { protocol :  client.protocol, config: {}};
    $scope.mapperTypes = serverInfo.protocolMapperTypes[protocol];
    */
    $scope.model = {
        realm: realm,
        client: client,
        create: true,
        protocol: client.protocol,
        mapper: { protocol :  client.protocol, config: {}},
        changed: false,
        mapperTypes: serverInfo.protocolMapperTypes[protocol]
    };

    // apply default configurations on change for selected protocolmapper type.
    $scope.$watch('model.mapperType', function() {
        var currentMapperType = $scope.model.mapperType;
        var defaultConfig = {};

        if (currentMapperType && Array.isArray(currentMapperType.properties)) {
            for (var i = 0; i < currentMapperType.properties.length; i++) {
                var property = currentMapperType.properties[i];
                if (property && property.name && property.defaultValue) {
                    defaultConfig[property.name] = property.defaultValue;
                }
            }
        }

        $scope.model.mapper.config = defaultConfig;
    }, true);

    $scope.model.mapperType = $scope.model.mapperTypes[0];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.model.mapper.protocolMapper = $scope.model.mapperType.id;
        ClientProtocolMapper.save({
            realm : realm.realm, client: client.id
        }, $scope.model.mapper, function(data, headers) {
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

module.controller('ClientTemplateTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeClientTemplate = function() {
        Dialog.confirmDelete($scope.template.name, 'client template', function() {
            $scope.template.$remove({
                realm : Current.realm.realm,
                template : $scope.template.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/client-templates");
                Notifications.success("The client template has been deleted.");
            });
        });
    };
});



module.controller('ClientTemplateListCtrl', function($scope, realm, templates, ClientTemplate, serverInfo, $route, Dialog, Notifications, $location) {
    $scope.realm = realm;
    $scope.templates = templates;

    $scope.removeClientTemplate = function(template) {
        Dialog.confirmDelete(template.name, 'client template', function() {
            ClientTemplate.remove({
                realm : realm.realm,
                template : template.id
            }, function() {
                $route.reload();
                Notifications.success("The client template been deleted.");
            });
        });
    };
});

module.controller('ClientTemplateDetailCtrl', function($scope, realm, template, $route, serverInfo, ClientTemplate, $location, $modal, Dialog, Notifications) {
    $scope.protocols = serverInfo.listProviderIds('login-protocol');

    $scope.realm = realm;
    $scope.create = !template.name;

    function updateProperties() {
        if ($scope.template.protocol) {
            $scope.protocol = $scope.protocols[$scope.protocols.indexOf($scope.template.protocol)];
        } else {
            $scope.protocol = $scope.protocols[0];
        }
    }

    if (!$scope.create) {
        $scope.template = angular.copy(template);
        updateProperties();
    } else {
        $scope.template = {
        };
        $scope.protocol = $scope.protocols[0];
    }


    $scope.switchChange = function() {
        $scope.changed = true;
    }

    $scope.changeProtocol = function() {
        if ($scope.protocol == "openid-connect") {
            $scope.template.protocol = "openid-connect";
        } else if ($scope.protocol == "saml") {
            $scope.template.protocol = "saml";
        }
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    function isChanged() {
        if (!angular.equals($scope.template, template)) {
            return true;
        }
        return false;
    }

    $scope.$watch('template', function() {
        $scope.changed = isChanged();
    }, true);

    $scope.save = function() {
        $scope.template.protocol = $scope.protocol;

        if ($scope.create) {
            ClientTemplate.save({
                realm: realm.realm,
                template: ''
            }, $scope.template, function (data, headers) {
                $scope.changed = false;
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/client-templates/" + id);
                Notifications.success("The client template has been created.");
            });
        } else {
            ClientTemplate.update({
                realm : realm.realm,
                template : template.id
            }, $scope.template, function() {
                $scope.changed = false;
                template = angular.copy($scope.template);
                $location.url("/realms/" + realm.realm + "/client-templates/" + template.id);
                Notifications.success("Your changes have been saved to the client template.");
            });
        }
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/client-templates");
    };
});

module.controller('ClientTemplateProtocolMapperListCtrl', function($scope, realm, template, serverInfo,
                                                           ClientTemplateProtocolMappersByProtocol, ClientTemplateProtocolMapper,
                                                           $route, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.template = template;
    if (template.protocol == null) {
        template.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[template.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.removeMapper = function(mapper) {
        console.debug(mapper);
        Dialog.confirmDelete(mapper.name, 'mapper', function() {
            ClientTemplateProtocolMapper.remove({ realm: realm.realm, template: template.id, id : mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $route.reload();
            });
        });
    };

    var updateMappers = function() {
        $scope.mappers = ClientTemplateProtocolMappersByProtocol.query({realm : realm.realm, template : template.id, protocol : template.protocol});
    };

    updateMappers();
});

module.controller('ClientTemplateProtocolMapperCtrl', function($scope, realm, serverInfo, template, mapper, clients, ClientTemplateProtocolMapper, Notifications, Dialog, $location, $route) {
    $scope.realm = realm;
    $scope.clients = clients;

    if (template.protocol == null) {
        template.protocol = 'openid-connect';
    }

    $scope.model = {
        realm: realm,
        template: template,
        create: false,
        protocol: template.protocol,
        mapper: angular.copy(mapper),
        changed: false
    }

    var protocolMappers = serverInfo.protocolMapperTypes[template.protocol];
    for (var i = 0; i < protocolMappers.length; i++) {
        if (protocolMappers[i].id == mapper.protocolMapper) {
            $scope.model.mapperType = protocolMappers[i];
        }
    }
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('model.mapper', function() {
        if (!angular.equals($scope.model.mapper, mapper)) {
            $scope.model.changed = true;
        }
    }, true);

    $scope.save = function() {
        ClientTemplateProtocolMapper.update({
            realm : realm.realm,
            template: template.id,
            id : mapper.id
        }, $scope.model.mapper, function() {
            $route.reload();
            Notifications.success("Your changes have been saved.");
        });
    };

    $scope.reset = function() {
        $scope.model.mapper = angular.copy(mapper);
        $scope.model.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.model.mapper.name, 'mapper', function() {
            ClientTemplateProtocolMapper.remove({ realm: realm.realm, template: template.id, id : $scope.model.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/client-templates/' + template.id + "/mappers");
            });
        });
    };

});

module.controller('ClientTemplateProtocolMapperCreateCtrl', function($scope, realm, serverInfo, template, clients, ClientTemplateProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.clients = clients;

    if (template.protocol == null) {
        template.protocol = 'openid-connect';
    }
    var protocol = template.protocol;
    $scope.model = {
        realm: realm,
        template: template,
        create: true,
        protocol: template.protocol,
        mapper: { protocol :  template.protocol, config: {}},
        changed: false,
        mapperTypes: serverInfo.protocolMapperTypes[protocol]
    }

    $scope.model.mapperType = $scope.model.mapperTypes[0];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.model.mapper.protocolMapper = $scope.model.mapperType.id;
        ClientTemplateProtocolMapper.save({
            realm : realm.realm, template: template.id
        }, $scope.model.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/client-templates/' + template.id + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});

module.controller('ClientTemplateAddBuiltinProtocolMapperCtrl', function($scope, realm, template, serverInfo,
                                                           ClientTemplateProtocolMappersByProtocol,
                                                           $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.template = template;
    if (template.protocol == null) {
        template.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[template.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;




    var updateMappers = function() {
        var clientMappers = ClientTemplateProtocolMappersByProtocol.query({realm : realm.realm, template : template.id, protocol : template.protocol}, function() {
            var builtinMappers = serverInfo.builtinProtocolMappers[template.protocol];
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
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/client-templates/' + template.id + '/protocol-mappers/add-models',
            toAdd).then(function() {
                Notifications.success("Mappers added");
                $location.url('/realms/' + realm.realm + '/client-templates/' + template.id +  '/mappers');
            }).catch(function() {
                Notifications.error("Error adding mappers");
                $location.url('/realms/' + realm.realm + '/client-templates/' + template.id +  '/mappers');
            });
    };

});


module.controller('ClientTemplateScopeMappingCtrl', function($scope, $http, realm, template, clients, Notifications,
                                                     ClientTemplate,
                                                     ClientTemplateRealmScopeMapping, ClientTemplateClientScopeMapping, ClientRole,
                                                     ClientTemplateAvailableRealmScopeMapping, ClientTemplateAvailableClientScopeMapping,
                                                     ClientTemplateCompositeRealmScopeMapping, ClientTemplateCompositeClientScopeMapping) {
    $scope.realm = realm;
    $scope.template = angular.copy(template);
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
        ClientTemplate.update({
            realm : realm.realm,
            template : template.id
        }, $scope.template, function() {
            $scope.changed = false;
            template = angular.copy($scope.template);
            updateTemplateRealmRoles();
            Notifications.success("Scope mappings updated.");
        });
    }



    function updateTemplateRealmRoles() {
        $scope.realmRoles = ClientTemplateAvailableRealmScopeMapping.query({realm : realm.realm, template : template.id});
        $scope.realmMappings = ClientTemplateRealmScopeMapping.query({realm : realm.realm, template : template.id});
        $scope.realmComposite = ClientTemplateCompositeRealmScopeMapping.query({realm : realm.realm, template : template.id});
    }

    function updateTemplateClientRoles() {
        if ($scope.targetClient) {
            $scope.clientRoles = ClientTemplateAvailableClientScopeMapping.query({realm : realm.realm, template : template.id, targetClient : $scope.targetClient.id});
            $scope.clientMappings = ClientTemplateClientScopeMapping.query({realm : realm.realm, template : template.id, targetClient : $scope.targetClient.id});
            $scope.clientComposite = ClientTemplateCompositeClientScopeMapping.query({realm : realm.realm, template : template.id, targetClient : $scope.targetClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
    }

    $scope.changeClient = function() {
        updateTemplateClientRoles();
    };

    $scope.addRealmRole = function() {
        var roles = $scope.selectedRealmRoles;
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/client-templates/' + template.id + '/scope-mappings/realm',
            roles).then(function() {
                updateTemplateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        var roles = $scope.selectedRealmMappings;
        $scope.selectedRealmMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/client-templates/' + template.id +  '/scope-mappings/realm',
            {data : roles, headers : {"content-type" : "application/json"}}).then(function () {
                updateTemplateRealmRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        var roles = $scope.selectedClientRoles;
        $scope.selectedClientRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/client-templates/' + template.id +  '/scope-mappings/clients/' + $scope.targetClient.id,
            roles).then(function () {
                updateTemplateClientRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        var roles = $scope.selectedClientMappings;
        $scope.selectedClientMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/client-templates/' + template.id +  '/scope-mappings/clients/' + $scope.targetClient.id,
            {data : roles, headers : {"content-type" : "application/json"}}).then(function () {
                updateTemplateClientRoles();
                Notifications.success("Scope mappings updated.");
            });
    };

    updateTemplateRealmRoles();
});
