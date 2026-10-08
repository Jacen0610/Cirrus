package si.jacen.cirrus.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import si.jacen.cirrus.data.local.CirrusDatabase
import si.jacen.cirrus.data.local.ObservationDao
import javax.inject.Singleton

/**
 * 教 Hilt 构造「构造函数搞不定」的对象：
 * CirrusDatabase 需要建造者模式 + Context，无法用 @Inject constructor 表达，
 * 因此用 @Provides 方法显式描述装配方式。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CirrusDatabase =
        Room.databaseBuilder(
            context = context,
            klass = CirrusDatabase::class.java,
            name = CirrusDatabase.NAME,
        ).build()

    @Provides
    fun provideObservationDao(database: CirrusDatabase): ObservationDao =
        database.observationDao()
}
