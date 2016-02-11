(function (angular) {

    const SECTION_NAME = "overview";

    angular
        .module("BrowserSync")
        .controller("OverviewController", [
            "options",
            "pagesConfig",
            OverviewController
        ]);

    /**
     * @param options
     * @param pagesConfig
     */
    function OverviewController (options, pagesConfig) {
        var ctrl     = this;
        ctrl.section = pagesConfig[SECTION_NAME];
        ctrl.options = options.bs;
        ctrl.ui = {
            snippet: !ctrl.options.server && !ctrl.options.proxy
        };
    }

    /**
     * Url Info - this handles rendering of each server
     * info item
     */
    angular
        .module("BrowserSync")
        .directive("urlInfo", function () {
            return {
                restrict: "E",
                replace: true,
                scope: {
                    "options": "="
                },
                templateUrl: "url-info.html",
                controller: [
                    "$scope",
                    "$rootScope",
                    "Clients",
                    urlInfoController
                ]
            };
        });

    /**
     * @param $scope
     * @param $rootScope
     * @param Clients
     */
    function urlInfoController($scope, $rootScope, Clients) {

        var options = $scope.options;
        var urls    = options.urls;

        $scope.ui = {
            server: false,
            proxy: false
        };

        if ($scope.options.mode === "server") {
            $scope.ui.server = true;
            if (!Array.isArray($scope.options.server.baseDir)) {
                $scope.options.server.baseDir = [$scope.options.server.baseDir];
            }
        }

        if ($scope.options.mode === "proxy") {
            $scope.ui.proxy = true;
        }

        $scope.urls = [];

        $scope.urls.push({
            title: "Local",
            tagline: "URL for the machine you are running BrowserSync on",
            url: urls.local,
            icon: "imac"
        });

        if (urls.external) {
            $scope.urls.push({
                title: "External",
                tagline: "Other devices on the same wifi network",
                url: urls.external,
                icon: "wifi"
            });
        }

        if (urls.tunnel) {
            $scope.urls.push({
                title: "Tunnel",
                tagline: "Secure HTTPS public url",
                url: urls.tunnel,
                icon: "globe"
            });
        }

        /**
         *
         */
        $scope.sendAllTo = function (path) {
            Clients.sendAllTo(path);
            $rootScope.$emit("notify:flash", {
                heading: "Instruction sent:",
                message: "Sync all Browsers to: " + path
            });
        };
    }

    /**
     * Display the snippet when in snippet mode
     */
    angular
        .module("BrowserSync")
        .directive("snippetInfo", function () {
            return {
                restrict: "E",
                replace: true,
                scope: {
                    "options": "="
                },
                templateUrl: "snippet-info.html",
                controller: ["$scope", function snippetInfoController() {/*noop*/}]
            };
        });

})(angular);