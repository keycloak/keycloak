(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Accordion/accordion", "@patternfly/react-icons/dist/js/icons/angle-right-icon", "./AccordionContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Accordion/accordion"), require("@patternfly/react-icons/dist/js/icons/angle-right-icon"), require("./AccordionContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.accordion, global.angleRightIcon, global.AccordionContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _accordion, _angleRightIcon, _AccordionContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.AccordionToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _accordion2 = _interopRequireDefault(_accordion);

  var _angleRightIcon2 = _interopRequireDefault(_angleRightIcon);

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

  const AccordionToggle = exports.AccordionToggle = _ref => {
    let {
      className = '',
      id,
      isExpanded = false,
      children = null,
      component
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "id", "isExpanded", "children", "component"]);

    return React.createElement(_AccordionContext.AccordionContext.Consumer, null, ({
      ToggleContainer
    }) => {
      const Container = component || ToggleContainer;
      return React.createElement(Container, null, React.createElement("button", _extends({
        id: id,
        className: (0, _reactStyles.css)(_accordion2.default.accordionToggle, isExpanded && _accordion2.default.modifiers.expanded, className)
      }, props, {
        "aria-expanded": isExpanded
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_accordion2.default.accordionToggleText)
      }, children), React.createElement(_angleRightIcon2.default, {
        className: (0, _reactStyles.css)(_accordion2.default.accordionToggleIcon)
      })));
    });
  };

  AccordionToggle.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isExpanded: _propTypes2.default.bool,
    id: _propTypes2.default.string.isRequired,
    component: _propTypes2.default.any
  };
});
//# sourceMappingURL=AccordionToggle.js.map