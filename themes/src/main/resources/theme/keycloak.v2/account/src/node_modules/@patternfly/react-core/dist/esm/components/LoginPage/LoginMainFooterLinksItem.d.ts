import * as React from 'react';
export interface LoginMainFooterLinksItemProps extends React.HTMLProps<HTMLLIElement> {
    /** Content rendered inside the footer Link Item */
    children?: React.ReactNode;
    /** HREF for Footer Link Item */
    href?: string;
    /** Target for Footer Link Item */
    target?: string;
    /** Additional classes added to the Footer Link Item  */
    className?: string;
    /** Component used to render the Footer Link Item */
    linkComponent?: React.ReactNode;
    /** Props for the LinkComponent */
    linkComponentProps?: any;
}
export declare const LoginMainFooterLinksItem: React.FunctionComponent<LoginMainFooterLinksItemProps>;
//# sourceMappingURL=LoginMainFooterLinksItem.d.ts.map