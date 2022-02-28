import * as React from 'react';
export interface DrawerHeadProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the drawer head. */
    className?: string;
    /** Content to be rendered in the drawer head */
    children?: React.ReactNode;
    /** Indicates if there should be no padding around the drawer panel body of the head*/
    noPadding?: boolean;
}
export declare const DrawerHead: React.SFC<DrawerHeadProps>;
