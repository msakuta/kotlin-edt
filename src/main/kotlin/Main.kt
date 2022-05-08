import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val inputFile = if (args.size > 1) {
        args[1]
    }
    else {
        "Rust_logo.png"
    }

    println("Program arguments: ${args.joinToString()}")

    val file = File(inputFile)
    val img = ImageIO.read(file)

    println("Width: ${img.width} Height: ${img.height}")

    val data = img.data

    val iArray = IntArray(img.width * img.height)
    data.getPixels(0, 0, img.width, img.height, iArray)

    val bArray = iArray.map { i -> i != 0 }

    val edtResult: Array<Double>
    val time = measureTimeMillis {
        edtResult = edt(bArray, Pair(img.width, img.height))
    }

    println("time: $time")

    val edtInt = edtResult.map { i -> i.toInt() }.toIntArray()

    val maxValue = edtInt.reduce { acc, i -> acc.coerceAtLeast(i) }

    val fHorzEdtPixel = edtInt.map {
            i ->
        val v = i * 127 / maxValue
        v or (v shl 8) or (v shl 16)
    }.toIntArray()

    val outImage = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
    outImage.setRGB(0, 0, img.width, img.height, fHorzEdtPixel, 0, img.width)

    val outFile = File("out.png")
    if (outFile.createNewFile()) {
        ImageIO.write(outImage, "png", outFile)
    }
    else {
        ImageIO.write(outImage, "png", outFile)
    }
}

internal fun horizontalEdt(buffer: List<Boolean>, shape: Pair<Int, Int>): Array<Double> {
    val horzEdt = Array(shape.first*shape.second) { 0.0 }

    val edtMax = shape.first.coerceAtLeast(shape.second).toDouble()

    var countTrue = 0

    for (y in 0 until shape.second) {
        for (x in 0 until shape.first) {
            if (buffer[x+y*shape.first]) {
                horzEdt[x+y*shape.first] = edtMax
                countTrue += 1
            }
        }
    }

    println("count: $countTrue")

    var maxVal = 0.0

    val scan = {
        x: Int, y: Int, min_val: Double ->
        val f = horzEdt[x+y*shape.first]
        val next = min_val + 1.0
        val v = f.coerceAtMost(next)
        horzEdt[x+y*shape.first] = v
        if (maxVal < v) {
            maxVal = v
        }
        v
    }

    for (y in 0 until shape.second) {
        var minVal = 0.0
        for (x in 0 until shape.first) {
            minVal = scan(x, y, minVal)
        }
        minVal = 0.0
        for (x in shape.first-1 downTo 0) {
            minVal = scan(x, y, minVal)
        }
    }

    return horzEdt
}

fun edt(buffer: List<Boolean>, shape: Pair<Int, Int>): Array<Double> {
    val horzEdt = horizontalEdt(buffer, shape)

    var maxEdt = shape.first.coerceAtLeast(shape.second).toDouble()
    maxEdt *= maxEdt

    val verticalScan = fun(x: Int, y: Int): Double {
        var totalEdt = maxEdt
        for (y2 in 0 until shape.second) {
            val horzVal = horzEdt[x+y2*shape.first]
            val dy = y2 - y
            val v = dy*dy + horzVal*horzVal
            if (v < totalEdt) {
                totalEdt = v
            }
        }
        var dy = y.toDouble()
        if (dy*dy < totalEdt) {
            return dy * dy
        }
        dy = (shape.second - y).toDouble()
        if (dy*dy < totalEdt) {
            return dy * dy
        }
        return totalEdt
    }

    val ret = horzEdt.clone()

    for (x in 0 until shape.first) {
        for (y in 0 until shape.second) {
            ret[x+y*shape.first] = verticalScan(x, y)
        }
    }

    return ret
}
