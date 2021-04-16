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
package com.microfocus.caf.worker.taskunstowing;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
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
import com.microfocus.caf.worker.taskunstowing.database.DatabaseClient;
import com.microfocus.caf.worker.taskunstowing.database.StowedTaskRow;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskUnstowingWorker implements DocumentWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUnstowingWorker.class);
    // FAIL_ON_UNKNOWN_PROPERTIES is set to false because of:
    // UnrecognizedPropertyException: Unrecognized field \"jobId\" (class com.hpe.caf.api.worker.TrackingInfo), not marked as ignorable
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final DatabaseClient databaseClient;

    public TaskUnstowingWorker(final DatabaseClient databaseClient)
    {
        this.databaseClient = databaseClient;
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
        final String partitionId = document.getCustomData("partitionId");
        if (Strings.isNullOrEmpty(partitionId)) {
            processFailure(TaskUnstowingWorkerFailure.PARTITION_ID_MISSING_FROM_CUSTOM_DATA, document);
            return;
        }

        final String jobId = document.getCustomData("jobId");
        if (Strings.isNullOrEmpty(jobId)) {
            processFailure(TaskUnstowingWorkerFailure.JOB_ID_MISSING_FROM_CUSTOM_DATA, document);
            return;
        }

        final WorkerTaskData workerTaskData = document.getTask().getService(WorkerTaskData.class);
        if (workerTaskData == null) {
            processFailure(TaskUnstowingWorkerFailure.FAILED_TO_GET_WORKER_TASK_DATA, document);
            return;
        }

        LOGGER.info("Searching for stowed tasks for partition id: {} and job id: {}", partitionId, jobId);
        final List<StowedTaskRow> stowedTaskRows;
        try {
            stowedTaskRows = databaseClient.getAndDeleteStowedTasks(partitionId, jobId);
            LOGGER.info("Found: {} stowed task(s) for partition id: {} and job id: {}", stowedTaskRows.size(), partitionId, jobId);
        } catch (final Exception exception) {
            processFailure(TaskUnstowingWorkerFailure.FAILED_TO_READ_AND_DELETE_FROM_DATABASE, exception, document);
            return;
        }

        for (final StowedTaskRow stowedTaskRow : stowedTaskRows) {
            final TaskMessage taskMessage;
            try {
                taskMessage = convertStowedTaskRowToTaskMessage(stowedTaskRow);
            } catch (final IOException exception) {
                processFailure(TaskUnstowingWorkerFailure.FAILED_TO_CONVERT_DATABASE_ROW_TO_TASK_MESSAGE, exception, document);
                continue;
            }

            try {
                workerTaskData.sendMessage(taskMessage);
                LOGGER.info("Sent unstowed task message for partition id: {} and job id: {} to queue: {}",
                            partitionId, jobId, taskMessage.getTo());
            } catch (final Exception exception) {
                // TODO What to do now, try to insert row again?
                // TODO add job id etc to failure messages?
                processFailure(TaskUnstowingWorkerFailure.FAILED_TO_SEND_UNSTOWED_TASK_MESSAGE_TO_WORKER, exception, document);
            }
        }
    }

    private static void processFailure(
        final TaskUnstowingWorkerFailure TaskUnstowingWorkerFailure,
        final Document document)
    {
        LOGGER.error(TaskUnstowingWorkerFailure.getFailureMsg());
        TaskUnstowingWorkerFailure.addToDoc(document);
    }

    private static void processFailure(
        final TaskUnstowingWorkerFailure TaskUnstowingWorkerFailure,
        final Throwable cause,
        final Document document)
    {
        LOGGER.error(TaskUnstowingWorkerFailure.getFailureMsg(), cause);
        TaskUnstowingWorkerFailure.addToDoc(document, cause);
    }

    private static TaskMessage convertStowedTaskRowToTaskMessage(final StowedTaskRow stowedTaskRow) throws IOException
    {
        return new TaskMessage(
            UUID.randomUUID().toString(),
            stowedTaskRow.getTaskClassifier(),
            stowedTaskRow.getTaskApiVersion(),
            stowedTaskRow.getTaskData(),
            TaskStatus.valueOf(stowedTaskRow.getTaskStatus()),
            stowedTaskRow.getContext() != null
            ? OBJECT_MAPPER.readValue(stowedTaskRow.getContext(), Map.class)
            : Collections.<String, byte[]>emptyMap(),
            stowedTaskRow.getTo(),
            stowedTaskRow.getTrackingInfo() != null ? OBJECT_MAPPER.readValue(stowedTaskRow.getTrackingInfo(), TrackingInfo.class) : null,
            stowedTaskRow.getSourceInfo() != null ? OBJECT_MAPPER.readValue(stowedTaskRow.getSourceInfo(), TaskSourceInfo.class) : null,
            stowedTaskRow.getCorrelationId());
    }
}
