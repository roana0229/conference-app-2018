package io.github.droidkaigi.confsched2018.presentation.search

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v7.widget.SearchView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.github.droidkaigi.confsched2018.R
import io.github.droidkaigi.confsched2018.databinding.FragmentSearchBinding
import io.github.droidkaigi.confsched2018.di.Injectable
import io.github.droidkaigi.confsched2018.model.Session
import io.github.droidkaigi.confsched2018.presentation.NavigationController
import io.github.droidkaigi.confsched2018.presentation.Result
import io.github.droidkaigi.confsched2018.presentation.common.binding.FragmentDataBindingComponent
import io.github.droidkaigi.confsched2018.presentation.search.item.SearchResultSpeakerItem
import io.github.droidkaigi.confsched2018.presentation.search.item.SearchSpeakersSection
import io.github.droidkaigi.confsched2018.presentation.sessions.item.SimpleSessionsSection
import io.github.droidkaigi.confsched2018.presentation.sessions.item.SpeechSessionItem
import io.github.droidkaigi.confsched2018.util.ext.color
import io.github.droidkaigi.confsched2018.util.ext.eachChildView
import io.github.droidkaigi.confsched2018.util.ext.observe
import io.github.droidkaigi.confsched2018.util.ext.toGone
import io.github.droidkaigi.confsched2018.util.ext.toVisible
import timber.log.Timber
import javax.inject.Inject

class SearchFragment : Fragment(), Injectable {
    private lateinit var binding: FragmentSearchBinding
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var navigationController: NavigationController

    private val sessionsSection = SimpleSessionsSection(this)
    private val speakersSection = SearchSpeakersSection(FragmentDataBindingComponent(this))

    private val searchViewModel: SearchViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
    }

    private val onFavoriteClickListener = { session: Session.SpeechSession ->
        // Since it takes time to change the favorite state, change only the state of View first
        session.isFavorited = !session.isFavorited
        binding.sessionsRecycler.adapter.notifyDataSetChanged()

        searchViewModel.onFavoriteClick(session)
    }

    private var searchedQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchBeforeTabs()
        setupSearch()
    }

    private fun setupSearchBeforeTabs() {
        binding.sessionsViewPager.adapter =
                SearchBeforeViewPagerAdapter(context!!, childFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.sessionsViewPager)
    }

    private fun setupSearch() {
        setupRecyclerView()
        searchViewModel.result.observe(this, { result ->
            when (result) {
                is Result.Success -> {
                    val searchResult = result.data
                    sessionsSection.updateSessions(searchResult.sessions, onFavoriteClickListener, searchedQuery)
                    speakersSection.updateSpeakers(searchResult.speakers)
                    binding.sessionsRecycler.scrollToPosition(0)
                    binding.sessionsRecycler.adapter.notifyDataSetChanged()
                }
                is Result.Failure -> {
                    Timber.e(result.e)
                }
            }
        })
    }

    private fun setupRecyclerView() {
        val groupAdapter = GroupAdapter<ViewHolder>().apply {
            add(sessionsSection)
            add(speakersSection)
            setOnItemClickListener({ item, _ ->
                when (item) {
                    is SpeechSessionItem -> {
                        navigationController.navigateToSessionDetailActivity(item.session)
                    }
                    is SearchResultSpeakerItem -> {
                        navigationController.navigateToSpeakerDetailActivity(item.speaker.id)
                    }
                }
            })
        }
        binding.sessionsRecycler.apply {
            adapter = groupAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater!!.inflate(R.menu.search, menu)
        val menuSearchItem = menu!!.findItem(R.id.action_search)

        val searchView = menuSearchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()
                searchedQuery = query
                searchViewModel.onQuery(query)
                if (query.isNotBlank()) {
                    binding.searchBeforeGroup.toGone()
                    binding.searchResultGroup.toVisible()
                } else {
                    binding.searchBeforeGroup.toVisible()
                    binding.searchResultGroup.toGone()
                }
                return false
            }
        })
        changeSearchViewTextColor(searchView)
    }

    private fun changeSearchViewTextColor(view: View) {
        if (view is TextView) {
            view.setTextColor(context!!.color(R.color.app_bar_text_color))
        }

        if (view is ViewGroup) {
            view.eachChildView {
                changeSearchViewTextColor(it)
            }
        }
    }

    companion object {
        fun newInstance(): SearchFragment = SearchFragment()
    }
}

class SearchBeforeViewPagerAdapter(
        val context: Context,
        fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager) {

    enum class Tab(@StringRes val title: Int) {
        Session(R.string.search_before_tab_session),
        Topic(R.string.search_before_tab_topic),
        Speakers(R.string.search_before_tab_speaker);
    }

    override fun getPageTitle(position: Int): CharSequence =
            context.getString(Tab.values()[position].title)

    override fun getItem(position: Int): Fragment {
        val tab = Tab.values()[position]
        return when (tab) {
            Tab.Session -> SearchSessionsFragment.newInstance()
            Tab.Topic -> SearchTopicsFragment.newInstance()
            Tab.Speakers -> SearchSpeakersFragment.newInstance()
        }
    }

    override fun getCount(): Int = Tab.values().size
}
