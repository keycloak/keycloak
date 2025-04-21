"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginMainBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const login_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Login/login"));
const LoginMainBody = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(login_1.default.loginMainBody, className) }, props), children));
};
exports.LoginMainBody = LoginMainBody;
exports.LoginMainBody.displayName = 'LoginMainBody';
//# sourceMappingURL=LoginMainBody.js.map