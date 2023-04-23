package org.tensorflow.strokechange

import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        var dbManager = DBManager(this.context)
        dbManager.open()
        val cursor: Cursor = dbManager.fetch()
        var xAxis: MutableList<Date> = ArrayList()
        var yAxis1: MutableList<Double> = ArrayList()
        var yAxis2: MutableList<Double> = ArrayList()

        // SC: Convert java.sql.datetime to java.util.datetime to add it to datapoint.
        // SC: Set timezone to avoid default timezone
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        formatter.setTimeZone(TimeZone.getTimeZone("GMT-7"))
        while (cursor.moveToNext()) {

            var index: Int
            index = cursor.getColumnIndexOrThrow("datetime")
            val dateTime = cursor.getString(index)
            index = cursor.getColumnIndexOrThrow("eyeSeverity")
            val eye = cursor.getDouble(index)
            index = cursor.getColumnIndexOrThrow("mouthSeverity")
            val mouth = cursor.getDouble(index)
            var sr = StrokeReport(dateTime,eye,mouth)
            yAxis1.add(eye)
            yAxis2.add(mouth)
            formatter.parse(dateTime)?.let { xAxis.add(it) }
        }

        dbManager.close()

        if (yAxis1.size < 1 && yAxis2.size < 1){
            return rootView
        }

        var datapoints: Array<DataPoint?> = arrayOfNulls<DataPoint>(yAxis1.size)

        for(i in 0 until yAxis1.size){
            datapoints[i]=(DataPoint(xAxis[i],yAxis1[i]))
        }
        val series1: LineGraphSeries<DataPoint?> = LineGraphSeries(datapoints)

        for(i in 0 until yAxis2.size){
            datapoints[i]=(DataPoint(xAxis[i],yAxis2[i]))
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

            graphView.title = "Severity Report"


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
                Toast.makeText(
                    this.context,
                    "Severity: " + dataPoint.y+ "\nDateTime: " + formatter.format(java.sql.Date(dataPoint.x.toLong()).time),
                    Toast.LENGTH_LONG
                ).show()
            })

            series2.setOnDataPointTapListener(OnDataPointTapListener { series2, dataPoint ->
                Toast.makeText(
                    this.context,
                    "Severity: " + dataPoint.y + "\nDateTime: " + formatter.format(java.sql.Date(dataPoint.x.toLong()).time),
                    Toast.LENGTH_LONG
                ).show()
            })




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
}