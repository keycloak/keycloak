import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Title } from '../Title';
import TimesIcon from '@patternfly/react-icons/dist/js/icons/times-icon';

export interface WizardHeaderProps {
  /** Callback function called when the X (Close) button is clicked */
  onClose?: () => void;
  /** Title of the wizard */
  title: string;
  /** Description of the wizard */
  description?: string;
  /** aria-label applied to the X (Close) button */
  ariaLabelCloseButton?: string;
  /** id for the title */
  titleId?: string;
  /** id for the description */
  descriptionId?: string;
}

export const WizardHeader: React.FunctionComponent<WizardHeaderProps> = ({
  onClose = () => undefined,
  title,
  description,
  ariaLabelCloseButton,
  titleId,
  descriptionId
}: WizardHeaderProps) => (
  <div className={css(styles.wizardHeader)}>
    <Button variant="plain" className={css(styles.wizardClose)} aria-label={ariaLabelCloseButton} onClick={onClose}>
      <TimesIcon aria-hidden="true" />
    </Button>
    <Title size="3xl" className={css(styles.wizardTitle)} aria-label={title} id={titleId}>
      {title || <>&nbsp;</>}
    </Title>
    {description && (
      <p className={css(styles.wizardDescription)} id={descriptionId}>
        {description}
      </p>
    )}
  </div>
);
