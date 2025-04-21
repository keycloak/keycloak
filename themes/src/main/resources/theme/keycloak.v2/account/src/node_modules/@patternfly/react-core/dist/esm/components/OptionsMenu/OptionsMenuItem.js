import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { DropdownItem } from '../Dropdown';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
export const OptionsMenuItem = (_a) => {
    var { children = null, isSelected = false, onSelect = () => null, id = '', isDisabled } = _a, props = __rest(_a, ["children", "isSelected", "onSelect", "id", "isDisabled"]);
    return (React.createElement(DropdownItem, Object.assign({ id: id, component: "button", isDisabled: isDisabled, onClick: (event) => onSelect(event) }, (isDisabled && { 'aria-disabled': true }), props),
        children,
        isSelected && (React.createElement("span", { className: css(styles.optionsMenuMenuItemIcon) },
            React.createElement(CheckIcon, { "aria-hidden": isSelected })))));
};
OptionsMenuItem.displayName = 'OptionsMenuItem';
//# sourceMappingURL=OptionsMenuItem.js.map