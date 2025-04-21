import React from 'react';
import {
  Form,
  FormGroup,
  FormFieldGroup,
  FormFieldGroupExpandable,
  FormFieldGroupHeader,
  TextInput,
  Button
} from '@patternfly/react-core';
import TrashIcon from '@patternfly/react-icons/dist/esm/icons/trash-icon';

export const FormFieldGroups: React.FunctionComponent = () => {
  const initialValues = {
    '0-label1': '',
    '0-label2': '',
    '1-expanded-group1-label1': '',
    '1-expanded-group1-label2': '',
    '1-expanded-group2-label1': '',
    '1-expanded-group2-label2': '',
    '1-expanded-group3-label1': '',
    '1-expanded-group3-label2': '',
    '1-group1-label1': '',
    '1-group1-label2': '',
    '2-label1': '',
    '2-label2': '',
    '3-label1': '',
    '3-label2': '',
    '3-nonexpand-group1-label1': '',
    '3-nonexpand-group1-label2': '',
    '3-nonexpand-group2-label1': '',
    '3-nonexpand-group2-label2': '',
    '4-nonexpand-label1': '',
    '4-nonexpand-label2': '',
    '0-label3': '',
    '0-label4': ''
  };

  const [inputValues, setInputValues] = React.useState(initialValues);

  const handleChange = (value: string, event: React.FormEvent<HTMLInputElement>) => {
    const { name } = event.currentTarget;
    setInputValues({ ...inputValues, [name]: value });
  };

  return (
    <Form>
      <FormGroup label="Label 1" isRequired fieldId="0-label1">
        <TextInput isRequired id="0-label1" name="0-label1" value={inputValues['0-label1']} onChange={handleChange} />
      </FormGroup>
      <FormGroup label="Label 2" isRequired fieldId="0-label2">
        <TextInput isRequired id="0-label2" name="0-label2" value={inputValues['0-label2']} onChange={handleChange} />
      </FormGroup>
      <FormFieldGroupExpandable
        isExpanded
        toggleAriaLabel="Details"
        header={
          <FormFieldGroupHeader
            titleText={{ text: 'Field group 1', id: 'field-group1-titleText-id' }}
            titleDescription="Field group 1 description text."
            actions={
              <>
                <Button variant="link">Delete all</Button> <Button variant="secondary">Add parameter</Button>
              </>
            }
          />
        }
      >
        <FormFieldGroupExpandable
          isExpanded
          toggleAriaLabel="Details"
          header={
            <FormFieldGroupHeader
              titleText={{ text: 'Nested field group 1', id: 'nested-field-group1-titleText-id' }}
              titleDescription="Nested field group 1 description text."
              actions={
                <Button variant="plain" aria-label="Remove">
                  <TrashIcon />
                </Button>
              }
            />
          }
        >
          <FormGroup label="Label 1" isRequired fieldId="1-expanded-group1-label1">
            <TextInput
              isRequired
              id="1-expanded-group1-label1"
              name="1-expanded-group1-label1"
              value={inputValues['1-expanded-group1-label1']}
              onChange={handleChange}
            />
          </FormGroup>
          <FormGroup label="Label 2" isRequired fieldId="1-expanded-group1-label2">
            <TextInput
              isRequired
              id="1-expanded-group1-label2"
              name="1-expanded-group1-label2"
              value={inputValues['1-expanded-group1-label2']}
              onChange={handleChange}
            />
          </FormGroup>
        </FormFieldGroupExpandable>
        <FormFieldGroupExpandable
          toggleAriaLabel="Details"
          header={
            <FormFieldGroupHeader
              titleText={{ text: 'Nested field group 2', id: 'nested-field-group2-titleText-id' }}
              actions={
                <Button variant="plain" aria-label="Remove">
                  <TrashIcon />
                </Button>
              }
            />
          }
        >
          <FormGroup label="Label 1" isRequired fieldId="1-expanded-group2-label1">
            <TextInput
              isRequired
              id="1-expanded-group2-label1"
              name="1-expanded-group2-label1"
              value={inputValues['1-expanded-group2-label1']}
              onChange={handleChange}
            />
          </FormGroup>
          <FormGroup label="Label 2" isRequired fieldId="1-expanded-group2-label2">
            <TextInput
              isRequired
              id="1-expanded-group2-label2"
              name="1-expanded-group2-label2"
              value={inputValues['1-expanded-group2-label2']}
              onChange={handleChange}
            />
          </FormGroup>
        </FormFieldGroupExpandable>
        <FormFieldGroupExpandable
          toggleAriaLabel="Details"
          header={
            <FormFieldGroupHeader
              titleText={{ text: 'Nested field group 3', id: 'nested-field-group3-titleText-id' }}
              titleDescription="Field group 3 description text."
              actions={
                <Button variant="plain" aria-label="Remove">
                  <TrashIcon />
                </Button>
              }
            />
          }
        >
          <FormGroup label="Label 1" isRequired fieldId="1-expanded-group3-label1">
            <TextInput
              isRequired
              id="1-expanded-group3-label1"
              name="1-expanded-group3-label1"
              value={inputValues['1-expanded-group3-label1']}
              onChange={handleChange}
            />
          </FormGroup>
          <FormGroup label="Label 2" isRequired fieldId="1-expanded-group3-label2">
            <TextInput
              isRequired
              id="1-expanded-group3-label2"
              name="1-expanded-group3-label2"
              value={inputValues['1-expanded-group3-label2']}
              onChange={handleChange}
            />
          </FormGroup>
        </FormFieldGroupExpandable>
        <FormGroup label="Label 1" isRequired fieldId="1-group1-label1">
          <TextInput
            isRequired
            id="1-group1-label1"
            name="1-group1-label1"
            value={inputValues['1-group1-label1']}
            onChange={handleChange}
          />
        </FormGroup>
        <FormGroup label="Label 2" isRequired fieldId="1-group1-label2">
          <TextInput
            isRequired
            id="1-group1-label2"
            name="1-group1-label2"
            value={inputValues['1-group1-label2']}
            onChange={handleChange}
          />
        </FormGroup>
      </FormFieldGroupExpandable>
      <FormFieldGroupExpandable
        toggleAriaLabel="Details"
        header={
          <FormFieldGroupHeader
            titleText={{ text: 'Field group 2', id: 'field-group2-titleText-id' }}
            titleDescription="Field group 2 description text."
            actions={
              <>
                <Button variant="link">Delete all</Button> <Button variant="secondary">Add parameter</Button>
              </>
            }
          />
        }
      >
        <FormGroup label="Label 1" isRequired fieldId="2-label1">
          <TextInput isRequired id="2-label1" name="2-label1" value={inputValues['2-label1']} onChange={handleChange} />
        </FormGroup>
        <FormGroup label="Label 2" isRequired fieldId="2-label2">
          <TextInput isRequired id="2-label2" name="2-label2" value={inputValues['2-label2']} onChange={handleChange} />
        </FormGroup>
      </FormFieldGroupExpandable>
      <FormFieldGroupExpandable
        isExpanded
        toggleAriaLabel="Details"
        header={
          <FormFieldGroupHeader
            titleText={{ text: 'Field group 3', id: 'field-group3-titleText-id' }}
            titleDescription="Field group 3 description text."
          />
        }
      >
        <FormGroup label="Label 1" isRequired fieldId="3-label1">
          <TextInput isRequired id="3-label1" name="3-label1" value={inputValues['3-label1']} onChange={handleChange} />
        </FormGroup>
        <FormGroup label="Label 2" isRequired fieldId="3-label2">
          <TextInput isRequired id="3-label2" name="3-label2" value={inputValues['3-label2']} onChange={handleChange} />
        </FormGroup>
        <FormFieldGroup
          header={
            <FormFieldGroupHeader
              titleText={{
                text: 'Nested field group 1 (non-expandable)',
                id: 'nested-field-group1-non-expandable-titleText-id'
              }}
            />
          }
        >
          <FormGroup label="Label 1" isRequired fieldId="3-nonexpand-group1-label1">
            <TextInput
              isRequired
              id="3-nonexpand-group1-label1"
              name="3-nonexpand-group1-label1"
              value={inputValues['3-nonexpand-group1-label1']}
              onChange={handleChange}
            />
          </FormGroup>
          <FormGroup label="Label 2" isRequired fieldId="3-nonexpand-group1-label2">
            <TextInput
              isRequired
              id="3-nonexpand-group1-label2"
              name="3-nonexpand-group1-label2"
              value={inputValues['3-nonexpand-group1-label2']}
              onChange={handleChange}
            />
          </FormGroup>
        </FormFieldGroup>
        <FormFieldGroup
          header={
            <FormFieldGroupHeader
              titleText={{
                text: 'Nested field group 2 (non-expandable)',
                id: 'nested-field-group2-non-expandable-titleText-id'
              }}
              titleDescription="Field group 2 description text."
            />
          }
        >
          <FormGroup label="Label 1" isRequired fieldId="3-nonexpand-group2-label1">
            <TextInput
              isRequired
              id="3-nonexpand-group2-label1"
              name="3-nonexpand-group2-label1"
              value={inputValues['3-nonexpand-group2-label1']}
              onChange={handleChange}
            />
          </FormGroup>
          <FormGroup label="Label 2" isRequired fieldId="3-nonexpand-group2-label2">
            <TextInput
              isRequired
              id="3-nonexpand-group2-label2"
              name="3-nonexpand-group2-label2"
              value={inputValues['3-nonexpand-group2-label2']}
              onChange={handleChange}
            />
          </FormGroup>
        </FormFieldGroup>
      </FormFieldGroupExpandable>
      <FormFieldGroup
        header={
          <FormFieldGroupHeader
            titleText={{ text: 'Field group 4 (non-expandable)', id: 'field-group4-non-expandable-titleText-id' }}
            titleDescription="Field group 4 description text."
            actions={
              <>
                <Button variant="link">Delete all</Button> <Button variant="secondary">Add parameter</Button>
              </>
            }
          />
        }
      >
        <FormGroup label="Label 1" isRequired fieldId="4-nonexpand-label1">
          <TextInput
            isRequired
            id="4-nonexpand-label1"
            name="4-nonexpand-label1"
            value={inputValues['4-nonexpand-label1']}
            onChange={handleChange}
          />
        </FormGroup>
        <FormGroup label="Label 2" isRequired fieldId="4-nonexpand-label2">
          <TextInput
            isRequired
            id="4-nonexpand-label2"
            name="4-nonexpand-label2"
            value={inputValues['4-nonexpand-label2']}
            onChange={handleChange}
          />
        </FormGroup>
      </FormFieldGroup>
      <FormGroup label="Label 3" isRequired fieldId="0-label3">
        <TextInput isRequired id="0-label3" name="0-label3" value={inputValues['0-label3']} onChange={handleChange} />
      </FormGroup>
      <FormGroup label="Label 4" isRequired fieldId="0-label4">
        <TextInput isRequired id="0-label4" name="0-label4" value={inputValues['0-label4']} onChange={handleChange} />
      </FormGroup>
    </Form>
  );
};
