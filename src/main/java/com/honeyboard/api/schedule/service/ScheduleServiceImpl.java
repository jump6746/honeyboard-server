package com.honeyboard.api.schedule.service;

import com.honeyboard.api.exception.BusinessException;
import com.honeyboard.api.exception.ErrorCode;
import com.honeyboard.api.schedule.mapper.ScheduleMapper;
import com.honeyboard.api.schedule.model.request.SceduleRequest;
import com.honeyboard.api.schedule.model.response.ScheduleList;
import com.honeyboard.api.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

	private final ScheduleMapper scheduleMapper;
	private final UserMapper userMapper;

	// 일정 추가
	@Override
	public void addSchedule(SceduleRequest schedule, int userId) {
		log.info("일정 추가 시작 - 내용: {}", schedule.getContent());

		if (schedule == null) {
			throw new IllegalArgumentException("일정 정보가 없습니다.");
		}

		log.info("수정 전 일정 시간 - 시작: {}, 종료: {}", schedule.getStartDate(), schedule.getEndDate());
		// 날짜 변환 (UTC to KST)
		LocalDateTime startDate = LocalDateTime.parse(schedule.getStartDate(), DateTimeFormatter.ISO_DATE_TIME)
				.plusHours(9);
		LocalDateTime endDate = LocalDateTime.parse(schedule.getEndDate(), DateTimeFormatter.ISO_DATE_TIME)
				.plusHours(9);

		schedule.setStartDate(startDate.toLocalDate().toString());
		schedule.setEndDate(endDate.toLocalDate().toString());

		log.info("수정 후 일정 시간 - 시작: {}, 종료: {}", schedule.getStartDate(), schedule.getEndDate());
		Integer generationId = userMapper.selectActiveGenerationId();

		if (generationId == null) {
			throw new IllegalArgumentException("활성화된 기수 정보를 찾을 수 없습니다.");
		}

		int result = scheduleMapper.insertSchedule(schedule, userId, generationId);

		if (result != 1) {
			throw new BusinessException(ErrorCode.SCHEDULE_CREATE_FAILED);
		}

		log.info("일정 추가 완료 - 일정 내용: {}", schedule.getContent());
	}

	// 일정 조회
	@Override
	public List<ScheduleList> getScheduleByMonth(int year, int month, Integer generationId, String role) {
		log.info("월별 일정 조회 시작 - year: {}, month: {}, generationId: {}, role: {}",
				year, month, generationId, role);

		if (year <= 0 || month <= 0 || month > 12) {
			throw new IllegalArgumentException("유효하지 않은 날짜입니다.");
		}

		List<ScheduleList> schedules = scheduleMapper.selectScheduleByMonth(year, month, generationId, role);
		List<ScheduleList> trackProjects = scheduleMapper.selectTrackProjectByMonth(year, month, generationId);

		// trackProjects를 schedules에 추가
		schedules.addAll(trackProjects);

		//날짜순 정렬
		Collections.sort(schedules, (s1, s2) -> s1.getStartDate().compareTo(s2.getStartDate()));

		log.info("월별 일정 조회 완료 - 조회된 일정 수: {}", schedules.size());
		return schedules;
	}

	// 일정 수정
	@Override
	public void updateSchedule(SceduleRequest schedule, int id) {
		log.info("일정 수정 시작 - 내용: {}", schedule.getContent());

		if (schedule == null) {
			throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND);
		}

		// 날짜 변환 (UTC to KST)
		LocalDateTime startDate = LocalDateTime.parse(schedule.getStartDate(), DateTimeFormatter.ISO_DATE_TIME)
				.plusHours(9);
		LocalDateTime endDate = LocalDateTime.parse(schedule.getEndDate(), DateTimeFormatter.ISO_DATE_TIME)
				.plusHours(9);

		schedule.setStartDate(startDate.toLocalDate().toString());
		schedule.setEndDate(endDate.toLocalDate().toString());

		int result = scheduleMapper.updateSchedule(schedule, id);

		if (result != 1) {
			throw new BusinessException(ErrorCode.SCHEDULE_UPDATE_FAILED);
		}

		log.info("일정 수정 완료 - 내용: {}", schedule.getContent());
	}

	//일정 삭제
	@Override
	public void deleteSchedule(int id) {
		log.info("일정 삭제 시작 - 일정 ID: {}", id);

		if (id <= 0) {
			throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND);
		}

		int result = scheduleMapper.deleteSchedule(id);

		if (result != 1) {
			throw new BusinessException(ErrorCode.SCHEDULE_DELETE_FAILED);
		}

		log.info("일정 삭제 완료 - 일정 ID: {}", id);
	}

}
