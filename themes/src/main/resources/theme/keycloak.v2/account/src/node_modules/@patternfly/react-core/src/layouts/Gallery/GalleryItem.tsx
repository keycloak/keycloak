import * as React from 'react';

export interface GalleryItemProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Gallery Item */
  children?: React.ReactNode;
  /** Sets the base component to render. defaults to div */
  component?: React.ElementType<any> | React.ComponentType<any>;
}

export const GalleryItem: React.FunctionComponent<GalleryItemProps> = ({
  children = null,
  component = 'div',
  ...props
}: GalleryItemProps) => {
  const Component: any = component;

  return <Component {...props}>{children}</Component>;
};
GalleryItem.displayName = 'GalleryItem';
