import React from 'react';
import { HelperText, HelperTextItem } from '@patternfly/react-core';

export const HelperTextStatic: React.FunctionComponent = () => (
  <React.Fragment>
    <HelperText>
      <HelperTextItem>This is default helper text</HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="indeterminate">This is indeterminate helper text</HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="warning">This is warning helper text</HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="success">This is success helper text</HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="error">This is error helper text</HelperTextItem>
    </HelperText>
  </React.Fragment>
);
