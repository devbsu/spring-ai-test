package com.example.demo.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DateTimeTools {
    // LLM이 사용할 수 있는 도구가 된다 `@Tool` 언제 사용할 수 있는지 설명해준다
    @Tool(description = "현재 날짜와 시간 정보를 제공한다.")
    public String getCurrentDateTime() {
        // LLM이 사용할 수 있도록 매개값으로 줄 값이 있는지 결정한다

        // 현재 날짜와 시간을 설정한다
        String nowTime = LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
        log.info("현재 시간: {}", nowTime);
        return nowTime;
    }

    // @Tool은 매개변수에 대한 설명, @ToolParam은 메소드에 대한 설명
    @Tool(description = "지정된 시간에 알람을 설정한다. 지정된 시간의 형식은 ISO-8601 형식의 시간이다.")
    public void setAlarm(@ToolParam(description = "ISO-8601 형식의 시간", required = true) String time) {

        /*
         * LLM은 다음과 같은 값을 제공할 수 있습니다.
         * 2025-07-03T24:12:29+09:00
         * 하지만 이 값은 유효하지 않은 ISO-8601 날짜/시간 포맷입니다.
         * 시간의 유효 범위를 0 ~ 23 으로 제한하기 때문에 24:12:29 는 파싱 불가능합니다.
         * 따라서 24:... 를 00:... 로 변환하면서 날짜를 다음 날로 증가시켜야 합니다.
         */

        // "T24:" 패턴처리
        if (time.contains("T24:")) {
            int tIndex = time.indexOf("T");
            String datePart = time.substring(0, tIndex);
            String timePart = time.substring(tIndex + 1);
            // 날짜 +1
            LocalDate date = LocalDate.parse(datePart);
            date = date.plusDays(1);
            // "24:" → "00:"으로 교체
            timePart = timePart.replaceFirst("24:", "00:");
            // 재조합
            time = date + "T" + timePart;
        }
        // 파싱 시도
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        log.info("알람 설정 시간: " + alarmTime);
    }
}
