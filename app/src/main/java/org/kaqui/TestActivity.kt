package org.kaqui

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.NestedScrollView
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.*
import org.kaqui.model.*
import android.graphics.Rect

class TestActivity : TestActivityBase() {
    companion object {
        private const val TAG = "TestActivity"
        private const val COLUMNS = 2
    }

    private lateinit var answerTexts: List<TextView>

    private lateinit var testLayout: TestLayout

    override val historyScrollView: NestedScrollView get() = testLayout.historyScrollView
    override val historyActionButton: FloatingActionButton get() = testLayout.historyActionButton
    override val historyView: LinearLayout get() = testLayout.historyView
    override val sessionScore: TextView get() = testLayout.sessionScore
    override val mainView: View get() = testLayout.mainView
    override val mainCoordLayout: CoordinatorLayout get() = testLayout.mainCoordinatorLayout
    private lateinit var dontKnowButton: Button

    override val testType
        get() = intent.extras.getSerializable("test_type") as TestType

    override fun onCreate(savedInstanceState: Bundle?) {
        val answerCount = getAnswerCount(testType)
        val answerTexts = mutableListOf<TextView>()

        val questionMinSize =
                when (testType) {
                    TestType.WORD_TO_READING, TestType.WORD_TO_MEANING, TestType.KANJI_TO_READING, TestType.KANJI_TO_MEANING -> 50
                    TestType.READING_TO_WORD, TestType.MEANING_TO_WORD, TestType.READING_TO_KANJI, TestType.MEANING_TO_KANJI -> 10
                    TestType.HIRAGANA_TO_ROMAJI, TestType.ROMAJI_TO_HIRAGANA, TestType.KATAKANA_TO_ROMAJI, TestType.ROMAJI_TO_KATAKANA -> 50
                    else -> throw RuntimeException("unsupported test type for TestActivity")
                }

        testLayout = TestLayout(this) { testLayout ->
            testLayout.makeMainBlock(this@TestActivity, this, questionMinSize) {
                testLayout.wrapInScrollView(this) {
                    verticalLayout {
                        view {
                            backgroundColor = Color.rgb(0xcc, 0xcc, 0xcc)
                        }.lparams(width = matchParent, height = dip(1))
                        when (testType) {
                            TestType.WORD_TO_READING, TestType.WORD_TO_MEANING, TestType.KANJI_TO_READING, TestType.KANJI_TO_MEANING -> {
                                repeat(answerCount) {
                                    verticalLayout {
                                        linearLayout {
                                            gravity = Gravity.CENTER_VERTICAL

                                            val answerText = textView().lparams(weight = 1f)
                                            val position = answerTexts.size
                                            answerTexts.add(answerText)

                                            button(R.string.maybe) {
                                                setExtTint(R.color.answerMaybe)
                                                setOnClickListener { onAnswerClicked(this, Certainty.MAYBE, position) }
                                            }
                                            button(R.string.sure) {
                                                setExtTint(R.color.answerSure)
                                                setOnClickListener { onAnswerClicked(this, Certainty.SURE, position) }
                                            }
                                        }
                                        view {
                                            backgroundColor = Color.rgb(0xcc, 0xcc, 0xcc)
                                        }.lparams(width = matchParent, height = dip(1))
                                    }
                                }
                            }

                            TestType.READING_TO_WORD, TestType.MEANING_TO_WORD, TestType.READING_TO_KANJI, TestType.MEANING_TO_KANJI, TestType.HIRAGANA_TO_ROMAJI, TestType.ROMAJI_TO_HIRAGANA, TestType.KATAKANA_TO_ROMAJI, TestType.ROMAJI_TO_KATAKANA -> {
                                repeat(answerCount / COLUMNS) {
                                    linearLayout {
                                        repeat(COLUMNS) {
                                            verticalLayout {
                                                linearLayout {
                                                    gravity = Gravity.CENTER_VERTICAL

                                                    val answerText = textView {
                                                        setTextSize(TypedValue.COMPLEX_UNIT_SP,
                                                                when (testType) {
                                                                    TestType.READING_TO_WORD, TestType.MEANING_TO_WORD -> 30f
                                                                    else -> 50f
                                                                })
                                                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                                                    }.lparams(weight = 1f)
                                                    val position = answerTexts.size
                                                    answerTexts.add(answerText)

                                                    verticalLayout {
                                                        button(R.string.sure) {
                                                            minimumHeight = 0
                                                            minimumWidth = 0
                                                            minHeight = 0
                                                            minWidth = 0
                                                            setExtTint(R.color.answerSure)
                                                            setOnClickListener { onAnswerClicked(this, Certainty.SURE, position) }
                                                        }.lparams(width = matchParent, height = wrapContent)
                                                        button(R.string.maybe) {
                                                            minimumHeight = 0
                                                            minimumWidth = 0
                                                            minHeight = 0
                                                            minWidth = 0
                                                            setExtTint(R.color.answerMaybe)
                                                            setOnClickListener { onAnswerClicked(this, Certainty.MAYBE, position) }
                                                        }.lparams(width = matchParent, height = wrapContent)
                                                    }.lparams(weight = 0f)
                                                }.lparams(width = matchParent, height = wrapContent)
                                                view {
                                                    backgroundColor = Color.rgb(0xcc, 0xcc, 0xcc)
                                                }.lparams(width = matchParent, height = dip(1))
                                            }.lparams(weight = 1f)
                                        }
                                    }.lparams(width = matchParent, height = wrapContent)
                                }
                            }
                            else -> throw RuntimeException("unsupported test type for TestActivity")
                        }
                        dontKnowButton = button(R.string.dont_know) {
                            setExtTint(R.color.answerDontKnow)
                            setOnClickListener { this@TestActivity.onAnswerClicked(this, Certainty.DONTKNOW, 0) }
                        }.lparams(width = matchParent)
                    }.lparams(width = matchParent, height = wrapContent)
                }
            }
        }

        testLayout.questionText.setOnLongClickListener {
            if (testEngine.currentDebugData != null)
                showItemProbabilityData(testEngine.currentQuestion.text, testEngine.currentDebugData!!)
            true
        }

        this.answerTexts = answerTexts

        super.onCreate(savedInstanceState)

        showCurrentQuestion()
    }

    override fun showCurrentQuestion() {
        super.showCurrentQuestion()

        testLayout.questionText.text = testEngine.currentQuestion.getQuestionText(testType)

        for (i in 0 until testEngine.answerCount) {
            answerTexts[i].text = testEngine.currentAnswers[i].getAnswerText(testType)
        }
    }

    private fun onAnswerClicked(button: View, certainty: Certainty, position: Int) {
        val result = testEngine.selectAnswer(certainty, position)

        val offsetViewBounds = Rect()
        button.getDrawingRect(offsetViewBounds)
        testLayout.mainCoordinatorLayout.offsetDescendantRectToMyCoords(button, offsetViewBounds)
        testLayout.overlay.trigger(offsetViewBounds.centerX(), offsetViewBounds.centerY(), resources.getColor(result.toColorRes()))

        testEngine.prepareNewQuestion()
        showCurrentQuestion()
    }
}
