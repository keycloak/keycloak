import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
export const AboutModalBox = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ role: "dialog", "aria-modal": "true", className: css(styles.aboutModalBox, className) }, props), children));
};
AboutModalBox.displayName = 'AboutModalBox';
//# sourceMappingURL=AboutModalBox.js.map