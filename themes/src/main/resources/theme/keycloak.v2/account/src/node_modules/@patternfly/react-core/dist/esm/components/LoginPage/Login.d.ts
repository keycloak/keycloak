import * as React from 'react';
export interface LoginProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the main section of the login layout */
    children?: React.ReactNode;
    /** Additional classes added to the login layout */
    className?: string;
    /** Footer component (e.g. <LoginFooter />) */
    footer?: React.ReactNode;
    /** Header component (e.g. <LoginHeader />) */
    header?: React.ReactNode;
}
export declare const Login: React.FunctionComponent<LoginProps>;
//# sourceMappingURL=Login.d.ts.map