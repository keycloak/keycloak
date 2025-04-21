import React from 'react';
import { Form, FormGroup, TextInput, Grid, GridItem } from '@patternfly/react-core';

export const FormGrid: React.FunctionComponent = () => {
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
      <Grid hasGutter md={6}>
        <GridItem span={12}>
          <FormGroup
            label="Full name"
            isRequired
            fieldId="grid-form-name-01"
            helperText="Include your middle name if you have one."
          >
            <TextInput
              isRequired
              type="text"
              id="grid-form-name-01"
              name="grid-form-name-01"
              aria-describedby="grid-form-name-01-helper"
              value={name}
              onChange={handleNameChange}
            />
          </FormGroup>
        </GridItem>
        <FormGroup label="Email" isRequired fieldId="grid-form-email-01">
          <TextInput
            isRequired
            type="email"
            id="grid-form-email-01"
            name="grid-form-email-01"
            value={email}
            onChange={handleEmailChange}
          />
        </FormGroup>
        <FormGroup label="Phone number" isRequired fieldId="grid-form-number-01">
          <TextInput
            isRequired
            type="tel"
            id="grid-form-number-01"
            placeholder="555-555-5555"
            name="grid-form-number-01"
            value={phone}
            onChange={handlePhoneChange}
          />
        </FormGroup>
      </Grid>
    </Form>
  );
};
