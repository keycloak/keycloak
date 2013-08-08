'use strict';

var module = angular.module('keycloak.controllers', [ 'keycloak.services' ]);

var realmslist = {};


module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications) {
	$scope.addMessage = function() {
		Notifications.success("test");
	};

	$scope.auth = Auth;
    $http.get('/auth-server/rest/saas/whoami').success(function(data, status) {
        Auth.user = data;
        Auth.loggedIn = true;
    })
        .error(function(data, status) {
            Auth.loggedIn = false;
        });

	$scope.$watch(function() {
		return $location.path();
	}, function() {
		$scope.path = $location.path().substring(1).split("/");
	});

    $http.get('/auth-server/rest/saas/admin/realms').success(function(data) {
        Current.realms = data;
        var count = 0;
        var showrealm = false;
        var id = null;
        for (var key in data) {
            if (count > 0) {
                showrealm = false;
                break;
            }
            id = key;
            showrealm = true;
            count++;
        }

        if (showrealm) {
            console.log('default redirect to realm: ' + id);
            Current.realm = Current.realms[id];
            $location.url("/realms/" + id);
        } else {
            //console.log('not redirecting');
        }
    });
});

module.controller('RealmListCtrl', function($scope, Realm, Current) {
	$scope.realms = Realm.get();
    Current.realms = $scope.realms;
});

module.controller('RealmDropdownCtrl', function($scope, Realm, Current, Auth, $location) {
//    Current.realms = Realm.get();
    $scope.current = Current;
    $scope.changeRealm = function() {
        for (var id in Current.realms) {
            var val = Current.realms[id];
            if (val == Current.realm) {
               $location.url("/realms/" + id);
               break;
            }
        }
    };
    $scope.showNav = function() {
        var show = false;
        for (var key in Current.realms) {
            if (typeof Current.realms[key] != "function") {
                if (Current.realms[key] == Current.realm) {
                    $scope.currentRealmId = key;
                }
                show = true;
            }
        }
        return Auth.loggedIn && show;
    }
});

module.controller('RealmDetailCtrl', function($scope, Current, Realm, realm, $location, Dialog, Notifications) {
	$scope.realm = angular.copy(realm);
	$scope.createRealm = !realm.id;

    if ($scope.createRealm) {
        $scope.realm.enabled = true;
        $scope.realm.requireSsl = true;
        $scope.realm.cookieLoginAllowed = true;
        $scope.realm.tokenLifespan = 300;
        $scope.realm.tokenLifespanUnit = 'SECONDS';
        $scope.realm.accessCodeLifespan = 300;
        $scope.realm.accessCodeLifespanUnit = 'SECONDS';
    } else {
        $scope.realm.name = realm.realm;
        $scope.realm.requireSsl = !$scope.realm.sslNotRequired;
        $scope.realm.tokenLifespanUnit = 'SECONDS';
        $scope.realm.acessCodeLifespanUnit = 'SECONDS';
    }

	$scope.changed = $scope.create;

	$scope.$watch('realm', function() {
		if (!angular.equals($scope.realm, realm)) {
			$scope.changed = true;
		}
	}, true);

	$scope.addRole = function() {
		if ($scope.newRole) {
			if ($scope.realm.roles) {
				for ( var i = 0; i < $scope.realm.roles.length; i++) {
					if ($scope.realm.roles[i] == $scope.newRole) {
						Notifications.warn("Role already exists");
						$scope.newRole = null;
						return;
					}
				}
			}

			if (!$scope.realm.roles) {
				$scope.realm.roles = [];
			}

			$scope.realm.roles.push($scope.newRole);
			$scope.newRole = null;
		}
	}

	$scope.removeRole = function(role) {
		Dialog.confirmDelete(role, 'role', function() {
			var i = $scope.realm.roles.indexOf(role);
			if (i > -1) {
				$scope.realm.roles.splice(i, 1);
			}
			
			if ($scope.realm.initialRoles) {
				$scope.removeInitialRole(role);
			}
		});
	};

	$scope.addInitialRole = function() {
		if ($scope.newInitialRole) {
			if (!$scope.realm.initialRoles) {
				$scope.realm.initialRoles = [];
			}

			$scope.realm.initialRoles.push($scope.newInitialRole);
			$scope.newInitialRole = null;
		}
	}

	$scope.removeInitialRole = function(role) {
		var i = $scope.realm.initialRoles.indexOf(role);
		if (i > -1) {
			$scope.realm.initialRoles.splice(i, 1);
		}
	};

	$scope.save = function() {
		if ($scope.realmForm.$valid) {
            var realmCopy = {
                realm: $scope.realm.name,
                enabled: $scope.realm.enabled,
                cookieLoginAllowed: $scope.realm.cookieLoginAllowed,
                sslNotRequired: !$scope.realm.requireSsl,
                tokenLifespan: $scope.realm.tokenLifespan,
                accessCodeLifespan: $scope.realm.accessCodeLifespan

            };

            if ($scope.createRealm) {
				Realm.save(realmCopy, function(data, headers) {
					var l = headers().location;
					var id = l.substring(l.lastIndexOf("/") + 1);

                    var data = Realm.get(function() {
                        Current.realms = data;
                        Current.realm = Current.realms[id];
                    });
                    $location.url("/realms/" + id);
					Notifications.success("Created realm");
				});
			} else {
				Realm.update(realmCopy, function() {
                    Current.realms = Realm.get();
					$scope.changed = false;
					realm = angular.copy($scope.realm);
					Notifications.success("Saved changes to realm");
				});
			}
		} else {
			$scope.realmForm.showErrors = true;
		}
	};

	$scope.reset = function() {
		$scope.realm = angular.copy(realm);
		$scope.changed = false;
		$scope.realmForm.showErrors = false;
	};

	$scope.cancel = function() {
		$location.url("/realms");
	};

	$scope.remove = function() {
		Dialog.confirmDelete($scope.realm.name, 'realm', function() {
			Realm.remove($scope.realm, function() {
                Current.realms = Realm.get();
				$location.url("/realms");
				Notifications.success("Deleted realm");
			});
		});
	};
});


module.controller('UserListCtrl', function($scope, realm, users) {
	$scope.realm = realm;
	$scope.users = users;
});

module.controller('UserDetailCtrl', function($scope, realm, user, User, $location, Dialog, Notifications) {
	$scope.realm = realm;
	$scope.user = angular.copy(user);
	$scope.create = !user.userId;

	$scope.changed = $scope.create;

	$scope.$watch('user', function() {
		if (!angular.equals($scope.user, user)) {
			$scope.changed = true;
		}
	}, true);

	$scope.save = function() {
		if ($scope.userForm.$valid) {



			User.save({
				realm : realm.id
			}, $scope.user, function() {
				$scope.changed = false;
				user = angular.copy($scope.user);

				if ($scope.create) {
					$location.url("/realms/" + realm.id + "/users/" + $scope.user.userId);
					Notifications.success("Created user");
				} else {
					Notifications.success("Saved changes to user");
				}
			});
		} else {
			$scope.userForm.showErrors = true;
		}
	};

	$scope.reset = function() {
		$scope.user = angular.copy(user);
		$scope.changed = false;
		$scope.userForm.showErrors = false;
	};

	$scope.cancel = function() {
		$location.url("/realms/" + realm.id + "/users");
	};

	$scope.remove = function() {
		Dialog.confirmDelete($scope.user.userId, 'user', function() {
			$scope.user.$remove({
				realm : realm.id,
				userId : $scope.user.userId
			}, function() {
				$location.url("/realms/" + realm.id + "/users");
				Notifications.success("Deleted user");
			});
		});
	};
});

module.controller('RoleListCtrl', function($scope, realm, roles) {
    $scope.realm = realm;
    $scope.roles = roles;
});

module.controller('RoleDetailCtrl', function($scope, realm, role, Role, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.$watch('role', function() {
        if (!angular.equals($scope.role, role)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.roleForm.$valid) {

            if ($scope.create) {
                Role.save({
                    realm: realm.id
                }, $scope.role, function (data, headers) {
                    $scope.changed = false;
                    role = angular.copy($scope.role);

                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.id + "/roles/" + id);
                    Notifications.success("Created role");

                });
            } else {
                Role.update({
                    realm : realm.id,
                    roleId : role.id
                }, $scope.role, function() {
                    $scope.changed = false;
                    role = angular.copy($scope.role);
                    Notifications.success("Saved changes to role");
                });
            }

         } else {
            $scope.roleForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.role = angular.copy(role);
        $scope.changed = false;
        $scope.roleForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.id,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.id + "/roles");
                Notifications.success("Deleted role");
            });
        });
    };
});

module.controller('ApplicationListCtrl', function($scope, realm, applications, Application, $location, Dialog, Notifications) {
    console.log('ApplicationListCtrl');
    $scope.realm = realm;
    $scope.selection = {
        applications : angular.copy(applications),
        application : null
    };


    $scope.create = false;

    $scope.changeApplication = function() {
        console.log('ApplicationListCtrl.changeApplication() - ' + $scope.selection.application.name);
        $location.url("/realms/" + realm.id + "/applications/" + $scope.selection.application.id);
    };


});

module.controller('ApplicationDetailCtrl', function($scope, realm, applications, application, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.realm = realm;
    $scope.create = !application.id;
    var selection = {
        applications : null,
        application : null
    };

    selection.applications = angular.copy(applications);

    for (var i=0;i < selection.applications.length; i++) {
        if (selection.applications[i].name == application.name) {
            console.log('app name: ' + application.name);
            selection.application = selection.applications[i];
            break;
        }
    }

    $scope.selection = selection;

    $scope.application = angular.copy(application);



    $scope.changeApplication = function() {
        console.log('ApplicationDetailCtrl.changeApplication() - ' + $scope.selection.application.name);
        $location.url("/realms/" + realm.id + "/applications/" + $scope.selection.application.id);
    };

    $scope.$watch('application', function() {
        if (!angular.equals($scope.application, application)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.applicationForm.$valid) {

            if ($scope.create) {
                Application.save({
                    realm: realm.id
                }, $scope.application, function (data, headers) {
                    $scope.changed = false;
                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.id + "/applications/" + id);
                    Notifications.success("Created application");
                });
            } else {
                Application.update({
                    realm : realm.id,
                    id : application.id
                }, $scope.application, function() {
                    $scope.changed = false;
                    application = angular.copy($scope.application);
                    Notifications.success("Saved changes to application");
                });
            }

        } else {
            $scope.applicationForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.application = angular.copy(application);
        $scope.changed = false;
        $scope.applicationForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/applications");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.application.name, 'application', function() {
            $scope.application.$remove({
                realm : realm.id,
                id : $scope.applicatino.id
            }, function() {
                $location.url("/realms/" + realm.id + "/applications");
                Notifications.success("Deleted application");
            });
        });
    };


});



module.controller('RoleMappingCtrl', function($scope, realm, User, users, role, RoleMapping, Notifications) {
	$scope.realm = realm;
	$scope.realmId = realm.realm || realm.id;
	$scope.allUsers = User.query({ realm : $scope.realmId });
	$scope.users = users;
	$scope.role = role;

	$scope.addUser = function() {
		var user = $scope.newUser;
		$scope.newUser = null;
		
		for ( var i = 0; i < $scope.allUsers.length; i++) {
			if ($scope.allUsers[i].userId == user) {
				user = $scope.allUsers[i];
				RoleMapping.save({
					realm : $scope.realmId,
					role : role
				}, user, function() {
					$scope.users = RoleMapping.query({
						realm : $scope.realmId,
						role : role
					});
					Notifications.success("Added role mapping for user");
				});
			}
		}
	}

	$scope.removeUser = function(userId) {
		for (var i = 0; i < $scope.users.length; i++) {
			var user = $scope.users[i];
			if ($scope.users[i].userId == userId) {
				RoleMapping.delete({
					realm : $scope.realmId,
					role : role
				}, user, function() {
					$scope.users = RoleMapping.query({
						realm : $scope.realmId,
						role : role
					});

					Notifications.success("Removed role mapping for user");
				});
			}
		}
	}
});
