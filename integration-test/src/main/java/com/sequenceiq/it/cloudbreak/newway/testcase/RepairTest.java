package com.sequenceiq.it.cloudbreak.newway.testcase;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterRepairAction;
import com.sequenceiq.it.cloudbreak.newway.action.database.DatabaseCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.kerberos.KerberosTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.spark.DynamicRouteStack;
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestResponse;
import com.sequenceiq.it.spark.ambari.AmbariGetHostComponentsReponse;
import com.sequenceiq.it.spark.ambari.AmbariGetServiceComponentInfoResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;

public class RepairTest extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepairTest.class);
    private static final int HTTP_CREATED = 201;

    private static final String AMBARI_HOST_COMPONENTS = "/api/v1/clusters/:cluster/hosts/:host/host_components";

    private static final String AMBARI_CLUSTER_COMPONENTS = "api/v1/clusters/:cluster/components/:component";

    private static final String AMBARI_KERBEROS_CREDENTIAL = "/api/v1/clusters/:cluster/credentials/kdc.admin.credential";

    private static final String AMBARI_REGENERATE_KEYTABS = "/api/v1/clusters/:cluster?regenerate_keytabs=all";

    private static final String AMBARI_CLUSTER_REQUESTS = "/api/v1/clusters/:cluster/requests";

    private final Set<String> components = Set.of("component-new-liga", "component-liga-client", "component-yellow-submarine");

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testRepairMasterNodeNoKerberos(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForMock();
        String ambariRdsName = getNameGenerator().getRandomNameForMock();
        createEnvWithResources(testContext, ambariRdsName);
        testContext
                .given(StackEntity.class)
                .withName(clusterName)
                .withCluster(getCluster(testContext, null, ambariRdsName))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE);
        StackEntity stackEntity = testContext.get(StackEntity.class);
        InstanceMetaDataV4Response instanceMetaData = stackEntity.getInstanceMetaData(HostGroupType.MASTER.getName()).stream().findFirst().get();
        String hostName = instanceMetaData.getDiscoveryFQDN();
        String publicIp = instanceMetaData.getPublicIp();
        addAmbariMocks(testContext, clusterName, hostName, publicIp);

        testContext
                .given(StackEntity.class)
                .when(ClusterRepairAction.valid())
                .await(StackEntity.class, STACK_AVAILABLE);

        AmbariPathResolver ambariPathResolver = new AmbariPathResolver(clusterName, hostName);
        assertGetClusterComponentCalls(stackEntity, AMBARI_CLUSTER_COMPONENTS, components, ambariPathResolver);
        assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "INSTALLED", 2, ambariPathResolver);
        assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "INIT", 1, ambariPathResolver);
        assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "STARTED", 1, ambariPathResolver);
        testContext.given(StackEntity.class)
                .then(MockVerification.verify(HttpMethod.GET, ambariPathResolver.resolve(AMBARI_HOST_COMPONENTS)).exactTimes(2))
                .then(MockVerification.verify(HttpMethod.PUT, ambariPathResolver.resolve(AMBARI_HOST_COMPONENTS)).exactTimes(4))
                .then(MockVerification.verify(HttpMethod.POST, ambariPathResolver.resolve(AMBARI_KERBEROS_CREDENTIAL)).exactTimes(0))
                .then(MockVerification.verify(HttpMethod.PUT, ambariPathResolver.resolve(AMBARI_REGENERATE_KEYTABS)).bodyContains("KERBEROS").exactTimes(0))
                .then(MockVerification.verify(HttpMethod.POST, ambariPathResolver.resolve(AMBARI_CLUSTER_REQUESTS)).bodyContains("RESTART").exactTimes(0))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testRepairMasterNodeWithKerberos(MockedTestContext testContext) {
        String ambariRdsName = getNameGenerator().getRandomNameForMock();
        createEnvWithResources(testContext, ambariRdsName);
        KerberosV4Request kerberosRequest = getKerberosRequest();
        String clusterName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(KerberosTestDto.class).valid().withRequest(kerberosRequest).withName(kerberosRequest.getName())
                .when(KerberosTestAction::post)
                .given(StackEntity.class)
                .withName(clusterName)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(getCluster(testContext, kerberosRequest.getName(), ambariRdsName))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE);
        StackEntity stackEntity = testContext.get(StackEntity.class);
        InstanceMetaDataV4Response instanceMetaData = stackEntity.getInstanceMetaData(HostGroupType.MASTER.getName()).stream().findFirst().get();
        String hostName = instanceMetaData.getDiscoveryFQDN();
        String publicIp = instanceMetaData.getPublicIp();
        addAmbariMocks(testContext, clusterName, hostName, publicIp);

        testContext
                .given(StackEntity.class)
                .when(ClusterRepairAction.valid())
                .await(StackEntity.class, STACK_AVAILABLE);

        AmbariPathResolver ambariPathResolver = new AmbariPathResolver(clusterName, hostName);
        assertGetClusterComponentCalls(stackEntity, AMBARI_CLUSTER_COMPONENTS, components, ambariPathResolver);
        assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "INSTALLED", 2, ambariPathResolver);
        assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "INIT", 1, ambariPathResolver);
        assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "STARTED", 1, ambariPathResolver);
        stackEntity
                .then(MockVerification.verify(HttpMethod.GET, ambariPathResolver.resolve(AMBARI_HOST_COMPONENTS)).exactTimes(2))
                .then(MockVerification.verify(HttpMethod.POST, ambariPathResolver.resolve(AMBARI_KERBEROS_CREDENTIAL)))
                .then(MockVerification.verify(HttpMethod.PUT, ambariPathResolver.resolve(AMBARI_REGENERATE_KEYTABS)).bodyContains("KERBEROS"))
                .then(MockVerification.verify(HttpMethod.POST, ambariPathResolver.resolve(AMBARI_CLUSTER_REQUESTS)).bodyContains("RESTART"))
                .validate();
    }

    private void assertGetClusterComponentCalls(StackEntity stackEntity, String path, Set<String> components, AmbariPathResolver ambariPathResolver) {
        components.forEach(component -> stackEntity.then(MockVerification.verify(HttpMethod.GET, ambariPathResolver.resolveComponent(path, component))));
    }

    private void assertSetComponentStateCalls(StackEntity stackEntity, String path, Set<String> components, String expectedState, int times, AmbariPathResolver ambariPathResolver) {
        MockVerification mockVerification = MockVerification.verify(HttpMethod.PUT, ambariPathResolver.resolve(path))
                .bodyContains(expectedState);
        components.forEach(mockVerification::bodyContains);
        mockVerification.exactTimes(times);
        stackEntity.then(mockVerification);
    }

    private void createEnvWithResources(MockedTestContext testContext, String ambariRdsName) {
        testContext.given(EnvironmentEntity.class)
                .withRegions(EnvironmentEntity.VALID_REGION)
                .withLocation(EnvironmentEntity.LONDON)
                .withRdsConfigs(createAmbariRdsConfig(testContext, ambariRdsName))
                .withLdapConfigs(createDefaultLdapConfig(testContext))
                .withProxyConfigs(createDefaultProxyConfig(testContext))
                .when(Environment::post);
    }

    private ClusterEntity getCluster(MockedTestContext testContext, String kerberosConfigName, String ambariRdsName) {
        testContext.given(ClusterEntity.class)
                .valid()
                .withRdsConfigNames(ambariRdsName);

        if (kerberosConfigName != null && !kerberosConfigName.isEmpty()) {
            testContext.given(ClusterEntity.class).withKerberos(kerberosConfigName);
        }

        return testContext.given(ClusterEntity.class);
    }

    private Set<String> createAmbariRdsConfig(MockedTestContext testContext, String ambariRdsName) {

        testContext
                .given(DatabaseEntity.class)
                .withType(DatabaseType.AMBARI.name())
                .withName(ambariRdsName)
                .when(new DatabaseCreateIfNotExistsAction());
        Set<String> validRds = new HashSet<>();
        validRds.add(getRdsConfigName(testContext));
        return validRds;
    }

    private String getRdsConfigName(MockedTestContext testContext) {
        return testContext.get(DatabaseEntity.class).getName();
    }

    private String getEnvironmentName(MockedTestContext testContext) {
        return testContext.get(EnvironmentEntity.class).getName();
    }

    private String getEnvironmentRegion(MockedTestContext testContext) {
        return testContext.get(EnvironmentEntity.class).getRequest().getRegions().iterator().next();
    }

    private void addAmbariMocks(MockedTestContext testContext, String clusterName, String hostName, String publicIp) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getAmbariMock().getDynamicRouteStack();
        dynamicRouteStack.get(AMBARI_HOST_COMPONENTS, new AmbariGetHostComponentsReponse(components));
        components.forEach(comp -> dynamicRouteStack.get(AMBARI_CLUSTER_COMPONENTS, new AmbariGetServiceComponentInfoResponse(comp)));
        dynamicRouteStack.put(AMBARI_HOST_COMPONENTS, new AmbariClusterRequestResponse(publicIp, clusterName));
        dynamicRouteStack.post(AMBARI_KERBEROS_CREDENTIAL, new EmptyAmbariResponse(HTTP_CREATED));
        dynamicRouteStack.put(AMBARI_REGENERATE_KEYTABS, new AmbariClusterRequestResponse(publicIp, clusterName));
        dynamicRouteStack.post(AMBARI_CLUSTER_REQUESTS, new AmbariClusterRequestResponse(publicIp, clusterName));
    }

    private KerberosV4Request getKerberosRequest() {
        KerberosV4Request request = new KerberosV4Request();
        request.setName("adKerberos");
        ActiveDirectoryKerberosDescriptor activeDirectory = new ActiveDirectoryKerberosDescriptor();
        activeDirectory.setTcpAllowed(true);
        activeDirectory.setPrincipal("admin/principal");
        activeDirectory.setPassword("kerberosPassword");
        activeDirectory.setUrl("someurl.com");
        activeDirectory.setAdminUrl("admin.url.com");
        activeDirectory.setRealm("realm");
        activeDirectory.setLdapUrl("otherurl.com");
        activeDirectory.setContainerDn("{}");
        request.setActiveDirectory(activeDirectory);
        return request;
    }

    private static class AmbariPathResolver {
        private static final String VAR_CLUSTER = ":cluster";

        private static final String VAR_HOSTNAME = ":host";

        private static final String VAR_COMPONENT = ":component";

        private final String hostName;

        private final String clusterName;

        private AmbariPathResolver(String clusterName, String hostName) {
            this.hostName = hostName;
            this.clusterName = clusterName;
        }

        private String resolve(String path) {
            return resolvePath(path);
        }

        private String resolveComponent(String path, String component) {
            return resolvePath(path)
                    .replaceAll(VAR_COMPONENT, component);
        }

        private String resolvePath(String path) {
            return eraseQuery(path)
                    .replaceAll(VAR_CLUSTER, clusterName)
                    .replaceAll(VAR_HOSTNAME, hostName);
        }

        private String eraseQuery(String pathWithQuery) {
            return pathWithQuery.split("\\?")[0];
        }
    }
}
