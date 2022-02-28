import React from 'react';
import { shallow } from 'enzyme';
import { FormSelect } from '../FormSelect';
import { FormSelectOption } from '../FormSelectOption';
import { FormSelectOptionGroup } from '../FormSelectOptionGroup';
import { ValidatedOptions } from '../../../helpers/constants';

const props = {
  options: [
    { value: 'please choose', label: 'Please Choose', disabled: true },
    { value: 'mr', label: 'Mr', disabled: false },
    { value: 'miss', label: 'Miss', disabled: false },
    { value: 'mrs', label: 'Mrs', disabled: false },
    { value: 'ms', label: 'Ms', disabled: false },
    { value: 'dr', label: 'Dr', disabled: false },
    { value: 'other', label: 'Other', disabled: true }
  ],
  value: 'mrs'
};

const groupedProps = {
  groups: [
    {
      groupLabel: 'Group1',
      disabled: false,
      options: [
        { value: '1', label: 'The First Option', disabled: false },
        { value: '2', label: 'Second option is selected by default', disabled: false }
      ]
    },
    {
      groupLabel: 'Group2',
      disabled: false,
      options: [
        { value: '3', label: 'The Third Option', disabled: false },
        { value: '4', label: 'The Fourth option', disabled: false }
      ]
    },
    {
      groupLabel: 'Group3',
      disabled: true,
      options: [
        { value: '5', label: 'The Fifth Option', disabled: false },
        { value: '6', label: 'The Sixth option', disabled: false }
      ]
    }
  ],
  value: '2'
};

test('Simple FormSelect input', () => {
  const view = shallow(
    <FormSelect value={props.value} aria-label="simple FormSelect">
      {props.options.map((option, index) => (
        <FormSelectOption isDisabled={option.disabled} key={index} value={option.value} label={option.label} />
      ))}
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
});

test('Grouped FormSelect input', () => {
  const view = shallow(
    <FormSelect value={groupedProps.value} aria-label=" grouped FormSelect">
      {groupedProps.groups.map((group, index) => (
        <FormSelectOptionGroup isDisabled={group.disabled} key={index} label={group.groupLabel}>
          {group.options.map((option, i) => (
            <FormSelectOption isDisabled={option.disabled} key={i} value={option.value} label={option.label} />
          ))}
        </FormSelectOptionGroup>
      ))}
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
});

test('Disabled FormSelect input ', () => {
  const view = shallow(
    <FormSelect isDisabled aria-label="disabled  FormSelect">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
});

test('FormSelect input with aria-label does not generate console error', () => {
  const myMock = jest.fn() as any;
  global.console = { error: myMock } as any;
  const view = shallow(
    <FormSelect aria-label="FormSelect with aria-label">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
  expect(myMock).not.toBeCalled();
});

test('FormSelect input with id does not generate console error', () => {
  const myMock = jest.fn() as any;
  global.console = { error: myMock } as any;
  const view = shallow(
    <FormSelect id="id">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
  expect(myMock).not.toBeCalled();
});

test('FormSelect input with no aria-label or id generates console error', () => {
  const myMock = jest.fn() as any;
  global.console = { error: myMock } as any;
  const view = shallow(
    <FormSelect>
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
  expect(myMock).toBeCalled();
});

test('invalid FormSelect input', () => {
  const view = shallow(
    <FormSelect isValid={false} aria-label="invalid FormSelect">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
});

test('validated success FormSelect input', () => {
  const view = shallow(
    <FormSelect validated={ValidatedOptions.success} aria-label="validated FormSelect">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view.find('.pf-c-form-control.pf-m-success').length).toBe(1);
  expect(view).toMatchSnapshot();
});

test('validated error FormSelect input', () => {
  const view = shallow(
    <FormSelect validated={ValidatedOptions.error} aria-label="validated FormSelect">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
});

test('required FormSelect input', () => {
  const view = shallow(
    <FormSelect required aria-label="required FormSelect">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  expect(view).toMatchSnapshot();
});

test('FormSelect passes value and event to onChange handler', () => {
  const myMock = jest.fn();
  const newValue = 1;
  const event = {
    currentTarget: { value: newValue }
  };
  const view = shallow(
    <FormSelect onChange={myMock} aria-label="onchange FormSelect">
      <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
    </FormSelect>
  );
  view.find('select').simulate('change', event);
  expect(myMock).toBeCalledWith(newValue, event);
});
