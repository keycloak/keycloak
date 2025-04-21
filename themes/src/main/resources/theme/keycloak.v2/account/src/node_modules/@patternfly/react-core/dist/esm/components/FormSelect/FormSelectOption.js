import { __rest } from "tslib";
import * as React from 'react';
export const FormSelectOption = (_a) => {
    var { className = '', value = '', isDisabled = false, label, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isPlaceholder = false } = _a, props = __rest(_a, ["className", "value", "isDisabled", "label", "isPlaceholder"]);
    return (React.createElement("option", Object.assign({}, props, { className: className, value: value, disabled: isDisabled }), label));
};
FormSelectOption.displayName = 'FormSelectOption';
//# sourceMappingURL=FormSelectOption.js.map