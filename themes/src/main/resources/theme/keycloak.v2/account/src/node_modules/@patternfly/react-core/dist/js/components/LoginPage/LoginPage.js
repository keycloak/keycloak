"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginPage = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const BackgroundImage_1 = require("../BackgroundImage");
const Brand_1 = require("../Brand");
const List_1 = require("../List");
const Login_1 = require("./Login");
const LoginHeader_1 = require("./LoginHeader");
const LoginFooter_1 = require("./LoginFooter");
const LoginMainHeader_1 = require("./LoginMainHeader");
const LoginMainBody_1 = require("./LoginMainBody");
const LoginMainFooter_1 = require("./LoginMainFooter");
const LoginPage = (_a) => {
    var { children = null, className = '', brandImgSrc = '', brandImgAlt = '', backgroundImgSrc = '', backgroundImgAlt = '', footerListItems = null, textContent = '', footerListVariants, loginTitle, loginSubtitle, signUpForAccountMessage = null, forgotCredentials = null, socialMediaLoginContent = null } = _a, props = tslib_1.__rest(_a, ["children", "className", "brandImgSrc", "brandImgAlt", "backgroundImgSrc", "backgroundImgAlt", "footerListItems", "textContent", "footerListVariants", "loginTitle", "loginSubtitle", "signUpForAccountMessage", "forgotCredentials", "socialMediaLoginContent"]);
    const HeaderBrand = (React.createElement(React.Fragment, null,
        React.createElement(Brand_1.Brand, { src: brandImgSrc, alt: brandImgAlt })));
    const Header = React.createElement(LoginHeader_1.LoginHeader, { headerBrand: HeaderBrand });
    const Footer = (React.createElement(LoginFooter_1.LoginFooter, null,
        React.createElement("p", null, textContent),
        React.createElement(List_1.List, { variant: footerListVariants }, footerListItems)));
    return (React.createElement(React.Fragment, null,
        backgroundImgSrc && React.createElement(BackgroundImage_1.BackgroundImage, { src: backgroundImgSrc, alt: backgroundImgAlt }),
        React.createElement(Login_1.Login, Object.assign({ header: Header, footer: Footer, className: react_styles_1.css(className) }, props),
            React.createElement(LoginMainHeader_1.LoginMainHeader, { title: loginTitle, subtitle: loginSubtitle }),
            React.createElement(LoginMainBody_1.LoginMainBody, null, children),
            (socialMediaLoginContent || forgotCredentials || signUpForAccountMessage) && (React.createElement(LoginMainFooter_1.LoginMainFooter, { socialMediaLoginContent: socialMediaLoginContent, forgotCredentials: forgotCredentials, signUpForAccountMessage: signUpForAccountMessage })))));
};
exports.LoginPage = LoginPage;
exports.LoginPage.displayName = 'LoginPage';
//# sourceMappingURL=LoginPage.js.map