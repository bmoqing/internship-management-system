/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.service;

import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.entity.InternshipRecord;
import fun.bmoqing.internship.mapper.InternshipRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InternshipRecordService {

    private final InternshipRecordMapper internshipRecordMapper;

    public InternshipRecordService(InternshipRecordMapper internshipRecordMapper) {
        this.internshipRecordMapper = internshipRecordMapper;
    }

    public void addRecord(Long studentId, String eventType, String eventDetail, Long relatedId) {
        if (studentId == null) {
            return;
        }

        InternshipRecord record = new InternshipRecord();
        record.setStudentId(studentId);
        record.setEventType(eventType);
        record.setEventDetail(eventDetail);
        record.setRelatedId(relatedId);
        record.setOperatorId(AuthUtil.currentUserId());
        record.setCreateTime(LocalDateTime.now());
        internshipRecordMapper.insert(record);
    }
}
