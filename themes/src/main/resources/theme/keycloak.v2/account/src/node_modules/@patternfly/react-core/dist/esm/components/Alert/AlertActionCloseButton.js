import { __rest } from "tslib";
import * as React from 'react';
import { Button, ButtonVariant } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { AlertContext } from './AlertContext';
export const AlertActionCloseButton = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', onClose = () => undefined, 'aria-label': ariaLabel = '', variantLabel } = _a, props = __rest(_a, ["className", "onClose", 'aria-label', "variantLabel"]);
    return (React.createElement(AlertContext.Consumer, null, ({ title, variantLabel: alertVariantLabel }) => (React.createElement(Button, Object.assign({ variant: ButtonVariant.plain, onClick: onClose, "aria-label": ariaLabel === '' ? `Close ${variantLabel || alertVariantLabel} alert: ${title}` : ariaLabel }, props),
        React.createElement(TimesIcon, null)))));
};
AlertActionCloseButton.displayName = 'AlertActionCloseButton';
//# sourceMappingURL=AlertActionCloseButton.js.map