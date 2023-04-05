package sung.gyun.inappupdatesample

import android.os.Message
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DownloadViewModel : ViewModel() {

    private var downloadRepository: DownloadRepository =
        DownloadRepository(DownloadApi.getInstance())

    private var downloadState: MutableLiveData<DownloadState> = MutableLiveData()
    val _downloadState get() = downloadState

    private var updateInfo: MutableLiveData<UpdateInfo> = MutableLiveData()
    val _updateInfo get() = updateInfo

    fun downloadFile(filename: String, destinationDirectory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val body = downloadRepository.downloadFile(filename)!!
            downloadState.postValue(
                DownloadState(
                    downloadState = Status.STARTING,
                    0L
                )
            )
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded = 0

                input = body.byteStream()
                output = FileOutputStream(File(destinationDirectory + filename))
                while (true) {
                    val read = input.read(fileReader)
                    if (read == -1) break

                    fileSizeDownloaded += read
                    output.write(fileReader, 0, read)
                    downloadState.postValue(
                        DownloadState(
                            downloadState = Status.PROGRESS,
                            progress = fileSizeDownloaded.toLong(),
                            fileSize = fileSize
                        )
                    )
                }
                output.flush()

            } catch (e: Exception) {
            } finally {
                downloadState.postValue(
                    DownloadState(
                        downloadState = Status.COMPLETE,
                        100L
                    )
                )
                input?.close()
                output?.close()
            }
        }
    }

    fun getUpdateInfo(filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = downloadRepository.downloadUpdateInfo(filename)
            if (res != null) {
                updateInfo.postValue(res)
            }
        }
    }

}