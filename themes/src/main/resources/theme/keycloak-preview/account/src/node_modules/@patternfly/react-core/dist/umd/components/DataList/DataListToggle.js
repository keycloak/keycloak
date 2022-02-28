(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/angle-right-icon", "@patternfly/react-styles/css/components/DataList/data-list", "../Button"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/angle-right-icon"), require("@patternfly/react-styles/css/components/DataList/data-list"), require("../Button"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.angleRightIcon, global.dataList, global.Button);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _angleRightIcon, _dataList, _Button) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataListToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _angleRightIcon2 = _interopRequireDefault(_angleRightIcon);

  var _dataList2 = _interopRequireDefault(_dataList);

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

  const DataListToggle = exports.DataListToggle = _ref => {
    let {
      className = '',
      isExpanded = false,
      'aria-controls': ariaControls = '',
      'aria-label': ariaLabel = 'Details',
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      'aria-labelledby': ariaLabelledBy = '',
      rowid = '',
      id
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "isExpanded", "aria-controls", "aria-label", "aria-labelledby", "rowid", "id"]);

    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(_dataList2.default.dataListItemControl, className)
    }, props), React.createElement("div", {
      className: (0, _reactStyles.css)(_dataList2.default.dataListToggle)
    }, React.createElement(_Button.Button, {
      id: id,
      variant: _Button.ButtonVariant.plain,
      "aria-controls": ariaControls !== '' && ariaControls,
      "aria-label": ariaLabel,
      "aria-labelledby": ariaLabel !== 'Details' ? null : `${rowid} ${id}`,
      "aria-expanded": isExpanded
    }, React.createElement(_angleRightIcon2.default, null))));
  };

  DataListToggle.propTypes = {
    className: _propTypes2.default.string,
    isExpanded: _propTypes2.default.bool,
    id: _propTypes2.default.string.isRequired,
    rowid: _propTypes2.default.string,
    'aria-labelledby': _propTypes2.default.string,
    'aria-label': _propTypes2.default.string,
    'aria-controls': _propTypes2.default.string
  };
});
//# sourceMappingURL=DataListToggle.js.map