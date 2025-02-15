import { useTranslation } from "react-i18next";
import { FormGroup, Radio } from "@patternfly/react-core";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { UserSelect } from "../../components/users/UserSelect";
import { ClientSelect } from "../../components/client/ClientSelect";

type ResourceTypeProps = {
  resourceType: string;
};

const resourceTypeSelectComponents: Record<string, React.ElementType> = {
  Users: UserSelect,
  Clients: ClientSelect,
};

export const ResourceType = ({ resourceType }: ResourceTypeProps) => {
  const { t } = useTranslation();
  const form = useFormContext();
  const resourceIds: string[] = form.getValues("resources");

  const [isSpecificResources, setIsSpecificResources] = useState(
    resourceIds.some((id) => id !== resourceType),
  );

  const ResourceTypeSelectComponent =
    resourceTypeSelectComponents[resourceType];

  return (
    <>
      <FormGroup
        label={t("EnforceAccessTo")}
        labelIcon={
          <HelpItem
            helpText={t("enforceAccessToHelpText")}
            fieldLabelId="enforce-access-to"
          />
        }
        fieldId="EnforceAccessTo"
        hasNoPaddingTop
        isRequired
      >
        <Radio
          id="allResources"
          data-testid="allResources"
          isChecked={!isSpecificResources}
          name="EnforceAccessTo"
          label={t(`allResourceType`, { resourceType })}
          onChange={() => {
            setIsSpecificResources(false);
            form.setValue("resources", []);
          }}
          className="pf-v5-u-mb-md"
        />
        <Radio
          id="specificResources"
          data-testid="specificResources"
          isChecked={isSpecificResources}
          name="EnforceAccessTo"
          label={t(`specificResourceType`, { resourceType })}
          onChange={() => {
            setIsSpecificResources(true);
            form.setValue("resources", []);
          }}
          className="pf-v5-u-mb-md"
        />
      </FormGroup>
      {isSpecificResources && (
        <ResourceTypeSelectComponent
          name="resources"
          label={resourceType}
          helpText={t("resourceTypeHelpText", { resourceType })}
          defaultValue={[]}
          variant="typeaheadMulti"
        />
      )}
    </>
  );
};
