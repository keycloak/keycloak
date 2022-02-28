import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import globalBreakpointMd from '@patternfly/react-tokens/dist/js/global_breakpoint_md';
import { debounce } from '../../helpers/util';
export let PageLayouts;

(function (PageLayouts) {
  PageLayouts["vertical"] = "vertical";
  PageLayouts["horizontal"] = "horizontal";
})(PageLayouts || (PageLayouts = {}));

const PageContext = React.createContext({});
export const PageContextProvider = PageContext.Provider;
export const PageContextConsumer = PageContext.Consumer;
export class Page extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleResize", () => {
      const {
        onPageResize
      } = this.props;
      const windowSize = window.innerWidth; // eslint-disable-next-line radix

      const mobileView = windowSize < Number.parseInt(globalBreakpointMd.value, 10);

      if (onPageResize) {
        onPageResize({
          mobileView,
          windowSize
        });
      } // eslint-disable-next-line @typescript-eslint/no-unused-vars


      this.setState(prevState => ({
        mobileView
      }));
    });

    _defineProperty(this, "onNavToggleMobile", () => {
      this.setState(prevState => ({
        mobileIsNavOpen: !prevState.mobileIsNavOpen
      }));
    });

    _defineProperty(this, "onNavToggleDesktop", () => {
      this.setState(prevState => ({
        desktopIsNavOpen: !prevState.desktopIsNavOpen
      }));
    });

    const {
      isManagedSidebar,
      defaultManagedSidebarIsOpen
    } = props;
    const managedSidebarOpen = !isManagedSidebar ? true : defaultManagedSidebarIsOpen;
    this.state = {
      desktopIsNavOpen: managedSidebarOpen,
      mobileIsNavOpen: false,
      mobileView: false
    };
  }

  componentDidMount() {
    const {
      isManagedSidebar,
      onPageResize
    } = this.props;

    if (isManagedSidebar || onPageResize) {
      window.addEventListener('resize', debounce(this.handleResize, 250)); // Initial check if should be shown

      this.handleResize();
    }
  }

  componentWillUnmount() {
    const {
      isManagedSidebar,
      onPageResize
    } = this.props;

    if (isManagedSidebar || onPageResize) {
      window.removeEventListener('resize', debounce(this.handleResize, 250));
    }
  }

  render() {
    const _this$props = this.props,
          {
      breadcrumb,
      className,
      children,
      header,
      sidebar,
      skipToContent,
      role,
      mainContainerId,
      isManagedSidebar,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      defaultManagedSidebarIsOpen,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onPageResize,
      mainAriaLabel
    } = _this$props,
          rest = _objectWithoutProperties(_this$props, ["breadcrumb", "className", "children", "header", "sidebar", "skipToContent", "role", "mainContainerId", "isManagedSidebar", "defaultManagedSidebarIsOpen", "onPageResize", "mainAriaLabel"]);

    const {
      mobileView,
      mobileIsNavOpen,
      desktopIsNavOpen
    } = this.state;
    const context = {
      isManagedSidebar,
      onNavToggle: mobileView ? this.onNavToggleMobile : this.onNavToggleDesktop,
      isNavOpen: mobileView ? mobileIsNavOpen : desktopIsNavOpen
    };
    return React.createElement(PageContextProvider, {
      value: context
    }, React.createElement("div", _extends({}, rest, {
      className: css(styles.page, className)
    }), skipToContent, header, sidebar, React.createElement("main", {
      role: role,
      id: mainContainerId,
      className: css(styles.pageMain),
      tabIndex: -1,
      "aria-label": mainAriaLabel
    }, breadcrumb && React.createElement("section", {
      className: css(styles.pageMainBreadcrumb)
    }, breadcrumb), children)));
  }

}

_defineProperty(Page, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  header: _pt.node,
  sidebar: _pt.node,
  skipToContent: _pt.element,
  role: _pt.string,
  mainContainerId: _pt.string,
  isManagedSidebar: _pt.bool,
  defaultManagedSidebarIsOpen: _pt.bool,
  onPageResize: _pt.func,
  breadcrumb: _pt.node,
  mainAriaLabel: _pt.string
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
  onPageResize: () => null,
  mainContainerId: null,
  role: undefined
});
//# sourceMappingURL=Page.js.map