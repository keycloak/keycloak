import * as React from 'react';
export interface OptionsMenuItemGroupProps extends React.HTMLProps<HTMLElement> {
    /** Content to be rendered in the options menu items component */
    children?: React.ReactNode;
    /** Classes applied to root element of the options menu items group */
    className?: string;
    /** Provides an accessible name for the options menu items group */
    'aria-label'?: string;
    /** Optional title for the options menu items group */
    groupTitle?: string | React.ReactNode;
    /** Flag indicating this options menu items group will be followed by a horizontal separator */
    hasSeparator?: boolean;
}
export declare const OptionsMenuItemGroup: React.FunctionComponent<OptionsMenuItemGroupProps>;
//# sourceMappingURL=OptionsMenuItemGroup.d.ts.map