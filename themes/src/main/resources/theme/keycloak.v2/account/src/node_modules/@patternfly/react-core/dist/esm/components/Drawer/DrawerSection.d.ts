import * as React from 'react';
import { DrawerColorVariant } from './Drawer';
export interface DrawerSectionProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the drawer section. */
    className?: string;
    /** Content to be rendered in the drawer section. */
    children?: React.ReactNode;
    /** Color variant of the background of the drawer Section */
    colorVariant?: DrawerColorVariant | 'light-200' | 'default';
}
export declare const DrawerSection: React.FunctionComponent<DrawerSectionProps>;
//# sourceMappingURL=DrawerSection.d.ts.map