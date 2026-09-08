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
import {
  getAdminPreviewBackgroundColor,
  toAdminPreviewCssVars,
} from "./previewCssVars";

type PreviewWindowProps = {
  cssVars: Record<string, string>;
};

export const PreviewWindow = ({ cssVars }: PreviewWindowProps) => (
  <>
    <style>{`
      .preview {
        ${toAdminPreviewCssVars(cssVars)}
      }
    `}</style>
    <Page className="preview" masthead={<Header />}>
      <PageSection
        hasBodyWrapper={false}
        style={{
          backgroundColor: getAdminPreviewBackgroundColor(cssVars),
        }}
      >
        <Tabs activeKey={1} className="pf-v6-u-p-lg">
          <Tab eventKey={0} title={<TabTitleText>Tab One</TabTitleText>} />
          <Tab eventKey={1} title={<TabTitleText>Tab Two</TabTitleText>} />
        </Tabs>
        <Alert title="Error" isInline variant="danger" />
        <Alert title="Success" isInline variant="success" />
        <p className="pf-v6-u-p-lg pf-v6-c-content">
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
