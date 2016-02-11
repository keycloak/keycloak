var Immutable  = require("immutable");

var defaults = Immutable.fromJS({
    port: 3001,
    weinre: {
        port: 8080
    }
});

/**
 * @param {Object} obj
 * @returns {Map}
 */
module.exports.merge = function (obj) {
    return defaults.mergeDeep(Immutable.fromJS(obj));
};

/**
 * @param {Immutable.Map} obj
 * @returns {*}
 */
//function transformOptions(obj) {
//
//    var out;
//
//    Object.keys(transforms).forEach(function (key) {
//        out = obj.set(key, transforms[key](obj));
//    });
//
//    return out;
//}