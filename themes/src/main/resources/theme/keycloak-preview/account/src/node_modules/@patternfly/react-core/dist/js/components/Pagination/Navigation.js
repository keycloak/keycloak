"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Navigation = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _pagination = _interopRequireDefault(require("@patternfly/react-styles/css/components/Pagination/pagination"));

var _reactStyles = require("@patternfly/react-styles");

var _angleLeftIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-left-icon"));

var _angleDoubleLeftIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-double-left-icon"));

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

var _angleDoubleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-double-right-icon"));

var _Button = require("../Button");

var _helpers = require("../../helpers");

var _constants = require("../../helpers/constants");

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

var Navigation =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Navigation, _React$Component);

  function Navigation(props) {
    var _this;

    _classCallCheck(this, Navigation);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Navigation).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "handleNewPage", function (_evt, newPage) {
      var _this$props = _this.props,
          perPage = _this$props.perPage,
          onSetPage = _this$props.onSetPage;
      var startIdx = (newPage - 1) * perPage;
      var endIdx = newPage * perPage;
      return onSetPage(_evt, newPage, perPage, startIdx, endIdx);
    });

    _this.state = {
      userInputPage: _this.props.page
    };
    return _this;
  }

  _createClass(Navigation, [{
    key: "onChange",
    value: function onChange(event, lastPage) {
      var inputPage = Navigation.parseInteger(event.target.value, lastPage);
      this.setState({
        userInputPage: Number.isNaN(inputPage) ? event.target.value : inputPage
      });
    }
  }, {
    key: "onKeyDown",
    value: function onKeyDown(event, page, lastPage, onPageInput) {
      if (event.keyCode === _constants.KEY_CODES.ENTER) {
        var inputPage = Navigation.parseInteger(this.state.userInputPage, lastPage);
        onPageInput(event, Number.isNaN(inputPage) ? page : inputPage);
        this.handleNewPage(event, Number.isNaN(inputPage) ? page : inputPage);
      }
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate(lastState) {
      if (this.props.page !== lastState.page && this.props.page <= this.props.lastPage && this.state.userInputPage !== this.props.page) {
        this.setState({
          userInputPage: this.props.page
        });
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props2 = this.props,
          page = _this$props2.page,
          perPage = _this$props2.perPage,
          onSetPage = _this$props2.onSetPage,
          isDisabled = _this$props2.isDisabled,
          lastPage = _this$props2.lastPage,
          firstPage = _this$props2.firstPage,
          pagesTitle = _this$props2.pagesTitle,
          toLastPage = _this$props2.toLastPage,
          toNextPage = _this$props2.toNextPage,
          toFirstPage = _this$props2.toFirstPage,
          toPreviousPage = _this$props2.toPreviousPage,
          currPage = _this$props2.currPage,
          paginationTitle = _this$props2.paginationTitle,
          onNextClick = _this$props2.onNextClick,
          onPreviousClick = _this$props2.onPreviousClick,
          onFirstClick = _this$props2.onFirstClick,
          onLastClick = _this$props2.onLastClick,
          onPageInput = _this$props2.onPageInput,
          className = _this$props2.className,
          isCompact = _this$props2.isCompact,
          props = _objectWithoutProperties(_this$props2, ["page", "perPage", "onSetPage", "isDisabled", "lastPage", "firstPage", "pagesTitle", "toLastPage", "toNextPage", "toFirstPage", "toPreviousPage", "currPage", "paginationTitle", "onNextClick", "onPreviousClick", "onFirstClick", "onLastClick", "onPageInput", "className", "isCompact"]);

      var userInputPage = this.state.userInputPage;
      return React.createElement("nav", _extends({
        className: (0, _reactStyles.css)(_pagination["default"].paginationNav, className),
        "aria-label": paginationTitle
      }, props), !isCompact && React.createElement(_Button.Button, {
        variant: _Button.ButtonVariant.plain,
        isDisabled: isDisabled || page === firstPage || page === 0,
        "aria-label": toFirstPage,
        "data-action": "first",
        onClick: function onClick(event) {
          onFirstClick(event, 1);

          _this2.handleNewPage(event, 1);

          _this2.setState({
            userInputPage: 1
          });
        }
      }, React.createElement(_angleDoubleLeftIcon["default"], null)), React.createElement(_Button.Button, {
        variant: _Button.ButtonVariant.plain,
        isDisabled: isDisabled || page === firstPage || page === 0,
        "data-action": "previous",
        onClick: function onClick(event) {
          var newPage = page - 1 >= 1 ? page - 1 : 1;
          onPreviousClick(event, newPage);

          _this2.handleNewPage(event, newPage);

          _this2.setState({
            userInputPage: newPage
          });
        },
        "aria-label": toPreviousPage
      }, React.createElement(_angleLeftIcon["default"], null)), !isCompact && React.createElement("div", {
        className: (0, _reactStyles.css)(_pagination["default"].paginationNavPageSelect)
      }, React.createElement("input", {
        className: (0, _reactStyles.css)(_pagination["default"].formControl),
        "aria-label": currPage,
        type: "number",
        disabled: isDisabled || page === firstPage && page === lastPage || page === 0,
        min: lastPage <= 0 && firstPage <= 0 ? 0 : 1,
        max: lastPage,
        value: userInputPage,
        onKeyDown: function onKeyDown(event) {
          return _this2.onKeyDown(event, page, lastPage, onPageInput);
        },
        onChange: function onChange(event) {
          return _this2.onChange(event, lastPage);
        }
      }), React.createElement("span", {
        "aria-hidden": "true"
      }, "of ", pagesTitle ? (0, _helpers.pluralize)(lastPage, pagesTitle) : lastPage)), React.createElement(_Button.Button, {
        variant: _Button.ButtonVariant.plain,
        isDisabled: isDisabled || page === lastPage,
        "aria-label": toNextPage,
        "data-action": "next",
        onClick: function onClick(event) {
          var newPage = page + 1 <= lastPage ? page + 1 : lastPage;
          onNextClick(event, newPage);

          _this2.handleNewPage(event, newPage);

          _this2.setState({
            userInputPage: newPage
          });
        }
      }, React.createElement(_angleRightIcon["default"], null)), !isCompact && React.createElement(_Button.Button, {
        variant: _Button.ButtonVariant.plain,
        isDisabled: isDisabled || page === lastPage,
        "aria-label": toLastPage,
        "data-action": "last",
        onClick: function onClick(event) {
          onLastClick(event, lastPage);

          _this2.handleNewPage(event, lastPage);

          _this2.setState({
            userInputPage: lastPage
          });
        }
      }, React.createElement(_angleDoubleRightIcon["default"], null)));
    }
  }], [{
    key: "parseInteger",
    value: function parseInteger(input, lastPage) {
      // eslint-disable-next-line radix
      var inputPage = Number.parseInt(input, 10);

      if (!Number.isNaN(inputPage)) {
        inputPage = inputPage > lastPage ? lastPage : inputPage;
        inputPage = inputPage < 1 ? 1 : inputPage;
      }

      return inputPage;
    }
  }]);

  return Navigation;
}(React.Component);

exports.Navigation = Navigation;

_defineProperty(Navigation, "propTypes", {
  className: _propTypes["default"].string,
  isDisabled: _propTypes["default"].bool,
  isCompact: _propTypes["default"].bool,
  lastPage: _propTypes["default"].number,
  firstPage: _propTypes["default"].number,
  pagesTitle: _propTypes["default"].string,
  toLastPage: _propTypes["default"].string,
  toPreviousPage: _propTypes["default"].string,
  toNextPage: _propTypes["default"].string,
  toFirstPage: _propTypes["default"].string,
  currPage: _propTypes["default"].string,
  paginationTitle: _propTypes["default"].string,
  page: _propTypes["default"].node.isRequired,
  perPage: _propTypes["default"].number,
  onSetPage: _propTypes["default"].any.isRequired,
  onNextClick: _propTypes["default"].func,
  onPreviousClick: _propTypes["default"].func,
  onFirstClick: _propTypes["default"].func,
  onLastClick: _propTypes["default"].func,
  onPageInput: _propTypes["default"].func
});

_defineProperty(Navigation, "defaultProps", {
  className: '',
  isDisabled: false,
  isCompact: false,
  lastPage: 0,
  firstPage: 0,
  pagesTitle: '',
  toLastPage: 'Go to last page',
  toNextPage: 'Go to next page',
  toFirstPage: 'Go to first page',
  toPreviousPage: 'Go to previous page',
  currPage: 'Current page',
  paginationTitle: 'Pagination',
  onNextClick: function onNextClick() {
    return undefined;
  },
  onPreviousClick: function onPreviousClick() {
    return undefined;
  },
  onFirstClick: function onFirstClick() {
    return undefined;
  },
  onLastClick: function onLastClick() {
    return undefined;
  },
  onPageInput: function onPageInput() {
    return undefined;
  }
});
//# sourceMappingURL=Navigation.js.map