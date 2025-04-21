import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Page/page';
export const PageBreadcrumb = (_a) => {
    var { className = '', children, isWidthLimited, sticky, hasShadowTop = false, hasShadowBottom = false, hasOverflowScroll = false } = _a, props = __rest(_a, ["className", "children", "isWidthLimited", "sticky", "hasShadowTop", "hasShadowBottom", "hasOverflowScroll"]);
    return (React.createElement("section", Object.assign({ className: css(styles.pageMainBreadcrumb, isWidthLimited && styles.modifiers.limitWidth, sticky === 'top' && styles.modifiers.stickyTop, sticky === 'bottom' && styles.modifiers.stickyBottom, hasShadowTop && styles.modifiers.shadowTop, hasShadowBottom && styles.modifiers.shadowBottom, hasOverflowScroll && styles.modifiers.overflowScroll, className) }, (hasOverflowScroll && { tabIndex: 0 }), props),
        isWidthLimited && React.createElement("div", { className: css(styles.pageMainBody) }, children),
        !isWidthLimited && children));
};
PageBreadcrumb.displayName = 'PageBreadcrumb';
//# sourceMappingURL=PageBreadcrumb.js.map