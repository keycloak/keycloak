import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
export const NotificationDrawerList = (_a) => {
    var { children, className = '', isHidden = false } = _a, props = __rest(_a, ["children", "className", "isHidden"]);
    return (React.createElement("ul", Object.assign({}, props, { className: css('pf-c-notification-drawer__list', className), hidden: isHidden }), children));
};
NotificationDrawerList.displayName = 'NotificationDrawerList';
//# sourceMappingURL=NotificationDrawerList.js.map