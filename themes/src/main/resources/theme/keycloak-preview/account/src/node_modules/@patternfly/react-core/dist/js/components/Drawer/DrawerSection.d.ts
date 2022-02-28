import * as React from 'react';
export interface DrawerSectionProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the drawer section. */
    className?: string;
    /** Content to be rendered in the drawer section. */
    children?: React.ReactNode;
}
export declare const DrawerSection: React.SFC<DrawerSectionProps>;
