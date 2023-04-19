package org.tensorflow.strokechange

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.jjoe64.graphview.GraphView
import org.tensorflow.strokechange.objectdetection.R


/**
 * A simple [Fragment] subclass.
 * Use the [Information.newInstance] factory method to
 * create an instance of this fragment.
 */
class Information : Fragment() {

    private lateinit var card : CardView
    private lateinit var text : TextView
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
        var rootView = inflater.inflate(R.layout.fragment_information, container, false)
        card = rootView.findViewById(R.id.card_view)
        text =  rootView.findViewById(R.id.info_text)
        var content: String ="The goal of StrokeChange is to detect the stroke related facial features " +
                "each time the patient uses the system. " +
                "The concept is the patient would use the system daily to monitor their current state." +
                "Future work would be to look at changes in the daily detections to create customized " +
                "reports for care workers/doctors."

        this.activity?.runOnUiThread {
            text.setText(content)
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
         * @return A new instance of fragment Information.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Information().apply {
                arguments = Bundle().apply {
                }
            }
    }
}