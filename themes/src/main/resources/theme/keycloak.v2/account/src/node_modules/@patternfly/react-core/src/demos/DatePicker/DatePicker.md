---
id: Date picker
section: components
beta: true
---

## Demos

### Date range picker

This is intended to be used as a filter. After selecting a start date, the next date is automatically selected.

```js
import { Split, SplitItem, DatePicker, isValidDate, yyyyMMddFormat } from '@patternfly/react-core';

DateRangePicker = () => {
  const [from, setFrom] = React.useState();
  const [to, setTo] = React.useState();

  const toValidator = date => isValidDate(from) && date >= from ? '' : 'To date must be less than from date';
  const onFromChange = (_str, date) => {
    setFrom(new Date(date));
    if (isValidDate(date)) {
      date.setDate(date.getDate() + 1);
      setTo(yyyyMMddFormat(date));
    }
    else {
      setTo('');
    }
  };

  return (
    <Split>
      <SplitItem>
        <DatePicker
          onChange={onFromChange}
          aria-label="Start date"
          placeholder="YYYY-MM-DD"
        />
      </SplitItem>
      <SplitItem style={{ padding: '6px 12px 0 12px' }}>
        to
      </SplitItem>
      <SplitItem>
        <DatePicker
          value={to}
          onChange={date => setTo(date)}
          isDisabled={!isValidDate(from)}
          rangeStart={from}
          validators={[toValidator]}
          aria-label="End date"
          placeholder="YYYY-MM-DD"
        />
      </SplitItem>
    </Split>
  );
}
```

### Date and time range picker

```js
import { Flex, FlexItem, InputGroup, DatePicker, isValidDate, TimePicker, yyyyMMddFormat, updateDateTime } from '@patternfly/react-core';

DateTimeRangePicker = () => {
  const [from, setFrom] = React.useState();
  const [to, setTo] = React.useState();

  const toValidator = date => {
    return isValidDate(from) && yyyyMMddFormat(date) >= yyyyMMddFormat(from) ? '' : 'To date must after from date';
  };
  
  const onFromDateChange = (inputDate, newFromDate) => {
    if (isValidDate(from) && isValidDate(newFromDate) && inputDate === yyyyMMddFormat(newFromDate)) {
      newFromDate.setHours(from.getHours());
      newFromDate.setMinutes(from.getMinutes());
    }
    if (isValidDate(newFromDate) && inputDate === yyyyMMddFormat(newFromDate)) {
      setFrom(new Date(newFromDate));
    }
  };
  
  const onFromTimeChange = (time, hour, minute) => {
    if (isValidDate(from)) {
      const updatedFromDate = new Date(from);
      updatedFromDate.setHours(hour);
      updatedFromDate.setMinutes(minute);
      setFrom(updatedFromDate);
    }
  };

  const onToDateChange = (inputDate, newToDate) => {
    if (isValidDate(to) && isValidDate(newToDate) && inputDate === yyyyMMddFormat(newToDate)) {
      newToDate.setHours(to.getHours());
      newToDate.setMinutes(to.getMinutes());
    }
    if (isValidDate(newToDate) && inputDate === yyyyMMddFormat(newToDate)){
      setTo(newToDate);
    }
  };
  
  const onToTimeChange = (time, hour, minute) => {
    if (isValidDate(to)) {
      const updatedToDate = new Date(to);
      updatedToDate.setHours(hour);
      updatedToDate.setMinutes(minute);
      setTo(updatedToDate);
    }
  };

  return (
    <Flex direction={{default: 'column', lg: 'row'}}>
      <FlexItem>
        <InputGroup>
          <DatePicker
            onChange={onFromDateChange}
            aria-label="Start date"
            placeholder="YYYY-MM-DD"
          />
          <TimePicker 
            aria-label="Start time"
            style={{width: '150px'}} 
            onChange={onFromTimeChange} 
          />
        </InputGroup>
      </FlexItem>
      <FlexItem>
        to
      </FlexItem>
      <FlexItem>
        <InputGroup>
          <DatePicker
            value={isValidDate(to) ? yyyyMMddFormat(to) : to}
            onChange={onToDateChange}
            isDisabled={!isValidDate(from)}
            rangeStart={from}
            validators={[toValidator]}
            aria-label="End date"
            placeholder="YYYY-MM-DD"
          />
          <TimePicker style={{width: '150px'}} onChange={onToTimeChange} isDisabled={!isValidDate(from)}/>
        </InputGroup>
      </FlexItem>
    </Flex>
  );
}
```


### Date and time pickers in modal
Modals trap focus and watch a few document level events. In order to place a date picker in a modal:
- To avoid the modal's escape press event handler from overruling the date picker's escape press handlers, use the `DatePickerRef` to close the calendar when it is open and the escape key is pressed.
- Append the calendar to the modal to keep it as close to the date picker in the DOM while maintaining correct layouts visually
  In order to place a time picker in the modal, its menu must be appended to the time picker's parent.
```ts file="./examples/DateTimePickerInModal.tsx"
```
