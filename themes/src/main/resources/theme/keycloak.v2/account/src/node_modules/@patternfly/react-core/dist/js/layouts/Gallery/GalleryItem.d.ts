import * as React from 'react';
export interface GalleryItemProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Gallery Item */
    children?: React.ReactNode;
    /** Sets the base component to render. defaults to div */
    component?: React.ElementType<any> | React.ComponentType<any>;
}
export declare const GalleryItem: React.FunctionComponent<GalleryItemProps>;
//# sourceMappingURL=GalleryItem.d.ts.map