import * as React from 'react';
export interface CardBodyProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Card Body */
    children?: React.ReactNode;
    /** Additional classes added to the Card Body */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: keyof JSX.IntrinsicElements;
    /** Enables the body Content to fill the height of the card */
    isFilled?: boolean;
}
export declare const CardBody: React.FunctionComponent<CardBodyProps>;
//# sourceMappingURL=CardBody.d.ts.map