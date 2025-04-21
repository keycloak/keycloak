import React from 'react';
import { DatePicker } from '@patternfly/react-core';

export const DatePickerAmerican: React.FunctionComponent = () => {
  const dateFormat = (date: Date) =>
    date.toLocaleDateString('en-US', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
  const dateParse = (date: string) => {
    const split = date.split('-');
    if (split.length !== 3) {
      return new Date();
    }
    const month = split[0];
    const day = split[1];
    const year = split[2];
    return new Date(`${year.padStart(4, '0')}-${month.padStart(2, '0')}-${day.padStart(2, '0')}T00:00:00`);
  };

  return <DatePicker value="03-05-2020" placeholder="MM-DD-YYYY" dateFormat={dateFormat} dateParse={dateParse} />;
};
