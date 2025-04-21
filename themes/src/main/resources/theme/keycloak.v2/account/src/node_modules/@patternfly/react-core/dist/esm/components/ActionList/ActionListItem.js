import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
export const ActionListItem = (_a) => {
    var { children, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: css('pf-c-action-list__item', className) }, props), children));
};
ActionListItem.displayName = 'ActionListItem';
//# sourceMappingURL=ActionListItem.js.map