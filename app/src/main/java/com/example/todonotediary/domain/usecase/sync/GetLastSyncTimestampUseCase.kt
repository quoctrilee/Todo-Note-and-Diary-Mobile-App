//package com.example.todonotediary.domain.usecase.sync
//
//import android.content.SharedPreferences
//import javax.inject.Inject
//
///**
// * Use case để lấy timestamp của lần đồng bộ cuối cùng
// */
//class GetLastSyncTimestampUseCase @Inject constructor(
//    private val sharedPreferences: SharedPreferences
//) {
//    companion object {
//        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
//    }
//
//    operator fun invoke(): Long {
//        return sharedPreferences.getLong(KEY_LAST_SYNC_TIMESTAMP, 0)
//    }
//}