import * as React from 'react';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import { Toggle } from './Toggle';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { css } from '@patternfly/react-styles';

export interface DropdownToggleProps extends React.HTMLProps<HTMLButtonElement> {
  /** HTML ID of dropdown toggle */
  id?: string;
  /** Anything which can be rendered as dropdown toggle button */
  children?: React.ReactNode;
  /** Classes applied to root element of dropdown toggle button */
  className?: string;
  /** Flag to indicate if menu is opened */
  isOpen?: boolean;
  /** Callback called when toggle is clicked */
  onToggle?: (isOpen: boolean) => void;
  /** Element which wraps toggle */
  parentRef?: HTMLElement;
  /** Forces focus state */
  isFocused?: boolean;
  /** Forces hover state */
  isHovered?: boolean;
  /** Forces active state */
  isActive?: boolean;
  /** Display the toggle with no border or background */
  isPlain?: boolean;
  /** Whether or not the <div> has a disabled state */
  isDisabled?: boolean;
  /** Whether or not the dropdown toggle button should have primary button styling */
  isPrimary?: boolean;
  /** The icon to display for the toggle. Defaults to CaretDownIcon. Set to null to not show an icon. */
  iconComponent?: React.ElementType | null;
  /** Elements to display before the toggle button. When included, renders the toggle as a split button. */
  splitButtonItems?: React.ReactNode[];
  /** Variant of split button toggle */
  splitButtonVariant?: 'action' | 'checkbox';
  /** Accessible label for the dropdown toggle button */
  'aria-label'?: string;
  /** Accessibility property to indicate correct has popup */
  ariaHasPopup?: boolean | 'listbox' | 'menu' | 'dialog' | 'grid' | 'listbox' | 'tree';
  /** Type to put on the button */
  type?: 'button' | 'submit' | 'reset';
  /** Callback called when the Enter key is pressed */
  onEnter?: (event?: React.MouseEvent<HTMLButtonElement>) => void;
}

export const DropdownToggle: React.FunctionComponent<DropdownToggleProps> = ({
  id = '',
  children = null,
  className = '',
  isOpen = false,
  parentRef = null,
  isFocused = false,
  isHovered = false,
  isActive = false,
  isDisabled = false,
  isPlain = false,
  isPrimary = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onToggle = (_isOpen: boolean) => undefined as any,
  iconComponent: IconComponent = CaretDownIcon,
  splitButtonItems,
  splitButtonVariant = 'checkbox',
  ariaHasPopup,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  ref, // Types of Ref are different for React.FC vs React.Component
  ...props
}: DropdownToggleProps) => {
  const toggle = (
    <DropdownContext.Consumer>
      {({ toggleTextClass, toggleIconClass }) => (
        <Toggle
          {...props}
          id={id}
          className={className}
          isOpen={isOpen}
          parentRef={parentRef}
          isFocused={isFocused}
          isHovered={isHovered}
          isActive={isActive}
          isDisabled={isDisabled}
          isPlain={isPlain}
          isPrimary={isPrimary}
          onToggle={onToggle}
          ariaHasPopup={ariaHasPopup}
          {...(splitButtonItems && { isSplitButton: true, 'aria-label': props['aria-label'] || 'Select' })}
        >
          {children && <span className={IconComponent && css(toggleTextClass)}>{children}</span>}
          {IconComponent && <IconComponent className={css(children && toggleIconClass)} />}
        </Toggle>
      )}
    </DropdownContext.Consumer>
  );

  if (splitButtonItems) {
    return (
      <div
        className={css(
          styles.dropdownToggle,
          styles.modifiers.splitButton,
          splitButtonVariant === 'action' && styles.modifiers.action,
          isDisabled && styles.modifiers.disabled
        )}
      >
        {splitButtonItems}
        {toggle}
      </div>
    );
  }

  return toggle;
};
