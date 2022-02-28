(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Accordion/accordion", "./AccordionContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Accordion/accordion"), require("./AccordionContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.accordion, global.AccordionContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _accordion, _AccordionContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.AccordionContent = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _accordion2 = _interopRequireDefault(_accordion);

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

  const AccordionContent = exports.AccordionContent = _ref => {
    let {
      className = '',
      children = null,
      id = '',
      isHidden = false,
      isFixed = false,
      'aria-label': ariaLabel = '',
      component
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "children", "id", "isHidden", "isFixed", "aria-label", "component"]);

    return React.createElement(_AccordionContext.AccordionContext.Consumer, null, ({
      ContentContainer
    }) => {
      const Container = component || ContentContainer;
      return React.createElement(Container, _extends({
        id: id,
        className: (0, _reactStyles.css)(_accordion2.default.accordionExpandedContent, isFixed && _accordion2.default.modifiers.fixed, !isHidden && _accordion2.default.modifiers.expanded, className),
        hidden: isHidden,
        "aria-label": ariaLabel
      }, props), React.createElement("div", {
        className: (0, _reactStyles.css)(_accordion2.default.accordionExpandedContentBody)
      }, children));
    });
  };

  AccordionContent.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    id: _propTypes2.default.string,
    isHidden: _propTypes2.default.bool,
    isFixed: _propTypes2.default.bool,
    'aria-label': _propTypes2.default.string,
    component: _propTypes2.default.any
  };
});
//# sourceMappingURL=AccordionContent.js.map