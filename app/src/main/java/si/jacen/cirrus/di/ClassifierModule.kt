package si.jacen.cirrus.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import si.jacen.cirrus.data.classifier.DummyCloudClassifier
import si.jacen.cirrus.data.classifier.LitertCloudClassifier
import si.jacen.cirrus.domain.classifier.CloudClassifier
import javax.inject.Qualifier
import javax.inject.Singleton

/** 换识别引擎只改 @Binds 注解上的限定符，调用方零感知。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ClassifierModule {

    @Binds
    @Singleton
    @LitertEngine
    abstract fun bindLitertCloudClassifier(impl: LitertCloudClassifier): CloudClassifier

    @Binds
    @Singleton
    @DummyEngine
    abstract fun bindDummyCloudClassifier(impl: DummyCloudClassifier): CloudClassifier
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LitertEngine

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DummyEngine

