import * as React from 'react';
export interface BackgroundImageSrcMap {
    xs: string;
    xs2x: string;
    sm: string;
    sm2x: string;
    lg: string;
}
export interface BackgroundImageProps extends Omit<React.HTMLProps<HTMLDivElement>, 'src'> {
    /** Additional classes added to the background. */
    className?: string;
    /** Override svg filter to use */
    filter?: React.ReactElement;
    /** Override image styles using a string or BackgroundImageSrc */
    src: string | BackgroundImageSrcMap;
}
export declare const BackgroundImage: React.FunctionComponent<BackgroundImageProps>;
//# sourceMappingURL=BackgroundImage.d.ts.map