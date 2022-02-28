"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Tabs = exports.TabsVariant = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _tabs = _interopRequireDefault(require("@patternfly/react-styles/css/components/Tabs/tabs"));

var _button = _interopRequireDefault(require("@patternfly/react-styles/css/components/Button/button"));

var _reactStyles = require("@patternfly/react-styles");

var _angleLeftIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-left-icon"));

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

var _util = require("../../helpers/util");

var _constants = require("../../helpers/constants");

var _TabButton = require("./TabButton");

var _TabContent = require("./TabContent");

var _withOuia = require("../withOuia");

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

var TabsVariant;
exports.TabsVariant = TabsVariant;

(function (TabsVariant) {
  TabsVariant["div"] = "div";
  TabsVariant["nav"] = "nav";
})(TabsVariant || (exports.TabsVariant = TabsVariant = {}));

var Tabs =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Tabs, _React$Component);

  function Tabs(props) {
    var _this;

    _classCallCheck(this, Tabs);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Tabs).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "tabList", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "handleScrollButtons", function () {
      if (_this.tabList.current) {
        var container = _this.tabList.current; // get first element and check if it is in view

        var showLeftScrollButton = !(0, _util.isElementInView)(container, container.firstChild, false); // get lase element and check if it is in view

        var showRightScrollButton = !(0, _util.isElementInView)(container, container.lastChild, false); // determine if selected tab is out of view and apply styles

        var selectedTab;
        var childrenArr = Array.from(container.children);
        childrenArr.forEach(function (child) {
          var className = child.className;

          if (className.search('pf-m-current') > 0) {
            selectedTab = child;
          }
        });
        var sideOutOfView = (0, _util.sideElementIsOutOfView)(container, selectedTab);

        _this.setState({
          showLeftScrollButton: showLeftScrollButton,
          showRightScrollButton: showRightScrollButton,
          highlightLeftScrollButton: (sideOutOfView === _constants.SIDE.LEFT || sideOutOfView === _constants.SIDE.BOTH) && showLeftScrollButton,
          highlightRightScrollButton: (sideOutOfView === _constants.SIDE.RIGHT || sideOutOfView === _constants.SIDE.BOTH) && showRightScrollButton
        });
      }
    });

    _defineProperty(_assertThisInitialized(_this), "scrollLeft", function () {
      // find first Element that is fully in view on the left, then scroll to the element before it
      if (_this.tabList.current) {
        var container = _this.tabList.current;
        var childrenArr = Array.from(container.children);
        var firstElementInView;
        var lastElementOutOfView;
        var i;

        for (i = 0; i < childrenArr.length && !firstElementInView; i++) {
          if ((0, _util.isElementInView)(container, childrenArr[i], false)) {
            firstElementInView = childrenArr[i];
            lastElementOutOfView = childrenArr[i - 1];
          }
        }

        if (lastElementOutOfView) {
          container.scrollLeft -= lastElementOutOfView.scrollWidth;
        }
      }
    });

    _defineProperty(_assertThisInitialized(_this), "scrollRight", function () {
      // find last Element that is fully in view on the right, then scroll to the element after it
      if (_this.tabList.current) {
        var container = _this.tabList.current;
        var childrenArr = Array.from(container.children);
        var lastElementInView;
        var firstElementOutOfView;

        for (var i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
          if ((0, _util.isElementInView)(container, childrenArr[i], false)) {
            lastElementInView = childrenArr[i];
            firstElementOutOfView = childrenArr[i + 1];
          }
        }

        if (firstElementOutOfView) {
          container.scrollLeft += firstElementOutOfView.scrollWidth;
        }
      }
    });

    _this.state = {
      showLeftScrollButton: false,
      showRightScrollButton: false,
      highlightLeftScrollButton: false,
      highlightRightScrollButton: false,
      shownKeys: [_this.props.activeKey] // only for mountOnEnter case

    };
    return _this;
  }

  _createClass(Tabs, [{
    key: "handleTabClick",
    value: function handleTabClick(event, eventKey, tabContentRef, mountOnEnter) {
      var _this2 = this;

      var shownKeys = this.state.shownKeys;
      this.props.onSelect(event, eventKey); // process any tab content sections outside of the component

      if (tabContentRef) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        React.Children.map(this.props.children, function (child, i) {
          child.props.tabContentRef.current.hidden = true;
        }); // most recently selected tabContent

        tabContentRef.current.hidden = false;
      } // Update scroll button state and which button to highlight


      setTimeout(function () {
        _this2.handleScrollButtons();
      }, 1);

      if (mountOnEnter) {
        this.setState({
          shownKeys: shownKeys.concat(eventKey)
        });
      }
    }
  }, {
    key: "componentDidMount",
    value: function componentDidMount() {
      window.addEventListener('resize', this.handleScrollButtons, false); // call the handle resize function to check if scroll buttons should be shown

      this.handleScrollButtons();
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      document.removeEventListener('resize', this.handleScrollButtons, false);
    }
  }, {
    key: "render",
    value: function render() {
      var _this3 = this;

      var _this$props = this.props,
          className = _this$props.className,
          children = _this$props.children,
          activeKey = _this$props.activeKey,
          id = _this$props.id,
          isFilled = _this$props.isFilled,
          isSecondary = _this$props.isSecondary,
          leftScrollAriaLabel = _this$props.leftScrollAriaLabel,
          rightScrollAriaLabel = _this$props.rightScrollAriaLabel,
          ariaLabel = _this$props['aria-label'],
          variant = _this$props.variant,
          ouiaContext = _this$props.ouiaContext,
          ouiaId = _this$props.ouiaId,
          mountOnEnter = _this$props.mountOnEnter,
          unmountOnExit = _this$props.unmountOnExit,
          props = _objectWithoutProperties(_this$props, ["className", "children", "activeKey", "id", "isFilled", "isSecondary", "leftScrollAriaLabel", "rightScrollAriaLabel", "aria-label", "variant", "ouiaContext", "ouiaId", "mountOnEnter", "unmountOnExit"]);

      var _this$state = this.state,
          showLeftScrollButton = _this$state.showLeftScrollButton,
          showRightScrollButton = _this$state.showRightScrollButton,
          highlightLeftScrollButton = _this$state.highlightLeftScrollButton,
          highlightRightScrollButton = _this$state.highlightRightScrollButton,
          shownKeys = _this$state.shownKeys;
      var uniqueId = id || (0, _util.getUniqueId)();
      var Component = variant === TabsVariant.nav ? 'nav' : 'div';
      return React.createElement(React.Fragment, null, React.createElement(Component, _extends({
        "aria-label": ariaLabel,
        className: (0, _reactStyles.css)(_tabs["default"].tabs, isFilled && _tabs["default"].modifiers.fill, isSecondary && _tabs["default"].modifiers.tabsSecondary, showLeftScrollButton && _tabs["default"].modifiers.start, showRightScrollButton && _tabs["default"].modifiers.end, highlightLeftScrollButton && _tabs["default"].modifiers.startCurrent, highlightRightScrollButton && _tabs["default"].modifiers.endCurrent, className)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Tabs',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }, {
        id: id && id
      }, props), React.createElement("button", {
        className: (0, _reactStyles.css)(_tabs["default"].tabsScrollButton, isSecondary && _button["default"].modifiers.secondary),
        "aria-label": leftScrollAriaLabel,
        onClick: this.scrollLeft
      }, React.createElement(_angleLeftIcon["default"], null)), React.createElement("ul", {
        className: (0, _reactStyles.css)(_tabs["default"].tabsList),
        ref: this.tabList,
        onScroll: this.handleScrollButtons
      }, React.Children.map(children, function (child, index) {
        var _child$props = child.props,
            title = _child$props.title,
            eventKey = _child$props.eventKey,
            tabContentRef = _child$props.tabContentRef,
            childId = _child$props.id,
            tabContentId = _child$props.tabContentId,
            rest = _objectWithoutProperties(_child$props, ["title", "eventKey", "tabContentRef", "id", "tabContentId"]);

        return React.createElement("li", {
          key: index,
          className: (0, _reactStyles.css)(_tabs["default"].tabsItem, eventKey === activeKey && _tabs["default"].modifiers.current, className)
        }, React.createElement(_TabButton.TabButton, _extends({
          className: (0, _reactStyles.css)(_tabs["default"].tabsButton),
          onClick: function onClick(event) {
            return _this3.handleTabClick(event, eventKey, tabContentRef, mountOnEnter);
          },
          id: "pf-tab-".concat(eventKey, "-").concat(childId || uniqueId),
          "aria-controls": tabContentId ? "".concat(tabContentId) : "pf-tab-section-".concat(eventKey, "-").concat(childId || uniqueId),
          tabContentRef: tabContentRef
        }, rest), title));
      })), React.createElement("button", {
        className: (0, _reactStyles.css)(_tabs["default"].tabsScrollButton, isSecondary && _button["default"].modifiers.secondary),
        "aria-label": rightScrollAriaLabel,
        onClick: this.scrollRight
      }, React.createElement(_angleRightIcon["default"], null))), React.Children.map(children, function (child, index) {
        if (!child.props.children || unmountOnExit && child.props.eventKey !== activeKey || mountOnEnter && shownKeys.indexOf(child.props.eventKey) === -1) {
          return null;
        } else {
          return React.createElement(_TabContent.TabContent, {
            key: index,
            activeKey: activeKey,
            child: child,
            id: child.props.id || uniqueId
          });
        }
      }));
    }
  }]);

  return Tabs;
}(React.Component);

_defineProperty(Tabs, "propTypes", {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  activeKey: _propTypes["default"].oneOfType([_propTypes["default"].number, _propTypes["default"].string]),
  onSelect: _propTypes["default"].func,
  id: _propTypes["default"].string,
  isFilled: _propTypes["default"].bool,
  isSecondary: _propTypes["default"].bool,
  leftScrollAriaLabel: _propTypes["default"].string,
  rightScrollAriaLabel: _propTypes["default"].string,
  variant: _propTypes["default"].oneOf(['div', 'nav']),
  'aria-label': _propTypes["default"].string,
  mountOnEnter: _propTypes["default"].bool,
  unmountOnExit: _propTypes["default"].bool
});

_defineProperty(Tabs, "defaultProps", {
  className: '',
  activeKey: 0,
  onSelect: function onSelect() {
    return undefined;
  },
  isFilled: false,
  isSecondary: false,
  leftScrollAriaLabel: 'Scroll left',
  rightScrollAriaLabel: 'Scroll right',
  variant: TabsVariant.div,
  mountOnEnter: false,
  unmountOnExit: false
});

var TabsWithOuiaContext = (0, _withOuia.withOuiaContext)(Tabs);
exports.Tabs = TabsWithOuiaContext;
//# sourceMappingURL=Tabs.js.map