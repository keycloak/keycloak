/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Allows time manipulation using the java.time API. It makes the testing API more flexible as well
 * as allowing you to adjust the clock at runtime. The functionality is similar to the
 * <b>org.keycloak.common.util.Time</b> class. The <b><i>keycloak-commons</i></b> project uses java
 * 1.7 and for this reason it is not possible to add this behavior in the Time class.
 *
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClockUtil {

  public static final String CLOCK_ADVANCE_HOURS = "keycloak.clock.advance.hours";

  private static Clock clock = Clock.systemDefaultZone();

  public static Clock getClock() {
    return clock;
  }

  public static Clock resetClock() {
    clock = Clock.systemDefaultZone();
    return clock;
  }


  public static Clock atInstant(Instant instant) {
    clock = Clock.fixed(instant, ZoneId.systemDefault());
    return clock;
  }

  public static Clock withZoneId(ZoneId zoneId) {
    clock = clock.withZone(zoneId);
    return clock;
  }

  public static Clock plusSeconds(long seconds) {
    clock = Clock.offset(clock, Duration.ofSeconds(seconds));
    return clock;
  }

  public static Clock minusSeconds(long seconds) {
    plusSeconds(Math.negateExact(seconds));
    return clock;
  }

  public static Clock plusMinutes(int minutes) {
    clock = Clock.offset(clock, Duration.ofMinutes(minutes));
    return clock;
  }

  public static Clock minusMinutes(int minutes) {
    plusMinutes(Math.negateExact(minutes));
    return clock;
  }

  public static Clock plusHours(int hours) {
    clock = Clock.offset(clock, Duration.ofHours(hours));
    return clock;
  }

  public static Clock minusHours(int hours) {
    plusHours(Math.negateExact(hours));
    return clock;
  }

  public static int currentTimeInSeconds() {
    Long epochSecond = LocalDateTime.now(clock).toEpochSecond(ZonedDateTime.now(clock).getOffset());
    return epochSecond.intValue();
  }

  public static LocalDateTime currentTime() {
    return LocalDateTime.now(clock);
  }

  public static LocalDateTime fromEpochSeconds(long seconds) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), clock.getZone());
  }
}
