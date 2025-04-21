import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import StarIcon from '@patternfly/react-icons/dist/esm/icons/star-icon';
import { MenuContext, MenuItemContext } from './MenuContext';
const MenuItemActionBase = (_a) => {
    var { className = '', icon, onClick, 'aria-label': ariaLabel, isFavorited = null, isDisabled, actionId, innerRef } = _a, props = __rest(_a, ["className", "icon", "onClick", 'aria-label', "isFavorited", "isDisabled", "actionId", "innerRef"]);
    return (React.createElement(MenuContext.Consumer, null, ({ onActionClick }) => (React.createElement(MenuItemContext.Consumer, null, ({ itemId, isDisabled: isDisabledContext }) => {
        const onClickButton = (event) => {
            // event specified on the MenuItemAction
            onClick && onClick(event);
            // event specified on the Menu
            onActionClick && onActionClick(event, itemId, actionId);
        };
        return (React.createElement("button", Object.assign({ className: css(styles.menuItemAction, isFavorited !== null && styles.modifiers.favorite, isFavorited && styles.modifiers.favorited, className), "aria-label": ariaLabel, onClick: onClickButton }, ((isDisabled === true || isDisabledContext === true) && { disabled: true }), { ref: innerRef, tabIndex: -1 }, props),
            React.createElement("span", { className: css(styles.menuItemActionIcon) }, icon === 'favorites' || isFavorited !== null ? React.createElement(StarIcon, { "aria-hidden": true }) : icon)));
    }))));
};
export const MenuItemAction = React.forwardRef((props, ref) => (React.createElement(MenuItemActionBase, Object.assign({}, props, { innerRef: ref }))));
MenuItemAction.displayName = 'MenuItemAction';
//# sourceMappingURL=MenuItemAction.js.map