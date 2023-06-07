package org.tensorflow.strokechange

// SC : database package

// SC : regression package

// SC : objectdetection package

// SC : operations package
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.strokechange.Regression.EyeMouthRegression
import org.tensorflow.strokechange.database.DBManager
import org.tensorflow.strokechange.objectdetection.R
import org.tensorflow.strokechange.operations.Actions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 */
class Home : Fragment(R.layout.fragment_home), View.OnClickListener {
    private lateinit var captureImageFab: Button
    private lateinit var inputImageView: ImageView
//    private lateinit var imgSampleOne: ImageView
//    private lateinit var imgSampleTwo: ImageView
//    private lateinit var imgSampleThree: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var currentPhotoPath: String

    private lateinit var saveImageButton: Button

    var dbManager = DBManager(this.context)

    var eye: Double = 0.0
    var mouth: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var rootView = inflater.inflate(R.layout.fragment_home, container, false)
        

        inputImageView = rootView.findViewById(R.id.imageView)
        tvPlaceholder = rootView.findViewById(R.id.tvPlaceholder)
        saveImageButton = rootView.findViewById(R.id.save_image)
        saveImageButton.visibility = View.GONE
        captureImageFab = rootView.findViewById(R.id.captureImageFab)

        captureImageFab.setOnClickListener(this)
        saveImageButton.setOnClickListener(this)

        return rootView
    }

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 96F
        private const val STORAGE_PERMISSION_CODE = 100
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Home().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE &&
            resultCode == Activity.RESULT_OK
        ) {
            setViewAndDetect(getCapturedImage())
        }
    }



    /**
     * onClick(v: View?)
     *      Detect touches on the UI components
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.captureImageFab -> {
                try {
                    dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
//            R.id.imgSampleOne -> {
//                setViewAndDetect(getSampleImage(R.drawable.image1))
//            }
//            R.id.imgSampleTwo -> {
//                setViewAndDetect(getSampleImage(R.drawable.image2))
//            }
//            R.id.imgSampleThree -> {
//                setViewAndDetect(getSampleImage(R.drawable.image3))
//            }
        }
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.3f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this.context,
            "eye-mouth-detector.tflite",
            options
        )


        // Step 3: Feed given image to the detector
        val results = detector.detect(image)

        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        // Draw the detection result on the bitmap and show it.
        val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)

        Log.d("bitmap", imgWithResult.toString())

        this.activity?.runOnUiThread{
            saveImageButton.isEnabled = true
            inputImageView.setImageBitmap(imgWithResult)
            saveImageButton.visibility = View.VISIBLE

            saveImageButton.setOnClickListener(){
                if (checkPermission()){
                    Log.d(TAG, "onCreate: Permission already granted, create folder")
                    // SC : Insert into database
                    val fileName = System.currentTimeMillis().toString()+".png"

                    dbManager.insert(eye,mouth,fileName)
                    dbManager.close()

                    saveImageButton.isEnabled = false

                    val actions = Actions()

                    actions.saveToGallery(imgWithResult, fileName)



                } else{
                    Log.d(TAG, "onCreate: Permission was not granted, request")
                    requestPermission()
                }

            }
        }


    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above

            try {
                Log.d(TAG, "requestPermission: try")
                val intent = Intent()

                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;
               // intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.context?.packageName.toString(), null)
                startActivityForResult(intent, 2296);
            }
            catch (e: Exception){
                Log.e(TAG, "requestPermission: ", e)
            }

            /**
            try {
                var intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",this.context.packageName)));
                startActivityForResult(intent, 2296);
            } catch (e: Exception) {
                var intent =  Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);}
            **/
        }
        else{
            //Android is below 11(R)
            this.activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }



    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            Environment.isExternalStorageManager()
        }
        else{
            //Android is below 11(R)
            val write = this.context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE) }
            val read = this.context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE) }
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun createFolder() {
        //folder name
        val folderName = "StrokeImages"

        //create folder using name we just input
        val file = File("${Environment.getExternalStorageDirectory()}/$folderName")
        //create folder
        val folderCreated = file.mkdir()

        System.out.println("folder created? =" + folderCreated.toString());

    }

    /**
     * SC REG: runRegression
     * Loads Eye Regression and Mouth Regression models
     * process image using Image Processor and pass it through interpreter
     * return the severity value
     */
    private fun runRegression(bitmap: Bitmap, text: String): Float {

        // SC REG: create instance of class EyeMouthRegression
        var eyeMouthReg = EyeMouthRegression()
        var tflite: Interpreter


        //SC REG: if the label contains eye then run Eye regression else Mouth regression
        if ("eye" in text){
            tflite = Interpreter(eyeMouthReg.loadEyeModelFile(this.activity))
        }
        else{
            tflite = Interpreter(eyeMouthReg.loadMouthModelFile(this.activity))
        }


        val row1: DoubleArray = doubleArrayOf()
        val row2: DoubleArray = doubleArrayOf()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = processImage(tensorImage)
        val tbuffer = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        tbuffer.loadBuffer(tensorImage.buffer)

        try{
            tflite.run(tbuffer.buffer, tbuffer.buffer)
        }
        catch(e: java.lang.Exception){
            Log.e("Regression",e.toString())
        }
        return tbuffer.getFloatValue(0)
    }

    /**
     * debugPrint(visionObjects: List<Detection>)
     *      Print the detection result to logcat to examine
     */
    private fun debugPrint(results : List<Detection>) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Detected object: ${i} ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
        }
    }

    /**
     * SC REG: processImage
     * Using ImageProcessor resize the image
     */
    private fun processImage(tensorImage : TensorImage): TensorImage{
        var imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224,ResizeOp.ResizeMethod.BILINEAR))
            .build()
        imageProcessor.process(tensorImage)

        return tensorImage
    }

    /**
     * setViewAndDetect(bitmap: Bitmap)
     *      Set image to view and call object detection
     */
    private fun setViewAndDetect(bitmap: Bitmap) {
        // Display capture image
        inputImageView.setImageBitmap(bitmap)
        tvPlaceholder.visibility = View.INVISIBLE

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) { runObjectDetection(bitmap) }
    }

    /**
     * getCapturedImage():
     *      Decodes and crops the captured image from camera.
     */
    private fun getCapturedImage(): Bitmap {
        // Get the dimensions of the View
        val targetW: Int = inputImageView.width
        val targetH: Int = inputImageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = max(1, min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inMutable = true
        }
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

    /**
     * getSampleImage():
     *      Get image form drawable and convert to bitmap.
     */
    private fun getSampleImage(drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable, BitmapFactory.Options().apply {
            inMutable = true
        })
    }

    /**
     * rotateImage():
     *     Decodes and crops the captured image from camera.
     */
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    /**
     * createImageFile():
     *     Generates a temporary image file for the Camera app to write to.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = this.activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    /**
     * dispatchTakePictureIntent():
     *     Start the Camera app to take a photo.
     */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            this.activity?.let {
                takePictureIntent.resolveActivity(it.packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (e: IOException) {
                        Log.e(TAG, e.message.toString())
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri? = this.context?.let { it1 ->
                            FileProvider.getUriForFile(
                                it1,
                                "org.tensorflow.strokechange.objectdetection.fileprovider",
                                it
                            )
                        }
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        }
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        dbManager = DBManager(this.context)
        dbManager.open()



        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)

            val tagSize = Rect(0, 0, 0, 0)

            // SC REG: crop image based on bounding box
            Log.d("Regression","Before regression")
            val cropped = Bitmap.createBitmap(bitmap, box.left.toInt(), box.top.toInt(), box.width().toInt(), box.height().toInt())
            val output = runRegression(cropped, it.text)

            if("eye" in it.text){
                eye = output.toDouble()


            }
            else if("mouth" in it.text){
                mouth = output.toDouble()
            }

            Log.d("Regression",output.toString())

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE

            // pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            pen.getTextBounds(output.toString(), 0, output.toString().length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                "Severity: "+output.toString(), box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )

        }

        // SC : Insert into database
//        dbManager.insert(eye,mouth)
//        dbManager.close()

        return outputBitmap
    }

    /**
     * DetectionResult
     *      A class to store the visualization info of a detected object.
     */
    data class DetectionResult(val boundingBox: RectF, val text: String)
}