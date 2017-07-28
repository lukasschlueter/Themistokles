package de.codebot.silverhazard.themistokles.jsoup

import de.codebot.silverhazard.themistokles.Browser
import de.codebot.silverhazard.themistokles.compareTo
import de.codebot.silverhazard.themistokles.isValidEmail
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import org.jsoup.select.Elements
import java.io.IOException

class JsoupBrowser(initialPage: String, override var minimumTimeout: Int = 1000, private val userAgent: String =
"Themistokles/0.1") : Browser {

    val cookies = mutableMapOf<String, String>()
    lateinit var document: Document
    private var referrer = "https://www.google.com"
    private lateinit var raw: String    // The plain response as a String


    init {
        get(initialPage)
    }


    /**
     * Wait until the time since last call is larger than minimum timeout.
     *
     * @see Browser.minimumTimeout
     */
    private fun handleTimeout() {
        val now = System.currentTimeMillis()
        val delta = now - lastExecution

        if (delta < minimumTimeout)
            Thread.sleep(minimumTimeout - delta)

        lastExecution = System.currentTimeMillis()
    }

    private var lastExecution = System.currentTimeMillis()

    /**
     * Click on a string.
     *
     * This method will try matching by id, name and text and interpreting
     * `string` as a css selector. It will use the first element found.
     *
     * @param string The string to be clicked.
     * @param exact Whether it should be an exact match. Used for text only.
     * @return `true` if at least one element is found and the click was successful.
     * `false` otherwise.
     */
    override fun click(string: String, exact: Boolean): Boolean {
        handleTimeout()

        var element: Element?

        element = document.getElementById(string)
        if (element == null) element = document.getElementsByAttributeValue("name", string).firstOrNull()
        if (element == null) element = document.getElementsContainingOwnText(string).firstOrNull()
        if (element == null) element = document.select(string).firstOrNull()
        if (element == null) return false

        return click(element)
    }

    /**
     * Click on a [Element]
     *
     * Handles buttons, checkboxes, option boxes and links. If [element] is not clickable, tries clicking its parent.
     *
     * Does **not** handle timeout!
     *
     * @param element [Element] to click
     */
    private fun click(element: Element): Boolean {
        // Stop the recursion
        if (element.tagName() in listOf("html", "head", "body"))
            return false

        // If element is a button or a submit input
        if (element.tagName() == "button" || element.tagName() == "input" && element.attr("type") == "submit") {
            // Execute the parent form
            val form = element.parents().first { it.tagName() == "form" } as FormElement
            form.exec()
            return true

        } else if (element is FormElement) {
            element.exec()
            return true

        } else if (element.tagName() == "input" && element.attr("type") == "checkbox") {
            // element is a checkbox, invert its state
            val isChecked = element.attr("checked") == "checked"

            if (isChecked) element.removeAttr("checked")
            else element.attr("checked", "checked")

            return true

        } else if (element.tagName() == "option") {
            // element is an option element, invert its state
            val isSelected = element.attr("selected") == "selected"

            if (isSelected) element.removeAttr("selected")
            else element.attr("selected", "selected")

            return true

        }

        // element has no special tag, assume it's a link

        if (!element.hasAttr("href")) {
            // element itself is not "clickable", try to click its parent
            return click(element.parent())
        }

        // "click" element
        get(element.absUrl("href"))
        return true
    }

    /**
     * Click on a link.
     *
     * This method will try to find a link using its text.
     * If none is found, `string` is interpreted as a css selector.
     *
     * @param string The string identifying the link.
     * @param exact Whether the whole text should be matched instead of allowing a partial match.
     * @param `true` if at least one element is found and the click on the first element
     * was successful. `false` otherwise.
     */
    override fun clickLink(string: String, exact: Boolean): Boolean {
        handleTimeout()

        val elements: Elements

        if (exact) {
            // Match the whole string
            elements = document.getElementsMatchingOwnText("^$string$")
        } else {
            elements = document.getElementsContainingOwnText(string)
        }

        if (elements.size == 0)
            elements.addAll(document.select(string))
        if (elements.size == 0) return false

        // "click" element
        get(elements.first().absUrl("href"))
        return true
    }

    /**
     * Check if the current page contains`text`.
     * If matching by text yields no result,`text` will be interpreted as a css selector.
     *
     * @param text The text to be found.
     */
    override fun contains(text: String): Boolean {
        return document.getElementsContainingText(text).isNotEmpty() || document.select(text).isNotEmpty()
    }

    /**
     * Executes a [request][Connection].
     *
     * Sets or overrides cookies, user agent and referrer of the request and removes the timeout.
     * Stores the response as parsed web page in [document] and the raw response in [raw].
     *
     * @param request The request to be executed.
     * @param overrideRedirectPolicy Whether [request.followRedirects][Connection.followRedirects] should be overridden to true.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun execute(request: Connection, overrideRedirectPolicy: Boolean = true) {
        request.ignoreHttpErrors(true)
        request.ignoreContentType(true)
        request.cookies(cookies).userAgent(userAgent).referrer(referrer).timeout(0)

        // Follows redirects unless specified otherwise
        if (overrideRedirectPolicy) {
            request.followRedirects(true)
        }

        // Finally executeWithoutValue the request
        val response = request.execute()

        // Adds all cookies to the cookie store
        // TODO: Handle cookies getting deleted
        cookies.putAll(response.cookies())

        // Update properties
        referrer = response.url().toExternalForm()
        document = response.parse()
        raw = response.body()
    }

    /**
     * Execute a form using specified `inputs`.
     *
     * This method will try to find a form with an input element for all names of `inputs`
     * using id, name, text and interpretation as a css selector.
     *
     * @param inputs List of [pairs][Pair] (name: String, value: String)
     * @return`true` if a form with all names exists and its execution succeeds.
     * `false` otherwise.
     */
    override fun executeForm(vararg inputs: Pair<String, String>): Boolean {
        handleTimeout()

        val forms = document.select("form")

        forms@ for (form in forms) {
            // This can never happen, just do the smart cast to FormElement
            if (form !is FormElement)
                continue@forms

            // Store everything that could be the desired element
            val inputElements = form.select("input")
            inputElements.addAll(form.select("textarea"))

            // Some inputs are hidden and their values are set by a different element
            // These "helper" elements might be marked by some role attribute
            inputElements.addAll(form.getElementsByAttribute("role")) // Maybe a little too permissive

            if (inputs > inputElements) continue // We don't have enough elements for all inputs

            for ((name, value) in inputs) {
                // Try to find a matching element for the input

                // TODO: Improve this part, its not very nice to read
                // Look for $name in id, name and text
                var element = inputElements.singleOrNull { it.id() == name }
                if (element == null) element = inputElements.singleOrNull { it.attr("name") == name }
                if (element == null) element = inputElements.singleOrNull { name in it.text() }

                // Nothing found, maybe $name is a css selector?
                if (element == null) element = form.select(name).singleOrNull()

                if (element == null) continue@forms // No element found, assume it's the wrong form

                // Hooray, we found an element
                element.`val`(value)
            }
            form.exec()
            return true
        }
        // No fitting form was found
        return false
    }

    /**
     * Navigate to a page.
     *
     * @param url The url of the page.
     */
    override fun get(url: String) = execute(url.connect())

    /**
     * Return the parsed html code of the current page as a String.
     * Useful for RegEx.
     */
    override fun getPageContentAsString(): String = raw

    /**
     * Log in to current page using `username` and `password`. Uses `validator` to check for success.
     *
     * This method will try to find the login form by looking at its inputs' types.
     * It uses type "text" for the username and type "password" for the password.
     * If `username` is a valid email, type "email" will additionally be tried.
     * After the form is executed, `validator` will be run. If it returns `false`,
     * the remaining forms will be tried.
     *
     * @param username The username used to login.
     * @param password The password for the login.
     * @param validator The validator used to determine if the login was successful.
     * @return `true` if a form is found and `validator` returns `true`.
     */
    override fun login(username: String, password: String, validator: (Browser) -> Boolean): Boolean {
        handleTimeout()

        val forms = document.select("form")

        for (form in forms) {
            // This can never happen, just do the smart cast to FormElement
            if (form !is FormElement)
                continue

            var usernameInput: Element? = null

            // Store which inputs might be relevant
            val inputs = form.select("input")

            if (username.isValidEmail()) {
                // Try to find an email input
                usernameInput = inputs.singleOrNull { it.attr("type") == "email" }
            }

            // No single email input found or username is not an email address
            if (usernameInput == null) {
                // Try to find an text input
                usernameInput = inputs.singleOrNull {
                    it.attr("type") == "text"
                } ?: continue // Nothing found, next form
            }

            // Password should be in a password input
            val passwordInput = inputs.singleOrNull {
                it.attr("type") == "password"
            } ?: continue // Nothing found, next form

            usernameInput.`val`(username)

            passwordInput.`val`(password)

            // Save document for failure
            val backup = document

            // Seems to be a good form, let's try
            form.exec()

            if (validator(this)) return true

            // Nope, that didn't work, try again
            document = backup
            // TODO: Might need to reset cookies as well
        }
        return false
    }

    /**
     * Shut down the Browser
     */
    override fun shutdown() {
        // Nothing to do actually
    }

    /* Extension methods */

    private fun String.connect() = Jsoup.connect(this)!!

    private fun FormElement.exec() = execute(this.submit())
}