import * as React from 'react';
export interface CardTitleProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the CardTitle */
    children?: React.ReactNode;
    /** Additional classes added to the CardTitle */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: keyof JSX.IntrinsicElements;
}
export declare const CardTitle: React.FunctionComponent<CardTitleProps>;
//# sourceMappingURL=CardTitle.d.ts.map