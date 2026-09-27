package com.appmodeler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts domain navigation metadata into one canonical menu tree.
 * If no explicit navigation exists, parentId is used. As a final fallback,
 * entities are exposed as top-level menu items. Foreign keys never define
 * navigation.
 */
public final class MenuInterpreter {

    public List<Map<String, Object>> interpret(Map<String, Object> domain) {
        Object explicit = domain.get("navigation");
        if (explicit instanceof List<?> list && !list.isEmpty()) {
            return copyMenu(list);
        }

        Map<String, Map<String, Object>> entities = entities(domain);
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : entities.entrySet()) {
            String id = entry.getKey();
            Map<String, Object> entity = entry.getValue();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", id);
            node.put("label", displayName(id, entity));
            node.put("target", id);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(id, node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : entities.entrySet()) {
            String id = entry.getKey();
            String parentId = string(entry.getValue().get("parentId"));
            Map<String, Object> node = nodes.get(id);
            if (parentId != null && nodes.containsKey(parentId)) {
                children(nodes.get(parentId)).add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> entities(Map<String, Object> domain) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Object raw = domain.get("entities");
        if (raw instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (value instanceof Map<?, ?> entity) {
                    result.put(String.valueOf(key), (Map<String, Object>) entity);
                }
            });
        }
        return result;
    }

    private String displayName(String id, Map<String, Object> entity) {
        Object display = entity.get("displayName");
        if (display == null) {
            display = entity.get("label");
        }
        if (display == null) {
            display = entity.get("name");
        }
        return display == null ? id : String.valueOf(display);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> children(Map<String, Object> node) {
        return (List<Map<String, Object>>) node.get("children");
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> copyMenu(List<?> source) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Map<?, ?> raw) {
                result.add(new LinkedHashMap<>((Map<String, Object>) raw));
            }
        }
        return result;
    }
}
