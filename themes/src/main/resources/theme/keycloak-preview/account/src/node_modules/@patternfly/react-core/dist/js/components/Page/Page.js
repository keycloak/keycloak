"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Page = exports.PageContextConsumer = exports.PageContextProvider = exports.PageLayouts = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _page = _interopRequireDefault(require("@patternfly/react-styles/css/components/Page/page"));

var _reactStyles = require("@patternfly/react-styles");

var _global_breakpoint_md = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/global_breakpoint_md"));

var _util = require("../../helpers/util");

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

var PageLayouts;
exports.PageLayouts = PageLayouts;

(function (PageLayouts) {
  PageLayouts["vertical"] = "vertical";
  PageLayouts["horizontal"] = "horizontal";
})(PageLayouts || (exports.PageLayouts = PageLayouts = {}));

var PageContext = React.createContext({});
var PageContextProvider = PageContext.Provider;
exports.PageContextProvider = PageContextProvider;
var PageContextConsumer = PageContext.Consumer;
exports.PageContextConsumer = PageContextConsumer;

var Page =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Page, _React$Component);

  function Page(props) {
    var _this;

    _classCallCheck(this, Page);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Page).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "handleResize", function () {
      var onPageResize = _this.props.onPageResize;
      var windowSize = window.innerWidth; // eslint-disable-next-line radix

      var mobileView = windowSize < Number.parseInt(_global_breakpoint_md["default"].value, 10);

      if (onPageResize) {
        onPageResize({
          mobileView: mobileView,
          windowSize: windowSize
        });
      } // eslint-disable-next-line @typescript-eslint/no-unused-vars


      _this.setState(function (prevState) {
        return {
          mobileView: mobileView
        };
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onNavToggleMobile", function () {
      _this.setState(function (prevState) {
        return {
          mobileIsNavOpen: !prevState.mobileIsNavOpen
        };
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onNavToggleDesktop", function () {
      _this.setState(function (prevState) {
        return {
          desktopIsNavOpen: !prevState.desktopIsNavOpen
        };
      });
    });

    var isManagedSidebar = props.isManagedSidebar,
        defaultManagedSidebarIsOpen = props.defaultManagedSidebarIsOpen;
    var managedSidebarOpen = !isManagedSidebar ? true : defaultManagedSidebarIsOpen;
    _this.state = {
      desktopIsNavOpen: managedSidebarOpen,
      mobileIsNavOpen: false,
      mobileView: false
    };
    return _this;
  }

  _createClass(Page, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var _this$props = this.props,
          isManagedSidebar = _this$props.isManagedSidebar,
          onPageResize = _this$props.onPageResize;

      if (isManagedSidebar || onPageResize) {
        window.addEventListener('resize', (0, _util.debounce)(this.handleResize, 250)); // Initial check if should be shown

        this.handleResize();
      }
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      var _this$props2 = this.props,
          isManagedSidebar = _this$props2.isManagedSidebar,
          onPageResize = _this$props2.onPageResize;

      if (isManagedSidebar || onPageResize) {
        window.removeEventListener('resize', (0, _util.debounce)(this.handleResize, 250));
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props3 = this.props,
          breadcrumb = _this$props3.breadcrumb,
          className = _this$props3.className,
          children = _this$props3.children,
          header = _this$props3.header,
          sidebar = _this$props3.sidebar,
          skipToContent = _this$props3.skipToContent,
          role = _this$props3.role,
          mainContainerId = _this$props3.mainContainerId,
          isManagedSidebar = _this$props3.isManagedSidebar,
          defaultManagedSidebarIsOpen = _this$props3.defaultManagedSidebarIsOpen,
          onPageResize = _this$props3.onPageResize,
          mainAriaLabel = _this$props3.mainAriaLabel,
          rest = _objectWithoutProperties(_this$props3, ["breadcrumb", "className", "children", "header", "sidebar", "skipToContent", "role", "mainContainerId", "isManagedSidebar", "defaultManagedSidebarIsOpen", "onPageResize", "mainAriaLabel"]);

      var _this$state = this.state,
          mobileView = _this$state.mobileView,
          mobileIsNavOpen = _this$state.mobileIsNavOpen,
          desktopIsNavOpen = _this$state.desktopIsNavOpen;
      var context = {
        isManagedSidebar: isManagedSidebar,
        onNavToggle: mobileView ? this.onNavToggleMobile : this.onNavToggleDesktop,
        isNavOpen: mobileView ? mobileIsNavOpen : desktopIsNavOpen
      };
      return React.createElement(PageContextProvider, {
        value: context
      }, React.createElement("div", _extends({}, rest, {
        className: (0, _reactStyles.css)(_page["default"].page, className)
      }), skipToContent, header, sidebar, React.createElement("main", {
        role: role,
        id: mainContainerId,
        className: (0, _reactStyles.css)(_page["default"].pageMain),
        tabIndex: -1,
        "aria-label": mainAriaLabel
      }, breadcrumb && React.createElement("section", {
        className: (0, _reactStyles.css)(_page["default"].pageMainBreadcrumb)
      }, breadcrumb), children)));
    }
  }]);

  return Page;
}(React.Component);

exports.Page = Page;

_defineProperty(Page, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  header: _propTypes["default"].node,
  sidebar: _propTypes["default"].node,
  skipToContent: _propTypes["default"].element,
  role: _propTypes["default"].string,
  mainContainerId: _propTypes["default"].string,
  isManagedSidebar: _propTypes["default"].bool,
  defaultManagedSidebarIsOpen: _propTypes["default"].bool,
  onPageResize: _propTypes["default"].func,
  breadcrumb: _propTypes["default"].node,
  mainAriaLabel: _propTypes["default"].string
});

_defineProperty(Page, "defaultProps", {
  breadcrumb: null,
  children: null,
  className: '',
  header: null,
  sidebar: null,
  skipToContent: null,
  isManagedSidebar: false,
  defaultManagedSidebarIsOpen: true,
  onPageResize: function onPageResize() {
    return null;
  },
  mainContainerId: null,
  role: undefined
});
//# sourceMappingURL=Page.js.map