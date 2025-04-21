import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { NavContext } from './Nav';
import { PageSidebarContext } from '../Page/PageSidebar';
import { useOUIAProps } from '../../helpers';
import { Popper } from '../../helpers/Popper/Popper';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
export const NavItem = (_a) => {
    var { children, styleChildren = true, className, to, isActive = false, groupId = null, itemId = null, preventDefault = false, onClick = null, component = 'a', flyout, onShowFlyout, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["children", "styleChildren", "className", "to", "isActive", "groupId", "itemId", "preventDefault", "onClick", "component", "flyout", "onShowFlyout", "ouiaId", "ouiaSafe"]);
    const { flyoutRef, setFlyoutRef } = React.useContext(NavContext);
    const { isNavOpen } = React.useContext(PageSidebarContext);
    const [flyoutTarget, setFlyoutTarget] = React.useState(null);
    const [isHovered, setIsHovered] = React.useState(false);
    const ref = React.useRef();
    const flyoutVisible = ref === flyoutRef;
    const popperRef = React.useRef();
    const Component = component;
    const hasFlyout = flyout !== undefined;
    const showFlyout = (show, override) => {
        if ((!flyoutVisible || override) && show) {
            setFlyoutRef(ref);
        }
        else if ((flyoutVisible || override) && !show) {
            setFlyoutRef(null);
        }
        onShowFlyout && show && onShowFlyout();
    };
    const onMouseOver = (event) => {
        const evtContainedInFlyout = event.target.closest('.pf-c-nav__item.pf-m-flyout');
        if (hasFlyout && !flyoutVisible) {
            showFlyout(true);
        }
        else if (flyoutRef !== null && !evtContainedInFlyout) {
            setFlyoutRef(null);
        }
    };
    const onFlyoutClick = (event) => {
        const target = event.target;
        const closestItem = target.closest('.pf-m-flyout');
        if (!closestItem) {
            if (hasFlyout) {
                showFlyout(false, true);
            }
            else if (flyoutRef !== null) {
                setFlyoutRef(null);
            }
        }
    };
    const handleFlyout = (event) => {
        var _a, _b;
        const key = event.key;
        const target = event.target;
        if (!(((_a = popperRef === null || popperRef === void 0 ? void 0 : popperRef.current) === null || _a === void 0 ? void 0 : _a.contains(target)) || (hasFlyout && ((_b = ref === null || ref === void 0 ? void 0 : ref.current) === null || _b === void 0 ? void 0 : _b.contains(target))))) {
            return;
        }
        if (key === ' ' || key === 'ArrowRight') {
            event.stopPropagation();
            event.preventDefault();
            if (!flyoutVisible) {
                showFlyout(true);
                setFlyoutTarget(target);
            }
        }
        if (key === 'Escape' || key === 'ArrowLeft') {
            if (flyoutVisible) {
                event.stopPropagation();
                event.preventDefault();
                showFlyout(false);
            }
        }
    };
    React.useEffect(() => {
        if (hasFlyout) {
            window.addEventListener('click', onFlyoutClick);
        }
        return () => {
            if (hasFlyout) {
                window.removeEventListener('click', onFlyoutClick);
            }
        };
    }, []);
    React.useEffect(() => {
        if (flyoutTarget) {
            if (flyoutVisible) {
                const flyoutItems = Array.from(popperRef.current.getElementsByTagName('UL')[0].children).filter(el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider')));
                flyoutItems[0].firstChild.focus();
            }
            else {
                flyoutTarget.focus();
            }
        }
    }, [flyoutVisible, flyoutTarget]);
    const flyoutButton = (React.createElement("span", { className: css(styles.navToggle) },
        React.createElement("span", { className: css(styles.navToggleIcon) },
            React.createElement(AngleRightIcon, { "aria-hidden": true }))));
    const renderDefaultLink = (context) => {
        const preventLinkDefault = preventDefault || !to;
        return (React.createElement(Component, Object.assign({ href: to, onClick: (e) => context.onSelect(e, groupId, itemId, to, preventLinkDefault, onClick), className: css(styles.navLink, isActive && styles.modifiers.current, isHovered && styles.modifiers.hover, className), "aria-current": isActive ? 'page' : null, tabIndex: isNavOpen ? null : '-1' }, props),
            children,
            flyout && flyoutButton));
    };
    const renderClonedChild = (context, child) => React.cloneElement(child, Object.assign(Object.assign({ onClick: (e) => context.onSelect(e, groupId, itemId, to, preventDefault, onClick), 'aria-current': isActive ? 'page' : null }, (styleChildren && {
        className: css(styles.navLink, isActive && styles.modifiers.current, child.props && child.props.className)
    })), { tabIndex: child.props.tabIndex || isNavOpen ? null : -1, children: hasFlyout ? (React.createElement(React.Fragment, null,
            child.props.children,
            flyoutButton)) : (child.props.children) }));
    const ouiaProps = useOUIAProps(NavItem.displayName, ouiaId, ouiaSafe);
    const handleMouseEnter = () => {
        setIsHovered(true);
    };
    const handleMouseLeave = () => {
        setIsHovered(false);
    };
    const flyoutPopper = (React.createElement(Popper, { reference: ref, popper: React.createElement("div", { ref: popperRef, onMouseEnter: handleMouseEnter, onMouseLeave: handleMouseLeave }, flyout), placement: "right-start", isVisible: flyoutVisible, onDocumentKeyDown: handleFlyout }));
    const navItem = (React.createElement(React.Fragment, null,
        React.createElement("li", Object.assign({ onMouseOver: onMouseOver, className: css(styles.navItem, hasFlyout && styles.modifiers.flyout, className), ref: ref }, ouiaProps),
            React.createElement(NavContext.Consumer, null, context => React.isValidElement(children)
                ? renderClonedChild(context, children)
                : renderDefaultLink(context))),
        flyout && flyoutPopper));
    return navItem;
};
NavItem.displayName = 'NavItem';
//# sourceMappingURL=NavItem.js.map