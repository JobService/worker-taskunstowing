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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import com.github.jobservice.workers.taskunstowing.database.DatabaseClient;
import com.github.jobservice.workers.taskunstowing.database.DatabaseExceptionChecker;
import com.github.jobservice.workers.taskunstowing.database.StowedTaskRow;
import com.github.jobservice.workers.taskunstowing.queue.QueueServices;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskUnstowingWorker implements DocumentWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUnstowingWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DatabaseClient databaseClient;
    private final QueueServices queueServices;

    public TaskUnstowingWorker(final DatabaseClient databaseClient, final QueueServices queueServices)
    {
        this.databaseClient = databaseClient;
        this.queueServices = queueServices;
    }

    @Override
    public void checkHealth(final HealthMonitor healthMonitor)
    {
        try {
            databaseClient.checkHealth();
        } catch (final Exception exception) {
            final String message = "Database is unhealthy";
            LOGGER.error(message, exception);
            healthMonitor.reportUnhealthy(message);
        }
    }

    @Override
    public void processDocument(final Document document) throws InterruptedException, DocumentWorkerTransientException
    {
        LOGGER.info("Received request to unstow task(s)");

        final String partitionId = document.getCustomData("partitionId");
        if (Strings.isNullOrEmpty(partitionId)) {
            final TaskUnstowingWorkerFailure failure = TaskUnstowingWorkerFailure.PARTITION_ID_MISSING_FROM_CUSTOM_DATA_ID;
            processFailure(failure, document);
            return;
        }

        final String jobId = document.getCustomData("jobId");
        if (Strings.isNullOrEmpty(jobId)) {
            final TaskUnstowingWorkerFailure failure = TaskUnstowingWorkerFailure.JOB_ID_MISSING_FROM_CUSTOM_DATA_ID;
            processFailure(failure, document);
            return;
        }

        final WorkerTaskData workerTaskData = document.getTask().getService(WorkerTaskData.class);
        if (workerTaskData == null) {
            final TaskUnstowingWorkerFailure failure = TaskUnstowingWorkerFailure.FAILED_TO_GET_WORKER_TASK_DATA_ID;
            processFailure(failure, document);
            return;
        }

        LOGGER.info("Querying for stowed tasks for partition id {} and job id {}", partitionId, jobId);
        final List<StowedTaskRow> stowedTaskRows;
        try {
            stowedTaskRows = databaseClient.getStowedTasks(partitionId, jobId);
            LOGGER.info("Found {} stowed task(s) for partition id {} and job id {}", stowedTaskRows.size(), partitionId, jobId);
        } catch (final Exception exception) {
            final TaskUnstowingWorkerFailure failure = TaskUnstowingWorkerFailure.FAILED_TO_READ_FROM_DATABASE_ID;
            processFailure(failure, exception, document, partitionId, jobId);
            return;
        }

        for (final StowedTaskRow stowedTaskRow : stowedTaskRows) {
            final TaskMessage taskMessage;
            try {
                taskMessage = convertStowedTaskRowToTaskMessage(stowedTaskRow);
            } catch (final IOException exception) {
                final TaskUnstowingWorkerFailure failure = TaskUnstowingWorkerFailure.FAILED_TO_CONVERT_DATABASE_ROW_TO_TASK_MESSAGE_ID;
                processFailure(failure, exception, document, stowedTaskRow.getPartitionId(), stowedTaskRow.getJobId(),
                               stowedTaskRow.getTrackingInfoJobTaskId());
                continue;
            }

            try {
                queueServices.sendTaskMessage(taskMessage);
                LOGGER.info("Sent unstowed task message with partition id {}, job id {} and job task id {} to queue {}",
                            partitionId, jobId, taskMessage.getTracking().getJobTaskId(), taskMessage.getTo());
            } catch (final Exception exception) {
                final TaskUnstowingWorkerFailure failure = TaskUnstowingWorkerFailure.FAILED_TO_SEND_UNSTOWED_TASK_MESSAGE_TO_QUEUE_ID;
                processFailure(failure, exception, document, partitionId, jobId, taskMessage.getTracking().getJobTaskId(),
                               taskMessage.getTo());
                continue;
            }

            try {
                databaseClient.deleteStowedTask(partitionId, jobId, taskMessage.getTracking().getJobTaskId());
            } catch (final Exception exception) {
                final TaskUnstowingWorkerFailure failure
                    = TaskUnstowingWorkerFailure.FAILED_TO_DELETE_UNSTOWED_TASK_MESSAGE_FROM_DATABASE_ID;
                if (DatabaseExceptionChecker.isTransientException(exception)) {
                    LOGGER.error(
                        failure.toString(partitionId, jobId, taskMessage.getTracking().getJobTaskId(), taskMessage.getTo()), exception);
                    throw new DocumentWorkerTransientException(
                        failure.toString(partitionId, jobId, taskMessage.getTracking().getJobTaskId(), taskMessage.getTo()), exception);
                } else {
                    processFailure(
                        failure, exception, document, partitionId, jobId, taskMessage.getTracking().getJobTaskId(), taskMessage.getTo());
                }
            }
        }
    }

    @Override
    public void close() {
        queueServices.close();
    }

    private static void processFailure(
        final TaskUnstowingWorkerFailure taskUnstowingWorkerFailure,
        final Document document)
    {
        LOGGER.error(taskUnstowingWorkerFailure.toString());
        taskUnstowingWorkerFailure.addToDoc(document);
    }

    private static void processFailure(
        final TaskUnstowingWorkerFailure taskUnstowingWorkerFailure,
        final Throwable cause,
        final Document document,
        final Object... arguments)
    {
        LOGGER.error(taskUnstowingWorkerFailure.toString(arguments), cause);
        taskUnstowingWorkerFailure.addToDoc(document, cause, arguments);
    }

    private static TaskMessage convertStowedTaskRowToTaskMessage(final StowedTaskRow stowedTaskRow) throws IOException
    {
        final TrackingInfo trackingInfo = new TrackingInfo(
            stowedTaskRow.getTrackingInfoJobTaskId(),
            new Date(stowedTaskRow.getTrackingInfoLastStatusCheckTime()),
            stowedTaskRow.getTrackingInfoStatusCheckIntervalMillis(),
            stowedTaskRow.getTrackingInfoStatusCheckUrl(),
            stowedTaskRow.getTrackingInfoTrackingPipe(),
            stowedTaskRow.getTrackingInfoTrackTo());

        return new TaskMessage(
            UUID.randomUUID().toString(),
            stowedTaskRow.getTaskClassifier(),
            stowedTaskRow.getTaskApiVersion(),
            stowedTaskRow.getTaskData(),
            TaskStatus.valueOf(stowedTaskRow.getTaskStatus()),
            Collections.<String, byte[]>emptyMap(),
            stowedTaskRow.getTo(),
            trackingInfo,
            stowedTaskRow.getSourceInfo() != null ? OBJECT_MAPPER.readValue(stowedTaskRow.getSourceInfo(), TaskSourceInfo.class) : null,
            stowedTaskRow.getCorrelationId());
    }
}
