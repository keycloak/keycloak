import React from 'react';
import { HelperText, HelperTextItem } from '@patternfly/react-core';
import InfoIcon from '@patternfly/react-icons/dist/esm/icons/info-icon';
import QuestionIcon from '@patternfly/react-icons/dist/esm/icons/question-icon';
import ExclamationIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-icon';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

export const HelperTextStaticWithCustomIcon: React.FunctionComponent = () => (
  <React.Fragment>
    <HelperText>
      <HelperTextItem icon={<InfoIcon />}>This is default helper text</HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="indeterminate" icon={<QuestionIcon />}>
        This is indeterminate helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="warning" icon={<ExclamationIcon />}>
        This is warning helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="success" icon={<CheckIcon />}>
        This is success helper text
      </HelperTextItem>
    </HelperText>
    <HelperText>
      <HelperTextItem variant="error" icon={<TimesIcon />}>
        This is error helper text
      </HelperTextItem>
    </HelperText>
  </React.Fragment>
);
