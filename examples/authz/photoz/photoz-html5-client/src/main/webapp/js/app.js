var module = angular.module('photoz', ['ngRoute', 'ngResource']);

var resourceServerId = 'photoz-restful-api';
var apiUrl = window.location.origin + '/' + resourceServerId;

angular.element(document).ready(function ($http) {
    var keycloak = new Keycloak('keycloak.json');
    keycloak.init({onLoad: 'login-required'}).success(function () {
        console.log('User is now authenticated.');

        module.factory('Identity', function () {
            return new Identity(keycloak);
        });

        angular.bootstrap(document, ["photoz"]);
    }).error(function () {
        window.location.reload();
    });
});

module.config(function ($httpProvider, $routeProvider) {
    $httpProvider.interceptors.push('authInterceptor');
    $routeProvider.when('/', {
        templateUrl: 'partials/home.html',
        controller: 'GlobalCtrl'
    }).when('/album/create', {
        templateUrl: 'partials/album/create.html',
        controller: 'AlbumCtrl',
    }).when('/album/:id', {
        templateUrl: 'partials/album/detail.html',
        controller: 'AlbumCtrl',
    }).when('/admin/album', {
        templateUrl: 'partials/admin/albums.html',
        controller: 'AdminAlbumCtrl',
    }).when('/profile', {
        templateUrl: 'partials/profile.html',
        controller: 'ProfileCtrl',
    });
});

module.controller('GlobalCtrl', function ($scope, $http, $route, $location, Album, Identity) {
    Album.query(function (albums) {
        $scope.albums = albums;
    });
    Album.shares(function (albums) {
        $scope.shares = albums;
    });

    $scope.Identity = Identity;

    $scope.deleteAlbum = function (album) {
        new Album(album).$delete({id: album.id}, function () {
            $route.reload();
        });
    }

    $scope.requestDeleteAccess = function (album) {
        new Album(album).$delete({id: album.id}, function () {
            // no-op
        }, function () {
            document.getElementById("output").innerHTML = 'Sent authorization request to resource owner, please, wait for approval.';
        });
    }

    $scope.hasAccess = function (share, scope) {
        for (i = 0; i < share.scopes.length; i++) {
            if (share.scopes[i] == scope) {
                return true;
            }
        }
        return false;
    }
});

module.controller('TokenCtrl', function ($scope, Identity) {
    $scope.showRpt = function () {
        document.getElementById("output").innerHTML = JSON.stringify(jwt_decode(Identity.authorization.rpt), null, '  ');
    }

    $scope.showAccessToken = function () {
        document.getElementById("output").innerHTML = JSON.stringify(jwt_decode(Identity.authc.token), null, '  ');
    }

    $scope.requestEntitlements = function () {
        Identity.authorization.entitlement('photoz-restful-api').then(function (rpt) {
            $scope.showRpt()
        });
    }

    $scope.Identity = Identity;
});

module.controller('AlbumCtrl', function ($scope, $http, $routeParams, $location, Album) {
    $scope.album = {};
    if ($routeParams.id) {
        $scope.album = Album.get({id: $routeParams.id});
    }
    $scope.create = function () {
        var newAlbum = new Album($scope.album);
        newAlbum.$save({}, function (data) {
            $location.path('/');
        });
    };
    $scope.goto = function (path) {
        $location.path(path)
    }
});

module.controller('ProfileCtrl', function ($scope, $http, $routeParams, $location, Profile) {
    $scope.profile = Profile.get();
});

module.controller('AdminAlbumCtrl', function ($scope, $http, $route, $location, AdminAlbum, Album) {
    $scope.albums = {};
    $http.get(apiUrl + '/admin/album').success(function (data) {
        $scope.albums = data;
    });
    $scope.deleteAlbum = function (album) {
        new Album(album).$delete({id: album.id}, function () {
            $route.reload();
        });
    }
});

module.factory('Album', ['$resource', function ($resource) {
    return $resource(apiUrl + '/album/:id', {id: '@id'}, {
            shares: {url: apiUrl + '/album/shares', method: 'GET', isArray: true}
        });
}]);

module.factory('Profile', ['$resource', function ($resource) {
    return $resource(apiUrl + '/profile');
}]);

module.factory('AdminAlbum', ['$resource', function ($resource) {
    return $resource(apiUrl + '/admin/album/:id');
}]);

module.factory('authInterceptor', function ($q, $injector, $timeout, Identity) {
    return {
        request: function (request) {
            document.getElementById("output").innerHTML = '';
            if (Identity.authorization && Identity.authorization.rpt && request.url.indexOf('/authorize') == -1) {
                retries = 0;
                request.headers.Authorization = 'Bearer ' + Identity.authorization.rpt;
            } else {
                request.headers.Authorization = 'Bearer ' + Identity.authc.token;
            }
            return request;
        },
        responseError: function (rejection) {
            var status = rejection.status;

            if (status == 403 || status == 401) {
                var retry = (!rejection.config.retry ||  rejection.config.retry < 1);

                if (!retry) {
                    document.getElementById("output").innerHTML = 'You can not access or perform the requested operation on this resource.';
                    return $q.reject(rejection);
                }

                if (rejection.config.url.indexOf('/authorize') == -1 && retry) {
                    // here is the authorization logic, which tries to obtain an authorization token from the server in case the resource server
                    // returns a 403 or 401.
                    var wwwAuthenticateHeader = rejection.headers('WWW-Authenticate');

                    // when using UMA, a WWW-Authenticate header should be returned by the resource server
                    if (!wwwAuthenticateHeader) {
                        return $q.reject(rejection);
                    }

                    // when using UMA, a WWW-Authenticate header should contain UMA data
                    if (wwwAuthenticateHeader.indexOf('UMA') == -1) {
                        return $q.reject(rejection);
                    }

                    var deferred = $q.defer();

                    var params = wwwAuthenticateHeader.split(',');
                    var ticket;

                    // try to extract the permission ticket from the WWW-Authenticate header
                    for (i = 0; i < params.length; i++) {
                        var param = params[i].split('=');

                        if (param[0] == 'ticket') {
                            ticket = param[1].substring(1, param[1].length - 1).trim();
                            break;
                        }
                    }

                    // a permission ticket must exist in order to send an authorization request
                    if (!ticket) {
                        return $q.reject(rejection);
                    }

                    // prepare a authorization request with the permission ticket
                    var authorizationRequest = {};
                    authorizationRequest.ticket = ticket;

                    // send the authorization request, if successful retry the request
                    Identity.authorization.authorize(authorizationRequest).then(function (rpt) {
                        deferred.resolve(rejection);
                    }, function () {
                        document.getElementById("output").innerHTML = 'You can not access or perform the requested operation on this resource.';
                    }, function () {
                        document.getElementById("output").innerHTML = 'Unexpected error from server.';
                    });

                    var promise = deferred.promise;

                    return promise.then(function (res) {
                        if (!res.config.retry) {
                            res.config.retry = 1;
                        } else {
                            res.config.retry++;
                        }

                        var $http = $injector.get("$http");

                        return $http(res.config).then(function (response) {
                            return response;
                        });
                    });
                }
            }

            return $q.reject(rejection);
        }
    };
});