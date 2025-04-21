import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import { css } from '@patternfly/react-styles';
export const PopoverContent = (_a) => {
    var { className = null, children } = _a, props = __rest(_a, ["className", "children"]);
    return (React.createElement("div", Object.assign({ className: css(styles.popoverContent, className) }, props), children));
};
PopoverContent.displayName = 'PopoverContent';
//# sourceMappingURL=PopoverContent.js.map