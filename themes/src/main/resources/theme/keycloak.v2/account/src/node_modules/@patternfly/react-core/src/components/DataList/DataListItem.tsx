import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { DataListContext } from './DataList';
import { KeyTypes } from '../../helpers/constants';
import { DataListDragButton, DataListDragButtonProps } from './DataListDragButton';

export interface DataListItemProps extends Omit<React.HTMLProps<HTMLLIElement>, 'children' | 'ref'> {
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
  /** @beta Aria label to apply to the selectable input if one is rendered */
  selectableInputAriaLabel?: string;
}

export interface DataListItemChildProps {
  /** Id for the row */
  rowid: string;
}

function findDataListDragButton(node: React.ReactNode): React.ReactElement<DataListDragButtonProps> | null {
  if (!React.isValidElement(node)) {
    return null;
  }
  if (node.type === DataListDragButton) {
    return node as React.ReactElement<DataListDragButtonProps>;
  }
  if (node.props.children) {
    for (const child of React.Children.toArray(node.props.children)) {
      const button = findDataListDragButton(child);
      if (button) {
        return button;
      }
    }
  }

  return null;
}

export class DataListItem extends React.Component<DataListItemProps> {
  static displayName = 'DataListItem';
  static defaultProps: DataListItemProps = {
    isExpanded: false,
    className: '',
    id: '',
    children: null,
    'aria-labelledby': ''
  };
  render() {
    const {
      children,
      isExpanded,
      className,
      id,
      'aria-labelledby': ariaLabelledBy,
      selectableInputAriaLabel,
      ...props
    } = this.props;
    return (
      <DataListContext.Consumer>
        {({
          isSelectable,
          selectedDataListItemId,
          updateSelectedDataListItem,
          selectableRow,
          isDraggable,
          dragStart,
          dragEnd,
          drop
        }) => {
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

          // We made the DataListDragButton determine if the entire item is draggable instead of
          // DataListItem like we should have.
          // Recursively search children for the DataListDragButton and see if it's disabled...
          const dragButton = findDataListDragButton(children);
          const dragProps = isDraggable && {
            draggable: dragButton ? !dragButton.props.isDisabled : true,
            onDrop: drop,
            onDragEnd: dragEnd,
            onDragStart: dragStart
          };

          const isSelected = selectedDataListItemId === id;

          const selectableInputAriaProps = selectableInputAriaLabel
            ? { 'aria-label': selectableInputAriaLabel }
            : { 'aria-labelledby': ariaLabelledBy };

          const selectableInputType = selectableRow?.type === 'multiple' ? 'checkbox' : 'radio';

          return (
            <li
              id={id}
              className={css(
                styles.dataListItem,
                isExpanded && styles.modifiers.expanded,
                isSelectable && styles.modifiers.selectable,
                selectedDataListItemId && isSelected && styles.modifiers.selected,
                className
              )}
              aria-labelledby={ariaLabelledBy}
              {...(isSelectable && { tabIndex: 0, onClick: selectDataListItem, onKeyDown })}
              {...(isSelectable && isSelected && { 'aria-selected': true })}
              {...props}
              {...dragProps}
            >
              {selectableRow && (
                <input
                  className="pf-screen-reader"
                  type={selectableInputType}
                  checked={isSelected}
                  onChange={event => selectableRow.onChange(id, event)}
                  tabIndex={-1}
                  {...selectableInputAriaProps}
                />
              )}
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
  }
}
