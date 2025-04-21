import React from 'react';
import { Button, DatePicker } from '@patternfly/react-core';

export const DatePickerControlledCalendar: React.FunctionComponent = () => {
  const dateRef = React.useRef(null);
  const onClick = () => {
    if (dateRef.current) {
      dateRef.current.toggleCalendar();
    }
  };
  return (
    <React.Fragment>
      <Button onClick={onClick}>Toggle calendar</Button>
      <DatePicker ref={dateRef} />
    </React.Fragment>
  );
};
