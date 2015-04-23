var module = angular.module('keycloak-tools', [ 'ngRoute', 'ngResource' ]);

module.config([ '$routeProvider', function ($routeProvider) {

    $routeProvider
        .when('/perf', {
            templateUrl: 'pages/perf.html',
            controller: 'PerfCtrl'
        })
        .when('/mail', {
            templateUrl: 'pages/mail.html',
            controller: 'MailCtrl'
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

module.controller('MailCtrl', function ($scope, $resource) {

    $scope.start = function() {
        $resource('/keycloak-tools/mail/start').get({}, function(status) {
            $scope.status = status;
        });
    }

    $scope.stop = function() {
        $resource('/keycloak-tools/mail/stop').get({}, function(status) {
            $scope.status = status;
        });
    }
    $scope.loadMessages = function() {
        $scope.messages = $resource('/keycloak-tools/mail/messages').query();
    }

    $scope.status = $resource('/keycloak-tools/mail/status').get();

});
