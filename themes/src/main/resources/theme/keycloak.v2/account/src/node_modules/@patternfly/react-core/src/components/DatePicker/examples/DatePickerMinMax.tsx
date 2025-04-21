import React from 'react';
import { DatePicker } from '@patternfly/react-core';

export const DatePickerMinMax: React.FunctionComponent = () => {
  const minDate = new Date(2020, 2, 16);
  const maxDate = new Date(2020, 2, 20);
  const rangeValidator = (date: Date) => {
    if (date < minDate) {
      return 'Date is before the allowable range.';
    } else if (date > maxDate) {
      return 'Date is after the allowable range.';
    }

    return '';
  };
  return <DatePicker value="2020-03-17" validators={[rangeValidator]} />;
};
