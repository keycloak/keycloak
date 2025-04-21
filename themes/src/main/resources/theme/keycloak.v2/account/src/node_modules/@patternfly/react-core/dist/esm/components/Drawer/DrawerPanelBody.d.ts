import * as React from 'react';
export interface DrawerPanelBodyProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Drawer. */
    className?: string;
    /** Content to be rendered in the drawer */
    children?: React.ReactNode;
    /** Indicates if there should be no padding around the drawer panel body */
    hasNoPadding?: boolean;
}
export declare const DrawerPanelBody: React.FunctionComponent<DrawerPanelBodyProps>;
//# sourceMappingURL=DrawerPanelBody.d.ts.map