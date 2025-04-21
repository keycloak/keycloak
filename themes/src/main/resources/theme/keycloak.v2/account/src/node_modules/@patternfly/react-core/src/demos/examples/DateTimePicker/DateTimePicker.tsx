import React from 'react';
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  CalendarMonth,
  InputGroup,
  TextInput,
  Button,
  Popover
} from '@patternfly/react-core';
import OutlinedCalendarAltIcon from '@patternfly/react-icons/dist/esm/icons/outlined-calendar-alt-icon';
import OutlinedClockIcon from '@patternfly/react-icons/dist/esm/icons/outlined-clock-icon';

export const DateTimePicker: React.FunctionComponent = () => {
  const [isCalendarOpen, setIsCalendarOpen] = React.useState(false);
  const [isTimeOpen, setIsTimeOpen] = React.useState(false);
  const [valueDate, setValueDate] = React.useState('MM-DD-YYYY');
  const [valueTime, setValueTime] = React.useState('HH:MM');
  const times = Array.from(new Array(10), (_, i) => i + 8);
  const defaultTime = '0:00';
  const dateFormat = (date: Date) =>
    date.toLocaleDateString('en-US', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');

  const onToggleCalendar = () => {
    setIsCalendarOpen(!isCalendarOpen);
    setIsTimeOpen(false);
  };

  const onToggleTime = (_value: boolean, _event: any) => {
    setIsTimeOpen(!isTimeOpen);
    setIsCalendarOpen(false);
  };

  const onSelectCalendar = (newValueDate: Date) => {
    const newValue = dateFormat(newValueDate);
    setValueDate(newValue);
    setIsCalendarOpen(!isCalendarOpen);
    // setting default time when it is not picked
    if (valueTime === 'HH:MM') {
      setValueTime(defaultTime);
    }
  };

  const onSelectTime = (ev: React.SyntheticEvent<HTMLDivElement>) => {
    setValueTime(ev.currentTarget.textContent);
    setIsTimeOpen(!isTimeOpen);
  };

  const timeOptions = times.map(time => (
    <DropdownItem key={time} component="button" value={`${time}:00`}>
      {`${time}:00`}
    </DropdownItem>
  ));

  const calendar = <CalendarMonth date={new Date(valueDate)} onChange={onSelectCalendar} />;

  const time = (
    <Dropdown
      onSelect={onSelectTime}
      toggle={
        <DropdownToggle
          aria-label="Toggle the time picker menu"
          toggleIndicator={null}
          onToggle={onToggleTime}
          style={{ padding: '6px 16px' }}
        >
          <OutlinedClockIcon />
        </DropdownToggle>
      }
      isOpen={isTimeOpen}
      dropdownItems={timeOptions}
    />
  );

  const calendarButton = (
    <Button variant="control" aria-label="Toggle the calendar" onClick={onToggleCalendar}>
      <OutlinedCalendarAltIcon />
    </Button>
  );

  return (
    <div style={{ width: '300px' }}>
      <Popover
        position="bottom"
        bodyContent={calendar}
        showClose={false}
        isVisible={isCalendarOpen}
        hasNoPadding
        hasAutoWidth
      >
        <InputGroup>
          <TextInput
            type="text"
            id="date-time"
            aria-label="date and time picker demo"
            value={valueDate + ' ' + valueTime}
            isReadOnly
          />
          {calendarButton}
          {time}
        </InputGroup>
      </Popover>
    </div>
  );
};
