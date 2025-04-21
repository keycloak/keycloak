import * as React from 'react';
import Dropzone, { DropzoneProps, DropFileEventHandler } from 'react-dropzone';
import styles from '@patternfly/react-styles/css/components/MultipleFileUpload/multiple-file-upload';
import { css } from '@patternfly/react-styles';

export interface MultipleFileUploadProps extends Omit<React.HTMLProps<HTMLDivElement>, 'value'> {
  /** Content rendered inside the multi upload field */
  children?: React.ReactNode;
  /** Class to add to outer div */
  className?: string;
  /** Optional extra props to customize react-dropzone. */
  dropzoneProps?: DropzoneProps;
  /** Flag setting the component to horizontal styling mode */
  isHorizontal?: boolean;
  /** When files are dropped or uploaded this callback will be called with all accepted files */
  onFileDrop?: (data: File[]) => void;
}

export const MultipleFileUploadContext = React.createContext({
  open: () => {}
});

export const MultipleFileUpload: React.FunctionComponent<MultipleFileUploadProps> = ({
  className,
  children,
  dropzoneProps = {},
  isHorizontal,
  onFileDrop = () => {},
  ...props
}: MultipleFileUploadProps) => {
  const onDropAccepted: DropFileEventHandler = (acceptedFiles: File[], event) => {
    onFileDrop(acceptedFiles);
    // allow users to set a custom drop accepted handler rather than using on data change
    dropzoneProps.onDropAccepted && dropzoneProps.onDropAccepted(acceptedFiles, event);
  };

  const onDropRejected: DropFileEventHandler = (rejectedFiles, event) => {
    dropzoneProps.onDropRejected && dropzoneProps?.onDropRejected(rejectedFiles, event);
  };

  return (
    <Dropzone multiple={true} {...dropzoneProps} onDropAccepted={onDropAccepted} onDropRejected={onDropRejected}>
      {({ getRootProps, getInputProps, isDragActive, open }) => {
        const rootProps = getRootProps({
          ...props,
          onClick: event => event.preventDefault() // Prevents clicking TextArea from opening file dialog
        });
        const inputProps = getInputProps();

        return (
          <MultipleFileUploadContext.Provider value={{ open }}>
            <div
              className={css(
                styles.multipleFileUpload,
                isDragActive && styles.modifiers.dragOver,
                isHorizontal && styles.modifiers.horizontal,
                className
              )}
              {...rootProps}
              {...props}
            >
              <input
                /* hidden, necessary for react-dropzone */
                {...inputProps}
              />
              {children}
            </div>
          </MultipleFileUploadContext.Provider>
        );
      }}
    </Dropzone>
  );
};

MultipleFileUpload.displayName = 'MultipleFileUpload';
