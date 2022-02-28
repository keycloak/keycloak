import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';

export interface ClipboardCopyToggleProps
  extends React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement> {
  onClick: (event: React.MouseEvent) => void;
  id: string;
  textId: string;
  contentId: string;
  isExpanded?: boolean;
  className?: string;
}

export const ClipboardCopyToggle: React.FunctionComponent<ClipboardCopyToggleProps> = ({
  onClick,
  className = '',
  id,
  textId,
  contentId,
  isExpanded = false,
  ...props
}: ClipboardCopyToggleProps) => (
  <button
    type="button"
    onClick={onClick}
    className={css(styles.clipboardCopyGroupToggle, className)}
    id={id}
    aria-labelledby={`${id} ${textId}`}
    aria-controls={`${id} ${contentId}`}
    aria-expanded={isExpanded}
    {...props}
  >
    <AngleRightIcon aria-hidden="true" className={css(styles.clipboardCopyGroupToggleIcon)} />
  </button>
);
