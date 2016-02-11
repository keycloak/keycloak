var http        = require("http");
var fs          = require("fs");
var path        = require("path");
var config      = require("./config");
var svg         = publicFile(config.defaults.public.svg);
var indexPage   = publicFile(config.defaults.indexPage);
//var css         = publicFile(config.defaults.public.css);
var header      = staticFile(config.defaults.components.header);
var footer      = staticFile(config.defaults.components.footer);
var zlib        = require("zlib");

/**
 * @param {UI} ui
 * @returns {*}
 */
function startServer(ui) {

    var connect     = ui.bs.utils.connect;
    var serveStatic = ui.bs.utils.serveStatic;

    /**
     * Create a connect server
     */
    var app         = connect();
    var socketJs    = getSocketJs(ui);
    var jsFilename  = "/" + md5(socketJs, 10) + ".js";
    //var cssFilename = "/" + md5(css, 10)   + ".css";

    /**
     * Create a single big file with all deps
     */
    //app.use(serveFile(jsFilename, "js", socketJs));
    app.use(serveFile(config.defaults.socketJs, "js", socketJs));

    // also serve for convenience/testing
    app.use(serveFile(config.defaults.pagesConfig, "js", ui.pagesConfig));

    //
    app.use(serveFile(config.defaults.clientJs, "js",    ui.clientJs));

    /**
     * Add any markup from plugins/hooks/templates
     */
    insertPageMarkupFromHooks(
        app,
        ui.pages,
        indexPage
            .replace("%pageMarkup%", ui.pageMarkup)
            .replace("%templates%", ui.templates)
            .replace("%svg%", svg)
            .replace("%header%", header)
            .replace(/%footer%/g, footer)
    );

    /**
     * gzip css
     */
    //app.use(serveFile(cssFilename, "css", css));

    app.use(serveStatic(path.join(__dirname, "../public")));

    /**
     * all public dir as static
     */
    app.use(serveStatic(publicDir("")));

    /**
     * History API fallback
     */
    app.use(require("connect-history-api-fallback"));

    /**
     * Development use
     */
    app.use("/node_modules", serveStatic(packageDir("node_modules")));

    /**
     * Return the server.
     */
    return {
        server: http.createServer(app),
        app: app
    };
}

/**
 * @param app
 * @param pages
 * @param markup
 */
function insertPageMarkupFromHooks(app, pages, markup) {

    var cached;

    app.use(function (req, res, next) {

        if (req.url === "/" || pages[req.url.slice(1)]) {
            res.writeHead(200, {"Content-Type": "text/html", "Content-Encoding": "gzip"});
            if (!cached) {
                var buf = new Buffer(markup, "utf-8");
                zlib.gzip(buf, function (_, result) {
                    cached = result;
                    res.end(result);
                });
            } else {
                res.end(cached);
            }
        } else {
            next();
        }
    });
}

/**
 * Serve Gzipped files & cache them
 * @param app
 * @param all
 */
var gzipCache = {};
function serveFile(path, type, string) {
    var typemap = {
        js:  "application/javascript",
        css: "text/css"
    };
    return function (req, res, next) {
        if (req.url !== path) {
            return next();
        }

        res.writeHead(200, {
            "Content-Type": typemap[type],
            "Content-Encoding": "gzip",
            "Cache-Control": "public, max-age=2592000000",
            "Expires": new Date(Date.now() + 2592000000).toUTCString()
        });

        if (gzipCache[path]) {
            return res.end(gzipCache[path]);
        }
        var buf = new Buffer(string, "utf-8");
        zlib.gzip(buf, function (_, result) {
            gzipCache[path] = result;
            res.end(result);
        });
    };
}


/**
 * @param cp
 * @returns {string}
 */
function getSocketJs (cp) {

    return [
        cp.bs.getSocketIoScript(),
        cp.bs.getExternalSocketConnector({namespace: "/browser-sync-cp"})
    ].join(";");
}

///**
// * @returns {*}
// * @param filepath
// */
//function fileContent (filepath) {
//    return fs.readFileSync(require.resolve(filepath), "utf8");
//}

/**
 * @param src
 * @param length
 */
function md5(src, length) {
    var crypto = require("crypto");
    var hash   = crypto.createHash("md5").update(src, "utf8").digest("hex");
    return hash.slice(0, length);
}

/**
 * CWD directory helper for static dir
 * @param {string} filepath
 * @returns {string}
 */
function publicDir (filepath) {
    return path.join(__dirname, "/../public" + filepath) || "";
}

/**
 * @param {string} filepath
 * @returns {string|string}
 */
function staticDir (filepath) {
    return path.join(__dirname, "/../static" + filepath) || "";
}

/**
 * @param {string} filepath
 * @returns {*}
 */
function publicFile(filepath) {
    return fs.readFileSync(publicDir(filepath), "utf-8");
}

/**
 * @param filepath
 * @returns {*}
 */
function staticFile(filepath) {
    return fs.readFileSync(staticDir(filepath), "utf-8");
}

/**
 * @param {string} filepath
 * @returns {string}
 */
function packageDir (filepath) {
    return path.join(__dirname, "/../" + filepath);
}

module.exports = startServer;