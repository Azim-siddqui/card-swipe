package com.example.testapplication

import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.animation.doOnEnd
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewPager = findViewById<ViewPager2>(R.id.pager)
        val adapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = adapter
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment = when(position){
            0-> FirstFragement()
            1-> SecondFragement()
            2-> ThirdFragment()
            else->FirstFragement()
        }
    }
}

