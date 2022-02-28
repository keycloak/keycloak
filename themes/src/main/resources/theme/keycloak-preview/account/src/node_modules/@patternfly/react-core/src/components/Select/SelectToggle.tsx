import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import { KeyTypes, SelectVariant } from './selectConstants';
import { PickOptional } from '../../helpers/typeUtils';

export interface SelectToggleProps extends React.HTMLProps<HTMLElement> {
  /** HTML ID of dropdown toggle */
  id: string;
  /** Anything which can be rendered as dropdown toggle */
  children: React.ReactNode;
  /** Classes applied to root element of dropdown toggle */
  className?: string;
  /** Flag to indicate if select is expanded */
  isExpanded?: boolean;
  /** Callback called when toggle is clicked */
  onToggle?: (isExpanded: boolean) => void;
  /** Callback for toggle open on keyboard entry */
  onEnter?: () => void;
  /** Callback for toggle close */
  onClose?: () => void;
  /** Internal callback for toggle keyboard navigation */
  handleTypeaheadKeys?: (position: string) => void;
  /** Element which wraps toggle */
  parentRef: React.RefObject<HTMLDivElement>;
  /** Forces focus state */
  isFocused?: boolean;
  /** Forces hover state */
  isHovered?: boolean;
  /** Forces active state */
  isActive?: boolean;
  /** Display the toggle with no border or background */
  isPlain?: boolean;
  /** Flag indicating if select is disabled */
  isDisabled?: boolean;
  /** Type of the toggle button, defaults to 'button' */
  type?: 'reset' | 'button' | 'submit' | undefined;
  /** Id of label for the Select aria-labelledby */
  ariaLabelledBy?: string;
  /** Label for toggle of select variants */
  ariaLabelToggle?: string;
  /** Flag for variant, determines toggle rules and interaction */
  variant?: 'single' | 'checkbox' | 'typeahead' | 'typeaheadmulti';
  /** Flag indicating if select toggle has an clear button */
  hasClearButton?: boolean;
}

export class SelectToggle extends React.Component<SelectToggleProps> {
  private toggle: React.RefObject<HTMLDivElement> | React.RefObject<HTMLButtonElement>;

  static defaultProps: PickOptional<SelectToggleProps> = {
    className: '',
    isExpanded: false,
    isFocused: false,
    isHovered: false,
    isActive: false,
    isPlain: false,
    isDisabled: false,
    hasClearButton: false,
    variant: 'single',
    ariaLabelledBy: '',
    ariaLabelToggle: '',
    type: 'button',
    onToggle: () => {},
    onEnter: () => {},
    onClose: () => {}
  };

  constructor(props: SelectToggleProps) {
    super(props);
    const { variant } = props;
    const isTypeahead = variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti;
    this.toggle = isTypeahead ? React.createRef<HTMLDivElement>() : React.createRef<HTMLButtonElement>();
  }

  componentDidMount() {
    document.addEventListener('mousedown', this.onDocClick);
    document.addEventListener('touchstart', this.onDocClick);
    document.addEventListener('keydown', this.onEscPress);
  }

  componentWillUnmount() {
    document.removeEventListener('mousedown', this.onDocClick);
    document.removeEventListener('touchstart', this.onDocClick);
    document.removeEventListener('keydown', this.onEscPress);
  }

  onDocClick = (event: Event) => {
    const { parentRef, isExpanded, onToggle, onClose } = this.props;
    if (isExpanded && parentRef && !parentRef.current.contains(event.target as Node)) {
      onToggle(false);
      onClose();
      this.toggle.current.focus();
    }
  };

  onEscPress = (event: KeyboardEvent) => {
    const { parentRef, isExpanded, variant, onToggle, onClose } = this.props;
    if (event.key === KeyTypes.Tab && variant === SelectVariant.checkbox) {
      return;
    }
    if (
      isExpanded &&
      (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) &&
      parentRef &&
      parentRef.current.contains(event.target as Node)
    ) {
      onToggle(false);
      onClose();
      this.toggle.current.focus();
    }
  };

  onKeyDown = (event: React.KeyboardEvent) => {
    const { isExpanded, onToggle, variant, onClose, onEnter, handleTypeaheadKeys } = this.props;
    if (
      (event.key === KeyTypes.ArrowDown || event.key === KeyTypes.ArrowUp) &&
      (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti)
    ) {
      handleTypeaheadKeys((event.key === KeyTypes.ArrowDown && 'down') || (event.key === KeyTypes.ArrowUp && 'up'));
    }
    if (
      event.key === KeyTypes.Enter &&
      (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti)
    ) {
      if (isExpanded) {
        handleTypeaheadKeys('enter');
      } else {
        onToggle(!isExpanded);
      }
    }

    if (
      (event.key === KeyTypes.Tab && variant === SelectVariant.checkbox) ||
      (event.key === KeyTypes.Tab && !isExpanded) ||
      (event.key !== KeyTypes.Enter && event.key !== KeyTypes.Space) ||
      ((event.key === KeyTypes.Space || event.key === KeyTypes.Enter) &&
        (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti))
    ) {
      return;
    }
    event.preventDefault();
    if ((event.key === KeyTypes.Tab || event.key === KeyTypes.Enter || event.key === KeyTypes.Space) && isExpanded) {
      onToggle(!isExpanded);
      onClose();
      this.toggle.current.focus();
    } else if ((event.key === KeyTypes.Enter || event.key === KeyTypes.Space) && !isExpanded) {
      onToggle(!isExpanded);
      onEnter();
    }
  };

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const {
      className,
      children,
      isExpanded,
      isFocused,
      isActive,
      isHovered,
      isPlain,
      isDisabled,
      variant,
      onToggle,
      onEnter,
      onClose,
      handleTypeaheadKeys,
      parentRef,
      id,
      type,
      hasClearButton,
      ariaLabelledBy,
      ariaLabelToggle,
      ...props
    } = this.props;
    /* eslint-enable @typescript-eslint/no-unused-vars */
    const isTypeahead =
      variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti || hasClearButton;
    const toggleProps: {
      id: string;
      'aria-labelledby': string;
      'aria-expanded': boolean;
      'aria-haspopup': 'listbox' | null;
    } = {
      id,
      'aria-labelledby': ariaLabelledBy,
      'aria-expanded': isExpanded,
      'aria-haspopup': (variant !== SelectVariant.checkbox && 'listbox') || null
    };
    return (
      <React.Fragment>
        {!isTypeahead && (
          <button
            {...props}
            {...toggleProps}
            ref={this.toggle as React.RefObject<HTMLButtonElement>}
            type={type}
            className={css(
              styles.selectToggle,
              isFocused && styles.modifiers.focus,
              isHovered && styles.modifiers.hover,
              isDisabled && styles.modifiers.disabled,
              isActive && styles.modifiers.active,
              isPlain && styles.modifiers.plain,
              className
            )}
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            onClick={_event => {
              onToggle(!isExpanded);
              if (isExpanded) {
                onClose();
              }
            }}
            onKeyDown={this.onKeyDown}
            disabled={isDisabled}
          >
            {children}
            <CaretDownIcon className={css(styles.selectToggleArrow)} />
          </button>
        )}
        {isTypeahead && (
          <div
            {...props}
            ref={this.toggle as React.RefObject<HTMLDivElement>}
            className={css(
              styles.selectToggle,
              isFocused && styles.modifiers.focus,
              isHovered && styles.modifiers.hover,
              isActive && styles.modifiers.active,
              isDisabled && styles.modifiers.disabled,
              isPlain && styles.modifiers.plain,
              isTypeahead && styles.modifiers.typeahead,
              className
            )}
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            onClick={_event => {
              if (!isDisabled) {
                onToggle(true);
              }
            }}
            onKeyDown={this.onKeyDown}
          >
            {children}
            <button
              {...toggleProps}
              type={type}
              className={css(buttonStyles.button, styles.selectToggleButton, styles.modifiers.plain)}
              aria-label={ariaLabelToggle}
              onClick={_event => {
                _event.stopPropagation();
                onToggle(!isExpanded);
                if (isExpanded) {
                  onClose();
                }
              }}
              disabled={isDisabled}
            >
              <CaretDownIcon className={css(styles.selectToggleArrow)} />
            </button>
          </div>
        )}
      </React.Fragment>
    );
  }
}
