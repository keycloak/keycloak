import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AlertVariant } from "@patternfly/react-core";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { AttributeForm } from "../../components/key-value-form/AttributeForm";
import { RoleForm } from "../../components/role-form/RoleForm";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toClient } from "../routes/Client";
import { toClientRole } from "../routes/ClientRole";
import { NewRoleParams } from "../routes/NewRole";

export default function CreateClientRole() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const navigate = useNavigate();
  const { clientId } = useParams<NewRoleParams>();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const onSubmit: SubmitHandler<AttributeForm> = async (formValues) => {
    const role: RoleRepresentation = {
      ...formValues,
      name: formValues.name?.trim(),
      attributes: {},
    };

    try {
      await adminClient.clients.createRole({
        id: clientId,
        ...role,
      });

      const createdRole = (await adminClient.clients.findRole({
        id: clientId!,
        roleName: role.name!,
      }))!;

      addAlert(t("roleCreated"), AlertVariant.success);
      navigate(
        toClientRole({
          realm,
          clientId: clientId!,
          id: createdRole.id!,
          tab: "details",
        }),
      );
    } catch (error) {
      addError("roleCreateError", error);
    }
  };

  return (
    <FormProvider {...form}>
      <RoleForm
        form={form}
        onSubmit={onSubmit}
        cancelLink={toClient({
          realm,
          clientId: clientId!,
          tab: "roles",
        })}
        role="manage-clients"
        editMode={false}
      />
    </FormProvider>
  );
}
