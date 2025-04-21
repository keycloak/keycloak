import * as React from 'react';
export interface SplitProps extends React.HTMLProps<HTMLDivElement> {
    /** Adds space between children. */
    hasGutter?: boolean;
    /** Allows children to wrap */
    isWrappable?: boolean;
    /** content rendered inside the Split layout */
    children?: React.ReactNode;
    /** additional classes added to the Split layout */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: React.ReactNode;
}
export declare const Split: React.FunctionComponent<SplitProps>;
//# sourceMappingURL=Split.d.ts.map