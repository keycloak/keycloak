package org.keycloak.testframework.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

public final class Logs {

    private final List<LogEntry> entries;
    private final int sharedEndOffset;
    private final int classStartOffset;
    private int startupEndOffset = -1;

    Logs() {
        this.entries = new CopyOnWriteArrayList<>();
        this.sharedEndOffset = 0;
        this.classStartOffset = 0;
    }

    private Logs(List<LogEntry> entries, int sharedEndOffset, int classStartOffset) {
        this.entries = entries;
        this.sharedEndOffset = sharedEndOffset;
        this.classStartOffset = classStartOffset;
    }

    void markStartupComplete() {
        startupEndOffset = entries.size();
    }

    Logs createClassView() {
        return new Logs(entries, startupEndOffset, entries.size());
    }

    public List<LogEntry> getEntries() {
        return Collections.unmodifiableList(visibleEntries());
    }

    void add(LogEntry entry) {
        entries.add(entry);
    }

    public void assertContains(String message) {
        List<LogEntry> visible = visibleEntries();
        if (visible.stream().noneMatch(e -> e.message().contains(message))) {
            throw new AssertionError("Expected log output to contain: %s%n%s".formatted(message, formatEntries(visible)));
        }
    }

    public void assertNotContains(String message) {
        visibleEntries().stream()
                .filter(e -> e.message().contains(message))
                .findFirst()
                .ifPresent(e -> {
                    throw new AssertionError("Expected log output to NOT contain: %s%nFound: %s".formatted(message, e.rawLine()));
                });
    }

    public void assertContains(Logger.Level level, String message) {
        List<LogEntry> visible = visibleEntries();
        if (visible.stream().noneMatch(e -> level.equals(e.level()) && e.message().contains(message))) {
            throw new AssertionError("Expected log output to contain message at level %s: %s%n%s".formatted(level, message, formatEntries(visible)));
        }
    }

    public void assertContains(Logger.Level level, String category, String message) {
        List<LogEntry> visible = visibleEntries();
        if (visible.stream().noneMatch(e ->
                level.equals(e.level())
                        && e.category() != null && e.category().startsWith(category)
                        && e.message().contains(message))) {
            throw new AssertionError("Expected log output to contain message at level %s with category %s: %s%n%s".formatted(level, category, message, formatEntries(visible)));
        }
    }

    public void assertNotContains(Logger.Level level, String message) {
        visibleEntries().stream()
                .filter(e -> level.equals(e.level()) && e.message().contains(message))
                .findFirst()
                .ifPresent(e -> {
                    throw new AssertionError("Expected log output to NOT contain message at level %s: %s%nFound: %s".formatted(level, message, e.rawLine()));
                });
    }

    public void assertCount(String message, int expectedCount) {
        long actualCount = visibleEntries().stream().filter(e -> e.message().contains(message)).count();
        if (actualCount != expectedCount) {
            throw new AssertionError("Expected log output to contain '%s' exactly %d time(s), but found %d".formatted(message, expectedCount, actualCount));
        }
    }

    public void assertStdErrContains(String message) {
        List<LogEntry> visible = visibleEntries();
        if (visible.stream().noneMatch(e -> e.stderr() && e.message().contains(message))) {
            throw new AssertionError("Expected stderr to contain: %s%n%s".formatted(message, formatStdErr(visible)));
        }
    }

    public void assertStdErrNotContains(String message) {
        visibleEntries().stream()
                .filter(e -> e.stderr() && e.message().contains(message))
                .findFirst()
                .ifPresent(e -> {
                    throw new AssertionError("Expected stderr to NOT contain: %s%nFound: %s".formatted(message, e.rawLine()));
                });
    }

    public void clear() {
        entries.clear();
    }

    public String getOutput() {
        return visibleEntries().stream().map(LogEntry::rawLine).collect(Collectors.joining(System.lineSeparator()));
    }

    public String getStdErr() {
        return visibleEntries().stream().filter(LogEntry::stderr).map(LogEntry::rawLine).collect(Collectors.joining(System.lineSeparator()));
    }

    private List<LogEntry> visibleEntries() {
        int size = entries.size();
        if (sharedEndOffset == 0 && classStartOffset == 0) {
            return entries;
        }
        List<LogEntry> result = new ArrayList<>();
        if (sharedEndOffset > 0) {
            result.addAll(entries.subList(0, Math.min(sharedEndOffset, size)));
        }
        if (classStartOffset < size) {
            result.addAll(entries.subList(classStartOffset, size));
        }
        return result;
    }

    private static String formatEntries(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return "(no log entries)";
        }
        return entries.stream().map(LogEntry::rawLine).collect(Collectors.joining(System.lineSeparator()));
    }

    private static String formatStdErr(List<LogEntry> entries) {
        List<LogEntry> stderrEntries = entries.stream().filter(LogEntry::stderr).toList();
        if (stderrEntries.isEmpty()) {
            return "(no stderr entries)";
        }
        return stderrEntries.stream().map(LogEntry::rawLine).collect(Collectors.joining(System.lineSeparator()));
    }
}
