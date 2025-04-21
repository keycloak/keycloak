"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginMainHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Title_1 = require("../Title");
const react_styles_1 = require("@patternfly/react-styles");
const login_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Login/login"));
const LoginMainHeader = (_a) => {
    var { children = null, className = '', title = '', subtitle = '' } = _a, props = tslib_1.__rest(_a, ["children", "className", "title", "subtitle"]);
    return (React.createElement("header", Object.assign({ className: react_styles_1.css(login_1.default.loginMainHeader, className) }, props),
        title && (React.createElement(Title_1.Title, { headingLevel: "h2", size: Title_1.TitleSizes['3xl'] }, title)),
        subtitle && React.createElement("p", { className: react_styles_1.css(login_1.default.loginMainHeaderDesc) }, subtitle),
        children));
};
exports.LoginMainHeader = LoginMainHeader;
exports.LoginMainHeader.displayName = 'LoginMainHeader';
//# sourceMappingURL=LoginMainHeader.js.map