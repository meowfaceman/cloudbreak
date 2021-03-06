package com.sequenceiq.it.cloudbreak.newway.action.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackPostAction implements Action<StackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPostAction.class);

    @Override
    public StackEntity action(TestContext testContext, StackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, " Name: " + entity.getRequest().getName());
        logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Stack created was successfully:\n", entity.getResponse());
        log(LOGGER, " ID: " + entity.getResponse().getId());

        return entity;
    }
}
