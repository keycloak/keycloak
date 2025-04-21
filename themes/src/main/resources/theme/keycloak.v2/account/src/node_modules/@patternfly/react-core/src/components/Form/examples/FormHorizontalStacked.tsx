import React from 'react';
import { Form, FormGroup, Checkbox } from '@patternfly/react-core';

export const FormHorizontalStacked: React.FunctionComponent = () => (
  <Form isHorizontal>
    <FormGroup role="group" label="Label" hasNoPaddingTop fieldId="horizontal-form-stacked-options" isStack>
      <Checkbox label="option 1" id="option-01" />
      <Checkbox label="option 2" id="option-02" />
    </FormGroup>
  </Form>
);
