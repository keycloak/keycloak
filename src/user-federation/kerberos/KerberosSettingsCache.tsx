import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import React, { useEffect, useState } from "react";
import { convertToFormValues } from "../../util";
import { useForm, Controller, useWatch } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useParams } from "react-router-dom";
import _ from "lodash";

export const KerberosSettingsCache = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const adminClient = useAdminClient();
  const { register, control, setValue } = useForm<ComponentRepresentation>();
  const { id } = useParams<{ id: string }>();

  const cachePolicyType = useWatch({
    control: control,
    name: "config.cachePolicy",
  });

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      setValue("config.cachePolicy", component.config?.cachePolicy);
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", setValue);
      } else {
        setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  const [isCachePolicyDropdownOpen, setIsCachePolicyDropdownOpen] = useState(
    false
  );
  const [isEvictionHourDropdownOpen, setIsEvictionHourDropdownOpen] = useState(
    false
  );
  const [
    isEvictionMinuteDropdownOpen,
    setIsEvictionMinuteDropdownOpen,
  ] = useState(false);
  const [isEvictionDayDropdownOpen, setIsEvictionDayDropdownOpen] = useState(
    false
  );

  const hourOptions = [
    <SelectOption key={0} value={t("common:selectOne")} isPlaceholder />,
  ];
  for (let index = 1; index <= 24; index++) {
    hourOptions.push(<SelectOption key={index + 1} value={[`${index}`]} />);
  }

  const minuteOptions = [
    <SelectOption key={0} value={t("common:selectOne")} isPlaceholder />,
  ];
  for (let index = 1; index <= 60; index++) {
    minuteOptions.push(<SelectOption key={index + 1} value={[`${index}`]} />);
  }

  return (
    <>
      {/* Cache settings */}
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("cachePolicy")}
          labelIcon={
            <HelpItem
              helpText={helpText("cachePolicyHelp")}
              forLabel={t("cachePolicy")}
              forID="kc-cache-policy"
            />
          }
          fieldId="kc-cache-policy"
        >
          <Controller
            name="config.cachePolicy"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-cache-policy"
                required
                onToggle={() =>
                  setIsCachePolicyDropdownOpen(!isCachePolicyDropdownOpen)
                }
                isOpen={isCachePolicyDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsCachePolicyDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption
                  key={0}
                  value={t("common:selectOne")}
                  isPlaceholder
                />
                <SelectOption key={1} value={["DEFAULT"]} />
                <SelectOption key={2} value={["EVICT_DAILY"]} />
                <SelectOption key={3} value={["EVICT_WEEKLY"]} />
                <SelectOption key={4} value={["MAX_LIFESPAN"]} />
                <SelectOption key={5} value={["NO_CACHE"]} />
              </Select>
            )}
          ></Controller>
        </FormGroup>

        {_.isEqual(cachePolicyType, ["EVICT_WEEKLY"]) ? (
          <FormGroup
            label={t("evictionDay")}
            labelIcon={
              <HelpItem
                helpText={helpText("evictionDayHelp")}
                forLabel={t("evictionDay")}
                forID="kc-eviction-day"
              />
            }
            fieldId="kc-eviction-day"
          >
            <Controller
              name="config.evictionDay"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-eviction-day"
                  required
                  onToggle={() =>
                    setIsEvictionDayDropdownOpen(!isEvictionDayDropdownOpen)
                  }
                  isOpen={isEvictionDayDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setIsEvictionDayDropdownOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                >
                  <SelectOption
                    key={0}
                    value={t("common:selectOne")}
                    isPlaceholder
                  />
                  <SelectOption key={1} value={["1"]}>
                    {t("common:Sunday")}
                  </SelectOption>
                  <SelectOption key={2} value={["2"]}>
                    {t("common:Monday")}
                  </SelectOption>
                  <SelectOption key={3} value={["3"]}>
                    {t("common:Tuesday")}
                  </SelectOption>
                  <SelectOption key={4} value={["4"]}>
                    {t("common:Wednesday")}
                  </SelectOption>
                  <SelectOption key={5} value={["5"]}>
                    {t("common:Thursday")}
                  </SelectOption>
                  <SelectOption key={6} value={["6"]}>
                    {t("common:Friday")}
                  </SelectOption>
                  <SelectOption key={7} value={["7"]}>
                    {t("common:Saturday")}
                  </SelectOption>
                </Select>
              )}
            ></Controller>
          </FormGroup>
        ) : (
          <></>
        )}

        {_.isEqual(cachePolicyType, ["EVICT_DAILY"]) ||
        _.isEqual(cachePolicyType, ["EVICT_WEEKLY"]) ? (
          <>
            <FormGroup
              label={t("evictionHour")}
              labelIcon={
                <HelpItem
                  helpText={helpText("evictionHourHelp")}
                  forLabel={t("evictionHour")}
                  forID="kc-eviction-hour"
                />
              }
              fieldId="kc-eviction-hour"
            >
              <Controller
                name="config.evictionHour"
                defaultValue=""
                control={control}
                render={({ onChange, value }) => (
                  <Select
                    toggleId="kc-eviction-hour"
                    onToggle={() =>
                      setIsEvictionHourDropdownOpen(!isEvictionHourDropdownOpen)
                    }
                    isOpen={isEvictionHourDropdownOpen}
                    onSelect={(_, value) => {
                      onChange(value as string);
                      setIsEvictionHourDropdownOpen(false);
                    }}
                    selections={value}
                    variant={SelectVariant.single}
                  >
                    {hourOptions}
                  </Select>
                )}
              ></Controller>
            </FormGroup>

            <FormGroup
              label={t("evictionMinute")}
              labelIcon={
                <HelpItem
                  helpText={helpText("evictionMinuteHelp")}
                  forLabel={t("evictionMinute")}
                  forID="kc-eviction-minute"
                />
              }
              fieldId="kc-eviction-minute"
            >
              <Controller
                name="config.evictionMinute"
                defaultValue=""
                control={control}
                render={({ onChange, value }) => (
                  <Select
                    toggleId="kc-eviction-minute"
                    onToggle={() =>
                      setIsEvictionMinuteDropdownOpen(
                        !isEvictionMinuteDropdownOpen
                      )
                    }
                    isOpen={isEvictionMinuteDropdownOpen}
                    onSelect={(_, value) => {
                      onChange(value as string);
                      setIsEvictionMinuteDropdownOpen(false);
                    }}
                    selections={value}
                    variant={SelectVariant.single}
                  >
                    {minuteOptions}
                  </Select>
                )}
              ></Controller>
            </FormGroup>
          </>
        ) : (
          <></>
        )}
        {_.isEqual(cachePolicyType, ["MAX_LIFESPAN"]) ? (
          <FormGroup
            label={t("maxLifespan")}
            labelIcon={
              <HelpItem
                helpText={helpText("maxLifespanHelp")}
                forLabel={t("maxLifespan")}
                forID="kc-max-lifespan"
              />
            }
            fieldId="kc-max-lifespan"
          >
            <TextInput
              isRequired
              type="text"
              id="kc-max-lifespan"
              name="config.maxLifespan"
              ref={register}
            />
          </FormGroup>
        ) : (
          <></>
        )}
      </FormAccess>
    </>
  );
};
