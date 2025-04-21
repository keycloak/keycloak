import _typeof from "./typeof.js";
import _WeakMap from "@babel/runtime-corejs3/core-js/weak-map";
import _reduceInstanceProperty from "@babel/runtime-corejs3/core-js/instance/reduce";
import _Object$keys from "@babel/runtime-corejs3/core-js/object/keys";
import _Object$create from "@babel/runtime-corejs3/core-js/object/create";
import _Symbol$replace from "@babel/runtime-corejs3/core-js/symbol/replace";
import _sliceInstanceProperty from "@babel/runtime-corejs3/core-js/instance/slice";
import setPrototypeOf from "./setPrototypeOf.js";
import inherits from "./inherits.js";
export default function _wrapRegExp() {
  _wrapRegExp = function _wrapRegExp(re, groups) {
    return new BabelRegExp(re, void 0, groups);
  };

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