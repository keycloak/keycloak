import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
import { MultipleFileUploadTitleIcon } from './MultipleFileUploadTitleIcon';
import { MultipleFileUploadTitleText } from './MultipleFileUploadTitleText';
import { MultipleFileUploadTitleTextSeparator } from './MultipleFileUploadTitleTextSeparator';
export interface MultipleFileUploadTitleProps extends React.HTMLProps<HTMLDivElement> {
  /** Class to add to outer div */
  className?: string;
  /** Content rendered inside the title icon div */
  icon?: React.ReactNode;
  /** Content rendered inside the title text div */
  text?: React.ReactNode;
  /** Content rendered inside the title text separator div */
  textSeparator?: React.ReactNode;
}

export const MultipleFileUploadTitle: React.FunctionComponent<MultipleFileUploadTitleProps> = ({
  className,
  icon,
  text = '',
  textSeparator = '',
  ...props
}: MultipleFileUploadTitleProps) => (
  <div className={css(styles.multipleFileUploadTitle, className)} {...props}>
    {icon && <MultipleFileUploadTitleIcon>{icon}</MultipleFileUploadTitleIcon>}
    {text && (
      <MultipleFileUploadTitleText>
        {`${text} `}
        {textSeparator && <MultipleFileUploadTitleTextSeparator>{textSeparator}</MultipleFileUploadTitleTextSeparator>}
      </MultipleFileUploadTitleText>
    )}
  </div>
);

MultipleFileUploadTitle.displayName = 'MultipleFileUploadTitle';
