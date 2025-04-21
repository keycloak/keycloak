import * as React from 'react';
export interface MastheadBrandProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLAnchorElement>, HTMLAnchorElement> {
    /** Content rendered inside of the masthead brand. */
    children?: React.ReactNode;
    /** Additional classes added to the masthead brand. */
    className?: string;
    /** Component type of the masthead brand. */
    component?: React.ElementType<any> | React.ComponentType<any>;
}
export declare const MastheadBrand: React.FunctionComponent<MastheadBrandProps>;
//# sourceMappingURL=MastheadBrand.d.ts.map