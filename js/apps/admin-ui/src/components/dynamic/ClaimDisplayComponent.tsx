import {
  ActionList,
  ActionListItem,
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateFooter,
  Flex,
  FlexItem,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useEffect, useState, useRef } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import type { ComponentProps } from "./components";

type ClaimDisplayEntry = {
  name: string;
  locale: string;
};

type IdClaimDisplayEntry = ClaimDisplayEntry & {
  id: string;
};

const generateId = () => crypto.randomUUID();

export const ClaimDisplayComponent = ({
  name,
  label,
  helpText,
  required,
  isDisabled,
  defaultValue,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { getValues, setValue, register } = useFormContext();
  const [displays, setDisplays] = useState<IdClaimDisplayEntry[]>([]);
  const fieldName = convertToName(name!);
  const debounceTimeoutRef = useRef<number | null>(null);

  useEffect(() => {
    register(fieldName);
    const value = getValues(fieldName) || defaultValue;

    try {
      const parsed: ClaimDisplayEntry[] = value
        ? typeof value === "string"
          ? JSON.parse(value)
          : value
        : [];
      setDisplays(parsed.map((entry) => ({ ...entry, id: generateId() })));
    } catch {
      setDisplays([]);
    }
  }, [defaultValue, fieldName, getValues, register]);

  useEffect(() => {
    return () => {
      if (debounceTimeoutRef.current !== null) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, []);

  const appendNew = () => {
    const newDisplays = [
      ...displays,
      { name: "", locale: "", id: generateId() },
    ];
    setDisplays(newDisplays);
    syncFormValue(newDisplays);
  };

  const syncFormValue = (val = displays) => {
    const filteredEntries = val
      .filter((e) => e.name !== "" && e.locale !== "")
      .map((entry) => ({ name: entry.name, locale: entry.locale }));

    setValue(fieldName, JSON.stringify(filteredEntries), {
      shouldDirty: true,
      shouldValidate: true,
    });
  };

  const debouncedUpdate = (val: IdClaimDisplayEntry[]) => {
    if (debounceTimeoutRef.current !== null) {
      clearTimeout(debounceTimeoutRef.current);
    }
    debounceTimeoutRef.current = window.setTimeout(() => {
      syncFormValue(val);
      debounceTimeoutRef.current = null;
    }, 300);
  };

  const flushUpdate = () => {
    if (debounceTimeoutRef.current !== null) {
      clearTimeout(debounceTimeoutRef.current);
      debounceTimeoutRef.current = null;
    }
    syncFormValue();
  };

  const updateName = (index: number, name: string) => {
    const newDisplays = [
      ...displays.slice(0, index),
      { ...displays[index], name },
      ...displays.slice(index + 1),
    ];
    setDisplays(newDisplays);
    debouncedUpdate(newDisplays);
  };

  const updateLocale = (index: number, locale: string) => {
    const newDisplays = [
      ...displays.slice(0, index),
      { ...displays[index], locale },
      ...displays.slice(index + 1),
    ];
    setDisplays(newDisplays);
    debouncedUpdate(newDisplays);
  };

  const remove = (index: number) => {
    const value = [...displays.slice(0, index), ...displays.slice(index + 1)];
    setDisplays(value);
    syncFormValue(value);
  };

  return displays.length !== 0 ? (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <Flex direction={{ default: "column" }}>
        <Flex>
          <FlexItem flex={{ default: "flex_1" }}>
            <strong>{t("claimDisplayName")}</strong>
          </FlexItem>
          <FlexItem flex={{ default: "flex_1" }}>
            <strong>{t("claimDisplayLocale")}</strong>
          </FlexItem>
        </Flex>
        {displays.map((display, index) => (
          <Flex key={display.id} data-testid="claim-display-row">
            <FlexItem flex={{ default: "flex_1" }}>
              <TextInput
                id={`${fieldName}.${index}.name`}
                data-testid={`${fieldName}.${index}.name`}
                value={display.name}
                onChange={(_event, value) => updateName(index, value)}
                onBlur={() => flushUpdate()}
                isDisabled={isDisabled}
                placeholder={t("claimDisplayNamePlaceholder")}
              />
            </FlexItem>
            <FlexItem flex={{ default: "flex_1" }}>
              <TextInput
                id={`${fieldName}.${index}.locale`}
                data-testid={`${fieldName}.${index}.locale`}
                value={display.locale}
                onChange={(_event, value) => updateLocale(index, value)}
                onBlur={() => flushUpdate()}
                isDisabled={isDisabled}
                placeholder={t("claimDisplayLocalePlaceholder")}
              />
            </FlexItem>
            <FlexItem>
              <Button
                variant="link"
                title={t("removeClaimDisplay")}
                isDisabled={isDisabled}
                onClick={() => remove(index)}
                data-testid={`${fieldName}.${index}.remove`}
              >
                <MinusCircleIcon />
              </Button>
            </FlexItem>
          </Flex>
        ))}
      </Flex>
      <ActionList>
        <ActionListItem>
          <Button
            data-testid={`${fieldName}-add-row`}
            className="pf-v5-u-px-0 pf-v5-u-mt-sm"
            variant="link"
            icon={<PlusCircleIcon />}
            onClick={() => appendNew()}
          >
            {t("addClaimDisplay")}
          </Button>
        </ActionListItem>
      </ActionList>
    </FormGroup>
  ) : (
    <EmptyState
      data-testid={`${fieldName}-empty-state`}
      className="pf-v5-u-p-0"
      variant="xs"
    >
      <EmptyStateBody>{t("noClaimDisplayEntries")}</EmptyStateBody>
      <EmptyStateFooter>
        <Button
          data-testid={`${fieldName}-add-row`}
          variant="link"
          icon={<PlusCircleIcon />}
          size="sm"
          onClick={appendNew}
          isDisabled={isDisabled}
        >
          {t("addClaimDisplay")}
        </Button>
      </EmptyStateFooter>
    </EmptyState>
  );
};
