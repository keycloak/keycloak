"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataToolbar = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _dataToolbar = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"));

var _reactStyles = require("@patternfly/react-styles");

var _DataToolbarUtils = require("./DataToolbarUtils");

var _DataToolbarChipGroupContent = require("./DataToolbarChipGroupContent");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var DataToolbar =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DataToolbar, _React$Component);

  function DataToolbar(props) {
    var _this;

    _classCallCheck(this, DataToolbar);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(DataToolbar).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "chipGroupContentRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "isToggleManaged", function () {
      return !(_this.props.isExpanded || !!_this.props.toggleIsExpanded);
    });

    _defineProperty(_assertThisInitialized(_this), "toggleIsExpanded", function () {
      _this.setState(function (prevState) {
        return {
          isManagedToggleExpanded: !prevState.isManagedToggleExpanded
        };
      });
    });

    _defineProperty(_assertThisInitialized(_this), "closeExpandableContent", function () {
      _this.setState(function () {
        return {
          isManagedToggleExpanded: false
        };
      });
    });

    _defineProperty(_assertThisInitialized(_this), "updateNumberFilters", function (categoryName, numberOfFilters) {
      var filterInfoToUpdate = _objectSpread({}, _this.state.filterInfo);

      if (!filterInfoToUpdate.hasOwnProperty(categoryName) || filterInfoToUpdate[categoryName] !== numberOfFilters) {
        filterInfoToUpdate[categoryName] = numberOfFilters;

        _this.setState({
          filterInfo: filterInfoToUpdate
        });
      }
    });

    _defineProperty(_assertThisInitialized(_this), "getNumberOfFilters", function () {
      return Object.values(_this.state.filterInfo).reduce(function (acc, cur) {
        return acc + cur;
      }, 0);
    });

    _this.state = {
      isManagedToggleExpanded: false,
      filterInfo: {}
    };
    return _this;
  }

  _createClass(DataToolbar, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      if (this.isToggleManaged()) {
        window.addEventListener('resize', this.closeExpandableContent);
      }

      if (process.env.NODE_ENV !== 'production' && !DataToolbar.hasWarnBeta) {
        // eslint-disable-next-line no-console
        console.warn('You are using a beta component (DataToolbar). These api parts are subject to change in the future.');
        DataToolbar.hasWarnBeta = true;
      }
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      if (this.isToggleManaged()) {
        window.removeEventListener('resize', this.closeExpandableContent);
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props = this.props,
          clearAllFilters = _this$props.clearAllFilters,
          clearFiltersButtonText = _this$props.clearFiltersButtonText,
          collapseListedFiltersBreakpoint = _this$props.collapseListedFiltersBreakpoint,
          isExpanded = _this$props.isExpanded,
          toggleIsExpanded = _this$props.toggleIsExpanded,
          className = _this$props.className,
          children = _this$props.children,
          id = _this$props.id,
          props = _objectWithoutProperties(_this$props, ["clearAllFilters", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "isExpanded", "toggleIsExpanded", "className", "children", "id"]);

      var isManagedToggleExpanded = this.state.isManagedToggleExpanded;
      var isToggleManaged = this.isToggleManaged();
      var numberOfFilters = this.getNumberOfFilters();
      var showClearFiltersButton = numberOfFilters > 0;
      return React.createElement("div", _extends({
        className: (0, _reactStyles.css)(_dataToolbar["default"].dataToolbar, className),
        id: id
      }, props), React.createElement(_DataToolbarUtils.DataToolbarContext.Provider, {
        value: {
          isExpanded: this.isToggleManaged() ? isManagedToggleExpanded : isExpanded,
          toggleIsExpanded: isToggleManaged ? this.toggleIsExpanded : toggleIsExpanded,
          chipGroupContentRef: this.chipGroupContentRef,
          updateNumberFilters: this.updateNumberFilters,
          numberOfFilters: numberOfFilters
        }
      }, React.Children.map(children, function (child) {
        if (React.isValidElement(child)) {
          return React.cloneElement(child, {
            clearAllFilters: clearAllFilters,
            clearFiltersButtonText: clearFiltersButtonText,
            showClearFiltersButton: showClearFiltersButton,
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
  }]);

  return DataToolbar;
}(React.Component);

exports.DataToolbar = DataToolbar;

_defineProperty(DataToolbar, "propTypes", {
  clearAllFilters: _propTypes["default"].func,
  clearFiltersButtonText: _propTypes["default"].string,
  collapseListedFiltersBreakpoint: _propTypes["default"].oneOf(['md', 'lg', 'xl', '2xl']),
  isExpanded: _propTypes["default"].bool,
  toggleIsExpanded: _propTypes["default"].func,
  className: _propTypes["default"].string,
  children: _propTypes["default"].node,
  id: _propTypes["default"].string.isRequired
});

_defineProperty(DataToolbar, "hasWarnBeta", false);
//# sourceMappingURL=DataToolbar.js.map