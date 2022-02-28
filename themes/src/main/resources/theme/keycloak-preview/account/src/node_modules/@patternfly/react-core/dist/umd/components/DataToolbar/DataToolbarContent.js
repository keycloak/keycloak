(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/DataToolbar/data-toolbar", "@patternfly/react-styles", "./DataToolbarUtils", "../../helpers/util", "./DataToolbarExpandableContent"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"), require("@patternfly/react-styles"), require("./DataToolbarUtils"), require("../../helpers/util"), require("./DataToolbarExpandableContent"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.dataToolbar, global.reactStyles, global.DataToolbarUtils, global.util, global.DataToolbarExpandableContent);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _dataToolbar, _reactStyles, _DataToolbarUtils, _util, _DataToolbarExpandableContent) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataToolbarContent = undefined;

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

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  class DataToolbarContent extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "expandableContentRef", React.createRef());

      _defineProperty(this, "chipContainerRef", React.createRef());
    }

    render() {
      const _this$props = this.props,
            {
        className,
        children,
        isExpanded,
        toolbarId,
        breakpointMods,
        clearAllFilters,
        showClearFiltersButton,
        clearFiltersButtonText
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "children", "isExpanded", "toolbarId", "breakpointMods", "clearAllFilters", "showClearFiltersButton", "clearFiltersButtonText"]);

      const expandableContentId = `${toolbarId}-expandable-content-${DataToolbarContent.currentId++}`;
      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_dataToolbar2.default.dataToolbarContent, (0, _util.formatBreakpointMods)(breakpointMods, _dataToolbar2.default), className)
      }, props), React.createElement(_DataToolbarUtils.DataToolbarContentContext.Provider, {
        value: {
          expandableContentRef: this.expandableContentRef,
          expandableContentId,
          chipContainerRef: this.chipContainerRef
        }
      }, React.createElement("div", {
        className: (0, _reactStyles.css)(_dataToolbar2.default.dataToolbarContentSection)
      }, children), React.createElement(_DataToolbarExpandableContent.DataToolbarExpandableContent, {
        id: expandableContentId,
        isExpanded: isExpanded,
        expandableContentRef: this.expandableContentRef,
        chipContainerRef: this.chipContainerRef,
        clearAllFilters: clearAllFilters,
        showClearFiltersButton: showClearFiltersButton,
        clearFiltersButtonText: clearFiltersButtonText
      })));
    }

  }

  exports.DataToolbarContent = DataToolbarContent;

  _defineProperty(DataToolbarContent, "propTypes", {
    className: _propTypes2.default.string,
    breakpointMods: _propTypes2.default.arrayOf(_propTypes2.default.any),
    children: _propTypes2.default.node,
    isExpanded: _propTypes2.default.bool,
    clearAllFilters: _propTypes2.default.func,
    showClearFiltersButton: _propTypes2.default.bool,
    clearFiltersButtonText: _propTypes2.default.string,
    toolbarId: _propTypes2.default.string
  });

  _defineProperty(DataToolbarContent, "currentId", 0);

  _defineProperty(DataToolbarContent, "defaultProps", {
    isExpanded: false,
    breakpointMods: [],
    showClearFiltersButton: false
  });
});
//# sourceMappingURL=DataToolbarContent.js.map