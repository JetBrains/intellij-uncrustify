package org.jetbrains.uncrustify;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

public class UncrustifyConfigWriter extends Writer {
    public static final int DEFAULT_OPTION_ASSIGN_ALIGN = 32;

    private final Writer writer;
    private final StringBuilder builder;

    public UncrustifyConfigWriter(@NotNull Writer out) {
        super(out);
        this.writer = out;
        this.builder = new StringBuilder();
    }

    public static String lineSeparator() {
        return System.lineSeparator();
    }

    public void newLine() throws IOException {
        writer.write(lineSeparator());
    }

    public void write_option(@NotNull String name, @NotNull String value) throws IOException {
        writer.write(name);
        for (int i = 0; i < DEFAULT_OPTION_ASSIGN_ALIGN - name.length(); i++) {
            writer.write(' ');
        }
        writer.write("= ");
        writer.write(value);
        newLine();
        newLine();
    }

    public void write_comment(@NotNull String text) throws IOException {
        builder.setLength(0);
        text.lines().forEach(l -> {
            builder.append("# ");
            builder.append(l);
            builder.append(lineSeparator());
        });
        writer.write(builder.toString());
    }

    @Override
    public void write(char @NotNull [] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
