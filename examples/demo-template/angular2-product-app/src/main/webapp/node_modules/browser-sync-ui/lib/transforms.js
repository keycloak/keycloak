module.exports = {
    "mode": function (obj) {
        if (obj.get("server")) {
            return "Server";
        }
        if (obj.get("proxy")) {
            return "Proxy";
        }
        return "Snippet";
    }
};