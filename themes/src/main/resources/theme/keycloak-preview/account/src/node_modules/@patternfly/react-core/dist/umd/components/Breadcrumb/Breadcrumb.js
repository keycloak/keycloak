(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Breadcrumb/breadcrumb", "@patternfly/react-styles", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Breadcrumb/breadcrumb"), require("@patternfly/react-styles"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.breadcrumb, global.reactStyles, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _breadcrumb, _reactStyles, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Breadcrumb = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _breadcrumb2 = _interopRequireDefault(_breadcrumb);

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

  const Breadcrumb = _ref => {
    let {
      children = null,
      className = '',
      'aria-label': ariaLabel = 'Breadcrumb',
      ouiaContext = null,
      ouiaId = null
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "aria-label", "ouiaContext", "ouiaId"]);

    return React.createElement("nav", _extends({}, props, {
      "aria-label": ariaLabel,
      className: (0, _reactStyles.css)(_breadcrumb2.default.breadcrumb, className)
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Breadcrumb',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }), React.createElement("ol", {
      className: (0, _reactStyles.css)(_breadcrumb2.default.breadcrumbList)
    }, children));
  };

  Breadcrumb.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    'aria-label': _propTypes2.default.string
  };
  const BreadcrumbWithOuiaContext = (0, _withOuia.withOuiaContext)(Breadcrumb);
  exports.Breadcrumb = BreadcrumbWithOuiaContext;
});
//# sourceMappingURL=Breadcrumb.js.map