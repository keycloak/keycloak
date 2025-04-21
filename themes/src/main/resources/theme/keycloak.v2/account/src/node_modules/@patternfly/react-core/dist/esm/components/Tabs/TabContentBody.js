import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/TabContent/tab-content';
export const TabContentBody = (_a) => {
    var { children, className, hasPadding } = _a, props = __rest(_a, ["children", "className", "hasPadding"]);
    return (React.createElement("div", Object.assign({ className: css(styles.tabContentBody, hasPadding && styles.modifiers.padding, className) }, props), children));
};
TabContentBody.displayName = 'TabContentBody';
//# sourceMappingURL=TabContentBody.js.map