import * as React from 'react';
export interface JumpLinksItemProps extends Omit<React.HTMLProps<HTMLLIElement>, 'onClick'> {
    /** Whether this item is active. Parent JumpLinks component sets this when passed a `scrollableSelector`. */
    isActive?: boolean;
    /** Href for this link */
    href?: string;
    /** Selector or HTMLElement to spy on */
    node?: string | HTMLElement;
    /** Text to be rendered inside span */
    children?: React.ReactNode;
    /** Click handler for anchor tag. Parent JumpLinks components tap into this. */
    onClick?: (ev: React.MouseEvent<HTMLAnchorElement>) => void;
    /** Class to add to li */
    className?: string;
}
export declare const JumpLinksItem: React.FunctionComponent<JumpLinksItemProps>;
//# sourceMappingURL=JumpLinksItem.d.ts.map