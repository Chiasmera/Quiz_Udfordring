package com.chiasmera.quizudfordring

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.os.BundleCompat.getParcelableArray
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.chiasmera.quizudfordring.databinding.FragmentCategoryListBinding
import com.chiasmera.quizudfordring.databinding.FragmentQuestionBinding
import java.util.Collections.addAll


class QuestionFragment : Fragment() {
    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val index = arguments?.getInt("index")
        val questions = arguments?.getParcelableArray("questionArray")


        if (index == null || questions == null || questions.isEmpty()) {
            val action = QuestionFragmentDirections.actionQuestionFragmentToCategoryListFragment()
            view.findNavController().navigate(action)
        } else {
            val currentQuestion = questions[index] as Question

            binding.questionView.text = currentQuestion.question

            val randomIndex =
                Math.floor(Math.random() * (currentQuestion.wrongAnswers.size + 1)).toInt()

            val answers: MutableList<String> = mutableListOf<String>()
            answers.addAll(currentQuestion.wrongAnswers)
            answers.add(randomIndex, currentQuestion.correctAnswer)

            for (answer in answers) {
                val button = Button(activity)
                button.text = answer
                button.setOnClickListener {
                    if (answer == currentQuestion.correctAnswer) {
                        button.setBackgroundColor(Color.GREEN)
                    } else {
                        button.setBackgroundColor(Color.RED)
                    }
                    toggleButtons()
                }

                binding.answerView.addView(button)
            }

            binding.nextButton.isEnabled = false
            binding.nextButton.setOnClickListener {
                if (index < questions.size - 1) {
                    val action =
                        QuestionFragmentDirections.actionQuestionFragmentSelf(
                            questionArray = questions as Array<Question>, index = index + 1
                        )
                    view.findNavController().navigate(action)
                } else {
                    val action = QuestionFragmentDirections.actionQuestionFragmentToCategoryListFragment()
                    view.findNavController().navigate(action)
                }
            }

            binding.homeButton.setOnClickListener {
                val action = QuestionFragmentDirections.actionQuestionFragmentToCategoryListFragment()
                view.findNavController().navigate(action)
            }
        }
    }


    fun toggleButtons() {
        //disables all buttons when one answer is given. Useful if only one answer is allowed
//        for (answerButton in binding.answerView.children) {
//            answerButton.isEnabled = false
//        }
        binding.nextButton.isEnabled = true
    }
}