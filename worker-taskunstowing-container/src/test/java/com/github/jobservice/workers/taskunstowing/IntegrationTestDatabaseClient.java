/*
 * Copyright 2021 Micro Focus or one of its affiliates.
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
package com.github.jobservice.workers.taskunstowing;

import com.github.jobservice.workers.taskunstowing.database.StowedTaskRow;
import com.github.jobservice.workers.taskunstowing.database.StowedTaskRowMapper;
import java.util.List;
import org.jdbi.v3.core.Jdbi;
import org.testng.Assert;
import static com.github.jobservice.workers.taskunstowing.IntegrationTestSystemProperties.*;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

final class IntegrationTestDatabaseClient
{
    private final Jdbi jdbi;

    public IntegrationTestDatabaseClient()
    {
        final String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", DOCKER_HOST_ADDRESS, DATABASE_PORT, DATABASE_NAME);
        this.jdbi = Jdbi
            .create(jdbcUrl, DATABASE_USERANME, DATABASE_PASSWORD)
            .installPlugin(new PostgresPlugin())
            .installPlugin(new SqlObjectPlugin());
    }

    public void insertStowedTask(
        final String partitionId,
        final String jobId,
        final String taskClassifier,
        final int taskApiVersion,
        final byte[] taskData,
        final String taskStatus,
        final String to,
        final String trackingInfoJobTaskId,
        final Long trackingInfoLastStatusCheckTime,
        final Long trackingInfoStatusCheckIntervalMillis,
        final String trackingInfoStatusCheckUrl,
        final String trackingInfoTrackingPipe,
        final String trackingInfoTrackTo,
        final byte[] sourceInfo,
        final String correlationId) throws Exception
    {
        jdbi.useHandle(handle -> {
            final StowedTaskDAO stowedTaskDAO = handle.attach(StowedTaskDAO.class);
            stowedTaskDAO.insertStowedTask(
                DATABASE_TABLE_NAME,
                partitionId,
                jobId,
                taskClassifier,
                taskApiVersion,
                taskData,
                taskStatus,
                to,
                trackingInfoJobTaskId,
                trackingInfoLastStatusCheckTime,
                trackingInfoStatusCheckIntervalMillis,
                trackingInfoStatusCheckUrl,
                trackingInfoTrackingPipe,
                trackingInfoTrackTo,
                sourceInfo,
                correlationId);
        });
    }

    public List<StowedTaskRow> waitUntilStowedTaskTableContains(final int expectedNumberOfStowedTasks, final int timeoutMillis)
        throws InterruptedException, Exception
    {
        final long deadline = System.currentTimeMillis() + timeoutMillis;
        List<StowedTaskRow> stowedTasks = getStowedTasks();
        while (stowedTasks.size() != expectedNumberOfStowedTasks) {
            Thread.sleep(500);
            long remaining = deadline - System.currentTimeMillis();
            if (remaining < 0) {
                Assert.fail("Timed out out after " + timeoutMillis + " milliseconds waiting on " + DATABASE_TABLE_NAME + " to contain "
                    + expectedNumberOfStowedTasks + " stowed tasks. Actual number of stowed tasks is: " + stowedTasks.size());
            }
            stowedTasks = getStowedTasks();
        }
        return stowedTasks;
    }

    public List<StowedTaskRow> getStowedTasks() throws Exception
    {
        return jdbi.withHandle(handle -> {
            return handle.createQuery("SELECT * FROM " + DATABASE_TABLE_NAME)
                .map(new StowedTaskRowMapper())
                .list();
        });
    }

    public void deleteStowedTasks() throws Exception
    {
        jdbi.useHandle(handle -> {
            handle.execute("DELETE FROM " + DATABASE_TABLE_NAME);
        });
    }
}
