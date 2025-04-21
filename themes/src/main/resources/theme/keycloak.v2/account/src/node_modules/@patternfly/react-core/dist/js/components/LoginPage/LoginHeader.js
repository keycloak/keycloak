"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const login_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Login/login"));
const react_styles_1 = require("@patternfly/react-styles");
const LoginHeader = (_a) => {
    var { className = '', children = null, headerBrand = null } = _a, props = tslib_1.__rest(_a, ["className", "children", "headerBrand"]);
    return (React.createElement("header", Object.assign({ className: react_styles_1.css(login_1.default.loginHeader, className) }, props),
        headerBrand,
        children));
};
exports.LoginHeader = LoginHeader;
exports.LoginHeader.displayName = 'LoginHeader';
//# sourceMappingURL=LoginHeader.js.map