module.controller('ProtocolListCtrl', function($scope, realm, serverInfo, $location) {
    $scope.realm = realm;
    $scope.protocols = serverInfo.protocols;
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ProtocolMapperListCtrl', function($scope, realm, serverInfo, protocol, mappers, $location) {
    $scope.realm = realm;
    $scope.protocol = protocol;
    var protocolMappers = serverInfo.protocolMapperTypes[protocol];
    var mapperTypes = {};
    if (protocolMappers) {
        for (var i = 0; i < protocolMappers.length; i++) {
            mapperTypes[protocolMappers[i].id] = protocolMappers[i].name;
        }
    }
    $scope.mapperTypes = mapperTypes;

    $scope.mappers = mappers;
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ProtocolMapperCtrl', function($scope, realm, serverInfo, protocol, mapper, RealmProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.create = false;
    $scope.protocol = protocol;
    $scope.mapper = angular.copy(mapper);
    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    console.log('protocol: ' + protocol);
    var protocolMappers = serverInfo.protocolMapperTypes[protocol];
    for (var i = 0; i < protocolMappers.length; i++) {
        if (protocolMappers[i].id == mapper.protocolMapper) {
            $scope.mapperType = protocolMappers[i];
        }
    }
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('mapper', function() {
        if (!angular.equals($scope.mapper, mapper)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        RealmProtocolMapper.update({
            realm : realm.realm,
            id : mapper.id
        }, $scope.mapper, function() {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + "/protocols/" + protocol + "/mappers/" + mapper.id);
            Notifications.success("Your changes have been saved.");
        });
    };

    $scope.reset = function() {
        $scope.mapper = angular.copy(mapper);
        $scope.changed = false;
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.mapper.name, 'mapper', function() {
            RealmProtocolMapper.remove({ realm: realm.realm, id : $scope.mapper.id }, function() {
                Notifications.success("The mapper has been deleted.");
                $location.url("/realms/" + realm.realm + "/protocols/" + protocol + "/mappers");
            });
        });
    };

});

module.controller('ProtocolMapperCreateCtrl', function($scope, realm, serverInfo, protocol, RealmProtocolMapper, Notifications, Dialog, $location) {
    $scope.realm = realm;
    $scope.create = true;
    $scope.protocol = protocol;
    $scope.mapper = { protocol : protocol, config: {}};
    $scope.mapperTypes = serverInfo.protocolMapperTypes[protocol];

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function() {
        $scope.mapper.protocolMapper = $scope.mapperType.id;
        RealmProtocolMapper.save({
            realm : realm.realm
        }, $scope.mapper, function(data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + "/protocols/" + protocol + "/mappers/" + id);
            Notifications.success("Mapper has been created.");
        });
    };

    $scope.cancel = function() {
        //$location.url("/realms");
        window.history.back();
    };


});



