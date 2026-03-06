package xyz.kip.gateway.model;

import xyz.kip.open.common.base.ToString;

import java.util.List;
import java.util.Map;

/**
 * 路由定义模型
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
public class RouteDefinitionVO extends ToString {

    private String id;
    private String uri;
    private List<PredicateDefinition> predicates;
    private List<FilterDefinition> filters;
    private Map<String, Object> metadata;
    private Integer order;

    public static class PredicateDefinition {
        private String name;
        private Map<String, String> args;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public void setArgs(Map<String, String> args) {
            this.args = args;
        }
    }

    public static class FilterDefinition {
        private String name;
        private Map<String, String> args;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public void setArgs(Map<String, String> args) {
            this.args = args;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<PredicateDefinition> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<PredicateDefinition> predicates) {
        this.predicates = predicates;
    }

    public List<FilterDefinition> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterDefinition> filters) {
        this.filters = filters;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
