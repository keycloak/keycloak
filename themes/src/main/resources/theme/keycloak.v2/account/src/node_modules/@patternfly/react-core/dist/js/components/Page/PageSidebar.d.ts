import * as React from 'react';
export interface PageSidebarProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the page sidebar */
    className?: string;
    /** Component to render the side navigation (e.g. <Nav /> */
    nav?: React.ReactNode;
    /**
     * If true, manages the sidebar open/close state and there is no need to pass the isNavOpen boolean into
     * the sidebar component or add a callback onNavToggle function into the PageHeader component
     */
    isManagedSidebar?: boolean;
    /** Programmatically manage if the side nav is shown, if isManagedSidebar is set to true in the Page component, this prop is managed */
    isNavOpen?: boolean;
    /** Indicates the color scheme of the sidebar */
    theme?: 'dark' | 'light';
}
export interface PageSidebarContextProps {
    isNavOpen: boolean;
}
export declare const pageSidebarContextDefaults: PageSidebarContextProps;
export declare const PageSidebarContext: React.Context<Partial<PageSidebarContextProps>>;
export declare const PageSidebar: React.FunctionComponent<PageSidebarProps>;
//# sourceMappingURL=PageSidebar.d.ts.map