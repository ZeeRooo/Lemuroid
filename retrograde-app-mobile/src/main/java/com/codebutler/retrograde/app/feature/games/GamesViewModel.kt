package com.codebutler.retrograde.app.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.LivePagedListBuilder
import com.codebutler.retrograde.lib.library.db.RetrogradeDatabase

class GamesViewModel(private val retrogradeDb: RetrogradeDatabase) : ViewModel() {

    class Factory(val retrogradeDb: RetrogradeDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GamesViewModel(retrogradeDb) as T
        }
    }

    fun getGames(systemId: String) =
            LivePagedListBuilder(retrogradeDb.gameDao().selectBySystem(systemId), 50).build()
}
