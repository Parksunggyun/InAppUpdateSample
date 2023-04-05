package sung.gyun.inappupdatesample

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadService {

    @Streaming
    @GET
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>
    @Streaming
    @GET
    fun downloadUpdateInfo(@Url fileUrl: String): Call<UpdateInfo>

}