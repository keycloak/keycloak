var url      = require("url");
var http     = require("http");

/**
 * @param localUrl
 * @param urlPath
 * @returns {*}
 */
function createUrl(localUrl, urlPath) {
    return url.parse(url.resolve(localUrl, urlPath));
}

/**
 * @param url
 * @param cb
 */
function verifyUrl(url, cb) {

    url.headers = {
        "accept": "text/html"
    };

    http.get(url, function (res) {
        if (res.statusCode === 200) {
            cb(null, res);
        } else {
            cb("not 200");
        }
    }).on("error", function(e) {
        console.log("Got error: " + e.message);
    });
}

module.exports.createUrl = createUrl;
module.exports.verifyUrl = verifyUrl;
