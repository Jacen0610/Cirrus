package si.jacen.cirrus.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 观察照片的文件仓库：统一管理内部存储 photos/ 目录。
 * 数据库只存文件名，文件本体在这里落盘与销毁。
 */
@Singleton
class PhotoFileStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val photosDir: File = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }

    fun newPhotoFile(): File = File(photosDir, "${UUID.randomUUID()}$JPEG_SUFFIX")

    fun file(fileName: String): File = File(photosDir, fileName)

    /** 按需降采样解码；文件缺失或损坏时返回 null，由调用方决定降级路径。 */
    fun loadBitmap(fileName: String, maxDimension: Int): Bitmap? {
        val target = file(fileName)
        if (!target.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / (sampleSize * 2) >= maxDimension ||
            bounds.outHeight / (sampleSize * 2) >= maxDimension
        ) {
            sampleSize *= 2
        }

        return BitmapFactory.decodeFile(
            target.path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    fun delete(fileName: String?) {
        if (fileName == null) return
        file(fileName).delete()
    }

    companion object {
        const val PHOTOS_DIR = "photos"
        private const val JPEG_SUFFIX = ".jpg"
    }
}
