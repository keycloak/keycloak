import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Hint/hint';
import { css } from '@patternfly/react-styles';
export const Hint = (_a) => {
    var { children, className, actions } = _a, props = __rest(_a, ["children", "className", "actions"]);
    return (React.createElement("div", Object.assign({ className: css(styles.hint, className) }, props),
        React.createElement("div", { className: css(styles.hintActions) }, actions),
        children));
};
Hint.displayName = 'Hint';
//# sourceMappingURL=Hint.js.map