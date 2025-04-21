import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuGroup = (_a) => {
    var { className, children, isPersistent = false, groupType } = _a, props = __rest(_a, ["className", "children", "isPersistent", "groupType"]);
    return (React.createElement(OverflowMenuContext.Consumer, null, value => (isPersistent || !value.isBelowBreakpoint) && (React.createElement("div", Object.assign({ className: css(styles.overflowMenuGroup, groupType === 'button' && styles.modifiers.buttonGroup, groupType === 'icon' && styles.modifiers.iconButtonGroup, className) }, props), children))));
};
OverflowMenuGroup.displayName = 'OverflowMenuGroup';
//# sourceMappingURL=OverflowMenuGroup.js.map