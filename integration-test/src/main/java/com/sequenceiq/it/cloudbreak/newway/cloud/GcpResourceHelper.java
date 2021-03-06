package com.sequenceiq.it.cloudbreak.newway.cloud;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;
import static com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix.GCS;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Ranger;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Storage;

public class GcpResourceHelper extends ResourceHelper<GcsCloudStorageV4Parameters> {

    private static final String RANGER_RDS_ENTITY_ID = "GCP_RANGER_DB_CONFIG";

    private static final String HIVE_RDS_ENTITY_ID = "GCP_HIVE_DB_CONFIG";

    GcpResourceHelper(TestParameter testParameter) {
        super(testParameter);
    }

    GcpResourceHelper(TestParameter testParameter, String postfix) {
        super(testParameter, postfix);
    }

    @Override
    public DatabaseEntity aValidHiveDatabase() {
        var hive = DatabaseEntity.isCreated(HIVE_RDS_ENTITY_ID);
        hive.setRequest(createRdsRequestWithProperties(Hive.CONFIG_NAME, Hive.USER_NAME_KEY, Hive.PASSWORD_KEY, Hive.CONNECTION_URL_KEY, HIVE));
        return hive;
    }

    @Override
    public DatabaseEntity aValidRangerDatabase() {
        var ranger = DatabaseEntity.isCreated(RANGER_RDS_ENTITY_ID);
        ranger.setRequest(createRdsRequestWithProperties(Ranger.CONFIG_NAME, Ranger.USER_NAME_KEY, Ranger.PASSWORD_KEY, Ranger.CONNECTION_URL_KEY, RANGER));
        return ranger;
    }

    @Override
    public CloudStorageV4Request getCloudStorageRequestForDatalake() {
        var request = new CloudStorageV4Request();
        request.setGcs(getCloudStorage());
        request.setLocations(defaultDatalakeStorageLocations(GCS, getTestParameter().get(Storage.BUCKET_NAME)));
        return request;
    }

    @Override
    public CloudStorageV4Request getCloudStorageRequestForAttachedCluster() {
        return getCloudStorageForAttachedCluster(GCS, getTestParameter().get(Storage.BUCKET_NAME), getCloudStorage());
    }

    @Override
    protected GcsCloudStorageV4Parameters getCloudStorage() {
        return new GcsCloudStorageV4Parameters();
    }
}
