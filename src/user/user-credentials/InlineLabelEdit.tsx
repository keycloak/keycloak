import React from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import {
  AlertVariant,
  Button,
  Form,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { CheckIcon, PencilAltIcon, TimesIcon } from "@patternfly/react-icons";

import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";

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
  const { t } = useTranslation("users");
  const { register, handleSubmit } = useForm<UserLabelForm>();

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const saveUserLabel = async (userLabel: UserLabelForm) => {
    try {
      await adminClient.users.updateCredentialLabel(
        {
          id: userId,
          credentialId: credential.id!,
        },
        userLabel.userLabel || ""
      );
      addAlert(t("updateCredentialUserLabelSuccess"), AlertVariant.success);
      toggle();
    } catch (error) {
      addError("users:updateCredentialUserLabelError", error);
    }
  };

  return (
    <Form isHorizontal className="kc-form-userLabel">
      <FormGroup fieldId="kc-userLabel" className="kc-userLabel-row">
        <div className="kc-form-group-userLabel">
          {isEditable ? (
            <>
              <TextInput
                name="userLabel"
                defaultValue={credential.userLabel}
                ref={register()}
                type="text"
                className="kc-userLabel"
                aria-label={t("userLabel")}
                data-testid="user-label-fld"
              />
              <div className="kc-userLabel-actionBtns">
                <Button
                  data-testid="editUserLabel-acceptBtn"
                  variant="link"
                  className="kc-editUserLabel-acceptBtn"
                  onClick={() => {
                    handleSubmit(saveUserLabel)();
                  }}
                  icon={<CheckIcon />}
                />
                <Button
                  data-testid="editUserLabel-cancelBtn"
                  variant="link"
                  className="kc-editUserLabel-cancelBtn"
                  onClick={toggle}
                  icon={<TimesIcon />}
                />
              </div>
            </>
          ) : (
            <>
              {credential.userLabel}
              <Button
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
