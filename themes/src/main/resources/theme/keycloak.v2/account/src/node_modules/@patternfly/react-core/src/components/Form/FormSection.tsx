import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';

export interface FormSectionProps extends Omit<React.HTMLProps<HTMLDivElement>, 'title'> {
  /** Content rendered inside the section */
  children?: React.ReactNode;
  /** Additional classes added to the section */
  className?: string;
  /** Title for the section */
  title?: React.ReactNode;
  /** Element to wrap the section title*/
  titleElement?: 'div' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

export const FormSection: React.FunctionComponent<FormSectionProps> = ({
  className = '',
  children,
  title = '',
  titleElement: TitleElement = 'div',
  ...props
}: FormSectionProps) => (
  <GenerateId prefix="pf-form-section-title">
    {sectionId => (
      <section
        className={css(styles.formSection, className)}
        role="group"
        {...(title && { 'aria-labelledby': sectionId })}
        {...props}
      >
        {title && (
          <TitleElement id={sectionId} className={css(styles.formSectionTitle, className)}>
            {title}
          </TitleElement>
        )}
        {children}
      </section>
    )}
  </GenerateId>
);
FormSection.displayName = 'FormSection';
