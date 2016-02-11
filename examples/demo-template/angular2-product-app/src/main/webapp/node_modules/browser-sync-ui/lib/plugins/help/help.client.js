(function (angular) {

    const SECTION_NAME = "history";

    angular
        .module("BrowserSync")
        .controller("HelpAboutController", [
            "options",
            "pagesConfig",
            helpAboutController
        ]);

    /**
     * @param options
     * @param pagesConfig
     */
    function helpAboutController(options, pagesConfig) {
        var ctrl = this;
        ctrl.options = options.bs;
        ctrl.section = pagesConfig[SECTION_NAME];
    }

})(angular);

