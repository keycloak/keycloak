import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { NavVariants } from './NavVariants';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/js/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { isElementInView } from '../../helpers/util';
import { NavContext } from './Nav';
export class NavList extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "navList", React.createRef());

    _defineProperty(this, "handleScrollButtons", () => {
      if (this.navList.current) {
        const {
          updateScrollButtonState
        } = this.context;
        const container = this.navList.current; // get first element and check if it is in view

        const showLeftScrollButton = !isElementInView(container, container.firstChild, false); // get last element and check if it is in view

        const showRightScrollButton = !isElementInView(container, container.lastChild, false);
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
          if (isElementInView(container, childrenArr[i], false)) {
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
          if (isElementInView(container, childrenArr[i], false)) {
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
    const isHorizontal = variant === NavVariants.horizontal || variant === NavVariants.tertiary;

    if (isHorizontal) {
      window.addEventListener('resize', this.handleScrollButtons, false); // call the handle resize function to check if scroll buttons should be shown

      this.handleScrollButtons();
    }
  }

  componentWillUnmount() {
    const {
      variant
    } = this.props;
    const isHorizontal = variant === NavVariants.horizontal || variant === NavVariants.tertiary;

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
      [NavVariants.default]: styles.navList,
      [NavVariants.simple]: styles.navSimpleList,
      [NavVariants.horizontal]: styles.navHorizontalList,
      [NavVariants.tertiary]: styles.navTertiaryList
    };
    const isHorizontal = variant === NavVariants.horizontal || variant === NavVariants.tertiary;
    return React.createElement(React.Fragment, null, isHorizontal && React.createElement("button", {
      className: css(styles.navScrollButton),
      "aria-label": ariaLeftScroll,
      onClick: this.scrollLeft
    }, React.createElement(AngleLeftIcon, null)), React.createElement("ul", _extends({
      ref: this.navList,
      className: css(variantStyle[variant], className)
    }, props), children), isHorizontal && React.createElement("button", {
      className: css(styles.navScrollButton),
      "aria-label": ariaRightScroll,
      onClick: this.scrollRight
    }, React.createElement(AngleRightIcon, null)));
  }

}

_defineProperty(NavList, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  variant: _pt.oneOf(['default', 'simple', 'horizontal', 'tertiary']),
  ariaLeftScroll: _pt.string,
  ariaRightScroll: _pt.string
});

_defineProperty(NavList, "contextType", NavContext);

_defineProperty(NavList, "defaultProps", {
  variant: 'default',
  children: null,
  className: '',
  ariaLeftScroll: 'Scroll left',
  ariaRightScroll: 'Scroll right'
});
//# sourceMappingURL=NavList.js.map