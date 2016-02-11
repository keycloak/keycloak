(function (angular) {

    const SECTION_NAME = "history";

    angular
        .module("BrowserSync")
        .controller("HistoryController", [
            "$scope",
            "options",
            "History",
            "pagesConfig",
            historyController
        ]);

    /**
     * @param $scope
     * @param options
     * @param History
     * @param pagesConfig
     */
    function historyController($scope, options, History, pagesConfig) {

        var ctrl       = this;
        ctrl.options = options.bs;
        ctrl.section = pagesConfig[SECTION_NAME];
        ctrl.visited = [];

        ctrl.update  = function (items) {
            ctrl.visited = items;
            $scope.$digest();
        };

        History.get().then(function (items) {
            ctrl.visited = items;
        });

        History.on("change", ctrl.update);

        $scope.$on("$destroy", function () {
            History.off(ctrl.update);
        });

        ctrl.clearVisited = function () {
            History.clear();
        };
    }

    angular
        .module("BrowserSync")
        .directive("historyList", function () {
            return {
                restrict: "E",
                scope: {
                    options: "=",
                    visited: "="
                },
                templateUrl: "history.directive.html",
                controller: ["$scope", "History", "Clients", historyDirective],
                controllerAs: "ctrl"
            };
        });

    /**
     * Controller for the URL sync
     * @param $scope - directive scope
     * @param History
     * @param Clients
     */
    function historyDirective($scope, History, Clients) {

        var ctrl = this;

        ctrl.visited = [];

        ctrl.utils = {};

        ctrl.utils.localUrl = function (path) {
            return [$scope.options.urls.local, path].join("");
        };

        ctrl.updateVisited = function (data) {
            ctrl.visited = data;
            $scope.$digest();
        };

        ctrl.sendAllTo = function (url) {
            url.success = true;
            Clients.sendAllTo(url.path);
            setTimeout(function () {
                url.success = false;
                $scope.$digest();
            }, 1000);
        };

        ctrl.removeVisited = function (item) {
            History.remove(item);
        };

        History.get().then(function (items) {
            ctrl.visited = items;
        });

        History.on("change", ctrl.updateVisited);

        $scope.$on("$destroy", function () {
            History.off(ctrl.updateVisited);
        });
    }

})(angular);

