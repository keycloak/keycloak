import * as React from 'react';
export interface BrandProps extends React.DetailedHTMLProps<React.ImgHTMLAttributes<HTMLImageElement>, HTMLImageElement> {
    /** Additional classes added to the Brand. */
    className?: string;
    /** Attribute that specifies the URL of the image for the Brand. */
    src?: string;
    /** Attribute that specifies the alt text of the image for the Brand. */
    alt: string;
}
export declare const Brand: React.FunctionComponent<BrandProps>;
