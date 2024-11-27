package com.example.tasktimer

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.tasktimer.view.AlgorithmFragment
import com.example.tasktimer.view.TaskFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)

        // Устанавливаем адаптер для ViewPager2
        viewPager.adapter = MainPagerAdapter(this)

        // Связываем TabLayout с ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Задачи" // Название вкладки
                1 -> "Алгоритмы"
                else -> throw IllegalStateException("Unexpected position: $position")
            }
        }.attach()
    }
}
class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2 // У нас 2 вкладки: Задачи и Алгоритмы

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TaskFragment() // Возвращаем фрагмент для задач
            1 -> AlgorithmFragment() // Возвращаем фрагмент для алгоритмов
            else -> throw IllegalStateException("Unexpected position: $position")
        }
    }
}