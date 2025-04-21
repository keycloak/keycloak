import * as React from 'react';
export declare enum DrawerColorVariant {
    default = "default",
    light200 = "light-200"
}
export interface DrawerProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Drawer. */
    className?: string;
    /** Content rendered in the left hand panel */
    children?: React.ReactNode;
    /** Indicates if the drawer is expanded */
    isExpanded?: boolean;
    /** Indicates if the content element and panel element are displayed side by side. */
    isInline?: boolean;
    /** Indicates if the drawer will always show both content and panel. */
    isStatic?: boolean;
    /** Position of the drawer panel */
    position?: 'left' | 'right' | 'bottom';
    /** Callback when drawer panel is expanded after waiting 250ms for animation to complete. */
    onExpand?: () => void;
}
export interface DrawerContextProps {
    isExpanded: boolean;
    isStatic: boolean;
    onExpand?: () => void;
    position?: string;
    drawerRef?: React.RefObject<HTMLDivElement>;
    drawerContentRef?: React.RefObject<HTMLDivElement>;
    isInline: boolean;
}
export declare const DrawerContext: React.Context<Partial<DrawerContextProps>>;
export declare const Drawer: React.FunctionComponent<DrawerProps>;
//# sourceMappingURL=Drawer.d.ts.map