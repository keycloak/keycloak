import React from 'react';
import { DatePicker } from '@patternfly/react-core';

export const DatePickerBasic: React.FunctionComponent = () => (
  <DatePicker
    // eslint-disable-next-line no-console
    onBlur={(str, date) => console.log('onBlur', str, date)}
    // eslint-disable-next-line no-console
    onChange={(str, date) => console.log('onChange', str, date)}
  />
);
