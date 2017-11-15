module.controller('RoleMembersCtrl', function($scope, realm, role, RoleMembership, Dialog, Notifications, $location, RealmRoleRemover) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.role = role;

    $scope.query = {
        realm: realm.realm,
        role: role.name,
        max : 5,
        first : 0
    }

    $scope.remove = function() {
        RealmRoleRemover.remove($scope.role, realm, Dialog, $location, Notifications);
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

    $scope.searchQuery = function() {
        console.log("query.search: " + $scope.query.search);
        $scope.searchLoaded = false;

        $scope.users = RoleMembership.query($scope.query, function() {
            console.log('search loaded');
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.searchQuery();

});
