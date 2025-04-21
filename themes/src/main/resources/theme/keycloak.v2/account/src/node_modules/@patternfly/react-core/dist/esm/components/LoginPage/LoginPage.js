import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { BackgroundImage } from '../BackgroundImage';
import { Brand } from '../Brand';
import { List } from '../List';
import { Login } from './Login';
import { LoginHeader } from './LoginHeader';
import { LoginFooter } from './LoginFooter';
import { LoginMainHeader } from './LoginMainHeader';
import { LoginMainBody } from './LoginMainBody';
import { LoginMainFooter } from './LoginMainFooter';
export const LoginPage = (_a) => {
    var { children = null, className = '', brandImgSrc = '', brandImgAlt = '', backgroundImgSrc = '', backgroundImgAlt = '', footerListItems = null, textContent = '', footerListVariants, loginTitle, loginSubtitle, signUpForAccountMessage = null, forgotCredentials = null, socialMediaLoginContent = null } = _a, props = __rest(_a, ["children", "className", "brandImgSrc", "brandImgAlt", "backgroundImgSrc", "backgroundImgAlt", "footerListItems", "textContent", "footerListVariants", "loginTitle", "loginSubtitle", "signUpForAccountMessage", "forgotCredentials", "socialMediaLoginContent"]);
    const HeaderBrand = (React.createElement(React.Fragment, null,
        React.createElement(Brand, { src: brandImgSrc, alt: brandImgAlt })));
    const Header = React.createElement(LoginHeader, { headerBrand: HeaderBrand });
    const Footer = (React.createElement(LoginFooter, null,
        React.createElement("p", null, textContent),
        React.createElement(List, { variant: footerListVariants }, footerListItems)));
    return (React.createElement(React.Fragment, null,
        backgroundImgSrc && React.createElement(BackgroundImage, { src: backgroundImgSrc, alt: backgroundImgAlt }),
        React.createElement(Login, Object.assign({ header: Header, footer: Footer, className: css(className) }, props),
            React.createElement(LoginMainHeader, { title: loginTitle, subtitle: loginSubtitle }),
            React.createElement(LoginMainBody, null, children),
            (socialMediaLoginContent || forgotCredentials || signUpForAccountMessage) && (React.createElement(LoginMainFooter, { socialMediaLoginContent: socialMediaLoginContent, forgotCredentials: forgotCredentials, signUpForAccountMessage: signUpForAccountMessage })))));
};
LoginPage.displayName = 'LoginPage';
//# sourceMappingURL=LoginPage.js.map