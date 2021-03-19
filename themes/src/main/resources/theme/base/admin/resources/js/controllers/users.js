module.controller('UserRoleMappingCtrl', function($scope, $http, $route, realm, user, client, Client, Notifications, RealmRoleMapping,
                                                  ClientRoleMapping, AvailableRealmRoleMapping, AvailableClientRoleMapping,
                                                  CompositeRealmRoleMapping, CompositeClientRoleMapping, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.client = client;
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];
    $scope.selectedClient = null;


    $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.id});
    $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
    $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});

    $scope.addRealmRole = function() {
        $scope.realmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm',
                $scope.realmRolesToAdd).then(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.selectedClient) {
                    console.log('load available');
                    $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                    $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                    $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success($translate.instant('user.roles.add.success'));

            });
    };

    $scope.deleteRealmRole = function() {
        $scope.realmRolesToRemove = JSON.parse('[' + $scope.selectedRealmMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm',
            {data : $scope.realmRolesToRemove, headers : {"content-type" : "application/json"}}).then(function() {
                $scope.realmMappings = RealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.selectedClient) {
                    console.log('load available');
                    $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                    $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                    $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                Notifications.success($translate.instant('user.roles.remove.success'));
            });
    };

    $scope.addClientRole = function() {
        $scope.clientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/clients/' + $scope.selectedClient.id,
                $scope.clientRolesToAdd).then(function() {
                $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                Notifications.success($translate.instant('user.roles.add.success'));
            });
    };

    $scope.deleteClientRole = function() {
        $scope.clientRolesToRemove = JSON.parse('[' + $scope.selectedClientMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/clients/' + $scope.selectedClient.id,
            {data : $scope.clientRolesToRemove, headers : {"content-type" : "application/json"}}).then(function() {
                $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
                $scope.selectedClientRoles = [];
                $scope.selectedClientMappings = [];
                $scope.realmComposite = CompositeRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                $scope.realmRoles = AvailableRealmRoleMapping.query({realm : realm.realm, userId : user.id});
                Notifications.success($translate.instant('user.roles.remove.success'));
            });
    };

    $scope.changeClient = function(client) {
        console.log("selected client: ", client);
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        } else {
            $scope.selectedClient = client;
        }
        if ($scope.selectedClient) {
            console.log('load available');
            $scope.clientComposite = CompositeClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
            $scope.clientRoles = AvailableClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
            $scope.clientMappings = ClientRoleMapping.query({realm : realm.realm, userId : user.id, client : $scope.selectedClient.id});
        } else {
            $scope.clientRoles = null;
            $scope.clientMappings = null;
            $scope.clientComposite = null;
        }
        $scope.selectedClientRoles = [];
        $scope.selectedClientMappings = [];
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
});

module.controller('UserSessionsCtrl', function($scope, realm, user, sessions, UserSessions, UserLogout, UserSessionLogout, Notifications, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.sessions = sessions;

    $scope.logoutAll = function() {
        UserLogout.save({realm : realm.realm, user: user.id}, function () {
            Notifications.success($translate.instant('user.logout.all.success'));
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
                Notifications.success($translate.instant('user.logout.session.success'));
            })
        });
    }
});

module.controller('UserFederatedIdentityCtrl', function($scope, $location, realm, user, federatedIdentities, UserFederatedIdentity, Notifications, Dialog, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.federatedIdentities = federatedIdentities;

    $scope.hasAnyProvidersToCreate = function() {
        return realm.identityProviders.length - $scope.federatedIdentities.length > 0;
    }

    $scope.removeProviderLink = function(providerLink) {

        console.log("Removing provider link: " + providerLink.identityProvider);

        Dialog.confirmWithButtonText(
            $translate.instant('user.fedid.link.remove.confirm.title', {name: providerLink.identityProvider}),
            $translate.instant('user.fedid.link.remove.confirm.message', {name: providerLink.identityProvider}),
            $translate.instant('dialogs.delete.confirm'),
            function() {
                UserFederatedIdentity.remove({ realm: realm.realm, user: user.id, provider: providerLink.identityProvider }, function() {
                    Notifications.success($translate.instant('user.fedid.link.remove.success'));
                    var indexToRemove = $scope.federatedIdentities.indexOf(providerLink);
                    $scope.federatedIdentities.splice(indexToRemove, 1);
                });
            }
        );
    }
});

module.controller('UserFederatedIdentityAddCtrl', function($scope, $location, realm, user, federatedIdentities, UserFederatedIdentity, Notifications, $translate) {
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
            Notifications.success($translate.instant('user.fedid.link.add.success'));
        });
    };

    $scope.cancel = function() {
         $location.url("/realms/" + realm.realm + '/users/' + $scope.user.id + '/federated-identity');
    };

});

module.controller('UserConsentsCtrl', function($scope, realm, user, userConsents, UserConsents, Notifications, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.userConsents = userConsents;

    $scope.revokeConsent = function(clientId) {
        UserConsents.delete({realm : realm.realm, user: user.id, client: clientId }, function () {
            UserConsents.query({realm: realm.realm, user: user.id}, function(updated) {
                $scope.userConsents = updated;
            })
            Notifications.success($translate.instant('user.consent.revoke.success'));
        }, function() {
            Notifications.error($translate.instant('user.consent.revoke.error'));
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


module.controller('UserListCtrl', function($scope, realm, User, UserSearchState, UserImpersonation, BruteForce, Notifications, $route, Dialog, $translate) {

    $scope.init = function() {
        $scope.realm = realm;

        UserSearchState.query.realm = realm.realm;
        $scope.query = UserSearchState.query;
        $scope.query.briefRepresentation = 'true';

        if (!UserSearchState.isFirstSearch) $scope.searchQuery();
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
            Notifications.success($translate.instant('user.unlock.success'));
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
            UserSearchState.isFirstSearch = false;
        });
    };

    $scope.removeUser = function(user) {
        Dialog.confirmWithButtonText(
            $translate.instant('user.remove.confirm.title', {name: user.id}),
            $translate.instant('user.remove.confirm.message', {name: user.id}),
            $translate.instant('dialogs.delete.confirm'),
            function() {
                user.$remove({
                    realm : realm.realm,
                    userId : user.id
                }, function() {
                    $route.reload();
    
                    if ($scope.users.length === 1 && $scope.query.first > 0) {
                        $scope.previousPage();
                    }
    
                    Notifications.success($translate.instant('user.remove.success'));
                }, function() {
                    Notifications.error($translate.instant('user.remove.error'));
                });
            }
        );
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
                Notifications.success($translate.instant('user.remove.success'));
            }, function() {
                Notifications.error($translate.instant('user.remove.error'));
            });
        });
    };
});

function loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location) {
        if(user.federationLink) {
            console.log("federationLink is not null. It is " + user.federationLink);

            if ($scope.access.viewRealm) {
                Components.get({realm: realm.realm, componentId: user.federationLink}, function (link) {
                    $scope.federationLinkName = link.name;
                    $scope.federationLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
                });
            } else {
                // KEYCLOAK-4328
                UserStorageOperations.simpleName.get({realm: realm.realm, componentId: user.federationLink}, function (link) {
                    $scope.federationLinkName = link.name;
                    $scope.federationLink = $location.absUrl();
                })
            }

        } else {
            console.log("federationLink is null");
        }

        if(user.origin) {
            if ($scope.access.viewRealm) {
                Components.get({realm: realm.realm, componentId: user.origin}, function (link) {
                    $scope.originName = link.name;
                    $scope.originLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
                })
            }
            else {
                // KEYCLOAK-4328
                UserStorageOperations.simpleName.get({realm: realm.realm, componentId: user.origin}, function (link) {
                    $scope.originName = link.name;
                    $scope.originLink = $location.absUrl();
                })
             }
        } else {
            console.log("origin is null");
        }
};

module.controller('UserDetailCtrl', function($scope, realm, user, BruteForceUser, User,
                                             Components,
                                             UserImpersonation, RequiredActions,
                                             UserStorageOperations,
                                             $location, $http, Dialog, Notifications, $translate, Groups) {
    $scope.realm = realm;
    $scope.create = !user.id;
    $scope.editUsername = $scope.create || $scope.realm.editUsernameAllowed;
    $scope.emailAsUsername = $scope.realm.registrationEmailAsUsername;
    $scope.groupSearch = { selectedGroup : null };

    if ($scope.create) {
        $scope.user = { enabled: true, attributes: {}, groups: [] }
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

        loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location);

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
    console.log("---------------------");
    console.log("ng-model: user.requiredActions=" + JSON.stringify($scope.user.requiredActions));
    console.log("---------------------");
    console.log("ng-repeat: userReqActionList=" + JSON.stringify($scope.userReqActionList));
    console.log("---------------------");
    });
    $scope.$watch('user', function() {
        if (!angular.equals($scope.user, user)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        convertAttributeValuesToLists();

        if ($scope.create) {
            pushSelectedGroupsToUser();

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
                Notifications.success($translate.instant('user.create.success'));
            });
        } else {
            User.update({
                realm: realm.realm,
                userId: $scope.user.id
            }, $scope.user, function () {
                $scope.changed = false;
                convertAttributeValuesToString($scope.user);
                user = angular.copy($scope.user);
                Notifications.success($translate.instant('user.edit.success'));
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

    function pushSelectedGroupsToUser() {
        var groups = $scope.user.groups;
        if ($scope.selectedGroups) {
            for (i = 0; i < $scope.selectedGroups.length; i++) {
                var groupPath = $scope.selectedGroups[i].path;
                if (!groups.includes(groupPath)) {
                    groups.push(groupPath);
                }
            }
        }
    }

    function bfs(tree, collection) {
    	if (!tree["subGroups"] || tree["subGroups"].length === 0) return;
    	for (var i=0; i < tree["subGroups"].length; i++) {
    		var child = tree["subGroups"][i]
    		collection.push(child);
    		bfs(child, collection);
    	}
    	return;
    }

    function flattenGroups(groups) {
        var flattenedGroups = [];
        if (!groups || groups.length === 0) return groups;
        for (var i=0; i < groups.length; i++) {
            flattenedGroups.push(groups[i]);
            bfs(groups[i], flattenedGroups);
        }

        return flattenedGroups;
    }

    /**
     * Only keep groups that :
     *   - include the search term in their path
     *   - are not already selected
     */
    function filterSearchedGroups(groups, term, selectedGroups) {
        if (!groups || groups.length === 0) return groups;
        if (!selectedGroups) selectedGroups = [];

        return groups.filter(group => group.path?.includes(term) && !selectedGroups.some(selGroup => selGroup.id === group.id));
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

    $scope.groupsUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = {results: []};
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            $scope.query = {
                realm: realm.realm,
                search: query.term.trim(),
                max : 20,
                first : 0
            };
            Groups.query($scope.query, function(response) {
                data.results = filterSearchedGroups(flattenGroups(response), query.term.trim(), $scope.selectedGroups);
                query.callback(data);
            });
        },
        formatResult: function(object, container, query) {
            object.text = object.path;
            return object.path;
        }
    };

    $scope.removeGroup = function(list, group) {
        for (i = 0; i < angular.copy(list).length; i++) {
            if (group.id == list[i].id) {
                list.splice(i, 1);
            }
        }
    }

    $scope.selectGroup = function(group) {
        if (!group || !group.id) {
            return;
        }

        $scope.groupSearch.selectedGroup = group;

        if (!$scope.selectedGroups) {
            $scope.selectedGroups = [];
        }

        for (i = 0; i < $scope.selectedGroups.length; i++) {
            if ($scope.selectedGroups[i].id == group.id) {
                return;
            }
        }

        $scope.selectedGroups.push(group);
        $scope.groupSearch.selectedGroup = null;
    }

    $scope.clearGroupSelection = function() {
        $scope.groupSearch.selectedGroup = null;
        $('#groups').val(null).trigger('change.select2');
    }
});

module.controller('UserCredentialsCtrl', function($scope, realm, user, $route, $location, RequiredActions, User, UserExecuteActionsEmail,
                                                  UserCredentials, Notifications, Dialog, TimeUnit2, Components, UserStorageOperations, $modal, $translate) {
    console.log('UserCredentialsCtrl');

    $scope.hasPassword = false;

    loadCredentials();

    loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location);

    $scope.getUserStorageProviderName = function() {
        return user.federationLink ? $scope.federationLinkName : $scope.originName;
    }

    $scope.getUserStorageProviderLink = function() {
        return user.federationLink ? $scope.federationLink : $scope.originLink;
    }

    $scope.updateCredentialLabel = function(credential) {
        UserCredentials.updateCredentialLabel({ realm: realm.realm, userId: user.id, credentialId: credential.id }, {
            'id': credential.id,
            'userLabel': credential.userLabel ? credential.userLabel : "",
            // We JSONify the credential data
            'credentialData': JSON.stringify(credential.credentialData)
        }, function() {
            Notifications.success($translate.instant('user.credential.update.success'));
        }, function(err) {
            Notifications.error($translate.instant('user.credential.update.error'));
            console.log(err);
        });
    }

    $scope.deleteCredential = function(credential) {
        Dialog.confirmWithButtonText(
            $translate.instant('user.credential.remove.confirm.title', {name: credential.id}),
            $translate.instant('user.credential.remove.confirm.message', {name: credential.id}),
            $translate.instant('dialogs.delete.confirm'),
            function() {
                UserCredentials.deleteCredential({ realm: realm.realm, userId: user.id, credentialId: credential.id }, null, function() {
                    Notifications.success($translate.instant('user.credential.remove.success'));
                    $route.reload();
                }, function(err) {
                    Notifications.error($translate.instant('user.credential.remove.error'));
                    console.log(err);
                })
            }
        );
    }

    $scope.moveUp = function(credentials, index) {
        // Safety first
        if (index == 0) {
            return;
        } else if (index == 1) {
            UserCredentials.moveToFirst(
                {
                    realm: realm.realm,
                    userId: user.id,
                    credentialId: credentials[index].id
                },
                function () {
                    $route.reload();
                },
                function (err) {
                    Notifications.error($translate.instant('user.credential.move-top.error'));
                    console.log(err);
                });

        } else {
            UserCredentials.moveCredentialAfter(
                {
                    realm: realm.realm,
                    userId: user.id,
                    credentialId: credentials[index].id,
                    newPreviousCredentialId: credentials[index - 2].id
                },
                function () {
                    $route.reload();
                },
                function (err) {
                    Notifications.error($translate.instant('user.credential.move-up.error'));
                    console.log(err);
                });
        }
    }

    $scope.moveDown = function(credentials, index) {
        // Safety first
        if (index == credentials.length - 1) {
            return;
        }
        UserCredentials.moveCredentialAfter(
            {
                realm: realm.realm,
                userId: user.id,
                credentialId: credentials[index].id,
                newPreviousCredentialId: credentials[index + 1].id
            },
            function() {
                $route.reload();
            },
            function(err) {
                Notifications.error($translate.instant('user.credential.move-down.error'));
                console.log(err);
            });
    }

    $scope.showData = function(credentialData) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/user-credential-data.html',
            controller: 'UserCredentialsDataModalCtrl',
            resolve: {
                credentialData: function () {
                    return credentialData;
                }
            }
        })
    }

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

    function loadCredentials() {
        UserCredentials.getCredentials({ realm: realm.realm, userId: user.id }, null, function(credentials) {
            $scope.credentials = credentials.map(function(c) {
                // We de-JSONify the credential data
                if (c.credentialData) {
                    c.credentialData = JSON.parse(c.credentialData);
                }
                if (c.type == 'password') {
                    $scope.hasPassword = true;
                }
                return c;
            });
        }, function(err) {
            Notifications.error($translate.instant('user.credential.fetch.error'));
            console.log(err);
        });

        UserCredentials.getConfiguredUserStorageCredentialTypes({ realm: realm.realm, userId: user.id }, null, function(userStorageCredentialTypes) {
            $scope.userStorageCredentialTypes =  userStorageCredentialTypes;
            $scope.hasPassword = $scope.hasPassword || userStorageCredentialTypes.lastIndexOf("password") > -1;
        }, function(err) {
            Notifications.error($translate.instant('user.credential.storage.fetch.error'));
            console.log(err);
        });
    }

    $scope.resetPassword = function() {
        // hit enter without entering both fields - ignore
        if (!$scope.passwordAndConfirmPasswordEntered()) return;

        if ($scope.pwdChange) {
            if ($scope.password != $scope.confirmPassword) {
                Notifications.error($translate.instant('user.password.error.not-matching'));
                return;
            }
        }

        var msgTitle = ($scope.hasPassword ? $translate.instant('user.password.reset.confirm.title') : $translate.instant('user.password.set.confirm.title'));
        var msg = ($scope.hasPassword ? $translate.instant('user.password.reset.confirm.message') : $translate.instant('user.password.set.confirm.message'));
        var msgSuccess = ($scope.hasPassword ? $translate.instant('user.password.reset.success') : $translate.instant('user.password.set.success'));

        Dialog.confirm(msgTitle, msg, function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id }, { type : "password", value : $scope.password, temporary: $scope.temporaryPassword }, function() {
                Notifications.success(msgSuccess);
                $scope.password = null;
                $scope.confirmPassword = null;
                $route.reload();
            });
        }, function() {
            $scope.password = null;
            $scope.confirmPassword = null;
        });
    };

    $scope.passwordAndConfirmPasswordEntered = function() {
        return $scope.password && $scope.confirmPassword;
    }

    $scope.disableCredentialTypes = function() {
        Dialog.confirm(
            $translate.instant('user.credential.disable.confirm.title'),
            $translate.instant('user.credential.disable.confirm.message'),
            function() {
            UserCredentials.disableCredentialTypes({ realm: realm.realm, userId: user.id }, $scope.disableableCredentialTypes, function() {
                $route.reload();
                Notifications.success($translate.instant('user.credential.disable.confirm.success'));
            }, function() {
                Notifications.error($translate.instant('user.credential.disable.confirm.error'));
            });
        });
    };

    $scope.emailActions = [];
    $scope.emailActionsTimeout = TimeUnit2.asUnit(realm.actionTokenGeneratedByAdminLifespan);
    $scope.disableableCredentialTypes = [];

    $scope.sendExecuteActionsEmail = function() {
        if ($scope.changed) {
            Dialog.message($translate.instant('user.actions-email.send.pending-changes.title'),
                $translate.instant('user.actions-email.send.pending-changes.message'));
            return;
        }
        Dialog.confirm(
            $translate.instant('user.actions-email.send.confirm.title'),
            $translate.instant('user.actions-email.send.confirm.message'),
            function() {
            UserExecuteActionsEmail.update({ realm: realm.realm, userId: user.id, lifespan: $scope.emailActionsTimeout.toSeconds() }, $scope.emailActions, function() {
                Notifications.success($translate.instant('user.actions-email.send.confirm.success'));
            }, function() {
                Notifications.error($translate.instant('user.actions-email.send.confirm.error'));
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

module.controller('UserCredentialsDataModalCtrl', function($scope, credentialData) {
    $scope.credentialData = credentialData;

    $scope.keys = function(object) {
        return object ? Object.keys(object) : [];
    }
});

module.controller('UserFederationCtrl', function($scope, $location, $route, realm, serverInfo, Components, Notifications, Dialog, $translate) {
    console.log('UserFederationCtrl ++++****');
    $scope.realm = realm;
    $scope.providers = serverInfo.componentTypes['org.keycloak.storage.UserStorageProvider'];
    $scope.instancesLoaded = false;

    if (!$scope.providers) $scope.providers = [];

    $scope.addProvider = function(provider) {
        console.log('Add provider: ' + provider.id);
        $location.url("/create/user-storage/" + realm.realm + "/providers/" + provider.id);
    };

    $scope.getInstanceLink = function(instance) {
        return "/realms/" + realm.realm + "/user-storage/providers/" + instance.providerId + "/" + instance.id;
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
            console.log('getInstancePriority is undefined');
            return -1;
        }
        return +instance.config['priority'][0];
    }

    Components.query({realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.storage.UserStorageProvider'
    }, function(data) {
        $scope.instances = data;
        $scope.instancesLoaded = true;
    });

    $scope.removeInstance = function(instance) {
        Dialog.confirmWithButtonText(
            $translate.instant('user.storage.remove.confirm.title', {name: instance.name}),
            $translate.instant('user.storage.remove.confirm.message', {name: instance.name}),
            $translate.instant('dialogs.delete.confirm'),
            function() {
                Components.remove({
                    realm : realm.realm,
                    componentId : instance.id
                }, function() {
                    $route.reload();
                    Notifications.success($translate.instant('user.storage.remove.success'));
                });
            }
        );
    };
});

module.controller('GenericUserStorageCtrl', function($scope, $location, Notifications, $route, Dialog, realm,
                                                     serverInfo, instance, providerId, Components, UserStorageOperations, $translate) {
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
    $scope.showSync = false;
    $scope.changed = false;

    console.log("providerFactory: " + providerFactory.id);

    function initUserStorageSettings() {
        if ($scope.create) {
            $scope.changed = true;
            instance.name = providerFactory.id;
            instance.providerId = providerFactory.id;
            instance.providerType = 'org.keycloak.storage.UserStorageProvider';
            instance.parentId = realm.id;
            instance.config = {

            };
            instance.config['priority'] = ["0"];
            instance.config['enabled'] = ["true"];

            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;
            if (providerFactory.metadata.synchronizable) {
                instance.config['fullSyncPeriod'] = ['-1'];
                instance.config['changedSyncPeriod'] = ['-1'];

            }
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
            $scope.fullSyncEnabled = (instance.config['fullSyncPeriod'] && instance.config['fullSyncPeriod'][0] > 0);
            $scope.changedSyncEnabled = (instance.config['changedSyncPeriod'] && instance.config['changedSyncPeriod'][0]> 0);
            if (providerFactory.metadata.synchronizable) {
                if (!instance.config['fullSyncPeriod']) {
                    console.log('setting to -1');
                    instance.config['fullSyncPeriod'] = ['-1'];

                }
                if (!instance.config['changedSyncPeriod']) {
                    console.log('setting to -1');
                    instance.config['changedSyncPeriod'] = ['-1'];

                }
            }
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
        if (providerFactory.metadata.synchronizable) {
            if (instance.config && instance.config['importEnabled']) {
                $scope.showSync = instance.config['importEnabled'][0] == 'true';
            } else {
                $scope.showSync = true;
            }
        }

    }

    initUserStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

     $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

    }, true);

    $scope.$watch('fullSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['fullSyncPeriod'][0] = $scope.fullSyncEnabled ? "604800" : "-1";
        $scope.changed = true;
    });

    $scope.$watch('changedSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['changedSyncPeriod'][0] = $scope.changedSyncEnabled ? "86400" : "-1";
        $scope.changed = true;
    });


    $scope.save = function() {
        console.log('save provider');
        $scope.changed = false;
        if ($scope.create) {
            console.log('saving new provider');
            Components.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/user-storage/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success($translate.instant('user.storage.create.success'));
            });
        } else {
            console.log('update existing provider');
            Components.update({realm: realm.realm,
                    componentId: instance.id
                },
                $scope.instance,  function () {
                    $route.reload();
                    Notifications.success($translate.instant('user.storage.edit.success'));
                });
        }
    };

    $scope.reset = function() {
        //initUserStorageSettings();
        //$scope.instance = angular.copy(instance);
        $route.reload();
    };

    $scope.cancel = function() {
        console.log('cancel');
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
        UserStorageOperations.sync.save({ action: action, realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function(syncResult) {
            $route.reload();
            Notifications.success($translate.instant('user.storage.sync.success',{status: syncResult.status}));
        }, function() {
            $route.reload();
            Notifications.error($translate.instant('user.storage.sync.error'));
        });
    }
    $scope.removeImportedUsers = function() {
        UserStorageOperations.removeImportedUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function(syncResult) {
            $route.reload();
            Notifications.success($translate.instant('user.storage.remove-users.success'));
        }, function() {
            $route.reload();
            Notifications.error($translate.instant('user.storage.remove-users.error'));
        });
    };
    $scope.unlinkUsers = function() {
        UserStorageOperations.unlinkUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function(syncResult) {
            $route.reload();
            Notifications.success($translate.instant('user.storage.unlink.success'));
        }, function() {
            $route.reload();
            Notifications.error($translate.instant('user.storage.unlink.error'));
        });
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

module.controller('UserGroupMembershipCtrl', function($scope, $q, realm, user, UserGroupMembership, UserGroupMembershipCount, UserGroupMapping, Notifications, Groups, GroupsCount, ComponentUtils, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.groupList = [];
    $scope.allGroupMemberships = [];
    $scope.groupMemberships = [];
    $scope.tree = [];
    $scope.membershipTree = [];

    $scope.searchCriteria = '';
    $scope.searchCriteriaMembership = '';
    $scope.currentPage = 1;
    $scope.currentMembershipPage = 1;
    $scope.currentPageInput = $scope.currentPage;
    $scope.currentMembershipPageInput = $scope.currentMembershipPage;
    $scope.pageSize = 20;
    $scope.numberOfPages = 1;
    $scope.numberOfMembershipPages = 1;

    var refreshCompleteUserGroupMembership = function() {
        var queryParams = {
            realm : realm.realm,
            userId: user.id
        };

        var promiseGetCompleteUserGroupMembership = $q.defer();
        UserGroupMembership.query(queryParams, function(entry) {
            promiseGetCompleteUserGroupMembership.resolve(entry);
        }, function() {
            promiseGetCompleteUserGroupMembership.reject($translate.instant('user.groups.fetch.all.error', {params: queryParams}));
        });
        promiseGetCompleteUserGroupMembership.promise.then(function(groups) {
            for (var i = 0; i < groups.length; i++) {
                $scope.allGroupMemberships.push(groups[i]);
                $scope.getGroupClass(groups[i]);
            }
        }, function (failed) {
            Notifications.error(failed);
        });
        return promiseGetCompleteUserGroupMembership.promise;
    };

    var refreshUserGroupMembership = function (search) {
        $scope.currentMembershipPageInput = $scope.currentMembershipPage;
        var first = ($scope.currentMembershipPage * $scope.pageSize) - $scope.pageSize;
        var queryParams = {
            realm : realm.realm,
            userId: user.id,
            first : first,
            max : $scope.pageSize
        };

        var countParams = {
            realm : realm.realm,
            userId: user.id
        };

        var isSearch = function() {
            return angular.isDefined(search) && search !== '';
        };

        if (isSearch()) {
            queryParams.search = search;
            countParams.search = search;
        }

        var promiseGetUserGroupMembership = $q.defer();
        UserGroupMembership.query(queryParams, function(entry) {
            promiseGetUserGroupMembership.resolve(entry);
        }, function() {
            promiseGetUserGroupMembership.reject($translate.instant('user.groups.fetch.error', {params: queryParams}));
        });

        var promiseMembershipCount = $q.defer();

        promiseGetUserGroupMembership.promise.then(function(groups) {
            $scope.groupMemberships = groups;
            UserGroupMembershipCount.query(countParams, function(entry) {
                promiseMembershipCount.resolve(entry);
            }, function() {
                promiseMembershipCount.reject($translate.instant('user.groups.fetch.error', {params: countParams}));
            });
            promiseMembershipCount.promise.then(function(membershipEntry) {
                if(angular.isDefined(membershipEntry.count) && membershipEntry.count > $scope.pageSize) {
                    $scope.numberOfMembershipPages = Math.ceil(membershipEntry.count/$scope.pageSize);
                } else {
                    $scope.numberOfMembershipPages = 1;
                }
                if (parseInt($scope.currentMembershipPage, 10) > $scope.numberOfMembershipPages) {
                    $scope.currentMembershipPage = $scope.numberOfMembershipPages;
                }
            }, function (failed) {
                Notifications.error(failed);
            });
        }, function (failed) {
            Notifications.error(failed);
        });

        return promiseMembershipCount.promise;
    };

    var refreshAvailableGroups = function (search) {
        $scope.currentPageInput = $scope.currentPage;
        var first = ($scope.currentPage * $scope.pageSize) - $scope.pageSize;
        var queryParams = {
            realm : realm.realm,
            first : first,
            max : $scope.pageSize
        };

        var countParams = {
            realm : realm.realm,
            top : 'true'
        };

        if(angular.isDefined(search) && search !== '') {
            queryParams.search = search;
            countParams.search = search;
        }

        var promiseGetGroups = $q.defer();
        Groups.query(queryParams, function(entry) {
            promiseGetGroups.resolve(entry);
        }, function() {
            promiseGetGroups.reject($translate.instant('user.groups.fetch.error', {params: queryParams}));
        });

        var promiseCount = $q.defer();

        promiseGetGroups.promise.then(function(groups) {
            $scope.groupList = ComponentUtils.sortGroups('name', groups);
            GroupsCount.query(countParams, function(entry) {
                promiseCount.resolve(entry);
            }, function() {
                promiseCount.reject($translate.instant('user.groups.fetch.error', {params: countParams}));
            });
            promiseCount.promise.then(function(entry) {
                if(angular.isDefined(entry.count) && entry.count > $scope.pageSize) {
                    $scope.numberOfPages = Math.ceil(entry.count/$scope.pageSize);
                } else {
                    $scope.numberOfPages = 1;
                }
            }, function (failed) {
                Notifications.error(failed);
            });
        }, function (failed) {
            Notifications.error(failed);
        });

        return promiseCount.promise;
    };

    $scope.clearSearchMembership = function() {
        $scope.searchCriteriaMembership = '';
        if (parseInt($scope.currentMembershipPage, 10) === 1) {
            refreshUserGroupMembership();
        } else {
            $scope.currentMembershipPage = 1;
        }
    };

    $scope.searchGroupMembership = function() {
        if (parseInt($scope.currentMembershipPage, 10) === 1) {
            refreshUserGroupMembership($scope.searchCriteriaMembership);
        } else {
            $scope.currentMembershipPage = 1;
        }
    };

    refreshUserGroupMembership().then(function() {
        refreshAvailableGroups();
        refreshCompleteUserGroupMembership();
    });

    $scope.$watch('currentPage', function(newValue, oldValue) {
        if(parseInt(newValue, 10) !== parseInt(oldValue, 10)) {
            refreshAvailableGroups($scope.searchCriteria);
        }
    });

    $scope.$watch('currentMembershipPage', function(newValue, oldValue) {
        if(parseInt(newValue, 10) !== parseInt(oldValue, 10)) {
            refreshUserGroupMembership($scope.searchCriteriaMembership);
        }
    });

    $scope.clearSearch = function() {
        $scope.searchCriteria = '';
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshAvailableGroups();
        } else {
            $scope.currentPage = 1;
        }
    };

    $scope.searchGroup = function() {
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshAvailableGroups($scope.searchCriteria);
        } else {
            $scope.currentPage = 1;
        }
    };

    $scope.joinGroup = function() {
        if (!$scope.tree.currentNode) {
            Notifications.error($translate.instant('user.groups.join.error.no-group-selected'));
            return;
        }
        if (isMember($scope.tree.currentNode)) {
            Notifications.error($translate.instant('user.groups.join.error.already-added'));
            return;
        }
        UserGroupMapping.update({realm: realm.realm, userId: user.id, groupId: $scope.tree.currentNode.id}, function() {
            $scope.allGroupMemberships.push($scope.tree.currentNode);
            refreshUserGroupMembership($scope.searchCriteriaMembership);
            Notifications.success($translate.instant('user.groups.join.success'));
        });

    };

    $scope.leaveGroup = function() {
        if (!$scope.membershipTree.currentNode) {
            Notifications.error($translate.instant('user.groups.leave.error.no-group-selected'));
            return;
        }
        UserGroupMapping.remove({realm: realm.realm, userId: user.id, groupId: $scope.membershipTree.currentNode.id}, function () {
            removeGroupMember($scope.allGroupMemberships, $scope.membershipTree.currentNode);
            refreshUserGroupMembership($scope.searchCriteriaMembership);
            Notifications.success($translate.instant('user.groups.leave.success'));
        });

    };

    var isLeaf = function(node) {
        return node.id !== 'realm' && (!node.subGroups || node.subGroups.length === 0);
    };

    var isMember = function(node) {
        for (var i = 0; i < $scope.allGroupMemberships.length; i++) {
            var member = $scope.allGroupMemberships[i];
            if (node.id === member.id) {
                return true;
            }
        }
        return false;
    };

    $scope.getGroupClass = function(node) {
        if (node.id == "realm") {
            return 'pficon pficon-users';
        }
        if (isMember(node)) {
            return 'normal deactivate';
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
            if (isMember(node)) {
                return "deactivate_selected";
            } else {
                return 'selected';
            }
        } else if ($scope.cutNode && $scope.cutNode.id === node.id) {
            return 'cut';
        }
        return undefined;
    }

});

module.controller('LDAPUserStorageCtrl', function($scope, $location, Notifications, $route, Dialog, realm,
                                                    serverInfo, instance, Components, UserStorageOperations,
                                                    RealmLDAPConnectionTester, $http) {
    console.log('LDAPUserStorageCtrl');
    var providerId = 'ldap';
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
    $scope.showSync = false;

    if (serverInfo.profileInfo.name == 'community') {
        $scope.ldapVendors = [
            {"id": "ad", "name": "Active Directory"},
            {"id": "rhds", "name": "Red Hat Directory Server"},
            {"id": "tivoli", "name": "Tivoli"},
            {"id": "edirectory", "name": "Novell eDirectory"},
            {"id": "other", "name": "Other"}
        ];
    } else {
        $scope.ldapVendors = [
            {"id": "ad", "name": "Active Directory"},
            {"id": "rhds", "name": "Red Hat Directory Server"}
        ];
    }

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


    console.log("providerFactory: " + providerFactory.id);

    $scope.changed = false;
    function initUserStorageSettings() {
        if ($scope.create) {
            $scope.changed = true;
            instance.name = 'ldap';
            instance.providerId = 'ldap';
            instance.providerType = 'org.keycloak.storage.UserStorageProvider';
            instance.parentId = realm.id;
            instance.config = {

            };
            instance.config['enabled'] = ["true"];
            instance.config['priority'] = ["0"];

            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;
            instance.config['fullSyncPeriod'] = ['-1'];
            instance.config['changedSyncPeriod'] = ['-1'];
            instance.config['cachePolicy'] = ['DEFAULT'];
            instance.config['evictionDay'] = [''];
            instance.config['evictionHour'] = [''];
            instance.config['evictionMinute'] = [''];
            instance.config['maxLifespan'] = [''];
            instance.config['batchSizeForSync'] = [DEFAULT_BATCH_SIZE];
            //instance.config['importEnabled'] = ['true'];

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
            $scope.fullSyncEnabled = (instance.config['fullSyncPeriod'] && instance.config['fullSyncPeriod'][0] > 0);
            $scope.changedSyncEnabled = (instance.config['changedSyncPeriod'] && instance.config['changedSyncPeriod'][0]> 0);
            if (!instance.config['fullSyncPeriod']) {
                console.log('setting to -1');
                instance.config['fullSyncPeriod'] = ['-1'];

            }
            if (!instance.config['enabled']) {
                instance.config['enabled'] = ['true'];
            }
            if (!instance.config['changedSyncPeriod']) {
                console.log('setting to -1');
                instance.config['changedSyncPeriod'] = ['-1'];

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
            if (!instance.config['importEnabled']) {
                instance.config['importEnabled'] = ['true'];
            }

            if (providerFactory.properties) {

                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (!instance.config[configProperty.name]) {
                        if (configProperty.defaultValue) {
                            instance.config[configProperty.name] = [configProperty.defaultValue];
                        } else {
                            instance.config[configProperty.name] = [''];
                        }
                    }

                }
            }

            for (var i=0 ; i<$scope.ldapVendors.length ; i++) {
                if ($scope.ldapVendors[i].id === instance.config['vendor'][0]) {
                    $scope.vendorName = $scope.ldapVendors[i].name;
                }
            };



        }
        if (instance.config && instance.config['importEnabled']) {
            $scope.showSync = instance.config['importEnabled'][0] == 'true';
        } else {
            $scope.showSync = true;
        }

        $scope.lastVendor = instance.config['vendor'][0];
    }

    initUserStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

    $scope.$watch('instance', function() {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

        if (!angular.equals($scope.instance.config['vendor'][0], $scope.lastVendor)) {
            console.log("LDAP vendor changed. Previous=" + $scope.lastVendor + " New=" + $scope.instance.config['vendor'][0]);
            $scope.lastVendor = $scope.instance.config['vendor'][0];

            if ($scope.lastVendor === "ad") {
                $scope.instance.config['usernameLDAPAttribute'][0] = "cn";
                $scope.instance.config['userObjectClasses'][0] = "person, organizationalPerson, user";
            } else {
                $scope.instance.config['usernameLDAPAttribute'][0] = "uid";
                $scope.instance.config['userObjectClasses'][0] = "inetOrgPerson, organizationalPerson";
            }

            $scope.instance.config['rdnLDAPAttribute'][0] = $scope.instance.config['usernameLDAPAttribute'][0];

            var vendorToUUID = {
                rhds: "nsuniqueid",
                tivoli: "uniqueidentifier",
                edirectory: "guid",
                ad: "objectGUID",
                other: "entryUUID"
            };
            $scope.instance.config['uuidLDAPAttribute'][0] = vendorToUUID[$scope.lastVendor];
        }


    }, true);

    $scope.$watch('fullSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['fullSyncPeriod'][0] = $scope.fullSyncEnabled ? "604800" : "-1";
        $scope.changed = true;
    });

    $scope.$watch('changedSyncEnabled', function(newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['changedSyncPeriod'][0] = $scope.changedSyncEnabled ? "86400" : "-1";
        $scope.changed = true;
    });


    $scope.save = function() {
        $scope.changed = false;
        if (!$scope.instance.config['batchSizeForSync'] || !parseInt($scope.instance.config['batchSizeForSync'][0])) {
            $scope.instance.config['batchSizeForSync'] = [ DEFAULT_BATCH_SIZE ];
        } else {
            $scope.instance.config['batchSizeForSync'][0] = parseInt($scope.instance.config.batchSizeForSync).toString();
        }

        if ($scope.create) {
            Components.save({realm: realm.realm}, $scope.instance,  function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/user-storage/providers/" + $scope.instance.providerId + "/" + id);
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
        UserStorageOperations.sync.save({ action: action, realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function(syncResult) {
            $route.reload();
            Notifications.success("Sync of users finished successfully. " + syncResult.status);
        }, function() {
            $route.reload();
            Notifications.error("Error during sync of users");
        });
    }
    $scope.removeImportedUsers = function() {
        UserStorageOperations.removeImportedUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function(syncResult) {
            $route.reload();
            Notifications.success("Remove imported users finished successfully. ");
        }, function() {
            $route.reload();
            Notifications.error("Error during remove");
        });
    };
    $scope.unlinkUsers = function() {
        UserStorageOperations.unlinkUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function(syncResult) {
            $route.reload();
            Notifications.success("Unlink of users finished successfully. ");
        }, function() {
            $route.reload();
            Notifications.error("Error during unlink");
        });
    };

    var initConnectionTest = function(testAction, ldapConfig) {
        return {
            action: testAction,
            connectionUrl: ldapConfig.connectionUrl && ldapConfig.connectionUrl[0],
            authType: ldapConfig.authType && ldapConfig.authType[0],
            bindDn: ldapConfig.bindDn && ldapConfig.bindDn[0],
            bindCredential: ldapConfig.bindCredential && ldapConfig.bindCredential[0],
            useTruststoreSpi: ldapConfig.useTruststoreSpi && ldapConfig.useTruststoreSpi[0],
            connectionTimeout: ldapConfig.connectionTimeout && ldapConfig.connectionTimeout[0],
            startTls: ldapConfig.startTls && ldapConfig.startTls[0],
            componentId: instance.id
        };
    };

    $scope.testConnection = function() {
        console.log('LDAPCtrl: testConnection');
        RealmLDAPConnectionTester.save({realm: realm.realm}, initConnectionTest("testConnection", $scope.instance.config), function() {
            Notifications.success("LDAP connection successful.");
        }, function() {
            Notifications.error("Error when trying to connect to LDAP. See server.log for details.");
        });
    }

    $scope.testAuthentication = function() {
        console.log('LDAPCtrl: testAuthentication');
        RealmLDAPConnectionTester.save({realm: realm.realm}, initConnectionTest("testAuthentication", $scope.instance.config), function() {
            Notifications.success("LDAP authentication successful.");
        }, function() {
            Notifications.error("LDAP authentication failed. See server.log for details");
        });
    }

    $scope.queryAndSetLdapSupportedExtensions = function() {
        console.log('LDAPCtrl: getLdapSupportedExtensions');
        const PASSWORD_MODIFY_OID = '1.3.6.1.4.1.4203.1.11.1';

        $http.post(
            authUrl + '/admin/realms/' + realm.realm + '/ldap-server-capabilities',
            initConnectionTest("queryServerCapabilities", $scope.instance.config)).then(
            function(response) {
                Notifications.success("LDAP supported extensions successfully requested.");
                const ldapOids = response.data;
                if (angular.isArray(ldapOids)) {
                    const passwordModifyOid = ldapOids.filter(function(ldapOid) { return ldapOid.oid === PASSWORD_MODIFY_OID; });
                    $scope.instance.config['usePasswordModifyExtendedOp'][0] = (passwordModifyOid.length > 0).toString();
                }
            },
            function() {
                Notifications.error("Error when trying to request supported extensions of LDAP. See server.log for details.");
            });
    }

});

module.controller('LDAPTabCtrl', function(Dialog, $scope, Current, Notifications, $location) {
    $scope.removeUserFederation = function() {
        Dialog.confirmDelete($scope.instance.name, 'ldap provider', function() {
            $scope.instance.$remove({
                realm : Current.realm.realm,
                componentId : $scope.instance.id
            }, function() {
                $location.url("/realms/" + Current.realm.realm + "/user-federation");
                Notifications.success("The provider has been deleted.");
            });
        });
    };
});


module.controller('LDAPMapperListCtrl', function($scope, $location, Notifications, $route, Dialog, realm, provider, mappers) {
    console.log('LDAPMapperListCtrl');

    $scope.realm = realm;
    $scope.provider = provider;
    $scope.instance = provider;

    $scope.mappers = mappers;

});

module.controller('LDAPMapperCtrl', function($scope, $route, realm,  provider, mapperTypes, mapper, clients, Components, LDAPMapperSync, Notifications, Dialog, $location) {
    console.log('LDAPMapperCtrl');
    $scope.realm = realm;
    $scope.provider = provider;
    $scope.clients = clients;
    $scope.create = false;
    $scope.changed = false;

    for (var i = 0; i < mapperTypes.length; i++) {
        console.log('mapper.providerId: ' + mapper.providerId);
        console.log('mapperTypes[i].id ' + mapperTypes[i].id);
        if (mapperTypes[i].id == mapper.providerId) {
            $scope.mapperType = mapperTypes[i];
            break;
        }
    }

    if ($scope.mapperType.properties) {

        for (var i = 0; i < $scope.mapperType.properties.length; i++) {
            var configProperty = $scope.mapperType.properties[i];
            if (!mapper.config[configProperty.name]) {
                if (configProperty.defaultValue) {
                    mapper.config[configProperty.name] = [configProperty.defaultValue];
                } else {
                    mapper.config[configProperty.name] = [''];
                }
            }

        }
    }
    $scope.mapper = angular.copy(mapper);


    $scope.$watch('mapper', function() {
        if (!angular.equals($scope.mapper, mapper)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        Components.update({realm: realm.realm,
                componentId: mapper.id
            },
            $scope.mapper,  function () {
                $route.reload();
                Notifications.success("The mapper has been updated.");
            });
    };

    $scope.reset = function() {
        $scope.mapper = angular.copy(mapper);
        $scope.changed = false;
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.mapper.name, 'ldap mapper', function() {
            Components.remove({
                realm : realm.realm,
                componentId : mapper.id
            }, function() {
                $location.url("/realms/" + realm.realm + '/ldap-mappers/' + provider.id);
                Notifications.success("The provider has been deleted.");
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
        LDAPMapperSync.save({ direction: direction, realm: realm.realm, parentId: provider.id, mapperId : $scope.mapper.id }, {}, function(syncResult) {
            Notifications.success("Data synced successfully. " + syncResult.status);
        }, function(error) {
            Notifications.error(error.data.errorMessage);
        });
    }

});

module.controller('LDAPMapperCreateCtrl', function($scope, realm, provider, mapperTypes, clients, Components, Notifications, Dialog, $location) {
    console.log('LDAPMapperCreateCtrl');
    $scope.realm = realm;
    $scope.provider = provider;
    $scope.clients = clients;
    $scope.create = true;
    $scope.mapper = { config: {}};
    $scope.mapperTypes = mapperTypes;
    $scope.mapperType = null;
    $scope.changed = true;

    $scope.$watch('mapperType', function() {
        if ($scope.mapperType != null) {
            $scope.mapper.config = {};
            if ($scope.mapperType.properties) {

                for (var i = 0; i < $scope.mapperType.properties.length; i++) {
                    var configProperty = $scope.mapperType.properties[i];
                    if (!$scope.mapper.config[configProperty.name]) {
                        if (configProperty.defaultValue) {
                            $scope.mapper.config[configProperty.name] = [configProperty.defaultValue];
                        } else {
                            $scope.mapper.config[configProperty.name] = [''];
                        }
                    }

                }
            }
        }
    }, true);

    $scope.save = function() {
        if ($scope.mapperType == null) {
            Notifications.error("You need to select mapper type!");
            return;
        }

        $scope.mapper.providerId = $scope.mapperType.id;
        $scope.mapper.providerType = 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper';
        $scope.mapper.parentId = provider.id;

        if ($scope.mapper.config && $scope.mapper.config["role"] && !Array.isArray($scope.mapper.config["role"])) {
            $scope.mapper.config["role"] = [$scope.mapper.config["role"]];
        }

        Components.save({realm: realm.realm}, $scope.mapper,  function (data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);

            $location.url("/realms/" + realm.realm + "/ldap-mappers/" + $scope.mapper.parentId + "/mappers/" + id);
            Notifications.success("The mapper has been created.");
        });
    };

    $scope.reset = function() {
        $location.url("/realms/" + realm.realm + '/ldap-mappers/' + provider.id);
    };


});
