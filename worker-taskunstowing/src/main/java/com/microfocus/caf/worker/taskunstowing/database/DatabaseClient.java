/*
 * Copyright 2021 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.microfocus.caf.worker.taskunstowing.database;

import com.microfocus.caf.worker.taskunstowing.factory.TaskUnstowingWorkerConfiguration;
import org.jdbi.v3.core.Jdbi;
import java.util.List;
import org.jdbi.v3.postgres.PostgresPlugin;
import static com.microfocus.caf.worker.taskunstowing.database.StowedTaskColumnName.*;

public final class DatabaseClient
{
    private final Jdbi jdbi;
    private final String tableName;

    public DatabaseClient(final TaskUnstowingWorkerConfiguration configuration)
    {
        this.jdbi = Jdbi
            .create(HikariDataSourceFactory.createHikariDataSource(configuration))
            .installPlugin(new PostgresPlugin());
        this.tableName = configuration.getDatabaseTableName();
    }

    public void checkHealth() throws Exception
    {
        jdbi.useHandle(handle -> {
            handle.execute("select version();");
        });
    }

    public List<StowedTaskRow> getStowedTasks(final String partitionId, final String jobId) throws Exception
    {
        return jdbi.withHandle(handle -> {
            return handle.createQuery(
                "DELETE FROM " + tableName + " WHERE " + PARTITION_ID + " = :partitionId AND " + JOB_ID + " = :jobId RETURNING *;")
                .bind("partitionId", partitionId)
                .bind("jobId", jobId)
                .map(new StowedTaskRowMapper())
                .list();
        });
    }
}
