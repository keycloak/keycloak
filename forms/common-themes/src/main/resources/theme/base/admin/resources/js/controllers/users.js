module.controller('UserRoleMappingCtrl', function($scope, $http, realm, user, clients, Notifications, RealmRoleMapping,
                                                  ClientRoleMapping, AvailableRealmRoleMapping, AvailableClientRoleMapping,
                                                  CompositeRealmRoleMapping, CompositeClientRoleMapping) {
    $scope.realm = realm;
    $scope.user = user;
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

    $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username});
    $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.username});
    $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.username});

    $scope.addRealmRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/realm',
                $scope.selectedRealmRoles).success(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.client) {
                    console.log('load available');
                    $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                    $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                    $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success("Role mappings updated.");

            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/realm',
            {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.username});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.client) {
                    console.log('load available');
                    $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                    $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                    $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success("Role mappings updated.");
            });
    };

    $scope.addClientRole = function() {
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/clients-by-id/' + $scope.client.id,
                $scope.selectedClientRoles).success(function() {
                $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                Notifications.success("Role mappings updated.");
            });
    };

    $scope.deleteClientRole = function() {
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.username + '/role-mappings/clients-by-id/' + $scope.client.id,
            {data : $scope.selectedClientMappings, headers : {"content-type" : "application/json"}}).success(function() {
                $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                Notifications.success("Role mappings updated.");
            });
    };


    $scope.changeClient = function() {
        console.log('changeClient');
        if ($scope.client) {
            console.log('load available');
            $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
            $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
            $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.username, client : $scope.client.id});
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
        UserLogout.save({realm : realm.realm, user: user.username}, function () {
            Notifications.success('Logged out user in all clients');
            UserSessions.query({realm: realm.realm, user: user.username}, function(updated) {
                $scope.sessions = updated;
            })
        });
    };

    $scope.logoutSession = function(sessionId) {
        console.log('here in logoutSession');
        UserSessionLogout.delete({realm : realm.realm, session: sessionId}, function() {
            UserSessions.query({realm: realm.realm, user: user.username}, function(updated) {
                $scope.sessions = updated;
                Notifications.success('Logged out session');
            })
        });
    }
});

module.controller('UserFederatedIdentityCtrl', function($scope, realm, user, federatedIdentities) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.federatedIdentities = federatedIdentities;
});

module.controller('UserConsentsCtrl', function($scope, realm, user, userConsents, UserConsents, Notifications) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.userConsents = userConsents;

    $scope.revokeConsent = function(clientId) {
        UserConsents.delete({realm : realm.realm, user: user.username, client: clientId }, function () {
            UserConsents.query({realm: realm.realm, user: user.username}, function(updated) {
                $scope.userConsents = updated;
            })
            Notifications.success('Consent revoked successfully');
        }, function() {
            Notifications.error("Consent couldn't be revoked");
        });
        console.log("Revoke consent " + clientId);
    }
});


module.controller('UserListCtrl', function($scope, realm, User) {
    $scope.realm = realm;
    $scope.page = 0;

    $scope.query = {
        realm: realm.realm,
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
        console.log("query.search: " + $scope.query.search);
        $scope.searchLoaded = false;

        $scope.users = User.query($scope.query, function() {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
});



module.controller('UserDetailCtrl', function($scope, realm, user, User, UserFederationInstances, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.create = !user.username;

    if ($scope.create) {
        $scope.user = { enabled: true, attributes: {} }
    } else {
        if (!user.attributes) {
            user.attributes = {}
        }
        $scope.user = angular.copy(user);
        if(user.federationLink) {
            console.log("federationLink is not null");
            UserFederationInstances.get({realm : realm.realm, instance: user.federationLink}, function(link) {
                $scope.federationLinkName = link.displayName;
                $scope.federationLink = "#/realms/" + realm.realm + "/user-federation/providers/" + link.providerName + "/" + link.id;
            })
        } else {
            console.log("federationLink is null");
        }
    }

    $scope.changed = false; // $scope.create;

    // ID - Name map for required actions. IDs are enum names.
    $scope.userReqActionList = [
        {id: "VERIFY_EMAIL", text: "Verify Email"},
        {id: "UPDATE_PROFILE", text: "Update Profile"},
        {id: "CONFIGURE_TOTP", text: "Configure Totp"},
        {id: "UPDATE_PASSWORD", text: "Update Password"}
    ];

    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.create) {
            User.save({
                realm: realm.realm
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);

                $location.url("/realms/" + realm.realm + "/users/" + $scope.user.username);
                Notifications.success("The user has been created.");
            });
        } else {
            User.update({
                realm: realm.realm,
                userId: $scope.user.username
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);
                Notifications.success("Your changes have been saved to the user.");
            });
        }
    };

    $scope.reset = function() {
        $scope.user = angular.copy(user);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.realm + "/users");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.user.username, 'user', function() {
            $scope.user.$remove({
                realm : realm.realm,
                userId : $scope.user.username
            }, function() {
                $location.url("/realms/" + realm.realm + "/users");
                Notifications.success("The user has been deleted.");
            }, function() {
                Notifications.error("User couldn't be deleted");
            });
        });
    };
});

module.controller('UserCredentialsCtrl', function($scope, realm, user, User, UserCredentials, Notifications, Dialog) {
    console.log('UserCredentialsCtrl');

    $scope.realm = realm;
    $scope.user = angular.copy(user);
    $scope.temporaryPassword = true;

    $scope.isTotp = false;
    if(!!user.totp){
        $scope.isTotp = user.totp;
    }

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
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.username }, { type : "password", value : $scope.password, temporary: $scope.temporaryPassword }, function() {
                Notifications.success("The password has been reset");
                $scope.password = null;
                $scope.confirmPassword = null;
            }, function() {
                Notifications.error("Failed to reset user password");
            });
        }, function() {
            $scope.password = null;
            $scope.confirmPassword = null;
        });
    };

    $scope.removeTotp = function() {
        Dialog.confirm('Remove totp', 'Are you sure you want to remove the users totp configuration?', function() {
            UserCredentials.removeTotp({ realm: realm.realm, userId: user.username }, { }, function() {
                Notifications.success("The users totp configuration has been removed");
                $scope.user.totp = false;
            }, function() {
                Notifications.error("Failed to remove the users totp configuration");
            });
        });
    };

    $scope.resetPasswordEmail = function() {
        Dialog.confirm('Reset password email', 'Are you sure you want to send password reset email to user?', function() {
            UserCredentials.resetPasswordEmail({ realm: realm.realm, userId: user.username }, { }, function() {
                Notifications.success("Password reset email sent to user");
            }, function() {
                Notifications.error("Failed to send password reset mail to user");
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

module.controller('UserFederationCtrl', function($scope, $location, realm, UserFederationProviders, UserFederationInstances, Notifications, Dialog) {
    console.log('UserFederationCtrl ++++****');
    $scope.realm = realm;
    $scope.providers = UserFederationProviders.query({realm: realm.realm});

    $scope.addProvider = function(provider) {
        console.log('Add provider: ' + provider.id);
        $location.url("/create/user-federation/" + realm.realm + "/providers/" + provider.id);
    };

    $scope.instances = UserFederationInstances.query({realm: realm.realm});

});

module.controller('GenericUserFederationCtrl', function($scope, $location, Notifications, $route, Dialog, realm, instance, providerFactory, UserFederationInstances, UserFederationSync) {
    console.log('GenericUserFederationCtrl');

    $scope.create = !instance.providerName;
    $scope.providerFactory = providerFactory;

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
            });
        } else {
            UserFederationInstances.update({realm: realm.realm,
                    instance: instance.id
                },
                $scope.instance,  function () {
                    $route.reload();
                    Notifications.success("The provider has been updated.");
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

    $scope.triggerFullSync = function() {
        console.log('GenericCtrl: triggerFullSync');
        triggerSync('triggerFullSync');
    }

    $scope.triggerChangedUsersSync = function() {
        console.log('GenericCtrl: triggerChangedUsersSync');
        triggerSync('triggerChangedUsersSync');
    }

    function triggerSync(action) {
        UserFederationSync.get({ action: action, realm: $scope.realm.realm, provider: $scope.instance.id }, function() {
            Notifications.success("Sync of users finished successfully");
        }, function() {
            Notifications.error("Error during sync of users");
        });
    }
});


module.controller('LDAPCtrl', function($scope, $location, $route, Notifications, Dialog, realm, instance, UserFederationInstances, UserFederationSync, RealmLDAPConnectionTester) {
    console.log('LDAPCtrl');
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

            instance.config.batchSizeForSync = DEFAULT_BATCH_SIZE;

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

            if (!instance.config.batchSizeForSync) {
                instance.config.batchSizeForSync = DEFAULT_BATCH_SIZE;
            }
            $scope.fullSyncEnabled = (instance.fullSyncPeriod && instance.fullSyncPeriod > 0);
            $scope.changedSyncEnabled = (instance.changedSyncPeriod && instance.changedSyncPeriod > 0);
        }

        $scope.changed = false;
        $scope.lastVendor = instance.config.vendor;
    }

    initFederationSettings();
    $scope.instance = angular.copy(instance);

    $scope.ldapVendors = [
        { "id": "ad", "name": "Active Directory" },
        { "id": "rhds", "name": "Red Hat Directory Server" },
        { "id": "tivoli", "name": "Tivoli" },
        { "id": "edirectory", "name": "Novell eDirectory" },
        { "id": "other", "name": "Other" }
    ];

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
                $scope.instance.config.usernameLDAPAttribute = "sAMAccountName";
                $scope.instance.config.userObjectClasses = "person, organizationalPerson, user";
            } else {
                $scope.instance.config.usernameLDAPAttribute = "uid";
                $scope.instance.config.userObjectClasses = "inetOrgPerson, organizationalPerson";
            }
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
            });
        } else {
            UserFederationInstances.update({realm: realm.realm,
                                          instance: instance.id
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
            bindCredential: ldapConfig.bindCredential
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
        UserFederationSync.get({ action: action, realm: $scope.realm.realm, provider: $scope.instance.id }, function() {
            Notifications.success("Sync of users finished successfully");
        }, function() {
            Notifications.error("Error during sync of users");
        });
    }

});

