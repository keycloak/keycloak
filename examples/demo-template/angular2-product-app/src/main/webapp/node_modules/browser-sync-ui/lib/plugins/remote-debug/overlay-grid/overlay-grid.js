var Immutable = require("immutable");
var fs        = require("fs");
var path      = require("path");
var baseHorizontal = fs.readFileSync(path.resolve(__dirname, "css/grid-overlay-horizontal.css"), "utf8");
var baseVertical   = fs.readFileSync(path.resolve(__dirname, "css/grid-overlay-vertical.css"), "utf8");

function template (string, obj) {
    obj = obj || {};
    return string.replace(/\{\{(.+?)\}\}/g, function () {
        if (obj[arguments[1]]) {
           return obj[arguments[1]];
        }
        return "";
    });
}

function getCss(opts) {

    var base = opts.selector + " {position:relative;}";

    if (opts.horizontal) {
        base += baseHorizontal;
    }

    if (opts.vertical) {
        base += baseVertical;
    }

    return template(base, opts);
}

module.exports.init = function (ui) {

    const TRANSMIT_EVENT = "ui:remote-debug:css-overlay-grid";
    const READY_EVENT    = "ui:remote-debug:css-overlay-grid:ready";
    const OPT_PATH       = ["remote-debug", "overlay-grid"];

    var defaults = {
        offsetY:  "0",
        offsetX:  "0",
        size:     "16px",
        selector: "body",
        color:    "rgba(0, 0, 0, .2)",
        horizontal: true,
        vertical: true
    };

    ui.clients.on("connection", function (client) {
        client.on(READY_EVENT, function () {
            client.emit(TRANSMIT_EVENT, {
                innerHTML: getCss(ui.options.getIn(OPT_PATH).toJS())
            });
        });
    });

    ui.setOptionIn(OPT_PATH, Immutable.Map({
        name: "overlay-grid",
        title: "Overlay CSS Grid",
        active: false,
        tagline: "Add an adjustable CSS overlay grid to your webpage",
        innerHTML: ""
    }).merge(defaults));


    var methods = {
        toggle: function (value) {
            if (value !== true) {
                value = false;
            }
            if (value) {
                ui.setOptionIn(OPT_PATH.concat("active"), true);
                ui.enableElement({name: "overlay-grid-js"});
            } else {
                ui.setOptionIn(OPT_PATH.concat("active"), false);
                ui.disableElement({name: "overlay-grid-js"});
                ui.clients.emit("ui:element:remove", {id: "__bs_overlay-grid-styles__"});
            }
        },
        adjust: function (data) {

            ui.setOptionIn(OPT_PATH, ui.getOptionIn(OPT_PATH).merge(data));

            ui.clients.emit(TRANSMIT_EVENT, {
                innerHTML: getCss(ui.options.getIn(OPT_PATH).toJS())
            });
        },
        "toggle:axis": function (item) {

            ui.setOptionIn(OPT_PATH.concat([item.axis]), item.value);

            ui.clients.emit(TRANSMIT_EVENT, {
                innerHTML: getCss(ui.options.getIn(OPT_PATH).toJS())
            });
        },
        event: function (event) {
            methods[event.event](event.data);
        }
    };

    return methods;
};