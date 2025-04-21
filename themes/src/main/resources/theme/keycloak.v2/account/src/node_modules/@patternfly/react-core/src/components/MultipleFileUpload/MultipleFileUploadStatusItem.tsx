import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';
import { Progress } from '../Progress';
import { Button } from '../Button';
import FileIcon from '@patternfly/react-icons/dist/esm/icons/file-icon';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';

export interface MultipleFileUploadStatusItemProps extends React.HTMLProps<HTMLLIElement> {
  /** Class to add to outer div */
  className?: string;
  /** Adds accessibility text to the status item deletion button */
  buttonAriaLabel?: string;
  /** The file object being represented by the status item */
  file?: File;
  /** A callback for when a selected file starts loading */
  onReadStarted?: (fileHandle: File) => void;
  /** A callback for when a selected file finishes loading */
  onReadFinished?: (fileHandle: File) => void;
  /** A callback for when the FileReader successfully reads the file */
  onReadSuccess?: (data: string, file: File) => void;
  /** A callback for when the FileReader API fails */
  onReadFail?: (error: DOMException, onReadFail: File) => void;
  /** Clear button was clicked */
  onClearClick?: React.MouseEventHandler<HTMLButtonElement>;

  // Props to bypass built in behavior

  /** A callback to process file reading in a custom way */
  customFileHandler?: (file: File) => void;
  /** A custom icon to show in place of the generic file icon */
  fileIcon?: React.ReactNode;
  /** A custom name to display for the file rather than using built in functionality to auto-fill it */
  fileName?: string;
  /** A custom file size to display for the file rather than using built in functionality to auto-fill it */
  fileSize?: number;
  /** A custom value to display for the progress component rather than using built in functionality to auto-fill it */
  progressValue?: number;
  /** A custom variant to apply to the progress component rather than using built in functionality to auto-fill it */
  progressVariant?: 'danger' | 'success' | 'warning';

  // Props passed through to the progress component

  /** Adds accessible text to the progress bar. Required when title not used and there is not any label associated with the progress bar */
  progressAriaLabel?: string;
  /** Associates the progress bar with it's label for accessibility purposes. Required when title not used */
  progressAriaLabelledBy?: string;
  /** Unique identifier for progress. Generated if not specified. */
  progressId?: string;
}

export const MultipleFileUploadStatusItem: React.FunctionComponent<MultipleFileUploadStatusItemProps> = ({
  className,
  file,
  fileIcon,
  onReadStarted = () => {},
  onReadFinished = () => {},
  onReadSuccess = () => {},
  onReadFail = () => {},
  onClearClick = () => {},
  customFileHandler,
  fileName,
  fileSize,
  progressValue,
  progressVariant,
  progressAriaLabel,
  progressAriaLabelledBy,
  progressId,
  buttonAriaLabel = 'Remove from list',
  ...props
}: MultipleFileUploadStatusItemProps) => {
  const [loadPercentage, setLoadPercentage] = React.useState(0);
  const [loadResult, setLoadResult] = React.useState<undefined | 'danger' | 'success'>();

  function readFile(file: File) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = () => reject(reader.error);
      reader.onprogress = data => {
        if (data.lengthComputable) {
          setLoadPercentage((data.loaded / data.total) * 100);
        }
      };
      reader.readAsDataURL(file);
    });
  }

  React.useEffect(() => {
    if (customFileHandler) {
      customFileHandler(file);
    } else {
      onReadStarted(file);
      readFile(file)
        .then(data => {
          setLoadResult('success');
          setLoadPercentage(100);
          onReadFinished(file);
          onReadSuccess(data as string, file);
        })
        .catch((error: DOMException) => {
          onReadFinished(file);
          onReadFail(error, file);
          setLoadResult('danger');
        });
    }
  }, []);

  const getHumanReadableFileSize = (size: number) => {
    const prefixes = ['', 'K', 'M', 'G', 'T'];
    let prefixUnit = 0;
    while (size >= 1000) {
      prefixUnit += 1;
      size = size / 1000;
    }

    if (prefixUnit >= prefixes.length) {
      return 'File size too large';
    }

    return `${Math.round(size)}${prefixes[prefixUnit]}B`;
  };

  const title = (
    <span className={styles.multipleFileUploadStatusItemProgress}>
      <span className={styles.multipleFileUploadStatusItemProgressText}>{fileName || file?.name || ''}</span>
      <span className={styles.multipleFileUploadStatusItemProgressSize}>
        {fileSize || getHumanReadableFileSize(file?.size || 0)}
      </span>
    </span>
  );

  return (
    <li className={css(styles.multipleFileUploadStatusItem, className)} {...props}>
      <div className={styles.multipleFileUploadStatusItemIcon}>{fileIcon || <FileIcon />}</div>
      <div className={styles.multipleFileUploadStatusItemMain}>
        <Progress
          title={title}
          value={progressValue || loadPercentage}
          variant={progressVariant || loadResult}
          aria-label={progressAriaLabel}
          aria-labelledby={progressAriaLabelledBy}
          id={progressId}
        />
      </div>
      <div className={styles.multipleFileUploadStatusItemClose}>
        <Button variant="plain" aria-label={buttonAriaLabel} onClick={onClearClick}>
          <TimesCircleIcon />
        </Button>
      </div>
    </li>
  );
};

MultipleFileUploadStatusItem.displayName = 'MultipleFileUploadStatusItem';
