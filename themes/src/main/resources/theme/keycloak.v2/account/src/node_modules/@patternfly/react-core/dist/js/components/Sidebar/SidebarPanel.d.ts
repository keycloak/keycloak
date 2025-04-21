import * as React from 'react';
export interface SidebarPanelProps extends Omit<React.HTMLProps<HTMLDivElement>, 'width'> {
    children: React.ReactNode;
    /** Indicates whether the panel is positioned statically or sticky to the top. Default is sticky on small screens when the orientation is stack, and static on medium and above screens when the orientation is split. */
    variant?: 'default' | 'sticky' | 'static';
    /** Removes the background color. */
    hasNoBackground?: boolean;
    /** Sets the panel width at various breakpoints. Default is 250px when the orientation is split. */
    width?: {
        default?: 'default' | 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        sm?: 'default' | 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        md?: 'default' | 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        lg?: 'default' | 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        xl?: 'default' | 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
        '2xl'?: 'default' | 'width_25' | 'width_33' | 'width_50' | 'width_66' | 'width_75' | 'width_100';
    };
}
export declare const SidebarPanel: React.FunctionComponent<SidebarPanelProps>;
//# sourceMappingURL=SidebarPanel.d.ts.map