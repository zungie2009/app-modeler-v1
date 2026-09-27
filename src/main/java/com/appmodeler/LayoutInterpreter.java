package com.appmodeler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Interprets external layout folders. A layout owns structure and behavior only:
 * layout.html + layout.js. Visual styling is handled by StyleInterpreter.
 */
public final class LayoutInterpreter {
    private static final String DEFAULT_LAYOUT = "default";

    private final Path layoutsRoot;

    public LayoutInterpreter(Path layoutsRoot) {
        this.layoutsRoot = layoutsRoot;
    }

    public List<String> layouts() throws IOException {
        if (!Files.isDirectory(layoutsRoot)) {
            return List.of(DEFAULT_LAYOUT);
        }

        try (var stream = Files.list(layoutsRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(this::isCompleteLayout)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    public LayoutBlocks interpret(String requestedLayout) throws IOException {
        Path layout = resolveLayout(requestedLayout);
        String html = Files.readString(layout.resolve("layout.html"), StandardCharsets.UTF_8);
        return new LayoutBlocks(html, "");
    }

    private Path resolveLayout(String requestedLayout) throws IOException {
        String safeName = requestedLayout == null ? "" : requestedLayout.trim();
        Path candidate = layoutsRoot.resolve(safeName).normalize();

        if (!safeName.isEmpty()
                && candidate.startsWith(layoutsRoot.normalize())
                && isCompleteLayout(candidate)) {
            return candidate;
        }

        Path fallback = layoutsRoot.resolve(DEFAULT_LAYOUT);
        if (!isCompleteLayout(fallback)) {
            throw new FileNotFoundException("Missing default layout: " + fallback.toAbsolutePath());
        }
        return fallback;
    }

    private boolean isCompleteLayout(Path directory) {
        return Files.isRegularFile(directory.resolve("layout.html"))
                && Files.isRegularFile(directory.resolve("layout.js"));
    }

    public record LayoutBlocks(String html, String script) {
    }
}
