import * as React from 'react';
export interface SkeletonProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Skeleton */
    className?: string;
    /** The width of the Skeleton. Must specify pixels or percentage. */
    width?: string;
    /** The height of the Skeleton. Must specify pixels or percentage. */
    height?: string;
    /** The font size height of the Skeleton */
    fontSize?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl';
    /** The shape of the Skeleton */
    shape?: 'circle' | 'square';
    /** Text read just to screen reader users */
    screenreaderText?: string;
}
export declare const Skeleton: React.FunctionComponent<SkeletonProps>;
//# sourceMappingURL=Skeleton.d.ts.map