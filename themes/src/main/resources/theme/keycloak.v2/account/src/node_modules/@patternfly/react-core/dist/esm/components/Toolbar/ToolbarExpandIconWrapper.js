import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
export const ToolbarExpandIconWrapper = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({}, props, { className: css(styles.toolbarExpandAllIcon, className) }), children));
};
ToolbarExpandIconWrapper.displayName = 'ToolbarExpandIconWrapper';
//# sourceMappingURL=ToolbarExpandIconWrapper.js.map