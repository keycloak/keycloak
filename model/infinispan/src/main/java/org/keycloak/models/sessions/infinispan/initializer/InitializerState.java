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

package org.keycloak.models.sessions.infinispan.initializer;

import org.jboss.logging.Logger;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * Note that this state is <b>NOT</b> thread safe. Currently it is only used from single thread so it's fine
 * but further optimizations might need to revisit this (see {@link InfinispanCacheInitializer}).
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(InitializerState.ExternalizerImpl.class)
public class InitializerState extends SessionEntity {

    private static final Logger log = Logger.getLogger(InitializerState.class);

    private final int sessionsCount;
    private final int segmentsCount;
    private final BitSet segments;
    private int lowestUnfinishedSegment = 0;

    public InitializerState(int sessionsCount, int sessionsPerSegment) {
        this.sessionsCount = sessionsCount;

        int segmentsCountLocal = sessionsCount / sessionsPerSegment;
        if (sessionsPerSegment * segmentsCountLocal < sessionsCount) {
            segmentsCountLocal = segmentsCountLocal + 1;
        }
        this.segmentsCount = segmentsCountLocal;
        this.segments = new BitSet(segmentsCountLocal);

        log.debugf("sessionsCount: %d, sessionsPerSegment: %d, segmentsCount: %d", sessionsCount, sessionsPerSegment, segmentsCountLocal);

        updateLowestUnfinishedSegment();
    }

    private InitializerState(String realmId, int sessionsCount, int segmentsCount, BitSet segments) {
        super(realmId);
        this.sessionsCount = sessionsCount;
        this.segmentsCount = segmentsCount;
        this.segments = segments;

        log.debugf("sessionsCount: %d, segmentsCount: %d", sessionsCount, segmentsCount);

        updateLowestUnfinishedSegment();
    }

    /** Return true just if computation is entirely finished (all segments are true) */
    public boolean isFinished() {
        return segments.cardinality() == segmentsCount;
    }

    /** Return next un-finished segments. It returns at most {@code maxSegmentCount} segments. */
    public List<Integer> getUnfinishedSegments(int maxSegmentCount) {
        List<Integer> result = new LinkedList<>();
        int next = lowestUnfinishedSegment;
        boolean remaining = lowestUnfinishedSegment != -1;

        while (remaining && result.size() < maxSegmentCount) {
            next = getNextUnfinishedSegmentFromIndex(next);
            if (next == -1) {
                remaining = false;
            } else {
                result.add(next);
                next++;
            }
        }

        return result;
    }

    public void markSegmentFinished(int index) {
        segments.set(index);
        updateLowestUnfinishedSegment();
    }

    private void updateLowestUnfinishedSegment() {
        this.lowestUnfinishedSegment = getNextUnfinishedSegmentFromIndex(lowestUnfinishedSegment);
    }

    private int getNextUnfinishedSegmentFromIndex(int index) {
        final int nextFreeSegment = this.segments.nextClearBit(index);
        return (nextFreeSegment < this.segmentsCount)
          ? nextFreeSegment
          : -1;
    }

    @Override
    public String toString() {
        int finished = segments.cardinality();
        int nonFinished = this.segmentsCount;

        return "sessionsCount: "
          + sessionsCount
          + (", finished segments count: " + finished)
          + (", non-finished segments count: " + nonFinished);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.sessionsCount;
        hash = 97 * hash + this.segmentsCount;
        hash = 97 * hash + Objects.hashCode(this.segments);
        hash = 97 * hash + this.lowestUnfinishedSegment;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InitializerState other = (InitializerState) obj;
        if (this.sessionsCount != other.sessionsCount) {
            return false;
        }
        if (this.segmentsCount != other.segmentsCount) {
            return false;
        }
        if (this.lowestUnfinishedSegment != other.lowestUnfinishedSegment) {
            return false;
        }
        if ( ! Objects.equals(this.segments, other.segments)) {
            return false;
        }
        return true;
    }

    public static class ExternalizerImpl implements Externalizer<InitializerState> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, InitializerState value) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(value.getRealmId(), output);
            output.writeInt(value.sessionsCount);
            output.writeInt(value.segmentsCount);
            MarshallUtil.marshallByteArray(value.segments.toByteArray(), output);
        }

        @Override
        public InitializerState readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public InitializerState readObjectVersion1(ObjectInput input) throws IOException {
            return new InitializerState(
              MarshallUtil.unmarshallString(input),
              input.readInt(),
              input.readInt(),
              BitSet.valueOf(MarshallUtil.unmarshallByteArray(input))
            );
        }

    }
}
