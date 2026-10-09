package si.jacen.cirrus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import si.jacen.cirrus.ui.camera.CameraScreen
import si.jacen.cirrus.ui.diary.DiaryScreen
import si.jacen.cirrus.ui.diary.ObservationDetailScreen
import si.jacen.cirrus.ui.result.ResultScreen

object Routes {
    const val ARG_PHOTO_FILE_NAME = "photoFileName"
    const val ARG_OBSERVATION_ID = "id"

    const val CAMERA = "camera"
    const val RESULT = "result/{$ARG_PHOTO_FILE_NAME}"
    const val DIARY = "diary"
    const val OBSERVATION = "observation/{$ARG_OBSERVATION_ID}"
    const val BENCHMARK = "benchmark"

    fun result(photoFileName: String) = "result/$photoFileName"
    fun observation(id: Long) = "observation/$id"
}

@Composable
fun CirrusNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA,
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onPhotoCaptured = { fileName ->
                    navController.navigate(Routes.result(fileName))
                },
                onOpenDiary = { navController.navigate(Routes.DIARY) },
                onOpenBenchmark = { navController.navigate(Routes.BENCHMARK) },
            )
        }

        composable(Routes.BENCHMARK) {
            si.jacen.cirrus.ui.benchmark.BenchmarkScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument(Routes.ARG_PHOTO_FILE_NAME) { type = NavType.StringType },
            ),
        ) {
            ResultScreen(
                onSaved = {
                    navController.navigate(Routes.DIARY) {
                        popUpTo(Routes.CAMERA)
                    }
                },
                onDiscard = { navController.popBackStack() },
            )
        }

        composable(Routes.DIARY) {
            DiaryScreen(
                onBack = { navController.popBackStack() },
                onOpenObservation = { id ->
                    navController.navigate(Routes.observation(id))
                },
            )
        }

        composable(
            route = Routes.OBSERVATION,
            arguments = listOf(
                navArgument(Routes.ARG_OBSERVATION_ID) { type = NavType.LongType },
            ),
        ) {
            ObservationDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
