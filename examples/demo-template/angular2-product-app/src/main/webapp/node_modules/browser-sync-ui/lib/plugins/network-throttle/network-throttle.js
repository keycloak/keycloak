var Immutable = require("immutable");

module.exports.init = function (ui) {

    var optPath = ["network-throttle"];
    var serverOptPath = optPath.concat(["servers"]);
    ui.servers  = {};

    ui.setOptionIn(optPath, Immutable.fromJS({
        name: "network-throttle",
        title: "Network Throttle",
        active: false,
        targets: require("./targets")
    }));

    ui.setOptionIn(serverOptPath, Immutable.Map({}));

    /**
     * @param input
     * @returns {number}
     */
    function getPortArg(input) {
        input = input.trim();
        if (input.length && input.match(/\d{3,5}/)) {
            input = parseInt(input, 10);
        } else {
            input = ui.bs.options.get("port") + 1;
        }
        return input;
    }

    /**
     * @returns {string}
     */
    function getTargetUrl() {
        return require("url").parse(ui.bs.options.getIn(["urls", "local"]));
    }

    var methods = {
        /**
         * @param data
         */
        "server:create": function (data) {

            data.port = getPortArg(data.port);
            data.cb   = data.cb || function () { /* noop */};

            /**
             * @param opts
             */
            function saveThrottleInfo (opts) {

                var urls = getUrls(ui.bs.options.set("port", opts.port).toJS());

                ui.setOptionIn(serverOptPath.concat([opts.port]), Immutable.fromJS({
                    urls: urls,
                    speed: opts.speed
                }));

                setTimeout(function () {

                    ui.socket.emit("ui:network-throttle:update", {
                        servers: ui.getOptionIn(serverOptPath).toJS(),
                        event: "server:create"
                    });

                    ui.servers[opts.port] = opts.server;

                    data.cb(null, opts);

                }, 300);

            }

            /**
             * @param err
             * @param port
             */
            function createThrottle (err, port) {

                var target = getTargetUrl();

                var args = {
                    port: port,
                    target: target,
                    speed: data.speed
                };

                if (ui.bs.getOption("scheme") === "https") {
                    var certs = require("browser-sync/lib/server/utils").getKeyAndCert(ui.bs.options);
                    args.key  = certs.key;
                    args.cert = certs.cert;
                }

                args.server = require("./throttle-server")(args);
                args.server.listen(port);

                saveThrottleInfo(args);
            }

            /**
             * Try for a free port
             */
            ui.bs.utils.portscanner.findAPortNotInUse(data.port, data.port + 100, "127.0.0.1", function (err, port) {
                if (err) {
                    return createThrottle(err);
                } else {
                    createThrottle(null, port);
                }
            });
        },
        /**
         * @param data
         */
        "server:destroy": function (data) {
            if (ui.servers[data.port]) {
                ui.servers[data.port].close();
                ui.setMany(function (item) {
                    item.deleteIn(serverOptPath.concat([parseInt(data.port, 10)]));
                });
                delete ui.servers[data.port];
            }
            ui.socket.emit("ui:network-throttle:update", {
                servers: ui.getOptionIn(serverOptPath).toJS(),
                event: "server:destroy"
            });
        },
        /**
         * @param event
         */
        event: function (event) {
            methods[event.event](event.data);
        }
    };

    return methods;
};

/**
 * Get local + external urls with a different port
 * @param opts
 * @returns {List<T>|List<any>}
 */
function getUrls (opts) {

    var list    = [];

    var bsLocal = require("url").parse(opts.urls.local);

    list.push([bsLocal.protocol + "//", bsLocal.hostname, ":", opts.port].join(""));

    if (opts.urls.external) {
        var external = require("url").parse(opts.urls.external);
        list.push([bsLocal.protocol + "//", external.hostname, ":", opts.port].join(""));
    }

    return Immutable.List(list);
}