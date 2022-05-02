package org.keycloak.quarkus.runtime.cli;

import picocli.CommandLine;

import java.util.function.Function;

public class PicocliConverterAdapter {

    public static class ConverterAdapter<T> implements CommandLine.ITypeConverter<T> {
        private final Function<String, T> conversionFn;

        public ConverterAdapter(Function<String, T> fn) {
            this.conversionFn = fn;
        }

        @Override
        public T convert(String s) throws Exception {
            try {
                return this.conversionFn.apply(s);
            } catch (Exception e) {
                throw new CommandLine.TypeConversionException(e.getMessage());
            }
        }
    }

    public static <T> CommandLine.ITypeConverter<T> build(Function<String, T> fn) {
        return new ConverterAdapter<T>(fn);
    }
}
