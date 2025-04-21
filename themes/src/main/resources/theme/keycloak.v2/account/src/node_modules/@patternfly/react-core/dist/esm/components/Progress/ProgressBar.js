import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';
export const ProgressBar = (_a) => {
    var { progressBarAriaProps, className = '', children = null, value } = _a, props = __rest(_a, ["progressBarAriaProps", "className", "children", "value"]);
    return (React.createElement("div", Object.assign({}, props, { className: css(styles.progressBar, className) }, progressBarAriaProps),
        React.createElement("div", { className: css(styles.progressIndicator), style: { width: `${value}%` } },
            React.createElement("span", { className: css(styles.progressMeasure) }, children))));
};
ProgressBar.displayName = 'ProgressBar';
//# sourceMappingURL=ProgressBar.js.map