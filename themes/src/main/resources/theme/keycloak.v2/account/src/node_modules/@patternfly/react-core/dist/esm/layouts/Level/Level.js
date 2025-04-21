import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Level/level';
export const Level = (_a) => {
    var { hasGutter, className = '', children = null } = _a, props = __rest(_a, ["hasGutter", "className", "children"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.level, hasGutter && styles.modifiers.gutter, className) }), children));
};
Level.displayName = 'Level';
//# sourceMappingURL=Level.js.map