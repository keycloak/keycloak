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

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.common.util.Time;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.util.SecurityActions;
import org.keycloak.saml.common.util.SystemPropertiesUtil;

/**
 * Util class dealing with xml based time
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 6, 2009
 */
public class XMLTimeUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Add additional time in milliseconds
     *
     * @param value calendar whose value needs to be updated
     * @param millis
     *
     * @return calendar value with the addition
     */
    public static XMLGregorianCalendar add(XMLGregorianCalendar value, long millis) {
        if (value == null) {
            return null;
        }

        XMLGregorianCalendar newVal = (XMLGregorianCalendar) value.clone();

        if (millis == 0) {
            return newVal;
        }

        Duration duration;
        duration = DATATYPE_FACTORY.get().newDuration(millis);
        newVal.add(duration);
        return newVal;
    }

    /**
     * Subtract some milliseconds from the time value
     *
     * @param value
     * @param millis milliseconds entered in a positive value
     *
     * @return
     */
    public static XMLGregorianCalendar subtract(XMLGregorianCalendar value, long millis) {
        return add(value, - millis);
    }

    /**
     * Returns a XMLGregorianCalendar in the timezone specified. If the timezone is not valid, then the timezone falls
     * back to
     * "GMT"
     *
     * @param timezone
     *
     * @return
     */
    public static XMLGregorianCalendar getIssueInstant(String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        DatatypeFactory dtf;
        dtf = DATATYPE_FACTORY.get();

        GregorianCalendar gc = new GregorianCalendar(tz);
        XMLGregorianCalendar xgc = dtf.newXMLGregorianCalendar(gc);

        Long offsetMilis = TimeUnit.MILLISECONDS.convert(Time.getOffset(), TimeUnit.SECONDS);
        if (offsetMilis != 0) {
            if (logger.isDebugEnabled()) logger.debug(XMLTimeUtil.class.getName() + " timeOffset: " + offsetMilis);
            xgc.add(parseAsDuration(offsetMilis.toString()));
        }
        if (logger.isDebugEnabled()) logger.debug(XMLTimeUtil.class.getName() + " issueInstant: " + xgc.toString());
        return xgc;
    }

    /**
     * Get the current instant of time
     *
     * @return
     */
    public static XMLGregorianCalendar getIssueInstant() {
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
     * Convert the minutes into milliseconds
     *
     * @param valueInMins
     *
     * @return
     */
    public static long inMilis(int valueInMins) {
        return (long) valueInMins * 60 * 1000;
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
        int val;

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
     */
    public static Duration parseAsDuration(String timeValue) {
        if (timeValue == null) {
            PicketLinkLoggerFactory.getLogger().nullArgumentError("duration time");
        }

        DatatypeFactory factory = DATATYPE_FACTORY.get();

        try {
            // checks if it is a ISO 8601 period. If not it must be a numeric value.
            if (timeValue.startsWith("P")) {
                return factory.newDuration(timeValue);
            } else {
                return factory.newDuration(Long.parseLong(timeValue));
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
     */
    public static XMLGregorianCalendar parse(String timeString) {
        DatatypeFactory factory = DATATYPE_FACTORY.get();
        return factory.newXMLGregorianCalendar(timeString);
    }

    private static final ThreadLocal<DatatypeFactory> DATATYPE_FACTORY = new ThreadLocal<DatatypeFactory>() {
        @Override
        protected DatatypeFactory initialValue() {
            try {
                return newDatatypeFactory();
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * Create a new {@link DatatypeFactory}
     *
     * @return
     *
     * @throws DatatypeConfigurationException
     */
    private static DatatypeFactory newDatatypeFactory() throws DatatypeConfigurationException {
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
