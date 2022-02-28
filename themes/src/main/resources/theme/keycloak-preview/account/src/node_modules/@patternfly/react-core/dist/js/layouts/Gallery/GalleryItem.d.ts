import * as React from 'react';
export interface GalleryItemProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Gallery Item */
    children?: React.ReactNode;
}
export declare const GalleryItem: React.FunctionComponent<GalleryItemProps>;
