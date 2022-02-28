(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ToggleTemplate = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  function _getRequireWildcardCache() {
    if (typeof WeakMap !== "function") return null;
    var cache = new WeakMap();

    _getRequireWildcardCache = function () {
      return cache;
    };

    return cache;
  }

  function _interopRequireWildcard(obj) {
    if (obj && obj.__esModule) {
      return obj;
    }

    var cache = _getRequireWildcardCache();

    if (cache && cache.has(obj)) {
      return cache.get(obj);
    }

    var newObj = {};

    if (obj != null) {
      var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor;

      for (var key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null;

          if (desc && (desc.get || desc.set)) {
            Object.defineProperty(newObj, key, desc);
          } else {
            newObj[key] = obj[key];
          }
        }
      }
    }

    newObj.default = obj;

    if (cache) {
      cache.set(obj, newObj);
    }

    return newObj;
  }

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  const ToggleTemplate = exports.ToggleTemplate = ({
    firstIndex = 0,
    lastIndex = 0,
    itemCount = 0,
    itemsTitle = 'items'
  }) => React.createElement(React.Fragment, null, React.createElement("b", null, firstIndex, " - ", lastIndex), ' ', "of ", React.createElement("b", null, itemCount), " ", itemsTitle);

  ToggleTemplate.propTypes = {
    firstIndex: _propTypes2.default.number,
    lastIndex: _propTypes2.default.number,
    itemCount: _propTypes2.default.number,
    itemsTitle: _propTypes2.default.string
  };
});
//# sourceMappingURL=ToggleTemplate.js.map