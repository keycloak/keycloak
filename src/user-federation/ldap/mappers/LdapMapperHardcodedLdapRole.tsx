import { AlertVariant, Button, FormGroup, TextInput } from "@patternfly/react-core";
import React, { useState } from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../../context/auth/AdminClient";
import { useAlerts } from "../../../components/alert/Alerts";
import { RoleMappingPayload } from "keycloak-admin/lib/defs/roleRepresentation";


import { AddRoleMappingModal, MappingType } from "../../../components/role-mapping/AddRoleMappingModal";

export type LdapMapperHardcodedLdapRoleProps = {
  form: UseFormMethods;
};

export const LdapMapperHardcodedLdapRole = ({
  form,
}: LdapMapperHardcodedLdapRoleProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const adminClient = useAdminClient();

  const [showAssign, setShowAssign] = useState(false);

  const { addAlert } = useAlerts();


  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      await adminClient.clientScopes.addRealmScopeMappings(
        {
          id,
        },
        realmRoles
      );
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.clientScopes.addClientScopeMappings(
              {
                id,
                client: row.client!.id!,
              },
              [row.role as RoleMappingPayload]
            )
          )
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("roleMappingUpdatedError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
          {showAssign && (
        // MF 042921 hardcoded for now, to see modal displayed
        <AddRoleMappingModal
        id="e2d7fe7c-f7bc-4903-9562-3d079ae8667c"
        type="client-scope"
        name="name"
          // id={id}
          // type={type}
          // name={name}
        onAssign={assignRoles}
        onClose={() => setShowAssign(false)}
        />)}

      <FormGroup
        label={t("common:role")}
        labelIcon={
          <HelpItem
            helpText={helpText("roleHelp")}
            forLabel={t("common:role")}
            forID="kc-role"
          />
        }
        fieldId="kc-role"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-role"
          data-testid="role"
          name="config.role[0]"
          ref={form.register}
        />
              <Button
                data-testid="assignRole"
                onClick={() => setShowAssign(true)}
              >
                {t("assignRole")}
              </Button>
      </FormGroup>
    </>
  );
};
