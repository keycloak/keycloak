(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "./NavVariants", "@patternfly/react-styles/css/components/Nav/nav", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/angle-left-icon", "@patternfly/react-icons/dist/js/icons/angle-right-icon", "../../helpers/util", "./Nav"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("./NavVariants"), require("@patternfly/react-styles/css/components/Nav/nav"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/angle-left-icon"), require("@patternfly/react-icons/dist/js/icons/angle-right-icon"), require("../../helpers/util"), require("./Nav"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.NavVariants, global.nav, global.reactStyles, global.angleLeftIcon, global.angleRightIcon, global.util, global.Nav);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _NavVariants, _nav, _reactStyles, _angleLeftIcon, _angleRightIcon, _util, _Nav) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.NavList = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _nav2 = _interopRequireDefault(_nav);

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

  class NavList extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "navList", React.createRef());

      _defineProperty(this, "handleScrollButtons", () => {
        if (this.navList.current) {
          const {
            updateScrollButtonState
          } = this.context;
          const container = this.navList.current; // get first element and check if it is in view

          const showLeftScrollButton = !(0, _util.isElementInView)(container, container.firstChild, false); // get last element and check if it is in view

          const showRightScrollButton = !(0, _util.isElementInView)(container, container.lastChild, false);
          updateScrollButtonState({
            showLeftScrollButton,
            showRightScrollButton
          });
        }
      });

      _defineProperty(this, "scrollLeft", () => {
        // find first Element that is fully in view on the left, then scroll to the element before it
        if (this.navList.current) {
          const container = this.navList.current;
          const childrenArr = Array.from(container.children);
          let firstElementInView;
          let lastElementOutOfView;

          for (let i = 0; i < childrenArr.length && !firstElementInView; i++) {
            if ((0, _util.isElementInView)(container, childrenArr[i], false)) {
              firstElementInView = childrenArr[i];
              lastElementOutOfView = childrenArr[i - 1];
            }
          }

          if (lastElementOutOfView) {
            container.scrollLeft -= lastElementOutOfView.scrollWidth;
          }

          this.handleScrollButtons();
        }
      });

      _defineProperty(this, "scrollRight", () => {
        // find last Element that is fully in view on the right, then scroll to the element after it
        if (this.navList.current) {
          const container = this.navList.current;
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

          this.handleScrollButtons();
        }
      });
    }

    componentDidMount() {
      const {
        variant
      } = this.props;
      const isHorizontal = variant === _NavVariants.NavVariants.horizontal || variant === _NavVariants.NavVariants.tertiary;

      if (isHorizontal) {
        window.addEventListener('resize', this.handleScrollButtons, false); // call the handle resize function to check if scroll buttons should be shown

        this.handleScrollButtons();
      }
    }

    componentWillUnmount() {
      const {
        variant
      } = this.props;
      const isHorizontal = variant === _NavVariants.NavVariants.horizontal || variant === _NavVariants.NavVariants.tertiary;

      if (isHorizontal) {
        document.removeEventListener('resize', this.handleScrollButtons, false);
      }
    }

    render() {
      const _this$props = this.props,
            {
        variant,
        children,
        className,
        ariaLeftScroll,
        ariaRightScroll
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["variant", "children", "className", "ariaLeftScroll", "ariaRightScroll"]);

      const variantStyle = {
        [_NavVariants.NavVariants.default]: _nav2.default.navList,
        [_NavVariants.NavVariants.simple]: _nav2.default.navSimpleList,
        [_NavVariants.NavVariants.horizontal]: _nav2.default.navHorizontalList,
        [_NavVariants.NavVariants.tertiary]: _nav2.default.navTertiaryList
      };
      const isHorizontal = variant === _NavVariants.NavVariants.horizontal || variant === _NavVariants.NavVariants.tertiary;
      return React.createElement(React.Fragment, null, isHorizontal && React.createElement("button", {
        className: (0, _reactStyles.css)(_nav2.default.navScrollButton),
        "aria-label": ariaLeftScroll,
        onClick: this.scrollLeft
      }, React.createElement(_angleLeftIcon2.default, null)), React.createElement("ul", _extends({
        ref: this.navList,
        className: (0, _reactStyles.css)(variantStyle[variant], className)
      }, props), children), isHorizontal && React.createElement("button", {
        className: (0, _reactStyles.css)(_nav2.default.navScrollButton),
        "aria-label": ariaRightScroll,
        onClick: this.scrollRight
      }, React.createElement(_angleRightIcon2.default, null)));
    }

  }

  exports.NavList = NavList;

  _defineProperty(NavList, "propTypes", {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    variant: _propTypes2.default.oneOf(['default', 'simple', 'horizontal', 'tertiary']),
    ariaLeftScroll: _propTypes2.default.string,
    ariaRightScroll: _propTypes2.default.string
  });

  _defineProperty(NavList, "contextType", _Nav.NavContext);

  _defineProperty(NavList, "defaultProps", {
    variant: 'default',
    children: null,
    className: '',
    ariaLeftScroll: 'Scroll left',
    ariaRightScroll: 'Scroll right'
  });
});
//# sourceMappingURL=NavList.js.map