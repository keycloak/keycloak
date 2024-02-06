import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { Controller, useForm } from "react-hook-form";

import { useAlerts } from "../components/alert/Alerts";
import useOrgFetcher from "./useOrgFetcher";
import { useRealm } from "../context/realm-context/RealmContext";
import type { OrgRepresentation } from "./routes";
import { HelpItem } from "../components/help-enabler/HelpItem";

type AddInvitationProps = {
  toggleVisibility: () => void;
  org: OrgRepresentation;
  refresh: () => void;
};
export default function AddInvitation({
  toggleVisibility,
  org,
  refresh,
}: AddInvitationProps) {
  const {
    formState: { errors },
    handleSubmit,
    control,
  } = useForm();
  const { realm } = useRealm();
  const { createInvitation } = useOrgFetcher(realm);
  const { addAlert } = useAlerts();

  const submitForm = async (invitation: any) => {
    await createInvitation(
      org.id,
      invitation.email,
      true,
      invitation.redirectUri,
    );
    addAlert(`${invitation.email} has been invited`);
    refresh();
    toggleVisibility();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title="Invite a User"
      isOpen={true}
      onClose={toggleVisibility}
      actions={[
        <Button
          data-testid="createinvitation"
          key="confirm"
          variant="primary"
          type="submit"
          form="invitation-form"
        >
          Create
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            toggleVisibility();
          }}
        >
          Cancel
        </Button>,
      ]}
    >
      <Form
        id="invitation-form"
        isHorizontal
        onSubmit={handleSubmit(submitForm)}
      >
        <FormGroup
          name="create-modal-org-invitation"
          label="Email"
          fieldId="email"
          helperTextInvalid="Required"
          validated={
            errors.email ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <Controller
            name="email"
            control={control}
            rules={{ required: true }}
            render={({ field }) => (
              <TextInput
                autoFocus
                id="email"
                value={field.value}
                onChange={field.onChange}
                data-testid="email-input"
              />
            )}
          />
        </FormGroup>
        <FormGroup
          name="create-modal-org-invitation"
          label="Redirect URI"
          fieldId="redirectUri"
          labelIcon={
            <HelpItem helpText="redirectUriHelp" fieldLabelId="redirectUri" />
          }
          validated={
            errors.redirectUri
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        >
          <Controller
            name="redirectUri"
            control={control}
            rules={{ required: false }}
            render={({ field }) => (
              <TextInput
                id="redirectUri"
                value={field.value}
                onChange={field.onChange}
                data-testid="redirectUri-input"
              />
            )}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
}
