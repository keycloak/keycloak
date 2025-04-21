import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import { css } from '@patternfly/react-styles';
export const PopoverBody = (_a) => {
    var { children, id, className } = _a, props = __rest(_a, ["children", "id", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.popoverBody, className), id: id }, props), children));
};
PopoverBody.displayName = 'PopoverBody';
//# sourceMappingURL=PopoverBody.js.map