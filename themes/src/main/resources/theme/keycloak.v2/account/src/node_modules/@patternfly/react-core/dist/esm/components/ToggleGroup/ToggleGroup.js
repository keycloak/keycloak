import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ToggleGroup/toggle-group';
import { ToggleGroupItem } from './ToggleGroupItem';
export const ToggleGroup = (_a) => {
    var { className, children, isCompact = false, areAllGroupsDisabled = false, 'aria-label': ariaLabel } = _a, props = __rest(_a, ["className", "children", "isCompact", "areAllGroupsDisabled", 'aria-label']);
    const toggleGroupItemList = React.Children.map(children, child => {
        const childCompName = child.type.name;
        return childCompName !== ToggleGroupItem.name
            ? child
            : React.cloneElement(child, areAllGroupsDisabled ? { isDisabled: true } : {});
    });
    return (React.createElement("div", Object.assign({ className: css(styles.toggleGroup, isCompact && styles.modifiers.compact, className), role: "group", "aria-label": ariaLabel }, props), toggleGroupItemList));
};
ToggleGroup.displayName = 'ToggleGroup';
//# sourceMappingURL=ToggleGroup.js.map