# Task Unstowing Worker

The Task Unstowing Worker is used to stow tasks received on it's input queue to a specified database table.

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
    **Default**: `postgres`  
    **Description**: The username to use when establishing the connection to the PostgreSQL server.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_PASSWORD`  
    **Default**: `postgres`  
    **Description**: The password to use when establishing the connection to the PostgreSQL server.

* `CAF_WORKER_TASKUNSTOWINGG_DATABASE_APPNAME`  
    **Default**: `worker_taskunstowing`  
    **Description**: The application name used for PostgreSQL logging and monitoring.

* `CAF_WORKER_TASKUNSTOWING_DATABASE_MAXIMUM_POOL_SIZE`  
    **Default**: `5`  
    **Description**: The maximum size that the connection pool is allowed to reach, including both idle and in-use connections.

#### Logging Configuration

* `CAF_LOG_LEVEL`  
    **Default**: `WARN`  
    **Description**: The log level for this worker.
