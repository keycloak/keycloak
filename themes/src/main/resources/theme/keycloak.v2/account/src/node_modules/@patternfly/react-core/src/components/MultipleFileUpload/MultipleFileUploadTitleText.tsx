import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';

export interface MultipleFileUploadTitleTextProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside multiple file upload title text */
  children?: React.ReactNode;
  /** Class to add to outer div */
  className?: string;
}

export const MultipleFileUploadTitleText: React.FunctionComponent<MultipleFileUploadTitleTextProps> = ({
  className,
  children,
  ...props
}: MultipleFileUploadTitleTextProps) => (
  <div className={css(styles.multipleFileUploadTitleText, className)} {...props}>
    {children}
  </div>
);

MultipleFileUploadTitleText.displayName = 'MultipleFileUploadTitleText';
