import React, { useMemo, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStatePrimary,
  PageSection,
  Select,
  SelectOption,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { parsePolicy, SubmittedValues } from "./util";
import { PolicyRow } from "./PolicyRow";
import { serializePolicy } from "./util";

type PolicySelectProps = {
  onSelect: (row: PasswordPolicyTypeRepresentation) => void;
  selectedPolicies: PasswordPolicyTypeRepresentation[];
};

const PolicySelect = ({ onSelect, selectedPolicies }: PolicySelectProps) => {
  const { t } = useTranslation("authentication");
  const { passwordPolicies } = useServerInfo();
  const [open, setOpen] = useState(false);

  const policies = useMemo(
    () =>
      passwordPolicies?.filter(
        (p) => selectedPolicies.find((o) => o.id === p.id) === undefined
      ),
    [selectedPolicies]
  );

  return (
    <Select
      width={300}
      onSelect={(_, selection) => {
        onSelect(selection as PasswordPolicyTypeRepresentation);
        setOpen(false);
      }}
      onToggle={(value) => setOpen(value)}
      isOpen={open}
      selections={t("addPolicy")}
      isDisabled={policies?.length === 0}
    >
      {policies?.map((policy) => (
        <SelectOption key={policy.id} value={policy}>
          {policy.displayName}
        </SelectOption>
      ))}
    </Select>
  );
};

export const PasswordPolicy = () => {
  const { t } = useTranslation("authentication");
  const { passwordPolicies } = useServerInfo();

  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();

  const [realm, setRealm] = useState<RealmRepresentation>();
  const [rows, setRows] = useState<PasswordPolicyTypeRepresentation[]>([]);
  const onSelect = (row: PasswordPolicyTypeRepresentation) =>
    setRows([...rows, row]);

  const form = useForm<SubmittedValues>({ shouldUnregister: false });
  const { handleSubmit, setValue, getValues } = form;

  const setupForm = (realm: RealmRepresentation) => {
    const values = parsePolicy(realm.passwordPolicy || "", passwordPolicies!);
    values.forEach((v) => {
      setValue(v.id!, v.value);
    });
    setRows(values);
  };

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      if (!realm) {
        throw new Error(t("common:notFound"));
      }
      return realm;
    },
    (realm) => {
      setRealm(realm);
      setupForm(realm);
    },
    []
  );

  const save = async (values: SubmittedValues) => {
    const updatedRealm = {
      ...realm,
      passwordPolicy: serializePolicy(rows, values),
    };
    try {
      await adminClient.realms.update({ realm: realmName }, updatedRealm);
      setRealm(updatedRealm);
      addAlert(t("updatePasswordPolicySuccess"), AlertVariant.success);
    } catch (error: any) {
      addError("authentication:updatePasswordPolicyError", error);
    }
  };

  if (!realm) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light" className="pf-u-p-0">
      {(rows.length !== 0 || realm.passwordPolicy) && (
        <>
          <Toolbar>
            <ToolbarContent>
              <ToolbarItem>
                <PolicySelect onSelect={onSelect} selectedPolicies={rows} />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
          <Divider />
          <PageSection variant="light">
            <FormProvider {...form}>
              <FormAccess
                role="manage-realm"
                isHorizontal
                onSubmit={handleSubmit(save)}
              >
                {rows.map((r, index) => (
                  <PolicyRow
                    key={`${r.id}-${index}`}
                    policy={r}
                    onRemove={(id) => setRows(rows.filter((r) => r.id !== id))}
                  />
                ))}
                <ActionGroup>
                  <Button
                    data-testid="save"
                    variant="primary"
                    type="submit"
                    isDisabled={
                      serializePolicy(rows, getValues()) ===
                      realm.passwordPolicy
                    }
                  >
                    {t("common:save")}
                  </Button>
                  <Button
                    data-testid="reload"
                    variant={ButtonVariant.link}
                    onClick={() => setupForm(realm)}
                  >
                    {t("common:reload")}
                  </Button>
                </ActionGroup>
              </FormAccess>
            </FormProvider>
          </PageSection>
        </>
      )}
      {!rows.length && !realm.passwordPolicy && (
        <EmptyState data-testid="empty-state" variant="large">
          <EmptyStateIcon icon={PlusCircleIcon} />
          <Title headingLevel="h1" size="lg">
            {t("noPasswordPolicies")}
          </Title>
          <EmptyStateBody>{t("noPasswordPoliciesInstructions")}</EmptyStateBody>
          <EmptyStatePrimary>
            <PolicySelect onSelect={onSelect} selectedPolicies={[]} />
          </EmptyStatePrimary>
        </EmptyState>
      )}
    </PageSection>
  );
};
