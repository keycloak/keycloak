var tokenize   = require("html-tokenize");
var through2   = require("through2");
var vinyl      = require("vinyl");
var select     = require("html-select");

/**
 * @param config
 * @param item
 * @param markup
 * @param done
 */
function directiveStripper(config, item, markup, done) {

    var replacer = getReplacer(item, config);
    var chunks = [];

    new vinyl({
            contents: new Buffer(markup)
        })
        .pipe(tokenize())
        .pipe(replacer)
        .pipe(through2.obj(function (row, buf, next) {
            chunks.push(row[1]);
            next();
        }, function () {
            done(null, chunks.join(""));
        }));

    replacer.resume();
}

/**
 * @param name
 * @param item
 * @returns {*|exports}
 */
function getReplacer (name, markup) {

    return select(name, function (e) {

        var tr = through2.obj(function (row, buf, next) {

            if (row[0] === "open") {
                this.push([row[0], directive(name, String(row[1]), markup)]);
            } else {
                this.push([ row[0], "" ]);
            }

            next();
        });

        tr.pipe(e.createStream()).pipe(tr);
    });
}

/**
 * @param name
 * @param content
 * @param item
 * @returns {*|string}
 */
function directive (name, content, item) {

    var angularDir;
    try {
        angularDir = require("../src/scripts/directives/" + name)();
    } catch (e) {
        console.log("Directive not found, cannot re-use");
        return content;
    }

    var scope = item;

    scope = angularDir.link(scope, {}, {});

    return angularDir.template.replace(/\{\{(.+?)\}\}/, function ($1, $2) {
        if ($2 in scope) {
            return scope[$2];
        }
        return $1;
    });
}

module.exports.getReplacer       = getReplacer;
module.exports.directive         = directive;
module.exports.directiveStripper = directiveStripper;