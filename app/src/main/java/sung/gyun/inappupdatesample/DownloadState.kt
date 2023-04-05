package sung.gyun.inappupdatesample

data class DownloadState(
    var downloadState: Status,
    var progress: Long = 0L,
    var fileSize: Long = 0L
)

enum class Status {
    STARTING,
    PROGRESS,
    COMPLETE
}