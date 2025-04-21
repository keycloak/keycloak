"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Page = exports.PageContextConsumer = exports.PageContextProvider = exports.PageContext = exports.pageContextDefaults = exports.PageLayouts = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const react_styles_1 = require("@patternfly/react-styles");
const global_breakpoint_xl_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_xl'));
const util_1 = require("../../helpers/util");
const Drawer_1 = require("../Drawer");
const PageGroup_1 = require("./PageGroup");
const resizeObserver_1 = require("../../helpers/resizeObserver");
const util_2 = require("../../helpers/util");
var PageLayouts;
(function (PageLayouts) {
    PageLayouts["vertical"] = "vertical";
    PageLayouts["horizontal"] = "horizontal";
})(PageLayouts = exports.PageLayouts || (exports.PageLayouts = {}));
exports.pageContextDefaults = {
    isManagedSidebar: false,
    isNavOpen: false,
    onNavToggle: () => null,
    width: null,
    getBreakpoint: util_2.getBreakpoint
};
exports.PageContext = React.createContext(exports.pageContextDefaults);
exports.PageContextProvider = exports.PageContext.Provider;
exports.PageContextConsumer = exports.PageContext.Consumer;
class Page extends React.Component {
    constructor(props) {
        super(props);
        this.mainRef = React.createRef();
        this.pageRef = React.createRef();
        this.observer = () => { };
        this.getWindowWidth = () => {
            if (util_1.canUseDOM) {
                return this.pageRef.current ? this.pageRef.current.clientWidth : window.innerWidth;
            }
            else {
                return 1200;
            }
        };
        this.isMobile = () => 
        // eslint-disable-next-line radix
        this.getWindowWidth() < Number.parseInt(global_breakpoint_xl_1.default.value, 10);
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
        this.handleResize = util_1.debounce(this.resize, 250);
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
            this.observer = resizeObserver_1.getResizeObserver(this.pageRef.current, this.handleResize);
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
        onPageResize, getBreakpoint, mainAriaLabel, mainTabIndex, tertiaryNav, isTertiaryNavGrouped, isBreadcrumbGrouped, additionalGroupedContent, groupProps } = _a, rest = tslib_1.__rest(_a, ["breadcrumb", "isBreadcrumbWidthLimited", "className", "children", "header", "sidebar", "notificationDrawer", "isNotificationDrawerExpanded", "onNotificationDrawerExpand", "isTertiaryNavWidthLimited", "skipToContent", "role", "mainContainerId", "isManagedSidebar", "defaultManagedSidebarIsOpen", "onPageResize", "getBreakpoint", "mainAriaLabel", "mainTabIndex", "tertiaryNav", "isTertiaryNavGrouped", "isBreadcrumbGrouped", "additionalGroupedContent", "groupProps"]);
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
            nav = (React.createElement("div", { className: react_styles_1.css(page_1.default.pageMainNav, page_1.default.modifiers.limitWidth) },
                React.createElement("div", { className: react_styles_1.css(page_1.default.pageMainBody) }, tertiaryNav)));
        }
        else if (tertiaryNav) {
            nav = React.createElement("div", { className: react_styles_1.css(page_1.default.pageMainNav) }, tertiaryNav);
        }
        let crumb = null;
        if (breadcrumb && isBreadcrumbWidthLimited) {
            crumb = (React.createElement("section", { className: react_styles_1.css(page_1.default.pageMainBreadcrumb, page_1.default.modifiers.limitWidth) },
                React.createElement("div", { className: react_styles_1.css(page_1.default.pageMainBody) }, breadcrumb)));
        }
        else if (breadcrumb) {
            crumb = React.createElement("section", { className: react_styles_1.css(page_1.default.pageMainBreadcrumb) }, breadcrumb);
        }
        const isGrouped = isTertiaryNavGrouped || isBreadcrumbGrouped || additionalGroupedContent;
        const group = isGrouped ? (React.createElement(PageGroup_1.PageGroup, Object.assign({}, groupProps),
            isTertiaryNavGrouped && nav,
            isBreadcrumbGrouped && crumb,
            additionalGroupedContent)) : null;
        const main = (React.createElement("main", { ref: this.mainRef, role: role, id: mainContainerId, className: react_styles_1.css(page_1.default.pageMain), tabIndex: mainTabIndex, "aria-label": mainAriaLabel },
            group,
            !isTertiaryNavGrouped && nav,
            !isBreadcrumbGrouped && crumb,
            children));
        const panelContent = React.createElement(Drawer_1.DrawerPanelContent, null, notificationDrawer);
        return (React.createElement(exports.PageContextProvider, { value: context },
            React.createElement("div", Object.assign({ ref: this.pageRef }, rest, { className: react_styles_1.css(page_1.default.page, width !== null && 'pf-m-resize-observer', width !== null && `pf-m-breakpoint-${getBreakpoint(width)}`, className) }),
                skipToContent,
                header,
                sidebar,
                notificationDrawer && (React.createElement("div", { className: react_styles_1.css(page_1.default.pageDrawer) },
                    React.createElement(Drawer_1.Drawer, { isExpanded: isNotificationDrawerExpanded, onExpand: onNotificationDrawerExpand },
                        React.createElement(Drawer_1.DrawerContent, { panelContent: panelContent },
                            React.createElement(Drawer_1.DrawerContentBody, null, main))))),
                !notificationDrawer && main)));
    }
}
exports.Page = Page;
Page.displayName = 'Page';
Page.defaultProps = {
    isManagedSidebar: false,
    isBreadcrumbWidthLimited: false,
    defaultManagedSidebarIsOpen: true,
    onPageResize: () => null,
    mainTabIndex: -1,
    isNotificationDrawerExpanded: false,
    onNotificationDrawerExpand: () => null,
    getBreakpoint: util_2.getBreakpoint
};
//# sourceMappingURL=Page.js.map