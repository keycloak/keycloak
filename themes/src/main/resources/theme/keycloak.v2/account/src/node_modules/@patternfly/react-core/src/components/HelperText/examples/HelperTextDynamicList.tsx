import React from 'react';
import { HelperText, HelperTextItem } from '@patternfly/react-core';

export const HelperTextDynamicList: React.FunctionComponent = () => (
  <HelperText component="ul">
    <HelperTextItem isDynamic variant="success" component="li">
      Must be at least 14 characters
    </HelperTextItem>
    <HelperTextItem isDynamic variant="error" component="li">
      Cannot contain any variation of the word "redhat"
    </HelperTextItem>
    <HelperTextItem isDynamic variant="success" component="li">
      Must include at least 3 of the following: lowercase letter, uppercase letters, numbers, symbols
    </HelperTextItem>
  </HelperText>
);
