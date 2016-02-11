var Immutable = require("immutable");

module.exports.init = function (ui, bs) {

    var timeout = 0;
    var optPath = ["remote-debug", "latency"];

    ui.setOptionIn(optPath, Immutable.Map({
        name: "latency",
        title: "Latency",
        active: false,
        tagline: "Simulate slower connections by throttling the response time of each request.",
        rate: 0
    }));

    var methods = {
        toggle: function (value) {
            if (value !== true) {
                value = false;
            }
            if (value) {
                ui.setOptionIn(optPath.concat("active"), true);
                bs.addMiddleware("*", function (req, res, next) {
                    setTimeout(next, timeout);
                }, {id: "cp-latency", override: true});
            } else {
                ui.setOptionIn(optPath.concat("active"), false);
                bs.removeMiddleware("cp-latency");
            }
        },
        adjust: function (data) {
            timeout   = parseFloat(data.rate) * 1000;
            var saved = ui.options.getIn(optPath.concat("rate"));
            if (saved !== data.rate) {
                ui.setOptionIn(optPath.concat("rate"), timeout/1000);
            }
        },
        event: function (event) {
            methods[event.event](event.data);
        }
    };

    return methods;
};