package com.appmodeler.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves optional UI-library dependencies declared by a layout.
 * Java understands only the generic library contract, not Bootstrap itself.
 */
public final class UiLibraryInterpreter {

    private final Path librariesRoot;
    private final ObjectMapper json;

    public UiLibraryInterpreter(Path librariesRoot, ObjectMapper json) {
        this.librariesRoot = librariesRoot;
        this.json = json;
    }

    public LibraryAssets interpret(Path selectedLayout) throws IOException {
        Path descriptor = selectedLayout.resolve("layout.json");
        if (!Files.isRegularFile(descriptor)) {
            return LibraryAssets.empty();
        }

        Map<String, Object> layout = json.readValue(
                descriptor.toFile(), new TypeReference<>() {});

        Object requiresObject = layout.get("requires");
        if (!(requiresObject instanceof List<?> requires)) {
            return LibraryAssets.empty();
        }

        StringBuilder css = new StringBuilder();
        StringBuilder javascript = new StringBuilder();
        List<String> resolved = new ArrayList<>();

        for (Object value : requires) {
            String id = String.valueOf(value).trim();
            if (!id.isEmpty()) {
                appendLibrary(id, css, javascript);
                resolved.add(id);
            }
        }

        return new LibraryAssets(
                css.toString(),
                javascript.toString(),
                List.copyOf(resolved));
    }

    private void appendLibrary(
            String id,
            StringBuilder css,
            StringBuilder javascript) throws IOException {

        Path directory = librariesRoot.resolve(id).normalize();
        Path normalizedRoot = librariesRoot.normalize();

        if (!directory.startsWith(normalizedRoot)
                || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Unknown UI library: " + id);
        }

        Path descriptor = directory.resolve("library.json");
        if (!Files.isRegularFile(descriptor)) {
            throw new IllegalArgumentException(
                    "Missing library.json for UI library: " + id);
        }

        Map<String, Object> library = json.readValue(
                descriptor.toFile(), new TypeReference<>() {});

        appendAssets(directory, library.get("css"), css, "CSS");
        appendAssets(directory, library.get("js"), javascript, "JavaScript");
    }

    private static void appendAssets(
            Path directory,
            Object assetsObject,
            StringBuilder target,
            String type) throws IOException {

        if (!(assetsObject instanceof List<?> assets)) {
            return;
        }

        for (Object value : assets) {
            String fileName = String.valueOf(value);
            Path asset = directory.resolve(fileName).normalize();

            if (!asset.startsWith(directory.normalize())
                    || !Files.isRegularFile(asset)) {
                throw new IllegalArgumentException(
                        "Missing " + type + " UI-library asset: " + asset);
            }

            target.append("\n/* UI library asset: ")
                    .append(fileName)
                    .append(" */\n")
                    .append(Files.readString(asset, StandardCharsets.UTF_8))
                    .append('\n');
        }
    }

    public record LibraryAssets(
            String css,
            String javascript,
            List<String> resolvedLibraries) {

        public static LibraryAssets empty() {
            return new LibraryAssets("", "", List.of());
        }
    }
}
