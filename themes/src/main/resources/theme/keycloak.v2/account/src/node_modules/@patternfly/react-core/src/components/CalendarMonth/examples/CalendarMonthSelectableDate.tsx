import React from 'react';
import { CalendarMonth } from '@patternfly/react-core';

export const CalendarMonthSelectableDate: React.FunctionComponent = () => {
  const [date, setDate] = React.useState(new Date(2020, 10, 24));

  return (
    <React.Fragment>
      <pre>Selected date: {date.toString()}</pre>
      <CalendarMonth date={date} onChange={setDate} />
    </React.Fragment>
  );
};
