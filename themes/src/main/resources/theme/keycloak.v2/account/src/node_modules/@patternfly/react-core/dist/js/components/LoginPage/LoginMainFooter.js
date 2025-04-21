"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginMainFooter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const login_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Login/login"));
const LoginMainFooter = (_a) => {
    var { children = null, socialMediaLoginContent = null, signUpForAccountMessage = null, forgotCredentials = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "socialMediaLoginContent", "signUpForAccountMessage", "forgotCredentials", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(login_1.default.loginMainFooter, className) }, props),
        children,
        socialMediaLoginContent && React.createElement("ul", { className: react_styles_1.css(login_1.default.loginMainFooterLinks) }, socialMediaLoginContent),
        (signUpForAccountMessage || forgotCredentials) && (React.createElement("div", { className: react_styles_1.css(login_1.default.loginMainFooterBand) },
            signUpForAccountMessage,
            forgotCredentials))));
};
exports.LoginMainFooter = LoginMainFooter;
exports.LoginMainFooter.displayName = 'LoginMainFooter';
//# sourceMappingURL=LoginMainFooter.js.map