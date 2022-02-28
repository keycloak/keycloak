import * as React from 'react';
import { DropdownToggle, DropdownContext } from '../Dropdown';

export interface OptionsMenuToggleProps extends React.HTMLProps<HTMLButtonElement> {
  /** Id of the parent options menu component */
  parentId?: string;
  /** Callback for when this options menu is toggled */
  onToggle?: (isOpen: boolean) => void;
  /** Flag to indicate if menu is open */
  isOpen?: boolean;
  /** Flag to indicate if the button is plain */
  isPlain?: boolean;
  /** Forces display of the hover state of the options menu */
  isFocused?: boolean;
  /** Forces display of the hover state of the options menu */
  isHovered?: boolean;
  isSplitButton?: boolean;
  /** Forces display of the active state of the options menu */
  isActive?: boolean;
  /** Disables the options menu toggle */
  isDisabled?: boolean;
  /** hide the toggle caret */
  hideCaret?: boolean;
  /** Provides an accessible name for the button when an icon is used instead of text */
  'aria-label'?: string;
  /** Internal function to implement enter click */
  onEnter?: (event: React.MouseEvent<HTMLButtonElement>) => void;
  /** Internal parent reference */
  parentRef?: HTMLElement;
  /** Content to be rendered in the options menu toggle button */
  toggleTemplate?: React.ReactNode;
}

export const OptionsMenuToggle: React.FunctionComponent<OptionsMenuToggleProps> = ({
  isPlain = false,
  isHovered = false,
  isActive = false,
  isFocused = false,
  isDisabled = false,
  isOpen = false,
  parentId = '',
  toggleTemplate = <React.Fragment />,
  hideCaret = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  isSplitButton = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  type,
  'aria-label': ariaLabel = 'Options menu',
  ...props
}: OptionsMenuToggleProps) => (
  <DropdownContext.Consumer>
    {({ id: contextId }) => (
      <DropdownToggle
        {...((isPlain || hideCaret) && { iconComponent: null })}
        {...props}
        isPlain={isPlain}
        isOpen={isOpen}
        isDisabled={isDisabled}
        isHovered={isHovered}
        isActive={isActive}
        isFocused={isFocused}
        id={parentId ? `${parentId}-toggle` : `${contextId}-toggle`}
        ariaHasPopup="listbox"
        aria-label={ariaLabel}
        aria-expanded={isOpen}
        {...(toggleTemplate ? { children: toggleTemplate } : {})}
      />
    )}
  </DropdownContext.Consumer>
);
