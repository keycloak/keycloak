import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import {
  AlertVariant,
  Button,
  Form,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { CheckIcon, PencilAltIcon, TimesIcon } from "@patternfly/react-icons";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type UserLabelForm = {
  userLabel: string;
};

type InlineLabelEditProps = {
  userId: string;
  credential: CredentialRepresentation;
  isEditable: boolean;
  toggle: () => void;
};

export const InlineLabelEdit = ({
  userId,
  credential,
  isEditable,
  toggle,
}: InlineLabelEditProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { register, handleSubmit } = useForm<UserLabelForm>();

  const { addAlert, addError } = useAlerts();

  const saveUserLabel = async (userLabel: UserLabelForm) => {
    try {
      await adminClient.users.updateCredentialLabel(
        {
          id: userId,
          credentialId: credential.id!,
        },
        userLabel.userLabel || "",
      );
      addAlert(t("updateCredentialUserLabelSuccess"), AlertVariant.success);
      toggle();
    } catch (error) {
      addError("updateCredentialUserLabelError", error);
    }
  };

  return (
    <Form
      isHorizontal
      className="kc-form-userLabel"
      onSubmit={handleSubmit(saveUserLabel)}
    >
      <FormGroup fieldId="kc-userLabel" className="kc-userLabel-row">
        <div className="kc-form-group-userLabel">
          {isEditable ? (
            <>
              <TextInput
                data-testid="userLabelFld"
                defaultValue={credential.userLabel}
                className="kc-userLabel"
                aria-label={t("userLabel")}
                {...register("userLabel")}
              />
              <div className="kc-userLabel-actionBtns">
                <Button
                  data-testid="editUserLabelAcceptBtn"
                  variant="link"
                  className="kc-editUserLabelAcceptBtn"
                  aria-label={t("acceptBtn")}
                  type="submit"
                  icon={<CheckIcon />}
                />
                <Button
                  data-testid="editUserLabelCancelBtn"
                  variant="link"
                  className="kc-editUserLabel-cancelBtn"
                  aria-label={t("cancelBtn")}
                  onClick={toggle}
                  icon={<TimesIcon />}
                />
              </div>
            </>
          ) : (
            <>
              {credential.userLabel}
              <Button
                aria-label={t("editUserLabel")}
                variant="link"
                className="kc-editUserLabel-btn"
                onClick={toggle}
                data-testid="editUserLabelBtn"
                icon={<PencilAltIcon />}
              />
            </>
          )}
        </div>
      </FormGroup>
    </Form>
  );
};
