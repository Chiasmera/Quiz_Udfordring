package com.chiasmera.quizudfordring

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.Audio.Radio
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController

import com.chiasmera.quizudfordring.databinding.FragmentCategoryListBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder


class CategoryListFragment : Fragment() {
    val TAG: String = "CategoryListFragment"
    private val categorySet = mutableSetOf<Category>()
    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runBlocking {
            fetchCategories(categorySet)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedRadio: RadioButton =
            binding.difficultyRadioGroup.findViewById(binding.difficultyRadioGroup.checkedRadioButtonId)
        createCategoryList(selectedRadio.text.toString())
        for (radioButton in binding.difficultyRadioGroup.children) {
            radioButton.setOnClickListener {
                val button: RadioButton = radioButton as RadioButton
                createCategoryList(button.text.toString())
            }
        }
    }


    fun createCategoryList(difficulty: String) {
        binding.categoryListView.removeAllViews()

        for (category in categorySet.sortedBy { it.toString() }) {
            val button = Button(activity)
            val categoryCountForDifficulty = if (difficulty.lowercase() == "easy") {
                category.easyCount
            } else if (difficulty.lowercase() == "hard") {
                category.hardCount
            } else {
                category.mediumCount
            }
            button.text = "${category} (${categoryCountForDifficulty})"
            button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            button.setOnClickListener {

                var questionArray: Array<Question>

                val fragment = this
                lifecycleScope.launch {
                    questionArray = fetchQuestions(category, difficulty)

                    val action =
                        CategoryListFragmentDirections.actionCategoryListFragmentToQuestionFragment(
                            questionArray = questionArray, index = 0
                        )
                    NavHostFragment.findNavController(fragment).navigate(action)
                }

            }


            binding.categoryListView.addView(button)
        }
    }

    private suspend fun fetchQuestions(
        category: Category, difficulty: String = "Medium"
    ): Array<Question> {
        val list = arrayListOf<Question>()
        withContext(Dispatchers.IO) {
            var amount = 10
            if (difficulty.lowercase() == "easy") {
                amount = if (category.easyCount < 10) {
                    category.easyCount
                } else {
                    10
                }
            } else if (difficulty.lowercase() == "hard") {
                amount = if (category.hardCount < 10) {
                    category.hardCount
                } else {
                    10
                }
            } else {
                amount = if (category.mediumCount < 10) {
                    category.mediumCount
                } else {
                    10
                }
            }
            val url =
                URL("https://opentdb.com/api.php?amount=${amount}&category=${category.id}&difficulty=${difficulty.lowercase()}")
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == 200) {
                val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
                val result = bufferedReader.readLine().trimIndent()

                val jsonObject = JSONObject(result)
                val jsonArray = jsonObject.getJSONArray("results")
                for (i in 0 until jsonArray.length()) {
                    val current = jsonArray.getJSONObject(i)
                    val category =
                        Html.fromHtml(current.getString("category"), FROM_HTML_MODE_LEGACY)
                            .toString()
                    val type =
                        Html.fromHtml(current.getString("type"), FROM_HTML_MODE_LEGACY).toString()
                    val difficulty =
                        Html.fromHtml(current.getString("difficulty"), FROM_HTML_MODE_LEGACY)
                            .toString()
                    val question =
                        Html.fromHtml(current.getString("question"), FROM_HTML_MODE_LEGACY)
                            .toString()
                    val correct =
                        Html.fromHtml(current.getString("correct_answer"), FROM_HTML_MODE_LEGACY)
                            .toString()
                    val incorrectJsonArray = current.getJSONArray("incorrect_answers")
                    val incorrect = mutableListOf<String>()
                    for (i in 0 until incorrectJsonArray.length()) {
                        incorrect.add(
                            Html.fromHtml(
                                incorrectJsonArray[i].toString(), FROM_HTML_MODE_LEGACY
                            ).toString()
                        )
//                        incorrect.add(incorrectJsonArray[i].toString())
                    }

                    list.add(Question(category, type, difficulty, question, correct, incorrect))
                }

            } else {
                Log.e(
                    TAG,
                    "Failed to fetch questions. Error code: ${connection.responseCode} (${connection.responseMessage})"
                )
            }

        }

        return list.toTypedArray()
    }

    /**
     * HTML GETs a list of quiz categories asynchronously and adds them to the given set
     */
    suspend fun fetchCategories(categorySet: MutableSet<Category>) {
        withContext(Dispatchers.IO) {
            val connection: HttpURLConnection =
                URL("https://opentdb.com/api_category.php").openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == 200) {
                val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
                val result = bufferedReader.readLine().trimIndent()

                val parsed = JSONObject(result)
                val parsedArray = parsed.getJSONArray("trivia_categories")
                for (i in 0 until parsedArray.length()) {
                    val currentCategory = parsedArray.getJSONObject(i)
                    val id = currentCategory.getInt("id")
                    val fullName = currentCategory.getString("name")
                    val name = fullName

                    val category = Category(id, name)
                    fetchQuestionCountForCategory(category)
                    categorySet.add(category)
                }
            } else {
                Log.e(
                    TAG,
                    "Failed to fetch categories. Error code: ${connection.responseCode} (${connection.responseMessage})"
                )
            }
            connection.disconnect()
        }
    }

    /**
     * GET's a count of availible questions for the given category, and assigns it to the properties of that category
     */
    suspend fun fetchQuestionCountForCategory(category: Category) {
        withContext(Dispatchers.IO) {
            val url = URL("https://opentdb.com/api_count.php?category=${category.id}")
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == 200) {
                val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonObject =
                    JSONObject(bufferedReader.readLine()).getJSONObject("category_question_count")
                category.totalCount = jsonObject.getInt("total_question_count")
                category.easyCount = jsonObject.getInt("total_easy_question_count")
                category.mediumCount = jsonObject.getInt("total_medium_question_count")
                category.hardCount = jsonObject.getInt("total_hard_question_count")
            }
        }
    }

}