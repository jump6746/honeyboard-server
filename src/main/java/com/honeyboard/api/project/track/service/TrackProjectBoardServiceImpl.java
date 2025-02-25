package com.honeyboard.api.project.track.service;

import com.honeyboard.api.common.model.CreateResponse;
import com.honeyboard.api.exception.BusinessException;
import com.honeyboard.api.exception.ErrorCode;
import com.honeyboard.api.project.track.mapper.TrackProjectBoardMapper;
import com.honeyboard.api.project.track.model.request.TrackProjectBoardRequest;
import com.honeyboard.api.project.track.model.response.TrackProjectBoardDetail;
import com.honeyboard.api.project.track.model.response.TrackProjectBoardList;
import com.honeyboard.api.project.track.model.response.TrackProjectDetail;
import com.honeyboard.api.web.recommend.mapper.WebRecommendMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackProjectBoardServiceImpl implements TrackProjectBoardService{

    private final TrackProjectBoardMapper trackProjectBoardMapper;
    private final WebRecommendMapper webRecommendMapper;


    @Override
    public TrackProjectBoardDetail getBoard(int trackProjectId, int trackTeamId, int boardId) {
        validateBoardId(trackProjectId, trackTeamId, boardId);

        log.info("게시글 상세 조회 시작 - 게시글ID: {}", boardId);
        return trackProjectBoardMapper.selectTrackProjectBoard(trackProjectId, trackTeamId, boardId);
    }

    // 관통 게시글 추가
    @Override
    @Transactional
    public CreateResponse addBoard(int trackProjectId, int trackTeamId, int userId, TrackProjectBoardRequest board) {

        if (trackProjectId <= 0) {
            // 유효하지 않은 Project ID
            throw new BusinessException(ErrorCode.INVALID_PROJECT_ID);
        }
        validateBoard(board);

        log.info("게시글 생성 시작 - 제목: {}", board.getTitle());
        CreateResponse createResponse = new CreateResponse();
        int result = trackProjectBoardMapper.insertTrackProjectBoard(trackProjectId, trackTeamId, userId, board, createResponse);

        if (result <= 0) {
            log.info("게시글 생성 실패 - 제목: {}", board.getTitle());
            throw new BusinessException(ErrorCode.BOARD_CREATE_FAILED);
        }


        log.info("게시글 생성 완료, 팀 제출 현황 업데이트 시작 - 게시글 ID: {}, 팀 ID: {}", createResponse.getId(), trackTeamId);
        // is_completed 업데이트 추가
        result = trackProjectBoardMapper.updateTeamCompleted(trackTeamId);

        if(result <= 0) {
            log.info("팀 제출 현황 업데이트 실패 - 팀 ID: {}", trackTeamId);
            throw new BusinessException(ErrorCode.TEAM_STATUS_UPDATE_FAILED);
        }

        log.info("팀 제출 현황 업데이트 성공 - 팀 ID: {}", trackTeamId);

        return createResponse;
    }

    // 관통 게시글 수정
    @Override
    public void updateBoard(int trackProjectId, int trackTeamId, int boardId, TrackProjectBoardRequest board) {
        validateBoardId(trackProjectId, trackTeamId, boardId);
        validateBoard(board);

        log.info("게시글 수정 시작 - ID: {}", boardId);
        int result = trackProjectBoardMapper.updateTrackProjectBoard(trackProjectId, trackTeamId, boardId, board);

        if (result <= 0) {
            log.info("게시글 수정 실패 - 제목: {}", board.getTitle());
            throw new BusinessException(ErrorCode.BOARD_UPDATE_FAILED);
        }

        log.info("게시글 수정 완료 - ID: {}", boardId);
    }

    // 관통 게시글 삭제
    @Override
    @Transactional
    public void softDeleteBoard(int trackProjectId, int trackTeamId, int boardId) {
        validateBoardId(trackProjectId, trackTeamId, boardId);
        log.info("게시글 삭제 시작 - ID: {}", boardId);
        int result = trackProjectBoardMapper.deleteTrackProjectBoard(boardId);

        if (result != 1) {
            log.info("게시글 삭제 실패 - 제목: {}", boardId);
            throw new BusinessException(ErrorCode.BOARD_DELETE_FAILED);
        }

        log.info("게시글 삭제 완료 - ID: {}", boardId);

        log.info("팀 삭제 처리 - ID: {}", trackTeamId);
        result = trackProjectBoardMapper.updateTeamCompleted(trackTeamId);

        if(result != 1) {
            log.info("팀 삭제 처리 실패 - ID: {}", trackTeamId);
            throw new BusinessException(ErrorCode.TEAM_STATUS_UPDATE_FAILED);
        }

        log.info("팀 삭제 처리 완료 - ID: {}", trackTeamId);
    }

    private void validateBoardId(int trackProjectId, int trackTeamId, int boardId) {
        if (trackProjectId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_ID);
        }
        if (trackTeamId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TEAM_ID);
        }
        if (boardId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_ID);
        }
    }

    private void validateBoard(TrackProjectBoardRequest board) {
        if (board == null) {
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }
        if (board.getTitle() == null || board.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BOARD_TITLE_REQUIRED);
        }
        if (board.getContent() == null || board.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BOARD_CONTENT_REQUIRED);
        }
    }
}
