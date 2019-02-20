package com.sequenceiq.it.cloudbreak.newway.testcase;

import static java.lang.String.format;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogEntity;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ImageCatalogTest extends AbstractIntegrationTest {

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

    private static final String SPECIAL_IMAGECATALOG_NAME = "@#$%|:&*;";

    private static final String INVALID_IMAGECATALOG_JSON_URL = "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
            + "templates/imagecatalog_invalid.json";
    public static final String CB_DEFAULT_IMG_CATALOG_NAME = "cloudbreak-default";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenURLIsValidAndExists(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), imgCatalogName, false);
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenNameIsTooShort(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock().substring(0, 4);

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenNameIsTooLong(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock().concat(longStringGeneratorUtil.stringGenerator(100));

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

//    @Test(dataProvider = "testContext")
    public void testImageCatalogCreationWhenNameContainsSpecialCharacters(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock().concat(SPECIAL_IMAGECATALOG_NAME);

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenTheCatalogURLIsInvalid(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        String invalidURL = "https:/google.com/imagecatalog";

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(invalidURL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".* " + invalidURL + ", error: A valid image catalog must be available on the given URL"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenTheCatalogURLPointsNotToAnImageCatalogJson(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL + "/notanimagecatalog")
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*A valid image catalog must be available on the given URL.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenTheCatalogURLPointsToAnInvalidImageCatalogJson(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(INVALID_IMAGECATALOG_JSON_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("ImageCatalogPostAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogPostAction")
                        .withExpectedMessage(".*" + INVALID_IMAGECATALOG_JSON_URL + ", error: A valid image catalog must be available on the given URL.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogCreationWhenCatalogWithTheSameNameDeletedRightBeforeCreation(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .select(imgCatalog -> imgCatalog.getResponse().getId(), RunningParameter.key("ImageCatalogPostActionSelect"))
                .when(new ImageCatalogDeleteAction())
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImageCatalogV4Response imageCatalogV4Response = cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint()
                            .get(cloudbreakClient.getWorkspaceId(), imgCatalogName, false);
                    Long firstPostEntityId = testContext1.getSelected("ImageCatalogPostActionSelect");
                    if (imageCatalogV4Response.getId().equals(firstPostEntityId)) {
                        throw new IllegalArgumentException("The re-created ImageCatalog should have a different id.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogDeletion(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogDeleteAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), imgCatalogName, false);
                    return entity;
                }, RunningParameter.key("ImageCatalogDeleteAction"))
                .expect(ForbiddenException.class, RunningParameter.key("ImageCatalogDeleteAction")
                        .withExpectedMessage("HTTP 403 Forbidden"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testImageCatalogDeletionWithDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogEntity.class).withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .when(new ImageCatalogDeleteAction(), RunningParameter.key("ImageCatalogDeleteAction"))
                .expect(BadRequestException.class, RunningParameter.key("ImageCatalogDeleteAction")
                        .withExpectedMessage(format(".*%s cannot be deleted because it is an environment default image catalog.*",
                                CB_DEFAULT_IMG_CATALOG_NAME)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogEntity.class).withName(CB_DEFAULT_IMG_CATALOG_NAME)
                .then((testContext1, entity, cloudbreakClient) -> {
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), CB_DEFAULT_IMG_CATALOG_NAME, false);
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogWhenCatalogContainsTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getImagesByName(cloudbreakClient.getWorkspaceId(), imgCatalogName, null, CloudPlatform.MOCK.name());
                    entity.setResponseByProvider(catalog);
                    if (catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty()) {
                        throw new IllegalArgumentException("The Images response should contain results for MOCK provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogWhenCatalogDoesNotContainTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getImagesByName(cloudbreakClient.getWorkspaceId(), imgCatalogName, null, CloudPlatform.AWS.name());
                    entity.setResponseByProvider(catalog);
                    if (!(catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty())) {
                        throw new IllegalArgumentException("The Images response should NOT contain results for AWS provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogWhenDefaultCatalogContainsTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getImages(cloudbreakClient.getWorkspaceId(), null, CloudPlatform.AWS.name());
                    entity.setResponseByProvider(catalog);
                    if (catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty()) {
                        throw new IllegalArgumentException("The Images response should contain results for AWS provider.");
                    }
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetImageCatalogWhenDefaultCatalogDoesNotContainTheRequestedProvider(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl())
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    ImagesV4Response catalog = cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getImages(cloudbreakClient.getWorkspaceId(), null, CloudPlatform.MOCK.name());
                    entity.setResponseByProvider(catalog);
                    if (!(catalog.getBaseImages().isEmpty() && catalog.getHdpImages().isEmpty() && catalog.getHdfImages().isEmpty())) {
                        throw new IllegalArgumentException("The Images response should NOT contain results for MOCK provider.");
                    }
                    return entity;
                })
                .validate();
    }
    //TODO test get images from catalog for existing Stack
    //TODO test get images from default catalog for existing Stack

    //TODO ImageCatalogMockServerSetup to be configurable com.sequenceiq.it.cloudbreak.newway.mock.ImageCatalogMockServerSetup.getImageCatalogUrl


}
