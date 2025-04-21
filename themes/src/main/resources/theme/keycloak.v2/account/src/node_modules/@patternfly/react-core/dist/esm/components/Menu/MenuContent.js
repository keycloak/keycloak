import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import { MenuContext } from './MenuContext';
export const MenuContent = React.forwardRef((props, ref) => {
    const { getHeight, children, menuHeight, maxMenuHeight } = props, rest = __rest(props, ["getHeight", "children", "menuHeight", "maxMenuHeight"]);
    const menuContentRef = React.createRef();
    const refCallback = (el, menuId, onGetMenuHeight) => {
        if (el) {
            let clientHeight = el.clientHeight;
            // if this menu is a submenu, we need to account for the root menu list's padding and root menu content's border.
            let rootMenuList = null;
            let parentEl = el.closest(`.${styles.menuList}`);
            while (parentEl !== null && parentEl.nodeType === 1) {
                if (parentEl.classList.contains(styles.menuList)) {
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
    return (React.createElement(MenuContext.Consumer, null, ({ menuId, onGetMenuHeight }) => (React.createElement("div", Object.assign({}, rest, { className: css(styles.menuContent, props.className), ref: el => refCallback(el, menuId, onGetMenuHeight), style: Object.assign(Object.assign({}, (menuHeight && { '--pf-c-menu__content--Height': menuHeight })), (maxMenuHeight && { '--pf-c-menu__content--MaxHeight': maxMenuHeight })) }), children))));
});
MenuContent.displayName = 'MenuContent';
//# sourceMappingURL=MenuContent.js.map