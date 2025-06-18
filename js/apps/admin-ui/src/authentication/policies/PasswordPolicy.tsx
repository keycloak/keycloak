import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Select,
  MenuToggle,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { useEffect, useMemo, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { PolicyRow } from "./PolicyRow";
import { SubmittedValues, parsePolicy, serializePolicy } from "./util";

type PolicySelectProps = {
  onSelect: (row: PasswordPolicyTypeRepresentation) => void;
  selectedPolicies: PasswordPolicyTypeRepresentation[];
};

const PolicySelect = ({ onSelect, selectedPolicies }: PolicySelectProps) => {
  const { t } = useTranslation();
  const { passwordPolicies } = useServerInfo();
  const [open, setOpen] = useState(false);

  const policies = useMemo(
    () =>
      passwordPolicies?.filter(
        (p) => selectedPolicies.find((o) => o.id === p.id) === undefined,
      ),
    [selectedPolicies],
  );

  return (
    <Select
      onSelect={(_, selection) => {
        onSelect(selection as PasswordPolicyTypeRepresentation);
        setOpen(false);
      }}
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          onClick={() => setOpen(!open)}
          isExpanded={open}
          isDisabled={policies?.length === 0}
          style={{ width: "300px" }}
          data-testid="add-policy"
        >
          {t("addPolicy")}
        </MenuToggle>
      )}
      isOpen={open}
    >
      <SelectList>
        {policies?.map((policy) => (
          <SelectOption key={policy.id} value={policy}>
            {policy.displayName}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
};

type PasswordPolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
};

export const PasswordPolicy = ({
  realm,
  realmUpdated,
}: PasswordPolicyProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { passwordPolicies } = useServerInfo();

  const { addAlert, addError } = useAlerts();
  const { realm: realmName, refresh } = useRealm();

  const [rows, setRows] = useState<PasswordPolicyTypeRepresentation[]>([]);
  const onSelect = (row: PasswordPolicyTypeRepresentation) => {
    setRows([...rows, row]);
    setValue(row.id!, row.defaultValue!, { shouldDirty: true });
  };

  const form = useForm<SubmittedValues>({
    defaultValues: {},
  });
  const {
    handleSubmit,
    setValue,
    reset,
    formState: { isDirty },
  } = form;

  const setupForm = (realm: RealmRepresentation) => {
    reset();
    const values = parsePolicy(realm.passwordPolicy || "", passwordPolicies!);
    values.forEach((v) => {
      setValue(v.id!, v.value!);
    });
    setRows(values);
  };

  useEffect(() => setupForm(realm), []);

  const save = async (values: SubmittedValues) => {
    const updatedRealm = {
      ...realm,
      passwordPolicy: serializePolicy(rows, values),
    };
    try {
      await adminClient.realms.update({ realm: realmName }, updatedRealm);
      realmUpdated(updatedRealm);
      setupForm(updatedRealm);
      refresh();
      addAlert(t("updatePasswordPolicySuccess"), AlertVariant.success);
    } catch (error: any) {
      addError("updatePasswordPolicyError", error);
    }
  };

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
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
                className="keycloak__policies_authentication__form"
                role="manage-realm"
                isHorizontal
                onSubmit={handleSubmit(save)}
              >
                {rows.map((r, index) => (
                  <PolicyRow
                    key={`${r.id}-${index}`}
                    policy={r}
                    onRemove={(id) => {
                      setRows(rows.filter((r) => r.id !== id));
                      setValue(r.id!, "", { shouldDirty: true });
                    }}
                  />
                ))}
                <ActionGroup>
                  <Button
                    data-testid="save"
                    variant="primary"
                    type="submit"
                    isDisabled={!isDirty}
                  >
                    {t("save")}
                  </Button>
                  <Button
                    data-testid="reload"
                    variant={ButtonVariant.link}
                    onClick={() => setupForm(realm)}
                  >
                    {t("reload")}
                  </Button>
                </ActionGroup>
              </FormAccess>
            </FormProvider>
          </PageSection>
        </>
      )}
      {!rows.length && !realm.passwordPolicy && (
        <EmptyState data-testid="empty-state" variant="lg">
          <EmptyStateHeader
            titleText={<>{t("noPasswordPolicies")}</>}
            icon={<EmptyStateIcon icon={PlusCircleIcon} />}
            headingLevel="h1"
          />
          <EmptyStateBody>{t("noPasswordPoliciesInstructions")}</EmptyStateBody>
          <EmptyStateFooter>
            <EmptyStateActions>
              <PolicySelect onSelect={onSelect} selectedPolicies={[]} />
            </EmptyStateActions>
          </EmptyStateFooter>
        </EmptyState>
      )}
    </PageSection>
  );
};
