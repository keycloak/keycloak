var _typeof = require("./typeof.js")["default"];

var _WeakMap = require("@babel/runtime-corejs3/core-js/weak-map");

var _reduceInstanceProperty = require("@babel/runtime-corejs3/core-js/instance/reduce");

var _Object$keys = require("@babel/runtime-corejs3/core-js/object/keys");

var _Object$create = require("@babel/runtime-corejs3/core-js/object/create");

var _Symbol$replace = require("@babel/runtime-corejs3/core-js/symbol/replace");

var _sliceInstanceProperty = require("@babel/runtime-corejs3/core-js/instance/slice");

var setPrototypeOf = require("./setPrototypeOf.js");

var inherits = require("./inherits.js");

function _wrapRegExp() {
  module.exports = _wrapRegExp = function _wrapRegExp(re, groups) {
    return new BabelRegExp(re, void 0, groups);
  }, module.exports.__esModule = true, module.exports["default"] = module.exports;

  var _super = RegExp.prototype,
      _groups = new _WeakMap();

  function BabelRegExp(re, flags, groups) {
    var _this = new RegExp(re, flags);

    return _groups.set(_this, groups || _groups.get(re)), setPrototypeOf(_this, BabelRegExp.prototype);
  }

  function buildGroups(result, re) {
    var _context;

    var g = _groups.get(re);

    return _reduceInstanceProperty(_context = _Object$keys(g)).call(_context, function (groups, name) {
      return groups[name] = result[g[name]], groups;
    }, _Object$create(null));
  }

  return inherits(BabelRegExp, RegExp), BabelRegExp.prototype.exec = function (str) {
    var result = _super.exec.call(this, str);

    return result && (result.groups = buildGroups(result, this)), result;
  }, BabelRegExp.prototype[_Symbol$replace] = function (str, substitution) {
    if ("string" == typeof substitution) {
      var groups = _groups.get(this);

      return _super[_Symbol$replace].call(this, str, substitution.replace(/\$<([^>]+)>/g, function (_, name) {
        return "$" + groups[name];
      }));
    }

    if ("function" == typeof substitution) {
      var _this = this;

      return _super[_Symbol$replace].call(this, str, function () {
        var args = arguments;
        return "object" != _typeof(args[args.length - 1]) && (args = _sliceInstanceProperty([]).call(args)).push(buildGroups(args, _this)), substitution.apply(this, args);
      });
    }

    return _super[_Symbol$replace].call(this, str, substitution);
  }, _wrapRegExp.apply(this, arguments);
}

module.exports = _wrapRegExp, module.exports.__esModule = true, module.exports["default"] = module.exports;