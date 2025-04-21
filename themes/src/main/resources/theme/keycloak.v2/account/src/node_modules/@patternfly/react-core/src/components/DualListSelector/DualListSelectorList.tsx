import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { DualListSelectorListItem } from './DualListSelectorListItem';
import * as React from 'react';
import { DualListSelectorListContext } from './DualListSelectorContext';

export interface DualListSelectorListProps extends React.HTMLProps<HTMLUListElement> {
  children?: React.ReactNode;
}

export const DualListSelectorList: React.FunctionComponent<DualListSelectorListProps> = ({
  children,
  ...props
}: DualListSelectorListProps) => {
  const {
    setFocusedOption,
    isTree,
    ariaLabelledBy,
    focusedOption,
    displayOption,
    selectedOptions,
    id,
    onOptionSelect,
    options,
    isDisabled
  } = React.useContext(DualListSelectorListContext);

  // only called when options are passed via options prop
  const onOptionClick = (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, index: number, id: string) => {
    setFocusedOption(id);
    onOptionSelect(e, index, id);
  };

  return (
    <ul
      className={css(styles.dualListSelectorList)}
      role={isTree ? 'tree' : 'listbox'}
      aria-multiselectable="true"
      aria-labelledby={ariaLabelledBy}
      aria-activedescendant={focusedOption}
      aria-disabled={isDisabled ? 'true' : undefined}
      {...props}
    >
      {options.length === 0
        ? children
        : options.map((option, index) => {
            if (displayOption(option)) {
              return (
                <DualListSelectorListItem
                  key={index}
                  isSelected={(selectedOptions as number[]).indexOf(index) !== -1}
                  id={`${id}-option-${index}`}
                  onOptionSelect={(e, id) => onOptionClick(e, index, id)}
                  orderIndex={index}
                  isDisabled={isDisabled}
                >
                  {option}
                </DualListSelectorListItem>
              );
            }
            return;
          })}
    </ul>
  );
};
DualListSelectorList.displayName = 'DualListSelectorList';
