package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOption
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsPreferenceHelper
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.security.InvalidParameterException

class GameMenuCoreOptionsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.empty_preference_screen)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val coreOptions = arguments?.getSerializable(GameMenuContract.EXTRA_CORE_OPTIONS) as Array<CoreOption>?
            ?: throw InvalidParameterException("Missing EXTRA_CORE_OPTIONS")

        val game = arguments?.getSerializable(GameMenuContract.EXTRA_GAME) as Game?
            ?: throw InvalidParameterException("Missing EXTRA_GAME")

        coreOptions
            .map {
                CoreOptionsPreferenceHelper.convertToPreference(
                    preferenceScreen.context,
                    it,
                    game.systemId
                )
            }
            .forEach { preferenceScreen.addPreference(it) }
    }

    @dagger.Module
    class Module
}
