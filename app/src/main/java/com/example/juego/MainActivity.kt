package com.example.juego

import android.animation.ObjectAnimator
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var gameLayout: FrameLayout
    private lateinit var characterView: ImageView
    private lateinit var restartButton: Button
    private lateinit var loseText: TextView
    private val enemies = mutableListOf<ImageView>()
    private var isGameOver = false
    private var spawnInterval = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameLayout = findViewById(R.id.gameLayout)
        characterView = findViewById(R.id.character)
        restartButton = findViewById(R.id.restartButton)
        loseText = findViewById(R.id.loseText)

        val gameOverLayout: LinearLayout = findViewById(R.id.gameOverLayout)

        restartButton.setOnClickListener {
            gameOverLayout.visibility = LinearLayout.GONE
            restartGame()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometer == null) {
            Toast.makeText(this, "Acelerómetro no disponible", Toast.LENGTH_SHORT).show()
        }
        if (gyroscope == null) {
            Toast.makeText(this, "Giroscopio no disponible", Toast.LENGTH_SHORT).show()
        }

        startEnemySpawn()
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || isGameOver) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                updateCharacterPosition(x, y)
            }
        }
    }

    private fun updateCharacterPosition(x: Float, y: Float) {
        val newX = (characterView.x - x * 10).coerceIn(
            0f,
            gameLayout.width.toFloat() - characterView.width
        )
        val newY = (characterView.y + y * 10).coerceIn(
            0f,
            gameLayout.height.toFloat() - characterView.height
        )

        characterView.x = newX
        characterView.y = newY

        checkCollision()
    }

    private fun createEnemy() {
        if (isGameOver || enemies.size >= 5) return

        val enemyView = ImageView(this)
        enemyView.setImageResource(R.drawable.malo)
        enemyView.layoutParams = FrameLayout.LayoutParams(150, 150)

        enemyView.x = gameLayout.width.toFloat() + 200f
        enemyView.y = Random.nextInt(0, gameLayout.height - 150).toFloat()

        gameLayout.addView(enemyView)
        enemies.add(enemyView)

        val animator = ObjectAnimator.ofFloat(enemyView, "translationX", -200f)
        animator.duration = spawnInterval
        animator.start()

        animator.doOnEnd {
            if (!isGameOver) {
                gameLayout.removeView(enemyView)
                enemies.remove(enemyView)
            }
        }
    }

    private fun startEnemySpawn() {
        android.os.Handler().postDelayed(object : Runnable {
            override fun run() {
                if (!isGameOver) {
                    createEnemy()
                    spawnInterval =
                        (spawnInterval * 0.9).coerceAtLeast(1000.0).toLong()
                    android.os.Handler().postDelayed(this, spawnInterval)
                }
            }
        }, spawnInterval)
    }

    private fun checkCollision() {
        for (enemy in enemies) {
            val characterRect = android.graphics.Rect(
                characterView.x.toInt(),
                characterView.y.toInt(),
                (characterView.x + characterView.width).toInt(),
                (characterView.y + characterView.height).toInt()
            )
            val enemyRect = android.graphics.Rect(
                enemy.x.toInt(),
                enemy.y.toInt(),
                (enemy.x + enemy.width).toInt(),
                (enemy.y + enemy.height).toInt()
            )

            if (characterRect.intersect(enemyRect)) {
                endGame()
                break
            }
        }
    }

    private fun endGame() {
        isGameOver = true

        val gameOverLayout: LinearLayout = findViewById(R.id.gameOverLayout)
        gameOverLayout.visibility = LinearLayout.VISIBLE

        for (enemy in enemies) {
            gameLayout.removeView(enemy)
        }
        enemies.clear()
    }


    private fun restartGame() {
        isGameOver = false
        spawnInterval = 1500L

        val gameOverLayout: LinearLayout = findViewById(R.id.gameOverLayout)
        gameOverLayout.visibility = LinearLayout.GONE

        for (enemy in enemies) {
            gameLayout.removeView(enemy)
        }
        enemies.clear()

        startEnemySpawn()
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
}
