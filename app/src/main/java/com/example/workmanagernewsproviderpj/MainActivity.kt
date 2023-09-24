package com.example.workmanagernewsproviderpj

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.workmanagernewsproviderpj.databinding.ActivityMainBinding
import com.pyo.basic.work.NewsWorker

const val WORKER_STAGE_TAG = "current_worker_state"
const val NEWS_CATEGORY = "news_kind"
const val NEWS_TEXT = "news_text"

class MainActivity : AppCompatActivity() {
    val TAG: String = "로그"
    private lateinit var binding: ActivityMainBinding
    /* Service를 받을 BroadcastReceiver onReceive 메서드 재정의 */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.workmanagernewsproviderpj.NEW_MESSAGE_ACTION") {
                val newsMessage = intent.getStringExtra("news_message")
                if (newsMessage != null) {
                    Log.d(TAG, "MainActivity - onReceive() called ${newsMessage}")
                    val currentText = binding.newsTv.text.toString()

                    /** EditText setText, clear를 해주어야 이전 텍스트를 지운 후
                     * 이전 텍스트 + 다음 텍스트를 같이 추가할 수 있다.
                     * append가 아닌 setText로 하면 계속 변경되므로 append를 이용하여 텍스트를 추가해야 한다
                     * */
                    binding.newsTv.text.clear()
                    binding.newsTv.append("${currentText}\n ${newsMessage}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        /* BroadCastReceiver 등록 */
        val filter = IntentFilter("com.example.workmanagernewsproviderpj.NEW_MESSAGE_ACTION")
        registerReceiver(receiver, filter)

        /* Button 동작 */
        with(binding) {
            startBtn.setOnClickListener {
                startWorkerManager()
                stopBtn.isEnabled = true
                startBtn.isEnabled = false
            }
            stopBtn.setOnClickListener {
                workerManager.cancelWorkById(newsWorkRequest.id)
            }
        }
    }


    private lateinit var workerManager: WorkManager
    private lateinit var newsWorkRequest: OneTimeWorkRequest

    @SuppressLint("RestrictedApi")
    private fun startWorkerManager() {
        val workerConstraints = Constraints.Builder()
            /**
             * +++ 제약 사향 ++++++++++
             * + 단말기에서 Network 가능
             * + 충전중이 아니라도 상관 없음
             * + 밧데리가 부족하면 안된다
             * + 단말기에 저장공간은 상관없다
             */
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(false)
            .build()
        newsWorkRequest = OneTimeWorkRequestBuilder<NewsWorker>()
            //.addTag(NEWS_WORKER_TAG)
            .setInputData(Data(mutableMapOf(NEWS_CATEGORY to 2))) //초기에 넘겨줄 데이터 설정
            .setConstraints(workerConstraints)
            .build()
        workerManager = WorkManager.getInstance(this@MainActivity)
        workerManager.apply {
            enqueue(newsWorkRequest)
            Log.d(TAG, "데이터가 잘 넘어오는지 확인")
//            binding.newsTv.text =
            /**
             * 비동기 메소드 존재(LiveData, Listener):주기적으로 Worker를 모니터링 할 수 있다
             */
            getWorkInfoById(newsWorkRequest.id).get()?.let {
                when (it.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.e(WORKER_STAGE_TAG, it.state.name)
                    }

                    WorkInfo.State.FAILED -> {
                        Log.e(WORKER_STAGE_TAG, it.state.name)
                    }

                    WorkInfo.State.RUNNING -> {
                        Log.e(WORKER_STAGE_TAG, it.state.name)
                    }
                    else -> {
                        Log.e(WORKER_STAGE_TAG, it.state.name)
                    }
                }

            } ?: Log.e(WORKER_STAGE_TAG, "WorkInfo state null")

        }
        Log.d(TAG, "${Thread.currentThread().name}-> Main Activity Finish")
    }
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "MainActivity - onStop() called")
        if(::workerManager.isInitialized){
            workerManager.cancelWorkById(newsWorkRequest.id)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /* BroadcastReceiver 해제 */
        Log.d(TAG, "MainActivity - onDestroy() called")
        unregisterReceiver(receiver)
    }
}