package si.jacen.cirrus.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import si.jacen.cirrus.data.export.CorrectionExport
import si.jacen.cirrus.data.export.CorrectionExporter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @Singleton
    abstract fun bindCorrectionExport(impl: CorrectionExporter): CorrectionExport
}
