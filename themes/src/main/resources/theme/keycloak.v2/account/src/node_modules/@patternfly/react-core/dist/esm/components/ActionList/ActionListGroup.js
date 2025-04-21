import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ActionList/action-list';
export const ActionListGroup = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css(styles.actionListGroup, className) }, props), children));
};
ActionListGroup.displayName = 'ActionListGroup';
//# sourceMappingURL=ActionListGroup.js.map