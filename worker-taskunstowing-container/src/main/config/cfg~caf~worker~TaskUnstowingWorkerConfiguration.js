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
({
    databaseHost: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_HOST") || "localhost",
    databasePort: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_PORT") || 5432,
    databaseName: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_NAME") || "jobservice",
    databaseTableName: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_TABLENAME") || "stowed_task",
    databaseUsername: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_USERNAME"),
    databasePassword: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_PASSWORD"),
    databaseAppName: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_APPNAME") || "worker-taskunstowing",
    databaseMaximumPoolSize: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_MAXIMUM_POOL_SIZE") || 5,
    databaseMaximumBatchSize: getenv("CAF_WORKER_TASKUNSTOWING_DATABASE_MAXIMUM_BATCH_SIZE") || 10,
    rabbitMQHost: getenv("CAF_RABBITMQ_HOST") || "localhost",
    rabbitMQPort: getenv("CAF_RABBITMQ_PORT") || 5432,
    rabbitMQUsername: getenv("CAF_RABBITMQ_USERNAME") || "guest",
    rabbitMQPassword: getenv("CAF_RABBITMQ_PASSWORD") || "guest"
});
