"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.PaginationOptionsMenu = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _optionsMenu = _interopRequireDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));

var _pagination = _interopRequireDefault(require("@patternfly/react-styles/css/components/Pagination/pagination"));

var _reactStyles = require("@patternfly/react-styles");

var _Dropdown = require("../Dropdown");

var _checkIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/check-icon"));

var _OptionsToggle = require("./OptionsToggle");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var PaginationOptionsMenu =
/*#__PURE__*/
function (_React$Component) {
  _inherits(PaginationOptionsMenu, _React$Component);

  function PaginationOptionsMenu(props) {
    var _this;

    _classCallCheck(this, PaginationOptionsMenu);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(PaginationOptionsMenu).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "parentRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "onToggle", function (isOpen) {
      _this.setState({
        isOpen: isOpen
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onSelect", function () {
      _this.setState(function (prevState) {
        return {
          isOpen: !prevState.isOpen
        };
      });
    });

    _defineProperty(_assertThisInitialized(_this), "handleNewPerPage", function (_evt, newPerPage) {
      var _this$props = _this.props,
          page = _this$props.page,
          onPerPageSelect = _this$props.onPerPageSelect,
          itemCount = _this$props.itemCount,
          defaultToFullPage = _this$props.defaultToFullPage;
      var newPage = page;

      while (Math.ceil(itemCount / newPerPage) < newPage) {
        newPage--;
      }

      if (defaultToFullPage) {
        if (itemCount / newPerPage !== newPage) {
          while (newPage > 1 && itemCount - newPerPage * newPage < 0) {
            newPage--;
          }
        }
      }

      var startIdx = (newPage - 1) * newPerPage;
      var endIdx = newPage * newPerPage;
      return onPerPageSelect(_evt, newPerPage, newPage, startIdx, endIdx);
    });

    _defineProperty(_assertThisInitialized(_this), "renderItems", function () {
      var _this$props2 = _this.props,
          perPageOptions = _this$props2.perPageOptions,
          perPage = _this$props2.perPage,
          perPageSuffix = _this$props2.perPageSuffix;
      return perPageOptions.map(function (_ref) {
        var value = _ref.value,
            title = _ref.title;
        return React.createElement(_Dropdown.DropdownItem, {
          key: value,
          component: "button",
          "data-action": "per-page-".concat(value),
          className: (0, _reactStyles.css)(perPage === value && 'pf-m-selected'),
          onClick: function onClick(event) {
            return _this.handleNewPerPage(event, value);
          }
        }, title, React.createElement("span", {
          className: (0, _reactStyles.css)(_pagination["default"].paginationMenuText)
        }, " ".concat(perPageSuffix)), perPage === value && React.createElement("i", {
          className: (0, _reactStyles.css)(_optionsMenu["default"].optionsMenuMenuItemIcon)
        }, React.createElement(_checkIcon["default"], null)));
      });
    });

    _this.state = {
      isOpen: false
    };
    return _this;
  }

  _createClass(PaginationOptionsMenu, [{
    key: "render",
    value: function render() {
      var _this$props3 = this.props,
          widgetId = _this$props3.widgetId,
          isDisabled = _this$props3.isDisabled,
          itemsPerPageTitle = _this$props3.itemsPerPageTitle,
          dropDirection = _this$props3.dropDirection,
          optionsToggle = _this$props3.optionsToggle,
          perPageOptions = _this$props3.perPageOptions,
          toggleTemplate = _this$props3.toggleTemplate,
          firstIndex = _this$props3.firstIndex,
          lastIndex = _this$props3.lastIndex,
          itemCount = _this$props3.itemCount,
          itemsTitle = _this$props3.itemsTitle;
      var isOpen = this.state.isOpen;
      return React.createElement(_Dropdown.DropdownContext.Provider, {
        value: {
          id: widgetId,
          onSelect: this.onSelect,
          toggleIconClass: _optionsMenu["default"].optionsMenuToggleIcon,
          toggleTextClass: _optionsMenu["default"].optionsMenuToggleText,
          menuClass: _optionsMenu["default"].optionsMenuMenu,
          itemClass: _optionsMenu["default"].optionsMenuMenuItem,
          toggleClass: ' ',
          baseClass: _optionsMenu["default"].optionsMenu,
          disabledClass: _optionsMenu["default"].modifiers.disabled,
          menuComponent: 'ul',
          baseComponent: 'div'
        }
      }, React.createElement(_Dropdown.DropdownWithContext, {
        direction: dropDirection,
        isOpen: isOpen,
        toggle: React.createElement(_OptionsToggle.OptionsToggle, {
          optionsToggle: optionsToggle,
          itemsPerPageTitle: itemsPerPageTitle,
          showToggle: perPageOptions && perPageOptions.length > 0,
          onToggle: this.onToggle,
          isOpen: isOpen,
          widgetId: widgetId,
          firstIndex: firstIndex,
          lastIndex: lastIndex,
          itemCount: itemCount,
          itemsTitle: itemsTitle,
          toggleTemplate: toggleTemplate,
          parentRef: this.parentRef.current,
          isDisabled: isDisabled
        }),
        dropdownItems: this.renderItems(),
        isPlain: true
      }));
    }
  }]);

  return PaginationOptionsMenu;
}(React.Component);

exports.PaginationOptionsMenu = PaginationOptionsMenu;

_defineProperty(PaginationOptionsMenu, "propTypes", {
  className: _propTypes["default"].string,
  widgetId: _propTypes["default"].string,
  isDisabled: _propTypes["default"].bool,
  dropDirection: _propTypes["default"].oneOf(['up', 'down']),
  perPageOptions: _propTypes["default"].arrayOf(_propTypes["default"].any),
  itemsPerPageTitle: _propTypes["default"].string,
  page: _propTypes["default"].number,
  perPageSuffix: _propTypes["default"].string,
  itemsTitle: _propTypes["default"].string,
  optionsToggle: _propTypes["default"].string,
  itemCount: _propTypes["default"].number,
  firstIndex: _propTypes["default"].number,
  lastIndex: _propTypes["default"].number,
  defaultToFullPage: _propTypes["default"].bool,
  perPage: _propTypes["default"].number,
  lastPage: _propTypes["default"].number,
  toggleTemplate: _propTypes["default"].oneOfType([_propTypes["default"].func, _propTypes["default"].string]),
  onPerPageSelect: _propTypes["default"].any
});

_defineProperty(PaginationOptionsMenu, "defaultProps", {
  className: '',
  widgetId: '',
  isDisabled: false,
  dropDirection: _Dropdown.DropdownDirection.down,
  perPageOptions: [],
  itemsPerPageTitle: 'Items per page',
  perPageSuffix: 'per page',
  optionsToggle: 'Select',
  perPage: 0,
  firstIndex: 0,
  lastIndex: 0,
  defaultToFullPage: false,
  itemCount: 0,
  itemsTitle: 'items',
  toggleTemplate: function toggleTemplate(_ref2) {
    var firstIndex = _ref2.firstIndex,
        lastIndex = _ref2.lastIndex,
        itemCount = _ref2.itemCount,
        itemsTitle = _ref2.itemsTitle;
    return React.createElement(React.Fragment, null, React.createElement("b", null, firstIndex, " - ", lastIndex), ' ', "of", React.createElement("b", null, itemCount), " ", itemsTitle);
  },
  onPerPageSelect: function onPerPageSelect() {
    return null;
  }
});
//# sourceMappingURL=PaginationOptionsMenu.js.map