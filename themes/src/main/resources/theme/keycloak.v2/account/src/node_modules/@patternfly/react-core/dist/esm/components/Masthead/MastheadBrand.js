import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Masthead/masthead';
import { css } from '@patternfly/react-styles';
export const MastheadBrand = (_a) => {
    var { children, className, component = 'a' } = _a, props = __rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.mastheadBrand, className), tabIndex: 0 }, props), children));
};
MastheadBrand.displayName = 'MastheadBrand';
//# sourceMappingURL=MastheadBrand.js.map