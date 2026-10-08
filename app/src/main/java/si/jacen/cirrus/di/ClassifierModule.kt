package si.jacen.cirrus.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import si.jacen.cirrus.data.classifier.DummyCloudClassifier
import si.jacen.cirrus.domain.classifier.CloudClassifier
import javax.inject.Singleton

/**
 * 识别实现的绑定开关：换实现只改这一处。
 * 未来加 MediaPipe / LiteRT 实现时，在此用 @Qualifier 或配置切换。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ClassifierModule {

    @Binds
    @Singleton
    abstract fun bindCloudClassifier(impl: DummyCloudClassifier): CloudClassifier
}
