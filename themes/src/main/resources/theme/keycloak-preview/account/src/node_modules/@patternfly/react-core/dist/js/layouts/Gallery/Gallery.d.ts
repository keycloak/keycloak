import * as React from 'react';
export interface GalleryProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Gallery layout */
    children?: React.ReactNode;
    /** additional classes added to the Gallery layout */
    className?: string;
    /** Adds space between children. */
    gutter?: 'sm' | 'md' | 'lg';
}
export declare const Gallery: React.FunctionComponent<GalleryProps>;
