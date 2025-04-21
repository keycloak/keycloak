import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
export const FormAlert = (_a) => {
    var { children = null, className = '' } = _a, props = __rest(_a, ["children", "className"]);
    return (
    // There are currently no associated styles with the pf-c-form_alert class.
    // Therefore, it does not exist in react-styles
    React.createElement("div", Object.assign({}, props, { className: css('pf-c-form__alert', className) }), children));
};
FormAlert.displayName = 'FormAlert';
//# sourceMappingURL=FormAlert.js.map