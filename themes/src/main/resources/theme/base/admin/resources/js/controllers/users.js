module.controller('UserRoleMappingCtrl', function($scope, $http, realm, user, clients, client, Notifications, RealmRoleMapping,
                                                  ClientRoleMapping, AvailableRealmRoleMapping, AvailableClientRoleMapping,
                                                  CompositeRealmRoleMapping, CompositeClientRoleMapping) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clients = clients;
    $scope.client = client;
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];

    $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.id});
    $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
    $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});

    $scope.addRealmRole = function() {
        var roles = $scope.selectedRealmRoles;
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm',
                roles).success(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.targetClient) {
                    console.log('load available');
                    $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                    $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                    $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success("Role mappings updated.");

            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.targetClient) {
                    console.log('load available');
                    $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                    $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                    $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success("Role mappings updated.");
            }).error(function(response) {
                if (response && response['error_description']) {
                    Notifications.error(response['error_description']);
                } else {
                    Notifications.error("Failed to remove role mapping");
                }
            });
    };

    $scope.addClientRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/clients/' + $scope.targetClient.id,
                $scope.selectedClientRoles).success(function() {
                $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                Notifications.success("Role mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/clients/' + $scope.targetClient.id,
            {data : $scope.selectedClientMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                Notifications.success("Role mappings updated.");
            }).error(function(response) {
                if (response && response['error_description']) {
                    Notifications.error(response['error_description']);
                } else {
                    Notifications.error("Failed to remove role mapping");
                }
            });
    };


    $scope.changeClient = function() {
        console.log('changeClient');
        if ($scope.targetClient) {
            console.log('load available');
            $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
            $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
            $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.targetClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
        $scope.selectedClientRoles = [];
        $scope.selectedClientMappings = [];
    };



});

module.controller('UserSessionsCtrl', function($scope, realm, user, sessions, UserSessions, UserLogout, UserSessionLogout, Notifications) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.sessions = sessions;

    $scope.logoutAll = function() {
        UserLogout.save({realm : realm.realm, user: user.id}, function () {
            Notifications.success('Logged out user in all clients');
            UserSessions.query({realm: realm.realm, user: user.id}, function(updated) {
                $scope.sessions = updated;
            })
        });
    };

    $scope.logoutSession = function(sessionId) {
        console.log('here in logoutSession');
        UserSessionLogout.delete({realm : realm.realm, session: sessionId}, function() {
            UserSessions.query({realm: realm.realm, user: user.id}, function(updated) {
                $scope.sessions = updated;
                Notifications.success('Logged out session');
            })
        });
    }
});

module.controller('UserFederatedIdentityCtrl', function($scope, $location, realm, user, federatedIdentities, UserFederatedIdentity, Notifications, Dialog) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.federatedIdentities = federatedIdentities;

    $scope.hasAnyProvidersToCreate = function() {
        return realm.identityProviders.length - $scope.federatedIdentities.length > 0;
    }

    $scope.removeProviderLink = function(providerLink) {

        console.log("Removing provider link: " + providerLink.identityProvider);

        Dialog.confirmDelete(providerLink.identityProvider, 'Identity Provider Link', function() {
            UserFederatedIdentity.remove({ realm: realm.realm, user: user.id, provider: providerLink.identityProvider }, function() {
                Notifications.success("The provider link has been deleted.");
                var indexToRemove = $scope.federatedIdentities.indexOf(providerLink);
                $scope.federatedIdentities.splice(indexToRemove, 1);
            });
        });
    }
});

module.controller('UserFederatedIdentityAddCtrl', function($scope, $location, realm, user, federatedIdentities, UserFederatedIdentity, Notifications) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.federatedIdentity = {};

    var getAvailableProvidersToCreate = function() {
        var realmProviders = [];
        for (var i=0 ; i<realm.identityProviders.length ; i++) {
            var providerAlias = realm.identityProviders[i].alias;
            realmProviders.push(providerAlias);
        };

        for (var i=0 ; i<federatedIdentities.length ; i++) {
            var providerAlias = federatedIdentities[i].identityProvider;
            var index = realmProviders.indexOf(providerAlias);
            realmProviders.splice(index, 1);
        }

        return realmProviders;
    }
    $scope.availableProvidersToCreate = getAvailableProvidersToCreate();

    $scope.save = function() {
        UserFederatedIdentity.save({
            realm : realm.realm,
            user: user.id,
            provider: $scope.federatedIdentity.identityProvider
        }, $scope.federatedIdentity, function(data, headers) {
            $location.url("/realms/" + realm.realm + '/users/' + $scope.user.id + '/federated-identity');
            Notifications.success("Provider link has been created.");
        });
    };

    $scope.cancel = function() {
         $location.url("/realms/" + realm.realm + '/users/' + $scope.user.id + '/federated-identity');
    };

});

module.controller('UserConsentsCtrl', function($scope, realm, user, userConsents, UserConsents, Notifications) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.userConsents = userConsents;

    $scope.revokeConsent = function(clientId) {
        UserConsents.delete({realm : realm.realm, user: user.id, client: clientId }, function () {
            UserConsents.query({realm: realm.realm, user: user.id}, function(updated) {
                $scope.userConsents = updated;
            })
            Notifications.success('Grant revoked successfully');
        }, function() {
            Notifications.error("Grant couldn't be revoked");
        });
        console.log("Revoke consent " + clientId);
    }
});

module.controller('UserOfflineSessionsCtrl', function($scope, $location, realm, user, client, offlineSessions) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.client = client;
    $scope.offlineSessions = offlineSessions;

    $scope.cancel = function() {
         $location.url("/realms/" + realm.realm + '/users/' + user.id + '/consents');
    };
});


module.controller('UserListCtrl', function($scope, realm, User, UserImpersonation, BruteForce, Notifications, $route, Dialog) {
    $scope.realm = realm;
    $scope.page = 0;

    $scope.query = {
        realm: realm.realm,
        max : 20,
        first : 0
    };

    $scope.impersonate = function(userId) {
        UserImpersonation.save({realm : realm.realm, user: userId}, function (data) {
            if (data.sameRealm) {
                window.location = data.redirect;
            } else {
                window.open(data.redirect, "_blank");
            }
        });
    };

    $scope.unlockUsers = function() {
        BruteForce.delete({realm: realm.realm}, function(data) {
            Notifications.success("Any temporarily locked users are now unlocked.");
        });
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
        console.log("query.search: " + $scope.query.search);
        $scope.searchLoaded = false;

        $scope.users = User.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.removeUser = function(user) {
        Dialog.confirmDelete(user.id, 'user', function() {
            user.$remove({
                realm : realm.realm,
                userId : user.id
            }, function() {
                $route.reload();
                Notifications.success("The user has been deleted.");
            }, function() {
                Notifications.error("User couldn't be deleted");
            });
        });
    };
});


module.controller('UserTabCtrl', function($scope, $location, Dialog, Notifications, Current) {
    $scope.removeUser = function() {
        Dialog.confirmDelete($scope.user.id, 'user', function() {
            $scope.user.$remove({
                realm : Current.realm.realm,
                userId : $scope.user.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/users");
                Notifications.success("The user has been deleted.");
            }, function() {
                Notifications.error("User couldn't be deleted");
            });
        });
    };
});

module.controller('UserDetailCtrl', function($scope, realm, user, BruteForceUser, User,
                                             UserFederationInstances, UserImpersonation, RequiredActions,
                                             $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.create = !user.id;
    $scope.editUsername = $scope.create || $scope.realm.editUsernameAllowed;

    if ($scope.create) {
        $scope.user = { enabled: true, attributes: {} }
    } else {
        if (!user.attributes) {
            user.attributes = {}
        }
        convertAttributeValuesToString(user);


        $scope.user = angular.copy(user);
        $scope.impersonate = function() {
            UserImpersonation.save({realm : realm.realm, user: $scope.user.id}, function (data) {
                if (data.sameRealm) {
                    window.location = data.redirect;
                } else {
                    window.open(data.redirect, "_blank");
                }
            });
        };
        if(user.federationLink) {
            console.log("federationLink is not null");
            UserFederationInstances.get({realm : realm.realm, instance: user.federationLink}, function(link) {
                $scope.federationLinkName = link.displayName;
                $scope.federationLink = "#/realms/" + realm.realm + "/user-federation/providers/" + link.providerName + "/" + link.id;
            })
        } else {
            console.log("federationLink is null");
        }
        console.log('realm brute force? ' + realm.bruteForceProtected)
        $scope.temporarilyDisabled = false;
        var isDisabled = function () {
            BruteForceUser.get({realm: realm.realm, userId: user.id}, function(data) {
                console.log('here in isDisabled ' + data.disabled);
                $scope.temporarilyDisabled = data.disabled;
            });
        };

        console.log("check if disabled");
        isDisabled();

        $scope.unlockUser = function() {
            BruteForceUser.delete({realm: realm.realm, userId: user.id}, function(data) {
                isDisabled();
            });
        }
    }

    $scope.changed = false; // $scope.create;
    if (user.requiredActions) {
        for (var i = 0; i < user.requiredActions.length; i++) {
            console.log("user require action: " + user.requiredActions[i]);
        }
    }
    // ID - Name map for required actions. IDs are enum names.
    RequiredActions.query({realm: realm.realm}, function(data) {
        $scope.userReqActionList = [];
        for (var i = 0; i < data.length; i++) {
            console.log("listed required action: " + data[i].name);
            if (data[i].enabled) {
                var item = data[i];
                $scope.userReqActionList.push(item);
            }
        }

    });
    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        convertAttributeValuesToLists();

        if ($scope.create) {
            User.save({
                realm: realm.realm
            }, $scope.user, function (data, headers) {
                $scope.changed = false;
                convertAttributeValuesToString($scope.user);
                user = angular.copy($scope.user);
                var l = headers().location;

                console.debug("Location == " + l);

                var id = l.substring(l.lastIndexOf("/") + 1);


                $location.url("/realms/" + realm.realm + "/users/" + id);
                Notifications.success("The user has been created.");
            });
        } else {
            User.update({
                realm: realm.realm,
                userId: $scope.user.id
            }, $scope.user, function () {
                $scope.changed = false;
                convertAttributeValuesToString($scope.user);
                user = angular.copy($scope.user);
                Notifications.success("Your changes have been saved to the user.");
            });
        }
    };

    function convertAttributeValuesToLists() {
        var attrs = $scope.user.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                var attrVals = attrs[attribute].split("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    function convertAttributeValuesToString(user) {
        var attrs = user.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                var attrVals = attrs[attribute].join("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    $scope.reset = function() {
        $scope.user = angular.copy(user);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/users");
    };

    $scope.addAttribute = function() {
        $scope.user.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
    }

    $scope.removeAttribute = function(key) {
        delete $scope.user.attributes[key];
    }
});

module.controller('UserCredentialsCtrl', function($scope, realm, user, RequiredActions, User, UserExecuteActionsEmail, UserCredentials, Notifications, Dialog) {
    console.log('UserCredentialsCtrl');

    $scope.realm = realm;
    $scope.user = angular.copy(user);
    $scope.temporaryPassword = true;

    $scope.isTotp = false;
    if(!!user.totp){
        $scope.isTotp = user.totp;
    }
    // ID - Name map for required actions. IDs are enum names.
    RequiredActions.query({realm: realm.realm}, function(data) {
        $scope.userReqActionList = [];
        for (var i = 0; i < data.length; i++) {
            console.log("listed required action: " + data[i].name);
            if (data[i].enabled) {
                var item = data[i];
                $scope.userReqActionList.push(item);
            }
        }

    });

    $scope.resetPassword = function() {
        if ($scope.pwdChange) {
            if ($scope.password != $scope.confirmPassword) {
                Notifications.error("Password and confirmation does not match.");
                return;
            }
        }

        var msgTitle = 'Change password';
        var msg = 'Are you sure you want to change the users password?';

        Dialog.confirm(msgTitle, msg, function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id }, { type : "password", value : $scope.password, temporary: $scope.temporaryPassword }, function() {
                Notifications.success("The password has been reset");
                $scope.password = null;
                $scope.confirmPassword = null;
            }, function(response) {
                if (response.data && response.data['error_description']) {
                    Notifications.error(response.data['error_description']);
                } else {
                    Notifications.error("Failed to reset user password");
                }
            });
        }, function() {
            $scope.password = null;
            $scope.confirmPassword = null;
        });
    };

    $scope.removeTotp = function() {
        Dialog.confirm('Remove totp', 'Are you sure you want to remove the users totp configuration?', function() {
            UserCredentials.removeTotp({ realm: realm.realm, userId: user.id }, { }, function() {
                Notifications.success("The users totp configuration has been removed");
                $scope.user.totp = false;
            }, function() {
                Notifications.error("Failed to remove the users totp configuration");
            });
        });
    };

    $scope.emailActions = [];

    $scope.sendExecuteActionsEmail = function() {
        if ($scope.changed) {
            Dialog.message("Cannot send email", "You must save your current changes before you can send an email");
            return;
        }
        Dialog.confirm('Send Email', 'Are you sure you want to send email to user?', function() {
            UserExecuteActionsEmail.update({ realm: realm.realm, userId: user.id }, $scope.emailActions, function() {
                Notifications.success("Email sent to user");
                $scope.emailActions = [];
            }, function() {
                Notifications.error("Failed to send email to user");
            });
        });
    };



    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.userChange = true;
        } else {
            $scope.userChange = false;
        }
    }, true);

    $scope.$watch('password', function() {
        if (!!$scope.password){
            $scope.pwdChange = true;
        } else {
            $scope.pwdChange = false;
        }
    }, true);

    $scope.reset = function() {
        $scope.password = "";
        $scope.confirmPassword = "";

        $scope.user = angular.copy(user);

        $scope.isTotp = false;
        if(!!user.totp){
            $scope.isTotp = user.totp;
        }

        $scope.pwdChange = false;
        $scope.userChange = false;
    };
});

module.controller('UserFederationCtrl', function($scope, $location, $route, realm, serverInfo, Components, UserFederationProviders, UserFederationInstances, Notifications, Dialog) {
    console.log('UserFederationCtrl ++++****');
    $scope.realm = realm;
    $scope.providers = serverInfo.componentTypes['org.keycloak.storage.UserStorageProvider'];

    if (!$scope.providers) $scope.providers = [];
    
    for (var i = 0; i < $scope.providers.length; i++) {
        $scope.providers[i].isUserFederationProvider = false;
    }
    UserFederationProviders.query({realm: realm.realm}, function(data) {
        for (var i = 0; i < data.length; i++) {
            data[i].isUserFederationProvider = true;
            $scope.providers.push(data[i]);
        }
    });

    $scope.addProvider = function(provider) {
        console.log('Add provider: ' + provider.id);
        if (provider.isUserFederationProvider) {
            $location.url("/create/user-federation/" + realm.realm + "/providers/" + provider.id);
        } else {
            $location.url("/create/user-storage/" + realm.realm + "/providers/" + provider.id);
            
        }
    };

    $scope.getInstanceLink = function(instance) {
        if (instance.isUserFederationProvider) {
            return "/realms/" + realm.realm + "/user-federation/providers/" + instance.providerName + "/" + instance.id;
        } else {
            return "/realms/" + realm.realm + "/user-storage/providers/" + instance.providerId + "/" + instance.id;
        }
    }

    $scope.getInstanceName = function(instance) {
        if (instance.isUserFederationProvider) {
            return instance.displayName;
        } else {
            return instance.name;
        }
    }
    $scope.getInstanceProvider = function(instance) {
        if (instance.isUserFederationProvider) {
            return instance.providerName;
        } else {
            return instance.providerId;
        }
    }

    $scope.getInstancePriority = function(instance) {
        if (instance.isUserFederationProvider) {
            return instance.priority;
        } else {
            return instance.config['priority'][0];
        }
    }

    Components.query({realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.storage.UserStorageProvider'
    }, function(data) {
        $scope.instances = data;
        for (var i = 0; i < data.length; i++) {
            data[i].isUserFederationProvider = false;
        }
        UserFederationInstances.query({realm: realm.realm}, function(data) {
            for (var i = 0; i < data.length; i++) {
                data[i].isUserFederationProvider = true;
                $scope.instances.push(data[i]);
            }
            
        });
    });

    $scope.removeInstance = function(instance) {
        if (instance.isUserFederationProvider) {
            Dialog.confirmDelete(instance.displayName, 'user federation provider', function() {
                UserFederationInstances.remove({
                    realm : realm.realm,
                    instance : instance.id
                }, function() {
                    $route.reload();
                    Notifications.success("The provider has been deleted.");
                });
            });

        } else {
            Dialog.confirmDelete(instance.name, 'user storage provider', function() {
                Components.remove({
                    realm : realm.realm,
                    componentId : instance.id
                }, function() {
                    $route.reload();
                    Notifications.success("The provider has been deleted.");
                });
            });
        }
    };
});

module.controller('GenericUserStorageCtrl', function($scope, $location, Notifications, $route, Dialog, realm, serverInfo, instance, providerId, Components) {
    console.log('GenericUserStorageCtrl');
    console.log('providerId: ' + providerId);
    $scope.create = !instance.providerId;
    console.log('create: ' + $scope.create);
    var providers = serverInfo.componentTypes['org.keycloak.storage.UserStorageProvider'];
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
    $scope.provider = instance;

    console.log("providerFactory: " + providerFactory.id);

    function initUserStorageSettings() {
        if ($scope.create) {
            instance.name = providerFactory.id;
            instance.providerId = providerFactory.id;
            instance.providerType = 'org.keycloak.storage.UserStorageProvider';
            instance.parentId = realm.id;
            instance.config = {

            };
            instance.config['priority'] = ["0"];

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
            /*
            console.log('Manage instance');
            console.log(instance.name);
            console.log(instance.providerId);
            console.log(instance.providerType);
            console.log(instance.parentId);
            for (var k in instance.config) {
                console.log('config[' + k + "] =");
            }
            */
        }

        $scope.changed = false;
    }

    initUserStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

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

                $location.url("/realms/" + realm.realm + "/user-storage/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success("The provider has been created.");
            }, function (errorResponse) {
                if (errorResponse.data && errorResponse.data['error_description']) {
                    Notifications.error(errorResponse.data['error_description']);
                }
            });
        } else {
            Components.update({realm: realm.realm,
                    componentId: instance.id
                },
                $scope.instance,  function () {
                    $route.reload();
                    Notifications.success("The provider has been updated.");
                }, function (errorResponse) {
                    if (errorResponse.data && errorResponse.data['error_description']) {
                        Notifications.error(errorResponse.data['error_description']);
                    }
                });
        }
    };

    $scope.reset = function() {
        initUserStorageSettings();
        $scope.instance = angular.copy(instance);
    };

    $scope.cancel = function() {
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/user-storage");
        } else {
            $route.reload();
        }
    };
});


module.controller('UserFederationTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeUserFederation = function() {
        Dialog.confirmDelete($scope.instance.displayName, 'user federation provider', function() {
            $scope.instance.$remove({
                realm : Current.realm.realm,
                instance : $scope.instance.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/user-federation");
                Notifications.success("The provider has been deleted.");
            });
        });
    };
});


module.controller('GenericUserFederationCtrl', function($scope, $location, Notifications, $route, Dialog, realm, instance, providerFactory, UserFederationInstances, UserFederationSync) {
    console.log('GenericUserFederationCtrl');

    $scope.create = !instance.providerName;
    $scope.providerFactory = providerFactory;
    $scope.provider = instance;

    console.log("providerFactory: " + providerFactory.id);

    function initFederationSettings() {
        if ($scope.create) {
            instance.providerName = providerFactory.id;
            instance.config = {};
            instance.priority = 0;
            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;

            if (providerFactory.id === 'kerberos') {
                instance.config.debug = false;
                instance.config.allowPasswordAuthentication = true;
                instance.config.editMode = 'UNSYNCED';
                instance.config.updateProfileFirstLogin = true;
                instance.config.allowKerberosAuthentication = true;
            }

            if (providerFactory.properties) {

                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    instance.config[configProperty.name] = configProperty.defaultValue;
                }
            }

        } else {
            $scope.fullSyncEnabled = (instance.fullSyncPeriod && instance.fullSyncPeriod > 0);
            $scope.changedSyncEnabled = (instance.changedSyncPeriod && instance.changedSyncPeriod > 0);

            if (providerFactory.id === 'kerberos') {
                instance.config.debug = (instance.config.debug === 'true' || instance.config.debug === true);
                instance.config.allowPasswordAuthentication = (instance.config.allowPasswordAuthentication === 'true' || instance.config.allowPasswordAuthentication === true);
                instance.config.updateProfileFirstLogin = (instance.config.updateProfileFirstLogin === 'true' || instance.config.updateProfileFirstLogin === true);
            }
        }

        $scope.changed = false;
    }

    initFederationSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

    $scope.$watch('fullSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.fullSyncPeriod = $scope.fullSyncEnabled ? 604800 : -1;
        $scope.changed = true;
    });

    $scope.$watch('changedSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.changedSyncPeriod = $scope.changedSyncEnabled ? 86400 : -1;
        $scope.changed = true;
    });

    $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

    }, true);

    $scope.save = function() {
        $scope.changed = false;
        if ($scope.create) {
            UserFederationInstances.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/user-federation/providers/" + $scope.instance.providerName + "/" + id);
                Notifications.success("The provider has been created.");
            }, function (errorResponse) {
                if (errorResponse.data && errorResponse.data['error_description']) {
                    Notifications.error(errorResponse.data['error_description']);
                }
            });
        } else {
            UserFederationInstances.update({realm: realm.realm,
                    instance: instance.id
                },
                $scope.instance,  function () {
                    $route.reload();
                    Notifications.success("The provider has been updated.");
                }, function (errorResponse) {
                    if (errorResponse.data && errorResponse.data['error_description']) {
                        Notifications.error(errorResponse.data['error_description']);
                    }
                });
        }
    };

    $scope.reset = function() {
        initFederationSettings();
        $scope.instance = angular.copy(instance);
    };

    $scope.cancel = function() {
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/user-federation");
        } else {
            $route.reload();
        }
    };

    $scope.triggerFullSync = function() {
        console.log('GenericCtrl: triggerFullSync');
        triggerSync('triggerFullSync');
    }

    $scope.triggerChangedUsersSync = function() {
        console.log('GenericCtrl: triggerChangedUsersSync');
        triggerSync('triggerChangedUsersSync');
    }

    function triggerSync(action) {
        UserFederationSync.save({ action: action, realm: $scope.realm.realm, provider: $scope.instance.id }, {}, function(syncResult) {
            Notifications.success("Sync of users finished successfully. " + syncResult.status);
        }, function() {
            Notifications.error("Error during sync of users");
        });
    }
});


module.controller('LDAPCtrl', function($scope, $location, $route, Notifications, Dialog, realm, instance, UserFederationInstances, UserFederationSync, RealmLDAPConnectionTester) {
    console.log('LDAPCtrl');

    $scope.ldapVendors = [
        { "id": "ad", "name": "Active Directory" },
        { "id": "rhds", "name": "Red Hat Directory Server" },
        { "id": "tivoli", "name": "Tivoli" },
        { "id": "edirectory", "name": "Novell eDirectory" },
        { "id": "other", "name": "Other" }
    ];

    $scope.authTypes = [
        { "id": "none", "name": "none" },
        { "id": "simple", "name": "simple" }
    ];

    $scope.searchScopes = [
        { "id": "1", "name": "One Level" },
        { "id": "2", "name": "Subtree" }
    ];

    $scope.useTruststoreOptions = [
        { "id": "always", "name": "Always" },
        { "id": "ldapsOnly", "name": "Only for ldaps" },
        { "id": "never", "name": "Never" }
    ];

    var DEFAULT_BATCH_SIZE = "1000";

    $scope.create = !instance.providerName;

    function initFederationSettings() {
        if ($scope.create) {
            instance.providerName = "ldap";
            instance.config = {};
            instance.priority = 0;

            instance.config.syncRegistrations = false;
            instance.config.userAccountControlsAfterPasswordUpdate = true;
            instance.config.connectionPooling = true;
            instance.config.pagination = true;

            instance.config.allowKerberosAuthentication = false;
            instance.config.debug = false;
            instance.config.useKerberosForPasswordAuthentication = false;

            instance.config.authType = 'simple';
            instance.config.batchSizeForSync = DEFAULT_BATCH_SIZE;
            instance.config.searchScope = "1";
            instance.config.useTruststoreSpi = "ldapsOnly";

            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;
        } else {
            instance.config.syncRegistrations = (instance.config.syncRegistrations === 'true' || instance.config.syncRegistrations === true);
            instance.config.userAccountControlsAfterPasswordUpdate = (instance.config.userAccountControlsAfterPasswordUpdate === 'true' || instance.config.userAccountControlsAfterPasswordUpdate === true);
            instance.config.connectionPooling = (instance.config.connectionPooling === 'true' || instance.config.connectionPooling === true);
            instance.config.pagination = (instance.config.pagination === 'true' || instance.config.pagination === true);

            instance.config.allowKerberosAuthentication = (instance.config.allowKerberosAuthentication === 'true' || instance.config.allowKerberosAuthentication === true);
            instance.config.debug = (instance.config.debug === 'true' || instance.config.debug === true);
            instance.config.useKerberosForPasswordAuthentication = (instance.config.useKerberosForPasswordAuthentication === 'true' || instance.config.useKerberosForPasswordAuthentication === true);

            if (!instance.config.authType) {
                instance.config.authType = 'simple';
            }
            if (!instance.config.batchSizeForSync) {
                instance.config.batchSizeForSync = DEFAULT_BATCH_SIZE;
            }
            if (!instance.config.searchScope) {
                instance.config.searchScope = '1';
            }
            if (!instance.config.useTruststoreSpi) {
                instance.config.useTruststoreSpi = "ldapsOnly";
            }

            $scope.fullSyncEnabled = (instance.fullSyncPeriod && instance.fullSyncPeriod > 0);
            $scope.changedSyncEnabled = (instance.changedSyncPeriod && instance.changedSyncPeriod > 0);

            for (var i=0 ; i<$scope.ldapVendors.length ; i++) {
                if ($scope.ldapVendors[i].id === instance.config.vendor) {
                    $scope.vendorName = $scope.ldapVendors[i].name;
                }
            };
        }

        $scope.changed = false;
        $scope.lastVendor = instance.config.vendor;
    }

    initFederationSettings();
    $scope.instance = angular.copy(instance);

    $scope.realm = realm;

    $scope.$watch('fullSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.fullSyncPeriod = $scope.fullSyncEnabled ? 604800 : -1;
        $scope.changed = true;
    });

    $scope.$watch('changedSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.changedSyncPeriod = $scope.changedSyncEnabled ? 86400 : -1;
        $scope.changed = true;
    });

    $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

        if (!angular.equals($scope.instance.config.vendor, $scope.lastVendor)) {
            console.log("LDAP vendor changed");
            $scope.lastVendor = $scope.instance.config.vendor;

            if ($scope.lastVendor === "ad") {
                $scope.instance.config.usernameLDAPAttribute = "cn";
                $scope.instance.config.userObjectClasses = "person, organizationalPerson, user";
            } else {
                $scope.instance.config.usernameLDAPAttribute = "uid";
                $scope.instance.config.userObjectClasses = "inetOrgPerson, organizationalPerson";
            }

            $scope.instance.config.rdnLDAPAttribute = $scope.instance.config.usernameLDAPAttribute;

            var vendorToUUID = {
                rhds: "nsuniqueid",
                tivoli: "uniqueidentifier",
                edirectory: "guid",
                ad: "objectGUID",
                other: "entryUUID"
            };
            $scope.instance.config.uuidLDAPAttribute = vendorToUUID[$scope.lastVendor];
        }
    }, true);

    $scope.save = function() {
        $scope.changed = false;

        if (!parseInt($scope.instance.config.batchSizeForSync)) {
            $scope.instance.config.batchSizeForSync = DEFAULT_BATCH_SIZE;
        } else {
            $scope.instance.config.batchSizeForSync = parseInt($scope.instance.config.batchSizeForSync).toString();
        }

        if ($scope.create) {
            UserFederationInstances.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/user-federation/providers/" + $scope.instance.providerName + "/" + id);
                Notifications.success("The provider has been created.");
            }, function (errorResponse) {
                if (errorResponse.data && errorResponse.data['error_description']) {
                    Notifications.error(errorResponse.data['error_description']);
                }
            });
        } else {
            UserFederationInstances.update({realm: realm.realm,
                                          instance: instance.id
                },
                $scope.instance,  function () {
                $route.reload();
                Notifications.success("The provider has been updated.");
            }, function (errorResponse) {
                if (errorResponse.data && errorResponse.data['error_description']) {
                    Notifications.error(errorResponse.data['error_description']);
                }
            });
        }
    };

    $scope.reset = function() {
        $route.reload();
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/user-federation");
    };

    $scope.remove = function() {
        Dialog.confirm('Delete', 'Are you sure you want to permanently delete this provider?  All imported users will also be deleted.', function() {
            $scope.instance.$remove({
                realm : realm.realm,
                instance : $scope.instance.id
            }, function() {
                $location.url("/realms/" + realm.realm + "/user-federation");
                Notifications.success("The provider has been deleted.");
            });
        });
    };


    var initConnectionTest = function(testAction, ldapConfig) {
        return {
            action: testAction,
            realm: $scope.realm.realm,
            connectionUrl: ldapConfig.connectionUrl,
            bindDn: ldapConfig.bindDn,
            bindCredential: ldapConfig.bindCredential,
            useTruststoreSpi: ldapConfig.useTruststoreSpi
        };
    };

    $scope.testConnection = function() {
        console.log('LDAPCtrl: testConnection');
        RealmLDAPConnectionTester.get(initConnectionTest("testConnection", $scope.instance.config), function() {
            Notifications.success("LDAP connection successful.");
        }, function() {
            Notifications.error("Error when trying to connect to LDAP. See server.log for details.");
        });
    }

    $scope.testAuthentication = function() {
        console.log('LDAPCtrl: testAuthentication');
        RealmLDAPConnectionTester.get(initConnectionTest("testAuthentication", $scope.instance.config), function() {
            Notifications.success("LDAP authentication successful.");
        }, function() {
            Notifications.error("LDAP authentication failed. See server.log for details");
        });
    }

    $scope.triggerFullSync = function() {
        console.log('LDAPCtrl: triggerFullSync');
        triggerSync('triggerFullSync');
    }

    $scope.triggerChangedUsersSync = function() {
        console.log('LDAPCtrl: triggerChangedUsersSync');
        triggerSync('triggerChangedUsersSync');
    }

    function triggerSync(action) {
        UserFederationSync.save({ action: action, realm: $scope.realm.realm, provider: $scope.instance.id }, {}, function(syncResult) {
            Notifications.success("Sync of users finished successfully. " + syncResult.status);
        }, function() {
            Notifications.error("Error during sync of users");
        });
    }

});


module.controller('UserFederationMapperListCtrl', function($scope, $location, Notifications, $route, Dialog, realm, provider, mapperTypes, mappers) {
    console.log('UserFederationMapperListCtrl');

    $scope.realm = realm;
    $scope.provider = provider;
    $scope.instance = provider;

    $scope.mapperTypes = mapperTypes;
    $scope.mappers = mappers;

    $scope.hasAnyMapperTypes = false;
    for (var property in mapperTypes) {
        if (!(property.indexOf('$') === 0)) {
            $scope.hasAnyMapperTypes = true;
            break;
        }
    }

});

module.controller('UserFederationMapperCtrl', function($scope, realm,  provider, mapperTypes, mapper, clients, UserFederationMapper, UserFederationMapperSync, Notifications, Dialog, $location) {
    console.log('UserFederationMapperCtrl');
    $scope.realm = realm;
    $scope.provider = provider;
    $scope.clients = clients;
    $scope.create = false;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    $scope.mapperType = mapperTypes[mapper.federationMapperType];

    $scope.$watch('mapper', function() {
        if (!angular.equals($scope.mapper, mapper)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        UserFederationMapper.update({
            realm : realm.realm,
            provider: provider.id,
            mapperId : mapper.id
        }, $scope.mapper, function() {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/user-federation/providers/' + provider.providerName + '/' + provider.id + '/mappers/' + mapper.id);
            Notifications.success("Your changes have been saved.");
        }, function(error) {
            if (error.status == 400 && error.data.error_description) {
                Notifications.error(error.data.error_description);
            } else {
                Notifications.error('Unexpected error when creating mapper');
            }
        });
    };

    $scope.reset = function() {
        $scope.mapper = angular.copy(mapper);
        $scope.changed = false;
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.mapper.name, 'mapper', function() {
            UserFederationMapper.remove({ realm: realm.realm, provider: provider.id, mapperId : $scope.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + '/user-federation/providers/' + provider.providerName + '/' + provider.id + '/mappers');
            });
        });
    };

    $scope.triggerFedToKeycloakSync = function() {
        triggerMapperSync("fedToKeycloak")
    }

    $scope.triggerKeycloakToFedSync = function() {
        triggerMapperSync("keycloakToFed");
    }

    function triggerMapperSync(direction) {
        UserFederationMapperSync.save({ direction: direction, realm: realm.realm, provider: provider.id, mapperId : $scope.mapper.id }, {}, function(syncResult) {
            Notifications.success("Data synced successfully. " + syncResult.status);
        }, function(error) {
            Notifications.error(error.data.errorMessage);
        });
    }

});

module.controller('UserFederationMapperCreateCtrl', function($scope, realm, provider, mapperTypes, clients, UserFederationMapper, Notifications, Dialog, $location) {
    console.log('UserFederationMapperCreateCtrl');
    $scope.realm = realm;
    $scope.provider = provider;
    $scope.clients = clients;
    $scope.create = true;
    $scope.mapper = { federationProviderDisplayName: provider.displayName, config: {}};
    $scope.mapperTypes = mapperTypes;
    $scope.mapperType = null;
    $scope.changed = true;

    $scope.$watch('mapperType', function() {
        if ($scope.mapperType != null) {
            $scope.mapper.config = $scope.mapperType.defaultConfig;
        }
    }, true);

    $scope.save = function() {
        if ($scope.mapperType == null) {
            Notifications.error("You need to select mapper type!");
            return;
        }

        $scope.mapper.federationMapperType = $scope.mapperType.id;
        UserFederationMapper.save({
            realm : realm.realm, provider: provider.id
        }, $scope.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url('/realms/' + realm.realm +'/user-federation/providers/' + provider.providerName + '/' + provider.id + '/mappers/' + id);
            Notifications.success("Mapper has been created.");
        }, function(error) {
            if (error.status == 400 && error.data.error_description) {
                Notifications.error(error.data.error_description);
            } else {
                Notifications.error('Unexpected error when creating mapper');
            }
        });
    };

    $scope.reset = function() {
        $location.url("/realms/" + realm.realm + '/user-federation/providers/' + provider.providerName + '/' + provider.id + '/mappers');
    };


});

function removeGroupMember(groups, member) {
    for (var j = 0; j < groups.length; j++) {
        //console.log('checking: ' + groups[j].path);
        if (member.path == groups[j].path) {
            groups.splice(j, 1);
            break;
        }
        if (groups[j].subGroups && groups[j].subGroups.length > 0) {
            //console.log('going into subgroups');
            removeGroupMember(groups[j].subGroups, member);
        }
    }
}
module.controller('UserGroupMembershipCtrl', function($scope, $route, realm, groups, user, UserGroupMembership, UserGroupMapping, Notifications, $location, Dialog) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.groupList = groups;
    $scope.selectedGroup = null;
    $scope.tree = [];

    UserGroupMembership.query({realm: realm.realm, userId: user.id}, function(data) {
        $scope.groupMemberships = data;
        for (var i = 0; i < data.length; i++) {
            var member = data[i];
            removeGroupMember(groups, member);
        }

    });



    $scope.joinGroup = function() {
        if (!$scope.tree.currentNode) {
            Notifications.error('Please select a group to add');
            return;
        };
        UserGroupMapping.update({realm: realm.realm, userId: user.id, groupId: $scope.tree.currentNode.id}, function() {
            Notifications.success('Added group membership');
            $route.reload();
        });

    };

    $scope.leaveGroup = function() {
        if (!$scope.selectedGroup) {
            return;

        }
        UserGroupMapping.remove({realm: realm.realm, userId: user.id, groupId: $scope.selectedGroup.id}, function() {
            Notifications.success('Removed group membership');
            $route.reload();
        }, function(response) {
            if (response.data && response.data['error_description']) {
                Notifications.error(response.data['error_description']);
            } else {
                Notifications.error("Failed to leave group");
            }
        });

    };

    var isLeaf = function(node) {
        return node.id != "realm" && (!node.subGroups || node.subGroups.length == 0);
    };

    $scope.getGroupClass = function(node) {
        if (node.id == "realm") {
            return 'pficon pficon-users';
        }
        if (isLeaf(node)) {
            return 'normal';
        }
        if (node.subGroups.length && node.collapsed) return 'collapsed';
        if (node.subGroups.length && !node.collapsed) return 'expanded';
        return 'collapsed';

    }

    $scope.getSelectedClass = function(node) {
        if (node.selected) {
            return 'selected';
        } else if ($scope.cutNode && $scope.cutNode.id == node.id) {
            return 'cut';
        }
        return undefined;
    }

});


