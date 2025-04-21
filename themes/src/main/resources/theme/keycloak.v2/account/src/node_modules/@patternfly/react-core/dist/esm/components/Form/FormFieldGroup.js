import { __rest } from "tslib";
import * as React from 'react';
import { InternalFormFieldGroup } from './InternalFormFieldGroup';
export const FormFieldGroup = (_a) => {
    var { children, className, header } = _a, props = __rest(_a, ["children", "className", "header"]);
    return (React.createElement(InternalFormFieldGroup, Object.assign({ className: className, header: header }, props), children));
};
FormFieldGroup.displayName = 'FormFieldGroup';
//# sourceMappingURL=FormFieldGroup.js.map