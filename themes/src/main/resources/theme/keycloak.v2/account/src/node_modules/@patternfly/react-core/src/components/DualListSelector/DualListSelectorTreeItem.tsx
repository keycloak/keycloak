import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { css } from '@patternfly/react-styles';
import { DualListSelectorTreeItemData } from './DualListSelectorTree';
import { Badge } from '../Badge';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { flattenTree } from './treeUtils';
import { DualListSelectorListContext } from './DualListSelectorContext';

export interface DualListSelectorTreeItemProps extends React.HTMLProps<HTMLLIElement> {
  /** Content rendered inside the dual list selector. */
  children?: React.ReactNode;
  /** Additional classes applied to the dual list selector. */
  className?: string;
  /** Flag indicating this option is expanded by default. */
  defaultExpanded?: boolean;
  /** Flag indicating this option has a badge */
  hasBadge?: boolean;
  /** Callback fired when an option is checked */
  onOptionCheck?: (
    event: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent,
    isChecked: boolean,
    itemData: DualListSelectorTreeItemData
  ) => void;
  /** ID of the option */
  id: string;
  /** Text of the option */
  text: string;
  /** Flag indicating if this open is checked. */
  isChecked?: boolean;
  /** Additional properties to pass to the option checkbox */
  checkProps?: any;
  /** Additional properties to pass to the option badge */
  badgeProps?: any;
  /** Raw data of the option */
  itemData?: DualListSelectorTreeItemData;
  /** Flag indicating whether the component is disabled. */
  isDisabled?: boolean;
  /** Flag indicating the DualListSelector tree should utilize memoization to help render large data sets. */
  useMemo?: boolean;
}

const DualListSelectorTreeItemBase: React.FunctionComponent<DualListSelectorTreeItemProps> = ({
  onOptionCheck,
  children,
  className,
  id,
  text,
  defaultExpanded,
  hasBadge,
  isChecked,
  checkProps,
  badgeProps,
  itemData,
  isDisabled = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  useMemo,
  ...props
}: DualListSelectorTreeItemProps) => {
  const ref = React.useRef(null);
  const [isExpanded, setIsExpanded] = React.useState(defaultExpanded || false);
  const { setFocusedOption } = React.useContext(DualListSelectorListContext);

  React.useEffect(() => {
    setIsExpanded(defaultExpanded);
  }, [defaultExpanded]);

  return (
    <li
      className={css(
        styles.dualListSelectorListItem,
        className,
        children && styles.modifiers.expandable,
        isExpanded && styles.modifiers.expanded,
        isDisabled && styles.modifiers.disabled
      )}
      id={id}
      {...props}
      aria-selected={isChecked}
      role="treeitem"
      {...(isExpanded && { 'aria-expanded': 'true' })}
    >
      <div
        className={css(
          styles.dualListSelectorListItemRow,
          isChecked && styles.modifiers.selected,
          styles.modifiers.check
        )}
      >
        <div
          className={css(styles.dualListSelectorItem)}
          ref={ref}
          tabIndex={-1}
          onClick={
            isDisabled
              ? undefined
              : evt => {
                  onOptionCheck && onOptionCheck(evt, !isChecked, itemData);
                  setFocusedOption(id);
                }
          }
        >
          <span className={css(styles.dualListSelectorItemMain)}>
            {children && (
              <div
                className={css(styles.dualListSelectorItemToggle)}
                onClick={e => {
                  if (children) {
                    setIsExpanded(!isExpanded);
                  }
                  e.stopPropagation();
                }}
                onKeyDown={(e: React.KeyboardEvent) => {
                  if (e.key === ' ' || e.key === 'Enter') {
                    (document.activeElement as HTMLElement).click();
                    e.preventDefault();
                  }
                }}
                tabIndex={-1}
              >
                <span className={css(styles.dualListSelectorItemToggleIcon)}>
                  <AngleRightIcon aria-hidden />
                </span>
              </div>
            )}
            <span className={css(styles.dualListSelectorItemCheck)}>
              <input
                type="checkbox"
                onChange={(evt: React.ChangeEvent<HTMLInputElement>) => {
                  onOptionCheck && onOptionCheck(evt, !isChecked, itemData);
                  setFocusedOption(id);
                }}
                onClick={(evt: React.MouseEvent) => evt.stopPropagation()}
                onKeyDown={(e: React.KeyboardEvent) => {
                  if (e.key === ' ' || e.key === 'Enter') {
                    onOptionCheck && onOptionCheck(e, !isChecked, itemData);
                    setFocusedOption(id);
                    e.preventDefault();
                  }
                }}
                ref={elem => elem && (elem.indeterminate = isChecked === null)}
                checked={isChecked || false}
                tabIndex={-1}
                {...checkProps}
              />
            </span>

            <span className={css(styles.dualListSelectorItemText)}>{text}</span>
            {hasBadge && children && (
              <span className={css(styles.dualListSelectorItemCount)}>
                <Badge {...badgeProps}>{flattenTree((children as React.ReactElement).props.data).length}</Badge>
              </span>
            )}
          </span>
        </div>
      </div>
      {isExpanded && children}
    </li>
  );
};

export const DualListSelectorTreeItem = React.memo(DualListSelectorTreeItemBase, (prevProps, nextProps) => {
  if (!nextProps.useMemo) {
    return false;
  }

  if (
    prevProps.className !== nextProps.className ||
    prevProps.text !== nextProps.text ||
    prevProps.id !== nextProps.id ||
    prevProps.defaultExpanded !== nextProps.defaultExpanded ||
    prevProps.checkProps !== nextProps.checkProps ||
    prevProps.hasBadge !== nextProps.hasBadge ||
    prevProps.badgeProps !== nextProps.badgeProps ||
    prevProps.isChecked !== nextProps.isChecked ||
    prevProps.itemData !== nextProps.itemData
  ) {
    return false;
  }

  return true;
});

DualListSelectorTreeItem.displayName = 'DualListSelectorTreeItem';
