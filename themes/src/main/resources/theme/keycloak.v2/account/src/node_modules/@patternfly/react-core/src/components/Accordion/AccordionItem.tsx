import * as React from 'react';

export interface AccordionItemProps {
  /** Content rendered inside the Accordion item  */
  children?: React.ReactNode;
}

export const AccordionItem: React.FunctionComponent<AccordionItemProps> = ({ children = null }: AccordionItemProps) => (
  <React.Fragment>{children}</React.Fragment>
);
AccordionItem.displayName = 'AccordionItem';
