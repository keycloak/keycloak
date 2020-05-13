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

module.controller('ClientRoleListCtrl', function($scope, $route, realm, client, ClientRoleList, RoleById, Notifications, Dialog) {
    $scope.realm = realm;
    $scope.roles = [];
    $scope.client = client;

    $scope.query = {
        realm: realm.realm,
        client: $scope.client.id,
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

        $scope.roles = ClientRoleList.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.searchQuery();

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
            case 'client-secret-jwt':
                $scope.clientAuthenticatorConfigPartial = 'client-credentials-secret-jwt.html';
                break;
            case 'client-x509':
                $scope.clientAuthenticatorConfigPartial = 'client-credentials-x509.html';
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

module.controller('ClientSecretCtrl', function($scope, $location, Client, ClientSecret, Notifications) {
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

    $scope.tokenEndpointAuthSigningAlg = $scope.client.attributes['token.endpoint.auth.signing.alg'];

    $scope.switchChange = function() {
        $scope.changed = true;
    }

    $scope.save = function() {
        $scope.client.attributes['token.endpoint.auth.signing.alg'] = $scope.tokenEndpointAuthSigningAlg;

        Client.update({
            realm : $scope.realm.realm,
            client : $scope.client.id
        }, $scope.client, function() {
            $scope.changed = false;
            $scope.clientCopy = angular.copy($scope.client);
            Notifications.success("Client authentication configuration has been saved to the client.");
        });
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

module.controller('ClientX509Ctrl', function($scope, $location, Client, Notifications) {
    console.log('ClientX509Ctrl invoked');

    $scope.clientCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch('client', function() {
        if (!angular.equals($scope.client, $scope.clientCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if (!$scope.client.attributes["x509.subjectdn"]) {
            Notifications.error("The SubjectDN must not be empty.");
        } else {
            Client.update({
                realm : $scope.realm.realm,
                client : $scope.client.id
            }, $scope.client, function() {
                $scope.changed = false;
                $scope.clientCopy = angular.copy($scope.client);
                Notifications.success("Client authentication configuration has been saved to the client.");
            }, function() {
                Notifications.error("The SubjectDN was not changed due to a problem.");
                $scope.subjectdn = "error";
            });
        }
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.reset = function() {
        $scope.client.attributes["x509.subjectdn"] = $scope.clientCopy.attributes["x509.subjectdn"];
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

    $scope.tokenEndpointAuthSigningAlg = $scope.client.attributes['token.endpoint.auth.signing.alg'];

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
        $scope.client.attributes['token.endpoint.auth.signing.alg'] = $scope.tokenEndpointAuthSigningAlg;

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

module.controller('ClientRoleDetailCtrl', function($scope, $route, realm, client, role, roles, Client,
                                                        Role, ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
                                                        $http, $location, Dialog, Notifications, ComponentUtils) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;
    
    $scope.save = function() {
        convertAttributeValuesToLists();
        if ($scope.create) {
            ClientRole.save({
                realm: realm.realm,
                client : client.id
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                convertAttributeValuesToString($scope.role);
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
            }
        }
    }

    roleControl($scope, $route, realm, role, roles, Client,
        ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
        $http, $location, Notifications, Dialog, ComponentUtils);

});

module.controller('ClientRoleMembersCtrl', function($scope, realm, client, role, ClientRoleMembership, Dialog, Notifications, $location) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.role = role;
    $scope.client = client;

    $scope.query = {
        realm: realm.realm,
        role: role.name,
        client: client.id,
        max : 5,
        first : 0
    }

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

        $scope.users = ClientRoleMembership.query($scope.query, function() {
            console.log('search loaded');
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.searchQuery();
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


module.controller('ClientListCtrl', function($scope, realm, Client, ClientListSearchState, $route, Dialog, Notifications) {
    $scope.init = function() {
        $scope.realm = realm;
        $scope.searchLoaded = true;

        ClientListSearchState.query.realm = realm.realm;
        $scope.query = ClientListSearchState.query;

        if (!ClientListSearchState.isFirstSearch) { 
            $scope.searchQuery();
        } else {
            $scope.query.clientId = null;
            $scope.firstPage();
        }
    };
    
    $scope.searchQuery = function() {
        console.log("query.search: ", $scope.query);
        $scope.searchLoaded = false;

        $scope.clients = Client.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            ClientListSearchState.isFirstSearch = false;
        });
    };

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

        if (clientCopy.protocolMappers) {
            for (var i = 0; i < clientCopy.protocolMappers.length; i++) {
                delete clientCopy.protocolMappers[i].id;
            }
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


module.controller('ClientDetailCtrl', function($scope, realm, client, flows, $route, serverInfo, Client, ClientDescriptionConverter, Components, ClientStorageOperations, $location, $modal, Dialog, Notifications, TimeUnit2) {
    $scope.serverInfo = serverInfo;
    $scope.flows = [];
    $scope.clientFlows = [];
    var emptyFlow = {
        id: "",
        alias: ""
    }
    for (var i=0 ; i<flows.length ; i++) {
        if (flows[i].providerId == 'client-flow') {
            $scope.clientFlows.push(flows[i]);
        } else {
            $scope.flows.push(flows[i]);
        }
    }
    $scope.flows.push(emptyFlow)
    $scope.clientFlows.push(emptyFlow)



    $scope.accessTypes = [
        "confidential",
        "public",
        "bearer-only"
    ];

    $scope.protocols = serverInfo.listProviderIds('login-protocol');

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

    $scope.requestObjectRequiredOptions = [
        "not required",
        "request or request_uri",
        "request only",
        "request_uri only"
    ];

    $scope.changePkceCodeChallengeMethodOptions = [
        "S256",
        "plain",
        ""
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
    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
    $scope.tlsClientCertificateBoundAccessTokens = false;

    $scope.accessTokenLifespan = TimeUnit2.asUnit(client.attributes['access.token.lifespan']);
    $scope.samlAssertionLifespan = TimeUnit2.asUnit(client.attributes['saml.assertion.lifespan']);
    $scope.clientSessionIdleTimeout = TimeUnit2.asUnit(client.attributes['client.session.idle.timeout']);
    $scope.clientSessionMaxLifespan = TimeUnit2.asUnit(client.attributes['client.session.max.lifespan']);
    $scope.clientOfflineSessionIdleTimeout = TimeUnit2.asUnit(client.attributes['client.offline.session.idle.timeout']);
    $scope.clientOfflineSessionMaxLifespan = TimeUnit2.asUnit(client.attributes['client.offline.session.max.lifespan']);

    if(client.origin) {
        if ($scope.access.viewRealm) {
            Components.get({realm: realm.realm, componentId: client.origin}, function (link) {
                $scope.originName = link.name;
                //$scope.originLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
            })
        }
        else {
            // KEYCLOAK-4328
            ClientStorageOperations.simpleName.get({realm: realm.realm, componentId: client.origin}, function (link) {
                $scope.originName = link.name;
                //$scope.originLink = $location.absUrl();
            })
        }
    } else {
        console.log("origin is null");
    }


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

        $scope.accessTokenSignedResponseAlg = $scope.client.attributes['access.token.signed.response.alg'];
        $scope.idTokenSignedResponseAlg = $scope.client.attributes['id.token.signed.response.alg'];
        $scope.idTokenEncryptedResponseAlg = $scope.client.attributes['id.token.encrypted.response.alg'];
        $scope.idTokenEncryptedResponseEnc = $scope.client.attributes['id.token.encrypted.response.enc'];

        var attrVal1 = $scope.client.attributes['user.info.response.signature.alg'];
        $scope.userInfoSignedResponseAlg = attrVal1==null ? 'unsigned' : attrVal1;

        var attrVal2 = $scope.client.attributes['request.object.signature.alg'];
        $scope.requestObjectSignatureAlg = attrVal2==null ? 'any' : attrVal2;

        var attrVal3 = $scope.client.attributes['request.object.required'];
        $scope.requestObjectRequired = attrVal3==null ? 'not required' : attrVal3;

        var attrVal4 = $scope.client.attributes['pkce.code.challenge.method'];
        $scope.pkceCodeChallengeMethod = attrVal4==null ? 'none' : attrVal4;

        if ($scope.client.attributes["exclude.session.state.from.auth.response"]) {
            if ($scope.client.attributes["exclude.session.state.from.auth.response"] == "true") {
                $scope.excludeSessionStateFromAuthResponse = true;
            } else {
                $scope.excludeSessionStateFromAuthResponse = false;
            }
        }

        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
       if ($scope.client.attributes["tls.client.certificate.bound.access.tokens"]) {
           if ($scope.client.attributes["tls.client.certificate.bound.access.tokens"] == "true") {
               $scope.tlsClientCertificateBoundAccessTokens = true;
           } else {
               $scope.tlsClientCertificateBoundAccessTokens = false;
           }
       }

        if ($scope.client.attributes["display.on.consent.screen"]) {
            if ($scope.client.attributes["display.on.consent.screen"] == "true") {
                $scope.displayOnConsentScreen = true;
            } else {
                $scope.displayOnConsentScreen = false;
            }
        }
    }

    if (!$scope.create) {
        $scope.client = client;
        updateProperties();

        $scope.clientEdit = angular.copy(client);
    }


    $scope.samlIdpInitiatedUrl = function(ssoName) {
        return encodeURI($location.absUrl().replace(/\/admin.*/, "/realms/") + realm.realm + "/protocol/saml/clients/") + encodeURIComponent(ssoName)
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
            $scope.clientEdit.alwaysDisplayInConsole = false;
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

    $scope.changeAccessTokenSignedResponseAlg = function() {
        $scope.clientEdit.attributes['access.token.signed.response.alg'] = $scope.accessTokenSignedResponseAlg;
    };

    $scope.changeIdTokenSignedResponseAlg = function() {
        $scope.clientEdit.attributes['id.token.signed.response.alg'] = $scope.idTokenSignedResponseAlg;
    };

    $scope.changeIdTokenEncryptedResponseAlg = function() {
        $scope.clientEdit.attributes['id.token.encrypted.response.alg'] = $scope.idTokenEncryptedResponseAlg;
    };

    $scope.changeIdTokenEncryptedResponseEnc = function() {
        $scope.clientEdit.attributes['id.token.encrypted.response.enc'] = $scope.idTokenEncryptedResponseEnc;
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

    $scope.changeRequestObjectRequired = function() {
        if ($scope.requestObjectRequired === 'not required') {
            $scope.clientEdit.attributes['request.object.required'] = null;
        } else {
            $scope.clientEdit.attributes['request.object.required'] = $scope.requestObjectRequired;
        }
    };

    $scope.changePkceCodeChallengeMethod = function() {
        $scope.clientEdit.attributes['pkce.code.challenge.method'] = $scope.pkceCodeChallengeMethod;
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

    $scope.updateTimeouts = function() {
        if ($scope.accessTokenLifespan.time) {
            if ($scope.accessTokenLifespan.time === -1) {
                $scope.clientEdit.attributes['access.token.lifespan'] = -1;
            } else {
                $scope.clientEdit.attributes['access.token.lifespan'] = $scope.accessTokenLifespan.toSeconds();
            }
        } else {
            $scope.clientEdit.attributes['access.token.lifespan'] = null;
        }
    }

    $scope.updateAssertionLifespan = function() {
        if ($scope.samlAssertionLifespan.time) {
            $scope.clientEdit.attributes['saml.assertion.lifespan'] = $scope.samlAssertionLifespan.toSeconds();
        } else {
            $scope.clientEdit.attributes['saml.assertion.lifespan'] = null;
        }
    }

    $scope.updateClientSessionIdleTimeout = function() {
        if ($scope.clientSessionIdleTimeout.time) {
            $scope.clientEdit.attributes['client.session.idle.timeout'] = $scope.clientSessionIdleTimeout.toSeconds();
        } else {
            $scope.clientEdit.attributes['client.session.idle.timeout'] = null;
        }
    }

    $scope.updateClientSessionMaxLifespan = function() {
        if ($scope.clientSessionMaxLifespan.time) {
            $scope.clientEdit.attributes['client.session.max.lifespan'] = $scope.clientSessionMaxLifespan.toSeconds();
        } else {
            $scope.clientEdit.attributes['client.session.max.lifespan'] = null;
        }
    }

    $scope.updateClientOfflineSessionIdleTimeout = function() {
        if ($scope.clientOfflineSessionIdleTimeout.time) {
            $scope.clientEdit.attributes['client.offline.session.idle.timeout'] = $scope.clientOfflineSessionIdleTimeout.toSeconds();
        } else {
            $scope.clientEdit.attributes['client.offline.session.idle.timeout'] = null;
        }
    }

    $scope.updateClientOfflineSessionMaxLifespan = function() {
        if ($scope.clientOfflineSessionMaxLifespan.time) {
            $scope.clientEdit.attributes['client.offline.session.max.lifespan'] = $scope.clientOfflineSessionMaxLifespan.toSeconds();
        } else {
            $scope.clientEdit.attributes['client.offline.session.max.lifespan'] = null;
        }
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

        if ($scope.excludeSessionStateFromAuthResponse == true) {
            $scope.clientEdit.attributes["exclude.session.state.from.auth.response"] = "true";
        } else {
            $scope.clientEdit.attributes["exclude.session.state.from.auth.response"] = "false";

        }

        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
        if ($scope.tlsClientCertificateBoundAccessTokens == true) {
            $scope.clientEdit.attributes["tls.client.certificate.bound.access.tokens"] = "true";
        } else {
            $scope.clientEdit.attributes["tls.client.certificate.bound.access.tokens"] = "false";
        }

        if ($scope.displayOnConsentScreen == true) {
            $scope.clientEdit.attributes["display.on.consent.screen"] = "true";
        } else {
            $scope.clientEdit.attributes["display.on.consent.screen"] = "false";
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

module.controller('CreateClientCtrl', function($scope, realm, client, $route, serverInfo, Client, ClientDescriptionConverter, $location, $modal, Dialog, Notifications) {
    $scope.protocols = serverInfo.listProviderIds('login-protocol');
    $scope.create = true;

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

module.controller('ClientScopeMappingCtrl', function($scope, $http, realm, $route, client, clients, Notifications,
                                                          Client, ClientScope,
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

    $scope.hideRoleSelector = function() {
       return $scope.client.fullScopeAllowed;
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

    
    $scope.selectedClient = null;

    $scope.selectClient = function(client) {
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }

        $scope.selectedClient = client;
        updateClientRoles();
    }

    function updateRealmRoles() {
        $scope.realmRoles = ClientAvailableRealmScopeMapping.query({realm : realm.realm, client : client.id});
        $scope.realmMappings = ClientRealmScopeMapping.query({realm : realm.realm, client : client.id});
        $scope.realmComposite = ClientCompositeRealmScopeMapping.query({realm : realm.realm, client : client.id});
    }

    function updateClientRoles() {
        if ($scope.selectedClient) {
            $scope.clientRoles = ClientAvailableClientScopeMapping.query({realm : realm.realm, client : client.id, targetClient : $scope.selectedClient.id});
            $scope.clientMappings = ClientClientScopeMapping.query({realm : realm.realm, client : client.id, targetClient : $scope.selectedClient.id});
            $scope.clientComposite = ClientCompositeClientScopeMapping.query({realm : realm.realm, client : client.id, targetClient : $scope.selectedClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
    }

    $scope.addRealmRole = function() {
        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id + '/scope-mappings/realm',
            $scope.selectedRealmRolesToAdd).then(function() {
                updateRealmRoles();
                $scope.selectedRealmRolesToAdd = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        $scope.selectedRealmMappingsToRemove = JSON.parse('[' + $scope.selectedRealmMappings + ']');
        $scope.selectedRealmMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/realm',
            {data : $scope.selectedRealmMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function () {
                updateRealmRoles();
                $scope.selectedRealmMappingsToRemove = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        $scope.selectedClientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $scope.selectedClientRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/clients/' + $scope.selectedClient.id,
                $scope.selectedClientRolesToAdd).then(function () {
                updateClientRoles();
                $scope.selectedClientRolesToAdd = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        $scope.selectedClientMappingsToRemove = JSON.parse('[' + $scope.selectedClientMappings + ']');
        $scope.selectedClientMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/clients/' + client.id +  '/scope-mappings/clients/' + $scope.selectedClient.id,
            {data : $scope.selectedClientMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function () {
                updateClientRoles();
                $scope.selectedClientMappingsToRemove = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
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

module.controller('ClientProtocolMapperListCtrl', function($scope, realm, client, serverInfo,
                                                           Client,
                                                           ClientProtocolMappersByProtocol, ClientProtocolMapper,
                                                           $route, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.client = client;
    if (client.protocol == null) {
        client.protocol = 'openid-connect';
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

    $scope.sortMappersByPriority = function(mapper) {
        return $scope.mapperTypes[mapper.protocolMapper].priority;
    }

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
    console.log("mapper types: ", $scope.model.mapperTypes);

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


module.controller('ClientClientScopesSetupCtrl', function($scope, realm, Realm, client, clientScopes, serverInfo,
        clientDefaultClientScopes, ClientDefaultClientScopes, clientOptionalClientScopes, ClientOptionalClientScopes, $route, Notifications, $location) {
    console.log('ClientClientScopesSetupCtrl');

    $scope.realm = realm;
    $scope.client = client;

    $scope.clientDefaultClientScopes = clientDefaultClientScopes;
    $scope.clientOptionalClientScopes = clientOptionalClientScopes;

    $scope.availableClientScopes = [];
    $scope.selectedDefaultClientScopes = [];
    $scope.selectedDefDefaultClientScopes = [];

    $scope.selectedOptionalClientScopes = [];
    $scope.selectedDefOptionalClientScopes = [];

    // Populate available client scopes. Available client scopes are neither already assigned to 'default' or 'optional'
    for (var i = 0; i < clientScopes.length; i++) {
        var clientScope = clientScopes[i];
        var scopeName = clientScopes[i].name;

        var available = true;
        if (clientScope.protocol != client.protocol) {
            available = false;
        }

        for (var j = 0; j < $scope.clientDefaultClientScopes.length; j++) {
            if (scopeName === $scope.clientDefaultClientScopes[j].name) {
                available = false;
            }
        }
        for (var j = 0; j < $scope.clientOptionalClientScopes.length; j++) {
            if (scopeName === $scope.clientOptionalClientScopes[j].name) {
                available = false;
            }
        }

        if (available) {
            $scope.availableClientScopes.push(clientScope);
        }
    }

    $scope.addDefaultClientScope = function () {
        $scope.selectedDefaultClientScopesToAdd = JSON.parse('[' + $scope.selectedDefaultClientScopes + ']');
        toAdd = $scope.selectedDefaultClientScopesToAdd.length;

        for (var i = 0; i < $scope.selectedDefaultClientScopesToAdd.length; i++) {
            var currentScope = $scope.selectedDefaultClientScopesToAdd[i];

            ClientDefaultClientScopes.update({
                realm : realm.realm,
                client : client.id,
                clientScopeId : currentScope.id
            }, function () {
                toAdd = toAdd - 1;
                if (toAdd === 0) {
                    $route.reload();
                    Notifications.success("Default scopes updated.");
                }
            });
        }
        $scope.selectedDefaultClientScopesToAdd = [];
    };

    $scope.deleteDefaultClientScope = function () {
        $scope.selectedDefDefaultClientScopesToRemove = JSON.parse('[' + $scope.selectedDefDefaultClientScopes + ']');
        toRemove = $scope.selectedDefDefaultClientScopesToRemove.length;

        for (var i = 0; i < $scope.selectedDefDefaultClientScopesToRemove.length; i++) {
            var currentScope = $scope.selectedDefDefaultClientScopesToRemove[i];

            ClientDefaultClientScopes.remove({
                realm : realm.realm,
                client : client.id,
                clientScopeId : currentScope.id
            }, function () {
                toRemove = toRemove - 1;
                if (toRemove === 0) {
                    $route.reload();
                    Notifications.success("Default scopes updated.");
                }
            });
        }
        $scope.selectedDefDefaultClientScopesToRemove = [];
    };

    $scope.addOptionalClientScope = function () {
        $scope.selectedOptionalClientScopesToAdd = JSON.parse('[' + $scope.selectedOptionalClientScopes + ']');
        toAdd = $scope.selectedOptionalClientScopesToAdd.length;

        for (var i = 0; i < $scope.selectedOptionalClientScopesToAdd.length; i++) {
            var currentScope = $scope.selectedOptionalClientScopesToAdd[i];

            ClientOptionalClientScopes.update({
                realm : realm.realm,
                client : client.id,
                clientScopeId : currentScope.id
            }, function () {
                toAdd = toAdd - 1;
                if (toAdd === 0) {
                    $route.reload();
                    Notifications.success("Optional scopes updated.");
                }
            });
        }
    };

    $scope.deleteOptionalClientScope = function () {
        $scope.selectedDefOptionalClientScopesToRemove = JSON.parse('[' + $scope.selectedDefOptionalClientScopes + ']');
        toRemove = $scope.selectedDefOptionalClientScopesToRemove.length;

        for (var i = 0; i < $scope.selectedDefOptionalClientScopesToRemove.length; i++) {
            var currentScope = $scope.selectedDefOptionalClientScopesToRemove[i];

            ClientOptionalClientScopes.remove({
                realm : realm.realm,
                client : client.id,
                clientScopeId : currentScope.id
            }, function () {
                toRemove = toRemove - 1;
                if (toRemove === 0) {
                    $route.reload();
                    Notifications.success("Optional scopes updated.");
                }
            });
        }
        $scope.selectedDefOptionalClientScopesToRemove = [];
    };

});

module.controller('ClientClientScopesEvaluateCtrl', function($scope, Realm, User, ClientEvaluateProtocolMappers, ClientEvaluateGrantedRoles,
        ClientEvaluateNotGrantedRoles, ClientEvaluateGenerateExampleToken, realm, client, clients, clientScopes, serverInfo,
        ComponentUtils, clientOptionalClientScopes, clientDefaultClientScopes, $route, $routeParams, $http, Notifications, $location,
        Client) {

    console.log('ClientClientScopesEvaluateCtrl');

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.realm = realm;
    $scope.client = client;
    $scope.clients = clients;
    $scope.userId = null;

    $scope.availableClientScopes = [];
    $scope.assignedClientScopes = [];
    $scope.selectedClientScopes = [];
    $scope.selectedDefClientScopes = [];
    $scope.effectiveClientScopes = [];

    // Populate available client scopes. Available client scopes are neither already assigned to 'default' or 'optional'
    for (var i = 0; i < clientOptionalClientScopes.length; i++) {
        $scope.availableClientScopes.push(clientOptionalClientScopes[i]);
    }

    function clearEvalResponse() {
        $scope.protocolMappers = null;
        $scope.grantedRealmRoles = null;
        $scope.notGrantedRealmRoles = null;
        $scope.grantedClientRoles = null;
        $scope.notGrantedClientRoles = null;
        $scope.targetClient = null;
        $scope.oidcAccessToken = null;

        $scope.selectedTab = 0;
    }

    function updateState() {
        // Compute scope parameter
        $scope.scopeParam = 'openid';
        for (var i = 0; i < $scope.assignedClientScopes.length; i++) {
            var currentScopeParam = $scope.assignedClientScopes[i].name;
            $scope.scopeParam = $scope.scopeParam + ' ' + currentScopeParam;
        }

        // Compute effective scopes
        $scope.effectiveClientScopes = [];

        for (var i = 0; i < clientDefaultClientScopes.length; i++) {
            var currentScope = clientDefaultClientScopes[i];
            $scope.effectiveClientScopes.push(currentScope);
        }
        for (var i = 0; i < $scope.assignedClientScopes.length; i++) {
            var currentScope = $scope.assignedClientScopes[i];
            $scope.effectiveClientScopes.push(currentScope);
        }

        // Clear the evaluation response
        clearEvalResponse();
    }

    updateState();


    $scope.addAppliedClientScope = function () {
        $scope.selectedClientScopesToAdd = JSON.parse('[' + $scope.selectedClientScopes + ']');
        for (var i = 0; i < $scope.selectedClientScopesToAdd.length; i++) {
            var currentScope = $scope.selectedClientScopesToAdd[i];

            $scope.assignedClientScopes.push(currentScope);

            var index = ComponentUtils.findIndexById($scope.availableClientScopes, currentScope.id);
            if (index > -1) {
                $scope.availableClientScopes.splice(index, 1);
            }
        }

        $scope.selectedClientScopes = [];
        $scope.selectedClientScopesToAdd = [];
        updateState();
    };

    $scope.deleteAppliedClientScope = function () {
        $scope.selectedDefClientScopesToRemove = JSON.parse('[' + $scope.selectedDefClientScopes + ']');
        for (var i = 0; i < $scope.selectedDefClientScopesToRemove.length; i++) {
            var currentScope = $scope.selectedDefClientScopesToRemove[i];

            $scope.availableClientScopes.push(currentScope);

            var index = ComponentUtils.findIndexById($scope.assignedClientScopes, currentScope.id);
            if (index > -1) {
                $scope.assignedClientScopes.splice(index, 1);
            }
        }

        $scope.selectedDefClientScopes = [];
        $scope.selectedDefClientScopesToRemove = [];

        updateState();
    };

    $scope.usersUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            User.query({realm: $route.current.params.realm, search: query.term.trim(), max: 20}, function(response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            object.text = object.username;
            return object.username;
        }
    };

    $scope.selectedUser = null;

    $scope.selectUser = function(user) {
        clearEvalResponse();

        if (!user || !user.id) {
            $scope.selectedUser = null;
            $scope.userId = '';
            return;
        }

        $scope.userId = user.id;
    }

    clientSelectControl($scope, $route.current.params.realm, Client);
    
    $scope.selectedClient = null;

    $scope.selectClient = function(client) {
        console.log("selected client: ", client);
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }

        $scope.selectedClient = client;
        updateScopeClientRoles();
    }


    $scope.sendEvaluationRequest = function () {

        // Send request for retrieve protocolMappers
        $scope.protocolMappers = ClientEvaluateProtocolMappers.query({
            realm: realm.realm,
            client: client.id,
            scopeParam: $scope.scopeParam
        });

        // Send request for retrieve realmRoles
        updateScopeRealmRoles();

        // Send request for retrieve accessToken (in case user was selected)
        if (client.protocol === 'openid-connect' && $scope.userId != null && $scope.userId !== '') {
            var url = ClientEvaluateGenerateExampleToken.url({
                realm: realm.realm,
                client: client.id,
                userId: $scope.userId,
                scopeParam: $scope.scopeParam
            });

            $http.get(url).then(function (response) {
                if (response.data) {
                    var oidcAccessToken = angular.fromJson(response.data);
                    oidcAccessToken = angular.toJson(oidcAccessToken, true);
                    $scope.oidcAccessToken = oidcAccessToken;
                } else {
                    $scope.oidcAccessToken = null;
                }
            });
        }

        $scope.showTab(1);
    };


    $scope.isResponseAvailable = function () {
        return $scope.protocolMappers != null;
    }

    $scope.isTokenAvailable = function () {
        return $scope.oidcAccessToken != null;
    }

    $scope.showTab = function (tab) {
        $scope.selectedTab = tab;

        // Check if there is more clever way to do it... :/
        if (tab === 1) {
            $scope.tabCss = { tab1: 'active', tab2: '', tab3: '' }
        } else if (tab === 2) {
            $scope.tabCss = { tab1: '', tab2: 'active', tab3: '' }
        } else if (tab === 3) {
            $scope.tabCss = { tab1: '', tab2: '', tab3: 'active' }
        }
    }

    $scope.protocolMappersShown = function () {
        return $scope.selectedTab === 1;
    }

    $scope.rolesShown = function () {
        return $scope.selectedTab === 2;
    }

    $scope.tokenShown = function () {
        return $scope.selectedTab === 3;
    }

    $scope.sortMappersByPriority = function(mapper) {
        return $scope.mapperTypes[mapper.protocolMapper].priority;
    }


    // Roles

    function updateScopeRealmRoles() {
        $scope.grantedRealmRoles = ClientEvaluateGrantedRoles.query({
            realm: realm.realm,
            client: client.id,
            roleContainer: realm.realm,
            scopeParam: $scope.scopeParam
        });
        $scope.notGrantedRealmRoles = ClientEvaluateNotGrantedRoles.query({
            realm: realm.realm,
            client: client.id,
            roleContainer: realm.realm,
            scopeParam: $scope.scopeParam
        });
    }

    function updateScopeClientRoles() {
        if ($scope.selectedClient) {
            $scope.grantedClientRoles = ClientEvaluateGrantedRoles.query({
                realm: realm.realm,
                client: client.id,
                roleContainer: $scope.selectedClient.id,
                scopeParam: $scope.scopeParam
            });
            $scope.notGrantedClientRoles = ClientEvaluateNotGrantedRoles.query({
                realm: realm.realm,
                client: client.id,
                roleContainer: $scope.selectedClient.id,
                scopeParam: $scope.scopeParam
            });
        } else {
            $scope.grantedClientRoles = null;
            $scope.notGrantedClientRoles = null;
        }
    }
});


module.controller('ClientScopeTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeClientScope = function() {
        Dialog.confirmDelete($scope.clientScope.name, 'client scope', function() {
            $scope.clientScope.$remove({
                realm : Current.realm.realm,
                clientScope : $scope.clientScope.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/client-scopes");
                Notifications.success("The client scope has been deleted.");
            });
        });
    };
});



module.controller('ClientScopeListCtrl', function($scope, realm, clientScopes, ClientScope, serverInfo, $route, Dialog, Notifications, $location) {
    $scope.realm = realm;
    $scope.clientScopes = clientScopes;

    $scope.removeClientScope = function(clientScope) {
        Dialog.confirmDelete(clientScope.name, 'client scope', function() {
            ClientScope.remove({
                realm : realm.realm,
                clientScope : clientScope.id
            }, function() {
                $route.reload();
                Notifications.success("The client scope been deleted.");
            });
        });
    };
});

module.controller('ClientScopesRealmDefaultCtrl', function($scope, realm, Realm, clientScopes, realmDefaultClientScopes, RealmDefaultClientScopes,
        realmOptionalClientScopes, RealmOptionalClientScopes, serverInfo, $route, Dialog, Notifications, $location) {

    console.log('ClientScopesRealmDefaultCtrl');

    $scope.realm = realm;
    $scope.realmDefaultClientScopes = realmDefaultClientScopes;
    $scope.realmOptionalClientScopes = realmOptionalClientScopes;

    $scope.availableClientScopes = [];
    $scope.selectedDefaultClientScopes = [];
    $scope.selectedDefDefaultClientScopes = [];

    $scope.selectedOptionalClientScopes = [];
    $scope.selectedDefOptionalClientScopes = [];

    // Populate available client scopes. Available client scopes are neither already assigned to 'default' or 'optional'
    for (var i = 0; i < clientScopes.length; i++) {
        var scopeName = clientScopes[i].name;

        var available = true;
        for (var j = 0; j < $scope.realmDefaultClientScopes.length; j++) {
            if (scopeName === $scope.realmDefaultClientScopes[j].name) {
                available = false;
            }
        }
        for (var j = 0; j < $scope.realmOptionalClientScopes.length; j++) {
            if (scopeName === $scope.realmOptionalClientScopes[j].name) {
                available = false;
            }
        }

        if (available) {
            $scope.availableClientScopes.push(clientScopes[i]);
        }
    }

    $scope.addDefaultClientScope = function () {
        $scope.selectedDefaultClientScopesToAdd = JSON.parse('[' + $scope.selectedDefaultClientScopes + ']');
        toAdd = $scope.selectedDefaultClientScopesToAdd.length;

        for (var i = 0; i < $scope.selectedDefaultClientScopesToAdd.length; i++) {
            var currentScope = $scope.selectedDefaultClientScopesToAdd[i];

            RealmDefaultClientScopes.update({
                realm : realm.realm,
                clientScopeId : currentScope.id
            }, function () {
                toAdd = toAdd - 1;
                console.log('toAdd: ' + toAdd);
                if (toAdd === 0) {
                    $route.reload();
                    Notifications.success("Realm default scopes updated.");
                }
            });
        }
        $scope.selectedDefaultClientScopesToAdd = [];
    };

    $scope.deleteDefaultClientScope = function () {
        $scope.selectedDefDefaultClientScopesToRemove = JSON.parse('[' + $scope.selectedDefDefaultClientScopes + ']');
        toRemove = $scope.selectedDefDefaultClientScopesToRemove.length;

        for (var i = 0; i < $scope.selectedDefDefaultClientScopesToRemove.length; i++) {
            var currentScope = $scope.selectedDefDefaultClientScopesToRemove[i];

            RealmDefaultClientScopes.remove({
                realm : realm.realm,
                clientScopeId : currentScope.id
            }, function () {
                toRemove = toRemove - 1;
                if (toRemove === 0) {
                    $route.reload();
                    Notifications.success("Realm default scopes updated.");
                }
            });
        }
        $scope.selectedDefDefaultClientScopesToRemove = [];
    };

    $scope.addOptionalClientScope = function () {
        $scope.selectedOptionalClientScopesToAdd = JSON.parse('[' + $scope.selectedOptionalClientScopes + ']');
        toAdd = $scope.selectedOptionalClientScopesToAdd.length;

        for (var i = 0; i < $scope.selectedOptionalClientScopesToAdd.length; i++) {
            var currentScope = $scope.selectedOptionalClientScopesToAdd[i];

            RealmOptionalClientScopes.update({
                realm : realm.realm,
                clientScopeId : currentScope.id
            }, function () {
                toAdd = toAdd - 1;
                console.log('toAdd: ' + toAdd);
                if (toAdd === 0) {
                    $route.reload();
                    Notifications.success("Realm optional scopes updated.");
                }
            });
        }
        $scope.selectedOptionalClientScopesToAdd = [];
    };

    $scope.deleteOptionalClientScope = function () {
        $scope.selectedDefOptionalClientScopesToRemove = JSON.parse('[' + $scope.selectedDefOptionalClientScopes + ']');
        toRemove = $scope.selectedDefOptionalClientScopesToRemove.length;

        for (var i = 0; i < $scope.selectedDefOptionalClientScopesToRemove.length; i++) {
            var currentScope = $scope.selectedDefOptionalClientScopesToRemove[i];

            RealmOptionalClientScopes.remove({
                realm : realm.realm,
                clientScopeId : currentScope.id
            }, function () {
                toRemove = toRemove - 1;
                if (toRemove === 0) {
                    $route.reload();
                    Notifications.success("Realm optional scopes updated.");
                }
            });
        }
        $scope.selectedDefOptionalClientScopesToRemove = [];
    };
});

module.controller('ClientScopeDetailCtrl', function($scope, realm, clientScope, $route, serverInfo, ClientScope, $location, $modal, Dialog, Notifications) {
    $scope.protocols = serverInfo.listProviderIds('login-protocol');

    $scope.realm = realm;
    $scope.create = !clientScope.name;

    function updateProperties() {
        if (!$scope.clientScope.attributes) {
            $scope.clientScope.attributes = {};
        }

        if ($scope.clientScope.protocol) {
            $scope.protocol = $scope.protocols[$scope.protocols.indexOf($scope.clientScope.protocol)];
        } else {
            $scope.protocol = $scope.protocols[0];
        }

        if ($scope.clientScope.attributes["display.on.consent.screen"]) {
            if ($scope.clientScope.attributes["display.on.consent.screen"] == "true") {
                $scope.displayOnConsentScreen = true;
            } else {
                $scope.displayOnConsentScreen = false;
            }
        } else {
            $scope.displayOnConsentScreen = true;
        }

        if ($scope.clientScope.attributes["include.in.token.scope"]) {
            if ($scope.clientScope.attributes["include.in.token.scope"] == "true") {
                $scope.includeInTokenScope = true;
            } else {
                $scope.includeInTokenScope = false;
            }
        } else {
            $scope.includeInTokenScope = true;
        }
    }

    if (!$scope.create) {
        $scope.clientScope = angular.copy(clientScope);
    } else {
        $scope.clientScope = {};
    }

    updateProperties();


    $scope.switchChange = function() {
        $scope.changed = true;
    }

    $scope.changeProtocol = function() {
        if ($scope.protocol == "openid-connect") {
            $scope.clientScope.protocol = "openid-connect";
        } else if ($scope.protocol == "saml") {
            $scope.clientScope.protocol = "saml";
        }
    };

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    function isChanged() {
        if (!angular.equals($scope.clientScope, clientScope)) {
            return true;
        }
        return false;
    }

    $scope.$watch('clientScope', function() {
        $scope.changed = isChanged();
    }, true);

    $scope.save = function() {
        $scope.clientScope.protocol = $scope.protocol;

        if ($scope.displayOnConsentScreen == true) {
            $scope.clientScope.attributes["display.on.consent.screen"] = "true";
        } else {
            $scope.clientScope.attributes["display.on.consent.screen"] = "false";
        }

        if ($scope.includeInTokenScope == true) {
            $scope.clientScope.attributes["include.in.token.scope"] = "true";
        } else {
            $scope.clientScope.attributes["include.in.token.scope"] = "false";
        }

        if ($scope.create) {
            ClientScope.save({
                realm: realm.realm,
                clientScope: ''
            }, $scope.clientScope, function (data, headers) {
                $scope.changed = false;
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/client-scopes/" + id);
                Notifications.success("The client scope has been created.");
            });
        } else {
            ClientScope.update({
                realm : realm.realm,
                clientScope : clientScope.id
            }, $scope.clientScope, function() {
                $scope.changed = false;
                clientScope = angular.copy($scope.clientScope);
                $location.url("/realms/" + realm.realm + "/client-scopes/" + clientScope.id);
                Notifications.success("Your changes have been saved to the client scope.");
            });
        }
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/client-scopes");
    };
});

module.controller('ClientScopeProtocolMapperListCtrl', function($scope, realm, clientScope, serverInfo,
                                                           ClientScopeProtocolMappersByProtocol, ClientScopeProtocolMapper,
                                                           $route, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.clientScope = clientScope;
    if (clientScope.protocol == null) {
        clientScope.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[clientScope.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.removeMapper = function(mapper) {
        console.debug(mapper);
        Dialog.confirmDelete(mapper.name, 'mapper', function() {
            ClientScopeProtocolMapper.remove({ realm: realm.realm, clientScope: clientScope.id, id : mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $route.reload();
            });
        });
    };

    $scope.sortMappersByPriority = function(mapper) {
        return $scope.mapperTypes[mapper.protocolMapper].priority;
    }

    var updateMappers = function() {
        $scope.mappers = ClientScopeProtocolMappersByProtocol.query({realm : realm.realm, clientScope : clientScope.id, protocol : clientScope.protocol});
    };

    updateMappers();
});

module.controller('ClientScopeProtocolMapperCtrl', function($scope, realm, serverInfo, clientScope, mapper, clients, ClientScopeProtocolMapper, Notifications, Dialog, $location, $route) {
    $scope.realm = realm;
    $scope.clients = clients;

    if (clientScope.protocol == null) {
        clientScope.protocol = 'openid-connect';
    }

    $scope.model = {
        realm: realm,
        clientScope: clientScope,
        create: false,
        protocol: clientScope.protocol,
        mapper: angular.copy(mapper),
        changed: false
    }

    var protocolMappers = serverInfo.protocolMapperTypes[clientScope.protocol];
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
        ClientScopeProtocolMapper.update({
            realm : realm.realm,
            clientScope: clientScope.id,
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
            ClientScopeProtocolMapper.remove({ realm: realm.realm, clientScope: clientScope.id, id : $scope.model.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/client-scopes/' + clientScope.id + "/mappers");
            });
        });
    };

});

module.controller('ClientScopeProtocolMapperCreateCtrl', function($scope, realm, serverInfo, clientScope, clients, ClientScopeProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.clients = clients;

    if (clientScope.protocol == null) {
        clientScope.protocol = 'openid-connect';
    }
    var protocol = clientScope.protocol;
    $scope.model = {
        realm: realm,
        clientScope: clientScope,
        create: true,
        protocol: clientScope.protocol,
        mapper: { protocol :  clientScope.protocol, config: {}},
        changed: false,
        mapperTypes: serverInfo.protocolMapperTypes[protocol]
    }

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
        ClientScopeProtocolMapper.save({
            realm : realm.realm, clientScope: clientScope.id
        }, $scope.model.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/client-scopes/' + clientScope.id + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});

module.controller('ClientScopeAddBuiltinProtocolMapperCtrl', function($scope, realm, clientScope, serverInfo,
                                                           ClientScopeProtocolMappersByProtocol,
                                                           $http, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.clientScope = clientScope;
    if (clientScope.protocol == null) {
        clientScope.protocol = 'openid-connect';
    }

    var protocolMappers = serverInfo.protocolMapperTypes[clientScope.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
        mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;




    var updateMappers = function() {
        var clientMappers = ClientScopeProtocolMappersByProtocol.query({realm : realm.realm, clientScope : clientScope.id, protocol : clientScope.protocol}, function() {
            var builtinMappers = serverInfo.builtinProtocolMappers[clientScope.protocol];
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
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/client-scopes/' + clientScope.id + '/protocol-mappers/add-models',
            toAdd).then(function() {
                Notifications.success("Mappers added");
                $location.url('/realms/' + realm.realm + '/client-scopes/' + clientScope.id +  '/mappers');
            }).catch(function() {
                Notifications.error("Error adding mappers");
                $location.url('/realms/' + realm.realm + '/client-scopes/' + clientScope.id +  '/mappers');
            });
    };

});


module.controller('ClientScopeScopeMappingCtrl', function($scope, $http, $route, realm, clientScope, Notifications,
                                                     ClientScope, Client,
                                                     ClientScopeRealmScopeMapping, ClientScopeClientScopeMapping, ClientRole,
                                                     ClientScopeAvailableRealmScopeMapping, ClientScopeAvailableClientScopeMapping,
                                                     ClientScopeCompositeRealmScopeMapping, ClientScopeCompositeClientScopeMapping) {
    $scope.realm = realm;
    $scope.clientScope = angular.copy(clientScope);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];
    $scope.selectedClient = null;

    function updateScopeRealmRoles() {
        $scope.realmRoles = ClientScopeAvailableRealmScopeMapping.query({realm : realm.realm, clientScope : clientScope.id});
        $scope.realmMappings = ClientScopeRealmScopeMapping.query({realm : realm.realm, clientScope : clientScope.id});
        $scope.realmComposite = ClientScopeCompositeRealmScopeMapping.query({realm : realm.realm, clientScope : clientScope.id});
    }

    function updateScopeClientRoles() {
        if ($scope.selectedClient) {
            $scope.clientRoles = ClientScopeAvailableClientScopeMapping.query({realm : realm.realm, clientScope : clientScope.id, targetClient : $scope.selectedClient.id});
            $scope.clientMappings = ClientScopeClientScopeMapping.query({realm : realm.realm, clientScope : clientScope.id, targetClient : $scope.selectedClient.id});
            $scope.clientComposite = ClientScopeCompositeClientScopeMapping.query({realm : realm.realm, clientScope : clientScope.id, targetClient : $scope.selectedClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
    }

    $scope.changeClient = function(client) {
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        updateScopeClientRoles();
    };

    $scope.addRealmRole = function() {
        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/client-scopes/' + clientScope.id + '/scope-mappings/realm',
            $scope.selectedRealmRolesToAdd).then(function() {
                updateScopeRealmRoles();
                $scope.selectedRealmRolesToAdd = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteRealmRole = function() {
        $scope.selectedRealmMappingsToRemove = JSON.parse('[' + $scope.selectedRealmMappings + ']');
        $scope.selectedRealmMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/client-scopes/' + clientScope.id +  '/scope-mappings/realm',
            {data : $scope.selectedRealmMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function () {
                updateScopeRealmRoles();
                $scope.selectedRealmMappingsToRemove = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        $scope.selectedClientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $scope.selectedClientRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/client-scopes/' + clientScope.id +  '/scope-mappings/clients/' + $scope.selectedClient.id,
            $scope.selectedClientRolesToAdd).then(function () {
                updateScopeClientRoles();
                $scope.selectedClientRolesToAdd = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        $scope.selectedClientMappingsToRemove = JSON.parse('[' + $scope.selectedClientMappings + ']');
        $scope.selectedClientMappings = [];
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/client-scopes/' + clientScope.id +  '/scope-mappings/clients/' + $scope.selectedClient.id,
            {data : $scope.selectedClientMappingsToRemove, headers : {"content-type" : "application/json"}}).then(function () {
                updateScopeClientRoles();
                $scope.selectedClientMappingsToRemove = [];
                Notifications.success("Scope mappings updated.");
            });
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
    updateScopeRealmRoles();
});

module.controller('ClientStoresCtrl', function($scope, $location, $route, realm, serverInfo, Components, Notifications, Dialog) {
    console.log('ClientStoresCtrl ++++****');
    $scope.realm = realm;
    $scope.providers = serverInfo.componentTypes['org.keycloak.storage.client.ClientStorageProvider'];
    $scope.clientStorageProviders = serverInfo.componentTypes['org.keycloak.storage.client.ClientStorageProvider'];
    $scope.instancesLoaded = false;

    if (!$scope.providers) $scope.providers = [];

    $scope.addProvider = function(provider) {
        console.log('Add provider: ' + provider.id);
        $location.url("/create/client-storage/" + realm.realm + "/providers/" + provider.id);
    };

    $scope.getInstanceLink = function(instance) {
        return "/realms/" + realm.realm + "/client-storage/providers/" + instance.providerId + "/" + instance.id;
    }

    $scope.getInstanceName = function(instance) {
        return instance.name;
    }
    $scope.getInstanceProvider = function(instance) {
        return instance.providerId;
    }

    $scope.isProviderEnabled = function(instance) {
        return !instance.config['enabled'] || instance.config['enabled'][0] == 'true';
    }

    $scope.getInstancePriority = function(instance) {
        if (!instance.config['priority']) {
            return "0";
        }
        return instance.config['priority'][0];
    }

    Components.query({realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.storage.client.ClientStorageProvider'
    }, function(data) {
        $scope.instances = data;
        $scope.instancesLoaded = true;
    });

    $scope.removeInstance = function(instance) {
        Dialog.confirmDelete(instance.name, 'client storage provider', function() {
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

module.controller('GenericClientStorageCtrl', function($scope, $location, Notifications, $route, Dialog, realm,
                                                     serverInfo, instance, providerId, Components) {
    console.log('GenericClientStorageCtrl');
    console.log('providerId: ' + providerId);
    $scope.create = !instance.providerId;
    console.log('create: ' + $scope.create);
    var providers = serverInfo.componentTypes['org.keycloak.storage.client.ClientStorageProvider'];
    console.log('providers length ' + providers.length);
    var providerFactory = null;
    for (var i = 0; i < providers.length; i++) {
        var p = providers[i];
        console.log('provider: ' + p.id);
        if (p.id == providerId) {
            $scope.providerFactory = p;
            providerFactory = p;
            break;
        }

    }
    $scope.changed = false;

    console.log("providerFactory: " + providerFactory.id);

    function initClientStorageSettings() {
        if ($scope.create) {
            $scope.changed = true;
            instance.name = providerFactory.id;
            instance.providerId = providerFactory.id;
            instance.providerType = 'org.keycloak.storage.client.ClientStorageProvider';
            instance.parentId = realm.id;
            instance.config = {

            };
            instance.config['priority'] = ["0"];
            instance.config['enabled'] = ["true"];

            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;
            instance.config['cachePolicy'] = ['DEFAULT'];
            instance.config['evictionDay'] = [''];
            instance.config['evictionHour'] = [''];
            instance.config['evictionMinute'] = [''];
            instance.config['maxLifespan'] = [''];
            if (providerFactory.properties) {

                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (configProperty.defaultValue) {
                        instance.config[configProperty.name] = [configProperty.defaultValue];
                    } else {
                        instance.config[configProperty.name] = [''];
                    }

                }
            }

        } else {
            $scope.changed = false;
             if (!instance.config['enabled']) {
                instance.config['enabled'] = ['true'];
            }
            if (!instance.config['cachePolicy']) {
                instance.config['cachePolicy'] = ['DEFAULT'];

            }
            if (!instance.config['evictionDay']) {
                instance.config['evictionDay'] = [''];

            }
            if (!instance.config['evictionHour']) {
                instance.config['evictionHour'] = [''];

            }
            if (!instance.config['evictionMinute']) {
                instance.config['evictionMinute'] = [''];

            }
            if (!instance.config['maxLifespan']) {
                instance.config['maxLifespan'] = [''];

            }
            if (!instance.config['priority']) {
                instance.config['priority'] = ['0'];
            }

            if (providerFactory.properties) {
                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (!instance.config[configProperty.name]) {
                        instance.config[configProperty.name] = [''];
                    }
                }
            }

        }
    }

    initClientStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

     $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

    }, true);

    $scope.save = function() {
        console.log('save provider');
        $scope.changed = false;
        if ($scope.create) {
            console.log('saving new provider');
            Components.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/client-storage/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success("The provider has been created.");
            });
        } else {
            console.log('update existing provider');
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
        console.log('cancel');
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/client-stores");
        } else {
            $route.reload();
        }
    };



});


