import * as React from 'react';
export declare enum PageSectionVariants {
    default = "default",
    light = "light",
    dark = "dark",
    darker = "darker"
}
export declare enum PageSectionTypes {
    default = "default",
    nav = "nav"
}
export interface PageSectionProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the section */
    children?: React.ReactNode;
    /** Additional classes added to the section */
    className?: string;
    /** Section background color variant */
    variant?: 'default' | 'light' | 'dark' | 'darker';
    /** Section type variant */
    type?: 'default' | 'nav';
    /** Enables the page section to fill the available vertical space */
    isFilled?: boolean;
    /** Modifies a main page section to have no padding */
    noPadding?: boolean;
    /** Modifies a main page section to have no padding on mobile */
    noPaddingMobile?: boolean;
}
export declare const PageSection: ({ className, children, variant, type, noPadding, noPaddingMobile, isFilled, ...props }: PageSectionProps) => JSX.Element;
