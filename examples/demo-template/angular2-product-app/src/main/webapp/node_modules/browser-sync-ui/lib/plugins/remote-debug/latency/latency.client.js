(function (angular) {

    const SECTION_NAME = "remote-debug";
    /**
     * Display the snippet when in snippet mode
     */
    angular
        .module("BrowserSync")
        .directive("latency", function () {
            return {
                restrict:     "E",
                replace:      true,
                scope:        {
                    "options": "="
                },
                templateUrl:  "latency.html",
                controller:   ["$scope", "Socket", latencyDirectiveControlller],
                controllerAs: "ctrl"
            };
        });

    /**
     * @param $scope
     * @param Socket
     */
    function latencyDirectiveControlller($scope, Socket) {

        var ctrl = this;
        var ns = SECTION_NAME + ":latency";

        ctrl.latency = $scope.options[SECTION_NAME]["latency"];

        ctrl.alterLatency = function () {
            Socket.emit("ui", {
                namespace: ns,
                event:     "adjust",
                data:      {
                    rate: ctrl.latency.rate
                }
            });
        };
    }
})(angular);
