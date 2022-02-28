import * as React from 'react';

import { Title, TitleLevel } from '../Title';

export interface ModalBoxHeaderProps {
  /** Content rendered inside the Header */
  children?: React.ReactNode;
  /** Additional classes added to the button */
  className?: string;
  /** Flag to hide the title */
  hideTitle?: boolean;
  /** The heading level to use */
  headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

export const ModalBoxHeader: React.FunctionComponent<ModalBoxHeaderProps> = ({
  children = null,
  className = '',
  hideTitle = false,
  headingLevel = TitleLevel.h1,
  ...props
}: ModalBoxHeaderProps) =>
  hideTitle ? null : (
    <React.Fragment>
      <Title size="2xl" headingLevel={headingLevel} className={className} {...props}>
        {children}
      </Title>
    </React.Fragment>
  );
