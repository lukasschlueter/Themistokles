package de.codebot.silverhazard.themistokles

interface Browser {
    /**
     * The minimum timeout in ms between each call.
     *
     * This should be used in every call that results in a new request.
     * Prevents network overload and bot detection.
     *
     * Set to 0 to disable timeout.
     */
    var minimumTimeout: Int

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
    fun click(string: String, exact: Boolean = false): Boolean

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
    fun clickLink(string: String, exact: Boolean = false): Boolean

    /**
     * Check if the current page contains`text`.
     * If matching by text yields no result,`text` will be interpreted as a css selector.
     *
     * @param text The text to be found.
     */
    operator fun contains(text: String): Boolean

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
    fun executeForm(vararg inputs: Pair<String, String>): Boolean

    /**
     * Navigate to a page.
     *
     * @param url The url of the page.
     */
    fun get(url: String)

    /**
     * Return the parsed html code of the current page as a String.
     * Useful for RegEx.
     */
    fun getPageContentAsString(): String

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
    fun login(username: String, password: String, validator: (Browser) -> Boolean): Boolean

    /**
     * Shut down the Browser
     */
    fun shutdown()
}