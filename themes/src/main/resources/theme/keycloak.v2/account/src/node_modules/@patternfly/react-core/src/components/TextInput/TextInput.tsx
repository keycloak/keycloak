import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FormControl/form-control';
import { css } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';
import { trimLeft } from '../../helpers/util';
import { getDefaultOUIAId, getOUIAProps, OUIAProps } from '../../helpers';
import { getResizeObserver } from '../../helpers/resizeObserver';

export enum TextInputTypes {
  text = 'text',
  date = 'date',
  datetimeLocal = 'datetime-local',
  email = 'email',
  month = 'month',
  number = 'number',
  password = 'password',
  search = 'search',
  tel = 'tel',
  time = 'time',
  url = 'url'
}

export interface TextInputProps
  extends Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'onFocus' | 'onBlur' | 'disabled' | 'ref'>,
    OUIAProps {
  /** Additional classes added to the TextInput. */
  className?: string;
  /** Flag to show if the input is disabled. */
  isDisabled?: boolean;
  /** Flag to show if the input is read only. */
  isReadOnly?: boolean;
  /** Flag to show if the input is required. */
  isRequired?: boolean;
  /** Value to indicate if the input is modified to show that validation state.
   * If set to success, input will be modified to indicate valid state.
   * If set to error,  input will be modified to indicate error state.
   */
  validated?: 'success' | 'warning' | 'error' | 'default';
  /** A callback for when the input value changes. */
  onChange?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
  /** Type that the input accepts. */
  type?:
    | 'text'
    | 'date'
    | 'datetime-local'
    | 'email'
    | 'month'
    | 'number'
    | 'password'
    | 'search'
    | 'tel'
    | 'time'
    | 'url';
  /** Value of the input. */
  value?: string | number;
  /** Aria-label. The input requires an associated id or aria-label. */
  'aria-label'?: string;
  /** A reference object to attach to the input box. */
  innerRef?: React.RefObject<any>;
  /** Trim text on left */
  isLeftTruncated?: boolean;
  /** Callback function when input is focused */
  onFocus?: (event?: any) => void;
  /** Callback function when input is blurred (focus leaves) */
  onBlur?: (event?: any) => void;
  /** icon variant */
  iconVariant?: 'calendar' | 'clock' | 'search';
  /** Use the external file instead of a data URI */
  isIconSprite?: boolean;
  /** Custom icon url to set as the input's background-image */
  customIconUrl?: string;
  /** Dimensions for the custom icon set as the input's background-size */
  customIconDimensions?: string;
}

interface TextInputState {
  ouiaStateId: string;
}

export class TextInputBase extends React.Component<TextInputProps, TextInputState> {
  static displayName = 'TextInputBase';
  static defaultProps: TextInputProps = {
    'aria-label': null,
    className: '',
    isRequired: false,
    validated: 'default' as 'success' | 'warning' | 'error' | 'default',
    isDisabled: false,
    isReadOnly: false,
    isIconSprite: false,
    type: TextInputTypes.text,
    isLeftTruncated: false,
    onChange: (): any => undefined,
    ouiaSafe: true
  };
  inputRef = React.createRef<HTMLInputElement>();
  observer: any = () => {};

  constructor(props: TextInputProps) {
    super(props);
    if (!props.id && !props['aria-label'] && !props['aria-labelledby']) {
      // eslint-disable-next-line no-console
      console.error('Text input:', 'Text input requires either an id or aria-label to be specified');
    }
    this.state = {
      ouiaStateId: getDefaultOUIAId(TextInputBase.displayName)
    };
  }

  handleChange = (event: React.FormEvent<HTMLInputElement>) => {
    if (this.props.onChange) {
      this.props.onChange(event.currentTarget.value, event);
    }
  };

  componentDidMount() {
    if (this.props.isLeftTruncated) {
      const inputRef = this.props.innerRef || this.inputRef;
      this.observer = getResizeObserver(inputRef.current, this.handleResize);
      this.handleResize();
    }
  }

  componentWillUnmount() {
    if (this.props.isLeftTruncated) {
      this.observer();
    }
  }

  handleResize = () => {
    const inputRef = this.props.innerRef || this.inputRef;
    if (inputRef && inputRef.current) {
      trimLeft(inputRef.current, String(this.props.value));
    }
  };

  restoreText = () => {
    const inputRef = this.props.innerRef || this.inputRef;
    // restore the value
    (inputRef.current as HTMLInputElement).value = String(this.props.value);
    // make sure we still see the rightmost value to preserve cursor click position
    inputRef.current.scrollLeft = inputRef.current.scrollWidth;
  };

  onFocus = (event?: any) => {
    const { isLeftTruncated, onFocus } = this.props;
    if (isLeftTruncated) {
      this.restoreText();
    }
    onFocus && onFocus(event);
  };

  onBlur = (event?: any) => {
    const { isLeftTruncated, onBlur } = this.props;
    if (isLeftTruncated) {
      this.handleResize();
    }
    onBlur && onBlur(event);
  };

  render() {
    const {
      innerRef,
      className,
      type,
      value,
      validated,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      onChange,
      onFocus,
      onBlur,
      isLeftTruncated,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      isReadOnly,
      isRequired,
      isDisabled,
      isIconSprite,
      iconVariant,
      customIconUrl,
      customIconDimensions,
      ouiaId,
      ouiaSafe,
      ...props
    } = this.props;

    const customIconStyle = {} as any;
    if (customIconUrl) {
      customIconStyle.backgroundImage = `url('${customIconUrl}')`;
    }
    if (customIconDimensions) {
      customIconStyle.backgroundSize = customIconDimensions;
    }

    return (
      <input
        {...props}
        onFocus={this.onFocus}
        onBlur={this.onBlur}
        className={css(
          styles.formControl,
          isIconSprite && styles.modifiers.iconSprite,
          validated === ValidatedOptions.success && styles.modifiers.success,
          validated === ValidatedOptions.warning && styles.modifiers.warning,
          ((iconVariant && iconVariant !== 'search') || customIconUrl) && styles.modifiers.icon,
          iconVariant && styles.modifiers[iconVariant],
          className
        )}
        onChange={this.handleChange}
        type={type}
        value={this.sanitizeInputValue(value)}
        aria-invalid={props['aria-invalid'] ? props['aria-invalid'] : validated === ValidatedOptions.error}
        required={isRequired}
        disabled={isDisabled}
        readOnly={isReadOnly}
        ref={innerRef || this.inputRef}
        {...((customIconUrl || customIconDimensions) && { style: customIconStyle })}
        {...getOUIAProps(TextInput.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
      />
    );
  }

  private sanitizeInputValue = (value: string | number) =>
    typeof value === 'string' ? value.replace(/\n/g, ' ') : value;
}

export const TextInput = React.forwardRef((props: TextInputProps, ref: React.Ref<HTMLInputElement>) => (
  <TextInputBase {...props} innerRef={ref as React.MutableRefObject<any>} />
));
TextInput.displayName = 'TextInput';
