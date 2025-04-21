import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/JumpLinks/jump-links';
export const JumpLinksList = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("ul", Object.assign({ className: css(styles.jumpLinksList, className) }, props), children));
};
JumpLinksList.displayName = 'JumpLinksList';
//# sourceMappingURL=JumpLinksList.js.map