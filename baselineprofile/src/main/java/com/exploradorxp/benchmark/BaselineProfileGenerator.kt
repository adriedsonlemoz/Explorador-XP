package com.exploradorxp.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    /**
     * Mantém o Startup Profile enxuto: somente o caminho até a primeira pasta ficar pronta.
     * Isso ajuda o R8 a organizar o DEX sem marcar o fluxo inteiro de rolagem como startup.
     */
    @Test
    fun startup() {
        val device = benchmarkDevice()
        prepareBenchmarkDataset(device)

        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
        ) {
            startActivityAndWait { intent ->
                intent.putExtra(EXTRA_START_PATH, BENCHMARK_DIR)
            }
            device.wait(Until.hasObject(By.text("bench_1.txt")), 10_000)
        }
    }

    /**
     * Adiciona ao Baseline Profile o caminho crítico que motivou esta otimização:
     * carregar uma pasta grande e percorrer a lista. Ele não entra no Startup Profile.
     */
    @Test
    fun explorerJourney() {
        val device = benchmarkDevice()
        prepareBenchmarkDataset(device)

        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = false,
        ) {
            startActivityAndWait { intent ->
                intent.putExtra(EXTRA_START_PATH, BENCHMARK_DIR)
            }
            device.wait(Until.hasObject(By.text("bench_1.txt")), 10_000)
            scrollExplorer(device)
        }
    }
}
