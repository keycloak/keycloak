import * as React from 'react';

export interface ToggleTemplateProps {
  /** The first index of the items being paginated */
  firstIndex?: number;
  /** The last index of the items being paginated */
  lastIndex?: number;
  /** The total number of items being paginated */
  itemCount?: number;
  /** The type or title of the items being paginated */
  itemsTitle?: string;
  /** The word that joins the index and itemCount/itemsTitle */
  ofWord?: React.ReactNode;
}

export const ToggleTemplate: React.FunctionComponent<ToggleTemplateProps> = ({
  firstIndex = 0,
  lastIndex = 0,
  itemCount = 0,
  itemsTitle = 'items',
  ofWord = 'of'
}: ToggleTemplateProps) => (
  <React.Fragment>
    <b>
      {firstIndex} - {lastIndex}
    </b>{' '}
    {ofWord} <b>{itemCount}</b> {itemsTitle}
  </React.Fragment>
);
ToggleTemplate.displayName = 'ToggleTemplate';
