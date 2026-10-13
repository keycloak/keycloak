import {
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import type { ExpandableExecution } from "../execution-model";

type FlowRequirementDropdownProps = {
  flow: ExpandableExecution;
  onChange: (flow: ExpandableExecution) => void;
};

export const FlowRequirementDropdown = ({
  flow,
  onChange,
}: FlowRequirementDropdownProps) => {
  const { t } = useTranslation();
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
          onOpenChange={(isOpen) => setOpen(isOpen)}
          onSelect={(_event, value) => {
            flow.requirement = value?.toString();
            onChange(flow);
            setOpen(false);
          }}
          selected={flow.requirement}
          isOpen={open}
          toggle={(ref) => (
            <MenuToggle
              className="keycloak__authentication__requirement-dropdown"
              ref={ref}
              onClick={() => setOpen(!open)}
              isExpanded={open}
            >
              {t(`requirements.${flow.requirement}`)}
            </MenuToggle>
          )}
        >
          <SelectList>{options}</SelectList>
        </Select>
      )}
      {(!flow.requirementChoices || flow.requirementChoices.length <= 1) && (
        <>{t(`requirements.${flow.requirement}`)}</>
      )}
    </>
  );
};
