import React from 'react';
import { Modal, ModalVariant, Button, Form, FormGroup, Popover, TextInput } from '@patternfly/react-core';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';

export const ModalWithForm: React.FunctionComponent = () => {
  const [isModalOpen, setModalOpen] = React.useState(false);
  const [nameValue, setNameValue] = React.useState('');
  const [emailValue, setEmailValue] = React.useState('');
  const [addressValue, setAddressValue] = React.useState('');
  const nameInputRef = React.useRef();

  const handleModalToggle = () => {
    setModalOpen(!isModalOpen);
  };

  const handleNameInputChange = value => {
    setNameValue(value);
  };

  const handleEmailInputChange = value => {
    setEmailValue(value);
  };
  const handleAddressInputChange = value => {
    setAddressValue(value);
  };

  React.useEffect(() => {
    if (isModalOpen && nameInputRef && nameInputRef.current) {
      (nameInputRef.current as HTMLInputElement).focus();
    }
  }, [isModalOpen]);

  return (
    <React.Fragment>
      <Button variant="primary" onClick={handleModalToggle}>
        Show modal
      </Button>
      <Modal
        variant={ModalVariant.small}
        title="Create account"
        description="Enter your personal information below to create an account."
        isOpen={isModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button key="create" variant="primary" form="modal-with-form-form" onClick={handleModalToggle}>
            Confirm
          </Button>,
          <Button key="cancel" variant="link" onClick={handleModalToggle}>
            Cancel
          </Button>
        ]}
      >
        <Form id="modal-with-form-form">
          <FormGroup
            label="Name"
            labelIcon={
              <Popover
                headerContent={
                  <div>
                    The
                    <a href="https://schema.org/name" target="_blank" rel="noopener noreferrer">
                      name
                    </a>
                    of a
                    <a href="https://schema.org/Person" target="_blank" rel="noopener noreferrer">
                      Person
                    </a>
                  </div>
                }
                bodyContent={
                  <div>
                    Often composed of
                    <a href="https://schema.org/givenName" target="_blank" rel="noopener noreferrer">
                      givenName
                    </a>
                    and
                    <a href="https://schema.org/familyName" target="_blank" rel="noopener noreferrer">
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
                  aria-describedby="modal-with-form-form-name"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            }
            isRequired
            fieldId="modal-with-form-form-name"
          >
            <TextInput
              isRequired
              type="email"
              id="modal-with-form-form-name"
              name="modal-with-form-form-name"
              value={nameValue}
              onChange={handleNameInputChange}
              ref={nameInputRef}
            />
          </FormGroup>
          <FormGroup
            label="E-mail"
            labelIcon={
              <Popover
                headerContent={
                  <div>
                    The
                    <a href="https://schema.org/email" target="_blank" rel="noopener noreferrer">
                      e-mail
                    </a>
                    of a
                    <a href="https://schema.org/Person" target="_blank" rel="noopener noreferrer">
                      person
                    </a>
                  </div>
                }
                bodyContent={
                  <div>
                    Valid
                    <a href="https://schema.org/email" target="_blank" rel="noopener noreferrer">
                      e-mail
                    </a>
                    address.
                  </div>
                }
              >
                <button
                  type="button"
                  aria-label="More info for e-mail field"
                  onClick={e => e.preventDefault()}
                  aria-describedby="modal-with-form-form-email"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            }
            isRequired
            fieldId="modal-with-form-form-email"
          >
            <TextInput
              isRequired
              type="email"
              id="modal-with-form-form-email"
              name="modal-with-form-form-email"
              value={emailValue}
              onChange={handleEmailInputChange}
            />
          </FormGroup>
          <FormGroup
            label="Address"
            labelIcon={
              <Popover
                headerContent={
                  <div>
                    The
                    <a href="https://schema.org/address" target="_blank" rel="noopener noreferrer">
                      adress
                    </a>
                    of a
                    <a href="https://schema.org/Person" target="_blank" rel="noopener noreferrer">
                      person
                    </a>
                  </div>
                }
                bodyContent={
                  <div>
                    Valid
                    <a href="https://schema.org/PostalAddress" target="_blank" rel="noopener noreferrer">
                      postal address.
                    </a>
                  </div>
                }
              >
                <button
                  type="button"
                  aria-label="More info for address field"
                  onClick={e => e.preventDefault()}
                  aria-describedby="modal-with-form-form-address"
                  className="pf-c-form__group-label-help"
                >
                  <HelpIcon noVerticalAlign />
                </button>
              </Popover>
            }
            isRequired
            fieldId="modal-with-form-form-address"
          >
            <TextInput
              isRequired
              type="email"
              id="modal-with-form-form-address"
              name="modal-with-form-form-address"
              value={addressValue}
              onChange={handleAddressInputChange}
            />
          </FormGroup>
        </Form>
      </Modal>
    </React.Fragment>
  );
};
