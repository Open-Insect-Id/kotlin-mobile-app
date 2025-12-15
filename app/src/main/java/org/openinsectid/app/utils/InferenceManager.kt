package org.openinsectid.app.utils

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import org.json.JSONObject
import java.io.InputStream
import kotlin.collections.iterator

object InferenceManager {

    private var ortSession: OrtSession? = null
    private var ordreClasses: List<String> = emptyList()
    private var famillesClasses: List<String> = emptyList()
    private var genreClasses: List<String> = emptyList()
    private var especesClasses: List<String> = emptyList()


    fun init(context: Context) {
        if (ortSession != null) return  // already initialized

        val env = OrtEnvironment.getEnvironment()
        val modelBytes = context.assets.open("insect_model.onnx").readBytes()
        ortSession = env.createSession(modelBytes)

        val hierarchyJson =
            context.assets.open("hierarchy_map.json").bufferedReader().use { it.readText() }
        val hierarchy = JSONObject(hierarchyJson).getJSONObject("hierarchy_map")

        val ordres = mutableSetOf<String>()
        val familles = mutableSetOf<String>()
        val genres = mutableSetOf<String>()
        val especes = mutableSetOf<String>()

        for (key in hierarchy.keys()) {
            val value = hierarchy.getJSONObject(key)
            ordres.add(value.getString("ordre"))
            familles.add(value.getString("famille"))
            genres.add(value.getString("genre"))
            especes.add(value.getString("espece"))
        }

        ordreClasses = ordres.sorted()
        famillesClasses = familles.sorted()
        genreClasses = genres.sorted()
        especesClasses = especes.sorted()
    }


    fun runInference(bitmap: Bitmap): Map<String, String> {
        val session = ortSession ?: error("InferenceManager not initialized")

        // Exact Python preprocessing
        val resizedBitmap = bitmap.scale(224, 224)
        val pixels = IntArray(224 * 224)
        resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        val inputArray = Array(3) { Array(224) { FloatArray(224) } }
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = pixels[y * 224 + x]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputArray[0][y][x] = (r - 0.485f) / 0.229f
                inputArray[1][y][x] = (g - 0.456f) / 0.224f
                inputArray[2][y][x] = (b - 0.406f) / 0.225f
            }
        }

        val flatInput = FloatArray(3 * 224 * 224)
        var idx = 0
        for (c in 0 until 3) for (y in 0 until 224) for (x in 0 until 224) {
            flatInput[idx++] = inputArray[c][y][x]
        }

        val inputName = session.inputNames.first()
        val inputBuffer = java.nio.FloatBuffer.wrap(flatInput)
        val inputTensor = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            inputBuffer,
            longArrayOf(1, 3, 224, 224)
        )
        val inputs = mapOf(inputName to inputTensor)

        // Returns OrtSession.Result with ALL outputs
        val outputs = session.run(inputs)

        // Get output names from session (like Python: [output.name for output in session.get_outputs()])
        val outputNames = session.outputNames.toList()  // ["ordre", "famille", "genre", "espece"]

        val predictions = mutableMapOf<String, String>()

        // Map output names to class lists (like Python globals()[f"{name}_classes"])
        val classLists = mapOf(
            "ordre" to ordreClasses,
            "famille" to famillesClasses,
            "genre" to genreClasses,
            "espece" to especesClasses
        )

        // Process each output like Python: for name, output in zip(output_names, outputs):
        for ((i, outputName) in outputNames.withIndex()) {
            val outputTensor = outputs[i] ?: continue  // outputs[i] not outputs.values[i]
            val floatArray = when (val tensorValue = outputTensor.value) {
                is FloatArray -> tensorValue
                is Array<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val arr = (tensorValue[0] as? FloatArray) ?: continue
                    arr
                }
                else -> {
                    android.util.Log.w("Inference", "Unexpected tensor type: ${tensorValue::class}")
                    continue
                }
            }

            val predictedIndex = floatArray.indices.maxByOrNull { floatArray[it] } ?: continue
            val classList = classLists[outputName] ?: continue

            if (predictedIndex < classList.size) {
                predictions[outputName] = classList[predictedIndex]
                android.util.Log.d("Inference", "$outputName: ${classList[predictedIndex]} (idx=$predictedIndex)")
            }
        }


        android.util.Log.d("Inference", "Total predictions: ${predictions.size}")
        return predictions
    }

    fun runInferenceFromUri(context: Context, uri: Uri): Map<String, String>? {
        val bitmap: Bitmap? = try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (bitmap == null) return null
        if (ortSession == null) init(context)

        return runInference(bitmap)
    }
}
