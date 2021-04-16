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

import com.google.common.base.MoreObjects;
import com.hpe.caf.worker.document.model.Document;

import java.text.MessageFormat;

public enum TaskUnstowingWorkerFailure
{
    PARTITION_ID_MISSING_FROM_CUSTOM_DATA("TUW-001", "Custom data should contain a non-empty 'partitionId' property"),
    JOB_ID_MISSING_FROM_CUSTOM_DATA("TUW-002", "Custom data should contain a non-empty 'jobId' property"),
    FAILED_TO_GET_WORKER_TASK_DATA("TUW-003", "Failed to get worker task data"),
    FAILED_TO_READ_AND_DELETE_FROM_DATABASE("TUW-004", "Failed to read and delete from database"),
    FAILED_TO_CONVERT_DATABASE_ROW_TO_TASK_MESSAGE("TUW-005", "Failed to convert database row to task message"),
    FAILED_TO_SEND_UNSTOWED_TASK_MESSAGE_TO_WORKER("TUW-006", "Failed to send unstowed task message to worker");

    private final String failureId;
    private final String failureMsg;

    TaskUnstowingWorkerFailure(final String failureId, final String failureMsg)
    {
        this.failureId = failureId;
        this.failureMsg = failureMsg;
    }

    public String getFailureMsg()
    {
        return failureMsg;
    }

    public void addToDoc(final Document document)
    {
        document.addFailure(failureId, failureMsg);
    }

    public void addToDoc(final Document document, final String context, final Throwable cause)
    {
        document.getFailures().add(failureId, MessageFormat.format(failureMsg, context), cause);
    }

    public void addToDoc(final Document document, final Throwable cause)
    {
        document.getFailures().add(failureId, failureMsg, cause);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
            .add("failureId", failureId)
            .add("failureMsg", failureMsg)
            .toString();
    }
}
