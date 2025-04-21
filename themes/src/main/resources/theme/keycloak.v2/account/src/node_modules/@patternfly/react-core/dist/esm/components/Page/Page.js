import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import globalBreakpointXl from '@patternfly/react-tokens/dist/esm/global_breakpoint_xl';
import { debounce, canUseDOM } from '../../helpers/util';
import { Drawer, DrawerContent, DrawerContentBody, DrawerPanelContent } from '../Drawer';
import { PageGroup } from './PageGroup';
import { getResizeObserver } from '../../helpers/resizeObserver';
import { getBreakpoint } from '../../helpers/util';
export var PageLayouts;
(function (PageLayouts) {
    PageLayouts["vertical"] = "vertical";
    PageLayouts["horizontal"] = "horizontal";
})(PageLayouts || (PageLayouts = {}));
export const pageContextDefaults = {
    isManagedSidebar: false,
    isNavOpen: false,
    onNavToggle: () => null,
    width: null,
    getBreakpoint
};
export const PageContext = React.createContext(pageContextDefaults);
export const PageContextProvider = PageContext.Provider;
export const PageContextConsumer = PageContext.Consumer;
export class Page extends React.Component {
    constructor(props) {
        super(props);
        this.mainRef = React.createRef();
        this.pageRef = React.createRef();
        this.observer = () => { };
        this.getWindowWidth = () => {
            if (canUseDOM) {
                return this.pageRef.current ? this.pageRef.current.clientWidth : window.innerWidth;
            }
            else {
                return 1200;
            }
        };
        this.isMobile = () => 
        // eslint-disable-next-line radix
        this.getWindowWidth() < Number.parseInt(globalBreakpointXl.value, 10);
        this.resize = () => {
            const { onPageResize } = this.props;
            const mobileView = this.isMobile();
            if (onPageResize) {
                onPageResize({ mobileView, windowSize: this.getWindowWidth() });
            }
            if (mobileView !== this.state.mobileView) {
                this.setState({ mobileView });
            }
            this.pageRef.current && this.setState({ width: this.pageRef.current.clientWidth });
        };
        this.handleResize = debounce(this.resize, 250);
        this.handleMainClick = () => {
            if (this.isMobile() && this.state.mobileIsNavOpen && this.mainRef.current) {
                this.setState({ mobileIsNavOpen: false });
            }
        };
        this.onNavToggleMobile = () => {
            this.setState(prevState => ({
                mobileIsNavOpen: !prevState.mobileIsNavOpen
            }));
        };
        this.onNavToggleDesktop = () => {
            this.setState(prevState => ({
                desktopIsNavOpen: !prevState.desktopIsNavOpen
            }));
        };
        const { isManagedSidebar, defaultManagedSidebarIsOpen } = props;
        const managedSidebarOpen = !isManagedSidebar ? true : defaultManagedSidebarIsOpen;
        this.state = {
            desktopIsNavOpen: managedSidebarOpen,
            mobileIsNavOpen: false,
            mobileView: false,
            width: null
        };
    }
    componentDidMount() {
        const { isManagedSidebar, onPageResize } = this.props;
        if (isManagedSidebar || onPageResize) {
            this.observer = getResizeObserver(this.pageRef.current, this.handleResize);
            const currentRef = this.mainRef.current;
            if (currentRef) {
                currentRef.addEventListener('mousedown', this.handleMainClick);
                currentRef.addEventListener('touchstart', this.handleMainClick);
            }
            // Initial check if should be shown
            this.resize();
        }
    }
    componentWillUnmount() {
        const { isManagedSidebar, onPageResize } = this.props;
        if (isManagedSidebar || onPageResize) {
            this.observer();
            const currentRef = this.mainRef.current;
            if (currentRef) {
                currentRef.removeEventListener('mousedown', this.handleMainClick);
                currentRef.removeEventListener('touchstart', this.handleMainClick);
            }
        }
    }
    render() {
        const _a = this.props, { breadcrumb, isBreadcrumbWidthLimited, className, children, header, sidebar, notificationDrawer, isNotificationDrawerExpanded, onNotificationDrawerExpand, isTertiaryNavWidthLimited, skipToContent, role, mainContainerId, isManagedSidebar, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        defaultManagedSidebarIsOpen, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onPageResize, getBreakpoint, mainAriaLabel, mainTabIndex, tertiaryNav, isTertiaryNavGrouped, isBreadcrumbGrouped, additionalGroupedContent, groupProps } = _a, rest = __rest(_a, ["breadcrumb", "isBreadcrumbWidthLimited", "className", "children", "header", "sidebar", "notificationDrawer", "isNotificationDrawerExpanded", "onNotificationDrawerExpand", "isTertiaryNavWidthLimited", "skipToContent", "role", "mainContainerId", "isManagedSidebar", "defaultManagedSidebarIsOpen", "onPageResize", "getBreakpoint", "mainAriaLabel", "mainTabIndex", "tertiaryNav", "isTertiaryNavGrouped", "isBreadcrumbGrouped", "additionalGroupedContent", "groupProps"]);
        const { mobileView, mobileIsNavOpen, desktopIsNavOpen, width } = this.state;
        const context = {
            isManagedSidebar,
            onNavToggle: mobileView ? this.onNavToggleMobile : this.onNavToggleDesktop,
            isNavOpen: mobileView ? mobileIsNavOpen : desktopIsNavOpen,
            width,
            getBreakpoint
        };
        let nav = null;
        if (tertiaryNav && isTertiaryNavWidthLimited) {
            nav = (React.createElement("div", { className: css(styles.pageMainNav, styles.modifiers.limitWidth) },
                React.createElement("div", { className: css(styles.pageMainBody) }, tertiaryNav)));
        }
        else if (tertiaryNav) {
            nav = React.createElement("div", { className: css(styles.pageMainNav) }, tertiaryNav);
        }
        let crumb = null;
        if (breadcrumb && isBreadcrumbWidthLimited) {
            crumb = (React.createElement("section", { className: css(styles.pageMainBreadcrumb, styles.modifiers.limitWidth) },
                React.createElement("div", { className: css(styles.pageMainBody) }, breadcrumb)));
        }
        else if (breadcrumb) {
            crumb = React.createElement("section", { className: css(styles.pageMainBreadcrumb) }, breadcrumb);
        }
        const isGrouped = isTertiaryNavGrouped || isBreadcrumbGrouped || additionalGroupedContent;
        const group = isGrouped ? (React.createElement(PageGroup, Object.assign({}, groupProps),
            isTertiaryNavGrouped && nav,
            isBreadcrumbGrouped && crumb,
            additionalGroupedContent)) : null;
        const main = (React.createElement("main", { ref: this.mainRef, role: role, id: mainContainerId, className: css(styles.pageMain), tabIndex: mainTabIndex, "aria-label": mainAriaLabel },
            group,
            !isTertiaryNavGrouped && nav,
            !isBreadcrumbGrouped && crumb,
            children));
        const panelContent = React.createElement(DrawerPanelContent, null, notificationDrawer);
        return (React.createElement(PageContextProvider, { value: context },
            React.createElement("div", Object.assign({ ref: this.pageRef }, rest, { className: css(styles.page, width !== null && 'pf-m-resize-observer', width !== null && `pf-m-breakpoint-${getBreakpoint(width)}`, className) }),
                skipToContent,
                header,
                sidebar,
                notificationDrawer && (React.createElement("div", { className: css(styles.pageDrawer) },
                    React.createElement(Drawer, { isExpanded: isNotificationDrawerExpanded, onExpand: onNotificationDrawerExpand },
                        React.createElement(DrawerContent, { panelContent: panelContent },
                            React.createElement(DrawerContentBody, null, main))))),
                !notificationDrawer && main)));
    }
}
Page.displayName = 'Page';
Page.defaultProps = {
    isManagedSidebar: false,
    isBreadcrumbWidthLimited: false,
    defaultManagedSidebarIsOpen: true,
    onPageResize: () => null,
    mainTabIndex: -1,
    isNotificationDrawerExpanded: false,
    onNotificationDrawerExpand: () => null,
    getBreakpoint
};
//# sourceMappingURL=Page.js.map