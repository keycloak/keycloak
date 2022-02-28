(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/OverflowMenu/overflow-menu", "./OverflowMenuContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/OverflowMenu/overflow-menu"), require("./OverflowMenuContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.overflowMenu, global.OverflowMenuContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _overflowMenu, _OverflowMenuContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OverflowMenuContent = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _overflowMenu2 = _interopRequireDefault(_overflowMenu);

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

  const OverflowMenuContent = exports.OverflowMenuContent = ({
    className,
    children,
    isPersistent
  }) => React.createElement(_OverflowMenuContext.OverflowMenuContext.Consumer, null, value => (!value.isBelowBreakpoint || isPersistent) && React.createElement("div", {
    className: (0, _reactStyles.css)(_overflowMenu2.default.overflowMenuContent, className)
  }, children));

  OverflowMenuContent.propTypes = {
    children: _propTypes2.default.any,
    className: _propTypes2.default.string,
    isPersistent: _propTypes2.default.bool
  };
});
//# sourceMappingURL=OverflowMenuContent.js.map