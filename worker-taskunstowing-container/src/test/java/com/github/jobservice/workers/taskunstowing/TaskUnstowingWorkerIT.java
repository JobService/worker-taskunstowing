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
import com.google.common.collect.ImmutableList;
import com.hpe.caf.api.worker.TaskMessage;
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

import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import java.time.LocalDate;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

public class TaskUnstowingWorkerIT
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUnstowingWorkerIT.class);
    // FAIL_ON_UNKNOWN_PROPERTIES is set to false because of:
    // UnrecognizedPropertyException: Unrecognized field \"jobId\" (class com.hpe.caf.api.worker.TrackingInfo), not marked as ignorable
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String WORKER_TASKUNSTOWING_IN_QUEUE = "worker-taskunstowing-in";
    private static final String TARGET_QUEUE_FOR_UNSTOWED_TASKS = "dataprocessing-elasticquery-in";
    private static final Date ONE_DAY_AGO = java.sql.Date.valueOf(LocalDate.now().minusDays(1));
    private static final long TWO_MINUTES_IN_MILLIS = 120000L;

    private final IntegrationTestDatabaseClient integrationTestDatabaseClient = new IntegrationTestDatabaseClient();
    private IntegrationTestQueueServices integrationTestQueueServices;

    @BeforeMethod
    public void setUpTest(Method method) throws Exception
    {
        LOGGER.info("Starting: {}", method.getName());
        integrationTestQueueServices = new IntegrationTestQueueServices();
    }

    @AfterMethod
    public void cleanupTest(Method method) throws Exception
    {
        integrationTestQueueServices.close();
        integrationTestDatabaseClient.deleteStowedTasks();
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
            tenant1PartitionId,
            tenant1JobId,
            "DocumentWorkerTask",
            1,
            tenant1TaskDataBytes,
            "NEW_TASK",
            OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap()),
            TARGET_QUEUE_FOR_UNSTOWED_TASKS,
            tenant1TrackingInfoBytes,
            null,
            "1");

        final String tenant2PartitionId = "tenant2";
        final String tenant2JobId = "job2";
        final DocumentWorkerDocumentTask tenant2DocumentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        tenant2DocumentWorkerDocumentTask.document = createSampleDocumentWithField("TENANT", tenant2PartitionId);
        final byte[] tenant2TaskDataBytes = OBJECT_MAPPER.writeValueAsBytes(tenant2DocumentWorkerDocumentTask);

        final TrackingInfo tenant2TrackingInfo = new TrackingInfo(
            tenant2PartitionId + ":" + tenant2JobId,
            ONE_DAY_AGO,
            TWO_MINUTES_IN_MILLIS,
            "dummy-url",
            TARGET_QUEUE_FOR_UNSTOWED_TASKS,
            null);
        final byte[] tenant2TrackingInfoBytes = OBJECT_MAPPER.writeValueAsBytes(tenant2TrackingInfo);

        integrationTestDatabaseClient.insertStowedTask(
            tenant2PartitionId,
            tenant2JobId,
            "DocumentWorkerTask",
            1,
            tenant2TaskDataBytes,
            "NEW_TASK",
            OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap()),
            TARGET_QUEUE_FOR_UNSTOWED_TASKS,
            tenant2TrackingInfoBytes,
            null,
            "1");

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

        integrationTestQueueServices.startListening();
        integrationTestQueueServices.sendTaskMessage(resumeJobTaskMessage);

        // Then the task should have been removed from the database
        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);

        // And the task should have been sent onto the worker it is intended for
        integrationTestQueueServices.waitForUnstowedTaskQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the queue named in the unstowed task message's 'to' field", 1,
                     integrationTestQueueServices.getUnstowedTaskQueueMessages().size());

        final TaskMessage unstowedTaskMessage
            = OBJECT_MAPPER.readValue(integrationTestQueueServices.getUnstowedTaskQueueMessages().get(0), TaskMessage.class);
        assertNotNull("Unable to convert unstowed task message to an instance of TaskMessage", unstowedTaskMessage);

        // Single value task message fields
        assertEquals("Unstowed task message has unexpected taskApiVersion",
                     1, unstowedTaskMessage.getTaskApiVersion());
        assertEquals("Unstowed task message has unexpected taskClassifier",
                     "DocumentWorkerTask", unstowedTaskMessage.getTaskClassifier());
        assertEquals("Unstowed task message has unexpected taskStatus",
                     TaskStatus.NEW_TASK, unstowedTaskMessage.getTaskStatus());
        assertEquals("Unstowed task message has unexpected to",
                     TARGET_QUEUE_FOR_UNSTOWED_TASKS, unstowedTaskMessage.getTo());
        assertEquals("Unstowed task message has unexpected correlationId",
                     "1", unstowedTaskMessage.getCorrelationId());

        // taskData
        final DocumentWorkerDocumentTask unstowedTaskMessageTaskData
            = OBJECT_MAPPER.readValue(unstowedTaskMessage.getTaskData(), DocumentWorkerDocumentTask.class);
        assertNotNull("Unable to convert unstowed task message's taskData to an instance of DocumentWorkerDocumentTask",
                      unstowedTaskMessageTaskData);
        assertNotNull("Unstowed task message should have a non-null document", unstowedTaskMessageTaskData.document);
        assertEquals("Unstowed task message has unexpected value for the TENANT field in it's document",
                     tenant2PartitionId, unstowedTaskMessageTaskData.document.fields.get("TENANT").get(0).data);

        // tracking
        final TrackingInfo unstowedTaskMessageTracking = unstowedTaskMessage.getTracking();
        assertNotNull("Unstowed task message should have a non-null tracking property", unstowedTaskMessageTracking);
        assertEquals("Unstowed task message has unexpected value for tracking.jobTaskId",
                     tenant2PartitionId + ":" + tenant2JobId, unstowedTaskMessageTracking.getJobTaskId());
        assertEquals("Unstowed task message has unexpected value for tracking.lastStatusCheckTime",
                      ONE_DAY_AGO, unstowedTaskMessageTracking.getLastStatusCheckTime());
        assertEquals("Unstowed task message has unexpected value for tracking.statusCheckIntervalMillis",
                     TWO_MINUTES_IN_MILLIS, unstowedTaskMessageTracking.getStatusCheckIntervalMillis());
        assertEquals("Unstowed task message has unexpected value for tracking.statusCheckUrl",
                     "dummy-url", unstowedTaskMessageTracking.getStatusCheckUrl());
        assertEquals("Unstowed task message has unexpected value for tracking.trackingPipe",
                     TARGET_QUEUE_FOR_UNSTOWED_TASKS, unstowedTaskMessageTracking.getTrackingPipe());
        assertNull("Unstowed task message should have a null tracking.trackTo field", unstowedTaskMessageTracking.getTrackTo());

        // context
        assertEquals("Unstowed task message has unexpected context",
                     Collections.<String, byte[]>emptyMap(), unstowedTaskMessage.getContext());

        // sourceInfo
        assertNull("Unstowed task message should have a null sourceInfo property", unstowedTaskMessage.getSourceInfo());

    }

    @Test
    public void testPartitionIdMissingFromCustomData() throws IOException, InterruptedException, Exception
    {
        // Given a database with a stowed task message
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
            tenant1PartitionId,
            tenant1JobId,
            "DocumentWorkerTask",
            1,
            tenant1TaskDataBytes,
            "NEW_TASK",
            OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap()),
            TARGET_QUEUE_FOR_UNSTOWED_TASKS, tenant1TrackingInfoBytes, null, "1");

        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);

        // When a resume job message without a partitionId in custom data is sent to the worker
        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.customData = new HashMap<>();
        documentWorkerDocumentTask.customData.put("jobId", tenant1JobId);
        // No partitionId set on customDaata

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

        integrationTestQueueServices.startListening();
        integrationTestQueueServices.sendTaskMessage(resumeJobTaskMessage);

        // Then the task should have been sent to the worker's error queue
        integrationTestQueueServices.waitForErrorQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the workers error queue", 1,
                     integrationTestQueueServices.getErrorQueueMessages().size());

        // And the task should NOT have been removed from the database
        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);
    }

    @Test
    public void testJobIdMissingFromCustomData() throws IOException, InterruptedException, Exception
    {
        // Given a database with a stowed task message
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
            tenant1PartitionId,
            tenant1JobId,
            "DocumentWorkerTask",
            1,
            tenant1TaskDataBytes,
            "NEW_TASK",
            OBJECT_MAPPER.writeValueAsBytes(Collections.<String, byte[]>emptyMap()),
            TARGET_QUEUE_FOR_UNSTOWED_TASKS, tenant1TrackingInfoBytes, null, "1");

        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);

        // When a resume job message without a jobId in custom data is sent to the worker
        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.customData = new HashMap<>();
        documentWorkerDocumentTask.customData.put("partitionId", tenant1PartitionId);
        // No jobId set on customData

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

        integrationTestQueueServices.startListening();
        integrationTestQueueServices.sendTaskMessage(resumeJobTaskMessage);

        // Then the task should have been sent to the worker's error queue
        integrationTestQueueServices.waitForErrorQueueMessages(1, 30000);
        assertEquals("Expected 1 message to have been sent to the workers error queue", 1,
                     integrationTestQueueServices.getErrorQueueMessages().size());

        // And the task should NOT have been removed from the database
        integrationTestDatabaseClient.waitUntilStowedTaskTableContains(1, 30000);
    }

    private static DocumentWorkerDocument createSampleDocumentWithField(final String fieldKey, final String fieldValue)
    {
        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();
        final Map<String, List<DocumentWorkerFieldValue>> documentWorkerDocumentFields = new HashMap<>();
        final DocumentWorkerFieldValue documentWorkerFieldValue = new DocumentWorkerFieldValue();
        documentWorkerFieldValue.encoding = DocumentWorkerFieldEncoding.utf8;
        documentWorkerFieldValue.data = fieldValue;
        documentWorkerDocumentFields.put(fieldKey, ImmutableList.of(documentWorkerFieldValue));
        documentWorkerDocument.fields = documentWorkerDocumentFields;
        return documentWorkerDocument;
    }
}
