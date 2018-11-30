package com.sequenceiq.it.spark.ambari;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariGetHostComponentsReponse extends ITResponse {

    private Set<String> components;

    private String hostName;

    public AmbariGetHostComponentsReponse(Set<String> components) {
        this.components = components;
    }

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/plain");
        String url = request.url();
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("href", url + "?fields=HostRoles");

        ArrayNode items = rootNode.putArray("items");
        components.forEach(comp -> addComponentNode(items, url, "", comp));

        return rootNode;
    }

    private void addComponentNode(ArrayNode items, String url, String clusterName, String component) {
        ObjectNode item = items.addObject();
        item.put("href", url + "/" + component);
        ObjectNode hostRoleNode = item.putObject("HostRoles");
        hostRoleNode.put("cluster_name", clusterName);
        hostRoleNode.put("component_name", component);
        hostRoleNode.put("host_name", "host1");
        hostRoleNode.put("state", "INSTALLED");
        ObjectNode hostNode = item.putObject("host").put("href", url);
    }
}
