var url            = require("url");
var Immutable      = require("immutable");
var path           = require("path");

var weinreApp;

const WEINRE_NAME      = "weinre-debug";
const WEINRE_ID        = "#browsersync";
const WEINRE_ELEM_ID   = "__browser-sync-weinre__";

var weinreTargetUrl    = {
    protocol: "http:",
    pathname: "/target/target-script-min.js",
    hash: WEINRE_ID
};

var weinreClientUrl    = {
    protocol: "http:",
    pathname: "/client/",
    hash: WEINRE_ID
};

/**
 * Prepare weinre for later possible use.
 * @param ui
 */
function init (ui) {

    var hostUrl    = getHostUrl(ui, ui.bs);
    var weinrePort = ui.getOptionIn(["weinre", "port"]);

    weinreTargetUrl.hostname = hostUrl.hostname;
    weinreClientUrl.hostname = hostUrl.hostname;
    weinreClientUrl.port     = weinrePort;
    weinreTargetUrl.port     = weinrePort;

    ui.setOption(WEINRE_NAME, Immutable.fromJS({
        name:  WEINRE_NAME,
        active: false,
        url:    false,
        targetUrl: url.format(weinreTargetUrl),
        clientUrl: url.format(weinreClientUrl),
        port: weinrePort
    }));

    setWeinreClientUrl(ui, url.format(weinreClientUrl));

    var methods = {
        toggle: function (data) {
            toggleWeinre(ui.socket, ui.clients, ui, ui.bs, data);
        },
        event: function (event) {
            methods[event.event](event.data);
        }
    };

    return methods;
}

/**
 * Get a suitable host URL for weinre
 * @param ui
 * @param bs
 * @returns {*}
 */
function getHostUrl(ui, bs) {

    var url = bs.getOptionIn(["urls", "external"]);

    if (!url) {
        url = bs.getOptionIn(["urls", "local"]);
    }

    return require("url").parse(url);
}


/**
 * @param ui
 * @param weinreClientUrl
 */
function setWeinreClientUrl(ui, weinreClientUrl) {
    var weinre = ui.options.getIn(["clientFiles", "weinre"]).toJS();
    ui.setMany(function (item) {
        item.setIn(["clientFiles", "weinre", "hidden"], weinre.hidden.replace("%s", weinreClientUrl));
        return item;
    });
}

/**
 * @param socket
 * @param clients
 * @param ui
 * @param bs
 * @param value
 */
function toggleWeinre (socket, clients, ui, bs, value) {

    if (value !== true) {
        value = false;
    }

    if (value) {

        var _debugger = enableWeinre(ui, bs);

        // set the state of weinre
        ui.setMany(function (item) {
            item.setIn([WEINRE_NAME, "active"], true);
            item.setIn([WEINRE_NAME, "url"], _debugger.url);
            item.setIn([WEINRE_NAME, "active"], true);
            item.setIn(["clientFiles", "weinre", "active"], true);
        }, {silent: true});


        // Let the UI know about it
        socket.emit("ui:weinre:enabled", _debugger);

        var fileitem = {
            type: "js",
            src: ui.getOptionIn([WEINRE_NAME, "targetUrl"]),
            id: WEINRE_ELEM_ID
        };

        // Add the element to all clients
        ui.addElement(clients, fileitem);

        // Save for page refreshes
        //clientScripts = clientScripts.set("weinre", fileitem);

    } else {

        // Stop it
        disableWeinre(ui, bs);

        //clientScripts = clientScripts.remove("weinre");

        // Reset the state
        ui.setOptionIn([WEINRE_NAME, "active"], false, {silent: false}); // Force a reload here
        ui.setOptionIn(["clientFiles", "weinre", "active"], false); // Force a reload here

        // Let the UI know
        socket.emit("ui:weinre:disabled");

        // Reload all browsers to remove weinre elements/JS
        clients.emit("browser:reload");

    }
}

/**
 * Enable the debugger
 * @param ui
 * @param bs
 * @returns {{url: string, port: number}}
 */
function enableWeinre (ui, bs) {

    if (weinreApp) {
        weinreApp.close();
        weinreApp = false;
    }

    var port     = ui.getOptionIn([WEINRE_NAME, "port"]);

    var logger   = require(path.join(path.dirname(require.resolve("weinre")), "utils.js"));

    logger.log = function (message) {
        ui.logger.debug("[weinre]: %s", message);
    };

    var weinre   = require("weinre");
    var external = getHostUrl(ui, bs);

    weinreApp = weinre.run({
        httpPort: port,
        boundHost: external.hostname,
        verbose: false,
        debug: false,
        readTimeout: 5,
        deathTimeout: 15 });

    return ui.options.get(WEINRE_NAME).toJS();
}

/**
 * @param ui
 * @returns {any|*}
 */
function disableWeinre (ui) {
    if (weinreApp) {
        weinreApp.close();
        weinreApp = false;
    }
    return ui.options.get(WEINRE_NAME).toJS();
}

module.exports.init         = init;
module.exports.toggleWeinre = toggleWeinre;