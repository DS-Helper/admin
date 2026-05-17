package com.project.ds_helper.domain.board;

import com.project.ds_helper.domain.board.dto.response.GetLikedBoardsResponseDto;
import com.project.ds_helper.domain.board.dto.response.GetMyBoardsResponseDto;
import com.project.ds_helper.domain.board.entity.Board;
import com.project.ds_helper.domain.board.entity.BoardImage;
import com.project.ds_helper.domain.board.entity.BoardLike;
import com.project.ds_helper.domain.board.entity.BoardScrap;
import com.project.ds_helper.domain.board.enums.BoardCategory;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardDtoEntityTest {

    @Test
    @DisplayName("BoardCategory는 한글명으로 카테고리를 찾고 잘못된 값은 거부한다")
    void boardCategory_findsByKoreanAndRejectsInvalidValue() {
        assertThat(BoardCategory.findByKorean(BoardCategory.DAILY.getKorean())).isEqualTo(BoardCategory.DAILY);

        assertThatThrownBy(() -> BoardCategory.findByKorean("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid BoardCategory");
    }

    @Test
    @DisplayName("Board 엔티티는 카운트와 이미지 관계를 관리한다")
    void board_updatesCountersAndImageRelation() {
        Board board = board();
        BoardImage image = BoardImage.builder().storedName("stored.png").build();

        board.increaseLikeCount();
        board.decreaseLikeCount();
        board.increaseViewCount();
        board.decreaseViewCount();
        board.increaseCommentCount();
        board.decreaseCommentCount();
        board.addImage(image);
        board.removeImage(image);
        board.update("updated title", "updated content");
        board.softDelete();

        assertThat(board.getLikeCount()).isZero();
        assertThat(board.getViewCount()).isZero();
        assertThat(board.getCommentCount()).isZero();
        assertThat(board.getBoardImages()).isEmpty();
        assertThat(image.getBoard()).isNull();
        assertThat(board.getTitle()).isEqualTo("updated title");
        assertThat(board.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Board 계열 엔티티는 PrePersist에서 ID를 생성한다")
    void boardEntities_generateIds() {
        Board board = board();
        BoardImage image = BoardImage.builder().build();
        BoardLike like = BoardLike.builder().board(board).user(user()).build();
        BoardScrap scrap = BoardScrap.builder().board(board).user(user()).build();
        board.setId(null);

        ReflectionTestUtils.invokeMethod(board, "prePersistGenerateId");
        image.generatedId();
        ReflectionTestUtils.invokeMethod(like, "prePersistGenerateId");
        ReflectionTestUtils.invokeMethod(scrap, "prePersistGenerateId");

        assertThat(board.getId()).isNotBlank();
        assertThat(image.getId()).isNotBlank();
        assertThat(like.getId()).isNotBlank();
        assertThat(scrap.getId()).isNotBlank();
    }

    @Test
    @DisplayName("Board response DTO는 엔티티 값을 응답 값으로 변환한다")
    void boardResponseDtos_convertEntityValues() {
        Board board = board();

        GetMyBoardsResponseDto myBoard = GetMyBoardsResponseDto.from(board, 3, true, "thumb");
        GetLikedBoardsResponseDto.Board likedBoard = GetLikedBoardsResponseDto.Board.toBoard(board, "thumb");

        assertThat(myBoard.id()).isEqualTo("board-1");
        assertThat(myBoard.writerId()).isEqualTo("user-1");
        assertThat(myBoard.commentCount()).isEqualTo(3);
        assertThat(myBoard.isLiked()).isTrue();
        assertThat(likedBoard.getId()).isEqualTo("board-1");
        assertThat(likedBoard.getThumbNailUrl()).isEqualTo("thumb");
    }

    private Board board() {
        return Board.builder()
                .id("board-1")
                .user(user())
                .category("DAILY")
                .title("title")
                .content("content")
                .build();
    }

    private User user() {
        return User.builder()
                .id("user-1")
                .name("tester")
                .profileImageUrl("profile")
                .build();
    }
}
