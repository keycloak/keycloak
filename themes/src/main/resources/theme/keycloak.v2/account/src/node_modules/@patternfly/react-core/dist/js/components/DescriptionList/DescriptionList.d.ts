import * as React from 'react';
export interface BreakpointModifiers {
    default?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
}
export interface DescriptionListProps extends Omit<React.HTMLProps<HTMLDListElement>, 'type'> {
    /** Anything that can be rendered inside of the list */
    children?: React.ReactNode;
    /** Additional classes added to the list */
    className?: string;
    /** Sets the description list to auto fit. */
    isAutoFit?: boolean;
    /** Sets the description list component term and description pair to a horizontal layout. */
    isHorizontal?: boolean;
    /** Sets the description list to format automatically. */
    isAutoColumnWidths?: boolean;
    /** Modifies the description list display to inline-grid. */
    isInlineGrid?: boolean;
    /** Sets the description list to compact styling. */
    isCompact?: boolean;
    /** Sets a horizontal description list to have fluid styling. */
    isFluid?: boolean;
    /** Sets the the default placement of description list groups to fill from top to bottom. */
    isFillColumns?: boolean;
    /** Sets the number of columns on the description list at various breakpoints */
    columnModifier?: {
        default?: '1Col' | '2Col' | '3Col';
        sm?: '1Col' | '2Col' | '3Col';
        md?: '1Col' | '2Col' | '3Col';
        lg?: '1Col' | '2Col' | '3Col';
        xl?: '1Col' | '2Col' | '3Col';
        '2xl'?: '1Col' | '2Col' | '3Col';
    };
    /** Indicates how the menu will align at various breakpoints. */
    orientation?: {
        sm?: 'vertical' | 'horizontal';
        md?: 'vertical' | 'horizontal';
        lg?: 'vertical' | 'horizontal';
        xl?: 'vertical' | 'horizontal';
        '2xl'?: 'vertical' | 'horizontal';
    };
    /** Sets the minimum column size for the auto-fit (isAutoFit) layout at various breakpoints. */
    autoFitMinModifier?: {
        default?: string;
        sm?: string;
        md?: string;
        lg?: string;
        xl?: string;
        '2xl'?: string;
    };
    /** Sets the horizontal description list's term column width at various breakpoints. */
    horizontalTermWidthModifier?: {
        default?: string;
        sm?: string;
        md?: string;
        lg?: string;
        xl?: string;
        '2xl'?: string;
    };
}
export declare const DescriptionList: React.FunctionComponent<DescriptionListProps>;
//# sourceMappingURL=DescriptionList.d.ts.map