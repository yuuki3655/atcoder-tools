package atcodertools.intellij.uitest

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.testGuiFramework.fixtures.ActionButtonFixture
import com.intellij.testGuiFramework.impl.GuiTestCase
import com.intellij.testGuiFramework.impl.button
import com.intellij.testGuiFramework.impl.jList
import com.intellij.testGuiFramework.util.attempt
import com.intellij.testGuiFramework.util.step
import com.intellij.testGuiFramework.util.waitFor
import org.fest.assertions.Assertions.assertThat
import org.fest.swing.core.Robot
import org.fest.swing.exception.ComponentLookupException
import org.fest.swing.exception.WaitTimedOutError
import org.junit.Test
import java.awt.Container

class EndToEndIntegrationTest : GuiTestCase() {
    private val logger: Logger = Logger.getInstance(EndToEndIntegrationTest::class.java)

    @Test
    fun createAtCoderToolsProject() {
        welcomeFrame {
            createNewProject()
            dialog("New Project") {
                jList("AtCoderTools").clickItem("AtCoderTools")
                button("Next").click()
                typeText("agc029")
                button("Next").click()
                button("Finish").click()
            }
        }

        ideFrame {
            waitForFirstIndexing()

            editor {
                waitFor {
                    currentFile != null
                }

                // Make sure that gen command finishes successfully and the solution file of
                // the first problem is open automatically.
                val currentSelectedFile = requireNotNull(currentFile)
                assertThat(currentSelectedFile.name).isEqualTo("main.java")
                assertThat(currentSelectedFile.parent.name).isEqualTo("A")

                // Also make sure that all other solution files are opened.
                waitFor { hasTab("A/main.java") }
                waitFor { hasTab("B/main.java") }
                waitFor { hasTab("C/main.java") }
                waitFor { hasTab("D/main.java") }
                waitFor { hasTab("E/main.java") }
                waitFor { hasTab("F/main.java") }
            }

            toolwindow("AtCoderTools") {
                content("Console") {
                    step("Run AtCoderTools test command") {
                        attempt(2) {
                            waitFor {
                                !ActionButtonFixture.fixtureByTextAnyState(
                                    getContent().actionsContextComponent, myRobot, "AtCoder Stop").isEnabled
                            }
                            waitFor {
                                ActionButtonFixture.fixtureByTextAnyState(
                                    getContent().actionsContextComponent, myRobot, "AtCoder Test").isEnabled
                            }
                            waitFor {
                                ActionButtonFixture.fixtureByTextAnyState(
                                    getContent().actionsContextComponent, myRobot, "AtCoder Submit").isEnabled
                            }
                            ActionButtonFixture.fixtureByText(
                                getContent().actionsContextComponent, myRobot, "AtCoder Test"
                            ).click()
                            // Compiling file may time some time.
                            waitFor(120) {
                                findConsoleView(getContent().component, myRobot)?.editor?.document?.text
                                    ?.contains("Some cases FAILED (passed 0 of 2)") ?: false
                            }
                        }
                    }
                }
            }

            closeProjectAndWaitWelcomeFrame()
        }
    }

    private fun waitForFirstIndexing() {
        ideFrame {
            val secondToWaitIndexing = 10
            try {
                waitForStartingIndexing(secondToWaitIndexing)
            }
            catch (timedOutError: WaitTimedOutError) {
                logger.warn("Waiting for indexing has been exceeded $secondToWaitIndexing seconds")
            }
            waitForBackgroundTasksToFinish()
        }
    }

    private fun findConsoleView(container: Container, robot: Robot): ConsoleViewImpl? {
        return try {
            robot.finder().findByType(container, ConsoleViewImpl::class.java, false)
        } catch (e: ComponentLookupException) {
            null
        }
    }
}