"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertActionLink = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Button_1 = require("../Button");
const AlertActionLink = (_a) => {
    var { className = '', children } = _a, props = tslib_1.__rest(_a, ["className", "children"]);
    return (React.createElement(Button_1.Button, Object.assign({ variant: Button_1.ButtonVariant.link, isInline: true, className: className }, props), children));
};
exports.AlertActionLink = AlertActionLink;
exports.AlertActionLink.displayName = 'AlertActionLink';
//# sourceMappingURL=AlertActionLink.js.map