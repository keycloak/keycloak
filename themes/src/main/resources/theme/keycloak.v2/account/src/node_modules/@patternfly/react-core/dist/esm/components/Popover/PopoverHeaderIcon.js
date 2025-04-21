import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
export const PopoverHeaderIcon = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: css(styles.popoverTitleIcon, className) }, props), children));
};
PopoverHeaderIcon.displayName = 'PopoverHeaderIcon';
//# sourceMappingURL=PopoverHeaderIcon.js.map