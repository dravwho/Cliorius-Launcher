package net.kdt.pojavlaunch.firefly.benchmark

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firefly.utils.PGWTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.firefly.R
import net.kdt.pojavlaunch.firefly.Tools
import net.kdt.pojavlaunch.firefly.mobileglues.DeviceInfoProvider
import net.kdt.pojavlaunch.firefly.mobileglues.MGBench
import net.kdt.pojavlaunch.firefly.mobileglues.MgQuery
import net.kdt.pojavlaunch.firefly.mobileglues.settings.MultidrawBenchReport
import net.kdt.pojavlaunch.firefly.mobileglues.utils.Constants
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences
import java.io.File
import java.util.Locale

/**
 * Halaman Benchmark Cliorius.
 *
 * Menguji tiga hal yang benar-benar dipakai saat game jalan:
 *  1. GPU & GLES — dibaca dari driver lewat EGL context sungguhan.
 *  2. RAM — mengukur berapa memori yang benar-benar bisa dipesan proses ini,
 *     dibandingkan dengan alokasi yang akan diberikan ke JVM (-Xmx).
 *  3. Renderer — menjalankan micro-benchmark MultiDraw milik renderer
 *     (jalur yang sama dengan halaman MobileGlues), lalu menampilkan
 *     biaya per backend dalam mikrodetik.
 *
 * Semua pengukuran berjalan di background thread; UI hanya menampilkan hasil.
 */
class BenchmarkFragment : Fragment(R.layout.fragment_benchmark) {

    companion object {
        const val TAG = "BENCHMARK_FRAGMENT"
    }

    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var runButton: Button
    private lateinit var deviceGpu: TextView
    private lateinit var deviceGles: TextView
    private lateinit var deviceRam: TextView
    private lateinit var deviceRenderer: TextView
    private lateinit var ramResult: TextView
    private lateinit var benchResult: TextView

    private var running = false
    private lateinit var appContext: android.content.Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appContext = requireContext().applicationContext

        status = view.findViewById(R.id.bench_status)
        progress = view.findViewById(R.id.bench_progress)
        runButton = view.findViewById(R.id.bench_run_button)
        deviceGpu = view.findViewById(R.id.bench_device_gpu)
        deviceGles = view.findViewById(R.id.bench_device_gles)
        deviceRam = view.findViewById(R.id.bench_device_ram)
        deviceRenderer = view.findViewById(R.id.bench_device_renderer)
        ramResult = view.findViewById(R.id.bench_ram_result)
        benchResult = view.findViewById(R.id.bench_renderer_result)

        view.findViewById<Button>(R.id.bench_back_button).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        runButton.setOnClickListener { startBenchmark() }

        showStaticInfo()
    }

    /** Info perangkat yang bisa dibaca tanpa menjalankan benchmark. */
    private fun showStaticInfo() {
        val ctx = requireContext()
        deviceRenderer.text = getString(
            R.string.bench_renderer_current,
            LauncherPreferences.PREF_RENDERER,
        )
        deviceRam.text = getString(
            R.string.bench_ram_device,
            Tools.getTotalDeviceMemory(ctx),
            Tools.getFreeDeviceMemory(ctx),
            LauncherPreferences.PREF_RAM_ALLOCATION,
        )

        lifecycleScope.launch {
            val info = runCatching { DeviceInfoProvider.get(ctx) }.getOrNull()
            deviceGpu.text = getString(
                R.string.bench_gpu_value,
                info?.gpuRenderer?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.bench_unknown),
            )
            deviceGles.text = getString(
                R.string.bench_gles_value,
                info?.glesVersion?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.bench_unknown),
            )
        }
    }

    private fun startBenchmark() {
        if (running) return
        running = true
        runButton.isEnabled = false
        progress.visibility = View.VISIBLE
        progress.progress = 0
        benchResult.text = ""
        ramResult.text = ""
        status.text = getString(R.string.bench_status_running)

        lifecycleScope.launch {
            // 1) Uji RAM
            status.text = getString(R.string.bench_status_ram)
            progress.progress = 15
            val ram = withContext(Dispatchers.IO) { measureRam() }
            ramResult.text = ram

            // 2) Uji renderer (micro-benchmark MultiDraw)
            status.text = getString(R.string.bench_status_renderer)
            progress.progress = 40
            val rendererText = withContext(Dispatchers.IO) { measureRenderer() }
            benchResult.text = rendererText

            progress.progress = 100
            status.text = getString(R.string.bench_status_done)
            progress.visibility = View.GONE
            runButton.isEnabled = true
            running = false
        }
    }

    /**
     * Mengukur memori yang benar-benar bisa dipesan proses ini.
     *
     * Target = alokasi RAM yang akan diberikan ke JVM. Kalau proses tidak sanggup
     * memesan sebesar itu, artinya nilai -Xmx di Settings terlalu besar untuk
     * perangkat ini dan game akan gagal start / di-kill sistem.
     */
    private fun measureRam(): String {
        val targetMb = LauncherPreferences.PREF_RAM_ALLOCATION
        val chunkMb = 8
        val chunk = ByteArray(chunkMb * 1024 * 1024)
        val blocks = ArrayList<ByteArray>()
        var allocatedMb = 0
        val started = System.currentTimeMillis()
        var failed = false

        try {
            while (allocatedMb < targetMb) {
                // sentuh halaman supaya benar-benar dipesan (bukan lazy)
                var i = 0
                while (i < chunk.size) { chunk[i] = 1; i += 4096 }
                blocks.add(chunk.copyOf())
                allocatedMb += chunkMb
            }
        } catch (e: OutOfMemoryError) {
            failed = true
        } catch (e: Throwable) {
            failed = true
        }

        val elapsed = System.currentTimeMillis() - started
        blocks.clear()

        val runtime = Runtime.getRuntime()
        val maxMb = runtime.maxMemory() / (1024 * 1024)
        val totalMb = runtime.totalMemory() / (1024 * 1024)
        val freeMb = runtime.freeMemory() / (1024 * 1024)

        return buildString {
            appendLine(getString(R.string.bench_ram_target, targetMb))
            appendLine(getString(R.string.bench_ram_allocated, allocatedMb, elapsed))
            appendLine(getString(R.string.bench_ram_heap, totalMb, maxMb, freeMb))
            append(
                if (failed) getString(R.string.bench_ram_limited)
                else getString(R.string.bench_ram_ok)
            )
        }
    }

    /**
     * Menjalankan micro-benchmark MultiDraw milik renderer lewat jalur resmi
     * (MgQuery → proses :mgquery sekali pakai), lalu merangkum biaya per backend.
     */
    private fun measureRenderer(): String {
        val mgDir = runCatching { File(Constants.MG_DIRECTORY) }.getOrNull()
            ?: return getString(R.string.bench_renderer_no_dir)

        val raw = runCatching {
            kotlinx.coroutines.runBlocking {
                MgQuery(appContext).use { query ->
                    query.runBench(mgDir.path, "", 0, 0)
                }
            }
        }.getOrElse { e ->
            return getString(R.string.bench_renderer_error, e.message ?: e.javaClass.simpleName)
        }

        val report = MultidrawBenchReport.parse(raw)
        if (report.error != null) {
            return getString(R.string.bench_renderer_error, report.error)
        }
        if (report.timings.isEmpty()) {
            return getString(R.string.bench_renderer_empty)
        }

        return buildString {
            report.renderer?.let { appendLine(getString(R.string.bench_renderer_name, it)) }
            appendLine(getString(R.string.bench_renderer_elapsed, report.elapsedMs.toLong()))
            if (report.sections > 0) {
                appendLine(getString(R.string.bench_renderer_sections, report.sections))
            }
            appendLine()
            // backend tercepat rata-rata di seluruh fungsi
            val perBackend = LinkedHashMap<String, MutableList<Double>>()
            report.timings.forEach { (_, backends) ->
                backends.forEach { (backend, micros) ->
                    perBackend.getOrPut(backend.key) { ArrayList() }.add(micros)
                }
            }
            val ranked = perBackend.mapValues { (_, list) -> list.average() }
                .toList().sortedBy { it.second }

            ranked.forEachIndexed { index, (key, avg) ->
                appendLine(
                    getString(
                        R.string.bench_renderer_row,
                        index + 1,
                        key,
                        String.format(Locale.US, "%.1f", avg),
                    )
                )
            }
            appendLine()
            append(getString(R.string.bench_renderer_note))
        }
    }
}
