package sung.gyun.inappupdatesample

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DownloadApi {

    companion object {
        const val IP_ADDRESS = "ftp_ip_address/"

        fun getInstance(): DownloadService = Retrofit.Builder()
            .baseUrl(IP_ADDRESS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DownloadService::class.java)

    }

}