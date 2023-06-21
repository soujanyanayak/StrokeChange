package org.tensorflow.strokechange

import android.database.Cursor
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.LegendRenderer
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.OnDataPointTapListener
import org.tensorflow.strokechange.database.DBManager
import org.tensorflow.strokechange.database.StrokeReport
import org.tensorflow.strokechange.objectdetection.R
import org.tensorflow.strokechange.operations.Actions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.ExperimentalTime


/**
 * A simple [Fragment] subclass.
 * Use the [Report.newInstance] factory method to
 * create an instance of this fragment.
 */
class Report : Fragment(R.layout.fragment_report) {
    private lateinit var graphView : GraphView
    private lateinit var button : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    @OptIn(ExperimentalTime::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        var rootView = inflater.inflate(R.layout.fragment_report, container, false)
        graphView = rootView.findViewById(R.id.graphView)

        button = rootView.findViewById(R.id.save_pdf)

        var dbManager = DBManager(this.context)
        dbManager.open()
        val cursor: Cursor = dbManager.fetch()
        var xAxis: MutableList<Date> = ArrayList()
        var yAxis1: MutableList<Double> = ArrayList()
        var yAxis2: MutableList<Double> = ArrayList()

        val data: MutableList<StrokeReport> = ArrayList()

        // SC: Convert java.sql.datetime to java.util.datetime to add it to datapoint.
        // SC: Set timezone to avoid default timezone
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        formatter.setTimeZone(TimeZone.getTimeZone("GMT-7"))
        if (cursor.moveToFirst()){
            do{

                var index: Int
                index = cursor.getColumnIndexOrThrow("datetime")
                val dateTime = cursor.getString(index)
                index = cursor.getColumnIndexOrThrow("eyeSeverity")
                val eye = cursor.getDouble(index)
                index = cursor.getColumnIndexOrThrow("mouthSeverity")
                val mouth = cursor.getDouble(index)
                index = cursor.getColumnIndexOrThrow("imageFile")
                val image = cursor.getString(index)
                yAxis1.add(eye)
                yAxis2.add(mouth)
                data.add(StrokeReport(dateTime,eye,mouth,image))
                formatter.parse(dateTime)?.let { xAxis.add(it) }
            }while (cursor.moveToNext())
        }



        dbManager.close()

        if (yAxis1.size < 1 && yAxis2.size < 1){
            return rootView
        }

        var datapoints: Array<DataPoint?> = arrayOfNulls<DataPoint>(yAxis1.size)

        for(i in 0 until yAxis1.size){
            // datapoints[i]=(DataPoint(xAxis[i],yAxis1[i]))
            datapoints[i]=(DataPoint(i.toDouble(),yAxis1[i]))
        }
        val series1: LineGraphSeries<DataPoint?> = LineGraphSeries(datapoints)

        for(i in 0 until yAxis2.size){
            // datapoints[i]=(DataPoint(xAxis[i],yAxis2[i]))
            datapoints[i]=(DataPoint(i.toDouble(),yAxis2[i]))
        }


        val series2: LineGraphSeries<DataPoint?> = LineGraphSeries(datapoints)

        // styling series
        series1.setTitle("Eye Stroke");
        series1.setDrawDataPoints(true);
        series1.setDataPointsRadius(10F);
        series1.setThickness(8);
        series1.setColor(Color.GREEN)
        series2.setColor(Color.MAGENTA)
        series2.setTitle("Mouth Stroke");
        series2.setDrawDataPoints(true);
        series2.setDataPointsRadius(10F);
        series2.setThickness(8);

        this.activity?.runOnUiThread {

            // on below line adding animation
            graphView.animate()

            //graphView.title = "Severity Report"


            // on below line we are setting scrollable
            // for point graph view
            graphView.viewport.isScrollable = true

            // on below line we are setting scalable.
            graphView.viewport.isScalable = true

            // on below line we are setting scalable y
            graphView.viewport.setScalableY(true)

            // on below line we are setting scrollable y
            graphView.getViewport().setScrollableY(true)
            graphView.getViewport().setXAxisBoundsManual(true)
            graphView.getViewport().setYAxisBoundsManual(true)
            graphView.getViewport().setMaxY(10.0)
            graphView.getViewport().setMinY(0.0)
            datapoints[datapoints.size-1]?.let { graphView.getViewport().setMaxX(it.x) }
            datapoints[0]?.let { graphView.getViewport().setMinX(it.x) }


//            graphView.getViewport().scrollToEnd()



            // styling legend
            graphView.getGridLabelRenderer().setGridStyle( GridLabelRenderer.GridStyle.NONE );
            graphView.getLegendRenderer().setVisible(true);
            graphView.getGridLabelRenderer().setVerticalAxisTitleTextSize(55F);
            graphView.getGridLabelRenderer().setVerticalAxisTitle("Severity");
            graphView.getGridLabelRenderer().setHorizontalAxisTitleTextSize(55F);
            graphView.getGridLabelRenderer().setHorizontalAxisTitle("Oldest to Recent Samples");
            graphView.getGridLabelRenderer().setHorizontalLabelsAngle(90)
            graphView.getGridLabelRenderer().setLabelFormatter(DateAsXAxisLabelFormatter( context,formatter));
            graphView.getGridLabelRenderer().setNumHorizontalLabels(datapoints.size);

            graphView.getLegendRenderer().setTextSize(25F);
            graphView.getLegendRenderer().setBackgroundColor(Color.argb(150, 50, 0, 0));
            graphView.getLegendRenderer().setTextColor(Color.WHITE);

            graphView.getGridLabelRenderer().isHorizontalLabelsVisible = false

            graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
            graphView.getLegendRenderer().setMargin(30);
           // graphView.getLegendRenderer().setFixedPosition(150, 0);

            // on below line we are adding
            // data series to our graph view.
            graphView.addSeries(series1)
            graphView.addSeries(series2)

            // styling viewport
            graphView.getViewport().setBackgroundColor(Color.argb(255, 222, 222, 222));
            graphView.getViewport().setDrawBorder(true);
            graphView.getViewport().setBorderColor(Color.BLUE);
            graphView.getViewport().setXAxisBoundsManual(true);

            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.DOWN

            series1.setOnDataPointTapListener(OnDataPointTapListener { series1, dataPoint ->
//                Toast.makeText(
//                    this.context,
//                    "Severity: " + dataPoint.y+ "\nDateTime: " + formatter.format(java.sql.Date(dataPoint.x.toLong()).time),
//                    Toast.LENGTH_LONG
//                ).show()
                Toast.makeText(
                    this.context,
                    "Severity: " + dataPoint.y+ "\nDateTime: " + xAxis[dataPoint.x.toInt()],
                    Toast.LENGTH_LONG
                ).show()
            })

            series2.setOnDataPointTapListener(OnDataPointTapListener { series2, dataPoint ->
//                Toast.makeText(
//                    this.context,
//                    "Severity: " + dataPoint.y + "\nDateTime: " + formatter.format(java.sql.Date(dataPoint.x.toLong()).time),
//                    Toast.LENGTH_LONG
//                ).show()
                Toast.makeText(
                    this.context,
                    "Severity: " + dataPoint.y + "\nDateTime: " + xAxis[dataPoint.x.toInt()],
                    Toast.LENGTH_LONG
                ).show()
            })




        }

        button.setOnClickListener(){
            val bitmap = Bitmap.createBitmap(graphView!!.width, graphView!!.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw the View into the Canvas

            // Draw the View into the Canvas
            graphView!!.draw(canvas)

            // Return the resulting Bitmap

            // Return the resulting Bitmap
            this.createPDF(bitmap,data)
        }

        return rootView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Report.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Report().apply {
                arguments = Bundle().apply {

                }
            }
    }

    private fun generatePDF(
        bitmap: Bitmap,
        yAxis1: MutableList<Double>,
        yAxis2: MutableList<Double>,
        xAxis: MutableList<Date>
    ) {
        // creating an object variable
        // for our PDF document.
        val pdfDocument = PdfDocument()

        val pageHeight = 1120
        val pagewidth = 792

        //val numLinesText = (1120 -200(margin))/ pixel size of the font   ---em versus px versus f for font size
        //val numLinesText = 45;  //empirically found given fonts chosen below

        // two variables for paint "paint" is used
        // for drawing shapes and we will use "title"
        // for adding text in our PDF file.
        val paint = Paint()
        val title = Paint()

        // we are adding page info to our PDF file
        // in which we will be passing our pageWidth,
        // pageHeight and number of pages and after that
        // we are calling it to create our PDF.
        val mypageInfo = PageInfo.Builder(pagewidth, pageHeight, 1).create()

        // below line is used for setting
        // start page for our PDF file.
        val myPage = pdfDocument.startPage(mypageInfo)

        // creating a variable for canvas
        // from our page of PDF.
        val canvas: Canvas = myPage.canvas

        // below line is used to draw our image on our PDF file.
        // the first parameter of our drawbitmap method is
        // our bitmap
        // second parameter is position from left
        // third parameter is position from top and last
        // one is our variable for paint.
        canvas.drawBitmap(bitmap, null , Rect(10,150,500,800),paint)
        //canvas.drawBitmap(bitmap,bitmap.height.toFloat(),bitmap.width.toFloat(),paint)
        pdfDocument.finishPage(myPage)

        // below line is used for adding typeface for
        // our text which we will be adding in our PDF file.
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))

        // below line is used for setting text size
        // which we will be displaying in our PDF file.
        title.setTextSize(25F)
        title.setTextAlign(Paint.Align.LEFT)

        // below line is sued for setting color
        // of our text inside our PDF file.
        this.context?.let { ContextCompat.getColor(it, R.color.purple_200) }
            ?.let { title.setColor(it) }

        // below line is used to draw text in our PDF file.
        // the first parameter is our text, second parameter
        // is position from start, third parameter is position from top
        // and then we are passing our variable of paint which is title.
        canvas.drawText("Severity Report", 20F, 100F, title)
        // similarly we are creating another text and in this
        // we are aligning this text to center of our PDF file.
        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        this.context?.let { ContextCompat.getColor(it, R.color.black) }
            ?.let { title.setColor(it) }
        title.setTextSize(15F)

        // below line is used for setting
        // our text to center of PDF.
        title.setTextAlign(Paint.Align.LEFT)

        var content: String = ""
//        val myPage = pdfDocument.startPage(mypageInfo)
//        for(i in 0 until yAxis1.size){
//
//            if(i/40 == 0){
//                pdfDocument.finishPage(myPage)
//                pdfDocument.startPage(mypageInfo)
//            }
//            content="DateTime: " + xAxis[i].toString() + " | Eye-Severity: "+
//                    Math.round(yAxis1[i] * 10.0) / 10.0 + " | Mouth-Severity: " + Math.round(yAxis2[i] *10.0)/10.0
//            canvas.drawText(content, 20F, 900F+ i*20, title)
//        }

        //canvas.drawText(content, 396F, 560F, title)


        // after adding all attributes to our
        // PDF file we will be finishing our page.


        // below line is used to set the name of
        // our PDF file and its path.
        val filename = String.format("StrokeReport-%d.pdf", System.currentTimeMillis())
       val file = File(Environment.getExternalStorageDirectory().absolutePath + "/StrokeImages", filename)

     //   val file = File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/StrokeImages", filename);


        try {

            // after creating a file name we will
            // write our PDF file to that location.
            pdfDocument.writeTo(FileOutputStream(file))

            // below line is to print toast message
            // on completion of PDF generation.
            Toast.makeText(
                this.context,
                "PDF file generated successfully.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            // below line is used
            // to handle error
            e.printStackTrace()
        }
        // after storing our pdf to that
        // location we are closing our PDF file.
        pdfDocument.close()
    }

    private fun createPDF(
        bitmap: Bitmap,
        data: MutableList<StrokeReport>
    ) {

        // Create a new document.
        val document = PdfDocument()

// Create the first page.
        val firstPageInfo = PageInfo.Builder(1000, 1000, 1).create()
        val firstPage = document.startPage(firstPageInfo)
        val paint = Paint()
        val title = Paint()
        val body = Paint()
        title.textAlign = Paint.Align.CENTER
        title.textSize = 15F

        body.textAlign = Paint.Align.LEFT
        body.textSize = 10F

        val canvas: Canvas = firstPage.canvas
        canvas.drawText("StrokeChange Report", 396F, 560F, title)
        canvas.drawBitmap(bitmap, null, Rect(10, 150, 500, 800), paint)

        document.finishPage(firstPage)

        val numPages = (data.size + 1) / 2
        var index: Int = 0
        var pagenumber = 2
        for (i in 0 until numPages) {
            val newPageInfo = PageInfo.Builder(1000, 1000, pagenumber).create()
            val newPage = document.startPage(newPageInfo)
            val newCanvas: Canvas = newPage.canvas
            var path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+"/"+ data[index].ImageFile
            var bitmap = BitmapFactory.decodeFile(path)
//            canvas.drawBitmap(bitmap, null , Rect(10,10,500,600),null)
            var content =
                "DateTime: " + data[index].DateTime.toString() + " | Eye-Severity: " + data[index].EyeSeverity +
                        " | Mouth-Severity: " + data[index].MouthSeverity
            newCanvas.drawText(content, 10F, 400F, body)
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+"/"+ data[index+1].ImageFile
            bitmap = BitmapFactory.decodeFile(path)
//            canvas.drawBitmap(bitmap, null , Rect(10,450,200,200),paint)
            content =
                "DateTime: " + data[index+1].DateTime.toString() + " | Eye-Severity: " + data[index+1].EyeSeverity +
                        " | Mouth-Severity: " + data[index+1].MouthSeverity
            newCanvas.drawText(content, 10F, 700F, body)

            document.finishPage(newPage)

            index += 2
            pagenumber +=1

        }

        val filename = String.format("StrokeReport-%d.pdf", System.currentTimeMillis())

        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                filename
            )
            // after creating a file name we will
            // write our PDF file to that location.
            document.writeTo(FileOutputStream(file))

            // below line is to print toast message
            // on completion of PDF generation.
            Toast.makeText(
                context,
                "PDF file generated successfully.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            // below line is used
            // to handle error
            Toast.makeText(
                context,
                "Unable to save to external Storage",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }

        document.close()
    }
}