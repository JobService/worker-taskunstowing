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
import com.google.common.collect.ImmutableList;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microfocus.caf.worker.taskunstowing.IntegrationTestSystemProperties.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import com.hpe.caf.api.worker.JobStatus;
import java.time.LocalDate;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.BeforeClass;

public class TaskUnstowingWorkerIT
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUnstowingWorkerIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String WORKER_TASKUNSTOWING_IN_QUEUE = "worker-taskunstowing-in";
    private static final String TARGET_QUEUE_FOR_UNSTOWED_TASKS = "dataprocessing-elasticquery-in";
    private static final Date ONE_DAY_AGO = java.sql.Date.valueOf(LocalDate.now().minusDays(1));
    private static final long TWO_MINUTES_IN_MILLIS = 120000L;

    private final IntegrationTestDatabaseClient integrationTestDatabaseClient = new IntegrationTestDatabaseClient();
    private QueueServices queueServices;

    @BeforeMethod
    public void setUpTest(Method method) throws Exception
    {
        LOGGER.info("Starting: {}", method.getName());
        queueServices = new QueueServices();
    }

    @AfterMethod
    public void cleanupTest(Method method) throws Exception
    {
        queueServices.close();
    //    integrationTestDatabaseClient.deleteStowedTasks();
        LOGGER.info("End of: {}", method.getName());
    }

    @Test
    public void testUnstowTask() throws IOException, InterruptedException, Exception
    {
        // Given a database with 2 stowed task messages for 2 different tenants
        final String tenant1PartitionId = "tenant1";
        final String tenant1JobId = "job1";
        final DocumentWorkerDocumentTask tenant1DocumentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        tenant1DocumentWorkerDocumentTask.document = createSampleDocumentWithField("TENANT", tenant1PartitionId);
        final byte[] tenant1TaskDataBytes = OBJECT_MAPPER.writeValueAsBytes(tenant1DocumentWorkerDocumentTask);

        final TrackingInfo tenant1TrackingInfo = new TrackingInfo(
            tenant1PartitionId + ":" + tenant1JobId,
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            "dummy-url",
            TARGET_QUEUE_FOR_UNSTOWED_TASKS,
            null);
        final byte[] tenant1TrackingInfoBytes = OBJECT_MAPPER.writeValueAsBytes(tenant1TrackingInfo);

        integrationTestDatabaseClient.insertStowedTask(
            tenant1PartitionId, tenant1JobId, "DocumentWorkerTask", 1, tenant1TaskDataBytes, "NEW_TASK", new byte[0],
            TARGET_QUEUE_FOR_UNSTOWED_TASKS, tenant1TrackingInfoBytes, null, "1");

        final String tenant2PartitionId = "tenant2";
        final String tenant2JobId = "job2";
        final DocumentWorkerDocumentTask tenant2DocumentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        tenant2DocumentWorkerDocumentTask.document = createSampleDocumentWithField("TENANT", tenant2PartitionId);
        final byte[] tenant2TaskDataBytes = OBJECT_MAPPER.writeValueAsBytes(tenant1DocumentWorkerDocumentTask);

        final TrackingInfo tenant2TrackingInfo = new TrackingInfo(
            tenant2PartitionId + ":" + tenant2JobId,
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            "dummy-url",
            TARGET_QUEUE_FOR_UNSTOWED_TASKS,
            null);
        final byte[] tenant2TrackingInfoBytes = OBJECT_MAPPER.writeValueAsBytes(tenant2TrackingInfo);

        integrationTestDatabaseClient.insertStowedTask(
            tenant2PartitionId, tenant2JobId, "DocumentWorkerTask", 1, tenant2TaskDataBytes, "NEW_TASK", new byte[0],
            TARGET_QUEUE_FOR_UNSTOWED_TASKS, tenant2TrackingInfoBytes, null, "1");

        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(2, 30000);

        // When a resume job message for tenant2's job is sent to the worker
        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.customData = new HashMap<>();
        documentWorkerDocumentTask.customData.put("partitionId", tenant2PartitionId);
        documentWorkerDocumentTask.customData.put("jobId", tenant2JobId);

        final TaskMessage resumeJobTaskMessage = new TaskMessage(
            UUID.randomUUID().toString(),
            "DocumentWorkerTask",
            2,
            OBJECT_MAPPER.writeValueAsBytes(documentWorkerDocumentTask),
            TaskStatus.NEW_TASK,
            Collections.<String, byte[]>emptyMap(),
            WORKER_TASKUNSTOWING_IN_QUEUE,
            null,
            null,
            "1");

        queueServices.startListening();
        queueServices.sendTaskMessage(resumeJobTaskMessage);

        // Then the task should have been removed from the database
        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);

        // And the task should have been sent onto the worker it is intended for
        // TODO
    }


    private static DocumentWorkerDocument createSampleDocumentWithField(final String fieldKey, final String fieldValue)
    {
        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();
        documentWorkerDocument.reference = "1";
        final Map<String, List<DocumentWorkerFieldValue>> documentWorkerDocumentFields = new HashMap<>();
        final DocumentWorkerFieldValue documentWorkerFieldValue = new DocumentWorkerFieldValue();
        documentWorkerFieldValue.encoding = DocumentWorkerFieldEncoding.utf8;
        documentWorkerFieldValue.data = fieldValue;
        documentWorkerDocumentFields.put(fieldKey, ImmutableList.of(documentWorkerFieldValue));
        documentWorkerDocument.fields = documentWorkerDocumentFields;
        return documentWorkerDocument;
    }
}
