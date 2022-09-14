/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.utils;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author pesilva
 *
 */
public class StringUtilTest {

    @Test
    public void removeAccent() {
        String accents     = "È,É,Ê,Ë,Û,Ù,Ï,Î,À,Â,Ô,è,é,ê,ë,û,ù,ï,î,à,â,ô,Ç,ç,Ã,ã,Õ,õ";
        String expected    = "E,E,E,E,U,U,I,I,A,A,O,e,e,e,e,u,u,i,i,a,a,o,C,c,A,a,O,o";

        String accents2    = "çÇáéíóúýÁÉÍÓÚÝàèìòùÀÈÌÒÙãõñäëïöüÿÄËÏÖÜÃÕÑâêîôûÂÊÎÔÛ";
        String expected2   = "cCaeiouyAEIOUYaeiouAEIOUaonaeiouyAEIOUAONaeiouAEIOU";

        assertEquals(expected,  StringUtil.removeAccent(accents));
        assertEquals(expected2, StringUtil.removeAccent(accents2));
    }
    
    @Test
    public void removeSpecialCharaters() {
        String text = "This - text ! has \\ /allot # of % special % characters";
        String expected = "Thistexthasallotofspecialcharacters";

        assertEquals(expected,  StringUtil.removeSpecialCharacters(text));
    }
    
    @Test
    public void removeSpecialCharatersAndAccents() {
        String text = "spècial.Characters#*_";
        String expected = "specialCharacters";

        assertEquals(expected,  StringUtil.removeAccentAndSpecialCharacters(text));
    }

}
