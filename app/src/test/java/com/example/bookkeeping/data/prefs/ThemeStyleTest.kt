package com.example.bookkeeping.data.prefs

import org.junit.Assert.*
import org.junit.Test

class ThemeStyleTest {
    @Test fun existingInstallationUsesClassicWhenSettingMissing() {
        assertEquals(ThemeStyle.CLASSIC, ThemeStyle.from(null))
    }
    @Test fun unknownFutureStyleSafelyFallsBackToClassic() {
        assertEquals(ThemeStyle.CLASSIC, ThemeStyle.from("future-theme"))
    }
    @Test fun savedStyleIdentifiersAreStableAndIndependentOfBrightness() {
        assertEquals(ThemeStyle.SONG, ThemeStyle.from("song"))
        assertEquals(ThemeMode.DARK, ThemeMode.from("dark"))
        assertEquals(ThemeStyle.CLASSIC, ThemeStyle.from("dark"))
    }
}
