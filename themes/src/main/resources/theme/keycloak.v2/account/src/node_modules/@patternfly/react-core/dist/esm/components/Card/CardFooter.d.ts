import * as React from 'react';
export interface CardFooterProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Card Footer */
    children?: React.ReactNode;
    /** Additional classes added to the Footer */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: keyof JSX.IntrinsicElements;
}
export declare const CardFooter: React.FunctionComponent<CardFooterProps>;
//# sourceMappingURL=CardFooter.d.ts.map