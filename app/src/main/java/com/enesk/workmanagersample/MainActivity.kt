package com.enesk.workmanagersample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.compose.rememberImagePainter
import com.enesk.workmanagersample.ui.theme.WorkManagerSampleTheme
import com.enesk.workmanagersample.util.WorkerKeys

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .build()
        val workManager = WorkManager.getInstance(applicationContext)

        setContent {
            WorkManagerSampleTheme {
                val workInfos = workManager
                    .getWorkInfosForUniqueWorkLiveData("download")
                    .observeAsState()
                    .value

                val downloadInfo = remember(key1 = workInfos) {
                    workInfos?.find {
                        it.id == downloadRequest.id
                    }
                }

                val filterInfo = remember(key1 = workInfos) {
                    workInfos?.find {
                        it.id == colorFilterRequest.id
                    }
                }

                val imageUri by derivedStateOf {
                    val downloadUri = downloadInfo?.outputData?.getString(WorkerKeys.IMAGE_URI)
                        ?.toUri()
                    val filterUri = filterInfo?.outputData?.getString(WorkerKeys.FILTER_URI)
                        ?.toUri()
                    filterUri ?: downloadUri
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageUri?.let {
                        Image(
                            painter = rememberImagePainter(data = it),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            workManager
                                .beginUniqueWork(
                                    "download",
                                    ExistingWorkPolicy.KEEP,
                                    downloadRequest
                                )
                                .then(colorFilterRequest)
                                .enqueue()
                        },
                        enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Start download")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (downloadInfo?.state) {
                        WorkInfo.State.RUNNING -> Text(text = "Downloading..")
                        WorkInfo.State.SUCCEEDED -> Text(text = "Download Succeeded")
                        WorkInfo.State.FAILED -> Text(text = "Download failed")
                        WorkInfo.State.CANCELLED -> Text(text = "Download cancelled")
                        WorkInfo.State.ENQUEUED -> Text(text = "Download enqueued")
                        WorkInfo.State.BLOCKED -> Text(text = "Download blocked")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (filterInfo?.state) {
                        WorkInfo.State.RUNNING -> Text(text = "Applying filter..")
                        WorkInfo.State.SUCCEEDED -> Text(text = "Filter Succeeded")
                        WorkInfo.State.FAILED -> Text(text = "Filter failed")
                        WorkInfo.State.CANCELLED -> Text(text = "Filter cancelled")
                        WorkInfo.State.ENQUEUED -> Text(text = "Filter enqueued")
                        WorkInfo.State.BLOCKED -> Text(text = "Filter blocked")
                    }
                }
            }
        }
    }
}

