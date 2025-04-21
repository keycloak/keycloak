import * as React from 'react';
export interface PageHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the page header */
    className?: string;
    /** Component to render the logo/brand, use <Brand /> */
    logo?: React.ReactNode;
    /** Additional props passed to the logo anchor container */
    logoProps?: object;
    /** Component to use to wrap the passed <logo> */
    logoComponent?: React.ReactNode;
    /** Component to render the header tools, use <PageHeaderTools />  */
    headerTools?: React.ReactNode;
    /** Component to render navigation within the header, use <Nav /> */
    topNav?: React.ReactNode;
    /** True to show the nav toggle button (toggles side nav) */
    showNavToggle?: boolean;
    /** True if the side nav is shown  */
    isNavOpen?: boolean;
    /** This prop is no longer managed through PageHeader but in the Page component. */
    isManagedSidebar?: boolean;
    /** Sets the value for role on the <main> element */
    role?: string;
    /** Callback function to handle the side nav toggle button, managed by the Page component if the Page isManagedSidebar prop is set to true */
    onNavToggle?: () => void;
    /** Aria Label for the nav toggle button */
    'aria-label'?: string;
}
export declare const PageHeader: React.FunctionComponent<PageHeaderProps>;
//# sourceMappingURL=PageHeader.d.ts.map