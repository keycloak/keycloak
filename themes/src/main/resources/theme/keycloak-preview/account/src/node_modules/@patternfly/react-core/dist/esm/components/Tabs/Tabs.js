import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/js/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import { getUniqueId, isElementInView, sideElementIsOutOfView } from '../../helpers/util';
import { SIDE } from '../../helpers/constants';
import { TabButton } from './TabButton';
import { TabContent } from './TabContent';
import { withOuiaContext } from '../withOuia';
export let TabsVariant;

(function (TabsVariant) {
  TabsVariant["div"] = "div";
  TabsVariant["nav"] = "nav";
})(TabsVariant || (TabsVariant = {}));

class Tabs extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "tabList", React.createRef());

    _defineProperty(this, "handleScrollButtons", () => {
      if (this.tabList.current) {
        const container = this.tabList.current; // get first element and check if it is in view

        const showLeftScrollButton = !isElementInView(container, container.firstChild, false); // get lase element and check if it is in view

        const showRightScrollButton = !isElementInView(container, container.lastChild, false); // determine if selected tab is out of view and apply styles

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
        const sideOutOfView = sideElementIsOutOfView(container, selectedTab);
        this.setState({
          showLeftScrollButton,
          showRightScrollButton,
          highlightLeftScrollButton: (sideOutOfView === SIDE.LEFT || sideOutOfView === SIDE.BOTH) && showLeftScrollButton,
          highlightRightScrollButton: (sideOutOfView === SIDE.RIGHT || sideOutOfView === SIDE.BOTH) && showRightScrollButton
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
          if (isElementInView(container, childrenArr[i], false)) {
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
          if (isElementInView(container, childrenArr[i], false)) {
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
    const uniqueId = id || getUniqueId();
    const Component = variant === TabsVariant.nav ? 'nav' : 'div';
    return React.createElement(React.Fragment, null, React.createElement(Component, _extends({
      "aria-label": ariaLabel,
      className: css(styles.tabs, isFilled && styles.modifiers.fill, isSecondary && styles.modifiers.tabsSecondary, showLeftScrollButton && styles.modifiers.start, showRightScrollButton && styles.modifiers.end, highlightLeftScrollButton && styles.modifiers.startCurrent, highlightRightScrollButton && styles.modifiers.endCurrent, className)
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Tabs',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }, {
      id: id && id
    }, props), React.createElement("button", {
      className: css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary),
      "aria-label": leftScrollAriaLabel,
      onClick: this.scrollLeft
    }, React.createElement(AngleLeftIcon, null)), React.createElement("ul", {
      className: css(styles.tabsList),
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
        className: css(styles.tabsItem, eventKey === activeKey && styles.modifiers.current, className)
      }, React.createElement(TabButton, _extends({
        className: css(styles.tabsButton),
        onClick: event => this.handleTabClick(event, eventKey, tabContentRef, mountOnEnter),
        id: `pf-tab-${eventKey}-${childId || uniqueId}`,
        "aria-controls": tabContentId ? `${tabContentId}` : `pf-tab-section-${eventKey}-${childId || uniqueId}`,
        tabContentRef: tabContentRef
      }, rest), title));
    })), React.createElement("button", {
      className: css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary),
      "aria-label": rightScrollAriaLabel,
      onClick: this.scrollRight
    }, React.createElement(AngleRightIcon, null))), React.Children.map(children, (child, index) => {
      if (!child.props.children || unmountOnExit && child.props.eventKey !== activeKey || mountOnEnter && shownKeys.indexOf(child.props.eventKey) === -1) {
        return null;
      } else {
        return React.createElement(TabContent, {
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
  children: _pt.node.isRequired,
  className: _pt.string,
  activeKey: _pt.oneOfType([_pt.number, _pt.string]),
  onSelect: _pt.func,
  id: _pt.string,
  isFilled: _pt.bool,
  isSecondary: _pt.bool,
  leftScrollAriaLabel: _pt.string,
  rightScrollAriaLabel: _pt.string,
  variant: _pt.oneOf(['div', 'nav']),
  'aria-label': _pt.string,
  mountOnEnter: _pt.bool,
  unmountOnExit: _pt.bool
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

const TabsWithOuiaContext = withOuiaContext(Tabs);
export { TabsWithOuiaContext as Tabs };
//# sourceMappingURL=Tabs.js.map