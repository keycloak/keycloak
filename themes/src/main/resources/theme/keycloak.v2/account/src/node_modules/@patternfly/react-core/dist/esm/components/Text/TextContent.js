import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Content/content';
import { css } from '@patternfly/react-styles';
export const TextContent = (_a) => {
    var { children = null, className = '', isVisited = false } = _a, props = __rest(_a, ["children", "className", "isVisited"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.content, isVisited && styles.modifiers.visited, className) }), children));
};
TextContent.displayName = 'TextContent';
//# sourceMappingURL=TextContent.js.map