package si.jacen.cirrus.data.export

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import si.jacen.cirrus.data.local.PhotoFileStore
import si.jacen.cirrus.domain.model.Observation
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 修正数据导出：把用户改判过的观察（照片 + 修正标签）打包为 zip，
 * 直接可作为训练管线的微调数据——「使用中攒数据，导出后反哺训练」的飞轮落地。
 * 输出位置：应用专属外部目录（无需存储权限，可通过文件管理器访问）。
 */
@Singleton
class CorrectionExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoFileStore: PhotoFileStore,
) : CorrectionExport {

    /** 无可导出数据（没有任何修正记录）时返回 null。 */
    override suspend fun export(observations: List<Observation>): File? = withContext(Dispatchers.IO) {
        val corrected = observations.filter {
            it.userCorrectedLabel != null && it.photoFileName != null
        }
        if (corrected.isEmpty()) return@withContext null

        val exportDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "exports",
        ).apply { mkdirs() }
        val zipFile = File(exportDir, "cirrus-corrections-${System.currentTimeMillis()}.zip")

        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("labels.csv"))
            zip.bufferedWriter().use { writer ->
                writer.write("photo,label\n")
                corrected.forEach { observation ->
                    writer.write("${observation.photoFileName},${observation.userCorrectedLabel}\n")
                }
            }
            zip.closeEntry()

            corrected.forEach { observation ->
                val photo = photoFileStore.file(observation.photoFileName!!)
                if (photo.exists()) {
                    zip.putNextEntry(ZipEntry("images/${observation.photoFileName}"))
                    photo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        zipFile
    }
}
