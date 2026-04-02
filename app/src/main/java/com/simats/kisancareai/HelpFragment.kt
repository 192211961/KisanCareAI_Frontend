package com.simats.kisancareai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class HelpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FAQ Toggle Logic
        setupFaqToggle(view, R.id.layout_faq_1, R.id.answer_faq_1, R.id.arrow_faq_1)
        setupFaqToggle(view, R.id.layout_faq_2, R.id.answer_faq_2, R.id.arrow_faq_2)
        setupFaqToggle(view, R.id.layout_faq_3, R.id.answer_faq_3, R.id.arrow_faq_3)
        setupFaqToggle(view, R.id.layout_faq_4, R.id.answer_faq_4, R.id.arrow_faq_4)
        setupFaqToggle(view, R.id.layout_faq_5, R.id.answer_faq_5, R.id.arrow_faq_5)
        setupFaqToggle(view, R.id.layout_faq_6, R.id.answer_faq_6, R.id.arrow_faq_6)
        setupFaqToggle(view, R.id.layout_faq_7, R.id.answer_faq_7, R.id.arrow_faq_7)
        setupFaqToggle(view, R.id.layout_faq_8, R.id.answer_faq_8, R.id.arrow_faq_8)
    }

    private fun setupFaqToggle(parent: View, layoutId: Int, answerId: Int, arrowId: Int) {
        val layout = parent.findViewById<View>(layoutId)
        val answer = parent.findViewById<TextView>(answerId)
        val arrow = parent.findViewById<ImageView>(arrowId)

        layout.setOnClickListener {
            if (answer.visibility == View.GONE) {
                answer.visibility = View.VISIBLE
                arrow.rotation = 90f // Rotate arrow down
            } else {
                answer.visibility = View.GONE
                arrow.rotation = 0f // Reset arrow
            }
        }
    }
}
