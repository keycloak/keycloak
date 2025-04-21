import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AlertGroup/alert-group';
export const AlertGroupInline = (_a) => {
    var { className, children, isToast, isLiveRegion, onOverflowClick, overflowMessage } = _a, rest = __rest(_a, ["className", "children", "isToast", "isLiveRegion", "onOverflowClick", "overflowMessage"]);
    return (React.createElement("ul", Object.assign({ "aria-live": isLiveRegion ? 'polite' : null, "aria-atomic": isLiveRegion ? false : null, className: css(styles.alertGroup, className, isToast ? styles.modifiers.toast : '') }, rest),
        React.Children.toArray(children).map((Alert, index) => (React.createElement("li", { key: index }, Alert))),
        overflowMessage && (React.createElement("li", null,
            React.createElement("button", { onClick: onOverflowClick, className: css(styles.alertGroupOverflowButton) }, overflowMessage)))));
};
AlertGroupInline.displayName = 'AlertGroupInline';
//# sourceMappingURL=AlertGroupInline.js.map