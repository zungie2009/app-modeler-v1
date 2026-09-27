package com.appmodeler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Final UI assembler. External configuration is the source; runtime/index.html
 * is the materialized browser artifact written to disk.
 */
public final class HtmlAssembler {

    public void assemble(
            Path output,
            String css,
            LayoutInterpreter.LayoutBlocks layout,
            String javascript) throws IOException {

        String template = loadHostTemplate();
        String html = template
                .replace("/* APP_MODELER_STYLE */", css)
                .replace("<!-- APP_MODELER_LAYOUT -->", layout.html())
                .replace("/* APP_MODELER_LAYOUT_SCRIPT */", javascript);

        Files.createDirectories(output.getParent());
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
        Files.writeString(temporary, html, StandardCharsets.UTF_8);

        try {
            Files.move(
                    temporary,
                    output,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String loadHostTemplate() throws IOException {
        try (InputStream input = HtmlAssembler.class.getResourceAsStream("/static/index.template.html")) {
            if (input == null) {
                throw new FileNotFoundException("index.template.html");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
