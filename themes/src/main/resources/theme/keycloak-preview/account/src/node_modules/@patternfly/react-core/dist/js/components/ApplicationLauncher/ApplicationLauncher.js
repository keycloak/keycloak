"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ApplicationLauncher = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _appLauncher = _interopRequireDefault(require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"));

var _formControl = _interopRequireDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));

var _thIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/th-icon"));

var _Dropdown = require("../Dropdown");

var _DropdownWithContext = require("../Dropdown/DropdownWithContext");

var _ApplicationLauncherGroup = require("./ApplicationLauncherGroup");

var _ApplicationLauncherSeparator = require("./ApplicationLauncherSeparator");

var _ApplicationLauncherItem = require("./ApplicationLauncherItem");

var _ApplicationLauncherContext = require("./ApplicationLauncherContext");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance"); }

function _iterableToArray(iter) { if (Symbol.iterator in Object(iter) || Object.prototype.toString.call(iter) === "[object Arguments]") return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = new Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var ApplicationLauncher =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ApplicationLauncher, _React$Component);

  function ApplicationLauncher() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, ApplicationLauncher);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(ApplicationLauncher)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "createSearchBox", function () {
      var _this$props = _this.props,
          onSearch = _this$props.onSearch,
          searchPlaceholderText = _this$props.searchPlaceholderText,
          searchProps = _this$props.searchProps;
      return React.createElement("div", {
        key: "search",
        className: (0, _reactStyles.css)(_appLauncher["default"].appLauncherMenuSearch)
      }, React.createElement(_ApplicationLauncherItem.ApplicationLauncherItem, {
        customChild: React.createElement("input", _extends({
          type: "search",
          className: (0, _reactStyles.css)(_formControl["default"].formControl),
          placeholder: searchPlaceholderText,
          onChange: function onChange(e) {
            return onSearch(e.target.value);
          }
        }, searchProps))
      }));
    });

    _defineProperty(_assertThisInitialized(_this), "createRenderableFavorites", function () {
      var _this$props2 = _this.props,
          items = _this$props2.items,
          isGrouped = _this$props2.isGrouped,
          favorites = _this$props2.favorites;

      if (isGrouped) {
        var favoriteItems = [];
        items.forEach(function (group) {
          return group.props.children.filter(function (item) {
            return favorites.includes(item.props.id);
          }).map(function (item) {
            return favoriteItems.push(React.cloneElement(item, {
              isFavorite: true,
              enterTriggersArrowDown: true
            }));
          });
        });
        return favoriteItems;
      }

      return items.filter(function (item) {
        return favorites.includes(item.props.id);
      }).map(function (item) {
        return React.cloneElement(item, {
          isFavorite: true,
          enterTriggersArrowDown: true
        });
      });
    });

    _defineProperty(_assertThisInitialized(_this), "extendItemsWithFavorite", function () {
      var _this$props3 = _this.props,
          items = _this$props3.items,
          isGrouped = _this$props3.isGrouped,
          favorites = _this$props3.favorites;

      if (isGrouped) {
        return items.map(function (group) {
          return React.cloneElement(group, {
            children: React.Children.map(group.props.children, function (item) {
              if (item.type === _ApplicationLauncherSeparator.ApplicationLauncherSeparator) {
                return item;
              }

              return React.cloneElement(item, {
                isFavorite: favorites.some(function (favoriteId) {
                  return favoriteId === item.props.id;
                })
              });
            })
          });
        });
      }

      return items.map(function (item) {
        return React.cloneElement(item, {
          isFavorite: favorites.some(function (favoriteId) {
            return favoriteId === item.props.id;
          })
        });
      });
    });

    return _this;
  }

  _createClass(ApplicationLauncher, [{
    key: "render",
    value: function render() {
      var _this$props4 = this.props,
          ariaLabel = _this$props4['aria-label'],
          isOpen = _this$props4.isOpen,
          onToggle = _this$props4.onToggle,
          toggleIcon = _this$props4.toggleIcon,
          toggleId = _this$props4.toggleId,
          onSelect = _this$props4.onSelect,
          isDisabled = _this$props4.isDisabled,
          className = _this$props4.className,
          isGrouped = _this$props4.isGrouped,
          dropdownItems = _this$props4.dropdownItems,
          favorites = _this$props4.favorites,
          onFavorite = _this$props4.onFavorite,
          onSearch = _this$props4.onSearch,
          items = _this$props4.items,
          searchPlaceholderText = _this$props4.searchPlaceholderText,
          searchProps = _this$props4.searchProps,
          ref = _this$props4.ref,
          favoritesLabel = _this$props4.favoritesLabel,
          searchNoResultsText = _this$props4.searchNoResultsText,
          props = _objectWithoutProperties(_this$props4, ["aria-label", "isOpen", "onToggle", "toggleIcon", "toggleId", "onSelect", "isDisabled", "className", "isGrouped", "dropdownItems", "favorites", "onFavorite", "onSearch", "items", "searchPlaceholderText", "searchProps", "ref", "favoritesLabel", "searchNoResultsText"]);

      var renderableItems = [];

      if (onFavorite) {
        var favoritesGroup = [];
        var renderableFavorites = [];

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
        renderableItems = [this.createSearchBox()].concat(_toConsumableArray(renderableItems));
      }

      return React.createElement(_ApplicationLauncherContext.ApplicationLauncherContext.Provider, {
        value: {
          onFavorite: onFavorite
        }
      }, React.createElement(_Dropdown.DropdownContext.Provider, {
        value: {
          onSelect: onSelect,
          menuClass: _appLauncher["default"].appLauncherMenu,
          itemClass: _appLauncher["default"].appLauncherMenuItem,
          toggleClass: _appLauncher["default"].appLauncherToggle,
          baseClass: _appLauncher["default"].appLauncher,
          baseComponent: 'nav',
          sectionClass: _appLauncher["default"].appLauncherGroup,
          sectionTitleClass: _appLauncher["default"].appLauncherGroupTitle,
          sectionComponent: 'section',
          disabledClass: _appLauncher["default"].modifiers.disabled,
          hoverClass: _appLauncher["default"].modifiers.hover,
          separatorClass: _appLauncher["default"].appLauncherSeparator
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
  }]);

  return ApplicationLauncher;
}(React.Component);

exports.ApplicationLauncher = ApplicationLauncher;

_defineProperty(ApplicationLauncher, "propTypes", {
  className: _propTypes["default"].string,
  direction: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['up']), _propTypes["default"].oneOf(['down'])]),
  dropdownItems: _propTypes["default"].arrayOf(_propTypes["default"].node),
  items: _propTypes["default"].arrayOf(_propTypes["default"].node),
  isDisabled: _propTypes["default"].bool,
  isOpen: _propTypes["default"].bool,
  position: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['right']), _propTypes["default"].oneOf(['left'])]),
  onSelect: _propTypes["default"].func,
  onToggle: _propTypes["default"].func,
  'aria-label': _propTypes["default"].string,
  isGrouped: _propTypes["default"].bool,
  toggleIcon: _propTypes["default"].node,
  favorites: _propTypes["default"].arrayOf(_propTypes["default"].string),
  onFavorite: _propTypes["default"].func,
  onSearch: _propTypes["default"].func,
  searchPlaceholderText: _propTypes["default"].string,
  searchNoResultsText: _propTypes["default"].string,
  searchProps: _propTypes["default"].any,
  favoritesLabel: _propTypes["default"].string,
  toggleId: _propTypes["default"].string
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
  onSelect: function onSelect(_event) {
    return undefined;
  },
  onToggle: function onToggle(_value) {
    return undefined;
  },

  /* eslint-enable @typescript-eslint/no-unused-vars */
  'aria-label': 'Application launcher',
  isGrouped: false,
  toggleIcon: React.createElement(_thIcon["default"], null),
  searchPlaceholderText: 'Filter by name...',
  searchNoResultsText: 'No results found',
  favoritesLabel: 'Favorites'
});
//# sourceMappingURL=ApplicationLauncher.js.map