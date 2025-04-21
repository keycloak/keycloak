import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Split/split';
import { css } from '@patternfly/react-styles';
export const SplitItem = (_a) => {
    var { isFilled = false, className = '', children = null } = _a, props = __rest(_a, ["isFilled", "className", "children"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.splitItem, isFilled && styles.modifiers.fill, className) }), children));
};
SplitItem.displayName = 'SplitItem';
//# sourceMappingURL=SplitItem.js.map