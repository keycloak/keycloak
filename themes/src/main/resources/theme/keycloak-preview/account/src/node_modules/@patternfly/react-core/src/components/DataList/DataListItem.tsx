import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { DataListContext } from './DataList';
import { KeyTypes } from '../Select';

export interface DataListItemProps extends Omit<React.HTMLProps<HTMLLIElement>, 'children'> {
  /** Flag to show if the expanded content of the DataList item is visible */
  isExpanded?: boolean;
  /** Content rendered inside the DataList item */
  children: React.ReactNode;
  /** Additional classes added to the DataList item should be either <DataListItemRow> or <DataListContent> */
  className?: string;
  /** Adds accessible text to the DataList item */
  'aria-labelledby': string;
  /** Unique id for the DataList item */
  id?: string;
}

export interface DataListItemChildProps {
  /** Id for the row */
  rowid: string;
}

export const DataListItem: React.FunctionComponent<DataListItemProps> = ({
  isExpanded = false,
  className = '',
  id = '',
  'aria-labelledby': ariaLabelledBy,
  children,
  ...props
}: DataListItemProps) => (
  <DataListContext.Consumer>
    {({ isSelectable, selectedDataListItemId, updateSelectedDataListItem }) => {
      const selectDataListItem = (event: React.MouseEvent) => {
        let target: any = event.target;
        while (event.currentTarget !== target) {
          if (
            ('onclick' in target && target.onclick) ||
            target.parentNode.classList.contains(styles.dataListItemAction) ||
            target.parentNode.classList.contains(styles.dataListItemControl)
          ) {
            // check other event handlers are not present.
            return;
          } else {
            target = target.parentNode;
          }
        }
        updateSelectedDataListItem(id);
      };

      const onKeyDown = (event: React.KeyboardEvent) => {
        if (event.key === KeyTypes.Enter) {
          updateSelectedDataListItem(id);
        }
      };

      return (
        <li
          id={id}
          className={css(
            styles.dataListItem,
            isExpanded && styles.modifiers.expanded,
            isSelectable && styles.modifiers.selectable,
            selectedDataListItemId && selectedDataListItemId === id && styles.modifiers.selected,
            className
          )}
          aria-labelledby={ariaLabelledBy}
          {...(isSelectable && { tabIndex: 0, onClick: selectDataListItem, onKeyDown })}
          {...(isSelectable && selectedDataListItemId === id && { 'aria-selected': true })}
          {...props}
        >
          {React.Children.map(
            children,
            child =>
              React.isValidElement(child) &&
              React.cloneElement(child as React.ReactElement<any>, {
                rowid: ariaLabelledBy
              })
          )}
        </li>
      );
    }}
  </DataListContext.Consumer>
);
