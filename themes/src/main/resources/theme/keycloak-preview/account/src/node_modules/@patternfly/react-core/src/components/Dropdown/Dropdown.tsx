import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownPosition, DropdownDirection, DropdownContext } from './dropdownConstants';
import { DropdownWithContext } from './DropdownWithContext';

export interface DropdownProps extends React.HTMLProps<HTMLDivElement> {
  /** Anything which can be rendered in a dropdown */
  children?: React.ReactNode;
  /** Classes applied to root element of dropdown */
  className?: string;
  /** Array of DropdownItem nodes that will be rendered in the dropdown Menu list */
  dropdownItems?: any[];
  /** Flag to indicate if menu is opened */
  isOpen?: boolean;
  /** Display the toggle with no border or background */
  isPlain?: boolean;
  /** Indicates where menu will be aligned horizontally */
  position?: DropdownPosition | 'right' | 'left';
  /** Display menu above or below dropdown toggle */
  direction?: DropdownDirection | 'up' | 'down';
  /** Flag to indicate if dropdown has groups */
  isGrouped?: boolean;
  /** Toggle for the dropdown, examples: <DropdownToggle> or <DropdownToggleCheckbox> */
  toggle: React.ReactElement<any>;
  /** Function callback called when user selects item */
  onSelect?: (event?: React.SyntheticEvent<HTMLDivElement>) => void;
  /** Flag to indicate if the first dropdown item should gain initial focus, set false when adding
   * a specific auto-focus item (like a current selection) otherwise leave as true
   */
  autoFocus?: boolean;
  ouiaComponentType?: string;
}

export const Dropdown: React.FunctionComponent<DropdownProps> = ({
  onSelect,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  ref, // Types of Ref are different for React.FC vs React.Component
  ...props
}: DropdownProps) => (
  <DropdownContext.Provider
    value={{
      onSelect: event => onSelect && onSelect(event),
      toggleTextClass: styles.dropdownToggleText,
      toggleIconClass: styles.dropdownToggleIcon,
      menuClass: styles.dropdownMenu,
      itemClass: styles.dropdownMenuItem,
      toggleClass: styles.dropdownToggle,
      baseClass: styles.dropdown,
      baseComponent: 'div',
      sectionClass: styles.dropdownGroup,
      sectionTitleClass: styles.dropdownGroupTitle,
      sectionComponent: 'section',
      disabledClass: styles.modifiers.disabled,
      hoverClass: styles.modifiers.hover,
      separatorClass: styles.dropdownSeparator
    }}
  >
    <DropdownWithContext {...props} />
  </DropdownContext.Provider>
);
