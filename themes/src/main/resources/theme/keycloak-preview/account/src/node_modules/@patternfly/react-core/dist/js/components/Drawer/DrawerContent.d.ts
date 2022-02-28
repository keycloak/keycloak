import * as React from 'react';
export interface DrawerContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Drawer. */
    className?: string;
    /** Content to be rendered in the drawer. */
    children?: React.ReactNode;
    /** Content rendered in the drawer panel. */
    panelContent: React.ReactNode;
}
export declare const DrawerContent: React.SFC<DrawerContentProps>;
