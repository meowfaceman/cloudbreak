package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants.CLOUDPROVIDER;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalog;
import com.sequenceiq.it.util.LongStringGeneratorUtil;


//TODO This test and integration-test/src/main/java/com/sequenceiq/it/cloudbreak/newway/ImageCatalog.java integration-test/src/main/java/com/sequenceiq/it/cloudbreak/newway/ImageCatalogAction.java class should be DELETED

public class ImageCatalogTests extends CloudbreakTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogTests.class);

    private static final String VALID_IMAGECATALOG_NAME = "valid-imagecat";

    private static final String INVALID_IMAGECATALOG_NAME_SHORT = "test";

    private static final String SPECIAL_IMAGECATALOG_NAME = "@#$%|:&*;";

    private static final String VALID_IMAGECATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-dev-cb-image-catalog.json";

    private static final String INVALID_IMAGECATALOG_URL = "google.com";

    private static final String INVALID_IMAGECATALOG_JSON = "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
            + "templates/imagecatalog_invalid.json";

    private static final String DEFAULT_IMAGECATALOG_NAME = "cloudbreak-default";

    private static final String[] IMAGECATALOG_NAMES = {VALID_IMAGECATALOG_NAME, VALID_IMAGECATALOG_NAME + "-default", VALID_IMAGECATALOG_NAME + "-old",
            VALID_IMAGECATALOG_NAME + "-new"};

    private static final String[] PROVIDERS = {"aws", "azure", "openstack", "gcp"};

    private static final String INVALID_PROVIDER_NAME = "testasdfgx";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;



    // TODO: if this test and the test of cluster creation are run parallel, wrong image id is found by image finder.
    // Find a UUID from this catalog and run with the original catalog (cloudbreak-default)
    @Test(enabled = false)
    public void testSetNewDefaultImageCatalog() throws Exception {
        given(CloudbreakClient.created());
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-default")
                .withUrl(VALID_IMAGECATALOG_URL), "an imagecatalog request and set as default"

        );
        when(ImageCatalog.post());
        when(ImageCatalog.setDefault());
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertEquals(imageCatalog.getResponse().getName(), VALID_IMAGECATALOG_NAME + "-default");
                    Assert.assertEquals(imageCatalog.getResponse().isUsedAsDefault(), true);
                }), "check imagecatalog is created and set as default");
    }

    // TODO: if this test and the test of cluster creation are run parallel, wrong image id is found by image finder.
    // Find a UUID from this catalog and run with the original catalog (cloudbreak-default)
    @Test(enabled = false)
    public void testSetImageCatalogBackAsNotDefault() throws Exception {
        given(CloudbreakClient.created());

        given(ImageCatalog.isCreatedAsDefault()
                .withName(VALID_IMAGECATALOG_NAME + "-old")
                .withUrl(VALID_IMAGECATALOG_URL)
        );

        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-new")
                .withUrl(VALID_IMAGECATALOG_URL)
        );
        when(ImageCatalog.post());
        when(ImageCatalog.setDefault());
        when(ImageCatalog.getAll());
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Set<ImageCatalogV4Response> imageCatalogResponses = imageCatalog.getResponses();
                    for (ImageCatalogV4Response response : imageCatalogResponses) {
                        if (response.getName().equals(VALID_IMAGECATALOG_NAME + "-old")) {
                            Assert.assertEquals(response.isUsedAsDefault(), false);
                        }
                        if (response.getName().equals(VALID_IMAGECATALOG_NAME + "new")) {
                            Assert.assertEquals(response.isUsedAsDefault(), true);
                        }
                    }
                })
        );
    }

    @Test
    public void testGetImageCatalogByProvider() throws Exception {
        given(CloudbreakClient.created());
        for (String provider : PROVIDERS) {
            getItContext().putContextParam(CLOUDPROVIDER, provider);
            when(ImageCatalog.getImagesByProvider(), "get the imagecatalog by provider " + provider);
            then(ImageCatalog.assertThis(
                    (imageCatalog, t) -> {
                        Assert.assertFalse(imageCatalog.getResponseByProvider().getBaseImages().isEmpty());
                        Assert.assertFalse(imageCatalog.getResponseByProvider().getHdfImages().isEmpty());
                        Assert.assertFalse(imageCatalog.getResponseByProvider().getHdpImages().isEmpty());
                    }), "check base/hdf/hdp images are listed");
        }
    }

    @Test
    public void testGetByProviderFromImageCatalog() throws Exception {
        given(CloudbreakClient.created());
        for (String provider : PROVIDERS) {
            getItContext().putContextParam(CLOUDPROVIDER, provider);
            given(ImageCatalog.request()
                    .withName(DEFAULT_IMAGECATALOG_NAME));
            when(ImageCatalog.getImagesByProviderFromImageCatalog(), " get by provider " + provider + " from image catalog");
            then(ImageCatalog.assertThis(
                    (imageCatalog, t) -> {
                        Assert.assertFalse(imageCatalog.getResponseByProvider().getBaseImages().isEmpty());
                        Assert.assertFalse(imageCatalog.getResponseByProvider().getHdfImages().isEmpty());
                        Assert.assertFalse(imageCatalog.getResponseByProvider().getHdpImages().isEmpty());
                    }), "check base/hdf/hdp images are listed");
        }
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void testGetImageCatalogByInvalidProvider() throws Exception {
        given(CloudbreakClient.created());
        getItContext().putContextParam(CLOUDPROVIDER, INVALID_PROVIDER_NAME);
        when(ImageCatalog.getImagesByProvider(), "get the imagecatalog by invalid provider");
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void testGetByInvalidProviderFromImageCatalog() throws Exception {
        given(CloudbreakClient.created());
        getItContext().putContextParam(CLOUDPROVIDER, INVALID_PROVIDER_NAME);
        given(ImageCatalog.request()
                .withName(DEFAULT_IMAGECATALOG_NAME));
        when(ImageCatalog.getImagesByProviderFromImageCatalog(), " get by invalid provider from image catalog");
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertTrue(imageCatalog.getResponseByProvider().getBaseImages().isEmpty());
                    Assert.assertTrue(imageCatalog.getResponseByProvider().getHdfImages().isEmpty());
                    Assert.assertTrue(imageCatalog.getResponseByProvider().getHdpImages().isEmpty());
                }), "check no base/hdf/hdp images are listed");
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void testGetByProviderFromNotExistingImageCatalog() throws Exception {
        given(CloudbreakClient.created());
        getItContext().putContextParam(CLOUDPROVIDER, "openstack");
        given(ImageCatalog.request()
                .withName("asdfghj987x"));
        when(ImageCatalog.getImagesByProviderFromImageCatalog(), " get by invalid provider from image catalog");
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void testRequestFromNotExistingImageCatalog() throws Exception {
        given(CloudbreakClient.created());
        given(ImageCatalog.request()
                .withName(DEFAULT_IMAGECATALOG_NAME + "sss"));
        when(ImageCatalog.getRequestFromName(), " get request of not existing image catalog");
    }

    @AfterSuite
    public void cleanAll() throws Exception {
        for (String name : IMAGECATALOG_NAMES) {
            try {
                given(CloudbreakClient.created());
                given(ImageCatalog.request()
                        .withName(name)
                );
                when(ImageCatalog.delete());
            } catch (ForbiddenException e) {
                String exceptionMessage = e.getResponse().readEntity(String.class);
                String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("ForbiddenException message ::: " + errorMessage);
            }
        }
    }

    @AfterSuite
    public void setDefaults() throws Exception {
        given(CloudbreakClient.created());
        given(ImageCatalog.request()
                .withName(DEFAULT_IMAGECATALOG_NAME)
        );
        when(ImageCatalog.setDefault());
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertEquals(imageCatalog.getResponse().getName(), DEFAULT_IMAGECATALOG_NAME);
                    Assert.assertEquals(imageCatalog.getResponse().isUsedAsDefault(), true);

                }), "check default imagecatalog is set back");
    }

    private void checkNameNotAssertEquals(String a, String b) throws Exception {
        when(ImageCatalog.post(), "post the imagecatalog request");
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> Assert.assertNotEquals(imageCatalog.getResponse().getName(), a)), "check imagecatalog is not created with " + b);
    }
}