import React from "react";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  TextArea,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { FormAccess } from "../components/form-access/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { Link } from "react-router-dom";
import { useRealm } from "../context/realm-context/RealmContext";

export const NewClientProfileForm = () => {
  const { t } = useTranslation("realm-settings");
  const { register, errors } = useForm();
  const { realm } = useRealm();

  function save() {
    //TODO
  }

  return (
    <>
      <ViewHeader titleKey={t("newClientProfile")} divider />
      <PageSection variant="light">
        <FormAccess isHorizontal role="view-realm" className="pf-u-mt-lg">
          <FormGroup
            label={t("newClientProfileName")}
            fieldId="kc-name"
            isRequired
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              ref={register({ required: true })}
              type="text"
              id="kc-client-profile-name"
              name="name"
            />
          </FormGroup>
          <FormGroup
            label={t("common:description")}
            fieldId="kc-description"
            validated={
              errors.description
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={errors.description?.message}
          >
            <TextArea
              name="description"
              aria-label={t("description")}
              ref={register()}
              type="text"
              id="kc-client-profile-description"
            />
          </FormGroup>
          <ActionGroup>
            <Button
              variant="primary"
              onClick={save}
              data-testid="realm-settings-client-profile-save-button"
            >
              {t("common:save")}
            </Button>
            <Button
              id="cancelCreateProfile"
              component={Link}
              // @ts-ignore
              to={`/${realm}/realm-settings/clientPolicies`}
              data-testid="cancelCreateProfile"
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
