import * as React from 'react';
export interface LoginMainHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Login Main Header */
    children?: React.ReactNode;
    /** Additional classes added to the Login Main Header */
    className?: string;
    /** Title for the Login Main Header */
    title?: string;
    /** Subtitle that contains the Text, URL, and URL Text for the Login Main Header */
    subtitle?: string;
}
export declare const LoginMainHeader: React.FunctionComponent<LoginMainHeaderProps>;
//# sourceMappingURL=LoginMainHeader.d.ts.map