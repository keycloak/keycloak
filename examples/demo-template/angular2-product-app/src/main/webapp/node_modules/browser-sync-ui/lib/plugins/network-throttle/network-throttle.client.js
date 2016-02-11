(function (angular) {

    const SECTION_NAME = "network-throttle";

    angular
        .module("BrowserSync")
        .controller("NetworkThrottleController", [
            "options",
            "pagesConfig",
            "Socket",
            "$scope",
            NetworkThrottleController
        ]);

    /**
     * @param options
     * @param pagesConfig
     * @param Socket
     * @param $scope
     */
    function NetworkThrottleController (options, pagesConfig, Socket, $scope) {

        var ctrl         = this;

        ctrl.section     = pagesConfig[SECTION_NAME];
        ctrl.options     = options.bs;
        ctrl.uiOptions   = options.ui;
        ctrl.clientFiles = options.ui.clientFiles || {};
        ctrl.section     = pagesConfig[SECTION_NAME];

        ctrl.throttle    = ctrl.uiOptions[SECTION_NAME];
        ctrl.selected    = ctrl.throttle.targets[0].id;
        ctrl.servers     = ctrl.throttle.servers;
        ctrl.port        = "";
        ctrl.portEntry   = "auto";
        ctrl.serverCount = Object.keys(ctrl.servers).length;
        ctrl.blurs       = [];

        ctrl.state = {
            success: false,
            waiting: false,
            classname: "ready"
        };

        ctrl.createServer = function (selected, event) {

            if (ctrl.blurs.indexOf(event.target) === -1) {
                ctrl.blurs.push(event.target);
            }

            var item = getByProp(ctrl.throttle.targets, "id", ctrl.selected);


            if (ctrl.portEntry === "auto") {
                return send("");
            }

            if (!ctrl.port || !ctrl.port.length) {
                setError();
                return;
            }

            if (!ctrl.port.match(/\d{4,5}/)) {
                setError();
                return;
            }

            var port = parseInt(ctrl.port, 10);

            if (port < 1024 || port > 65535) {
                setError();
                return;
            }

            send(ctrl.port);

            function setError() {
                ctrl.state.waiting   = false;
                ctrl.state.portError = true;
            }

            function send (port) {

                ctrl.state.classname = "waiting";
                ctrl.state.waiting   = true;

                Socket.uiEvent({
                    namespace: SECTION_NAME,
                    event: "server:create",
                    data: {
                        speed: item,
                        port: port
                    }
                });
            }
        };

        ctrl.destroyServer = function (item, port) {
            Socket.uiEvent({
                namespace: SECTION_NAME,
                event: "server:destroy",
                data: {
                    speed: item,
                    port: port
                }
            });
        };

        ctrl.toggleSpeed = function (item) {
            if (!item.active) {
                item.urls = [];
            }
        };

        ctrl.update = function (data) {

            ctrl.servers     = data.servers;
            ctrl.serverCount = Object.keys(ctrl.servers).length;

            if (data.event === "server:create") {
                updateButtonState();
            }

            $scope.$digest();
        };

        function updateButtonState() {

            ctrl.state.success = true;
            ctrl.state.classname = "success";

            setTimeout(function () {

                ctrl.blurs.forEach(function (elem) {
                    elem.blur();
                });

                setTimeout(function () {
                    ctrl.state.success   = false;
                    ctrl.state.waiting   = false;
                    ctrl.state.classname = "ready";

                    $scope.$digest();

                }, 500);

            }, 300);
        }

        /**
         * @param collection
         * @param prop
         * @returns {*}
         */
        function getByProp (collection, prop, name) {
            var match = collection.filter(function (item) {
                return item[prop] === name;
            });
            if (match.length) {
                return match[0];
            }
            return false;
        }

        Socket.on("ui:network-throttle:update", ctrl.update);
        $scope.$on("$destroy", function () {
            Socket.off("ui:network-throttle:update", ctrl.update);
        });
    }

    /**
     * Display the snippet when in snippet mode
     */
    angular
        .module("BrowserSync")
        .directive("throttle", function () {
            return {
                restrict: "E",
                replace: true,
                scope: {
                    "target": "=",
                    "options": "="
                },
                templateUrl: "network-throttle.directive.html",
                controller: ["$scope", "Socket", throttleDirectiveControlller],
                controllerAs: "ctrl"
            };
        });

    /**
     * @param $scope
     */
    function throttleDirectiveControlller ($scope) {

        var ctrl = this;

        ctrl.throttle = $scope.options[SECTION_NAME];

    }

})(angular);