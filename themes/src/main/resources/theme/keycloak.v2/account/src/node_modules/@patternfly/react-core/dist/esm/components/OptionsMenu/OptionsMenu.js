import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { DropdownContext } from '../Dropdown';
import { DropdownWithContext } from '../Dropdown/DropdownWithContext';
import { useOUIAId } from '../../helpers';
export var OptionsMenuPosition;
(function (OptionsMenuPosition) {
    OptionsMenuPosition["right"] = "right";
    OptionsMenuPosition["left"] = "left";
})(OptionsMenuPosition || (OptionsMenuPosition = {}));
export var OptionsMenuDirection;
(function (OptionsMenuDirection) {
    OptionsMenuDirection["up"] = "up";
    OptionsMenuDirection["down"] = "down";
})(OptionsMenuDirection || (OptionsMenuDirection = {}));
export const OptionsMenu = (_a) => {
    var { className = '', menuItems, toggle, isText = false, isGrouped = false, id, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, menuAppendTo = 'inline', ouiaId, ouiaSafe = true } = _a, props = __rest(_a, ["className", "menuItems", "toggle", "isText", "isGrouped", "id", "ref", "menuAppendTo", "ouiaId", "ouiaSafe"]);
    return (React.createElement(DropdownContext.Provider, { value: {
            id,
            onSelect: () => undefined,
            toggleIndicatorClass: styles.optionsMenuToggleIcon,
            toggleTextClass: styles.optionsMenuToggleText,
            menuClass: styles.optionsMenuMenu,
            itemClass: styles.optionsMenuMenuItem,
            toggleClass: isText ? styles.optionsMenuToggleButton : styles.optionsMenuToggle,
            baseClass: styles.optionsMenu,
            disabledClass: styles.modifiers.disabled,
            menuComponent: isGrouped ? 'div' : 'ul',
            baseComponent: 'div',
            ouiaId: useOUIAId(OptionsMenu.displayName, ouiaId),
            ouiaSafe,
            ouiaComponentType: OptionsMenu.displayName
        } },
        React.createElement(DropdownWithContext, Object.assign({ id: id, dropdownItems: menuItems, className: className, isGrouped: isGrouped, toggle: toggle, menuAppendTo: menuAppendTo }, props))));
};
OptionsMenu.displayName = 'OptionsMenu';
//# sourceMappingURL=OptionsMenu.js.map