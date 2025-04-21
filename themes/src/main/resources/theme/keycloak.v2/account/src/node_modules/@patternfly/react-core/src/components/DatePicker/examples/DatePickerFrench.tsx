import React from 'react';
import { DatePicker, Weekday } from '@patternfly/react-core';

export const DatePickerFrench: React.FunctionComponent = () => {
  const minDate = new Date(2020, 2, 16);
  const maxDate = new Date(2020, 2, 20);
  const rangeValidator = (date: Date) => {
    if (date < minDate) {
      return 'Cette date est antérieure à la première date valide.';
    } else if (date > maxDate) {
      return 'Cette date est postérieure à la dernière date valide.';
    }

    return '';
  };
  return (
    <DatePicker
      value="2020-03-17"
      validators={[rangeValidator]}
      placeholder="aaaa-mm-jj"
      invalidFormatText="Cette date est invalide."
      locale="fr"
      weekStart={Weekday.Monday}
    />
  );
};
