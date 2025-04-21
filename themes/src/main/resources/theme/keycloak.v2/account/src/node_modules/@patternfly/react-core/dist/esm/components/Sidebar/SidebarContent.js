import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Sidebar/sidebar';
export const SidebarContent = (_a) => {
    var { className, children, hasNoBackground } = _a, props = __rest(_a, ["className", "children", "hasNoBackground"]);
    return (React.createElement("div", Object.assign({ className: css(styles.sidebarContent, hasNoBackground && styles.modifiers.noBackground, className) }, props), children));
};
SidebarContent.displayName = 'SidebarContent';
//# sourceMappingURL=SidebarContent.js.map