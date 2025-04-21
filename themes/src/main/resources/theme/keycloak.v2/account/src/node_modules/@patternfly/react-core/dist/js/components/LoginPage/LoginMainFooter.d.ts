import * as React from 'react';
export interface LoginMainFooterProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Login Main Footer */
    className?: string;
    /** Content rendered inside the Login Main Footer */
    children?: React.ReactNode;
    /** Content rendered inside the Login Main Footer as Social Media Links* */
    socialMediaLoginContent?: React.ReactNode;
    /** Content rendered inside of Login Main Footer Band to display a sign up for account message */
    signUpForAccountMessage?: React.ReactNode;
    /** Content rendered inside of Login Main Footer Band do display a forgot credentials link* */
    forgotCredentials?: React.ReactNode;
}
export declare const LoginMainFooter: React.FunctionComponent<LoginMainFooterProps>;
//# sourceMappingURL=LoginMainFooter.d.ts.map