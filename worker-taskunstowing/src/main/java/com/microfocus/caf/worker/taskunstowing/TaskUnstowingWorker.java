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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import com.microfocus.caf.worker.taskunstowing.database.DatabaseClient;
import com.microfocus.caf.worker.taskunstowing.database.StowedTaskRow;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskUnstowingWorker implements DocumentWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUnstowingWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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

        LOGGER.info("Searching for stowed tasks for partition id: {} and job id: {}", partitionId, jobId);
        try {
            final List<StowedTaskRow> stowedTasks = databaseClient.getStowedTasks(partitionId, jobId);
            LOGGER.info("Found: {} stowed task(s) for partition id: {} and job id: {}", stowedTasks.size(), partitionId, jobId);
            stowedTasks.forEach(st -> {
                System.out.println("st.getTaskApiVersion()" + st.getTaskApiVersion());
                System.out.println("st.getTaskClassifier()" + st.getTaskClassifier());
                System.out.println("st.getPartitionId()" + st.getPartitionId());
                System.out.println("st.getJobId()" + st.getJobId());
                System.out.println("st.getTo()" + st.getTo());
                System.out.println("st.getContext()" + st.getContext());
                System.out.println("st.getId()" + st.getId());
                System.out.println("st.getSourceInfo()" + st.getSourceInfo());
                System.out.println("st.getTaskData()" + st.getTaskData());
                System.out.println("st.getTrackingInfo()" + st.getTrackingInfo());
            });
            // TODO Map List<StowedTaskRow> to tasks and send message(s) to worker(s)
         } catch (final Exception exception) {
            processFailure(TaskUnstowingWorkerFailure.FAILED_TO_READ_FROM_DATABASE, exception, document);
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
}
