var async       = require("./async");

module.exports = [
    {
        step: "Setting default plugins",
        fn:   async.initDefaultHooks
    },
    {
        step: "Finding a free port",
        fn:   async.findAFreePort
    },
    {
        step: "Setting options also relevant to UI from BS",
        fn:   async.setBsOptions
    },
    {
        step: "Setting available URLS for UI",
        fn:   async.setUrlOptions
    },
    {
        step: "Starting the Control Panel Server",
        fn:   async.startServer
    },
    {
        step: "Add element events",
        fn:   async.addElementEvents
    },
    {
        step: "Registering default plugins",
        fn:   async.registerPlugins
    },
    {
        step: "Add options setting event",
        fn: async.addOptionsEvent
    }
];