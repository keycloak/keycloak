import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/HelperText/helper-text';
import { css } from '@patternfly/react-styles';
export const HelperText = (_a) => {
    var { children, className, component = 'div', id, isLiveRegion = false } = _a, props = __rest(_a, ["children", "className", "component", "id", "isLiveRegion"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ id: id, className: css(styles.helperText, className) }, (isLiveRegion && { 'aria-live': 'polite' }), props), children));
};
HelperText.displayName = 'HelperText';
//# sourceMappingURL=HelperText.js.map