package sung.gyun.inappupdatesample

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import androidx.activity.viewModels


class MainActivity : AppCompatActivity() {
    //새버전의 프로그램이 존재하는지 여부
    private var newver = 0
    private var oldver = 0
    private var strVer: String? = null

    private var fileName: String? = null

    // Progress Dialog
    private var textVersion: TextView? = null

    private var progressTitle: TextView? = null
    private var progressText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var downloadUpdateLayout: RelativeLayout? = null

    val model: DownloadViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        progressText = findViewById<View>(R.id.progressText) as TextView
        progressTitle = findViewById<View>(R.id.progressTitle) as TextView
        downloadUpdateLayout = findViewById<View>(R.id.layoutDownloadUpdate) as RelativeLayout
        textVersion = findViewById<View>(R.id.textVersion) as TextView

        model._updateInfo.observe(this) { updateInfo ->
            checkForUpdate(updateInfo)
        }
        model._downloadState.observe(this) { state ->
            Log.e("_downloadState", "$state")
            when (state.downloadState) {
                Status.STARTING -> {
                    Log.d(MSG_TAG, "프로그레스바 시작")
                    progressBar!!.isIndeterminate = true
                    val title = "다운로드"
                    progressTitle!!.text = title
                    progressText!!.text = "시작 중..."
                    downloadUpdateLayout!!.visibility = View.VISIBLE
                }
                Status.PROGRESS -> {
                    progressBar!!.isIndeterminate = false
                    val downloadingSize = "${state.progress}k /${state.fileSize}k"
                    progressText!!.text = downloadingSize
                    val downloadMsg = "$fileName 로 다운로드 중입니다."
                    progressTitle!!.text = downloadMsg
                    progressBar!!.progress = (state.progress * 100 / state.fileSize).toInt()
                }
                else -> {
                    Log.d(MSG_TAG, "다운로드 완료.")
                    progressText!!.text = ""
                    progressTitle!!.text = ""
                    downloadUpdateLayout!!.visibility = View.GONE

                    //다운로드 받은 패키지를 인스톨한다.
                    val apkFile = File(getDownloadDirectory() + fileName)
                    executeFile(applicationContext, apkFile)

                    /*
                     * 안드로이드 프로세스는  단지 finish() 만 호출 하면 죽지 않는다.
                     * 만약 프로세스를 강제로 Kill 하기위해서는 화면에 떠있는 Activity를 BackGround로 보내고
                     * 강제로 Kill하면 프로세스가  완전히 종료가 된다.
                     * 종료 방법에 대한 Source는 아래 부분을 참조 하면 될것 같다.
                     */
                    moveTaskToBack(true)
                    finish()
                    Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL)
                }
            }
        }
        model.getUpdateInfo(UPDATE_INFO_JSON)
        try {
            strVer = BuildConfig.VERSION_NAME
        } catch (e1: PackageManager.NameNotFoundException) {
            e1.printStackTrace()
        }
        val verText = "버전 $strVer, 현재 시간: ${
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREAN).format(Date())
        }"
        textVersion!!.text = verText

        // 파일 및 미디어 접근 권한 요청
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0
            )
        }
    }

    //앱 업데이트 검사
    private fun checkForUpdate(updateInfo: UpdateInfo?) {
        updateInfo?.let {
            newver = it.versionCode
            try {
                //설치된 앱 정보 얻기
                oldver = BuildConfig.VERSION_CODE

                //다운로드 폴더 얻어오기
                val localpath = getDownloadDirectory()
                Log.d("앱버전", "앱버전 : $oldver")
                Log.d("서버상", "서버에 있는 파일 버전 : $newver")
                Log.d("받아온경로", "받아온 경로: $localpath")
                if (oldver < newver) {    //파일 버전비교
                    this.runOnUiThread {
                        openUpdateDialog(
                            "${DownloadApi.IP_ADDRESS}${it.fileName}",
                            it.fileName,
                            it.message,
                            it.title,
                            localpath
                        )
                    }
                    // 동일한 .apk 파일이 존재할 경우 삭제
                    deleteInstallFile(it.fileName, getDownloadDirectory())
                } else {
                    Log.d(MSG_TAG, " 최신버전입니다. 버전 : ${it.versionName}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //다운로드 폴더 얻기
    private fun getDownloadDirectory(): String {
        val sdcardPath: String =
            if (isUsableSDCard()) {    //외장메모리 사용가능할 경우
                Environment.getExternalStorageDirectory().path
            } else {                       //내장메모리 위치
                val file: File = Environment.getRootDirectory()
                file.absolutePath
            }
        return "$sdcardPath/Download/"
    }

    //외장메모리 사용 가능여부 확인
    private fun isUsableSDCard(): Boolean {
        val state: String = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            return true
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            return true
        }
        return false
    }

    //업데이트 할 것인지 확인
    private fun openUpdateDialog(
        downloadFileUrl: String, fileName: String?,
        message: CharSequence?, updateTitle: String?, localpath4down: String
    ) {
        val li = LayoutInflater.from(this)
        val dialog: AlertDialog.Builder
        val view: View = li.inflate(R.layout.updateview, null)
        val messageView = view.findViewById(R.id.updateMessage) as TextView
        val updateNowText = view.findViewById(R.id.updateNowText) as TextView
        if (fileName!!.isEmpty()) // No filename, hide 'download now?' string
            updateNowText.visibility = View.GONE
        messageView.text = message
        dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle(updateTitle)
            .setView(view)
        if (fileName.isNotEmpty()) {
            // Display Yes/No for if a filename is available.
            dialog.setNeutralButton(
                "No"
            ) { _, _ ->
                Log.d(
                    MSG_TAG,
                    "No pressed"
                )
            }
            dialog.setNegativeButton(
                "YES"
            ) { _, _ ->
                Log.d(MSG_TAG, "Yes pressed")
                Log.d("경로명", "경로명 : $downloadFileUrl 파일명 : $fileName")
                downloadUpdate(fileName, localpath4down)
            }
        } else {
            dialog.setNeutralButton(
                "확인"
            ) { _, _ ->
                Log.d(
                    MSG_TAG,
                    "Ok pressed"
                )
            }
        }
        dialog.show()
    }

    //다운로드 받은 앱을 설치, 이전 실행 앱 종료
    private fun downloadUpdate(fileName: String?, localpath: String) {
        Log.d("downloadUpdate", "경로1:$localpath$fileName")
        //viewUpdateHandler.sendMessage(msg)
        downloadUpdateFile(fileName, localpath)
    }

    private fun deleteInstallFile(
        destinationFilename: String?,
        localPath: String
    ): Boolean {
        val downloadFile = File(localPath + destinationFilename)
        if (downloadFile.exists()) {
            return downloadFile.delete()
        }
        return false
    }

    private fun downloadUpdateFile(
        destinationFilename: String?,
        localPath: String
    ) {
        // isExternalStorageWritable
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return
        }
        val downloadDir = File(localPath)
        if (!downloadDir.exists()) downloadDir.mkdirs()
        else {
            val downloadFile = File(localPath + destinationFilename)
            if (downloadFile.exists()) downloadFile.delete()
        }

        model.downloadFile(destinationFilename!!, localPath)
    }

    private fun executeFile(
        context: Context,
        file: File
    ) {
        val uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, PACKAGE_ARCHIVE)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
        )
    }


    companion object {

        const val MSG_TAG = "MainActivity"

        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"

        // apk 확장자의 경우 아래 MIME 형식과 연결되어 있다.
        private const val PACKAGE_ARCHIVE = "application/vnd.android.package-archive"

        // Update 정보가 담긴 json 파일 명
        private const val UPDATE_INFO_JSON = "update_info.json"

    }
}