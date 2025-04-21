import * as React from 'react';
export interface BrandProps extends React.DetailedHTMLProps<React.ImgHTMLAttributes<HTMLImageElement>, HTMLImageElement> {
    /** Transforms the Brand into a <picture> element from an <img> element. Container for <source> child elements. */
    children?: React.ReactNode;
    /** Additional classes added to the either type of Brand. */
    className?: string;
    /** Attribute that specifies the URL of a <img> Brand. For a <picture> Brand this specifies the fallback <img> URL. */
    src?: string;
    /** Attribute that specifies the alt text of a <img> Brand. For a <picture> Brand this specifies the fallback <img> alt text. */
    alt: string;
    /** Widths at various breakpoints for a <picture> Brand. */
    widths?: {
        default?: string;
        sm?: string;
        md?: string;
        lg?: string;
        xl?: string;
        '2xl'?: string;
    };
    /** Heights at various breakpoints for a <picture> Brand. */
    heights?: {
        default?: string;
        sm?: string;
        md?: string;
        lg?: string;
        xl?: string;
        '2xl'?: string;
    };
}
export declare const Brand: React.FunctionComponent<BrandProps>;
//# sourceMappingURL=Brand.d.ts.map