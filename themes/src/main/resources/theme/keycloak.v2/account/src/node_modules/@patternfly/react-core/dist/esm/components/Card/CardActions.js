import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Card/card';
export const CardActions = (_a) => {
    var { children = null, className = '', hasNoOffset = false } = _a, props = __rest(_a, ["children", "className", "hasNoOffset"]);
    return (React.createElement("div", Object.assign({ className: css(styles.cardActions, hasNoOffset && styles.modifiers.noOffset, className) }, props), children));
};
CardActions.displayName = 'CardActions';
//# sourceMappingURL=CardActions.js.map