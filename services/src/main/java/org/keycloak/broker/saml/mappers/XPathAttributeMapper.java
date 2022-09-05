package org.keycloak.broker.saml.mappers;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.w3c.dom.Document;

public class XPathAttributeMapper extends AbstractIdentityProviderMapper implements SamlMetadataDescriptorUpdater {

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    private static final Logger LOGGER = Logger.getLogger(XPathAttributeMapper.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String ATTRIBUTE_XPATH = "attribute.xpath";
    public static final String ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_FRIENDLY_NAME = "attribute.friendly.name";
    public static final String USER_ATTRIBUTE = "user.attribute";
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("xmlns:(\\w+)=\"(.+?)\"");

    private static final ThreadLocal<XPathFactory> XPATH_FACTORY = ThreadLocal.withInitial(() -> {
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        xPathFactory.setXPathVariableResolver(variableName -> {
            throw new RuntimeException("resolveVariable for variable " + variableName + " not supported");
        });
        xPathFactory.setXPathFunctionResolver((functionName, arity) -> {
            throw new RuntimeException("resolveFunction for function " + functionName + " not supported");
        });
        return xPathFactory;
    });

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_XPATH);
        property.setLabel("Attribute XPath");
        property.setHelpText("XPath expression to search for. All attributes are surrounded with a <root> element. Given prefixes "
                + "and namespaces are preserved. Example: <root><myPrefix:Person xmlns:myPrefix=\"http://my.namespace/schema\">"
                + "<myPrefix:FirstName>John</myPrefix:FirstName>...</myPrefix:Person></root> or <root>Some attribute value of anyType</root>");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME);
        property.setLabel("Attribute Name");
        property.setHelpText("Name of attribute to search for in assertion and apply XPath. You can leave this blank to try to apply XPath to all attributes or specify a friendly name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_FRIENDLY_NAME);
        property.setLabel("Friendly Name");
        property.setHelpText("Friendly name of attribute to search for in assertion. You can leave this blank to try to apply XPath to all attributes or specify a name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store XPath value. Use " + UserModel.EMAIL + ", " + UserModel.FIRST_NAME + ", and " + UserModel.LAST_NAME + " for e-mail, first and last name, respectively.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-xpath-attribute-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "XPath Attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (StringUtil.isNullOrEmpty(attribute)) {
            return;
        }
        String attributeName = getAttributeNameFromMapperModel(mapperModel);
        String attributeXPath = mapperModel.getConfig().get(ATTRIBUTE_XPATH);

        List<String> attributeValuesInContext = findAttributeValuesInContext(attributeName, attributeXPath, context);
        if (!attributeValuesInContext.isEmpty()) {
            context.setUserAttribute(attribute, attributeValuesInContext);
        }
    }

    private String getAttributeNameFromMapperModel(IdentityProviderMapperModel mapperModel) {
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (attributeName == null) {
            attributeName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        }
        return attributeName;
    }

    private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
        return attributeType -> {
            AttributeType attribute = attributeType.getAttribute();
            return attributeName == null
                    || Objects.equals(attribute.getName(), attributeName)
                    || Objects.equals(attribute.getFriendlyName(), attributeName);
        };
    }

    private static Function<String, Object> applyXPath(String attributeXPath) {
        return xml -> {
            try {
                LOGGER.tracef("Trying to parse: %s", xml);

                Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(xml);
                Map<String, String> namespaces = new HashMap<>();
                Map<String, String> prefixes = new HashMap<>();
                while (namespaceMatcher.find()) {
                    namespaces.put(namespaceMatcher.group(1), namespaceMatcher.group(2));
                    prefixes.put(namespaceMatcher.group(2), namespaceMatcher.group(1));
                }

                XPath xPath = XPATH_FACTORY.get().newXPath();
                xPath.setNamespaceContext(new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String prefix) {
                        if (namespaces.containsKey(prefix)) {
                            return namespaces.get(prefix);
                        }

                        return XMLConstants.NULL_NS_URI;
                    }

                    @Override
                    public String getPrefix(String namespaceURI) {
                        if (prefixes.containsKey(namespaceURI)) {
                            return prefixes.get(namespaceURI);
                        }

                        return null;
                    }

                    @Override
                    public Iterator<String> getPrefixes(String namespaceURI) {
                        List<String> list = new ArrayList<>();
                        if (prefixes.containsKey(namespaceURI)) {
                            list.add(prefixes.get(namespaceURI));
                        }

                        return list.iterator();
                    }
                });
                Document document = DocumentUtil.getDocument(new StringReader(xml));
                return xPath.compile(attributeXPath).evaluate(document, XPathConstants.STRING);
            } catch (XPathExpressionException e) {
                LOGGER.warn("Unparsable element will be ignored", e);
                return "";
            } catch (Exception e) {
                throw new RuntimeException("Could not parse xml element", e);
            }
        };
    }

    private List<String> findAttributeValuesInContext(String attributeName, String attributeXPath, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()
                .map(AttributeStatementType::getAttributes)
                .flatMap(Collection::stream)
                .filter(elementWith(attributeName))
                .map(AttributeStatementType.ASTChoiceType::getAttribute)
                .map(AttributeType::getAttributeValue)
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(Object::toString)
                .map(s -> "<root>" + s + "</root>")
                .map(applyXPath(attributeXPath))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (StringUtil.isNullOrEmpty(attribute)) {
            return;
        }
        String attributeName = getAttributeNameFromMapperModel(mapperModel);
        String attributeXPath = mapperModel.getConfig().get(ATTRIBUTE_XPATH);
        List<String> attributeValuesInContext = findAttributeValuesInContext(attributeName, attributeXPath, context);
        if (!attributeValuesInContext.isEmpty()) {
            user.setAttribute(attribute, attributeValuesInContext);
        }
    }

    @Override
    public String getHelpText() {
        return "Extract text of a saml attribute via XPath expression and import into the specified user property or attribute.";
    }

    // ISpMetadataAttributeProvider interface
    @Override
    public void updateMetadata(IdentityProviderMapperModel mapperModel, EntityDescriptorType entityDescriptor) {
        RequestedAttributeType requestedAttribute = new RequestedAttributeType(mapperModel.getConfig().get(XPathAttributeMapper.ATTRIBUTE_NAME));
        requestedAttribute.setIsRequired(null);
        requestedAttribute.setNameFormat(ATTRIBUTE_FORMAT_BASIC.get());

        String attributeFriendlyName = mapperModel.getConfig().get(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME);
        if (attributeFriendlyName != null && attributeFriendlyName.length() > 0)
            requestedAttribute.setFriendlyName(attributeFriendlyName);

        // Add the requestedAttribute item to any AttributeConsumingServices
        for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
            List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();

            if (descriptors != null) {
                for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                    if (descriptor.getSpDescriptor() != null && descriptor.getSpDescriptor().getAttributeConsumingService() != null) {
                        for (AttributeConsumingServiceType attributeConsumingService: descriptor.getSpDescriptor().getAttributeConsumingService())
                        {
                            attributeConsumingService.addRequestedAttribute(requestedAttribute);
                        }
                    }
                }
            }
        }
    }
}
