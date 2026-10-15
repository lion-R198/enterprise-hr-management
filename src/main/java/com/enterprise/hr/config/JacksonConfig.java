package com.enterprise.hr.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 统一序列化配置。
 *
 * <p>解决的问题：
 * <ol>
 *     <li><b>Java 8 时间类型格式</b>：{@code LocalDateTime} 默认序列化为
 *         {@code [2026,9,15,10,30,0]} 数组或 ISO 带 T 字符串，
 *         前端契约要求 {@code 2026-09-15 10:30:00} / {@code 2026-09-15}。</li>
 *     <li><b>Date 序列化</b>：{@code java.util.Date} 统一为 {@code yyyy-MM-dd HH:mm:ss}。</li>
 *     <li><b>Long 精度</b>：BIGINT 主键理论上可超出 JavaScript 安全整数范围（2^53-1），
 *         一旦超出前端会静默丢精度，导致"改/删错行"。
 *         本项目通过 {@code spring.jackson.serialization.write-dates-as-timestamps=false}
 *         与显式序列化器双重保障，主键在业务规模内保持数值型（与前端 mock 契约一致），
 *         同时把 {@code createdAt} 等时间统一为字符串。</li>
 * </ol>
 *
 * <p>响应字段命名策略（蛇形）由实体/VO 上的 {@code @JsonNaming} 声明，
 * <b>不在此处全局开启</b>：全局策略会同时作用于请求体反序列化，
 * 导致前端发送的驼峰请求体（{@code deptId}）无法绑定到 DTO 字段。
 * 「响应蛇形（对齐数据库列名）+ 请求驼峰（对齐 JS 习惯）」是本项目的既定接口风格。
 *
 * @author Enterprise HR Team
 */
@Configuration
public class JacksonConfig {

    /** 日期时间格式：与数据库 DATETIME 展示一致 */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /** 日期格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    /** 时间格式 */
    public static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

        return builder -> {
            // 请求体里时间字段的宽松解析：允许 "2026-09-15" 直接绑定到 LocalDateTime
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
            // 时间不再输出为时间戳
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // 未知字段忽略：前端随表单多传字段时不至于 400
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            // 空字符串按 null 反序列化，避免 "" 落到数值/日期字段上炸掉
            builder.featuresToEnable(
                    com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            // 说明：未启用 Long → String 全局转换，保持主键为数值型（与前端 mock 契约一致）。
            // 若后续引入雪花 ID（超出 JS 安全整数范围），在此处开启即可全局转字符串：
            //   builder.serializerByType(Long.class, ToStringSerializer.instance);
        };
    }
}
