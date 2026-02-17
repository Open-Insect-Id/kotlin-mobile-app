package org.openinsectid.app.utils

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import org.json.JSONObject
import java.io.InputStream
import kotlin.collections.iterator
import kotlin.math.exp
import kotlin.math.ln

object InferenceManager {

    private var ortSession: OrtSession? = null
    private var ordreClasses: List<String> = emptyList()
    private var famillesClasses: List<String> = emptyList()
    private var genreClasses: List<String> = emptyList()
    private var especesClasses: List<String> = emptyList()


    fun init(context: Context) {
        if (ortSession != null) return  // already initialized

        try {
            val env = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("insect_model.onnx").readBytes()
            ortSession = env.createSession(modelBytes)

            val hierarchyJson =
                context.assets.open("hierarchy_map.json").bufferedReader().use { it.readText() }
            val hierarchy = JSONObject(hierarchyJson).getJSONObject("full_taxa_map")

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
        } catch (e: Exception) {
            Log.e("InferenceManager", "Error while loading Inference manager: $e")
        }
    }


    fun runInference(bitmap: Bitmap): Map<String, Pair<String, Float>> {
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
        val outputTensor = outputs[0]
        val value = outputTensor.value


        @Suppress("UNCHECKED_CAST")
        val float3D = value as Array<Array<FloatArray>> // [batch, 4, total_classes]

        val predictions = mutableMapOf<String, Pair<String, Float>>()
        val classLists = listOf(ordreClasses, famillesClasses, genreClasses, especesClasses)
        val names = listOf("ordre", "famille", "genre", "espece")

        val batch = float3D[0]

        for ((i, classList) in classLists.withIndex()) {
            val slice = batch[i].sliceArray(0 until classList.size)
            val probs = softmax(slice)
            val predictedIndex = probs.indices.maxByOrNull { probs[it] } ?: 0
            val ent = entropy(probs)
            predictions[names[i]] = classList[predictedIndex] to ent

            Log.d("Inference", "${names[i]}: ${classList[predictedIndex]} (idx=$predictedIndex), entropy=$ent")
        }


        Log.d("Inference", "Total predictions: ${predictions.size}")
        return predictions
    }

    fun runInferenceFromUri(context: Context, uri: Uri): Map<String, Pair<String, Float>>? {
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



/**
 * Computes the softmax of a given array of logits.
 *
 * Softmax converts raw scores (logits) into probabilities that sum to 1.
 * This is commonly used in classification tasks to interpret model outputs
 * as probabilities for each class.
 *
 * The subtraction of the maximum logit (maxLogit) improves numerical stability
 * and prevents overflow when exponentiating large numbers.
 *
 * @param logits FloatArray of raw model scores for each class.
 * @return FloatArray of probabilities corresponding to each class.
 */
fun softmax(logits: FloatArray): FloatArray {
    val maxLogit = logits.maxOrNull() ?: 0f
    val exps = logits.map { exp((it - maxLogit).toDouble()) }
    val sumExps = exps.sum()
    return exps.map { (it / sumExps).toFloat() }.toFloatArray()
}

/**
 * Computes the entropy of a probability distribution.
 *
 * Entropy measures the uncertainty in the predicted probabilities.
 * Low entropy means the model is confident (one class dominates),
 * high entropy means the model is uncertain (probabilities are more uniform).
 *
 * Logarithm of zero is avoided by filtering out zero probabilities.
 *
 * @param probs FloatArray of probabilities (should sum to ~1, e.g., from softmax).
 * @return Float representing the entropy of the distribution.
 */
fun entropy(probs: FloatArray): Float {
    return -probs.filter { it > 0f } // avoid log(0)
        .sumOf { (it * ln(it.toDouble())) }
        .toFloat()
}
