var path   = require("path");

module.exports = function (bs) {
    /**
     * Transform server options to offer additional functionality
     * @param bs
     */

    var options = bs.options;
    var server  = options.server;
    var cwd     = bs.cwd;

    /**
     * Transform server option
     */
    if (server) {
        if (Array.isArray(server.baseDir)) {
            server.baseDirs = options.server.baseDir.map(function (item) {
                return path.join(cwd, item);
            });
        } else {
            server.baseDirs = [path.join(cwd, server.baseDir)];
        }
    }
};