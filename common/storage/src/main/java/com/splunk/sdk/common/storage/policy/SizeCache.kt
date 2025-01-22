/*
 * Copyright 2025 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.sdk.common.storage.policy

import com.cisco.android.common.logger.Logger
import com.cisco.android.common.utils.extensions.safeSubmit
import com.cisco.android.common.utils.thread.NamedThreadFactory
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.seconds

internal object SizeCache {

    private const val TAG = "SizeCache"
    private const val NON_EXISTING_DIR_SIZE = 0L

    private val CACHE_MAX_AGE = 30.seconds.inWholeMilliseconds

    private val cache = hashMapOf<String, SizeCacheEntry>()
    private var runningCalculation: Future<*>? = null
    private val calculationService: ExecutorService by lazy {
        Executors.newSingleThreadExecutor(NamedThreadFactory("fsize"))
    }

    // TODO: dirSize should be called from a background thread
    fun dirSize(dir: File): Long {
        if (!dir.exists()) {
            return NON_EXISTING_DIR_SIZE
        }

        val entry = cache[dir.path]

        return if (entry != null && entry.timestamp.age <= CACHE_MAX_AGE) {
            if (runningCalculation?.isDone != false) {
                runningCalculation = calculationService.safeSubmit {
                    runCatching {
                        cache[dir.path] = SizeCacheEntry(calculateDirSize(dir))
                    }.onFailure {
                        Logger.e(TAG, "Failed to calculate dir size")
                    }
                }
            }
            entry.size
        } else {
            val size = calculateDirSize(dir)
            cache[dir.path] = SizeCacheEntry(size)
            size
        }
    }

    private fun calculateDirSize(dir: File): Long = dir.walkTopDown().sumOf { it.length() }

    private val Long.age: Long
        get() = System.currentTimeMillis() - this

    private data class SizeCacheEntry(
        val size: Long,
        val timestamp: Long = System.currentTimeMillis()
    )
}
