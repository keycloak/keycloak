var minimatch = require("minimatch");
var utils     = exports;

utils.overwriteBody = function overwriteBody(rules, body, res) {

    rules.forEach(function (rule, i) {

        /**
         * Try to use the replace string/fn first
         */
        if (rule.replace) {
            rule.fn = rule.replace;
        }

        /**
         * if rule.match is a string, convert
         * it to a global regex and do the replacement
         */
        if (typeof rule.match === "string") {
            return body = body.replace(rule.match, handleMatch);
        }

        /**
         * A regex was given? do the replacement
         */
        body = body.replace(rule.match, handleMatch);

        function handleMatch() {

            var inarray = res.rulesWritten.indexOf(i) > -1;

            if (rule.once && inarray) {
                return arguments[0];
            }

            var args = Array.prototype.slice.call(arguments);

            if (!inarray) {
                res.rulesWritten.push(i);
            }

            if (typeof rule.fn === 'function') {
                return rule.fn.apply(this, args);
            } else {
                return rule.fn
            }
        }

    });

    return body;
};

/**
 * Extensions that will be ignored by default
 * @type {Array}
 */
utils.defaultIgnoreTypes = [
    // text files
    "js", "json", "css",
    // image files
    "png", "jpg", "jpeg", "gif", "ico", "tif", "tiff", "bmp", "webp", "psd",
    // vector & font
    "svg", "woff", "ttf", "otf", "eot", "eps", "ps", "ai",
    // audio
    "mp3", "wav", "aac", "m4a", "m3u", "mid", "wma",
    // video & other media
    "mpg", "mpeg", "mp4", "m4v", "webm", "swf", "flv", "avi", "mov", "wmv",
    // document files
    "pdf", "doc", "docx", "xls", "xlsx", "pps", "ppt", "pptx", "odt", "ods", "odp", "pages", "key", "rtf", "txt", "csv",
    // data files
    "zip", "rar", "tar", "gz", "xml", "app", "exe", "jar", "dmg", "pkg", "iso"
].map(function (ext) {
        return "\\." + ext + "(\\?.*)?$";
    });

/**
 * Check if a URL was white-listed
 * @param url
 * @param whitelist
 * @returns {boolean}
 */
utils.isWhitelisted = function isWhitelisted(url, whitelist) {

    if (whitelist.indexOf(url) > -1) {
        return true;
    }

    return whitelist.some(function (pattern) {
        return minimatch(url, pattern);
    });
};

/**
 * Check if a URL was white-listed with single path
 * @param url
 * @param opts
 * @returns {boolean}
 */
utils.isWhiteListedForSingle = function isWhiteListedForSingle(url, rules) {

    return rules.filter(function (item) {
        return item.paths && utils.isWhitelisted(url, utils.toArray(item.paths));
    });
};

/**
 * Determine if a response should be overwritten
 * @param url
 * @returns {boolean}
 */
utils.inBlackList = function inBlackList(url, opts) {

    // First check for an exact match
    if (!url || opts.blacklist.indexOf(url) > -1) {
        return true;
    }

    if (url.length === 1 && url === "/") {
        return false;
    }

    // second, check that the URL does not contain a
    // file extension that should be ignored by default
    if (opts.ignore.some(function (pattern) {
            return new RegExp(pattern).test(url);
        })) {
        return true;
    }

    // Finally, check any mini-match patterns for paths that have been excluded
    if (opts.blacklist.some(function (pattern) {
            return minimatch(url, pattern);
        })) {
        return true;
    }

    return false;
};

/**
 * @param req
 * @returns {*}
 */
utils.hasAcceptHeaders = function hasAcceptHeaders(req) {
    var acceptHeader = req.headers["accept"];
    if (!acceptHeader) {
        return false;
    }
    return (~acceptHeader.indexOf("html"));
};

/**
 * @param body
 * @returns {boolean}
 */
utils.snip = function snip(body) {
    if (!body) {
        return false;
    }
};

utils.exists = function exists(body, regex) {
    if (!body) {
        return false;
    }
    return regex.test(body);
};

utils.toArray = function toArray(item) {
    if (!item) {
        return item;
    }
    if (!Array.isArray(item)) {
        return [item];
    }
    return item;
};

utils.isHtml = function isHtml(str) {
    if (!str) {
        return false;
    }
    // Test to see if start of file contents matches:
    // - Optional byte-order mark (BOM)
    // - Zero or more spaces
    // - Any sort of HTML tag, comment, or doctype tag (basically, <...>)
    return /^(\uFEFF|\uFFFE)?\s*<[^>]+>/i.test(str);
};
