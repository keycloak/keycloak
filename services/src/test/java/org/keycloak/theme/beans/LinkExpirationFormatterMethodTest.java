/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.theme.beans;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import freemarker.template.TemplateModelException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkExpirationFormatterMethodTest {

    protected static final Locale locale = Locale.ENGLISH;
    protected static final Properties messages = new Properties();
    static {
        messages.put("linkExpirationFormatter.timePeriodUnit.seconds", "{0,choice,0#seconds|1#second|1<seconds}");
        messages.put("linkExpirationFormatter.timePeriodUnit.minutes", "{0,choice,0#minutes|1#minute|2#minutes|3#minutes-3|3<minutes}");
        messages.put("linkExpirationFormatter.timePeriodUnit.hours", "{0,choice,0#hours|1#hour|1<hours}");
        messages.put("linkExpirationFormatter.timePeriodUnit.days", "{0,choice,0#days|1#day|1<days}");
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
        //noinspection UnnecessaryBoxing
        Assert.assertEquals("5 minutes", tested.exec(toList(Integer.valueOf(5))));
        //noinspection UnnecessaryBoxing
        Assert.assertEquals("5 minutes", tested.exec(toList(Long.valueOf(5))));
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

    /**
     * There are some languages where the choice format is not needed. Test that this still works.
     */
    @Test
    public void format_simple_no_choice() throws TemplateModelException {
        Properties simpleMessages = new Properties();
        simpleMessages.put("linkExpirationFormatter.timePeriodUnit.seconds", "seconds-simple");
        simpleMessages.put("linkExpirationFormatter.timePeriodUnit.minutes", "minutes-simple");
        simpleMessages.put("linkExpirationFormatter.timePeriodUnit.hours", "hours-simple");
        simpleMessages.put("linkExpirationFormatter.timePeriodUnit.days", "days-simple");
        LinkExpirationFormatterMethod tested = new LinkExpirationFormatterMethod(simpleMessages, locale);
        Assert.assertEquals("2 days-simple", tested.exec(toList(2 * 24 * 60)));
        Assert.assertEquals("5 days-simple", tested.exec(toList(5 * 24 * 60)));
    }

    /**
     * This ignored test conserves the code to translate the properties from Keycloak 22 and before to the new
     * {@link java.text.ChoiceFormat}.
     * The code appends the translated properties to the end of the file, and the user can then review the new properties and remove the old.
     */
    @Ignore
    @Test
    public void convert() throws IOException {
        String[] units = { "seconds", "minutes", "hours", "days"};
        for (Path path : Files.list(Paths.get("../themes/src/main/resources-community/theme/base/email/messages" )).collect(Collectors.toList())) {
            Properties p = new Properties();
            p.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
            FileWriter fw = new FileWriter(path.toFile(), true);
            boolean firstEntry = true;
            for (String unit : units) {
                StringBuilder choicePattern = new StringBuilder();
                String base = "linkExpirationFormatter.timePeriodUnit." + unit;
                String defaultValue = p.getProperty(base);
                if (defaultValue == null) {
                    continue;
                }
                choicePattern.append("{0,choice,0#").append(defaultValue).append("|");
                int last = 0;
                int entry = 0;
                String previous = defaultValue;
                for (int i = 0; i < 10; ++i) {
                    String value = p.getProperty(base + "." + i);
                    if (value != null) {
                        last = i;
                        if (!Objects.equals(value, previous)) {
                            entry = i;
                            previous = value;
                            choicePattern.append(i).append("#").append(value).append("|");
                        }
                    }
                }
                choicePattern.append(last).append("<").append(defaultValue).append("}");
                if (entry == 0) {
                    choicePattern.setLength(0);
                    choicePattern.append(defaultValue);
                }
                choicePattern.insert(0, base + "=");
                choicePattern.append("\n");
                if (firstEntry) {
                    fw.write("\n");
                    firstEntry = false;
                }
                fw.write(choicePattern.toString());
            }
            fw.close();
        }
    }


}
