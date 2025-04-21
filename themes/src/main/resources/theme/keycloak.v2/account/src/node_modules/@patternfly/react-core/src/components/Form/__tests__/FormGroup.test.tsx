import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ValidatedOptions } from '../../../helpers/constants';
import { FormGroup } from '../FormGroup';
import { Form } from '../Form';

describe('FormGroup', () => {
  const returnFunction = () => <div>label</div>;

  test('should render default form group variant', () => {
    const { asFragment } = render(
      <FormGroup label="label" fieldId="label-id" helperText="this is helper text">
        <input id="label-id" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render inline form group variant', () => {
    render(
      <FormGroup isInline label="label" fieldId="label-id" helperText="this is helper text">
        <input id="label-id" />
      </FormGroup>
    );
    expect(screen.getByLabelText('label').parentElement).toHaveClass('pf-m-inline');
  });

  test('should render no padding-top form group variant', () => {
    render(
      <FormGroup
        hasNoPaddingTop
        label="label"
        fieldId="label-id"
        helperText="this is helper text"
        data-testid="form-group-test-id"
      >
        <input id="label-id" />
      </FormGroup>
    );
    expect(screen.getByTestId('form-group-test-id').firstElementChild).toHaveClass('pf-m-no-padding-top');
  });

  test('should render form group variant with required label', () => {
    const { asFragment } = render(
      <FormGroup label="label" isRequired fieldId="label-id">
        <input id="label-id" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group variant with node label', () => {
    const { asFragment } = render(
      <FormGroup fieldId="id" label={<h1>Header</h1>}>
        <input aria-label="input" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group with additonal label info', () => {
    const { asFragment } = render(
      <FormGroup fieldId="id" label={<h1>Header</h1>} labelInfo="more info">
        <input aria-label="input" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group variant with function label', () => {
    render(
      <FormGroup fieldId="id" label={returnFunction()}>
        <input aria-label="input" />
      </FormGroup>
    );

    expect(screen.getByText('label')).toBeInTheDocument();
  });

  test('should render form group variant with node helperText', () => {
    render(
      <FormGroup label="Label" fieldId="label-id" helperText={<h1>Header</h1>}>
        <input id="label-id" />
      </FormGroup>
    );

    expect(screen.getByRole('heading', { name: 'Header' })).toBeInTheDocument();
  });

  test('should render form group variant with function helperText', () => {
    render(
      <FormGroup label="Label" fieldId="label-id" helperText={returnFunction()}>
        <input id="label-id" />
      </FormGroup>
    );

    expect(screen.getByText('label')).toBeInTheDocument();
  });

  test('should render horizontal form group variant', () => {
    const { asFragment } = render(
      <Form isHorizontal>
        <FormGroup label="label" fieldId="label-id" helperText="this is helperText">
          <input id="label-id" />
        </FormGroup>
      </Form>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render stacked horizontal form group variant', () => {
    const { asFragment } = render(
      <Form isHorizontal>
        <FormGroup label="label" fieldId="label-id" isStack helperText="this is helperText">
          <input id="label-id" />
        </FormGroup>
      </Form>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render helper text above input', () => {
    const { asFragment } = render(
      <Form isHorizontal>
        <FormGroup label="label" fieldId="label-id" helperText="this is helperText" isHelperTextBeforeField>
          <input id="label-id" />
        </FormGroup>
      </Form>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group variant without label', () => {
    const { asFragment } = render(
      <FormGroup fieldId="id">
        <input aria-label="input" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group invalid variant', () => {
    const { asFragment } = render(
      <FormGroup label="label" fieldId="label-id" validated={'error'} helperTextInvalid="Invalid FormGroup">
        <input id="id" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group validated success variant', () => {
    const { asFragment } = render(
      <FormGroup
        label="label"
        fieldId="label-id"
        validated={ValidatedOptions.success}
        helperText="Validated FormGroup"
        data-testid="form-group-test-id"
      >
        <input id="id" />
      </FormGroup>
    );

    expect(screen.getByTestId('form-group-test-id').querySelector('input + div')).toHaveClass('pf-m-success');
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group validated error variant', () => {
    const { asFragment } = render(
      <FormGroup label="label" fieldId="label-id" validated={ValidatedOptions.error} helperText="Validated FormGroup">
        <input id="id" />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render form group validated warning variant', () => {
    const { asFragment } = render(
      <FormGroup
        label="label"
        fieldId="label-id"
        validated={ValidatedOptions.warning}
        helperText="Validated FormGroup"
        data-testid="form-group-test-id"
      >
        <input id="id" />
      </FormGroup>
    );

    expect(screen.getByTestId('form-group-test-id').querySelector('input + div')).toHaveClass('pf-m-warning');
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render correctly when label is not a string with Children = Array', () => {
    const { asFragment } = render(
      <FormGroup fieldId="id" label={returnFunction()}>
        <input aria-label="label" />
        <input id="id" />
        <input />
      </FormGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render without group or radiogroup role when role is not passed in', () => {
    render(
      <FormGroup label="label" fieldId="no-group-role">
        <input />
      </FormGroup>
    );

    expect(screen.queryByRole('group')).not.toBeInTheDocument();
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
  });

  test('input should receive focus when clicking text label', () => {
    render(
      <FormGroup label="label" fieldId="native-label-element">
        <input id="native-label-element" />
      </FormGroup>
    );

    const labelElement = screen.getByText('label');
    const input = screen.getByRole('textbox');

    userEvent.click(labelElement);
    expect(input).toHaveFocus();
  });

  describe('With multiple inputs per group', () => {
    test('inputs should not receive focus when clicking text label', () => {
      render(
        <FormGroup label="label" fieldId="span-label-element" role="group">
          <input id="span-label-element" />
          <input />
        </FormGroup>
      );

      const labelElement = screen.getByText('label');
      const inputs = screen.getAllByRole('textbox');

      userEvent.click(labelElement);
      inputs.forEach(input => {
        expect(input).not.toHaveFocus();
      });
    });

    test('should render with group role', () => {
      render(
        <FormGroup label="label" fieldId="group-role" role="group">
          <input />
          <input />
        </FormGroup>
      );

      expect(screen.getByRole('group')).toBeInTheDocument();
    });

    test('should render with radiogroup role', () => {
      render(
        <FormGroup label="label" fieldId="radiogroup-role" role="radiogroup">
          <input />
          <input />
        </FormGroup>
      );

      expect(screen.getByRole('radiogroup')).toBeInTheDocument();
    });

    test('should have accessible name when group role is passed in', () => {
      render(
        <FormGroup label="label" fieldId="accessible-name" role="group">
          <input />
          <input />
        </FormGroup>
      );

      expect(screen.getByRole('group')).toHaveAccessibleName('label');
    });

    test('should have accessible name when radiogroup role is passed in', () => {
      render(
        <FormGroup label="label" fieldId="accessible-name" role="radiogroup">
          <input />
          <input />
        </FormGroup>
      );

      expect(screen.getByRole('radiogroup')).toHaveAccessibleName('label');
    });
  });
});
