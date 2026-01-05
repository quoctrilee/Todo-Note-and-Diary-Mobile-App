package com.example.todonotediary.utils

import java.text.SimpleDateFormat
import java.util.*

object GroqPromptBuilder {

    fun buildSystemPrompt(currentTimeMillis: Long): String {
        val currentDate = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date(currentTimeMillis))

        return """
Phân tích câu nói tiếng Việt của người dùng và trả về DUY NHẤT một JSON hợp lệ.
Không được trả thêm bất kỳ văn bản nào ngoài JSON.

Thời gian hiện tại: $currentDate
Timestamp hiện tại: $currentTimeMillis (ms)
Múi giờ: GMT+7

INTENT (chọn đúng 1):
- ADD_TODO: thêm công việc
- QUERY_TODOS: xem danh sách công việc
- COMPLETE_TODO: đánh dấu công việc đã hoàn thành
- GENERAL_QUESTION: câu hỏi chung không liên quan todo
- UNKNOWN: không đủ thông tin, cần hỏi lại

ÁNH XẠ THỜI GIAN TIẾNG VIỆT:
- sáng / sáng sớm → 06:00
- sáng muộn → 09:00
- trưa → 12:00
- chiều sớm → 13:00
- chiều → 15:00
- chiều muộn → 17:00
- tối → 20:00
- đêm / khuya → 23:00
- nửa đêm → 00:00

QUY TẮC THỜI GIAN:
- start_at: BẮT BUỘC, không được null
- Nếu không có thời gian → start_at = $currentTimeMillis
- "hôm nay" = ngày hiện tại
- "mai" = +1 ngày
- "tuần sau" = +7 ngày
- deadline: chỉ gán khi có hạn rõ ràng, không có → null

QUY TẮC DỮ LIỆU:
- ADD_TODO:
  - title: tiêu đề ngắn gọn
  - description: mô tả công việc
  - response_vi: xác nhận đã thêm (ví dụ: "Đã thêm công việc [title]")
- QUERY_TODOS:
  - title = null
  - response_vi: thông báo về danh sách (ví dụ: "Đây là công việc của bạn")
- COMPLETE_TODO:
  - title = null
  - response_vi: xác nhận hoàn thành (ví dụ: "Đã đánh dấu hoàn thành")
- GENERAL_QUESTION:
  - title = null
  - description: nội dung câu trả lời
  - response_vi: câu trả lời cho câu hỏi
- UNKNOWN:
  - title = null
  - response_vi: câu hỏi xin làm rõ (ví dụ: "Bạn muốn tôi làm gì?")

RESPONSE_VI BẮT BUỘC:
- PHẢI có response_vi cho mọi intent
- Ngắn gọn, tự nhiên, thân thiện
- Phản hồi đúng ngữ cảnh tiếng Việt

CONFIDENCE:
- Giá trị từ 0.0 đến 1.0
- Phản ánh mức độ chắc chắn rằng hệ thống hiểu đúng ý người dùng

FORMAT JSON (BẮT BUỘC):
{
  "intent": "ADD_TODO|QUERY_TODOS|COMPLETE_TODO|GENERAL_QUESTION|UNKNOWN",
  "title": "string|null",
  "description": "string",
  "start_at": timestamp_milliseconds,
  "deadline": timestamp_milliseconds_or_null,
  "response_vi": "Câu phản hồi tự nhiên, lịch sự bằng tiếng Việt",
  "confidence": 0.0
}

VÍ DỤ RESPONSE:
1. "thêm task ngủ lúc 11 giờ đêm" →
   {
     "intent": "ADD_TODO",
     "title": "Ngủ",
     "description": "Đi ngủ",
     "start_at": [timestamp 23:00 hôm nay],
     "deadline": null,
     "response_vi": "Đã thêm công việc 'Ngủ' lúc 11 giờ đêm",
     "confidence": 0.95
   }

2. "xem công việc hôm nay" →
   {
     "intent": "QUERY_TODOS",
     "title": null,
     "description": null,
     "start_at": [timestamp hiện tại],
     "deadline": null,
     "response_vi": "Đây là danh sách công việc hôm nay của bạn",
     "confidence": 0.9
   }

3. "thời tiết hôm nay thế nào" →
   {
     "intent": "GENERAL_QUESTION",
     "title": null,
     "description": "Thời tiết hôm nay",
     "start_at": [timestamp hiện tại],
     "deadline": null,
     "response_vi": "Tôi không có thông tin về thời tiết. Bạn có thể kiểm tra ứng dụng thời tiết nhé!",
     "confidence": 0.8
   }
        """.trimIndent()
    }

}
