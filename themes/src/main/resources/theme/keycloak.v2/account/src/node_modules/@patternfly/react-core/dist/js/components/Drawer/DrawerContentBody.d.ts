import * as React from 'react';
export interface DrawerContentBodyProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Drawer. */
    className?: string;
    /** Content to be rendered in the drawer */
    children?: React.ReactNode;
    /** Indicates if there should be padding around the drawer content body */
    hasPadding?: boolean;
}
export declare const DrawerContentBody: React.FunctionComponent<DrawerContentBodyProps>;
//# sourceMappingURL=DrawerContentBody.d.ts.map