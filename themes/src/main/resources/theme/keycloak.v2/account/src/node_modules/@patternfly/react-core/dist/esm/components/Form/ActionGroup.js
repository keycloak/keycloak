import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
export const ActionGroup = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    const customClassName = css(styles.formGroup, styles.modifiers.action, className);
    const formActionsComponent = React.createElement("div", { className: css(styles.formActions) }, children);
    return (React.createElement("div", Object.assign({}, props, { className: customClassName }),
        React.createElement("div", { className: css(styles.formGroupControl) }, formActionsComponent)));
};
ActionGroup.displayName = 'ActionGroup';
//# sourceMappingURL=ActionGroup.js.map