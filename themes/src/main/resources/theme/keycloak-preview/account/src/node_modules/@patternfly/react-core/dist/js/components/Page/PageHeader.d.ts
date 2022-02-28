import * as React from 'react';
export interface PageHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the page header */
    className?: string;
    /** Component to render the logo/brand (e.g. <Brand />) */
    logo?: React.ReactNode;
    /** Additional props passed to the logo anchor container */
    logoProps?: object;
    /** Component to use to wrap the passed <logo> */
    logoComponent?: React.ReactNode;
    /** Component to render the toolbar (e.g. <Toolbar />) */
    toolbar?: React.ReactNode;
    /** Component to render the avatar (e.g. <Avatar /> */
    avatar?: React.ReactNode;
    /** Component to render navigation within the header (e.g. <Nav /> */
    topNav?: React.ReactNode;
    /** True to show the nav toggle button (toggles side nav) */
    showNavToggle?: boolean;
    /** True if the side nav is shown  */
    isNavOpen?: boolean;
    /**
     * If true, manages the sidebar open/close state and there is no need to pass the isNavOpen boolean into
     * the sidebar component or add a callback onNavToggle function into the PageHeader component
     */
    isManagedSidebar?: boolean;
    /** Sets the value for role on the <main> element */
    role?: string;
    /** Callback function to handle the side nav toggle button, managed by the Page component if the Page isManagedSidebar prop is set to true */
    onNavToggle?: () => void;
    /** Aria Label for the nav toggle button */
    'aria-label'?: string;
}
export declare const PageHeader: ({ className, logo, logoProps, logoComponent, toolbar, avatar, topNav, isNavOpen, role, showNavToggle, onNavToggle, "aria-label": ariaLabel, ...props }: PageHeaderProps) => JSX.Element;
