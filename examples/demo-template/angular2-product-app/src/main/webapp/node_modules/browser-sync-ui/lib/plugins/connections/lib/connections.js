var Immutable = require("immutable");

/**
 * Track connected clients
 * @param {UI} ui
 * @param {BrowserSync} bs
 */
module.exports.init = function (ui, bs) {

    var uaParser = new bs.utils.UAParser();

    var currentConnections = [];

    ui.clients.on("connection", function (client) {
        client.on("client:heartbeat", function (data) {
            var match;
            if (currentConnections.some(function (item, index) {
                    if (item.id === client.id) {
                        match = index;
                        return true;
                    }
                    return false;
                })) {
                if (typeof match === "number") {
                    currentConnections[match].timestamp = new Date().getTime();
                    currentConnections[match].data = data;
                }
            } else {
                currentConnections.push({
                    id: client.id,
                    timestamp: new Date().getTime(),
                    browser: uaParser.setUA(client.handshake.headers["user-agent"]).getBrowser(),
                    data: data
                });
            }
        });
    });

    var registry;
    var temp;
    var initialSent;

    var int = setInterval(function () {

        if (ui.clients.sockets.length) {
            temp = Immutable.List(ui.clients.sockets.map(function (client) {
                return Immutable.fromJS({
                    id: client.id,
                    browser: uaParser.setUA(client.handshake.headers["user-agent"]).getBrowser()
                });
            }));
            if (!registry) {
                registry = temp;
                sendUpdated(ui.socket, decorateClients(registry.toJS(), currentConnections));
            } else {
                if (Immutable.is(registry, temp)) {
                    if (!initialSent) {
                        sendUpdated(ui.socket, decorateClients(registry.toJS(), currentConnections));
                        initialSent = true;
                    }
                } else {
                    registry = temp;
                    sendUpdated(ui.socket, decorateClients(registry.toJS(), currentConnections));
                }
            }
        } else {
            sendUpdated(ui.socket, []);
        }

    }, 1000);

    bs.registerCleanupTask(function () {
        clearInterval(int);
    });
};


/**
 * Use heart-beated data to decorate clients
 * @param clients
 * @param clientsInfo
 * @returns {*}
 */
function decorateClients(clients, clientsInfo) {
    return clients.map(function (item) {
        clientsInfo.forEach(function (client) {
            if (client.id === item.id) {
                item.data = client.data;
                return false;
            }
        });
        return item;
    });
}

/**
 * @param socket
 * @param connectedClients
 */
function sendUpdated(socket, connectedClients) {
    socket.emit("ui:connections:update", connectedClients);
}

/**
 * @param clients
 * @param data
 */
//function highlightClient (clients, data) {
//    var socket = getClientById(clients, data.id);
//    if (socket) {
//        socket.emit("highlight");
//    }
//}

/**
 * @param clients
 * @param id
 */
//function getClientById (clients, id) {
//    var match;
//    clients.sockets.some(function (item, i) {
//        if (item.id === id) {
//            match = clients.sockets[i];
//            return true;
//        }
//    });
//    return match;
//}