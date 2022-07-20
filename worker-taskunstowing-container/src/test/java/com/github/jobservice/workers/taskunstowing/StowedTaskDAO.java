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
        + TASK_STATUS + ", \""
        + TO + "\", "
        + TRACKING_INFO_JOB_TASK_ID + ", "
        + TRACKING_INFO_LAST_STATUS_CHECK_TIME + ", "
        + TRACKING_INFO_STATUS_CHECK_INTERVAL_MILLIS + ", "
        + TRACKING_INFO_STATUS_CHECK_URL + ", "
        + TRACKING_INFO_TRACKING_PIPE + ", "
        + TRACKING_INFO_TRACK_TO + ", "
        + SOURCE_INFO + ", "
        + CORRELATION_ID + ") values "
        + "(:partitionId, :jobId, :taskClassifier, :taskApiVersion, :taskData, :taskStatus, :to, :trackingInfoJobTaskId, "
        + ":trackingInfoLastStatusCheckTime, :trackingInfoStatusCheckIntervalMillis, :trackingInfoStatusCheckUrl, "
        + ":trackingInfoTrackingPipe, :trackingInfoTrackTo, :sourceInfo, :correlationId)")
    void insertStowedTask(@Define("table") String table,
                          @Bind("partitionId") String partitionId,
                          @Bind("jobId") String jobId,
                          @Bind("taskClassifier") String taskClassifier,
                          @Bind("taskApiVersion") Integer taskApiVersion,
                          @Bind("taskData") byte[] taskData,
                          @Bind("taskStatus") String taskStatus,
                          @Bind("to") String to,
                          @Bind("trackingInfoJobTaskId") String trackingInfoJobTaskId,
                          @Bind("trackingInfoLastStatusCheckTime") Long trackingInfoLastStatusCheckTime,
                          @Bind("trackingInfoStatusCheckIntervalMillis") Long trackingInfoStatusCheckIntervalMillis,
                          @Bind("trackingInfoStatusCheckUrl") String trackingInfoStatusCheckUrl,
                          @Bind("trackingInfoTrackingPipe") String trackingInfoTrackingPipe,
                          @Bind("trackingInfoTrackTo") String trackingInfoTrackTo,
                          @Bind("sourceInfo") byte[] sourceInfo,
                          @Bind("correlationId") String correlationId);
}
