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
        assertEquals(ThemeStyle.CELADON, ThemeStyle.from("celadon"))
        assertEquals(ThemeStyle.INK_GOLD, ThemeStyle.from("ink_gold"))
        assertEquals(ThemeMode.DARK, ThemeMode.from("dark"))
        assertEquals(ThemeStyle.CLASSIC, ThemeStyle.from("dark"))
    }
}
