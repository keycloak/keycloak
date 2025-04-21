import * as React from 'react';
export declare enum ToolbarItemVariant {
    separator = "separator",
    'bulk-select' = "bulk-select",
    'overflow-menu' = "overflow-menu",
    pagination = "pagination",
    'search-filter' = "search-filter",
    label = "label",
    'chip-group' = "chip-group",
    'expand-all' = "expand-all"
}
export interface ToolbarItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Classes applied to root element of the data toolbar item */
    className?: string;
    /** A type modifier which modifies spacing specifically depending on the type of item */
    variant?: ToolbarItemVariant | 'bulk-select' | 'overflow-menu' | 'pagination' | 'search-filter' | 'label' | 'chip-group' | 'separator' | 'expand-all';
    /** Visibility at various breakpoints. */
    visibility?: {
        default?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** @deprecated prop misspelled */
    visiblity?: {
        default?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** Alignment at various breakpoints. */
    alignment?: {
        default?: 'alignRight' | 'alignLeft';
        md?: 'alignRight' | 'alignLeft';
        lg?: 'alignRight' | 'alignLeft';
        xl?: 'alignRight' | 'alignLeft';
        '2xl'?: 'alignRight' | 'alignLeft';
    };
    /** Spacers at various breakpoints. */
    spacer?: {
        default?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        md?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        lg?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        xl?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        '2xl'?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
    };
    /** Widths at various breakpoints. */
    widths?: {
        default?: string;
        sm?: string;
        md?: string;
        lg?: string;
        xl?: string;
        '2xl'?: string;
    };
    /** id for this data toolbar item */
    id?: string;
    /** Flag indicating if the expand-all variant is expanded or not */
    isAllExpanded?: boolean;
    /** Content to be rendered inside the data toolbar item */
    children?: React.ReactNode;
}
export declare const ToolbarItem: React.FunctionComponent<ToolbarItemProps>;
//# sourceMappingURL=ToolbarItem.d.ts.map