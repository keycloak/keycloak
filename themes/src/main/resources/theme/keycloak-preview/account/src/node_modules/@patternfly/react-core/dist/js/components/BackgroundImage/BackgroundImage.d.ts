import * as React from 'react';
export declare enum BackgroundImageSrc {
    xs = "xs",
    xs2x = "xs2x",
    sm = "sm",
    sm2x = "sm2x",
    lg = "lg",
    filter = "filter"
}
export interface BackgroundImageSrcMap {
    xs: string;
    xs2x: string;
    sm: string;
    sm2x: string;
    lg: string;
    filter?: string;
}
export interface BackgroundImageProps extends Omit<React.HTMLProps<HTMLDivElement>, 'src'> {
    /** Additional classes added to the background. */
    className?: string;
    /** Override image styles using a string or BackgroundImageSrc */
    src: string | BackgroundImageSrcMap;
}
export declare const BackgroundImage: React.FunctionComponent<BackgroundImageProps>;
