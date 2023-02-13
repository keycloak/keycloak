/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.theme.beans;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import freemarker.template.TemplateModelException;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkExpirationFormatterMethodTest {

    protected static final Locale locale = Locale.ENGLISH;
    protected static final Properties messages = new Properties();
    static {
        messages.put("linkExpirationFormatter.timePeriodUnit.seconds.1", "second");
        messages.put("linkExpirationFormatter.timePeriodUnit.seconds", "seconds");
        messages.put("linkExpirationFormatter.timePeriodUnit.minutes.1", "minute");
        messages.put("linkExpirationFormatter.timePeriodUnit.minutes.3", "minutes-3");
        messages.put("linkExpirationFormatter.timePeriodUnit.minutes", "minutes");
        messages.put("linkExpirationFormatter.timePeriodUnit.hours.1", "hour");
        messages.put("linkExpirationFormatter.timePeriodUnit.hours", "hours");
        messages.put("linkExpirationFormatter.timePeriodUnit.days.1", "day");
        messages.put("linkExpirationFormatter.timePeriodUnit.days", "days");
    }

    protected List<Object> toList(Object... objects) {
        return Arrays.asList(objects);
    }
    
    @Test
    public void inputtypes_null() throws TemplateModelException{
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("", tested.exec(Collections.emptyList()));
    }
    
    @Test
    public void inputtypes_string_empty() throws TemplateModelException{
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("", tested.exec(toList("")));
        Assert.assertEquals(" ", tested.exec(toList(" ")));
    }

    @Test
    public void inputtypes_string_number() throws TemplateModelException{
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("2 minutes", tested.exec(toList("2")));
        Assert.assertEquals("2 minutes", tested.exec(toList(" 2 ")));
    }

    @Test
    public void inputtypes_string_notanumber() throws TemplateModelException{
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("ahoj", tested.exec(toList("ahoj")));
    }
    
    @Test
    public void inputtypes_number() throws TemplateModelException{
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("5 minutes", tested.exec(toList(new Integer(5))));
        Assert.assertEquals("5 minutes", tested.exec(toList(new Long(5))));
    }

    @Test
    public void format_second_zero() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("0 seconds", tested.exec(toList(0)));
    }

    @Test
    public void format_minute_one() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("1 minute", tested.exec(toList(1)));
    }

    @Test
    public void format_minute_more() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("2 minutes", tested.exec(toList(2)));
        //test support for languages with more plurals depending on the value
        Assert.assertEquals("3 minutes-3", tested.exec(toList(3)));
        Assert.assertEquals("5 minutes", tested.exec(toList(5)));
        Assert.assertEquals("24 minutes", tested.exec(toList(24)));
        Assert.assertEquals("59 minutes", tested.exec(toList(59)));
        Assert.assertEquals("61 minutes", tested.exec(toList(61)));
    }

    @Test
    public void format_hour_one() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("1 hour", tested.exec(toList(60)));
    }

    @Test
    public void format_hour_more() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("2 hours", tested.exec(toList(2 * 60)));
        Assert.assertEquals("5 hours", tested.exec(toList(5 * 60)));
        Assert.assertEquals("23 hours", tested.exec(toList(23 * 60)));
        Assert.assertEquals("25 hours", tested.exec(toList(25 * 60)));
    }

    @Test
    public void format_day_one() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("1 day", tested.exec(toList(60 * 24)));
    }

    @Test
    public void format_day_more() throws TemplateModelException {
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(messages, locale);
        Assert.assertEquals("2 days", tested.exec(toList(2 * 24 * 60)));
        Assert.assertEquals("5 days", tested.exec(toList(5 * 24 * 60)));
    }


}
