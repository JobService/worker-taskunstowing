# Task Unstowing Worker

The Task Unstowing Worker is a Document Worker used to read stowed tasks from a database, send them onto the queue named in the task's  
`to` field, and then delete them from the database.

## Input Message Format

The input message for this worker is expected to contain the following two `customData` properties, which the Worker uses to identify  
which tasks to unstow:

* `partitionId` - The identifier of the partition
* `jobId` - The identifier of the job

Example input message:

```json
{
    "version": 3,
    "taskId": "89dfb12d-5de0-4b40-85ec-835e33ea447d",
    "taskClassifier": "DocumentWorkerTask",
    "taskApiVersion": 1,
    "taskData": "ew0KICAiY3VzdG9tRGF0YSI6IHsNCiAgICAicGFydGl0aW9uSWQiOiAiYWNtZS1jb3JwIiwNCiAgICAiam9iSWQiOiAiam9iLTEiDQogIH0NCn0=",
    "taskStatus": "NEW_TASK",
    "context": {},
    "to": "worker-taskunstowing-in",
    "tracking": null,
    "sourceInfo": null,
    "priority": null,
    "correlationId": "1"
  }
```

and it's base64-decoded `taskData`:

```json
{
  "customData": {
    "partitionId": "acme-corp",
    "jobId": "job-1"
  }
}
```

## Configuration

### Environment Variables

#### Database Configuration

* `CAF_WORKER_TASKUNSTOWING_DATABASE_HOST`  
    **Default**: `localhost`  
    **Description**: The host name of the machine on which the PostgreSQL server is running.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_PORT`  
    **Default**: `5432`  
    **Description**: The TCP port on which the PostgreSQL server is listening for connections.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_NAME`  
    **Default**: `jobservice`  
    **Description**: The name of the PostgreSQL database from which read stowed task messages from.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_TABLENAME`  
    **Default**: `stowed_task`  
    **Description**: The name of the PostgreSQL database table from which to read stowed task messages from.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_USERNAME`  
    **Description**: The username to use when establishing the connection to the PostgreSQL server.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_PASSWORD`  
    **Description**: The password to use when establishing the connection to the PostgreSQL server.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_APPNAME`  
    **Default**: `worker-taskunstowing`  
    **Description**: The application name used for PostgreSQL logging and monitoring.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_MAXIMUM_POOL_SIZE`  
    **Default**: `5`  
    **Description**: The maximum size that the connection pool is allowed to reach, including both idle and in-use connections.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_MAXIMUM_BATCH_SIZE`  
    **Default**: `10`  
    **Description**: The maximum number of database rows that will be read into memory and processed before more rows are read.

#### Logging Configuration

* `CAF_LOG_LEVEL`  
    **Default**: `WARN`  
    **Description**: The log level for this worker.
