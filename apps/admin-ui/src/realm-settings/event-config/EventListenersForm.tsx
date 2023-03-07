import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

type EventListenersFormProps = {
  form: UseFormReturn;
  reset: () => void;
};

export const EventListenersForm = ({
  form,
  reset,
}: EventListenersFormProps) => {
  const { t } = useTranslation("realm-settings");
  const {
    control,
    formState: { isDirty },
  } = form;

  const [selectEventListenerOpen, setSelectEventListenerOpen] = useState(false);
  const serverInfo = useServerInfo();
  const eventListeners = serverInfo.providers?.eventsListener.providers;

  return (
    <>
      <FormGroup
        hasNoPaddingTop
        label={t("eventListeners")}
        fieldId={"kc-eventListeners"}
        labelIcon={
          <HelpItem
            helpText={t("eventListenersHelpText")}
            fieldLabelId="realm-settings:eventListeners"
          />
        }
      >
        <Controller
          name="eventsListeners"
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              name="eventsListeners"
              className="kc_eventListeners_select"
              data-testid="eventListeners-select"
              chipGroupProps={{
                numChips: 3,
                expandedText: t("common:hide"),
                collapsedText: t("common:showRemaining"),
              }}
              variant={SelectVariant.typeaheadMulti}
              typeAheadAriaLabel="Select"
              onToggle={(isOpen) => setSelectEventListenerOpen(isOpen)}
              selections={field.value}
              onSelect={(_, selectedValue) => {
                const option = selectedValue.toString();
                const changedValue = field.value.includes(option)
                  ? field.value.filter((item: string) => item !== option)
                  : [...field.value, option];
                field.onChange(changedValue);
              }}
              onClear={(operation) => {
                operation.stopPropagation();
                field.onChange([]);
              }}
              isOpen={selectEventListenerOpen}
              aria-labelledby={"eventsListeners"}
            >
              {Object.keys(eventListeners!).map((event) => (
                <SelectOption key={event} value={event} />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <ActionGroup>
        <Button
          variant="primary"
          type="submit"
          data-testid={"saveEventListenerBtn"}
          isDisabled={!isDirty}
        >
          {t("common:save")}
        </Button>
        <Button
          variant="link"
          data-testid={"revertEventListenerBtn"}
          onClick={reset}
        >
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </>
  );
};
