import React from 'react';
import { HelperText, HelperTextItem } from '@patternfly/react-core';

export const HelperTextMultipleStatic: React.FunctionComponent = () => (
  <HelperText>
    <HelperTextItem>This is default helper text</HelperTextItem>
    <HelperTextItem>This is another default helper text in the same block</HelperTextItem>
    <HelperTextItem>And this is more default text in the same block</HelperTextItem>
  </HelperText>
);
