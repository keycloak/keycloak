import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';
import { useOUIAProps, OUIAProps } from '../../helpers';

export interface CardProps extends React.HTMLProps<HTMLElement>, OUIAProps {
  /** Content rendered inside the Card */
  children?: React.ReactNode;
  /** ID of the Card. Also passed back in the CardHeader onExpand callback. */
  id?: string;
  /** Additional classes added to the Card */
  className?: string;
  /** Sets the base component to render. defaults to article */
  component?: keyof JSX.IntrinsicElements;
  /** @deprecated to make a card hoverable, use isSelectable or isSelectableRaised. */
  isHoverable?: boolean;
  /** Modifies the card to include compact styling. Should not be used with isLarge. */
  isCompact?: boolean;
  /** Modifies the card to include selectable styling */
  isSelectable?: boolean;
  /** @beta Specifies the card is selectable, and applies the new raised styling on hover and select */
  isSelectableRaised?: boolean;
  /** Modifies the card to include selected styling */
  isSelected?: boolean;
  /** @beta Modifies a raised selectable card to have disabled styling */
  isDisabledRaised?: boolean;
  /** Modifies the card to include flat styling */
  isFlat?: boolean;
  /** Modifies the card to include rounded styling */
  isRounded?: boolean;
  /** Modifies the card to be large. Should not be used with isCompact. */
  isLarge?: boolean;
  /** Cause component to consume the available height of its container */
  isFullHeight?: boolean;
  /** Modifies the card to include plain styling; this removes border and background */
  isPlain?: boolean;
  /** Flag indicating if a card is expanded. Modifies the card to be expandable. */
  isExpanded?: boolean;
  /** Flag indicating that the card should render a hidden input to make it selectable */
  hasSelectableInput?: boolean;
  /** Aria label to apply to the selectable input if one is rendered */
  selectableInputAriaLabel?: string;
  /** Callback that executes when the selectable input is changed */
  onSelectableInputChange?: (labelledBy: string, event: React.FormEvent<HTMLInputElement>) => void;
}

interface CardContextProps {
  cardId: string;
  registerTitleId: (id: string) => void;
  isExpanded: boolean;
}

interface AriaProps {
  'aria-label'?: string;
  'aria-labelledby'?: string;
}

export const CardContext = React.createContext<Partial<CardContextProps>>({
  cardId: '',
  registerTitleId: () => {},
  isExpanded: false
});

export const Card: React.FunctionComponent<CardProps> = ({
  children = null,
  id = '',
  className = '',
  component = 'article',
  isHoverable = false,
  isCompact = false,
  isSelectable = false,
  isSelectableRaised = false,
  isSelected = false,
  isDisabledRaised = false,
  isFlat = false,
  isExpanded = false,
  isRounded = false,
  isLarge = false,
  isFullHeight = false,
  isPlain = false,
  ouiaId,
  ouiaSafe = true,
  hasSelectableInput = false,
  selectableInputAriaLabel,
  onSelectableInputChange = () => {},
  ...props
}: CardProps) => {
  const Component = component as any;
  const ouiaProps = useOUIAProps(Card.displayName, ouiaId, ouiaSafe);
  const [titleId, setTitleId] = React.useState('');
  const [ariaProps, setAriaProps] = React.useState<AriaProps>();

  if (isCompact && isLarge) {
    // eslint-disable-next-line no-console
    console.warn('Card: Cannot use isCompact with isLarge. Defaulting to isCompact');
    isLarge = false;
  }

  const getSelectableModifiers = () => {
    if (isDisabledRaised) {
      return css(styles.modifiers.nonSelectableRaised);
    }
    if (isSelectableRaised) {
      return css(styles.modifiers.selectableRaised, isSelected && styles.modifiers.selectedRaised);
    }
    if (isSelectable || isHoverable) {
      return css(styles.modifiers.selectable, isSelected && styles.modifiers.selected);
    }
    return '';
  };

  const containsCardTitleChildRef = React.useRef(false);

  const registerTitleId = (id: string) => {
    setTitleId(id);
    containsCardTitleChildRef.current = !!id;
  };

  React.useEffect(() => {
    if (selectableInputAriaLabel) {
      setAriaProps({ 'aria-label': selectableInputAriaLabel });
    } else if (titleId) {
      setAriaProps({ 'aria-labelledby': titleId });
    } else if (hasSelectableInput && !containsCardTitleChildRef.current) {
      setAriaProps({});
      // eslint-disable-next-line no-console
      console.warn(
        'If no CardTitle component is passed as a child of Card the selectableInputAriaLabel prop must be passed'
      );
    }
  }, [hasSelectableInput, selectableInputAriaLabel, titleId]);

  return (
    <CardContext.Provider
      value={{
        cardId: id,
        registerTitleId,
        isExpanded
      }}
    >
      {hasSelectableInput && (
        <input
          className="pf-screen-reader"
          id={`${id}-input`}
          {...ariaProps}
          type="checkbox"
          checked={isSelected}
          onChange={event => onSelectableInputChange(id, event)}
          disabled={isDisabledRaised}
          tabIndex={-1}
        />
      )}
      <Component
        id={id}
        className={css(
          styles.card,
          isCompact && styles.modifiers.compact,
          isExpanded && styles.modifiers.expanded,
          isFlat && styles.modifiers.flat,
          isRounded && styles.modifiers.rounded,
          isLarge && styles.modifiers.displayLg,
          isFullHeight && styles.modifiers.fullHeight,
          isPlain && styles.modifiers.plain,
          getSelectableModifiers(),
          className
        )}
        tabIndex={isSelectable || isSelectableRaised ? '0' : undefined}
        {...props}
        {...ouiaProps}
      >
        {children}
      </Component>
    </CardContext.Provider>
  );
};
Card.displayName = 'Card';
