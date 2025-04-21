import * as React from 'react';
export declare type gridItemSpanValueShape = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
export interface GridProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Grid layout */
    children?: React.ReactNode;
    /** additional classes added to the Grid layout */
    className?: string;
    /** Adds space between children. */
    hasGutter?: boolean;
    /** The number of rows a column in the grid should span.  Value should be a number 1-12 */
    span?: gridItemSpanValueShape;
    /** the number of columns all grid items should span on a small device */
    sm?: gridItemSpanValueShape;
    /** the number of columns all grid items should span on a medium device */
    md?: gridItemSpanValueShape;
    /** the number of columns all grid items should span on a large device */
    lg?: gridItemSpanValueShape;
    /** the number of columns all grid items should span on a xLarge device */
    xl?: gridItemSpanValueShape;
    /** the number of columns all grid items should span on a 2xLarge device */
    xl2?: gridItemSpanValueShape;
    /** Modifies the flex layout element order property */
    order?: {
        default?: string;
        md?: string;
        lg?: string;
        xl?: string;
        '2xl'?: string;
    };
    /** Sets the base component to render. defaults to div */
    component?: React.ElementType<any> | React.ComponentType<any>;
}
export declare const Grid: React.FunctionComponent<GridProps>;
//# sourceMappingURL=Grid.d.ts.map