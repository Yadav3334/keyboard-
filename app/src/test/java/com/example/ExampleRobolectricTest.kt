package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Keyboard Settings", appName)
  }

  @Test
  fun `settings activity launches successfully`() {
    ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertNotNull(activity)
      }
    }
  }

  @Test
  fun `activity alias component state toggles properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageManager = context.packageManager
    val aliasComponent = ComponentName(context, "com.example.SettingsActivityAlias")

    // Disable alias (hide icon)
    packageManager.setComponentEnabledSetting(
      aliasComponent,
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      packageManager.getComponentEnabledSetting(aliasComponent)
    )

    // Enable alias (show icon)
    packageManager.setComponentEnabledSetting(
      aliasComponent,
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP
    )
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      packageManager.getComponentEnabledSetting(aliasComponent)
    )
  }
}

