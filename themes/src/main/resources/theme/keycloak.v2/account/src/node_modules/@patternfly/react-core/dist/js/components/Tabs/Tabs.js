"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Tabs = exports.TabsComponent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const tabs_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Tabs/tabs"));
const button_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Button/button"));
const react_styles_1 = require("@patternfly/react-styles");
const angle_left_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-left-icon'));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const plus_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/plus-icon'));
const util_1 = require("../../helpers/util");
const TabContent_1 = require("./TabContent");
const TabsContext_1 = require("./TabsContext");
const Button_1 = require("../Button");
const helpers_1 = require("../../helpers");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
var TabsComponent;
(function (TabsComponent) {
    TabsComponent["div"] = "div";
    TabsComponent["nav"] = "nav";
})(TabsComponent = exports.TabsComponent || (exports.TabsComponent = {}));
const variantStyle = {
    default: '',
    light300: tabs_1.default.modifiers.colorSchemeLight_300
};
class Tabs extends React.Component {
    constructor(props) {
        super(props);
        this.tabList = React.createRef();
        this.scrollTimeout = null;
        this.handleScrollButtons = () => {
            // add debounce to the scroll event
            clearTimeout(this.scrollTimeout);
            this.scrollTimeout = setTimeout(() => {
                const container = this.tabList.current;
                let disableLeftScrollButton = true;
                let disableRightScrollButton = true;
                let showScrollButtons = false;
                if (container && !this.props.isVertical) {
                    // get first element and check if it is in view
                    const overflowOnLeft = !util_1.isElementInView(container, container.firstChild, false);
                    // get last element and check if it is in view
                    const overflowOnRight = !util_1.isElementInView(container, container.lastChild, false);
                    showScrollButtons = overflowOnLeft || overflowOnRight;
                    disableLeftScrollButton = !overflowOnLeft;
                    disableRightScrollButton = !overflowOnRight;
                }
                this.setState({
                    showScrollButtons,
                    disableLeftScrollButton,
                    disableRightScrollButton
                });
            }, 100);
        };
        this.scrollLeft = () => {
            // find first Element that is fully in view on the left, then scroll to the element before it
            if (this.tabList.current) {
                const container = this.tabList.current;
                const childrenArr = Array.from(container.children);
                let firstElementInView;
                let lastElementOutOfView;
                let i;
                for (i = 0; i < childrenArr.length && !firstElementInView; i++) {
                    if (util_1.isElementInView(container, childrenArr[i], false)) {
                        firstElementInView = childrenArr[i];
                        lastElementOutOfView = childrenArr[i - 1];
                    }
                }
                if (lastElementOutOfView) {
                    container.scrollLeft -= lastElementOutOfView.scrollWidth;
                }
            }
        };
        this.scrollRight = () => {
            // find last Element that is fully in view on the right, then scroll to the element after it
            if (this.tabList.current) {
                const container = this.tabList.current;
                const childrenArr = Array.from(container.children);
                let lastElementInView;
                let firstElementOutOfView;
                for (let i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
                    if (util_1.isElementInView(container, childrenArr[i], false)) {
                        lastElementInView = childrenArr[i];
                        firstElementOutOfView = childrenArr[i + 1];
                    }
                }
                if (firstElementOutOfView) {
                    container.scrollLeft += firstElementOutOfView.scrollWidth;
                }
            }
        };
        this.state = {
            showScrollButtons: false,
            disableLeftScrollButton: true,
            disableRightScrollButton: true,
            shownKeys: this.props.defaultActiveKey !== undefined ? [this.props.defaultActiveKey] : [this.props.activeKey],
            uncontrolledActiveKey: this.props.defaultActiveKey,
            uncontrolledIsExpandedLocal: this.props.defaultIsExpanded,
            ouiaStateId: helpers_1.getDefaultOUIAId(Tabs.displayName)
        };
        if (this.props.isVertical && this.props.expandable !== undefined) {
            if (!this.props.toggleAriaLabel && !this.props.toggleText) {
                // eslint-disable-next-line no-console
                console.error('Tabs:', 'toggleAriaLabel or the toggleText prop is required to make the toggle button accessible');
            }
        }
    }
    handleTabClick(event, eventKey, tabContentRef) {
        const { shownKeys } = this.state;
        const { onSelect, defaultActiveKey } = this.props;
        // if defaultActiveKey Tabs are uncontrolled, set new active key internally
        if (defaultActiveKey !== undefined) {
            this.setState({
                uncontrolledActiveKey: eventKey
            });
        }
        else {
            onSelect(event, eventKey);
        }
        // process any tab content sections outside of the component
        if (tabContentRef) {
            React.Children.toArray(this.props.children)
                .map(child => child)
                .filter(child => child.props && child.props.tabContentRef && child.props.tabContentRef.current)
                .forEach(child => (child.props.tabContentRef.current.hidden = true));
            // most recently selected tabContent
            if (tabContentRef.current) {
                tabContentRef.current.hidden = false;
            }
        }
        if (this.props.mountOnEnter) {
            this.setState({
                shownKeys: shownKeys.concat(eventKey)
            });
        }
    }
    componentDidMount() {
        if (!this.props.isVertical) {
            if (helpers_1.canUseDOM) {
                window.addEventListener('resize', this.handleScrollButtons, false);
            }
            // call the handle resize function to check if scroll buttons should be shown
            this.handleScrollButtons();
        }
    }
    componentWillUnmount() {
        if (!this.props.isVertical) {
            if (helpers_1.canUseDOM) {
                window.removeEventListener('resize', this.handleScrollButtons, false);
            }
        }
        clearTimeout(this.scrollTimeout);
    }
    componentDidUpdate(prevProps) {
        const { activeKey, mountOnEnter, children } = this.props;
        const { shownKeys } = this.state;
        if (prevProps.activeKey !== activeKey && mountOnEnter && shownKeys.indexOf(activeKey) < 0) {
            this.setState({
                shownKeys: shownKeys.concat(activeKey)
            });
        }
        if (prevProps.children &&
            children &&
            React.Children.toArray(prevProps.children).length !== React.Children.toArray(children).length) {
            this.handleScrollButtons();
        }
    }
    render() {
        const _a = this.props, { className, children, activeKey, defaultActiveKey, id, isFilled, isSecondary, isVertical, isBox, hasBorderBottom, hasSecondaryBorderBottom, leftScrollAriaLabel, rightScrollAriaLabel, 'aria-label': ariaLabel, component, ouiaId, ouiaSafe, mountOnEnter, unmountOnExit, usePageInsets, inset, variant, expandable, isExpanded, defaultIsExpanded, toggleText, toggleAriaLabel, addButtonAriaLabel, onToggle, onClose, onAdd } = _a, props = tslib_1.__rest(_a, ["className", "children", "activeKey", "defaultActiveKey", "id", "isFilled", "isSecondary", "isVertical", "isBox", "hasBorderBottom", "hasSecondaryBorderBottom", "leftScrollAriaLabel", "rightScrollAriaLabel", 'aria-label', "component", "ouiaId", "ouiaSafe", "mountOnEnter", "unmountOnExit", "usePageInsets", "inset", "variant", "expandable", "isExpanded", "defaultIsExpanded", "toggleText", "toggleAriaLabel", "addButtonAriaLabel", "onToggle", "onClose", "onAdd"]);
        const { showScrollButtons, disableLeftScrollButton, disableRightScrollButton, shownKeys, uncontrolledActiveKey, uncontrolledIsExpandedLocal } = this.state;
        const filteredChildren = React.Children.toArray(children)
            .filter(Boolean)
            .filter(child => !child.props.isHidden);
        const uniqueId = id || util_1.getUniqueId();
        const Component = component === TabsComponent.nav ? 'nav' : 'div';
        const localActiveKey = defaultActiveKey !== undefined ? uncontrolledActiveKey : activeKey;
        const isExpandedLocal = defaultIsExpanded !== undefined ? uncontrolledIsExpandedLocal : isExpanded;
        /*  Uncontrolled expandable tabs */
        const toggleTabs = (newValue) => {
            if (isExpanded === undefined) {
                this.setState({ uncontrolledIsExpandedLocal: newValue });
            }
            else {
                onToggle(newValue);
            }
        };
        return (React.createElement(TabsContext_1.TabsContextProvider, { value: {
                variant,
                mountOnEnter,
                unmountOnExit,
                localActiveKey,
                uniqueId,
                handleTabClick: (...args) => this.handleTabClick(...args),
                handleTabClose: onClose
            } },
            React.createElement(Component, Object.assign({ "aria-label": ariaLabel, className: react_styles_1.css(tabs_1.default.tabs, isFilled && tabs_1.default.modifiers.fill, isSecondary && tabs_1.default.modifiers.secondary, isVertical && tabs_1.default.modifiers.vertical, isVertical && expandable && util_1.formatBreakpointMods(expandable, tabs_1.default), isVertical && expandable && isExpandedLocal && tabs_1.default.modifiers.expanded, isBox && tabs_1.default.modifiers.box, showScrollButtons && !isVertical && tabs_1.default.modifiers.scrollable, usePageInsets && tabs_1.default.modifiers.pageInsets, !hasBorderBottom && tabs_1.default.modifiers.noBorderBottom, hasSecondaryBorderBottom && tabs_1.default.modifiers.borderBottom, util_1.formatBreakpointMods(inset, tabs_1.default), variantStyle[variant], className) }, helpers_1.getOUIAProps(Tabs.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), { id: id && id }, props),
                expandable && isVertical && (React.createElement(GenerateId_1.GenerateId, null, randomId => (React.createElement("div", { className: react_styles_1.css(tabs_1.default.tabsToggle) },
                    React.createElement("div", { className: react_styles_1.css(tabs_1.default.tabsToggleButton) },
                        React.createElement(Button_1.Button, { onClick: () => toggleTabs(!isExpandedLocal), variant: "plain", "aria-label": toggleAriaLabel, "aria-expanded": isExpandedLocal, id: `${randomId}-button`, "aria-labelledby": `${randomId}-text ${randomId}-button` },
                            React.createElement("span", { className: react_styles_1.css(tabs_1.default.tabsToggleIcon) },
                                React.createElement(angle_right_icon_1.default, { "arian-hidden": "true" })),
                            toggleText && (React.createElement("span", { className: react_styles_1.css('pf-c-tabs__toggle-text'), id: `${randomId}-text` }, toggleText)))))))),
                React.createElement("button", { className: react_styles_1.css(tabs_1.default.tabsScrollButton, isSecondary && button_1.default.modifiers.secondary), "aria-label": leftScrollAriaLabel, onClick: this.scrollLeft, disabled: disableLeftScrollButton, "aria-hidden": disableLeftScrollButton },
                    React.createElement(angle_left_icon_1.default, null)),
                React.createElement("ul", { className: react_styles_1.css(tabs_1.default.tabsList), ref: this.tabList, onScroll: this.handleScrollButtons, role: "tablist" }, filteredChildren),
                React.createElement("button", { className: react_styles_1.css(tabs_1.default.tabsScrollButton, isSecondary && button_1.default.modifiers.secondary), "aria-label": rightScrollAriaLabel, onClick: this.scrollRight, disabled: disableRightScrollButton, "aria-hidden": disableRightScrollButton },
                    React.createElement(angle_right_icon_1.default, null)),
                onAdd !== undefined && (React.createElement("span", { className: react_styles_1.css(tabs_1.default.tabsAdd) },
                    React.createElement(Button_1.Button, { variant: "plain", "aria-label": addButtonAriaLabel || 'Add tab', onClick: onAdd },
                        React.createElement(plus_icon_1.default, null))))),
            filteredChildren
                .filter(child => child.props.children &&
                !(unmountOnExit && child.props.eventKey !== localActiveKey) &&
                !(mountOnEnter && shownKeys.indexOf(child.props.eventKey) === -1))
                .map(child => (React.createElement(TabContent_1.TabContent, { key: child.props.eventKey, activeKey: localActiveKey, child: child, id: child.props.id || uniqueId, ouiaId: child.props.ouiaId })))));
    }
}
exports.Tabs = Tabs;
Tabs.displayName = 'Tabs';
Tabs.defaultProps = {
    activeKey: 0,
    onSelect: () => undefined,
    isFilled: false,
    isSecondary: false,
    isVertical: false,
    isBox: false,
    hasBorderBottom: true,
    leftScrollAriaLabel: 'Scroll left',
    rightScrollAriaLabel: 'Scroll right',
    component: TabsComponent.div,
    mountOnEnter: false,
    unmountOnExit: false,
    ouiaSafe: true,
    variant: 'default',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle: (isExpanded) => undefined
};
//# sourceMappingURL=Tabs.js.map