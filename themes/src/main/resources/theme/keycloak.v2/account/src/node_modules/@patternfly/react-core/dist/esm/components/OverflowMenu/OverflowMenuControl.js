import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuControl = (_a) => {
    var { className, children, hasAdditionalOptions } = _a, props = __rest(_a, ["className", "children", "hasAdditionalOptions"]);
    return (React.createElement(OverflowMenuContext.Consumer, null, value => (value.isBelowBreakpoint || hasAdditionalOptions) && (React.createElement("div", Object.assign({ className: css(styles.overflowMenuControl, className) }, props),
        ' ',
        children,
        ' '))));
};
OverflowMenuControl.displayName = 'OverflowMenuControl';
//# sourceMappingURL=OverflowMenuControl.js.map