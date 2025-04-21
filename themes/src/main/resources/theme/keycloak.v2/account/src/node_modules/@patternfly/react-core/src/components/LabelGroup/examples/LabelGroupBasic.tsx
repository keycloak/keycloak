import React from 'react';
import { Label, LabelGroup } from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';

export const LabelGroupBasic: React.FunctionComponent = () => (
  <LabelGroup>
    <Label icon={<InfoCircleIcon />}>Label 1</Label>
    <Label icon={<InfoCircleIcon />} color="blue">
      Label 2
    </Label>
    <Label icon={<InfoCircleIcon />} color="green">
      Label 3
    </Label>
  </LabelGroup>
);
