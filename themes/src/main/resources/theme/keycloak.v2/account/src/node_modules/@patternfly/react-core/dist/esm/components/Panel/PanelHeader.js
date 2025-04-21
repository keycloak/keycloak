import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Panel/panel';
import { css } from '@patternfly/react-styles';
export const PanelHeader = (_a) => {
    var { className, children } = _a, props = __rest(_a, ["className", "children"]);
    return (React.createElement("div", Object.assign({ className: css(styles.panelHeader, className) }, props), children));
};
PanelHeader.displayName = 'PanelHeader';
//# sourceMappingURL=PanelHeader.js.map