"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertActionCloseButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../Button");
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const AlertContext_1 = require("./AlertContext");
const AlertActionCloseButton = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', onClose = () => undefined, 'aria-label': ariaLabel = '', variantLabel } = _a, props = tslib_1.__rest(_a, ["className", "onClose", 'aria-label', "variantLabel"]);
    return (React.createElement(AlertContext_1.AlertContext.Consumer, null, ({ title, variantLabel: alertVariantLabel }) => (React.createElement(Button_1.Button, Object.assign({ variant: Button_1.ButtonVariant.plain, onClick: onClose, "aria-label": ariaLabel === '' ? `Close ${variantLabel || alertVariantLabel} alert: ${title}` : ariaLabel }, props),
        React.createElement(times_icon_1.default, null)))));
};
exports.AlertActionCloseButton = AlertActionCloseButton;
exports.AlertActionCloseButton.displayName = 'AlertActionCloseButton';
//# sourceMappingURL=AlertActionCloseButton.js.map