import * as React from 'react';
import { BackgroundImageSrcMap } from '../BackgroundImage';
import { ListVariant } from '../List';
export interface LoginPageProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything that can be rendered inside of the LoginPage (e.g. <LoginPageForm>) */
    children?: React.ReactNode;
    /** Additional classes added to the LoginPage. */
    className?: string;
    /** Attribute that specifies the URL of the brand image for the LoginPage */
    brandImgSrc?: string;
    /** Attribute that specifies the alt text of the brand image for the LoginPage. */
    brandImgAlt?: string;
    /** Attribute that specifies the URL of the background image for the LoginPage */
    backgroundImgSrc?: string | BackgroundImageSrcMap;
    /** Attribute that specifies the alt text of the background image for the LoginPage. */
    backgroundImgAlt?: string;
    /** Content rendered inside of the Text Component of the LoginPage */
    textContent?: string;
    /** Items rendered inside of the Footer List Component of the LoginPage */
    footerListItems?: React.ReactNode;
    /** Adds list variant styles for the Footer List component of the LoginPage. The only current value is'inline' */
    footerListVariants?: ListVariant.inline;
    /** Title for the Login Main Body Header of the LoginPage */
    loginTitle: string;
    /** Subtitle for the Login Main Body Header of the LoginPage */
    loginSubtitle?: string;
    /** Content rendered inside of Login Main Footer Band to display a sign up for account message */
    signUpForAccountMessage?: React.ReactNode;
    /** Content rendered inside of Login Main Footer Band to display a forgot credentials link* */
    forgotCredentials?: React.ReactNode;
    /** Content rendered inside of Social Media Login footer section . */
    socialMediaLoginContent?: React.ReactNode;
}
export declare const LoginPage: React.FunctionComponent<LoginPageProps>;
//# sourceMappingURL=LoginPage.d.ts.map