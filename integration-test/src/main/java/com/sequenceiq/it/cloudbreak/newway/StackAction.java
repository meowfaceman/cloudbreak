package com.sequenceiq.it.cloudbreak.newway;


import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackAction {
    private static final String VPC_ID_KEY = "vpcId";

    private static final String SUBNET_ID_KEY = "subnetId";

    private static final String NETWORK_ID_KEY = "networkId";

    private static final String RESOURCE_GROUP_NAME_KEY = "resourceGroupName";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackAction.class);

    private StackAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get stack " + stackEntity.getName());
        stackEntity.setResponse(
                client.getCloudbreakClient().stackV4Endpoint()
                        .get(integrationTestContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class), stackEntity.getName(),
                                Collections.emptySet()));
        Log.logJSON(" stack get response: ", stackEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get all stack");
        stackEntity.setResponses(new HashSet<>(stackEntity.getAll(client)));
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        delete(integrationTestContext, (StackEntity) entity, Boolean.FALSE);
    }

    public static void deleteWithForce(IntegrationTestContext integrationTestContext, Entity entity) {
        delete(integrationTestContext, (StackEntity) entity, Boolean.TRUE);
    }

    private static void delete(IntegrationTestContext integrationTestContext, StackEntity entity, Boolean forced) {
        StackEntity stackEntity = entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" delete: " + stackEntity.getName());
        client.getCloudbreakClient().stackV4Endpoint()
                .delete(stackEntity.getResponse().getId(), stackEntity.getName(), forced, false);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
            // TODO: Exception class is too wide. A narrower exception should be caught (e.g. NotFound or something like that.)
        } catch (Exception e) {
            LOGGER.warn("Failed to get stack. Trying to create it.", e);
            new StackPostStrategy().doAction(integrationTestContext, entity);
        }
    }

    public static void determineNetworkAwsFromDatalakeStack(IntegrationTestContext integrationTestContext, Entity entity) {
        var stackEntity = (StackEntity) entity;
        var datalakeStack = DatalakeCluster.getTestContextDatalakeCluster().apply(integrationTestContext);
        if (isDatalakeExistAndHasNetwork(datalakeStack)) {
            prepareNetworkParam(stackEntity);
            stackEntity.getRequest().getNetwork().getAws().setSubnetId(datalakeStack.getResponse().getNetwork().getAws().getSubnetId());
            stackEntity.getRequest().getNetwork().getAws().setVpcId(datalakeStack.getResponse().getNetwork().getAws().getVpcId());
        } else {
            throw new AssertionError("Datalake cluster does not cointain network or datalake cluster does not exist");
        }
    }

    public static void determineNetworkAzureFromDatalakeStack(IntegrationTestContext integrationTestContext, Entity entity) {
        var stackEntity = (StackEntity) entity;
        var datalakeStack = DatalakeCluster.getTestContextDatalakeCluster().apply(integrationTestContext);
        if (isDatalakeExistAndHasNetwork(datalakeStack)) {
            prepareNetworkParam(stackEntity);
            stackEntity.getRequest().getNetwork().getAzure().setSubnetId(datalakeStack.getResponse().getNetwork().getAzure().getSubnetId());
            stackEntity.getRequest().getNetwork().getAzure().setNetworkId(datalakeStack.getResponse().getNetwork().getAzure().getNetworkId());
            stackEntity.getRequest().getNetwork().getAzure().setResourceGroupName(datalakeStack.getResponse().getNetwork().getAzure().getResourceGroupName());
        } else {
            throw new AssertionError("Datalake cluster does not cointain network or datalake cluster does not exist");
        }
    }

    private static void prepareNetworkParam(StackEntity stackEntity) {
        if (stackEntity.getRequest().getNetwork() == null) {
            var network = new NetworkV4Request();
            stackEntity.getRequest().setNetwork(network);
        }
    }

    private static boolean isDatalakeExistAndHasNetwork(DatalakeCluster datalakeStack) {
        return datalakeStack != null && datalakeStack.getResponse() != null && datalakeStack.getResponse().getNetwork() != null;
    }
}
