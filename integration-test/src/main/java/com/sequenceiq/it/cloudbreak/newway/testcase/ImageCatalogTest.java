package com.sequenceiq.it.cloudbreak.newway.testcase;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogEntity;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ImageCatalogTest extends AbstractIntegrationTest {

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

    private static final String SPECIAL_IMAGECATALOG_NAME = "@#$%|:&*;";

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

    @Test(dataProvider = "testContext")
    public void testImageCatalogCreationWhenURLIsValidAndExists(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock();

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction())
                .then((testContext1, entity, cloudbreakClient) -> {
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), imgCatalogName, false);
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testImageCatalogCreationWhenNameIsTooShort(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock().substring(0, 4);

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("shortNamedImgCatalog"))
                .expect(BadRequestException.class, RunningParameter.key("shortNamedImgCatalog").withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testImageCatalogCreationWhenNameIsTooLong(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock().concat(longStringGeneratorUtil.stringGenerator(100));

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("shortNamedImgCatalog"))
                .expect(BadRequestException.class, RunningParameter.key("shortNamedImgCatalog").withExpectedMessage(".*The length of the credential's name has to be in range of 5 to 100"))
                .validate();
    }

//    @Test(dataProvider = "testContext")
    public void testImageCatalogCreationWhenNameContainsSpecialCharacters(TestContext testContext) {
        String imgCatalogName = getNameGenerator().getRandomNameForMock().concat(SPECIAL_IMAGECATALOG_NAME);

        testContext
                .given(ImageCatalogEntity.class).withName(imgCatalogName).withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), RunningParameter.key("shortNamedImgCatalog"))
                .expect(BadRequestException.class, RunningParameter.key("shortNamedImgCatalog"))
                .validate();
    }
}
