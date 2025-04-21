import React from 'react';
import { Form, FormGroup, FormSection, TextInput } from '@patternfly/react-core';

export const FormSections: React.FunctionComponent = () => {
  const [input1, setInput1] = React.useState('');
  const [input2, setInput2] = React.useState('');

  const handleInputChange1 = (input1: string, _event: React.FormEvent<HTMLInputElement>) => {
    setInput1(input1);
  };

  const handleInputChange2 = (input2: string, _event: React.FormEvent<HTMLInputElement>) => {
    setInput2(input2);
  };

  return (
    <Form>
      <FormSection>
        <FormGroup label="Form section 1 input" isRequired fieldId="simple-form-section-1-input">
          <TextInput
            isRequired
            type="text"
            id="simple-form-section-1-input"
            name="simple-form-section-1-input"
            value={input1}
            onChange={handleInputChange1}
          />
        </FormGroup>
      </FormSection>
      <FormSection title="Form section 2 (optional title)" titleElement="h2">
        <FormGroup label="Form section 2 input" isRequired fieldId="simple-form-section-2-input">
          <TextInput
            isRequired
            type="text"
            id="simple-form-section-2-input"
            name="simple-form-section-2-input"
            value={input2}
            onChange={handleInputChange2}
          />
        </FormGroup>
      </FormSection>
    </Form>
  );
};
