package com.trafficracer.game

import android.content.Context

class HighScoreManager(context: Context) {
    private val prefs = context.getSharedPreferences("traffic_racer_prefs", Context.MODE_PRIVATE)

    fun getHighScore(): Long = prefs.getLong("high_score", 0L)

    fun saveHighScore(score: Long) {
        prefs.edit().putLong("high_score", score).apply()
    }

    fun getTopScores(): List<Long> {
        val scores = mutableListOf<Long>()
        for (i in 0 until 5) {
            val s = prefs.getLong("top_score_$i", 0L)
            if (s > 0) scores.add(s)
        }
        return scores.sortedDescending()
    }

    fun addScore(score: Long) {
        val scores = getTopScores().toMutableList()
        scores.add(score)
        scores.sortDescending()
        val top = scores.take(5)
        val editor = prefs.edit()
        top.forEachIndexed { index, s ->
            editor.putLong("top_score_$index", s)
        }
        if (score > getHighScore()) {
            editor.putLong("high_score", score)
        }
        editor.apply()
    }

    fun getTotalGamesPlayed(): Int = prefs.getInt("total_games", 0)

    fun incrementGamesPlayed() {
        prefs.edit().putInt("total_games", getTotalGamesPlayed() + 1).apply()
    }
}
