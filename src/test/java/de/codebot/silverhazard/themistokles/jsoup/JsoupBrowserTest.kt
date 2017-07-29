package de.codebot.silverhazard.themistokles.jsoup

import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsoupBrowserTest {
    lateinit var browser: JsoupBrowser
    private val userAgent = "Themistokles/JsoupBrowserTest"

    @Before
    fun init() {
        browser = JsoupBrowser("https://www.example.com", userAgent = userAgent)
    }

    @After
    fun exit() {
        browser.shutdown()
    }

    @Test
    fun click() {
        println(browser.cookies)
        browser.get("https://httpbin.org")
        assertTrue(browser.click("/forms/post"))
        assertTrue(browser.click("input[name='topping'][value='onion']"))
        assertTrue(browser.click("Submit order"))
        assertTrue(browser.contains("\"topping\": \"onion\""))
    }

    @Test
    fun clickLink() {
        browser.get("https://httpbin.org")
        assertTrue(browser.clickLink("/get"))
        assertTrue(browser.contains("\"url\": \"https://httpbin.org/get\""))
    }

    @Test
    fun cookies() {
        assertEquals(emptyMap<String, String>(), browser.cookies)
        browser.get("https://httpbin.org/cookies/set?key1=val1&key2=val2")
        assertTrue(browser.contains(""""cookies": { "key1": "val1", "key2": "val2" }"""))
        assertEquals(mapOf("key1" to "val1", "key2" to "val2"), browser.cookies)
    }

    @Test
    fun executeForm() {
        browser.get("https://httpbin.org/forms/post")
        assertTrue {
            browser.executeForm(
                    "input[name='custname']" to "Themistokles",
                    "custemail" to "themistokles@example.com",
                    "comments" to "Add some extra machine oil and screws"
            )
        }
        assertTrue(browser.contains(""""form": { "comments": "Add some extra machine oil and screws", "custemail": "themistokles@example.com", "custname": "Themistokles", "custtel": "", "delivery": "" }"""))
    }

    @Test
    fun get() {
        browser.get("https://httpbin.org")
        assertTrue(browser.contains("HTTP Request & Response Service"))
        assertTrue(browser.contains("h1"))
    }

}