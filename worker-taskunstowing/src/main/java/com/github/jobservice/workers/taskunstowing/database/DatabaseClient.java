/*
 * Copyright 2021-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jobservice.workers.taskunstowing.database;

import com.github.jobservice.workers.taskunstowing.factory.TaskUnstowingWorkerConfiguration;
import org.jdbi.v3.core.Jdbi;
import java.util.List;
import org.jdbi.v3.postgres.PostgresPlugin;
import static com.github.jobservice.workers.taskunstowing.database.StowedTaskColumnName.*;
import java.util.ArrayList;

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

    public List<StowedTaskRow> getStowedTasks(final String partitionId, final String jobId, final int maxRows) throws Exception
    {
        return jdbi.withHandle(handle -> {
            return handle.createQuery(
                "SELECT * FROM <table> WHERE " + PARTITION_ID + " = :partitionId AND " + JOB_ID + " = :jobId LIMIT " + maxRows)
                .define("table", tableName)
                .bind("partitionId", partitionId)
                .bind("jobId", jobId)
                .map(new StowedTaskRowMapper())
                .list();
        });
    }

    public void deleteStowedTask(final String partitionId, final String jobId, final String jobTaskId) throws Exception
    {
        jdbi.useHandle(handle -> {
            handle.createUpdate(
                "DELETE FROM <table> WHERE " + PARTITION_ID + " = :partitionId AND " + JOB_ID + " = :jobId AND "
                + TRACKING_INFO_JOB_TASK_ID + " = :jobTaskId")
                .define("table", tableName)
                .bind("partitionId", partitionId)
                .bind("jobId", jobId)
                .bind("jobTaskId", jobTaskId)
                .execute();
        });
    }
}
