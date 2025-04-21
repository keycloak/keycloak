import React from 'react';
import { Form, FormGroup, TextInput, Popover } from '@patternfly/react-core';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';

export const FormGroupLabelInfo: React.FunctionComponent = () => {
  const [name, setName] = React.useState('');

  const handleNameChange = (name: string, _event: React.FormEvent<HTMLInputElement>) => {
    setName(name);
  };

  return (
    <Form>
      <FormGroup
        label="Full name"
        labelInfo="Additional label info"
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
              aria-describedby="form-group-label-info"
              className="pf-c-form__group-label-help"
            >
              <HelpIcon noVerticalAlign />
            </button>
          </Popover>
        }
        isRequired
        fieldId="form-group-label-info"
        helperText="Include your middle name if you have one."
      >
        <TextInput
          isRequired
          type="text"
          id="form-group-label-info"
          name="form-group-label-info"
          aria-describedby="form-group-label-info-helper"
          value={name}
          onChange={handleNameChange}
        />
      </FormGroup>
    </Form>
  );
};
