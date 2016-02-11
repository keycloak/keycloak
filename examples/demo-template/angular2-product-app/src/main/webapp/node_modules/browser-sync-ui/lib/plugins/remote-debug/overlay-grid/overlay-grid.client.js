(function (angular) {

    const SECTION_NAME = "remote-debug";

    /**
     * Display the snippet when in snippet mode
     */
    angular
        .module("BrowserSync")
        .directive("cssGrid", function () {
            return {
                restrict:     "E",
                replace:      true,
                scope:        {
                    "options": "="
                },
                templateUrl:  "overlay-grid.html",
                controller:   ["$scope", "Socket", overlayGridDirectiveControlller],
                controllerAs: "ctrl"
            };
        });

    /**
     * @param $scope
     * @param Socket
     */
    function overlayGridDirectiveControlller($scope, Socket) {

        var ctrl = this;

        ctrl.overlayGrid = $scope.options[SECTION_NAME]["overlay-grid"];
        ctrl.size = ctrl.overlayGrid.size;

        var ns = SECTION_NAME + ":overlay-grid";

        ctrl.alter = function (value) {
            Socket.emit("ui", {
                namespace: ns,
                event:     "adjust",
                data:      value
            });
        };

        ctrl.toggleAxis = function (axis, value) {
            Socket.emit("ui", {
                namespace: ns,
                event:     "toggle:axis",
                data:      {
                    axis:  axis,
                    value: value
                }
            });
        };
    }

})(angular);
