import * as React from 'react';
export declare enum PageSectionVariants {
    default = "default",
    light = "light",
    dark = "dark",
    darker = "darker"
}
export declare enum PageSectionTypes {
    default = "default",
    nav = "nav",
    subNav = "subnav",
    breadcrumb = "breadcrumb",
    tabs = "tabs",
    wizard = "wizard"
}
export interface PageSectionProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the section */
    children?: React.ReactNode;
    /** Additional classes added to the section */
    className?: string;
    /** Section background color variant */
    variant?: 'default' | 'light' | 'dark' | 'darker';
    /** Section type variant */
    type?: 'default' | 'nav' | 'subnav' | 'breadcrumb' | 'tabs' | 'wizard';
    /** Enables the page section to fill the available vertical space */
    isFilled?: boolean;
    /** Limits the width of the section */
    isWidthLimited?: boolean;
    /** Flag indicating if the section content is center aligned. isWidthLimited must be set for this to work  */
    isCenterAligned?: boolean;
    /** Padding at various breakpoints. */
    padding?: {
        default?: 'padding' | 'noPadding';
        sm?: 'padding' | 'noPadding';
        md?: 'padding' | 'noPadding';
        lg?: 'padding' | 'noPadding';
        xl?: 'padding' | 'noPadding';
        '2xl'?: 'padding' | 'noPadding';
    };
    /** Modifier indicating if PageSection is sticky to the top or bottom */
    sticky?: 'top' | 'bottom';
    /** Modifier indicating if PageSection should have a shadow at the top */
    hasShadowTop?: boolean;
    /** Modifier indicating if PageSection should have a shadow at the bottom */
    hasShadowBottom?: boolean;
    /** Flag indicating if the PageSection has a scrolling overflow */
    hasOverflowScroll?: boolean;
}
export declare const PageSection: React.FunctionComponent<PageSectionProps>;
//# sourceMappingURL=PageSection.d.ts.map