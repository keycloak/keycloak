(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/DataToolbar/data-toolbar", "@patternfly/react-styles", "../../helpers/util"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"), require("@patternfly/react-styles"), require("../../helpers/util"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.dataToolbar, global.reactStyles, global.util);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _dataToolbar, _reactStyles, _util) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataToolbarItem = exports.DataToolbarItemVariant = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _dataToolbar2 = _interopRequireDefault(_dataToolbar);

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

  let DataToolbarItemVariant = exports.DataToolbarItemVariant = undefined;

  (function (DataToolbarItemVariant) {
    DataToolbarItemVariant["separator"] = "separator";
    DataToolbarItemVariant["bulk-select"] = "bulk-select";
    DataToolbarItemVariant["overflow-menu"] = "overflow-menu";
    DataToolbarItemVariant["pagination"] = "pagination";
    DataToolbarItemVariant["search-filter"] = "search-filter";
    DataToolbarItemVariant["label"] = "label";
    DataToolbarItemVariant["chip-group"] = "chip-group";
  })(DataToolbarItemVariant || (exports.DataToolbarItemVariant = DataToolbarItemVariant = {}));

  const DataToolbarItem = exports.DataToolbarItem = _ref => {
    let {
      className,
      variant,
      breakpointMods = [],
      id,
      children
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "variant", "breakpointMods", "id", "children"]);

    const labelVariant = variant === 'label';
    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(_dataToolbar2.default.dataToolbarItem, variant && (0, _reactStyles.getModifier)(_dataToolbar2.default, variant), (0, _util.formatBreakpointMods)(breakpointMods, _dataToolbar2.default), className)
    }, labelVariant && {
      'aria-hidden': true
    }, {
      id: id
    }, props), children);
  };

  DataToolbarItem.propTypes = {
    className: _propTypes2.default.string,
    variant: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['separator']), _propTypes2.default.oneOf(['bulk-select']), _propTypes2.default.oneOf(['overflow-menu']), _propTypes2.default.oneOf(['pagination']), _propTypes2.default.oneOf(['search-filter']), _propTypes2.default.oneOf(['label']), _propTypes2.default.oneOf(['chip-group'])]),
    breakpointMods: _propTypes2.default.arrayOf(_propTypes2.default.any),
    id: _propTypes2.default.string,
    children: _propTypes2.default.node
  };
});
//# sourceMappingURL=DataToolbarItem.js.map