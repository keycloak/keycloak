import React, { useEffect, useState } from "react";
import { useHistory, useParams, useRouteMatch } from "react-router-dom";
import {
  PageSection,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useFieldArray, useForm } from "react-hook-form";

import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { UserForm } from "./UserForm";

export const UsersTabs = () => {
  const { t } = useTranslation("roles");
  const form = useForm<UserRepresentation>({ mode: "onChange" });
//   const history = useHistory();

//   const adminClient = useAdminClient();
//   const [role, setRole] = useState<RoleFormType>();

//   const { id, clientId } = useParams<{ id: string; clientId: string }>();
//   const { url } = useRouteMatch();

//   const { realm } = useRealm();

//   const [key, setKey] = useState("");


//   const { addAlert } = useAlerts();

//   const { fields, append, remove } = useFieldArray({
//     control: form.control,
//     name: "attributes",
//   });

  useEffect(() => append({ key: "", value: "" }), [append, role]);

//   const save = async (user: UserRepresentation) => {
    // try {
    //   const { attributes, ...rest } = role;
    //   const roleRepresentation: RoleRepresentation = rest;
    //   if (id) {
    //     if (attributes) {
    //       roleRepresentation.attributes = arrayToAttributes(attributes);
    //     }
    //     if (!clientId) {
    //       await adminClient.roles.updateById({ id }, roleRepresentation);
    //     } else {
    //       await adminClient.clients.updateRole(
    //         { id: clientId, roleName: role.name! },
    //         roleRepresentation
    //       );
    //     }

    //     await adminClient.roles.createComposite(
    //       { roleId: id, realm },
    //       additionalRoles
    //     );

    //     setRole(role);
    //   } else {
    //     let createdRole;
    //     if (!clientId) {
    //       await adminClient.roles.create(roleRepresentation);
    //       createdRole = await adminClient.roles.findOneByName({
    //         name: role.name!,
    //       });
    //     } else {
    //       await adminClient.clients.createRole({
    //         id: clientId,
    //         name: role.name,
    //       });
    //       if (role.description) {
    //         await adminClient.clients.updateRole(
    //           { id: clientId, roleName: role.name! },
    //           roleRepresentation
    //         );
    //       }
    //       createdRole = await adminClient.clients.findRole({
    //         id: clientId,
    //         roleName: role.name!,
    //       });
    //     }
    //     setRole(convert(createdRole));
    //     history.push(
    //       url.substr(0, url.lastIndexOf("/") + 1) + createdRole.id + "/details"
    //     );
    //   }
    //   addAlert(t(id ? "roleSaveSuccess" : "roleCreated"), AlertVariant.success);
    // } catch (error) {
    //   addAlert(
    //     t((id ? "roleSave" : "roleCreate") + "Error", {
    //       error: error.response.data?.errorMessage || error,
    //     }),
    //     AlertVariant.danger
    //   );
    // }
//   };

  return (
    <>
      <ViewHeader
        titleKey={role?.name || t("createRole")}
        subKey={id ? "" : "roles:roleCreateExplain"}
        actionsDropdownId="roles-actions-dropdown"
      />
      <PageSection variant="light">
          <UserForm
            reset={() => form.reset()}
            form={form}
            save={save}
            editMode={false}
          />
      </PageSection>
    </>
  );
};
