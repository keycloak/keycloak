import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  PageSection,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useEffect } from "react";

import { LdapSettingsAdvanced } from "./ldap/LdapSettingsAdvanced";
import { LdapSettingsKerberosIntegration } from "./ldap/LdapSettingsKerberosIntegration";
import { LdapSettingsCache } from "./ldap/LdapSettingsCache";
import { LdapSettingsSynchronization } from "./ldap/LdapSettingsSynchronization";
import { LdapSettingsGeneral } from "./ldap/LdapSettingsGeneral";
import { LdapSettingsConnection } from "./ldap/LdapSettingsConnection";
import { LdapSettingsSearching } from "./ldap/LdapSettingsSearching";
import { ScrollForm } from "../components/scroll-form/ScrollForm";

import { useHistory, useParams } from "react-router-dom";
import { useRealm } from "../context/realm-context/RealmContext";
import { convertToFormValues } from "../util";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

import { useForm } from "react-hook-form";

export const UserFederationLdapSettings = () => {
  const { t } = useTranslation("user-federation");
  const form = useForm<ComponentRepresentation>();
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const { id } = useParams<{ id: string }>();
  const { addAlert } = useAlerts();

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", form.setValue);
      } else {
        form.setValue(entry[0], entry[1]);
      }
    });
  };

  const save = async (component: ComponentRepresentation) => {
    try {
      await adminClient.components.update({ id }, component);
      setupForm(component as ComponentRepresentation);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(`${t("saveError")} '${error}'`, AlertVariant.danger);
    }
  };

  return (
    <>
      <PageSection variant="light" isFilled>
        <ScrollForm
          sections={[
            t("generalOptions"),
            t("connectionAndAuthenticationSettings"),
            t("ldapSearchingAndUpdatingSettings"),
            t("synchronizationSettings"),
            t("kerberosIntegration"),
            t("cacheSettings"),
            t("advancedSettings"),
          ]}
        >
          <LdapSettingsGeneral form={form} />
          <LdapSettingsConnection form={form} />
          <LdapSettingsSearching form={form} />
          <LdapSettingsSynchronization form={form} />
          <LdapSettingsKerberosIntegration form={form} />
          <LdapSettingsCache form={form} />
          <LdapSettingsAdvanced form={form} />
        </ScrollForm>
        <Form onSubmit={form.handleSubmit(save)}>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              onClick={() => history.push(`/${realm}/user-federation`)}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
