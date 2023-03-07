import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Select, SelectOption, SelectVariant } from "@patternfly/react-core";

import type { ExpandableExecution } from "../execution-model";

type FlowRequirementDropdownProps = {
  flow: ExpandableExecution;
  onChange: (flow: ExpandableExecution) => void;
};

export const FlowRequirementDropdown = ({
  flow,
  onChange,
}: FlowRequirementDropdownProps) => {
  const { t } = useTranslation("authentication");
  const [open, setOpen] = useState(false);

  const options = flow.requirementChoices!.map((option, index) => (
    <SelectOption key={index} value={option}>
      {t(`requirements.${option}`)}
    </SelectOption>
  ));

  return (
    <>
      {flow.requirementChoices && flow.requirementChoices.length > 1 && (
        <Select
          className="keycloak__authentication__requirement-dropdown"
          variant={SelectVariant.single}
          onToggle={setOpen}
          onSelect={(_event, value) => {
            flow.requirement = value.toString();
            onChange(flow);
            setOpen(false);
          }}
          selections={[flow.requirement]}
          isOpen={open}
        >
          {options}
        </Select>
      )}
      {(!flow.requirementChoices || flow.requirementChoices.length <= 1) && (
        <>{t(`requirements.${flow.requirement}`)}</>
      )}
    </>
  );
};
