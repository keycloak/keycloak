import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Panel/panel';
import { css } from '@patternfly/react-styles';
export const PanelMain = (_a) => {
    var { className, children, maxHeight } = _a, props = __rest(_a, ["className", "children", "maxHeight"]);
    return (React.createElement("div", Object.assign({ className: css(styles.panelMain, className), style: { '--pf-c-panel__main--MaxHeight': maxHeight } }, props), children));
};
PanelMain.displayName = 'PanelMain';
//# sourceMappingURL=PanelMain.js.map