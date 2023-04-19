package org.tensorflow.strokechange

import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.LegendRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.tensorflow.strokechange.database.DBManager
import org.tensorflow.strokechange.database.StrokeReport
import org.tensorflow.strokechange.objectdetection.R

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
        var xAxis: MutableList<String> = ArrayList()
        var yAxis1: MutableList<Double> = ArrayList()
        var yAxis2: MutableList<Double> = ArrayList()

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
            xAxis.add(dateTime)
        }

        dbManager.close()

        if (xAxis.size == 0){
            return rootView
        }

        var datapoints: Array<DataPoint?> = arrayOfNulls<DataPoint>(yAxis1.size)

        for(i in 0 until yAxis1.size){
            datapoints[i]=(DataPoint(i.toDouble(),yAxis1[i]))
        }
        val series1: LineGraphSeries<DataPoint?> = LineGraphSeries(datapoints)

        for(i in 0 until yAxis2.size){
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

            graphView.title = "Severity Report"


            // on below line we are setting scrollable
            // for point graph view
            graphView.viewport.isScrollable = true

            // on below line we are setting scalable.
            graphView.viewport.isScalable = true

            // on below line we are setting scalable y
            //graphView.viewport.setScalableY(true)

            // on below line we are setting scrollable y
            graphView.viewport.setScrollableY(true)


            // styling legend
            graphView.getLegendRenderer().setVisible(true);
            graphView.getGridLabelRenderer().setVerticalAxisTitleTextSize(40F);
            graphView.getGridLabelRenderer().setVerticalAxisTitle("Severity");
            graphView.getGridLabelRenderer().setHorizontalAxisTitleTextSize(40F);
            graphView.getGridLabelRenderer().setHorizontalAxisTitle("Images");
            graphView.getLegendRenderer().setTextSize(25F);
            graphView.getLegendRenderer().setBackgroundColor(Color.argb(150, 50, 0, 0));
            graphView.getLegendRenderer().setTextColor(Color.WHITE);

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