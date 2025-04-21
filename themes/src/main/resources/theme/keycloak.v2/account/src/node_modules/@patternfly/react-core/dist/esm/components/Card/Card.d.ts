import * as React from 'react';
import { OUIAProps } from '../../helpers';
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
export declare const CardContext: React.Context<Partial<CardContextProps>>;
export declare const Card: React.FunctionComponent<CardProps>;
export {};
//# sourceMappingURL=Card.d.ts.map