import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
export interface MultipleFileUploadTitleIconProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside multiple file upload title icon */
  children?: React.ReactNode;
  /** Class to add to outer div */
  className?: string;
}

export const MultipleFileUploadTitleIcon: React.FunctionComponent<MultipleFileUploadTitleIconProps> = ({
  children,
  className,
  ...props
}: MultipleFileUploadTitleIconProps) => (
  <div className={css(styles.multipleFileUploadTitleIcon, className)} {...props}>
    {children}
  </div>
);

MultipleFileUploadTitleIcon.displayName = 'MultipleFileUploadTitleIcon';
