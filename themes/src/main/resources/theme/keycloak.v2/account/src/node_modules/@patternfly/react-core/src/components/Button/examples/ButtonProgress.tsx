import React from 'react';
import { Button } from '@patternfly/react-core';
import UploadIcon from '@patternfly/react-icons/dist/esm/icons/upload-icon';

interface LoadingPropsType {
  spinnerAriaValueText: string;
  spinnerAriaLabelledBy?: string;
  spinnerAriaLabel?: string;
  isLoading: boolean;
}

export const ButtonProgress: React.FunctionComponent = () => {
  const [isPrimaryLoading, setIsPrimaryLoading] = React.useState<boolean>(true);
  const [isSecondaryLoading, setIsSecondaryLoading] = React.useState<boolean>(true);
  const [isUploading, setIsUploading] = React.useState<boolean>(false);

  const primaryLoadingProps = {} as LoadingPropsType;
  if (isPrimaryLoading) {
    primaryLoadingProps.spinnerAriaValueText = 'Loading';
    primaryLoadingProps.spinnerAriaLabelledBy = 'primary-loading-button';
    primaryLoadingProps.isLoading = true;
  }
  const secondaryLoadingProps = {} as LoadingPropsType;
  if (isSecondaryLoading) {
    secondaryLoadingProps.spinnerAriaValueText = 'Loading';
    secondaryLoadingProps.spinnerAriaLabel = 'Content being loaded';
    secondaryLoadingProps.isLoading = true;
  }
  const uploadingProps = {} as LoadingPropsType;
  if (isUploading) {
    uploadingProps.spinnerAriaValueText = 'Loading';
    uploadingProps.isLoading = true;
    uploadingProps.spinnerAriaLabel = 'Uploading data';
  }

  return (
    <React.Fragment>
      <Button
        variant="primary"
        id="primary-loading-button"
        onClick={() => setIsPrimaryLoading(!isPrimaryLoading)}
        {...primaryLoadingProps}
      >
        {isPrimaryLoading ? 'Pause loading logs' : 'Resume loading logs'}
      </Button>{' '}
      <Button variant="secondary" onClick={() => setIsSecondaryLoading(!isSecondaryLoading)} {...secondaryLoadingProps}>
        {isSecondaryLoading ? 'Click to stop loading' : 'Click to start loading'}
      </Button>{' '}
      <Button
        variant="plain"
        {...(!isUploading && { 'aria-label': 'Upload' })}
        onClick={() => setIsUploading(!isUploading)}
        icon={<UploadIcon />}
        {...uploadingProps}
      />
      <br />
      <br />
    </React.Fragment>
  );
};
