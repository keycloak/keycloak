import * as React from 'react';
export interface WizardNavItemProps {
    /** Can nest a WizardNav component for substeps */
    children?: React.ReactNode;
    /** The content to display in the nav item */
    content?: React.ReactNode;
    /** Whether the nav item is the currently active item */
    isCurrent?: boolean;
    /** Whether the nav item is disabled */
    isDisabled?: boolean;
    /** The step passed into the onNavItemClick callback */
    step: number;
    /** Callback for when the nav item is clicked */
    onNavItemClick?: (step: number) => any;
    /** Component used to render WizardNavItem */
    navItemComponent?: 'button' | 'a';
    /** An optional url to use for when using an anchor component */
    href?: string;
    /** Flag indicating that this NavItem has child steps and is expandable */
    isExpandable?: boolean;
    /** The id for the nav item */
    id?: string | number;
}
export declare const WizardNavItem: React.FunctionComponent<WizardNavItemProps>;
//# sourceMappingURL=WizardNavItem.d.ts.map