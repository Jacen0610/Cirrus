package si.jacen.cirrus.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import si.jacen.cirrus.R

/** 品牌主页：应用说明 + 拍摄入口。相机在此页不初始化，权限由用户点击进入拍摄页时再请求。 */
@Composable
fun HomeScreen(onStartCapture: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 品牌图：与启动页共用 brand_logo 资源，替换该文件即可全局换图（见 README）
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.cirrus_sky)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.home_feature_capture))
            Text(stringResource(R.string.home_feature_diary))
            Text(stringResource(R.string.home_feature_offline))
        }

        Button(
            onClick = onStartCapture,
            modifier = Modifier.padding(top = 32.dp),
        ) {
            Text(stringResource(R.string.action_start_capture))
        }
    }
}
