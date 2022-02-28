(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Tabs/tabs", "@patternfly/react-styles/css/components/Button/button", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/angle-left-icon", "@patternfly/react-icons/dist/js/icons/angle-right-icon", "../../helpers/util", "../../helpers/constants", "./TabButton", "./TabContent", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Tabs/tabs"), require("@patternfly/react-styles/css/components/Button/button"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/angle-left-icon"), require("@patternfly/react-icons/dist/js/icons/angle-right-icon"), require("../../helpers/util"), require("../../helpers/constants"), require("./TabButton"), require("./TabContent"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.tabs, global.button, global.reactStyles, global.angleLeftIcon, global.angleRightIcon, global.util, global.constants, global.TabButton, global.TabContent, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _tabs, _button, _reactStyles, _angleLeftIcon, _angleRightIcon, _util, _constants, _TabButton, _TabContent, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Tabs = exports.TabsVariant = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _tabs2 = _interopRequireDefault(_tabs);

  var _button2 = _interopRequireDefault(_button);

  var _angleLeftIcon2 = _interopRequireDefault(_angleLeftIcon);

  var _angleRightIcon2 = _interopRequireDefault(_angleRightIcon);

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

  let TabsVariant = exports.TabsVariant = undefined;

  (function (TabsVariant) {
    TabsVariant["div"] = "div";
    TabsVariant["nav"] = "nav";
  })(TabsVariant || (exports.TabsVariant = TabsVariant = {}));

  class Tabs extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "tabList", React.createRef());

      _defineProperty(this, "handleScrollButtons", () => {
        if (this.tabList.current) {
          const container = this.tabList.current; // get first element and check if it is in view

          const showLeftScrollButton = !(0, _util.isElementInView)(container, container.firstChild, false); // get lase element and check if it is in view

          const showRightScrollButton = !(0, _util.isElementInView)(container, container.lastChild, false); // determine if selected tab is out of view and apply styles

          let selectedTab;
          const childrenArr = Array.from(container.children);
          childrenArr.forEach(child => {
            const {
              className
            } = child;

            if (className.search('pf-m-current') > 0) {
              selectedTab = child;
            }
          });
          const sideOutOfView = (0, _util.sideElementIsOutOfView)(container, selectedTab);
          this.setState({
            showLeftScrollButton,
            showRightScrollButton,
            highlightLeftScrollButton: (sideOutOfView === _constants.SIDE.LEFT || sideOutOfView === _constants.SIDE.BOTH) && showLeftScrollButton,
            highlightRightScrollButton: (sideOutOfView === _constants.SIDE.RIGHT || sideOutOfView === _constants.SIDE.BOTH) && showRightScrollButton
          });
        }
      });

      _defineProperty(this, "scrollLeft", () => {
        // find first Element that is fully in view on the left, then scroll to the element before it
        if (this.tabList.current) {
          const container = this.tabList.current;
          const childrenArr = Array.from(container.children);
          let firstElementInView;
          let lastElementOutOfView;
          let i;

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

      _defineProperty(this, "scrollRight", () => {
        // find last Element that is fully in view on the right, then scroll to the element after it
        if (this.tabList.current) {
          const container = this.tabList.current;
          const childrenArr = Array.from(container.children);
          let lastElementInView;
          let firstElementOutOfView;

          for (let i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
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

      this.state = {
        showLeftScrollButton: false,
        showRightScrollButton: false,
        highlightLeftScrollButton: false,
        highlightRightScrollButton: false,
        shownKeys: [this.props.activeKey] // only for mountOnEnter case

      };
    }

    handleTabClick(event, eventKey, tabContentRef, mountOnEnter) {
      const {
        shownKeys
      } = this.state;
      this.props.onSelect(event, eventKey); // process any tab content sections outside of the component

      if (tabContentRef) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        React.Children.map(this.props.children, (child, i) => {
          child.props.tabContentRef.current.hidden = true;
        }); // most recently selected tabContent

        tabContentRef.current.hidden = false;
      } // Update scroll button state and which button to highlight


      setTimeout(() => {
        this.handleScrollButtons();
      }, 1);

      if (mountOnEnter) {
        this.setState({
          shownKeys: shownKeys.concat(eventKey)
        });
      }
    }

    componentDidMount() {
      window.addEventListener('resize', this.handleScrollButtons, false); // call the handle resize function to check if scroll buttons should be shown

      this.handleScrollButtons();
    }

    componentWillUnmount() {
      document.removeEventListener('resize', this.handleScrollButtons, false);
    }

    render() {
      const _this$props = this.props,
            {
        className,
        children,
        activeKey,
        id,
        isFilled,
        isSecondary,
        leftScrollAriaLabel,
        rightScrollAriaLabel,
        'aria-label': ariaLabel,
        variant,
        ouiaContext,
        ouiaId,
        mountOnEnter,
        unmountOnExit
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "children", "activeKey", "id", "isFilled", "isSecondary", "leftScrollAriaLabel", "rightScrollAriaLabel", "aria-label", "variant", "ouiaContext", "ouiaId", "mountOnEnter", "unmountOnExit"]);

      const {
        showLeftScrollButton,
        showRightScrollButton,
        highlightLeftScrollButton,
        highlightRightScrollButton,
        shownKeys
      } = this.state;
      const uniqueId = id || (0, _util.getUniqueId)();
      const Component = variant === TabsVariant.nav ? 'nav' : 'div';
      return React.createElement(React.Fragment, null, React.createElement(Component, _extends({
        "aria-label": ariaLabel,
        className: (0, _reactStyles.css)(_tabs2.default.tabs, isFilled && _tabs2.default.modifiers.fill, isSecondary && _tabs2.default.modifiers.tabsSecondary, showLeftScrollButton && _tabs2.default.modifiers.start, showRightScrollButton && _tabs2.default.modifiers.end, highlightLeftScrollButton && _tabs2.default.modifiers.startCurrent, highlightRightScrollButton && _tabs2.default.modifiers.endCurrent, className)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Tabs',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }, {
        id: id && id
      }, props), React.createElement("button", {
        className: (0, _reactStyles.css)(_tabs2.default.tabsScrollButton, isSecondary && _button2.default.modifiers.secondary),
        "aria-label": leftScrollAriaLabel,
        onClick: this.scrollLeft
      }, React.createElement(_angleLeftIcon2.default, null)), React.createElement("ul", {
        className: (0, _reactStyles.css)(_tabs2.default.tabsList),
        ref: this.tabList,
        onScroll: this.handleScrollButtons
      }, React.Children.map(children, (child, index) => {
        const _child$props = child.props,
              {
          title,
          eventKey,
          tabContentRef,
          id: childId,
          tabContentId
        } = _child$props,
              rest = _objectWithoutProperties(_child$props, ["title", "eventKey", "tabContentRef", "id", "tabContentId"]);

        return React.createElement("li", {
          key: index,
          className: (0, _reactStyles.css)(_tabs2.default.tabsItem, eventKey === activeKey && _tabs2.default.modifiers.current, className)
        }, React.createElement(_TabButton.TabButton, _extends({
          className: (0, _reactStyles.css)(_tabs2.default.tabsButton),
          onClick: event => this.handleTabClick(event, eventKey, tabContentRef, mountOnEnter),
          id: `pf-tab-${eventKey}-${childId || uniqueId}`,
          "aria-controls": tabContentId ? `${tabContentId}` : `pf-tab-section-${eventKey}-${childId || uniqueId}`,
          tabContentRef: tabContentRef
        }, rest), title));
      })), React.createElement("button", {
        className: (0, _reactStyles.css)(_tabs2.default.tabsScrollButton, isSecondary && _button2.default.modifiers.secondary),
        "aria-label": rightScrollAriaLabel,
        onClick: this.scrollRight
      }, React.createElement(_angleRightIcon2.default, null))), React.Children.map(children, (child, index) => {
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

  }

  _defineProperty(Tabs, "propTypes", {
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    activeKey: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    onSelect: _propTypes2.default.func,
    id: _propTypes2.default.string,
    isFilled: _propTypes2.default.bool,
    isSecondary: _propTypes2.default.bool,
    leftScrollAriaLabel: _propTypes2.default.string,
    rightScrollAriaLabel: _propTypes2.default.string,
    variant: _propTypes2.default.oneOf(['div', 'nav']),
    'aria-label': _propTypes2.default.string,
    mountOnEnter: _propTypes2.default.bool,
    unmountOnExit: _propTypes2.default.bool
  });

  _defineProperty(Tabs, "defaultProps", {
    className: '',
    activeKey: 0,
    onSelect: () => undefined,
    isFilled: false,
    isSecondary: false,
    leftScrollAriaLabel: 'Scroll left',
    rightScrollAriaLabel: 'Scroll right',
    variant: TabsVariant.div,
    mountOnEnter: false,
    unmountOnExit: false
  });

  const TabsWithOuiaContext = (0, _withOuia.withOuiaContext)(Tabs);
  exports.Tabs = TabsWithOuiaContext;
});
//# sourceMappingURL=Tabs.js.map