/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.requires.push('ui.ace');

module.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
        .when('/realms/:realm/authz', {
            templateUrl: resourceUrl + '/partials/authz/resource-server-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'ResourceServerCtrl'
        }).when('/realms/:realm/clients/:client/authz/resource-server/create', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            },
            clients: function (ClientListLoader) {
                return ClientListLoader();
            }
        },
        controller: 'ResourceServerDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            },
            clients: function (ClientListLoader) {
                return ClientListLoader();
            },
            serverInfo: function (ServerInfoLoader) {
                return ServerInfoLoader();
            }
        },
        controller: 'ResourceServerDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/export-settings', {
              templateUrl: resourceUrl + '/partials/authz/resource-server-export-settings.html',
              resolve: {
                  realm: function (RealmLoader) {
                      return RealmLoader();
                  },
                  client : function(ClientLoader) {
                      return ClientLoader();
                  },
                  clients: function (ClientListLoader) {
                      return ClientListLoader();
                  },
                  serverInfo: function (ServerInfoLoader) {
                      return ServerInfoLoader();
                  }
              },
              controller: 'ResourceServerDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/evaluate', {
        templateUrl: resourceUrl + '/partials/authz/policy/resource-server-policy-evaluate.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            },
            clients: function (ClientListLoader) {
                return ClientListLoader();
            },
            roles: function (RoleListLoader) {
                return new RoleListLoader();
            }
        },
        controller: 'PolicyEvaluateCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/evaluate/result', {
        templateUrl: resourceUrl + '/partials/authz/policy/resource-server-policy-evaluate-result.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            },
        },
        controller: 'PolicyEvaluateCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/resource', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-resource-list.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerResourceCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/resource/create', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-resource-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerResourceDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/resource/:rsrid', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-resource-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerResourceDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/scope', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-scope-list.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerScopeCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/scope/create', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-scope-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerScopeDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/scope/:id', {
        templateUrl: resourceUrl + '/partials/authz/resource-server-scope-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerScopeDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/permission', {
        templateUrl: resourceUrl + '/partials/authz/permission/resource-server-permission-list.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPermissionCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy', {
        templateUrl: resourceUrl + '/partials/authz/policy/resource-server-policy-list.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/rules/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-drools-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyDroolsDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/rules/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-drools-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyDroolsDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/permission/resource/create', {
        templateUrl: resourceUrl + '/partials/authz/permission/provider/resource-server-policy-resource-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyResourceDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/permission/resource/:id', {
        templateUrl: resourceUrl + '/partials/authz/permission/provider/resource-server-policy-resource-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyResourceDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/permission/scope/create', {
        templateUrl: resourceUrl + '/partials/authz/permission/provider/resource-server-policy-scope-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyScopeDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/permission/scope/:id', {
        templateUrl: resourceUrl + '/partials/authz/permission/provider/resource-server-policy-scope-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyScopeDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/user/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-user-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyUserDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/user/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-user-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyUserDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/client/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-client-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyClientDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/client/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-client-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyClientDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/role/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-role-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyRoleDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/role/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-role-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyRoleDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/group/create', {
          templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-group-detail.html',
          resolve: {
              realm: function (RealmLoader) {
                  return RealmLoader();
              },
              client : function(ClientLoader) {
                  return ClientLoader();
              }
          },
          controller: 'ResourceServerPolicyGroupDetailCtrl'
      }).when('/realms/:realm/clients/:client/authz/resource-server/policy/group/:id', {
          templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-group-detail.html',
          resolve: {
              realm: function (RealmLoader) {
                  return RealmLoader();
              },
              client : function(ClientLoader) {
                  return ClientLoader();
              }
          },
          controller: 'ResourceServerPolicyGroupDetailCtrl'
      }).when('/realms/:realm/clients/:client/authz/resource-server/policy/js/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-js-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyJSDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/js/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-js-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyJSDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/time/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-time-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyTimeDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/time/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-time-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyTimeDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/aggregate/create', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-aggregate-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyAggregateDetailCtrl'
    }).when('/realms/:realm/clients/:client/authz/resource-server/policy/aggregate/:id', {
        templateUrl: resourceUrl + '/partials/authz/policy/provider/resource-server-policy-aggregate-detail.html',
        resolve: {
            realm: function (RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            }
        },
        controller: 'ResourceServerPolicyAggregateDetailCtrl'
    }).when('/realms/:realm/roles/:role/permissions', {
        templateUrl : resourceUrl + '/partials/authz/mgmt/realm-role-permissions.html',
        resolve : {
            realm : function(RealmLoader) {
                return RealmLoader();
            },
            role : function(RoleLoader) {
                return RoleLoader();
            }
        },
        controller : 'RealmRolePermissionsCtrl'
    }).when('/realms/:realm/clients/:client/roles/:role/permissions', {
        templateUrl : resourceUrl + '/partials/authz/mgmt/client-role-permissions.html',
        resolve : {
            realm : function(RealmLoader) {
                return RealmLoader();
            },
            client : function(ClientLoader) {
                return ClientLoader();
            },
            role : function(RoleLoader) {
                return RoleLoader();
            }
        },
        controller : 'ClientRolePermissionsCtrl'
    }).when('/realms/:realm/users-permissions', {
        templateUrl : resourceUrl + '/partials/authz/mgmt/users-permissions.html',
        resolve : {
            realm : function(RealmLoader) {
                return RealmLoader();
            }
        },
        controller : 'UsersPermissionsCtrl'
    })
        .when('/realms/:realm/clients/:client/permissions', {
            templateUrl : resourceUrl + '/partials/authz/mgmt/client-permissions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                client : function(ClientLoader) {
                    return ClientLoader();
                }
            },
            controller : 'ClientPermissionsCtrl'
        })
        .when('/realms/:realm/groups/:group/permissions', {
            templateUrl : resourceUrl + '/partials/authz/mgmt/group-permissions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                group : function(GroupLoader) {
                    return GroupLoader();
                }
            },
            controller : 'GroupPermissionsCtrl'
        })
        .when('/realms/:realm/identity-provider-settings/provider/:provider_id/:alias/permissions', {
            templateUrl : function(params){ return resourceUrl + '/partials/authz/mgmt/broker-permissions.html'; },
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                identityProvider : function(IdentityProviderLoader) {
                    return IdentityProviderLoader();
                }
             },
            controller : 'IdentityProviderPermissionCtrl'
        })
    ;
}]);

module.directive('kcTabsResourceServer', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/authz/kc-tabs-resource-server.html'
    }
});

module.filter('unique', function () {

    return function (items, filterOn) {

        if (filterOn === false) {
            return items;
        }

        if ((filterOn || angular.isUndefined(filterOn)) && angular.isArray(items)) {
            var hashCheck = {}, newItems = [];

            var extractValueToCompare = function (item) {
                if (angular.isObject(item) && angular.isString(filterOn)) {
                    return item[filterOn];
                } else {
                    return item;
                }
            };

            angular.forEach(items, function (item) {
                var valueToCheck, isDuplicate = false;

                for (var i = 0; i < newItems.length; i++) {
                    if (angular.equals(extractValueToCompare(newItems[i]), extractValueToCompare(item))) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    newItems.push(item);
                }

            });
            items = newItems;
        }
        return items;
    };
});

module.filter('toCamelCase', function () {
    return function (input) {
        input = input || '';
        return input.replace(/\w\S*/g, function (txt) {
            return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
        });
    };
})