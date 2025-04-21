import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
export const AboutModalBoxBrand = (_a) => {
    var { className = '', src = '', alt } = _a, props = __rest(_a, ["className", "src", "alt"]);
    return (React.createElement("div", Object.assign({ className: css(styles.aboutModalBoxBrand, className) }, props),
        React.createElement("img", { className: css(styles.aboutModalBoxBrandImage), src: src, alt: alt })));
};
AboutModalBoxBrand.displayName = 'AboutModalBoxBrand';
//# sourceMappingURL=AboutModalBoxBrand.js.map