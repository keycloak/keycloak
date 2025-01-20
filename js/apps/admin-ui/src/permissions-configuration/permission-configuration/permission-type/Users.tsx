import { useTranslation } from "react-i18next";
import { FormGroup, Radio } from "@patternfly/react-core";
import { HelpItem, useFetch } from "@keycloak/keycloak-ui-shared";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { UserSelect } from "../../../components/users/UserSelect";
import { useAdminClient } from "../../../admin-client";
import { useParams } from "react-router-dom";
import { PermissionConfigurationDetailsParams } from "../../routes/PermissionConfigurationDetails";

export const Users = () => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useParams<PermissionConfigurationDetailsParams>();
  const form = useFormContext();
  const [allUses, setAllUses] = useState<string[]>([]);
  const [isSpecificUsers, setIsSpecificUsers] = useState(false);

  useFetch(
    () =>
      adminClient.users.find({
        realm,
      }),
    (users) => {
      const usersIds = users.map((user: any) => user.id);
      setAllUses(usersIds);
      form.setValue("resources", usersIds);
    },
    [realm],
  );

  return (
    <>
      <FormGroup
        label={t("resourceScope")}
        labelIcon={
          <HelpItem
            helpText={t("resourceScopeHelpText")}
            fieldLabelId="resource-scope"
          />
        }
        fieldId="resourceScope"
        hasNoPaddingTop
      >
        <Radio
          id="allUsers"
          data-testid="allUsers"
          isChecked={!isSpecificUsers}
          name="resourceScope"
          label={t("allUsers")}
          onChange={() => {
            setIsSpecificUsers(false);
            form.setValue("users", allUses);
          }}
          className="pf-v5-u-mb-md"
        />
        <Radio
          id="specificUsers"
          data-testid="specificUsers"
          isChecked={isSpecificUsers}
          name="resourceScope"
          label={t("specificUsers")}
          onChange={() => {
            setIsSpecificUsers(true);
            form.setValue("users", []);
          }}
          className="pf-v5-u-mb-md"
        />
      </FormGroup>
      {isSpecificUsers && (
        <UserSelect
          name="resources"
          helpText={t("permissionUsersHelpText")}
          defaultValue={[]}
          variant="typeaheadMulti"
        />
      )}
    </>
  );
};
