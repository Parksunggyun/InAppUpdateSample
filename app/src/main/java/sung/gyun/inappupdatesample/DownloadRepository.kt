package sung.gyun.inappupdatesample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

class DownloadRepository(
    private val downloadService: DownloadService
) {

    suspend fun downloadFile(filename: String): ResponseBody? {
        return withContext(Dispatchers.IO) {
            val res = downloadService.downloadFile(filename).execute()
            if(res.body() != null) {
                res.body()!!
            } else {
                null
            }
        }
    }

    suspend fun downloadUpdateInfo(filename: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            val res = downloadService.downloadUpdateInfo(filename).execute()
            if(res.body() != null) {
                res.body()!!
            } else {
                null
            }
        }
    }
}