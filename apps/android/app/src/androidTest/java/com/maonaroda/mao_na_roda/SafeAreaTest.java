package com.maonaroda.mao_na_roda;

import static org.junit.Assert.*;

import android.os.Bundle;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.appbar.MaterialToolbar;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SafeAreaTest {
    @Test
    public void authToolbarStaysBelowSystemBarsAndInsetsDoNotAccumulate() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putBoolean("signIn", true);
                NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host);
                host.getNavController().navigate(R.id.authFragment, args);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View root = activity.findViewById(R.id.nav_host);
                WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(root);
                assertNotNull(windowInsets);
                Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
                assertTrue("Device must expose a top system inset", safe.top > 0);
                assertEquals(safe.top, root.getPaddingTop());
                assertEquals(safe.bottom, root.getPaddingBottom());
                MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
                assertNotNull(toolbar.getNavigationIcon());
                int[] location = new int[2];
                toolbar.getLocationOnScreen(location);
                assertTrue("Toolbar overlaps the status bar", location[1] >= safe.top);

                // Reapplying insets must not add spacing.
                ViewCompat.dispatchApplyWindowInsets(root, windowInsets);
                ViewCompat.dispatchApplyWindowInsets(root, windowInsets);
                assertEquals(safe.top, root.getPaddingTop());
                assertEquals(safe.bottom, root.getPaddingBottom());
            });
        }
    }
}
