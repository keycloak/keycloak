import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Accordion/accordion';

export interface AccordionExpandedContentBodyProps {
  /** Content rendered inside the accordion content body  */
  children?: React.ReactNode;
}

export const AccordionExpandedContentBody: React.FunctionComponent<AccordionExpandedContentBodyProps> = ({
  children = null
}: AccordionExpandedContentBodyProps) => <div className={css(styles.accordionExpandedContentBody)}>{children}</div>;
AccordionExpandedContentBody.displayName = 'AccordionExpandedContentBody';
