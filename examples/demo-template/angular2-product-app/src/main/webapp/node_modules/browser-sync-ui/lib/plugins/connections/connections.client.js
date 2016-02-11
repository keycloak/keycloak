(function (angular) {

    const SECTION_NAME = "connections";

    angular
        .module("BrowserSync")
        .controller("ConnectionsController", [
            "pagesConfig",
            ConnectionsControllers
        ]);

    /**
     * @param pagesConfig
     * @constructor
     */
    function ConnectionsControllers(pagesConfig) {
        var ctrl = this;
        ctrl.section = pagesConfig[SECTION_NAME];
    }

    angular
        .module("BrowserSync")
        .directive("connectionList", function () {
            return {
                restrict:    "E",
                scope:       {
                    options:     "="
                },
                templateUrl: "connections.directive.html",
                controller:  ["$scope", "Clients", "Socket", connectionListDirective],
                controllerAs: "ctrl"
            };
        });

    /**
     * Controller for the URL sync
     * @param $scope - directive scope
     * @param Clients
     * @param Socket
     */
    function connectionListDirective($scope, Clients, Socket) {

        var ctrl = this;
        ctrl.connections = [];

        ctrl.update = function (data) {
            ctrl.connections = data;
            $scope.$digest();
        };

        // Always try to retreive the sockets first time.
        Socket.getData("clients").then(function (data) {
            ctrl.connections = data;
        });

        // Listen to events to update the list on the fly
        Socket.on("ui:connections:update", ctrl.update);

        $scope.$on("$destroy", function () {
            Socket.off("ui:connections:update", ctrl.update);
        });

        ctrl.highlight = function (connection) {
            Clients.highlight(connection);
        };
    }

})(angular);

