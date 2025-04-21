import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

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

describe('FormSelect', () => {
  test('Simple FormSelect input', () => {
    const { asFragment } = render(
      <FormSelect value={props.value} aria-label="simple FormSelect">
        {props.options.map((option, index) => (
          <FormSelectOption isDisabled={option.disabled} key={index} value={option.value} label={option.label} />
        ))}
      </FormSelect>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Grouped FormSelect input', () => {
    const { asFragment } = render(
      <FormSelect value={groupedProps.value} aria-label="grouped FormSelect">
        {groupedProps.groups.map((group, index) => (
          <FormSelectOptionGroup isDisabled={group.disabled} key={index} label={group.groupLabel}>
            {group.options.map((option, i) => (
              <FormSelectOption isDisabled={option.disabled} key={i} value={option.value} label={option.label} />
            ))}
          </FormSelectOptionGroup>
        ))}
      </FormSelect>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Disabled FormSelect input ', () => {
    const { asFragment } = render(
      <FormSelect isDisabled aria-label="disabled FormSelect">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('FormSelect input with aria-label does not generate console error', () => {
    const myMock = jest.fn() as any;
    global.console = { error: myMock } as any;

    const { asFragment } = render(
      <FormSelect aria-label="label">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    expect(asFragment()).toMatchSnapshot();
    expect(myMock).not.toHaveBeenCalled();
  });

  test('FormSelect input with id does not generate console error', () => {
    const myMock = jest.fn() as any;
    global.console = { error: myMock } as any;

    const { asFragment } = render(
      <FormSelect id="id" aria-label="label">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    expect(asFragment()).toMatchSnapshot();
    expect(myMock).not.toHaveBeenCalled();
  });

  test('FormSelect input with no aria-label or id generates console error', () => {
    const myMock = jest.fn() as any;
    global.console = { error: myMock } as any;

    const { asFragment } = render(
      <FormSelect>
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    expect(asFragment()).toMatchSnapshot();
    expect(myMock).toHaveBeenCalled();
  });

  test('invalid FormSelect input', () => {
    const { asFragment } = render(
      <FormSelect validated="error" aria-label="invalid FormSelect">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('validated success FormSelect input', () => {
    const { asFragment } = render(
      <FormSelect validated={ValidatedOptions.success} aria-label="validated FormSelect">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    const formSelect = screen.getByLabelText('validated FormSelect');

    expect(formSelect).toHaveClass('pf-m-success');
    expect(asFragment()).toMatchSnapshot();
  });

  test('validated warning FormSelect input', () => {
    const { asFragment } = render(
      <FormSelect validated={ValidatedOptions.warning} aria-label="validated FormSelect">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    const formSelect = screen.getByLabelText('validated FormSelect');

    expect(formSelect).toHaveClass('pf-m-warning');
    expect(asFragment()).toMatchSnapshot();
  });

  test('required FormSelect input', () => {
    render(
      <FormSelect isRequired aria-label="required FormSelect">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    expect(screen.getByLabelText('required FormSelect')).toHaveAttribute('required');
  });

  test('FormSelect passes value and event to onChange handler', () => {
    const myMock = jest.fn();

    render(
      <FormSelect onChange={myMock} aria-label="Some label">
        <FormSelectOption key={1} value={props.options[1].value} label={props.options[1].label} />
      </FormSelect>
    );

    userEvent.selectOptions(screen.getByLabelText('Some label'), 'Mr');

    expect(myMock).toHaveBeenCalled();
    expect(myMock.mock.calls[0][0]).toEqual('mr');
  });
});
