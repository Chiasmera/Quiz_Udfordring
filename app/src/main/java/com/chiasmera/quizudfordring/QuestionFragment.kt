package com.chiasmera.quizudfordring

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.chiasmera.quizudfordring.databinding.FragmentQuestionBinding

/**
 * Fragment showing a question and possible answers
 */
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

        //Arguments recieved on navigation. A list of questions and the index of the current question in the list
        val index = arguments?.getInt("index")
        val questions = arguments?.getParcelableArray("questionArray") as Array<Question>

        //If arguments are null somehow, navigate back to categories
        if (index == null || questions == null || questions.isEmpty()) {
            val action = QuestionFragmentDirections.actionQuestionFragmentToCategoryListFragment()
            view.findNavController().navigate(action)
        } else {
            val currentQuestion = questions[index]

            //Setting the question text
            binding.questionView.text = currentQuestion.question

            //Randomizing an index for the correct answer, as it should not be in a predicable position on screen
            val randomIndex = Math.floor(Math.random() * (currentQuestion.wrongAnswers.size + 1)).toInt()

            //List of ALL answers, both right and wrong, with the correct in the random position.
            val answers: MutableList<String> = mutableListOf<String>()
            answers.addAll(currentQuestion.wrongAnswers)
            answers.add(randomIndex, currentQuestion.correctAnswer)

            //Creates a button for each answer, giving it a listener that checks for correctness and changes background color accordingly
            for (answer in answers) {
                val button = Button(activity)
                button.text = answer
                button.setOnClickListener {
                    if (answer == currentQuestion.correctAnswer) {
                        button.setBackgroundColor(Color.GREEN)
                    } else {
                        button.setBackgroundColor(Color.RED)
                    }
                    //Toggles the "next" button to true upon any answer
                    answerSelected()
                }

                binding.answerView.addView(button)
            }

            //the "next" button is disabled until any answer is clicked
            binding.nextButton.isEnabled = false
            //onclick listener navigating either to the next question, or back to categories if this was the last question in the list
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

            //onclick listener for the "categories" button. Navigates back to all categories
            binding.homeButton.setOnClickListener {
                val action = QuestionFragmentDirections.actionQuestionFragmentToCategoryListFragment()
                view.findNavController().navigate(action)
            }
        }
    }


    /**
     * Performs any required actions upon the user selecting an answer, such as enabling/disabling buttons
     */
    fun answerSelected() {
        //unused code that disallows choosing more than one answer.
        //I have chosen for the app to allow you to choose as many options as you want.
        // Another option could be to allow only for one answer, in which case you would disable the other buttons as below
//        for (answerButton in binding.answerView.children) {
//            answerButton.isEnabled = false
//        }
        binding.nextButton.isEnabled = true
    }
}