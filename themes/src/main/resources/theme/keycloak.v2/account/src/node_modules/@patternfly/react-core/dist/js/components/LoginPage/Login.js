"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Login = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const login_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Login/login"));
const react_styles_1 = require("@patternfly/react-styles");
const Login = (_a) => {
    var { className = '', children = null, footer = null, header = null } = _a, props = tslib_1.__rest(_a, ["className", "children", "footer", "header"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(login_1.default.login, className) }),
        React.createElement("div", { className: react_styles_1.css(login_1.default.loginContainer) },
            header,
            React.createElement("main", { className: react_styles_1.css(login_1.default.loginMain) }, children),
            footer)));
};
exports.Login = Login;
exports.Login.displayName = 'Login';
//# sourceMappingURL=Login.js.map