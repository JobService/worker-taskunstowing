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
package com.github.jobservice.workers.taskunstowing.database;

public final class StowedTaskColumnName
{
    public static final String ID = "id";
    public static final String PARTITION_ID = "partition_id";
    public static final String JOB_ID = "job_id";
    public static final String TASK_CLASSIFIER = "task_classifier";
    public static final String TASK_API_VERSION = "task_api_version";
    public static final String TASK_DATA = "task_data";
    public static final String TASK_STATUS = "task_status";
    public static final String CONTEXT = "context";
    public static final String TO = "to";
    public static final String TRACKING_INFO = "tracking_info";
    public static final String SOURCE_INFO = "source_info";
    public static final String CORRELATION_ID = "correlation_id";

    private StowedTaskColumnName()
    {
    }
}
