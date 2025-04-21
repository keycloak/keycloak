import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import { css } from '@patternfly/react-styles';
export const PopoverPosition = {
    top: 'top',
    bottom: 'bottom',
    left: 'left',
    right: 'right'
};
export const PopoverDialog = (_a) => {
    var { position = 'top', children = null, className = null } = _a, props = __rest(_a, ["position", "children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.popover, styles.modifiers[position] || styles.modifiers.top, className), role: "dialog", "aria-modal": "true" }, props), children));
};
PopoverDialog.displayName = 'PopoverDialog';
//# sourceMappingURL=PopoverDialog.js.map