package com.specknet.pdiotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.cxc.arduinobluecontrol.DatabaseManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {


    private val homeFragment = HomeFragment.newInstance()
    private val bluetoothFragment = BluetoothFragment.newInstance()
    private val historyFragment = HistoryFragment.newInstance()

    val fragmentList = listOf(homeFragment, bluetoothFragment, historyFragment)
    val titleList = listOf("Home", "Bluetooth", "History")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initView()
        initDate()

    }

    private fun initView() {
        val viewPager = findViewById<ViewPager>(R.id.view_pager)
        viewPager.offscreenPageLimit = 3

        viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getCount(): Int {
                return 3
            }

            override fun getItem(position: Int): Fragment {
                return fragmentList[position]
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return titleList[position]
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        bottomNavigationView.setOnNavigationItemSelectedListener {

            when (it.itemId) {
                R.id.navigation_home -> {
                    viewPager.currentItem = 0
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_bluetoot -> {
                    viewPager.currentItem = 1
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_history -> {
                    viewPager.currentItem = 2
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                Log.d("viewPager", "onPageSelected position = $position")
                when (position) {
                    0 -> {
                        bottomNavigationView.selectedItemId = R.id.navigation_home
                    }

                    1 -> {
                        bottomNavigationView.selectedItemId = R.id.navigation_bluetoot
                    }

                    2 -> {
                        bottomNavigationView.selectedItemId = R.id.navigation_history
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }

        })
    }

    private fun initDate() {

        DatabaseManager.initDatabase()
    }

    /**
     * 这个方法被BluetoothFragment调用
     * 内部去调用homeFragment
     */
    fun notifyHomeStartListen() {
        homeFragment.addIncomingDataListener();
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.home_title_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.navigation_message -> {
                startActivity(Intent(this, MessageActivity::class.java))
                true
            }

            R.id.navigation_logout -> {
                logout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        Firebase.auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()

    }

}