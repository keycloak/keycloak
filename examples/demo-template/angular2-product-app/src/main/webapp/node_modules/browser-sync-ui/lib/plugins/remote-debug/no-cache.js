var Immutable = require("immutable");

module.exports.init = function (ui, bs) {

    var optPath = ["remote-debug", "no-cache"];

    ui.setOptionIn(optPath, Immutable.Map({
        name: "no-cache",
        title: "No Cache",
        active: false,
        tagline: "Disable all Browser Caching"
    }));

    var methods = {
        toggle: function (value) {
            if (value !== true) {
                value = false;
            }
            if (value) {
                ui.setOptionIn(optPath.concat("active"), true);
                bs.addMiddleware("*", function (req, res, next) {
                    res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    res.setHeader("Pragma", "no-cache");
                    res.setHeader("Expires", "0");
                    next();
                }, {id: "ui-no-cache", override: true});
            } else {
                ui.setOptionIn(optPath.concat("active"), false);
                bs.removeMiddleware("ui-no-cache");
            }
        },
        event: function (event) {
            methods[event.event](event.data);
        }
    };

    return methods;
};