(function (angular) {

    const SECTION_NAME = "remote-debug";

    angular
        .module("BrowserSync")
        .controller("RemoteDebugController", [
            "options",
            "Socket",
            "pagesConfig",
            RemoteDebugController
        ]);

    /**
     * @param options
     * @param Socket
     * @param pagesConfig
     */
    function RemoteDebugController(options, Socket, pagesConfig) {

        var ctrl         = this;
        ctrl.options     = options.bs;
        ctrl.uiOptions   = options.ui;
        ctrl.clientFiles = options.ui.clientFiles || {};
        ctrl.section     = pagesConfig[SECTION_NAME];
        ctrl.overlayGrid = options.ui[SECTION_NAME]["overlay-grid"];
        ctrl.items = [];

        if (Object.keys(ctrl.clientFiles).length) {
            Object.keys(ctrl.clientFiles).forEach(function (key) {
                if (ctrl.clientFiles[key].context === SECTION_NAME) {
                    ctrl.items.push(ctrl.clientFiles[key]);
                }
            });
        }

        ctrl.toggleClientFile = function (item) {
            if (item.name === "weinre") {
                return ctrl.toggleWeinre(item);
            }
            if (item.active) {
                return ctrl.enable(item);
            }
            return ctrl.disable(item);
        };

        ctrl.toggleWeinre = function (item) {
            Socket.uiEvent({
                namespace: SECTION_NAME + ":weinre",
                event: "toggle",
                data: item.active
            });
        };

        ctrl.toggleOverlayGrid = function (item) {
            var ns = SECTION_NAME + ":overlay-grid";
            Socket.uiEvent({
                namespace: ns,
                event: "toggle",
                data: item.active
            });
        };

        ctrl.enable = function (item) {
            Socket.uiEvent({
                namespace: SECTION_NAME + ":files",
                event: "enableFile",
                data: item
            });
        };

        ctrl.disable = function (item) {
            Socket.uiEvent({
                namespace: SECTION_NAME + ":files",
                event: "disableFile",
                data: item
            });
        };
    }

    /**
     * Display the snippet when in snippet mode
     */
    angular
        .module("BrowserSync")
        .directive("noCache", function () {
            return {
                restrict: "E",
                replace: true,
                scope: {
                    "options": "="
                },
                templateUrl: "no-cache.html",
                controller: ["$scope", "Socket", noCacheDirectiveControlller],
                controllerAs: "ctrl"
            };
        });

    /**
     * @param $scope
     * @param Socket
     */
    function noCacheDirectiveControlller ($scope, Socket) {

        var ctrl = this;

        ctrl.noCache = $scope.options[SECTION_NAME]["no-cache"];

        ctrl.toggleLatency = function (item) {
            Socket.emit("ui:no-cache", {
                event: "toggle",
                data: item.active
            });
        };
    }


    /**
     * Display the snippet when in snippet mode
     */
    angular
        .module("BrowserSync")
        .directive("compression", function () {
            return {
                restrict: "E",
                replace: true,
                scope: {
                    "options": "="
                },
                templateUrl: "compression.html",
                controller: ["$scope", "Socket", compressionDirectiveControlller],
                controllerAs: "ctrl"
            };
        });

    /**
     * @param $scope
     * @param Socket
     */
    function compressionDirectiveControlller ($scope, Socket) {

        var ctrl = this;

        ctrl.compression = $scope.options[SECTION_NAME]["compression"];

        ctrl.toggleLatency = function (item) {
            Socket.emit("ui:compression", {
                event: "toggle",
                data: item.active
            });
        };
    }

})(angular);

