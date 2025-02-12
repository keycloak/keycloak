import { useTranslation } from "react-i18next";
import { FormGroup, Radio } from "@patternfly/react-core";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { UserSelect } from "../../components/users/UserSelect";

type PermissionTypeProps = {
  resourceType: string;
};

export const ResourceType = ({ resourceType }: PermissionTypeProps) => {
  const { t } = useTranslation();
  const form = useFormContext();
  const resourceIds: string[] = form.getValues("resources");
  const [isSpecificUsers, setIsSpecificUsers] = useState(
    resourceIds.filter((id) => {
      return "Users" !== id;
    }).length > 0,
  );

  return (
    <>
      <FormGroup
        label={t("EnforceAccessTo")}
        labelIcon={
          <HelpItem
            helpText={t("enforceAccessToHelpText")}
            fieldLabelId="apply-permission-to"
          />
        }
        fieldId="EnforceAccessTo"
        hasNoPaddingTop
        isRequired
      >
        <Radio
          id="allResources"
          data-testid="allResources"
          isChecked={!isSpecificUsers}
          name="EnforceAccessTo"
          label={t(`allResourceType`, { resourceType })}
          onChange={() => {
            setIsSpecificUsers(false);
            form.setValue("resources", []);
          }}
          className="pf-v5-u-mb-md"
        />
        <Radio
          id="specificResources"
          data-testid="specificResources"
          isChecked={isSpecificUsers}
          name="EnforceAccessTo"
          label={t(`specificResourceType`, { resourceType })}
          onChange={() => {
            setIsSpecificUsers(true);
            form.setValue("resources", []);
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
