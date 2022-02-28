(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.TabContent = undefined;

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

  function _extends() {
    _extends = Object.assign || function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

    return _extends.apply(this, arguments);
  }

  function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};

    var target = _objectWithoutPropertiesLoose(source, excluded);

    var key, i;

    if (Object.getOwnPropertySymbols) {
      var sourceSymbolKeys = Object.getOwnPropertySymbols(source);

      for (i = 0; i < sourceSymbolKeys.length; i++) {
        key = sourceSymbolKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
        target[key] = source[key];
      }
    }

    return target;
  }

  function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;

    for (i = 0; i < sourceKeys.length; i++) {
      key = sourceKeys[i];
      if (excluded.indexOf(key) >= 0) continue;
      target[key] = source[key];
    }

    return target;
  }

  const TabContentBase = _ref => {
    let {
      id,
      activeKey,
      'aria-label': ariaLabel,
      child,
      children,
      className,
      eventKey,
      innerRef
    } = _ref,
        props = _objectWithoutProperties(_ref, ["id", "activeKey", "aria-label", "child", "children", "className", "eventKey", "innerRef"]);

    if (children || child) {
      let labelledBy;

      if (ariaLabel) {
        labelledBy = null;
      } else {
        labelledBy = children ? `pf-tab-${eventKey}-${id}` : `pf-tab-${child.props.eventKey}-${id}`;
      }

      return React.createElement("section", _extends({
        ref: innerRef,
        hidden: children ? null : child.props.eventKey !== activeKey,
        className: children ? (0, _reactStyles.css)('pf-c-tab-content', className) : (0, _reactStyles.css)('pf-c-tab-content', child.props.className),
        id: children ? id : `pf-tab-section-${child.props.eventKey}-${id}`,
        "aria-label": ariaLabel,
        "aria-labelledby": labelledBy,
        role: "tabpanel",
        tabIndex: 0
      }, props), children || child.props.children);
    }

    return null;
  };

  TabContentBase.propTypes = {
    children: _propTypes2.default.any,
    child: _propTypes2.default.element,
    className: _propTypes2.default.string,
    activeKey: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    eventKey: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    innerRef: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.func, _propTypes2.default.object]),
    id: _propTypes2.default.string.isRequired,
    'aria-label': _propTypes2.default.string
  };
  const TabContent = exports.TabContent = React.forwardRef((props, ref) => React.createElement(TabContentBase, _extends({}, props, {
    innerRef: ref
  })));
});
//# sourceMappingURL=TabContent.js.map