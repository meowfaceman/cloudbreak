package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.math.BigDecimal;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerCommandPollerObject extends ClouderaManagerPollerObject {

    private BigDecimal id;

    public ClouderaManagerCommandPollerObject(Stack stack, ApiClient apiClient, BigDecimal id) {
        super(stack, apiClient);
        this.id = id;
    }

    public BigDecimal getId() {
        return id;
    }
}
