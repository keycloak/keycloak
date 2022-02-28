(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/AppLauncher/app-launcher", "@patternfly/react-styles/css/components/FormControl/form-control", "@patternfly/react-icons/dist/js/icons/th-icon", "../Dropdown", "../Dropdown/DropdownWithContext", "./ApplicationLauncherGroup", "./ApplicationLauncherSeparator", "./ApplicationLauncherItem", "./ApplicationLauncherContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"), require("@patternfly/react-styles/css/components/FormControl/form-control"), require("@patternfly/react-icons/dist/js/icons/th-icon"), require("../Dropdown"), require("../Dropdown/DropdownWithContext"), require("./ApplicationLauncherGroup"), require("./ApplicationLauncherSeparator"), require("./ApplicationLauncherItem"), require("./ApplicationLauncherContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.appLauncher, global.formControl, global.thIcon, global.Dropdown, global.DropdownWithContext, global.ApplicationLauncherGroup, global.ApplicationLauncherSeparator, global.ApplicationLauncherItem, global.ApplicationLauncherContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _appLauncher, _formControl, _thIcon, _Dropdown, _DropdownWithContext, _ApplicationLauncherGroup, _ApplicationLauncherSeparator, _ApplicationLauncherItem, _ApplicationLauncherContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ApplicationLauncher = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _appLauncher2 = _interopRequireDefault(_appLauncher);

  var _formControl2 = _interopRequireDefault(_formControl);

  var _thIcon2 = _interopRequireDefault(_thIcon);

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

  class ApplicationLauncher extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "createSearchBox", () => {
        const {
          onSearch,
          searchPlaceholderText,
          searchProps
        } = this.props;
        return React.createElement("div", {
          key: "search",
          className: (0, _reactStyles.css)(_appLauncher2.default.appLauncherMenuSearch)
        }, React.createElement(_ApplicationLauncherItem.ApplicationLauncherItem, {
          customChild: React.createElement("input", _extends({
            type: "search",
            className: (0, _reactStyles.css)(_formControl2.default.formControl),
            placeholder: searchPlaceholderText,
            onChange: e => onSearch(e.target.value)
          }, searchProps))
        }));
      });

      _defineProperty(this, "createRenderableFavorites", () => {
        const {
          items,
          isGrouped,
          favorites
        } = this.props;

        if (isGrouped) {
          const favoriteItems = [];
          items.forEach(group => group.props.children.filter(item => favorites.includes(item.props.id)).map(item => favoriteItems.push(React.cloneElement(item, {
            isFavorite: true,
            enterTriggersArrowDown: true
          }))));
          return favoriteItems;
        }

        return items.filter(item => favorites.includes(item.props.id)).map(item => React.cloneElement(item, {
          isFavorite: true,
          enterTriggersArrowDown: true
        }));
      });

      _defineProperty(this, "extendItemsWithFavorite", () => {
        const {
          items,
          isGrouped,
          favorites
        } = this.props;

        if (isGrouped) {
          return items.map(group => React.cloneElement(group, {
            children: React.Children.map(group.props.children, item => {
              if (item.type === _ApplicationLauncherSeparator.ApplicationLauncherSeparator) {
                return item;
              }

              return React.cloneElement(item, {
                isFavorite: favorites.some(favoriteId => favoriteId === item.props.id)
              });
            })
          }));
        }

        return items.map(item => React.cloneElement(item, {
          isFavorite: favorites.some(favoriteId => favoriteId === item.props.id)
        }));
      });
    }

    render() {
      const _this$props = this.props,
            {
        'aria-label': ariaLabel,
        isOpen,
        onToggle,
        toggleIcon,
        toggleId,
        onSelect,
        isDisabled,
        className,
        isGrouped,
        dropdownItems,
        favorites,
        onFavorite,
        onSearch,
        items,

        /* eslint-disable @typescript-eslint/no-unused-vars */
        searchPlaceholderText,
        searchProps,
        ref,

        /* eslint-enable @typescript-eslint/no-unused-vars */
        favoritesLabel,
        searchNoResultsText
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["aria-label", "isOpen", "onToggle", "toggleIcon", "toggleId", "onSelect", "isDisabled", "className", "isGrouped", "dropdownItems", "favorites", "onFavorite", "onSearch", "items", "searchPlaceholderText", "searchProps", "ref", "favoritesLabel", "searchNoResultsText"]);

      let renderableItems = [];

      if (onFavorite) {
        let favoritesGroup = [];
        let renderableFavorites = [];

        if (favorites.length > 0) {
          renderableFavorites = this.createRenderableFavorites();
          favoritesGroup = [React.createElement(_ApplicationLauncherGroup.ApplicationLauncherGroup, {
            key: "favorites",
            label: favoritesLabel
          }, renderableFavorites, React.createElement(_ApplicationLauncherSeparator.ApplicationLauncherSeparator, {
            key: "separator"
          }))];
        }

        if (renderableFavorites.length > 0) {
          renderableItems = favoritesGroup.concat(this.extendItemsWithFavorite());
        } else {
          renderableItems = this.extendItemsWithFavorite();
        }
      } else {
        renderableItems = items;
      }

      if (items.length === 0 && dropdownItems.length === 0) {
        renderableItems = [React.createElement(_ApplicationLauncherGroup.ApplicationLauncherGroup, {
          key: "no-results-group"
        }, React.createElement(_ApplicationLauncherItem.ApplicationLauncherItem, {
          key: "no-results"
        }, searchNoResultsText))];
      }

      if (onSearch) {
        renderableItems = [this.createSearchBox(), ...renderableItems];
      }

      return React.createElement(_ApplicationLauncherContext.ApplicationLauncherContext.Provider, {
        value: {
          onFavorite
        }
      }, React.createElement(_Dropdown.DropdownContext.Provider, {
        value: {
          onSelect,
          menuClass: _appLauncher2.default.appLauncherMenu,
          itemClass: _appLauncher2.default.appLauncherMenuItem,
          toggleClass: _appLauncher2.default.appLauncherToggle,
          baseClass: _appLauncher2.default.appLauncher,
          baseComponent: 'nav',
          sectionClass: _appLauncher2.default.appLauncherGroup,
          sectionTitleClass: _appLauncher2.default.appLauncherGroupTitle,
          sectionComponent: 'section',
          disabledClass: _appLauncher2.default.modifiers.disabled,
          hoverClass: _appLauncher2.default.modifiers.hover,
          separatorClass: _appLauncher2.default.appLauncherSeparator
        }
      }, React.createElement(_DropdownWithContext.DropdownWithContext, _extends({}, props, {
        dropdownItems: renderableItems.length ? renderableItems : dropdownItems,
        isOpen: isOpen,
        className: className,
        "aria-label": ariaLabel,
        toggle: React.createElement(_Dropdown.DropdownToggle, {
          id: toggleId,
          iconComponent: null,
          isOpen: isOpen,
          onToggle: onToggle,
          isDisabled: isDisabled,
          "aria-label": ariaLabel
        }, toggleIcon),
        isGrouped: isGrouped
      }))));
    }

  }

  exports.ApplicationLauncher = ApplicationLauncher;

  _defineProperty(ApplicationLauncher, "propTypes", {
    className: _propTypes2.default.string,
    direction: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['up']), _propTypes2.default.oneOf(['down'])]),
    dropdownItems: _propTypes2.default.arrayOf(_propTypes2.default.node),
    items: _propTypes2.default.arrayOf(_propTypes2.default.node),
    isDisabled: _propTypes2.default.bool,
    isOpen: _propTypes2.default.bool,
    position: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['right']), _propTypes2.default.oneOf(['left'])]),
    onSelect: _propTypes2.default.func,
    onToggle: _propTypes2.default.func,
    'aria-label': _propTypes2.default.string,
    isGrouped: _propTypes2.default.bool,
    toggleIcon: _propTypes2.default.node,
    favorites: _propTypes2.default.arrayOf(_propTypes2.default.string),
    onFavorite: _propTypes2.default.func,
    onSearch: _propTypes2.default.func,
    searchPlaceholderText: _propTypes2.default.string,
    searchNoResultsText: _propTypes2.default.string,
    searchProps: _propTypes2.default.any,
    favoritesLabel: _propTypes2.default.string,
    toggleId: _propTypes2.default.string
  });

  _defineProperty(ApplicationLauncher, "defaultProps", {
    className: '',
    isDisabled: false,
    direction: _Dropdown.DropdownDirection.down,
    dropdownItems: [],
    favorites: [],
    items: [],
    isOpen: false,
    position: _Dropdown.DropdownPosition.left,

    /* eslint-disable @typescript-eslint/no-unused-vars */
    onSelect: _event => undefined,
    onToggle: _value => undefined,

    /* eslint-enable @typescript-eslint/no-unused-vars */
    'aria-label': 'Application launcher',
    isGrouped: false,
    toggleIcon: React.createElement(_thIcon2.default, null),
    searchPlaceholderText: 'Filter by name...',
    searchNoResultsText: 'No results found',
    favoritesLabel: 'Favorites'
  });
});
//# sourceMappingURL=ApplicationLauncher.js.map