import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  NumberControl,
  SelectControl,
  SelectControlOption,
} from "@keycloak/keycloak-ui-shared";
import { UseFormReturn, UseFormSetValue, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type SettingsCacheProps = {
  form: UseFormReturn;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
  unWrap?: boolean;
};

const CACHE_CONFIG_KEYS = [
  "cachePolicy",
  "evictionDay",
  "evictionHour",
  "evictionMinute",
  "maxLifespan",
];

/**
 * Restores the multivalued shape of the config entries rendered by this
 * component. They are addressed with an array index (`config.maxLifespan[0]`),
 * which only resolves correctly as long as the form holds the raw
 * ComponentRepresentation. Screens that load a component through
 * `convertToFormValues()` need to call this, as that unwraps single valued
 * entries into plain strings, against which the index would resolve to the
 * first character of the value instead.
 */
export const setupCacheForm = (
  component: ComponentRepresentation,
  setValue: UseFormSetValue<ComponentRepresentation>,
) =>
  CACHE_CONFIG_KEYS.forEach((key) => {
    const value = component.config?.[key];
    if (value !== undefined) {
      setValue(`config.${key}`, Array.isArray(value) ? value : [value]);
    }
  });

const getValue = (value: string | string[] | undefined) => {
  if (Array.isArray(value)) {
    return value[0];
  }
  return value;
};

const CacheFields = ({ form }: { form: UseFormReturn }) => {
  const { t } = useTranslation();

  const cachePolicyType = useWatch({
    control: form.control,
    name: "config.cachePolicy",
  });

  const cachePolicy = getValue(cachePolicyType);

  const hourOptions: SelectControlOption[] = [];
  let hourDisplay = "";
  for (let index = 0; index < 24; index++) {
    if (index < 10) {
      hourDisplay = `0${index}`;
    } else {
      hourDisplay = `${index}`;
    }
    hourOptions.push({ key: `${index}`, value: hourDisplay });
  }

  const minuteOptions: SelectControlOption[] = [];
  let minuteDisplay = "";
  for (let index = 0; index < 60; index++) {
    if (index < 10) {
      minuteDisplay = `0${index}`;
    } else {
      minuteDisplay = `${index}`;
    }
    minuteOptions.push({ key: `${index}`, value: minuteDisplay });
  }

  return (
    <>
      <SelectControl
        id="kc-cache-policy"
        name="config.cachePolicy"
        label={t("cachePolicy")}
        labelIcon={t("cachePolicyHelp")}
        controller={{
          defaultValue: ["DEFAULT"],
        }}
        aria-label={t("selectCacheType")}
        options={[
          "DEFAULT",
          "EVICT_DAILY",
          "EVICT_WEEKLY",
          "MAX_LIFESPAN",
          "NO_CACHE",
        ]}
      />
      {cachePolicy === "EVICT_WEEKLY" ? (
        <SelectControl
          id="kc-eviction-day"
          name="config.evictionDay[0]"
          label={t("evictionDay")}
          labelIcon={t("evictionDayHelp")}
          controller={{
            defaultValue: "1",
          }}
          aria-label={t("selectEvictionDay")}
          options={[
            { key: "1", value: t("Sunday") },
            { key: "2", value: t("Monday") },
            { key: "3", value: t("Tuesday") },
            { key: "4", value: t("Wednesday") },
            { key: "5", value: t("Thursday") },
            { key: "6", value: t("Friday") },
            { key: "7", value: t("Saturday") },
          ]}
        />
      ) : null}
      {cachePolicy === "EVICT_DAILY" || cachePolicy === "EVICT_WEEKLY" ? (
        <>
          <SelectControl
            id="kc-eviction-hour"
            name="config.evictionHour[0]"
            label={t("evictionHour")}
            labelIcon={t("evictionHourHelp")}
            controller={{
              defaultValue: "0",
            }}
            aria-label={t("selectEvictionHour")}
            options={hourOptions}
          />
          <SelectControl
            id="kc-eviction-minute"
            name="config.evictionMinute[0]"
            label={t("evictionMinute")}
            labelIcon={t("evictionMinuteHelp")}
            controller={{
              defaultValue: "0",
            }}
            aria-label={t("selectEvictionMinute")}
            options={minuteOptions}
          />
        </>
      ) : null}
      {cachePolicy === "MAX_LIFESPAN" ? (
        <NumberControl
          data-testid="kerberos-cache-lifespan"
          name="config.maxLifespan[0]"
          label={t("maxLifespan")}
          labelIcon={t("maxLifespanHelp")}
          unit={t("ms")}
          controller={{ defaultValue: 0, rules: { min: 0 } }}
        />
      ) : null}
    </>
  );
};

export const SettingsCache = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
  unWrap = false,
}: SettingsCacheProps) => {
  const { t } = useTranslation();

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("cacheSettings")}
          description={t("cacheSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}
      {unWrap ? (
        <CacheFields form={form} />
      ) : (
        <FormAccess role="manage-realm" isHorizontal>
          <CacheFields form={form} />
        </FormAccess>
      )}
    </>
  );
};
