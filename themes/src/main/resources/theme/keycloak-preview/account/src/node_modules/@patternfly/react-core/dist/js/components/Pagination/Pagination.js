"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Pagination = exports.PaginationVariant = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _Dropdown = require("../Dropdown");

var _ToggleTemplate = require("./ToggleTemplate");

var _pagination = _interopRequireDefault(require("@patternfly/react-styles/css/components/Pagination/pagination"));

var _reactStyles = require("@patternfly/react-styles");

var _Navigation = require("./Navigation");

var _PaginationOptionsMenu = require("./PaginationOptionsMenu");

var _withOuia = require("../withOuia");

var _reactTokens = require("@patternfly/react-tokens");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var PaginationVariant;
exports.PaginationVariant = PaginationVariant;

(function (PaginationVariant) {
  PaginationVariant["top"] = "top";
  PaginationVariant["bottom"] = "bottom";
  PaginationVariant["left"] = "left";
  PaginationVariant["right"] = "right";
})(PaginationVariant || (exports.PaginationVariant = PaginationVariant = {}));

var defaultPerPageOptions = [{
  title: '10',
  value: 10
}, {
  title: '20',
  value: 20
}, {
  title: '50',
  value: 50
}, {
  title: '100',
  value: 100
}];

var handleInputWidth = function handleInputWidth(lastPage, node) {
  if (!node) {
    return;
  }

  var len = String(lastPage).length;

  if (len >= 3) {
    node.style.setProperty(_reactTokens.c_pagination__nav_page_select_c_form_control_width_chars.name, "".concat(len));
  } else {
    node.style.setProperty(_reactTokens.c_pagination__nav_page_select_c_form_control_width_chars.name, '2');
  }
};

var paginationId = 0;

var Pagination =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Pagination, _React$Component);

  function Pagination() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, Pagination);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Pagination)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "paginationRef", React.createRef());

    return _this;
  }

  _createClass(Pagination, [{
    key: "getLastPage",
    value: function getLastPage() {
      var _this$props = this.props,
          itemCount = _this$props.itemCount,
          perPage = _this$props.perPage;
      return Math.ceil(itemCount / perPage) || 0;
    }
  }, {
    key: "componentDidMount",
    value: function componentDidMount() {
      var node = this.paginationRef.current;
      handleInputWidth(this.getLastPage(), node);
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate(prevProps) {
      var node = this.paginationRef.current;

      if (prevProps.perPage !== this.props.perPage || prevProps.itemCount !== this.props.itemCount) {
        handleInputWidth(this.getLastPage(), node);
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props2 = this.props,
          children = _this$props2.children,
          className = _this$props2.className,
          variant = _this$props2.variant,
          isDisabled = _this$props2.isDisabled,
          isCompact = _this$props2.isCompact,
          perPage = _this$props2.perPage,
          titles = _this$props2.titles,
          firstPage = _this$props2.firstPage,
          propPage = _this$props2.page,
          offset = _this$props2.offset,
          defaultToFullPage = _this$props2.defaultToFullPage,
          itemCount = _this$props2.itemCount,
          itemsStart = _this$props2.itemsStart,
          itemsEnd = _this$props2.itemsEnd,
          perPageOptions = _this$props2.perPageOptions,
          dropDirection = _this$props2.dropDirection,
          widgetId = _this$props2.widgetId,
          toggleTemplate = _this$props2.toggleTemplate,
          onSetPage = _this$props2.onSetPage,
          onPerPageSelect = _this$props2.onPerPageSelect,
          onFirstClick = _this$props2.onFirstClick,
          onPreviousClick = _this$props2.onPreviousClick,
          onNextClick = _this$props2.onNextClick,
          onPageInput = _this$props2.onPageInput,
          onLastClick = _this$props2.onLastClick,
          ouiaContext = _this$props2.ouiaContext,
          ouiaId = _this$props2.ouiaId,
          props = _objectWithoutProperties(_this$props2, ["children", "className", "variant", "isDisabled", "isCompact", "perPage", "titles", "firstPage", "page", "offset", "defaultToFullPage", "itemCount", "itemsStart", "itemsEnd", "perPageOptions", "dropDirection", "widgetId", "toggleTemplate", "onSetPage", "onPerPageSelect", "onFirstClick", "onPreviousClick", "onNextClick", "onPageInput", "onLastClick", "ouiaContext", "ouiaId"]);

      var page = propPage;

      if (!page && offset) {
        page = Math.ceil(offset / perPage);
      }

      var lastPage = this.getLastPage();

      if (page < firstPage && itemCount > 0) {
        page = firstPage;
      } else if (page > lastPage) {
        page = lastPage;
      }

      var firstIndex = itemCount <= 0 ? 0 : (page - 1) * perPage + 1;
      var lastIndex;

      if (itemCount <= 0) {
        lastIndex = 0;
      } else {
        lastIndex = page === lastPage ? itemCount : page * perPage;
      }

      return React.createElement("div", _extends({
        ref: this.paginationRef,
        className: (0, _reactStyles.css)(_pagination["default"].pagination, variant === PaginationVariant.bottom && _pagination["default"].modifiers.footer, isCompact && _pagination["default"].modifiers.compact, className),
        id: "".concat(widgetId, "-").concat(paginationId++)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Pagination',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }, props), variant === PaginationVariant.top && React.createElement("div", {
        className: (0, _reactStyles.css)(_pagination["default"].paginationTotalItems)
      }, React.createElement(_ToggleTemplate.ToggleTemplate, {
        firstIndex: firstIndex,
        lastIndex: lastIndex,
        itemCount: itemCount,
        itemsTitle: titles.items
      })), React.createElement(_PaginationOptionsMenu.PaginationOptionsMenu, {
        itemsPerPageTitle: titles.itemsPerPage,
        perPageSuffix: titles.perPageSuffix,
        itemsTitle: isCompact ? '' : titles.items,
        optionsToggle: titles.optionsToggle,
        perPageOptions: perPageOptions,
        firstIndex: itemsStart !== null ? itemsStart : firstIndex,
        lastIndex: itemsEnd !== null ? itemsEnd : lastIndex,
        defaultToFullPage: defaultToFullPage,
        itemCount: itemCount,
        page: page,
        perPage: perPage,
        lastPage: lastPage,
        onPerPageSelect: onPerPageSelect,
        dropDirection: dropDirection,
        widgetId: widgetId,
        toggleTemplate: toggleTemplate,
        isDisabled: isDisabled
      }), React.createElement(_Navigation.Navigation, {
        pagesTitle: titles.page,
        toLastPage: titles.toLastPage,
        toPreviousPage: titles.toPreviousPage,
        toNextPage: titles.toNextPage,
        toFirstPage: titles.toFirstPage,
        currPage: titles.currPage,
        paginationTitle: titles.paginationTitle,
        page: itemCount <= 0 ? 0 : page,
        perPage: perPage,
        firstPage: itemsStart !== null ? itemsStart : 1,
        lastPage: lastPage,
        onSetPage: onSetPage,
        onFirstClick: onFirstClick,
        onPreviousClick: onPreviousClick,
        onNextClick: onNextClick,
        onLastClick: onLastClick,
        onPageInput: onPageInput,
        isDisabled: isDisabled,
        isCompact: isCompact
      }), children);
    }
  }]);

  return Pagination;
}(React.Component);

_defineProperty(Pagination, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  itemCount: _propTypes["default"].number.isRequired,
  variant: _propTypes["default"].oneOf(['top', 'bottom', 'left', 'right']),
  isDisabled: _propTypes["default"].bool,
  isCompact: _propTypes["default"].bool,
  perPage: _propTypes["default"].number,
  perPageOptions: _propTypes["default"].arrayOf(_propTypes["default"].shape({
    title: _propTypes["default"].string,
    value: _propTypes["default"].number
  })),
  defaultToFullPage: _propTypes["default"].bool,
  firstPage: _propTypes["default"].number,
  page: _propTypes["default"].number,
  offset: _propTypes["default"].number,
  itemsStart: _propTypes["default"].number,
  itemsEnd: _propTypes["default"].number,
  widgetId: _propTypes["default"].string,
  dropDirection: _propTypes["default"].oneOf(['up', 'down']),
  titles: _propTypes["default"].shape({
    page: _propTypes["default"].string,
    items: _propTypes["default"].string,
    itemsPerPage: _propTypes["default"].string,
    perPageSuffix: _propTypes["default"].string,
    toFirstPage: _propTypes["default"].string,
    toPreviousPage: _propTypes["default"].string,
    toLastPage: _propTypes["default"].string,
    toNextPage: _propTypes["default"].string,
    optionsToggle: _propTypes["default"].string,
    currPage: _propTypes["default"].string,
    paginationTitle: _propTypes["default"].string
  }),
  toggleTemplate: _propTypes["default"].oneOfType([_propTypes["default"].func, _propTypes["default"].string]),
  onSetPage: _propTypes["default"].func,
  onFirstClick: _propTypes["default"].func,
  onPreviousClick: _propTypes["default"].func,
  onNextClick: _propTypes["default"].func,
  onLastClick: _propTypes["default"].func,
  onPageInput: _propTypes["default"].func,
  onPerPageSelect: _propTypes["default"].func
});

_defineProperty(Pagination, "defaultProps", {
  children: null,
  className: '',
  variant: PaginationVariant.top,
  isDisabled: false,
  isCompact: false,
  perPage: defaultPerPageOptions[0].value,
  titles: {
    items: '',
    page: '',
    itemsPerPage: 'Items per page',
    perPageSuffix: 'per page',
    toFirstPage: 'Go to first page',
    toPreviousPage: 'Go to previous page',
    toLastPage: 'Go to last page',
    toNextPage: 'Go to next page',
    optionsToggle: 'Items per page',
    currPage: 'Current page',
    paginationTitle: 'Pagination'
  },
  firstPage: 1,
  page: 0,
  offset: 0,
  defaultToFullPage: false,
  itemsStart: null,
  itemsEnd: null,
  perPageOptions: defaultPerPageOptions,
  dropDirection: _Dropdown.DropdownDirection.down,
  widgetId: 'pagination-options-menu',
  toggleTemplate: _ToggleTemplate.ToggleTemplate,
  onSetPage: function onSetPage() {
    return undefined;
  },
  onPerPageSelect: function onPerPageSelect() {
    return undefined;
  },
  onFirstClick: function onFirstClick() {
    return undefined;
  },
  onPreviousClick: function onPreviousClick() {
    return undefined;
  },
  onNextClick: function onNextClick() {
    return undefined;
  },
  onPageInput: function onPageInput() {
    return undefined;
  },
  onLastClick: function onLastClick() {
    return undefined;
  },
  ouiaContext: null,
  ouiaId: null
});

var PaginationWithOuiaContext = (0, _withOuia.withOuiaContext)(Pagination);
exports.Pagination = PaginationWithOuiaContext;
//# sourceMappingURL=Pagination.js.map