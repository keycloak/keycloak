import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import { css } from '@patternfly/react-styles';
export const PopoverFooter = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("footer", Object.assign({ className: css(styles.popoverFooter, className) }, props), children));
};
PopoverFooter.displayName = 'PopoverFooter';
//# sourceMappingURL=PopoverFooter.js.map