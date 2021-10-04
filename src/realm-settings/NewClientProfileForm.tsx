import React, { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  DropdownItem,
  Flex,
  FlexItem,
  FormGroup,
  PageSection,
  Text,
  TextArea,
  TextInput,
  TextVariants,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { FormAccess } from "../components/form-access/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { Link, useHistory } from "react-router-dom";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { PlusCircleIcon } from "@patternfly/react-icons";
import "./RealmSettingsSection.css";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";

type NewClientProfileForm = Required<ClientProfileRepresentation>;

const defaultValues: NewClientProfileForm = {
  name: "",
  description: "",
  executors: [],
};

export const NewClientProfileForm = () => {
  const { t } = useTranslation("realm-settings");
  const { getValues, register, errors } = useForm<NewClientProfileForm>({
    defaultValues,
  });
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const adminClient = useAdminClient();
  const [globalProfiles, setGlobalProfiles] = useState<
    ClientProfileRepresentation[]
  >([]);
  const [profiles, setProfiles] = useState<ClientProfileRepresentation[]>([]);
  const [showAddExecutorsForm, setShowAddExecutorsForm] = useState(false);
  const [createdProfile, setCreatedProfile] =
    useState<ClientProfileRepresentation>();
  const form = getValues();
  const history = useHistory();

  useFetch(
    () =>
      adminClient.clientPolicies.listProfiles({ includeGlobalProfiles: true }),
    (profiles) => {
      setGlobalProfiles(profiles.globalProfiles ?? []);
      setProfiles(profiles.profiles ?? []);
    },
    []
  );

  const save = async () => {
    const form = getValues();

    const createdProfile = {
      ...form,
      executors: [],
    };

    const allProfiles = profiles.concat(createdProfile);

    try {
      await adminClient.clientPolicies.createProfiles({
        profiles: allProfiles,
        globalProfiles: globalProfiles,
      });
      addAlert(
        t("realm-settings:createClientProfileSuccess"),
        AlertVariant.success
      );
      setShowAddExecutorsForm(true);
      setCreatedProfile(createdProfile);
    } catch (error) {
      addError("realm-settings:createClientProfileError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientProfileConfirmTitle"),
    messageKey: t("deleteClientProfileConfirm"),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      const updatedProfiles = profiles.filter(
        (profile) => profile.name !== createdProfile?.name
      );

      try {
        await adminClient.clientPolicies.createProfiles({
          profiles: updatedProfiles,
          globalProfiles,
        });
        addAlert(t("deleteClientSuccess"), AlertVariant.success);
        history.push(`/${realm}/realm-settings/clientPolicies`);
      } catch (error) {
        addError(t("deleteClientError"), error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={showAddExecutorsForm ? form.name : t("newClientProfile")}
        divider
        dropdownItems={
          showAddExecutorsForm
            ? [
                <DropdownItem
                  key="delete"
                  value="delete"
                  onClick={toggleDeleteDialog}
                  data-testid="deleteClientProfileDropdown"
                >
                  {t("deleteClientProfile")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess isHorizontal role="view-realm" className="pf-u-mt-lg">
          <FormGroup
            label={t("newClientProfileName")}
            fieldId="kc-name"
            helperText={t("createClientProfileNameHelperText")}
            isRequired
            helperTextInvalid={t("common:required")}
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
          >
            <TextInput
              ref={register({ required: true })}
              type="text"
              id="kc-client-profile-name"
              name="name"
              data-testid="client-profile-name"
            />
          </FormGroup>
          <FormGroup label={t("common:description")} fieldId="kc-description">
            <TextArea
              name="description"
              aria-label={t("description")}
              ref={register()}
              type="text"
              id="kc-client-profile-description"
              data-testid="client-profile-description"
            />
          </FormGroup>
          <ActionGroup>
            <Button
              variant="primary"
              onClick={save}
              data-testid="saveCreateProfile"
              isDisabled={showAddExecutorsForm ? true : false}
            >
              {t("common:save")}
            </Button>
            <Button
              id="cancelCreateProfile"
              component={(props) => (
                <Link
                  {...props}
                  to={`/${realm}/realm-settings/clientPolicies`}
                />
              )}
              data-testid="cancelCreateProfile"
            >
              {showAddExecutorsForm
                ? t("realm-settings:reload")
                : t("common:cancel")}
            </Button>
          </ActionGroup>
          {showAddExecutorsForm && (
            <>
              <Flex>
                <FlexItem>
                  <Text className="kc-executors" component={TextVariants.h1}>
                    {t("executors")}
                    <HelpItem
                      helpText={t("realm-settings:executorsHelpText")}
                      forLabel={t("executorsHelpItem")}
                      forID={t("executors")}
                    />
                  </Text>
                </FlexItem>
                <FlexItem align={{ default: "alignRight" }}>
                  <Button
                    id="addExecutor"
                    component={(props) => (
                      <Link
                        {...props}
                        to={`/${realm}/realm-settings/clientPolicies`}
                      ></Link>
                    )}
                    variant="link"
                    className="kc-addExecutor"
                    data-testid="cancelCreateProfile"
                    icon={<PlusCircleIcon />}
                  >
                    {t("realm-settings:addExecutor")}
                  </Button>
                </FlexItem>
              </Flex>
              <Divider />
              <Text className="kc-emptyExecutors" component={TextVariants.h6}>
                {t("realm-settings:emptyExecutors")}
              </Text>
            </>
          )}
        </FormAccess>
      </PageSection>
    </>
  );
};
