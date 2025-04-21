"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertToggleExpandButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../Button");
const AlertContext_1 = require("./AlertContext");
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const react_styles_1 = require("@patternfly/react-styles");
const alert_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Alert/alert"));
const AlertToggleExpandButton = (_a) => {
    var { 'aria-label': ariaLabel, variantLabel, onToggleExpand, isExpanded } = _a, props = tslib_1.__rest(_a, ['aria-label', "variantLabel", "onToggleExpand", "isExpanded"]);
    const { title, variantLabel: alertVariantLabel } = React.useContext(AlertContext_1.AlertContext);
    return (React.createElement(Button_1.Button, Object.assign({ variant: Button_1.ButtonVariant.plain, onClick: onToggleExpand, "aria-expanded": isExpanded, "aria-label": ariaLabel === '' ? `Toggle ${variantLabel || alertVariantLabel} alert: ${title}` : ariaLabel }, props),
        React.createElement("span", { className: react_styles_1.css(alert_1.default.alertToggleIcon) },
            React.createElement(angle_right_icon_1.default, { "aria-hidden": "true" }))));
};
exports.AlertToggleExpandButton = AlertToggleExpandButton;
exports.AlertToggleExpandButton.displayName = 'AlertToggleExpandButton';
//# sourceMappingURL=AlertToggleExpandButton.js.map