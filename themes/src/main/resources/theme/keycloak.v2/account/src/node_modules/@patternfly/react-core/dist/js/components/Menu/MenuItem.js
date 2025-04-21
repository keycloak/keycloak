"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Menu/menu"));
const react_styles_1 = require("@patternfly/react-styles");
const c_menu_m_flyout__menu_top_offset_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_menu_m_flyout__menu_top_offset'));
const c_menu_m_flyout__menu_m_left_right_offset_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_menu_m_flyout__menu_m_left_right_offset'));
const c_menu_m_flyout__menu_left_offset_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_menu_m_flyout__menu_left_offset'));
const external_link_alt_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/external-link-alt-icon'));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const angle_left_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-left-icon'));
const check_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-icon'));
const Checkbox_1 = require("../Checkbox");
const MenuContext_1 = require("./MenuContext");
const MenuItemAction_1 = require("./MenuItemAction");
const util_1 = require("../../helpers/util");
const useIsomorphicLayout_1 = require("../../helpers/useIsomorphicLayout");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
const FlyoutContext = React.createContext({
    direction: 'right'
});
const MenuItemBase = (_a) => {
    var { children, className, itemId = null, to, hasCheck = false, isActive = null, isFavorited = null, isLoadButton = false, isLoading = false, flyoutMenu, direction, description = null, onClick = () => { }, component = 'button', isDisabled = false, isExternalLink = false, isSelected = null, icon, actions, onShowFlyout, drilldownMenu, isOnPath, innerRef } = _a, props = tslib_1.__rest(_a, ["children", "className", "itemId", "to", "hasCheck", "isActive", "isFavorited", "isLoadButton", "isLoading", "flyoutMenu", "direction", "description", "onClick", "component", "isDisabled", "isExternalLink", "isSelected", "icon", "actions", "onShowFlyout", "drilldownMenu", "isOnPath", "innerRef"]);
    const { menuId, parentMenu, onSelect, onActionClick, activeItemId, selected, drilldownItemPath, onDrillIn, onDrillOut, flyoutRef, setFlyoutRef, disableHover } = React.useContext(MenuContext_1.MenuContext);
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
    useIsomorphicLayout_1.useIsomorphicLayoutEffect(() => {
        if (hasFlyout && ref.current && util_1.canUseDOM) {
            const flyoutMenu = ref.current.lastElementChild;
            if (flyoutMenu && flyoutMenu.classList.contains(menu_1.default.menu)) {
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
                        flyoutMenu.classList.add(menu_1.default.modifiers.left);
                        flyoutMenu.style.setProperty(c_menu_m_flyout__menu_m_left_right_offset_1.default.name, `-${xOffset}px`);
                    }
                    else {
                        flyoutMenu.style.setProperty(c_menu_m_flyout__menu_left_offset_1.default.name, `-${xOffset}px`);
                    }
                    const spaceLeftBot = window.innerHeight - origin.y - rect.height;
                    const spaceLeftTop = window.innerHeight - rect.height;
                    if (spaceLeftTop < 0 && spaceLeftBot < 0) {
                        // working idea: page can usually scroll down, but not up
                        // TODO: proper scroll buttons
                    }
                    else if (spaceLeftBot < 0) {
                        flyoutMenu.style.setProperty(c_menu_m_flyout__menu_top_offset_1.default.name, `${spaceLeftBot}px`);
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
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(menu_1.default.menuListItem, isDisabled && menu_1.default.modifiers.disabled, _isOnPath && menu_1.default.modifiers.currentPath, isLoadButton && menu_1.default.modifiers.load, isLoading && menu_1.default.modifiers.loading, className), onMouseOver: onMouseOver }, (flyoutMenu && { onKeyDown: handleFlyout }), { ref: ref, role: !hasCheck ? 'none' : 'menuitem' }, props),
        React.createElement(GenerateId_1.GenerateId, null, randomId => (React.createElement(Component, Object.assign({ tabIndex: -1, className: react_styles_1.css(menu_1.default.menuItem, getIsSelected() && !hasCheck && menu_1.default.modifiers.selected, className), "aria-current": getAriaCurrent() }, (!hasCheck && { disabled: isDisabled }), (!hasCheck && { role: 'menuitem' }), { ref: innerRef }, (!hasCheck && {
            onClick: (event) => {
                onItemSelect(event, onSelect);
                _drill && _drill();
            }
        }), (hasCheck && { htmlFor: randomId }), additionalProps),
            React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemMain) },
                direction === 'up' && (React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemToggleIcon) },
                    React.createElement(angle_left_icon_1.default, { "aria-hidden": true }))),
                icon && React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemIcon) }, icon),
                hasCheck && (React.createElement("span", { className: react_styles_1.css('pf-c-menu__item-check') },
                    React.createElement(Checkbox_1.Checkbox, { id: randomId, component: "span", isChecked: isSelected || false, onChange: event => onItemSelect(event, onSelect), isDisabled: isDisabled }))),
                React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemText) }, children),
                isExternalLink && (React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemExternalIcon) },
                    React.createElement(external_link_alt_icon_1.default, { "aria-hidden": true }))),
                (flyoutMenu || direction === 'down') && (React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemToggleIcon) },
                    React.createElement(angle_right_icon_1.default, { "aria-hidden": true }))),
                getIsSelected() && (React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemSelectIcon) },
                    React.createElement(check_icon_1.default, { "aria-hidden": true })))),
            description && direction !== 'up' && (React.createElement("span", { className: react_styles_1.css(menu_1.default.menuItemDescription) },
                React.createElement("span", null, description)))))),
        flyoutVisible && (React.createElement(MenuContext_1.MenuContext.Provider, { value: { disableHover } },
            React.createElement(FlyoutContext.Provider, { value: { direction: flyoutXDirection } }, flyoutMenu))),
        typeof drilldownMenu === 'function' ? drilldownMenu() : drilldownMenu,
        React.createElement(MenuContext_1.MenuItemContext.Provider, { value: { itemId, isDisabled } },
            actions,
            isFavorited !== null && (React.createElement(MenuItemAction_1.MenuItemAction, { icon: "favorites", isFavorited: isFavorited, "aria-label": isFavorited ? 'starred' : 'not starred', onClick: event => onActionClick(event, itemId), tabIndex: -1, actionId: "fav" })))));
};
exports.MenuItem = React.forwardRef((props, ref) => (React.createElement(MenuItemBase, Object.assign({}, props, { innerRef: ref }))));
exports.MenuItem.displayName = 'MenuItem';
//# sourceMappingURL=MenuItem.js.map