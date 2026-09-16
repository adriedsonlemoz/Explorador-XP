package com.exploradorxp.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExplorerMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupWithoutBaselineProfile() = startupBenchmark(CompilationMode.None())

    @Test
    fun startupWithBaselineProfile() = startupBenchmark(
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require)
    )

    @Test
    fun scrollWithoutBaselineProfile() = scrollBenchmark(CompilationMode.None())

    @Test
    fun scrollWithBaselineProfile() = scrollBenchmark(
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require)
    )

    private fun startupBenchmark(compilationMode: CompilationMode) {
        val device = benchmarkDevice()
        prepareBenchmarkDataset(device)

        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait { intent ->
                intent.putExtra(EXTRA_START_PATH, BENCHMARK_DIR)
            }
            device.wait(Until.hasObject(By.text("bench_1.txt")), 10_000)
        }
    }

    private fun scrollBenchmark(compilationMode: CompilationMode) {
        val device = benchmarkDevice()
        prepareBenchmarkDataset(device)

        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            startupMode = null,
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait { intent ->
                    intent.putExtra(EXTRA_START_PATH, BENCHMARK_DIR)
                }
                device.wait(Until.hasObject(By.text("bench_1.txt")), 10_000)
            },
        ) {
            scrollExplorer(device)
        }
    }
}
