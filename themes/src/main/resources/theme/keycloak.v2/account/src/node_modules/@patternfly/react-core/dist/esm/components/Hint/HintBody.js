import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Hint/hint';
import { css } from '@patternfly/react-styles';
export const HintBody = (_a) => {
    var { children, className } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.hintBody, className) }, props), children));
};
HintBody.displayName = 'HintBody';
//# sourceMappingURL=HintBody.js.map