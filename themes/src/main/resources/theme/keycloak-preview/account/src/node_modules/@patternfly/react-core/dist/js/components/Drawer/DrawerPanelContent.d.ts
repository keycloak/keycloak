import * as React from 'react';
export interface DrawerPanelContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the drawer. */
    className?: string;
    /** Content to be rendered in the drawer panel. */
    children?: React.ReactNode;
    hasBorder?: boolean;
    width?: 25 | 33 | 50 | 66 | 75 | 100;
    widthOnLg?: 25 | 33 | 50 | 66 | 75 | 100;
    widthOnXl?: 25 | 33 | 50 | 66 | 75 | 100;
    widthOn2Xl?: 25 | 33 | 50 | 66 | 75 | 100;
}
export declare const DrawerPanelContent: React.SFC<DrawerPanelContentProps>;
