var Immutable = require("immutable");

module.exports.init = function (ui, bs) {

    var optPath = ["remote-debug", "compression"];

    ui.setOptionIn(optPath, Immutable.Map({
        name: "compression",
        title: "Compression",
        active: false,
        tagline: "Add Gzip Compression to all responses"
    }));

    var methods = {
        toggle: function (value) {
            if (value !== true) {
                value = false;
            }
            if (value) {
                ui.setOptionIn(optPath.concat("active"), true);
                bs.addMiddleware("", require("compression")(), {id: "ui-compression", override: true});
            } else {
                ui.setOptionIn(optPath.concat("active"), false);
                bs.removeMiddleware("ui-compression");
            }
        },
        event: function (event) {
            methods[event.event](event.data);
        }
    };

    return methods;
};