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

final class TaskUnstowingWorkerFailure
{
    public static final String PARTITION_ID_MISSING_FROM_CUSTOM_DATA_ID = "TUW-001";
    public static final String JOB_ID_MISSING_FROM_CUSTOM_DATA_ID = "TUW-002";
    public static final String FAILED_TO_GET_WORKER_TASK_DATA_ID = "TUW-003";
    public static final String FAILED_TO_READ_FROM_DATABASE_ID = "TUW-004";
    public static final String FAILED_TO_CONVERT_DATABASE_ROW_TO_TASK_MESSAGE_ID = "TUW-005";
    public static final String FAILED_TO_DELETE_UNSTOWED_TASK_MESSAGE_FROM_DATABASE_ID = "TUW-006";
    public static final String FAILED_TO_SEND_UNSTOWED_TASK_MESSAGE_TO_QUEUE_ID = "TUW-007";

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
