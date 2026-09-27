package com.appmodeler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads an external *-domain.json file and normalizes the small set of
 * structural properties required by App Modeler. It contains no industry
 * knowledge.
 */
public final class DomainInterpreter {
    private final ObjectMapper json;

    public DomainInterpreter(ObjectMapper json) {
        this.json = json;
    }

    public Map<String, Object> interpret(Path file) throws IOException {
        Map<String, Object> domain = json.readValue(file.toFile(), new TypeReference<>() {});
        normalizeEntities(domain);
        normalizeRelations(domain);
        return domain;
    }

    @SuppressWarnings("unchecked")
    private void normalizeEntities(Map<String, Object> domain) {
        Object rawEntities = domain.get("entities");
        if (rawEntities instanceof Map<?, ?>) {
            return;
        }

        if (rawEntities instanceof List<?> list) {
            Map<String, Object> entities = new LinkedHashMap<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> raw) {
                    Map<String, Object> entity = (Map<String, Object>) raw;
                    String id = first(entity, "id", "entityId", "name");
                    if (id != null) {
                        entity.putIfAbsent("id", id);
                        entities.put(id, entity);
                    }
                }
            }
            domain.put("entities", entities);
        }
    }

    @SuppressWarnings("unchecked")
    private void normalizeRelations(Map<String, Object> domain) {
        Object rawRelations = domain.get("relations");
        if (!(rawRelations instanceof List<?> list)) {
            return;
        }

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> relation = (Map<String, Object>) raw;
            String from = first(relation, "fromEntity", "sourceEntity", "from", "source");
            String to = first(relation, "toEntity", "targetEntity", "to", "target");
            if (from != null) {
                relation.put("fromEntity", stripField(from));
            }
            if (to != null) {
                relation.put("toEntity", stripField(to));
            }
        }
    }

    private String first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String stripField(String value) {
        int dot = value.indexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }
}
