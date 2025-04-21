import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
export const TabTitleText = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: css(styles.tabsItemText, className) }, props), children));
};
TabTitleText.displayName = 'TabTitleText';
//# sourceMappingURL=TabTitleText.js.map