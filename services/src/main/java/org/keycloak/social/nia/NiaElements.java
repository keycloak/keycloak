/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.social.nia;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

/**
 *
 * @author lubbe
 */
public class NiaElements implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        NiaWriter niaWriter = new NiaWriter(writer);
        niaWriter.writeStartingTag();
        StaxUtil.flush(writer);

    }

}
