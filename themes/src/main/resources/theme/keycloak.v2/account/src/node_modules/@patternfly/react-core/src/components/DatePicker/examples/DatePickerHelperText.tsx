import React from 'react';
import { DatePicker } from '@patternfly/react-core';

export const DatePickerHelperText: React.FunctionComponent = () => (
  <DatePicker value="2020-03-05" helperText="Select a date." />
);
