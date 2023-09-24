package com.pyo.basic.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.workmanagernewsproviderpj.MainActivity
import com.example.workmanagernewsproviderpj.NEWS_CATEGORY
import com.example.workmanagernewsproviderpj.NEWS_TEXT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

const val BR_ACTION_NAME = "com.example.workmanagernewsproviderpj.NEW_MESSAGE_ACTION"

class NewsWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private lateinit var newsMessage: String
    val TAG: String = "로그"
    override fun onStopped() {
        super.onStopped()
        if(currentThread.isActive){
            currentThread.cancel()
            Log.d(TAG, "NewsWorker - onStopped() called")
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "Current Thread ${Thread.currentThread().name}")
        return if(newsService()){
            Result.success()
        }else{
            Result.failure()
        }
    }
    private lateinit var currentThread: CoroutineScope
    private fun newsService() : Boolean {
        var flag = false
        val newsMap: HashMap<String, String> = HashMap()
        val random = Random(System.currentTimeMillis())

        newsMap["정치"] = "한반도 통일 이루어져"
        newsMap["사회"] = "한국에 모든 대학 무상 교육 실시 드디어 이루어져"
        newsMap["경제"] = "통일한국 GNP 10만달러 달성"
        newsMap["문화"] = "소녀시대 윤아 아카데미 주연상 수상"

        var newsCategory = workerParams.inputData.getInt(NEWS_CATEGORY, -1)
        currentThread = MainScope()
        while (!Thread.currentThread().isInterrupted) {
            newsMessage = when (newsCategory) {
                0 -> newsMap["정치"]!!
                1 -> newsMap["경제"]!!
                2 -> newsMap["사회"]!!
                else -> newsMap["문화"]!!
            }
            try {
                currentThread.launch(Dispatchers.Main) {
                    /* BroadCastReceiver intent 전송 */
                    val intent = Intent(BR_ACTION_NAME)
                    intent.putExtra("news_message", newsMessage)
                    applicationContext.sendBroadcast(intent)
                //                    Toast.makeText(applicationContext, newsMessage, Toast.LENGTH_SHORT).show()
                }
                newsCategory = random.nextInt(4)
                flag = true
                Thread.sleep(3000)
            } catch (id: CancellationException) {
                flag = false
                currentThread.cancel()
            }
        }
        return flag
    }
}