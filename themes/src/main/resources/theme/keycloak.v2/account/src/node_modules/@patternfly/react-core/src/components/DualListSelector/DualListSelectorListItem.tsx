import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { css } from '@patternfly/react-styles';
import { getUniqueId } from '../../helpers';
import GripVerticalIcon from '@patternfly/react-icons/dist/esm/icons/grip-vertical-icon';
import { Button, ButtonVariant } from '../Button';
import { DualListSelectorListContext } from './DualListSelectorContext';

export interface DualListSelectorListItemProps extends React.HTMLProps<HTMLLIElement> {
  /** Content rendered inside the dual list selector. */
  children?: React.ReactNode;
  /** Additional classes applied to the dual list selector. */
  className?: string;
  /** Flag indicating the list item is currently selected. */
  isSelected?: boolean;
  /** Callback fired when an option is selected.  */
  onOptionSelect?: (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, id?: string) => void;
  /** ID of the option. */
  id?: string;
  /** @hide Internal field used to keep track of order of unfiltered options. */
  orderIndex?: number;
  /** @hide Forwarded ref */
  innerRef?: React.RefObject<HTMLLIElement>;
  /** Flag indicating this item is draggable for reordring */
  isDraggable?: boolean;
  /** Accessible label for the draggable button on draggable list items */
  draggableButtonAriaLabel?: string;
  /** Flag indicating if the dual list selector is in a disabled state */
  isDisabled?: boolean;
}

export const DualListSelectorListItemBase: React.FunctionComponent<DualListSelectorListItemProps> = ({
  onOptionSelect,
  orderIndex,
  children,
  className,
  id = getUniqueId('dual-list-selector-list-item'),
  isSelected,
  innerRef,
  isDraggable = false,
  isDisabled,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  draggableButtonAriaLabel = 'Reorder option',
  ...props
}: DualListSelectorListItemProps) => {
  const ref = innerRef || React.useRef<HTMLLIElement>(null);
  const { setFocusedOption } = React.useContext(DualListSelectorListContext);

  return (
    <li
      className={css(styles.dualListSelectorListItem, className, isDisabled && styles.modifiers.disabled)}
      key={orderIndex}
      onClick={
        isDisabled
          ? undefined
          : (e: React.MouseEvent) => {
              setFocusedOption(id);
              onOptionSelect(e, id);
            }
      }
      onKeyDown={(e: React.KeyboardEvent) => {
        if (e.key === ' ' || e.key === 'Enter') {
          (document.activeElement as HTMLElement).click();
          e.preventDefault();
        }
      }}
      aria-selected={isSelected}
      id={id}
      ref={ref}
      role="option"
      tabIndex={-1}
      {...props}
    >
      <div className={css(styles.dualListSelectorListItemRow, isSelected && styles.modifiers.selected)}>
        {isDraggable && !isDisabled && (
          <div className={css(styles.dualListSelectorDraggable)}>
            {/** TODO once keyboard accessibility is enabled, remove `component=span`
             and add `aria-label={draggableButtonAriaLabel}` */}
            <Button variant={ButtonVariant.plain} component="span">
              <GripVerticalIcon style={{ verticalAlign: '-0.3em' }} />
            </Button>
          </div>
        )}
        <span className={css(styles.dualListSelectorItem)}>
          <span className={css(styles.dualListSelectorItemMain)}>
            <span className={css(styles.dualListSelectorItemText)}>{children}</span>
          </span>
        </span>
      </div>
    </li>
  );
};
DualListSelectorListItemBase.displayName = 'DualListSelectorListItemBase';

export const DualListSelectorListItem = React.forwardRef(
  (props: DualListSelectorListItemProps, ref: React.Ref<HTMLLIElement>) => (
    <DualListSelectorListItemBase innerRef={ref as React.MutableRefObject<any>} {...props} />
  )
);
DualListSelectorListItem.displayName = 'DualListSelectorListItem';
