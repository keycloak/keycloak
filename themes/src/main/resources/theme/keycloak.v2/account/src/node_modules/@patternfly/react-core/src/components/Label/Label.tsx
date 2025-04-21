import * as React from 'react';
import { useState } from 'react';
import styles from '@patternfly/react-styles/css/components/Label/label';
import labelGrpStyles from '@patternfly/react-styles/css/components/LabelGroup/label-group';
import { Button } from '../Button';
import { Tooltip, TooltipPosition } from '../Tooltip';
import { css } from '@patternfly/react-styles';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { useIsomorphicLayoutEffect } from '../../helpers';

export interface LabelProps extends React.HTMLProps<HTMLSpanElement> {
  /** Content rendered inside the label. */
  children?: React.ReactNode;
  /** Additional classes added to the label. */
  className?: string;
  /** Color of the label. */
  color?: 'blue' | 'cyan' | 'green' | 'orange' | 'purple' | 'red' | 'grey' | 'gold';
  /** Variant of the label. */
  variant?: 'outline' | 'filled';
  /** Flag indicating the label is compact. */
  isCompact?: boolean;
  /** @beta Flag indicating the label is editable. */
  isEditable?: boolean;
  /** @beta Additional props passed to the editable label text div. Optionally passing onInput and onBlur callbacks will allow finer custom text input control. */
  editableProps?: any;
  /** @beta Callback when an editable label completes an edit. */
  onEditComplete?: (newText: string) => void;
  /** @beta Callback when an editable label cancels an edit. */
  onEditCancel?: (previousText: string) => void;
  /** Flag indicating the label text should be truncated. */
  isTruncated?: boolean;
  /** Position of the tooltip which is displayed if text is truncated */
  tooltipPosition?:
    | TooltipPosition
    | 'auto'
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'top-start'
    | 'top-end'
    | 'bottom-start'
    | 'bottom-end'
    | 'left-start'
    | 'left-end'
    | 'right-start'
    | 'right-end';
  /** Icon added to the left of the label text. */
  icon?: React.ReactNode;
  /** Close click callback for removable labels. If present, label will have a close button. */
  onClose?: (event: React.MouseEvent) => void;
  /** Node for custom close button. */
  closeBtn?: React.ReactNode;
  /** Aria label for close button */
  closeBtnAriaLabel?: string;
  /** Additional properties for the default close button. */
  closeBtnProps?: any;
  /** Href for a label that is a link. If present, the label will change to an anchor element. */
  href?: string;
  /** Flag indicating if the label is an overflow label */
  isOverflowLabel?: boolean;
  /** Forwards the label content and className to rendered function.  Use this prop for react router support.*/
  render?: ({
    className,
    content,
    componentRef
  }: {
    className: string;
    content: React.ReactNode;
    componentRef: any;
  }) => React.ReactNode;
}

const colorStyles = {
  blue: styles.modifiers.blue,
  cyan: styles.modifiers.cyan,
  green: styles.modifiers.green,
  orange: styles.modifiers.orange,
  purple: styles.modifiers.purple,
  red: styles.modifiers.red,
  gold: styles.modifiers.gold,
  grey: ''
};

export const Label: React.FunctionComponent<LabelProps> = ({
  children,
  className = '',
  color = 'grey',
  variant = 'filled',
  isCompact = false,
  isEditable = false,
  editableProps,
  isTruncated = false,
  tooltipPosition,
  icon,
  onClose,
  onEditCancel,
  onEditComplete,
  closeBtn,
  closeBtnAriaLabel,
  closeBtnProps,
  href,
  isOverflowLabel,
  render,
  ...props
}: LabelProps) => {
  const [isEditableActive, setIsEditableActive] = useState(false);
  const [currValue, setCurrValue] = useState(children);
  const editableButtonRef = React.useRef<HTMLButtonElement>();
  const editableInputRef = React.useRef<HTMLInputElement>();

  React.useEffect(() => {
    document.addEventListener('mousedown', onDocMouseDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onDocMouseDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  });

  const onDocMouseDown = (event: MouseEvent) => {
    if (
      isEditableActive &&
      editableInputRef &&
      editableInputRef.current &&
      !editableInputRef.current.contains(event.target as Node)
    ) {
      if (editableInputRef.current.value) {
        onEditComplete && onEditComplete(editableInputRef.current.value);
      }
      setIsEditableActive(false);
    }
  };

  const onKeyDown = (event: KeyboardEvent) => {
    const key = event.key;
    if (
      (!isEditableActive &&
        (!editableButtonRef ||
          !editableButtonRef.current ||
          !editableButtonRef.current.contains(event.target as Node))) ||
      (isEditableActive &&
        (!editableInputRef || !editableInputRef.current || !editableInputRef.current.contains(event.target as Node)))
    ) {
      return;
    }
    if (isEditableActive && (key === 'Enter' || key === 'Tab')) {
      event.preventDefault();
      event.stopImmediatePropagation();
      if (editableInputRef.current.value) {
        onEditComplete && onEditComplete(editableInputRef.current.value);
      }
      setIsEditableActive(false);
    }
    if (isEditableActive && key === 'Escape') {
      event.preventDefault();
      event.stopImmediatePropagation();
      // Reset div text to initial children prop - pre-edit
      if (editableInputRef.current.value) {
        editableInputRef.current.value = children as string;
        onEditCancel && onEditCancel(children as string);
      }
      setIsEditableActive(false);
    }
    if (!isEditableActive && key === 'Enter') {
      event.preventDefault();
      event.stopImmediatePropagation();
      setIsEditableActive(true);

      // Set cursor position to end of text
      const el = event.target as HTMLElement;
      const range = document.createRange();
      const sel = window.getSelection();
      range.selectNodeContents(el);
      range.collapse(false);
      sel.removeAllRanges();
      sel.addRange(range);
    }
  };

  const LabelComponent = (isOverflowLabel ? 'button' : 'span') as any;

  const button = closeBtn ? (
    closeBtn
  ) : (
    <Button
      type="button"
      variant="plain"
      onClick={onClose}
      aria-label={closeBtnAriaLabel || `Close ${children}`}
      {...closeBtnProps}
    >
      <TimesIcon />
    </Button>
  );
  const textRef = React.createRef<any>();
  // ref to apply tooltip when rendered is used
  const componentRef = React.useRef();
  const [isTooltipVisible, setIsTooltipVisible] = React.useState(false);
  useIsomorphicLayoutEffect(() => {
    const currTextRef = isEditable ? editableButtonRef : textRef;
    if (!isEditableActive) {
      setIsTooltipVisible(currTextRef.current && currTextRef.current.offsetWidth < currTextRef.current.scrollWidth);
    }
  }, [isEditableActive]);
  const content = (
    <React.Fragment>
      {icon && <span className={css(styles.labelIcon)}>{icon}</span>}
      {isTruncated && (
        <span ref={textRef} className={css(styles.labelText)}>
          {children}
        </span>
      )}
      {!isTruncated && children}
    </React.Fragment>
  );

  React.useEffect(() => {
    if (isEditableActive && editableInputRef) {
      editableInputRef.current && editableInputRef.current.focus();
    }
  }, [editableInputRef, isEditableActive]);

  const updateVal = () => {
    setCurrValue(editableInputRef.current.value);
  };

  let labelComponentChild = <span className={css(styles.labelContent)}>{content}</span>;

  if (href) {
    labelComponentChild = (
      <a className={css(styles.labelContent)} href={href}>
        {content}
      </a>
    );
  } else if (isEditable) {
    labelComponentChild = (
      <button
        ref={editableButtonRef}
        className={css(styles.labelContent)}
        onClick={(e: React.MouseEvent) => {
          setIsEditableActive(true);
          e.stopPropagation();
        }}
        {...editableProps}
      >
        {content}
      </button>
    );
  }

  if (render) {
    labelComponentChild = (
      <React.Fragment>
        {isTooltipVisible && <Tooltip reference={componentRef} content={children} position={tooltipPosition} />}
        {render({
          className: styles.labelContent,
          content,
          componentRef
        })}
      </React.Fragment>
    );
  } else if (isTooltipVisible) {
    labelComponentChild = (
      <Tooltip content={children} position={tooltipPosition}>
        {labelComponentChild}
      </Tooltip>
    );
  }

  return (
    <LabelComponent
      {...props}
      className={css(
        styles.label,
        colorStyles[color],
        variant === 'outline' && styles.modifiers.outline,
        isOverflowLabel && styles.modifiers.overflow,
        isCompact && styles.modifiers.compact,
        isEditable && labelGrpStyles.modifiers.editable,
        isEditableActive && styles.modifiers.editableActive,
        className
      )}
    >
      {!isEditableActive && labelComponentChild}
      {!isEditableActive && onClose && button}
      {isEditableActive && (
        <input
          className={css(styles.labelContent)}
          type="text"
          id="editable-input"
          ref={editableInputRef}
          value={currValue}
          onChange={updateVal}
          {...editableProps}
        />
      )}
    </LabelComponent>
  );
};
Label.displayName = 'Label';
