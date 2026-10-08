package si.jacen.cirrus.ui.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import si.jacen.cirrus.data.local.PhotoFileStore
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val photoStore: PhotoFileStore,
) : ViewModel() {

    var capturing by mutableStateOf(false)
        private set

    /**
     * 触发拍照：ImageCapture 把 JPEG 直接写入 photos/ 目录，
     * 成功回调文件名，失败回调由界面提示。capturing 期间拒绝重复点击。
     */
    fun capture(
        context: Context,
        imageCapture: ImageCapture,
        onSaved: (fileName: String) -> Unit,
        onError: () -> Unit,
    ) {
        if (capturing) return
        capturing = true

        val file = photoStore.newPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturing = false
                    onSaved(file.name)
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    onError()
                }
            },
        )
    }
}
