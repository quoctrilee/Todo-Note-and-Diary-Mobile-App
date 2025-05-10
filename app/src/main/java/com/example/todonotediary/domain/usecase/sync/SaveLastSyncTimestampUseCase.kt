//package com.example.todonotediary.domain.usecase.sync
//
//import android.content.SharedPreferences
//import javax.inject.Inject
//
///**
// * Use case để lưu timestamp của lần đồng bộ hiện tại
// */
//class SaveLastSyncTimestampUseCase @Inject constructor(
//    private val sharedPreferences: SharedPreferences
//) {
//    companion object {
//        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
//    }
//
//    operator fun invoke() {
//        sharedPreferences.edit()
//            .putLong(KEY_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
//            .apply()
//    }
//}