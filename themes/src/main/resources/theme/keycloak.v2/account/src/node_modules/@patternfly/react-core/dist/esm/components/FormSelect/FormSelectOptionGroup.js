import { __rest } from "tslib";
import * as React from 'react';
export const FormSelectOptionGroup = (_a) => {
    var { children = null, className = '', isDisabled = false, label } = _a, props = __rest(_a, ["children", "className", "isDisabled", "label"]);
    return (React.createElement("optgroup", Object.assign({}, props, { disabled: !!isDisabled, className: className, label: label }), children));
};
FormSelectOptionGroup.displayName = 'FormSelectOptionGroup';
//# sourceMappingURL=FormSelectOptionGroup.js.map