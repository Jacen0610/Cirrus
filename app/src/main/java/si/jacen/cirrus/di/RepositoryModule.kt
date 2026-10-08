package si.jacen.cirrus.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import si.jacen.cirrus.data.repository.ObservationRepositoryImpl
import si.jacen.cirrus.domain.repository.ObservationRepository
import javax.inject.Singleton

/**
 * @Binds 与 @Provides 的分工：
 * 实现类自带 @Inject 构造函数（Hilt 已会造它），这里只是声明
 * 「要 ObservationRepository 接口时，给这个实现」的映射关系。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindObservationRepository(
        impl: ObservationRepositoryImpl,
    ): ObservationRepository
}
