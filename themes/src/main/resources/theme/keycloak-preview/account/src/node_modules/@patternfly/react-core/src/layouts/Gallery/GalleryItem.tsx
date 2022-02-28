import * as React from 'react';

export interface GalleryItemProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Gallery Item */
  children?: React.ReactNode;
}

export const GalleryItem: React.FunctionComponent<GalleryItemProps> = ({
  children = null,
  ...props
}: GalleryItemProps) => <div {...props}>{children}</div>;
