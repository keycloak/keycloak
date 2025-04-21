import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ToggleGroup/toggle-group';
import { ToggleGroupItemVariant, ToggleGroupItemElement } from './ToggleGroupItemElement';
export const ToggleGroupItem = (_a) => {
    var { text, icon, className, isDisabled = false, isSelected = false, 'aria-label': ariaLabel = '', onChange = () => { }, buttonId = '' } = _a, props = __rest(_a, ["text", "icon", "className", "isDisabled", "isSelected", 'aria-label', "onChange", "buttonId"]);
    const handleChange = (event) => {
        onChange(!isSelected, event);
    };
    if (!ariaLabel && icon && !text) {
        /* eslint-disable no-console */
        console.warn('An accessible aria-label is required when using the toggle group item icon variant.');
    }
    return (React.createElement("div", Object.assign({ className: css(styles.toggleGroupItem, className) }, props),
        React.createElement("button", Object.assign({ type: "button", className: css(styles.toggleGroupButton, isSelected && styles.modifiers.selected), "aria-pressed": isSelected, onClick: handleChange }, (ariaLabel && { 'aria-label': ariaLabel }), (isDisabled && { disabled: true }), (buttonId && { id: buttonId })),
            icon ? React.createElement(ToggleGroupItemElement, { variant: ToggleGroupItemVariant.icon }, icon) : null,
            text ? React.createElement(ToggleGroupItemElement, { variant: ToggleGroupItemVariant.text }, text) : null)));
};
ToggleGroupItem.displayName = 'ToggleGroupItem';
//# sourceMappingURL=ToggleGroupItem.js.map