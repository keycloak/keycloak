import * as React from 'react';
import { DrawerColorVariant } from './Drawer';
export interface DrawerPanelContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the drawer. */
    className?: string;
    /** ID of the drawer panel */
    id?: string;
    /** Content to be rendered in the drawer panel. */
    children?: React.ReactNode;
    /** Flag indicating that the drawer panel should not have a border. */
    hasNoBorder?: boolean;
    /** Flag indicating that the drawer panel should be resizable. */
    isResizable?: boolean;
    /** Callback for resize end. */
    onResize?: (width: number, id: string) => void;
    /** The minimum size of a drawer, in either pixels or percentage. */
    minSize?: string;
    /** The starting size of a resizable drawer, in either pixels or percentage. */
    defaultSize?: string;
    /** The maximum size of a drawer, in either pixels or percentage. */
    maxSize?: string;
    /** The increment amount for keyboard drawer resizing, in pixels. */
    increment?: number;
    /** Aria label for the resizable drawer splitter. */
    resizeAriaLabel?: string;
    /** Width for drawer panel at various breakpoints. Overriden by resizable drawer minSize and defaultSize. */
    widths?: {
        default?: 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        lg?: 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        xl?: 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        '2xl'?: 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
    };
    /** Color variant of the background of the drawer panel */
    colorVariant?: DrawerColorVariant | 'light-200' | 'default';
}
export declare const DrawerPanelContent: React.FunctionComponent<DrawerPanelContentProps>;
//# sourceMappingURL=DrawerPanelContent.d.ts.map