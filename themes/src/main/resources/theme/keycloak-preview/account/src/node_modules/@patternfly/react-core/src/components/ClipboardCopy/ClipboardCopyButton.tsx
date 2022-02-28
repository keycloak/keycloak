import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy';
import { css } from '@patternfly/react-styles';
import CopyIcon from '@patternfly/react-icons/dist/js/icons/copy-icon';
import { Tooltip } from '../Tooltip';

export interface ClipboardCopyButtonProps
  extends React.DetailedHTMLProps<React.ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement> {
  onClick: (event: React.MouseEvent) => void;
  children: React.ReactNode;
  id: string;
  textId: string;
  className?: string;
  exitDelay?: number;
  entryDelay?: number;
  maxWidth?: string;
  position?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
  'aria-label'?: string;
}

export const ClipboardCopyButton: React.FunctionComponent<ClipboardCopyButtonProps> = ({
  onClick,
  className = '',
  exitDelay = 100,
  entryDelay = 100,
  maxWidth = '100px',
  position = 'top',
  'aria-label': ariaLabel = 'Copyable input',
  id,
  textId,
  children,
  ...props
}: ClipboardCopyButtonProps) => (
  <Tooltip
    trigger="mouseenter focus click"
    exitDelay={exitDelay}
    entryDelay={entryDelay}
    maxWidth={maxWidth}
    position={position}
    content={<div>{children}</div>}
  >
    <button
      type="button"
      onClick={onClick}
      className={css(styles.clipboardCopyGroupCopy, className)}
      aria-label={ariaLabel}
      id={id}
      aria-labelledby={`${id} ${textId}`}
      {...props}
    >
      <CopyIcon />
    </button>
  </Tooltip>
);
