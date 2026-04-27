import {
  Alert,
  Button,
  Form,
  Page,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
  TextInput,
} from "@patternfly/react-core";
import { Header } from "../../PageHeader";

type PreviewWindowProps = {
  cssVars: Record<string, string>;
};

export const PreviewWindow = ({ cssVars }: PreviewWindowProps) => (
  <>
    <style>{`
      .preview {
        ${Object.entries(cssVars)
          .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
          .join("\n")}
      }
    `}</style>
    <Page className="preview" header={<Header />}>
      <PageSection
        variant="light"
        style={{
          backgroundColor: cssVars["BackgroundColor--light-100"],
        }}
      >
        <Tabs activeKey={1} className="pf-v5-u-p-lg">
          <Tab eventKey={0} title={<TabTitleText>Tab One</TabTitleText>} />
          <Tab eventKey={1} title={<TabTitleText>Tab Two</TabTitleText>} />
        </Tabs>
        <Alert title="Error" isInline variant="danger" />
        <Alert title="Success" isInline variant="success" />
        <p className="pf-v5-u-p-lg pf-v5-c-content">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        </p>
        <Form>
          <TextInput id="test" placeholder="Text input" />
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="link">Link button</Button>
        </Form>
      </PageSection>
    </Page>
  </>
);
