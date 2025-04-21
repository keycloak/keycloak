"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertIcon = exports.variantIcons = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const alert_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Alert/alert"));
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const exclamation_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-circle-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const info_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/info-circle-icon'));
const bell_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/bell-icon'));
exports.variantIcons = {
    success: check_circle_icon_1.default,
    danger: exclamation_circle_icon_1.default,
    warning: exclamation_triangle_icon_1.default,
    info: info_circle_icon_1.default,
    default: bell_icon_1.default
};
const AlertIcon = (_a) => {
    var { variant, customIcon, className = '' } = _a, props = tslib_1.__rest(_a, ["variant", "customIcon", "className"]);
    const Icon = exports.variantIcons[variant];
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(alert_1.default.alertIcon, className) }), customIcon || React.createElement(Icon, null)));
};
exports.AlertIcon = AlertIcon;
//# sourceMappingURL=AlertIcon.js.map