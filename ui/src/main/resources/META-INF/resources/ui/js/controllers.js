'use strict';

var module = angular.module('keycloak.controllers', [ 'keycloak.services' ]);

module.controller('GlobalCtrl', function($scope, Auth, $location, Notifications) {
	$scope.addMessage = function() {
		Notifications.success("test");
	};

	$scope.auth = Auth;

	$scope.$watch(function() {
		return $location.path();
	}, function() {
		$scope.path = $location.path().substring(1).split("/");
	});
});

module.controller('ApplicationListCtrl', function($scope, Application) {
	$scope.applications = Application.query();
});

module.controller('ApplicationDetailCtrl', function($scope, application, Application, realms, $location, $window, Dialog,
		Notifications) {
	$scope.application = angular.copy(application);
	$scope.realms = realms;

	$scope.create = !application.id;
	$scope.changed = $scope.create;

	$scope.$watch('application', function() {
		if (!angular.equals($scope.application, application)) {
			$scope.changed = true;
		}
	}, true);

	$scope.addRole = function() {
		if ($scope.newRole) {
			if ($scope.application.roles) {
				for ( var i = 0; i < $scope.application.roles.length; i++) {
					if ($scope.application.roles[i] == $scope.newRole) {
						Notifications.warn("Role already exists");
						$scope.newRole = null;
						return;
					}
				}
			}

			if (!$scope.application.roles) {
				$scope.application.roles = [];
			}

			$scope.application.roles.push($scope.newRole);
			$scope.newRole = null;
		}
	}

	$scope.removeRole = function(role) {
		Dialog.confirmDelete(role, 'role', function() {
			var i = $scope.application.roles.indexOf(role);
			if (i > -1) {
				$scope.application.roles.splice(i, 1);
			}
			
			if ($scope.application.initialRoles) {
				$scope.removeInitialRole(role);
			}
		});
	};

	$scope.addInitialRole = function() {
		if ($scope.newInitialRole) {
			if (!$scope.application.initialRoles) {
				$scope.application.initialRoles = [];
			}

			$scope.application.initialRoles.push($scope.newInitialRole);
			$scope.newInitialRole = null;
		}
	}

	$scope.removeInitialRole = function(role) {
		var i = $scope.application.initialRoles.indexOf(role);
		if (i > -1) {
			$scope.application.initialRoles.splice(i, 1);
		}
	};

	$scope.save = function() {
		if ($scope.applicationForm.$valid) {
			if ($scope.create) {
				Application.save($scope.application, function(data, headers) {
					var l = headers().location;
					var id = l.substring(l.lastIndexOf("/") + 1);
					$location.url("/applications/" + id);
					Notifications.success("Created application");
				});
			} else {
				Application.update($scope.application, function() {
					$scope.changed = false;
					application = angular.copy($scope.application);
					Notifications.success("Saved changes to the application");
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
		$location.url("/applications");
	};

	$scope.remove = function() {
		Dialog.confirmDelete($scope.application.name, 'application', function() {
			$scope.application.$remove(function() {
				$location.url("/applications");
				Notifications.success("Deleted application");
			});
		});
	};
});


module.controller('RealmListCtrl', function($scope, Realm) {
	$scope.realms = Realm.query();
});

module.controller('RealmDetailCtrl', function($scope, Realm, realm, $location, Dialog, Notifications) {
	$scope.realm = angular.copy(realm);
	$scope.create = !realm.name;

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
			if ($scope.create) {
				Realm.save($scope.realm, function(data, headers) {
					var l = headers().location;
					var id = l.substring(l.lastIndexOf("/") + 1);
					$location.url("/realms/" + id);
					Notifications.success("Created realm");
				});
			} else {
				Realm.update($scope.realm, function() {
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
				realm : realm.id,
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
