package com.exploradorxp.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

internal const val TARGET_PACKAGE = "com.exploradorxp.app"
internal const val EXTRA_START_PATH = "com.exploradorxp.app.extra.PERFORMANCE_START_PATH"
internal const val BENCHMARK_DIR = "/sdcard/Download/ExploradorXP-Benchmark"

internal fun benchmarkDevice(): UiDevice =
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

internal fun prepareBenchmarkDataset(device: UiDevice) {
    device.executeShellCommand("appops set $TARGET_PACKAGE MANAGE_EXTERNAL_STORAGE allow")
    device.executeShellCommand(
        "rm -rf $BENCHMARK_DIR; mkdir -p $BENCHMARK_DIR/subfolder; " +
            "i=1; while [ \$i -le 600 ]; do echo benchmark > $BENCHMARK_DIR/bench_\$i.txt; i=\$((i+1)); done; " +
            "i=1; while [ \$i -le 40 ]; do mkdir -p $BENCHMARK_DIR/folder_\$i; i=\$((i+1)); done"
    )
}

internal fun scrollExplorer(device: UiDevice, passes: Int = 6) {
    val x = device.displayWidth / 2
    val top = (device.displayHeight * 0.25f).toInt()
    val bottom = (device.displayHeight * 0.78f).toInt()
    repeat(passes) { device.swipe(x, bottom, x, top, 18) }
    repeat(passes / 2) { device.swipe(x, top, x, bottom, 18) }
}
