(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/DataToolbar/data-toolbar", "@patternfly/react-styles", "./DataToolbarUtils", "./DataToolbarChipGroupContent"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"), require("@patternfly/react-styles"), require("./DataToolbarUtils"), require("./DataToolbarChipGroupContent"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.dataToolbar, global.reactStyles, global.DataToolbarUtils, global.DataToolbarChipGroupContent);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _dataToolbar, _reactStyles, _DataToolbarUtils, _DataToolbarChipGroupContent) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataToolbar = undefined;

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

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      if (enumerableOnly) symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      });
      keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};

      if (i % 2) {
        ownKeys(source, true).forEach(function (key) {
          _defineProperty(target, key, source[key]);
        });
      } else if (Object.getOwnPropertyDescriptors) {
        Object.defineProperties(target, Object.getOwnPropertyDescriptors(source));
      } else {
        ownKeys(source).forEach(function (key) {
          Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
        });
      }
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

  class DataToolbar extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "chipGroupContentRef", React.createRef());

      _defineProperty(this, "isToggleManaged", () => !(this.props.isExpanded || !!this.props.toggleIsExpanded));

      _defineProperty(this, "toggleIsExpanded", () => {
        this.setState(prevState => ({
          isManagedToggleExpanded: !prevState.isManagedToggleExpanded
        }));
      });

      _defineProperty(this, "closeExpandableContent", () => {
        this.setState(() => ({
          isManagedToggleExpanded: false
        }));
      });

      _defineProperty(this, "updateNumberFilters", (categoryName, numberOfFilters) => {
        const filterInfoToUpdate = _objectSpread({}, this.state.filterInfo);

        if (!filterInfoToUpdate.hasOwnProperty(categoryName) || filterInfoToUpdate[categoryName] !== numberOfFilters) {
          filterInfoToUpdate[categoryName] = numberOfFilters;
          this.setState({
            filterInfo: filterInfoToUpdate
          });
        }
      });

      _defineProperty(this, "getNumberOfFilters", () => Object.values(this.state.filterInfo).reduce((acc, cur) => acc + cur, 0));

      this.state = {
        isManagedToggleExpanded: false,
        filterInfo: {}
      };
    }

    componentDidMount() {
      if (this.isToggleManaged()) {
        window.addEventListener('resize', this.closeExpandableContent);
      }

      if (process.env.NODE_ENV !== 'production' && !DataToolbar.hasWarnBeta) {
        // eslint-disable-next-line no-console
        console.warn('You are using a beta component (DataToolbar). These api parts are subject to change in the future.');
        DataToolbar.hasWarnBeta = true;
      }
    }

    componentWillUnmount() {
      if (this.isToggleManaged()) {
        window.removeEventListener('resize', this.closeExpandableContent);
      }
    }

    render() {
      const _this$props = this.props,
            {
        clearAllFilters,
        clearFiltersButtonText,
        collapseListedFiltersBreakpoint,
        isExpanded,
        toggleIsExpanded,
        className,
        children,
        id
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["clearAllFilters", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "isExpanded", "toggleIsExpanded", "className", "children", "id"]);

      const {
        isManagedToggleExpanded
      } = this.state;
      const isToggleManaged = this.isToggleManaged();
      const numberOfFilters = this.getNumberOfFilters();
      const showClearFiltersButton = numberOfFilters > 0;
      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_dataToolbar2.default.dataToolbar, className),
        id: id
      }, props), React.createElement(_DataToolbarUtils.DataToolbarContext.Provider, {
        value: {
          isExpanded: this.isToggleManaged() ? isManagedToggleExpanded : isExpanded,
          toggleIsExpanded: isToggleManaged ? this.toggleIsExpanded : toggleIsExpanded,
          chipGroupContentRef: this.chipGroupContentRef,
          updateNumberFilters: this.updateNumberFilters,
          numberOfFilters
        }
      }, React.Children.map(children, child => {
        if (React.isValidElement(child)) {
          return React.cloneElement(child, {
            clearAllFilters,
            clearFiltersButtonText,
            showClearFiltersButton,
            isExpanded: isToggleManaged ? isManagedToggleExpanded : isExpanded,
            toolbarId: id
          });
        } else {
          return child;
        }
      }), React.createElement(_DataToolbarChipGroupContent.DataToolbarChipGroupContent, {
        isExpanded: isToggleManaged ? isManagedToggleExpanded : isExpanded,
        chipGroupContentRef: this.chipGroupContentRef,
        clearAllFilters: clearAllFilters,
        showClearFiltersButton: showClearFiltersButton,
        clearFiltersButtonText: clearFiltersButtonText,
        numberOfFilters: numberOfFilters,
        collapseListedFiltersBreakpoint: collapseListedFiltersBreakpoint
      })));
    }

  }

  exports.DataToolbar = DataToolbar;

  _defineProperty(DataToolbar, "propTypes", {
    clearAllFilters: _propTypes2.default.func,
    clearFiltersButtonText: _propTypes2.default.string,
    collapseListedFiltersBreakpoint: _propTypes2.default.oneOf(['md', 'lg', 'xl', '2xl']),
    isExpanded: _propTypes2.default.bool,
    toggleIsExpanded: _propTypes2.default.func,
    className: _propTypes2.default.string,
    children: _propTypes2.default.node,
    id: _propTypes2.default.string.isRequired
  });

  _defineProperty(DataToolbar, "hasWarnBeta", false);
});
//# sourceMappingURL=DataToolbar.js.map