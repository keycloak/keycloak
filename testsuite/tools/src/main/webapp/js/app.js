var module = angular.module('keycloak-tools', [ 'ngRoute', 'ngResource' ]);

module.config([ '$routeProvider', function ($routeProvider) {

    $routeProvider
        .when('/perf', {
            templateUrl: 'pages/perf.html',
            controller: 'PerfCtrl'
        })
        .otherwise({
            templateUrl: 'pages/home.html'
        });
}]);

module.filter('reverse', function() {
    return function(items) {
        return items.slice().reverse();
    };
});

module.controller('PerfCtrl', function ($scope, $resource) {

    $scope.createUsersData = {
        realm: 'test',
        count: 100,
        start: 0,
        batch: 25
    }

    $scope.loadJobs = function() {
        $scope.jobs = $resource('/keycloak-tools/perf/jobs').query();
    }

    $scope.clearJobs = function() {
        $scope.jobs = $resource('/keycloak-tools/perf/delete-jobs').query({}, function() {
            $scope.loadJobs();
        });
    }

    $scope.createUsers = function() {
        console.debug($scope.createUsersData);
        $resource('/keycloak-tools/perf/:realm/create-users').get($scope.createUsersData, function() {
            $scope.loadJobs();
        });
    }

    $scope.loadJobs();

});
