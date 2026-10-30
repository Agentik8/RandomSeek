package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Случайная перемотка", appName)
  }

  @Test
  fun `verify index html asset exists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inputStream = context.assets.open("index.html")
    assertNotNull(inputStream)
    val content = inputStream.bufferedReader().use { it.readText() }
    assertTrue(content.contains("Случайная перемотка"))
    assertTrue(content.contains("makeDemoBuffer"))
  }
}
