import * as React from 'react';
export interface AvatarProps extends React.DetailedHTMLProps<React.ImgHTMLAttributes<HTMLImageElement>, HTMLImageElement> {
    /** Additional classes added to the Avatar. */
    className?: string;
    /** Attribute that specifies the URL of the image for the Avatar. */
    src?: string;
    /** Attribute that specifies the alternate text of the image for the Avatar. */
    alt: string;
    /** Border to add */
    border?: 'light' | 'dark';
    /** Size variant of avatar. */
    size?: 'sm' | 'md' | 'lg' | 'xl';
}
export declare const Avatar: React.FunctionComponent<AvatarProps>;
//# sourceMappingURL=Avatar.d.ts.map