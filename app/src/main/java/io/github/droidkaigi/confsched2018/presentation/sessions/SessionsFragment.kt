package io.github.droidkaigi.confsched2018.presentation.sessions

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.droidkaigi.confsched2018.R
import io.github.droidkaigi.confsched2018.databinding.FragmentSessionsBinding
import io.github.droidkaigi.confsched2018.di.Injectable
import io.github.droidkaigi.confsched2018.model.Room
import io.github.droidkaigi.confsched2018.presentation.Result
import io.github.droidkaigi.confsched2018.util.ext.observe
import timber.log.Timber
import javax.inject.Inject

class SessionsFragment : Fragment(), Injectable {
    private lateinit var binding: FragmentSessionsBinding
    private lateinit var sessionsViewPagerAdapter: SessionsViewPagerAdapter
    private lateinit var sessionsViewModel: SessionsViewModel
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSessionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionsViewPagerAdapter = SessionsViewPagerAdapter(childFragmentManager)
        binding.sessionsViewPager.adapter = sessionsViewPagerAdapter

        sessionsViewModel = ViewModelProviders
                .of(this, viewModelFactory)
                .get(SessionsViewModel::class.java)

        sessionsViewModel.rooms.observe(this, { result ->
            when (result) {
                is Result.InProgress -> {
                    binding.progress.show()
                }
                is Result.Success -> {
                    binding.progress.hide()
                    sessionsViewPagerAdapter.setRooms(result.data)
                }
                is Result.Failure -> {
                    Timber.e(result.e)
                    binding.progress.hide()
                }
            }
        })
        sessionsViewModel.refreshResult.observe(this, { result ->
            when (result) {
                is Result.Failure -> {
                    // If user is offline, not error. So we write log to debug
                    Timber.d(result.e)
                    Snackbar.make(view, R.string.session_fetch_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        })

        lifecycle.addObserver(sessionsViewModel)

        binding.tabLayout.setupWithViewPager(binding.sessionsViewPager)
    }

    companion object {
        fun newInstance(): SessionsFragment = SessionsFragment()
    }
}

class SessionsViewPagerAdapter(
        fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager) {

    private val tabs = arrayListOf<Tab>()
    private var roomTabs = mutableListOf<Tab.RoomTab>()

    sealed class Tab(val title: String) {
        object All : Tab("All")
        data class RoomTab(val room: Room) : Tab(room.name)
    }

    private fun setupTabs() {
        tabs.clear()
        tabs.add(Tab.All)
        tabs.addAll(roomTabs)
        notifyDataSetChanged()
    }

    override fun getPageTitle(position: Int): CharSequence = tabs[position].title

    override fun getItem(position: Int): Fragment {
        val tab = tabs[position]
        return when (tab) {
            Tab.All -> {
                AllSessionsFragment.newInstance()
            }
            is Tab.RoomTab -> {
                RoomSessionsFragment.newInstance(tab.room)
            }
        }
    }

    override fun getCount(): Int = tabs.size

    fun setRooms(rooms: List<Room>) {
        roomTabs = rooms.map {
            Tab.RoomTab(it)
        }.toMutableList()
        setupTabs()
    }
}
