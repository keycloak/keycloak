var url       = require("url");
var Immutable = require("immutable");

module.exports.init = function (ui, bs) {

    var validUrls = Immutable.OrderedSet();

    var methods = {
        /**
         * Send the url list to UI
         * @param urls
         */
        sendUpdatedUrls: function (urls) {
            ui.socket.emit("ui:history:update", decorateUrls(urls));
        },
        /**
         * Only send to UI if list changed
         * @param current
         * @param temp
         */
        sendUpdatedIfChanged: function (current, temp) {
            if (!Immutable.is(current, temp)) {
                validUrls = temp;
                methods.sendUpdatedUrls(validUrls);
            }
        },
        /**
         * Send all clients to a URL - this is a proxy
         * in case we need to limit/check anything.
         * @param data
         */
        sendToUrl: function (data) {

            var parsed = url.parse(data.path);

            data.override = true;
            data.path = parsed.path;
            data.url  = parsed.href;

            ui.clients.emit("browser:location", data);
        },
        /**
         * Add a new path
         * @param data
         */
        addPath: function (data) {
            var temp = addPath(validUrls, url.parse(data.href), bs.options.get("mode"));
            methods.sendUpdatedIfChanged(validUrls, temp, ui.socket);
        },
        /**
         * Remove a path
         * @param data
         */
        removePath: function (data) {
            var temp = removePath(validUrls, data.path);
            methods.sendUpdatedIfChanged(validUrls, temp, ui.socket);
        },
        /**
         * Get the current list
         */
        getVisited: function () {
            ui.socket.emit("ui:receive:visited", decorateUrls(validUrls));
        }
    };

    ui.clients.on("connection", function (client) {
        client.on("ui:history:connected", methods.addPath);
    });

    ui.socket.on("connection", function (uiClient) {
        /**
         * Send urls on first connection
         */
        uiClient.on("ui:get:visited",    methods.getVisited);
        methods.sendUpdatedUrls(validUrls);
    });

    ui.listen("history", {
        "sendAllTo": methods.sendToUrl,
        "remove":    methods.removePath,
        "clear":     function () {
            validUrls = Immutable.OrderedSet([]);
            methods.sendUpdatedUrls(validUrls);
        }
    });

    return methods;
};

/**
 * @param {Immutable.Set} urls
 * @returns {Array}
 */
function decorateUrls (urls) {
    var count = 0;
    return urls.map(function (value) {
        count += 1;
        return {
            path: value,
            key: count
        };
    }).toJS().reverse();
}

/**
 * If snippet mode, add the full URL
 * if server/proxy, add JUST the path
 * @param immSet
 * @param urlObj
 * @param mode
 * @returns {Set}
 */
function addPath(immSet, urlObj, mode) {
    return immSet.add(
        mode === "snippet"
            ? urlObj.href
            : urlObj.path
    );
}

module.exports.addPath = addPath;

/**
 * @param immSet
 * @param urlPath
 * @returns {*}
 */
function removePath(immSet, urlPath) {
    return immSet.remove(url.parse(urlPath).path);
}

module.exports.removePath = removePath;
