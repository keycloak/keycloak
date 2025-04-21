import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Banner/banner';
import { css } from '@patternfly/react-styles';
export const Banner = (_a) => {
    var { children, className, variant = 'default', screenReaderText, isSticky = false } = _a, props = __rest(_a, ["children", "className", "variant", "screenReaderText", "isSticky"]);
    return (React.createElement("div", Object.assign({ className: css(styles.banner, styles.modifiers[variant], isSticky && styles.modifiers.sticky, className) }, props),
        children,
        React.createElement("span", { className: "pf-u-screen-reader" }, screenReaderText || `${variant} banner`)));
};
Banner.displayName = 'Banner';
//# sourceMappingURL=Banner.js.map