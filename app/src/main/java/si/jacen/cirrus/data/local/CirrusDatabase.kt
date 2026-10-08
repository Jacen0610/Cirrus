package si.jacen.cirrus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ObservationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CirrusDatabase : RoomDatabase() {

    abstract fun observationDao(): ObservationDao

    companion object {
        const val NAME = "cirrus.db"
    }
}
