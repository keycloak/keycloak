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
package org.keycloak.models.map.storage.file.yaml;

import org.keycloak.models.map.storage.file.common.YamlContext;
import org.keycloak.models.map.storage.file.common.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.common.YamlContext.DefaultMapContext;
import org.keycloak.models.map.storage.file.common.YamlContext.DefaultObjectContext;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jboss.logging.Logger;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.YamlUnicodeReader;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.Event.ID;
import org.snakeyaml.engine.v2.events.NodeEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.exceptions.ConstructorException;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.parser.Parser;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.resolver.JsonScalarResolver;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.scanner.StreamReader;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 *
 * @author hmlnarik
 */
public class YamlContextAwareParser<E> {

    private static final Logger LOG = Logger.getLogger(YamlContextAwareParser.class);
    public static final String ARRAY_CONTEXT = "$@[]@$";

    private static final ScalarResolver RESOLVER = new JsonScalarResolver();
    private final Parser parser;
    private final YamlContextStack contextStack;

    // Leverage SnakeYaml's translation of primitive values
    private static final class MiniConstructor extends StandardConstructor {

        public MiniConstructor() {
            super(SETTINGS);
        }

        // This has been based on SnakeYaml's own org.snakeyaml.engine.v2.constructor.BaseConstructor.constructObjectNoCheck(Node node)
        @SuppressWarnings(value = "unchecked")
        public Object constructStandardJavaInstance(ScalarNode node) {
            return findConstructorFor(node)
                .map(constructor -> constructor.construct(node))
                .orElseThrow(() -> new ConstructorException(null, Optional.empty(), "could not determine a constructor for the tag " + node.getTag(), node.getStartMark()));
        }

        public static final MiniConstructor INSTANCE = new MiniConstructor();
    }

    private static final LoadSettings SETTINGS = LoadSettings.builder()
      .setAllowRecursiveKeys(false)
      .setParseComments(false)
      .build();

    public static <E> E parse(Path path, YamlContext<E> initialContext) {
        LOG.tracef("parse(%s,%s)%s", path, initialContext, getShortStackTrace());

        Objects.requireNonNull(path, "Path invalid");
        try (InputStream is = Files.newInputStream(path)) {
            if (Files.size(path) == 0) {
                return null;
            }
            Parser p = new ParserImpl(SETTINGS, new StreamReader(SETTINGS, new YamlUnicodeReader(is)));
            return new YamlContextAwareParser<>(p, initialContext).parse();
        } catch (IOException ex) {
            LOG.warn(ex);
            return null;
        }
    }

    protected YamlContextAwareParser(Parser p, YamlContext<E> initialContext) {
        this.parser = p;
        this.contextStack = new YamlContextStack(initialContext);
    }

    @SuppressWarnings("unchecked")
    protected <E> E parse() {
        consumeEvent(Event.ID.StreamStart, "Expected a stream");

        if (!parser.checkEvent(Event.ID.StreamEnd)) {
            consumeEvent(Event.ID.DocumentStart, "Expected a document in the stream");
            parseNode();
            consumeEvent(Event.ID.DocumentEnd, "Expected a single document in the stream");
        }

        consumeEvent(Event.ID.StreamEnd, "Expected a single document in the stream");

        return (E) contextStack.pop().getResult();
    }

    protected Object parseNode() {
        if (parser.checkEvent(Event.ID.Alias)) {
            throw new IllegalStateException("Aliases are not handled at this moment");
        }
        Event ev = parser.next();
//        System.out.println("  Parsing " + ev);
        if (!(ev instanceof NodeEvent)) {
            throw new IllegalArgumentException("Invalid event " + ev);
        }
//        if (anchor != null) {
//            node.setAnchor(anchor);
//            anchors.put(anchor, node);
//        }
//        try {
        switch (ev.getEventId()) {
            case Scalar:
                ScalarEvent se = (ScalarEvent) ev;
                boolean implicit = se.getImplicit().canOmitTagInPlainScalar();
                Tag nodeTag = constructTag(se.getTag(), se.getValue(), implicit);
                return parseScalar(nodeTag, se);
            case SequenceStart:
                return parseSequence();
            case MappingStart:
                return parseMapping();
            default:
                throw new IllegalStateException("Event not expected " + ev);
        }
//        } finally {
//            anchors.remove(anchor);
//        }
    }

    /**
     * Parses a sequence node inside the current context. Each sequence item is parsed in the context
     * supplied by the current
     * @return
     */
    protected Object parseSequence() {
        LOG.tracef("Parsing sequence");
        YamlContext context = contextStack.peek();
        while (! parser.checkEvent(Event.ID.SequenceEnd)) {
            context.add(parseNodeInFreshContext(ARRAY_CONTEXT));
        }
        consumeEvent(Event.ID.SequenceEnd, "Expected end of sequence");
        return context.getResult();
    }

    /**
     * Parses a mapping node inside the current context. Each mapping value is parsed in the context
     * supplied by the current context for the mapping key.
     * @return
     */
    protected Object parseMapping() {
        LOG.tracef("Parsing mapping");
        YamlContext context = contextStack.peek();
        while (! parser.checkEvent(Event.ID.MappingEnd)) {
            Object key = parseNodeInFreshContext();
            LOG.tracef("Parsed mapping key: %s", key);
            if (! (key instanceof String)) {
                throw new IllegalStateException("Invalid key in map: " + key);
            }
            Object value = parseNodeInFreshContext((String) key);
            LOG.tracef("Parsed mapping value: %s", value);
            context.add((String) key, value);
        }
        consumeEvent(Event.ID.MappingEnd, "Expected end of mapping");
        return context.getResult();
    }

    /**
     * Parses a scalar node inside the current context.
     * @return
     */
    protected Object parseScalar(Tag nodeTag, ScalarEvent se) {
        YamlContext context = contextStack.peek();
//        System.out.println("value: " + se.getValue() + ", context: " + context + ", type: " + nodeTag.getClassName());
        ScalarNode node = new ScalarNode(nodeTag, true, se.getValue(), se.getScalarStyle(), se.getStartMark(), se.getEndMark());
        final Object value = MiniConstructor.INSTANCE.constructStandardJavaInstance(node);
        context.add(value);
        return context.getResult();
    }

    private static final EnumMap<Event.ID, Supplier<YamlContext<?>>> CONTEXT_CONSTRUCTORS = new EnumMap<>(Event.ID.class);
    static {
        CONTEXT_CONSTRUCTORS.put(ID.Scalar, DefaultObjectContext::new);
        CONTEXT_CONSTRUCTORS.put(ID.SequenceStart, DefaultListContext::newDefaultListContext);
        CONTEXT_CONSTRUCTORS.put(ID.MappingStart, DefaultMapContext::new);
    }

    /**
     * Ensure that the next event is the expectedEventId, otherwise throw an exception, and consume that event
     */
    private Event consumeEvent(ID expectedEventId, String message) throws IllegalArgumentException {
        if (! parser.checkEvent(expectedEventId)) {
            Event event = parser.next();
            throw new IllegalArgumentException(message + " at " + event.getStartMark());
        }
        return parser.next();
    }

    private static Tag constructTag(Optional<String> tag, String value, boolean implicit) {
        // based on org.snakeyaml.engine.v2.composer.Composer.composeScalarNode(Optional<Anchor> anchor, List<CommentLine> blockComments)
        return tag.filter(t -> ! "!".equals(t))
          .map(Tag::new)
          .orElseGet(() -> RESOLVER.resolve(value, implicit));
    }

    /**
     * Parses the node in a context created for the given {@code key}.
     * @param key
     * @return
     * @throws IllegalStateException
     */
    private Object parseNodeInFreshContext(String key) throws IllegalStateException {
        Supplier<YamlContext<?>> cc = CONTEXT_CONSTRUCTORS.get(parser.peekEvent().getEventId());
        if (cc == null) {
            throw new IllegalStateException("Invalid value in map with key " + key);
        }
        contextStack.push((String) key, cc);
        Object value = parseNode();
        contextStack.pop();
        return value;
    }

    /**
     * Parses the node in a fresh context {@link DefaultObjectContext}.
     * @return
     * @throws IllegalStateException
     */
    private Object parseNodeInFreshContext() throws IllegalStateException {
        contextStack.push(new DefaultObjectContext());
        Object value = parseNode();
        contextStack.pop();
        return value;
    }

}
