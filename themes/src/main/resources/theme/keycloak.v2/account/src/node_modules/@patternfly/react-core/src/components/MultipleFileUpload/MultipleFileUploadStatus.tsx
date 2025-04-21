import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
import { ExpandableSection } from '../ExpandableSection';
import InProgressIcon from '@patternfly/react-icons/dist/esm/icons/in-progress-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';

export interface MultipleFileUploadStatusProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside multi file upload status list */
  children?: React.ReactNode;
  /** Class to add to outer div */
  className?: string;
  /** String to show in the status toggle */
  statusToggleText?: string;
  /** Icon to show in the status toggle */
  statusToggleIcon?: 'danger' | 'success' | 'inProgress' | React.ReactNode;
}

export const MultipleFileUploadStatus: React.FunctionComponent<MultipleFileUploadStatusProps> = ({
  children,
  className,
  statusToggleText,
  statusToggleIcon,
  ...props
}: MultipleFileUploadStatusProps) => {
  const [icon, setIcon] = React.useState<React.ReactNode>();
  const [isOpen, setIsOpen] = React.useState(true);

  React.useEffect(() => {
    switch (statusToggleIcon) {
      case 'danger':
        setIcon(<TimesCircleIcon />);
        break;
      case 'success':
        setIcon(<CheckCircleIcon />);
        break;
      case 'inProgress':
        setIcon(<InProgressIcon />);
        break;
      default:
        setIcon(statusToggleIcon);
    }
  }, [statusToggleIcon]);

  const toggle = (
    <div className={styles.multipleFileUploadStatusProgress}>
      <div className={styles.multipleFileUploadStatusProgressIcon}>{icon}</div>
      <div className={styles.multipleFileUploadStatusItemProgressText}>{statusToggleText}</div>
    </div>
  );

  const toggleExpandableSection = () => {
    setIsOpen(!isOpen);
  };

  return (
    <div className={css(styles.multipleFileUploadStatus, className)} {...props}>
      <ExpandableSection toggleContent={toggle} isExpanded={isOpen} onToggle={toggleExpandableSection}>
        <ul className="pf-c-multiple-file-upload__status-list">{children}</ul>
      </ExpandableSection>
    </div>
  );
};

MultipleFileUploadStatus.displayName = 'MultipleFileUploadStatus';
