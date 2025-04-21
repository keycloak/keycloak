import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';
import { getUniqueId, isElementInView, formatBreakpointMods } from '../../helpers/util';
import { TabContent } from './TabContent';
import { TabsContextProvider } from './TabsContext';
import { Button } from '../Button';
import { getOUIAProps, getDefaultOUIAId, canUseDOM } from '../../helpers';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
export var TabsComponent;
(function (TabsComponent) {
    TabsComponent["div"] = "div";
    TabsComponent["nav"] = "nav";
})(TabsComponent || (TabsComponent = {}));
const variantStyle = {
    default: '',
    light300: styles.modifiers.colorSchemeLight_300
};
export class Tabs extends React.Component {
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
                    const overflowOnLeft = !isElementInView(container, container.firstChild, false);
                    // get last element and check if it is in view
                    const overflowOnRight = !isElementInView(container, container.lastChild, false);
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
                    if (isElementInView(container, childrenArr[i], false)) {
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
                    if (isElementInView(container, childrenArr[i], false)) {
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
            ouiaStateId: getDefaultOUIAId(Tabs.displayName)
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
            if (canUseDOM) {
                window.addEventListener('resize', this.handleScrollButtons, false);
            }
            // call the handle resize function to check if scroll buttons should be shown
            this.handleScrollButtons();
        }
    }
    componentWillUnmount() {
        if (!this.props.isVertical) {
            if (canUseDOM) {
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
        const _a = this.props, { className, children, activeKey, defaultActiveKey, id, isFilled, isSecondary, isVertical, isBox, hasBorderBottom, hasSecondaryBorderBottom, leftScrollAriaLabel, rightScrollAriaLabel, 'aria-label': ariaLabel, component, ouiaId, ouiaSafe, mountOnEnter, unmountOnExit, usePageInsets, inset, variant, expandable, isExpanded, defaultIsExpanded, toggleText, toggleAriaLabel, addButtonAriaLabel, onToggle, onClose, onAdd } = _a, props = __rest(_a, ["className", "children", "activeKey", "defaultActiveKey", "id", "isFilled", "isSecondary", "isVertical", "isBox", "hasBorderBottom", "hasSecondaryBorderBottom", "leftScrollAriaLabel", "rightScrollAriaLabel", 'aria-label', "component", "ouiaId", "ouiaSafe", "mountOnEnter", "unmountOnExit", "usePageInsets", "inset", "variant", "expandable", "isExpanded", "defaultIsExpanded", "toggleText", "toggleAriaLabel", "addButtonAriaLabel", "onToggle", "onClose", "onAdd"]);
        const { showScrollButtons, disableLeftScrollButton, disableRightScrollButton, shownKeys, uncontrolledActiveKey, uncontrolledIsExpandedLocal } = this.state;
        const filteredChildren = React.Children.toArray(children)
            .filter(Boolean)
            .filter(child => !child.props.isHidden);
        const uniqueId = id || getUniqueId();
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
        return (React.createElement(TabsContextProvider, { value: {
                variant,
                mountOnEnter,
                unmountOnExit,
                localActiveKey,
                uniqueId,
                handleTabClick: (...args) => this.handleTabClick(...args),
                handleTabClose: onClose
            } },
            React.createElement(Component, Object.assign({ "aria-label": ariaLabel, className: css(styles.tabs, isFilled && styles.modifiers.fill, isSecondary && styles.modifiers.secondary, isVertical && styles.modifiers.vertical, isVertical && expandable && formatBreakpointMods(expandable, styles), isVertical && expandable && isExpandedLocal && styles.modifiers.expanded, isBox && styles.modifiers.box, showScrollButtons && !isVertical && styles.modifiers.scrollable, usePageInsets && styles.modifiers.pageInsets, !hasBorderBottom && styles.modifiers.noBorderBottom, hasSecondaryBorderBottom && styles.modifiers.borderBottom, formatBreakpointMods(inset, styles), variantStyle[variant], className) }, getOUIAProps(Tabs.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), { id: id && id }, props),
                expandable && isVertical && (React.createElement(GenerateId, null, randomId => (React.createElement("div", { className: css(styles.tabsToggle) },
                    React.createElement("div", { className: css(styles.tabsToggleButton) },
                        React.createElement(Button, { onClick: () => toggleTabs(!isExpandedLocal), variant: "plain", "aria-label": toggleAriaLabel, "aria-expanded": isExpandedLocal, id: `${randomId}-button`, "aria-labelledby": `${randomId}-text ${randomId}-button` },
                            React.createElement("span", { className: css(styles.tabsToggleIcon) },
                                React.createElement(AngleRightIcon, { "arian-hidden": "true" })),
                            toggleText && (React.createElement("span", { className: css('pf-c-tabs__toggle-text'), id: `${randomId}-text` }, toggleText)))))))),
                React.createElement("button", { className: css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary), "aria-label": leftScrollAriaLabel, onClick: this.scrollLeft, disabled: disableLeftScrollButton, "aria-hidden": disableLeftScrollButton },
                    React.createElement(AngleLeftIcon, null)),
                React.createElement("ul", { className: css(styles.tabsList), ref: this.tabList, onScroll: this.handleScrollButtons, role: "tablist" }, filteredChildren),
                React.createElement("button", { className: css(styles.tabsScrollButton, isSecondary && buttonStyles.modifiers.secondary), "aria-label": rightScrollAriaLabel, onClick: this.scrollRight, disabled: disableRightScrollButton, "aria-hidden": disableRightScrollButton },
                    React.createElement(AngleRightIcon, null)),
                onAdd !== undefined && (React.createElement("span", { className: css(styles.tabsAdd) },
                    React.createElement(Button, { variant: "plain", "aria-label": addButtonAriaLabel || 'Add tab', onClick: onAdd },
                        React.createElement(PlusIcon, null))))),
            filteredChildren
                .filter(child => child.props.children &&
                !(unmountOnExit && child.props.eventKey !== localActiveKey) &&
                !(mountOnEnter && shownKeys.indexOf(child.props.eventKey) === -1))
                .map(child => (React.createElement(TabContent, { key: child.props.eventKey, activeKey: localActiveKey, child: child, id: child.props.id || uniqueId, ouiaId: child.props.ouiaId })))));
    }
}
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