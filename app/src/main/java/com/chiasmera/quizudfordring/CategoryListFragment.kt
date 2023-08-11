package com.chiasmera.quizudfordring

import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.chiasmera.quizudfordring.databinding.FragmentCategoryListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fragment showing a list of categories and a choice between difficulties
 */
class CategoryListFragment : Fragment() {
    val TAG: String = "CategoryListFragment"

    /**
     * Set of all categories fetched from API. Filled onCreate
     */
    private val categorySet = mutableSetOf<Category>()
    //binding properties, to make it easier to find views
    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Fetch list of categories from API. Blocking in order to make sure categorySet gets filled before initial category button creation.
        runBlocking {
            fetchCategories(categorySet)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedRadio: RadioButton = binding.difficultyRadioGroup.findViewById(binding.difficultyRadioGroup.checkedRadioButtonId)
        //Creates the list of category buttons, with name and question count for selected difficulty (medium per default)
        createCategoryList(selectedRadio.text.toString())
        //Sets a click listener that re-creates the list of category buttons when difficulty is changed, in order to update question count for each category
        for (radioButton in binding.difficultyRadioGroup.children) {
            radioButton.setOnClickListener {
                val button: RadioButton = radioButton as RadioButton
                createCategoryList(button.text.toString().lowercase())
            }
        }

    }

    /**
     * Creates a sorted list of buttons with the name and question count for each category
     */
    private fun createCategoryList(difficulty: String) {

        //Removes all existing category buttons in the view
        binding.categoryListView.removeAllViews()

        //For each category, creates a button, adds the proper text and a listener, then adds it to the view
        for (category in categorySet.sortedBy { it.toString() }) {
            val button = Button(activity)

                 val buttonText= if (difficulty.lowercase() == "easy") {
                    "$category (${category.easyCount})"
                } else if (difficulty.lowercase() == "hard") {
                    "$category (${category.hardCount})"
                } else {
                    "$category (${category.mediumCount})"
                }

            button.text = buttonText
            button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START

            //onClick listener that fetches question for the given category and difficulty, then navigates to the first question
            button.setOnClickListener {
                var questionArray: Array<Question>

                val fragment = this
                lifecycleScope.launch {
                    questionArray = fetchQuestions(category, difficulty)

                    val action = CategoryListFragmentDirections.actionCategoryListFragmentToQuestionFragment(
                        questionArray = questionArray,
                        index = 0)
                    NavHostFragment.findNavController(fragment).navigate(action)
                }

            }

            binding.categoryListView.addView(button)
        }
    }

    /**
     * GET's questions from the API for the given category and difficulty and returns an array of Questions
     *
     * Fetches 10 questions if possible.
     * If there are less than 10 questions with the given parameters, only fetches as many as are possible
     * @param category Category object the questions should belong to
     * @param difficulty the currently chosen difficulty as a String
     * @return an Array of Questions
     */
    private suspend fun fetchQuestions(
        category: Category,
        difficulty: String = "Medium"
    ): Array<Question> {
        val list = arrayListOf<Question>()
        withContext(Dispatchers.IO) {
            //Sets the amount of questions to get to either 10 or the amount availible, whichever is higher
            val amount = getMaxQuestionAmount(category, difficulty.lowercase())

            //HTML GET request with Amount, Category ID and difficulty as parameters
            val url = URL("https://opentdb.com/api.php?amount=${amount}&category=${category.id}&difficulty=${difficulty.lowercase()}")
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == 200) {
                //Reads response and translates to a JSON array of JSON objects
                val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
                val result = bufferedReader.readLine().trimIndent()
                val jsonObject = JSONObject(result)
                val jsonArray = jsonObject.getJSONArray("results")

                //translates each JSON object in array into a Question object
                for (i in 0 until jsonArray.length()) {
                    list.add(
                        parseJSONToQuestion(
                            jsonArray.getJSONObject(i)
                        )
                    )
                }

            } else {
                //Should handle other status codes from the HTML response (but doesn't right now)
                //Proper handling would include throwing an error, then catching it and possibly showing the user
                // a (useful) messageallowing them to either try again or choose another category
                Log.e(
                    TAG,
                    "Failed to fetch questions. Error code: ${connection.responseCode} (${connection.responseMessage})"
                )
                Toast.makeText(context, "Something went wrong!",Toast.LENGTH_SHORT).show()
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

    /**
     * Parses a JSON representation of a question to a Question object
     * @param jsonObject a JSON object representing a question
     * @return a Question object
     */
    private fun parseJSONToQuestion (jsonObject : JSONObject) : Question {
        //I had trouble with encoding. Only thing that ended up working was parsing each value seperately.
        // There is propablæy a smarter way out there
        val category = Html.fromHtml(jsonObject.getString("category"), FROM_HTML_MODE_LEGACY).toString()
        val type = Html.fromHtml(jsonObject.getString("type"), FROM_HTML_MODE_LEGACY).toString()
        val difficulty = Html.fromHtml(jsonObject.getString("difficulty"), FROM_HTML_MODE_LEGACY).toString()
        val question = Html.fromHtml(jsonObject.getString("question"), FROM_HTML_MODE_LEGACY).toString()
        val correct = Html.fromHtml(jsonObject.getString("correct_answer"), FROM_HTML_MODE_LEGACY).toString()
        //Incorrects answers gets parsed to a List of Strings
        val incorrectJsonArray = jsonObject.getJSONArray("incorrect_answers")
        val incorrect = mutableListOf<String>()
        for (i in 0 until incorrectJsonArray.length()) {
            incorrect.add(
                Html.fromHtml(
                    incorrectJsonArray[i].toString(), FROM_HTML_MODE_LEGACY
                ).toString()
            )
        }
        return Question(category, type, difficulty, question, correct, incorrect)
    }

    /**
     * Returns 10 or the amount of questions for the given category and difficulty, if that amount is < 10
     */
    private fun getMaxQuestionAmount (category: Category, difficulty: String) : Int {
        return if (difficulty.lowercase() == "easy" && category.easyCount < 10) {
            category.easyCount
        } else if (difficulty.lowercase() == "hard" && category.mediumCount < 10) {
            category.mediumCount
        } else if (difficulty.lowercase() == "hard" && category.hardCount < 10) {
            category.mediumCount
        } else {
            10
        }
    }
}