import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AlertVariant } from "@patternfly/react-core";
import { SubmitHandler, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { useAlerts } from "../components/alert/Alerts";
import { AttributeForm } from "../components/key-value-form/AttributeForm";
import { RoleForm } from "../components/role-form/RoleForm";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { toRealmRole } from "./routes/RealmRole";
import { toRealmRoles } from "./routes/RealmRoles";

export default function CreateRealmRole() {
  const { t } = useTranslation("roles");
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const onSubmit: SubmitHandler<AttributeForm> = async (formValues) => {
    const role: RoleRepresentation = {
      ...formValues,
      name: formValues.name?.trim(),
      attributes: {},
    };

    try {
      await adminClient.roles.create(role);

      const createdRole = await adminClient.roles.findOneByName({
        name: formValues.name!,
      });

      if (!createdRole) {
        throw new Error(t("common:notFound"));
      }

      addAlert(t("roleCreated"), AlertVariant.success);
      navigate(toRealmRole({ realm, id: createdRole.id!, tab: "details" }));
    } catch (error) {
      addError("roles:roleCreateError", error);
    }
  };

  return (
    <RoleForm
      form={form}
      onSubmit={onSubmit}
      cancelLink={toRealmRoles({ realm })}
      role="manage-realm"
      editMode={false}
    />
  );
}
