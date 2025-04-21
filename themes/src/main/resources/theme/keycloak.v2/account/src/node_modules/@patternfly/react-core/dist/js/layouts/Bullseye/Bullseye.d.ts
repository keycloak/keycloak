import * as React from 'react';
export interface BullseyeProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Bullseye layout */
    children?: React.ReactNode;
    /** additional classes added to the Bullseye layout */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: keyof JSX.IntrinsicElements;
}
export declare const Bullseye: React.FunctionComponent<BullseyeProps>;
//# sourceMappingURL=Bullseye.d.ts.map