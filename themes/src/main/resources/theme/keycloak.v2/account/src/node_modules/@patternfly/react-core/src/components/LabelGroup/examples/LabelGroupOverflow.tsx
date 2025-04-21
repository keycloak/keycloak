import React from 'react';
import { Label, LabelGroup } from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';

export const LabelGroupOverflow: React.FunctionComponent = () => (
  <LabelGroup>
    <Label icon={<InfoCircleIcon />}>Label 1</Label>
    <Label icon={<InfoCircleIcon />} color="blue">
      Label 2
    </Label>
    <Label icon={<InfoCircleIcon />} color="green">
      Label 3
    </Label>
    <Label icon={<InfoCircleIcon />} color="orange">
      Label 4
    </Label>
    <Label icon={<InfoCircleIcon />} color="red">
      Label 5
    </Label>
    <Label icon={<InfoCircleIcon />} color="purple">
      Label 6
    </Label>
  </LabelGroup>
);
