(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../Dropdown", "./OverflowMenuContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../Dropdown"), require("./OverflowMenuContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.Dropdown, global.OverflowMenuContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _Dropdown, _OverflowMenuContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OverflowMenuDropdownItem = undefined;

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

  const OverflowMenuDropdownItem = exports.OverflowMenuDropdownItem = ({
    children,
    isShared = false
  }) => React.createElement(_OverflowMenuContext.OverflowMenuContext.Consumer, null, value => (!isShared || value.isBelowBreakpoint) && React.createElement(_Dropdown.DropdownItem, {
    component: "button"
  }, " ", children, " "));

  OverflowMenuDropdownItem.propTypes = {
    children: _propTypes2.default.any,
    isShared: _propTypes2.default.bool
  };
});
//# sourceMappingURL=OverflowMenuDropdownItem.js.map