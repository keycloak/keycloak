import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Masthead/masthead';
import { css } from '@patternfly/react-styles';
export const MastheadMain = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.mastheadMain, className) }, props), children));
};
MastheadMain.displayName = 'MastheadMain';
//# sourceMappingURL=MastheadMain.js.map