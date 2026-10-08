package si.jacen.cirrus.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import si.jacen.cirrus.data.local.ObservationDao
import si.jacen.cirrus.data.local.ObservationEntity
import si.jacen.cirrus.data.local.PhotoFileStore
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.domain.repository.ObservationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservationRepositoryImpl @Inject constructor(
    private val observationDao: ObservationDao,
    private val photoFileStore: PhotoFileStore,
) : ObservationRepository {

    override fun observeObservations(): Flow<List<Observation>> =
        observationDao.observeAll().map { entities -> entities.map(ObservationEntity::toDomain) }

    override suspend fun addObservation(observation: Observation): Long =
        observationDao.insertObservation(observation.toEntity())

    override suspend fun findObservation(id: Long): Observation? =
        observationDao.getById(id)?.toDomain()

    override suspend fun deleteObservation(id: Long) {
        val entity = observationDao.getById(id)
        observationDao.deleteById(id)
        entity?.photoFileName?.let(photoFileStore::delete)
    }
}

private fun ObservationEntity.toDomain() = Observation(
    id = id,
    timestampMillis = timestampMillis,
    cloudLabel = cloudLabel,
    confidence = confidence,
    photoFileName = photoFileName,
    userCorrectedLabel = userCorrectedLabel,
    latitude = latitude,
    longitude = longitude,
)

private fun Observation.toEntity() = ObservationEntity(
    id = id,
    timestampMillis = timestampMillis,
    cloudLabel = cloudLabel,
    confidence = confidence,
    photoFileName = photoFileName,
    userCorrectedLabel = userCorrectedLabel,
    latitude = latitude,
    longitude = longitude,
)
