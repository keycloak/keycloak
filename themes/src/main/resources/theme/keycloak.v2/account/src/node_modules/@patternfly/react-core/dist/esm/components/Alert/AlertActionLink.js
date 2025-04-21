import { __rest } from "tslib";
import * as React from 'react';
import { Button, ButtonVariant } from '../Button';
export const AlertActionLink = (_a) => {
    var { className = '', children } = _a, props = __rest(_a, ["className", "children"]);
    return (React.createElement(Button, Object.assign({ variant: ButtonVariant.link, isInline: true, className: className }, props), children));
};
AlertActionLink.displayName = 'AlertActionLink';
//# sourceMappingURL=AlertActionLink.js.map