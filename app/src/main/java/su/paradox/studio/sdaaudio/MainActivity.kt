package su.paradox.studio.sdaaudio

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL


class MainActivity : AppCompatActivity() {
    var playlist: List<track>? = null
    var mp: MediaPlayer? = null
    var nowPlay = 0
    var totalTime: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        val line = URL("https://sda-audio.ru/ajax_app.php").readText()
        val gson = GsonBuilder().create()
        playlist = gson.fromJson(line, Array<track>::class.java).toList()

        initTrack()

        img_play.setOnClickListener {
            if (mp?.isPlaying!!) pauseMusic()
            else playMusic()
        }

        img_previous.setOnClickListener {
            previousTrack()
        }

        img_next.setOnClickListener {
            nextTrack()
        }
    }

    private fun pauseMusic() {
        mp?.pause()
        img_play.setImageResource(R.drawable.ic_play_arrow_black_24dp)
    }

    private fun playMusic() {
        mp?.start()
         img_play.setImageResource(R.drawable.ic_pause_black_24dp)
    }

    private fun initTrack() {
        text_name.text = playlist!![nowPlay].UF_NAME
        text_singer.text = if (playlist!![nowPlay].UF_SINGER == "") "Неизвестный исполнитель" else playlist!![nowPlay].UF_SINGER

        mp = MediaPlayer()
        mp?.setDataSource(this, Uri.parse("https://sda-audio.ru"+playlist!![nowPlay].UF_URL))
        mp?.prepare()
        mp?.setOnCompletionListener {
            nextTrack()
        }

        totalTime = mp?.duration
        seek_track.max = totalTime!!

        seek_track.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mp?.seekTo(progress)
                    seek_track.progress = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        Thread(Runnable {
            while (mp != null) {
                try {
                    val msg = Message()
                    msg.what = mp?.currentPosition!!
                    handler.sendMessage(msg)
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {}
            }
        }).start()

    }

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            val currentPosition = msg.what
            seek_track.progress = currentPosition
            val elapsedTime: String = createTimeLabel(currentPosition).toString()
            text_start.text = elapsedTime
            val remainingTime: String = createTimeLabel(totalTime?.minus(currentPosition)!!).toString()
            text_end.text = "-$remainingTime"
        }
    }

    fun createTimeLabel(time: Int): String? {
        var timeLabel: String?
        val min = time / 1000 / 60
        val sec = time / 1000 % 60
        timeLabel = "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec
        return timeLabel
    }

    private fun nextTrack() {
        pauseMusic()
        mp?.release()
        if (playlist?.size == nowPlay) nowPlay = -1
        nowPlay += 1
        initTrack()
        playMusic()
    }

    private fun previousTrack() {
        pauseMusic()
        mp?.release()
        nowPlay -= 1
        initTrack()
        playMusic()
    }
}

class track (var ID: String, var UF_URL: String, var UF_NAME: String, var UF_DESC: String, var UF_AUTHOR_MUSIC: String, var UF_AUTHOR_LYRYCS: String, var UF_CATEGORY: String, var UF_SINGER: String, var UF_LANG: String)