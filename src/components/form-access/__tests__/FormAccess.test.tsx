import React from "react";
import { mount } from "enzyme";
import { Controller, useForm } from "react-hook-form";
import { FormGroup, Switch, TextInput } from "@patternfly/react-core";

import { WhoAmI, WhoAmIContext } from "../../../context/whoami/WhoAmI";
import whoami from "../../../context/whoami/__tests__/mock-whoami.json";
import { RealmContext } from "../../../context/realm-context/RealmContext";
import { AccessContextProvider } from "../../../context/access/Access";

import { FormAccess } from "../FormAccess";

describe("<FormAccess />", () => {
  const Form = ({ realm }: { realm: string }) => {
    const { register, control } = useForm();
    return (
      <WhoAmIContext.Provider value={new WhoAmI("master", whoami)}>
        <RealmContext.Provider value={{ realm, setRealm: () => {} }}>
          <AccessContextProvider>
            <FormAccess role="manage-clients">
              <FormGroup label="test" fieldId="field">
                <TextInput
                  type="text"
                  id="field"
                  name="fieldName"
                  ref={register()}
                />
              </FormGroup>
              <Controller
                name="consentRequired"
                defaultValue={false}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-consent"
                    label={"on"}
                    labelOff="off"
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormAccess>
          </AccessContextProvider>
        </RealmContext.Provider>
      </WhoAmIContext.Provider>
    );
  };
  it("render normal form", () => {
    const comp = mount(<Form realm="master" />);
    expect(comp).toMatchSnapshot();
  });

  it("render form disabled for test realm", () => {
    const container = mount(<Form realm="test" />);

    const disabled = container.find("input#field").props().disabled;
    expect(disabled).toBe(true);

    expect(container.find("input#kc-consent").props().disabled).toBe(true);
  });
});
