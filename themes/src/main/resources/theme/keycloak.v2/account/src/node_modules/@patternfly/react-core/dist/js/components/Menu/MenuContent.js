"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Menu/menu"));
const react_styles_1 = require("@patternfly/react-styles");
const MenuContext_1 = require("./MenuContext");
exports.MenuContent = React.forwardRef((props, ref) => {
    const { getHeight, children, menuHeight, maxMenuHeight } = props, rest = tslib_1.__rest(props, ["getHeight", "children", "menuHeight", "maxMenuHeight"]);
    const menuContentRef = React.createRef();
    const refCallback = (el, menuId, onGetMenuHeight) => {
        if (el) {
            let clientHeight = el.clientHeight;
            // if this menu is a submenu, we need to account for the root menu list's padding and root menu content's border.
            let rootMenuList = null;
            let parentEl = el.closest(`.${menu_1.default.menuList}`);
            while (parentEl !== null && parentEl.nodeType === 1) {
                if (parentEl.classList.contains(menu_1.default.menuList)) {
                    rootMenuList = parentEl;
                }
                parentEl = parentEl.parentElement;
            }
            if (rootMenuList) {
                const rootMenuListStyles = getComputedStyle(rootMenuList);
                const rootMenuListPaddingOffset = parseFloat(rootMenuListStyles.getPropertyValue('padding-top').replace(/px/g, '')) +
                    parseFloat(rootMenuListStyles.getPropertyValue('padding-bottom').replace(/px/g, '')) +
                    parseFloat(getComputedStyle(rootMenuList.parentElement)
                        .getPropertyValue('border-bottom-width')
                        .replace(/px/g, ''));
                clientHeight = clientHeight + rootMenuListPaddingOffset;
            }
            onGetMenuHeight && onGetMenuHeight(menuId, clientHeight);
            getHeight && getHeight(clientHeight.toString());
        }
        return ref || menuContentRef;
    };
    return (React.createElement(MenuContext_1.MenuContext.Consumer, null, ({ menuId, onGetMenuHeight }) => (React.createElement("div", Object.assign({}, rest, { className: react_styles_1.css(menu_1.default.menuContent, props.className), ref: el => refCallback(el, menuId, onGetMenuHeight), style: Object.assign(Object.assign({}, (menuHeight && { '--pf-c-menu__content--Height': menuHeight })), (maxMenuHeight && { '--pf-c-menu__content--MaxHeight': maxMenuHeight })) }), children))));
});
exports.MenuContent.displayName = 'MenuContent';
//# sourceMappingURL=MenuContent.js.map