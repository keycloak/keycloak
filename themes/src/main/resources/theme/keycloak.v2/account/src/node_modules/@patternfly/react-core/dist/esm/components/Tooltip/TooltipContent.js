import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tooltip/tooltip';
import { css } from '@patternfly/react-styles';
export const TooltipContent = (_a) => {
    var { className, children, isLeftAligned } = _a, props = __rest(_a, ["className", "children", "isLeftAligned"]);
    return (React.createElement("div", Object.assign({ className: css(styles.tooltipContent, isLeftAligned && styles.modifiers.textAlignLeft, className) }, props), children));
};
TooltipContent.displayName = 'TooltipContent';
//# sourceMappingURL=TooltipContent.js.map