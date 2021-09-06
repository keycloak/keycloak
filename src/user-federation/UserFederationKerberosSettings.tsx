import React, { useEffect } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  Form,
  PageSection,
} from "@patternfly/react-core";

import { KerberosSettingsRequired } from "./kerberos/KerberosSettingsRequired";
import { SettingsCache } from "./shared/SettingsCache";
import { useRealm } from "../context/realm-context/RealmContext";
import { convertToFormValues } from "../util";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";

import { Controller, useForm } from "react-hook-form";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useTranslation } from "react-i18next";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useHistory, useParams } from "react-router-dom";

type KerberosSettingsHeaderProps = {
  onChange: (value: string) => void;
  value: string;
  save: () => void;
  toggleDeleteDialog: () => void;
};

const KerberosSettingsHeader = ({
  onChange,
  value,
  save,
  toggleDeleteDialog,
}: KerberosSettingsHeaderProps) => {
  const { t } = useTranslation("user-federation");
  const { id } = useParams<{ id: string }>();
  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "user-federation:userFedDisableConfirmTitle",
    messageKey: "user-federation:userFedDisableConfirm",
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange("false");
      save();
    },
  });
  return (
    <>
      <DisableConfirm />
      {id === "new" ? (
        <ViewHeader titleKey="Kerberos" />
      ) : (
        <ViewHeader
          titleKey="Kerberos"
          dropdownItems={[
            <DropdownItem
              key="delete"
              onClick={() => toggleDeleteDialog()}
              data-testid="delete-kerberos-cmd"
            >
              {t("deleteProvider")}
            </DropdownItem>,
          ]}
          isEnabled={value === "true"}
          onToggle={(value) => {
            if (!value) {
              toggleDisableDialog();
            } else {
              onChange("" + value);
              save();
            }
          }}
        />
      )}
    </>
  );
};

export const UserFederationKerberosSettings = () => {
  const { t } = useTranslation("user-federation");
  const form = useForm<ComponentRepresentation>({ mode: "onChange" });
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const { id } = useParams<{ id: string }>();

  const { addAlert, addError } = useAlerts();

  useEffect(() => {
    (async () => {
      if (id !== "new") {
        const fetchedComponent = await adminClient.components.findOne({ id });
        if (fetchedComponent) {
          setupForm(fetchedComponent);
        }
      }
    })();
  }, []);

  const setupForm = (component: ComponentRepresentation) => {
    form.reset();
    Object.entries(component).map((entry) => {
      form.setValue(
        "config.allowPasswordAuthentication",
        component.config?.allowPasswordAuthentication
      );
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", form.setValue);
      }
      form.setValue(entry[0], entry[1]);
    });
  };

  const save = async (component: ComponentRepresentation) => {
    try {
      if (id) {
        if (id === "new") {
          await adminClient.components.create(component);
          history.push(`/${realm}/user-federation`);
        } else {
          await adminClient.components.update({ id }, component);
        }
      }
      setupForm(component as ComponentRepresentation);
      addAlert(
        t(id === "new" ? "createSuccess" : "saveSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError(
        `user-federation:${id === "new" ? "createError" : "saveError"}`,
        error
      );
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "user-federation:userFedDeleteConfirmTitle",
    messageKey: "user-federation:userFedDeleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id });
        addAlert(t("userFedDeletedSuccess"), AlertVariant.success);
        history.replace(`/${realm}/user-federation`);
      } catch (error: any) {
        addAlert("user-federation:userFedDeleteError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <Controller
        name="config.enabled[0]"
        defaultValue={["true"][0]}
        control={form.control}
        render={({ onChange, value }) => (
          <KerberosSettingsHeader
            value={value}
            onChange={onChange}
            save={() => save(form.getValues())}
            toggleDeleteDialog={toggleDeleteDialog}
          />
        )}
      />
      <PageSection variant="light">
        <KerberosSettingsRequired form={form} showSectionHeading />
      </PageSection>
      <PageSection variant="light" isFilled>
        <SettingsCache form={form} showSectionHeading />
        <Form onSubmit={form.handleSubmit(save)}>
          <ActionGroup>
            <Button
              isDisabled={!form.formState.isDirty}
              variant="primary"
              type="submit"
              data-testid="kerberos-save"
            >
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              onClick={() => history.push(`/${realm}/user-federation`)}
              data-testid="kerberos-cancel"
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
