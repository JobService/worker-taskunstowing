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

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import static com.github.jobservice.workers.taskunstowing.database.StowedTaskColumnName.*;

interface StowedTaskDAO
{
    @SqlUpdate("insert into <table> "
        + "("
        + PARTITION_ID + ", "
        + JOB_ID + ", "
        + TASK_CLASSIFIER + ", "
        + TASK_API_VERSION + ", "
        + TASK_DATA + ", "
        + TASK_STATUS + ", "
        + CONTEXT + ", \""
        + TO + "\", "
        + TRACKING_INFO + ", "
        + SOURCE_INFO + ", "
        + CORRELATION_ID + ") values "
        + "(:partitionId, :jobId, :taskClassifier, :taskApiVersion, :taskData, :taskStatus, :context, :to, :trackingInfo, :sourceInfo, :correlationId)")
    void insertStowedTask(@Define("table") final String table,
                          @Bind("partitionId") final String partitionId,
                          @Bind("jobId") final String jobId,
                          @Bind("taskClassifier") final String taskClassifier,
                          @Bind("taskApiVersion") final Integer taskApiVersion,
                          @Bind("taskData") final byte[] taskData,
                          @Bind("taskStatus") final String taskStatus,
                          @Bind("context") final byte[] context,
                          @Bind("to") final String to,
                          @Bind("trackingInfo") final byte[] trackingInfo,
                          @Bind("sourceInfo") final byte[] sourceInfo,
                          @Bind("correlationId") final String correlationId);
}
