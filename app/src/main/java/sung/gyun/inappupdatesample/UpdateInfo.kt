package sung.gyun.inappupdatesample

data class UpdateInfo(
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val versionName: String = BuildConfig.VERSION_NAME,
    val fileName: String? = null,
    val message: String? = null,
    val title: String? = null
)
