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
