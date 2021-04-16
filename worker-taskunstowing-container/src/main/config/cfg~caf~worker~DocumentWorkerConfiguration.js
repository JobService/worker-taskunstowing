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
({
    workerName: "worker-taskunstowing",
    workerVersion: "${project.version}",
    outputQueue: getenv("CAF_WORKER_OUTPUT_QUEUE") || undefined,
    failureQueue: getenv("CAF_WORKER_FAILURE_QUEUE") ||
        (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-err",
    threads: getenv("CAF_WORKER_THREADS") || 1,
    maxBatchSize: getenv("CAF_WORKER_MAX_BATCH_SIZE") || undefined,
    maxBatchTime: getenv("CAF_WORKER_MAX_BATCH_TIME") || undefined,
    inputMessageProcessing: {
        documentTasksAccepted: undefined,
        fieldEnrichmentTasksAccepted: undefined,
        processSubdocumentsSeparately: undefined
    },
    enableExceptionOnFailure: getenv("CAF_WORKER_ENABLE_EXCEPTION_ONFAILURE") || false,
    scriptCaching: {
        staticScriptCache: {
            maximumSize: getenv("CAF_WORKER_STATIC_SCRIPT_CACHE_SIZE") || undefined,
            expireAfterAccess: getenv("CAF_WORKER_STATIC_SCRIPT_CACHE_DURATION") || undefined
        },
        dynamicScriptCache: {
            maximumSize: getenv("CAF_WORKER_DYNAMIC_SCRIPT_CACHE_SIZE") || undefined,
            expireAfterWrite: getenv("CAF_WORKER_DYNAMIC_SCRIPT_CACHE_DURATION") || undefined
        }
    }
});
