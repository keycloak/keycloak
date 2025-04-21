import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Masthead/masthead';
import { css } from '@patternfly/react-styles';
export const MastheadToggle = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: css(styles.mastheadToggle, className) }, props), children));
};
MastheadToggle.displayName = 'MastheadToggle';
//# sourceMappingURL=MastheadToggle.js.map