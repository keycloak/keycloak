import * as React from 'react';
export interface MastheadProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLDivElement>, HTMLDivElement> {
    /** Content rendered inside of the masthead */
    children?: React.ReactNode;
    /** Additional classes added to the masthead */
    className?: string;
    /** Background theme color of the masthead */
    backgroundColor?: 'dark' | 'light' | 'light200';
    /** Display type at various breakpoints */
    display?: {
        default?: 'inline' | 'stack';
        sm?: 'inline' | 'stack';
        md?: 'inline' | 'stack';
        lg?: 'inline' | 'stack';
        xl?: 'inline' | 'stack';
        '2xl'?: 'inline' | 'stack';
    };
    /** Insets at various breakpoints */
    inset?: {
        default?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
        sm?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
        md?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
        lg?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
        xl?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
        '2xl'?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
    };
}
export declare const Masthead: React.FunctionComponent<MastheadProps>;
//# sourceMappingURL=Masthead.d.ts.map