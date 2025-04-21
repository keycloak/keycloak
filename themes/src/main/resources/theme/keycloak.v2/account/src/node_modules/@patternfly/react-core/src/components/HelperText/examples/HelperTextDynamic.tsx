import React from 'react';
import { HelperText, HelperTextItem } from '@patternfly/react-core';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

export const HelperTextDynamic: React.FunctionComponent = () => (
  <React.Fragment>
    <HelperText>
      <HelperTextItem isDynamic>This is default helper text</HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem isDynamic variant="indeterminate">
        This is indeterminate helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem isDynamic variant="warning">
        This is warning helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem isDynamic variant="success">
        This is success helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem isDynamic variant="error">
        This is error helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem isDynamic variant="error" icon={<TimesIcon />}>
        This is error helper text with a custom icon
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem isDynamic variant="error" hasIcon={false}>
        This is error helper text with no icon
      </HelperTextItem>
    </HelperText>
  </React.Fragment>
);
