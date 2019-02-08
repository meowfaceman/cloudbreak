package com.sequenceiq.it.cloudbreak.newway.testcase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackCreationTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationTest.class);

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testCreateNewRegularCluster(TestContext testContext) {
        testContext.given(StackEntity.class)
                .when(Stack.postV2())
                .awaitEvent(START_PROVISIONING_STATE)
                .awaitEvent(STACK_CREATION_FINISHED_STATE)
                .awaitEvent(INIT_STATE)
                .awaitEvent(STARTING_AMBARI_SERVICES_STATE)
                .awaitEvent(CLUSTER_CREATION_FINISHED_STATE)
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
