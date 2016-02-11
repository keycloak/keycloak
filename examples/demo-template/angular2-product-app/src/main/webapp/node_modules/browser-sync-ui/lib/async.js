var Immutable = require("immutable");
var url = require("url");

module.exports = {
    /**
     * The UI uses it's own server/port
     * @param ui
     * @param done
     */
    findAFreePort: function (ui, done) {
        var port = ui.options.get("port");
        ui.bs.utils.portscanner.findAPortNotInUse(port, port + 100, {
            host: "localhost",
            timeout: 1000
        }, function (err, port) {
            if (err) {
                return done(err);
            }
            done(null, {
                options: {
                    port: port
                }
            });
        });
    },
    /**
     * Default hooks do things like creating/joining JS files &
     * building angular config
     * @param ui
     * @param done
     */
    initDefaultHooks: function (ui, done) {

        var out = ui.pluginManager.hook("page", ui);

        done(null, {
            instance: {
                clientJs:    ui.pluginManager.hook("client:js", ui),
                templates:   ui.pluginManager.hook("templates", ui.getInitialTemplates(), ui),
                pagesConfig: out.pagesConfig,
                pages:       out.pagesObj,
                pageMarkup:  out.pageMarkup
            }
        });
    },
    setBsOptions: function (ui, done) {
        done(null, {
            options: {
                bs: Immutable.Map({
                    mode: ui.bs.options.get("mode"),
                    port: ui.bs.options.get("port")
                })
            }
        });
    },
    /**
     * @param ui
     * @param done
     */
    setUrlOptions: function (ui, done) {

        var port        = ui.options.get("port");
        var bsUrls      = ui.bs.getOptionIn(["urls"]).toJS();
        var urls        = {
            ui: "http://localhost:" + port
        };

        if (bsUrls.external) {
            urls["ui-external"] = ["http://", url.parse(bsUrls.external).hostname, ":", port].join("");
        }

        done(null, {
            options: {
                urls: Immutable.fromJS(urls)
            }
        });
    },
    /**
     * Simple static file server with some middlewares for custom
     * scripts/routes.
     * @param ui
     * @param done
     */
    startServer: function (ui, done) {

        var bs          = ui.bs;
        var port        = ui.options.get("port");

        ui.logger.debug("Using port %s", port);

        var server = require("./server")(ui, {
            middleware: {
                socket: bs.getMiddleware("socket-js"),
                connector: bs.getSocketConnector(bs.options.get("port"), {
                    path: bs.options.getIn(["socket", "path"]),
                    namespace: ui.config.getIn(["socket", "namespace"])
                })
            }
        });

        bs.registerCleanupTask(function () {
            if (server.server) {
                server.server.close();
            }
            if (ui.servers) {
                Object.keys(ui.servers).forEach(function (key) {
                    if (ui.servers[key].server) {
                        ui.servers[key].server.close();
                    }
                });
            }
        });

        done(null, {
            instance: {
                server: server.server.listen(port),
                app: server.app
            }
        });

    },
    /**
     * Allow an API for adding/removing elements to clients
     * @param ui
     * @param done
     */
    addElementEvents: function (ui, done) {

        var elems = ui.pluginManager.hook("elements");
        var bs    = ui.bs;

        if (!Object.keys(elems).length) {
            return done();
        }

        ui.setOption("clientFiles", Immutable.fromJS(elems));

        done(null, {
            instance: {
                enableElement:  require("./client-elements").enable(ui.clients, ui, bs),
                disableElement: require("./client-elements").disable(ui.clients, ui, bs),
                addElement:     require("./client-elements").addElement
            }
        });
    },
    /**
     * Run default plugins
     * @param ui
     * @param done
     */
    registerPlugins: function (ui, done) {
        Object.keys(ui.defaultPlugins).forEach(function (key) {
            ui.pluginManager.get(key)(ui, ui.bs);
        });
        done();
    },
    /**
     * The most important event is the initial connection where
     * the options are received from the socket
     * @param ui
     * @param done
     */
    addOptionsEvent: function (ui, done) {

        var bs = ui.bs;

        ui.clients.on("connection", function (client) {

            client.emit("ui:connection", ui.options.toJS());

            ui.options.get("clientFiles").map(function (item) {
                if (item.get("active")) {
                    ui.addElement(client, item.toJS());
                }
            });
        });

        ui.socket.on("connection", function (client) {

            client.emit("connection", bs.getOptions().toJS());

            client.emit("ui:connection", ui.options.toJS());

            client.on("ui:get:options", function () {
                client.emit("ui:receive:options", {
                    bs: bs.getOptions().toJS(),
                    ui: ui.options.toJS()
                });
            });

            // proxy client events
            client.on("ui:client:proxy", function (evt) {
                ui.clients.emit(evt.event, evt.data);
            });

            client.on("ui", function (data) {
                ui.delegateEvent(data);
            });
        });
        done();
    }
};