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
package org.keycloak.saml.processing.core.saml.v2.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.SystemPropertiesUtil;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Util class dealing with xml based time
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 6, 2009
 */
public class XMLTimeUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Add additional time in miliseconds
     *
     * @param value calendar whose value needs to be updated
     * @param milis
     *
     * @return calendar value with the addition
     *
     * @throws org.keycloak.saml.common.exceptions.ConfigurationException
     */
    public static XMLGregorianCalendar add(XMLGregorianCalendar value, long milis) throws ConfigurationException {
        XMLGregorianCalendar newVal = (XMLGregorianCalendar) value.clone();

        Duration duration;
        try {
            duration = newDatatypeFactory().newDuration(milis);
        } catch (DatatypeConfigurationException e) {
            throw logger.configurationError(e);
        }
        newVal.add(duration);
        return newVal;
    }

    /**
     * Subtract some miliseconds from the time value
     *
     * @param value
     * @param milis miliseconds entered in a positive value
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static XMLGregorianCalendar subtract(XMLGregorianCalendar value, long milis) throws ConfigurationException {
        if (milis < 0)
            throw logger.invalidArgumentError("milis should be a positive value");
        return add(value, -1 * milis);
    }

    /**
     * Returns a XMLGregorianCalendar in the timezone specified. If the timezone is not valid, then the timezone falls
     * back to
     * "GMT"
     *
     * @param timezone
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static XMLGregorianCalendar getIssueInstant(String timezone) throws ConfigurationException {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        DatatypeFactory dtf;
        try {
            dtf = newDatatypeFactory();
        } catch (DatatypeConfigurationException e) {
            throw logger.configurationError(e);
        }

        GregorianCalendar gc = new GregorianCalendar(tz);
        XMLGregorianCalendar xgc = dtf.newXMLGregorianCalendar(gc);

        return xgc;
    }

    /**
     * Get the current instant of time
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static XMLGregorianCalendar getIssueInstant() throws ConfigurationException {
        return getIssueInstant(getCurrentTimeZoneID());
    }

    public static String getCurrentTimeZoneID() {
        String timezonePropertyValue = SecurityActions.getSystemProperty(GeneralConstants.TIMEZONE, "GMT");

        TimeZone timezone;
        if (GeneralConstants.TIMEZONE_DEFAULT.equals(timezonePropertyValue)) {
            timezone = TimeZone.getDefault();
        } else {
            timezone = TimeZone.getTimeZone(timezonePropertyValue);
        }

        return timezone.getID();
    }

    /**
     * Convert the minutes into miliseconds
     *
     * @param valueInMins
     *
     * @return
     */
    public static long inMilis(int valueInMins) {
        return valueInMins * 60 * 1000;
    }

    /**
     * Validate that the current time falls between the two boundaries
     *
     * @param now
     * @param notbefore
     * @param notOnOrAfter
     *
     * @return
     */
    public static boolean isValid(XMLGregorianCalendar now, XMLGregorianCalendar notbefore, XMLGregorianCalendar notOnOrAfter) {
        int val = 0;

        if (notbefore != null) {
            val = notbefore.compare(now);

            if (val == DatatypeConstants.INDETERMINATE || val == DatatypeConstants.GREATER)
                return false;
        }

        if (notOnOrAfter != null) {
            val = notOnOrAfter.compare(now);

            if (val != DatatypeConstants.GREATER)
                return false;
        }

        return true;
    }

    /**
     * Given a string, get the Duration object. The string can be an ISO 8601 period representation (Eg.: P10M) or a
     * numeric
     * value. If a ISO 8601 period, the duration will reflect the defined format. If a numeric (Eg.: 1000) the duration
     * will
     * be calculated in milliseconds.
     *
     * @param timeValue
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ParsingException
     */
    public static Duration parseAsDuration(String timeValue) throws ParsingException {
        if (timeValue == null) {
            PicketLinkLoggerFactory.getLogger().nullArgumentError("duration time");
        }

        DatatypeFactory factory = null;

        try {
            factory = newDatatypeFactory();
        } catch (DatatypeConfigurationException e) {
            throw logger.parserError(e);
        }

        try {
            // checks if it is a ISO 8601 period. If not it must be a numeric value.
            if (timeValue.startsWith("P")) {
                return factory.newDuration(timeValue);
            } else {
                return factory.newDuration(Long.valueOf(timeValue));
            }
        } catch (Exception e) {
            throw logger.samlMetaDataFailedToCreateCacheDuration(timeValue);
        }
    }

    /**
     * Given a string representing xml time, parse into {@code XMLGregorianCalendar}
     *
     * @param timeString
     *
     * @return
     *
     * @throws ParsingException
     */
    public static XMLGregorianCalendar parse(String timeString) throws ParsingException {
        DatatypeFactory factory = null;
        try {
            factory = newDatatypeFactory();
        } catch (DatatypeConfigurationException e) {
            throw logger.parserError(e);
        }
        return factory.newXMLGregorianCalendar(timeString);
    }


    /**
     * Create a new {@link DatatypeFactory}
     *
     * @return
     *
     * @throws DatatypeConfigurationException
     */
    public static DatatypeFactory newDatatypeFactory() throws DatatypeConfigurationException {
        boolean tccl_jaxp = SystemPropertiesUtil.getSystemProperty(GeneralConstants.TCCL_JAXP, "false")
                .equalsIgnoreCase("true");
        ClassLoader prevTCCL = SecurityActions.getTCCL();
        try {
            if (tccl_jaxp) {
                SecurityActions.setTCCL(XMLTimeUtil.class.getClassLoader());
            }
            return DatatypeFactory.newInstance();
        } finally {
            if (tccl_jaxp) {
                SecurityActions.setTCCL(prevTCCL);
            }
        }
    }
}