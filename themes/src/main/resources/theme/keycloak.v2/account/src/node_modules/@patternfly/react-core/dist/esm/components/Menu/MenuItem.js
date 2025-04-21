import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import topOffset from '@patternfly/react-tokens/dist/esm/c_menu_m_flyout__menu_top_offset';
import rightOffset from '@patternfly/react-tokens/dist/esm/c_menu_m_flyout__menu_m_left_right_offset';
import leftOffset from '@patternfly/react-tokens/dist/esm/c_menu_m_flyout__menu_left_offset';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { Checkbox } from '../Checkbox';
import { MenuContext, MenuItemContext } from './MenuContext';
import { MenuItemAction } from './MenuItemAction';
import { canUseDOM } from '../../helpers/util';
import { useIsomorphicLayoutEffect } from '../../helpers/useIsomorphicLayout';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
const FlyoutContext = React.createContext({
    direction: 'right'
});
const MenuItemBase = (_a) => {
    var { children, className, itemId = null, to, hasCheck = false, isActive = null, isFavorited = null, isLoadButton = false, isLoading = false, flyoutMenu, direction, description = null, onClick = () => { }, component = 'button', isDisabled = false, isExternalLink = false, isSelected = null, icon, actions, onShowFlyout, drilldownMenu, isOnPath, innerRef } = _a, props = __rest(_a, ["children", "className", "itemId", "to", "hasCheck", "isActive", "isFavorited", "isLoadButton", "isLoading", "flyoutMenu", "direction", "description", "onClick", "component", "isDisabled", "isExternalLink", "isSelected", "icon", "actions", "onShowFlyout", "drilldownMenu", "isOnPath", "innerRef"]);
    const { menuId, parentMenu, onSelect, onActionClick, activeItemId, selected, drilldownItemPath, onDrillIn, onDrillOut, flyoutRef, setFlyoutRef, disableHover } = React.useContext(MenuContext);
    let Component = (to ? 'a' : component);
    if (hasCheck && !to) {
        Component = 'label';
    }
    const [flyoutTarget, setFlyoutTarget] = React.useState(null);
    const flyoutContext = React.useContext(FlyoutContext);
    const [flyoutXDirection, setFlyoutXDirection] = React.useState(flyoutContext.direction);
    const ref = React.useRef();
    const flyoutVisible = ref === flyoutRef;
    const hasFlyout = flyoutMenu !== undefined;
    const showFlyout = (show) => {
        if (!flyoutVisible && show) {
            setFlyoutRef(ref);
        }
        else if (flyoutVisible && !show) {
            setFlyoutRef(null);
        }
        onShowFlyout && show && onShowFlyout();
    };
    useIsomorphicLayoutEffect(() => {
        if (hasFlyout && ref.current && canUseDOM) {
            const flyoutMenu = ref.current.lastElementChild;
            if (flyoutMenu && flyoutMenu.classList.contains(styles.menu)) {
                const origin = ref.current.getClientRects()[0];
                const rect = flyoutMenu.getClientRects()[0];
                if (origin && rect) {
                    const spaceLeftLeft = origin.x - rect.width;
                    const spaceLeftRight = window.innerWidth - origin.x - origin.width - rect.width;
                    let xDir = flyoutXDirection;
                    if (spaceLeftRight < 0 && xDir !== 'left') {
                        setFlyoutXDirection('left');
                        xDir = 'left';
                    }
                    else if (spaceLeftLeft < 0 && xDir !== 'right') {
                        setFlyoutXDirection('right');
                        xDir = 'right';
                    }
                    let xOffset = 0;
                    if (spaceLeftLeft < 0 && spaceLeftRight < 0) {
                        xOffset = xDir === 'right' ? -spaceLeftRight : -spaceLeftLeft;
                    }
                    if (xDir === 'left') {
                        flyoutMenu.classList.add(styles.modifiers.left);
                        flyoutMenu.style.setProperty(rightOffset.name, `-${xOffset}px`);
                    }
                    else {
                        flyoutMenu.style.setProperty(leftOffset.name, `-${xOffset}px`);
                    }
                    const spaceLeftBot = window.innerHeight - origin.y - rect.height;
                    const spaceLeftTop = window.innerHeight - rect.height;
                    if (spaceLeftTop < 0 && spaceLeftBot < 0) {
                        // working idea: page can usually scroll down, but not up
                        // TODO: proper scroll buttons
                    }
                    else if (spaceLeftBot < 0) {
                        flyoutMenu.style.setProperty(topOffset.name, `${spaceLeftBot}px`);
                    }
                }
            }
        }
    }, [flyoutVisible, flyoutMenu]);
    React.useEffect(() => {
        setFlyoutXDirection(flyoutContext.direction);
    }, [flyoutContext]);
    React.useEffect(() => {
        if (flyoutTarget) {
            if (flyoutVisible) {
                const flyoutMenu = flyoutTarget.nextElementSibling;
                const flyoutItems = Array.from(flyoutMenu.getElementsByTagName('UL')[0].children).filter(el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider')));
                flyoutItems[0].firstChild.focus();
            }
            else {
                flyoutTarget.focus();
            }
        }
    }, [flyoutVisible, flyoutTarget]);
    const handleFlyout = (event) => {
        const key = event.key;
        const target = event.target;
        if (key === ' ' || key === 'Enter' || key === 'ArrowRight') {
            event.stopPropagation();
            if (!flyoutVisible) {
                showFlyout(true);
                setFlyoutTarget(target);
            }
        }
        if (key === 'Escape' || key === 'ArrowLeft') {
            if (flyoutVisible) {
                event.stopPropagation();
                showFlyout(false);
            }
        }
    };
    const onItemSelect = (event, onSelect) => {
        // Trigger callback for Menu onSelect
        onSelect && onSelect(event, itemId);
        // Trigger callback for item onClick
        onClick && onClick(event);
    };
    const _isOnPath = (isOnPath && isOnPath) || (drilldownItemPath && drilldownItemPath.includes(itemId)) || false;
    let _drill;
    if (direction) {
        if (direction === 'down') {
            _drill = () => onDrillIn &&
                onDrillIn(menuId, typeof drilldownMenu === 'function'
                    ? drilldownMenu().props.id
                    : drilldownMenu.props.id, itemId);
        }
        else {
            _drill = () => onDrillOut && onDrillOut(parentMenu, itemId);
        }
    }
    let additionalProps = {};
    if (Component === 'a') {
        additionalProps = {
            href: to,
            'aria-disabled': isDisabled ? true : null,
            // prevent invalid 'disabled' attribute on <a> tags
            disabled: null
        };
    }
    else if (Component === 'button') {
        additionalProps = {
            type: 'button'
        };
    }
    if (isOnPath) {
        additionalProps['aria-expanded'] = true;
    }
    else if (hasFlyout) {
        additionalProps['aria-haspopup'] = true;
        additionalProps['aria-expanded'] = flyoutVisible;
    }
    const getAriaCurrent = () => {
        if (isActive !== null) {
            if (isActive) {
                return 'page';
            }
            else {
                return null;
            }
        }
        else if (itemId !== null && activeItemId !== null) {
            return itemId === activeItemId;
        }
        return null;
    };
    const getIsSelected = () => {
        if (isSelected !== null) {
            return isSelected;
        }
        else if (selected !== null && itemId !== null) {
            return (Array.isArray(selected) && selected.includes(itemId)) || itemId === selected;
        }
        return false;
    };
    const onMouseOver = () => {
        if (disableHover) {
            return;
        }
        if (hasFlyout) {
            showFlyout(true);
        }
        else {
            setFlyoutRef(null);
        }
    };
    return (React.createElement("li", Object.assign({ className: css(styles.menuListItem, isDisabled && styles.modifiers.disabled, _isOnPath && styles.modifiers.currentPath, isLoadButton && styles.modifiers.load, isLoading && styles.modifiers.loading, className), onMouseOver: onMouseOver }, (flyoutMenu && { onKeyDown: handleFlyout }), { ref: ref, role: !hasCheck ? 'none' : 'menuitem' }, props),
        React.createElement(GenerateId, null, randomId => (React.createElement(Component, Object.assign({ tabIndex: -1, className: css(styles.menuItem, getIsSelected() && !hasCheck && styles.modifiers.selected, className), "aria-current": getAriaCurrent() }, (!hasCheck && { disabled: isDisabled }), (!hasCheck && { role: 'menuitem' }), { ref: innerRef }, (!hasCheck && {
            onClick: (event) => {
                onItemSelect(event, onSelect);
                _drill && _drill();
            }
        }), (hasCheck && { htmlFor: randomId }), additionalProps),
            React.createElement("span", { className: css(styles.menuItemMain) },
                direction === 'up' && (React.createElement("span", { className: css(styles.menuItemToggleIcon) },
                    React.createElement(AngleLeftIcon, { "aria-hidden": true }))),
                icon && React.createElement("span", { className: css(styles.menuItemIcon) }, icon),
                hasCheck && (React.createElement("span", { className: css('pf-c-menu__item-check') },
                    React.createElement(Checkbox, { id: randomId, component: "span", isChecked: isSelected || false, onChange: event => onItemSelect(event, onSelect), isDisabled: isDisabled }))),
                React.createElement("span", { className: css(styles.menuItemText) }, children),
                isExternalLink && (React.createElement("span", { className: css(styles.menuItemExternalIcon) },
                    React.createElement(ExternalLinkAltIcon, { "aria-hidden": true }))),
                (flyoutMenu || direction === 'down') && (React.createElement("span", { className: css(styles.menuItemToggleIcon) },
                    React.createElement(AngleRightIcon, { "aria-hidden": true }))),
                getIsSelected() && (React.createElement("span", { className: css(styles.menuItemSelectIcon) },
                    React.createElement(CheckIcon, { "aria-hidden": true })))),
            description && direction !== 'up' && (React.createElement("span", { className: css(styles.menuItemDescription) },
                React.createElement("span", null, description)))))),
        flyoutVisible && (React.createElement(MenuContext.Provider, { value: { disableHover } },
            React.createElement(FlyoutContext.Provider, { value: { direction: flyoutXDirection } }, flyoutMenu))),
        typeof drilldownMenu === 'function' ? drilldownMenu() : drilldownMenu,
        React.createElement(MenuItemContext.Provider, { value: { itemId, isDisabled } },
            actions,
            isFavorited !== null && (React.createElement(MenuItemAction, { icon: "favorites", isFavorited: isFavorited, "aria-label": isFavorited ? 'starred' : 'not starred', onClick: event => onActionClick(event, itemId), tabIndex: -1, actionId: "fav" })))));
};
export const MenuItem = React.forwardRef((props, ref) => (React.createElement(MenuItemBase, Object.assign({}, props, { innerRef: ref }))));
MenuItem.displayName = 'MenuItem';
//# sourceMappingURL=MenuItem.js.map