package org.keycloak.protocol.ssf.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.keycloak.protocol.ssf.event.subjects.SubjectId;

public class RemoveSubjectRequest {
        /**
         * REQUIRED. A string identifying the stream to which the subject is being added.
         */
        @JsonProperty("stream_id")
        private String streamId;

        /**
         * REQUIRED. A Subject claim identifying the subject to be added.
         */
        @JsonProperty("subject")
        private SubjectId subject;

        public String getStreamId() {
            return streamId;
        }

        public void setStreamId(String streamId) {
            this.streamId = streamId;
        }

        public SubjectId getSubject() {
            return subject;
        }

        public void setSubject(SubjectId subject) {
            this.subject = subject;
        }
    }
