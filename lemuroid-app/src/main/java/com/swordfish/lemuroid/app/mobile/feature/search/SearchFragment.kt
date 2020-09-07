package com.swordfish.lemuroid.app.mobile.feature.search

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.mobile.shared.GamesAdapter
import com.swordfish.lemuroid.app.mobile.shared.RecyclerViewFragment
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.ui.setVisibleOrGone
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchFragment : RecyclerViewFragment() {

    @Inject lateinit var retrogradeDb: RetrogradeDatabase
    @Inject lateinit var gameInteractor: GameInteractor

    private lateinit var searchViewModel: SearchViewModel

    private var searchSubject: PublishSubject<String> = PublishSubject.create()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
        setupSearchMenuItem(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = ViewModelProviders.of(this, SearchViewModel.Factory(retrogradeDb))
            .get(SearchViewModel::class.java)
    }

    private fun setupSearchMenuItem(menu: Menu) {
        val searchItem = menu.findItem(R.id.action_search)
        searchItem.expandActionView()
        searchItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    activity?.onBackPressed()
                    return true
                }

                override fun onMenuItemActionExpand(item: MenuItem?) = true
            }
        )

        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Integer.MAX_VALUE
        searchView.setQuery(searchViewModel.queryString.value, false)
        searchView.queryTextChanges()
            .debounce(1, TimeUnit.SECONDS)
            .map { it.toString() }
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribe(searchSubject)
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()

        val gamesAdapter = GamesAdapter(R.layout.layout_game_list, gameInteractor)
        searchViewModel.searchResults.observe(this) {
            gamesAdapter.submitList(it)
        }

        searchViewModel.emptyViewVisible.observe(this) {
            emptyView?.setVisibleOrGone(it)
        }

        searchSubject
            .distinctUntilChanged()
            .autoDispose(scope())
            .subscribe { searchViewModel.queryString.postValue(it) }

        recyclerView?.apply {
            adapter = gamesAdapter
            layoutManager = LinearLayoutManager(context)
        }
        restoreRecyclerViewState()
    }

    @dagger.Module
    class Module
}
