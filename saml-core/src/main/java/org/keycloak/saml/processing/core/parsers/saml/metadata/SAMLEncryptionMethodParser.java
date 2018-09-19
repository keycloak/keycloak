package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptionMethodType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import java.math.BigInteger;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.ENCRYPTION_METHOD;

/**
 * @author mhajas
 */
public class SAMLEncryptionMethodParser extends AbstractStaxSamlMetadataParser<EncryptionMethodType> {

    private static final SAMLEncryptionMethodParser INSTANCE = new SAMLEncryptionMethodParser();

    public SAMLEncryptionMethodParser() {
        super(ENCRYPTION_METHOD);
    }

    public static SAMLEncryptionMethodParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected EncryptionMethodType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new EncryptionMethodType(StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_ALGORITHM));
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, EncryptionMethodType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch(element) {
            case KEY_SIZE:
                {
                    StaxParserUtil.advance(xmlEventReader);
                    BigInteger keySize = BigInteger.valueOf(Long.valueOf(StaxParserUtil.getElementText(xmlEventReader)));

                    EncryptionMethodType.EncryptionMethod encMethod = target.getEncryptionMethod();
                    if (encMethod == null) {
                        encMethod = new EncryptionMethodType.EncryptionMethod();
                        target.setEncryptionMethod(encMethod);
                    }

                    encMethod.setKeySize(keySize);
                }
                break;

            case OAEP_PARAMS:
                {
                    StaxParserUtil.advance(xmlEventReader);
                    byte[] OAEPparams = StaxParserUtil.getElementText(xmlEventReader).getBytes(GeneralConstants.SAML_CHARSET);
                    EncryptionMethodType.EncryptionMethod encMethod = target.getEncryptionMethod();
                    if (encMethod == null){
                        encMethod = new EncryptionMethodType.EncryptionMethod();
                        target.setEncryptionMethod(encMethod);
                    }

                    encMethod.setOAEPparams(OAEPparams);
                }
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
