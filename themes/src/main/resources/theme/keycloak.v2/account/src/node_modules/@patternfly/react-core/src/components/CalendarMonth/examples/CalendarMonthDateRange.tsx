import React from 'react';
import { CalendarMonth } from '@patternfly/react-core';

export const CalendarMonthDateRange: React.FunctionComponent = () => {
  const startDate = new Date(2020, 10, 11);
  const endDate = new Date(2020, 10, 24);
  const disablePreStartDates = (date: Date) => date >= startDate;

  return <CalendarMonth validators={[disablePreStartDates]} date={endDate} rangeStart={startDate} />;
};
