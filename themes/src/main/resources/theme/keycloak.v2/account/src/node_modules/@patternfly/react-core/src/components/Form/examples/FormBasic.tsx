import React from 'react';
import { Form, FormGroup, TextInput, Checkbox, Popover, ActionGroup, Button, Radio } from '@patternfly/react-core';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';

export const FormBasic: React.FunctionComponent = () => {
  const [name, setName] = React.useState('');
  const [email, setEmail] = React.useState('');
  const [phone, setPhone] = React.useState('');

  const handleNameChange = (name: string) => {
    setName(name);
  };

  const handleEmailChange = (email: string) => {
    setEmail(email);
  };

  const handlePhoneChange = (phone: string) => {
    setPhone(phone);
  };

  return (
    <Form>
      <FormGroup
        label="Full name"
        labelIcon={
          <Popover
            headerContent={
              <div>
                The{' '}
                <a href="https://schema.org/name" target="_blank" rel="noreferrer">
                  name
                </a>{' '}
                of a{' '}
                <a href="https://schema.org/Person" target="_blank" rel="noreferrer">
                  Person
                </a>
              </div>
            }
            bodyContent={
              <div>
                Often composed of{' '}
                <a href="https://schema.org/givenName" target="_blank" rel="noreferrer">
                  givenName
                </a>{' '}
                and{' '}
                <a href="https://schema.org/familyName" target="_blank" rel="noreferrer">
                  familyName
                </a>
                .
              </div>
            }
          >
            <button
              type="button"
              aria-label="More info for name field"
              onClick={e => e.preventDefault()}
              aria-describedby="simple-form-name-01"
              className="pf-c-form__group-label-help"
            >
              <HelpIcon noVerticalAlign />
            </button>
          </Popover>
        }
        isRequired
        fieldId="simple-form-name-01"
        helperText="Include your middle name if you have one."
      >
        <TextInput
          isRequired
          type="text"
          id="simple-form-name-01"
          name="simple-form-name-01"
          aria-describedby="simple-form-name-01-helper"
          value={name}
          onChange={handleNameChange}
        />
      </FormGroup>
      <FormGroup label="Email" isRequired fieldId="simple-form-email-01">
        <TextInput
          isRequired
          type="email"
          id="simple-form-email-01"
          name="simple-form-email-01"
          value={email}
          onChange={handleEmailChange}
        />
      </FormGroup>
      <FormGroup label="Phone number" isRequired fieldId="simple-form-phone-01">
        <TextInput
          isRequired
          type="tel"
          id="simple-form-phone-01"
          name="simple-form-phone-01"
          placeholder="555-555-5555"
          value={phone}
          onChange={handlePhoneChange}
        />
      </FormGroup>
      <FormGroup role="group" isInline fieldId="basic-form-checkbox-group" label="How can we contact you?">
        <Checkbox label="Email" aria-label="Email" id="inlinecheck01" />
        <Checkbox label="Phone" aria-label="Phone" id="inlinecheck02" />
        <Checkbox label="Mail" aria-label="Mail" id="inlinecheck03" />
      </FormGroup>
      <FormGroup role="radiogroup" isInline fieldId="basic-form-radio-group" label="Time zone">
        <Radio name="basic-inline-radio" label="Eastern" id="basic-inline-radio-01" />
        <Radio name="basic-inline-radio" label="Central" id="basic-inline-radio-02" />
        <Radio name="basic-inline-radio" label="Pacific" id="basic-inline-radio-03" />
      </FormGroup>
      <FormGroup label="Additional note" fieldId="simple-form-note-01">
        <TextInput isDisabled type="text" id="simple-form-note-01" name="simple-form-number" value="disabled" />
      </FormGroup>
      <FormGroup fieldId="checkbox01">
        <Checkbox label="I'd like updates via email." id="checkbox01" name="checkbox01" aria-label="Update via email" />
      </FormGroup>
      <ActionGroup>
        <Button variant="primary">Submit</Button>
        <Button variant="link">Cancel</Button>
      </ActionGroup>
    </Form>
  );
};
