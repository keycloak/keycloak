import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { css } from '@patternfly/react-styles';
import { KEY_CODES } from '../../helpers/constants';
import { PickOptional } from '../../helpers/typeUtils';

export interface ToggleProps {
  /** HTML ID of dropdown toggle */
  id: string;
  /** Type to put on the button */
  type?: 'button' | 'submit' | 'reset';
  /** Anything which can be rendered as dropdown toggle */
  children?: React.ReactNode;
  /** Classes applied to root element of dropdown toggle */
  className?: string;
  /** Flag to indicate if menu is opened */
  isOpen?: boolean;
  /** Callback called when toggle is clicked */
  onToggle?: (
    isOpen: boolean,
    event: MouseEvent | TouchEvent | KeyboardEvent | React.KeyboardEvent<any> | React.MouseEvent<HTMLButtonElement>
  ) => void;
  /** Callback called when the Enter key is pressed */
  onEnter?: () => void;
  /** Element which wraps toggle */
  parentRef?: any;
  /** Forces focus state */
  isFocused?: boolean;
  /** Forces hover state */
  isHovered?: boolean;
  /** Forces active state */
  isActive?: boolean;
  /** Disables the dropdown toggle */
  isDisabled?: boolean;
  /** Display the toggle with no border or background */
  isPlain?: boolean;
  /** Display the toggle with a primary button style */
  isPrimary?: boolean;
  /** Style the toggle as a child of a split button */
  isSplitButton?: boolean;
  /** Flag for aria popup */
  ariaHasPopup?: boolean | 'listbox' | 'menu' | 'dialog' | 'grid' | 'listbox' | 'tree';
  /** Allows selecting toggle to select parent */
  bubbleEvent?: boolean;
}

export class Toggle extends React.Component<ToggleProps> {
  private buttonRef = React.createRef<HTMLButtonElement>();

  static defaultProps: PickOptional<ToggleProps> = {
    className: '',
    isOpen: false,
    isFocused: false,
    isHovered: false,
    isActive: false,
    isDisabled: false,
    isPlain: false,
    isPrimary: false,
    isSplitButton: false,
    onToggle: () => {},
    onEnter: () => {},
    bubbleEvent: false
  };

  componentDidMount = () => {
    document.addEventListener('mousedown', event => this.onDocClick(event));
    document.addEventListener('touchstart', event => this.onDocClick(event));
    document.addEventListener('keydown', event => this.onEscPress(event));
  };

  componentWillUnmount = () => {
    document.removeEventListener('mousedown', event => this.onDocClick(event));
    document.removeEventListener('touchstart', event => this.onDocClick(event));
    document.removeEventListener('keydown', event => this.onEscPress(event));
  };

  onDocClick = (event: MouseEvent | TouchEvent) => {
    if (
      this.props.isOpen &&
      this.props.parentRef &&
      this.props.parentRef.current &&
      !this.props.parentRef.current.contains(event.target)
    ) {
      this.props.onToggle(false, event);
      this.buttonRef.current.focus();
    }
  };

  onEscPress = (event: KeyboardEvent) => {
    const { parentRef } = this.props;
    const keyCode = event.keyCode || event.which;
    if (
      this.props.isOpen &&
      (keyCode === KEY_CODES.ESCAPE_KEY || event.key === 'Tab') &&
      parentRef &&
      parentRef.current &&
      parentRef.current.contains(event.target)
    ) {
      this.props.onToggle(false, event);
      this.buttonRef.current.focus();
    }
  };

  onKeyDown = (event: React.KeyboardEvent<any>) => {
    if (event.key === 'Tab' && !this.props.isOpen) {
      return;
    }
    if (!this.props.bubbleEvent) {
      event.stopPropagation();
    }
    event.preventDefault();
    if ((event.key === 'Tab' || event.key === 'Enter' || event.key === ' ') && this.props.isOpen) {
      this.props.onToggle(!this.props.isOpen, event);
    } else if ((event.key === 'Enter' || event.key === ' ') && !this.props.isOpen) {
      this.props.onToggle(!this.props.isOpen, event);
      this.props.onEnter();
    }
  };

  render() {
    const {
      className,
      children,
      isOpen,
      isFocused,
      isActive,
      isHovered,
      isDisabled,
      isPlain,
      isPrimary,
      isSplitButton,
      ariaHasPopup,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      bubbleEvent,
      onToggle,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onEnter,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      parentRef,
      id,
      type,
      ...props
    } = this.props;
    return (
      <DropdownContext.Consumer>
        {({ toggleClass }) => (
          <button
            {...props}
            id={id}
            ref={this.buttonRef}
            className={css(
              isSplitButton ? styles.dropdownToggleButton : toggleClass || styles.dropdownToggle,
              isFocused && styles.modifiers.focus,
              isHovered && styles.modifiers.hover,
              isActive && styles.modifiers.active,
              isPlain && styles.modifiers.plain,
              isPrimary && styles.modifiers.primary,
              className
            )}
            type={type || 'button'}
            onClick={event => onToggle(!isOpen, event)}
            aria-expanded={isOpen}
            aria-haspopup={ariaHasPopup}
            onKeyDown={event => this.onKeyDown(event)}
            disabled={isDisabled}
          >
            {children}
          </button>
        )}
      </DropdownContext.Consumer>
    );
  }
}
